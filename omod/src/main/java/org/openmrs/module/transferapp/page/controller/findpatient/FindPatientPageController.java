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
package org.openmrs.module.transferapp.page.controller.findpatient;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonNode;
import org.openmrs.module.appframework.domain.AppDescriptor;
import org.openmrs.module.appui.UiSessionContext;
import org.openmrs.module.transferapp.api.ProviderLicenseRequirement;
import org.openmrs.ui.framework.UiUtils;
import org.openmrs.ui.framework.page.PageModel;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Same contract as coreapps Find Patient, with approver / license banners in the GSP.
 * Redirects provider-role users missing Provider License to My Profile when possible.
 */
public class FindPatientPageController {

	private static final Log log = LogFactory.getLog(FindPatientPageController.class);

	public String get(PageModel model, @RequestParam("app") AppDescriptor app, UiSessionContext sessionContext,
			UiUtils ui) {
		try {
			if (ProviderLicenseRequirement.shouldPromptForProviderLicense()) {
				Map<String, Object> params = new LinkedHashMap<String, Object>();
				params.put("app", "transferapp.dashboard");
				return "redirect:" + ui.pageLinkWithoutContextPath("transferapp", "transferProfile", params);
			}
		}
		catch (Exception ex) {
			log.warn("Unable to evaluate provider license redirect on find patient", ex);
		}

		model.addAttribute("afterSelectedUrl", app.getConfig().get("afterSelectedUrl").getTextValue());
		model.addAttribute("heading", app.getConfig().get("heading").getTextValue());
		model.addAttribute("label", app.getConfig().get("label").getTextValue());
		model.addAttribute("showLastViewedPatients", app.getConfig().get("showLastViewedPatients").getBooleanValue());
		if (app.getConfig().get("registrationAppLink") == null) {
			model.addAttribute("registrationAppLink", "");
		}
		else {
			model.addAttribute("registrationAppLink", app.getConfig().get("registrationAppLink").getTextValue());
		}
		model.addAttribute("columnConfig", app.getConfig().get("columnConfig"));

		// Mirror coreapps BreadcrumbHelper for apps that define breadcrumbs in config.
		// Default findPatient apps leave this null and the GSP uses Home -> label.
		JsonNode breadcrumbsNode = app.getConfig() != null ? app.getConfig().get("breadcrumbs") : null;
		if (breadcrumbsNode != null && !breadcrumbsNode.isNull()) {
			model.addAttribute("breadcrumbs", breadcrumbsNode.toString());
		}
		else {
			model.addAttribute("breadcrumbs", null);
		}
		return null;
	}
}
