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
 * One patient visit that started in the selected month, with optional Transfer Id observation(s).
 */
public class PastTransferItem {

	private Integer visitId;

	private Integer patientId;

	private String upid;

	private String patientName;

	private Date visitStartDatetime;

	private Date visitStopDatetime;

	/** Comma-separated valid HIE transfer UUID(s) recorded on this visit. */
	private String transferRecords;

	/** First Transfer Id UUID from {@link #transferRecords}, when present. */
	private String primaryTransferId;

	/** Local {@code transfers} row UUID when this HIE transfer is already cached. */
	private String localTransferUuid;

	/** Insurance type from the visit's registration encounter (e.g. CBHI). */
	private String insuranceType;

	/** Insurance card/policy number from the visit's registration encounter. */
	private String insuranceId;

	public Integer getVisitId() {
		return visitId;
	}

	public void setVisitId(Integer visitId) {
		this.visitId = visitId;
	}

	public Integer getPatientId() {
		return patientId;
	}

	public void setPatientId(Integer patientId) {
		this.patientId = patientId;
	}

	public String getUpid() {
		return upid;
	}

	public void setUpid(String upid) {
		this.upid = upid;
	}

	public String getPatientName() {
		return patientName;
	}

	public void setPatientName(String patientName) {
		this.patientName = patientName;
	}

	public Date getVisitStartDatetime() {
		return visitStartDatetime;
	}

	public void setVisitStartDatetime(Date visitStartDatetime) {
		this.visitStartDatetime = visitStartDatetime;
	}

	public Date getVisitStopDatetime() {
		return visitStopDatetime;
	}

	public void setVisitStopDatetime(Date visitStopDatetime) {
		this.visitStopDatetime = visitStopDatetime;
	}

	public String getTransferRecords() {
		return transferRecords;
	}

	public void setTransferRecords(String transferRecords) {
		this.transferRecords = transferRecords;
	}

	public String getPrimaryTransferId() {
		return primaryTransferId;
	}

	public void setPrimaryTransferId(String primaryTransferId) {
		this.primaryTransferId = primaryTransferId;
	}

	public String getLocalTransferUuid() {
		return localTransferUuid;
	}

	public void setLocalTransferUuid(String localTransferUuid) {
		this.localTransferUuid = localTransferUuid;
	}

	public String getInsuranceType() {
		return insuranceType;
	}

	public void setInsuranceType(String insuranceType) {
		this.insuranceType = insuranceType;
	}

	public String getInsuranceId() {
		return insuranceId;
	}

	public void setInsuranceId(String insuranceId) {
		this.insuranceId = insuranceId;
	}
}
