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
package org.openmrs.module.transferapp.model;

import java.util.List;

/**
 * MOH Neonatal Transfer Form wizard data.
 */
public class NeonatalTransferFormData {

	private Integer patientId;

	private String patientDisplay;

	private String transferUuid;

	private String sendingFacility;

	private List<TransferFormOption> receivingFacilities;

	private List<String> receivingServices;

	private List<TransferFormOption> transferTypes;

	private List<TransferFormOption> transportTypes;

	// Facility details (header)
	private String province;

	private String district;

	private String hospitalName;

	private String referringFacilityName;

	private String referringUnit;

	// Step 1 — baby & referral info
	private String babyName;

	private String sex;

	private String dob;

	private String gestationalAgeWeeks;

	private String birthWeightG;

	private String currentWeightG;

	private String currentAgeDays;

	private String motherName;

	private String motherAge;

	private String motherCaregiverPhone;

	private String placeOfBirth;

	private String reasonForTransfer;

	private String modeOfTransport;

	private String transportOther;

	private String transferType;

	private String receivingFacilityCode;

	private String receivingService;

	private String callingTime;

	private String staffContactedName;

	private String staffContactedPhone;

	private String decisionToTransferAt;

	// Step 2 — maternal history
	private String motherAlive;

	private String obstetricGravida;

	private String obstetricParity;

	private String pregnancyType;

	private String ancScreening;

	private String pathologiesDuringPregnancy;

	private String pregnancyOtherPathologies;

	private String pregnancyTreatment;

	private String bloodGroup;

	private String rhFactor;

	private String hivStatus;

	private String hivRegimen;

	private String hivRecentVl;

	private String hivCd4Count;

	private String hivOpportunisticInfections;

	private String tetanusVaccineDoses;

	private String maternalIllicitDrugHistory;

	// Step 3 — labor details
	private String romAt;

	private String afQuality;

	private String afQuantity;

	private String feverTiming;

	private String steroidDoses;

	private String lastSteroidDoseAt;

	private String mgso4At;

	private String modeOfDelivery;

	private String laborComplications;

	private String laborComplicationsOther;

	private String maternalAnesthesia;

	private String maternalAnesthesiaOther;

	private String maternalAntibiotics;

	private String otherDrugs;

	private String sepsisRiskFactors;

	// Step 4 — neonatal history & drugs
	private String resuscitationAtBirth;

	private String resuscitationMethods;

	private String apgar1min;

	private String apgar5min;

	private String apgar10min;

	private String hie;

	private String hieGrade;

	private String allergies;

	private String immunization;

	private String immunizationDetails;

	private String vitaminK;

	private String tetracyclineEyeOintment;

	private String surfactant;

	// Step 5 — chief complaint & diagnoses
	private String chiefComplaintDetails;

	private String spo2Preductal;

	private String spo2Postductal;

	private String conditionTemp;

	private String conditionHr;

	private String conditionRr;

	private String conditionBp;

	private String neurologicalStatus;

	private String seizures;

	private String adverseEvents24h;

	private String diagnosis1;

	private String diagnosis2;

	private String diagnosis3;

	private String diagnosis4;

	// Step 6 — management at referring facility
	private String respiratorySupport;

	private String ventilationSettings;

	private String bloodGasAnalysis;

	private String ivFluidVol;

	private String passedUrine;

	private String inotropes;

	private String inotropesSpecify;

	private String peripheralIv;

	private String centralIv;

	private String intraosseousLine;

	private String antibiotic1Name;

	private String antibiotic1Doses;

	private String antibiotic1Durations;

	private String antibiotic2Name;

	private String antibiotic2Doses;

	private String antibiotic2Durations;

	private String arvs;

	private String npo;

	private String lastFeedTime;

	private String lastFeedAmount;

	private String feedVol;

	private String feedType;

	private String passedStool;

	private String nasogastricTube;

	private String labGlucose;

	private String labFbc;

	private String labHb;

	private String labWbc;

	private String labPlatelets;

	private String labCrp;

	private String labBiliTotal;

	private String labBiliDirect;

	private String labUe;

	private String labCultures;

	private String fbcDone;

	private String imagingResultsAvailable;

	private String imagingResults;

	private String painSedationDrugs;

