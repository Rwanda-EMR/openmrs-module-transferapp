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

import org.openmrs.BaseOpenmrsData;
import org.openmrs.Patient;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Persisted outbound Maternity/ANC-Delivery-PNC transfer referral record.
 */
@Entity(name = "TransferappMaternityTransfer")
@Table(name = "maternity_transfers")
public class MaternityTransfer extends BaseOpenmrsData {

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "maternity_transfer_id")
	private Integer maternityTransferId;

	@ManyToOne(optional = false)
	@JoinColumn(name = "patient_id", nullable = false)
	private Patient patient;

	// Facility details (header)

	@Column(name = "province", length = 255)
	private String province;

	@Column(name = "district", length = 255)
	private String district;

	@Column(name = "hospital_name", length = 255)
	private String hospitalName;

	@Column(name = "referring_facility_name", length = 255)
	private String referringFacilityName;

	@Column(name = "referring_unit", length = 255)
	private String referringUnit;

	// Client / referral (step 1)

	@Column(name = "client_name", length = 255)
	private String clientName;

	@Column(name = "serial_number_emr", length = 64)
	private String serialNumberEmr;

	@Column(name = "age_or_dob", length = 64)
	private String ageOrDob;

	@Column(name = "next_of_kin_name", length = 255)
	private String nextOfKinName;

	@Column(name = "next_of_kin_telephone", length = 64)
	private String nextOfKinTelephone;

	@Column(name = "client_district", length = 120)
	private String clientDistrict;

	@Column(name = "sector", length = 120)
	private String sector;

	@Column(name = "cell", length = 120)
	private String cell;

	@Column(name = "village", length = 120)
	private String village;

	@Column(name = "admission_at")
	private Date admissionAt;

	@Column(name = "decision_to_transfer_at")
	private Date decisionToTransferAt;

	@Column(name = "receiving_facility_code", length = 64)
	private String receivingFacilityCode;

	@Column(name = "receiving_service", length = 255)
	private String receivingService;

	@Column(name = "calling_time", length = 8)
	private String callingTime;

	@Column(name = "staff_contacted_name", length = 255)
	private String staffContactedName;

	@Column(name = "staff_contacted_phone", length = 64)
	private String staffContactedPhone;

	@Column(name = "reason_for_transfer")
	private String reasonForTransfer;

	@Column(name = "transfer_type", length = 32)
	private String transferType;

	@Column(name = "ambulance_called_time", length = 8)
	private String ambulanceCalledTime;

	@Column(name = "departure_from_referring_time", length = 8)
	private String departureFromReferringTime;

	@Column(name = "partograph_attached")
	private Boolean partographAttached;

	@Column(name = "clinical_presentation")
	private String clinicalPresentation;

	@Column(name = "disability_type", length = 255)
	private String disabilityType;

	/** Session (sending) facility snapshot — scopes outbound queries the same way Transfer.sendingFacility does. */
	@Column(name = "sending_facility", length = 255)
	private String sendingFacility;

	@Column(name = "hie_sent")
	private Boolean hieSent;

	@Column(name = "hie_sent_at")
	private Date hieSentAt;

	@Column(name = "hie_send_err", length = 500)
	private String hieSendError;

	@Column(name = "hie_transfer_id", length = 64)
	private String hieTransferId;

	// Obstetric history & current pregnancy (step 2)

	@Column(name = "obstetric_gravida", length = 16)
	private String obstetricGravida;

	@Column(name = "obstetric_parity", length = 16)
	private String obstetricParity;

	@Column(name = "obstetric_living_children", length = 16)
	private String obstetricLivingChildren;

	@Column(name = "obstetric_abortion", length = 16)
	private String obstetricAbortion;

	@Column(name = "obstetric_stillbirth", length = 16)
	private String obstetricStillbirth;

	@Column(name = "obstetric_neonatal_death", length = 16)
	private String obstetricNeonatalDeath;

	@Column(name = "obstetric_preterm_birth", length = 16)
	private String obstetricPretermBirth;

	@Column(name = "lmp_date")
	private Date lmpDate;

	@Column(name = "edd_date")
	private Date eddDate;

	@Column(name = "gestation_age", length = 32)
	private String gestationAge;

	@Column(name = "muac", length = 32)
	private String muac;

	@Column(name = "anc_completed_count", length = 16)
	private String ancCompletedCount;

	@Column(name = "tetanus_vaccine_doses", length = 16)
	private String tetanusVaccineDoses;

	@Column(name = "previous_significant_history")
	private String previousSignificantHistory;

	@Column(name = "multi_pregnancies_known_hiv")
	private String multiPregnanciesAndKnownHiv;

	@Column(name = "current_pregnancy_complications")
	private String currentPregnancyComplications;

	// Clinical findings (step 3)

	@Column(name = "latest_hemoglobin", length = 32)
	private String latestHemoglobin;

	@Column(name = "latest_hiv_status", length = 32)
	private String latestHivStatus;

	@Column(name = "latest_blood_group", length = 16)
	private String latestBloodGroup;

	@Column(name = "latest_other_results")
	private String latestOtherResults;

	@Column(name = "vital_bp", length = 32)
	private String vitalBp;

	@Column(name = "vital_temp", length = 32)
	private String vitalTemp;

	@Column(name = "vital_spo2", length = 32)
	private String vitalSpo2;

	@Column(name = "vital_rr", length = 32)
	private String vitalRr;

	@Column(name = "vital_pulse", length = 32)
	private String vitalPulse;

	@Column(name = "vital_weight", length = 32)
	private String vitalWeight;

	@Column(name = "vital_height", length = 32)
	private String vitalHeight;

	@Column(name = "fetal_presentation", length = 64)
	private String fetalPresentation;

	@Column(name = "fundal_height", length = 32)
	private String fundalHeight;

	@Column(name = "fetal_heart_rate", length = 32)
	private String fetalHeartRate;

	@Column(name = "contractions", length = 64)
	private String contractions;

	@Column(name = "vaginal_exam_at")
	private Date vaginalExamAt;

	@Column(name = "dilation", length = 16)
	private String dilation;

	@Column(name = "effacement", length = 16)
	private String effacement;

	@Column(name = "descent", length = 16)
	private String descent;

	@Column(name = "consistency", length = 32)
	private String consistency;

	@Column(name = "position", length = 32)
	private String position;

	@Column(name = "caput")
	private Boolean caput;

	@Column(name = "moulding")
	private Boolean moulding;

	@Column(name = "membranes_ruptured")
	private Boolean membranesRuptured;

	@Column(name = "membranes_ruptured_at")
	private Date membranesRupturedAt;

	@Column(name = "amniotic_fluid_color", length = 64)
	private String amnioticFluidColor;

	@Column(name = "offensive")
	private Boolean offensive;

	@Column(name = "estimated_blood_loss_ml", length = 16)
	private String estimatedBloodLossMl;

	@Column(name = "investigation_hgb", length = 32)
	private String investigationHgb;

	@Column(name = "investigation_urine_test", length = 64)
	private String investigationUrineTest;

	@Column(name = "investigation_other_test", length = 64)
	private String investigationOtherTest;

	@Column(name = "imaging_investigations")
	private String imagingInvestigations;

	@Column(name = "diagnosis")
	private String diagnosis;

	@Column(name = "procedures")
	private String procedures;

	@Column(name = "attached_lab_tests")
	private Boolean attachedLabTests;

	@Column(name = "attached_imaging")
	private Boolean attachedImaging;

	@Column(name = "attached_other")
	private String attachedOther;

	// Transport / insurance (step 4)

	@Column(name = "transport_type", length = 16)
	private String transportType;

	@Column(name = "transport_other", length = 255)
	private String transportOther;

	@Column(name = "health_insurance_type", length = 16)
	private String healthInsuranceType;

	@Column(name = "health_insurance_other", length = 255)
	private String healthInsuranceOther;

	// Sign-off (step 5)

	@Column(name = "referring_provider_name", length = 255)
	private String referringProviderName;

	@Column(name = "referring_provider_qualification", length = 255)
	private String referringProviderQualification;

	@Column(name = "referring_signed_date")
	private Date referringSignedDate;

	@Column(name = "referring_signed_time", length = 8)
	private String referringSignedTime;

	@Column(name = "referring_provider_phone", length = 64)
	private String referringProviderPhone;

	@OneToMany(mappedBy = "maternityTransfer", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	@OrderBy("maternityTransferTreatmentId asc")
	private List<MaternityTransferTreatment> treatments = new ArrayList<MaternityTransferTreatment>();

	@Override
	public Integer getId() {
		return getMaternityTransferId();
	}

	@Override
	public void setId(Integer id) {
		setMaternityTransferId(id);
	}

	public Integer getMaternityTransferId() {
		return maternityTransferId;
	}

	public void setMaternityTransferId(Integer maternityTransferId) {
		this.maternityTransferId = maternityTransferId;
	}

	public Patient getPatient() {
		return patient;
	}

	public void setPatient(Patient patient) {
		this.patient = patient;
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

	public Date getAdmissionAt() {
		return admissionAt;
	}

	public void setAdmissionAt(Date admissionAt) {
		this.admissionAt = admissionAt;
	}

	public Date getDecisionToTransferAt() {
		return decisionToTransferAt;
	}

	public void setDecisionToTransferAt(Date decisionToTransferAt) {
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

	public Boolean getPartographAttached() {
		return partographAttached;
	}

	public void setPartographAttached(Boolean partographAttached) {
		this.partographAttached = partographAttached;
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

	public String getSendingFacility() {
		return sendingFacility;
	}

	public void setSendingFacility(String sendingFacility) {
		this.sendingFacility = sendingFacility;
	}

	public Boolean getHieSent() {
		return hieSent;
	}

	public void setHieSent(Boolean hieSent) {
		this.hieSent = hieSent;
	}

	public boolean isSentToHie() {
		return Boolean.TRUE.equals(hieSent);
	}

	public Date getHieSentAt() {
		return hieSentAt;
	}

	public void setHieSentAt(Date hieSentAt) {
		this.hieSentAt = hieSentAt;
	}

	public String getHieSendError() {
		return hieSendError;
	}

	public void setHieSendError(String hieSendError) {
		this.hieSendError = hieSendError;
	}

	public String getHieTransferId() {
		return hieTransferId;
	}

	public void setHieTransferId(String hieTransferId) {
		this.hieTransferId = hieTransferId;
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

	public Date getLmpDate() {
		return lmpDate;
	}

	public void setLmpDate(Date lmpDate) {
		this.lmpDate = lmpDate;
	}

	public Date getEddDate() {
		return eddDate;
	}

	public void setEddDate(Date eddDate) {
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

	public Date getVaginalExamAt() {
		return vaginalExamAt;
	}

	public void setVaginalExamAt(Date vaginalExamAt) {
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

	public Boolean getCaput() {
		return caput;
	}

	public void setCaput(Boolean caput) {
		this.caput = caput;
	}

	public Boolean getMoulding() {
		return moulding;
	}

	public void setMoulding(Boolean moulding) {
		this.moulding = moulding;
	}

	public Boolean getMembranesRuptured() {
		return membranesRuptured;
	}

	public void setMembranesRuptured(Boolean membranesRuptured) {
		this.membranesRuptured = membranesRuptured;
	}

	public Date getMembranesRupturedAt() {
		return membranesRupturedAt;
	}

	public void setMembranesRupturedAt(Date membranesRupturedAt) {
		this.membranesRupturedAt = membranesRupturedAt;
	}

	public String getAmnioticFluidColor() {
		return amnioticFluidColor;
	}

	public void setAmnioticFluidColor(String amnioticFluidColor) {
		this.amnioticFluidColor = amnioticFluidColor;
	}

	public Boolean getOffensive() {
		return offensive;
	}

	public void setOffensive(Boolean offensive) {
		this.offensive = offensive;
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

	public Boolean getAttachedLabTests() {
		return attachedLabTests;
	}

	public void setAttachedLabTests(Boolean attachedLabTests) {
		this.attachedLabTests = attachedLabTests;
	}

	public Boolean getAttachedImaging() {
		return attachedImaging;
	}

	public void setAttachedImaging(Boolean attachedImaging) {
		this.attachedImaging = attachedImaging;
	}

	public String getAttachedOther() {
		return attachedOther;
	}

	public void setAttachedOther(String attachedOther) {
		this.attachedOther = attachedOther;
	}

	public String getTransportType() {
		return transportType;
	}

	public void setTransportType(String transportType) {
		this.transportType = transportType;
	}

	public String getTransportOther() {
		return transportOther;
	}

	public void setTransportOther(String transportOther) {
		this.transportOther = transportOther;
	}

	public String getHealthInsuranceType() {
		return healthInsuranceType;
	}

	public void setHealthInsuranceType(String healthInsuranceType) {
		this.healthInsuranceType = healthInsuranceType;
	}

	public String getHealthInsuranceOther() {
		return healthInsuranceOther;
	}

	public void setHealthInsuranceOther(String healthInsuranceOther) {
		this.healthInsuranceOther = healthInsuranceOther;
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

	public Date getReferringSignedDate() {
		return referringSignedDate;
	}

	public void setReferringSignedDate(Date referringSignedDate) {
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

	public List<MaternityTransferTreatment> getTreatments() {
		return treatments;
	}

	public void setTreatments(List<MaternityTransferTreatment> treatments) {
		this.treatments = treatments;
	}

	public void addTreatment(MaternityTransferTreatment treatment) {
		if (treatment == null) {
			return;
		}
		treatment.setMaternityTransfer(this);
		this.treatments.add(treatment);
	}

}
