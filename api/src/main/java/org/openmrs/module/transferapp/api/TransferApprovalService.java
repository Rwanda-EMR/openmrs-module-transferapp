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
import org.openmrs.module.transferapp.model.TransferApprovalItem;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Transactional
public interface TransferApprovalService {

	@Transactional(readOnly = true)
	List<TransferApprovalItem> getPendingApprovals();

	@Transactional(readOnly = true)
	int countPendingApprovals();

	/**
	 * Approves a pending external transfer, records the approver, and submits it to HIE.
	 */
	Transfer approveTransfer(String transferUuid);

	/**
	 * Rejects a pending external transfer. Reason is required.
	 */
	Transfer rejectTransfer(String transferUuid, String reason);
}
