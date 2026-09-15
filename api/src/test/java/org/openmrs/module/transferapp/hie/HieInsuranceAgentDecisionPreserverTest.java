/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 */
package org.openmrs.module.transferapp.hie;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class HieInsuranceAgentDecisionPreserverTest {

	private final HieInsuranceAgentDecisionPreserver preserver = new HieInsuranceAgentDecisionPreserver();

	private final ObjectMapper mapper = new ObjectMapper();

	@Test
	public void hasAgentDecisionWhenApprovedExtensionPresent() throws Exception {
		String existing = encounterJsonWithDecision(true, "Approved by agent", "King Faisal Hospital");
		assertTrue(preserver.hasAgentDecision(existing));
	}

	@Test
	public void mergeKeepsAgentDecisionAndClinicalUpdates() throws Exception {
		String existing = encounterJsonWithDecision(true, "Approved by agent", "King Faisal Hospital");
		String clinical = clinicalEncounter("enc-1", "Updated clinical note", "Original Hospital", "UPID-NEW");

		String merged = preserver.mergePreservingAgentDecision(clinical, existing, "enc-1", true);
		ObjectNode root = (ObjectNode) mapper.readTree(merged);

		assertEquals("enc-1", text(root.get("id")));
		assertEquals("Updated clinical note", findExtensionValueString(root,
				"http://example.org/fhir/StructureDefinition/clinical-presentation"));
		assertTrue(findExtensionBoolean(root,
				"http://example.org/fhir/StructureDefinition/requires-insurance-agent-verification"));
		assertTrue(findExtensionBoolean(root,
				"http://example.org/fhir/StructureDefinition/agent-approved"));
		assertEquals("Approved by agent", findExtensionValueString(root,
				"http://example.org/fhir/StructureDefinition/agent-comment"));
		assertEquals("King Faisal Hospital",
				text(root.get("hospitalization").get("destination").get("display")));
		assertEquals("King Faisal Hospital",
				text(root.get("serviceProvider").get("display")));
		assertEquals("King Faisal Hospital ED",
				text(root.get("location").get(0).get("location").get("display")));
		// Local UPID / subject update must win over stale HIE subject.
		assertEquals("UPID-NEW", text(root.get("subject").get("identifier").get("value")));
	}

	@Test
	public void mergePreservesAdditionalHieOwnedAgentExtensions() throws Exception {
		ObjectNode existing = mapper.createObjectNode();
		existing.put("resourceType", "Encounter");
		existing.put("id", "enc-3");
		ArrayNode extensions = existing.putArray("extension");

		ObjectNode approved = extensions.addObject();
		approved.put("url", "http://example.org/fhir/StructureDefinition/agent-approved");
		approved.put("valueBoolean", true);

		ObjectNode decidedAt = extensions.addObject();
		decidedAt.put("url", "http://example.org/fhir/StructureDefinition/agent-decided-at");
		decidedAt.put("valueDateTime", "2026-03-01T10:00:00+02:00");

		String clinical = clinicalEncounter("enc-3", "New presentation", "District Hospital", "UPID-1");
		String merged = preserver.mergePreservingAgentDecision(
				clinical, mapper.writeValueAsString(existing), "enc-3", false);

		ObjectNode root = (ObjectNode) mapper.readTree(merged);
		assertTrue(findExtensionBoolean(root,
				"http://example.org/fhir/StructureDefinition/agent-approved"));
		assertEquals("2026-03-01T10:00:00+02:00", findExtensionValueDateTime(root,
				"http://example.org/fhir/StructureDefinition/agent-decided-at"));
		assertEquals("New presentation", findExtensionValueString(root,
				"http://example.org/fhir/StructureDefinition/clinical-presentation"));
	}

	@Test
	public void mergeWithoutDecisionKeepsRequiresVerification() throws Exception {
		ObjectNode existing = mapper.createObjectNode();
		existing.put("resourceType", "Encounter");
		existing.put("id", "enc-2");
		ArrayNode extensions = existing.putArray("extension");
		ObjectNode requires = extensions.addObject();
		requires.put("url", "http://example.org/fhir/StructureDefinition/requires-insurance-agent-verification");
		requires.put("valueBoolean", true);

		String clinical = clinicalEncounter("enc-2", "New presentation", "District Hospital", "UPID-1");
		String merged = preserver.mergePreservingAgentDecision(
				clinical, mapper.writeValueAsString(existing), "enc-2", true);

		ObjectNode root = (ObjectNode) mapper.readTree(merged);
		assertTrue(findExtensionBoolean(root,
				"http://example.org/fhir/StructureDefinition/requires-insurance-agent-verification"));
		assertFalse(preserver.hasAgentDecision(merged));
		assertEquals("New presentation", findExtensionValueString(root,
				"http://example.org/fhir/StructureDefinition/clinical-presentation"));
	}

	@Test
	public void ensureRequiresVerificationAttachesFlag() throws Exception {
		String clinical = clinicalEncounter("enc-4", "Presentation", "Hospital", "UPID-1");
		String withFlag = preserver.ensureRequiresVerification(clinical);
		ObjectNode root = (ObjectNode) mapper.readTree(withFlag);
		assertTrue(findExtensionBoolean(root,
				"http://example.org/fhir/StructureDefinition/requires-insurance-agent-verification"));
		assertNotNull(findExtensionValueString(root,
				"http://example.org/fhir/StructureDefinition/clinical-presentation"));
	}

	private String encounterJsonWithDecision(boolean approved, String comment, String destinationDisplay)
			throws Exception {
		ObjectNode encounter = mapper.createObjectNode();
		encounter.put("resourceType", "Encounter");
		encounter.put("id", "enc-1");
		ArrayNode extensions = encounter.putArray("extension");

		ObjectNode requires = extensions.addObject();
		requires.put("url", "http://example.org/fhir/StructureDefinition/requires-insurance-agent-verification");
		requires.put("valueBoolean", true);

		ObjectNode approvedExt = extensions.addObject();
		approvedExt.put("url", "http://example.org/fhir/StructureDefinition/agent-approved");
		approvedExt.put("valueBoolean", approved);

		ObjectNode commentExt = extensions.addObject();
		commentExt.put("url", "http://example.org/fhir/StructureDefinition/agent-comment");
		commentExt.put("valueString", comment);

		ObjectNode hospitalization = encounter.putObject("hospitalization");
		ObjectNode destination = hospitalization.putObject("destination");
		destination.put("display", destinationDisplay);

		ObjectNode serviceProvider = encounter.putObject("serviceProvider");
		serviceProvider.put("display", destinationDisplay);

		ObjectNode locationEntry = encounter.putArray("location").addObject();
		ObjectNode location = locationEntry.putObject("location");
		location.put("display", destinationDisplay + " ED");

		ObjectNode staleClinical = extensions.addObject();
		staleClinical.put("url", "http://example.org/fhir/StructureDefinition/clinical-presentation");
		staleClinical.put("valueString", "Old presentation");

		ObjectNode subject = encounter.putObject("subject");
		ObjectNode identifier = subject.putObject("identifier");
		identifier.put("value", "UPID-OLD");

		return mapper.writeValueAsString(encounter);
	}

	private String clinicalEncounter(String id, String presentation, String destinationDisplay, String upid)
			throws Exception {
		ObjectNode encounter = mapper.createObjectNode();
		encounter.put("resourceType", "Encounter");
		encounter.put("id", id);
		ArrayNode extensions = encounter.putArray("extension");

		ObjectNode clinical = extensions.addObject();
		clinical.put("url", "http://example.org/fhir/StructureDefinition/clinical-presentation");
		clinical.put("valueString", presentation);

		ObjectNode hospitalization = encounter.putObject("hospitalization");
		ObjectNode destination = hospitalization.putObject("destination");
		destination.put("display", destinationDisplay);

		ObjectNode serviceProvider = encounter.putObject("serviceProvider");
		serviceProvider.put("display", destinationDisplay);

		ObjectNode locationEntry = encounter.putArray("location").addObject();
		ObjectNode location = locationEntry.putObject("location");
		location.put("display", destinationDisplay + " ward");

		ObjectNode subject = encounter.putObject("subject");
		ObjectNode identifier = subject.putObject("identifier");
		identifier.put("value", upid);

		return mapper.writeValueAsString(encounter);
	}

	private String findExtensionValueString(ObjectNode encounter, String url) {
		ArrayNode extensions = (ArrayNode) encounter.get("extension");
		for (int i = 0; i < extensions.size(); i++) {
			ObjectNode ext = (ObjectNode) extensions.get(i);
			if (url.equals(text(ext.get("url")))) {
				return text(ext.get("valueString"));
			}
		}
		return null;
	}

	private String findExtensionValueDateTime(ObjectNode encounter, String url) {
		ArrayNode extensions = (ArrayNode) encounter.get("extension");
		for (int i = 0; i < extensions.size(); i++) {
			ObjectNode ext = (ObjectNode) extensions.get(i);
			if (url.equals(text(ext.get("url")))) {
				return text(ext.get("valueDateTime"));
			}
		}
		return null;
	}

	private boolean findExtensionBoolean(ObjectNode encounter, String url) {
		ArrayNode extensions = (ArrayNode) encounter.get("extension");
		for (int i = 0; i < extensions.size(); i++) {
			ObjectNode ext = (ObjectNode) extensions.get(i);
			if (url.equals(text(ext.get("url")))) {
				JsonNode value = ext.get("valueBoolean");
				return value != null && !value.isNull() && value.getBooleanValue();
			}
		}
		return false;
	}

	private static String text(JsonNode node) {
		if (node == null || node.isNull()) {
			return "";
		}
		String value = node.getTextValue();
		return value == null ? "" : value;
	}
}
