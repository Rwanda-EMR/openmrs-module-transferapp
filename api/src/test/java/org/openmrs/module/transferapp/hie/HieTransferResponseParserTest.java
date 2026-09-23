/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 */
package org.openmrs.module.transferapp.hie;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class HieTransferResponseParserTest {

	@Test
	public void parsePageReadsTransfersAndPaginationMetadata() throws Exception {
		HieTransferResponsePage page = new HieTransferResponseParser().parsePage(pageJson(2, true, 250,
				"enc-101", "enc-102"));

		assertEquals(2, page.getTransfers().size());
		assertEquals("enc-101", page.getTransfers().get(0).get("id"));
		assertTrue(page.hasMore());
		assertEquals(Integer.valueOf(2), page.getPage());
		assertEquals(Integer.valueOf(2), page.getSize());
		assertEquals(Integer.valueOf(250), page.getTotal());
	}

	@Test
	public void parsePageUsesBundleTotalWhenParameterTotalIsMissing() throws Exception {
		String json = pageJson(1, false, 1, "enc-1")
				.replace(",{\"name\":\"total\",\"valueInteger\":1}", "");

		HieTransferResponsePage page = new HieTransferResponseParser().parsePage(json);

		assertFalse(page.hasMore());
		assertEquals(Integer.valueOf(1), page.getTotal());
	}

	@Test
	public void mapsResponseTwoPreviewFallbacks() throws Exception {
		java.io.File jsonFile = resolveDevsFile("response_two.json");
		assertTrue("response_two.json should exist for preview fallback mapping", jsonFile.exists());

		String json = new String(Files.readAllBytes(jsonFile.toPath()), StandardCharsets.UTF_8);
		Map<String, Object> transfer = new HieTransferResponseParser().parse(json).get(0);

		assertEquals("20 (2006-01-22)", transfer.get("ageDob"));
		assertEquals("N/A", transfer.get("clientTelephone"));
		assertEquals("opd", transfer.get("receivingService"));
		assertEquals("NA", transfer.get("others"));
		assertEquals("27", transfer.get("muac"));
		assertEquals("No drug prescriptions", transfer.get("proceduresAndTreatments"));
		assertEquals("", transfer.get("caregiverName"));
		assertEquals("", transfer.get("providerPhone"));
		assertTrue(String.valueOf(transfer.get("referringProviderName")).contains("HLC-PRAC-2024-00074"));
	}

	@Test
	public void mapsGahiniNestedTransferDetailsShape() throws Exception {
		java.io.File jsonFile = resolveDevsFile("sample_transfer_gahini.json");
		assertTrue("sample_transfer_gahini.json should exist", jsonFile.exists());

		String entry = new String(Files.readAllBytes(jsonFile.toPath()), StandardCharsets.UTF_8);
		String wrapped = "{\"resourceType\":\"Parameters\",\"parameter\":["
				+ "{\"name\":\"bundle\",\"resource\":{\"resourceType\":\"Bundle\",\"total\":1,\"entry\":["
				+ entry + "]}},"
				+ "{\"name\":\"hasMore\",\"valueBoolean\":false},"
				+ "{\"name\":\"page\",\"valueInteger\":1},"
				+ "{\"name\":\"size\",\"valueInteger\":1},"
				+ "{\"name\":\"total\",\"valueInteger\":1}]}";

		Map<String, Object> transfer = new HieTransferResponseParser().parse(wrapped).get(0);

		assertEquals("ishimwe messie", transfer.get("clientName"));
		assertEquals("MALE", transfer.get("sex"));
		assertEquals("4 years", transfer.get("ageDob"));
		assertEquals("0781729853", transfer.get("clientTelephone"));
		assertEquals("usanase", transfer.get("caregiverName"));
		assertEquals("Eastern Provence", transfer.get("province"));
		assertEquals("Kayonza", transfer.get("district"));
		assertEquals("Gatsibo", transfer.get("patientDistrict"));
		assertEquals("Kiramuruzi", transfer.get("patientSector"));
		assertEquals("Akabuga", transfer.get("patientCell"));
		assertEquals("Ubuhoro", transfer.get("patientVillage"));
		assertEquals("TRAUMATISME DE 3 EME PHALANGE", transfer.get("reasonForTransfer"));
		assertEquals("TRAUMATISME DE 3 EME PHALANGE", transfer.get("clinicalPresentation"));
		assertEquals("FRACTURE DU PHALANGE", transfer.get("diagnosis"));
		assertEquals("Kiziguro District Hospital", transfer.get("referringFacilityName"));
		assertEquals("Gahini District Hospital", transfer.get("receivingFacility"));
		assertEquals("Outpatient Clinic", transfer.get("referringUnit"));
		assertEquals("ORTHOPAEDIC SURGERY", transfer.get("receivingService"));
		assertEquals("TRF-894", transfer.get("transferBusinessId"));
		assertEquals("", transfer.get("staffContactedAtReceivingFacility"));
		assertEquals("", transfer.get("staffContactPhone"));
		assertEquals("", transfer.get("laboratory"));
		assertEquals("", transfer.get("proceduresAndTreatments"));
		assertTrue(isBlank(transfer.get("transportComments")));
		assertTrue("comma placeholders must not leak into others",
				isBlank(transfer.get("others")) || !",".equals(String.valueOf(transfer.get("others"))));
	}

	private static boolean isBlank(Object value) {
		return value == null || String.valueOf(value).trim().isEmpty();
	}

	@Test
	public void mapsNestedReferralFeedbackFromTransferDetails() throws Exception {
		String entry = "{"
				+ "\"resource\":{"
				+ "\"resourceType\":\"Encounter\","
				+ "\"id\":\"enc-fb-1\","
				+ "\"status\":\"finished\","
				+ "\"extension\":[{"
				+ "\"url\":\"http://example.rw/fhir/StructureDefinition/transfer-details\","
				+ "\"extension\":["
				+ "{\"url\":\"transfer-type\",\"valueCoding\":{\"code\":\"COUNTER_REFERRAL\",\"display\":\"Counter referral\"}},"
				+ "{\"url\":\"http://example.org/fhir/StructureDefinition/patient-demographics\",\"extension\":["
				+ "{\"url\":\"name\",\"valueString\":\"Jane Doe\"},"
				+ "{\"url\":\"gender\",\"valueString\":\"F\"},"
				+ "{\"url\":\"age\",\"valueString\":\"30 years\"}"
				+ "]},"
				+ "{\"url\":\"referral-feedback\",\"extension\":["
				+ "{\"url\":\"final-diagnosis-comment\",\"valueString\":\"Pneumonia resolved\"},"
				+ "{\"url\":\"treatment-given\",\"extension\":[{\"url\":\"description\",\"valueString\":\"IV antibiotics\"}]},"
				+ "{\"url\":\"outcome\",\"valueCoding\":{\"system\":\"http://example.rw/fhir/CodeSystem/transfer-outcome\","
				+ "\"code\":\"STABILIZED\",\"display\":\"Stabilized\"}},"
				+ "{\"url\":\"comments\",\"valueString\":\"Continue oral meds\"},"
				+ "{\"url\":\"date-of-admission\",\"valueDate\":\"2026-09-10\"},"
				+ "{\"url\":\"date-of-discharge\",\"valueDate\":\"2026-09-14\"},"
				+ "{\"url\":\"contact-person\",\"valueString\":\"Nurse A\"},"
				+ "{\"url\":\"provider-name\",\"valueString\":\"Dr B\"},"
				+ "{\"url\":\"qualification\",\"valueString\":\"GP\"},"
				+ "{\"url\":\"signed-date\",\"valueDate\":\"2026-09-14\"},"
				+ "{\"url\":\"signed-time\",\"valueString\":\"10:30\"},"
				+ "{\"url\":\"phone\",\"valueString\":\"0788000000\"}"
				+ "]},"
				+ "{\"url\":\"counter-referral\",\"extension\":["
				+ "{\"url\":\"recommendation\",\"valueString\":\"Continue oral meds\"},"
				+ "{\"url\":\"follow-up-date\",\"valueDate\":\"2026-09-21\"},"
				+ "{\"url\":\"referred-back-to\",\"extension\":["
				+ "{\"url\":\"facility-name\",\"valueString\":\"Kiziguro District Hospital\"},"
				+ "{\"url\":\"fosa-id\",\"valueString\":\"0395\"}"
				+ "]},"
				+ "{\"url\":\"provider-name\",\"valueString\":\"Dr B\"}"
				+ "]}"
				+ "]"
				+ "}]"
				+ "}}";

		String wrapped = "{\"resourceType\":\"Parameters\",\"parameter\":["
				+ "{\"name\":\"bundle\",\"resource\":{\"resourceType\":\"Bundle\",\"total\":1,\"entry\":["
				+ entry + "]}},"
				+ "{\"name\":\"hasMore\",\"valueBoolean\":false},"
				+ "{\"name\":\"page\",\"valueInteger\":1},"
				+ "{\"name\":\"size\",\"valueInteger\":1},"
				+ "{\"name\":\"total\",\"valueInteger\":1}]}";

		Map<String, Object> transfer = new HieTransferResponseParser().parse(wrapped).get(0);
		@SuppressWarnings("unchecked")
		Map<String, Object> feedback = (Map<String, Object>) transfer.get("referralFeedback");

		assertTrue("referralFeedback should be present when nested under transfer-details", feedback != null);
		assertEquals("Pneumonia resolved", feedback.get("finalDiagnosis"));
		assertEquals("IV antibiotics", feedback.get("treatmentGiven"));
		assertEquals("STABILIZED_CURED", feedback.get("outcome"));
		assertEquals("Stabilized/Cured", feedback.get("outcomeLabel"));
		assertEquals("STABILIZED", feedback.get("outcomeHieCode"));
		assertEquals("Continue oral meds", feedback.get("recommendations"));
		assertEquals("Kiziguro District Hospital", feedback.get("referBackToFacility"));
		assertEquals("0395", feedback.get("referBackToFacilityFosaId"));
		assertEquals("2026-09-10", feedback.get("dateOfAdmissionOrSeen"));
		assertEquals("2026-09-14", feedback.get("dateOfDischarge"));
		assertEquals("Dr B", feedback.get("providerName"));
		assertEquals("Jane Doe", feedback.get("clientName"));
		assertEquals("F", feedback.get("sex"));
		assertEquals("30 years", feedback.get("ageOrDob"));
		assertEquals(Boolean.TRUE, feedback.get("fromHie"));
	}

	static java.io.File resolveDevsFile(String name) {
		java.io.File jsonFile = new java.io.File("../devs/" + name);
		if (!jsonFile.exists()) {
			jsonFile = new java.io.File("devs/" + name);
		}
		return jsonFile;
	}

	static String pageJson(int page, boolean hasMore, int total, String... encounterIds) {
		StringBuilder entries = new StringBuilder();
		for (String encounterId : encounterIds) {
			if (entries.length() > 0) {
				entries.append(',');
			}
			entries.append("{\"resource\":{\"resourceType\":\"Encounter\",\"id\":\"")
					.append(encounterId)
					.append("\",\"status\":\"planned\"}}");
		}
		return "{\"resourceType\":\"Parameters\",\"parameter\":["
				+ "{\"name\":\"bundle\",\"resource\":{\"resourceType\":\"Bundle\",\"total\":" + total
				+ ",\"entry\":[" + entries + "]}},"
				+ "{\"name\":\"hasMore\",\"valueBoolean\":" + hasMore + "},"
				+ "{\"name\":\"page\",\"valueInteger\":" + page + "},"
				+ "{\"name\":\"size\",\"valueInteger\":" + encounterIds.length + "},"
				+ "{\"name\":\"total\",\"valueInteger\":" + total + "}]}";
	}
}
