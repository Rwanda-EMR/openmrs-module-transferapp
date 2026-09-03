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
import org.openmrs.User;
import org.openmrs.api.context.Context;
import org.openmrs.module.transferapp.TransferAppConstants;
import org.openmrs.module.transferapp.api.TransferAdminService;
import org.openmrs.module.transferapp.api.TransferProfileService;
import org.openmrs.module.transferapp.model.ReceivingFacility;
import org.openmrs.module.transferapp.model.Transfer;
import org.openmrs.module.transferapp.model.TransferProfile;

import java.util.Locale;
import java.util.UUID;

/**
 * Builds the External (General) Transfer Encounter payload to match
 * {@code devs/transfer.json}:
 * <ul>
 *   <li>{@code status=in-progress}, {@code class=IMP}</li>
 *   <li>{@code patient-phone}, {@code doctor-details}, {@code transfer-details}</li>
 *   <li>{@code transfer-type}: NORMAL_TRANSFER / REFERRAL / COUNTER_REFERRAL</li>
 *   <li>{@code transport} nested under transfer-details</li>
 *   <li>{@code subject}, {@code participant} (ATND), {@code diagnosis},
 *       {@code hospitalization}, {@code location}, {@code serviceProvider}</li>
 * </ul>
 * Optional sections from the sample (referral-feedback, counter-referral) are omitted
 * when the form has no data for them. Referral linkage is included when a previous
 * HIE encounter id is known.
 */
public class TransferEncounterPayloadBuilder {

	private static final String FOSA_ID_SYSTEM = "http://example.rw/fhir/fosa-id";

	private static final String LICENSE_SYSTEM = "http://example.rw/practitioner/license-number";

	private static final String DOCTOR_DETAILS_URL =
			"http://example.rw/fhir/StructureDefinition/doctor-details";

	private static final String TRANSFER_DETAILS_URL =
			"http://example.rw/fhir/StructureDefinition/transfer-details";

	private static final String TRANSFER_TYPE_SYSTEM =
			"http://example.rw/fhir/CodeSystem/transfer-type";

	private static final String TRANSPORT_TYPE_SYSTEM =
			"http://example.rw/fhir/CodeSystem/transfer-transport-type";

	private static final String AMBULANCE_SOURCE_SYSTEM =
			"http://example.rw/fhir/CodeSystem/ambulance-source";

	private static final String QUALIFICATION_SYSTEM =
			"http://example.rw/fhir/CodeSystem/doctor-qualification";

	private static final String SPECIALTY_SYSTEM =
			"http://example.rw/fhir/CodeSystem/doctor-specialty";

	private final ObjectMapper objectMapper = new ObjectMapper();

	private TransferAdminService transferAdminService;

	private TransferProfileService transferProfileService;

	public void setTransferAdminService(TransferAdminService transferAdminService) {
		this.transferAdminService = transferAdminService;
	}

	public void setTransferProfileService(TransferProfileService transferProfileService) {
		this.transferProfileService = transferProfileService;
	}

	public String buildEncounterJson(Transfer transfer, User user, String receivingFacilityLabel) {
		boolean externalReceivingFacility = isExternalReceivingFacility(transfer);
		return buildEncounterJson(transfer, user, receivingFacilityLabel, externalReceivingFacility, null);
	}

	public String buildEncounterJson(Transfer transfer, User user, String receivingFacilityLabel,
			boolean externalReceivingFacility) {
		return buildEncounterJson(transfer, user, receivingFacilityLabel, externalReceivingFacility, null);
	}

