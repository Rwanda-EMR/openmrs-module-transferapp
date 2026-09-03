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

import org.openmrs.module.transferapp.model.Transfer;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates, updates, or deletes ambulance bills via mohbilling when transport is Ambulance.
 * Stores {@code ambulanceConsommationId} on the transfer for later sync.
 */
@Transactional
public interface TransferAmbulanceBillingService {

	/**
	 * Sync ambulance billing after a local transfer save.
	 *
	 * @param transfer saved transfer
	 * @param previousReceivingFacilityCode destination before this save (null on create)
	 * @param previousTransportType transport type before this save (null on create)
	 * @return transfer with ambulanceConsommationId updated when a bill was created
	 */
	Transfer syncAmbulanceBill(Transfer transfer, String previousReceivingFacilityCode, String previousTransportType);

	/**
	 * Creates an ambulance bill for an already-stored transfer using the same mohbilling
	 * {@code createAmbulanceBill} path as outbound transfers, with caller-supplied distance
	 * and description (e.g. From / Via / To route text).
	 */
	Transfer createAmbulanceBillWithRoute(Transfer transfer, int kilometers, String description);

}
