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
import org.openmrs.module.transferapp.api.MissingCounterReferralItem;
import org.openmrs.module.transferapp.api.TransferDashboardService;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.page.PageModel;
import org.springframework.web.bind.annotation.RequestParam;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Lists outbound transfers that still do not have counter-referral feedback stored locally.
 */
public class MissingOutboundCounterReferralsPageController {

	private static final int MAX_DATE_RANGE_MONTHS = 6;

	private static final String DATE_PARAM_PATTERN = "yyyy-MM-dd";

	public void get(UiSessionContext sessionContext, PageModel model,
			@SpringBean("transferDashboardService") TransferDashboardService transferDashboardService,
			@RequestParam(value = "startDate", required = false) String startDateParam,
			@RequestParam(value = "endDate", required = false) String endDateParam,
			@RequestParam(value = "app", required = false) String app) {
		sessionContext.requireAuthentication();

		boolean canAccessDashboard = TransferPrivilegeHelper.hasPrivilege(TransferAppActivator.PRIVILEGE_DASHBOARD);
		boolean canListTransfers = TransferPrivilegeHelper.hasPrivilege(TransferAppActivator.PRIVILEGE_LIST_TRANSFERS);
		model.addAttribute("canAccessDashboard", canAccessDashboard);
		model.addAttribute("canListTransfers", canListTransfers);
		model.addAttribute("dashboardAccessDeniedMessage",
				TransferPrivilegeHelper.requiredPrivilegeMessage(TransferAppActivator.PRIVILEGE_DASHBOARD));
		model.addAttribute("appId", StringUtils.isNotBlank(app) ? app.trim() : "transferapp.dashboard");
		model.addAttribute("maxDateRangeMonths", MAX_DATE_RANGE_MONTHS);

		Date defaultEndDate = endOfDay(new Date());
		Date defaultStartDate = startOfDay(startOfMonth(defaultEndDate));
		Date startDate = parseDateParam(startDateParam, defaultStartDate);
		Date endDate = parseDateParam(endDateParam, defaultEndDate);
		Date[] normalizedRange = DashboardPageController.normalizeDateRange(startDate, endDate, MAX_DATE_RANGE_MONTHS);
		startDate = normalizedRange[0];
		endDate = normalizedRange[1];
		model.addAttribute("filterStartDate", formatDateParam(startDate));
		model.addAttribute("filterEndDate", formatDateParam(endDate));

		List<MissingCounterReferralItem> items = Collections.emptyList();
		if (!canAccessDashboard) {
			model.addAttribute("missingOutboundCounterReferrals", items);
			model.addAttribute("hasMissingOutboundCounterReferrals", false);
			return;
		}

		try {
			items = transferDashboardService.listMissingOutboundCounterReferrals(startDate, endDate);
			if (items == null) {
				items = Collections.emptyList();
			}
		}
		catch (Exception ex) {
			items = Collections.emptyList();
			model.addAttribute("canAccessDashboard", false);
			model.addAttribute("dashboardAccessDeniedMessage", TransferPrivilegeHelper.resolveUserFacingMessage(
					ex,
					TransferAppActivator.PRIVILEGE_DASHBOARD,
					TransferPrivilegeHelper.requiredPrivilegeMessage(TransferAppActivator.PRIVILEGE_DASHBOARD)));
		}

		model.addAttribute("missingOutboundCounterReferrals", items);
		model.addAttribute("hasMissingOutboundCounterReferrals", !items.isEmpty());
	}

	private static Date parseDateParam(String value, Date defaultValue) {
		if (StringUtils.isBlank(value)) {
			return defaultValue;
		}
		try {
			SimpleDateFormat formatter = new SimpleDateFormat(DATE_PARAM_PATTERN);
			formatter.setLenient(false);
			return formatter.parse(value.trim());
		}
		catch (ParseException ex) {
			return defaultValue;
		}
	}

	private static String formatDateParam(Date date) {
		if (date == null) {
			return "";
		}
		return new SimpleDateFormat(DATE_PARAM_PATTERN).format(date);
	}

	private static Date startOfDay(Date date) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		return calendar.getTime();
	}

	private static Date endOfDay(Date date) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		calendar.set(Calendar.HOUR_OF_DAY, 23);
		calendar.set(Calendar.MINUTE, 59);
		calendar.set(Calendar.SECOND, 59);
		calendar.set(Calendar.MILLISECOND, 999);
		return calendar.getTime();
	}

	private static Date startOfMonth(Date date) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		calendar.set(Calendar.DAY_OF_MONTH, 1);
		return calendar.getTime();
	}

}
