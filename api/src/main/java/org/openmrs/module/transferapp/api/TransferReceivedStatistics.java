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

/**
 * Summary counts for transfers received at the facility.
 */
public class TransferReceivedStatistics {

	private int today;

	private int thisWeek;

	private int total;

	private int pending;

	private int missingCounterReferral;

	public int getToday() {
		return today;
	}

	public void setToday(int today) {
		this.today = today;
	}

	public int getThisWeek() {
		return thisWeek;
	}

	public void setThisWeek(int thisWeek) {
		this.thisWeek = thisWeek;
	}

	public int getTotal() {
		return total;
	}

	public void setTotal(int total) {
		this.total = total;
	}

	public int getPending() {
		return pending;
	}

	public void setPending(int pending) {
		this.pending = pending;
	}

	public int getMissingCounterReferral() {
		return missingCounterReferral;
	}

	public void setMissingCounterReferral(int missingCounterReferral) {
		this.missingCounterReferral = missingCounterReferral;
	}

}
