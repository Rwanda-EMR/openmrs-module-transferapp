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

import org.openmrs.User;
import org.openmrs.api.context.Context;
import org.openmrs.module.appui.UiSessionContext;
import org.openmrs.module.transferapp.TransferAppActivator;
import org.openmrs.module.transferapp.TransferPrivilegeHelper;
import org.openmrs.module.transferapp.api.ApproverService;
import org.openmrs.module.transferapp.model.Approver;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.page.PageModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.LinkedHashMap;

public class ApproversPageController {

	public void get(UiSessionContext sessionContext, PageModel model,
			@SpringBean("approverService") ApproverService approverService) {

		sessionContext.requireAuthentication();

		boolean canAccess = TransferPrivilegeHelper.hasPrivilege(TransferAppActivator.PRIVILEGE_CONFIGURATION);
		model.addAttribute("canAccessApprovers", canAccess);
		model.addAttribute("requiredApproversPrivilege", TransferAppActivator.PRIVILEGE_CONFIGURATION);
		model.addAttribute("approversAccessDeniedMessage",
				TransferPrivilegeHelper.requiredPrivilegeMessage(TransferAppActivator.PRIVILEGE_CONFIGURATION));

		if (!canAccess) {
			model.addAttribute("approverRows", Collections.emptyList());
			model.addAttribute("clinicianOptions", Collections.emptyList());
			return;
		}

		try {
			List<Approver> approvers = approverService.getApprovers();
			List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
			Set<Integer> assignedUserIds = new HashSet<Integer>();
			for (Approver approver : approvers) {
				if (approver == null || approver.getUserId() == null) {
					continue;
				}
				assignedUserIds.add(approver.getUserId());
				User user = Context.getUserService().getUser(approver.getUserId());
				Map<String, Object> row = new LinkedHashMap<String, Object>();
				row.put("approverId", approver.getApproverId());
				row.put("userId", approver.getUserId());
				row.put("position", approver.getPosition());
				row.put("displayName", displayName(user));
				row.put("username", user != null ? user.getUsername() : "");
				rows.add(row);
			}
			model.addAttribute("approverRows", rows);

			List<Map<String, Object>> clinicianOptions = new ArrayList<Map<String, Object>>();
			for (User user : approverService.getClinicianUsers()) {
				if (user == null || user.getUserId() == null || assignedUserIds.contains(user.getUserId())) {
					continue;
				}
				Map<String, Object> option = new LinkedHashMap<String, Object>();
				option.put("userId", user.getUserId());
				option.put("label", optionLabel(user));
				clinicianOptions.add(option);
			}
			model.addAttribute("clinicianOptions", clinicianOptions);
		}
		catch (Exception ex) {
			model.addAttribute("canAccessApprovers", false);
			model.addAttribute("approversAccessDeniedMessage", TransferPrivilegeHelper.resolveUserFacingMessage(
					ex,
					TransferAppActivator.PRIVILEGE_CONFIGURATION,
					TransferPrivilegeHelper.requiredPrivilegeMessage(TransferAppActivator.PRIVILEGE_CONFIGURATION)));
			model.addAttribute("approverRows", Collections.emptyList());
			model.addAttribute("clinicianOptions", Collections.emptyList());
		}
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

	private static String optionLabel(User user) {
		String name = displayName(user);
		String username = user != null ? user.getUsername() : null;
		if (username != null && username.trim().length() > 0 && !username.equals(name)) {
			return name + " (" + username + ")";
		}
		return name;
	}
}
