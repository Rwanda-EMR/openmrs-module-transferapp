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
import org.openmrs.Patient;
import org.openmrs.api.PatientService;
import org.openmrs.module.appui.UiSessionContext;
import org.openmrs.module.transferapp.TransferAppActivator;
import org.openmrs.module.transferapp.TransferPrivilegeHelper;
import org.openmrs.module.transferapp.api.FacilityTransferRecordsService;
import org.openmrs.module.transferapp.api.TransferAdminService;
import org.openmrs.module.transferapp.model.FacilityTransferRecordItem;
import org.openmrs.module.transferapp.model.ReceivingFacility;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.page.PageModel;
import org.springframework.web.bind.annotation.RequestParam;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class RecordsPageController {

	private static final int MAX_DATE_RANGE_MONTHS = 3;

	private static final String DATE_PARAM_PATTERN = "yyyy-MM-dd";

	public void get(UiSessionContext sessionContext,
			PageModel model,
			@SpringBean("facilityTransferRecordsService") FacilityTransferRecordsService facilityTransferRecordsService,
			@SpringBean("transferAdminService") TransferAdminService transferAdminService,
			@SpringBean("patientService") PatientService patientService,
			@RequestParam(value = "patientId", required = false) Integer patientId,
			@RequestParam(value = "startDate", required = false) String startDateParam,
			@RequestParam(value = "endDate", required = false) String endDateParam,
			@RequestParam(value = "receivingFacilityCode", required = false) String receivingFacilityCode,
			@RequestParam(value = "formType", required = false) String formType,
			@RequestParam(value = "app", required = false) String app) {

		sessionContext.requireAuthentication();

		boolean canListTransfers = TransferPrivilegeHelper.hasPrivilege(TransferAppActivator.PRIVILEGE_LIST_TRANSFERS);
		boolean canCreateTransfer = TransferPrivilegeHelper.hasPrivilege(TransferAppActivator.PRIVILEGE_CREATE_TRANSFER);

		List<FacilityTransferRecordItem> records = Collections.emptyList();
		List<ReceivingFacility> receivingFacilities = Collections.emptyList();
		String filteredPatientName = null;
		String listAccessDeniedMessage = null;

		Date defaultEndDate = endOfDay(new Date());
		Date defaultStartDate = startOfDay(addMonths(defaultEndDate, -MAX_DATE_RANGE_MONTHS));
		Date startDate = parseDateParam(startDateParam, defaultStartDate);
		Date endDate = parseDateParam(endDateParam, defaultEndDate);
		Date[] normalizedRange = normalizeDateRange(startDate, endDate, MAX_DATE_RANGE_MONTHS);
		startDate = normalizedRange[0];
		endDate = normalizedRange[1];

		String selectedReceivingFacilityCode = StringUtils.trimToNull(receivingFacilityCode);
		String selectedFormType = StringUtils.trimToNull(formType);

		if (canListTransfers) {
			try {
				Integer sendingLocationId = transferAdminService.resolveCurrentSendingLocationId();
				if (sendingLocationId != null) {
					List<ReceivingFacility> facilities = transferAdminService.getReceivingFacilities(sendingLocationId);
					receivingFacilities = facilities != null ? facilities : Collections.<ReceivingFacility>emptyList();
				}

				records = facilityTransferRecordsService.getOutboundTransferRecords(
						patientId, startDate, endDate, selectedReceivingFacilityCode, selectedFormType);
				if (patientId != null) {
					Patient patient = patientService.getPatient(patientId);
					if (patient != null && patient.getPersonName() != null) {
						filteredPatientName = patient.getPersonName().getFullName();
					}
				}
			}
			catch (Exception ex) {
				canListTransfers = false;
				listAccessDeniedMessage = TransferPrivilegeHelper.resolveUserFacingMessage(
						ex,
						TransferAppActivator.PRIVILEGE_LIST_TRANSFERS,
						TransferPrivilegeHelper.requiredPrivilegeMessage(TransferAppActivator.PRIVILEGE_LIST_TRANSFERS));
				records = Collections.emptyList();
			}
		} else {
			listAccessDeniedMessage = TransferPrivilegeHelper.requiredPrivilegeMessage(
					TransferAppActivator.PRIVILEGE_LIST_TRANSFERS);
		}

		model.addAttribute("records", records);
		model.addAttribute("hasRecords", records != null && !records.isEmpty());
		model.addAttribute("canListTransfers", canListTransfers);
		model.addAttribute("canCreateTransfer", canCreateTransfer);
		model.addAttribute("requiredListPrivilege", TransferAppActivator.PRIVILEGE_LIST_TRANSFERS);
		model.addAttribute("listAccessDeniedMessage", listAccessDeniedMessage);
		model.addAttribute("filteredPatientId", patientId);
		model.addAttribute("filteredPatientName", filteredPatientName);
		model.addAttribute("appId", app != null ? app : "transferapp.dashboard");
		model.addAttribute("receivingFacilities", receivingFacilities);
		model.addAttribute("filterStartDate", formatDateParam(startDate));
		model.addAttribute("filterEndDate", formatDateParam(endDate));
		model.addAttribute("filterReceivingFacilityCode", selectedReceivingFacilityCode != null
				? selectedReceivingFacilityCode : "");
		model.addAttribute("filterFormType", selectedFormType != null ? selectedFormType : "");
		model.addAttribute("maxDateRangeMonths", MAX_DATE_RANGE_MONTHS);
	}

	static Date[] normalizeDateRange(Date startDate, Date endDate, int maxMonths) {
		Date normalizedStart = startOfDay(startDate != null ? startDate : new Date());
		Date normalizedEnd = endOfDay(endDate != null ? endDate : new Date());
		if (normalizedStart.after(normalizedEnd)) {
			Date swap = normalizedStart;
			normalizedStart = startOfDay(normalizedEnd);
			normalizedEnd = endOfDay(swap);
		}
		Date earliestAllowedStart = startOfDay(addMonths(normalizedEnd, -maxMonths));
		if (normalizedStart.before(earliestAllowedStart)) {
			normalizedStart = earliestAllowedStart;
		}
		return new Date[] { normalizedStart, normalizedEnd };
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

	private static Date addMonths(Date date, int months) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		calendar.add(Calendar.MONTH, months);
		return calendar.getTime();
	}

}
