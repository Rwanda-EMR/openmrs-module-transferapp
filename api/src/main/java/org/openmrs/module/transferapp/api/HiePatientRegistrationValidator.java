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
import org.openmrs.api.APIException;

import java.util.Map;

/**
 * Validates demographics required to safely register an HIE patient in OpenMRS.
 */
public final class HiePatientRegistrationValidator {

	public static final String MISSING_REQUIRED_DEMOGRAPHICS_MESSAGE =
			"Patient registration cannot be confirmed because given name, family name, date of birth, "
					+ "and gender are required from the HIE client registry. Please register this patient manually.";

	private HiePatientRegistrationValidator() {
	}

	public static boolean hasRequiredDemographics(Map<String, Object> registrationFields) {
		return registrationFields != null
				&& hasText(registrationFields.get("givenName"))
				&& hasText(registrationFields.get("familyName"))
				&& hasText(registrationFields.get("birthdate"))
				&& hasText(registrationFields.get("gender"));
	}

	public static void requireRequiredDemographics(Patient patient) {
		if (patient == null
				|| StringUtils.isBlank(patient.getGivenName())
				|| StringUtils.isBlank(patient.getFamilyName())
				|| patient.getBirthdate() == null
				|| StringUtils.isBlank(patient.getGender())) {
			throw new APIException(MISSING_REQUIRED_DEMOGRAPHICS_MESSAGE);
		}
	}

	private static boolean hasText(Object value) {
		return value != null && StringUtils.isNotBlank(String.valueOf(value));
	}
}
