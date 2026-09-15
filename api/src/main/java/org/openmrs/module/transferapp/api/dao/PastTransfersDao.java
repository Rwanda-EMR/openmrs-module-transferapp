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
	 * Visits whose {@code date_started} falls in {@code [start, end]} and that have at least one
	 * non-voided registration encounter. Newest start first.
	 *
	 * @return rows as {@code [visitId, patientId, startDatetime, stopDatetime]}
	 */
	List<Object[]> findVisitsWithRegistrationInRange(Date start, Date end, Integer registrationEncounterTypeId,
			int offset, int maxResults);

	int countVisitsWithRegistrationInRange(Date start, Date end, Integer registrationEncounterTypeId);

	/**
	 * Transfer Id observation value_text values keyed by visit id for the given concept.
	 */
	Map<Integer, List<String>> findTransferIdValuesByVisitIds(Collection<Integer> visitIds,
			Integer transferIdConceptId);

	/**
	 * Loads patients (with names + identifiers initialized) for UPID/name display.
	 */
	Map<Integer, Patient> findPatientsByIds(Collection<Integer> patientIds);

	/**
	 * Local transfers matching any of the HIE transfer ids (non-voided).
	 */
	List<Transfer> findTransfersByHieTransferIds(Collection<String> hieTransferIds);
}
