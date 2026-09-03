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
 * MOH Maternity/ANC-Delivery-PNC Transfer Form wizard data.
 */
public class MaternityTransferFormData {

	private Integer patientId;

	private String patientDisplay;

	private String transferUuid;

	private String sendingFacility;

	private List<TransferFormOption> receivingFacilities;

	private List<String> receivingServices;

	private List<TransferFormOption> transferTypes;

	private List<TransferFormOption> transportationTypes;

	private List<TransferFormOption> healthInsuranceTypes;

	private List<MaternityTransferTreatmentRow> defaultTreatmentRows;

	// Facility details (header)
	private String province;

	private String district;

	private String hospitalName;

	private String referringFacilityName;

	private String referringUnit;

	// Step 1 — client & referral info
	private String clientName;

	private String serialNumberEmr;

	private String ageOrDob;

	private String nextOfKinName;

	private String nextOfKinTelephone;

	private String clientDistrict;

	private String sector;

	private String cell;

	private String village;

	private String admissionAt;

	private String decisionToTransferAt;

	private String receivingFacilityCode;

	private String receivingService;

	private String callingTime;

	private String staffContactedName;

	private String staffContactedPhone;

	private String reasonForTransfer;

	private String transferType;

	private String ambulanceCalledTime;

	private String departureFromReferringTime;

	private String clinicalPresentation;

	private String disabilityType;

	private String partographAttached;

	// Step 2 — obstetric history & current pregnancy
	private String obstetricGravida;

	private String obstetricParity;

	private String obstetricLivingChildren;

	private String obstetricAbortion;

	private String obstetricStillbirth;

	private String obstetricNeonatalDeath;

	private String obstetricPretermBirth;

	private String lmpDate;

	private String eddDate;

	private String gestationAge;

	private String muac;

	private String ancCompletedCount;

	private String tetanusVaccineDoses;

	private String previousSignificantHistory;

	private String multiPregnanciesAndKnownHiv;

	private String currentPregnancyComplications;

	// Step 3 — clinical findings
	private String latestHemoglobin;

	private String latestHivStatus;

	private String latestBloodGroup;

	private String latestOtherResults;

	private String vitalBp;

	private String vitalTemp;

	private String vitalSpo2;

	private String vitalRr;

	private String vitalPulse;

	private String vitalWeight;

	private String vitalHeight;

	private String fetalPresentation;

	private String fundalHeight;

	private String fetalHeartRate;

	private String contractions;

	private String vaginalExamAt;

	private String dilation;

	private String effacement;

	private String descent;

	private String consistency;

	private String position;

	private String caput;

	private String moulding;

	private String membranesRuptured;

	private String membranesRupturedAt;

	private String amnioticFluidColor;

	private String estimatedBloodLossMl;

	private String investigationHgb;

	private String investigationUrineTest;

	private String investigationOtherTest;

	private String imagingInvestigations;

	private String diagnosis;

	private String procedures;

	private String attachedLabTests;

	private String attachedImaging;

	private String attachedOther;

	// Step 4 — treatment & transport
	private String transportationType;

	private String transportationOtherSpec;

	// Step 5 — sign-off
	private String healthInsuranceType;

	private String healthInsuranceOtherSpec;

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

	public List<TransferFormOption> getTransportationTypes() {
		return transportationTypes;
	}

	public void setTransportationTypes(List<TransferFormOption> transportationTypes) {
		this.transportationTypes = transportationTypes;
	}

	public List<TransferFormOption> getHealthInsuranceTypes() {
		return healthInsuranceTypes;
	}

	public void setHealthInsuranceTypes(List<TransferFormOption> healthInsuranceTypes) {
		this.healthInsuranceTypes = healthInsuranceTypes;
	}

	public List<MaternityTransferTreatmentRow> getDefaultTreatmentRows() {
		return defaultTreatmentRows;
	}