	/**
	 * @param forcedEncounterId when set, reused as Encounter.id so HIE updates the same resource
	 * @param externalReceivingFacility retained for callers; insurance-agent flag is not part of
	 *        the {@code transfer.json} shape and is not emitted here
	 */
	public String buildEncounterJson(Transfer transfer, User user, String receivingFacilityLabel,
			boolean externalReceivingFacility, String forcedEncounterId) {
		try {
			String upi = requireUpi(transfer);
			String encounterId = StringUtils.isNotBlank(forcedEncounterId)
					? forcedEncounterId.trim()
					: (transfer.getUuid() != null ? transfer.getUuid() : UUID.randomUUID().toString());
			String transferBusinessId = resolveTransferBusinessId(transfer, encounterId);
			TransferProfile profile = resolveProfile(user);
			String license = resolveLicense(transfer, user, profile);
			String providerDisplay = resolveProviderDisplayName(transfer, user);

			ObjectNode encounter = objectMapper.createObjectNode();
			encounter.put("resourceType", "Encounter");
			encounter.put("id", encounterId);
			encounter.put("status", "in-progress");

			addClass(encounter);
			addTopLevelExtensions(encounter, transfer, user, profile, license, upi, transferBusinessId,
					encounterId);
			addSubject(encounter, upi, transfer.getClientName());
			addParticipant(encounter, user, license, providerDisplay);
			addDiagnosis(encounter, transfer, encounterId);
			addHospitalization(encounter, transfer, receivingFacilityLabel);
			addLocation(encounter, transfer, receivingFacilityLabel);
			addServiceProvider(encounter, transfer, receivingFacilityLabel);

			return objectMapper.writeValueAsString(encounter);
		}
		catch (HieApiException ex) {
			throw ex;
		}
		catch (Exception ex) {
			throw new HieApiException("Failed to build transfer encounter payload", ex);
		}
	}

	public boolean isExternalReceivingFacility(Transfer transfer) {
		return resolveExternalReceivingFacility(transfer);
	}

	private boolean resolveExternalReceivingFacility(Transfer transfer) {
		if (transfer == null || StringUtils.isBlank(transfer.getReceivingFacilityCode())) {
			return false;
		}
		TransferAdminService adminService = getTransferAdminService();
		if (adminService == null) {
			return false;
		}
		String facilityCode = transfer.getReceivingFacilityCode().trim();
		Integer sendingLocationId = adminService.resolveCurrentSendingLocationId();
		ReceivingFacility facility = null;
		if (sendingLocationId != null) {
			facility = adminService.getReceivingFacilityByCode(sendingLocationId, facilityCode);
		}
		return facility != null && facility.isExternal();
	}

	private TransferAdminService getTransferAdminService() {
		if (transferAdminService != null) {
			return transferAdminService;
		}
		try {
			transferAdminService = Context.getRegisteredComponent("transferAdminService", TransferAdminService.class);
			return transferAdminService;
		}
		catch (Exception ignored) {
			return null;
		}
	}

	private void addClass(ObjectNode encounter) {
		ObjectNode classNode = encounter.putObject("class");
		classNode.put("system", "http://terminology.hl7.org/CodeSystem/v3-ActCode");
		classNode.put("code", "IMP");
		classNode.put("display", "inpatient encounter");
	}

	private void addTopLevelExtensions(ObjectNode encounter, Transfer transfer, User user,
			TransferProfile profile, String license, String upi, String transferBusinessId,
			String encounterId) {
		ArrayNode extensions = encounter.putArray("extension");

		String patientPhone = firstNonBlank(transfer.getClientTelephone(), transfer.getCaregiverTelephone());
		if (StringUtils.isNotBlank(patientPhone)) {
			ObjectNode phoneExt = addObjectNode(extensions);
			phoneExt.put("url", "patient-phone");
			phoneExt.put("valueString", patientPhone.trim());
		}

		addDoctorDetailsExtension(extensions, transfer, profile, license);
		addTransferDetailsExtension(extensions, transfer, upi, transferBusinessId, encounterId);
	}

