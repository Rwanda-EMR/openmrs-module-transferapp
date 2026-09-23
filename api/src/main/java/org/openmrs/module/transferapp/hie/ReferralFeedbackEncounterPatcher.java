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
import org.openmrs.module.transferapp.model.ReferralFeedbackOutcome;
import org.openmrs.module.transferapp.model.TransferReferralFeedback;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Merges referral-feedback and counter-referral extensions into an existing HIE Encounter
 * (see {@code devs/transfer.json} referral-feedback / counter-referral sections).
 */
public class ReferralFeedbackEncounterPatcher {

	public static final String TRANSFER_DETAILS_URL =
			"http://example.rw/fhir/StructureDefinition/transfer-details";

	public static final String OUTCOME_SYSTEM =
			"http://example.rw/fhir/CodeSystem/transfer-outcome";

	public static final String TRANSFER_TYPE_SYSTEM =
			"http://example.rw/fhir/CodeSystem/transfer-type";

	private static final String DATE_PATTERN = "yyyy-MM-dd";

	private final ObjectMapper objectMapper = new ObjectMapper();

	/**
	 * Injects/replaces {@code referral-feedback} and {@code counter-referral} under transfer-details
	 * and sets nested {@code transfer-type} to {@code COUNTER_REFERRAL}.
	 */
	public String mergeFeedbackIntoEncounter(String existingEncounterJson, TransferReferralFeedback feedback) {
		if (feedback == null) {
			throw new HieApiException("Referral feedback is required to build the HIE payload");
		}
		try {
			ObjectNode encounter = requireEncounterObject(existingEncounterJson);
			ArrayNode transferDetailsNested = ensureTransferDetailsNested(encounter);
			setTransferType(transferDetailsNested, "COUNTER_REFERRAL", "Counter referral");
			replaceNestedByUrl(transferDetailsNested, "referral-feedback", buildReferralFeedbackExtension(feedback));
			replaceNestedByUrl(transferDetailsNested, "counter-referral", buildCounterReferralExtension(feedback));
			return objectMapper.writeValueAsString(encounter);
		}
		catch (HieApiException ex) {
			throw ex;
		}
		catch (Exception ex) {
			throw new HieApiException("Failed to build referral feedback HIE payload: " + ex.getMessage(), ex);
		}
	}

	public ObjectNode buildReferralFeedbackExtension(TransferReferralFeedback feedback) {
		ObjectNode root = objectMapper.createObjectNode();
		root.put("url", "referral-feedback");
		ArrayNode nested = root.putArray("extension");

		addString(nested, "final-diagnosis-comment", feedback.getFinalDiagnosis());
		addTreatmentGiven(nested, feedback.getTreatmentGiven());
		addOutcome(nested, feedback.getOutcome());
		addString(nested, "comments", feedback.getRecommendations());
		addDate(nested, "date-of-admission", feedback.getDateOfAdmissionOrSeen());
		addDate(nested, "date-of-discharge", feedback.getDateOfDischarge());
		addString(nested, "contact-person", feedback.getContactPerson());
		addString(nested, "provider-name", feedback.getProviderName());
		addString(nested, "qualification", feedback.getQualification());
		addDate(nested, "signed-date", feedback.getSignedDate());
		addString(nested, "signed-time", feedback.getSignedTime());
		addString(nested, "phone", feedback.getPhone());
		return root;
	}

	public ObjectNode buildCounterReferralExtension(TransferReferralFeedback feedback) {
		ObjectNode root = objectMapper.createObjectNode();
		root.put("url", "counter-referral");
		ArrayNode nested = root.putArray("extension");

		addString(nested, "recommendation", feedback.getRecommendations());
		Date followUp = feedback.getSignedDate() != null ? feedback.getSignedDate() : feedback.getDateOfDischarge();
		addDate(nested, "follow-up-date", followUp);
		addReferredBackTo(nested, feedback);
		addString(nested, "contact-person", feedback.getContactPerson());
		addString(nested, "provider-name", feedback.getProviderName());
		addString(nested, "qualification", feedback.getQualification());
		addDate(nested, "signed-date", feedback.getSignedDate());
		addString(nested, "signed-time", feedback.getSignedTime());
		addString(nested, "phone", feedback.getPhone());
		return root;
	}

