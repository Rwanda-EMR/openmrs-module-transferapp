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
package org.openmrs.module.transferapp.hie;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.PersonAddress;
import org.openmrs.PersonName;
import org.openmrs.module.transferapp.api.TransferPatientSnapshotResolver;
import org.openmrs.module.transferapp.model.Transfer;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Builds a FHIR Patient JSON for Client Registry create, matching
 * {@code devs/client_registry_request_sample.json} and OpenMRS {@code person_address} mapping.
 */
public class ClientRegistryPatientPayloadBuilder {

	private static final String[] MOTHER_ATTR_NAMES = new String[] {
			"Mother's Name", "Mothers Name", "Mother Name", "Mother"
	};

	private static final String[] FATHER_ATTR_NAMES = new String[] {
			"Father's Name", "Fathers Name", "Father Name", "Father"
	};

	private static final String[] EDUCATION_ATTR_NAMES = new String[] {
			"Education", "Educational Level", "Education Level"
	};

	private static final String[] PROFESSION_ATTR_NAMES = new String[] {
			"Profession", "Occupation"
	};

	private final ObjectMapper objectMapper = new ObjectMapper();

	private TransferPatientSnapshotResolver patientSnapshotResolver = new TransferPatientSnapshotResolver();

	public void setPatientSnapshotResolver(TransferPatientSnapshotResolver patientSnapshotResolver) {
		this.patientSnapshotResolver = patientSnapshotResolver != null
				? patientSnapshotResolver
				: new TransferPatientSnapshotResolver();
	}

	public String buildPatientJson(Patient patient, Transfer transfer) {
		return buildPatientJson(patient,
				transfer != null ? transfer.getEmrId() : null,
				transfer != null ? transfer.getClientTelephone() : null,
				transfer != null ? transfer.getCaregiverTelephone() : null);
	}

	/**
	 * Form-agnostic overload: builds the same Client Registry patient payload without requiring
	 * an External-transfer-specific {@link Transfer} object. {@code upiFallback} and the two phone
	 * fallbacks are only used when the patient's own OpenMRS UPID / phone can't be resolved.
	 */
	public String buildPatientJson(Patient patient, String upiFallback, String phoneFallback1, String phoneFallback2) {
		if (patient == null) {
			throw new HieApiException("Cannot build Client Registry patient: OpenMRS patient is required");
		}
		String upi = resolveUpi(patient, upiFallback);
		if (StringUtils.isBlank(upi)) {
			throw new HieApiException("Cannot build Client Registry patient: UPID/UPI is required");
		}

		try {
			ObjectNode root = objectMapper.createObjectNode();
			root.put("resourceType", "Patient");
			root.put("id", upi.trim());

			addOptionalExtensions(root, patient);

			ArrayNode identifiers = root.putArray("identifier");
			addIdentifier(identifiers, "NID", resolveIdentifierValue(patient, "NID", "NATIONAL ID", "NATIONAL"));
			addIdentifier(identifiers, "NID_APPLICATION_NUMBER",
					resolveIdentifierValue(patient, "NID_APPLICATION", "NIDA", "APPLICATION"));
			addIdentifier(identifiers, "UPI", upi);
			addIdentifier(identifiers, "NIN", resolveIdentifierValue(patient, "NIN"));

			PersonName personName = patient.getPersonName();
			ObjectNode name = root.putArray("name").addObject();
			if (personName != null) {
				if (StringUtils.isNotBlank(personName.getFamilyName())) {
					name.put("family", personName.getFamilyName().trim());
				}
				ArrayNode given = name.putArray("given");
				addGivenPart(given, personName.getGivenName());
				addGivenPart(given, personName.getMiddleName());
			}

			String phone = firstNonBlank(
					patientSnapshotResolver.resolvePatientPhone(patient),
					phoneFallback1,
					phoneFallback2);
			if (StringUtils.isNotBlank(phone)) {
				root.putArray("telecom").addObject().put("value", phone.trim());
			}

			String gender = mapGender(patient.getGender());
			if (gender != null) {
				root.put("gender", gender);
			}

			String birthDate = formatBirthDate(patient.getBirthdate());
			if (birthDate != null) {
				root.put("birthDate", birthDate);
			}

			PersonAddress address = patientSnapshotResolver.resolveActivePersonAddress(patient);
			if (address != null) {
				root.putArray("address").add(buildAddress(address));
			}

			ArrayNode contacts = null;
			contacts = addContact(root, contacts, resolveAttribute(patient, MOTHER_ATTR_NAMES), "MOTHER NAME");
			addContact(root, contacts, resolveAttribute(patient, FATHER_ATTR_NAMES), "FATHER NAME");

			return objectMapper.writeValueAsString(root);
		}
		catch (HieApiException ex) {
			throw ex;
		}
		catch (Exception ex) {
			throw new HieApiException("Failed to build Client Registry patient payload: " + ex.getMessage(), ex);
		}
	}

