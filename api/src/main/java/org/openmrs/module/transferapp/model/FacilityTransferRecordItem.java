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

import java.util.Date;

/**
 * Outbound transfer row shown on the facility records page.
 */
public class FacilityTransferRecordItem {

	private String id;

	private Date transferDate;

	private Integer patientId;

	private String clientName;

	private String emrId;

	private String receivingFacility;

	private String service;

	private boolean hieSent;

	/** "External" or "Maternity" — which transfer form produced this row. */
	private String formType;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Date getTransferDate() {
		return transferDate;
	}

	public void setTransferDate(Date transferDate) {
		this.transferDate = transferDate;
	}

	public Integer getPatientId() {
		return patientId;
	}

	public void setPatientId(Integer patientId) {
		this.patientId = patientId;
	}

	public String getClientName() {
		return clientName;
	}

	public void setClientName(String clientName) {
		this.clientName = clientName;
	}

	public String getEmrId() {
		return emrId;
	}

	public void setEmrId(String emrId) {
		this.emrId = emrId;
	}

	public String getReceivingFacility() {
		return receivingFacility;
	}

	public void setReceivingFacility(String receivingFacility) {
		this.receivingFacility = receivingFacility;
	}

	public String getService() {
		return service;
	}

	public void setService(String service) {
		this.service = service;
	}

	public boolean isHieSent() {
		return hieSent;
	}

	public void setHieSent(boolean hieSent) {
		this.hieSent = hieSent;
	}

	public String getFormType() {
		return formType;
	}

	public void setFormType(String formType) {
		this.formType = formType;
	}

}
