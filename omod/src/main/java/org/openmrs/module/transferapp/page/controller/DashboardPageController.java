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
import org.openmrs.module.transferapp.TransferAppActivator;
import org.openmrs.module.transferapp.TransferPrivilegeHelper;
import org.openmrs.module.transferapp.api.TransferDashboardService;
import org.openmrs.module.transferapp.api.TransferReceivedStatistics;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.page.PageModel;

/**
 * Controller for the transfer dashboard page.
 */
public class DashboardPageController {

	public void get(UiSessionContext sessionContext, PageModel model,
			@SpringBean("transferDashboardService") TransferDashboardService transferDashboardService) {
		sessionContext.requireAuthentication();

		boolean canAccessDashboard = TransferPrivilegeHelper.hasPrivilege(TransferAppActivator.PRIVILEGE_DASHBOARD);
		model.addAttribute("canAccessDashboard", canAccessDashboard);
		model.addAttribute("requiredDashboardPrivilege", TransferAppActivator.PRIVILEGE_DASHBOARD);
		model.addAttribute("dashboardAccessDeniedMessage",
				TransferPrivilegeHelper.requiredPrivilegeMessage(TransferAppActivator.PRIVILEGE_DASHBOARD));

		if (!canAccessDashboard) {
			setZeroStats(model);
			return;
		}

		try {
			TransferReceivedStatistics received = transferDashboardService.getReceivedTransferStatistics();
			model.addAttribute("transfersReceivedToday", received.getToday());
			model.addAttribute("transfersReceivedThisWeek", received.getThisWeek());
			model.addAttribute("transfersReceivedTotal", received.getTotal());

			TransferReceivedStatistics sent = transferDashboardService.getSentTransferStatistics();
			model.addAttribute("transfersSentToday", sent.getToday());
			model.addAttribute("transfersSentThisWeek", sent.getThisWeek());
			model.addAttribute("transfersSentTotal", sent.getTotal());
			model.addAttribute("transfersSentPending", sent.getPending());

			TransferReceivedStatistics vouchers = transferDashboardService.getAmbulanceVoucherStatistics();
			model.addAttribute("ambulanceVouchersToday", vouchers.getToday());
			model.addAttribute("ambulanceVouchersThisWeek", vouchers.getThisWeek());
			model.addAttribute("ambulanceVouchersTotal", vouchers.getTotal());
		}
		catch (Exception ex) {
			setZeroStats(model);
			model.addAttribute("canAccessDashboard", false);
			model.addAttribute("dashboardAccessDeniedMessage", TransferPrivilegeHelper.resolveUserFacingMessage(
					ex,
					TransferAppActivator.PRIVILEGE_DASHBOARD,
					TransferPrivilegeHelper.requiredPrivilegeMessage(TransferAppActivator.PRIVILEGE_DASHBOARD)));
		}
	}

	private void setZeroStats(PageModel model) {
		model.addAttribute("transfersReceivedToday", 0);
		model.addAttribute("transfersReceivedThisWeek", 0);
		model.addAttribute("transfersReceivedTotal", 0);
		model.addAttribute("transfersSentToday", 0);
		model.addAttribute("transfersSentThisWeek", 0);
		model.addAttribute("transfersSentTotal", 0);
		model.addAttribute("transfersSentPending", 0);
		model.addAttribute("ambulanceVouchersToday", 0);
		model.addAttribute("ambulanceVouchersThisWeek", 0);
		model.addAttribute("ambulanceVouchersTotal", 0);
	}

}
