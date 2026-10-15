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
package org.openmrs.module.transferapp.page.controller.patient;

import org.openmrs.ui.framework.page.PageModel;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Ensures edit {@code transferUuid} (and patientId) from the query string are on the page
 * model so the included wizard fragment always receives the selected transfer.
 */
public class NewTransferOutFormPageController {

	public void controller(PageModel model,
			@RequestParam(value = "patientId", required = false) Integer patientId,
			@RequestParam(value = "transferUuid", required = false) String transferUuid) {
		model.addAttribute("patientId", patientId);
		model.addAttribute("transferUuid", transferUuid);
	}
}