	ObjectNode buildAddress(PersonAddress address) {
		ObjectNode addressNode = objectMapper.createObjectNode();
		String country = trim(address.getCountry());
		String state = trim(address.getStateProvince());
		String district = trim(address.getCountyDistrict());
		String sector = trim(address.getCityVillage());
		String cell = trim(address.getAddress3());
		String village = trim(address.getAddress1());

		ArrayNode line = addressNode.putArray("line");
		addLabeledLine(line, "Country", country);
		addLabeledLine(line, "Province", state);
		addLabeledLine(line, "District", district);
		addLabeledLine(line, "Sector", sector);
		addLabeledLine(line, "Cell", cell);
		addLabeledLine(line, "Village", village);

		putIfPresent(addressNode, "city", sector);
		putIfPresent(addressNode, "district", district);
		putIfPresent(addressNode, "state", state);
		putIfPresent(addressNode, "country", firstNonBlank(country, "Rwanda"));
		return addressNode;
	}

	private void addOptionalExtensions(ObjectNode root, Patient patient) {
		String education = resolveAttribute(patient, EDUCATION_ATTR_NAMES);
		String profession = resolveAttribute(patient, PROFESSION_ATTR_NAMES);
		if (StringUtils.isBlank(education) && StringUtils.isBlank(profession)) {
			return;
		}
		ArrayNode extensions = root.putArray("extension");
		if (StringUtils.isNotBlank(education)) {
			ObjectNode ext = extensions.addObject();
			ext.put("url", "https://fhir.hie.moh.gov.rw/fhir/StructureDefinition/extensions/patient-educational-level");
			ext.put("valueString", education);
		}
		if (StringUtils.isNotBlank(profession)) {
			ObjectNode ext = extensions.addObject();
			ext.put("url", "https://fhir.hie.moh.gov.rw/fhir/StructureDefinition/extensions/patient-profession");
			ext.put("valueString", profession);
		}
	}

	private String resolveUpi(Patient patient, String upiFallback) {
		String upi = patientSnapshotResolver.resolveUpid(patient);
		if (StringUtils.isBlank(upi)) {
			upi = firstNonBlank(upiFallback, null);
		}
		return StringUtils.trimToNull(upi);
	}

	private String resolveIdentifierValue(Patient patient, String... typeTokens) {
		if (patient == null || typeTokens == null) {
			return null;
		}
		for (PatientIdentifier identifier : patient.getActiveIdentifiers()) {
			if (identifier == null || identifier.getIdentifierType() == null) {
				continue;
			}
			String typeName = identifier.getIdentifierType().getName();
			if (StringUtils.isBlank(typeName) || StringUtils.isBlank(identifier.getIdentifier())) {
				continue;
			}
			String upper = typeName.toUpperCase(Locale.ENGLISH);
			// Prefer exact-ish matches; skip UPID when looking for NID/NIN.
			if (upper.contains("UPID") || upper.equals("UPI")) {
				continue;
			}
			for (String token : typeTokens) {
				if (token != null && upper.contains(token.toUpperCase(Locale.ENGLISH))) {
					return identifier.getIdentifier().trim();
				}
			}
		}
		return null;
	}

	private String resolveAttribute(Patient patient, String[] attributeNames) {
		return patientSnapshotResolver.resolvePersonAttribute(patient, attributeNames);
	}

	private static void addIdentifier(ArrayNode identifiers, String system, String value) {
		if (StringUtils.isBlank(value)) {
			return;
		}
		ObjectNode identifier = identifiers.addObject();
		identifier.put("system", system);
		identifier.put("value", value.trim());
	}

	private static void addGivenPart(ArrayNode given, String value) {
		if (StringUtils.isNotBlank(value)) {
			given.add(value.trim());
		}
	}

	private static void addLabeledLine(ArrayNode line, String label, String value) {
		if (StringUtils.isNotBlank(value)) {
			line.add(label + ": " + value.trim());
		}
	}

	private static void putIfPresent(ObjectNode node, String field, String value) {
		if (StringUtils.isNotBlank(value)) {
			node.put(field, value.trim());
		}
	}

	private static ArrayNode addContact(ObjectNode patient, ArrayNode contacts, String fullName,
			String defaultFamily) {
		if (StringUtils.isBlank(fullName)) {
			return contacts;
		}
		ArrayNode contactArray = contacts;
		if (contactArray == null) {
			contactArray = patient.putArray("contact");
		}
		String[] parts = fullName.trim().split("\\s+", 2);
		ObjectNode contactName = contactArray.addObject().putObject("name");
		if (parts.length > 1) {
			contactName.putArray("given").add(parts[0]);
			contactName.put("family", parts[1]);
		}
		else {
			contactName.put("family", defaultFamily);
			contactName.putArray("given").add(parts[0]);
		}
		return contactArray;
	}

	private static String mapGender(String sex) {
		if (StringUtils.isBlank(sex)) {
			return null;
		}
		String upper = sex.trim().toUpperCase(Locale.ENGLISH);
		if ("M".equals(upper) || "MALE".equals(upper)) {
			return "male";
		}
		if ("F".equals(upper) || "FEMALE".equals(upper)) {
			return "female";
		}
		return "other";
	}

	private static String formatBirthDate(Date birthdate) {
		if (birthdate == null) {
			return null;
		}
		return new SimpleDateFormat("yyyy-MM-dd").format(birthdate);
	}

	private static String trim(String value) {
		return StringUtils.trimToNull(value);
	}

	private static String firstNonBlank(String... values) {
		if (values == null) {
			return null;
		}
		for (String value : values) {
			if (StringUtils.isNotBlank(value)) {
				return value.trim();
			}
		}
		return null;
	}
}
