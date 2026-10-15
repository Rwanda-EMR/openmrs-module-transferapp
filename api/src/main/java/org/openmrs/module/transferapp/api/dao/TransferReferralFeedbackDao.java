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
package org.openmrs.module.transferapp.api.dao;

import org.openmrs.module.transferapp.model.TransferReferralFeedback;

import java.util.Date;
import java.util.Set;

public interface TransferReferralFeedbackDao {

	TransferReferralFeedback save(TransferReferralFeedback feedback);

	TransferReferralFeedback getByPatientAndHieTransferId(org.openmrs.Patient patient, String hieTransferId);

	TransferReferralFeedback getByPatientAndHieTransferId(Integer patientId, String hieTransferId);

	/**
	 * Non-voided counter-referral feedback rows. When {@code hieSentOnly} is true, only rows
	 * already submitted to HIE are counted.
	 */
	int countFeedback(boolean hieSentOnly);

	/**
	 * Same as {@link #countFeedback(boolean)} limited to {@code dateCreated} in
	 * {@code [fromDate, toDate]} (null bounds are unbounded).
	 */
	int countFeedback(boolean hieSentOnly, Date fromDate, Date toDate);

	/**
	 * Keys of feedback already submitted to HIE, formatted as {@code patientId|hieTransferId}.
	 */
	Set<String> listHieSentFeedbackKeys();

	/**
	 * Keys of all non-voided feedback rows, formatted as {@code patientId|hieTransferId}.
	 */
	Set<String> listFeedbackKeys();
}
