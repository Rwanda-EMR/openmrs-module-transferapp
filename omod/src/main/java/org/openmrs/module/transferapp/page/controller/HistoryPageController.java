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
import org.openmrs.module.transferapp.api.TransferHistoryService;
import org.openmrs.module.transferapp.model.TransferHistoryItem;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.page.PageModel;
import org.springframework.web.bind.annotation.RequestParam;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HistoryPageController {

	public void get(UiSessionContext sessionContext,
			PageModel model,
			@SpringBean("transferHistoryService") TransferHistoryService transferHistoryService,
			@RequestParam(value = "upid", required = false) String upid,
			@RequestParam(value = "month", required = false) String month,
			@RequestParam(value = "formType", required = false) String formType,
			@RequestParam(value = "app", required = false) String app) {

		sessionContext.requireAuthentication();

		boolean canListTransfers = TransferPrivilegeHelper.hasPrivilege(TransferAppActivator.PRIVILEGE_LIST_TRANSFERS);
		boolean canCreateTransfer = TransferPrivilegeHelper.hasPrivilege(TransferAppActivator.PRIVILEGE_CREATE_TRANSFER);
		String listAccessDeniedMessage = null;
		List<TransferHistoryItem> historyItems = Collections.emptyList();
		String filterUpid = StringUtils.trimToNull(upid);
		// Initial load (month param omitted) defaults to the first month option = current month.
		// Explicit empty month="" keeps the "(Default)" / today-only mode.
		String filterMonth = normalizeMonth(month);
		if (month == null) {
			filterMonth = currentYearMonth();
		}
		String filterFormType = normalizeFormType(formType);

		if (canListTransfers) {
			try {
				historyItems = transferHistoryService.findHistory(filterUpid, filterMonth, filterFormType);
			}
			catch (Exception ex) {
				canListTransfers = false;
				listAccessDeniedMessage = TransferPrivilegeHelper.resolveUserFacingMessage(
						ex,
						TransferAppActivator.PRIVILEGE_LIST_TRANSFERS,
						TransferPrivilegeHelper.requiredPrivilegeMessage(TransferAppActivator.PRIVILEGE_LIST_TRANSFERS));
				historyItems = Collections.emptyList();
			}
		}
		else {
			listAccessDeniedMessage = TransferPrivilegeHelper.requiredPrivilegeMessage(
					TransferAppActivator.PRIVILEGE_LIST_TRANSFERS);
		}

		model.addAttribute("canListTransfers", canListTransfers);
		model.addAttribute("canCreateTransfer", canCreateTransfer);
		model.addAttribute("listAccessDeniedMessage", listAccessDeniedMessage);
		model.addAttribute("historyItems", historyItems);
		model.addAttribute("hasHistory", historyItems != null && !historyItems.isEmpty());
		model.addAttribute("filterUpid", filterUpid != null ? filterUpid : "");
		model.addAttribute("filterMonth", filterMonth != null ? filterMonth : "");
		model.addAttribute("filterFormType", filterFormType != null ? filterFormType : "");
		model.addAttribute("monthOptions", buildMonthOptions());
		model.addAttribute("appId", StringUtils.isNotBlank(app) ? app.trim() : "transferapp.dashboard");
		model.addAttribute("defaultModeToday", filterUpid == null && filterMonth == null);
	}

	private String normalizeFormType(String formType) {
		String value = StringUtils.trimToNull(formType);
		if (value == null || "all".equalsIgnoreCase(value)) {
			return null;
		}
		String lower = value.toLowerCase(Locale.ENGLISH);
		if ("external".equals(lower) || "general".equals(lower)
				|| "maternity".equals(lower) || "neonatal".equals(lower)) {
			return "general".equals(lower) ? "external" : lower;
		}
		return null;
	}

	private String normalizeMonth(String month) {
		String value = StringUtils.trimToNull(month);
		if (value == null) {
			return null;
		}
		if (!value.matches("\\d{4}-\\d{2}")) {
			return null;
		}
		return value;
	}

	private String currentYearMonth() {
		return new SimpleDateFormat("yyyy-MM", Locale.ENGLISH).format(Calendar.getInstance().getTime());
	}

	private List<Map<String, String>> buildMonthOptions() {
		List<Map<String, String>> options = new ArrayList<Map<String, String>>();
		Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.DAY_OF_MONTH, 1);
		SimpleDateFormat valueFormat = new SimpleDateFormat("yyyy-MM", Locale.ENGLISH);
		SimpleDateFormat labelFormat = new SimpleDateFormat("MMMM yyyy", Locale.ENGLISH);
		for (int i = 0; i < 24; i++) {
			Map<String, String> option = new LinkedHashMap<String, String>();
			option.put("value", valueFormat.format(calendar.getTime()));
			option.put("label", labelFormat.format(calendar.getTime()));
			options.add(option);
			calendar.add(Calendar.MONTH, -1);
		}
		return options;
	}
}
