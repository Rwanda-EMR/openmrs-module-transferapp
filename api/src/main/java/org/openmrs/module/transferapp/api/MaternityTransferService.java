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
import org.openmrs.module.transferapp.model.MaternityTransfer;
import org.openmrs.module.transferapp.model.MaternityTransferFormData;
import org.openmrs.module.transferapp.model.MaternityTransferTreatmentRow;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Saves and retrieves Maternity/ANC-Delivery-PNC transfer referral records.
 *
 * <p>The save method takes the wizard's already-collected field values as a
 * {@link MaternityTransferFormData} (built by the controller from individual
 * request parameters — see {@code TransferSaveController#saveMaternityTransfer}) rather
 * than as ~45 individual positional method parameters, to keep the signature reviewable
 * and safe to hand-verify without a compiler available in this environment. This
 * mirrors {@link TransferService#saveReferralTransfer} in spirit (a plain data holder
 * rather than the persistent entity, and no Spring form-binding annotations on the
 * controller side) while staying maintainable at this field count.</p>
 */
@Transactional
public interface MaternityTransferService {

	@Authorized(TransferAppActivator.PRIVILEGE_CREATE_TRANSFER)
	MaternityTransfer saveMaternityTransfer(Integer patientId, Integer receivingFacilityId,
			MaternityTransferFormData formData, List<MaternityTransferTreatmentRow> treatmentRows);

	@Authorized(TransferAppActivator.PRIVILEGE_LIST_TRANSFERS)
	@Transactional(readOnly = true)
	List<MaternityTransfer> getMaternityTransfersByPatient(Patient patient);

	@Authorized(TransferAppActivator.PRIVILEGE_LIST_TRANSFERS)
	@Transactional(readOnly = true)
	List<MaternityTransfer> getMaternityTransfersByPatient(Patient patient, Integer limit);

	@Authorized(TransferAppActivator.PRIVILEGE_LIST_TRANSFERS)
	@Transactional(readOnly = true)
	int countMaternityTransfersByPatient(Patient patient);

	@Authorized(TransferAppActivator.PRIVILEGE_LIST_TRANSFERS)
	@Transactional(readOnly = true)
	MaternityTransfer getMaternityTransferByUuid(String uuid);

}
