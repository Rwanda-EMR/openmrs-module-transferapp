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
import org.openmrs.PersonAddress;
import org.openmrs.User;
import org.openmrs.api.APIException;
import org.openmrs.api.context.Context;
import org.openmrs.module.transferapp.api.NewNeonatalTransferOutService;
import org.openmrs.module.transferapp.api.TransferAdminService;
import org.openmrs.module.transferapp.api.TransferPatientSnapshotResolver;
import org.openmrs.module.transferapp.api.dao.NeonatalTransferDao;
import org.openmrs.module.transferapp.api.dao.TransferDao;
import org.openmrs.module.transferapp.model.NeonatalTransfer;
import org.openmrs.module.transferapp.model.NeonatalTransferFormData;
import org.openmrs.module.transferapp.model.ReceivingFacility;
import org.openmrs.module.transferapp.model.TransferFormOption;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Builds MOH Neonatal Transfer Form wizard data with OpenMRS patient prefill.
 *
 * <p>Wired to {@link TransferDao} (the External transfer DAO), not {@code NeonatalTransferDao} —
 * this mirrors {@code NewMaternityTransferOutServiceImpl}'s wiring quirk: the "New*OutService"
 * only needs generic prefill helpers such as {@code getPreferredPersonAddress}, so it shares the
 * same DAO as the External form rather than the type-specific one used for real persistence.</p>
 */
public class NewNeonatalTransferOutServiceImpl implements NewNeonatalTransferOutService {

	private static final String DATETIME_LOCAL_PATTERN = "yyyy-MM-dd'T'HH:mm";

	private static final String DATE_PATTERN = "yyyy-MM-dd";

	private static final String TIME_PATTERN = "HH:mm";

	private final TransferPatientSnapshotResolver patientSnapshotResolver = new TransferPatientSnapshotResolver();

	private TransferDao transferDao;

	private NeonatalTransferDao neonatalTransferDao;

	private TransferAdminService transferAdminService;

	public void setTransferDao(TransferDao transferDao) {
		this.transferDao = transferDao;
	}

	public void setNeonatalTransferDao(NeonatalTransferDao neonatalTransferDao) {
		this.neonatalTransferDao = neonatalTransferDao;
	}

	public void setTransferAdminService(TransferAdminService transferAdminService) {
		this.transferAdminService = transferAdminService;
	}

	@Override
	public NeonatalTransferFormData getNeonatalTransferFormData(Patient patient) {
		return getNeonatalTransferFormData(patient, null);
	}

	@Override
	public NeonatalTransferFormData getNeonatalTransferFormData(Patient patient, String transferUuid) {
		NeonatalTransferFormData formData = new NeonatalTransferFormData();
		formData.setPatientId(patient.getPatientId());
		formData.setPatientDisplay(patient.getPersonName() != null ? patient.getPersonName().getFullName() : "");
		formData.setSendingFacility(getCurrentFacilityName());
		formData.setHospitalName(getCurrentFacilityName());
		formData.setReferringFacilityName(getCurrentFacilityName());

		formData.setReceivingFacilities(getReceivingFacilities());
		formData.setReceivingServices(Collections.<String>emptyList());
		formData.setTransferTypes(getTransferTypes());
		formData.setTransportTypes(getTransportTypes());

		prefillFromPatient(formData, patient);
		prefillFromCurrentUser(formData);
		prefillDefaults(formData);

		if (StringUtils.isNotBlank(transferUuid)) {
			prefillFromExistingTransfer(formData, patient, transferUuid.trim());
		}

		return formData;
	}

