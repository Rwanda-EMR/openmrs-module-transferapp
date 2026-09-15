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
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * When clinicians edit and resubmit an insurance-gated transfer, rebuild clinical content
 * from the local Transfer but keep HIE-owned attributes (insurance approval status / agent
 * decision extensions, and agent destination redirect when present) from the existing
 * HIE Encounter. Local updates such as UPID and clinical fields stay from the rebuild.
 */
public class HieInsuranceAgentDecisionPreserver {

	public static final String REQUIRES_VERIFICATION_URL =
			"http://example.org/fhir/StructureDefinition/requires-insurance-agent-verification";
	public static final String AGENT_APPROVED_URL =
			"http://example.org/fhir/StructureDefinition/agent-approved";
	public static final String AGENT_COMMENT_URL =
			"http://example.org/fhir/StructureDefinition/agent-comment";
	public static final String RECEIVING_PROVINCE_URL =
			"http://example.org/fhir/StructureDefinition/receiving-province";
	public static final String RECEIVING_DISTRICT_URL =
			"http://example.org/fhir/StructureDefinition/receiving-district";

	private final ObjectMapper objectMapper = new ObjectMapper();

	/**
	 * @return true when the existing HIE encounter already carries an agent decision
	 *         ({@code agent-approved} extension is present).
	 */
	public boolean hasAgentDecision(String existingEncounterJson) {
		JsonNode encounter = unwrapEncounter(existingEncounterJson);
		return encounter != null && findExtension(encounter, AGENT_APPROVED_URL) != null;
	}

	/**
	 * Ensures external destinations always carry requires-insurance-agent-verification
	 * (used on first submit when no prior HIE Encounter exists).
	 */
	public String ensureRequiresVerification(String clinicalEncounterJson) {
		try {
			ObjectNode clinical = requireEncounterObject(clinicalEncounterJson);
			if (findExtension(clinical, REQUIRES_VERIFICATION_URL) == null) {
				ObjectNode requires = extensionsArray(clinical).addObject();
				requires.put("url", REQUIRES_VERIFICATION_URL);
				requires.put("valueBoolean", true);
			}
			return objectMapper.writeValueAsString(clinical);
		}
		catch (HieApiException ex) {
			throw ex;
		}
		catch (Exception ex) {
			throw new HieApiException("Failed to attach insurance verification flag: " + ex.getMessage(), ex);
		}
	}

	/**
	 * Rebuilds {@code clinicalEncounterJson} so it keeps the same Encounter id and copies
	 * HIE-owned insurance/agent attributes from {@code existingEncounterJson}.
	 * Clinical fields and identifiers (including updated UPID/subject) from the rebuilt
	 * payload are kept; approval status and related agent extensions (and destination when
	 * the agent has already decided) come from HIE.
	 *
	 * @param keepRequiresVerification when true and HIE has no requires-verification extension,
	 *        re-attach {@code requires-insurance-agent-verification=true} (external destination).
	 */
	public String mergePreservingAgentDecision(String clinicalEncounterJson, String existingEncounterJson,
			String encounterId, boolean keepRequiresVerification) {
		try {
			ObjectNode clinical = requireEncounterObject(clinicalEncounterJson);
			JsonNode existing = unwrapEncounter(existingEncounterJson);
			if (existing == null || !existing.isObject()) {
				throw new HieApiException("Existing HIE encounter could not be parsed for approval preservation");
			}

			if (StringUtils.isNotBlank(encounterId)) {
				clinical.put("id", encounterId.trim());
			}
			else {
				JsonNode existingId = existing.get("id");
				if (existingId != null && !existingId.isNull() && StringUtils.isNotBlank(existingId.getTextValue())) {
					clinical.put("id", existingId.getTextValue());
				}
			}

			boolean hasDecision = findExtension(existing, AGENT_APPROVED_URL) != null;
			removeHieOwnedExtensions(clinical);
			copyExtensionIfPresent(clinical, existing, REQUIRES_VERIFICATION_URL);
			copyExtensionIfPresent(clinical, existing, AGENT_APPROVED_URL);
			copyExtensionIfPresent(clinical, existing, AGENT_COMMENT_URL);
			// Any additional HIE-only agent / insurance-agent extensions not rebuilt locally.
			copyAdditionalHieOwnedExtensions(clinical, existing);

			if (keepRequiresVerification && findExtension(clinical, REQUIRES_VERIFICATION_URL) == null) {
				ObjectNode requires = extensionsArray(clinical).addObject();
				requires.put("url", REQUIRES_VERIFICATION_URL);
				requires.put("valueBoolean", true);
			}

			if (hasDecision) {
				// Agent may have redirected destination — keep HIE destination + related refs,
				// not the clinician rebuild from the original local receiving facility.
				copyHospitalizationDestination(clinical, (ObjectNode) existing);
				copyJsonFieldIfPresent(clinical, (ObjectNode) existing, "serviceProvider");
				copyJsonFieldIfPresent(clinical, (ObjectNode) existing, "location");
				replaceStringExtensionFromExisting(clinical, existing, RECEIVING_PROVINCE_URL);
				replaceStringExtensionFromExisting(clinical, existing, RECEIVING_DISTRICT_URL);
				replaceNestedTransferDetailsExtension(clinical, existing, RECEIVING_PROVINCE_URL);
				replaceNestedTransferDetailsExtension(clinical, existing, RECEIVING_DISTRICT_URL);
			}

			return objectMapper.writeValueAsString(clinical);
		}
		catch (HieApiException ex) {
			throw ex;
		}
		catch (Exception ex) {
			throw new HieApiException("Failed to preserve insurance agent decision on transfer update: "
					+ ex.getMessage(), ex);
		}
	}

