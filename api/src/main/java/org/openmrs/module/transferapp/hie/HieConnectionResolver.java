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
package org.openmrs.module.transferapp.hie;

import org.apache.commons.lang.StringUtils;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.context.Context;
import org.openmrs.module.transferapp.TransferAppConstants;

public class HieConnectionResolver {

	public HieBasicConnection resolveConnection() {
		AdministrationService adminService = Context.getAdministrationService();

		String primaryUrl = StringUtils.trimToNull(adminService.getGlobalProperty(TransferAppConstants.GP_RWANDAEMR_HIE_URL));
		String primaryUsername = StringUtils.trimToNull(adminService.getGlobalProperty(TransferAppConstants.GP_RWANDAEMR_HIE_USERNAME));
		String primaryPassword = StringUtils.trimToNull(adminService.getGlobalProperty(TransferAppConstants.GP_RWANDAEMR_HIE_PASSWORD));

		if (isComplete(primaryUrl, primaryUsername, primaryPassword)) {
			return new HieBasicConnection(normalizeBaseUrl(primaryUrl), primaryUsername, primaryPassword);
		}

		String fallbackUrl = StringUtils.trimToNull(adminService.getGlobalProperty(TransferAppConstants.GP_HIE_URL));
		String fallbackUsername = StringUtils.trimToNull(adminService.getGlobalProperty(TransferAppConstants.GP_HIE_USERNAME));
		String fallbackPassword = StringUtils.trimToNull(adminService.getGlobalProperty(TransferAppConstants.GP_HIE_PASSWORD));

		if (!isComplete(fallbackUrl, fallbackUsername, fallbackPassword)) {
			throw new HieConfigurationException(
					"HIE credentials are not configured. Set rwandaemr.hie.url, rwandaemr.hie.username, and rwandaemr.hie.password, "
							+ "or transferapp.hie.url, transferapp.hie.username, and transferapp.hie.password.");
		}

		return new HieBasicConnection(normalizeBaseUrl(fallbackUrl), fallbackUsername, fallbackPassword);
	}

	private static boolean isComplete(String baseUrl, String username, String password) {
		return StringUtils.isNotBlank(baseUrl)
				&& StringUtils.isNotBlank(username)
				&& StringUtils.isNotBlank(password);
	}

	public static String normalizeBaseUrl(String baseUrl) {
		if (baseUrl == null) {
			return "";
		}
		String trimmed = baseUrl.trim();
		while (trimmed.endsWith("/")) {
			trimmed = trimmed.substring(0, trimmed.length() - 1);
		}
		if (trimmed.endsWith("/api/v1")) {
			trimmed = trimmed.substring(0, trimmed.length() - "/api/v1".length());
		}
		return trimmed;
	}

	public boolean isHieConfigured() {
		try {
			resolveConnection();
			return true;
		}
		catch (HieConfigurationException ex) {
			return false;
		}
	}

}
