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
import org.openmrs.module.transferapp.api.NewMaternityTransferOutService;
import org.openmrs.module.transferapp.api.TransferAdminService;
import org.openmrs.module.transferapp.api.TransferPatientSnapshotResolver;
import org.openmrs.module.transferapp.api.dao.MaternityTransferDao;
import org.openmrs.module.transferapp.api.dao.TransferDao;
import org.openmrs.module.transferapp.model.MaternityTransfer;
import org.openmrs.module.transferapp.model.MaternityTransferFormData;
import org.openmrs.module.transferapp.model.MaternityTransferTreatment;
import org.openmrs.module.transferapp.model.MaternityTransferTreatmentRow;
import org.openmrs.module.transferapp.model.ReceivingFacility;
import org.openmrs.module.transferapp.model.TransferFormOption;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Builds MOH Maternity/ANC-Delivery-PNC Transfer Form wizard data with OpenMRS patient prefill.
 */
public class NewMaternityTransferOutServiceImpl implements NewMaternityTransferOutService {

	private static final String DATETIME_LOCAL_PATTERN = "yyyy-MM-dd'T'HH:mm";

	private static final String DATE_PATTERN = "yyyy-MM-dd";

	private static final String TIME_PATTERN = "HH:mm";

	private final TransferPatientSnapshotResolver patientSnapshotResolver = new TransferPatientSnapshotResolver();

	private TransferDao transferDao;

	private MaternityTransferDao maternityTransferDao;

	private TransferAdminService transferAdminService;

	public void setTransferDao(TransferDao transferDao) {
		this.transferDao = transferDao;
	}

	public void setMaternityTransferDao(MaternityTransferDao maternityTransferDao) {
		this.maternityTransferDao = maternityTransferDao;
	}

	public void setTransferAdminService(TransferAdminService transferAdminService) {
		this.transferAdminService = transferAdminService;
	}

	@Override
	public MaternityTransferFormData getMaternityTransferFormData(Patient patient) {
		return getMaternityTransferFormData(patient, null);
	}

	@Override
	public MaternityTransferFormData getMaternityTransferFormData(Patient patient, String transferUuid) {
		MaternityTransferFormData formData = new MaternityTransferFormData();
		formData.setPatientId(patient.getPatientId());
		formData.setPatientDisplay(patient.getPersonName() != null ? patient.getPersonName().getFullName() : "");
		formData.setSendingFacility(getCurrentFacilityName());
		formData.setHospitalName(getCurrentFacilityName());
		formData.setReferringFacilityName(getCurrentFacilityName());

		formData.setReceivingFacilities(getReceivingFacilities());
		formData.setReceivingServices(Collections.<String>emptyList());
		formData.setTransferTypes(getTransferTypes());
		formData.setTransportationTypes(getTransportationTypes());
		formData.setHealthInsuranceTypes(getHealthInsuranceTypes());
		formData.setDefaultTreatmentRows(getDefaultTreatmentRows());

		prefillFromPatient(formData, patient);
		prefillFromCurrentUser(formData);
		prefillDefaults(formData);

		if (StringUtils.isNotBlank(transferUuid)) {
			prefillFromExistingTransfer(formData, patient, transferUuid.trim());
		}

		return formData;
	}

