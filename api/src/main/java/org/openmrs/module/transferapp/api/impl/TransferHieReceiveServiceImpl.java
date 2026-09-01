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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.PersonAddress;
import org.openmrs.api.APIException;
import org.openmrs.api.PatientService;
import org.openmrs.api.context.Context;
import org.openmrs.module.transferapp.api.TransferAdminService;
import org.openmrs.module.transferapp.api.TransferHieReceiveService;
import org.openmrs.module.transferapp.api.TransferHieSearchService;
import org.openmrs.module.transferapp.api.TransferPatientSnapshotResolver;
import org.openmrs.module.transferapp.api.dao.TransferDao;
import org.openmrs.module.transferapp.hie.HieReceivedTransferMapper;
import org.openmrs.module.transferapp.model.Transfer;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TransferHieReceiveServiceImpl implements TransferHieReceiveService {

	private static final Log log = LogFactory.getLog(TransferHieReceiveServiceImpl.class);

	private TransferDao transferDao;

	private PatientService patientService;

	private TransferHieSearchService transferHieSearchService;

	private TransferAdminService transferAdminService;

	private TransferPatientSnapshotResolver patientSnapshotResolver = new TransferPatientSnapshotResolver();

	private HieReceivedTransferMapper receivedTransferMapper = new HieReceivedTransferMapper();

	public void setTransferDao(TransferDao transferDao) {
		this.transferDao = transferDao;
	}

	public void setPatientService(PatientService patientService) {
		this.patientService = patientService;
	}

	public void setTransferHieSearchService(TransferHieSearchService transferHieSearchService) {
		this.transferHieSearchService = transferHieSearchService;
	}

	public void setTransferAdminService(TransferAdminService transferAdminService) {
		this.transferAdminService = transferAdminService;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Transfer receiveTransferFromHie(Integer patientId, String hieTransferId) {
		if (patientId == null) {
			throw new APIException("Patient is required");
		}
		if (StringUtils.isBlank(hieTransferId)) {
			throw new APIException("HIE transfer id is required");
		}

		String normalizedHieTransferId = hieTransferId.trim();
		Transfer existing = transferDao.getTransferByHieTransferId(patientId, normalizedHieTransferId);
		if (existing != null) {
			log.info("Transfer already stored for patient " + patientId + " and HIE id " + normalizedHieTransferId);
			return existing;
		}

		Patient patient = patientService.getPatient(patientId);
		if (patient == null) {
			throw new APIException("Patient not found");
		}

		String upid = patientSnapshotResolver.resolveUpid(patient);
		if (StringUtils.isBlank(upid)) {
			throw new APIException("Patient UPID is required to receive a transfer from HIE");
		}

		Map<String, Object> searchResult = transferHieSearchService.searchTransfers(upid, normalizedHieTransferId, false);
		if (searchResult == null || !"success".equals(searchResult.get("status"))) {
			String message = searchResult != null && searchResult.get("message") != null
					? String.valueOf(searchResult.get("message"))
					: "Unable to fetch transfer from HIE";
			throw new APIException(message);
		}

		Object data = searchResult.get("data");
		if (!(data instanceof List) || ((List<?>) data).isEmpty()) {
			throw new APIException("Transfer not found in HIE for UPID " + upid);
		}

		Map<String, Object> hieTransfer = (Map<String, Object>) ((List<?>) data).get(0);
		return storeReceivedTransfer(patientId, hieTransfer);
	}

	@Override
	public Transfer storeReceivedTransfer(Integer patientId, Map<String, Object> hieTransfer) {
		if (patientId == null) {
			throw new APIException("Patient is required");
		}
		if (hieTransfer == null || hieTransfer.isEmpty()) {
			throw new APIException("HIE transfer data is required");
		}

		String normalizedHieTransferId = resolveHieTransferId(hieTransfer);
		if (StringUtils.isBlank(normalizedHieTransferId)) {
			throw new APIException("HIE transfer id is required");
		}

		Transfer existing = transferDao.getTransferByHieTransferId(patientId, normalizedHieTransferId);
		if (existing != null) {
			log.info("Transfer already stored for patient " + patientId + " and HIE id " + normalizedHieTransferId);
			return existing;
		}

		Patient patient = patientService.getPatient(patientId);
		if (patient == null) {
			throw new APIException("Patient not found");
		}

		String receivingFacilityName = transferAdminService != null
				? transferAdminService.resolveCurrentSendingFacilityName()
				: null;

		Transfer transfer = receivedTransferMapper.mapToTransfer(patient, hieTransfer, receivingFacilityName);
		if (StringUtils.isBlank(transfer.getHieTransferId())) {
			transfer.setHieTransferId(normalizedHieTransferId);
		}
		applyPatientFallbacks(transfer, patient);
		transfer.setUuid(UUID.randomUUID().toString());
		transfer.setCreator(Context.getAuthenticatedUser());
		transfer.setDateCreated(new Date());
		transfer.setVoided(false);

		return transferDao.saveTransfer(transfer);
	}

	private static String resolveHieTransferId(Map<String, Object> hieTransfer) {
		Object id = hieTransfer.get("id");
		if (id != null && StringUtils.isNotBlank(String.valueOf(id))) {
			return String.valueOf(id).trim();
		}
		Object uuid = hieTransfer.get("uuid");
		if (uuid != null && StringUtils.isNotBlank(String.valueOf(uuid))) {
			return String.valueOf(uuid).trim();
		}
		return null;
	}

	private void applyPatientFallbacks(Transfer transfer, Patient patient) {
		if (patient.getPersonName() != null && StringUtils.isBlank(transfer.getClientName())) {
			transfer.setClientName(patient.getPersonName().getFullName());
		}
		if (StringUtils.isBlank(transfer.getEmrId())) {
			transfer.setEmrId(patientSnapshotResolver.resolveUpid(patient));
		}
		if (StringUtils.isBlank(transfer.getClientTelephone())) {
			transfer.setClientTelephone(patientSnapshotResolver.resolvePatientPhone(patient, transferDao));
		}
		if (StringUtils.isBlank(transfer.getAgeOrDob())) {
			transfer.setAgeOrDob(patientSnapshotResolver.resolveAgeOrDob(patient));
		}
		if (StringUtils.isBlank(transfer.getSex())) {
			transfer.setSex(patientSnapshotResolver.mapGender(patient.getGender()));
		}

		PatientIdentifier nationalId = patientSnapshotResolver.resolveNationalIdentifier(patient);
		if (nationalId != null && nationalId.getIdentifierType() != null) {
			if (StringUtils.isBlank(transfer.getIdentifierType())) {
				transfer.setIdentifierType(nationalId.getIdentifierType().getName());
			}
			if (StringUtils.isBlank(transfer.getIdentifierValue())) {
				transfer.setIdentifierValue(nationalId.getIdentifier());
			}
		}

		if (StringUtils.isBlank(transfer.getCaregiverName())) {
			transfer.setCaregiverName(patientSnapshotResolver.resolveCaregiverName(patient));
		}
		if (StringUtils.isBlank(transfer.getCaregiverTelephone())) {
			transfer.setCaregiverTelephone(patientSnapshotResolver.resolveCaregiverTelephone(patient));
		}

		PersonAddress address = transferDao != null
				? transferDao.getPreferredPersonAddress(patient.getPatientId())
				: null;
		if (address == null) {
			address = patientSnapshotResolver.resolveActivePersonAddress(patient);
		}
		if (address != null) {
			if (StringUtils.isBlank(transfer.getClientDistrict())) {
				transfer.setClientDistrict(patientSnapshotResolver.resolveDistrict(address));
			}
			if (StringUtils.isBlank(transfer.getSector())) {
				transfer.setSector(patientSnapshotResolver.resolveSector(address));
			}
			if (StringUtils.isBlank(transfer.getCell())) {
				transfer.setCell(patientSnapshotResolver.resolveCell(address));
			}
			if (StringUtils.isBlank(transfer.getVillage())) {
				transfer.setVillage(patientSnapshotResolver.resolveVillage(address));
			}
		}
	}

}
