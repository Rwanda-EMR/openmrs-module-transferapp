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

import org.openmrs.Location;
import org.openmrs.module.appui.UiSessionContext;
import org.openmrs.module.transferapp.TransferAppActivator;
import org.openmrs.module.transferapp.TransferPrivilegeHelper;
import org.openmrs.module.transferapp.api.TransferAdminService;
import org.openmrs.module.transferapp.model.ReceivingFacility;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.page.PageModel;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Collections;
import java.util.List;

public class TransferAdminPageController {

	public void get(UiSessionContext sessionContext, PageModel model,
			@RequestParam(value = "locationId", required = false) Integer locationId,
			@RequestParam(value = "receivingFacilityId", required = false) Integer receivingFacilityId,
			@SpringBean("transferAdminService") TransferAdminService transferAdminService) {

		sessionContext.requireAuthentication();

		boolean canAccessAdmin = TransferPrivilegeHelper.hasPrivilege(TransferAppActivator.PRIVILEGE_CONFIGURATION);
		model.addAttribute("canAccessAdmin", canAccessAdmin);
		model.addAttribute("requiredAdminPrivilege", TransferAppActivator.PRIVILEGE_CONFIGURATION);
		model.addAttribute("adminAccessDeniedMessage",
				TransferPrivilegeHelper.requiredPrivilegeMessage(TransferAppActivator.PRIVILEGE_CONFIGURATION));

		if (!canAccessAdmin) {
			model.addAttribute("sendingLocations", Collections.emptyList());
			model.addAttribute("selectedLocationId", null);
			model.addAttribute("receivingFacilities", Collections.emptyList());
			model.addAttribute("selectedReceivingFacilityId", null);
			model.addAttribute("receivingServices", Collections.emptyList());
			return;
		}

		try {
			List<Location> sendingLocations = transferAdminService.getSendingLocations();
			model.addAttribute("sendingLocations", sendingLocations);

			Integer selectedLocationId = locationId;
			if (selectedLocationId == null) {
				selectedLocationId = transferAdminService.resolveCurrentSendingLocationId();
			}
			model.addAttribute("selectedLocationId", selectedLocationId);

			if (selectedLocationId != null) {
				List<ReceivingFacility> receivingFacilities = transferAdminService.getReceivingFacilities(selectedLocationId);
				model.addAttribute("receivingFacilities", receivingFacilities);

				Integer selectedReceivingFacilityId = receivingFacilityId;
				if (selectedReceivingFacilityId == null && receivingFacilities != null && !receivingFacilities.isEmpty()) {
					selectedReceivingFacilityId = receivingFacilities.get(0).getReceivingFacilityId();
				}
				model.addAttribute("selectedReceivingFacilityId", selectedReceivingFacilityId);

				if (selectedReceivingFacilityId != null) {
					model.addAttribute("receivingServices",
							transferAdminService.getReceivingServicesByFacility(selectedReceivingFacilityId));
				}
			}
		}
		catch (Exception ex) {
			model.addAttribute("canAccessAdmin", false);
			model.addAttribute("adminAccessDeniedMessage", TransferPrivilegeHelper.resolveUserFacingMessage(
					ex,
					TransferAppActivator.PRIVILEGE_CONFIGURATION,
					TransferPrivilegeHelper.requiredPrivilegeMessage(TransferAppActivator.PRIVILEGE_CONFIGURATION)));
			model.addAttribute("sendingLocations", Collections.emptyList());
			model.addAttribute("selectedLocationId", null);
			model.addAttribute("receivingFacilities", Collections.emptyList());
			model.addAttribute("selectedReceivingFacilityId", null);
			model.addAttribute("receivingServices", Collections.emptyList());
		}
	}

}