	private void addDoctorDetailsExtension(ArrayNode extensions, Transfer transfer, TransferProfile profile,
			String license) {
		String qualification = firstNonBlank(
				transfer.getProviderQualification(),
				profile != null ? profile.getQualification() : null);
		String specialty = profile != null ? StringUtils.trimToNull(profile.getSpeciality()) : null;
		String phone = firstNonBlank(
				transfer.getProviderPhone(),
				profile != null ? profile.getPhoneNumber() : null);
		if (StringUtils.isBlank(license) && StringUtils.isBlank(qualification)
				&& StringUtils.isBlank(specialty) && StringUtils.isBlank(phone)) {
			return;
		}

		ObjectNode doctorDetails = addObjectNode(extensions);
		doctorDetails.put("url", DOCTOR_DETAILS_URL);
		ArrayNode nested = doctorDetails.putArray("extension");

		if (StringUtils.isNotBlank(license)) {
			ObjectNode licenseExt = addObjectNode(nested);
			licenseExt.put("url", "license-number");
			licenseExt.put("valueString", license.trim());
		}
		if (StringUtils.isNotBlank(qualification)) {
			addCodeableConceptNested(nested, "qualification", QUALIFICATION_SYSTEM,
					toCodeToken(qualification), qualification.trim());
		}
		if (StringUtils.isNotBlank(specialty)) {
			addCodeableConceptNested(nested, "specialty", SPECIALTY_SYSTEM,
					toCodeToken(specialty), specialty.trim());
		}
		if (StringUtils.isNotBlank(phone)) {
			ObjectNode phoneExt = addObjectNode(nested);
			phoneExt.put("url", "phone-number");
			phoneExt.put("valueString", phone.trim());
		}
	}

	private void addTransferDetailsExtension(ArrayNode extensions, Transfer transfer, String upi,
			String transferBusinessId, String encounterId) {
		ObjectNode transferDetails = addObjectNode(extensions);
		transferDetails.put("url", TRANSFER_DETAILS_URL);
		ArrayNode nested = transferDetails.putArray("extension");

		// transfer-id — optional in sample
		ObjectNode transferIdExt = addObjectNode(nested);
		transferIdExt.put("url", "transfer-id");
		transferIdExt.put("valueString", transferBusinessId);

		// transfer-type: NORMAL_TRANSFER | REFERRAL | COUNTER_REFERRAL
		String workflowType = resolveWorkflowTransferType(transfer);
		ObjectNode typeExt = addObjectNode(nested);
		typeExt.put("url", "transfer-type");
		ObjectNode typeCoding = typeExt.putObject("valueCoding");
		typeCoding.put("system", TRANSFER_TYPE_SYSTEM);
		typeCoding.put("code", workflowType);
		typeCoding.put("display", workflowTransferTypeDisplay(workflowType));

		ObjectNode qrExt = addObjectNode(nested);
		qrExt.put("url", "qr-code");
		qrExt.put("valueString", "TRF:" + transferBusinessId + "|PATIENT:" + upi);

		// previous-encounter is required when linking to a prior transfer encounter
		addReferralLinkageIfPresent(nested, transfer, encounterId);

		addTransportExtension(nested, transfer);
	}

	/**
	 * External Transfer Form urgency (EMERGENCY / NOT_EMERGENCY / FOLLOW_UP) is separate from
	 * FHIR workflow transfer-type. Outbound general transfers are REFERRAL by default;
	 * FOLLOW_UP maps to NORMAL_TRANSFER; explicit counter-referral is not collected on this form.
	 */
	private static String resolveWorkflowTransferType(Transfer transfer) {
		String urgency = transfer != null ? StringUtils.trimToEmpty(transfer.getTransferType()) : "";
		if ("FOLLOW_UP".equals(urgency)) {
			return "NORMAL_TRANSFER";
		}
		return "REFERRAL";
	}

	private static String workflowTransferTypeDisplay(String code) {
		if ("NORMAL_TRANSFER".equals(code)) {
			return "Normal Transfer";
		}
		if ("COUNTER_REFERRAL".equals(code)) {
			return "Counter Referral";
		}
		return "Referral";
	}

	private void addReferralLinkageIfPresent(ArrayNode transferDetailsNested, Transfer transfer,
			String currentEncounterId) {
		String previousEncounterId = StringUtils.trimToNull(transfer.getHieTransferId());
		if (previousEncounterId == null || previousEncounterId.equals(currentEncounterId)) {
			return;
		}
		ObjectNode referral = addObjectNode(transferDetailsNested);
		referral.put("url", "referral");
		ArrayNode nested = referral.putArray("extension");

		ObjectNode previous = addObjectNode(nested);
		previous.put("url", "previous-encounter");
		ObjectNode ref = previous.putObject("valueReference");
		ref.put("reference", "Encounter/" + previousEncounterId);
		ref.put("type", "Encounter");
	}

