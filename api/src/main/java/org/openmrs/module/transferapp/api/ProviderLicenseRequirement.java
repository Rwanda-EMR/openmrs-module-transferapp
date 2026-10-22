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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.User;
import org.openmrs.api.context.Context;
import org.openmrs.module.transferapp.TransferAppConstants;

import java.util.List;

/**
 * Determines when the authenticated user must complete Provider License on My Profile.
 */
public final class ProviderLicenseRequirement {

	private static final Log log = LogFactory.getLog(ProviderLicenseRequirement.class);

	private ProviderLicenseRequirement() {
	}

	/**
	 * True when the current user has one of {@code transferapp.providerRoles} and does not yet have
	 * a {@code Provider License} attribute recorded.
	 */
	public static boolean shouldPromptForProviderLicense() {
		User user = Context.getAuthenticatedUser();
		if (user == null) {
			return false;
		}
		List<String> roleNames = configuredProviderRoles();
		if (!currentUserHasConfiguredProviderRole(user)) {
			if (log.isDebugEnabled()) {
				log.debug("Skip provider-license prompt for user '" + user.getUsername()
						+ "': none of roles " + roleNames + " matched");
			}
			return false;
		}
		if (ProviderProfileAttributes.hasProviderLicense(user)) {
			if (log.isDebugEnabled()) {
				log.debug("Skip provider-license prompt for user '" + user.getUsername()
						+ "': Provider License attribute already present");
			}
			return false;
		}
		return true;
	}

	public static boolean currentUserHasConfiguredProviderRole(User user) {
		if (user == null) {
			return false;
		}
		List<String> roleNames = configuredProviderRoles();
		if (roleNames.isEmpty()) {
			return false;
		}
		for (String roleName : roleNames) {
			if (user.hasRole(roleName, true)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * When true, My Profile blocks save until HIE Provider Registry validates the license.
	 * Default is false ({@link TransferAppConstants#DEFAULT_LICENSE_VALIDATION_ENABLED}).
	 */
	public static boolean isLicenseValidationEnabled() {
		String raw = StringUtils.trimToEmpty(Context.getAdministrationService().getGlobalProperty(
				TransferAppConstants.GP_LICENSE_VALIDATION_ENABLED,
				TransferAppConstants.DEFAULT_LICENSE_VALIDATION_ENABLED));
		if (StringUtils.isBlank(raw)) {
			raw = TransferAppConstants.DEFAULT_LICENSE_VALIDATION_ENABLED;
		}
		return Boolean.parseBoolean(raw);
	}

	public static List<String> configuredProviderRoles() {
		String raw = StringUtils.trimToNull(Context.getAdministrationService()
				.getGlobalProperty(TransferAppConstants.GP_PROVIDER_ROLES,
						TransferAppConstants.DEFAULT_PROVIDER_ROLES));
		if (raw == null) {
			raw = TransferAppConstants.DEFAULT_PROVIDER_ROLES;
		}
		return TransferSendingLocationResolver.parseFacilityNames(raw);
	}

}
