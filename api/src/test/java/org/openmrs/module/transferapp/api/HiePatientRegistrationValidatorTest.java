package org.openmrs.module.transferapp.api;

import org.junit.Test;
import org.openmrs.Patient;
import org.openmrs.PersonName;
import org.openmrs.api.APIException;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class HiePatientRegistrationValidatorTest {

	@Test
	public void hasRequiredDemographicsRequiresNamesBirthdateAndGender() {
		Map<String, Object> fields = completeRegistrationFields();
		assertTrue(HiePatientRegistrationValidator.hasRequiredDemographics(fields));

		String[] requiredFields = { "givenName", "familyName", "birthdate", "gender" };
		for (String requiredField : requiredFields) {
			Map<String, Object> incompleteFields = completeRegistrationFields();
			incompleteFields.put(requiredField, "   ");
			assertFalse(HiePatientRegistrationValidator.hasRequiredDemographics(incompleteFields));
		}
	}

	@Test
	public void requireRequiredDemographicsRejectsIncompletePatient() {
		Patient patient = completePatient();
		patient.setBirthdate(null);

		try {
			HiePatientRegistrationValidator.requireRequiredDemographics(patient);
			fail("Expected incomplete demographics to prevent registration");
		}
		catch (APIException ex) {
			assertTrue(ex.getMessage().contains("date of birth"));
		}
	}

	@Test
	public void requireRequiredDemographicsAcceptsCompletePatient() {
		HiePatientRegistrationValidator.requireRequiredDemographics(completePatient());
	}

	private Map<String, Object> completeRegistrationFields() {
		Map<String, Object> fields = new HashMap<String, Object>();
		fields.put("givenName", "Alice");
		fields.put("familyName", "Uwase");
		fields.put("birthdate", "1990-04-12");
		fields.put("gender", "F");
		return fields;
	}

	private Patient completePatient() {
		Patient patient = new Patient();
		patient.addName(new PersonName("Alice", null, "Uwase"));
		patient.setGender("F");
		Calendar birthdate = Calendar.getInstance();
		birthdate.clear();
		birthdate.set(1990, Calendar.APRIL, 12);
		patient.setBirthdate(birthdate.getTime());
		return patient;
	}
}
