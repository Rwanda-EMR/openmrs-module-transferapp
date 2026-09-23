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
package org.openmrs.module.transferapp.api.impl;

import org.apache.commons.lang.StringUtils;
import org.openmrs.Role;
import org.openmrs.User;
import org.openmrs.api.APIException;
import org.openmrs.api.UserService;
import org.openmrs.api.context.Context;
import org.openmrs.module.transferapp.api.ApproverService;
import org.openmrs.module.transferapp.api.TransferApprovalService;
import org.openmrs.module.transferapp.api.dao.ApproverDao;
import org.openmrs.module.transferapp.model.Approver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class ApproverServiceImpl implements ApproverService {

	private ApproverDao approverDao;

	public void setApproverDao(ApproverDao approverDao) {
		this.approverDao = approverDao;
	}

	@Override
	public List<Approver> getApprovers() {
		if (approverDao == null) {
			return Collections.emptyList();
		}
		List<Approver> approvers = approverDao.getApprovers(false);
		return approvers != null ? approvers : Collections.<Approver>emptyList();
	}

	@Override
	public Approver getApprover(Integer approverId) {
		return approverDao != null ? approverDao.getApprover(approverId) : null;
	}

	@Override
	public List<User> getClinicianUsers() {
		UserService userService = Context.getUserService();
		Role clinicianRole = resolveClinicianRole(userService);
		if (clinicianRole == null) {
			return Collections.emptyList();
		}
		List<User> users = userService.getUsersByRole(clinicianRole);
		if (users == null || users.isEmpty()) {
			return Collections.emptyList();
		}
		List<User> active = new ArrayList<User>();
		for (User user : users) {
			if (user == null || Boolean.TRUE.equals(user.getRetired()) || user.getUserId() == null) {
				continue;
			}
			active.add(user);
		}
		Collections.sort(active, new Comparator<User>() {
			@Override
			public int compare(User left, User right) {
				return displayName(left).compareToIgnoreCase(displayName(right));
			}
		});
		return active;
	}

	@Override
	public Approver saveApprover(Integer userId, String position) {
		if (approverDao == null) {
			throw new APIException("Approver data access is not configured");
		}
		if (userId == null) {
			throw new APIException("User is required");
		}
		String normalizedPosition = StringUtils.trimToNull(position);
		if (normalizedPosition == null) {
			throw new APIException("Position is required");
		}

		User user = Context.getUserService().getUser(userId);
		if (user == null || Boolean.TRUE.equals(user.getRetired())) {
			throw new APIException("Selected user was not found or is retired");
		}
		if (!userHasClinicianRole(user)) {
			throw new APIException("Selected user must have the Clinician role");
		}

		Approver existing = approverDao.getApproverByUserId(userId);
		Approver approver = existing != null ? existing : new Approver();
		if (approver.getUuid() == null) {
			approver.setUuid(UUID.randomUUID().toString());
		}
		approver.setUserId(userId);
		approver.setPosition(normalizedPosition);
		approver.setVoided(false);
		approver.setVoidedBy(null);
		approver.setDateVoided(null);
		approver.setVoidReason(null);

		User current = Context.getAuthenticatedUser();
		Date now = new Date();
		if (approver.getCreator() == null) {
			approver.setCreator(current);
			approver.setDateCreated(now);
		} else {
			approver.setChangedBy(current);
			approver.setDateChanged(now);
		}
		return approverDao.saveApprover(approver);
	}

	@Override
	public Approver voidApprover(Integer approverId, String reason) {
		if (approverDao == null) {
			throw new APIException("Approver data access is not configured");
		}
		Approver approver = approverDao.getApprover(approverId);
		if (approver == null) {
			throw new APIException("Approver was not found");
		}
		if (Boolean.TRUE.equals(approver.getVoided())) {
			return approver;
		}
		approver.setVoided(true);
		approver.setVoidedBy(Context.getAuthenticatedUser());
		approver.setDateVoided(new Date());
		approver.setVoidReason(StringUtils.defaultIfEmpty(StringUtils.trimToNull(reason), "Removed from approvers"));
		return approverDao.saveApprover(approver);
	}

	@Override
	public boolean isCurrentUserApprover() {
		User user = Context.getAuthenticatedUser();
		if (user == null || user.getUserId() == null || approverDao == null) {
			return false;
		}
		Approver approver = approverDao.getApproverByUserId(user.getUserId());
		return approver != null && !Boolean.TRUE.equals(approver.getVoided());
	}

	@Override
	public Approver getActiveApproverByUserId(Integer userId) {
		if (userId == null || approverDao == null) {
			return null;
		}
		Approver approver = approverDao.getApproverByUserId(userId);
		if (approver == null || Boolean.TRUE.equals(approver.getVoided())) {
			return null;
		}
		return approver;
	}

	@Override
	public int getPendingApprovalCountForCurrentUser() {
		if (!isCurrentUserApprover()) {
			return 0;
		}
		try {
			TransferApprovalService approvalService = Context.getService(TransferApprovalService.class);
			return approvalService != null ? approvalService.countPendingApprovals() : 0;
		}
		catch (Exception ex) {
			return 0;
		}
	}

	private Role resolveClinicianRole(UserService userService) {
		Role role = userService.getRole(CLINICIAN_ROLE_NAME);
		if (role != null) {
			return role;
		}
		List<Role> roles = userService.getAllRoles();
		if (roles == null) {
			return null;
		}
		for (Role candidate : roles) {
			if (candidate != null && CLINICIAN_ROLE_NAME.equalsIgnoreCase(candidate.getRole())) {
				return candidate;
			}
		}
		return null;
	}

	private boolean userHasClinicianRole(User user) {
		if (user == null) {
			return false;
		}
		for (Role role : user.getAllRoles()) {
			if (role != null && CLINICIAN_ROLE_NAME.equalsIgnoreCase(role.getRole())) {
				return true;
			}
		}
		return false;
	}

	private static String displayName(User user) {
		if (user == null) {
			return "";
		}
		if (user.getPersonName() != null) {
			String full = StringUtils.trimToEmpty(user.getPersonName().getFullName());
			if (StringUtils.isNotBlank(full)) {
				return full;
			}
		}
		return StringUtils.defaultString(user.getUsername());
	}
}
