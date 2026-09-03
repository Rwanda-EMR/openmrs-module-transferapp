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
import org.openmrs.api.APIException;
import org.openmrs.api.PatientService;
import org.openmrs.api.context.Context;
import org.openmrs.module.transferapp.api.MaternityTransferService;
import org.openmrs.module.transferapp.api.TransferAdminService;
import org.openmrs.module.transferapp.api.dao.MaternityTransferDao;
import org.openmrs.module.transferapp.model.MaternityTransfer;
import org.openmrs.module.transferapp.model.MaternityTransferFormData;
import org.openmrs.module.transferapp.model.MaternityTransferTreatment;
import org.openmrs.module.transferapp.model.MaternityTransferTreatmentRow;
import org.openmrs.module.transferapp.model.ReceivingFacility;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class MaternityTransferServiceImpl implements MaternityTransferService {

	private static final String DATETIME_LOCAL_PATTERN = "yyyy-MM-dd'T'HH:mm";
	private static final String DATETIME_SPACE_PATTERN = "yyyy-MM-dd HH:mm";
	private static final String DATE_PATTERN = "yyyy-MM-dd";
	private static final String TIME_PATTERN = "HH:mm";
	private static final String TRANSFER_TYPE_EMERGENCY = "EMERGENCY";
	private static final String TRANSPORT_TYPE_AMBULANCE = "AMBULANCE";
	private static final String HEALTH_INSURANCE_OTHER = "OTHER";

	private MaternityTransferDao maternityTransferDao;

	private PatientService patientService;

	private TransferAdminService transferAdminService;

	public void setMaternityTransferDao(MaternityTransferDao maternityTransferDao) {
		this.maternityTransferDao = maternityTransferDao;
	}

	public void setPatientService(PatientService patientService) {
		this.patientService = patientService;
	}

	public void setTransferAdminService(TransferAdminService transferAdminService) {
		this.transferAdminService = transferAdminService;
	}

	@Override
	public MaternityTransfer saveMaternityTransfer(Integer patientId, Integer receivingFacilityId,
			MaternityTransferFormData formData, List<MaternityTransferTreatmentRow> treatmentRows) {

		if (patientId == null) {
			throw new APIException("Patient is required");
		}
		if (formData == null) {
			throw new APIException("Maternity transfer form data is required");
		}

		Patient patient = patientService.getPatient(patientId);
		if (patient == null) {
			throw new APIException("Patient not found");
		}

		String normalizedTransferType = StringUtils.trimToNull(formData.getTransferType());
		validateTransferTypeFields(normalizedTransferType, formData.getAmbulanceCalledTime(),
				formData.getDepartureFromReferringTime());
		validateTransportationFields(normalizedTransferType, formData.getTransportationType(),
				formData.getTransportationOtherSpec());
		validateHealthInsuranceFields(formData.getHealthInsuranceType(), formData.getHealthInsuranceOtherSpec());
		ensureReceivingServiceConfigured(formData.getReceivingFacilityCode(), receivingFacilityId,
				formData.getReceivingService());

		boolean isUpdate = StringUtils.isNotBlank(formData.getTransferUuid());
		MaternityTransfer transfer;
		if (isUpdate) {
			transfer = maternityTransferDao.getMaternityTransferByUuid(formData.getTransferUuid().trim());
			if (transfer == null || transfer.isVoided()) {
				throw new APIException("Transfer not found");
			}
			if (transfer.getPatient() == null
					|| transfer.getPatient().getPatientId() == null
					|| !transfer.getPatient().getPatientId().equals(patientId)) {
				throw new APIException("Transfer does not belong to this patient");
			}
			transfer.getTreatments().clear();
		}
		else {
			transfer = new MaternityTransfer();
			transfer.setUuid(UUID.randomUUID().toString());
			transfer.setPatient(patient);
		}

		// Step 1 — client & referral info
		transfer.setProvince(StringUtils.trimToNull(formData.getProvince()));
		transfer.setDistrict(StringUtils.trimToNull(formData.getDistrict()));
		transfer.setHospitalName(StringUtils.trimToNull(formData.getHospitalName()));
		transfer.setReferringFacilityName(StringUtils.trimToNull(formData.getReferringFacilityName()));
		transfer.setReferringUnit(StringUtils.trimToNull(formData.getReferringUnit()));
		transfer.setClientName(StringUtils.trimToNull(formData.getClientName()));
		transfer.setSerialNumberEmr(StringUtils.trimToNull(formData.getSerialNumberEmr()));
		transfer.setAgeOrDob(StringUtils.trimToNull(formData.getAgeOrDob()));
		transfer.setNextOfKinName(StringUtils.trimToNull(formData.getNextOfKinName()));
		transfer.setNextOfKinTelephone(StringUtils.trimToNull(formData.getNextOfKinTelephone()));
		transfer.setClientDistrict(StringUtils.trimToNull(formData.getClientDistrict()));
		transfer.setSector(StringUtils.trimToNull(formData.getSector()));
		transfer.setCell(StringUtils.trimToNull(formData.getCell()));
		transfer.setVillage(StringUtils.trimToNull(formData.getVillage()));
		transfer.setAdmissionAt(parseDateTimeLocal(formData.getAdmissionAt()));
		transfer.setDecisionToTransferAt(parseDateTimeLocal(formData.getDecisionToTransferAt()));
		transfer.setReceivingFacilityCode(StringUtils.trimToNull(formData.getReceivingFacilityCode()));
		transfer.setReceivingService(StringUtils.trimToNull(formData.getReceivingService()));
		transfer.setCallingTime(StringUtils.trimToNull(formData.getCallingTime()));
		transfer.setStaffContactedName(StringUtils.trimToNull(formData.getStaffContactedName()));
		transfer.setStaffContactedPhone(StringUtils.trimToNull(formData.getStaffContactedPhone()));
		transfer.setReasonForTransfer(StringUtils.trimToNull(formData.getReasonForTransfer()));
		transfer.setTransferType(normalizedTransferType);
		if (TRANSFER_TYPE_EMERGENCY.equals(normalizedTransferType)) {
			transfer.setAmbulanceCalledTime(StringUtils.trimToNull(formData.getAmbulanceCalledTime()));
			transfer.setDepartureFromReferringTime(StringUtils.trimToNull(formData.getDepartureFromReferringTime()));
		}
		transfer.setPartographAttached(parseBoolean(formData.getPartographAttached()));
		transfer.setClinicalPresentation(StringUtils.trimToNull(formData.getClinicalPresentation()));
		transfer.setDisabilityType(StringUtils.trimToNull(formData.getDisabilityType()));
		transfer.setSendingFacility(resolveCurrentFacilityName());

		// Step 2 — obstetric history & current pregnancy
		transfer.setObstetricGravida(StringUtils.trimToNull(formData.getObstetricGravida()));
		transfer.setObstetricParity(StringUtils.trimToNull(formData.getObstetricParity()));
		transfer.setObstetricLivingChildren(StringUtils.trimToNull(formData.getObstetricLivingChildren()));
		transfer.setObstetricAbortion(StringUtils.trimToNull(formData.getObstetricAbortion()));
		transfer.setObstetricStillbirth(StringUtils.trimToNull(formData.getObstetricStillbirth()));
		transfer.setObstetricNeonatalDeath(StringUtils.trimToNull(formData.getObstetricNeonatalDeath()));
		transfer.setObstetricPretermBirth(StringUtils.trimToNull(formData.getObstetricPretermBirth()));
		transfer.setLmpDate(parseDateValue(formData.getLmpDate()));
		transfer.setEddDate(parseDateValue(formData.getEddDate()));
		transfer.setGestationAge(StringUtils.trimToNull(formData.getGestationAge()));
		transfer.setMuac(StringUtils.trimToNull(formData.getMuac()));
		transfer.setAncCompletedCount(StringUtils.trimToNull(formData.getAncCompletedCount()));
		transfer.setTetanusVaccineDoses(StringUtils.trimToNull(formData.getTetanusVaccineDoses()));
		transfer.setPreviousSignificantHistory(StringUtils.trimToNull(formData.getPreviousSignificantHistory()));
		transfer.setMultiPregnanciesAndKnownHiv(StringUtils.trimToNull(formData.getMultiPregnanciesAndKnownHiv()));
		transfer.setCurrentPregnancyComplications(StringUtils.trimToNull(formData.getCurrentPregnancyComplications()));

		// Step 3 — clinical findings
		transfer.setLatestHemoglobin(StringUtils.trimToNull(formData.getLatestHemoglobin()));
		transfer.setLatestHivStatus(StringUtils.trimToNull(formData.getLatestHivStatus()));
		transfer.setLatestBloodGroup(StringUtils.trimToNull(formData.getLatestBloodGroup()));
		transfer.setLatestOtherResults(StringUtils.trimToNull(formData.getLatestOtherResults()));
		transfer.setVitalBp(StringUtils.trimToNull(formData.getVitalBp()));
		transfer.setVitalTemp(StringUtils.trimToNull(formData.getVitalTemp()));
		transfer.setVitalSpo2(StringUtils.trimToNull(formData.getVitalSpo2()));
		transfer.setVitalRr(StringUtils.trimToNull(formData.getVitalRr()));
		transfer.setVitalPulse(StringUtils.trimToNull(formData.getVitalPulse()));
		transfer.setVitalWeight(StringUtils.trimToNull(formData.getVitalWeight()));
		transfer.setVitalHeight(StringUtils.trimToNull(formData.getVitalHeight()));
		transfer.setFetalPresentation(StringUtils.trimToNull(formData.getFetalPresentation()));
		transfer.setFundalHeight(StringUtils.trimToNull(formData.getFundalHeight()));
		transfer.setFetalHeartRate(StringUtils.trimToNull(formData.getFetalHeartRate()));
		transfer.setContractions(StringUtils.trimToNull(formData.getContractions()));
		transfer.setVaginalExamAt(parseDateTimeLocal(formData.getVaginalExamAt()));
		transfer.setDilation(StringUtils.trimToNull(formData.getDilation()));
		transfer.setEffacement(StringUtils.trimToNull(formData.getEffacement()));
		transfer.setDescent(StringUtils.trimToNull(formData.getDescent()));
		transfer.setConsistency(StringUtils.trimToNull(formData.getConsistency()));
		transfer.setPosition(StringUtils.trimToNull(formData.getPosition()));
		transfer.setCaput(parseBoolean(formData.getCaput()));
		transfer.setMoulding(parseBoolean(formData.getMoulding()));
		transfer.setMembranesRuptured(parseBoolean(formData.getMembranesRuptured()));
		transfer.setMembranesRupturedAt(parseDateTimeLocal(formData.getMembranesRupturedAt()));
		transfer.setAmnioticFluidColor(StringUtils.trimToNull(formData.getAmnioticFluidColor()));
		transfer.setEstimatedBloodLossMl(StringUtils.trimToNull(formData.getEstimatedBloodLossMl()));
		transfer.setInvestigationHgb(StringUtils.trimToNull(formData.getInvestigationHgb()));
		transfer.setInvestigationUrineTest(StringUtils.trimToNull(formData.getInvestigationUrineTest()));
		transfer.setInvestigationOtherTest(StringUtils.trimToNull(formData.getInvestigationOtherTest()));
		transfer.setImagingInvestigations(StringUtils.trimToNull(formData.getImagingInvestigations()));
		transfer.setDiagnosis(StringUtils.trimToNull(formData.getDiagnosis()));
		transfer.setProcedures(StringUtils.trimToNull(formData.getProcedures()));
		transfer.setAttachedLabTests(parseBoolean(formData.getAttachedLabTests()));
		transfer.setAttachedImaging(parseBoolean(formData.getAttachedImaging()));
		transfer.setAttachedOther(StringUtils.trimToNull(formData.getAttachedOther()));

		// Step 4 — treatment & transport
		applyTransportationSnapshot(transfer, normalizedTransferType, formData.getTransportationType(),
				formData.getTransportationOtherSpec());
		applyTreatmentRows(transfer, treatmentRows);

		// Step 5 — sign-off & insurance
		applyHealthInsurance(transfer, formData.getHealthInsuranceType(), formData.getHealthInsuranceOtherSpec());
		transfer.setReferringProviderName(StringUtils.trimToNull(formData.getReferringProviderName()));
		transfer.setReferringProviderQualification(StringUtils.trimToNull(formData.getReferringProviderQualification()));
		transfer.setReferringSignedDate(parseDateValue(formData.getReferringSignedDate()));
		transfer.setReferringSignedTime(StringUtils.trimToNull(formData.getReferringSignedTime()));
		transfer.setReferringProviderPhone(StringUtils.trimToNull(formData.getReferringProviderPhone()));

		Date now = new Date();
		if (isUpdate) {
			transfer.setChangedBy(Context.getAuthenticatedUser());
			transfer.setDateChanged(now);
			// Local correction must be submitted to HIE again.
			transfer.setHieSent(false);
			transfer.setHieSentAt(null);
			transfer.setHieSendError(null);
		}
		else {
			transfer.setCreator(Context.getAuthenticatedUser());
			transfer.setDateCreated(now);
			transfer.setVoided(false);
			transfer.setHieSent(false);
		}
		if (transfer.getReferringSignedDate() == null) {
			transfer.setReferringSignedDate(now);
		}
		if (StringUtils.isBlank(transfer.getReferringSignedTime())) {
			transfer.setReferringSignedTime(new SimpleDateFormat(TIME_PATTERN).format(now));
		}

		return maternityTransferDao.saveMaternityTransfer(transfer);
	}

	private void applyTreatmentRows(MaternityTransfer transfer, List<MaternityTransferTreatmentRow> treatmentRows) {
		if (treatmentRows == null) {
			return;
		}
		for (MaternityTransferTreatmentRow row : treatmentRows) {
			if (row == null) {
				continue;
			}
			String treatmentName = StringUtils.trimToNull(row.getTreatmentName());
			String dose = StringUtils.trimToNull(row.getDose());
			String givenDateText = StringUtils.trimToNull(row.getGivenDate());
			String givenTime = StringUtils.trimToNull(row.getGivenTime());
			if (treatmentName == null && dose == null && givenDateText == null && givenTime == null) {
				continue;
			}
			MaternityTransferTreatment treatment = new MaternityTransferTreatment();
			treatment.setTreatmentName(treatmentName);
			treatment.setDose(dose);
			treatment.setGivenDate(parseDateValue(givenDateText));
			treatment.setGivenTime(givenTime);
			transfer.addTreatment(treatment);
		}
	}

	private void applyHealthInsurance(MaternityTransfer transfer, String healthInsuranceType,
			String healthInsuranceOtherSpec) {
		String type = StringUtils.trimToNull(healthInsuranceType);
		transfer.setHealthInsuranceType(type);
		if (HEALTH_INSURANCE_OTHER.equals(type)) {
			transfer.setHealthInsuranceOther(StringUtils.trimToNull(healthInsuranceOtherSpec));
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
			String transportationOtherSpec) {
		if (TRANSFER_TYPE_EMERGENCY.equals(transferType)) {
			return;
		}
		String transport = StringUtils.trimToNull(transportationType);
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

	private void validateHealthInsuranceFields(String healthInsuranceType, String healthInsuranceOtherSpec) {
		String type = StringUtils.trimToNull(healthInsuranceType);
		if (type == null) {
			throw new APIException("Health insurance type is required");
		}
		if (!Arrays.asList("CBHI", "RSSB", "MMI", "OTHER", "NONE").contains(type)) {
			throw new APIException("Invalid health insurance type");
		}
		if (HEALTH_INSURANCE_OTHER.equals(type) && StringUtils.isBlank(healthInsuranceOtherSpec)) {
			throw new APIException("Please specify other health insurance type");
		}
	}

	private void applyTransportationSnapshot(MaternityTransfer transfer, String transferType,
			String transportationType, String transportationOtherSpec) {
		if (TRANSFER_TYPE_EMERGENCY.equals(transferType)) {
			transfer.setTransportType(TRANSPORT_TYPE_AMBULANCE);
			transfer.setTransportOther(null);
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
	}

	protected String resolveCurrentFacilityName() {
		Location sessionLocation = Context.getUserContext().getLocation();
		if (sessionLocation != null && sessionLocation.getParentLocation() != null) {
			return sessionLocation.getParentLocation().getName();
		} else if (sessionLocation != null) {
			return sessionLocation.getName();
		}
		return null;
	}

	@Override
	public List<MaternityTransfer> getMaternityTransfersByPatient(Patient patient) {
		return maternityTransferDao.getMaternityTransfersByPatient(patient);
	}

	@Override
	public List<MaternityTransfer> getMaternityTransfersByPatient(Patient patient, Integer limit) {
		return maternityTransferDao.getMaternityTransfersByPatient(patient, limit);
	}

	@Override
	public int countMaternityTransfersByPatient(Patient patient) {
		return maternityTransferDao.countMaternityTransfersByPatient(patient);
	}

	@Override
	public MaternityTransfer getMaternityTransferByUuid(String uuid) {
		if (StringUtils.isBlank(uuid)) {
			return null;
		}
		return maternityTransferDao.getMaternityTransferByUuid(uuid.trim());
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
		return null;
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

	protected Boolean parseBoolean(String value) {
		if (StringUtils.isBlank(value)) {
			return Boolean.FALSE;
		}
		String trimmed = value.trim();
		return "true".equalsIgnoreCase(trimmed) || "on".equalsIgnoreCase(trimmed) || "1".equals(trimmed);
	}

}
