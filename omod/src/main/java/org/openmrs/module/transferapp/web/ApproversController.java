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
import org.openmrs.User;
import org.openmrs.api.context.Context;
import org.openmrs.module.transferapp.TransferAppActivator;
import org.openmrs.module.transferapp.TransferPrivilegeHelper;
import org.openmrs.module.transferapp.api.ApproverService;
import org.openmrs.module.transferapp.model.Approver;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

@Controller
public class ApproversController {

	private static final Log log = LogFactory.getLog(ApproversController.class);

	private ApproverService getApproverService() {
		return Context.getService(ApproverService.class);
	}

	@RequestMapping(value = "/module/transferapp/approvers/save.form", method = RequestMethod.POST)
	public void saveApprover(HttpServletResponse response,
			@RequestParam("userId") Integer userId,
			@RequestParam("position") String position) throws Exception {

		Map<String, Object> data = new HashMap<String, Object>();
		if (!requireConfigurationPrivilege(response, data)) {
			return;
		}
		try {
			Approver approver = getApproverService().saveApprover(userId, position);
			User user = Context.getUserService().getUser(approver.getUserId());
			data.put("status", "success");
			data.put("approverId", approver.getApproverId());
			data.put("userId", approver.getUserId());
			data.put("position", approver.getPosition());
			data.put("displayName", displayName(user));
			data.put("username", user != null ? user.getUsername() : "");
		}
		catch (Exception e) {
			log.error("Unable to save approver", e);
			data.put("status", "error");
			data.put("message", TransferPrivilegeHelper.resolveUserFacingMessage(
					e, TransferAppActivator.PRIVILEGE_CONFIGURATION, "Unable to save approver"));
		}
		writeJson(response, data);
	}

	@RequestMapping(value = "/module/transferapp/approvers/void.form", method = RequestMethod.POST)
	public void voidApprover(HttpServletResponse response,
			@RequestParam("approverId") Integer approverId) throws Exception {

		Map<String, Object> data = new HashMap<String, Object>();
		if (!requireConfigurationPrivilege(response, data)) {
			return;
		}
		try {
			getApproverService().voidApprover(approverId, null);
			data.put("status", "success");
		}
		catch (Exception e) {
			log.error("Unable to remove approver", e);
			data.put("status", "error");
			data.put("message", TransferPrivilegeHelper.resolveUserFacingMessage(
					e, TransferAppActivator.PRIVILEGE_CONFIGURATION, "Unable to remove approver"));
		}
		writeJson(response, data);
	}

	private boolean requireConfigurationPrivilege(HttpServletResponse response, Map<String, Object> data)
			throws Exception {
		if (TransferPrivilegeHelper.hasPrivilege(TransferAppActivator.PRIVILEGE_CONFIGURATION)) {
			return true;
		}
		data.put("status", "error");
		data.put("message",
				TransferPrivilegeHelper.requiredPrivilegeMessage(TransferAppActivator.PRIVILEGE_CONFIGURATION));
		data.put("requiredPrivilege", TransferAppActivator.PRIVILEGE_CONFIGURATION);
		response.setStatus(HttpServletResponse.SC_FORBIDDEN);
		writeJson(response, data);
		return false;
	}

	private static String displayName(User user) {
		if (user == null) {
			return "";
		}
		if (user.getPersonName() != null) {
			String full = user.getPersonName().getFullName();
			if (full != null && full.trim().length() > 0) {
				return full.trim();
			}
		}
		return user.getUsername() != null ? user.getUsername() : "";
	}

	private void writeJson(HttpServletResponse response, Map<String, Object> data) throws Exception {
		response.setContentType("application/json");
		new ObjectMapper().writeValue(response.getOutputStream(), data);
	}
}
