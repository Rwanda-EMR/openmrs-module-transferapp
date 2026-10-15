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
package org.openmrs.module.transferapp.page.controller;

import org.openmrs.PersonName;
import org.openmrs.User;
import org.openmrs.api.context.Context;
import org.openmrs.module.appui.UiSessionContext;
import org.openmrs.module.transferapp.api.ProviderProfileAttributes;
import org.openmrs.module.transferapp.api.TransferProfileService;
import org.openmrs.module.transferapp.model.TransferProfile;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.page.PageModel;

import java.util.Map;

public class TransferProfilePageController {

	public void get(UiSessionContext sessionContext, PageModel model,
			@SpringBean("transferProfileService") TransferProfileService transferProfileService) {

		sessionContext.requireAuthentication();

		User user = Context.getAuthenticatedUser();
		TransferProfile profile = transferProfileService.getProfileForUser(user);
		String displayName = "";
		if (user != null && user.getPerson() != null && user.getPerson().getPersonName() != null) {
			PersonName personName = user.getPerson().getPersonName();
			displayName = personName.getFullName();
		}

		Map<String, String> licenseAttrs = ProviderProfileAttributes.readLicenseValidationForUser(user);

		model.addAttribute("providerDisplayName", displayName);
		model.addAttribute("username", user != null ? user.getUsername() : "");
		model.addAttribute("licenseNumber", profile != null ? profile.getLicenseNumber() : "");
		model.addAttribute("phoneNumber", profile != null ? profile.getPhoneNumber() : "");
		model.addAttribute("qualification", profile != null ? profile.getQualification() : "");
		model.addAttribute("speciality", profile != null ? profile.getSpeciality() : "");
		model.addAttribute("qualificationDisplay", profile != null ? profile.getQualificationWithSpeciality() : "");
		model.addAttribute("licenseValidated", "true".equals(licenseAttrs.get("licenseValidated")));
		model.addAttribute("licenseExpiryDate", licenseAttrs.get("licenseExpiryDate"));
		model.addAttribute("licenseStatus", licenseAttrs.get("licenseStatus"));
	}

}
