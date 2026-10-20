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
package org.openmrs.module.transferapp.fragment.controller.transfer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.User;
import org.openmrs.api.context.Context;
import org.openmrs.module.transferapp.TransferAppActivator;
import org.openmrs.module.transferapp.api.ApproverService;
import org.openmrs.module.transferapp.api.ProviderLicenseRequirement;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.fragment.FragmentModel;

/**
 * Nav visibility for Past Transfers, Approvals, Destinations, Approvers, and My Profile.
 * Approvals must use the injected ApproverService — Context.getService from the GSP
 * was failing silently, so the link only appeared when {@code activeTab == approvals}.
 */
public class TransferNavFragmentController {

	private static final Log log = LogFactory.getLog(TransferNavFragmentController.class);

	public void controller(FragmentModel model,
			@SpringBean("approverService") ApproverService approverService) {

		User user = Context.getAuthenticatedUser();
		boolean canPastTransfers = user != null
				&& user.hasPrivilege(TransferAppActivator.PRIVILEGE_PAST_TRANSFERS);
		boolean canConfigure = user != null
				&& user.hasPrivilege(TransferAppActivator.PRIVILEGE_CONFIGURATION);

		boolean canApprovals = false;
		try {
			canApprovals = approverService != null && approverService.isCurrentUserApprover();
		}
		catch (Exception ex) {
			log.warn("Unable to resolve Approvals nav visibility", ex);
			canApprovals = false;
		}

		boolean canProfile = false;
		try {
			canProfile = ProviderLicenseRequirement.currentUserHasConfiguredProviderRole(user);
		}
		catch (Exception ex) {
			log.warn("Unable to resolve My Profile nav visibility", ex);
			canProfile = false;
		}

		model.addAttribute("canPastTransfers", canPastTransfers);
		model.addAttribute("canConfigure", canConfigure);
		model.addAttribute("canApprovals", canApprovals);
		model.addAttribute("canProfile", canProfile);
	}
}