	private void addTransportExtension(ArrayNode transferDetailsNested, Transfer transfer) {
		String transportType = StringUtils.trimToNull(transfer.getTransportType());
		boolean ambulance = "AMBULANCE".equals(transportType);
		boolean other = "OTHER".equals(transportType);
		boolean na = "NA".equals(transportType);
		boolean emergency = "EMERGENCY".equals(StringUtils.trimToEmpty(transfer.getTransferType()));
		boolean required = ambulance || emergency;

		if (transportType == null && !required) {
			return;
		}

		ObjectNode transport = addObjectNode(transferDetailsNested);
		transport.put("url", "transport");
		ArrayNode nested = transport.putArray("extension");

		ObjectNode requiredExt = addObjectNode(nested);
		requiredExt.put("url", "required");
		requiredExt.put("valueBoolean", required);

		if (ambulance || other) {
			ObjectNode typeExt = addObjectNode(nested);
			typeExt.put("url", "transport-type");
			ObjectNode coding = typeExt.putObject("valueCoding");
			coding.put("system", TRANSPORT_TYPE_SYSTEM);
			if (ambulance) {
				coding.put("code", "AMBULANCE");
				coding.put("display", "Ambulance");
			}
			else {
				coding.put("code", "OTHER");
				coding.put("display", "Other");
			}
		}
		else if (na) {
			ObjectNode typeExt = addObjectNode(nested);
			typeExt.put("url", "transport-type");
			ObjectNode coding = typeExt.putObject("valueCoding");
			coding.put("system", TRANSPORT_TYPE_SYSTEM);
			coding.put("code", "OTHER");
			coding.put("display", "Other");
		}

		if (ambulance) {
			addAmbulanceSourceAndProvider(nested, transfer);
		}

		String comments = firstNonBlank(transfer.getTransportOther(), transfer.getOtherNotes());
		if (StringUtils.isNotBlank(comments)) {
			ObjectNode commentsExt = addObjectNode(nested);
			commentsExt.put("url", "transport-comments");
			commentsExt.put("valueString", comments.trim());
		}
	}

	private void addAmbulanceSourceAndProvider(ArrayNode transportNested, Transfer transfer) {
		String providerFosa = StringUtils.trimToNull(transfer.getAmbulanceProviderFosaId());
		String providerName = StringUtils.trimToNull(transfer.getAmbulanceProviderName());
		String sendingFosa = resolveSendingFosaId();
		boolean ownAmbulance = providerFosa == null
				|| (sendingFosa != null && sendingFosa.equalsIgnoreCase(providerFosa));

		ObjectNode sourceExt = addObjectNode(transportNested);
		sourceExt.put("url", "ambulance-source");
		ObjectNode sourceCoding = sourceExt.putObject("valueCoding");
		sourceCoding.put("system", AMBULANCE_SOURCE_SYSTEM);
		if (ownAmbulance) {
			sourceCoding.put("code", "OWN");
			sourceCoding.put("display", "Facility-owned ambulance");
		}
		else {
			sourceCoding.put("code", "BORROWED_FACILITY");
			sourceCoding.put("display", "Borrowed from another health facility");
		}

		if (StringUtils.isNotBlank(providerName)) {
			ObjectNode nameExt = addObjectNode(transportNested);
			nameExt.put("url", "facility-name");
			nameExt.put("valueString", providerName);
		}
		if (StringUtils.isNotBlank(providerFosa)) {
			ObjectNode fosaExt = addObjectNode(transportNested);
			fosaExt.put("url", "fosa-id");
			fosaExt.put("valueString", providerFosa);
		}
	}

	private void addSubject(ObjectNode encounter, String upi, String clientName) {
		ObjectNode subject = encounter.putObject("subject");
		subject.put("reference", "Patient/" + upi);
		subject.put("type", "Patient");
		ObjectNode identifier = subject.putObject("identifier");
		identifier.put("system", "UPI");
		identifier.put("value", upi);
		subject.put("display", blankToDefault(clientName, "Patient"));
	}

