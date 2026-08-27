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

import org.apache.commons.lang.StringUtils;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifierType;
import org.openmrs.api.APIException;
import org.openmrs.api.PatientService;
import org.openmrs.module.transferapp.TransferAppConstants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class PendingTransferPatientStatusResolver {

	private PendingTransferPatientStatusResolver() {
	}

	public static List<Map<String, Object>> addPatientStatus(List<Map<String, Object>> transfers,
			PatientService patientService) {
		if (transfers == null || transfers.isEmpty()) {
			return Collections.emptyList();
		}

		PatientIdentifierType upidIdentifierType = requireUpidIdentifierType(patientService);

		Map<String, Patient> existingPatientsByUpid = new HashMap<String, Patient>();
		Set<String> checkedUpids = new HashSet<String>();
		List<Map<String, Object>> resolvedTransfers = new ArrayList<Map<String, Object>>(transfers.size());
		for (Map<String, Object> transfer : transfers) {
			Map<String, Object> resolvedTransfer = transfer != null
					? new LinkedHashMap<String, Object>(transfer)
					: new LinkedHashMap<String, Object>();
			String upid = resolveUpid(transfer);
			Patient existingPatient = null;
			if (upid != null) {
				if (!checkedUpids.contains(upid)) {
					Patient match = findPatientByUpid(patientService, upidIdentifierType, upid);
					if (match != null) {
						existingPatientsByUpid.put(upid, match);
					}
					checkedUpids.add(upid);
				}
				existingPatient = existingPatientsByUpid.get(upid);
			}
			resolvedTransfer.put(TransferAppConstants.PENDING_TRANSFER_EXISTING_PATIENT_KEY,
					existingPatient != null);
			resolvedTransfer.put(TransferAppConstants.PENDING_TRANSFER_PATIENT_REFERENCE_KEY,
					patientReference(existingPatient));
			resolvedTransfers.add(resolvedTransfer);
		}
		return resolvedTransfers;
	}

	/**
	 * @return local OpenMRS patient for the UPID, or {@code null} when not registered locally
	 */
	public static Patient findLocalPatientByUpid(PatientService patientService, String upid) {
		String normalized = StringUtils.trimToNull(upid);
		if (normalized == null || patientService == null) {
			return null;
		}
		PatientIdentifierType upidIdentifierType = requireUpidIdentifierType(patientService);
		return findPatientByUpid(patientService, upidIdentifierType, normalized);
	}

	public static String patientReference(Patient patient) {
		if (patient == null) {
			return "";
		}
		String uuid = StringUtils.trimToNull(patient.getUuid());
		return uuid != null ? uuid : patient.getId() != null ? String.valueOf(patient.getId()) : "";
	}

	private static PatientIdentifierType requireUpidIdentifierType(PatientService patientService) {
		PatientIdentifierType upidIdentifierType = patientService.getPatientIdentifierTypeByName(
				TransferAppConstants.UPID_IDENTIFIER_TYPE_NAME);
		if (upidIdentifierType == null) {
			throw new APIException("UPID patient identifier type is not configured");
		}
		return upidIdentifierType;
	}

	private static Patient findPatientByUpid(PatientService patientService, PatientIdentifierType upidIdentifierType,
			String upid) {
		List<Patient> matches = patientService.getPatients(null, upid,
				Collections.singletonList(upidIdentifierType), true);
		if (matches == null || matches.isEmpty()) {
			return null;
		}
		return matches.get(0);
	}

	private static String resolveUpid(Map<String, Object> transfer) {
		if (transfer == null) {
			return null;
		}
		String upid = StringUtils.trimToNull(stringValue(transfer.get("upid")));
		return upid != null ? upid : StringUtils.trimToNull(stringValue(transfer.get("subject")));
	}

	private static String stringValue(Object value) {
		return value != null ? String.valueOf(value) : null;
	}
}
