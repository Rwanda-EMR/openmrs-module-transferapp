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

import org.junit.Test;
import org.openmrs.Location;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.PatientIdentifierType;
import org.openmrs.PersonAddress;
import org.openmrs.PersonAttribute;
import org.openmrs.PersonAttributeType;
import org.openmrs.PersonName;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.LocationService;
import org.openmrs.api.PatientService;
import org.openmrs.module.addresshierarchy.AddressHierarchyEntry;
import org.openmrs.module.addresshierarchy.service.AddressHierarchyService;
import org.openmrs.module.rwandaemr.RwandaEmrConfig;
import org.openmrs.module.rwandaemr.integration.ClientRegistryPatient;
import org.openmrs.module.rwandaemr.integration.ClientRegistryPatientProvider;
import org.openmrs.module.rwandaemr.integration.ClientRegistryPatientTranslator;
import org.openmrs.module.rwandaemr.integration.IntegrationConfig;
import org.openmrs.module.registrationcore.api.RegistrationCoreService;
import org.openmrs.module.transferapp.TransferAppConstants;
import org.openmrs.module.transferapp.api.HiePatientRegistrationResult;
import org.openmrs.util.OpenmrsConstants;

import java.util.Calendar;
import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ClientRegistryRegistrationServiceImplTest {

	@Test
	public void registerPatientByUpidReturnsExistingPatientWithoutCallingHie() {
		PatientIdentifierType upidType = identifierType("upid");
		PatientIdentifierType nationalIdType = identifierType("national-id");
		Patient existingPatient = new Patient();
		existingPatient.addIdentifier(identifier(upidType, "UPI-123"));
		PatientService patientService = mock(PatientService.class);
		when(patientService.getPatients(null, "UPI-123", Collections.singletonList(upidType), true))
				.thenReturn(Collections.singletonList(existingPatient));
		ClientRegistryPatientProvider provider = mock(ClientRegistryPatientProvider.class);
		RegistrationCoreService registrationCoreService = mock(RegistrationCoreService.class);

		ClientRegistryRegistrationServiceImpl service = registrationService(
				upidType, nationalIdType, patientService, provider,
				mock(ClientRegistryPatientTranslator.class), registrationCoreService);

		HiePatientRegistrationResult result = service.registerPatientByUpid(" UPI-123 ", new Location());

		assertFalse(result.isCreated());
		assertSame(existingPatient, result.getPatient());
		verify(provider, never()).fetchPatientFromClientRegistry("UPI-123", IntegrationConfig.IDENTIFIER_SYSTEM_UPI);
		verify(registrationCoreService, never()).registerPatient(eq(existingPatient), anyList(), eq(new Location()));
	}

	@Test
	public void registerPatientByUpidTranslatesAndRegistersCompleteHiePatient() {
		PatientIdentifierType upidType = identifierType("upid");
		PatientIdentifierType nationalIdType = identifierType("national-id");
		PatientService patientService = mock(PatientService.class);
		when(patientService.getPatients(null, "UPI-123", Collections.singletonList(upidType), true))
				.thenReturn(Collections.<Patient>emptyList());

		org.hl7.fhir.r4.model.Patient fhirPatient = new org.hl7.fhir.r4.model.Patient();
		fhirPatient.addName().addGiven("Consolée").addGiven("Marie").setFamily("Uwase");
		ClientRegistryPatient registryPatient = new ClientRegistryPatient(fhirPatient);
		ClientRegistryPatientProvider provider = mock(ClientRegistryPatientProvider.class);
		when(provider.fetchPatientFromClientRegistry("UPI-123", IntegrationConfig.IDENTIFIER_SYSTEM_UPI))
				.thenReturn(registryPatient);

		Patient translatedPatient = new Patient();
		PersonName translatedName = new PersonName("Consolée Marie", null, "Uwase");
		translatedName.setPreferred(true);
		translatedPatient.addName(translatedName);
		addBirthdateAndGender(translatedPatient);
		translatedPatient.addIdentifier(identifier(nationalIdType, "1199880011223344"));
		ClientRegistryPatientTranslator translator = mock(ClientRegistryPatientTranslator.class);
		when(translator.toPatient(registryPatient)).thenReturn(translatedPatient);

		Location location = new Location();
		RegistrationCoreService registrationCoreService = mock(RegistrationCoreService.class);
		when(registrationCoreService.registerPatient(eq(translatedPatient), anyList(), eq(location)))
				.thenReturn(translatedPatient);
		ClientRegistryRegistrationServiceImpl service = registrationService(
				upidType, nationalIdType, patientService, provider, translator, registrationCoreService);
		AdministrationService administrationService = mock(AdministrationService.class);
		when(administrationService.getGlobalProperty(
				OpenmrsConstants.GLOBAL_PROPERTY_PATIENT_NAME_REGEX)).thenReturn("[A-Za-z' -]+");
		service.setAdministrationService(administrationService);

		HiePatientRegistrationResult result = service.registerPatientByUpid("UPI-123", location);

		assertTrue(result.isCreated());
		assertSame(translatedPatient, result.getPatient());
		assertEquals("Consolee", translatedPatient.getGivenName());
		assertEquals("Marie", translatedPatient.getMiddleName());
		assertEquals("Uwase", translatedPatient.getFamilyName());
		assertEquals("UPI-123", translatedPatient.getPatientIdentifier(upidType).getIdentifier());
		assertSame(location, translatedPatient.getPatientIdentifier(upidType).getLocation());
		verify(registrationCoreService).registerPatient(eq(translatedPatient), anyList(), eq(location));
	}

	@Test
	public void normalizeNameForValidationPreservesAccentsWhenConfiguredPatternAllowsThem() {
		assertEquals("Consolée", ClientRegistryRegistrationServiceImpl.normalizeNameForValidation(
				"Consolée", "[\\p{L}]+"));
	}

	@Test
	public void registerPatientByUpidAllowsMissingOrBlankNationalId() {
		assertOptionalNationalIdAccepted(null);
		assertOptionalNationalIdAccepted("");
		assertOptionalNationalIdAccepted("   ");
	}

	@Test
	public void registerPatientByUpidRejectsMissingRequiredDemographics() {
		PatientIdentifierType upidType = identifierType("upid");
		PatientIdentifierType nationalIdType = identifierType("national-id");
		PatientService patientService = mock(PatientService.class);
		when(patientService.getPatients(null, "UPI-123", Collections.singletonList(upidType), true))
				.thenReturn(Collections.<Patient>emptyList());

		ClientRegistryPatient registryPatient = new ClientRegistryPatient(new org.hl7.fhir.r4.model.Patient());
		ClientRegistryPatientProvider provider = mock(ClientRegistryPatientProvider.class);
		when(provider.fetchPatientFromClientRegistry("UPI-123", IntegrationConfig.IDENTIFIER_SYSTEM_UPI))
				.thenReturn(registryPatient);
		Patient translatedPatient = new Patient();
		ClientRegistryPatientTranslator translator = mock(ClientRegistryPatientTranslator.class);
		when(translator.toPatient(registryPatient)).thenReturn(translatedPatient);
		RegistrationCoreService registrationCoreService = mock(RegistrationCoreService.class);
		ClientRegistryRegistrationServiceImpl service = registrationService(
				upidType, nationalIdType, patientService, provider, translator, registrationCoreService);

		try {
			service.registerPatientByUpid("UPI-123", new Location());
			fail("Expected incomplete demographics to prevent patient registration");
		}
		catch (org.openmrs.api.APIException ex) {
			assertEquals(org.openmrs.module.transferapp.api.HiePatientRegistrationValidator
					.MISSING_REQUIRED_DEMOGRAPHICS_MESSAGE, ex.getMessage());
		}
		verify(registrationCoreService, never()).registerPatient(eq(translatedPatient), anyList(),
				org.mockito.ArgumentMatchers.any(Location.class));
	}

	@Test
	public void registerPatientByUpidRejectsNationalIdThatIsNotExactlySixteenDigits() {
		assertInvalidNationalIdRejected("123456789012345");
		assertInvalidNationalIdRejected("12345678901234567");
		assertInvalidNationalIdRejected("123456789012345A");
	}

	@Test
	public void findRegistrationFieldsByUpidAllowsMissingNationalIdBeforePreview() {
		PatientIdentifierType upidType = identifierType("upid");
		PatientIdentifierType nationalIdType = identifierType("national-id");
		ClientRegistryPatient registryPatient = new ClientRegistryPatient(new org.hl7.fhir.r4.model.Patient());
		ClientRegistryPatientProvider provider = mock(ClientRegistryPatientProvider.class);
		when(provider.fetchPatientFromClientRegistry("UPI-123", IntegrationConfig.IDENTIFIER_SYSTEM_UPI))
				.thenReturn(registryPatient);
		ClientRegistryPatientTranslator translator = mock(ClientRegistryPatientTranslator.class);
		when(translator.toPatient(registryPatient)).thenReturn(new Patient());
		ClientRegistryRegistrationServiceImpl service = registrationService(
				upidType, nationalIdType, mock(PatientService.class), provider, translator,
				mock(RegistrationCoreService.class));

		Map<String, Object> fields = service.findRegistrationFieldsByUpid("UPI-123");

		assertEquals("UPI-123", fields.get("upid"));
	}

	@Test
	public void findRegistrationFieldsByUpidRejectsProvidedInvalidNationalIdBeforePreview() {
		PatientIdentifierType upidType = identifierType("upid");
		PatientIdentifierType nationalIdType = identifierType("national-id");
		ClientRegistryPatient registryPatient = new ClientRegistryPatient(new org.hl7.fhir.r4.model.Patient());
		ClientRegistryPatientProvider provider = mock(ClientRegistryPatientProvider.class);
		when(provider.fetchPatientFromClientRegistry("UPI-123", IntegrationConfig.IDENTIFIER_SYSTEM_UPI))
				.thenReturn(registryPatient);
		Patient translatedPatient = new Patient();
		translatedPatient.addIdentifier(identifier(nationalIdType, "12345"));
		ClientRegistryPatientTranslator translator = mock(ClientRegistryPatientTranslator.class);
		when(translator.toPatient(registryPatient)).thenReturn(translatedPatient);
		ClientRegistryRegistrationServiceImpl service = registrationService(
				upidType, nationalIdType, mock(PatientService.class), provider, translator,
				mock(RegistrationCoreService.class));

		try {
			service.findRegistrationFieldsByUpid("UPI-123");
			fail("Expected a provided invalid National ID to prevent the registration preview");
		}
		catch (org.openmrs.api.APIException ex) {
			assertEquals(ClientRegistryRegistrationServiceImpl.INVALID_NATIONAL_ID_MESSAGE, ex.getMessage());
		}
	}

	@Test
	public void findRegistrationFieldsByUpidIncludesInlineFhirPhoto() {
		PatientIdentifierType upidType = identifierType("upid");
		PatientIdentifierType nationalIdType = identifierType("national-id");
		org.hl7.fhir.r4.model.Patient fhirPatient = new org.hl7.fhir.r4.model.Patient();
		fhirPatient.addPhoto().setContentType("image/png").setData(new byte[] { 1, 2, 3 });
		ClientRegistryPatient registryPatient = new ClientRegistryPatient(fhirPatient);
		ClientRegistryPatientProvider provider = mock(ClientRegistryPatientProvider.class);
		when(provider.fetchPatientFromClientRegistry("UPI-123", IntegrationConfig.IDENTIFIER_SYSTEM_UPI))
				.thenReturn(registryPatient);
		ClientRegistryPatientTranslator translator = mock(ClientRegistryPatientTranslator.class);
		when(translator.toPatient(registryPatient)).thenReturn(new Patient());
		ClientRegistryRegistrationServiceImpl service = registrationService(
				upidType, nationalIdType, mock(PatientService.class), provider, translator,
				mock(RegistrationCoreService.class));

		Map<String, Object> fields = service.findRegistrationFieldsByUpid("UPI-123");

		assertEquals("data:image/png;base64,AQID", fields.get("photo"));
	}

	@Test
	public void findRegistrationFieldsByUpidRejectsIncompleteAddressBeforePreview() {
		PatientIdentifierType upidType = identifierType("upid");
		PatientIdentifierType nationalIdType = identifierType("national-id");
		ClientRegistryPatient registryPatient = new ClientRegistryPatient(new org.hl7.fhir.r4.model.Patient());
		ClientRegistryPatientProvider provider = mock(ClientRegistryPatientProvider.class);
		when(provider.fetchPatientFromClientRegistry("UPI-123", IntegrationConfig.IDENTIFIER_SYSTEM_UPI))
				.thenReturn(registryPatient);

		Patient translatedPatient = new Patient();
		PersonAddress incompleteAddress = rwandaAddress();
		incompleteAddress.setAddress1(null);
		translatedPatient.addAddress(incompleteAddress);
		ClientRegistryPatientTranslator translator = mock(ClientRegistryPatientTranslator.class);
		when(translator.toPatient(registryPatient)).thenReturn(translatedPatient);
		ClientRegistryRegistrationServiceImpl service = registrationService(
				upidType, nationalIdType, mock(PatientService.class), provider, translator,
				mock(RegistrationCoreService.class));
		service.setAddressHierarchyService(mock(AddressHierarchyService.class));
		setAddressValidation(service, true);

		try {
			service.findRegistrationFieldsByUpid("UPI-123");
			fail("Expected an incomplete address to prevent the registration preview");
		}
		catch (org.openmrs.api.APIException ex) {
			assertEquals(ClientRegistryRegistrationServiceImpl.INVALID_ADDRESS_MESSAGE, ex.getMessage());
		}
	}

	@Test
	public void registerPatientByUpidRejectsAddressOutsideConfiguredHierarchy() {
		PatientIdentifierType upidType = identifierType("upid");
		PatientIdentifierType nationalIdType = identifierType("national-id");
		PatientService patientService = mock(PatientService.class);
		when(patientService.getPatients(null, "UPI-123", Collections.singletonList(upidType), true))
				.thenReturn(Collections.<Patient>emptyList());

		ClientRegistryPatient registryPatient = new ClientRegistryPatient(new org.hl7.fhir.r4.model.Patient());
		ClientRegistryPatientProvider provider = mock(ClientRegistryPatientProvider.class);
		when(provider.fetchPatientFromClientRegistry("UPI-123", IntegrationConfig.IDENTIFIER_SYSTEM_UPI))
				.thenReturn(registryPatient);
		Patient translatedPatient = new Patient();
		translatedPatient.addAddress(rwandaAddress());
		ClientRegistryPatientTranslator translator = mock(ClientRegistryPatientTranslator.class);
		when(translator.toPatient(registryPatient)).thenReturn(translatedPatient);
		RegistrationCoreService registrationCoreService = mock(RegistrationCoreService.class);
		ClientRegistryRegistrationServiceImpl service = registrationService(
				upidType, nationalIdType, patientService, provider, translator, registrationCoreService);
		service.setAddressHierarchyService(addressHierarchy(false));
		setAddressValidation(service, true);

		try {
			service.registerPatientByUpid("UPI-123", new Location());
			fail("Expected an address outside the configured hierarchy to prevent patient registration");
		}
		catch (org.openmrs.api.APIException ex) {
			assertEquals(ClientRegistryRegistrationServiceImpl.INVALID_ADDRESS_MESSAGE, ex.getMessage());
		}
		verify(registrationCoreService, never()).registerPatient(eq(translatedPatient), anyList(),
				org.mockito.ArgumentMatchers.any(Location.class));
	}

	@Test
	public void validateAddressAllowsEmptyAddressAndAcceptsCompleteConfiguredHierarchy() {
		ClientRegistryRegistrationServiceImpl service = new ClientRegistryRegistrationServiceImpl();
		service.setAddressHierarchyService(addressHierarchy(true));
		setAddressValidation(service, true);

		Patient patientWithoutAddress = new Patient();
		service.validateAddress(patientWithoutAddress);

		Patient patientWithBlankAddress = new Patient();
		PersonAddress blankAddress = new PersonAddress();
		blankAddress.setCountry("   ");
		patientWithBlankAddress.addAddress(blankAddress);
		service.validateAddress(patientWithBlankAddress);
		assertNull(patientWithBlankAddress.getPersonAddress());

		Patient patientWithValidAddress = new Patient();
		PersonAddress validAddress = rwandaAddress();
		patientWithValidAddress.addAddress(validAddress);
		service.validateAddress(patientWithValidAddress);
		assertSame(validAddress, patientWithValidAddress.getPersonAddress());
	}

	@Test
	public void validateAddressIsDisabledWhenGlobalPropertyIsMissingOrFalse() {
		ClientRegistryRegistrationServiceImpl service = new ClientRegistryRegistrationServiceImpl();
		AdministrationService administrationService = mock(AdministrationService.class);
		service.setAdministrationService(administrationService);

		Patient patient = new Patient();
		PersonAddress incompleteAddress = rwandaAddress();
		incompleteAddress.setAddress1(null);
		patient.addAddress(incompleteAddress);

		service.validateAddress(patient);
		assertFalse(service.isPatientAddressValidationEnabled());

		when(administrationService.getGlobalProperty(
				TransferAppConstants.GP_HIE_VALIDATE_PATIENT_ADDRESS)).thenReturn(" false ");
		service.validateAddress(patient);
		assertFalse(service.isPatientAddressValidationEnabled());
		assertSame(incompleteAddress, patient.getPersonAddress());
	}

	@Test
	public void toRegistrationFieldsKeepsSeparateFhirGivenAndMiddleNames() {
		org.hl7.fhir.r4.model.Patient fhirPatient = new org.hl7.fhir.r4.model.Patient();
		fhirPatient.addName().addGiven("Alice").addGiven("Marie").addGiven("Grace").setFamily("Uwase");
		ClientRegistryPatient registryPatient = new ClientRegistryPatient(fhirPatient);

		Patient translatedPatient = new Patient();
		translatedPatient.addName(new PersonName("Alice Marie Grace", null, "Uwase"));

		Map<String, Object> fields = new ClientRegistryRegistrationServiceImpl()
				.toRegistrationFields(registryPatient, translatedPatient);

		assertEquals("Alice", fields.get("givenName"));
		assertEquals("Marie Grace", fields.get("middleName"));
		assertEquals("Uwase", fields.get("familyName"));
	}

	@Test
	public void toRegistrationFieldsMapsDemographicsBirthdateAndAddress() {
		Patient patient = new Patient();
		PersonName name = new PersonName("Alice", "Marie", "Uwase");
		name.setPreferred(true);
		patient.addName(name);
		patient.setGender("F");

		Calendar birthdate = Calendar.getInstance();
		birthdate.clear();
		birthdate.set(1990, Calendar.APRIL, 12);
		patient.setBirthdate(birthdate.getTime());
		patient.setBirthdateEstimated(false);

		PersonAddress address = new PersonAddress();
		address.setPreferred(true);
		address.setCountry("Rwanda");
		address.setStateProvince("Kigali City");
		address.setCountyDistrict("Gasabo");
		address.setCityVillage("Kimironko");
		address.setAddress3("Bibare");
		address.setAddress1("Imena");
		patient.addAddress(address);

		Map<String, Object> fields = new ClientRegistryRegistrationServiceImpl().toRegistrationFields(patient);

		assertEquals("Alice", fields.get("givenName"));
		assertEquals("Marie", fields.get("middleName"));
		assertEquals("Uwase", fields.get("familyName"));
		assertEquals("F", fields.get("gender"));
		assertEquals("1990-04-12", fields.get("birthdate"));
		assertEquals(12, fields.get("birthdateDay"));
		assertEquals(4, fields.get("birthdateMonth"));
		assertEquals(1990, fields.get("birthdateYear"));
		assertEquals(false, fields.get("birthdateEstimated"));
		assertEquals("Rwanda", fields.get("country"));
		assertEquals("Kigali City", fields.get("stateProvince"));
		assertEquals("Gasabo", fields.get("countyDistrict"));
		assertEquals("Kimironko", fields.get("cityVillage"));
		assertEquals("Bibare", fields.get("address3"));
		assertEquals("Imena", fields.get("address1"));
	}

	@Test
	public void toRegistrationFieldsMapsConfiguredIdentifiersAndAttributes() {
		PatientIdentifierType nationalId = identifierType("national-id");
		PatientIdentifierType applicationNumber = identifierType("application-number");
		PatientIdentifierType upid = identifierType("upid");
		PatientIdentifierType nin = identifierType("nin");
		PatientIdentifierType passportNumber = identifierType("passport-number");
		PersonAttributeType telephoneNumber = attributeType("telephone-number");
		PersonAttributeType mothersName = attributeType("mothers-name");
		PersonAttributeType fathersName = attributeType("fathers-name");
		PersonAttributeType educationLevel = attributeType("education-level");
		PersonAttributeType profession = attributeType("profession");
		PersonAttributeType religion = attributeType("religion");

		ClientRegistryRegistrationServiceImpl service = new ClientRegistryRegistrationServiceImpl();
		service.setRwandaEmrConfig(metadataConfig(nationalId, applicationNumber, upid, nin, passportNumber,
				telephoneNumber, mothersName, fathersName, educationLevel, profession, religion));

		Patient patient = new Patient();
		patient.addIdentifier(identifier(nationalId, "1199880011223344"));
		patient.addIdentifier(identifier(applicationNumber, "APP-123"));
		patient.addIdentifier(identifier(upid, "UPI-456"));
		patient.addIdentifier(identifier(nin, "NIN-789"));
		patient.addIdentifier(identifier(passportNumber, "PC012345"));
		patient.addAttribute(attribute(telephoneNumber, "0788000000"));
		patient.addAttribute(attribute(mothersName, "Aline"));
		patient.addAttribute(attribute(fathersName, "Patrick"));
		patient.addAttribute(attribute(educationLevel, "Secondary"));
		patient.addAttribute(attribute(profession, "Teacher"));
		patient.addAttribute(attribute(religion, "Catholic"));

		Map<String, Object> fields = service.toRegistrationFields(patient);

		assertEquals("1199880011223344", fields.get("nationalId"));
		assertEquals("APP-123", fields.get("applicationNumber"));
		assertEquals("UPI-456", fields.get("upid"));
		assertEquals("NIN-789", fields.get("nin"));
		assertEquals("PC012345", fields.get("passportNumber"));
		assertEquals("0788000000", fields.get("phoneNumber"));
		assertEquals("Aline", fields.get("mothersName"));
		assertEquals("Patrick", fields.get("fathersName"));
		assertEquals("Secondary", fields.get("educationLevel"));
		assertEquals("Teacher", fields.get("profession"));
		assertEquals("Catholic", fields.get("religion"));
	}

	private PatientIdentifierType identifierType(String uuid) {
		PatientIdentifierType type = new PatientIdentifierType();
		type.setUuid(uuid);
		return type;
	}

	private PersonAttributeType attributeType(String uuid) {
		PersonAttributeType type = new PersonAttributeType();
		type.setUuid(uuid);
		return type;
	}

	private PatientIdentifier identifier(PatientIdentifierType type, String value) {
		PatientIdentifier identifier = new PatientIdentifier();
		identifier.setIdentifierType(type);
		identifier.setIdentifier(value);
		return identifier;
	}

	private PersonAttribute attribute(PersonAttributeType type, String value) {
		PersonAttribute attribute = new PersonAttribute();
		attribute.setAttributeType(type);
		attribute.setValue(value);
		return attribute;
	}

	private PersonAddress rwandaAddress() {
		PersonAddress address = new PersonAddress();
		address.setCountry("Rwanda");
		address.setStateProvince("Kigali Province");
		address.setCountyDistrict("Nyarugenge");
		address.setCityVillage("Gitega");
		address.setAddress3("Akabahizi");
		address.setAddress1("Gihanga");
		return address;
	}

	private AddressHierarchyService addressHierarchy(boolean includeVillage) {
		AddressHierarchyService service = mock(AddressHierarchyService.class);
		AddressHierarchyEntry country = mock(AddressHierarchyEntry.class);
		AddressHierarchyEntry province = mock(AddressHierarchyEntry.class);
		AddressHierarchyEntry district = mock(AddressHierarchyEntry.class);
		AddressHierarchyEntry sector = mock(AddressHierarchyEntry.class);
		AddressHierarchyEntry cell = mock(AddressHierarchyEntry.class);
		AddressHierarchyEntry village = includeVillage ? mock(AddressHierarchyEntry.class) : null;
		when(service.getChildAddressHierarchyEntryByName(null, "Rwanda")).thenReturn(country);
		when(service.getChildAddressHierarchyEntryByName(country, "Kigali Province")).thenReturn(province);
		when(service.getChildAddressHierarchyEntryByName(province, "Nyarugenge")).thenReturn(district);
		when(service.getChildAddressHierarchyEntryByName(district, "Gitega")).thenReturn(sector);
		when(service.getChildAddressHierarchyEntryByName(sector, "Akabahizi")).thenReturn(cell);
		when(service.getChildAddressHierarchyEntryByName(cell, "Gihanga")).thenReturn(village);
		return service;
	}

	private void setAddressValidation(ClientRegistryRegistrationServiceImpl service, boolean enabled) {
		AdministrationService administrationService = mock(AdministrationService.class);
		when(administrationService.getGlobalProperty(
				TransferAppConstants.GP_HIE_VALIDATE_PATIENT_ADDRESS)).thenReturn(Boolean.toString(enabled));
		service.setAdministrationService(administrationService);
	}

	private void assertInvalidNationalIdRejected(String nationalId) {
		PatientIdentifierType upidType = identifierType("upid");
		PatientIdentifierType nationalIdType = identifierType("national-id");
		PatientService patientService = mock(PatientService.class);
		when(patientService.getPatients(null, "UPI-123", Collections.singletonList(upidType), true))
				.thenReturn(Collections.<Patient>emptyList());

		ClientRegistryPatient registryPatient = new ClientRegistryPatient(new org.hl7.fhir.r4.model.Patient());
		ClientRegistryPatientProvider provider = mock(ClientRegistryPatientProvider.class);
		when(provider.fetchPatientFromClientRegistry("UPI-123", IntegrationConfig.IDENTIFIER_SYSTEM_UPI))
				.thenReturn(registryPatient);

		Patient translatedPatient = new Patient();
		translatedPatient.addName(new PersonName("Alice", null, "Uwase"));
		addBirthdateAndGender(translatedPatient);
		if (nationalId != null) {
			translatedPatient.addIdentifier(identifier(nationalIdType, nationalId));
		}
		ClientRegistryPatientTranslator translator = mock(ClientRegistryPatientTranslator.class);
		when(translator.toPatient(registryPatient)).thenReturn(translatedPatient);
		RegistrationCoreService registrationCoreService = mock(RegistrationCoreService.class);
		ClientRegistryRegistrationServiceImpl service = registrationService(
				upidType, nationalIdType, patientService, provider, translator, registrationCoreService);

		try {
			service.registerPatientByUpid("UPI-123", new Location());
			fail("Expected invalid National ID to prevent patient registration");
		}
		catch (org.openmrs.api.APIException ex) {
			assertEquals(ClientRegistryRegistrationServiceImpl.INVALID_NATIONAL_ID_MESSAGE, ex.getMessage());
		}
		verify(registrationCoreService, never()).registerPatient(eq(translatedPatient), anyList(),
				org.mockito.ArgumentMatchers.any(Location.class));
	}

	private void assertOptionalNationalIdAccepted(String nationalId) {
		PatientIdentifierType upidType = identifierType("upid");
		PatientIdentifierType nationalIdType = identifierType("national-id");
		PatientService patientService = mock(PatientService.class);
		when(patientService.getPatients(null, "UPI-123", Collections.singletonList(upidType), true))
				.thenReturn(Collections.<Patient>emptyList());

		ClientRegistryPatient registryPatient = new ClientRegistryPatient(new org.hl7.fhir.r4.model.Patient());
		ClientRegistryPatientProvider provider = mock(ClientRegistryPatientProvider.class);
		when(provider.fetchPatientFromClientRegistry("UPI-123", IntegrationConfig.IDENTIFIER_SYSTEM_UPI))
				.thenReturn(registryPatient);
		Patient translatedPatient = new Patient();
		translatedPatient.addName(new PersonName("Alice", null, "Uwase"));
		addBirthdateAndGender(translatedPatient);
		if (nationalId != null) {
			translatedPatient.addIdentifier(identifier(nationalIdType, nationalId));
		}
		ClientRegistryPatientTranslator translator = mock(ClientRegistryPatientTranslator.class);
		when(translator.toPatient(registryPatient)).thenReturn(translatedPatient);
		Location location = new Location();
		RegistrationCoreService registrationCoreService = mock(RegistrationCoreService.class);
		when(registrationCoreService.registerPatient(eq(translatedPatient), anyList(), eq(location)))
				.thenReturn(translatedPatient);
		ClientRegistryRegistrationServiceImpl service = registrationService(
				upidType, nationalIdType, patientService, provider, translator, registrationCoreService);

		HiePatientRegistrationResult result = service.registerPatientByUpid("UPI-123", location);

		assertTrue(result.isCreated());
		if (nationalId != null && nationalId.trim().length() == 0) {
			assertNull(translatedPatient.getPatientIdentifier(nationalIdType));
		}
		verify(registrationCoreService).registerPatient(eq(translatedPatient), anyList(), eq(location));
	}

	private void addBirthdateAndGender(Patient patient) {
		patient.setGender("F");
		Calendar birthdate = Calendar.getInstance();
		birthdate.clear();
		birthdate.set(1990, Calendar.APRIL, 12);
		patient.setBirthdate(birthdate.getTime());
	}

	private ClientRegistryRegistrationServiceImpl registrationService(PatientIdentifierType upidType,
			PatientIdentifierType nationalIdType,
			PatientService patientService,
			ClientRegistryPatientProvider provider,
			ClientRegistryPatientTranslator translator,
			RegistrationCoreService registrationCoreService) {
		IntegrationConfig integrationConfig = mock(IntegrationConfig.class);
		when(integrationConfig.isHieEnabled()).thenReturn(true);
		RwandaEmrConfig rwandaEmrConfig = mock(RwandaEmrConfig.class);
		when(rwandaEmrConfig.getUPID()).thenReturn(upidType);
		when(rwandaEmrConfig.getNationalId()).thenReturn(nationalIdType);

		ClientRegistryRegistrationServiceImpl service = new ClientRegistryRegistrationServiceImpl();
		service.setIntegrationConfig(integrationConfig);
		service.setRwandaEmrConfig(rwandaEmrConfig);
		service.setPatientService(patientService);
		service.setLocationService(mock(LocationService.class));
		service.setClientRegistryPatientProvider(provider);
		service.setClientRegistryPatientTranslator(translator);
		service.setRegistrationCoreService(registrationCoreService);
		service.setAdministrationService(mock(AdministrationService.class));
		return service;
	}

	private RwandaEmrConfig metadataConfig(final PatientIdentifierType nationalId,
			final PatientIdentifierType applicationNumber,
			final PatientIdentifierType upid,
			final PatientIdentifierType nin,
			final PatientIdentifierType passportNumber,
			final PersonAttributeType telephoneNumber,
			final PersonAttributeType mothersName,
			final PersonAttributeType fathersName,
			final PersonAttributeType educationLevel,
			final PersonAttributeType profession,
			final PersonAttributeType religion) {
		RwandaEmrConfig config = mock(RwandaEmrConfig.class);
		when(config.getNationalId()).thenReturn(nationalId);
		when(config.getNidApplicationNumber()).thenReturn(applicationNumber);
		when(config.getUPID()).thenReturn(upid);
		when(config.getNIN()).thenReturn(nin);
		when(config.getPassportNumber()).thenReturn(passportNumber);
		when(config.getTelephoneNumber()).thenReturn(telephoneNumber);
		when(config.getMothersName()).thenReturn(mothersName);
		when(config.getFathersName()).thenReturn(fathersName);
		when(config.getEducationLevel()).thenReturn(educationLevel);
		when(config.getProfession()).thenReturn(profession);
		when(config.getReligion()).thenReturn(religion);
		return config;
	}
}
