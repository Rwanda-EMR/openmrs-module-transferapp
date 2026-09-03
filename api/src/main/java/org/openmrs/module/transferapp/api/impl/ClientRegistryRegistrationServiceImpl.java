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
package org.openmrs.module.transferapp.api.impl;

import org.apache.commons.lang.StringUtils;
import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.StringType;
import org.openmrs.Patient;
import org.openmrs.Location;
import org.openmrs.PatientIdentifier;
import org.openmrs.PatientIdentifierType;
import org.openmrs.PersonAddress;
import org.openmrs.PersonAttribute;
import org.openmrs.PersonAttributeType;
import org.openmrs.PersonName;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.APIException;
import org.openmrs.api.LocationService;
import org.openmrs.api.PatientService;
import org.openmrs.api.context.Context;
import org.openmrs.module.addresshierarchy.AddressHierarchyEntry;
import org.openmrs.module.addresshierarchy.service.AddressHierarchyService;
import org.openmrs.module.rwandaemr.RwandaEmrConfig;
import org.openmrs.module.rwandaemr.integration.ClientRegistryPatient;
import org.openmrs.module.rwandaemr.integration.ClientRegistryPatientProvider;
import org.openmrs.module.rwandaemr.integration.ClientRegistryPatientTranslator;
import org.openmrs.module.rwandaemr.integration.IntegrationConfig;
import org.openmrs.module.transferapp.TransferAppConstants;
import org.openmrs.module.transferapp.api.ClientRegistryRegistrationService;
import org.openmrs.module.transferapp.api.HiePatientRegistrationValidator;
import org.openmrs.module.transferapp.api.HiePatientRegistrationResult;
import org.openmrs.module.registrationcore.api.RegistrationCoreService;
import org.openmrs.util.OpenmrsConstants;

import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Calendar;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Uses RwandaEMR's configured HIE client-registry integration so credentials and FHIR mappings stay centralized.
 */
public class ClientRegistryRegistrationServiceImpl implements ClientRegistryRegistrationService {

	static final String INVALID_NATIONAL_ID_MESSAGE =
			"National ID is invalid. It must contain exactly 16 digits.";

	static final String INVALID_ADDRESS_MESSAGE =
			"Address is invalid. Country, Province, District, Sector, Cell, and Village must match the Rwanda address hierarchy.";

	private IntegrationConfig integrationConfig;

	private ClientRegistryPatientProvider clientRegistryPatientProvider;

	private ClientRegistryPatientTranslator clientRegistryPatientTranslator;

	private RwandaEmrConfig rwandaEmrConfig;

	private PatientService patientService;

	private LocationService locationService;

	private RegistrationCoreService registrationCoreService;

	private AddressHierarchyService addressHierarchyService;

	private AdministrationService administrationService;

	@Override
	public boolean isHieEnabled() {
		return integrationConfig != null && integrationConfig.isHieEnabled();
	}

	@Override
	public String getUpidIdentifierTypeUuid() {
		PatientIdentifierType upidType = rwandaEmrConfig != null ? rwandaEmrConfig.getUPID() : null;
		return upidType != null ? StringUtils.trimToEmpty(upidType.getUuid()) : "";
	}

	@Override
	public Map<String, Object> findRegistrationFieldsByUpid(String upid) {
		String normalizedUpid = StringUtils.trimToNull(upid);
		if (normalizedUpid == null) {
			throw new IllegalArgumentException("UPID is required");
		}
		if (!isHieEnabled()) {
			throw new IllegalStateException("The HIE connection is not enabled on this server");
		}

		ClientRegistryPatient registryPatient = clientRegistryPatientProvider.fetchPatientFromClientRegistry(
				normalizedUpid, IntegrationConfig.IDENTIFIER_SYSTEM_UPI);
		if (registryPatient == null) {
			return null;
		}

		Patient patient = clientRegistryPatientTranslator.toPatient(registryPatient);
		validateNationalId(patient);
		validateAddress(patient);
		Map<String, Object> fields = toRegistrationFields(registryPatient, patient);
		fields.put("upid", normalizedUpid);
		putIfNotBlank(fields, "photo", findFhirPhoto(registryPatient));
		return fields;
	}

