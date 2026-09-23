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

import org.openmrs.Patient;
import org.openmrs.module.transferapp.model.Transfer;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Bulk read helpers for the Past Transfers page (avoids per-visit N+1 queries).
 */
public interface PastTransfersDao {

	/**
	 * Resolves a non-voided patient id by exact UPID identifier match.
	 * Prefers identifier types named UPID; returns null when none found.
	 */
	Integer findPatientIdByUpid(String upid);

	/**
	 * Visits whose {@code date_started} falls in {@code [start, end]} and that have at least one
	 * non-voided registration encounter. Newest start first.
	 * When {@code patientId} is non-null, results are limited to that patient (fast UPID filter).
	 *
	 * @return rows as {@code [visitId, patientId, startDatetime, stopDatetime]}
	 */
	List<Object[]> findVisitsWithRegistrationInRange(Date start, Date end, Integer registrationEncounterTypeId,
			Integer patientId, int offset, int maxResults);

	int countVisitsWithRegistrationInRange(Date start, Date end, Integer registrationEncounterTypeId,
			Integer patientId);

	/**
	 * Transfer Id observation value_text values keyed by visit id for the given concept.
	 */
	Map<Integer, List<String>> findTransferIdValuesByVisitIds(Collection<Integer> visitIds,
			Integer transferIdConceptId);

	/**
	 * Registration-encounter insurance type / id for the given visits, keyed by visit id.
	 * Values are {@code [insuranceTypeDisplay, insuranceId]} (either may be blank).
	 * One batched query; does not load full Patient/Encounter graphs.
	 */
	Map<Integer, String[]> findInsuranceByVisitIds(Collection<Integer> visitIds, Integer insuranceTypeConceptId,
			Integer insuranceNumberConceptId, Integer registrationEncounterTypeId);

	/**
	 * Loads patients (with names + identifiers initialized) for UPID/name display.
	 */
	Map<Integer, Patient> findPatientsByIds(Collection<Integer> patientIds);

	/**
	 * Local transfers matching any of the HIE transfer ids (non-voided).
	 */
	List<Transfer> findTransfersByHieTransferIds(Collection<String> hieTransferIds);
}
