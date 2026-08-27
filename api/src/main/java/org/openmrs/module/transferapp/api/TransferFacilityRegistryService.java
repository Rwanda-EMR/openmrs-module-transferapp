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
import org.openmrs.module.transferapp.model.RegistryFacility;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Transactional(readOnly = true)
public interface TransferFacilityRegistryService {

	@Authorized(TransferAppActivator.PRIVILEGE_DASHBOARD)
	List<RegistryFacility> listReceivingFacilitiesFromHie();

	@Authorized(TransferAppActivator.PRIVILEGE_LIST_TRANSFERS)
	List<RegistryFacility> listCounterReferralFacilitiesFromHie();

	/**
	 * Facilities that may provide an ambulance vehicle (health centres through hospitals/clinics).
	 */
	@Authorized(value = {
			TransferAppActivator.PRIVILEGE_CREATE_TRANSFER,
			TransferAppActivator.PRIVILEGE_LIST_TRANSFERS,
			TransferAppActivator.PRIVILEGE_DASHBOARD }, requireAll = false)
	List<RegistryFacility> listAmbulanceProviderFacilitiesFromHie();

}