	@Override
	public String findUpidByNationalId(String nationalId) {
		String normalizedNid = StringUtils.trimToNull(nationalId);
		if (normalizedNid == null) {
			throw new IllegalArgumentException("National ID is required");
		}
		normalizedNid = normalizedNid.replaceAll("\\s+", "");
		if (!normalizedNid.matches("\\d{16}")) {
			throw new IllegalArgumentException(INVALID_NATIONAL_ID_MESSAGE);
		}
		if (!isHieEnabled()) {
			throw new IllegalStateException("The HIE connection is not enabled on this server");
		}
		if (clientRegistryPatientProvider == null) {
			return null;
		}

		ClientRegistryPatient registryPatient = clientRegistryPatientProvider.fetchPatientFromClientRegistry(
				normalizedNid, IntegrationConfig.IDENTIFIER_SYSTEM_NID);
		if (registryPatient == null) {
			return null;
		}
		return StringUtils.trimToNull(registryPatient.getIdentifierValue(IntegrationConfig.IDENTIFIER_SYSTEM_UPI));
	}

	@Override
	public synchronized HiePatientRegistrationResult registerPatientByUpid(String upid, Location identifierLocation) {
		String normalizedUpid = StringUtils.trimToNull(upid);
		if (normalizedUpid == null) {
			throw new IllegalArgumentException("UPID is required");
		}
		if (!isHieEnabled()) {
			throw new IllegalStateException("The HIE connection is not enabled on this server");
		}

		PatientIdentifierType upidType = rwandaEmrConfig.getUPID();
		if (upidType == null) {
			throw new APIException("UPID patient identifier type is not configured");
		}

		Patient existingPatient = findPatientByIdentifier(normalizedUpid, upidType);
		if (existingPatient != null) {
			return new HiePatientRegistrationResult(existingPatient, false);
		}

		ClientRegistryPatient registryPatient = clientRegistryPatientProvider.fetchPatientFromClientRegistry(
				normalizedUpid, IntegrationConfig.IDENTIFIER_SYSTEM_UPI);
		if (registryPatient == null) {
			throw new APIException("No patient was found in the HIE client registry for UPID " + normalizedUpid);
		}

		Patient patient = clientRegistryPatientTranslator.toPatient(registryPatient);
		applyRegistryName(registryPatient, patient);
		normalizePatientNamesForConfiguredValidation(patient);
		validateNationalId(patient);
		validateAddress(patient);
		HiePatientRegistrationValidator.requireRequiredDemographics(patient);
		Location resolvedLocation = identifierLocation != null ? identifierLocation : locationService.getDefaultLocation();
		ensureIdentifier(patient, normalizedUpid, upidType, resolvedLocation);
		for (PatientIdentifier identifier : patient.getIdentifiers()) {
			if (identifier.getLocation() == null) {
				identifier.setLocation(resolvedLocation);
			}
		}

		Patient registeredPatient = registrationCoreService.registerPatient(
				patient, Collections.emptyList(), resolvedLocation);
		return new HiePatientRegistrationResult(registeredPatient, true);
	}

	private void validateNationalId(Patient patient) {
		PatientIdentifierType nationalIdType = rwandaEmrConfig.getNationalId();
		if (nationalIdType == null || patient == null) {
			return;
		}

		List<PatientIdentifier> emptyNationalIds = new ArrayList<PatientIdentifier>();
		for (PatientIdentifier identifier : patient.getActiveIdentifiers()) {
			if (nationalIdType.equals(identifier.getIdentifierType())) {
				String nationalId = StringUtils.trimToNull(identifier.getIdentifier());
				if (nationalId == null) {
					emptyNationalIds.add(identifier);
				} else if (!nationalId.matches("[0-9]{16}")) {
					throw new APIException(INVALID_NATIONAL_ID_MESSAGE);
				}
			}
		}
		for (PatientIdentifier emptyNationalId : emptyNationalIds) {
			patient.removeIdentifier(emptyNationalId);
		}
	}

