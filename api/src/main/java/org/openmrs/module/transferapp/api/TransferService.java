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

import org.openmrs.Patient;
import org.openmrs.annotation.Authorized;
import org.openmrs.module.transferapp.TransferAppActivator;
import org.openmrs.module.transferapp.model.Transfer;
import org.openmrs.module.transferapp.model.TransferFormExtras;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Transactional
public interface TransferService {

	@Authorized(TransferAppActivator.PRIVILEGE_CREATE_TRANSFER)
	Transfer saveReferralTransfer(Integer patientId,
			String decisionToTransferAt,
			String callingTime,
			String receivingFacilityCode,
			Integer receivingFacilityId,
			String receivingService,
			String staffContactedName,
			String staffContactedPhone,
			String transferType,
			String ambulanceCalledTime,
			String departureFromReferringTime,
			String transportationType,
			String transportationOtherSpec,
			String reasonForTransfer);

	@Authorized(TransferAppActivator.PRIVILEGE_CREATE_TRANSFER)
	Transfer saveReferralTransfer(Integer patientId,
			String decisionToTransferAt,
			String callingTime,
			String receivingFacilityCode,
			Integer receivingFacilityId,
			String receivingService,
			String staffContactedName,
			String staffContactedPhone,
			String transferType,
			String ambulanceCalledTime,
			String departureFromReferringTime,
			String transportationType,
			String transportationOtherSpec,
			String reasonForTransfer,
			TransferFormExtras formExtras);

	@Authorized(TransferAppActivator.PRIVILEGE_CREATE_TRANSFER)
	Transfer saveReferralTransfer(Integer patientId,
			String transferUuid,
			String decisionToTransferAt,
			String callingTime,
			String receivingFacilityCode,
			Integer receivingFacilityId,
			String receivingService,
			String staffContactedName,
			String staffContactedPhone,
			String transferType,
			String ambulanceCalledTime,
			String departureFromReferringTime,
			String transportationType,
			String transportationOtherSpec,
			String reasonForTransfer,
			TransferFormExtras formExtras);

	@Authorized(TransferAppActivator.PRIVILEGE_CREATE_TRANSFER)
	Transfer saveReferralTransfer(Integer patientId,
			String transferUuid,
			String decisionToTransferAt,
			String callingTime,
			String receivingFacilityCode,
			Integer receivingFacilityId,
			String receivingService,
			String staffContactedName,
			String staffContactedPhone,
			String transferType,
			String ambulanceCalledTime,
			String departureFromReferringTime,
			String transportationType,
			String transportationOtherSpec,
			String ambulanceProviderFosaId,
			String ambulanceProviderName,
			String reasonForTransfer,
			TransferFormExtras formExtras);

	@Authorized(TransferAppActivator.PRIVILEGE_LIST_TRANSFERS)
	@Transactional(readOnly = true)
	List<Transfer> getTransfersByPatient(Patient patient);

	@Authorized(TransferAppActivator.PRIVILEGE_LIST_TRANSFERS)
	@Transactional(readOnly = true)
	List<Transfer> getTransfersByPatient(Patient patient, Integer limit);

	@Authorized(TransferAppActivator.PRIVILEGE_LIST_TRANSFERS)
	@Transactional(readOnly = true)
	int countTransfersByPatient(Patient patient);

	@Authorized(TransferAppActivator.PRIVILEGE_LIST_TRANSFERS)
	@Transactional(readOnly = true)
	Transfer getTransferByUuid(String uuid);

}