	private ObjectNode requireEncounterObject(String json) throws Exception {
		JsonNode root = objectMapper.readTree(json);
		JsonNode encounter = unwrapEncounterNode(root);
		if (encounter == null || !encounter.isObject()) {
			throw new HieApiException("Clinical transfer payload is not a valid FHIR Encounter");
		}
		return (ObjectNode) encounter;
	}

	private JsonNode unwrapEncounter(String json) {
		if (StringUtils.isBlank(json)) {
			return null;
		}
		try {
			return unwrapEncounterNode(objectMapper.readTree(json));
		}
		catch (Exception ex) {
			return null;
		}
	}

	private JsonNode unwrapEncounterNode(JsonNode root) {
		if (root == null || root.isNull()) {
			return null;
		}
		if (root.isObject() && "Encounter".equals(text(root.get("resourceType")))) {
			return root;
		}
		// Parameters / Bundle wrappers occasionally returned by HIE
		JsonNode resource = root.get("resource");
		if (resource != null && resource.isObject() && "Encounter".equals(text(resource.get("resourceType")))) {
			return resource;
		}
		JsonNode entry = root.get("entry");
		if (entry != null && entry.isArray() && entry.size() > 0) {
			JsonNode first = entry.get(0);
			if (first != null && first.isObject()) {
				JsonNode nested = first.get("resource");
				if (nested != null && nested.isObject() && "Encounter".equals(text(nested.get("resourceType")))) {
					return nested;
				}
			}
		}
		return root.isObject() ? root : null;
	}

	private void copyHospitalizationDestination(ObjectNode clinical, ObjectNode existing) {
		JsonNode existingHospitalization = existing.get("hospitalization");
		if (existingHospitalization == null || !existingHospitalization.isObject()) {
			return;
		}
		JsonNode existingDestination = existingHospitalization.get("destination");
		if (existingDestination == null || existingDestination.isNull()) {
			return;
		}
		JsonNode hospitalizationNode = clinical.get("hospitalization");
		ObjectNode hospitalization = hospitalizationNode != null && hospitalizationNode.isObject()
				? (ObjectNode) hospitalizationNode
				: clinical.putObject("hospitalization");
		hospitalization.put("destination", existingDestination);
	}