	private String imagingReportAttached;

	private String labReportsAttached;

	// Step 7 — summary & sign-off
	private String clinicalManagementSummary;

	private String referringProviderName;

	private String referringProviderQualification;

	private String referringSignedDate;

	private String referringSignedTime;

	private String referringProviderPhone;

	public Integer getPatientId() {
		return patientId;
	}

	public void setPatientId(Integer patientId) {
		this.patientId = patientId;
	}

	public String getPatientDisplay() {
		return patientDisplay;
	}

	public void setPatientDisplay(String patientDisplay) {
		this.patientDisplay = patientDisplay;
	}

	public String getTransferUuid() {
		return transferUuid;
	}

	public void setTransferUuid(String transferUuid) {
		this.transferUuid = transferUuid;
	}

	public String getSendingFacility() {
		return sendingFacility;
	}

	public void setSendingFacility(String sendingFacility) {
		this.sendingFacility = sendingFacility;
	}

	public List<TransferFormOption> getReceivingFacilities() {
		return receivingFacilities;
	}

	public void setReceivingFacilities(List<TransferFormOption> receivingFacilities) {
		this.receivingFacilities = receivingFacilities;
	}

	public List<String> getReceivingServices() {
		return receivingServices;
	}

	public void setReceivingServices(List<String> receivingServices) {
		this.receivingServices = receivingServices;
	}

	public List<TransferFormOption> getTransferTypes() {
		return transferTypes;
	}

	public void setTransferTypes(List<TransferFormOption> transferTypes) {
		this.transferTypes = transferTypes;
	}

	public List<TransferFormOption> getTransportTypes() {
		return transportTypes;
	}

	public void setTransportTypes(List<TransferFormOption> transportTypes) {
		this.transportTypes = transportTypes;
	}

	public String getProvince() {
		return province;
	}

	public void setProvince(String province) {
		this.province = province;
	}

	public String getDistrict() {
		return district;
	}

	public void setDistrict(String district) {
		this.district = district;
	}

	public String getHospitalName() {
		return hospitalName;
	}

	public void setHospitalName(String hospitalName) {
		this.hospitalName = hospitalName;
	}

	public String getReferringFacilityName() {
		return referringFacilityName;
	}

	public void setReferringFacilityName(String referringFacilityName) {
		this.referringFacilityName = referringFacilityName;
	}

	public String getReferringUnit() {
		return referringUnit;
	}

	public void setReferringUnit(String referringUnit) {
		this.referringUnit = referringUnit;
	}

	public String getBabyName() {
		return babyName;
	}

	public void setBabyName(String babyName) {
		this.babyName = babyName;
	}

	public String getSex() {
		return sex;
	}

	public void setSex(String sex) {
		this.sex = sex;
	}

	public String getDob() {
		return dob;
	}

	public void setDob(String dob) {
		this.dob = dob;
	}

	public String getGestationalAgeWeeks() {
		return gestationalAgeWeeks;
	}

	public void setGestationalAgeWeeks(String gestationalAgeWeeks) {
		this.gestationalAgeWeeks = gestationalAgeWeeks;
	}

	public String getBirthWeightG() {
		return birthWeightG;
	}

	public void setBirthWeightG(String birthWeightG) {
		this.birthWeightG = birthWeightG;
	}

	public String getCurrentWeightG() {
		return currentWeightG;
	}

	public void setCurrentWeightG(String currentWeightG) {
		this.currentWeightG = currentWeightG;
	}

	public String getCurrentAgeDays() {
		return currentAgeDays;
	}

	public void setCurrentAgeDays(String currentAgeDays) {
		this.currentAgeDays = currentAgeDays;
	}

	public String getMotherName() {
		return motherName;
	}

	public void setMotherName(String motherName) {
		this.motherName = motherName;
	}

	public String getMotherAge() {
		return motherAge;
	}

	public void setMotherAge(String motherAge) {
		this.motherAge = motherAge;
	}

	public String getMotherCaregiverPhone() {
		return motherCaregiverPhone;
	}

	public void setMotherCaregiverPhone(String motherCaregiverPhone) {
		this.motherCaregiverPhone = motherCaregiverPhone;
	}

	public String getPlaceOfBirth() {
		return placeOfBirth;
	}