	protected void prefillFromExistingTransfer(NeonatalTransferFormData formData, Patient patient, String transferUuid) {
		if (neonatalTransferDao == null) {
			throw new APIException("Unable to load neonatal transfer for editing");
		}
		NeonatalTransfer transfer = neonatalTransferDao.getNeonatalTransferByUuid(transferUuid);
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

		formData.setBabyName(StringUtils.defaultString(transfer.getBabyName()));
		formData.setSex(StringUtils.defaultString(transfer.getSex()));
		if (transfer.getDob() != null) {
			formData.setDob(formatDate(transfer.getDob()));
		}
		formData.setGestationalAgeWeeks(StringUtils.defaultString(transfer.getGestationalAgeWeeks()));
		formData.setBirthWeightG(StringUtils.defaultString(transfer.getBirthWeightG()));
		formData.setCurrentWeightG(StringUtils.defaultString(transfer.getCurrentWeightG()));
		formData.setCurrentAgeDays(StringUtils.defaultString(transfer.getCurrentAgeDays()));
		formData.setMotherName(StringUtils.defaultString(transfer.getMotherName()));
		formData.setMotherAge(StringUtils.defaultString(transfer.getMotherAge()));
		formData.setMotherCaregiverPhone(StringUtils.defaultString(transfer.getMotherCaregiverPhone()));
		formData.setPlaceOfBirth(StringUtils.defaultString(transfer.getPlaceOfBirth()));
		formData.setReasonForTransfer(StringUtils.defaultString(transfer.getReasonForTransfer()));
		formData.setModeOfTransport(StringUtils.defaultString(transfer.getModeOfTransport()));
		formData.setTransportOther(StringUtils.defaultString(transfer.getTransportOther()));
		formData.setTransferType(StringUtils.defaultString(transfer.getTransferType()));
		formData.setReceivingFacilityCode(StringUtils.defaultString(transfer.getReceivingFacilityCode()));
		formData.setReceivingService(StringUtils.defaultString(transfer.getReceivingService()));
		formData.setCallingTime(StringUtils.defaultString(transfer.getCallingTime()));
		formData.setStaffContactedName(StringUtils.defaultString(transfer.getStaffContactedName()));
		formData.setStaffContactedPhone(StringUtils.defaultString(transfer.getStaffContactedPhone()));
		if (transfer.getDecisionToTransferAt() != null) {
			formData.setDecisionToTransferAt(formatDateTimeSpace(transfer.getDecisionToTransferAt()));
		}

		formData.setMotherAlive(StringUtils.defaultString(transfer.getMotherAlive()));
		formData.setObstetricGravida(StringUtils.defaultString(transfer.getObstetricGravida()));
		formData.setObstetricParity(StringUtils.defaultString(transfer.getObstetricParity()));
		formData.setPregnancyType(StringUtils.defaultString(transfer.getPregnancyType()));
		formData.setAncScreening(StringUtils.defaultString(transfer.getAncScreening()));
		formData.setPathologiesDuringPregnancy(StringUtils.defaultString(transfer.getPathologiesDuringPregnancy()));
		formData.setPregnancyOtherPathologies(StringUtils.defaultString(transfer.getPregnancyOtherPathologies()));
		formData.setPregnancyTreatment(StringUtils.defaultString(transfer.getPregnancyTreatment()));
		formData.setBloodGroup(StringUtils.defaultString(transfer.getBloodGroup()));
		formData.setRhFactor(StringUtils.defaultString(transfer.getRhFactor()));
		formData.setHivStatus(StringUtils.defaultString(transfer.getHivStatus()));
		formData.setHivRegimen(StringUtils.defaultString(transfer.getHivRegimen()));
		formData.setHivRecentVl(StringUtils.defaultString(transfer.getHivRecentVl()));
		formData.setHivCd4Count(StringUtils.defaultString(transfer.getHivCd4Count()));
		formData.setHivOpportunisticInfections(StringUtils.defaultString(transfer.getHivOpportunisticInfections()));
		formData.setTetanusVaccineDoses(StringUtils.defaultString(transfer.getTetanusVaccineDoses()));
		formData.setMaternalIllicitDrugHistory(StringUtils.defaultString(transfer.getMaternalIllicitDrugHistory()));

		if (transfer.getRomAt() != null) {
			formData.setRomAt(formatDateTimeSpace(transfer.getRomAt()));
		}
		formData.setAfQuality(StringUtils.defaultString(transfer.getAfQuality()));
		formData.setAfQuantity(StringUtils.defaultString(transfer.getAfQuantity()));
		formData.setFeverTiming(StringUtils.defaultString(transfer.getFeverTiming()));
		formData.setSteroidDoses(StringUtils.defaultString(transfer.getSteroidDoses()));
		if (transfer.getLastSteroidDoseAt() != null) {
			formData.setLastSteroidDoseAt(formatDateTimeSpace(transfer.getLastSteroidDoseAt()));
		}
		if (transfer.getMgso4At() != null) {
			formData.setMgso4At(formatDateTimeSpace(transfer.getMgso4At()));
		}
		formData.setModeOfDelivery(StringUtils.defaultString(transfer.getModeOfDelivery()));
		formData.setLaborComplications(StringUtils.defaultString(transfer.getLaborComplications()));
		formData.setLaborComplicationsOther(StringUtils.defaultString(transfer.getLaborComplicationsOther()));
		formData.setMaternalAnesthesia(StringUtils.defaultString(transfer.getMaternalAnesthesia()));
		formData.setMaternalAnesthesiaOther(StringUtils.defaultString(transfer.getMaternalAnesthesiaOther()));
		formData.setMaternalAntibiotics(StringUtils.defaultString(transfer.getMaternalAntibiotics()));
		formData.setOtherDrugs(StringUtils.defaultString(transfer.getOtherDrugs()));
		formData.setSepsisRiskFactors(StringUtils.defaultString(transfer.getSepsisRiskFactors()));

		formData.setResuscitationAtBirth(StringUtils.defaultString(transfer.getResuscitationAtBirth()));
		formData.setResuscitationMethods(StringUtils.defaultString(transfer.getResuscitationMethods()));
		formData.setApgar1min(StringUtils.defaultString(transfer.getApgar1min()));
		formData.setApgar5min(StringUtils.defaultString(transfer.getApgar5min()));
		formData.setApgar10min(StringUtils.defaultString(transfer.getApgar10min()));
		formData.setHie(StringUtils.defaultString(transfer.getHie()));
		formData.setHieGrade(StringUtils.defaultString(transfer.getHieGrade()));
		formData.setAllergies(StringUtils.defaultString(transfer.getAllergies()));
		formData.setImmunization(StringUtils.defaultString(transfer.getImmunization()));
		formData.setImmunizationDetails(StringUtils.defaultString(transfer.getImmunizationDetails()));
		formData.setVitaminK(StringUtils.defaultString(transfer.getVitaminK()));
		formData.setTetracyclineEyeOintment(StringUtils.defaultString(transfer.getTetracyclineEyeOintment()));
		formData.setSurfactant(StringUtils.defaultString(transfer.getSurfactant()));

		formData.setChiefComplaintDetails(StringUtils.defaultString(transfer.getChiefComplaintDetails()));
		formData.setSpo2Preductal(StringUtils.defaultString(transfer.getSpo2Preductal()));
		formData.setSpo2Postductal(StringUtils.defaultString(transfer.getSpo2Postductal()));
		formData.setConditionTemp(StringUtils.defaultString(transfer.getConditionTemp()));
		formData.setConditionHr(StringUtils.defaultString(transfer.getConditionHr()));
		formData.setConditionRr(StringUtils.defaultString(transfer.getConditionRr()));
		formData.setConditionBp(StringUtils.defaultString(transfer.getConditionBp()));
		formData.setNeurologicalStatus(StringUtils.defaultString(transfer.getNeurologicalStatus()));
		formData.setAdverseEvents24h(StringUtils.defaultString(transfer.getAdverseEvents24h()));
		formData.setDiagnosis1(StringUtils.defaultString(transfer.getDiagnosis1()));
		formData.setDiagnosis2(StringUtils.defaultString(transfer.getDiagnosis2()));
		formData.setDiagnosis3(StringUtils.defaultString(transfer.getDiagnosis3()));
		formData.setDiagnosis4(StringUtils.defaultString(transfer.getDiagnosis4()));

		formData.setRespiratorySupport(StringUtils.defaultString(transfer.getRespiratorySupport()));
		formData.setVentilationSettings(StringUtils.defaultString(transfer.getVentilationSettings()));
		formData.setBloodGasAnalysis(StringUtils.defaultString(transfer.getBloodGasAnalysis()));
		formData.setIvFluidVol(StringUtils.defaultString(transfer.getIvFluidVol()));
		formData.setPassedUrine(StringUtils.defaultString(transfer.getPassedUrine()));
		formData.setInotropes(StringUtils.defaultString(transfer.getInotropes()));
		formData.setInotropesSpecify(StringUtils.defaultString(transfer.getInotropesSpecify()));
		formData.setPeripheralIv(StringUtils.defaultString(transfer.getPeripheralIv()));
		formData.setCentralIv(StringUtils.defaultString(transfer.getCentralIv()));
		formData.setIntraosseousLine(StringUtils.defaultString(transfer.getIntraosseousLine()));
		formData.setAntibiotic1Name(StringUtils.defaultString(transfer.getAntibiotic1Name()));
		formData.setAntibiotic1Doses(StringUtils.defaultString(transfer.getAntibiotic1Doses()));
		formData.setAntibiotic1Durations(StringUtils.defaultString(transfer.getAntibiotic1Durations()));
		formData.setAntibiotic2Name(StringUtils.defaultString(transfer.getAntibiotic2Name()));
		formData.setAntibiotic2Doses(StringUtils.defaultString(transfer.getAntibiotic2Doses()));
		formData.setAntibiotic2Durations(StringUtils.defaultString(transfer.getAntibiotic2Durations()));
		formData.setArvs(StringUtils.defaultString(transfer.getArvs()));
		formData.setNpo(StringUtils.defaultString(transfer.getNpo()));
		formData.setLastFeedTime(StringUtils.defaultString(transfer.getLastFeedTime()));
		formData.setLastFeedAmount(StringUtils.defaultString(transfer.getLastFeedAmount()));
		formData.setFeedVol(StringUtils.defaultString(transfer.getFeedVol()));
		formData.setFeedType(StringUtils.defaultString(transfer.getFeedType()));
		formData.setPassedStool(StringUtils.defaultString(transfer.getPassedStool()));
		formData.setNasogastricTube(StringUtils.defaultString(transfer.getNasogastricTube()));
		formData.setLabGlucose(StringUtils.defaultString(transfer.getLabGlucose()));
		formData.setLabHb(StringUtils.defaultString(transfer.getLabHb()));
		formData.setLabWbc(StringUtils.defaultString(transfer.getLabWbc()));
		formData.setLabPlatelets(StringUtils.defaultString(transfer.getLabPlatelets()));
		formData.setLabCrp(StringUtils.defaultString(transfer.getLabCrp()));
		formData.setLabBiliTotal(StringUtils.defaultString(transfer.getLabBiliTotal()));
		formData.setLabBiliDirect(StringUtils.defaultString(transfer.getLabBiliDirect()));
		formData.setLabUe(StringUtils.defaultString(transfer.getLabUe()));
		formData.setLabCultures(StringUtils.defaultString(transfer.getLabCultures()));
		formData.setFbcDone(StringUtils.defaultString(transfer.getFbcDone()));
		formData.setImagingResultsAvailable(StringUtils.defaultString(transfer.getImagingResultsAvailable()));
		formData.setImagingResults(StringUtils.defaultString(transfer.getImagingResults()));
		formData.setPainSedationDrugs(StringUtils.defaultString(transfer.getPainSedationDrugs()));
		formData.setImagingReportAttached(booleanToFormValue(transfer.getImagingReportAttached()));
		formData.setLabReportsAttached(booleanToFormValue(transfer.getLabReportsAttached()));

		formData.setClinicalManagementSummary(StringUtils.defaultString(transfer.getClinicalManagementSummary()));
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
	}