	private void copyJsonFieldIfPresent(ObjectNode clinical, ObjectNode existing, String fieldName) {
		if (StringUtils.isBlank(fieldName) || clinical == null || existing == null) {
			return;
		}
		JsonNode value = existing.get(fieldName);
		if (value != null && !value.isNull()) {
			clinical.put(fieldName, value);
		}
	}

	/**
	 * Province/district may live under transfer-details in the local rebuild — keep agent values there too.
	 */
	private void replaceNestedTransferDetailsExtension(ObjectNode clinical, JsonNode existing, String url) {
		JsonNode existingDetails = findExtension(existing, TRANSFER_DETAILS_URL);
		JsonNode clinicalDetails = findExtension(clinical, TRANSFER_DETAILS_URL);
		if (existingDetails == null || !existingDetails.isObject()
				|| clinicalDetails == null || !clinicalDetails.isObject()) {
			return;
		}
		JsonNode existingNestedExt = findNestedExtension(existingDetails, url);
		if (existingNestedExt == null || !existingNestedExt.isObject()) {
			return;
		}
		ObjectNode clinicalDetailsObj = (ObjectNode) clinicalDetails;
		ArrayNode nested = nestedExtensionsArray(clinicalDetailsObj);
		removeExtensionByUrl(nested, url);
		nested.add(existingNestedExt);
	}

	private static final String TRANSFER_DETAILS_URL =
			"http://example.rw/fhir/StructureDefinition/transfer-details";

	private JsonNode findNestedExtension(JsonNode parentExtension, String url) {
		if (parentExtension == null || !parentExtension.isObject()) {
			return null;
		}
		JsonNode nested = parentExtension.get("extension");
		if (nested == null || !nested.isArray()) {
			return null;
		}
		Iterator<JsonNode> iterator = nested.getElements();
		while (iterator.hasNext()) {
			JsonNode ext = iterator.next();
			if (ext != null && ext.isObject() && urlMatches(text(ext.get("url")), url)) {
				return ext;
			}
		}
		return null;
	}

	private ArrayNode nestedExtensionsArray(ObjectNode parentExtension) {
		JsonNode existing = parentExtension.get("extension");
		if (existing != null && existing.isArray()) {
			return (ArrayNode) existing;
		}
		return parentExtension.putArray("extension");
	}

	private void replaceStringExtensionFromExisting(ObjectNode clinical, JsonNode existing, String url) {
		JsonNode existingExt = findExtension(existing, url);
		removeExtensionByUrl(extensionsArray(clinical), url);
		if (existingExt != null && existingExt.isObject()) {
			extensionsArray(clinical).add(existingExt);
		}
	}

	private void copyExtensionIfPresent(ObjectNode clinical, JsonNode existing, String url) {
		JsonNode existingExt = findExtension(existing, url);
		if (existingExt != null && existingExt.isObject()) {
			extensionsArray(clinical).add(existingExt);
		}
	}

	/**
	 * Copies remaining HIE-owned extensions (any {@code agent-*} / {@code *insurance-agent*} URL)
	 * that the local clinical rebuild does not already carry.
	 */
	private void copyAdditionalHieOwnedExtensions(ObjectNode clinical, JsonNode existing) {
		JsonNode existingExtensions = existing.get("extension");
		if (existingExtensions == null || !existingExtensions.isArray()) {
			return;
		}
		Set<String> clinicalUrls = extensionUrlSet(clinical);
		Iterator<JsonNode> iterator = existingExtensions.getElements();
		while (iterator.hasNext()) {
			JsonNode ext = iterator.next();
			if (ext == null || !ext.isObject()) {
				continue;
			}
			String url = text(ext.get("url"));
			if (!isHieOwnedExtensionUrl(url)) {
				continue;
			}
			if (urlMatches(url, REQUIRES_VERIFICATION_URL)
					|| urlMatches(url, AGENT_APPROVED_URL)
					|| urlMatches(url, AGENT_COMMENT_URL)) {
				// Already handled explicitly above.
				continue;
			}
			if (clinicalUrlsContains(clinicalUrls, url)) {
				continue;
			}
			extensionsArray(clinical).add(ext);
			clinicalUrls.add(normalizeUrlKey(url));
		}
	}