	public Map<String, Object> buildPreviewSummary(TransferReferralFeedback feedback) {
		Map<String, Object> summary = new LinkedHashMap<String, Object>();
		if (feedback == null) {
			return summary;
		}
		summary.put("hieTransferId", feedback.getHieTransferId());
		summary.put("finalDiagnosis", StringUtils.trimToEmpty(feedback.getFinalDiagnosis()));
		summary.put("treatmentGiven", StringUtils.trimToEmpty(feedback.getTreatmentGiven()));
		ReferralFeedbackOutcome outcome = ReferralFeedbackOutcome.fromStoredValue(feedback.getOutcome());
		summary.put("outcome", feedback.getOutcome());
		summary.put("outcomeLabel", outcome != null ? outcome.getLabel() : StringUtils.trimToEmpty(feedback.getOutcome()));
		summary.put("outcomeHieCode", outcome != null ? outcome.getHieCode() : StringUtils.trimToEmpty(feedback.getOutcome()));
		summary.put("recommendations", StringUtils.trimToEmpty(feedback.getRecommendations()));
		summary.put("referBackToFacility", StringUtils.trimToEmpty(feedback.getReferBackToFacility()));
		summary.put("referBackToFacilityFosaId", StringUtils.trimToEmpty(feedback.getReferBackToFacilityFosaId()));
		summary.put("dateOfAdmissionOrSeen", formatDate(feedback.getDateOfAdmissionOrSeen()));
		summary.put("dateOfDischarge", formatDate(feedback.getDateOfDischarge()));
		summary.put("followUpDate", formatDate(
				feedback.getSignedDate() != null ? feedback.getSignedDate() : feedback.getDateOfDischarge()));
		summary.put("contactPerson", StringUtils.trimToEmpty(feedback.getContactPerson()));
		summary.put("providerName", StringUtils.trimToEmpty(feedback.getProviderName()));
		summary.put("qualification", StringUtils.trimToEmpty(feedback.getQualification()));
		summary.put("signedDate", formatDate(feedback.getSignedDate()));
		summary.put("signedTime", StringUtils.trimToEmpty(feedback.getSignedTime()));
		summary.put("phone", StringUtils.trimToEmpty(feedback.getPhone()));
		summary.put("transferType", "COUNTER_REFERRAL");
		return summary;
	}

	public String toJson(ObjectNode node) {
		try {
			return objectMapper.writeValueAsString(node);
		}
		catch (Exception ex) {
			throw new HieApiException("Unable to serialize feedback extension JSON: " + ex.getMessage(), ex);
		}
	}

	public String prettyPrintJson(String json) {
		try {
			JsonNode node = objectMapper.readTree(json);
			return objectMapper.writeValueAsString(node);
		}
		catch (Exception ex) {
			return json;
		}
	}

	public String prettyPrint(ObjectNode node) {
		return toJson(node);
	}

	private void addTreatmentGiven(ArrayNode parent, String treatmentGiven) {
		String text = StringUtils.trimToNull(treatmentGiven);
		if (text == null) {
			return;
		}
		ObjectNode treatmentGivenExt = parent.addObject();
		treatmentGivenExt.put("url", "treatment-given");
		ArrayNode nested = treatmentGivenExt.putArray("extension");
		ObjectNode description = nested.addObject();
		description.put("url", "description");
		description.put("valueString", text);
	}

	private void addOutcome(ArrayNode parent, String outcomeStored) {
		ReferralFeedbackOutcome parsed = ReferralFeedbackOutcome.fromStoredValue(outcomeStored);
		String code = parsed != null ? parsed.getHieCode() : StringUtils.trimToNull(outcomeStored);
		String display = parsed != null ? parsed.getHieDisplay() : code;
		if (code == null) {
			return;
		}
		ObjectNode outcomeExt = parent.addObject();
		outcomeExt.put("url", "outcome");
		ObjectNode coding = outcomeExt.putObject("valueCoding");
		coding.put("system", OUTCOME_SYSTEM);
		coding.put("code", code);
		coding.put("display", display);
	}

	private void addReferredBackTo(ArrayNode parent, TransferReferralFeedback feedback) {
		String facilityName = StringUtils.trimToNull(feedback.getReferBackToFacility());
		String fosaId = StringUtils.trimToNull(feedback.getReferBackToFacilityFosaId());
		if (facilityName == null && fosaId == null) {
			return;
		}
		ObjectNode referred = parent.addObject();
		referred.put("url", "referred-back-to");
		ArrayNode nested = referred.putArray("extension");
		if (facilityName != null) {
			ObjectNode nameExt = nested.addObject();
			nameExt.put("url", "facility-name");
			nameExt.put("valueString", facilityName);
		}
		if (fosaId != null) {
			ObjectNode fosaExt = nested.addObject();
			fosaExt.put("url", "fosa-id");
			fosaExt.put("valueString", fosaId);

			ObjectNode refExt = nested.addObject();
			refExt.put("url", "facility-reference");
			ObjectNode valueRef = refExt.putObject("valueReference");
			valueRef.put("reference", "Organization/" + fosaId);
			valueRef.put("type", "Organization");
			if (facilityName != null) {
				valueRef.put("display", facilityName);
			}
		}
	}

