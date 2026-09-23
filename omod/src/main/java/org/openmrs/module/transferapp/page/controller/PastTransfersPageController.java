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

import org.apache.commons.lang.StringUtils;
import org.openmrs.module.appui.UiSessionContext;
import org.openmrs.module.transferapp.TransferAppActivator;
import org.openmrs.module.transferapp.TransferPrivilegeHelper;
import org.openmrs.module.transferapp.api.PastTransfersService;
import org.openmrs.module.transferapp.model.PastTransferPageResult;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.page.PageModel;
import org.springframework.web.bind.annotation.RequestParam;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Locale;

public class PastTransfersPageController {

	public void get(UiSessionContext sessionContext,
			PageModel model,
			@SpringBean("pastTransfersService") PastTransfersService pastTransfersService,
			@RequestParam(value = "startDate", required = false) String startDate,
			@RequestParam(value = "endDate", required = false) String endDate,
			@RequestParam(value = "upid", required = false) String upid,
			@RequestParam(value = "app", required = false) String app) {

		sessionContext.requireAuthentication();

		boolean canAccess = TransferPrivilegeHelper.hasPrivilege(TransferAppActivator.PRIVILEGE_PAST_TRANSFERS);
		model.addAttribute("canAccessPastTransfers", canAccess);
		model.addAttribute("accessDeniedMessage",
				TransferPrivilegeHelper.requiredPrivilegeMessage(TransferAppActivator.PRIVILEGE_PAST_TRANSFERS));

		String today = currentDay();
		String filterStartDate = normalizeDay(startDate);
		String filterEndDate = normalizeDay(endDate);
		if (startDate == null && endDate == null) {
			filterStartDate = today;
			filterEndDate = today;
		} else {
			if (filterStartDate == null) {
				filterStartDate = filterEndDate != null ? filterEndDate : today;
			}
			if (filterEndDate == null) {
				filterEndDate = filterStartDate;
			}
		}
		String filterUpid = StringUtils.trimToEmpty(upid);

		PastTransferPageResult page = new PastTransferPageResult();
		page.setItems(Collections.emptyList());
		page.setOffset(0);
		page.setLimit(PastTransfersService.DEFAULT_PAGE_SIZE);
		page.setTotalCount(0);
		String listError = null;
		if (canAccess && StringUtils.isNotBlank(filterStartDate) && StringUtils.isNotBlank(filterEndDate)) {
			try {
				page = pastTransfersService.findVisitsInRange(
						filterStartDate, filterEndDate, filterUpid, 0, PastTransfersService.DEFAULT_PAGE_SIZE);
			}
			catch (Exception ex) {
				listError = TransferPrivilegeHelper.resolveUserFacingMessage(
						ex,
						TransferAppActivator.PRIVILEGE_PAST_TRANSFERS,
						"Unable to load past transfers");
				page = new PastTransferPageResult();
				page.setItems(Collections.emptyList());
				page.setOffset(0);
				page.setLimit(PastTransfersService.DEFAULT_PAGE_SIZE);
				page.setTotalCount(0);
			}
		}

		model.addAttribute("pastTransferItems", page.getItems());
		model.addAttribute("hasPastTransfers", page.getItems() != null && !page.getItems().isEmpty());
		model.addAttribute("pastTransfersTotalCount", page.getTotalCount());
		model.addAttribute("pastTransfersLoadedCount", page.getLoadedCount());
		model.addAttribute("pastTransfersHasMore", page.isHasMore());
		model.addAttribute("pastTransfersNextOffset", page.getNextOffset());
		model.addAttribute("pastTransfersPageSize", PastTransfersService.DEFAULT_PAGE_SIZE);
		model.addAttribute("filterStartDate", filterStartDate != null ? filterStartDate : "");
		model.addAttribute("filterEndDate", filterEndDate != null ? filterEndDate : "");
		model.addAttribute("filterUpid", filterUpid);
		model.addAttribute("listError", listError);
		model.addAttribute("appId", StringUtils.isNotBlank(app) ? app.trim() : "transferapp.dashboard");
		model.addAttribute("canCreateTransfer",
				TransferPrivilegeHelper.hasPrivilege(TransferAppActivator.PRIVILEGE_CREATE_TRANSFER));
		model.addAttribute("canListTransfers",
				TransferPrivilegeHelper.hasPrivilege(TransferAppActivator.PRIVILEGE_LIST_TRANSFERS)
						|| canAccess);
	}

	private String normalizeDay(String value) {
		String day = StringUtils.trimToNull(value);
		if (day == null || !day.matches("\\d{4}-\\d{2}-\\d{2}")) {
			return null;
		}
		return day;
	}

	private String currentDay() {
		return new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(Calendar.getInstance().getTime());
	}
}
