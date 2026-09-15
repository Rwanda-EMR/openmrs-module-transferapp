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
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.util.Date;

/**
 * Clinician referral feedback and counter-referral for an inbound HIE transfer.
 */
@Entity(name = "TransferappReferralFeedback")
@Table(name = "transfer_referral_feedback")
public class TransferReferralFeedback extends BaseOpenmrsData {

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "transfer_referral_feedback_id")
	private Integer transferReferralFeedbackId;

	@ManyToOne(optional = false)
	@JoinColumn(name = "patient_id", nullable = false)
	private Patient patient;

	@Column(name = "hie_transfer_id", nullable = false, length = 64)
	private String hieTransferId;

	@Column(name = "local_transfer_id")
	private Integer localTransferId;

	@Column(name = "visit_id")
	private Integer visitId;

	@Column(name = "registration_encounter_id")
	private Integer registrationEncounterId;

	@Temporal(TemporalType.DATE)
	@Column(name = "date_of_admission_or_seen", nullable = false)
	private Date dateOfAdmissionOrSeen;

	@Temporal(TemporalType.DATE)
	@Column(name = "date_of_discharge", nullable = false)
	private Date dateOfDischarge;

	@Column(name = "final_diagnosis", nullable = false)
	private String finalDiagnosis;

	@Column(name = "treatment_given", nullable = false)
	private String treatmentGiven;

	@Column(name = "outcome", nullable = false, length = 40)
	private String outcome;

	@Column(name = "recommendations", nullable = false)
	private String recommendations;

	@Column(name = "refer_back_to_facility", nullable = false, length = 255)
	private String referBackToFacility;

	@Column(name = "refer_back_to_facility_fosa_id", length = 64)
	private String referBackToFacilityFosaId;

	@Column(name = "contact_person", nullable = false, length = 255)
	private String contactPerson;

	@Column(name = "provider_name", nullable = false, length = 255)
	private String providerName;

	@Column(name = "qualification", nullable = false, length = 255)
	private String qualification;

	@Temporal(TemporalType.DATE)
	@Column(name = "signed_date", nullable = false)
	private Date signedDate;

	@Column(name = "signed_time", nullable = false, length = 8)
	private String signedTime;

	@Column(name = "phone", nullable = false, length = 64)
	private String phone;

	@Column(name = "completed", nullable = false)
	private Boolean completed = Boolean.TRUE;

	@Column(name = "completed_at")
	private Date completedAt;

	@Column(name = "completed_by")
	private Integer completedBy;

	@Column(name = "hie_sent", nullable = false)
	private Boolean hieSent = Boolean.FALSE;

	@Column(name = "hie_sent_at")
	private Date hieSentAt;

	@Column(name = "hie_send_err", length = 500)
	private String hieSendErr;

	@Override
	public Integer getId() {
		return getTransferReferralFeedbackId();
	}

	@Override
	public void setId(Integer id) {
		setTransferReferralFeedbackId(id);
	}

	public Integer getTransferReferralFeedbackId() {
		return transferReferralFeedbackId;
	}

	public void setTransferReferralFeedbackId(Integer transferReferralFeedbackId) {
		this.transferReferralFeedbackId = transferReferralFeedbackId;
	}

	public Patient getPatient() {
		return patient;
	}

	public void setPatient(Patient patient) {
		this.patient = patient;
	}

	public String getHieTransferId() {
		return hieTransferId;
	}

	public void setHieTransferId(String hieTransferId) {
		this.hieTransferId = hieTransferId;
	}

	public Integer getLocalTransferId() {
		return localTransferId;
	}

	public void setLocalTransferId(Integer localTransferId) {
		this.localTransferId = localTransferId;
	}

	public Integer getVisitId() {
		return visitId;
	}

	public void setVisitId(Integer visitId) {
		this.visitId = visitId;
	}

	public Integer getRegistrationEncounterId() {
		return registrationEncounterId;
	}

	public void setRegistrationEncounterId(Integer registrationEncounterId) {
		this.registrationEncounterId = registrationEncounterId;
	}

	public Date getDateOfAdmissionOrSeen() {
		return dateOfAdmissionOrSeen;
	}

	public void setDateOfAdmissionOrSeen(Date dateOfAdmissionOrSeen) {
		this.dateOfAdmissionOrSeen = dateOfAdmissionOrSeen;
	}

	public Date getDateOfDischarge() {
		return dateOfDischarge;
	}

	public void setDateOfDischarge(Date dateOfDischarge) {
		this.dateOfDischarge = dateOfDischarge;
	}

	public String getFinalDiagnosis() {
		return finalDiagnosis;
	}

	public void setFinalDiagnosis(String finalDiagnosis) {
		this.finalDiagnosis = finalDiagnosis;
	}

	public String getTreatmentGiven() {
		return treatmentGiven;
	}

	public void setTreatmentGiven(String treatmentGiven) {
		this.treatmentGiven = treatmentGiven;
	}

	public String getOutcome() {
		return outcome;
	}

	public void setOutcome(String outcome) {
		this.outcome = outcome;
	}

	public String getRecommendations() {
		return recommendations;
	}

	public void setRecommendations(String recommendations) {
		this.recommendations = recommendations;
	}

	public String getReferBackToFacility() {
		return referBackToFacility;
	}

	public void setReferBackToFacility(String referBackToFacility) {
		this.referBackToFacility = referBackToFacility;
	}

	public String getReferBackToFacilityFosaId() {
		return referBackToFacilityFosaId;
	}

	public void setReferBackToFacilityFosaId(String referBackToFacilityFosaId) {
		this.referBackToFacilityFosaId = referBackToFacilityFosaId;
	}

	public String getContactPerson() {
		return contactPerson;
	}

	public void setContactPerson(String contactPerson) {
		this.contactPerson = contactPerson;
	}

	public String getProviderName() {
		return providerName;
	}

	public void setProviderName(String providerName) {
		this.providerName = providerName;
	}

	public String getQualification() {
		return qualification;
	}

	public void setQualification(String qualification) {
		this.qualification = qualification;
	}

	public Date getSignedDate() {
		return signedDate;
	}

	public void setSignedDate(Date signedDate) {
		this.signedDate = signedDate;
	}

	public String getSignedTime() {
		return signedTime;
	}

	public void setSignedTime(String signedTime) {
		this.signedTime = signedTime;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public Boolean getCompleted() {
		return completed;
	}

	public void setCompleted(Boolean completed) {
		this.completed = completed;
	}

	public boolean isCompleted() {
		return Boolean.TRUE.equals(completed) && completedAt != null;
	}

	public Date getCompletedAt() {
		return completedAt;
	}

	public void setCompletedAt(Date completedAt) {
		this.completedAt = completedAt;
	}

	public Integer getCompletedBy() {
		return completedBy;
	}

	public void setCompletedBy(Integer completedBy) {
		this.completedBy = completedBy;
	}

	public Boolean getHieSent() {
		return hieSent;
	}

	public void setHieSent(Boolean hieSent) {
		this.hieSent = hieSent;
	}

	public boolean isHieSent() {
		return Boolean.TRUE.equals(hieSent);
	}

	public Date getHieSentAt() {
		return hieSentAt;
	}

	public void setHieSentAt(Date hieSentAt) {
		this.hieSentAt = hieSentAt;
	}

	public String getHieSendErr() {
		return hieSendErr;
	}

	public void setHieSendErr(String hieSendErr) {
		this.hieSendErr = hieSendErr;
	}
}
