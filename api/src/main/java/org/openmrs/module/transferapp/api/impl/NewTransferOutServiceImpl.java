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
import org.openmrs.Location;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.PersonAddress;
import org.openmrs.User;
import org.openmrs.api.APIException;
import org.openmrs.api.context.Context;
import org.openmrs.module.transferapp.TransferAppConstants;
import org.openmrs.module.transferapp.api.NewTransferOutService;
import org.openmrs.module.transferapp.api.TransferAdminService;
import org.openmrs.module.transferapp.api.TransferPatientSnapshotResolver;
import org.openmrs.module.transferapp.api.TransferProfileService;
import org.openmrs.module.transferapp.api.dao.TransferDao;
import org.openmrs.module.transferapp.model.NewTransferOutFormData;
import org.openmrs.module.transferapp.model.ReceivingFacility;
import org.openmrs.module.transferapp.model.Transfer;
import org.openmrs.module.transferapp.model.TransferFormOption;
import org.openmrs.module.transferapp.model.TransferProfile;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Builds MOH External Transfer Form wizard data with OpenMRS patient prefill.
 */
public class NewTransferOutServiceImpl implements NewTransferOutService {

	private static final String DATETIME_LOCAL_PATTERN = "yyyy-MM-dd'T'HH:mm";

	private static final String DATETIME_SPACE_PATTERN = "yyyy-MM-dd HH:mm";

	private static final String DATE_PATTERN = "yyyy-MM-dd";

	private static final String TIME_PATTERN = "HH:mm";

	private final TransferPatientSnapshotResolver patientSnapshotResolver = new TransferPatientSnapshotResolver();

	private TransferDao transferDao;

	private TransferAdminService transferAdminService;

	private TransferProfileService transferProfileService;

	public void setTransferDao(TransferDao transferDao) {
		this.transferDao = transferDao;
	}

	public void setTransferAdminService(TransferAdminService transferAdminService) {
		this.transferAdminService = transferAdminService;
	}

	public void setTransferProfileService(TransferProfileService transferProfileService) {
		this.transferProfileService = transferProfileService;
	}

	@Override
	public NewTransferOutFormData getNewTransferOutFormData(Patient patient) {
		return getNewTransferOutFormData(patient, null);
	}

	@Override
	public NewTransferOutFormData getNewTransferOutFormData(Patient patient, String transferUuid) {
		NewTransferOutFormData formData = new NewTransferOutFormData();
		formData.setPatientId(patient.getPatientId());
		formData.setPatientDisplay(patient.getPersonName() != null ? patient.getPersonName().getFullName() : "");
		formData.setSendingFacility(getCurrentFacilityName());

		formData.setIdentifierTypes(getIdentifierTypes());
		formData.setReceivingFacilities(getReceivingFacilities());
		formData.setReceivingServices(Collections.<String>emptyList());
		formData.setTransferTypes(getTransferTypes());
		formData.setTransportationTypes(getTransportationTypes());
		formData.setHealthInsuranceTypes(getHealthInsuranceTypes());
		formData.setCurrentSendingFosaId(StringUtils.trimToEmpty(
				Context.getAdministrationService().getGlobalProperty(
						TransferAppConstants.GP_SENDING_FOSA_ID,
						TransferAppConstants.DEFAULT_SENDING_FOSA_ID)));

		prefillFromPatient(formData, patient);
		prefillFromCurrentUser(formData);
		prefillDefaults(formData);

		if (StringUtils.isNotBlank(transferUuid)) {
			prefillFromExistingTransfer(formData, patient, transferUuid.trim());
		}

		return formData;
	}

