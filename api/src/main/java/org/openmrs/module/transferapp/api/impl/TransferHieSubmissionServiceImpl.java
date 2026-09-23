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
import org.openmrs.module.transferapp.api.PatientSmsNotificationService;
import org.openmrs.module.transferapp.api.TransferAdminService;
import org.openmrs.module.transferapp.api.TransferHieSubmissionService;
import org.openmrs.module.transferapp.api.TransferPatientSnapshotResolver;
import org.openmrs.module.transferapp.api.TransferProfileService;
import org.openmrs.module.transferapp.api.dao.TransferDao;
import org.openmrs.module.transferapp.hie.ClientRegistryPatientNotPresentClassifier;
import org.openmrs.module.transferapp.hie.ClientRegistryPatientPayloadBuilder;
import org.openmrs.module.transferapp.hie.HieApiException;
import org.openmrs.module.transferapp.hie.HieBasicConnection;
import org.openmrs.module.transferapp.hie.HieClientRegistryClient;
import org.openmrs.module.transferapp.hie.HieConfigurationException;
import org.openmrs.module.transferapp.hie.HieConnectionResolver;
import org.openmrs.module.transferapp.hie.HieInsuranceAgentDecisionPreserver;
import org.openmrs.module.transferapp.hie.HieShrClient;
import org.openmrs.module.transferapp.hie.TransferEncounterPayloadBuilder;
import org.openmrs.module.transferapp.model.Transfer;
import org.openmrs.module.transferapp.model.TransferApprovalStatus;
import org.openmrs.module.transferapp.model.TransferProfile;

import java.util.Date;
import java.util.UUID;

public class TransferHieSubmissionServiceImpl implements TransferHieSubmissionService {

	private static final Log log = LogFactory.getLog(TransferHieSubmissionServiceImpl.class);

	private TransferDao transferDao;

	private TransferAdminService transferAdminService;

	private TransferProfileService transferProfileService;

	private PatientSmsNotificationService patientSmsNotificationService;

	private HieConnectionResolver hieConnectionResolver = new HieConnectionResolver();

	private HieShrClient hieShrClient = new HieShrClient();

	private HieClientRegistryClient hieClientRegistryClient = new HieClientRegistryClient();

	private ClientRegistryPatientPayloadBuilder clientRegistryPatientPayloadBuilder =
			new ClientRegistryPatientPayloadBuilder();

	private TransferEncounterPayloadBuilder payloadBuilder = new TransferEncounterPayloadBuilder();

	private TransferPatientSnapshotResolver patientSnapshotResolver = new TransferPatientSnapshotResolver();

	private HieInsuranceAgentDecisionPreserver agentDecisionPreserver = new HieInsuranceAgentDecisionPreserver();

	public void setTransferDao(TransferDao transferDao) {
		this.transferDao = transferDao;
	}

	public void setTransferAdminService(TransferAdminService transferAdminService) {
		this.transferAdminService = transferAdminService;
	}

	public void setTransferProfileService(TransferProfileService transferProfileService) {
		this.transferProfileService = transferProfileService;
	}

	public void setPatientSmsNotificationService(PatientSmsNotificationService patientSmsNotificationService) {
		this.patientSmsNotificationService = patientSmsNotificationService;
	}

