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
import org.openmrs.PersonAddress;
import org.openmrs.PersonAttribute;
import org.openmrs.module.transferapp.model.Transfer;

import java.util.Date;
import java.util.List;

public interface TransferDao {

	Transfer saveTransfer(Transfer transfer);

	List<Transfer> getTransfersByPatient(Patient patient);

	List<Transfer> getTransfersByPatient(Patient patient, Integer limit);

	int countTransfersByPatient(Patient patient);

	List<Transfer> getOutboundTransfersBySendingFacility(String sendingFacility, Integer patientId, Integer limit);

	List<Transfer> getOutboundTransfersBySendingFacility(String sendingFacility, Integer patientId, Integer limit,
			Date startDate, Date endDate, String receivingFacilityCode);

	/**
	 * Transfers with an ambulance consommation for this facility: outbound
	 * ({@code sendingFacility}) and/or where this facility is the ambulance provider
	 * ({@code ambulanceProviderFosaId}).
	 */
	List<Transfer> getAmbulanceVoucherTransfers(String sendingFacility, String ambulanceProviderFosaId,
			Date startDate, Date endDate, Integer firstResult, Integer maxResults);

	int countAmbulanceVoucherTransfers(String sendingFacility, String ambulanceProviderFosaId,
			Date startDate, Date endDate);

	int countOutboundTransfers(String sendingFacility, Date fromDate, Boolean hieSent);

	Transfer getTransferByUuid(String uuid);

	Transfer getTransferByHieTransferId(Integer patientId, String hieTransferId);

	PersonAddress getPreferredPersonAddress(Integer personId);

	List<PersonAttribute> getPersonAttributes(Integer personId);

}