	protected void prefillFromExistingTransfer(NewTransferOutFormData formData, Patient patient, String transferUuid) {
		if (transferDao == null) {
			throw new APIException("Unable to load transfer for editing");
		}
		Transfer transfer = transferDao.getTransferByUuid(transferUuid);
		if (transfer == null || transfer.isVoided()) {
			throw new APIException("Transfer not found");
		}
		if (transfer.getPatient() == null
				|| transfer.getPatient().getPatientId() == null
				|| !transfer.getPatient().getPatientId().equals(patient.getPatientId())) {
			throw new APIException("Transfer does not belong to this patient");
		}

		formData.setTransferUuid(transfer.getUuid());
		if (transfer.getDecisionToTransferAt() != null) {
			formData.setDecisionToTransferAt(formatDateTimeSpace(transfer.getDecisionToTransferAt()));
		}
		formData.setCallingTime(StringUtils.defaultString(transfer.getCallingTime()));
		formData.setReceivingFacilityCode(StringUtils.defaultString(transfer.getReceivingFacilityCode()));
		formData.setReceivingFacilityId(resolveReceivingFacilityId(transfer.getReceivingFacilityCode()));
		formData.setReceivingService(StringUtils.defaultString(transfer.getReceivingService()));
		formData.setStaffContactedName(StringUtils.defaultString(transfer.getStaffContactedName()));
		formData.setStaffContactedPhone(StringUtils.defaultString(transfer.getStaffContactedPhone()));
		formData.setTransferType(StringUtils.defaultString(transfer.getTransferType()));
		formData.setAmbulanceCalledTime(StringUtils.defaultString(transfer.getAmbulanceCallTime()));
		formData.setDepartureFromReferringTime(StringUtils.defaultString(transfer.getDepartRefTime()));
		formData.setReasonForTransfer(StringUtils.defaultString(transfer.getReasonForTransfer()));
		formData.setClinicalPresentation(StringUtils.defaultString(transfer.getClinicalPresentation()));
		formData.setDisabilityType(StringUtils.defaultString(transfer.getDisabilityType()));
		formData.setLaboratory(StringUtils.defaultString(transfer.getLaboratory()));
		formData.setOthersNotes(StringUtils.defaultString(transfer.getOtherNotes()));
		formData.setDiagnosis(StringUtils.defaultString(transfer.getDiagnosis()));
		formData.setProceduresAndTreatments(StringUtils.defaultString(transfer.getProceduresTreatments()));
		formData.setTransportationType(StringUtils.defaultString(transfer.getTransportType()));
		formData.setTransportationOtherSpec(StringUtils.defaultString(transfer.getTransportOther()));
		formData.setAmbulanceProviderFosaId(StringUtils.defaultString(transfer.getAmbulanceProviderFosaId()));
		formData.setAmbulanceProviderName(StringUtils.defaultString(transfer.getAmbulanceProviderName()));
		formData.setCaregiverName(StringUtils.defaultString(transfer.getCaregiverName()));
		formData.setCaregiverTelephone(StringUtils.defaultString(transfer.getCaregiverTelephone()));
		if (transfer.getSignedDate() != null) {
			formData.setReferringSignedDate(formatDate(transfer.getSignedDate()));
		}
		formData.setReferringSignedTime(StringUtils.defaultString(transfer.getSignedTime()));
		if (StringUtils.isNotBlank(transfer.getReferringProviderName())) {
			String license = null;
			if (transferProfileService != null) {
				User user = Context.getAuthenticatedUser();
				if (user != null) {
					TransferProfile profile = transferProfileService.getProfileForUser(user);
					if (profile != null) {
						license = profile.getLicenseNumber();
					}
				}
			}
			formData.setReferringProviderName(TransferProfile.formatCareProviderName(
					transfer.getReferringProviderName(), license));
		}
		if (StringUtils.isNotBlank(transfer.getProviderQualification())) {
			formData.setReferringProviderQualification(transfer.getProviderQualification());
		}
		if (StringUtils.isNotBlank(transfer.getProviderPhone())) {
			formData.setReferringProviderPhone(transfer.getProviderPhone());
		}

		// Prefer live patient UPID so edit/resubmit can rebuild HIE payloads correctly.
		String upid = patientSnapshotResolver.resolveUpid(patient);
		if (StringUtils.isNotBlank(upid)) {
			formData.setSerialNumberEmr(upid);
		}
		else if (StringUtils.isNotBlank(transfer.getEmrId())) {
			formData.setSerialNumberEmr(transfer.getEmrId());
		}
	}