	void validateAddress(Patient patient) {
		if (patient == null || patient.getAddresses() == null || patient.getAddresses().isEmpty()
				|| !isPatientAddressValidationEnabled()) {
			return;
		}

		List<PersonAddress> emptyAddresses = new ArrayList<PersonAddress>();
		for (PersonAddress address : patient.getAddresses()) {
			if (Boolean.TRUE.equals(address.getVoided())) {
				continue;
			}

			String country = StringUtils.trimToNull(address.getCountry());
			String province = StringUtils.trimToNull(address.getStateProvince());
			String district = StringUtils.trimToNull(address.getCountyDistrict());
			String sector = StringUtils.trimToNull(address.getCityVillage());
			String cell = StringUtils.trimToNull(address.getAddress3());
			String village = StringUtils.trimToNull(address.getAddress1());

			if (country == null && province == null && district == null && sector == null
					&& cell == null && village == null) {
				emptyAddresses.add(address);
				continue;
			}
			if (country == null || province == null || district == null || sector == null
					|| cell == null || village == null) {
				throw new APIException(INVALID_ADDRESS_MESSAGE);
			}

			AddressHierarchyService hierarchyService = getAddressHierarchyService();
			AddressHierarchyEntry hierarchyEntry = hierarchyService
					.getChildAddressHierarchyEntryByName(null, country);
			String[] childNames = { province, district, sector, cell, village };
			for (String childName : childNames) {
				if (hierarchyEntry == null) {
					throw new APIException(INVALID_ADDRESS_MESSAGE);
				}
				hierarchyEntry = hierarchyService.getChildAddressHierarchyEntryByName(hierarchyEntry, childName);
			}
			if (hierarchyEntry == null) {
				throw new APIException(INVALID_ADDRESS_MESSAGE);
			}
		}

		for (PersonAddress emptyAddress : emptyAddresses) {
			patient.removeAddress(emptyAddress);
		}
	}

	private AddressHierarchyService getAddressHierarchyService() {
		return addressHierarchyService != null ? addressHierarchyService
				: Context.getService(AddressHierarchyService.class);
	}

	boolean isPatientAddressValidationEnabled() {
		String configuredValue = getAdministrationService().getGlobalProperty(
				TransferAppConstants.GP_HIE_VALIDATE_PATIENT_ADDRESS);
		return Boolean.parseBoolean(StringUtils.trimToEmpty(configuredValue));
	}

	private AdministrationService getAdministrationService() {
		return administrationService != null ? administrationService : Context.getAdministrationService();
	}

	private Patient findPatientByIdentifier(String identifier, PatientIdentifierType identifierType) {
		List<Patient> matches = patientService.getPatients(null, identifier,
				Collections.singletonList(identifierType), true);
		if (matches != null) {
			for (Patient patient : matches) {
				for (PatientIdentifier patientIdentifier : patient.getActiveIdentifiers()) {
					if (identifierType.equals(patientIdentifier.getIdentifierType())
							&& identifier.equalsIgnoreCase(StringUtils.trimToEmpty(patientIdentifier.getIdentifier()))) {
						return patient;
					}
				}
			}
		}
		return null;
	}

	private void ensureIdentifier(Patient patient, String identifier, PatientIdentifierType type, Location location) {
		for (PatientIdentifier patientIdentifier : patient.getIdentifiers()) {
			if (type.equals(patientIdentifier.getIdentifierType())
					&& identifier.equalsIgnoreCase(StringUtils.trimToEmpty(patientIdentifier.getIdentifier()))) {
				return;
			}
		}
		patient.addIdentifier(new PatientIdentifier(identifier, type, location));
	}

	private void applyRegistryName(ClientRegistryPatient registryPatient, Patient patient) {
		HumanName registryName = registryPatient.getName();
		PersonName patientName = patient.getPersonName();
		if (registryName == null || patientName == null) {
			return;
		}

		List<StringType> givenNames = registryName.getGiven();
		if (givenNames != null && !givenNames.isEmpty()) {
			patientName.setGivenName(givenNames.get(0).getValue());
			StringBuilder middleNames = new StringBuilder();
			for (int i = 1; i < givenNames.size(); i++) {
				String middleName = StringUtils.trimToNull(givenNames.get(i).getValue());
				if (middleName != null) {
					if (middleNames.length() > 0) {
						middleNames.append(' ');
					}
					middleNames.append(middleName);
				}
			}
			patientName.setMiddleName(StringUtils.trimToNull(middleNames.toString()));
		}
		if (StringUtils.isNotBlank(registryName.getFamily())) {
			patientName.setFamilyName(registryName.getFamily());
		}
	}

