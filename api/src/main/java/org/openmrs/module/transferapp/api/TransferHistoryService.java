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
import org.openmrs.module.transferapp.model.Transfer;
import org.openmrs.module.transferapp.model.TransferHistoryItem;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Lists registration encounters that have a recorded HIE Transfer Id.
 */
@Transactional(readOnly = true)
public interface TransferHistoryService {

	/**
	 * Default (no filters): registration encounters from today with a Transfer Id.
	 * With UPID: that patient's registration encounters that have a Transfer Id.
	 * With month ({@code yyyy-MM}): restrict to that calendar month.
	 * With UPID + month: that patient's registration encounters in the selected month.
	 * With formType ({@code external|maternity|neonatal}): restrict by local
	 * {@code transfers.form_kind} (rows with no local copy count as external).
	 */
	@Authorized(TransferAppActivator.PRIVILEGE_LIST_TRANSFERS)
	List<TransferHistoryItem> findHistory(String upid, String yearMonth, String formType);

	/**
	 * Schedules (or clears) a reuse rendez-vous date for a previously recorded HIE transfer.
	 * Date must be today or a future calendar day; blank clears the schedule.
	 * Ensures a local {@code transfers} row exists (receives from HIE when missing).
	 */
	@Authorized(TransferAppActivator.PRIVILEGE_CREATE_TRANSFER)
	@Transactional(readOnly = false)
	Transfer setReuseRendezvousDate(Integer patientId, String hieTransferId, String reuseDateYyyyMmDd);
}
