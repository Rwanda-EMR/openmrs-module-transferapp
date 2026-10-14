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
package org.openmrs.module.transferapp.api;

import java.util.Date;

/**
 * Transfer that still needs counter-referral handling (inbound: send feedback;
 * outbound: import feedback received from HIE into the local table).
 */
public class MissingCounterReferralItem {

	private Integer patientId;

	private String patientName;

	private String upid;

	private String hieTransferId;

	private Date receivedDate;

	private String destinationFacility;

	private String service;

	private String formType;

	private String insuranceType;

	public Integer getPatientId() {
		return patientId;
	}

	public void setPatientId(Integer patientId) {
		this.patientId = patientId;
	}

	public String getPatientName() {
		return patientName;
	}

	public void setPatientName(String patientName) {
		this.patientName = patientName;
	}

	public String getUpid() {
		return upid;
	}

	public void setUpid(String upid) {
		this.upid = upid;
	}

	public String getHieTransferId() {
		return hieTransferId;
	}

	public void setHieTransferId(String hieTransferId) {
		this.hieTransferId = hieTransferId;
	}

	public Date getReceivedDate() {
		return receivedDate;
	}

	public void setReceivedDate(Date receivedDate) {
		this.receivedDate = receivedDate;
	}

	public String getDestinationFacility() {
		return destinationFacility;
	}

	public void setDestinationFacility(String destinationFacility) {
		this.destinationFacility = destinationFacility;
	}

	public String getService() {
		return service;
	}

	public void setService(String service) {
		this.service = service;
	}

	public String getFormType() {
		return formType;
	}

	public void setFormType(String formType) {
		this.formType = formType;
	}

	public String getInsuranceType() {
		return insuranceType;
	}

	public void setInsuranceType(String insuranceType) {
		this.insuranceType = insuranceType;
	}

}
