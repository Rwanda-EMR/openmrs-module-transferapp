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
import org.openmrs.module.transferapp.api.TransferPatientSnapshotResolver;
import org.openmrs.module.transferapp.model.NeonatalTransfer;
import org.openmrs.module.transferapp.model.TransferFormKind;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;

/**
 * Builds the same generic "transfer referral" FHIR Encounter shape as {@link TransferEncounterPayloadBuilder}
 * (External Transfer), adapted to {@link NeonatalTransfer}'s field set. The Encounter envelope
 * (class/period/hospitalization/diagnosis/location) and the free-text extension pattern are
 * form-agnostic by design (see the existing transfer-form-kind extension), so this mirrors that
 * shape rather than inventing a new one.
 */
public class NeonatalTransferEncounterPayloadBuilder {

	private static final TimeZone RWANDA = TimeZone.getTimeZone("Africa/Kigali");

	private static final String SNOMED_SYSTEM = "http://snomed.info/sct";

	private static final String SNOMED_UNKNOWN_REASON_CODE = "261665006";

	private static final String SNOMED_UNKNOWN_REASON_DISPLAY = "Unknown (qualifier)";

	private final ObjectMapper objectMapper = new ObjectMapper();

	private TransferAdminService transferAdminService;

	private TransferPatientSnapshotResolver patientSnapshotResolver = new TransferPatientSnapshotResolver();

	public void setTransferAdminService(TransferAdminService transferAdminService) {
		this.transferAdminService = transferAdminService;
	}

	public void setPatientSnapshotResolver(TransferPatientSnapshotResolver patientSnapshotResolver) {
		this.patientSnapshotResolver = patientSnapshotResolver != null
				? patientSnapshotResolver
				: new TransferPatientSnapshotResolver();
	}

	public String buildEncounterJson(NeonatalTransfer transfer, User user, String receivingFacilityLabel) {
		return buildEncounterJson(transfer, user, receivingFacilityLabel, null);
	}