	private void removeHieOwnedExtensions(ObjectNode encounter) {
		ArrayNode extensions = extensionsArray(encounter);
		for (int i = extensions.size() - 1; i >= 0; i--) {
			JsonNode ext = extensions.get(i);
			if (ext != null && ext.isObject() && isHieOwnedExtensionUrl(text(ext.get("url")))) {
				extensions.remove(i);
			}
		}
	}

	/**
	 * HIE-owned attributes that clinicians do not edit locally: insurance verification gate
	 * and insurance-agent decision fields.
	 */
	static boolean isHieOwnedExtensionUrl(String url) {
		if (StringUtils.isBlank(url)) {
			return false;
		}
		String trimmed = url.trim().toLowerCase();
		String suffix = lastUrlSegment(trimmed);
		return trimmed.contains("insurance-agent")
				|| suffix.startsWith("agent-")
				|| suffix.equals("agent-approved")
				|| suffix.equals("agent-comment")
				|| suffix.equals("requires-insurance-agent-verification");
	}

	private ArrayNode extensionsArray(ObjectNode encounter) {
		JsonNode existing = encounter.get("extension");
		if (existing != null && existing.isArray()) {
			return (ArrayNode) existing;
		}
		return encounter.putArray("extension");
	}

	private Set<String> extensionUrlSet(ObjectNode encounter) {
		Set<String> urls = new HashSet<String>();
		JsonNode extensions = encounter.get("extension");
		if (extensions == null || !extensions.isArray()) {
			return urls;
		}
		Iterator<JsonNode> iterator = extensions.getElements();
		while (iterator.hasNext()) {
			JsonNode ext = iterator.next();
			if (ext != null && ext.isObject()) {
				String url = text(ext.get("url"));
				if (StringUtils.isNotBlank(url)) {
					urls.add(normalizeUrlKey(url));
				}
			}
		}
		return urls;
	}

	private boolean clinicalUrlsContains(Set<String> clinicalUrls, String url) {
		return clinicalUrls.contains(normalizeUrlKey(url))
				|| clinicalUrls.contains(lastUrlSegment(url.trim().toLowerCase()));
	}

	private static String normalizeUrlKey(String url) {
		String trimmed = url.trim().toLowerCase();
		return lastUrlSegment(trimmed);
	}

	private void removeExtensionByUrl(ArrayNode extensions, String url) {
		if (extensions == null || StringUtils.isBlank(url)) {
			return;
		}
		for (int i = extensions.size() - 1; i >= 0; i--) {
			JsonNode ext = extensions.get(i);
			if (ext != null && ext.isObject() && urlMatches(text(ext.get("url")), url)) {
				extensions.remove(i);
			}
		}
	}

	private JsonNode findExtension(JsonNode encounter, String url) {
		if (encounter == null || !encounter.isObject()) {
			return null;
		}
		JsonNode extensions = encounter.get("extension");
		if (extensions == null || !extensions.isArray()) {
			return null;
		}
		Iterator<JsonNode> iterator = extensions.getElements();
		while (iterator.hasNext()) {
			JsonNode ext = iterator.next();
			if (ext != null && ext.isObject() && urlMatches(text(ext.get("url")), url)) {
				return ext;
			}
		}
		return null;
	}

	private static boolean urlMatches(String actual, String expected) {
		if (StringUtils.isBlank(actual) || StringUtils.isBlank(expected)) {
			return false;
		}
		String a = actual.trim();
		String e = expected.trim();
		if (a.equals(e)) {
			return true;
		}
		String suffix = lastUrlSegment(e);
		return a.endsWith("/" + suffix) || a.endsWith(suffix);
	}

	private static String lastUrlSegment(String url) {
		if (url == null || url.isEmpty()) {
			return "";
		}
		int slash = url.lastIndexOf('/');
		return slash >= 0 ? url.substring(slash + 1) : url;
	}

	private static String text(JsonNode node) {
		if (node == null || node.isNull()) {
			return "";
		}
		String value = node.getTextValue();
		return value == null ? "" : value;
	}
}