	public void setDefaultTreatmentRows(List<MaternityTransferTreatmentRow> defaultTreatmentRows) {
		this.defaultTreatmentRows = defaultTreatmentRows;
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

	public String getClientName() {
		return clientName;
	}

	public void setClientName(String clientName) {
		this.clientName = clientName;
	}

	public String getSerialNumberEmr() {
		return serialNumberEmr;
	}

	public void setSerialNumberEmr(String serialNumberEmr) {
		this.serialNumberEmr = serialNumberEmr;
	}

	public String getAgeOrDob() {
		return ageOrDob;
	}

	public void setAgeOrDob(String ageOrDob) {
		this.ageOrDob = ageOrDob;
	}

	public String getNextOfKinName() {
		return nextOfKinName;
	}

	public void setNextOfKinName(String nextOfKinName) {
		this.nextOfKinName = nextOfKinName;
	}

	public String getNextOfKinTelephone() {
		return nextOfKinTelephone;
	}

	public void setNextOfKinTelephone(String nextOfKinTelephone) {
		this.nextOfKinTelephone = nextOfKinTelephone;
	}

	public String getClientDistrict() {
		return clientDistrict;
	}

	public void setClientDistrict(String clientDistrict) {
		this.clientDistrict = clientDistrict;
	}

	public String getSector() {
		return sector;
	}

	public void setSector(String sector) {
		this.sector = sector;
	}

	public String getCell() {
		return cell;
	}

	public void setCell(String cell) {
		this.cell = cell;
	}

	public String getVillage() {
		return village;
	}

	public void setVillage(String village) {
		this.village = village;
	}

	public String getAdmissionAt() {
		return admissionAt;
	}

	public void setAdmissionAt(String admissionAt) {
		this.admissionAt = admissionAt;
	}

	public String getDecisionToTransferAt() {
		return decisionToTransferAt;
	}

	public void setDecisionToTransferAt(String decisionToTransferAt) {
		this.decisionToTransferAt = decisionToTransferAt;
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

	public String getReasonForTransfer() {
		return reasonForTransfer;
	}

	public void setReasonForTransfer(String reasonForTransfer) {
		this.reasonForTransfer = reasonForTransfer;
	}

	public String getTransferType() {
		return transferType;
	}

	public void setTransferType(String transferType) {
		this.transferType = transferType;
	}

	public String getAmbulanceCalledTime() {
		return ambulanceCalledTime;
	}

	public void setAmbulanceCalledTime(String ambulanceCalledTime) {
		this.ambulanceCalledTime = ambulanceCalledTime;
	}

	public String getDepartureFromReferringTime() {
		return departureFromReferringTime;
	}

	public void setDepartureFromReferringTime(String departureFromReferringTime) {
		this.departureFromReferringTime = departureFromReferringTime;
	}

	public String getClinicalPresentation() {
		return clinicalPresentation;
	}

	public void setClinicalPresentation(String clinicalPresentation) {
		this.clinicalPresentation = clinicalPresentation;
	}

	public String getDisabilityType() {
		return disabilityType;
	}

	public void setDisabilityType(String disabilityType) {
		this.disabilityType = disabilityType;
	}

	public String getPartographAttached() {
		return partographAttached;
	}

	public void setPartographAttached(String partographAttached) {
		this.partographAttached = partographAttached;
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

	public String getObstetricLivingChildren() {
		return obstetricLivingChildren;
	}

	public void setObstetricLivingChildren(String obstetricLivingChildren) {
		this.obstetricLivingChildren = obstetricLivingChildren;
	}

	public String getObstetricAbortion() {
		return obstetricAbortion;
	}

	public void setObstetricAbortion(String obstetricAbortion) {
		this.obstetricAbortion = obstetricAbortion;
	}

	public String getObstetricStillbirth() {
		return obstetricStillbirth;
	}

	public void setObstetricStillbirth(String obstetricStillbirth) {
		this.obstetricStillbirth = obstetricStillbirth;
	}

	public String getObstetricNeonatalDeath() {
		return obstetricNeonatalDeath;
	}

	public void setObstetricNeonatalDeath(String obstetricNeonatalDeath) {
		this.obstetricNeonatalDeath = obstetricNeonatalDeath;
	}

	public String getObstetricPretermBirth() {
		return obstetricPretermBirth;
	}

	public void setObstetricPretermBirth(String obstetricPretermBirth) {
		this.obstetricPretermBirth = obstetricPretermBirth;
	}

	public String getLmpDate() {
		return lmpDate;
	}

	public void setLmpDate(String lmpDate) {
		this.lmpDate = lmpDate;
	}

	public String getEddDate() {
		return eddDate;
	}

	public void setEddDate(String eddDate) {
		this.eddDate = eddDate;
	}

	public String getGestationAge() {
		return gestationAge;
	}

	public void setGestationAge(String gestationAge) {
		this.gestationAge = gestationAge;
	}

	public String getMuac() {
		return muac;
	}

	public void setMuac(String muac) {
		this.muac = muac;
	}

	public String getAncCompletedCount() {
		return ancCompletedCount;
	}

	public void setAncCompletedCount(String ancCompletedCount) {
		this.ancCompletedCount = ancCompletedCount;
	}

	public String getTetanusVaccineDoses() {
		return tetanusVaccineDoses;
	}

	public void setTetanusVaccineDoses(String tetanusVaccineDoses) {
		this.tetanusVaccineDoses = tetanusVaccineDoses;
	}

	public String getPreviousSignificantHistory() {
		return previousSignificantHistory;
	}

	public void setPreviousSignificantHistory(String previousSignificantHistory) {
		this.previousSignificantHistory = previousSignificantHistory;
	}

	public String getMultiPregnanciesAndKnownHiv() {
		return multiPregnanciesAndKnownHiv;
	}

	public void setMultiPregnanciesAndKnownHiv(String multiPregnanciesAndKnownHiv) {
		this.multiPregnanciesAndKnownHiv = multiPregnanciesAndKnownHiv;
	}

	public String getCurrentPregnancyComplications() {
		return currentPregnancyComplications;
	}

	public void setCurrentPregnancyComplications(String currentPregnancyComplications) {
		this.currentPregnancyComplications = currentPregnancyComplications;
	}

	public String getLatestHemoglobin() {
		return latestHemoglobin;
	}

	public void setLatestHemoglobin(String latestHemoglobin) {
		this.latestHemoglobin = latestHemoglobin;
	}

	public String getLatestHivStatus() {
		return latestHivStatus;
	}

	public void setLatestHivStatus(String latestHivStatus) {
		this.latestHivStatus = latestHivStatus;
	}

	public String getLatestBloodGroup() {
		return latestBloodGroup;
	}

	public void setLatestBloodGroup(String latestBloodGroup) {
		this.latestBloodGroup = latestBloodGroup;
	}

	public String getLatestOtherResults() {
		return latestOtherResults;
	}

	public void setLatestOtherResults(String latestOtherResults) {
		this.latestOtherResults = latestOtherResults;
	}

	public String getVitalBp() {
		return vitalBp;
	}

	public void setVitalBp(String vitalBp) {
		this.vitalBp = vitalBp;
	}

	public String getVitalTemp() {
		return vitalTemp;
	}

	public void setVitalTemp(String vitalTemp) {
		this.vitalTemp = vitalTemp;
	}

	public String getVitalSpo2() {
		return vitalSpo2;
	}

	public void setVitalSpo2(String vitalSpo2) {
		this.vitalSpo2 = vitalSpo2;
	}

	public String getVitalRr() {
		return vitalRr;
	}

	public void setVitalRr(String vitalRr) {
		this.vitalRr = vitalRr;
	}

	public String getVitalPulse() {
		return vitalPulse;
	}

	public void setVitalPulse(String vitalPulse) {
		this.vitalPulse = vitalPulse;
	}

	public String getVitalWeight() {
		return vitalWeight;
	}

	public void setVitalWeight(String vitalWeight) {
		this.vitalWeight = vitalWeight;
	}

	public String getVitalHeight() {
		return vitalHeight;
	}

	public void setVitalHeight(String vitalHeight) {
		this.vitalHeight = vitalHeight;
	}

	public String getFetalPresentation() {
		return fetalPresentation;
	}

	public void setFetalPresentation(String fetalPresentation) {
		this.fetalPresentation = fetalPresentation;
	}

	public String getFundalHeight() {
		return fundalHeight;
	}

	public void setFundalHeight(String fundalHeight) {
		this.fundalHeight = fundalHeight;
	}

	public String getFetalHeartRate() {
		return fetalHeartRate;
	}

	public void setFetalHeartRate(String fetalHeartRate) {
		this.fetalHeartRate = fetalHeartRate;
	}

	public String getContractions() {
		return contractions;
	}

	public void setContractions(String contractions) {
		this.contractions = contractions;
	}

	public String getVaginalExamAt() {
		return vaginalExamAt;
	}

	public void setVaginalExamAt(String vaginalExamAt) {
		this.vaginalExamAt = vaginalExamAt;
	}

	public String getDilation() {
		return dilation;
	}

	public void setDilation(String dilation) {
		this.dilation = dilation;
	}

	public String getEffacement() {
		return effacement;
	}

	public void setEffacement(String effacement) {
		this.effacement = effacement;
	}

	public String getDescent() {
		return descent;
	}

	public void setDescent(String descent) {
		this.descent = descent;
	}

	public String getConsistency() {
		return consistency;
	}

	public void setConsistency(String consistency) {
		this.consistency = consistency;
	}

	public String getPosition() {
		return position;
	}

	public void setPosition(String position) {
		this.position = position;
	}

	public String getCaput() {
		return caput;
	}

	public void setCaput(String caput) {
		this.caput = caput;
	}

	public String getMoulding() {
		return moulding;
	}

	public void setMoulding(String moulding) {
		this.moulding = moulding;
	}

	public String getMembranesRuptured() {
		return membranesRuptured;
	}

	public void setMembranesRuptured(String membranesRuptured) {
		this.membranesRuptured = membranesRuptured;
	}

	public String getMembranesRupturedAt() {
		return membranesRupturedAt;
	}

	public void setMembranesRupturedAt(String membranesRupturedAt) {
		this.membranesRupturedAt = membranesRupturedAt;
	}

	public String getAmnioticFluidColor() {
		return amnioticFluidColor;
	}

	public void setAmnioticFluidColor(String amnioticFluidColor) {
		this.amnioticFluidColor = amnioticFluidColor;
	}

	public String getEstimatedBloodLossMl() {
		return estimatedBloodLossMl;
	}

	public void setEstimatedBloodLossMl(String estimatedBloodLossMl) {
		this.estimatedBloodLossMl = estimatedBloodLossMl;
	}

	public String getInvestigationHgb() {
		return investigationHgb;
	}

	public void setInvestigationHgb(String investigationHgb) {
		this.investigationHgb = investigationHgb;
	}

	public String getInvestigationUrineTest() {
		return investigationUrineTest;
	}

	public void setInvestigationUrineTest(String investigationUrineTest) {
		this.investigationUrineTest = investigationUrineTest;
	}

	public String getInvestigationOtherTest() {
		return investigationOtherTest;
	}

	public void setInvestigationOtherTest(String investigationOtherTest) {
		this.investigationOtherTest = investigationOtherTest;
	}

	public String getImagingInvestigations() {
		return imagingInvestigations;
	}

	public void setImagingInvestigations(String imagingInvestigations) {
		this.imagingInvestigations = imagingInvestigations;
	}

	public String getDiagnosis() {
		return diagnosis;
	}

	public void setDiagnosis(String diagnosis) {
		this.diagnosis = diagnosis;
	}

	public String getProcedures() {
		return procedures;
	}

	public void setProcedures(String procedures) {
		this.procedures = procedures;
	}

	public String getAttachedLabTests() {
		return attachedLabTests;
	}

	public void setAttachedLabTests(String attachedLabTests) {
		this.attachedLabTests = attachedLabTests;
	}

	public String getAttachedImaging() {
		return attachedImaging;
	}

	public void setAttachedImaging(String attachedImaging) {
		this.attachedImaging = attachedImaging;
	}

	public String getAttachedOther() {
		return attachedOther;
	}

	public void setAttachedOther(String attachedOther) {
		this.attachedOther = attachedOther;
	}

	public String getTransportationType() {
		return transportationType;
	}

	public void setTransportationType(String transportationType) {
		this.transportationType = transportationType;
	}

	public String getTransportationOtherSpec() {
		return transportationOtherSpec;
	}

	public void setTransportationOtherSpec(String transportationOtherSpec) {
		this.transportationOtherSpec = transportationOtherSpec;
	}

	public String getHealthInsuranceType() {
		return healthInsuranceType;
	}

	public void setHealthInsuranceType(String healthInsuranceType) {
		this.healthInsuranceType = healthInsuranceType;
	}

	public String getHealthInsuranceOtherSpec() {
		return healthInsuranceOtherSpec;
	}

	public void setHealthInsuranceOtherSpec(String healthInsuranceOtherSpec) {
		this.healthInsuranceOtherSpec = healthInsuranceOtherSpec;
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