	public void setPlaceOfBirth(String placeOfBirth) {
		this.placeOfBirth = placeOfBirth;
	}

	public String getReasonForTransfer() {
		return reasonForTransfer;
	}

	public void setReasonForTransfer(String reasonForTransfer) {
		this.reasonForTransfer = reasonForTransfer;
	}

	public String getModeOfTransport() {
		return modeOfTransport;
	}

	public void setModeOfTransport(String modeOfTransport) {
		this.modeOfTransport = modeOfTransport;
	}

	public String getTransportOther() {
		return transportOther;
	}

	public void setTransportOther(String transportOther) {
		this.transportOther = transportOther;
	}

	public String getTransferType() {
		return transferType;
	}

	public void setTransferType(String transferType) {
		this.transferType = transferType;
	}

	public String getReceivingFacilityCode() {
		return receivingFacilityCode;
	}

	public void setReceivingFacilityCode(String receivingFacilityCode) {
		this.receivingFacilityCode = receivingFacilityCode;
	}

	public String getReceivingService() {
		return receivingService;
	}

	public void setReceivingService(String receivingService) {
		this.receivingService = receivingService;
	}

	public String getCallingTime() {
		return callingTime;
	}

	public void setCallingTime(String callingTime) {
		this.callingTime = callingTime;
	}

	public String getStaffContactedName() {
		return staffContactedName;
	}

	public void setStaffContactedName(String staffContactedName) {
		this.staffContactedName = staffContactedName;
	}

	public String getStaffContactedPhone() {
		return staffContactedPhone;
	}

	public void setStaffContactedPhone(String staffContactedPhone) {
		this.staffContactedPhone = staffContactedPhone;
	}

	public String getDecisionToTransferAt() {
		return decisionToTransferAt;
	}

	public void setDecisionToTransferAt(String decisionToTransferAt) {
		this.decisionToTransferAt = decisionToTransferAt;
	}

	public String getMotherAlive() {
		return motherAlive;
	}

	public void setMotherAlive(String motherAlive) {
		this.motherAlive = motherAlive;
	}

	public String getObstetricGravida() {
		return obstetricGravida;
	}

	public void setObstetricGravida(String obstetricGravida) {
		this.obstetricGravida = obstetricGravida;
	}

	public String getObstetricParity() {
		return obstetricParity;
	}

	public void setObstetricParity(String obstetricParity) {
		this.obstetricParity = obstetricParity;
	}

	public String getPregnancyType() {
		return pregnancyType;
	}

	public void setPregnancyType(String pregnancyType) {
		this.pregnancyType = pregnancyType;
	}

	public String getAncScreening() {
		return ancScreening;
	}

	public void setAncScreening(String ancScreening) {
		this.ancScreening = ancScreening;
	}

	public String getPathologiesDuringPregnancy() {
		return pathologiesDuringPregnancy;
	}

	public void setPathologiesDuringPregnancy(String pathologiesDuringPregnancy) {
		this.pathologiesDuringPregnancy = pathologiesDuringPregnancy;
	}

	public String getPregnancyOtherPathologies() {
		return pregnancyOtherPathologies;
	}

	public void setPregnancyOtherPathologies(String pregnancyOtherPathologies) {
		this.pregnancyOtherPathologies = pregnancyOtherPathologies;
	}

	public String getPregnancyTreatment() {
		return pregnancyTreatment;
	}

	public void setPregnancyTreatment(String pregnancyTreatment) {
		this.pregnancyTreatment = pregnancyTreatment;
	}

	public String getBloodGroup() {
		return bloodGroup;
	}

	public void setBloodGroup(String bloodGroup) {
		this.bloodGroup = bloodGroup;
	}

	public String getRhFactor() {
		return rhFactor;
	}

	public void setRhFactor(String rhFactor) {
		this.rhFactor = rhFactor;
	}

	public String getHivStatus() {
		return hivStatus;
	}

	public void setHivStatus(String hivStatus) {
		this.hivStatus = hivStatus;
	}

	public String getHivRegimen() {
		return hivRegimen;
	}

	public void setHivRegimen(String hivRegimen) {
		this.hivRegimen = hivRegimen;
	}

	public String getHivRecentVl() {
		return hivRecentVl;
	}

