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
import org.openmrs.module.transferapp.model.TransferFormKind;
import org.openmrs.module.transferapp.model.TransferProfile;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TransferEncounterPayloadBuilder {

	private static final TimeZone RWANDA = TimeZone.getTimeZone("Africa/Kigali");

	private static final String SNOMED_SYSTEM = "http://snomed.info/sct";

	private static final String SNOMED_UNKNOWN_REASON_CODE = "261665006";

	private static final String SNOMED_UNKNOWN_REASON_DISPLAY = "Unknown (qualifier)";

	private static final Pattern AGE_ONLY = Pattern.compile("^(\\d+)\\s*years?$", Pattern.CASE_INSENSITIVE);

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
	 */
	public String buildEncounterJson(Transfer transfer, User user, String receivingFacilityLabel,
			boolean externalReceivingFacility, String forcedEncounterId) {
		try {
			String upi = requireUpi(transfer);
			String encounterId = StringUtils.isNotBlank(forcedEncounterId)
					? forcedEncounterId.trim()
					: (transfer.getUuid() != null ? transfer.getUuid() : UUID.randomUUID().toString());
			ObjectNode encounter = objectMapper.createObjectNode();
			encounter.put("resourceType", "Encounter");
			encounter.put("id", encounterId);
			encounter.put("status", "finished");

			addMeta(encounter);
			addExtensions(encounter, transfer, user, receivingFacilityLabel, externalReceivingFacility);
			addClass(encounter, transfer.getTransferType());
			addType(encounter);
			addServiceType(encounter, transfer.getReceivingService());
			addSubject(encounter, upi, transfer.getClientName());
			addParticipant(encounter, transfer, user);

			Date periodStart = transfer.getAdmissionAt() != null ? transfer.getAdmissionAt() : transfer.getDecisionToTransferAt();
			Date periodEnd = transfer.getDecisionToTransferAt() != null ? transfer.getDecisionToTransferAt() : periodStart;
			addPeriod(encounter, periodStart, periodEnd);
			addLength(encounter, periodStart, periodEnd);
			addReasonCode(encounter, transfer.getReasonForTransfer());
			addDiagnosis(encounter, transfer);
			addHospitalization(encounter, transfer, receivingFacilityLabel);
			addLocation(encounter, transfer, periodStart, periodEnd);
			addPartOf(encounter, transfer);

			return objectMapper.writeValueAsString(encounter);
		}
		catch (HieApiException ex) {
			throw ex;
		}
		catch (Exception ex) {
			throw new HieApiException("Failed to build transfer encounter payload", ex);
		}
	}

	/**
	 * Reads Destinations configuration for the transfer's selected receiving facility code.
	 */
	public boolean isExternalReceivingFacility(Transfer transfer) {
		return resolveExternalReceivingFacility(transfer);
	}

	/**
	 * Reads Destinations configuration for the transfer's selected receiving facility code.
	 */
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

	private void addMeta(ObjectNode encounter) {
		ObjectNode meta = encounter.putObject("meta");
		ObjectNode tag = addObjectNode(meta.putArray("tag"));
		tag.put("system", "http://fhir.openmrs.org/ext/encounter-tag");
		tag.put("code", "encounter");
		tag.put("display", "Encounter");
	}

	private void addExtensions(ObjectNode encounter, Transfer transfer, User user, String receivingFacilityLabel,
			boolean externalReceivingFacility) {
		addTransferFormKindExtension(encounter, transfer);
		addTransferTypeExtension(encounter, transfer.getTransferType());
		// Dedicated timestamps (not derived from Encounter.period / length calculations).
		addDateTimeExtension(encounter, "http://example.org/fhir/StructureDefinition/admission-datetime",
				transfer.getAdmissionAt());
		addDateTimeExtension(encounter, "http://example.org/fhir/StructureDefinition/decision-to-transfer-datetime",
				transfer.getDecisionToTransferAt());
		// Calling time is when the receiving facility staff was contacted (with staff name/phone).
		addDateTimeExtension(encounter, "http://example.org/fhir/StructureDefinition/calling-time",
				combineDateAndTime(transfer.getDecisionToTransferAt(), transfer.getCallingTime()));
		// Ambulance call time applies to emergency / ambulance transfers only.
		addDateTimeExtension(encounter, "http://example.org/fhir/StructureDefinition/ambulance-call-time",
				combineDateAndTime(transfer.getDecisionToTransferAt(), transfer.getAmbulanceCallTime()));
		addDateTimeExtension(encounter, "http://example.org/fhir/StructureDefinition/departure-time",
				combineDateAndTime(transfer.getDecisionToTransferAt(), transfer.getDepartRefTime()));
		// Dedicated referring unit (not only hospitalization.admitSource) so other systems can read it.
		addStringExtension(encounter, "http://example.org/fhir/StructureDefinition/referring-department",
				transfer.getReferringUnit());
		addReceivingClinicianContactExtension(encounter, transfer);
		addStringExtension(encounter, "http://example.org/fhir/StructureDefinition/receiving-province",
				transfer.getReceivingProvince());
		addStringExtension(encounter, "http://example.org/fhir/StructureDefinition/receiving-district",
				transfer.getReceivingDistrict());
		addInsuranceExtension(encounter, transfer.getHealthInsuranceType());
		addExternalFacilityExtension(encounter, externalReceivingFacility);
		addCaregiverExtension(encounter, transfer);
		addStringExtension(encounter, "http://example.org/fhir/StructureDefinition/vital-signs", formatVitals(transfer));
		addStringExtension(encounter, "http://example.org/fhir/StructureDefinition/clinical-presentation",
				transfer.getClinicalPresentation());
		addStringExtension(encounter, "http://example.org/fhir/StructureDefinition/lab-results",
				transfer.getLaboratory());
		addStringExtension(encounter, "http://example.org/fhir/StructureDefinition/others-notes",
				transfer.getOtherNotes());
		addStringExtension(encounter, "http://example.org/fhir/StructureDefinition/procedures-treatments",
				transfer.getProceduresTreatments());
		addTransportExtension(encounter, transfer.getTransportType());
		addAmbulanceProviderFacilityExtension(encounter, transfer);
		addPatientDemographicsExtension(encounter, transfer);
		addPatientAddressExtension(encounter, transfer);
		addPractitionerInfoExtension(encounter, transfer, user);
	}

	private void addClass(ObjectNode encounter, String transferType) {
		ObjectNode classNode = encounter.putObject("class");
		classNode.put("system", "http://terminology.hl7.org/CodeSystem/v3-ActCode");
		boolean emergency = "EMERGENCY".equals(transferType);
		classNode.put("code", emergency ? "EMER" : "AMB");
		classNode.put("display", emergency ? "Emergency" : transferTypeLabel(transferType));
	}

	private void addType(ObjectNode encounter) {
		ObjectNode typeEntry = addObjectNode(encounter.putArray("type"));
		ObjectNode coding = addObjectNode(typeEntry.putArray("coding"));
		coding.put("code", "TRANSFER_ENCOUNTER");
		coding.put("display", "TRANSFER_ENCOUNTER");
		typeEntry.put("text", "External Transfer");
	}

	private void addServiceType(ObjectNode encounter, String receivingService) {
		ObjectNode serviceType = encounter.putObject("serviceType");
		ObjectNode coding = addObjectNode(serviceType.putArray("coding"));
		coding.put("system", "http://terminology.hl7.org/CodeSystem/service-type");
		coding.put("code", "253");
		coding.put("display", StringUtils.isNotBlank(receivingService) ? receivingService.trim() : "");
	}

	private void addSubject(ObjectNode encounter, String upi, String clientName) {
		ObjectNode subject = encounter.putObject("subject");
		subject.put("reference", "Patient/" + upi);
		subject.put("type", "Patient");
		ObjectNode subjectIdentifier = subject.putObject("identifier");
		ObjectNode identifierType = subjectIdentifier.putObject("type");
		ObjectNode upiCoding = addObjectNode(identifierType.putArray("coding"));
		upiCoding.put("code", "UPI");
		upiCoding.put("display", "UPI");
		subjectIdentifier.put("value", upi);
		subject.put("display", blankToDefault(clientName, "Patient"));
	}

	private void addParticipant(ObjectNode encounter, Transfer transfer, User user) {
		String practitionerId = resolvePractitionerId(user);
		String displayName = formatReferringProviderName(transfer, user);

		ObjectNode participant = addObjectNode(encounter.putArray("participant"));
		ObjectNode participantType = addObjectNode(participant.putArray("type"));
		ObjectNode participantCoding = addObjectNode(participantType.putArray("coding"));
		participantCoding.put("system", "http://terminology.hl7.org/CodeSystem/v3-ParticipationType");
		participantCoding.put("code", "REF");
		participantCoding.put("display", "Referrer");
		ObjectNode individual = participant.putObject("individual");
		individual.put("reference", "Practitioner/" + practitionerId);
		individual.put("type", "Practitioner");
		individual.putObject("identifier").put("value", practitionerId);
		individual.put("display", displayName);
	}

	private void addPeriod(ObjectNode encounter, Date periodStart, Date periodEnd) {
		ObjectNode period = encounter.putObject("period");
		String start = formatDateTime(periodStart);
		String end = formatDateTime(periodEnd);
		if (start != null) {
			period.put("start", start);
		}
		if (end != null) {
			period.put("end", end);
		}
	}

	private void addLength(ObjectNode encounter, Date periodStart, Date periodEnd) {
		if (periodStart == null || periodEnd == null) {
			return;
		}
		long diffMs = Math.max(0, periodEnd.getTime() - periodStart.getTime());
		long hours = Math.max(1, diffMs / (1000L * 60L * 60L));
		ObjectNode length = encounter.putObject("length");
		length.put("value", hours);
		length.put("unit", "hours");
		length.put("system", "http://unitsofmeasure.org");
		length.put("code", "h");
	}

	private void addReasonCode(ObjectNode encounter, String reasonForTransfer) {
		ObjectNode reason = addObjectNode(encounter.putArray("reasonCode"));
		ObjectNode coding = addObjectNode(reason.putArray("coding"));
		coding.put("system", SNOMED_SYSTEM);
		coding.put("code", SNOMED_UNKNOWN_REASON_CODE);
		coding.put("display", SNOMED_UNKNOWN_REASON_DISPLAY);
		if (StringUtils.isNotBlank(reasonForTransfer)) {
			reason.put("text", reasonForTransfer.trim());
		}
	}

	private void addDiagnosis(ObjectNode encounter, Transfer transfer) {
		String conditionRef = transfer.getTransferId() != null
				? "Condition/transfer-diagnosis-" + transfer.getTransferId()
				: "Condition/transfer-diagnosis";
		String display = StringUtils.isNotBlank(transfer.getDiagnosis())
				? transfer.getDiagnosis().trim()
				: "";

		ObjectNode diagnosis = addObjectNode(encounter.putArray("diagnosis"));
		ObjectNode condition = diagnosis.putObject("condition");
		condition.put("reference", conditionRef);
		condition.put("display", display);
		ObjectNode use = diagnosis.putObject("use");
		ObjectNode useCoding = addObjectNode(use.putArray("coding"));
		useCoding.put("system", "http://terminology.hl7.org/CodeSystem/diagnosis-role");
		useCoding.put("code", "AD");
		useCoding.put("display", "Admission diagnosis");
	}

	private void addHospitalization(ObjectNode encounter, Transfer transfer, String receivingFacilityLabel) {
		ObjectNode hospitalization = encounter.putObject("hospitalization");

		ObjectNode origin = hospitalization.putObject("origin");
		populateLocationReference(origin, resolveSendingFosaId(), transfer.getSendingFacility(), "Referring facility");

		ObjectNode admitSource = hospitalization.putObject("admitSource");
		ObjectNode admitCoding = addObjectNode(admitSource.putArray("coding"));
		admitCoding.put("system", "http://terminology.hl7.org/CodeSystem/admit-source");
		admitCoding.put("code", "hosp-trans");
		admitCoding.put("display", blankToDefault(transfer.getReferringUnit(), "Hospital Transfer"));

		ObjectNode destination = hospitalization.putObject("destination");
		populateLocationReference(destination, transfer.getReceivingFacilityCode(), receivingFacilityLabel, "Receiving facility");

		ObjectNode dischargeDisposition = hospitalization.putObject("dischargeDisposition");
		ObjectNode dischargeCoding = addObjectNode(dischargeDisposition.putArray("coding"));
		dischargeCoding.put("system", "http://terminology.hl7.org/CodeSystem/discharge-disposition");
		dischargeCoding.put("code", "hosp");
		dischargeCoding.put("display", blankToDefault(transfer.getReceivingService(), "Hospital"));
	}

	private void addLocation(ObjectNode encounter, Transfer transfer, Date periodStart, Date periodEnd) {
		ObjectNode locationEntry = addObjectNode(encounter.putArray("location"));
		ObjectNode location = locationEntry.putObject("location");
		populateLocationReference(location, resolveSendingFosaId(), transfer.getSendingFacility(), "Referring facility");
		locationEntry.put("status", "completed");

		ObjectNode period = locationEntry.putObject("period");
		String start = formatDateTime(periodStart);
		String end = formatDateTime(periodEnd);
		if (start != null) {
			period.put("start", start);
		}
		if (end != null) {
			period.put("end", end);
		}
	}

	private void addPartOf(ObjectNode encounter, Transfer transfer) {
		if (transfer.getTransferId() == null) {
			return;
		}
		encounter.putObject("partOf").put("reference", "Encounter/transfer-admission-" + transfer.getTransferId());
	}

	private ObjectNode addObjectNode(ArrayNode arrayNode) {
		return (ObjectNode) arrayNode.addObject();
	}

	private void addCodeableConceptExtension(ObjectNode encounter, String url, String system, String code, String display) {
		ObjectNode extension = addObjectNode(extensionsArray(encounter));
		extension.put("url", url);
		ObjectNode value = extension.putObject("valueCodeableConcept");
		ObjectNode coding = addObjectNode(value.putArray("coding"));
		coding.put("system", system);
		coding.put("code", code);
		coding.put("display", display);
	}

	private void populateLocationReference(ObjectNode node, String facilityCode, String displayName, String fallbackDisplay) {
		String display = blankToDefault(displayName, fallbackDisplay);
		node.put("display", display);
		if (StringUtils.isNotBlank(facilityCode)) {
			node.put("reference", "Location/" + facilityCode.trim());
			ObjectNode identifier = node.putObject("identifier");
			identifier.put("system", "FOSAID");
			identifier.put("value", facilityCode.trim());
		}
	}

	private String resolveSendingFosaId() {
		String fosaId = Context.getAdministrationService().getGlobalProperty(
				TransferAppConstants.GP_SENDING_FOSA_ID,
				TransferAppConstants.DEFAULT_SENDING_FOSA_ID);
		return StringUtils.trimToNull(fosaId);
	}

	/**
	 * Marks which transfer form this Encounter represents so consumers can choose the
	 * correct preview / PDF layout (external vs maternity vs neonatal).
	 */
	private void addTransferFormKindExtension(ObjectNode encounter, Transfer transfer) {
		TransferFormKind kind = transfer != null && transfer.getFormKind() != null
				? transfer.getFormKind()
				: TransferFormKind.GENERAL;
		addCodeableConceptExtension(encounter,
				TransferFormKind.EXTENSION_URL,
				TransferFormKind.CODE_SYSTEM,
				kind.getCode(),
				kind.getDisplay());
	}

	private void addTransferTypeExtension(ObjectNode encounter, String transferType) {
		if (StringUtils.isBlank(transferType)) {
			return;
		}
		String code;
		String display = transferTypeLabel(transferType);
		if ("EMERGENCY".equals(transferType)) {
			code = "emergency";
		}
		else if ("NOT_EMERGENCY".equals(transferType)) {
			code = "not-emergency";
		}
		else if ("FOLLOW_UP".equals(transferType)) {
			code = "follow-up";
		}
		else {
			code = transferType.toLowerCase(Locale.ENGLISH);
		}
		ObjectNode extension = addObjectNode(extensionsArray(encounter));
		extension.put("url", "http://example.org/fhir/StructureDefinition/transfer-type");
		ObjectNode value = extension.putObject("valueCodeableConcept");
		ObjectNode coding = addObjectNode(value.putArray("coding"));
		coding.put("system", "http://example.org/fhir/CodeSystem/transfer-type");
		coding.put("code", code);
		coding.put("display", display);
	}

	private void addInsuranceExtension(ObjectNode encounter, String insuranceType) {
		if (StringUtils.isBlank(insuranceType)) {
			return;
		}
		String code;
		String display;
		if ("CBHI".equals(insuranceType)) {
			code = "cbhi";
			display = "CBHI (mutuelle)";
		}
		else if ("RSSB".equals(insuranceType)) {
			code = "rssb";
			display = "RSSB";
		}
		else if ("MMI".equals(insuranceType)) {
			code = "mmi";
			display = "MMI";
		}
		else if ("OTHER".equals(insuranceType)) {
			code = "other";
			display = "Other (Specify)";
		}
		else if ("NONE".equals(insuranceType)) {
			code = "none";
			display = "None";
		}
		else {
			code = insuranceType.toLowerCase(Locale.ENGLISH);
			display = insuranceType;
		}
		addCodeableConceptExtension(encounter,
				"http://example.org/fhir/StructureDefinition/insurance-type",
				"http://example.org/fhir/CodeSystem/insurance-type",
				code,
				display);
	}

	private void addTransportExtension(ObjectNode encounter, String transportType) {
		if (StringUtils.isBlank(transportType)) {
			return;
		}
		String code;
		String display;
		if ("AMBULANCE".equals(transportType)) {
			code = "ambulance";
			display = "Ambulance";
		}
		else if ("OTHER".equals(transportType)) {
			code = "other";
			display = "Other (specify)";
		}
		else if ("NA".equals(transportType)) {
			code = "na";
			display = "NA";
		}
		else {
			code = transportType.toLowerCase(Locale.ENGLISH);
			display = transportType;
		}
		addCodeableConceptExtension(encounter,
				"http://example.org/fhir/StructureDefinition/transport-type",
				"http://example.org/fhir/CodeSystem/transport-type",
				code,
				display);
	}

	/**
	 * Publishes which facility provides the ambulance so peers can match their FOSA id
	 * and create the ambulance bill when appropriate.
	 */
	private void addAmbulanceProviderFacilityExtension(ObjectNode encounter, Transfer transfer) {
		if (transfer == null || !"AMBULANCE".equals(StringUtils.trimToEmpty(transfer.getTransportType()))) {
			return;
		}
		String fosaId = StringUtils.trimToNull(transfer.getAmbulanceProviderFosaId());
		String name = StringUtils.trimToNull(transfer.getAmbulanceProviderName());
		if (fosaId == null && name == null) {
			return;
		}
		ObjectNode extension = addObjectNode(extensionsArray(encounter));
		extension.put("url", TransferAppConstants.EXT_AMBULANCE_PROVIDER_FACILITY);
		ArrayNode nested = extension.putArray("extension");
		addNestedExtensionField(nested, TransferAppConstants.EXT_AMBULANCE_PROVIDER_FOSA_ID, fosaId);
		addNestedExtensionField(nested, TransferAppConstants.EXT_AMBULANCE_PROVIDER_NAME, name);
	}

	private void addExternalFacilityExtension(ObjectNode encounter, boolean externalReceivingFacility) {
		if (!externalReceivingFacility) {
			return;
		}
		ObjectNode extension = addObjectNode(extensionsArray(encounter));
		extension.put("url", "http://example.org/fhir/StructureDefinition/requires-insurance-agent-verification");
		extension.put("valueBoolean", true);
	}

	private void addCaregiverExtension(ObjectNode encounter, Transfer transfer) {
		if (StringUtils.isBlank(transfer.getCaregiverName()) && StringUtils.isBlank(transfer.getCaregiverTelephone())) {
			return;
		}
		ObjectNode extension = addObjectNode(extensionsArray(encounter));
		extension.put("url", "http://example.org/fhir/StructureDefinition/caregiver-info");
		ArrayNode nested = extension.putArray("extension");
		if (StringUtils.isNotBlank(transfer.getCaregiverName())) {
			ObjectNode nameExtension = addObjectNode(nested);
			nameExtension.put("url", "name");
			nameExtension.put("valueString", transfer.getCaregiverName().trim());
		}
		if (StringUtils.isNotBlank(transfer.getCaregiverTelephone())) {
			ObjectNode phoneExtension = addObjectNode(nested);
			phoneExtension.put("url", "phone");
			phoneExtension.put("valueString", transfer.getCaregiverTelephone().trim());
		}
	}

	/**
	 * Staff contacted at receiving facility, with optional nested calling-time.
	 * FHIR forbids combining value[x] with nested extensions on the same element.
	 */
	private void addReceivingClinicianContactExtension(ObjectNode encounter, Transfer transfer) {
		String name = StringUtils.trimToNull(transfer.getStaffContactedName());
		String phone = StringUtils.trimToNull(transfer.getStaffContactedPhone());
		Date callingAt = combineDateAndTime(transfer.getDecisionToTransferAt(), transfer.getCallingTime());
		if (name == null && phone == null && callingAt == null) {
			return;
		}

		ObjectNode extension = addObjectNode(extensionsArray(encounter));
		extension.put("url", "http://example.org/fhir/StructureDefinition/receiving-clinician-contact");
		ArrayNode nested = extension.putArray("extension");
		addNestedExtensionField(nested, "name", name);
		addNestedExtensionField(nested, "phone", phone);
		if (callingAt != null) {
			ObjectNode callingTimeExtension = addObjectNode(nested);
			callingTimeExtension.put("url", "calling-time");
			callingTimeExtension.put("valueDateTime", formatDateTime(callingAt));
		}
	}

	private void addPractitionerInfoExtension(ObjectNode encounter, Transfer transfer, User user) {
		String license = resolveReferringProviderLicense(user);
		String name = formatReferringProviderName(transfer, user);
		String qualification = StringUtils.trimToNull(transfer.getProviderQualification());
		String phone = StringUtils.trimToNull(transfer.getProviderPhone());
		if (StringUtils.isBlank(name) && qualification == null && phone == null && StringUtils.isBlank(license)) {
			return;
		}

		ObjectNode extension = addObjectNode(extensionsArray(encounter));
		extension.put("url", "http://example.org/fhir/StructureDefinition/practitioner-info");
		ArrayNode nested = extension.putArray("extension");
		addNestedExtensionField(nested, "name", name);
		addNestedExtensionField(nested, "qualification", qualification);
		addNestedExtensionField(nested, "phone", phone);
		addNestedExtensionField(nested, "license-number", license);
	}

	private String formatReferringProviderName(Transfer transfer, User user) {
		String rawName = blankToDefault(transfer.getReferringProviderName(), resolveUserDisplayName(user));
		return TransferProfile.formatCareProviderName(rawName, resolveReferringProviderLicense(user));
	}

	private String resolveReferringProviderLicense(User user) {
		TransferProfileService profileService = getTransferProfileService();
		if (profileService == null || user == null) {
			return null;
		}
		try {
			TransferProfile profile = profileService.getProfileForUser(user);
			return profile != null ? StringUtils.trimToNull(profile.getLicenseNumber()) : null;
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
			transferProfileService = Context.getRegisteredComponent("transferProfileService", TransferProfileService.class);
			return transferProfileService;
		}
		catch (Exception ignored) {
			return null;
		}
	}

	private void addPatientDemographicsExtension(ObjectNode encounter, Transfer transfer) {
		String age = parseAge(transfer.getAgeOrDob());
		String gender = formatDemographicsGender(transfer.getSex());
		if (StringUtils.isBlank(transfer.getClientName())
				&& StringUtils.isBlank(age)
				&& StringUtils.isBlank(gender)
				&& StringUtils.isBlank(transfer.getClientTelephone())
				&& StringUtils.isBlank(transfer.getEmrId())) {
			return;
		}

		ObjectNode extension = addObjectNode(extensionsArray(encounter));
		extension.put("url", "http://example.org/fhir/StructureDefinition/patient-demographics");
		ArrayNode nested = extension.putArray("extension");
		addNestedExtensionField(nested, "name", transfer.getClientName());
		addNestedExtensionField(nested, "dob", null);
		addNestedExtensionField(nested, "gender", gender);
		addNestedExtensionField(nested, "age", age);
		addNestedExtensionField(nested, "phone", StringUtils.isNotBlank(transfer.getClientTelephone())
				? transfer.getClientTelephone()
				: "N/A");
		addNestedExtensionField(nested, "serial-number", transfer.getEmrId());
	}

	private void addPatientAddressExtension(ObjectNode encounter, Transfer transfer) {
		if (StringUtils.isBlank(transfer.getClientDistrict())
				&& StringUtils.isBlank(transfer.getSector())
				&& StringUtils.isBlank(transfer.getCell())
				&& StringUtils.isBlank(transfer.getVillage())) {
			return;
		}
		ObjectNode extension = addObjectNode(extensionsArray(encounter));
		extension.put("url", "http://example.org/fhir/StructureDefinition/patient-address");
		ArrayNode nested = extension.putArray("extension");
		addNestedExtensionField(nested, "district", transfer.getClientDistrict());
		addNestedExtensionField(nested, "sector", transfer.getSector());
		addNestedExtensionField(nested, "cell", transfer.getCell());
		addNestedExtensionField(nested, "village", transfer.getVillage());
	}

	private void addNestedExtensionField(ArrayNode nested, String url, String value) {
		if (StringUtils.isBlank(value)) {
			return;
		}
		ObjectNode nestedExtension = addObjectNode(nested);
		nestedExtension.put("url", url);
		nestedExtension.put("valueString", value.trim());
	}

	private ArrayNode extensionsArray(ObjectNode encounter) {
		if (encounter.get("extension") instanceof ArrayNode) {
			return (ArrayNode) encounter.get("extension");
		}
		return encounter.putArray("extension");
	}

	private void addStringExtension(ObjectNode encounter, String url, String value) {
		if (StringUtils.isBlank(value)) {
			return;
		}
		ObjectNode extension = addObjectNode(extensionsArray(encounter));
		extension.put("url", url);
		extension.put("valueString", value.trim());
	}

	private void addDateTimeExtension(ObjectNode encounter, String url, Date dateTime) {
		if (dateTime == null) {
			return;
		}
		ObjectNode extension = addObjectNode(extensionsArray(encounter));
		extension.put("url", url);
		extension.put("valueDateTime", formatDateTime(dateTime));
	}

	private static String requireUpi(Transfer transfer) {
		if (StringUtils.isBlank(transfer.getEmrId())) {
			throw new HieApiException("Cannot submit transfer: patient UPI (EMR ID) is missing.");
		}
		return transfer.getEmrId().trim();
	}

	private static String resolvePractitionerId(User user) {
		if (user != null && user.getUserId() != null) {
			return "transferapp-user-" + user.getUserId();
		}
		return "transferapp-user-unknown";
	}

	private static String resolveUserDisplayName(User user) {
		if (user != null && user.getPerson() != null && user.getPerson().getPersonName() != null) {
			return user.getPerson().getPersonName().getFullName();
		}
		return "Referring provider";
	}

	private static String formatVitals(Transfer transfer) {
		StringBuilder builder = new StringBuilder();
		appendVital(builder, "T", transfer.getVitalTemp());
		appendVital(builder, "SpO2", transfer.getVitalSpo2());
		appendVital(builder, "RR", transfer.getVitalRr());
		appendVital(builder, "Pulse", transfer.getVitalPulse());
		appendVital(builder, "BP", transfer.getVitalBp());
		appendVital(builder, "Weight", transfer.getVitalWt());
		appendVital(builder, "Height", transfer.getVitalHt());
		appendVital(builder, "MUAC", transfer.getVitalMuac());
		return builder.length() > 0 ? builder.toString() : null;
	}

	private static void appendVital(StringBuilder builder, String label, String value) {
		if (StringUtils.isBlank(value)) {
			return;
		}
		if (builder.length() > 0) {
			builder.append(", ");
		}
		builder.append(label).append(": ").append(value.trim());
	}

	private static String formatDateTime(Date date) {
		if (date == null) {
			return null;
		}
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
		formatter.setTimeZone(RWANDA);
		return formatter.format(date);
	}

	private static Date combineDateAndTime(Date baseDate, String timeValue) {
		if (StringUtils.isBlank(timeValue)) {
			return null;
		}
		Calendar calendar = Calendar.getInstance(RWANDA);
		if (baseDate != null) {
			calendar.setTime(baseDate);
		}
		String[] parts = timeValue.trim().split(":");
		if (parts.length >= 2) {
			try {
				calendar.set(Calendar.SECOND, 0);
				calendar.set(Calendar.MILLISECOND, 0);
				calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(parts[0]));
				calendar.set(Calendar.MINUTE, Integer.parseInt(parts[1]));
				return calendar.getTime();
			}
			catch (NumberFormatException ignored) {
				return null;
			}
		}
		return null;
	}

	private static String parseAge(String ageOrDob) {
		if (StringUtils.isBlank(ageOrDob)) {
			return null;
		}
		Matcher matcher = AGE_ONLY.matcher(ageOrDob.trim());
		if (matcher.matches()) {
			return matcher.group(1);
		}
		return ageOrDob.trim();
	}

	private static String formatDemographicsGender(String sex) {
		if (StringUtils.isBlank(sex)) {
			return null;
		}
		if ("MALE".equalsIgnoreCase(sex.trim()) || "M".equalsIgnoreCase(sex.trim())) {
			return "Male";
		}
		if ("FEMALE".equalsIgnoreCase(sex.trim()) || "F".equalsIgnoreCase(sex.trim())) {
			return "Female";
		}
		return sex.trim();
	}

	private static String transferTypeLabel(String transferType) {
		if ("EMERGENCY".equals(transferType)) {
			return "Emergency";
		}
		if ("NOT_EMERGENCY".equals(transferType)) {
			return "Not-Emergency";
		}
		if ("FOLLOW_UP".equals(transferType)) {
			return "Follow up";
		}
		return transferType;
	}

	private static String blankToDefault(String value, String defaultValue) {
		return StringUtils.isNotBlank(value) ? value.trim() : defaultValue;
	}

}
