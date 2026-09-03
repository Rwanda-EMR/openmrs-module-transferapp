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
import org.openmrs.module.transferapp.api.NeonatalTransferService;
import org.openmrs.module.transferapp.api.TransferAdminService;
import org.openmrs.module.transferapp.api.dao.NeonatalTransferDao;
import org.openmrs.module.transferapp.model.NeonatalTransfer;
import org.openmrs.module.transferapp.model.NeonatalTransferFormData;
import org.openmrs.module.transferapp.model.ReceivingFacility;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class NeonatalTransferServiceImpl implements NeonatalTransferService {

	private static final String DATETIME_LOCAL_PATTERN = "yyyy-MM-dd'T'HH:mm";
	private static final String DATETIME_SPACE_PATTERN = "yyyy-MM-dd HH:mm";
	private static final String DATE_PATTERN = "yyyy-MM-dd";
	private static final String TIME_PATTERN = "HH:mm";
	private static final String TRANSPORT_TYPE_OTHER = "OTHER";

	private NeonatalTransferDao neonatalTransferDao;

	private PatientService patientService;

	private TransferAdminService transferAdminService;

	public void setNeonatalTransferDao(NeonatalTransferDao neonatalTransferDao) {
		this.neonatalTransferDao = neonatalTransferDao;
	}

	public void setPatientService(PatientService patientService) {
		this.patientService = patientService;
	}

	public void setTransferAdminService(TransferAdminService transferAdminService) {
		this.transferAdminService = transferAdminService;
	}

	@Override
	public NeonatalTransfer saveNeonatalTransfer(Integer patientId, Integer receivingFacilityId,
			NeonatalTransferFormData formData) {

		if (patientId == null) {
			throw new APIException("Patient is required");
		}
		if (formData == null) {
			throw new APIException("Neonatal transfer form data is required");
		}

		Patient patient = patientService.getPatient(patientId);
		if (patient == null) {
			throw new APIException("Patient not found");
		}

		String normalizedTransferType = StringUtils.trimToNull(formData.getTransferType());
		validateTransferTypeFields(normalizedTransferType);
		validateTransportFields(formData.getModeOfTransport(), formData.getTransportOther());
		ensureReceivingServiceConfigured(formData.getReceivingFacilityCode(), receivingFacilityId,
				formData.getReceivingService());

		boolean isUpdate = StringUtils.isNotBlank(formData.getTransferUuid());
		NeonatalTransfer transfer;
		if (isUpdate) {
			transfer = neonatalTransferDao.getNeonatalTransferByUuid(formData.getTransferUuid().trim());
			if (transfer == null || transfer.isVoided()) {
				throw new APIException("Transfer not found");
			}
			if (transfer.getPatient() == null
					|| transfer.getPatient().getPatientId() == null
					|| !transfer.getPatient().getPatientId().equals(patientId)) {
				throw new APIException("Transfer does not belong to this patient");
			}
		}
		else {
			transfer = new NeonatalTransfer();
			transfer.setUuid(UUID.randomUUID().toString());
			transfer.setPatient(patient);
		}

		// Step 1 — baby & referral info
		transfer.setBabyName(StringUtils.trimToNull(formData.getBabyName()));
		transfer.setSex(StringUtils.trimToNull(formData.getSex()));
		transfer.setDob(parseDateValue(formData.getDob()));
		transfer.setGestationalAgeWeeks(StringUtils.trimToNull(formData.getGestationalAgeWeeks()));
		transfer.setBirthWeightG(StringUtils.trimToNull(formData.getBirthWeightG()));
		transfer.setCurrentWeightG(StringUtils.trimToNull(formData.getCurrentWeightG()));
		transfer.setCurrentAgeDays(StringUtils.trimToNull(formData.getCurrentAgeDays()));
		transfer.setMotherName(StringUtils.trimToNull(formData.getMotherName()));
		transfer.setMotherAge(StringUtils.trimToNull(formData.getMotherAge()));
		transfer.setMotherCaregiverPhone(StringUtils.trimToNull(formData.getMotherCaregiverPhone()));
		transfer.setPlaceOfBirth(StringUtils.trimToNull(formData.getPlaceOfBirth()));
		transfer.setReasonForTransfer(StringUtils.trimToNull(formData.getReasonForTransfer()));
		transfer.setModeOfTransport(StringUtils.trimToNull(formData.getModeOfTransport()));
		if (TRANSPORT_TYPE_OTHER.equals(StringUtils.trimToNull(formData.getModeOfTransport()))) {
			transfer.setTransportOther(StringUtils.trimToNull(formData.getTransportOther()));
		}
		else {
			transfer.setTransportOther(null);
		}
		transfer.setTransferType(normalizedTransferType);
		transfer.setReceivingFacilityCode(StringUtils.trimToNull(formData.getReceivingFacilityCode()));
		transfer.setReceivingService(StringUtils.trimToNull(formData.getReceivingService()));
		transfer.setCallingTime(StringUtils.trimToNull(formData.getCallingTime()));
		transfer.setStaffContactedName(StringUtils.trimToNull(formData.getStaffContactedName()));
		transfer.setStaffContactedPhone(StringUtils.trimToNull(formData.getStaffContactedPhone()));
		transfer.setDecisionToTransferAt(parseDateTimeLocal(formData.getDecisionToTransferAt()));
		transfer.setSendingFacility(resolveCurrentFacilityName());

		// Facility details (header)
		transfer.setProvince(StringUtils.trimToNull(formData.getProvince()));
		transfer.setDistrict(StringUtils.trimToNull(formData.getDistrict()));
		transfer.setHospitalName(StringUtils.trimToNull(formData.getHospitalName()));
		transfer.setReferringFacilityName(StringUtils.trimToNull(formData.getReferringFacilityName()));
		transfer.setReferringUnit(StringUtils.trimToNull(formData.getReferringUnit()));

		// Step 2 — maternal history
		transfer.setMotherAlive(StringUtils.trimToNull(formData.getMotherAlive()));
		transfer.setObstetricGravida(StringUtils.trimToNull(formData.getObstetricGravida()));
		transfer.setObstetricParity(StringUtils.trimToNull(formData.getObstetricParity()));
		transfer.setPregnancyType(StringUtils.trimToNull(formData.getPregnancyType()));
		transfer.setAncScreening(StringUtils.trimToNull(formData.getAncScreening()));
		transfer.setPathologiesDuringPregnancy(StringUtils.trimToNull(formData.getPathologiesDuringPregnancy()));
		transfer.setPregnancyOtherPathologies(StringUtils.trimToNull(formData.getPregnancyOtherPathologies()));
		transfer.setPregnancyTreatment(StringUtils.trimToNull(formData.getPregnancyTreatment()));
		transfer.setBloodGroup(StringUtils.trimToNull(formData.getBloodGroup()));
		transfer.setRhFactor(StringUtils.trimToNull(formData.getRhFactor()));
		transfer.setHivStatus(StringUtils.trimToNull(formData.getHivStatus()));
		transfer.setHivRegimen(StringUtils.trimToNull(formData.getHivRegimen()));
		transfer.setHivRecentVl(StringUtils.trimToNull(formData.getHivRecentVl()));
		transfer.setHivCd4Count(StringUtils.trimToNull(formData.getHivCd4Count()));
		transfer.setHivOpportunisticInfections(StringUtils.trimToNull(formData.getHivOpportunisticInfections()));
		transfer.setTetanusVaccineDoses(StringUtils.trimToNull(formData.getTetanusVaccineDoses()));
		transfer.setMaternalIllicitDrugHistory(StringUtils.trimToNull(formData.getMaternalIllicitDrugHistory()));

		// Step 3 — labor details
		transfer.setRomAt(parseDateTimeLocal(formData.getRomAt()));
		transfer.setAfQuality(StringUtils.trimToNull(formData.getAfQuality()));
		transfer.setAfQuantity(StringUtils.trimToNull(formData.getAfQuantity()));
		transfer.setFeverTiming(StringUtils.trimToNull(formData.getFeverTiming()));
		transfer.setSteroidDoses(StringUtils.trimToNull(formData.getSteroidDoses()));
		transfer.setLastSteroidDoseAt(parseDateTimeLocal(formData.getLastSteroidDoseAt()));
		transfer.setMgso4At(parseDateTimeLocal(formData.getMgso4At()));
		transfer.setModeOfDelivery(StringUtils.trimToNull(formData.getModeOfDelivery()));
		transfer.setLaborComplications(StringUtils.trimToNull(formData.getLaborComplications()));
		transfer.setLaborComplicationsOther(StringUtils.trimToNull(formData.getLaborComplicationsOther()));
		transfer.setMaternalAnesthesia(StringUtils.trimToNull(formData.getMaternalAnesthesia()));
		transfer.setMaternalAnesthesiaOther(StringUtils.trimToNull(formData.getMaternalAnesthesiaOther()));
		transfer.setMaternalAntibiotics(StringUtils.trimToNull(formData.getMaternalAntibiotics()));
		transfer.setOtherDrugs(StringUtils.trimToNull(formData.getOtherDrugs()));
		transfer.setSepsisRiskFactors(StringUtils.trimToNull(formData.getSepsisRiskFactors()));

		// Step 4 — neonatal history & drugs
		transfer.setResuscitationAtBirth(StringUtils.trimToNull(formData.getResuscitationAtBirth()));
		transfer.setResuscitationMethods(StringUtils.trimToNull(formData.getResuscitationMethods()));
		transfer.setApgar1min(StringUtils.trimToNull(formData.getApgar1min()));
		transfer.setApgar5min(StringUtils.trimToNull(formData.getApgar5min()));
		transfer.setApgar10min(StringUtils.trimToNull(formData.getApgar10min()));
		transfer.setHie(StringUtils.trimToNull(formData.getHie()));
		transfer.setHieGrade(StringUtils.trimToNull(formData.getHieGrade()));
		transfer.setAllergies(StringUtils.trimToNull(formData.getAllergies()));
		transfer.setImmunization(StringUtils.trimToNull(formData.getImmunization()));
		transfer.setImmunizationDetails(StringUtils.trimToNull(formData.getImmunizationDetails()));
		transfer.setVitaminK(StringUtils.trimToNull(formData.getVitaminK()));
		transfer.setTetracyclineEyeOintment(StringUtils.trimToNull(formData.getTetracyclineEyeOintment()));
		transfer.setSurfactant(StringUtils.trimToNull(formData.getSurfactant()));

		// Step 5 — chief complaint & diagnoses
		transfer.setChiefComplaintDetails(StringUtils.trimToNull(formData.getChiefComplaintDetails()));
		transfer.setSpo2Preductal(StringUtils.trimToNull(formData.getSpo2Preductal()));
		transfer.setSpo2Postductal(StringUtils.trimToNull(formData.getSpo2Postductal()));
		transfer.setConditionTemp(StringUtils.trimToNull(formData.getConditionTemp()));
		transfer.setConditionHr(StringUtils.trimToNull(formData.getConditionHr()));
		transfer.setConditionRr(StringUtils.trimToNull(formData.getConditionRr()));
		transfer.setConditionBp(StringUtils.trimToNull(formData.getConditionBp()));
		transfer.setNeurologicalStatus(StringUtils.trimToNull(formData.getNeurologicalStatus()));
		transfer.setSeizures(parseBoolean(formData.getSeizures()));
		transfer.setAdverseEvents24h(StringUtils.trimToNull(formData.getAdverseEvents24h()));
		transfer.setDiagnosis1(StringUtils.trimToNull(formData.getDiagnosis1()));
		transfer.setDiagnosis2(StringUtils.trimToNull(formData.getDiagnosis2()));
		transfer.setDiagnosis3(StringUtils.trimToNull(formData.getDiagnosis3()));
		transfer.setDiagnosis4(StringUtils.trimToNull(formData.getDiagnosis4()));

		// Step 6 — management at referring facility
		transfer.setRespiratorySupport(StringUtils.trimToNull(formData.getRespiratorySupport()));
		transfer.setVentilationSettings(StringUtils.trimToNull(formData.getVentilationSettings()));
		transfer.setBloodGasAnalysis(StringUtils.trimToNull(formData.getBloodGasAnalysis()));
		transfer.setIvFluidVol(StringUtils.trimToNull(formData.getIvFluidVol()));
		transfer.setPassedUrine(StringUtils.trimToNull(formData.getPassedUrine()));
		transfer.setInotropes(StringUtils.trimToNull(formData.getInotropes()));
		transfer.setInotropesSpecify(StringUtils.trimToNull(formData.getInotropesSpecify()));
		transfer.setPeripheralIv(StringUtils.trimToNull(formData.getPeripheralIv()));
		transfer.setCentralIv(StringUtils.trimToNull(formData.getCentralIv()));
		transfer.setIntraosseousLine(StringUtils.trimToNull(formData.getIntraosseousLine()));
		transfer.setAntibiotic1Name(StringUtils.trimToNull(formData.getAntibiotic1Name()));
		transfer.setAntibiotic1Doses(StringUtils.trimToNull(formData.getAntibiotic1Doses()));
		transfer.setAntibiotic1Durations(StringUtils.trimToNull(formData.getAntibiotic1Durations()));
		transfer.setAntibiotic2Name(StringUtils.trimToNull(formData.getAntibiotic2Name()));
		transfer.setAntibiotic2Doses(StringUtils.trimToNull(formData.getAntibiotic2Doses()));
		transfer.setAntibiotic2Durations(StringUtils.trimToNull(formData.getAntibiotic2Durations()));
		transfer.setArvs(StringUtils.trimToNull(formData.getArvs()));
		transfer.setNpo(StringUtils.trimToNull(formData.getNpo()));
		transfer.setLastFeedTime(StringUtils.trimToNull(formData.getLastFeedTime()));
		transfer.setLastFeedAmount(StringUtils.trimToNull(formData.getLastFeedAmount()));
		transfer.setFeedVol(StringUtils.trimToNull(formData.getFeedVol()));
		transfer.setFeedType(StringUtils.trimToNull(formData.getFeedType()));
		transfer.setPassedStool(StringUtils.trimToNull(formData.getPassedStool()));
		transfer.setNasogastricTube(StringUtils.trimToNull(formData.getNasogastricTube()));
		transfer.setLabGlucose(StringUtils.trimToNull(formData.getLabGlucose()));
		transfer.setLabFbc(StringUtils.trimToNull(formData.getLabFbc()));
		transfer.setLabHb(StringUtils.trimToNull(formData.getLabHb()));
		transfer.setLabWbc(StringUtils.trimToNull(formData.getLabWbc()));
		transfer.setLabPlatelets(StringUtils.trimToNull(formData.getLabPlatelets()));
		transfer.setLabCrp(StringUtils.trimToNull(formData.getLabCrp()));
		transfer.setLabBiliTotal(StringUtils.trimToNull(formData.getLabBiliTotal()));
		transfer.setLabBiliDirect(StringUtils.trimToNull(formData.getLabBiliDirect()));
		transfer.setLabUe(StringUtils.trimToNull(formData.getLabUe()));
		transfer.setLabCultures(StringUtils.trimToNull(formData.getLabCultures()));
		transfer.setFbcDone(StringUtils.trimToNull(formData.getFbcDone()));
		transfer.setImagingResultsAvailable(StringUtils.trimToNull(formData.getImagingResultsAvailable()));
		transfer.setImagingResults(StringUtils.trimToNull(formData.getImagingResults()));
		transfer.setPainSedationDrugs(StringUtils.trimToNull(formData.getPainSedationDrugs()));
		transfer.setImagingReportAttached(parseBoolean(formData.getImagingReportAttached()));
		transfer.setLabReportsAttached(parseBoolean(formData.getLabReportsAttached()));

		// Step 7 — summary & sign-off
		transfer.setClinicalManagementSummary(StringUtils.trimToNull(formData.getClinicalManagementSummary()));
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

		return neonatalTransferDao.saveNeonatalTransfer(transfer);
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

	private void validateTransferTypeFields(String transferType) {
		String type = StringUtils.trimToNull(transferType);
		if (type == null) {
			throw new APIException("Type of transfer is required");
		}
		if (!Arrays.asList("EMERGENCY", "NOT_EMERGENCY", "FOLLOW_UP").contains(type)) {
			throw new APIException("Invalid type of transfer");
		}
	}

	private void validateTransportFields(String modeOfTransport, String transportOther) {
		String transport = StringUtils.trimToNull(modeOfTransport);
		if (transport == null) {
			return;
		}
		if (!Arrays.asList("AMBULANCE", "OTHER", "NA").contains(transport)) {
			throw new APIException("Invalid mode of transport");
		}
		if (TRANSPORT_TYPE_OTHER.equals(transport) && StringUtils.isBlank(transportOther)) {
			throw new APIException("Please specify other mode of transport");
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
	public List<NeonatalTransfer> getNeonatalTransfersByPatient(Patient patient) {
		return neonatalTransferDao.getNeonatalTransfersByPatient(patient);
	}

	@Override
	public List<NeonatalTransfer> getNeonatalTransfersByPatient(Patient patient, Integer limit) {
		return neonatalTransferDao.getNeonatalTransfersByPatient(patient, limit);
	}

	@Override
	public int countNeonatalTransfersByPatient(Patient patient) {
		return neonatalTransferDao.countNeonatalTransfersByPatient(patient);
	}

	@Override
	public NeonatalTransfer getNeonatalTransferByUuid(String uuid) {
		if (StringUtils.isBlank(uuid)) {
			return null;
		}
		return neonatalTransferDao.getNeonatalTransferByUuid(uuid.trim());
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
