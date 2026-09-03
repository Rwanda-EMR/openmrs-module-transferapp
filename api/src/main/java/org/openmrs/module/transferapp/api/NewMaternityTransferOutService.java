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

import org.openmrs.Patient;
import org.openmrs.annotation.Authorized;
import org.openmrs.module.transferapp.TransferAppActivator;
import org.openmrs.module.transferapp.model.MaternityTransferFormData;
import org.springframework.transaction.annotation.Transactional;

/**
 * Provides data for the new Maternity/ANC-Delivery-PNC transfer out form.
 */
@Transactional
public interface NewMaternityTransferOutService {

	@Authorized(TransferAppActivator.PRIVILEGE_CREATE_TRANSFER)
	@Transactional(readOnly = true)
	MaternityTransferFormData getMaternityTransferFormData(Patient patient);

	@Authorized(TransferAppActivator.PRIVILEGE_CREATE_TRANSFER)
	@Transactional(readOnly = true)
	MaternityTransferFormData getMaternityTransferFormData(Patient patient, String transferUuid);

}