	void normalizePatientNamesForConfiguredValidation(Patient patient) {
		if (patient == null || patient.getNames() == null || patient.getNames().isEmpty()) {
			return;
		}

		String namePattern = StringUtils.trimToNull(getAdministrationService().getGlobalProperty(
				OpenmrsConstants.GLOBAL_PROPERTY_PATIENT_NAME_REGEX));
		if (namePattern == null) {
			return;
		}

		for (PersonName name : patient.getNames()) {
			if (name == null || Boolean.TRUE.equals(name.getVoided())) {
				continue;
			}
			name.setGivenName(normalizeNameForValidation(name.getGivenName(), namePattern));
			name.setMiddleName(normalizeNameForValidation(name.getMiddleName(), namePattern));
			name.setFamilyName(normalizeNameForValidation(name.getFamilyName(), namePattern));
			name.setFamilyName2(normalizeNameForValidation(name.getFamilyName2(), namePattern));
		}
	}

	static String normalizeNameForValidation(String value, String namePattern) {
		String trimmed = StringUtils.trimToNull(value);
		if (trimmed == null || StringUtils.isBlank(namePattern)) {
			return trimmed;
		}
		try {
			if (trimmed.matches(namePattern)) {
				return trimmed;
			}
			String withoutAccents = Normalizer.normalize(trimmed, Normalizer.Form.NFD)
					.replaceAll("\\p{M}+", "");
			return withoutAccents.matches(namePattern) ? withoutAccents : trimmed;
		}
		catch (RuntimeException ignored) {
			// Let OpenMRS report a malformed configured pattern through its normal validator.
			return trimmed;
		}
	}

	Map<String, Object> toRegistrationFields(ClientRegistryPatient registryPatient, Patient patient) {
		Map<String, Object> fields = toRegistrationFields(patient);
		if (registryPatient == null) {
			return fields;
		}

		HumanName registryName = registryPatient.getName();
		if (registryName == null) {
			return fields;
		}

		List<StringType> givenNames = registryName.getGiven();
		if (givenNames != null && !givenNames.isEmpty()) {
			putIfNotBlank(fields, "givenName", givenNames.get(0).getValue());
			StringBuilder middleNames = new StringBuilder();
			for (int i = 1; i < givenNames.size(); i++) {
				String middleName = StringUtils.trimToNull(givenNames.get(i).getValue());
				if (middleName != null) {
					if (middleNames.length() > 0) {
						middleNames.append(' ');
					}
					middleNames.append(middleName);
				}
			}
			putIfNotBlank(fields, "middleName", middleNames.toString());
		}
		putIfNotBlank(fields, "familyName", registryName.getFamily());
		return fields;
	}