	public void setHivRecentVl(String hivRecentVl) {
		this.hivRecentVl = hivRecentVl;
	}

	public String getHivCd4Count() {
		return hivCd4Count;
	}

	public void setHivCd4Count(String hivCd4Count) {
		this.hivCd4Count = hivCd4Count;
	}

	public String getHivOpportunisticInfections() {
		return hivOpportunisticInfections;
	}

	public void setHivOpportunisticInfections(String hivOpportunisticInfections) {
		this.hivOpportunisticInfections = hivOpportunisticInfections;
	}

	public String getTetanusVaccineDoses() {
		return tetanusVaccineDoses;
	}

	public void setTetanusVaccineDoses(String tetanusVaccineDoses) {
		this.tetanusVaccineDoses = tetanusVaccineDoses;
	}

	public String getMaternalIllicitDrugHistory() {
		return maternalIllicitDrugHistory;
	}

	public void setMaternalIllicitDrugHistory(String maternalIllicitDrugHistory) {
		this.maternalIllicitDrugHistory = maternalIllicitDrugHistory;
	}

	public String getRomAt() {
		return romAt;
	}

	public void setRomAt(String romAt) {
		this.romAt = romAt;
	}

	public String getAfQuality() {
		return afQuality;
	}

	public void setAfQuality(String afQuality) {
		this.afQuality = afQuality;
	}

	public String getAfQuantity() {
		return afQuantity;
	}

	public void setAfQuantity(String afQuantity) {
		this.afQuantity = afQuantity;
	}

	public String getFeverTiming() {
		return feverTiming;
	}

	public void setFeverTiming(String feverTiming) {
		this.feverTiming = feverTiming;
	}

	public String getSteroidDoses() {
		return steroidDoses;
	}

	public void setSteroidDoses(String steroidDoses) {
		this.steroidDoses = steroidDoses;
	}

	public String getLastSteroidDoseAt() {
		return lastSteroidDoseAt;
	}

	public void setLastSteroidDoseAt(String lastSteroidDoseAt) {
		this.lastSteroidDoseAt = lastSteroidDoseAt;
	}

	public String getMgso4At() {
		return mgso4At;
	}

	public void setMgso4At(String mgso4At) {
		this.mgso4At = mgso4At;
	}

	public String getModeOfDelivery() {
		return modeOfDelivery;
	}

	public void setModeOfDelivery(String modeOfDelivery) {
		this.modeOfDelivery = modeOfDelivery;
	}

	public String getLaborComplications() {
		return laborComplications;
	}

	public void setLaborComplications(String laborComplications) {
		this.laborComplications = laborComplications;
	}

	public String getLaborComplicationsOther() {
		return laborComplicationsOther;
	}

	public void setLaborComplicationsOther(String laborComplicationsOther) {
		this.laborComplicationsOther = laborComplicationsOther;
	}

	public String getMaternalAnesthesia() {
		return maternalAnesthesia;
	}

	public void setMaternalAnesthesia(String maternalAnesthesia) {
		this.maternalAnesthesia = maternalAnesthesia;
	}

	public String getMaternalAnesthesiaOther() {
		return maternalAnesthesiaOther;
	}

	public void setMaternalAnesthesiaOther(String maternalAnesthesiaOther) {
		this.maternalAnesthesiaOther = maternalAnesthesiaOther;
	}

	public String getMaternalAntibiotics() {
		return maternalAntibiotics;
	}

	public void setMaternalAntibiotics(String maternalAntibiotics) {
		this.maternalAntibiotics = maternalAntibiotics;
	}

	public String getOtherDrugs() {
		return otherDrugs;
	}

	public void setOtherDrugs(String otherDrugs) {
		this.otherDrugs = otherDrugs;
	}

	public String getSepsisRiskFactors() {
		return sepsisRiskFactors;
	}

	public void setSepsisRiskFactors(String sepsisRiskFactors) {
		this.sepsisRiskFactors = sepsisRiskFactors;
	}

	public String getResuscitationAtBirth() {
		return resuscitationAtBirth;
	}

	public void setResuscitationAtBirth(String resuscitationAtBirth) {
		this.resuscitationAtBirth = resuscitationAtBirth;
	}

	public String getResuscitationMethods() {
		return resuscitationMethods;
	}

