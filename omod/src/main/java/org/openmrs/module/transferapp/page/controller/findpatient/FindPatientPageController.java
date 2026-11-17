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
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.node.ObjectNode;
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

		model.addAttribute("breadcrumbs", breadcrumbsJson(app, ui));
		return null;
	}

	/**
	 * Does what coreapps' BreadcrumbHelper does for the coreapps Find Patient page (that class is in the coreapps
	 * omod, which this module doesn't depend on): adds the context path to each link, translates each label, and
	 * links the last breadcrumb to this page. Returns null if the app doesn't define breadcrumbs, so the GSP uses
	 * Home -> label.
	 */
	static String breadcrumbsJson(AppDescriptor app, UiUtils ui) {
		JsonNode config = app.getConfig() != null ? app.getConfig().get("breadcrumbs") : null;
		if (config == null || config.isNull()) {
			return null;
		}
		if (!config.isArray()) {
			throw new IllegalStateException("breadcrumbs defined in " + app + " must be null or an array");
		}
		String currentUrl = ui.thisUrlWithContextPath();
		if (currentUrl.endsWith("&")) {
			currentUrl = currentUrl.substring(0, currentUrl.length() - 1);
		}
		ArrayNode breadcrumbs = JsonNodeFactory.instance.arrayNode();
		for (int i = 0; i < config.size(); i++) {
			ObjectNode breadcrumb = JsonNodeFactory.instance.objectNode();
			JsonNode item = config.get(i);
			if (hasText(item, "icon")) {
				breadcrumb.put("icon", item.get("icon").getTextValue());
			}
			if (i == config.size() - 1) {
				breadcrumb.put("link", currentUrl);
			}
			else if (hasText(item, "link")) {
				breadcrumb.put("link", "/" + ui.contextPath() + item.get("link").getTextValue());
			}
			if (hasText(item, "label")) {
				breadcrumb.put("label", ui.message(item.get("label").getTextValue()));
			}
			breadcrumbs.add(breadcrumb);
		}
		return breadcrumbs.toString();
	}

	private static boolean hasText(JsonNode item, String field) {
		return item.get(field) != null && !item.get(field).isNull();
	}
}