	protected Integer resolveReceivingFacilityId(String facilityCode) {
		if (StringUtils.isBlank(facilityCode)) {
			return null;
		}
		for (TransferFormOption option : getReceivingFacilities()) {
			if (facilityCode.equals(option.getValue())) {
				return option.getReceivingFacilityId();
			}
		}
		return null;
	}

	protected String formatDateTimeSpace(Date date) {
		return new SimpleDateFormat(DATETIME_SPACE_PATTERN).format(date);
	}

	protected void prefillFromPatient(NewTransferOutFormData formData, Patient patient) {
		if (patient.getPersonName() != null) {
			formData.setClientName(patient.getPersonName().getFullName());
		}

		String upid = patientSnapshotResolver.resolveUpid(patient);
		if (upid != null) {
			formData.setSerialNumberEmr(upid);
		}

		PatientIdentifier nationalId = patientSnapshotResolver.resolveNationalIdentifier(patient);
		if (nationalId != null && nationalId.getIdentifierType() != null) {
			formData.setIdentifierType(nationalId.getIdentifierType().getName());
			formData.setIdentifierValue(nationalId.getIdentifier());
		}

		formData.setClientTelephone(patientSnapshotResolver.resolvePatientPhone(patient, transferDao));
		formData.setAgeOrDob(patientSnapshotResolver.resolveAgeOrDob(patient));
		formData.setSex(patientSnapshotResolver.mapGender(patient.getGender()));
		formData.setCaregiverName(patientSnapshotResolver.resolveCaregiverName(patient));
		formData.setCaregiverTelephone(patientSnapshotResolver.resolveCaregiverTelephone(patient));

		PersonAddress address = null;
		if (transferDao != null) {
			address = transferDao.getPreferredPersonAddress(patient.getPatientId());
		}
		if (address == null) {
			address = patientSnapshotResolver.resolveActivePersonAddress(patient);
		}
		if (address != null) {
			formData.setClientDistrict(patientSnapshotResolver.resolveDistrict(address));
			formData.setSector(patientSnapshotResolver.resolveSector(address));
			formData.setCell(patientSnapshotResolver.resolveCell(address));
			formData.setVillage(patientSnapshotResolver.resolveVillage(address));
		}

		formData.setClinicalPresentation(safeResolveClinicalPresentation(patient));
		formData.setDiagnosis(safeResolveDiagnosis(patient));
	}

	private String safeResolveClinicalPresentation(Patient patient) {
		try {
			return patientSnapshotResolver.resolveClinicalPresentation(patient);
		}
		catch (Exception ex) {
			return null;
		}
	}

	private String safeResolveDiagnosis(Patient patient) {
		try {
			return patientSnapshotResolver.resolveDiagnosis(patient);
		}
		catch (Exception ex) {
			return null;
		}
	}

	protected void prefillFromCurrentUser(NewTransferOutFormData formData) {
		User user = Context.getAuthenticatedUser();
		if (user == null) {
			return;
		}
		String userName = null;
		if (user.getPerson() != null && user.getPerson().getPersonName() != null) {
			userName = StringUtils.trimToNull(user.getPerson().getPersonName().getFullName());
		}
		if (userName == null) {
			userName = StringUtils.trimToNull(user.getUsername());
		}
		if (transferProfileService != null) {
			TransferProfile profile = transferProfileService.getProfileForUser(user);
			if (profile != null) {
				formData.setReferringProviderName(TransferProfile.formatCareProviderName(
						userName, profile.getLicenseNumber()));
				if (StringUtils.isNotBlank(profile.getPhoneNumber())) {
					formData.setReferringProviderPhone(StringUtils.trimToNull(profile.getPhoneNumber()));
				}
			}
		}
		if (StringUtils.isBlank(formData.getReferringProviderName()) && userName != null) {
			formData.setReferringProviderName(userName);
		}
	}