	Map<String, Object> toRegistrationFields(Patient patient) {
		Map<String, Object> fields = new LinkedHashMap<String, Object>();
		if (patient == null) {
			return fields;
		}

		putIfNotBlank(fields, "givenName", patient.getGivenName());
		putIfNotBlank(fields, "middleName", patient.getMiddleName());
		putIfNotBlank(fields, "familyName", patient.getFamilyName());
		putIfNotBlank(fields, "gender", patient.getGender());

		if (patient.getBirthdate() != null) {
			Calendar birthdate = Calendar.getInstance();
			birthdate.setTime(patient.getBirthdate());
			fields.put("birthdate", new SimpleDateFormat("yyyy-MM-dd").format(patient.getBirthdate()));
			fields.put("birthdateDay", birthdate.get(Calendar.DAY_OF_MONTH));
			fields.put("birthdateMonth", birthdate.get(Calendar.MONTH) + 1);
			fields.put("birthdateYear", birthdate.get(Calendar.YEAR));
			fields.put("birthdateEstimated", Boolean.TRUE.equals(patient.getBirthdateEstimated()));
		}

		for (PatientIdentifier identifier : patient.getIdentifiers()) {
			PatientIdentifierType type = identifier.getIdentifierType();
			if (sameMetadata(type, rwandaEmrConfig.getNationalId())) {
				putIfNotBlank(fields, "nationalId", identifier.getIdentifier());
			} else if (sameMetadata(type, rwandaEmrConfig.getNidApplicationNumber())) {
				putIfNotBlank(fields, "applicationNumber", identifier.getIdentifier());
			} else if (sameMetadata(type, rwandaEmrConfig.getUPID())) {
				putIfNotBlank(fields, "upid", identifier.getIdentifier());
			} else if (sameMetadata(type, rwandaEmrConfig.getNIN())) {
				putIfNotBlank(fields, "nin", identifier.getIdentifier());
			} else if (sameMetadata(type, rwandaEmrConfig.getPassportNumber())) {
				putIfNotBlank(fields, "passportNumber", identifier.getIdentifier());
			}
		}

		for (PersonAttribute attribute : patient.getAttributes()) {
			PersonAttributeType type = attribute.getAttributeType();
			if (sameMetadata(type, rwandaEmrConfig.getTelephoneNumber())) {
				putIfNotBlank(fields, "phoneNumber", attribute.getValue());
			} else if (sameMetadata(type, rwandaEmrConfig.getMothersName())) {
				putIfNotBlank(fields, "mothersName", attribute.getValue());
			} else if (sameMetadata(type, rwandaEmrConfig.getFathersName())) {
				putIfNotBlank(fields, "fathersName", attribute.getValue());
			} else if (sameMetadata(type, rwandaEmrConfig.getEducationLevel())) {
				putIfNotBlank(fields, "educationLevel", attribute.getValue());
			} else if (sameMetadata(type, rwandaEmrConfig.getProfession())) {
				putIfNotBlank(fields, "profession", attribute.getValue());
			} else if (sameMetadata(type, rwandaEmrConfig.getReligion())) {
				putIfNotBlank(fields, "religion", attribute.getValue());
			}
		}

		PersonAddress address = patient.getPersonAddress();
		if (address != null) {
			putIfNotBlank(fields, "country", address.getCountry());
			putIfNotBlank(fields, "stateProvince", address.getStateProvince());
			putIfNotBlank(fields, "countyDistrict", address.getCountyDistrict());
			putIfNotBlank(fields, "cityVillage", address.getCityVillage());
			putIfNotBlank(fields, "address3", address.getAddress3());
			putIfNotBlank(fields, "address1", address.getAddress1());
		}

		return fields;
	}

	private boolean sameMetadata(Object left, Object right) {
		return left != null && right != null && left.equals(right);
	}

	private void putIfNotBlank(Map<String, Object> fields, String name, String value) {
		if (StringUtils.isNotBlank(value)) {
			fields.put(name, value);
		}
	}

	private String findFhirPhoto(ClientRegistryPatient registryPatient) {
		if (registryPatient == null || registryPatient.getPatient() == null
				|| !registryPatient.getPatient().hasPhoto()) {
			return null;
		}

		for (Attachment attachment : registryPatient.getPatient().getPhoto()) {
			if (attachment == null) {
				continue;
			}
			if (attachment.hasData()) {
				byte[] data = attachment.getData();
				String contentType = resolveImageContentType(attachment.getContentType(), data);
				if (data != null && data.length > 0 && contentType != null) {
					return "data:" + contentType + ";base64," + Base64.getEncoder().encodeToString(data);
				}
			}
			if (attachment.hasUrl()) {
				String photo = normalizePhotoForBrowser(attachment.getUrl());
				if (StringUtils.isNotBlank(photo)) {
					return photo;
				}
			}
		}
		return null;
	}

	private String normalizePhotoForBrowser(String photo) {
		String trimmed = StringUtils.trimToNull(photo);
		if (trimmed == null) {
			return null;
		}
		String lower = trimmed.toLowerCase();
		if (lower.startsWith("http://") || lower.startsWith("https://") || trimmed.startsWith("/")) {
			return trimmed;
		}
		if (isSupportedImageDataUri(lower)) {
			return trimmed;
		}

		String compact = trimmed.replaceAll("\\s", "");
		String contentType = resolveBase64ImageContentType(compact);
		if (contentType != null && hasOnlyBase64Characters(compact)) {
			return "data:" + contentType + ";base64," + compact;
		}
		return null;
	}

