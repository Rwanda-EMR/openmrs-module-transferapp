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
import org.openmrs.User;
import org.openmrs.api.APIException;
import org.openmrs.api.context.Context;
import org.openmrs.module.transferapp.api.NeonatalTransferHieSubmissionService;
import org.openmrs.module.transferapp.api.TransferAdminService;
import org.openmrs.module.transferapp.api.TransferPatientSnapshotResolver;
import org.openmrs.module.transferapp.api.dao.NeonatalTransferDao;
import org.openmrs.module.transferapp.hie.ClientRegistryPatientPayloadBuilder;
import org.openmrs.module.transferapp.hie.ClientRegistryPatientNotPresentClassifier;
import org.openmrs.module.transferapp.hie.HieApiException;
import org.openmrs.module.transferapp.hie.HieBasicConnection;
import org.openmrs.module.transferapp.hie.HieClientRegistryClient;
import org.openmrs.module.transferapp.hie.HieConfigurationException;
import org.openmrs.module.transferapp.hie.HieConnectionResolver;
import org.openmrs.module.transferapp.hie.HieShrClient;
import org.openmrs.module.transferapp.hie.NeonatalTransferEncounterPayloadBuilder;
import org.openmrs.module.transferapp.model.NeonatalTransfer;

import java.util.Date;
import java.util.UUID;

public class NeonatalTransferHieSubmissionServiceImpl implements NeonatalTransferHieSubmissionService {

	private static final Log log = LogFactory.getLog(NeonatalTransferHieSubmissionServiceImpl.class);

	private NeonatalTransferDao neonatalTransferDao;

	private TransferAdminService transferAdminService;

	private HieConnectionResolver hieConnectionResolver = new HieConnectionResolver();

	private HieShrClient hieShrClient = new HieShrClient();

	private HieClientRegistryClient hieClientRegistryClient = new HieClientRegistryClient();

	private ClientRegistryPatientPayloadBuilder clientRegistryPatientPayloadBuilder =
			new ClientRegistryPatientPayloadBuilder();

	private NeonatalTransferEncounterPayloadBuilder payloadBuilder = new NeonatalTransferEncounterPayloadBuilder();

	private TransferPatientSnapshotResolver patientSnapshotResolver = new TransferPatientSnapshotResolver();

	public void setNeonatalTransferDao(NeonatalTransferDao neonatalTransferDao) {
		this.neonatalTransferDao = neonatalTransferDao;
	}

	public void setTransferAdminService(TransferAdminService transferAdminService) {
		this.transferAdminService = transferAdminService;
	}

	public void setHieShrClient(HieShrClient hieShrClient) {
		this.hieShrClient = hieShrClient != null ? hieShrClient : new HieShrClient();
	}

	public void setHieClientRegistryClient(HieClientRegistryClient hieClientRegistryClient) {
		this.hieClientRegistryClient = hieClientRegistryClient != null
				? hieClientRegistryClient
				: new HieClientRegistryClient();
	}

	public void setClientRegistryPatientPayloadBuilder(
			ClientRegistryPatientPayloadBuilder clientRegistryPatientPayloadBuilder) {
		this.clientRegistryPatientPayloadBuilder = clientRegistryPatientPayloadBuilder != null
				? clientRegistryPatientPayloadBuilder
				: new ClientRegistryPatientPayloadBuilder();
	}

	public void setPayloadBuilder(NeonatalTransferEncounterPayloadBuilder payloadBuilder) {
		this.payloadBuilder = payloadBuilder != null ? payloadBuilder : new NeonatalTransferEncounterPayloadBuilder();
	}

