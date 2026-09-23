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
package org.openmrs.module.transferapp.api;

import org.openmrs.User;
import org.openmrs.annotation.Authorized;
import org.openmrs.module.transferapp.TransferAppActivator;
import org.openmrs.module.transferapp.model.Approver;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Transactional
public interface ApproverService {

	String CLINICIAN_ROLE_NAME = "Clinician";

	@Authorized(TransferAppActivator.PRIVILEGE_CONFIGURATION)
	@Transactional(readOnly = true)
	List<Approver> getApprovers();

	@Authorized(TransferAppActivator.PRIVILEGE_CONFIGURATION)
	@Transactional(readOnly = true)
	Approver getApprover(Integer approverId);

	@Authorized(TransferAppActivator.PRIVILEGE_CONFIGURATION)
	@Transactional(readOnly = true)
	List<User> getClinicianUsers();

	@Authorized(TransferAppActivator.PRIVILEGE_CONFIGURATION)
	Approver saveApprover(Integer userId, String position);

	@Authorized(TransferAppActivator.PRIVILEGE_CONFIGURATION)
	Approver voidApprover(Integer approverId, String reason);

	/**
	 * True when the authenticated user has a non-voided row in {@code approvers}.
	 * Readable by any authenticated user (used by the home-page banner).
	 */
	@Transactional(readOnly = true)
	boolean isCurrentUserApprover();

	/**
	 * Active (non-voided) approver row for the given user, or null.
	 */
	@Transactional(readOnly = true)
	Approver getActiveApproverByUserId(Integer userId);

	/**
	 * Pending outbound external-transfer approvals for the current facility when the user is an approver.
	 */
	@Transactional(readOnly = true)
	int getPendingApprovalCountForCurrentUser();
}
