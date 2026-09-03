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
import org.openmrs.module.transferapp.model.NeonatalTransfer;
import org.openmrs.module.transferapp.model.NeonatalTransferFormData;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Saves and retrieves Neonatal transfer referral records.
 *
 * <p>The save method takes the wizard's already-collected field values as a
 * {@link NeonatalTransferFormData} (built by the controller from individual request
 * parameters — see {@code TransferSaveController#saveNeonatalTransfer}) rather than as
 * ~90 individual positional method parameters, mirroring {@link MaternityTransferService}.</p>
 */
@Transactional
public interface NeonatalTransferService {

	@Authorized(TransferAppActivator.PRIVILEGE_CREATE_TRANSFER)
	NeonatalTransfer saveNeonatalTransfer(Integer patientId, Integer receivingFacilityId,
			NeonatalTransferFormData formData);

	@Authorized(TransferAppActivator.PRIVILEGE_LIST_TRANSFERS)
	@Transactional(readOnly = true)
	List<NeonatalTransfer> getNeonatalTransfersByPatient(Patient patient);

	@Authorized(TransferAppActivator.PRIVILEGE_LIST_TRANSFERS)
	@Transactional(readOnly = true)
	List<NeonatalTransfer> getNeonatalTransfersByPatient(Patient patient, Integer limit);

	@Authorized(TransferAppActivator.PRIVILEGE_LIST_TRANSFERS)
	@Transactional(readOnly = true)
	int countNeonatalTransfersByPatient(Patient patient);

	@Authorized(TransferAppActivator.PRIVILEGE_LIST_TRANSFERS)
	@Transactional(readOnly = true)
	NeonatalTransfer getNeonatalTransferByUuid(String uuid);

}