	@Override
	public NeonatalTransfer submitNeonatalTransferToHie(String transferUuid) {
		if (StringUtils.isBlank(transferUuid)) {
			throw new APIException("Transfer UUID is required");
		}

		NeonatalTransfer transfer = neonatalTransferDao.getNeonatalTransferByUuid(transferUuid.trim());
		if (transfer == null) {
			throw new APIException("Transfer not found");
		}
		if (transfer.isSentToHie()) {
			throw new APIException("This transfer has already been sent to HIE.");
		}

		try {
			ensurePayloadBuilderConfigured();
			HieBasicConnection connection = hieConnectionResolver.resolveConnection();
			String receivingFacilityLabel = resolveReceivingFacilityLabel(transfer);
			User currentUser = Context.getAuthenticatedUser();

			String encounterId = StringUtils.isNotBlank(transfer.getUuid())
					? transfer.getUuid().trim()
					: UUID.randomUUID().toString();
			String encounterJson = payloadBuilder.buildEncounterJson(
					transfer, currentUser, receivingFacilityLabel, encounterId);

			postEncounterRegisteringPatientInCrIfNeeded(connection, transfer, encounterJson);

			transfer.setHieTransferId(encounterId);
			transfer.setHieSent(true);
			transfer.setHieSentAt(new Date());
			transfer.setHieSendError(null);

			return neonatalTransferDao.saveNeonatalTransfer(transfer);
		}
		catch (HieConfigurationException ex) {
			recordSubmissionFailure(transfer, ex.getMessage());
			throw ex;
		}
		catch (HieApiException ex) {
			recordSubmissionFailure(transfer, ex.getMessage());
			throw ex;
		}
		catch (RuntimeException ex) {
			recordSubmissionFailure(transfer, ex.getMessage());
			throw ex;
		}
	}

	/**
	 * Posts the transfer encounter. When SHR rejects with "Patient is not present in the CR",
	 * builds and pushes a Client Registry Patient from OpenMRS demographics, then retries once.
	 */
	private void postEncounterRegisteringPatientInCrIfNeeded(HieBasicConnection connection, NeonatalTransfer transfer,
			String encounterJson) {
		try {
			hieShrClient.updateTransferEncounter(connection, encounterJson);
		}
		catch (HieApiException ex) {
			if (!ClientRegistryPatientNotPresentClassifier.isPatientNotPresentInCr(ex.getMessage())) {
				throw ex;
			}
			log.warn("HIE rejected neonatal transfer because patient is missing from Client Registry; "
					+ "pushing patient then retrying. Cause: " + ex.getMessage());
			pushPatientToClientRegistry(connection, transfer);
			hieShrClient.updateTransferEncounter(connection, encounterJson);
		}
	}

	private void pushPatientToClientRegistry(HieBasicConnection connection, NeonatalTransfer transfer) {
		Patient patient = transfer != null ? transfer.getPatient() : null;
		if (patient == null) {
			throw new HieApiException(
					"Cannot register patient in Client Registry: transfer has no linked OpenMRS patient");
		}
		clientRegistryPatientPayloadBuilder.setPatientSnapshotResolver(patientSnapshotResolver);
		String patientJson = clientRegistryPatientPayloadBuilder.buildPatientJson(
				patient, null, transfer.getMotherCaregiverPhone(), null);
		hieClientRegistryClient.postPatientAllowingAlreadyExists(connection, patientJson);
	}

	private void recordSubmissionFailure(NeonatalTransfer transfer, String message) {
		transfer.setHieSent(false);
		transfer.setHieSendError(truncateMessage(message));
		neonatalTransferDao.saveNeonatalTransfer(transfer);
	}

	private String resolveReceivingFacilityLabel(NeonatalTransfer transfer) {
		String facilityCode = transfer.getReceivingFacilityCode();
		if (StringUtils.isBlank(facilityCode)) {
			return "";
		}
		if (transferAdminService != null) {
			Integer sendingLocationId = transferAdminService.resolveCurrentSendingLocationId();
			String label = transferAdminService.resolveReceivingFacilityName(sendingLocationId, facilityCode);
			if (!facilityCode.equals(label)) {
				return label;
			}
		}
		return facilityCode;
	}

	private void ensurePayloadBuilderConfigured() {
		if (payloadBuilder == null) {
			payloadBuilder = new NeonatalTransferEncounterPayloadBuilder();
		}
		if (transferAdminService != null) {
			payloadBuilder.setTransferAdminService(transferAdminService);
		}
		payloadBuilder.setPatientSnapshotResolver(patientSnapshotResolver);
	}

	private static String truncateMessage(String message) {
		if (message == null) {
			return null;
		}
		String trimmed = message.trim();
		if (trimmed.length() <= 500) {
			return trimmed;
		}
		return trimmed.substring(0, 497) + "...";
	}

}
