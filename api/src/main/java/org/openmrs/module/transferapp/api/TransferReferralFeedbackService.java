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

import org.openmrs.annotation.Authorized;
import org.openmrs.module.transferapp.TransferAppActivator;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Transactional
public interface TransferReferralFeedbackService {

	@Authorized(TransferAppActivator.PRIVILEGE_FEEDBACK)
	@Transactional(readOnly = true)
	Map<String, Object> getFeedbackForm(Integer patientId, String hieTransferId);

	@Authorized(TransferAppActivator.PRIVILEGE_FEEDBACK)
	Map<String, Object> saveFeedback(Integer patientId, String hieTransferId,
			String dateOfDischarge, String finalDiagnosis, String treatmentGiven, String outcome,
			String recommendations, String referBackToFacility, String referBackToFacilityFosaId,
			String contactPerson, String providerName, String qualification, String signedDate,
			String signedTime, String phone);

	/**
	 * Builds the Encounter JSON that would be upserted to HIE (existing transfer + feedback extensions).
	 */
	@Authorized(TransferAppActivator.PRIVILEGE_FEEDBACK)
	@Transactional(readOnly = true)
	Map<String, Object> previewFeedbackHiePayload(Integer patientId, String hieTransferId);

	/**
	 * Upserts the merged Encounter to HIE and marks the local feedback row as sent.
	 */
	@Authorized(TransferAppActivator.PRIVILEGE_FEEDBACK)
	Map<String, Object> submitFeedbackToHie(Integer patientId, String hieTransferId);
}
