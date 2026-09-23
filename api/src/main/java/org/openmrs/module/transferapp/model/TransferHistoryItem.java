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
 * One registration encounter that has a recorded HIE Transfer Id (for History).
 */
public class TransferHistoryItem {

	private Integer patientId;

	private String patientName;

	private String upid;

	private Integer encounterId;

	private Date encounterDatetime;

	private String transferId;

	private String phoneNumber;

	private String locationName;

	/** yyyy-MM-dd when set; used by History to schedule reuse for reception. */
	private String reuseRendezvousDate;

	/** Local {@code transfers} row UUID when a local copy exists for this HIE id. */
	private String localTransferUuid;

	/** {@link TransferFormKind} name: GENERAL, MATERNITY, or NEONATAL. */
	private String formKind;

	/** FHIR-style code: external, maternity, or neonatal. */
	private String formKindCode;

	/** Short label for History filter/column (External / Maternity / Neonatal). */
	private String formKindLabel;

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

	public Integer getEncounterId() {
		return encounterId;
	}

	public void setEncounterId(Integer encounterId) {
		this.encounterId = encounterId;
	}

	public Date getEncounterDatetime() {
		return encounterDatetime;
	}

	public void setEncounterDatetime(Date encounterDatetime) {
		this.encounterDatetime = encounterDatetime;
	}

	public String getTransferId() {
		return transferId;
	}

	public void setTransferId(String transferId) {
		this.transferId = transferId;
	}

	public String getPhoneNumber() {
		return phoneNumber;
	}

	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}

	public String getLocationName() {
		return locationName;
	}

	public void setLocationName(String locationName) {
		this.locationName = locationName;
	}

	public String getReuseRendezvousDate() {
		return reuseRendezvousDate;
	}

	public void setReuseRendezvousDate(String reuseRendezvousDate) {
		this.reuseRendezvousDate = reuseRendezvousDate;
	}

	public String getLocalTransferUuid() {
		return localTransferUuid;
	}

	public void setLocalTransferUuid(String localTransferUuid) {
		this.localTransferUuid = localTransferUuid;
	}

	public String getFormKind() {
		return formKind;
	}

	public void setFormKind(String formKind) {
		this.formKind = formKind;
	}

	public String getFormKindCode() {
		return formKindCode;
	}

	public void setFormKindCode(String formKindCode) {
		this.formKindCode = formKindCode;
	}

	public String getFormKindLabel() {
		return formKindLabel;
	}

	public void setFormKindLabel(String formKindLabel) {
		this.formKindLabel = formKindLabel;
	}
}