	public void setResuscitationMethods(String resuscitationMethods) {
		this.resuscitationMethods = resuscitationMethods;
	}

	public String getApgar1min() {
		return apgar1min;
	}

	public void setApgar1min(String apgar1min) {
		this.apgar1min = apgar1min;
	}

	public String getApgar5min() {
		return apgar5min;
	}

	public void setApgar5min(String apgar5min) {
		this.apgar5min = apgar5min;
	}

	public String getApgar10min() {
		return apgar10min;
	}

	public void setApgar10min(String apgar10min) {
		this.apgar10min = apgar10min;
	}

	public String getHie() {
		return hie;
	}

	public void setHie(String hie) {
		this.hie = hie;
	}

	public String getHieGrade() {
		return hieGrade;
	}

	public void setHieGrade(String hieGrade) {
		this.hieGrade = hieGrade;
	}

	public String getAllergies() {
		return allergies;
	}

	public void setAllergies(String allergies) {
		this.allergies = allergies;
	}

	public String getImmunization() {
		return immunization;
	}

	public void setImmunization(String immunization) {
		this.immunization = immunization;
	}

	public String getImmunizationDetails() {
		return immunizationDetails;
	}

	public void setImmunizationDetails(String immunizationDetails) {
		this.immunizationDetails = immunizationDetails;
	}

	public String getVitaminK() {
		return vitaminK;
	}

	public void setVitaminK(String vitaminK) {
		this.vitaminK = vitaminK;
	}

	public String getTetracyclineEyeOintment() {
		return tetracyclineEyeOintment;
	}

	public void setTetracyclineEyeOintment(String tetracyclineEyeOintment) {
		this.tetracyclineEyeOintment = tetracyclineEyeOintment;
	}

	public String getSurfactant() {
		return surfactant;
	}

	public void setSurfactant(String surfactant) {
		this.surfactant = surfactant;
	}

	public String getChiefComplaintDetails() {
		return chiefComplaintDetails;
	}

	public void setChiefComplaintDetails(String chiefComplaintDetails) {
		this.chiefComplaintDetails = chiefComplaintDetails;
	}

	public String getSpo2Preductal() {
		return spo2Preductal;
	}

	public void setSpo2Preductal(String spo2Preductal) {
		this.spo2Preductal = spo2Preductal;
	}

	public String getSpo2Postductal() {
		return spo2Postductal;
	}

	public void setSpo2Postductal(String spo2Postductal) {
		this.spo2Postductal = spo2Postductal;
	}

	public String getConditionTemp() {
		return conditionTemp;
	}

	public void setConditionTemp(String conditionTemp) {
		this.conditionTemp = conditionTemp;
	}

	public String getConditionHr() {
		return conditionHr;
	}

	public void setConditionHr(String conditionHr) {
		this.conditionHr = conditionHr;
	}

	public String getConditionRr() {
		return conditionRr;
	}

	public void setConditionRr(String conditionRr) {
		this.conditionRr = conditionRr;
	}

	public String getConditionBp() {
		return conditionBp;
	}

	public void setConditionBp(String conditionBp) {
		this.conditionBp = conditionBp;
	}

	public String getNeurologicalStatus() {
		return neurologicalStatus;
	}

	public void setNeurologicalStatus(String neurologicalStatus) {
		this.neurologicalStatus = neurologicalStatus;
	}

	public String getSeizures() {
		return seizures;
	}

	public void setSeizures(String seizures) {
		this.seizures = seizures;
	}

	public String getAdverseEvents24h() {
		return adverseEvents24h;
	}

	public void setAdverseEvents24h(String adverseEvents24h) {
		this.adverseEvents24h = adverseEvents24h;
	}

	public String getDiagnosis1() {
		return diagnosis1;
	}

	public void setDiagnosis1(String diagnosis1) {
		this.diagnosis1 = diagnosis1;
	}

	public String getDiagnosis2() {
		return diagnosis2;
	}

	public void setDiagnosis2(String diagnosis2) {
		this.diagnosis2 = diagnosis2;
	}

	public String getDiagnosis3() {
		return diagnosis3;
	}

	public void setDiagnosis3(String diagnosis3) {
		this.diagnosis3 = diagnosis3;
	}