	protected void prefillDefaults(NewTransferOutFormData formData) {
		Date now = new Date();
		formData.setAdmissionAt(formatDateTimeLocal(now));
		formData.setDecisionToTransferAt(formatDateTimeSpace(now));
		formData.setCallingTime(formatTime(now));
		formData.setReferringSignedDate(formatDate(now));
		formData.setReferringSignedTime(formatTime(now));
	}

	protected String getCurrentFacilityName() {
		if (transferAdminService != null) {
			String configuredName = transferAdminService.resolveOutboundFacilityName();
			if (StringUtils.isNotBlank(configuredName)) {
				return configuredName.trim();
			}
		}
		return "";
	}

	protected List<TransferFormOption> getIdentifierTypes() {
		return Arrays.asList(
				new TransferFormOption("NID", "National ID (NID)"),
				new TransferFormOption("NIDA_APPLICATION_NUMBER", "NIDA application number"),
				new TransferFormOption("NIN", "NIN"),
				new TransferFormOption("UPID", "UPID"));
	}

	protected List<TransferFormOption> getReceivingFacilities() {
		if (transferAdminService != null) {
			Integer sendingLocationId = transferAdminService.resolveCurrentSendingLocationId();
			if (sendingLocationId != null) {
				List<ReceivingFacility> facilities = transferAdminService.getReceivingFacilities(sendingLocationId);
				if (!facilities.isEmpty()) {
					List<TransferFormOption> options = new ArrayList<TransferFormOption>();
					for (ReceivingFacility facility : facilities) {
						options.add(new TransferFormOption(
								facility.getFacilityCode(),
								facility.getFacilityName(),
								facility.getReceivingFacilityId()));
					}
					return options;
				}
			}
		}
		return getDefaultReceivingFacilities();
	}

	protected List<TransferFormOption> getDefaultReceivingFacilities() {
		return Arrays.asList(
				new TransferFormOption("KUTH", "Kigali University Teaching Hospital"),
				new TransferFormOption("RUHENGERI", "Ruhengeri District Hospital"),
				new TransferFormOption("BUTARO", "Butaro District Hospital"),
				new TransferFormOption("KFH", "King Faisal Hospital"));
	}

	protected List<TransferFormOption> getTransferTypes() {
		return Arrays.asList(
				new TransferFormOption("EMERGENCY", "Emergency"),
				new TransferFormOption("NOT_EMERGENCY", "Not-Emergency"),
				new TransferFormOption("FOLLOW_UP", "Follow up"));
	}

	protected List<TransferFormOption> getTransportationTypes() {
		return Arrays.asList(
				new TransferFormOption("AMBULANCE", "Ambulance"),
				new TransferFormOption("OTHER", "Other (specify)"),
				new TransferFormOption("NA", "NA"));
	}

	protected List<TransferFormOption> getHealthInsuranceTypes() {
		return Arrays.asList(
				new TransferFormOption("CBHI", "CBHI (mutuelle)"),
				new TransferFormOption("RSSB", "RSSB"),
				new TransferFormOption("MMI", "MMI"),
				new TransferFormOption("OTHER", "Other (specify)"),
				new TransferFormOption("NONE", "None"));
	}

	protected String mapGender(String gender) {
		if (gender == null) {
			return "";
		}
		if ("M".equalsIgnoreCase(gender)) {
			return "MALE";
		}
		if ("F".equalsIgnoreCase(gender)) {
			return "FEMALE";
		}
		return "OTHER";
	}

	protected String formatDateTimeLocal(Date date) {
		return new SimpleDateFormat(DATETIME_LOCAL_PATTERN).format(date);
	}

	protected String formatDate(Date date) {
		return new SimpleDateFormat(DATE_PATTERN).format(date);
	}

	protected String formatTime(Date date) {
		return new SimpleDateFormat(TIME_PATTERN).format(date);
	}

}
