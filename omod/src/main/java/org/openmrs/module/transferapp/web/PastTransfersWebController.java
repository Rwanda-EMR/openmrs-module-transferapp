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
package org.openmrs.module.transferapp.web;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.openmrs.api.context.Context;
import org.openmrs.module.transferapp.TransferAppActivator;
import org.openmrs.module.transferapp.TransferPrivilegeHelper;
import org.openmrs.module.transferapp.api.PastTransfersService;
import org.openmrs.module.transferapp.model.PastTransferItem;
import org.openmrs.module.transferapp.model.PastTransferPageResult;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletResponse;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Controller
public class PastTransfersWebController {

	private static final String DISPLAY_DATETIME = "dd.MMM.yyyy, HH:mm:ss";

	@RequestMapping(value = "/module/transferapp/transfer/pastTransfersList.form", method = RequestMethod.GET)
	public void listPastTransfers(HttpServletResponse response,
			@RequestParam("month") String month,
			@RequestParam(value = "offset", required = false) Integer offset,
			@RequestParam(value = "limit", required = false) Integer limit) throws Exception {

		Map<String, Object> data = new LinkedHashMap<String, Object>();
		if (!TransferPrivilegeHelper.hasPrivilege(TransferAppActivator.PRIVILEGE_PAST_TRANSFERS)) {
			data.put("status", "error");
			data.put("message", TransferPrivilegeHelper.requiredPrivilegeMessage(
					TransferAppActivator.PRIVILEGE_PAST_TRANSFERS));
			data.put("requiredPrivilege", TransferAppActivator.PRIVILEGE_PAST_TRANSFERS);
			response.setStatus(HttpServletResponse.SC_FORBIDDEN);
			writeJson(response, data);
			return;
		}

		String normalizedMonth = StringUtils.trimToNull(month);
		if (normalizedMonth == null || !normalizedMonth.matches("\\d{4}-\\d{2}")) {
			data.put("status", "error");
			data.put("message", "Month is required. Use yyyy-MM.");
			writeJson(response, data);
			return;
		}

		try {
			PastTransfersService service = Context.getService(PastTransfersService.class);
			int pageOffset = offset != null && offset.intValue() > 0 ? offset.intValue() : 0;
			int pageLimit = limit != null && limit.intValue() > 0
					? limit.intValue()
					: PastTransfersService.DEFAULT_PAGE_SIZE;
			PastTransferPageResult page = service.findVisitsForMonth(normalizedMonth, pageOffset, pageLimit);

			data.put("status", "success");
			data.put("month", normalizedMonth);
			data.put("offset", page.getOffset());
			data.put("limit", page.getLimit());
			data.put("totalCount", page.getTotalCount());
			data.put("loadedCount", page.getLoadedCount());
			data.put("hasMore", page.isHasMore());
			data.put("nextOffset", page.getNextOffset());
			data.put("items", toRows(page.getItems(), page.getOffset()));
		}
		catch (Exception ex) {
			data.put("status", "error");
			data.put("message", TransferPrivilegeHelper.resolveUserFacingMessage(
					ex,
					TransferAppActivator.PRIVILEGE_PAST_TRANSFERS,
					"Unable to load past transfers"));
		}
		writeJson(response, data);
	}

	private List<Map<String, Object>> toRows(List<PastTransferItem> items, int offset) {
		List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
		if (items == null) {
			return rows;
		}
		SimpleDateFormat dayFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
		int index = offset;
		for (PastTransferItem item : items) {
			index += 1;
			Map<String, Object> row = new HashMap<String, Object>();
			row.put("rowNumber", Integer.valueOf(index));
			row.put("visitId", item.getVisitId());
			row.put("patientId", item.getPatientId());
			row.put("upid", StringUtils.defaultString(item.getUpid()));
			row.put("patientName", StringUtils.defaultString(item.getPatientName()));
			row.put("visitDateIso", formatDay(item.getVisitStartDatetime(), dayFormat));
			row.put("visitEndDateIso", formatDay(item.getVisitStopDatetime(), dayFormat));
			row.put("visitDateDisplay", formatDisplay(item.getVisitStartDatetime()));
			row.put("visitEndDateDisplay", item.getVisitStopDatetime() != null
					? formatDisplay(item.getVisitStopDatetime())
					: "");
			row.put("visitOpen", item.getVisitStopDatetime() == null);
			row.put("transferRecords", StringUtils.defaultString(item.getTransferRecords()));
			row.put("primaryTransferId", StringUtils.defaultString(item.getPrimaryTransferId()));
			row.put("localTransferUuid", StringUtils.defaultString(item.getLocalTransferUuid()));
			rows.add(row);
		}
		return rows;
	}

	private String formatDay(Date date, SimpleDateFormat dayFormat) {
		return date != null ? dayFormat.format(date) : "";
	}

	private String formatDisplay(Date date) {
		if (date == null) {
			return "";
		}
		return new SimpleDateFormat(DISPLAY_DATETIME, Locale.ENGLISH).format(date);
	}

	private void writeJson(HttpServletResponse response, Map<String, Object> data) throws Exception {
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		new ObjectMapper().writeValue(response.getWriter(), data);
	}
}
