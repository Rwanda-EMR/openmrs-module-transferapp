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
import org.openmrs.Patient;
import org.openmrs.User;
import org.openmrs.api.APIException;
import org.openmrs.api.context.Context;
import org.openmrs.module.transferapp.api.ApproverService;
import org.openmrs.module.transferapp.api.TransferAdminService;
import org.openmrs.module.transferapp.api.TransferApprovalService;
import org.openmrs.module.transferapp.api.TransferHieSubmissionService;
import org.openmrs.module.transferapp.api.TransferProfileService;
import org.openmrs.module.transferapp.api.dao.TransferDao;
import org.openmrs.module.transferapp.model.Approver;
import org.openmrs.module.transferapp.model.Transfer;
import org.openmrs.module.transferapp.model.TransferApprovalItem;
import org.openmrs.module.transferapp.model.TransferApprovalStatus;
import org.openmrs.module.transferapp.model.TransferProfile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class TransferApprovalServiceImpl implements TransferApprovalService {

	private TransferDao transferDao;

	private ApproverService approverService;

	private TransferHieSubmissionService transferHieSubmissionService;

	private TransferAdminService transferAdminService;

	private TransferProfileService transferProfileService;

	public void setTransferDao(TransferDao transferDao) {
		this.transferDao = transferDao;
	}

	public void setApproverService(ApproverService approverService) {
		this.approverService = approverService;
	}

	public void setTransferHieSubmissionService(TransferHieSubmissionService transferHieSubmissionService) {
		this.transferHieSubmissionService = transferHieSubmissionService;
	}

	public void setTransferAdminService(TransferAdminService transferAdminService) {
		this.transferAdminService = transferAdminService;
	}

	public void setTransferProfileService(TransferProfileService transferProfileService) {
		this.transferProfileService = transferProfileService;
	}

	@Override
	public List<TransferApprovalItem> getPendingApprovals() {
		requireApprover();
		if (transferDao == null) {
			return Collections.emptyList();
		}
		List<Transfer> transfers = transferDao.getTransfersByLocalApprovalStatus(TransferApprovalStatus.PENDING);
		if (transfers == null || transfers.isEmpty()) {
			return Collections.emptyList();
		}
		List<TransferApprovalItem> items = new ArrayList<TransferApprovalItem>();
		for (Transfer transfer : transfers) {
			items.add(toItem(transfer));
		}
		return items;
	}

	@Override
	public int countPendingApprovals() {
		if (transferDao == null) {
			return 0;
		}
		return transferDao.countTransfersByLocalApprovalStatus(TransferApprovalStatus.PENDING);
	}

	@Override
	public Transfer approveTransfer(String transferUuid) {
		requireApprover();
		Transfer transfer = requirePendingTransfer(transferUuid);
		User user = Context.getAuthenticatedUser();
		Date now = new Date();
		transfer.setLocalApprovalStatus(TransferApprovalStatus.APPROVED);
		transfer.setApproverUserId(user != null ? user.getUserId() : null);
		transfer.setApprovedAt(now);
		transfer.setRejectedAt(null);
		transfer.setRejectionReason(null);
		applyApproverSnapshot(transfer, user);
		transfer.setChangedBy(user);
		transfer.setDateChanged(now);
		transferDao.saveTransfer(transfer);

		if (transferHieSubmissionService == null) {
			throw new APIException("Transfer HIE submission is not configured");
		}
		return transferHieSubmissionService.submitTransferToHie(transfer.getUuid());
	}

	@Override
	public Transfer rejectTransfer(String transferUuid, String reason) {
		requireApprover();
		String normalizedReason = StringUtils.trimToNull(reason);
		if (normalizedReason == null) {
			throw new APIException("Rejection reason is required");
		}
		Transfer transfer = requirePendingTransfer(transferUuid);
		User user = Context.getAuthenticatedUser();
		Date now = new Date();
		transfer.setLocalApprovalStatus(TransferApprovalStatus.REJECTED);
		transfer.setApproverUserId(user != null ? user.getUserId() : null);
		transfer.setRejectedAt(now);
		transfer.setApprovedAt(null);
		transfer.setRejectionReason(normalizedReason);
		applyApproverSnapshot(transfer, user);
		transfer.setChangedBy(user);
		transfer.setDateChanged(now);
		return transferDao.saveTransfer(transfer);
	}

	private void applyApproverSnapshot(Transfer transfer, User user) {
		if (transfer == null) {
			return;
		}
		String name = "";
		if (user != null && user.getPersonName() != null) {
			name = StringUtils.trimToEmpty(user.getPersonName().getFullName());
		}
		if (StringUtils.isBlank(name) && user != null) {
			name = StringUtils.defaultString(user.getUsername());
		}
		transfer.setApproverName(StringUtils.trimToNull(name));

		Approver approver = null;
		if (approverService != null && user != null) {
			approver = approverService.getActiveApproverByUserId(user.getUserId());
		}
		transfer.setApproverPosition(approver != null ? StringUtils.trimToNull(approver.getPosition()) : null);

		String phone = null;
		if (transferProfileService != null && user != null) {
			TransferProfile profile = transferProfileService.getProfileForUser(user);
			if (profile != null) {
				phone = StringUtils.trimToNull(profile.getPhoneNumber());
			}
		}
		transfer.setApproverPhone(phone);
	}

	private Transfer requirePendingTransfer(String transferUuid) {
		if (transferDao == null) {
			throw new APIException("Transfer data access is not configured");
		}
		if (StringUtils.isBlank(transferUuid)) {
			throw new APIException("Transfer UUID is required");
		}
		Transfer transfer = transferDao.getTransferByUuid(transferUuid.trim());
		if (transfer == null || Boolean.TRUE.equals(transfer.getVoided())) {
			throw new APIException("Transfer not found");
		}
		if (!transfer.isAwaitingLocalApproval()) {
			throw new APIException("This transfer is not awaiting approval");
		}
		if (transfer.isSentToHie()) {
			throw new APIException("This transfer has already been sent to HIE");
		}
		return transfer;
	}

	private void requireApprover() {
		if (approverService == null || !approverService.isCurrentUserApprover()) {
			throw new APIException("Only configured approvers can manage transfer approvals");
		}
	}

	private TransferApprovalItem toItem(Transfer transfer) {
		TransferApprovalItem item = new TransferApprovalItem();
		item.setTransferId(transfer.getTransferId());
		item.setUuid(transfer.getUuid());
		Patient patient = transfer.getPatient();
		if (patient != null) {
			item.setPatientId(patient.getPatientId());
			if (patient.getPersonName() != null) {
				item.setPatientName(StringUtils.trimToEmpty(patient.getPersonName().getFullName()));
			}
		}
		item.setUpid(StringUtils.defaultString(transfer.getEmrId()));
		item.setReceivingFacilityCode(StringUtils.defaultString(transfer.getReceivingFacilityCode()));
		item.setReceivingFacilityName(resolveFacilityName(transfer.getReceivingFacilityCode()));
		item.setReceivingService(StringUtils.defaultString(transfer.getReceivingService()));
		item.setHealthInsuranceType(StringUtils.defaultString(transfer.getHealthInsuranceType()));
		item.setDecisionToTransferAt(transfer.getDecisionToTransferAt());
		item.setDateCreated(transfer.getDateCreated());
		item.setLocalApprovalStatus(transfer.getLocalApprovalStatus().name());
		item.setRejectionReason(StringUtils.defaultString(transfer.getRejectionReason()));
		return item;
	}

	private String resolveFacilityName(String facilityCode) {
		if (transferAdminService == null || StringUtils.isBlank(facilityCode)) {
			return StringUtils.defaultString(facilityCode);
		}
		Integer sendingLocationId = transferAdminService.resolveCurrentSendingLocationId();
		String name = transferAdminService.resolveReceivingFacilityName(sendingLocationId, facilityCode);
		return StringUtils.defaultIfEmpty(StringUtils.trimToNull(name), facilityCode);
	}
}
