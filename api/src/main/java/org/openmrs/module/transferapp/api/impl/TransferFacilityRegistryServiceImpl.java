/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package org.openmrs.module.transferapp.api.impl;

import org.apache.commons.lang.StringUtils;
import org.openmrs.api.APIException;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.context.Context;
import org.openmrs.module.transferapp.TransferAppConstants;
import org.openmrs.module.transferapp.api.TransferFacilityRegistryService;
import org.openmrs.module.transferapp.hie.HieApiException;
import org.openmrs.module.transferapp.hie.HieBasicConnection;
import org.openmrs.module.transferapp.hie.HieConfigurationException;
import org.openmrs.module.transferapp.hie.HieConnectionResolver;
import org.openmrs.module.transferapp.hie.HieFacilityBundleParser;
import org.openmrs.module.transferapp.hie.HieShrClient;
import org.openmrs.module.transferapp.model.RegistryFacility;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TransferFacilityRegistryServiceImpl implements TransferFacilityRegistryService {

	private static final int DEFAULT_PAGE = 1;

	private static final int DEFAULT_LIMIT = 5000;

	/**
	 * Only hospital-level facilities are selectable as transfer destinations.
	 * Matching is case-insensitive against {@code urn:frpr:org-category}.
	 */
	private static final Set<String> RECEIVING_CATEGORIES = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
			"district hospital",
			"referral hospital",
			"provincial hospital",
			"specialised hospital",
			"specialized hospital",
			"private hospital",
			"medical clinic")));

	/**
	 * Counter-referral destinations: health centres/posts plus hospital levels.
	 */
	private static final Set<String> COUNTER_REFERRAL_CATEGORIES = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
			"health center",
			"health centre",
			"medical clinic",
			"health post",
			"referral hospital",
			"health post 2nd generation",
			"health post 2nd-generation",
			"specialised hospital",
			"specialized hospital",
			"provincial hospital",
			"district hospital")));

	/**
	 * Facilities allowed as ambulance vehicle providers on outbound transfers.
	 */
	private static final Set<String> AMBULANCE_PROVIDER_CATEGORIES = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
			"health center",
			"health centre",
			"district hospital",
			"specialised hospital",
			"specialized hospital",
			"referral hospital",
			"provincial hospital",
			"medical clinic")));

	private HieConnectionResolver hieConnectionResolver = new HieConnectionResolver();

	private HieShrClient hieShrClient = new HieShrClient();

	private HieFacilityBundleParser facilityBundleParser = new HieFacilityBundleParser();

	@Override
	public List<RegistryFacility> listReceivingFacilitiesFromHie() {
		return listFacilitiesFromHie(RECEIVING_CATEGORIES);
	}

	@Override
	public List<RegistryFacility> listCounterReferralFacilitiesFromHie() {
		return listFacilitiesFromHie(COUNTER_REFERRAL_CATEGORIES);
	}

	@Override
	public List<RegistryFacility> listAmbulanceProviderFacilitiesFromHie() {
		return listFacilitiesFromHie(AMBULANCE_PROVIDER_CATEGORIES);
	}

	private List<RegistryFacility> listFacilitiesFromHie(Set<String> allowedCategories) {
		String authToken = resolveAuthToken();
		if (StringUtils.isBlank(authToken)) {
			throw new APIException("Facility registry token is not configured (transferapp.fr_token)");
		}

		HieBasicConnection connection;
		try {
			connection = hieConnectionResolver.resolveConnection();
		}
		catch (HieConfigurationException ex) {
			throw new APIException(ex.getMessage());
		}

		String path = TransferAppConstants.HIE_FACILITY_REGISTRY_PATH
				+ "?page=" + DEFAULT_PAGE + "&limit=" + DEFAULT_LIMIT;
		String json;
		try {
			json = hieShrClient.get(connection, path, authToken);
		}
		catch (HieApiException ex) {
			throw new APIException(ex.getMessage());
		}

		validateBundleResponse(json);
		List<RegistryFacility> facilities = facilityBundleParser.parse(json);
		List<RegistryFacility> filtered = new ArrayList<RegistryFacility>();
		for (RegistryFacility facility : facilities) {
			if (facility != null && isAllowedCategory(facility.getCategory(), allowedCategories)) {
				filtered.add(facility);
			}
		}

		Collections.sort(filtered, new Comparator<RegistryFacility>() {
			@Override
			public int compare(RegistryFacility left, RegistryFacility right) {
				return String.CASE_INSENSITIVE_ORDER.compare(
						left != null && left.getName() != null ? left.getName() : "",
						right != null && right.getName() != null ? right.getName() : "");
			}
		});
		return filtered;
	}

	private String resolveAuthToken() {
		AdministrationService adminService = Context.getAdministrationService();
		return StringUtils.trimToNull(adminService.getGlobalProperty(TransferAppConstants.GP_FR_TOKEN));
	}

	private static boolean isAllowedCategory(String category, Set<String> allowedCategories) {
		if (StringUtils.isBlank(category) || allowedCategories == null || allowedCategories.isEmpty()) {
			return false;
		}
		return allowedCategories.contains(normalizeCategory(category));
	}

	private static String normalizeCategory(String category) {
		return category.trim().toLowerCase().replaceAll("[-_]+", " ").replaceAll("\\s+", " ");
	}

	private static void validateBundleResponse(String body) {
		if (StringUtils.isBlank(body)) {
			throw new APIException("HIE facility registry returned an empty response");
		}
		String trimmed = body.trim();
		if (trimmed.contains("\"status\":\"error\"") || trimmed.contains("\"status\": \"error\"")) {
			throw new APIException("HIE facility registry returned an error response");
		}
		if (!trimmed.contains("\"resourceType\"") || !trimmed.contains("Bundle")) {
			throw new APIException("HIE facility registry did not return a FHIR Bundle");
		}
	}

}
