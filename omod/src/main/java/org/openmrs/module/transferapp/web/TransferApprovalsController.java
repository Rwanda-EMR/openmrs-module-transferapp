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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.map.ObjectMapper;
import org.openmrs.api.context.Context;
import org.openmrs.module.transferapp.TransferPrivilegeHelper;
import org.openmrs.module.transferapp.api.ApproverService;
import org.openmrs.module.transferapp.api.TransferApprovalService;
import org.openmrs.module.transferapp.model.Transfer;
import org.openmrs.module.transferapp.model.TransferApprovalItem;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class TransferApprovalsController {

	private static final Log log = LogFactory.getLog(TransferApprovalsController.class);

	private TransferApprovalService getTransferApprovalService() {
		return Context.getService(TransferApprovalService.class);
	}

	private ApproverService getApproverService() {
		return Context.getService(ApproverService.class);
	}

	@RequestMapping(value = "/module/transferapp/approvals/list.form", method = RequestMethod.GET)
	public void list(HttpServletResponse response) throws Exception {
		Map<String, Object> data = new HashMap<String, Object>();
		if (!requireApprover(response, data)) {
			return;
		}
		try {
			List<TransferApprovalItem> items = getTransferApprovalService().getPendingApprovals();
			data.put("status", "success");
			data.put("items", toMaps(items));
			data.put("count", items != null ? items.size() : 0);
		}
		catch (Exception e) {
			log.error("Unable to list pending approvals", e);
			data.put("status", "error");
			data.put("message", TransferPrivilegeHelper.resolveUserFacingMessage(
					e, null, "Unable to load pending approvals"));
			data.put("items", new ArrayList<Map<String, Object>>());
		}
		writeJson(response, data);
	}

	@RequestMapping(value = "/module/transferapp/approvals/approve.form", method = RequestMethod.POST)
	public void approve(HttpServletResponse response, @RequestParam("uuid") String uuid) throws Exception {
		Map<String, Object> data = new HashMap<String, Object>();
		if (!requireApprover(response, data)) {
			return;
		}
		try {
			Transfer transfer = getTransferApprovalService().approveTransfer(uuid);
			data.put("status", "success");
			data.put("uuid", transfer.getUuid());
			data.put("hieSent", transfer.isSentToHie());
			data.put("message", "Transfer approved and sent to HIE");
		}
		catch (Exception e) {
			log.error("Unable to approve transfer " + uuid, e);
			data.put("status", "error");
			data.put("message", TransferPrivilegeHelper.resolveUserFacingMessage(
					e, null, "Unable to approve transfer"));
		}
		writeJson(response, data);
	}

	@RequestMapping(value = "/module/transferapp/approvals/reject.form", method = RequestMethod.POST)
	public void reject(HttpServletResponse response,
			@RequestParam("uuid") String uuid,
			@RequestParam("reason") String reason) throws Exception {
		Map<String, Object> data = new HashMap<String, Object>();
		if (!requireApprover(response, data)) {
			return;
		}
		try {
			Transfer transfer = getTransferApprovalService().rejectTransfer(uuid, reason);
			data.put("status", "success");
			data.put("uuid", transfer.getUuid());
			data.put("message", "Transfer rejected");
		}
		catch (Exception e) {
			log.error("Unable to reject transfer " + uuid, e);
			data.put("status", "error");
			data.put("message", TransferPrivilegeHelper.resolveUserFacingMessage(
					e, null, "Unable to reject transfer"));
		}
		writeJson(response, data);
	}

	private boolean requireApprover(HttpServletResponse response, Map<String, Object> data) throws Exception {
		ApproverService approverService = getApproverService();
		if (approverService != null && approverService.isCurrentUserApprover()) {
			return true;
		}
		data.put("status", "error");
		data.put("message", "Only configured transfer approvers can manage approvals");
		response.setStatus(HttpServletResponse.SC_FORBIDDEN);
		writeJson(response, data);
		return false;
	}

	private List<Map<String, Object>> toMaps(List<TransferApprovalItem> items) {
		List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
		if (items == null) {
			return rows;
		}
		for (TransferApprovalItem item : items) {
			if (item == null) {
				continue;
			}
			Map<String, Object> row = new HashMap<String, Object>();
			row.put("transferId", item.getTransferId());
			row.put("uuid", item.getUuid());
			row.put("patientId", item.getPatientId());
			row.put("patientName", item.getPatientName());
			row.put("upid", item.getUpid());
			row.put("receivingFacilityCode", item.getReceivingFacilityCode());
			row.put("receivingFacilityName", item.getReceivingFacilityName());
			row.put("receivingService", item.getReceivingService());
			row.put("healthInsuranceType", item.getHealthInsuranceType());
			row.put("decisionToTransferAt", item.getDecisionToTransferAt());
			row.put("dateCreated", item.getDateCreated());
			row.put("localApprovalStatus", item.getLocalApprovalStatus());
			rows.add(row);
		}
		return rows;
	}

	private void writeJson(HttpServletResponse response, Map<String, Object> data) throws Exception {
		response.setContentType("application/json");
		new ObjectMapper().writeValue(response.getOutputStream(), data);
	}
}