	protected void prefillFromExistingTransfer(MaternityTransferFormData formData, Patient patient, String transferUuid) {
		if (maternityTransferDao == null) {
			throw new APIException("Unable to load maternity transfer for editing");
		}
		MaternityTransfer transfer = maternityTransferDao.getMaternityTransferByUuid(transferUuid);
		if (transfer == null || transfer.isVoided()) {
			throw new APIException("Transfer not found");
		}
		if (transfer.getPatient() == null
				|| transfer.getPatient().getPatientId() == null
				|| !transfer.getPatient().getPatientId().equals(patient.getPatientId())) {
			throw new APIException("Transfer does not belong to this patient");
		}

		formData.setTransferUuid(transfer.getUuid());
		formData.setProvince(StringUtils.defaultString(transfer.getProvince()));
		formData.setDistrict(StringUtils.defaultString(transfer.getDistrict()));
		formData.setHospitalName(StringUtils.defaultString(transfer.getHospitalName()));
		formData.setReferringFacilityName(StringUtils.defaultString(transfer.getReferringFacilityName()));
		formData.setReferringUnit(StringUtils.defaultString(transfer.getReferringUnit()));

		formData.setClientName(StringUtils.defaultString(transfer.getClientName()));
		formData.setSerialNumberEmr(StringUtils.defaultString(transfer.getSerialNumberEmr()));
		formData.setAgeOrDob(StringUtils.defaultString(transfer.getAgeOrDob()));
		formData.setNextOfKinName(StringUtils.defaultString(transfer.getNextOfKinName()));
		formData.setNextOfKinTelephone(StringUtils.defaultString(transfer.getNextOfKinTelephone()));
		formData.setClientDistrict(StringUtils.defaultString(transfer.getClientDistrict()));
		formData.setSector(StringUtils.defaultString(transfer.getSector()));
		formData.setCell(StringUtils.defaultString(transfer.getCell()));
		formData.setVillage(StringUtils.defaultString(transfer.getVillage()));
		if (transfer.getAdmissionAt() != null) {
			formData.setAdmissionAt(formatDateTimeSpace(transfer.getAdmissionAt()));
		}
		if (transfer.getDecisionToTransferAt() != null) {
			formData.setDecisionToTransferAt(formatDateTimeSpace(transfer.getDecisionToTransferAt()));
		}
		formData.setReceivingFacilityCode(StringUtils.defaultString(transfer.getReceivingFacilityCode()));
		formData.setReceivingService(StringUtils.defaultString(transfer.getReceivingService()));
		formData.setCallingTime(StringUtils.defaultString(transfer.getCallingTime()));
		formData.setStaffContactedName(StringUtils.defaultString(transfer.getStaffContactedName()));
		formData.setStaffContactedPhone(StringUtils.defaultString(transfer.getStaffContactedPhone()));
		formData.setReasonForTransfer(StringUtils.defaultString(transfer.getReasonForTransfer()));
		formData.setTransferType(StringUtils.defaultString(transfer.getTransferType()));
		formData.setAmbulanceCalledTime(StringUtils.defaultString(transfer.getAmbulanceCalledTime()));
		formData.setDepartureFromReferringTime(StringUtils.defaultString(transfer.getDepartureFromReferringTime()));
		formData.setPartographAttached(booleanToFormValue(transfer.getPartographAttached()));
		formData.setClinicalPresentation(StringUtils.defaultString(transfer.getClinicalPresentation()));
		formData.setDisabilityType(StringUtils.defaultString(transfer.getDisabilityType()));

		formData.setObstetricGravida(StringUtils.defaultString(transfer.getObstetricGravida()));
		formData.setObstetricParity(StringUtils.defaultString(transfer.getObstetricParity()));
		formData.setObstetricLivingChildren(StringUtils.defaultString(transfer.getObstetricLivingChildren()));
		formData.setObstetricAbortion(StringUtils.defaultString(transfer.getObstetricAbortion()));
		formData.setObstetricStillbirth(StringUtils.defaultString(transfer.getObstetricStillbirth()));
		formData.setObstetricNeonatalDeath(StringUtils.defaultString(transfer.getObstetricNeonatalDeath()));
		formData.setObstetricPretermBirth(StringUtils.defaultString(transfer.getObstetricPretermBirth()));
		if (transfer.getLmpDate() != null) {
			formData.setLmpDate(formatDate(transfer.getLmpDate()));
		}
		if (transfer.getEddDate() != null) {
			formData.setEddDate(formatDate(transfer.getEddDate()));
		}
		formData.setGestationAge(StringUtils.defaultString(transfer.getGestationAge()));
		formData.setMuac(StringUtils.defaultString(transfer.getMuac()));
		formData.setAncCompletedCount(StringUtils.defaultString(transfer.getAncCompletedCount()));
		formData.setTetanusVaccineDoses(StringUtils.defaultString(transfer.getTetanusVaccineDoses()));
		formData.setPreviousSignificantHistory(StringUtils.defaultString(transfer.getPreviousSignificantHistory()));
		formData.setMultiPregnanciesAndKnownHiv(StringUtils.defaultString(transfer.getMultiPregnanciesAndKnownHiv()));
		formData.setCurrentPregnancyComplications(StringUtils.defaultString(transfer.getCurrentPregnancyComplications()));

		formData.setLatestHemoglobin(StringUtils.defaultString(transfer.getLatestHemoglobin()));
		formData.setLatestHivStatus(StringUtils.defaultString(transfer.getLatestHivStatus()));
		formData.setLatestBloodGroup(StringUtils.defaultString(transfer.getLatestBloodGroup()));
		formData.setLatestOtherResults(StringUtils.defaultString(transfer.getLatestOtherResults()));
		formData.setVitalBp(StringUtils.defaultString(transfer.getVitalBp()));
		formData.setVitalTemp(StringUtils.defaultString(transfer.getVitalTemp()));
		formData.setVitalSpo2(StringUtils.defaultString(transfer.getVitalSpo2()));
		formData.setVitalRr(StringUtils.defaultString(transfer.getVitalRr()));
		formData.setVitalPulse(StringUtils.defaultString(transfer.getVitalPulse()));
		formData.setVitalWeight(StringUtils.defaultString(transfer.getVitalWeight()));
		formData.setVitalHeight(StringUtils.defaultString(transfer.getVitalHeight()));

		formData.setFetalPresentation(StringUtils.defaultString(transfer.getFetalPresentation()));
		formData.setFundalHeight(StringUtils.defaultString(transfer.getFundalHeight()));
		formData.setFetalHeartRate(StringUtils.defaultString(transfer.getFetalHeartRate()));
		formData.setContractions(StringUtils.defaultString(transfer.getContractions()));
		if (transfer.getVaginalExamAt() != null) {
			formData.setVaginalExamAt(formatDateTimeSpace(transfer.getVaginalExamAt()));
		}
		formData.setDilation(StringUtils.defaultString(transfer.getDilation()));
		formData.setEffacement(StringUtils.defaultString(transfer.getEffacement()));
		formData.setDescent(StringUtils.defaultString(transfer.getDescent()));
		formData.setConsistency(StringUtils.defaultString(transfer.getConsistency()));
		formData.setPosition(StringUtils.defaultString(transfer.getPosition()));
		formData.setCaput(booleanToFormValue(transfer.getCaput()));
		formData.setMoulding(booleanToFormValue(transfer.getMoulding()));
		formData.setMembranesRuptured(booleanToFormValue(transfer.getMembranesRuptured()));
		if (transfer.getMembranesRupturedAt() != null) {
			formData.setMembranesRupturedAt(formatDateTimeSpace(transfer.getMembranesRupturedAt()));
		}
		formData.setAmnioticFluidColor(StringUtils.defaultString(transfer.getAmnioticFluidColor()));
		formData.setEstimatedBloodLossMl(StringUtils.defaultString(transfer.getEstimatedBloodLossMl()));

		formData.setInvestigationHgb(StringUtils.defaultString(transfer.getInvestigationHgb()));
		formData.setInvestigationUrineTest(StringUtils.defaultString(transfer.getInvestigationUrineTest()));
		formData.setInvestigationOtherTest(StringUtils.defaultString(transfer.getInvestigationOtherTest()));
		formData.setImagingInvestigations(StringUtils.defaultString(transfer.getImagingInvestigations()));
		formData.setDiagnosis(StringUtils.defaultString(transfer.getDiagnosis()));
		formData.setProcedures(StringUtils.defaultString(transfer.getProcedures()));
		formData.setAttachedLabTests(booleanToFormValue(transfer.getAttachedLabTests()));
		formData.setAttachedImaging(booleanToFormValue(transfer.getAttachedImaging()));
		formData.setAttachedOther(StringUtils.defaultString(transfer.getAttachedOther()));

		formData.setTransportationType(StringUtils.defaultString(transfer.getTransportType()));
		formData.setTransportationOtherSpec(StringUtils.defaultString(transfer.getTransportOther()));
		formData.setHealthInsuranceType(StringUtils.defaultString(transfer.getHealthInsuranceType()));
		formData.setHealthInsuranceOtherSpec(StringUtils.defaultString(transfer.getHealthInsuranceOther()));

		if (StringUtils.isNotBlank(transfer.getReferringProviderName())) {
			formData.setReferringProviderName(transfer.getReferringProviderName());
		}
		if (StringUtils.isNotBlank(transfer.getReferringProviderQualification())) {
			formData.setReferringProviderQualification(transfer.getReferringProviderQualification());
		}
		if (transfer.getReferringSignedDate() != null) {
			formData.setReferringSignedDate(formatDate(transfer.getReferringSignedDate()));
		}
		if (StringUtils.isNotBlank(transfer.getReferringSignedTime())) {
			formData.setReferringSignedTime(transfer.getReferringSignedTime());
		}
		if (StringUtils.isNotBlank(transfer.getReferringProviderPhone())) {
			formData.setReferringProviderPhone(transfer.getReferringProviderPhone());
		}

		List<MaternityTransferTreatment> treatments = transfer.getTreatments();
		if (treatments != null && !treatments.isEmpty()) {
			List<MaternityTransferTreatmentRow> rows = new ArrayList<MaternityTransferTreatmentRow>();
			for (MaternityTransferTreatment treatment : treatments) {
				MaternityTransferTreatmentRow row = new MaternityTransferTreatmentRow();
				row.setTreatmentName(treatment.getTreatmentName());
				row.setDose(treatment.getDose());
				if (treatment.getGivenDate() != null) {
					row.setGivenDate(formatDate(treatment.getGivenDate()));
				}
				row.setGivenTime(treatment.getGivenTime());
				rows.add(row);
			}
			formData.setDefaultTreatmentRows(rows);
		}
	}