	public String getDiagnosis4() {
		return diagnosis4;
	}

	public void setDiagnosis4(String diagnosis4) {
		this.diagnosis4 = diagnosis4;
	}

	public String getRespiratorySupport() {
		return respiratorySupport;
	}

	public void setRespiratorySupport(String respiratorySupport) {
		this.respiratorySupport = respiratorySupport;
	}

	public String getVentilationSettings() {
		return ventilationSettings;
	}

	public void setVentilationSettings(String ventilationSettings) {
		this.ventilationSettings = ventilationSettings;
	}

	public String getBloodGasAnalysis() {
		return bloodGasAnalysis;
	}

	public void setBloodGasAnalysis(String bloodGasAnalysis) {
		this.bloodGasAnalysis = bloodGasAnalysis;
	}

	public String getIvFluidVol() {
		return ivFluidVol;
	}

	public void setIvFluidVol(String ivFluidVol) {
		this.ivFluidVol = ivFluidVol;
	}

	public String getPassedUrine() {
		return passedUrine;
	}

	public void setPassedUrine(String passedUrine) {
		this.passedUrine = passedUrine;
	}

	public String getInotropes() {
		return inotropes;
	}

	public void setInotropes(String inotropes) {
		this.inotropes = inotropes;
	}

	public String getInotropesSpecify() {
		return inotropesSpecify;
	}

	public void setInotropesSpecify(String inotropesSpecify) {
		this.inotropesSpecify = inotropesSpecify;
	}

	public String getPeripheralIv() {
		return peripheralIv;
	}

	public void setPeripheralIv(String peripheralIv) {
		this.peripheralIv = peripheralIv;
	}

	public String getCentralIv() {
		return centralIv;
	}

	public void setCentralIv(String centralIv) {
		this.centralIv = centralIv;
	}

	public String getIntraosseousLine() {
		return intraosseousLine;
	}

	public void setIntraosseousLine(String intraosseousLine) {
		this.intraosseousLine = intraosseousLine;
	}

	public String getAntibiotic1Name() {
		return antibiotic1Name;
	}

	public void setAntibiotic1Name(String antibiotic1Name) {
		this.antibiotic1Name = antibiotic1Name;
	}

	public String getAntibiotic1Doses() {
		return antibiotic1Doses;
	}

	public void setAntibiotic1Doses(String antibiotic1Doses) {
		this.antibiotic1Doses = antibiotic1Doses;
	}

	public String getAntibiotic1Durations() {
		return antibiotic1Durations;
	}

	public void setAntibiotic1Durations(String antibiotic1Durations) {
		this.antibiotic1Durations = antibiotic1Durations;
	}

	public String getAntibiotic2Name() {
		return antibiotic2Name;
	}

	public void setAntibiotic2Name(String antibiotic2Name) {
		this.antibiotic2Name = antibiotic2Name;
	}

	public String getAntibiotic2Doses() {
		return antibiotic2Doses;
	}

	public void setAntibiotic2Doses(String antibiotic2Doses) {
		this.antibiotic2Doses = antibiotic2Doses;
	}

	public String getAntibiotic2Durations() {
		return antibiotic2Durations;
	}

	public void setAntibiotic2Durations(String antibiotic2Durations) {
		this.antibiotic2Durations = antibiotic2Durations;
	}

	public String getArvs() {
		return arvs;
	}

	public void setArvs(String arvs) {
		this.arvs = arvs;
	}

	public String getNpo() {
		return npo;
	}

	public void setNpo(String npo) {
		this.npo = npo;
	}

	public String getLastFeedTime() {
		return lastFeedTime;
	}

	public void setLastFeedTime(String lastFeedTime) {
		this.lastFeedTime = lastFeedTime;
	}

	public String getLastFeedAmount() {
		return lastFeedAmount;
	}

	public void setLastFeedAmount(String lastFeedAmount) {
		this.lastFeedAmount = lastFeedAmount;
	}

	public String getFeedVol() {
		return feedVol;
	}

	public void setFeedVol(String feedVol) {
		this.feedVol = feedVol;
	}

	public String getFeedType() {
		return feedType;
	}

	public void setFeedType(String feedType) {
		this.feedType = feedType;
	}

