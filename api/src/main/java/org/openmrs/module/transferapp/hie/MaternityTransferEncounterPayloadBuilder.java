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
import org.openmrs.module.transferapp.model.MaternityTransfer;
import org.openmrs.module.transferapp.model.MaternityTransferTreatment;
import org.openmrs.module.transferapp.model.TransferFormKind;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;

/**
 * Builds the same generic "transfer referral" FHIR Encounter shape as {@link TransferEncounterPayloadBuilder}
 * (External Transfer) and {@link NeonatalTransferEncounterPayloadBuilder}, adapted to
 * {@link MaternityTransfer}'s field set.
 */
public class MaternityTransferEncounterPayloadBuilder {

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

	public String buildEncounterJson(MaternityTransfer transfer, User user, String receivingFacilityLabel) {
		return buildEncounterJson(transfer, user, receivingFacilityLabel, null);
	}

	/**
	 * @param forcedEncounterId when set, reused as Encounter.id so HIE updates the same resource
	 */
	public String buildEncounterJson(MaternityTransfer transfer, User user, String receivingFacilityLabel,
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
			addExtensions(encounter, transfer, user);
			addClass(encounter, transfer.getTransferType());
			addType(encounter);
			addServiceType(encounter, transfer.getReceivingService());
			addSubject(encounter, upi, transfer.getClientName());
			addParticipant(encounter, transfer);

			Date periodStart = transfer.getAdmissionAt() != null ? transfer.getAdmissionAt() : transfer.getDecisionToTransferAt();
			Date periodEnd = transfer.getDecisionToTransferAt() != null ? transfer.getDecisionToTransferAt() : periodStart;
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
			throw new HieApiException("Failed to build maternity transfer encounter payload", ex);
		}
	}

	private void addMeta(ObjectNode encounter) {
		ObjectNode meta = encounter.putObject("meta");
		ObjectNode tag = addObjectNode(meta.putArray("tag"));
		tag.put("system", "http://fhir.openmrs.org/ext/encounter-tag");
		tag.put("code", "encounter");
		tag.put("display", "Encounter");
	}

	private void addExtensions(ObjectNode encounter, MaternityTransfer transfer, User user) {
		addTransferFormKindExtension(encounter);
		addTransferTypeExtension(encounter, transfer.getTransferType());
		addDateTimeExtension(encounter, "http://example.org/fhir/StructureDefinition/admission-datetime",
				transfer.getAdmissionAt());
		addDateTimeExtension(encounter, "http://example.org/fhir/StructureDefinition/decision-to-transfer-datetime",
				transfer.getDecisionToTransferAt());
		addDateTimeExtension(encounter, "http://example.org/fhir/StructureDefinition/calling-time",
				combineDateAndTime(transfer.getDecisionToTransferAt(), transfer.getCallingTime()));
		addDateTimeExtension(encounter, "http://example.org/fhir/StructureDefinition/ambulance-call-time",
				combineDateAndTime(transfer.getDecisionToTransferAt(), transfer.getAmbulanceCalledTime()));
		addDateTimeExtension(encounter, "http://example.org/fhir/StructureDefinition/departure-time",
				combineDateAndTime(transfer.getDecisionToTransferAt(), transfer.getDepartureFromReferringTime()));
		addStringExtension(encounter, "http://example.org/fhir/StructureDefinition/referring-department",
				transfer.getReferringUnit());
		addReceivingClinicianContactExtension(encounter, transfer);
		addInsuranceExtension(encounter, transfer.getHealthInsuranceType());
		addTransportExtension(encounter, transfer.getTransportType());
		addPractitionerInfoExtension(encounter, transfer, user);
		addCaregiverExtension(encounter, transfer);
		addPatientDemographicsExtension(encounter, transfer);
		addPatientAddressExtension(encounter, transfer);
		addStringExtension(encounter, "http://example.org/fhir/StructureDefinition/clinical-presentation",
				transfer.getClinicalPresentation());
		addStringExtension(encounter, "http://example.org/fhir/StructureDefinition/vital-signs", formatVitals(transfer));
		addStringExtension(encounter, "http://example.org/fhir/StructureDefinition/lab-results", formatLabs(transfer));
		addStringExtension(encounter, "http://example.org/fhir/StructureDefinition/procedures-treatments",
				formatProceduresAndTreatments(transfer));
		addStringExtension(encounter, "http://example.org/fhir/StructureDefinition/others-notes",
				transfer.getCurrentPregnancyComplications());
		addStringExtension(encounter, "http://example.org/fhir/StructureDefinition/obstetric-history",
				formatObstetricHistory(transfer));
		addStringExtension(encounter, "http://example.org/fhir/StructureDefinition/abdominal-exam",
				formatAbdominalExam(transfer));
		addStringExtension(encounter, "http://example.org/fhir/StructureDefinition/vaginal-exam",
				formatVaginalExam(transfer));
		if (StringUtils.isNotBlank(transfer.getDisabilityType())) {
			addStringExtension(encounter, "http://example.org/fhir/StructureDefinition/disability-type",
					transfer.getDisabilityType());
		}
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
		typeEntry.put("text", "Maternity Transfer");
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

	private void addParticipant(ObjectNode encounter, MaternityTransfer transfer) {
		String practitionerId = "transferapp-maternity-transfer-" + blankToDefault(transfer.getUuid(), "unknown");
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

	private void addDiagnosis(ObjectNode encounter, MaternityTransfer transfer) {
		String conditionRef = transfer.getMaternityTransferId() != null
				? "Condition/maternity-transfer-diagnosis-" + transfer.getMaternityTransferId()
				: "Condition/maternity-transfer-diagnosis";
		String display = StringUtils.isNotBlank(transfer.getDiagnosis()) ? transfer.getDiagnosis().trim() : "";

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

	private void addHospitalization(ObjectNode encounter, MaternityTransfer transfer, String receivingFacilityLabel) {
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

	private void addLocation(ObjectNode encounter, MaternityTransfer transfer, Date periodStart, Date periodEnd) {
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
				TransferFormKind.MATERNITY.getCode(),
				TransferFormKind.MATERNITY.getDisplay());
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
		else if ("PRIVATE".equals(transportType)) {
			code = "private";
			display = "Private";
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

	private void addCaregiverExtension(ObjectNode encounter, MaternityTransfer transfer) {
		if (StringUtils.isBlank(transfer.getNextOfKinName()) && StringUtils.isBlank(transfer.getNextOfKinTelephone())) {
			return;
		}
		ObjectNode extension = addObjectNode(extensionsArray(encounter));
		extension.put("url", "http://example.org/fhir/StructureDefinition/caregiver-info");
		ArrayNode nested = extension.putArray("extension");
		addNestedExtensionField(nested, "name", transfer.getNextOfKinName());
		addNestedExtensionField(nested, "phone", transfer.getNextOfKinTelephone());
	}

	private void addReceivingClinicianContactExtension(ObjectNode encounter, MaternityTransfer transfer) {
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

	private void addPractitionerInfoExtension(ObjectNode encounter, MaternityTransfer transfer, User user) {
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

	private void addPatientDemographicsExtension(ObjectNode encounter, MaternityTransfer transfer) {
		if (StringUtils.isBlank(transfer.getClientName())
				&& StringUtils.isBlank(transfer.getAgeOrDob())
				&& StringUtils.isBlank(transfer.getSerialNumberEmr())) {
			return;
		}
		ObjectNode extension = addObjectNode(extensionsArray(encounter));
		extension.put("url", "http://example.org/fhir/StructureDefinition/patient-demographics");
		ArrayNode nested = extension.putArray("extension");
		addNestedExtensionField(nested, "name", transfer.getClientName());
		addNestedExtensionField(nested, "age", transfer.getAgeOrDob());
		addNestedExtensionField(nested, "serial-number", transfer.getSerialNumberEmr());
	}

	private void addPatientAddressExtension(ObjectNode encounter, MaternityTransfer transfer) {
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

	private String requireUpi(MaternityTransfer transfer) {
		String upi = patientSnapshotResolver.resolveUpid(transfer != null ? transfer.getPatient() : null);
		if (StringUtils.isBlank(upi)) {
			throw new HieApiException("Cannot submit maternity transfer: patient UPID is missing.");
		}
		return upi.trim();
	}

	private static String resolveUserDisplayName(User user) {
		if (user != null && user.getPerson() != null && user.getPerson().getPersonName() != null) {
			return user.getPerson().getPersonName().getFullName();
		}
		return "Referring provider";
	}

	private static String formatVitals(MaternityTransfer transfer) {
		StringBuilder builder = new StringBuilder();
		appendPart(builder, "BP", transfer.getVitalBp());
		appendPart(builder, "T", transfer.getVitalTemp());
		appendPart(builder, "SpO2", transfer.getVitalSpo2());
		appendPart(builder, "RR", transfer.getVitalRr());
		appendPart(builder, "Pulse", transfer.getVitalPulse());
		appendPart(builder, "Weight", transfer.getVitalWeight());
		appendPart(builder, "Height", transfer.getVitalHeight());
		return builder.length() > 0 ? builder.toString() : null;
	}

	private static String formatLabs(MaternityTransfer transfer) {
		StringBuilder builder = new StringBuilder();
		appendPart(builder, "Hemoglobin", transfer.getLatestHemoglobin());
		appendPart(builder, "HIV", transfer.getLatestHivStatus());
		appendPart(builder, "Blood group", transfer.getLatestBloodGroup());
		appendPart(builder, "Other results", transfer.getLatestOtherResults());
		appendPart(builder, "Hgb", transfer.getInvestigationHgb());
		appendPart(builder, "Urine test", transfer.getInvestigationUrineTest());
		appendPart(builder, "Other test", transfer.getInvestigationOtherTest());
		appendPart(builder, "Imaging investigations", transfer.getImagingInvestigations());
		return builder.length() > 0 ? builder.toString() : null;
	}

	private static String formatProceduresAndTreatments(MaternityTransfer transfer) {
		StringBuilder builder = new StringBuilder();
		appendPart(builder, "Procedures", transfer.getProcedures());
		appendPart(builder, "Lab tests attached", yesNo(transfer.getAttachedLabTests()));
		appendPart(builder, "Imaging attached", yesNo(transfer.getAttachedImaging()));
		appendPart(builder, "Other attachments", transfer.getAttachedOther());
		String treatments = formatTreatments(transfer.getTreatments());
		if (treatments != null) {
			appendPart(builder, "Treatments", treatments);
		}
		return builder.length() > 0 ? builder.toString() : null;
	}

	private static String formatTreatments(List<MaternityTransferTreatment> treatments) {
		if (treatments == null || treatments.isEmpty()) {
			return null;
		}
		StringBuilder builder = new StringBuilder();
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		dateFormat.setTimeZone(RWANDA);
		for (MaternityTransferTreatment treatment : treatments) {
			if (treatment == null || StringUtils.isBlank(treatment.getTreatmentName())) {
				continue;
			}
			if (builder.length() > 0) {
				builder.append("; ");
			}
			builder.append(treatment.getTreatmentName().trim());
			if (StringUtils.isNotBlank(treatment.getDose())) {
				builder.append(" dose ").append(treatment.getDose().trim());
			}
			if (treatment.getGivenDate() != null) {
				builder.append(" on ").append(dateFormat.format(treatment.getGivenDate()));
			}
			if (StringUtils.isNotBlank(treatment.getGivenTime())) {
				builder.append(" at ").append(treatment.getGivenTime().trim());
			}
		}
		return builder.length() > 0 ? builder.toString() : null;
	}

	private static String formatObstetricHistory(MaternityTransfer transfer) {
		StringBuilder builder = new StringBuilder();
		appendPart(builder, "Gravida", transfer.getObstetricGravida());
		appendPart(builder, "Parity", transfer.getObstetricParity());
		appendPart(builder, "Living children", transfer.getObstetricLivingChildren());
		appendPart(builder, "Abortion", transfer.getObstetricAbortion());
		appendPart(builder, "Stillbirth", transfer.getObstetricStillbirth());
		appendPart(builder, "Neonatal death", transfer.getObstetricNeonatalDeath());
		appendPart(builder, "Preterm birth", transfer.getObstetricPretermBirth());
		if (transfer.getLmpDate() != null) {
			appendPart(builder, "LMP", formatDateOnly(transfer.getLmpDate()));
		}
		if (transfer.getEddDate() != null) {
			appendPart(builder, "EDD", formatDateOnly(transfer.getEddDate()));
		}
		appendPart(builder, "Gestation age", transfer.getGestationAge());
		appendPart(builder, "MUAC", transfer.getMuac());
		appendPart(builder, "ANC completed", transfer.getAncCompletedCount());
		appendPart(builder, "Tetanus doses", transfer.getTetanusVaccineDoses());
		appendPart(builder, "Previous significant history", transfer.getPreviousSignificantHistory());
		appendPart(builder, "Multi pregnancies and known HIV", transfer.getMultiPregnanciesAndKnownHiv());
		return builder.length() > 0 ? builder.toString() : null;
	}

	private static String formatAbdominalExam(MaternityTransfer transfer) {
		StringBuilder builder = new StringBuilder();
		appendPart(builder, "Fetal presentation", transfer.getFetalPresentation());
		appendPart(builder, "Fundal height", transfer.getFundalHeight());
		appendPart(builder, "Fetal heart rate", transfer.getFetalHeartRate());
		appendPart(builder, "Contractions", transfer.getContractions());
		return builder.length() > 0 ? builder.toString() : null;
	}

	private static String formatVaginalExam(MaternityTransfer transfer) {
		StringBuilder builder = new StringBuilder();
		if (transfer.getVaginalExamAt() != null) {
			appendPart(builder, "Exam at", formatDateTime(transfer.getVaginalExamAt()));
		}
		appendPart(builder, "Dilation", transfer.getDilation());
		appendPart(builder, "Effacement", transfer.getEffacement());
		appendPart(builder, "Descent", transfer.getDescent());
		appendPart(builder, "Consistency", transfer.getConsistency());
		appendPart(builder, "Position", transfer.getPosition());
		appendPart(builder, "Caput", yesNo(transfer.getCaput()));
		appendPart(builder, "Moulding", yesNo(transfer.getMoulding()));
		appendPart(builder, "Membranes ruptured", yesNo(transfer.getMembranesRuptured()));
		if (transfer.getMembranesRupturedAt() != null) {
			appendPart(builder, "Membranes ruptured at", formatDateTime(transfer.getMembranesRupturedAt()));
		}
		appendPart(builder, "Amniotic fluid", transfer.getAmnioticFluidColor());
		appendPart(builder, "Estimated blood loss (mL)", transfer.getEstimatedBloodLossMl());
		return builder.length() > 0 ? builder.toString() : null;
	}

	private static String yesNo(Boolean value) {
		if (value == null) {
			return null;
		}
		return Boolean.TRUE.equals(value) ? "Yes" : "No";
	}

	private static void appendPart(StringBuilder builder, String label, String value) {
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
