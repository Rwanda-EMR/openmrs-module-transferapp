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

import org.openmrs.module.appui.UiSessionContext;
import org.openmrs.module.transferapp.api.ApproverService;
import org.openmrs.module.transferapp.api.TransferApprovalService;
import org.openmrs.module.transferapp.model.TransferApprovalItem;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.page.PageModel;

import java.util.Collections;
import java.util.List;

public class ApprovalsPageController {

	public void get(UiSessionContext sessionContext, PageModel model,
			@SpringBean("approverService") ApproverService approverService,
			@SpringBean("transferApprovalService") TransferApprovalService transferApprovalService) {

		sessionContext.requireAuthentication();

		boolean canAccess = approverService != null && approverService.isCurrentUserApprover();
		model.addAttribute("canAccessApprovals", canAccess);
		model.addAttribute("approvalsAccessDeniedMessage",
				"Only configured transfer approvers can access the Approvals page.");

		List<TransferApprovalItem> items = Collections.emptyList();
		String listError = null;
		if (canAccess) {
			try {
				items = transferApprovalService.getPendingApprovals();
			}
			catch (Exception ex) {
				listError = ex.getMessage() != null ? ex.getMessage() : "Unable to load pending approvals";
				items = Collections.emptyList();
			}
		}
		model.addAttribute("approvalItems", items != null ? items : Collections.emptyList());
		model.addAttribute("approvalListError", listError);
		model.addAttribute("appId", "transferapp.dashboard");
	}
}
