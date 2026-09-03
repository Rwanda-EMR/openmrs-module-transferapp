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
 * MOH External Transfer Form wizard data — field names align with etransfer {@code TransferRecord}.
 */
public class NewTransferOutFormData {

	private Integer patientId;

	private String patientDisplay;

	private String sendingFacility;

	private List<TransferFormOption> identifierTypes;

	private List<TransferFormOption> receivingFacilities;

	private List<String> receivingServices;

	private List<TransferFormOption> transferTypes;

	private List<TransferFormOption> transportationTypes;

	private List<TransferFormOption> healthInsuranceTypes;

	// Step 1
	private String identifierType;

	private String identifierValue;

	private String clientName;

	private String serialNumberEmr;

	private String clientTelephone;

	private String ageOrDob;

	private String sex;

	private String caregiverName;

	private String caregiverTelephone;

	private String clientDistrict;

	private String sector;

	private String cell;

	private String village;

	private String admissionAt;

	// Step 2
	private String decisionToTransferAt;

	private String receivingFacilityCode;

	private String receivingService;

	private String callingTime;

	private String staffContactedName;

	private String staffContactedPhone;

	// Step 3
	private String transferType;

	private String ambulanceCalledTime;

	private String departureFromReferringTime;

	private String reasonForTransfer;

	private String clinicalPresentation;

	private String disabilityType;

	// Step 4
	private String vitalTemp;

	private String vitalSpo2;

	private String vitalRr;

	private String vitalPulse;

	private String vitalBp;

	private String vitalWeight;

	private String vitalHeight;

	private String vitalMuac;

	// Step 5
	private String laboratory;

	private String othersNotes;

	private String diagnosis;

	private String proceduresAndTreatments;

	private String transportationType;

	private String transportationOtherSpec;

	/** FOSA id of the facility that will provide the ambulance vehicle. */
	private String ambulanceProviderFosaId;

	private String ambulanceProviderName;

	/** Current logged-in facility FOSA ({@code transferapp.sendingFosaId}) for default selection. */
	private String currentSendingFosaId;

	// Step 6
	private String healthInsuranceType;

	private String healthInsuranceOtherSpec;

	private String referringProviderName;

	private String referringProviderQualification;

	private String referringSignedDate;

	private String referringSignedTime;

	private String referringProviderPhone;

	/** When set, the form is editing an existing transfer instead of creating a new one. */
	private String transferUuid;

	private Integer receivingFacilityId;

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

	public String getSendingFacility() {
		return sendingFacility;
	}

	public void setSendingFacility(String sendingFacility) {
		this.sendingFacility = sendingFacility;
	}

	public List<TransferFormOption> getIdentifierTypes() {
		return identifierTypes;
	}

	public void setIdentifierTypes(List<TransferFormOption> identifierTypes) {
		this.identifierTypes = identifierTypes;
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

	public String getIdentifierType() {
		return identifierType;
	}

	public void setIdentifierType(String identifierType) {
		this.identifierType = identifierType;
	}

	public String getIdentifierValue() {
		return identifierValue;
	}

	public void setIdentifierValue(String identifierValue) {
		this.identifierValue = identifierValue;
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

	public String getClientTelephone() {
		return clientTelephone;
	}

	public void setClientTelephone(String clientTelephone) {
		this.clientTelephone = clientTelephone;
	}

	public String getAgeOrDob() {
		return ageOrDob;
	}

	public void setAgeOrDob(String ageOrDob) {
		this.ageOrDob = ageOrDob;
	}

	public String getSex() {
		return sex;
	}

	public void setSex(String sex) {
		this.sex = sex;
	}

	public String getCaregiverName() {
		return caregiverName;
	}

	public void setCaregiverName(String caregiverName) {
		this.caregiverName = caregiverName;
	}

	public String getCaregiverTelephone() {
		return caregiverTelephone;
	}

	public void setCaregiverTelephone(String caregiverTelephone) {
		this.caregiverTelephone = caregiverTelephone;
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

	public String getReasonForTransfer() {
		return reasonForTransfer;
	}

	public void setReasonForTransfer(String reasonForTransfer) {
		this.reasonForTransfer = reasonForTransfer;
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

	public String getVitalBp() {
		return vitalBp;
	}

	public void setVitalBp(String vitalBp) {
		this.vitalBp = vitalBp;
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

	public String getVitalMuac() {
		return vitalMuac;
	}

	public void setVitalMuac(String vitalMuac) {
		this.vitalMuac = vitalMuac;
	}

	public String getLaboratory() {
		return laboratory;
	}

	public void setLaboratory(String laboratory) {
		this.laboratory = laboratory;
	}

	public String getOthersNotes() {
		return othersNotes;
	}

	public void setOthersNotes(String othersNotes) {
		this.othersNotes = othersNotes;
	}

	public String getDiagnosis() {
		return diagnosis;
	}

	public void setDiagnosis(String diagnosis) {
		this.diagnosis = diagnosis;
	}

	public String getProceduresAndTreatments() {
		return proceduresAndTreatments;
	}

	public void setProceduresAndTreatments(String proceduresAndTreatments) {
		this.proceduresAndTreatments = proceduresAndTreatments;
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

	public String getAmbulanceProviderFosaId() {
		return ambulanceProviderFosaId;
	}

	public void setAmbulanceProviderFosaId(String ambulanceProviderFosaId) {
		this.ambulanceProviderFosaId = ambulanceProviderFosaId;
	}

	public String getAmbulanceProviderName() {
		return ambulanceProviderName;
	}

	public void setAmbulanceProviderName(String ambulanceProviderName) {
		this.ambulanceProviderName = ambulanceProviderName;
	}

	public String getCurrentSendingFosaId() {
		return currentSendingFosaId;
	}

	public void setCurrentSendingFosaId(String currentSendingFosaId) {
		this.currentSendingFosaId = currentSendingFosaId;
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

	public String getTransferUuid() {
		return transferUuid;
	}

	public void setTransferUuid(String transferUuid) {
		this.transferUuid = transferUuid;
	}

	public Integer getReceivingFacilityId() {
		return receivingFacilityId;
	}

	public void setReceivingFacilityId(Integer receivingFacilityId) {
		this.receivingFacilityId = receivingFacilityId;
	}

	public boolean isEditing() {
		return transferUuid != null && transferUuid.trim().length() > 0;
	}

}
