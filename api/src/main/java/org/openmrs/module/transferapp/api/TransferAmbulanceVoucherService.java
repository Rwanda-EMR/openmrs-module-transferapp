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

import org.openmrs.module.transferapp.model.AmbulanceVoucherPage;
import org.openmrs.module.transferapp.model.AmbulanceVoucherPreview;
import org.openmrs.module.transferapp.model.Transfer;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Map;

/**
 * Lists transfers that have a linked ambulance consommation (voucher).
 */
@Transactional
public interface TransferAmbulanceVoucherService {

	/**
	 * All ambulance vouchers whose transfer date falls in {@code [startDate, endDate]}.
	 * Intended for client-side DataTables search/pagination.
	 */
	@Transactional(readOnly = true)
	AmbulanceVoucherPage getVouchers(Date startDate, Date endDate);

	/**
	 * Full BON D'AMBULANCE preview for one transfer (must have ambulance consommation).
	 */
	@Transactional(readOnly = true)
	AmbulanceVoucherPreview getVoucherPreview(String transferUuid);

	/**
	 * Receives an HIE transfer for a local patient (if not already stored) and creates the
	 * ambulance consommation using the insurance card/policy number from the latest registration.
	 * Distance and covered district are supplied by the user; bill description is
	 * "{My Location} via {From} to {To}".
	 */
	Transfer createAmbulanceVoucherFromHie(Integer patientId, String hieTransferId, int kilometers,
			String coveredDistrict);

	/**
	 * Local link state for an HIE transfer id (uuid / whether a voucher already exists).
	 */
	@Transactional(readOnly = true)
	Map<String, Object> resolveLocalVoucherLink(Integer patientId, String hieTransferId);

}