	/**
	 * @param forcedEncounterId when set, reused as Encounter.id so HIE updates the same resource
	 */
	public String buildEncounterJson(NeonatalTransfer transfer, User user, String receivingFacilityLabel,
			String forcedEncounterId) {
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
			addExtensions(encounter, transfer, user, receivingFacilityLabel);
			addClass(encounter, transfer.getTransferType());
			addType(encounter);
			addServiceType(encounter, transfer.getReceivingService());
			addSubject(encounter, upi, transfer.getBabyName());
			addParticipant(encounter, transfer);

			Date periodStart = transfer.getDecisionToTransferAt();
			Date periodEnd = periodStart;
			addPeriod(encounter, periodStart, periodEnd);
			addReasonCode(encounter, transfer.getReasonForTransfer());
			addDiagnosis(encounter, transfer);
			addHospitalization(encounter, transfer, receivingFacilityLabel);
			addLocation(encounter, transfer, periodStart, periodEnd);

			return objectMapper.writeValueAsString(encounter);
		}
		catch (HieApiException ex) {
			throw ex;
		}
		catch (Exception ex) {
			throw new HieApiException("Failed to build neonatal transfer encounter payload", ex);
		}
	}

	private void addMeta(ObjectNode encounter) {
		ObjectNode meta = encounter.putObject("meta");
		ObjectNode tag = addObjectNode(meta.putArray("tag"));
		tag.put("system", "http://fhir.openmrs.org/ext/encounter-tag");
		tag.put("code", "encounter");
		tag.put("display", "Encounter");
	}

	private void addExtensions(ObjectNode encounter, NeonatalTransfer transfer, User user,
			String receivingFacilityLabel) {
		addTransferFormKindExtension(encounter);
		addTransferTypeExtension(encounter, transfer.getTransferType());
		addDateTimeExtension(encounter, "http://example.org/fhir/StructureDefinition/decision-to-transfer-datetime",
				transfer.getDecisionToTransferAt());
		addDateTimeExtension(encounter, "http://example.org/fhir/StructureDefinition/calling-time",
				combineDateAndTime(transfer.getDecisionToTransferAt(), transfer.getCallingTime()));
		addStringExtension(encounter, "http://example.org/fhir/StructureDefinition/referring-department",
				transfer.getReferringUnit());
		addReceivingClinicianContactExtension(encounter, transfer);
		addTransportExtension(encounter, transfer.getModeOfTransport());
		addPractitionerInfoExtension(encounter, transfer, user);
		addCaregiverExtension(encounter, transfer);
		addPatientDemographicsExtension(encounter, transfer);
		addStringExtension(encounter, "http://example.org/fhir/StructureDefinition/clinical-presentation",
				transfer.getChiefComplaintDetails());
		addStringExtension(encounter, "http://example.org/fhir/StructureDefinition/vital-signs", formatVitals(transfer));
		addStringExtension(encounter, "http://example.org/fhir/StructureDefinition/lab-results", formatLabs(transfer));
		addStringExtension(encounter, "http://example.org/fhir/StructureDefinition/procedures-treatments",
				formatProceduresAndTreatments(transfer));
		addStringExtension(encounter, "http://example.org/fhir/StructureDefinition/others-notes",
				transfer.getClinicalManagementSummary());
		addStringExtension(encounter, "http://example.org/fhir/StructureDefinition/neonatal-clinical-summary",
				formatNeonatalClinicalSummary(transfer));
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
		typeEntry.put("text", "Neonatal Transfer");
	}

	private void addServiceType(ObjectNode encounter, String receivingService) {
		ObjectNode serviceType = encounter.putObject("serviceType");
		ObjectNode coding = addObjectNode(serviceType.putArray("coding"));
		coding.put("system", "http://terminology.hl7.org/CodeSystem/service-type");
		coding.put("code", "253");
		coding.put("display", StringUtils.isNotBlank(receivingService) ? receivingService.trim() : "");
	}

	private void addSubject(ObjectNode encounter, String upi, String babyName) {
		ObjectNode subject = encounter.putObject("subject");
		subject.put("reference", "Patient/" + upi);
		subject.put("type", "Patient");
		ObjectNode subjectIdentifier = subject.putObject("identifier");
		ObjectNode identifierType = subjectIdentifier.putObject("type");
		ObjectNode upiCoding = addObjectNode(identifierType.putArray("coding"));
		upiCoding.put("code", "UPI");
		upiCoding.put("display", "UPI");
		subjectIdentifier.put("value", upi);
		subject.put("display", blankToDefault(babyName, "Patient"));
	}

	private void addParticipant(ObjectNode encounter, NeonatalTransfer transfer) {
		String practitionerId = "transferapp-neonatal-transfer-" + blankToDefault(transfer.getUuid(), "unknown");
		String displayName = blankToDefault(transfer.getReferringProviderName(), "Referring provider");

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

	private void addDiagnosis(ObjectNode encounter, NeonatalTransfer transfer) {
		String display = joinNonBlank(" / ", transfer.getDiagnosis1(), transfer.getDiagnosis2(),
				transfer.getDiagnosis3(), transfer.getDiagnosis4());
		String conditionRef = transfer.getNeonatalTransferId() != null
				? "Condition/neonatal-transfer-diagnosis-" + transfer.getNeonatalTransferId()
				: "Condition/neonatal-transfer-diagnosis";

		ObjectNode diagnosis = addObjectNode(encounter.putArray("diagnosis"));
		ObjectNode condition = diagnosis.putObject("condition");
		condition.put("reference", conditionRef);
		condition.put("display", display != null ? display : "");
		ObjectNode use = diagnosis.putObject("use");
		ObjectNode useCoding = addObjectNode(use.putArray("coding"));
		useCoding.put("system", "http://terminology.hl7.org/CodeSystem/diagnosis-role");
		useCoding.put("code", "AD");
		useCoding.put("display", "Admission diagnosis");
	}

	private void addHospitalization(ObjectNode encounter, NeonatalTransfer transfer, String receivingFacilityLabel) {
		ObjectNode hospitalization = encounter.putObject("hospitalization");

		ObjectNode origin = hospitalization.putObject("origin");
		populateLocationReference(origin, resolveSendingFosaId(), transfer.getSendingFacility(), "Referring facility");

		ObjectNode admitSource = hospitalization.putObject("admitSource");
		ObjectNode admitCoding = addObjectNode(admitSource.putArray("coding"));
		admitCoding.put("system", "http://terminology.hl7.org/CodeSystem/admit-source");
		admitCoding.put("code", "hosp-trans");
		admitCoding.put("display", blankToDefault(transfer.getReferringUnit(), "Hospital Transfer"));

		ObjectNode destination = hospitalization.putObject("destination");
		populateLocationReference(destination, transfer.getReceivingFacilityCode(), receivingFacilityLabel,
				"Receiving facility");

		ObjectNode dischargeDisposition = hospitalization.putObject("dischargeDisposition");
		ObjectNode dischargeCoding = addObjectNode(dischargeDisposition.putArray("coding"));
		dischargeCoding.put("system", "http://terminology.hl7.org/CodeSystem/discharge-disposition");
		dischargeCoding.put("code", "hosp");
		dischargeCoding.put("display", blankToDefault(transfer.getReceivingService(), "Hospital"));
	}

	private void addLocation(ObjectNode encounter, NeonatalTransfer transfer, Date periodStart, Date periodEnd) {
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

	private ObjectNode addObjectNode(ArrayNode arrayNode) {
		return (ObjectNode) arrayNode.addObject();
	}

	private void addCodeableConceptExtension(ObjectNode encounter, String url, String system, String code,
			String display) {
		ObjectNode extension = addObjectNode(extensionsArray(encounter));
		extension.put("url", url);
		ObjectNode value = extension.putObject("valueCodeableConcept");
		ObjectNode coding = addObjectNode(value.putArray("coding"));
		coding.put("system", system);
		coding.put("code", code);
		coding.put("display", display);
	}

	private void populateLocationReference(ObjectNode node, String facilityCode, String displayName,
			String fallbackDisplay) {
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

	private void addTransferFormKindExtension(ObjectNode encounter) {
		addCodeableConceptExtension(encounter,
				TransferFormKind.EXTENSION_URL,
				TransferFormKind.CODE_SYSTEM,
				TransferFormKind.NEONATAL.getCode(),
				TransferFormKind.NEONATAL.getDisplay());
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

	private void addTransportExtension(ObjectNode encounter, String modeOfTransport) {
		if (StringUtils.isBlank(modeOfTransport)) {
			return;
		}
		addCodeableConceptExtension(encounter,
				"http://example.org/fhir/StructureDefinition/transport-type",
				"http://example.org/fhir/CodeSystem/transport-type",
				modeOfTransport.trim().toLowerCase(Locale.ENGLISH).replace(' ', '-'),
				modeOfTransport.trim());
	}

	private void addCaregiverExtension(ObjectNode encounter, NeonatalTransfer transfer) {
		if (StringUtils.isBlank(transfer.getMotherName()) && StringUtils.isBlank(transfer.getMotherCaregiverPhone())) {
			return;
		}
		ObjectNode extension = addObjectNode(extensionsArray(encounter));
		extension.put("url", "http://example.org/fhir/StructureDefinition/caregiver-info");
		ArrayNode nested = extension.putArray("extension");
		addNestedExtensionField(nested, "name", transfer.getMotherName());
		addNestedExtensionField(nested, "phone", transfer.getMotherCaregiverPhone());
	}

	private void addReceivingClinicianContactExtension(ObjectNode encounter, NeonatalTransfer transfer) {
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

	private void addPractitionerInfoExtension(ObjectNode encounter, NeonatalTransfer transfer, User user) {
		String name = blankToDefault(transfer.getReferringProviderName(), resolveUserDisplayName(user));
		String qualification = StringUtils.trimToNull(transfer.getReferringProviderQualification());
		String phone = StringUtils.trimToNull(transfer.getReferringProviderPhone());
		if (StringUtils.isBlank(name) && qualification == null && phone == null) {
			return;
		}

		ObjectNode extension = addObjectNode(extensionsArray(encounter));
		extension.put("url", "http://example.org/fhir/StructureDefinition/practitioner-info");
		ArrayNode nested = extension.putArray("extension");
		addNestedExtensionField(nested, "name", name);
		addNestedExtensionField(nested, "qualification", qualification);
		addNestedExtensionField(nested, "phone", phone);
	}

	private void addPatientDemographicsExtension(ObjectNode encounter, NeonatalTransfer transfer) {
		if (StringUtils.isBlank(transfer.getBabyName())
				&& StringUtils.isBlank(transfer.getSex())
				&& StringUtils.isBlank(transfer.getCurrentAgeDays())) {
			return;
		}
		ObjectNode extension = addObjectNode(extensionsArray(encounter));
		extension.put("url", "http://example.org/fhir/StructureDefinition/patient-demographics");
		ArrayNode nested = extension.putArray("extension");
		addNestedExtensionField(nested, "name", transfer.getBabyName());
		addNestedExtensionField(nested, "gender", formatDemographicsGender(transfer.getSex()));
		addNestedExtensionField(nested, "age-days", transfer.getCurrentAgeDays());
		addNestedExtensionField(nested, "dob", formatDateOnly(transfer.getDob()));
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

	private String requireUpi(NeonatalTransfer transfer) {
		String upi = patientSnapshotResolver.resolveUpid(transfer != null ? transfer.getPatient() : null);
		if (StringUtils.isBlank(upi)) {
			throw new HieApiException("Cannot submit neonatal transfer: patient UPID is missing.");
		}
		return upi.trim();
	}

	private static String resolveUserDisplayName(User user) {
		if (user != null && user.getPerson() != null && user.getPerson().getPersonName() != null) {
			return user.getPerson().getPersonName().getFullName();
		}
		return "Referring provider";
	}

	private static String formatVitals(NeonatalTransfer transfer) {
		StringBuilder builder = new StringBuilder();
		appendVital(builder, "SpO2 pre", transfer.getSpo2Preductal());
		appendVital(builder, "SpO2 post", transfer.getSpo2Postductal());
		appendVital(builder, "T", transfer.getConditionTemp());
		appendVital(builder, "HR", transfer.getConditionHr());
		appendVital(builder, "RR", transfer.getConditionRr());
		appendVital(builder, "BP", transfer.getConditionBp());
		return builder.length() > 0 ? builder.toString() : null;
	}

	private static String formatLabs(NeonatalTransfer transfer) {
		StringBuilder builder = new StringBuilder();
		appendVital(builder, "Glucose", transfer.getLabGlucose());
		appendVital(builder, "FBC done", transfer.getFbcDone());
		appendVital(builder, "Hb", transfer.getLabHb());
		appendVital(builder, "WBC", transfer.getLabWbc());
		appendVital(builder, "Platelets", transfer.getLabPlatelets());
		appendVital(builder, "CRP", transfer.getLabCrp());
		appendVital(builder, "Bili total", transfer.getLabBiliTotal());
		appendVital(builder, "Bili direct", transfer.getLabBiliDirect());
		appendVital(builder, "U&E", transfer.getLabUe());
		appendVital(builder, "Cultures", transfer.getLabCultures());
		appendVital(builder, "Imaging available", transfer.getImagingResultsAvailable());
		appendVital(builder, "Imaging results", transfer.getImagingResults());
		return builder.length() > 0 ? builder.toString() : null;
	}

	private static String formatProceduresAndTreatments(NeonatalTransfer transfer) {
		StringBuilder builder = new StringBuilder();
		appendVital(builder, "Respiratory support", transfer.getRespiratorySupport());
		appendVital(builder, "Ventilation settings", transfer.getVentilationSettings());
		appendVital(builder, "Blood gas analysis", transfer.getBloodGasAnalysis());
		appendVital(builder, "Inotropes", transfer.getInotropes());
		appendVital(builder, "Inotropes specify", transfer.getInotropesSpecify());
		appendVital(builder, "Antibiotic 1", joinNonBlank(" ", transfer.getAntibiotic1Name(),
				transfer.getAntibiotic1Doses(), transfer.getAntibiotic1Durations()));
		appendVital(builder, "Antibiotic 2", joinNonBlank(" ", transfer.getAntibiotic2Name(),
				transfer.getAntibiotic2Doses(), transfer.getAntibiotic2Durations()));
		appendVital(builder, "ARVs", transfer.getArvs());
		appendVital(builder, "Feed type", transfer.getFeedType());
		return builder.length() > 0 ? builder.toString() : null;
	}

	private static String formatNeonatalClinicalSummary(NeonatalTransfer transfer) {
		StringBuilder builder = new StringBuilder();
		appendVital(builder, "Gestational age (wks)", transfer.getGestationalAgeWeeks());
		appendVital(builder, "Birth weight (g)", transfer.getBirthWeightG());
		appendVital(builder, "Current weight (g)", transfer.getCurrentWeightG());
		appendVital(builder, "APGAR 1/5/10", joinNonBlank("/", transfer.getApgar1min(), transfer.getApgar5min(),
				transfer.getApgar10min()));
		appendVital(builder, "Resuscitation at birth", transfer.getResuscitationAtBirth());
		appendVital(builder, "Resuscitation methods", transfer.getResuscitationMethods());
		appendVital(builder, "HIE grade", transfer.getHieGrade());
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

	private static String joinNonBlank(String separator, String... values) {
		StringBuilder builder = new StringBuilder();
		for (String value : values) {
			if (StringUtils.isBlank(value)) {
				continue;
			}
			if (builder.length() > 0) {
				builder.append(separator);
			}
			builder.append(value.trim());
		}
		return builder.length() > 0 ? builder.toString() : null;
	}

	private static String formatDateTime(Date date) {
		if (date == null) {
			return null;
		}
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
		formatter.setTimeZone(RWANDA);
		return formatter.format(date);
	}

	private static String formatDateOnly(Date date) {
		if (date == null) {
			return null;
		}
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
		formatter.setTimeZone(RWANDA);
		return formatter.format(date);
	}

	private static Date combineDateAndTime(Date baseDate, String timeValue) {
		if (StringUtils.isBlank(timeValue)) {
			return null;
		}
		java.util.Calendar calendar = java.util.Calendar.getInstance(RWANDA);
		if (baseDate != null) {
			calendar.setTime(baseDate);
		}
		String[] parts = timeValue.trim().split(":");
		if (parts.length >= 2) {
			try {
				calendar.set(java.util.Calendar.SECOND, 0);
				calendar.set(java.util.Calendar.MILLISECOND, 0);
				calendar.set(java.util.Calendar.HOUR_OF_DAY, Integer.parseInt(parts[0]));
				calendar.set(java.util.Calendar.MINUTE, Integer.parseInt(parts[1]));
				return calendar.getTime();
			}
			catch (NumberFormatException ignored) {
				return null;
			}
		}
		return null;
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
