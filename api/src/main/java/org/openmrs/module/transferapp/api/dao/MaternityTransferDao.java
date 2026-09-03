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
package org.openmrs.module.transferapp.api.dao;

import org.openmrs.Patient;
import org.openmrs.module.transferapp.model.MaternityTransfer;

import java.util.Date;
import java.util.List;

public interface MaternityTransferDao {

	MaternityTransfer saveMaternityTransfer(MaternityTransfer maternityTransfer);

	List<MaternityTransfer> getMaternityTransfersByPatient(Patient patient);

	List<MaternityTransfer> getMaternityTransfersByPatient(Patient patient, Integer limit);

	int countMaternityTransfersByPatient(Patient patient);

	List<MaternityTransfer> getOutboundMaternityTransfersBySendingFacility(String sendingFacility, Integer patientId,
			Integer limit);

	List<MaternityTransfer> getOutboundMaternityTransfersBySendingFacility(String sendingFacility, Integer patientId,
			Integer limit, Date startDate, Date endDate, String receivingFacilityCode);

	int countOutboundMaternityTransfers(String sendingFacility, Date fromDate);

	int countOutboundMaternityTransfers(String sendingFacility, Date fromDate, Boolean hieSent);

	MaternityTransfer getMaternityTransferByUuid(String uuid);

}
