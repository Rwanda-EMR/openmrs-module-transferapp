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
import org.openmrs.module.transferapp.api.ApproverService;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.fragment.FragmentModel;

/**
 * Home-page banner for users configured as transfer approvers.
 */
public class ApproverPendingBannerFragmentController {

	private static final Log log = LogFactory.getLog(ApproverPendingBannerFragmentController.class);

	public void controller(FragmentModel model,
			@SpringBean("approverService") ApproverService approverService) {

		boolean showBanner = false;
		int pendingCount = 0;
		try {
			showBanner = approverService != null && approverService.isCurrentUserApprover();
			if (showBanner) {
				pendingCount = approverService.getPendingApprovalCountForCurrentUser();
			}
		}
		catch (Exception ex) {
			log.warn("Unable to resolve approver pending banner state", ex);
			showBanner = false;
			pendingCount = 0;
		}
		model.addAttribute("showBanner", showBanner);
		model.addAttribute("pendingApprovalCount", pendingCount);
	}
}
