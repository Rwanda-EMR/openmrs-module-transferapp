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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.util.Date;

/**
 * Persisted outbound Neonatal transfer referral record.
 *
 * <p>Unlike {@link MaternityTransfer}, this form has no unbounded repeating structure
 * (its only repeating sections — 2 antibiotic blocks, 4 diagnosis slots — are fixed-size
 * per the paper form), so it is modeled as flat numbered columns with no child table,
 * matching how {@link Transfer} itself stores fixed fields flatly.</p>
 */
@Entity(name = "TransferappNeonatalTransfer")
@Table(name = "neonatal_transfers")
public class NeonatalTransfer extends BaseOpenmrsData {

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "neonatal_transfer_id")
	private Integer neonatalTransferId;

	@ManyToOne(optional = false)
	@JoinColumn(name = "patient_id", nullable = false)
	private Patient patient;

	// Step 1 — baby & referral info

	@Column(name = "baby_name", length = 255)
	private String babyName;

	@Column(name = "sex", length = 20)
	private String sex;

	@Column(name = "dob")
	private Date dob;

	@Column(name = "gestational_age_weeks", length = 16)
	private String gestationalAgeWeeks;

	@Column(name = "birth_weight_g", length = 16)
	private String birthWeightG;

	@Column(name = "current_weight_g", length = 16)
	private String currentWeightG;

	@Column(name = "current_age_days", length = 16)
	private String currentAgeDays;

	@Column(name = "mother_name", length = 255)
	private String motherName;

	@Column(name = "mother_age", length = 16)
	private String motherAge;

	@Column(name = "mother_caregiver_phone", length = 64)
	private String motherCaregiverPhone;

	@Column(name = "place_of_birth", length = 255)
	private String placeOfBirth;

	@Column(name = "reason_for_transfer")
	private String reasonForTransfer;

	@Column(name = "mode_of_transport", length = 32)
	private String modeOfTransport;

	@Column(name = "transport_other", length = 255)
	private String transportOther;

	@Column(name = "transfer_type", length = 32)
	private String transferType;

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

	@Column(name = "decision_to_transfer_at")
	private Date decisionToTransferAt;

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

	// Step 2 — maternal history

	@Column(name = "mother_alive", length = 16)
	private String motherAlive;

	@Column(name = "obstetric_gravida", length = 16)
	private String obstetricGravida;

	@Column(name = "obstetric_parity", length = 16)
	private String obstetricParity;

	@Column(name = "pregnancy_type", length = 32)
	private String pregnancyType;

	@Column(name = "anc_screening")
	private String ancScreening;

	@Column(name = "pathologies_during_pregnancy")
	private String pathologiesDuringPregnancy;

	@Column(name = "pregnancy_other_pathologies")
	private String pregnancyOtherPathologies;

	@Column(name = "pregnancy_treatment")
	private String pregnancyTreatment;

	@Column(name = "blood_group", length = 16)
	private String bloodGroup;

	@Column(name = "rh_factor", length = 16)
	private String rhFactor;

	@Column(name = "hiv_status", length = 32)
	private String hivStatus;

	@Column(name = "hiv_regimen", length = 255)
	private String hivRegimen;

	@Column(name = "hiv_recent_vl", length = 64)
	private String hivRecentVl;

	@Column(name = "hiv_cd4_count", length = 64)
	private String hivCd4Count;

	@Column(name = "hiv_opportunistic_infections")
	private String hivOpportunisticInfections;

	@Column(name = "tetanus_vaccine_doses", length = 16)
	private String tetanusVaccineDoses;

	@Column(name = "maternal_illicit_drug_history")
	private String maternalIllicitDrugHistory;

	// Step 3 — labor details

	@Column(name = "rom_at")
	private Date romAt;

	@Column(name = "af_quality", length = 64)
	private String afQuality;

	@Column(name = "af_quantity", length = 64)
	private String afQuantity;

	@Column(name = "fever_timing", length = 64)
	private String feverTiming;

	@Column(name = "steroid_doses", length = 16)
	private String steroidDoses;

	@Column(name = "last_steroid_dose_at")
	private Date lastSteroidDoseAt;

	@Column(name = "mgso4_at")
	private Date mgso4At;

	@Column(name = "mode_of_delivery", length = 64)
	private String modeOfDelivery;

	@Column(name = "labor_complications", length = 255)
	private String laborComplications;

	@Column(name = "labor_complications_other", length = 255)
	private String laborComplicationsOther;

	@Column(name = "maternal_anesthesia", length = 64)
	private String maternalAnesthesia;

	@Column(name = "maternal_anesthesia_other", length = 255)
	private String maternalAnesthesiaOther;

	@Column(name = "maternal_antibiotics")
	private String maternalAntibiotics;

	@Column(name = "other_drugs")
	private String otherDrugs;

	@Column(name = "sepsis_risk_factors")
	private String sepsisRiskFactors;

	// Step 4 — neonatal history & drugs

	@Column(name = "resuscitation_at_birth", length = 16)
	private String resuscitationAtBirth;

	@Column(name = "resuscitation_methods")
	private String resuscitationMethods;

	@Column(name = "apgar_1min", length = 8)
	private String apgar1min;

	@Column(name = "apgar_5min", length = 8)
	private String apgar5min;

	@Column(name = "apgar_10min", length = 8)
	private String apgar10min;

	@Column(name = "hie", length = 16)
	private String hie;

	@Column(name = "hie_grade", length = 16)
	private String hieGrade;

	@Column(name = "allergies", length = 255)
	private String allergies;

	@Column(name = "immunization", length = 16)
	private String immunization;

	@Column(name = "immunization_details")
	private String immunizationDetails;

	@Column(name = "vitamin_k", length = 16)
	private String vitaminK;

	@Column(name = "tetracycline_eye_ointment", length = 16)
	private String tetracyclineEyeOintment;

	@Column(name = "surfactant", length = 16)
	private String surfactant;

	// Step 5 — chief complaint & diagnoses

	@Column(name = "chief_complaint_details")
	private String chiefComplaintDetails;

	@Column(name = "spo2_preductal", length = 16)
	private String spo2Preductal;

	@Column(name = "spo2_postductal", length = 16)
	private String spo2Postductal;

	@Column(name = "condition_temp", length = 16)
	private String conditionTemp;

	@Column(name = "condition_hr", length = 16)
	private String conditionHr;

	@Column(name = "condition_rr", length = 16)
	private String conditionRr;

	@Column(name = "condition_bp", length = 32)
	private String conditionBp;

	@Column(name = "neurological_status", length = 64)
	private String neurologicalStatus;

	@Column(name = "seizures")
	private Boolean seizures;

	@Column(name = "adverse_events_24h")
	private String adverseEvents24h;

	@Column(name = "diagnosis_1", length = 255)
	private String diagnosis1;

	@Column(name = "diagnosis_2", length = 255)
	private String diagnosis2;

	@Column(name = "diagnosis_3", length = 255)
	private String diagnosis3;

	@Column(name = "diagnosis_4", length = 255)
	private String diagnosis4;

	// Step 6 — management at referring facility

	@Column(name = "respiratory_support", length = 64)
	private String respiratorySupport;

	@Column(name = "ventilation_settings")
	private String ventilationSettings;

	@Column(name = "blood_gas_analysis", length = 16)
	private String bloodGasAnalysis;

	@Column(name = "iv_fluid_vol", length = 32)
	private String ivFluidVol;

	@Column(name = "passed_urine", length = 16)
	private String passedUrine;

	@Column(name = "inotropes", length = 255)
	private String inotropes;

	@Column(name = "inotropes_specify", length = 255)
	private String inotropesSpecify;

	@Column(name = "peripheral_iv", length = 16)
	private String peripheralIv;

	@Column(name = "central_iv", length = 16)
	private String centralIv;

	@Column(name = "intraosseous_line", length = 16)
	private String intraosseousLine;

	@Column(name = "antibiotic1_name", length = 255)
	private String antibiotic1Name;

	@Column(name = "antibiotic1_doses", length = 120)
	private String antibiotic1Doses;

	@Column(name = "antibiotic1_durations", length = 120)
	private String antibiotic1Durations;

	@Column(name = "antibiotic2_name", length = 255)
	private String antibiotic2Name;

	@Column(name = "antibiotic2_doses", length = 120)
	private String antibiotic2Doses;

	@Column(name = "antibiotic2_durations", length = 120)
	private String antibiotic2Durations;

	@Column(name = "arvs", length = 255)
	private String arvs;

	@Column(name = "npo", length = 16)
	private String npo;

	@Column(name = "last_feed_time", length = 8)
	private String lastFeedTime;

	@Column(name = "last_feed_amount", length = 32)
	private String lastFeedAmount;

	@Column(name = "feed_vol", length = 32)
	private String feedVol;

	@Column(name = "feed_type", length = 64)
	private String feedType;

	@Column(name = "passed_stool", length = 16)
	private String passedStool;

	@Column(name = "nasogastric_tube", length = 16)
	private String nasogastricTube;

	@Column(name = "lab_glucose", length = 32)
	private String labGlucose;

	@Column(name = "lab_fbc", length = 64)
	private String labFbc;

	@Column(name = "lab_hb", length = 32)
	private String labHb;

	@Column(name = "lab_wbc", length = 32)
	private String labWbc;

	@Column(name = "lab_platelets", length = 32)
	private String labPlatelets;

	@Column(name = "lab_crp", length = 32)
	private String labCrp;

	@Column(name = "lab_bili_total", length = 32)
	private String labBiliTotal;

	@Column(name = "lab_bili_direct", length = 32)
	private String labBiliDirect;

	@Column(name = "lab_ue", length = 64)
	private String labUe;

	@Column(name = "lab_cultures", length = 255)
	private String labCultures;

	@Column(name = "fbc_done", length = 16)
	private String fbcDone;

	@Column(name = "imaging_results_available", length = 16)
	private String imagingResultsAvailable;

	@Column(name = "imaging_results")
	private String imagingResults;

	@Column(name = "pain_sedation_drugs")
	private String painSedationDrugs;

	@Column(name = "imaging_report_attached")
	private Boolean imagingReportAttached;

	@Column(name = "lab_reports_attached")
	private Boolean labReportsAttached;

	// Step 7 — summary & sign-off

	@Column(name = "clinical_management_summary")
	private String clinicalManagementSummary;

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

	@Override
	public Integer getId() {
		return getNeonatalTransferId();
	}

	@Override
	public void setId(Integer id) {
		setNeonatalTransferId(id);
	}

	public Integer getNeonatalTransferId() {
		return neonatalTransferId;
	}

	public void setNeonatalTransferId(Integer neonatalTransferId) {
		this.neonatalTransferId = neonatalTransferId;
	}

	public Patient getPatient() {
		return patient;
	}

	public void setPatient(Patient patient) {
		this.patient = patient;
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

	public Date getDob() {
		return dob;
	}

	public void setDob(Date dob) {
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

	public Date getDecisionToTransferAt() {
		return decisionToTransferAt;
	}

	public void setDecisionToTransferAt(Date decisionToTransferAt) {
		this.decisionToTransferAt = decisionToTransferAt;
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

	public Date getRomAt() {
		return romAt;
	}

	public void setRomAt(Date romAt) {
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

	public Date getLastSteroidDoseAt() {
		return lastSteroidDoseAt;
	}

	public void setLastSteroidDoseAt(Date lastSteroidDoseAt) {
		this.lastSteroidDoseAt = lastSteroidDoseAt;
	}

	public Date getMgso4At() {
		return mgso4At;
	}

	public void setMgso4At(Date mgso4At) {
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

	public Boolean getSeizures() {
		return seizures;
	}

	public void setSeizures(Boolean seizures) {
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

	public Boolean getImagingReportAttached() {
		return imagingReportAttached;
	}

	public void setImagingReportAttached(Boolean imagingReportAttached) {
		this.imagingReportAttached = imagingReportAttached;
	}

	public Boolean getLabReportsAttached() {
		return labReportsAttached;
	}

	public void setLabReportsAttached(Boolean labReportsAttached) {
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

}