	private boolean isSupportedImageDataUri(String lowerValue) {
		return lowerValue.startsWith("data:image/jpeg;base64,")
				|| lowerValue.startsWith("data:image/jpg;base64,")
				|| lowerValue.startsWith("data:image/png;base64,")
				|| lowerValue.startsWith("data:image/gif;base64,")
				|| lowerValue.startsWith("data:image/webp;base64,");
	}

	private String resolveImageContentType(String configuredType, byte[] data) {
		String normalized = StringUtils.trimToNull(configuredType);
		if (normalized != null) {
			normalized = normalized.toLowerCase();
			int parameters = normalized.indexOf(';');
			if (parameters >= 0) {
				normalized = normalized.substring(0, parameters).trim();
			}
			if ("image/jpeg".equals(normalized) || "image/jpg".equals(normalized)
					|| "image/png".equals(normalized) || "image/gif".equals(normalized)
					|| "image/webp".equals(normalized)) {
				return normalized;
			}
		}
		return resolveBinaryImageContentType(data);
	}

	private String resolveBinaryImageContentType(byte[] data) {
		if (data == null || data.length < 4) {
			return null;
		}
		if ((data[0] & 0xff) == 0xff && (data[1] & 0xff) == 0xd8 && (data[2] & 0xff) == 0xff) {
			return "image/jpeg";
		}
		if ((data[0] & 0xff) == 0x89 && data[1] == 0x50 && data[2] == 0x4e && data[3] == 0x47) {
			return "image/png";
		}
		if (data[0] == 'G' && data[1] == 'I' && data[2] == 'F') {
			return "image/gif";
		}
		if (data.length >= 12 && data[0] == 'R' && data[1] == 'I' && data[2] == 'F' && data[3] == 'F'
				&& data[8] == 'W' && data[9] == 'E' && data[10] == 'B' && data[11] == 'P') {
			return "image/webp";
		}
		return null;
	}

	private String resolveBase64ImageContentType(String value) {
		if (StringUtils.isBlank(value) || value.length() < 8) {
			return null;
		}
		if (value.startsWith("/9j/")) {
			return "image/jpeg";
		}
		if (value.startsWith("iVBOR")) {
			return "image/png";
		}
		if (value.startsWith("R0lGOD")) {
			return "image/gif";
		}
		if (value.startsWith("UklGR")) {
			return "image/webp";
		}
		return null;
	}

	private boolean hasOnlyBase64Characters(String value) {
		for (int i = 0; i < value.length(); i++) {
			char character = value.charAt(i);
			boolean valid = (character >= 'a' && character <= 'z')
					|| (character >= 'A' && character <= 'Z')
					|| (character >= '0' && character <= '9')
					|| character == '+' || character == '/' || character == '=';
			if (!valid) {
				return false;
			}
		}
		return true;
	}

	public void setIntegrationConfig(IntegrationConfig integrationConfig) {
		this.integrationConfig = integrationConfig;
	}

	public void setClientRegistryPatientProvider(ClientRegistryPatientProvider clientRegistryPatientProvider) {
		this.clientRegistryPatientProvider = clientRegistryPatientProvider;
	}

	public void setClientRegistryPatientTranslator(ClientRegistryPatientTranslator clientRegistryPatientTranslator) {
		this.clientRegistryPatientTranslator = clientRegistryPatientTranslator;
	}

	public void setRwandaEmrConfig(RwandaEmrConfig rwandaEmrConfig) {
		this.rwandaEmrConfig = rwandaEmrConfig;
	}

	public void setPatientService(PatientService patientService) {
		this.patientService = patientService;
	}

	public void setLocationService(LocationService locationService) {
		this.locationService = locationService;
	}

	public void setRegistrationCoreService(RegistrationCoreService registrationCoreService) {
		this.registrationCoreService = registrationCoreService;
	}

	public void setAddressHierarchyService(AddressHierarchyService addressHierarchyService) {
		this.addressHierarchyService = addressHierarchyService;
	}

	public void setAdministrationService(AdministrationService administrationService) {
		this.administrationService = administrationService;
	}
}