	public String getPassedStool() {
		return passedStool;
	}

	public void setPassedStool(String passedStool) {
		this.passedStool = passedStool;
	}

	public String getNasogastricTube() {
		return nasogastricTube;
	}

	public void setNasogastricTube(String nasogastricTube) {
		this.nasogastricTube = nasogastricTube;
	}

	public String getLabGlucose() {
		return labGlucose;
	}

	public void setLabGlucose(String labGlucose) {
		this.labGlucose = labGlucose;
	}

	public String getLabFbc() {
		return labFbc;
	}

	public void setLabFbc(String labFbc) {
		this.labFbc = labFbc;
	}

	public String getLabHb() {
		return labHb;
	}

	public void setLabHb(String labHb) {
		this.labHb = labHb;
	}

	public String getLabWbc() {
		return labWbc;
	}

	public void setLabWbc(String labWbc) {
		this.labWbc = labWbc;
	}

	public String getLabPlatelets() {
		return labPlatelets;
	}

	public void setLabPlatelets(String labPlatelets) {
		this.labPlatelets = labPlatelets;
	}

	public String getLabCrp() {
		return labCrp;
	}

	public void setLabCrp(String labCrp) {
		this.labCrp = labCrp;
	}

	public String getLabBiliTotal() {
		return labBiliTotal;
	}

	public void setLabBiliTotal(String labBiliTotal) {
		this.labBiliTotal = labBiliTotal;
	}

	public String getLabBiliDirect() {
		return labBiliDirect;
	}

	public void setLabBiliDirect(String labBiliDirect) {
		this.labBiliDirect = labBiliDirect;
	}

	public String getLabUe() {
		return labUe;
	}

	public void setLabUe(String labUe) {
		this.labUe = labUe;
	}

	public String getLabCultures() {
		return labCultures;
	}

	public void setLabCultures(String labCultures) {
		this.labCultures = labCultures;
	}

	public String getFbcDone() {
		return fbcDone;
	}

	public void setFbcDone(String fbcDone) {
		this.fbcDone = fbcDone;
	}

	public String getImagingResultsAvailable() {
		return imagingResultsAvailable;
	}

	public void setImagingResultsAvailable(String imagingResultsAvailable) {
		this.imagingResultsAvailable = imagingResultsAvailable;
	}

	public String getImagingResults() {
		return imagingResults;
	}

	public void setImagingResults(String imagingResults) {
		this.imagingResults = imagingResults;
	}

	public String getPainSedationDrugs() {
		return painSedationDrugs;
	}

	public void setPainSedationDrugs(String painSedationDrugs) {
		this.painSedationDrugs = painSedationDrugs;
	}

	public String getImagingReportAttached() {
		return imagingReportAttached;
	}

	public void setImagingReportAttached(String imagingReportAttached) {
		this.imagingReportAttached = imagingReportAttached;
	}

	public String getLabReportsAttached() {
		return labReportsAttached;
	}

	public void setLabReportsAttached(String labReportsAttached) {
		this.labReportsAttached = labReportsAttached;
	}

	public String getClinicalManagementSummary() {
		return clinicalManagementSummary;
	}

	public void setClinicalManagementSummary(String clinicalManagementSummary) {
		this.clinicalManagementSummary = clinicalManagementSummary;
	}

	public String getReferringProviderName() {
		return referringProviderName;
	}

	public void setReferringProviderName(String referringProviderName) {
		this.referringProviderName = referringProviderName;
	}

	public String getReferringProviderQualification() {
		return referringProviderQualification;
	}

	public void setReferringProviderQualification(String referringProviderQualification) {
		this.referringProviderQualification = referringProviderQualification;
	}

	public String getReferringSignedDate() {
		return referringSignedDate;
	}

	public void setReferringSignedDate(String referringSignedDate) {
		this.referringSignedDate = referringSignedDate;
	}

	public String getReferringSignedTime() {
		return referringSignedTime;
	}

	public void setReferringSignedTime(String referringSignedTime) {
		this.referringSignedTime = referringSignedTime;
	}

	public String getReferringProviderPhone() {
		return referringProviderPhone;
	}

	public void setReferringProviderPhone(String referringProviderPhone) {
		this.referringProviderPhone = referringProviderPhone;
	}

}
