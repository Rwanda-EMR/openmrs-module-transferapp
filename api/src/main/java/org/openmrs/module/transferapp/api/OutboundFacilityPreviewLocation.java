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
package org.openmrs.module.transferapp.api;

import org.apache.commons.lang.StringUtils;
import org.openmrs.api.context.Context;
import org.openmrs.module.transferapp.TransferAppConstants;
import org.openmrs.module.transferapp.api.impl.TransferRegistrationObsServiceImpl;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Preview-only Province/District fallback when a transfer targets this facility
 * ({@code transferapp.outboundFacilityName}) but HIE omitted those fields.
 * <p>
 * Must not be used when building outbound HIE payloads.
 */
public final class OutboundFacilityPreviewLocation {

	private OutboundFacilityPreviewLocation() {
	}

	/**
	 * If the transfer destination matches {@code transferapp.outboundFacilityName} (or is already
	 * marked {@code targetsCurrentFacility}) and province/district are blank, fills them from
	 * {@code transferapp.outboundProvince} / {@code transferapp.outboundDistrict}.
	 */
	public static void applyProvinceDistrictFallback(Map<String, Object> preview) {
		if (preview == null || preview.isEmpty()) {
			return;
		}
		String outboundFacilityName = StringUtils.trimToNull(Context.getAdministrationService()
				.getGlobalProperty(TransferAppConstants.GP_OUTBOUND_FACILITY_NAME));
		String outboundProvince = StringUtils.trimToNull(Context.getAdministrationService()
				.getGlobalProperty(TransferAppConstants.GP_OUTBOUND_PROVINCE));
		String outboundDistrict = StringUtils.trimToNull(Context.getAdministrationService()
				.getGlobalProperty(TransferAppConstants.GP_OUTBOUND_DISTRICT));
		if (outboundFacilityName == null || (outboundProvince == null && outboundDistrict == null)) {
			return;
		}
		if (!targetsOutboundFacility(preview, outboundFacilityName)) {
			return;
		}
		putIfBlank(preview, "province", outboundProvince);
		putIfBlank(preview, "district", outboundDistrict);
		putIfBlank(preview, "receivingProvince", outboundProvince);
		putIfBlank(preview, "receivingDistrict", outboundDistrict);
	}

	private static boolean targetsOutboundFacility(Map<String, Object> preview, String outboundFacilityName) {
		if (Boolean.TRUE.equals(preview.get("targetsCurrentFacility"))) {
			return true;
		}
		List<String> outboundOnly = Collections.singletonList(outboundFacilityName);
		List<String> destinationCandidates = Arrays.asList(
				asTrimmedString(preview.get("destinationDisplay")),
				asTrimmedString(preview.get("destination")),
				asTrimmedString(preview.get("receivingFacility")),
				asTrimmedString(preview.get("receivingFacilityCode")),
				asTrimmedString(preview.get("toFacility")));
		for (String candidate : destinationCandidates) {
			if (TransferRegistrationObsServiceImpl.destinationMatchesAnyFacility(candidate, outboundOnly)) {
				return true;
			}
		}
		return false;
	}

	private static void putIfBlank(Map<String, Object> preview, String key, String value) {
		if (value == null || StringUtils.isNotBlank(asTrimmedString(preview.get(key)))) {
			return;
		}
		preview.put(key, value);
	}

	private static String asTrimmedString(Object value) {
		return value == null ? null : StringUtils.trimToNull(String.valueOf(value));
	}

}