	protected String booleanToFormValue(Boolean value) {
		return Boolean.TRUE.equals(value) ? "true" : "";
	}

	protected String formatDateTimeSpace(Date date) {
		return new SimpleDateFormat("yyyy-MM-dd HH:mm").format(date);
	}

	protected void prefillFromPatient(NeonatalTransferFormData formData, Patient patient) {
		if (patient.getPersonName() != null) {
			formData.setBabyName(patient.getPersonName().getFullName());
		}
		if (patient.getGender() != null) {
			formData.setSex(patient.getGender());
		}
		if (patient.getBirthdate() != null) {
			formData.setDob(formatDate(patient.getBirthdate()));
		}

		formData.setMotherName(patientSnapshotResolver.resolvePersonAttribute(patient,
				"Caregiver Name", "CaregiverName", "Name of caregiver", "Next of Kin", "NextOfKin", "Mother's Name"));
		formData.setMotherCaregiverPhone(patientSnapshotResolver.resolvePersonAttribute(patient,
				"Caregiver Telephone", "Caregiver Phone", "CaregiverPhone", "Next of Kin Telephone"));

		PersonAddress address = null;
		if (transferDao != null) {
			address = transferDao.getPreferredPersonAddress(patient.getPatientId());
		}
		if (address == null) {
			address = patientSnapshotResolver.resolveActivePersonAddress(patient);
		}
		if (address != null) {
			formData.setDistrict(patientSnapshotResolver.resolveDistrict(address));
		}
	}

	protected void prefillFromCurrentUser(NeonatalTransferFormData formData) {
		User user = Context.getAuthenticatedUser();
		if (user != null && user.getPerson() != null && user.getPerson().getPersonName() != null) {
			formData.setReferringProviderName(user.getPerson().getPersonName().getFullName());
		}
	}

	protected void prefillDefaults(NeonatalTransferFormData formData) {
		Date now = new Date();
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

	protected List<TransferFormOption> getTransportTypes() {
		return Arrays.asList(
				new TransferFormOption("AMBULANCE", "Ambulance"),
				new TransferFormOption("OTHER", "Other (specify)"),
				new TransferFormOption("NA", "NA"));
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
