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
package org.openmrs.module.transferapp.fragment.controller.patient;

import org.openmrs.Patient;
import org.openmrs.api.PatientService;
import org.openmrs.api.context.Context;
import org.openmrs.module.transferapp.TransferAppActivator;
import org.openmrs.module.transferapp.TransferPrivilegeHelper;
import org.openmrs.module.transferapp.api.NewNeonatalTransferOutService;
import org.openmrs.module.transferapp.model.NeonatalTransferFormData;
import org.openmrs.ui.framework.UiUtils;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.fragment.FragmentModel;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Loads new Neonatal transfer out form content for the patient dashboard modal.
 */
public class NewNeonatalTransferOutFormFragmentController {

	public void controller(FragmentModel model,
			UiUtils ui,
			@RequestParam(value = "patientId", required = false) Integer patientId,
			@RequestParam(value = "transferUuid", required = false) String transferUuid,
			@SpringBean("newNeonatalTransferOutService") NewNeonatalTransferOutService newNeonatalTransferOutService) {

		model.addAttribute("error", "");
		model.addAttribute("formData", null);

		if (!TransferPrivilegeHelper.hasPrivilege(TransferAppActivator.PRIVILEGE_CREATE_TRANSFER)) {
			model.addAttribute("error",
					TransferPrivilegeHelper.requiredPrivilegeMessage(TransferAppActivator.PRIVILEGE_CREATE_TRANSFER));
			return;
		}

		if (patientId == null) {
			model.addAttribute("error", ui.message("transferapp.patient.transfers.wizard.missingPatient"));
			return;
		}

		PatientService patientService = Context.getPatientService();
		Patient patient = patientService.getPatient(patientId);
		if (patient == null) {
			model.addAttribute("error", ui.message("transferapp.patient.transfers.wizard.patientNotFound"));
			return;
		}

		try {
			NeonatalTransferFormData formData = newNeonatalTransferOutService.getNeonatalTransferFormData(patient, transferUuid);
			model.addAttribute("formData", formData);
		}
		catch (Exception ex) {
			model.addAttribute("error", TransferPrivilegeHelper.resolveUserFacingMessage(
					ex,
					TransferAppActivator.PRIVILEGE_CREATE_TRANSFER,
					ui.message("transferapp.patient.transfers.wizard.loadError")));
		}
	}

}