	public void setPayloadBuilder(TransferEncounterPayloadBuilder payloadBuilder) {
		this.payloadBuilder = payloadBuilder;
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

	public void setAgentDecisionPreserver(HieInsuranceAgentDecisionPreserver agentDecisionPreserver) {
		this.agentDecisionPreserver = agentDecisionPreserver != null
				? agentDecisionPreserver
				: new HieInsuranceAgentDecisionPreserver();
	}

	@Override
	public Transfer submitTransferToHie(String transferUuid) {
		if (StringUtils.isBlank(transferUuid)) {
			throw new APIException("Transfer UUID is required");
		}

		Transfer transfer = transferDao.getTransferByUuid(transferUuid.trim());
		if (transfer == null) {
			throw new APIException("Transfer not found");
		}
		if (transfer.isSentToHie()) {
			throw new APIException("This transfer has already been sent to HIE. Edit the transfer first to resubmit an update.");
		}
		if (transfer.isAwaitingLocalApproval()) {
			throw new APIException(
					"This transfer targets an external hospital and is awaiting approver approval before it can be sent to HIE.");
		}
		if (TransferApprovalStatus.REJECTED.equals(transfer.getLocalApprovalStatus())) {
			throw new APIException(
					"This transfer was rejected by an approver. Edit and save it again to resubmit for approval.");
		}
		boolean externalReceivingFacility = false;
		ensurePayloadBuilderConfigured();
		externalReceivingFacility = payloadBuilder.isExternalReceivingFacility(transfer);
		if (externalReceivingFacility && !transfer.isLocallyApproved()) {
			throw new APIException(
					"This transfer targets an external hospital and must be approved before it can be sent to HIE.");
		}

		boolean firstSuccessfulSubmit = StringUtils.isBlank(transfer.getHieTransferId());

		try {
			refreshDiagnosisFromObsIfNeeded(transfer);
			applyProviderQualificationWithSpeciality(transfer);
			applyConfiguredSendingFacility(transfer);
			ensureCaregiverFromPatientIfBlank(transfer);
			ensurePatientUpidPersisted(transfer);
			HieBasicConnection connection = hieConnectionResolver.resolveConnection();
			String receivingFacilityLabel = resolveReceivingFacilityLabel(transfer);
			User currentUser = Context.getAuthenticatedUser();

			boolean requiresInsuranceAgentVerification = payloadBuilder.requiresInsuranceAgentVerification(
					transfer, externalReceivingFacility);
			boolean isHieUpdate = StringUtils.isNotBlank(transfer.getHieTransferId());
			String encounterId = resolveEncounterIdForSubmit(transfer);
			String encounterJson = payloadBuilder.buildEncounterJson(
					transfer, currentUser, receivingFacilityLabel, externalReceivingFacility, encounterId);

			// On resubmit (or external destinations that need agent verification), pull the
			// existing HIE Encounter so HIE-owned attributes such as insurance approval status
			// are kept while local clinical/UPID updates from the rebuilt payload are applied.
			if (isHieUpdate || requiresInsuranceAgentVerification) {
				encounterJson = mergeWithExistingHieDecision(
						connection, encounterJson, encounterId, requiresInsuranceAgentVerification);
			}

			postEncounterRegisteringPatientInCrIfNeeded(connection, transfer, encounterJson);

			transfer.setHieTransferId(encounterId);
			transfer.setHieSent(true);
			transfer.setHieSentAt(new Date());
			transfer.setHieSendError(null);
			transfer.setChangedBy(currentUser);
			transfer.setDateChanged(new Date());

			// Patient SMS only on first successful HIE acceptance (not on clinical update resubmits).
			if (firstSuccessfulSubmit && patientSmsNotificationService != null && !transfer.isPatientSmsSent()) {
				patientSmsNotificationService.notifyPatientAfterHieAccepted(transfer, receivingFacilityLabel);
			}

			return transferDao.saveTransfer(transfer);
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
	private void postEncounterRegisteringPatientInCrIfNeeded(HieBasicConnection connection, Transfer transfer,
			String encounterJson) {
		try {
			hieShrClient.updateTransferEncounter(connection, encounterJson);
		}
		catch (HieApiException ex) {
			if (!ClientRegistryPatientNotPresentClassifier.isPatientNotPresentInCr(ex.getMessage())) {
				throw ex;
			}
			log.warn("HIE rejected transfer because patient is missing from Client Registry; "
					+ "pushing patient then retrying. Cause: " + ex.getMessage());
			pushPatientToClientRegistry(connection, transfer);
			hieShrClient.updateTransferEncounter(connection, encounterJson);
		}
	}

	private void pushPatientToClientRegistry(HieBasicConnection connection, Transfer transfer) {
		Patient patient = transfer != null ? transfer.getPatient() : null;
		if (patient == null) {
			throw new HieApiException(
					"Cannot register patient in Client Registry: transfer has no linked OpenMRS patient");
		}
		clientRegistryPatientPayloadBuilder.setPatientSnapshotResolver(patientSnapshotResolver);
		String patientJson = clientRegistryPatientPayloadBuilder.buildPatientJson(patient, transfer);
		hieClientRegistryClient.postPatientAllowingAlreadyExists(connection, patientJson);
	}

	/**
	 * Prefer the previously stored HIE encounter id so updates target the same resource.
	 */
	private String resolveEncounterIdForSubmit(Transfer transfer) {
		if (StringUtils.isNotBlank(transfer.getHieTransferId())) {
			return transfer.getHieTransferId().trim();
		}
		if (StringUtils.isNotBlank(transfer.getUuid())) {
			return transfer.getUuid().trim();
		}
		return UUID.randomUUID().toString();
	}

	/**
	 * Pulls the existing Encounter from HIE and re-attaches HIE-owned insurance/agent attributes
	 * (approval status, comments, and agent-redirected destination when decided) onto the newly
	 * built clinical payload. Local clinical fields and updated UPID remain from the rebuild.
	 */
	private String mergeWithExistingHieDecision(HieBasicConnection connection, String clinicalEncounterJson,
			String encounterId, boolean keepRequiresVerification) {
		String existingJson = hieShrClient.fetchEncounterById(connection, encounterId);
		if (StringUtils.isBlank(existingJson)) {
			// No prior HIE resource (404 or RHIE "Encounter … not found") — submit as a new transfer.
			log.info("No existing HIE Encounter for id " + encounterId
					+ "; submitting clinical payload as a new transfer.");
			if (keepRequiresVerification) {
				return agentDecisionPreserver.ensureRequiresVerification(clinicalEncounterJson);
			}
			return clinicalEncounterJson;
		}
		return agentDecisionPreserver.mergePreservingAgentDecision(
				clinicalEncounterJson, existingJson, encounterId, keepRequiresVerification);
	}

	private void applyProviderQualificationWithSpeciality(Transfer transfer) {
		if (transfer == null || transferProfileService == null) {
			return;
		}
		User user = Context.getAuthenticatedUser();
		if (user == null) {
			return;
		}
		TransferProfile profile = transferProfileService.getProfileForUser(user);
		if (profile == null) {
			return;
		}
		String combined = StringUtils.trimToNull(profile.getQualificationWithSpeciality());
		if (combined != null) {
			transfer.setProviderQualification(combined);
		}
		String phone = StringUtils.trimToNull(profile.getPhoneNumber());
		if (phone != null) {
			transfer.setProviderPhone(phone);
		}
		String referringName = StringUtils.trimToNull(transfer.getReferringProviderName());
		if (referringName == null && user.getPerson() != null && user.getPerson().getPersonName() != null) {
			referringName = StringUtils.trimToNull(user.getPerson().getPersonName().getFullName());
		}
		if (referringName == null) {
			referringName = StringUtils.trimToNull(user.getUsername());
		}
		transfer.setReferringProviderName(TransferProfile.formatCareProviderName(
				referringName, profile.getLicenseNumber()));
	}

	/**
	 * Fills caregiver from patient demographics when missing. Never uses the referring clinician.
	 */
	private void ensureCaregiverFromPatientIfBlank(Transfer transfer) {
		if (transfer == null || transfer.getPatient() == null) {
			return;
		}
		if (StringUtils.isBlank(transfer.getCaregiverName())) {
			transfer.setCaregiverName(patientSnapshotResolver.resolveCaregiverName(transfer.getPatient()));
		}
		if (StringUtils.isBlank(transfer.getCaregiverTelephone())) {
			transfer.setCaregiverTelephone(patientSnapshotResolver.resolveCaregiverTelephone(transfer.getPatient()));
		}
	}

	/**
	 * Aligns outbound sending facility with {@code transferapp.outboundFacilityName} before HIE submit.
	 */
	private void applyConfiguredSendingFacility(Transfer transfer) {
		if (transfer == null || transferAdminService == null) {
			return;
		}
		String configuredName = StringUtils.trimToNull(transferAdminService.resolveOutboundFacilityName());
		if (configuredName != null) {
			transfer.setSendingFacility(configuredName);
		}
	}

	private void refreshDiagnosisFromObsIfNeeded(Transfer transfer) {
		if (transfer == null || transfer.getPatient() == null) {
			return;
		}
		String current = StringUtils.trimToEmpty(transfer.getDiagnosis());
		boolean needsRefresh = StringUtils.isBlank(current)
				|| "Primary Diagnosis".equalsIgnoreCase(current)
				|| "Secondary Diagnosis".equalsIgnoreCase(current);
		if (!needsRefresh) {
			return;
		}
		String resolved = patientSnapshotResolver.resolveDiagnosis(transfer.getPatient());
		if (StringUtils.isNotBlank(resolved)) {
			transfer.setDiagnosis(resolved);
		} else if ("Primary Diagnosis".equalsIgnoreCase(current)
				|| "Secondary Diagnosis".equalsIgnoreCase(current)) {
			transfer.setDiagnosis(null);
		}
	}

	private void recordSubmissionFailure(Transfer transfer, String message) {
		transfer.setHieSent(false);
		transfer.setHieSendError(truncateMessage(message));
		transfer.setChangedBy(Context.getAuthenticatedUser());
		transfer.setDateChanged(new Date());
		transferDao.saveTransfer(transfer);
	}

	private String resolveReceivingFacilityLabel(Transfer transfer) {
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
		return defaultFacilityLabel(facilityCode);
	}

	/**
	 * Resolves the patient's UPID into {@code transfer.emrId} and persists it so the HIE
	 * payload (and later resubmits) include the identifier.
	 */
	private void ensurePatientUpidPersisted(Transfer transfer) {
		if (transfer == null) {
			return;
		}
		String before = StringUtils.trimToNull(transfer.getEmrId());
		String upid = patientSnapshotResolver.ensureEmrIdFromPatient(transfer, transfer.getPatient());
		if (StringUtils.isBlank(upid)) {
			throw new HieApiException(
					"Cannot submit transfer: patient UPID is missing. Register a UPID on the patient chart, then edit and resubmit the transfer.");
		}
		String after = StringUtils.trimToNull(transfer.getEmrId());
		if (!StringUtils.equals(before, after)) {
			transfer.setChangedBy(Context.getAuthenticatedUser());
			transfer.setDateChanged(new Date());
			transferDao.saveTransfer(transfer);
		}
	}

	private void ensurePayloadBuilderConfigured() {
		if (payloadBuilder == null) {
			payloadBuilder = new TransferEncounterPayloadBuilder();
		}
		if (transferAdminService != null) {
			payloadBuilder.setTransferAdminService(transferAdminService);
		}
		if (transferProfileService != null) {
			payloadBuilder.setTransferProfileService(transferProfileService);
		}
		if (patientSnapshotResolver != null) {
			payloadBuilder.setPatientSnapshotResolver(patientSnapshotResolver);
		}
	}

	private static String defaultFacilityLabel(String facilityCode) {
		if ("KUTH".equals(facilityCode)) {
			return "Kigali University Teaching Hospital";
		}
		if ("RUHENGERI".equals(facilityCode)) {
			return "Ruhengeri District Hospital";
		}
		if ("BUTARO".equals(facilityCode)) {
			return "Butaro District Hospital";
		}
		if ("KFH".equals(facilityCode)) {
			return "King Faisal Hospital";
		}
		return facilityCode;
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