	private void addParticipant(ObjectNode encounter, User user, String license, String displayName) {
		String practitionerId = resolvePractitionerId(user, license);
		ObjectNode participant = addObjectNode(encounter.putArray("participant"));
		ObjectNode participantType = addObjectNode(participant.putArray("type"));
		ObjectNode participantCoding = addObjectNode(participantType.putArray("coding"));
		participantCoding.put("system", "http://terminology.hl7.org/CodeSystem/v3-ParticipationType");
		participantCoding.put("code", "ATND");
		participantCoding.put("display", "attender");

		ObjectNode individual = participant.putObject("individual");
		individual.put("reference", "Practitioner/" + practitionerId);
		individual.put("type", "Practitioner");
		if (StringUtils.isNotBlank(license)) {
			ObjectNode identifier = individual.putObject("identifier");
			identifier.put("system", LICENSE_SYSTEM);
			identifier.put("value", license.trim());
		}
		individual.put("display", blankToDefault(displayName, "Referring provider"));
	}

	private void addDiagnosis(ObjectNode encounter, Transfer transfer, String encounterId) {
		String display = firstNonBlank(transfer.getDiagnosis(), transfer.getReasonForTransfer());
		ObjectNode diagnosis = addObjectNode(encounter.putArray("diagnosis"));
		ObjectNode condition = diagnosis.putObject("condition");
		condition.put("reference", "Condition/transfer-diagnosis-" + encounterId);
		condition.put("type", "Condition");
		condition.put("display", blankToDefault(display, ""));
		ObjectNode use = diagnosis.putObject("use");
		ObjectNode useCoding = addObjectNode(use.putArray("coding"));
		useCoding.put("system", "http://terminology.hl7.org/CodeSystem/diagnosis-role");
		useCoding.put("code", "AD");
		useCoding.put("display", "Admission diagnosis");
		diagnosis.put("rank", 1);
	}

	private void addHospitalization(ObjectNode encounter, Transfer transfer, String receivingFacilityLabel) {
		ObjectNode hospitalization = encounter.putObject("hospitalization");

		ObjectNode origin = hospitalization.putObject("origin");
		populateFacilityReference(origin, "Location", resolveSendingFosaId(),
				transfer.getSendingFacility(), "Referring facility");

		ObjectNode destination = hospitalization.putObject("destination");
		populateFacilityReference(destination, "Location", transfer.getReceivingFacilityCode(),
				receivingFacilityLabel, "Receiving facility");
	}

	private void addLocation(ObjectNode encounter, Transfer transfer, String receivingFacilityLabel) {
		ObjectNode locationEntry = addObjectNode(encounter.putArray("location"));
		ObjectNode location = locationEntry.putObject("location");
		String receivingCode = StringUtils.trimToNull(transfer.getReceivingFacilityCode());
		String service = StringUtils.trimToNull(transfer.getReceivingService());
		String display = firstNonBlank(
				service != null && receivingFacilityLabel != null
						? receivingFacilityLabel.trim() + " " + service
						: null,
				receivingFacilityLabel,
				service,
				"Receiving facility");
		if (receivingCode != null) {
			String ref = service != null
					? "Location/" + receivingCode + "-" + toCodeToken(service).toLowerCase(Locale.ENGLISH)
					: "Location/" + receivingCode;
			location.put("reference", ref);
		}
		location.put("type", "Location");
		location.put("display", display);
		locationEntry.put("status", "active");
	}

	private void addServiceProvider(ObjectNode encounter, Transfer transfer, String receivingFacilityLabel) {
		ObjectNode serviceProvider = encounter.putObject("serviceProvider");
		populateFacilityReference(serviceProvider, "Organization", transfer.getReceivingFacilityCode(),
				receivingFacilityLabel, "Receiving facility");
	}

	private void populateFacilityReference(ObjectNode node, String resourceType, String fosaId,
			String displayName, String fallbackDisplay) {
		String display = blankToDefault(displayName, fallbackDisplay);
		String code = StringUtils.trimToNull(fosaId);
		if (code != null) {
			node.put("reference", resourceType + "/" + code);
		}
		node.put("type", resourceType);
		if (code != null) {
			ObjectNode identifier = node.putObject("identifier");
			identifier.put("system", FOSA_ID_SYSTEM);
			identifier.put("value", code);
		}
		node.put("display", display);
	}

