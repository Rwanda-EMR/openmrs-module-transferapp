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
package org.openmrs.module.transferapp.fragment.controller.home;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.module.transferapp.api.ProviderLicenseRequirement;
import org.openmrs.ui.framework.fragment.FragmentModel;

/**
 * Danger banner when a configured provider-role user is missing Provider License.
 */
public class ProviderLicenseRequiredBannerFragmentController {

	private static final Log log = LogFactory.getLog(ProviderLicenseRequiredBannerFragmentController.class);

	public void controller(FragmentModel model) {
		boolean showBanner = false;
		try {
			showBanner = ProviderLicenseRequirement.shouldPromptForProviderLicense();
		}
		catch (Exception ex) {
			log.warn("Unable to resolve provider license required banner state", ex);
			showBanner = false;
		}
		model.addAttribute("showBanner", showBanner);
	}

}
