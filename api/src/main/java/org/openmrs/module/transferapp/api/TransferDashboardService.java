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

import org.openmrs.Concept;
import org.openmrs.annotation.Authorized;
import org.openmrs.module.transferapp.TransferAppActivator;
import org.springframework.transaction.annotation.Transactional;

/**
 * Provides dashboard statistics for patient transfers.
 */
@Transactional
public interface TransferDashboardService {

	@Authorized(TransferAppActivator.PRIVILEGE_DASHBOARD)
	@Transactional(readOnly = true)
	TransferReceivedStatistics getReceivedTransferStatistics();

	@Authorized(TransferAppActivator.PRIVILEGE_DASHBOARD)
	@Transactional(readOnly = true)
	TransferReceivedStatistics getSentTransferStatistics();

	/**
	 * Counts ambulance vouchers created at this facility (outbound transfers) and
	 * vouchers created here for HIE transfers where this facility is the ambulance provider.
	 */
	@Authorized(TransferAppActivator.PRIVILEGE_DASHBOARD)
	@Transactional(readOnly = true)
	TransferReceivedStatistics getAmbulanceVoucherStatistics();

	@Authorized(TransferAppActivator.PRIVILEGE_DASHBOARD)
	@Transactional(readOnly = true)
	Concept getReceivedTransferConcept();

}
