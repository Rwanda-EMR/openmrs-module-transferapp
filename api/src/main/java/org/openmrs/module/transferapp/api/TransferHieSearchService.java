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
public interface TransferHieSearchService {

	int DEFAULT_PENDING_WEEKS = 1;

	int MAX_PENDING_WEEKS = 4;

	@Authorized(value = {
			TransferAppActivator.PRIVILEGE_LIST_TRANSFERS,
			TransferAppActivator.PRIVILEGE_LIST_PENDING,
			TransferAppActivator.PRIVILEGE_PAST_TRANSFERS }, requireAll = false)
	@Transactional(readOnly = true)
	Map<String, Object> searchTransfers(String upid, String transferId, boolean activeOnly);

	/**
	 * Lists HIE transfers for a patient between inclusive ISO dates ({@code yyyy-MM-dd}).
	 * When either bound is blank, behaves like an unfiltered patient transfer list.
	 */
	@Authorized(value = {
			TransferAppActivator.PRIVILEGE_LIST_TRANSFERS,
			TransferAppActivator.PRIVILEGE_LIST_PENDING,
			TransferAppActivator.PRIVILEGE_PAST_TRANSFERS }, requireAll = false)
	@Transactional(readOnly = true)
	Map<String, Object> searchTransfers(String upid, String transferId, String fromDate, String endDate);

	/**
	 * Lists pending inbound transfers for the current facility from HIE.
	 * Uses targetOrg from transferapp.sendingFacilityName when set, otherwise the session
	 * location parent name; defaults to the most recent week.
	 */
	@Authorized(TransferAppActivator.PRIVILEGE_LIST_PENDING)
	@Transactional(readOnly = true)
	Map<String, Object> listPendingTransfersForCurrentFacility();

	/**
	 * Lists pending inbound transfers for the requested number of weeks. Values outside 1..4
	 * are normalized to the one-week default.
	 */
	@Authorized(TransferAppActivator.PRIVILEGE_LIST_PENDING)
	@Transactional(readOnly = true)
	Map<String, Object> listPendingTransfersForCurrentFacility(int weeks);

}
