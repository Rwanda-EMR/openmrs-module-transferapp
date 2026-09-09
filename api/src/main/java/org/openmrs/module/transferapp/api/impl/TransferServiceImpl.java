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
import org.openmrs.PersonAddress;
import org.openmrs.User;
import org.openmrs.api.APIException;
import org.openmrs.api.PatientService;
import org.openmrs.api.context.Context;
import org.openmrs.module.transferapp.TransferAppConstants;
import org.openmrs.module.transferapp.api.PatientInsuranceService;
import org.openmrs.module.transferapp.api.TransferAdminService;
import org.openmrs.module.transferapp.api.TransferAmbulanceBillingService;
import org.openmrs.module.transferapp.api.TransferFacilityRegistryService;
import org.openmrs.module.transferapp.api.TransferPatientSnapshotResolver;
import org.openmrs.module.transferapp.api.TransferProfileService;
import org.openmrs.module.transferapp.api.TransferService;
import org.openmrs.module.transferapp.api.dao.TransferDao;
import org.openmrs.module.transferapp.model.PatientInsuranceInfo;
import org.openmrs.module.transferapp.model.ReceivingFacility;
import org.openmrs.module.transferapp.model.RegistryFacility;
import org.openmrs.module.transferapp.model.Transfer;
import org.openmrs.module.transferapp.model.TransferFormExtras;
import org.openmrs.module.transferapp.model.TransferFormKind;
import org.openmrs.module.transferapp.model.TransferProfile;
import org.openmrs.module.rwandaemr.queue.QueueService;
import org.openmrs.module.rwandaemr.queue.model.QueueEntry;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class TransferServiceImpl implements TransferService {

	private static final Log log = LogFactory.getLog(TransferServiceImpl.class);

	private static final String DATETIME_LOCAL_PATTERN = "yyyy-MM-dd'T'HH:mm";
	private static final String DATETIME_SPACE_PATTERN = "yyyy-MM-dd HH:mm";
	private static final String DATE_PATTERN = "yyyy-MM-dd";
	private static final String TIME_PATTERN = "HH:mm";
	private static final String TRANSFER_TYPE_EMERGENCY = "EMERGENCY";
	private static final String TRANSPORT_TYPE_AMBULANCE = "AMBULANCE";

	private TransferDao transferDao;

	private PatientService patientService;

	private TransferAdminService transferAdminService;

	private PatientInsuranceService patientInsuranceService;

	private TransferProfileService transferProfileService;

	private QueueService queueService;

	private TransferPatientSnapshotResolver patientSnapshotResolver = new TransferPatientSnapshotResolver();

	private TransferAmbulanceBillingService transferAmbulanceBillingService;

	private TransferFacilityRegistryService transferFacilityRegistryService;

	public void setTransferDao(TransferDao transferDao) {
		this.transferDao = transferDao;
	}

	public void setPatientService(PatientService patientService) {
		this.patientService = patientService;
	}

	public void setTransferAdminService(TransferAdminService transferAdminService) {
		this.transferAdminService = transferAdminService;
	}

	public void setPatientInsuranceService(PatientInsuranceService patientInsuranceService) {
		this.patientInsuranceService = patientInsuranceService;
	}

	public void setTransferProfileService(TransferProfileService transferProfileService) {
		this.transferProfileService = transferProfileService;
	}

	public void setQueueService(QueueService queueService) {
		this.queueService = queueService;
	}

	public void setTransferAmbulanceBillingService(TransferAmbulanceBillingService transferAmbulanceBillingService) {
		this.transferAmbulanceBillingService = transferAmbulanceBillingService;
	}

	public void setTransferFacilityRegistryService(TransferFacilityRegistryService transferFacilityRegistryService) {
		this.transferFacilityRegistryService = transferFacilityRegistryService;
	}

	@Override
	public Transfer saveReferralTransfer(Integer patientId,
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
			String reasonForTransfer) {
		return saveReferralTransfer(patientId, null, decisionToTransferAt, callingTime, receivingFacilityCode,
				receivingFacilityId, receivingService, staffContactedName, staffContactedPhone, transferType,
				ambulanceCalledTime, departureFromReferringTime, transportationType, transportationOtherSpec,
				null, null, reasonForTransfer, null);
	}

	@Override
	public Transfer saveReferralTransfer(Integer patientId,
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
			TransferFormExtras formExtras) {
		return saveReferralTransfer(patientId, null, decisionToTransferAt, callingTime, receivingFacilityCode,
				receivingFacilityId, receivingService, staffContactedName, staffContactedPhone, transferType,
				ambulanceCalledTime, departureFromReferringTime, transportationType, transportationOtherSpec,
				null, null, reasonForTransfer, formExtras);
	}

	@Override
	public Transfer saveReferralTransfer(Integer patientId,
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
			TransferFormExtras formExtras) {
		return saveReferralTransfer(patientId, transferUuid, decisionToTransferAt, callingTime, receivingFacilityCode,
				receivingFacilityId, receivingService, staffContactedName, staffContactedPhone, transferType,
				ambulanceCalledTime, departureFromReferringTime, transportationType, transportationOtherSpec,
				null, null, reasonForTransfer, formExtras);
	}

	@Override
	public Transfer saveReferralTransfer(Integer patientId,
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
			TransferFormExtras formExtras) {

		if (patientId == null) {
			throw new APIException("Patient is required");
		}

		Patient patient = patientService.getPatient(patientId);
		if (patient == null) {
			throw new APIException("Patient not found");
		}

		String normalizedTransferType = StringUtils.trimToNull(transferType);
		validateTransferTypeFields(normalizedTransferType, ambulanceCalledTime, departureFromReferringTime);
		validateTransportationFields(normalizedTransferType, transportationType, transportationOtherSpec,
				ambulanceProviderFosaId, ambulanceProviderName);
		ensureReceivingServiceConfigured(receivingFacilityCode, receivingFacilityId, receivingService);

		boolean isUpdate = StringUtils.isNotBlank(transferUuid);
		Transfer transfer;
		String previousReceivingFacilityCode = null;
		String previousTransportType = null;
		if (isUpdate) {
			transfer = transferDao.getTransferByUuid(transferUuid.trim());
			if (transfer == null || transfer.isVoided()) {
				throw new APIException("Transfer not found");
			}
			if (transfer.getPatient() == null
					|| transfer.getPatient().getPatientId() == null
					|| !transfer.getPatient().getPatientId().equals(patientId)) {
				throw new APIException("Transfer does not belong to this patient");
			}
			previousReceivingFacilityCode = transfer.getReceivingFacilityCode();
			previousTransportType = transfer.getTransportType();
		}
		else {
			transfer = new Transfer();
			transfer.setUuid(UUID.randomUUID().toString());
			transfer.setPatient(patient);
			transfer.setCreator(Context.getAuthenticatedUser());
			transfer.setDateCreated(new Date());
			transfer.setVoided(false);
			transfer.setHieSent(false);
			transfer.setReceivedFromHie(false);
			transfer.setFormKind(TransferFormKind.GENERAL);
		}

		// This wizard always creates/updates the external (GENERAL) transfer form.
		if (transfer.getFormKind() == null) {
			transfer.setFormKind(TransferFormKind.GENERAL);
		}

		transfer.setDecisionToTransferAt(parseDateTimeLocal(decisionToTransferAt));
		if (transfer.getDecisionToTransferAt() == null) {
			throw new APIException("Date and time of decision to transfer is required");
		}
		transfer.setCallingTime(StringUtils.trimToNull(callingTime));
		transfer.setReceivingFacilityCode(StringUtils.trimToNull(receivingFacilityCode));
		applyReceivingFacilitySnapshot(transfer, receivingFacilityCode, receivingFacilityId);
		transfer.setReceivingService(StringUtils.trimToNull(receivingService));
		transfer.setStaffContactedName(StringUtils.trimToNull(staffContactedName));
		transfer.setStaffContactedPhone(StringUtils.trimToNull(staffContactedPhone));
		transfer.setTransferType(normalizedTransferType);
		if (TRANSFER_TYPE_EMERGENCY.equals(transfer.getTransferType())) {
			transfer.setAmbulanceCallTime(StringUtils.trimToNull(ambulanceCalledTime));
			transfer.setDepartRefTime(StringUtils.trimToNull(departureFromReferringTime));
		}
		else {
			transfer.setAmbulanceCallTime(null);
			transfer.setDepartRefTime(null);
		}
		applyTransportationSnapshot(transfer, normalizedTransferType, transportationType, transportationOtherSpec,
				ambulanceProviderFosaId, ambulanceProviderName);
		transfer.setReasonForTransfer(StringUtils.trimToNull(reasonForTransfer));
		if (StringUtils.isBlank(transfer.getReasonForTransfer())) {
			throw new APIException("Reason for Transfer is required");
		}

		String patientUpid = patientSnapshotResolver.resolveUpid(patient);
		if (!isUpdate) {
			if (StringUtils.isBlank(patientUpid)) {
				throw new APIException(
						"Patient UPID is required to create a transfer. Register a UPID on the patient chart first.");
			}
			applyHealthInsuranceSnapshot(transfer, patient);
			patientSnapshotResolver.applyPatientSnapshot(transfer, patient, transferDao);

			PersonAddress personAddress = transferDao.getPreferredPersonAddress(patient.getPatientId());
			if (personAddress == null) {
				personAddress = patientSnapshotResolver.resolveActivePersonAddress(patient);
			}
			patientSnapshotResolver.applyPersonAddressSnapshot(transfer, personAddress);
		}
		else {
			// Keep EMR/UPID snapshot current so HIE resubmit payloads include UPID.
			patientSnapshotResolver.ensureEmrIdFromPatient(transfer, patient);
			if (StringUtils.isBlank(transfer.getEmrId())) {
				throw new APIException(
						"Patient UPID is required to update this transfer. Register a UPID on the patient chart first.");
			}
		}

		applyFormExtras(transfer, formExtras, isUpdate);
		validateRequiredClinicalFields(transfer);
		ensureSendingFacilityMatchesOutbound(transfer);

		Date now = new Date();
		if (isUpdate) {
			transfer.setChangedBy(Context.getAuthenticatedUser());
			transfer.setDateChanged(now);
			// Local correction must be submitted to HIE again. Keep hieTransferId so the
			// resubmit updates the same Encounter and can preserve insurance-agent decisions.
			transfer.setHieSent(false);
			transfer.setHieSentAt(null);
			transfer.setHieSendError(null);
		}

		Transfer savedTransfer = transferDao.saveTransfer(transfer);
		if (!isUpdate) {
			markActiveQueueEntryTransferred(patient, savedTransfer, now);
		}
		// Ambulance billing must not run inside this transaction: mohbilling can mark the
		// shared TX rollback-only, and TransferAmbulanceBillingServiceImpl currently
		// swallows those errors → UnexpectedRollbackException on commit.
		scheduleAmbulanceBillSyncAfterCommit(savedTransfer, previousReceivingFacilityCode, previousTransportType);
		return savedTransfer;
	}

	/**
	 * Runs ambulance bill create/update/delete only after the transfer save has committed,
	 * in a separate service transaction.
	 */
	private void scheduleAmbulanceBillSyncAfterCommit(final Transfer savedTransfer,
			final String previousReceivingFacilityCode, final String previousTransportType) {
		if (transferAmbulanceBillingService == null || savedTransfer == null
				|| StringUtils.isBlank(savedTransfer.getUuid())) {
			return;
		}
		final String transferUuid = savedTransfer.getUuid();
		Runnable sync = new Runnable() {
			@Override
			public void run() {
				try {
					Transfer latest = transferDao.getTransferByUuid(transferUuid);
					if (latest == null || latest.isVoided()) {
						return;
					}
					transferAmbulanceBillingService.syncAmbulanceBill(
							latest, previousReceivingFacilityCode, previousTransportType);
				}
				catch (Exception ex) {
					log.warn("Ambulance bill sync failed after saving transfer " + transferUuid + ": "
							+ StringUtils.defaultString(ex.getMessage()), ex);
				}
			}
		};
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
				@Override
				public void afterCommit() {
					sync.run();
				}
			});
		}
		else {
			sync.run();
		}
	}

	protected void markActiveQueueEntryTransferred(Patient patient, Transfer transfer, Date transferDate) {
		QueueEntry queueEntry = queueService.getActiveQueueEntry(patient, transferDate);
		if (queueEntry != null) {
			queueService.markPatientTransferred(queueEntry, "External transfer created: " + transfer.getUuid());
		}
	}

	private void applyFormExtras(Transfer transfer, TransferFormExtras formExtras, boolean replaceClinicalFields) {
		if (formExtras != null) {
			if (replaceClinicalFields || StringUtils.isNotBlank(formExtras.getClinicalPresentation())) {
				transfer.setClinicalPresentation(StringUtils.trimToNull(formExtras.getClinicalPresentation()));
			}
			transfer.setDisabilityType(StringUtils.trimToNull(formExtras.getDisabilityType()));
			transfer.setLaboratory(StringUtils.trimToNull(formExtras.getLaboratory()));
			transfer.setProceduresTreatments(StringUtils.trimToNull(formExtras.getProceduresTreatments()));
			transfer.setOtherNotes(StringUtils.trimToNull(formExtras.getOtherNotes()));
			if (replaceClinicalFields || StringUtils.isNotBlank(formExtras.getDiagnosis())) {
				transfer.setDiagnosis(StringUtils.trimToNull(formExtras.getDiagnosis()));
			}
			if (StringUtils.isNotBlank(formExtras.getProviderQualification())) {
				transfer.setProviderQualification(StringUtils.trimToNull(formExtras.getProviderQualification()));
			}
			transfer.setSignedDate(parseDateValue(formExtras.getSignedDate()));
			transfer.setSignedTime(StringUtils.trimToNull(formExtras.getSignedTime()));
		}

		applyProviderProfileDetails(transfer);
		applyCaregiverFromFormOrPatient(transfer, formExtras, replaceClinicalFields);

		if (transfer.getSignedDate() == null) {
			transfer.setSignedDate(transfer.getDateCreated());
		}
		if (StringUtils.isBlank(transfer.getSignedTime()) && transfer.getDateCreated() != null) {
			transfer.setSignedTime(new SimpleDateFormat(TIME_PATTERN).format(transfer.getDateCreated()));
		}
	}

	private void validateRequiredClinicalFields(Transfer transfer) {
		if (StringUtils.isBlank(transfer.getClinicalPresentation())) {
			throw new APIException("Clinical Presentation is required");
		}
		if (StringUtils.isBlank(transfer.getDiagnosis())) {
			throw new APIException("Diagnosis is required");
		}
	}

	/**
	 * Ensures local {@code sendingFacility} matches {@code transferapp.outboundFacilityName}.
	 * On edit of older records this rewrites alias/session names to the normalized outbound value.
	 */
	private void ensureSendingFacilityMatchesOutbound(Transfer transfer) {
		if (transfer == null || transferAdminService == null) {
			return;
		}
		if (!transferAdminService.isOutboundFacilityNameConfigured()) {
			throw new APIException("Outbound facility name is not configured (transferapp.outboundFacilityName)");
		}
		String configuredName = StringUtils.trimToNull(transferAdminService.resolveOutboundFacilityName());
		if (configuredName == null) {
			throw new APIException("Outbound facility name is not configured (transferapp.outboundFacilityName)");
		}
		if (!configuredName.equals(StringUtils.trimToEmpty(transfer.getSendingFacility()))) {
			transfer.setSendingFacility(configuredName);
		}
	}

	/**
	 * Caregiver is the person helping the patient — never the referring clinician.
	 * Form values win when provided; otherwise patient person-attributes are used on create.
	 */
	private void applyCaregiverFromFormOrPatient(Transfer transfer, TransferFormExtras formExtras,
			boolean replaceFromForm) {
		if (transfer == null) {
			return;
		}
		if (formExtras != null) {
			if (replaceFromForm) {
				transfer.setCaregiverName(StringUtils.trimToNull(formExtras.getCaregiverName()));
				transfer.setCaregiverTelephone(StringUtils.trimToNull(formExtras.getCaregiverTelephone()));
			}
			else {
				if (StringUtils.isNotBlank(formExtras.getCaregiverName())) {
					transfer.setCaregiverName(StringUtils.trimToNull(formExtras.getCaregiverName()));
				}
				if (StringUtils.isNotBlank(formExtras.getCaregiverTelephone())) {
					transfer.setCaregiverTelephone(StringUtils.trimToNull(formExtras.getCaregiverTelephone()));
				}
			}
		}
		if (StringUtils.isBlank(transfer.getCaregiverName()) && transfer.getPatient() != null) {
			transfer.setCaregiverName(patientSnapshotResolver.resolveCaregiverName(transfer.getPatient()));
		}
		if (StringUtils.isBlank(transfer.getCaregiverTelephone()) && transfer.getPatient() != null) {
			transfer.setCaregiverTelephone(patientSnapshotResolver.resolveCaregiverTelephone(transfer.getPatient()));
		}
	}

	private void applyProviderProfileDetails(Transfer transfer) {
		if (transfer == null || transferProfileService == null) {
			return;
		}
		User user = Context.getAuthenticatedUser();
		if (user == null) {
			return;
		}
		TransferProfile profile = transferProfileService.getProfileForUser(user);
		if (profile == null) {
			throw new APIException(
					"Please complete My Profile with phone number, qualification, and speciality before creating a transfer");
		}
		if (!profile.isCompleteForTransfer()) {
			throw new APIException(
					"Please complete My Profile with phone number, qualification, and speciality before creating a transfer");
		}
		transfer.setProviderQualification(StringUtils.trimToNull(profile.getQualificationWithSpeciality()));
		transfer.setProviderPhone(StringUtils.trimToNull(profile.getPhoneNumber()));
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

	protected Date parseDateValue(String value) {
		if (StringUtils.isBlank(value)) {
			return null;
		}
		String trimmed = value.trim();
		for (String pattern : new String[] { DATE_PATTERN, "dd.MMM.yyyy", DATETIME_LOCAL_PATTERN, DATETIME_SPACE_PATTERN }) {
			try {
				return new SimpleDateFormat(pattern).parse(trimmed);
			}
			catch (ParseException ignored) {
				// try next pattern
			}
		}
		return null;
	}

	private void applyHealthInsuranceSnapshot(Transfer transfer, Patient patient) {
		if (patientInsuranceService == null) {
			return;
		}

		PatientInsuranceInfo insurance = patientInsuranceService.getPatientInsurance(patient);
		if (!insurance.isAvailable()) {
			throw new APIException("Patient insurance type and number are required before creating a transfer");
		}

		String category = StringUtils.trimToNull(insurance.getHealthInsuranceCategory());
		if (category == null) {
			category = TransferAppConstants.HEALTH_INSURANCE_OTHER;
		}
		transfer.setHealthInsuranceType(category);
		if (TransferAppConstants.HEALTH_INSURANCE_OTHER.equals(category)) {
			String otherSpec = StringUtils.trimToNull(insurance.getHealthInsuranceOtherSpec());
			if (otherSpec == null) {
				otherSpec = insurance.getInsuranceType();
			}
			transfer.setHealthInsuranceOther(otherSpec);
		}
		else {
			transfer.setHealthInsuranceOther(null);
		}
	}

	private void ensureReceivingServiceConfigured(String receivingFacilityCode, Integer receivingFacilityId,
			String receivingService) {
		if (transferAdminService == null || StringUtils.isBlank(receivingService)) {
			return;
		}
		Integer resolvedFacilityId = receivingFacilityId;
		if (resolvedFacilityId == null && StringUtils.isNotBlank(receivingFacilityCode)) {
			Integer sendingLocationId = transferAdminService.resolveCurrentSendingLocationId();
			if (sendingLocationId != null) {
				ReceivingFacility facility = transferAdminService
						.getReceivingFacilityByCode(sendingLocationId, receivingFacilityCode);
				if (facility != null) {
					resolvedFacilityId = facility.getReceivingFacilityId();
				}
			}
		}
		if (resolvedFacilityId != null) {
			transferAdminService.ensureReceivingServiceForFacility(resolvedFacilityId, receivingService);
		}
	}

	private void applyReceivingFacilitySnapshot(Transfer transfer, String receivingFacilityCode,
			Integer receivingFacilityId) {
		if (transferAdminService == null) {
			return;
		}

		ReceivingFacility facility = null;
		if (receivingFacilityId != null) {
			facility = transferAdminService.getReceivingFacility(receivingFacilityId);
		}
		if (facility == null && StringUtils.isNotBlank(receivingFacilityCode)) {
			Integer sendingLocationId = transferAdminService.resolveCurrentSendingLocationId();
			if (sendingLocationId != null) {
				facility = transferAdminService.getReceivingFacilityByCode(sendingLocationId, receivingFacilityCode);
			}
		}
		if (facility != null) {
			transfer.setReceivingProvince(StringUtils.trimToNull(facility.getProvince()));
			transfer.setReceivingDistrict(StringUtils.trimToNull(facility.getDistrict()));
		}
	}

	private void validateTransferTypeFields(String transferType, String ambulanceCalledTime,
			String departureFromReferringTime) {
		String type = StringUtils.trimToNull(transferType);
		if (type == null) {
			throw new APIException("Type of transfer is required");
		}
		if (!Arrays.asList("EMERGENCY", "NOT_EMERGENCY", "FOLLOW_UP").contains(type)) {
			throw new APIException("Invalid type of transfer");
		}
		if (TRANSFER_TYPE_EMERGENCY.equals(type)) {
			if (StringUtils.isBlank(ambulanceCalledTime)) {
				throw new APIException("Time ambulance called is required for emergency transfers");
			}
			if (StringUtils.isBlank(departureFromReferringTime)) {
				throw new APIException("Time of departure from referring facility is required for emergency transfers");
			}
		}
	}

	private void validateTransportationFields(String transferType, String transportationType,
			String transportationOtherSpec, String ambulanceProviderFosaId, String ambulanceProviderName) {
		boolean emergency = TRANSFER_TYPE_EMERGENCY.equals(transferType);
		String transport = emergency ? TRANSPORT_TYPE_AMBULANCE : StringUtils.trimToNull(transportationType);
		if (!emergency) {
			if (transport == null) {
				throw new APIException("Type of transportation is required");
			}
			if (TRANSPORT_TYPE_AMBULANCE.equals(transport)) {
				throw new APIException("Ambulance transportation is only allowed for emergency transfers");
			}
			if (!Arrays.asList("OTHER", "NA").contains(transport)) {
				throw new APIException("Invalid type of transportation");
			}
			if ("OTHER".equals(transport) && StringUtils.isBlank(transportationOtherSpec)) {
				throw new APIException("Please specify other transportation type");
			}
		}
		if (TRANSPORT_TYPE_AMBULANCE.equals(transport)) {
			if (StringUtils.isBlank(ambulanceProviderFosaId) || StringUtils.isBlank(ambulanceProviderName)) {
				throw new APIException("Select the facility that will provide the ambulance vehicle");
			}
		}
	}

	private void applyTransportationSnapshot(Transfer transfer, String transferType, String transportationType,
			String transportationOtherSpec, String ambulanceProviderFosaId, String ambulanceProviderName) {
		if (TRANSFER_TYPE_EMERGENCY.equals(transferType)) {
			transfer.setTransportType(TRANSPORT_TYPE_AMBULANCE);
			transfer.setTransportOther(null);
			String fosaId = StringUtils.trimToNull(ambulanceProviderFosaId);
			transfer.setAmbulanceProviderFosaId(fosaId);
			transfer.setAmbulanceProviderName(resolveAmbulanceProviderName(fosaId, ambulanceProviderName));
			return;
		}
		String transport = StringUtils.trimToNull(transportationType);
		transfer.setTransportType(transport);
		if ("OTHER".equals(transport)) {
			transfer.setTransportOther(StringUtils.trimToNull(transportationOtherSpec));
		}
		else {
			transfer.setTransportOther(null);
		}
		transfer.setAmbulanceProviderFosaId(null);
		transfer.setAmbulanceProviderName(null);
	}

	/**
	 * Prefer the Facility Registry display name for the selected FOSA id so a stale
	 * hidden-field value (e.g. current facility) cannot be persisted with the wrong code.
	 */
	private String resolveAmbulanceProviderName(String ambulanceProviderFosaId, String submittedName) {
		String fosaId = StringUtils.trimToNull(ambulanceProviderFosaId);
		if (fosaId == null) {
			return null;
		}
		String fromRegistry = findAmbulanceProviderNameInRegistry(fosaId);
		if (StringUtils.isNotBlank(fromRegistry)) {
			return fromRegistry.trim();
		}
		return StringUtils.trimToNull(submittedName);
	}

	private String findAmbulanceProviderNameInRegistry(String fosaId) {
		if (transferFacilityRegistryService == null || StringUtils.isBlank(fosaId)) {
			return null;
		}
		try {
			List<RegistryFacility> facilities = transferFacilityRegistryService.listAmbulanceProviderFacilitiesFromHie();
			if (facilities == null || facilities.isEmpty()) {
				return null;
			}
			String normalizedTarget = normalizeFosaCode(fosaId);
			for (RegistryFacility facility : facilities) {
				if (facility == null || StringUtils.isBlank(facility.getCode())) {
					continue;
				}
				if (fosaId.equals(facility.getCode())
						|| normalizedTarget.equals(normalizeFosaCode(facility.getCode()))) {
					return StringUtils.trimToNull(facility.getName());
				}
			}
		}
		catch (Exception ignored) {
			// Fall back to submitted name when registry lookup fails.
		}
		return null;
	}

	private static String normalizeFosaCode(String code) {
		String value = StringUtils.trimToEmpty(code);
		if (StringUtils.isNumeric(value) && value.length() < 4) {
			while (value.length() < 4) {
				value = "0" + value;
			}
		}
		return value;
	}

	@Override
	public List<Transfer> getTransfersByPatient(Patient patient) {
		return transferDao.getTransfersByPatient(patient);
	}

	@Override
	public List<Transfer> getTransfersByPatient(Patient patient, Integer limit) {
		return transferDao.getTransfersByPatient(patient, limit);
	}

	@Override
	public int countTransfersByPatient(Patient patient) {
		return transferDao.countTransfersByPatient(patient);
	}

	@Override
	public Transfer getTransferByUuid(String uuid) {
		if (StringUtils.isBlank(uuid)) {
			return null;
		}
		return transferDao.getTransferByUuid(uuid.trim());
	}

	protected Date parseDateTimeLocal(String value) {
		if (StringUtils.isBlank(value)) {
			return null;
		}
		String trimmed = value.trim();
		for (String pattern : new String[] { DATETIME_LOCAL_PATTERN, DATETIME_SPACE_PATTERN }) {
			try {
				return new SimpleDateFormat(pattern).parse(trimmed);
			}
			catch (ParseException ignored) {
				// try next pattern
			}
		}
		throw new APIException("Invalid decision date and time: " + value);
	}

}
