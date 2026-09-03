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

import org.openmrs.Location;

import java.util.Map;

/**
 * Retrieves a patient from the Rwanda HIE client registry and maps it to Registration App field names.
 */
public interface ClientRegistryRegistrationService {

	boolean isHieEnabled();

	String getUpidIdentifierTypeUuid();

	/**
	 * @return registration field values, or {@code null} when the UPID has no unique client-registry match
	 */
	Map<String, Object> findRegistrationFieldsByUpid(String upid);

	/**
	 * Looks up a patient in the Client Registry by National ID (NID) and returns their UPID.
	 *
	 * @return UPID when a unique match is found; {@code null} otherwise
	 */
	String findUpidByNationalId(String nationalId);

	/**
	 * Retrieves the HIE patient and saves it through the normal OpenMRS registration service.
	 * Returns the existing patient without creating a duplicate when the UPID is already registered.
	 */
	HiePatientRegistrationResult registerPatientByUpid(String upid, Location identifierLocation);
}