	protected String booleanToFormValue(Boolean value) {
		return Boolean.TRUE.equals(value) ? "true" : "";
	}

	protected String formatDateTimeSpace(Date date) {
		return new SimpleDateFormat("yyyy-MM-dd HH:mm").format(date);
	}

	protected void prefillFromPatient(MaternityTransferFormData formData, Patient patient) {
		if (patient.getPersonName() != null) {
			formData.setClientName(patient.getPersonName().getFullName());
		}

		String upid = patientSnapshotResolver.resolveUpid(patient);
		if (upid != null) {
			formData.setSerialNumberEmr(upid);
		} else {
			PatientIdentifier openMrsId = patient.getPatientIdentifier();
			if (openMrsId != null) {
				formData.setSerialNumberEmr(openMrsId.getIdentifier());
			}
		}

		formData.setAgeOrDob(patientSnapshotResolver.resolveAgeOrDob(patient));
		formData.setNextOfKinName(patientSnapshotResolver.resolvePersonAttribute(patient,
				"Caregiver Name", "CaregiverName", "Name of caregiver", "Next of Kin", "NextOfKin"));
		formData.setNextOfKinTelephone(patientSnapshotResolver.resolvePersonAttribute(patient,
				"Caregiver Telephone", "Caregiver Phone", "CaregiverPhone", "Next of Kin Telephone"));

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
	}

