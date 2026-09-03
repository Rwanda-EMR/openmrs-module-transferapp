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

/**
 * Non-persistent row used to seed the Maternity Transfer Form's drug-administration
 * table (Treatment / Dose / Date / Time) with suggested treatment names on the wizard.
 */
public class MaternityTransferTreatmentRow {

	private String treatmentName;

	private String dose;

	private String givenDate;

	private String givenTime;

	public MaternityTransferTreatmentRow() {
	}

	public MaternityTransferTreatmentRow(String treatmentName) {
		this.treatmentName = treatmentName;
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

	public String getGivenDate() {
		return givenDate;
	}

	public void setGivenDate(String givenDate) {
		this.givenDate = givenDate;
	}

	public String getGivenTime() {
		return givenTime;
	}

	public void setGivenTime(String givenTime) {
		this.givenTime = givenTime;
	}

}
