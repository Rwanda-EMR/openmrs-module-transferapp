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

import org.openmrs.Encounter;
import org.openmrs.Patient;
import org.openmrs.annotation.Authorized;
import org.openmrs.module.transferapp.TransferAppActivator;

import java.util.Map;

/**
 * Saves Transfer Id observations on the patient's active registration encounter.
 */
public interface TransferRegistrationObsService {

	String resolveCurrentFacilityName();

	boolean destinationMatchesCurrentFacility(String destination);

	/**
	 * True when destination matches any alias in {@code transferapp.sendingFacilityName}.
	 * Falls back to {@link #destinationMatchesCurrentFacility(String)} when that GP is blank.
	 */
	boolean destinationMatchesSendingFacilityName(String destination);

	Encounter findRegistrationEncounterMissingTransferId(Patient patient);

	/**
	 * Transfer Id (value_text) on a registration encounter of the patient's
	 * active visit, when that value is a valid UUID.
	 */
	String findRecordedHieTransferIdOnActiveVisit(Patient patient);

	/**
	 * {@code obs_datetime} of the Transfer Id observation on the active visit registration
	 * encounter when {@code value_text} matches {@code hieTransferId}.
	 */
	java.util.Date findTransferIdObsDatetimeOnActiveVisit(Patient patient, String hieTransferId);

	/**
	 * Transfer Id observation for this patient whose {@code value_text} matches {@code hieTransferId}.
	 * Prefers the active-visit registration encounter, then any matching patient obs.
	 */
	org.openmrs.Obs findMatchingTransferIdObs(Patient patient, String hieTransferId);

	/**
	 * Validates the HIE transfer, records Transfer Id on a registration encounter missing it,
	 * and caches the transfer locally. Uses the patient's active visit when {@code visitId} is null.
	 */
	@Authorized(TransferAppActivator.PRIVILEGE_CREATE_TRANSFER)
	Map<String, Object> validateAndSaveTransferId(Integer patientId, String hieTransferId);

	/**
	 * Same as {@link #validateAndSaveTransferId(Integer, String)} but records the Transfer Id on a
	 * registration encounter belonging to the given visit (required for Past Transfers).
	 */
	@Authorized(TransferAppActivator.PRIVILEGE_CREATE_TRANSFER)
	Map<String, Object> validateAndSaveTransferId(Integer patientId, String hieTransferId, Integer visitId);

}