	private void addString(ArrayNode parent, String url, String value) {
		String trimmed = StringUtils.trimToNull(value);
		if (trimmed == null) {
			return;
		}
		ObjectNode ext = parent.addObject();
		ext.put("url", url);
		ext.put("valueString", trimmed);
	}

	private void addDate(ArrayNode parent, String url, Date value) {
		String formatted = formatDate(value);
		if (StringUtils.isBlank(formatted)) {
			return;
		}
		ObjectNode ext = parent.addObject();
		ext.put("url", url);
		ext.put("valueDate", formatted);
	}

	private void setTransferType(ArrayNode transferDetailsNested, String code, String display) {
		ObjectNode typeExt = findObjectByUrl(transferDetailsNested, "transfer-type");
		if (typeExt == null) {
			typeExt = transferDetailsNested.addObject();
			typeExt.put("url", "transfer-type");
		}
		ObjectNode coding = typeExt.putObject("valueCoding");
		coding.put("system", TRANSFER_TYPE_SYSTEM);
		coding.put("code", code);
		coding.put("display", display);
	}

	private void replaceNestedByUrl(ArrayNode parent, String url, ObjectNode replacement) {
		removeByUrl(parent, url);
		parent.add(replacement);
	}

	private void removeByUrl(ArrayNode parent, String url) {
		if (parent == null || StringUtils.isBlank(url)) {
			return;
		}
		for (int i = parent.size() - 1; i >= 0; i--) {
			JsonNode node = parent.get(i);
			if (node != null && url.equals(text(node.get("url")))) {
				parent.remove(i);
			}
		}
	}

	private ObjectNode findObjectByUrl(ArrayNode parent, String url) {
		if (parent == null) {
			return null;
		}
		for (int i = 0; i < parent.size(); i++) {
			JsonNode node = parent.get(i);
			if (node != null && node.isObject() && url.equals(text(node.get("url")))) {
				return (ObjectNode) node;
			}
		}
		return null;
	}

	private ArrayNode ensureTransferDetailsNested(ObjectNode encounter) {
		ArrayNode extensions = extensionsArray(encounter);
		ObjectNode transferDetails = findObjectByUrl(extensions, TRANSFER_DETAILS_URL);
		if (transferDetails == null) {
			transferDetails = extensions.addObject();
			transferDetails.put("url", TRANSFER_DETAILS_URL);
		}
		JsonNode nestedNode = transferDetails.get("extension");
		if (nestedNode != null && nestedNode.isArray()) {
			return (ArrayNode) nestedNode;
		}
		return transferDetails.putArray("extension");
	}

	private ArrayNode extensionsArray(ObjectNode encounter) {
		JsonNode extension = encounter.get("extension");
		if (extension != null && extension.isArray()) {
			return (ArrayNode) extension;
		}
		return encounter.putArray("extension");
	}

	private ObjectNode requireEncounterObject(String json) {
		JsonNode root = unwrapEncounter(json);
		if (root == null || !root.isObject()) {
			throw new HieApiException("Existing HIE Encounter JSON is missing or invalid");
		}
		return (ObjectNode) root;
	}

	private JsonNode unwrapEncounter(String json) {
		if (StringUtils.isBlank(json)) {
			return null;
		}
		try {
			JsonNode root = objectMapper.readTree(json);
			if (root == null) {
				return null;
			}
			if (root.isObject() && "Encounter".equals(text(root.get("resourceType")))) {
				return root;
			}
			JsonNode wrapped = root.isObject() ? root.get("resource") : null;
			if (wrapped != null && wrapped.isObject()
					&& "Encounter".equals(text(wrapped.get("resourceType")))) {
				return wrapped;
			}
			if (root.isArray()) {
				Iterator<JsonNode> it = root.getElements();
				while (it.hasNext()) {
					JsonNode item = it.next();
					if (item != null && item.isObject() && "Encounter".equals(text(item.get("resourceType")))) {
						return item;
					}
					JsonNode itemResource = item != null && item.isObject() ? item.get("resource") : null;
					if (itemResource != null && itemResource.isObject()
							&& "Encounter".equals(text(itemResource.get("resourceType")))) {
						return itemResource;
					}
				}
			}
			return root.isObject() ? root : null;
		}
		catch (Exception ex) {
			throw new HieApiException("Unable to parse existing HIE Encounter JSON: " + ex.getMessage(), ex);
		}
	}

	private static String text(JsonNode node) {
		if (node == null || node.isNull()) {
			return null;
		}
		String value = node.getTextValue();
		return value != null ? value : node.toString();
	}

	private static String formatDate(Date date) {
		if (date == null) {
			return "";
		}
		return new SimpleDateFormat(DATE_PATTERN).format(date);
	}
}
