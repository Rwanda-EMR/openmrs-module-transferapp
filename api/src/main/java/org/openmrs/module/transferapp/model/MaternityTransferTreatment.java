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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.io.Serializable;
import java.util.Date;

/**
 * One row of the Maternity Transfer Form's drug-administration table
 * (Treatment name × Dose × Date × Time given), e.g. IV Fluids, Dexamethasone,
 * Magnesium sulphate, Nifedipine, Oxytocin, ATBs.
 */
@Entity(name = "TransferappMaternityTransferTreatment")
@Table(name = "maternity_transfer_treatments")
public class MaternityTransferTreatment implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "maternity_transfer_treatment_id")
	private Integer maternityTransferTreatmentId;

	@ManyToOne(optional = false)
	@JoinColumn(name = "maternity_transfer_id", nullable = false)
	private MaternityTransfer maternityTransfer;

	@Column(name = "treatment_name", length = 255)
	private String treatmentName;

	@Column(name = "dose", length = 64)
	private String dose;

	@Column(name = "given_date")
	private Date givenDate;

	@Column(name = "given_time", length = 8)
	private String givenTime;

	public Integer getMaternityTransferTreatmentId() {
		return maternityTransferTreatmentId;
	}

	public void setMaternityTransferTreatmentId(Integer maternityTransferTreatmentId) {
		this.maternityTransferTreatmentId = maternityTransferTreatmentId;
	}

	public MaternityTransfer getMaternityTransfer() {
		return maternityTransfer;
	}

	public void setMaternityTransfer(MaternityTransfer maternityTransfer) {
		this.maternityTransfer = maternityTransfer;
	}

	public String getTreatmentName() {
		return treatmentName;
	}

	public void setTreatmentName(String treatmentName) {
		this.treatmentName = treatmentName;
	}

	public String getDose() {
		return dose;
	}

	public void setDose(String dose) {
		this.dose = dose;
	}

	public Date getGivenDate() {
		return givenDate;
	}

	public void setGivenDate(Date givenDate) {
		this.givenDate = givenDate;
	}

	public String getGivenTime() {
		return givenTime;
	}

	public void setGivenTime(String givenTime) {
		this.givenTime = givenTime;
	}

}