	private void addCodeableConceptNested(ArrayNode nested, String url, String system, String code,
			String display) {
		ObjectNode ext = addObjectNode(nested);
		ext.put("url", url);
		ObjectNode value = ext.putObject("valueCodeableConcept");
		ObjectNode coding = addObjectNode(value.putArray("coding"));
		coding.put("system", system);
		coding.put("code", code);
		coding.put("display", display);
	}

	private ObjectNode addObjectNode(ArrayNode arrayNode) {
		return (ObjectNode) arrayNode.addObject();
	}

	private TransferProfile resolveProfile(User user) {
		TransferProfileService profileService = getTransferProfileService();
		if (profileService == null || user == null) {
			return null;
		}
		try {
			return profileService.getProfileForUser(user);
		}
		catch (Exception ignored) {
			return null;
		}
	}

	private TransferProfileService getTransferProfileService() {
		if (transferProfileService != null) {
			return transferProfileService;
		}
		try {
			transferProfileService = Context.getRegisteredComponent("transferProfileService",
					TransferProfileService.class);
			return transferProfileService;
		}
		catch (Exception ignored) {
			return null;
		}
	}

	private String resolveLicense(Transfer transfer, User user, TransferProfile profile) {
		if (profile != null && StringUtils.isNotBlank(profile.getLicenseNumber())) {
			return profile.getLicenseNumber().trim();
		}
		// Referring provider name may already be formatted as Name(LICENSE)
		String formatted = transfer != null ? StringUtils.trimToNull(transfer.getReferringProviderName()) : null;
		if (formatted != null) {
			int open = formatted.lastIndexOf('(');
			int close = formatted.lastIndexOf(')');
			if (open >= 0 && close > open + 1) {
				return formatted.substring(open + 1, close).trim();
			}
		}
		return null;
	}

	private String resolveProviderDisplayName(Transfer transfer, User user) {
		String raw = transfer != null ? StringUtils.trimToNull(transfer.getReferringProviderName()) : null;
		if (raw != null) {
			int open = raw.lastIndexOf('(');
			if (open > 0) {
				return raw.substring(0, open).trim();
			}
			return raw;
		}
		return resolveUserDisplayName(user);
	}

	private static String resolveTransferBusinessId(Transfer transfer, String encounterId) {
		if (transfer != null && transfer.getTransferId() != null) {
			return "TRF-" + transfer.getTransferId();
		}
		if (transfer != null && StringUtils.isNotBlank(transfer.getUuid())) {
			return transfer.getUuid().trim();
		}
		return encounterId;
	}

	private static String resolvePractitionerId(User user, String license) {
		if (StringUtils.isNotBlank(license)) {
			return "doctor-" + toCodeToken(license).toLowerCase(Locale.ENGLISH);
		}
		if (user != null && user.getUserId() != null) {
			return "transferapp-user-" + user.getUserId();
		}
		return "transferapp-user-unknown";
	}

	private String resolveSendingFosaId() {
		String fosaId = Context.getAdministrationService().getGlobalProperty(
				TransferAppConstants.GP_SENDING_FOSA_ID,
				TransferAppConstants.DEFAULT_SENDING_FOSA_ID);
		return StringUtils.trimToNull(fosaId);
	}

	private static String requireUpi(Transfer transfer) {
		if (StringUtils.isBlank(transfer.getEmrId())) {
			throw new HieApiException("Cannot submit transfer: patient UPI (EMR ID) is missing.");
		}
		return transfer.getEmrId().trim();
	}

	private static String resolveUserDisplayName(User user) {
		if (user != null && user.getPerson() != null && user.getPerson().getPersonName() != null) {
			return user.getPerson().getPersonName().getFullName();
		}
		return "Referring provider";
	}

	private static String toCodeToken(String value) {
		return StringUtils.trimToEmpty(value)
				.toUpperCase(Locale.ENGLISH)
				.replaceAll("[^A-Z0-9]+", "_")
				.replaceAll("^_|_$", "");
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

	private static String blankToDefault(String value, String defaultValue) {
		return StringUtils.isNotBlank(value) ? value.trim() : defaultValue;
	}
}