	protected void prefillFromCurrentUser(MaternityTransferFormData formData) {
		User user = Context.getAuthenticatedUser();
		if (user != null && user.getPerson() != null && user.getPerson().getPersonName() != null) {
			formData.setReferringProviderName(user.getPerson().getPersonName().getFullName());
		}
	}

	protected void prefillDefaults(MaternityTransferFormData formData) {
		Date now = new Date();
		formData.setAdmissionAt(formatDateTimeLocal(now));
		formData.setDecisionToTransferAt(formatDateTimeLocal(now));
		formData.setCallingTime(formatTime(now));
		formData.setReferringSignedDate(formatDate(now));
		formData.setReferringSignedTime(formatTime(now));
	}

	protected String getCurrentFacilityName() {
		Location sessionLocation = Context.getUserContext().getLocation();
		if (sessionLocation != null && sessionLocation.getParentLocation() != null) {
			return sessionLocation.getParentLocation().getName();
		} else if (sessionLocation != null && sessionLocation.getParentLocation() == null) {
			return sessionLocation.getName();
		}
		return "";
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
				new TransferFormOption("PRIVATE", "Private"),
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

	protected List<MaternityTransferTreatmentRow> getDefaultTreatmentRows() {
		List<MaternityTransferTreatmentRow> rows = new ArrayList<MaternityTransferTreatmentRow>();
		rows.add(new MaternityTransferTreatmentRow("IV Fluids"));
		rows.add(new MaternityTransferTreatmentRow("Dexamethasone"));
		rows.add(new MaternityTransferTreatmentRow("Magnesium sulphate"));
		rows.add(new MaternityTransferTreatmentRow("Nifedipine"));
		rows.add(new MaternityTransferTreatmentRow("Oxytocin"));
		rows.add(new MaternityTransferTreatmentRow("ATBs"));
		return rows;
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
