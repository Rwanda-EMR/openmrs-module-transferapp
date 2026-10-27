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

import org.openmrs.module.transferapp.model.OrphanTransferIdObs;
import org.openmrs.module.transferapp.model.Transfer;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Backfills {@code transfers} rows for Transfer Id obs that have no local transfer record,
 * by loading each Encounter from HIE.
 */
@Transactional
public interface OrphanTransferBackfillService {

	boolean isBackfillEnabled();

	int getBatchSize();

	int getDelaySeconds();

	int getLookbackDays();

	/**
	 * Orphan Transfer Id obs within the configured lookback window, limited to batch size.
	 */
	@Transactional(readOnly = true)
	List<OrphanTransferIdObs> findOrphanTransferIdObs();

	/**
	 * Fetches {@code GET /shr/Encounter/{hieTransferId}} and stores a local transfer when missing.
	 * Does not require patient UPID. Returns the existing or newly saved transfer; {@code null}
	 * when HIE has no encounter for the id.
	 */
	Transfer backfillOne(Integer patientId, String hieTransferId);
}
