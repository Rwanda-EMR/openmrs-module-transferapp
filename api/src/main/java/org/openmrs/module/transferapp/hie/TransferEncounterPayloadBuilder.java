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
import org.openmrs.module.transferapp.api.PatientInsuranceService;
import org.openmrs.module.transferapp.api.TransferPatientSnapshotResolver;
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

/**
 * Builds the External (General) Transfer Encounter payload to match
 * {@code devs/transfer.json}:
 * <ul>
 *   <li>{@code status=in-progress}, {@code class=IMP}</li>
 *   <li>{@code patient-phone}, {@code doctor-details}, {@code transfer-details}</li>
 *   <li>{@code transfer-type}: NORMAL_TRANSFER / REFERRAL / COUNTER_REFERRAL</li>
 *   <li>{@code transport} nested under transfer-details</li>
 *   <li>{@code type} (TRANSFER_ENCOUNTER), {@code serviceType} (HL7 253)</li>
 *   <li>{@code subject}, {@code participant} (ATND), {@code diagnosis},
 *       {@code hospitalization}, {@code location}, {@code serviceProvider}</li>
 * </ul>
 * Optional sections from the sample (referral-feedback, counter-referral) are omitted
 * when the form has no data for them. Referral linkage is included when a previous
 * HIE encounter id is known.
 */
public class TransferEncounterPayloadBuilder {

	private static final TimeZone RWANDA = TimeZone.getTimeZone("Africa/Kigali");

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

	private static final String INSURANCE_DETAILS_URL =
			"http://example.rw/fhir/StructureDefinition/insurance-details";

	private static final String INSURANCE_TYPE_SYSTEM =
			"http://example.rw/fhir/CodeSystem/insurance-type";

	private static final String INSURANCE_ELIGIBILITY_SYSTEM =
			"http://example.rw/fhir/CodeSystem/insurance-eligibility-status";

	private static final String SNOMED_SYSTEM = "http://snomed.info/sct";

	private static final String SNOMED_UNKNOWN_REASON_CODE = "261665006";

	private static final String SNOMED_UNKNOWN_REASON_DISPLAY = "Unknown (qualifier)";

	private static final String EXT_TRANSFER_TYPE =
			"http://example.org/fhir/StructureDefinition/transfer-type";

	private static final String EXT_TRANSFER_TYPE_SYSTEM =
			"http://example.org/fhir/CodeSystem/transfer-type";

	private static final String EXT_INSURANCE_TYPE =
			"http://example.org/fhir/StructureDefinition/insurance-type";

	private static final String EXT_INSURANCE_TYPE_SYSTEM =
			"http://example.org/fhir/CodeSystem/insurance-type";

	private static final String EXT_TRANSPORT_TYPE =
			"http://example.org/fhir/StructureDefinition/transport-type";

	private static final String EXT_TRANSPORT_TYPE_SYSTEM =
			"http://example.org/fhir/CodeSystem/transport-type";

	private final ObjectMapper objectMapper = new ObjectMapper();

	private TransferAdminService transferAdminService;

	private TransferProfileService transferProfileService;

	private PatientInsuranceService patientInsuranceService;

	private TransferPatientSnapshotResolver patientSnapshotResolver = new TransferPatientSnapshotResolver();

	public void setTransferAdminService(TransferAdminService transferAdminService) {
		this.transferAdminService = transferAdminService;
	}

	public void setTransferProfileService(TransferProfileService transferProfileService) {
		this.transferProfileService = transferProfileService;
	}

	public void setPatientInsuranceService(PatientInsuranceService patientInsuranceService) {
		this.patientInsuranceService = patientInsuranceService;
	}

	public void setPatientSnapshotResolver(TransferPatientSnapshotResolver patientSnapshotResolver) {
		this.patientSnapshotResolver = patientSnapshotResolver != null
				? patientSnapshotResolver
				: new TransferPatientSnapshotResolver();
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
	 * @param externalReceivingFacility when true, emits requires-insurance-agent-verification
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
			addMeta(encounter);

			addClass(encounter);
			addTopLevelExtensions(encounter, transfer, user, profile, license, upi, transferBusinessId,
					encounterId, externalReceivingFacility);
			addType(encounter);
			addServiceType(encounter, transfer.getReceivingService());
			addSubject(encounter, upi, transfer.getClientName());
			addParticipant(encounter, user, license, providerDisplay);

			Date periodStart = requireDecisionToTransferAt(transfer);
			Date periodEnd = plusOneMonth(periodStart);
			addPeriod(encounter, periodStart, periodEnd);
			addLength(encounter);
			addReasonCode(encounter, transfer.getReasonForTransfer());

			addDiagnosis(encounter, transfer, encounterId);
			addHospitalization(encounter, transfer, receivingFacilityLabel);
			addLocation(encounter, transfer, receivingFacilityLabel, periodStart, periodEnd);
			addServiceProvider(encounter, transfer, receivingFacilityLabel);
			addPartOfIfPresent(encounter, transfer, encounterId);

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

	/**
	 * Encounter.type from {@code transfer.json}: TRANSFER_ENCOUNTER / External Transfer.
	 */
	private void addType(ObjectNode encounter) {
		ObjectNode typeEntry = addObjectNode(encounter.putArray("type"));
		ObjectNode coding = addObjectNode(typeEntry.putArray("coding"));
		coding.put("code", "TRANSFER_ENCOUNTER");
		coding.put("display", "TRANSFER_ENCOUNTER");
		typeEntry.put("text", "External Transfer");
	}

	/**
	 * Encounter.serviceType from {@code transfer.json} (HL7 service-type 253).
	 * Display prefers the form's receiving service when present.
	 */
	private void addServiceType(ObjectNode encounter, String receivingService) {
		ObjectNode serviceType = encounter.putObject("serviceType");
		ObjectNode coding = addObjectNode(serviceType.putArray("coding"));
		coding.put("system", "http://terminology.hl7.org/CodeSystem/service-type");
		coding.put("code", "253");
		coding.put("display", StringUtils.isNotBlank(receivingService)
				? receivingService.trim()
				: "General medical practice");
	}

	private void addTopLevelExtensions(ObjectNode encounter, Transfer transfer, User user,
			TransferProfile profile, String license, String upi, String transferBusinessId,
			String encounterId, boolean externalReceivingFacility) {
		ArrayNode extensions = encounter.putArray("extension");

		String patientPhone = firstNonBlank(transfer.getClientTelephone(), transfer.getCaregiverTelephone());
		if (StringUtils.isNotBlank(patientPhone)) {
			ObjectNode phoneExt = addObjectNode(extensions);
			phoneExt.put("url", "patient-phone");
			phoneExt.put("valueString", patientPhone.trim());
		}

		addDoctorDetailsExtension(extensions, transfer, profile, license);
		addInsuranceDetailsExtension(extensions, transfer);
		if (externalReceivingFacility) {
			ObjectNode requires = addObjectNode(extensions);
			requires.put("url", "http://example.org/fhir/StructureDefinition/requires-insurance-agent-verification");
			requires.put("valueBoolean", true);
		}

		addTransferFormKindExtension(extensions);
		addUrgencyTransferTypeExtension(extensions, transfer.getTransferType());
		addTopLevelInsuranceTypeExtension(extensions, transfer);
		addTopLevelTransportTypeExtension(extensions, transfer.getTransportType());

		// Dedicated clinical timestamps — preview/validation must NOT use period.end for these.
		addDateTimeExtension(extensions, "http://example.org/fhir/StructureDefinition/admission-datetime",
				transfer.getAdmissionAt());
		addDateTimeExtension(extensions, "http://example.org/fhir/StructureDefinition/decision-to-transfer-datetime",
				transfer.getDecisionToTransferAt());
		addDateTimeExtension(extensions, "http://example.org/fhir/StructureDefinition/calling-time",
				combineDateAndTime(transfer.getDecisionToTransferAt(), transfer.getCallingTime()));
		addDateTimeExtension(extensions, "http://example.org/fhir/StructureDefinition/ambulance-call-time",
				combineDateAndTime(transfer.getDecisionToTransferAt(), transfer.getAmbulanceCallTime()));
		addDateTimeExtension(extensions, "http://example.org/fhir/StructureDefinition/departure-time",
				combineDateAndTime(transfer.getDecisionToTransferAt(), transfer.getDepartRefTime()));

		addStringExtension(extensions, "http://example.org/fhir/StructureDefinition/referring-department",
				transfer.getReferringUnit());
		addReceivingClinicianContactExtension(extensions, transfer);

		addTransferDetailsExtension(extensions, transfer, upi, transferBusinessId, encounterId, user, license);
	}

	private void addMeta(ObjectNode encounter) {
		ObjectNode meta = encounter.putObject("meta");
		ObjectNode tag = addObjectNode(meta.putArray("tag"));
		tag.put("system", "http://fhir.openmrs.org/ext/encounter-tag");
		tag.put("code", "encounter");
		tag.put("display", "Encounter");
	}

	private void addLength(ObjectNode encounter) {
		ObjectNode length = encounter.putObject("length");
		length.put("value", 1);
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

	private void addPartOfIfPresent(ObjectNode encounter, Transfer transfer, String currentEncounterId) {
		String previousEncounterId = StringUtils.trimToNull(transfer != null ? transfer.getHieTransferId() : null);
		if (previousEncounterId == null || previousEncounterId.equals(currentEncounterId)) {
			return;
		}
		ObjectNode partOf = encounter.putObject("partOf");
		partOf.put("reference", "Encounter/" + previousEncounterId);
	}

	private void addTransferFormKindExtension(ArrayNode extensions) {
		ObjectNode extension = addObjectNode(extensions);
		extension.put("url", TransferFormKind.EXTENSION_URL);
		ObjectNode value = extension.putObject("valueCodeableConcept");
		ObjectNode coding = addObjectNode(value.putArray("coding"));
		coding.put("system", TransferFormKind.CODE_SYSTEM);
		coding.put("code", TransferFormKind.GENERAL.getCode());
		coding.put("display", TransferFormKind.GENERAL.getDisplay());
	}

	private void addUrgencyTransferTypeExtension(ArrayNode extensions, String transferType) {
		if (StringUtils.isBlank(transferType)) {
			return;
		}
		String code;
		String display;
		if ("EMERGENCY".equals(transferType)) {
			code = "emergency";
			display = "Emergency";
		}
		else if ("NOT_EMERGENCY".equals(transferType)) {
			code = "not-emergency";
			display = "Not emergency";
		}
		else if ("FOLLOW_UP".equals(transferType)) {
			code = "follow-up";
			display = "Follow-up";
		}
		else {
			code = transferType.toLowerCase(Locale.ENGLISH);
			display = transferType.trim();
		}
		ObjectNode extension = addObjectNode(extensions);
		extension.put("url", EXT_TRANSFER_TYPE);
		ObjectNode value = extension.putObject("valueCodeableConcept");
		ObjectNode coding = addObjectNode(value.putArray("coding"));
		coding.put("system", EXT_TRANSFER_TYPE_SYSTEM);
		coding.put("code", code);
		coding.put("display", display);
	}

	private void addTopLevelInsuranceTypeExtension(ArrayNode extensions, Transfer transfer) {
		String insuranceType = transfer != null ? StringUtils.trimToNull(transfer.getHealthInsuranceType()) : null;
		if (insuranceType == null) {
			return;
		}
		String insuranceOther = transfer.getHealthInsuranceOther();
		ObjectNode extension = addObjectNode(extensions);
		extension.put("url", EXT_INSURANCE_TYPE);
		ObjectNode value = extension.putObject("valueCodeableConcept");
		ObjectNode coding = addObjectNode(value.putArray("coding"));
		coding.put("system", EXT_INSURANCE_TYPE_SYSTEM);
		coding.put("code", insuranceType.toLowerCase(Locale.ENGLISH));
		coding.put("display", insuranceTypeDisplay(insuranceType, insuranceOther));
	}

	private void addTopLevelTransportTypeExtension(ArrayNode extensions, String transportType) {
		if (StringUtils.isBlank(transportType) || "NA".equals(transportType)) {
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
			display = "Other";
		}
		else {
			code = transportType.toLowerCase(Locale.ENGLISH);
			display = transportType.trim();
		}
		ObjectNode extension = addObjectNode(extensions);
		extension.put("url", EXT_TRANSPORT_TYPE);
		ObjectNode value = extension.putObject("valueCodeableConcept");
		ObjectNode coding = addObjectNode(value.putArray("coding"));
		coding.put("system", EXT_TRANSPORT_TYPE_SYSTEM);
		coding.put("code", code);
		coding.put("display", display);
	}

	private void addReceivingClinicianContactExtension(ArrayNode extensions, Transfer transfer) {
		String name = StringUtils.trimToNull(transfer.getStaffContactedName());
		String phone = StringUtils.trimToNull(transfer.getStaffContactedPhone());
		Date callingAt = combineDateAndTime(transfer.getDecisionToTransferAt(), transfer.getCallingTime());
		if (name == null && phone == null && callingAt == null) {
			return;
		}
		ObjectNode extension = addObjectNode(extensions);
		extension.put("url", "http://example.org/fhir/StructureDefinition/receiving-clinician-contact");
		ArrayNode nested = extension.putArray("extension");
		addNestedString(nested, "name", name);
		addNestedString(nested, "phone", phone);
		if (callingAt != null) {
			ObjectNode callingTimeExtension = addObjectNode(nested);
			callingTimeExtension.put("url", "calling-time");
			callingTimeExtension.put("valueDateTime", formatDateTime(callingAt));
		}
	}

	private void addDateTimeExtension(ArrayNode extensions, String url, Date dateTime) {
		if (dateTime == null || StringUtils.isBlank(url)) {
			return;
		}
		ObjectNode extension = addObjectNode(extensions);
		extension.put("url", url);
		extension.put("valueDateTime", formatDateTime(dateTime));
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

	/**
	 * Insurance details: type (from form), insurance ID (patient registration/obs), and
	 * eligibility status derived from those values.
	 */
	private void addInsuranceDetailsExtension(ArrayNode extensions, Transfer transfer) {
		String insuranceType = transfer != null ? StringUtils.trimToNull(transfer.getHealthInsuranceType()) : null;
		String insuranceOther = transfer != null ? StringUtils.trimToNull(transfer.getHealthInsuranceOther()) : null;
		String insuranceId = resolveInsuranceId(transfer);
		String eligibilityCode = resolveInsuranceEligibilityCode(insuranceType, insuranceId);

		if (insuranceType == null && insuranceId == null) {
			return;
		}

		ObjectNode insuranceDetails = addObjectNode(extensions);
		insuranceDetails.put("url", INSURANCE_DETAILS_URL);
		ArrayNode nested = insuranceDetails.putArray("extension");

		if (insuranceType != null) {
			ObjectNode typeExt = addObjectNode(nested);
			typeExt.put("url", "insurance-type");
			ObjectNode typeCoding = typeExt.putObject("valueCoding");
			typeCoding.put("system", INSURANCE_TYPE_SYSTEM);
			typeCoding.put("code", insuranceType.toUpperCase(Locale.ENGLISH));
			typeCoding.put("display", insuranceTypeDisplay(insuranceType, insuranceOther));
		}

		if (StringUtils.isNotBlank(insuranceId)) {
			ObjectNode idExt = addObjectNode(nested);
			idExt.put("url", "insurance-id");
			idExt.put("valueString", insuranceId.trim());
		}

		if (eligibilityCode != null) {
			ObjectNode eligibilityExt = addObjectNode(nested);
			eligibilityExt.put("url", "eligibility-status");
			ObjectNode eligibilityCoding = eligibilityExt.putObject("valueCoding");
			eligibilityCoding.put("system", INSURANCE_ELIGIBILITY_SYSTEM);
			eligibilityCoding.put("code", eligibilityCode);
			eligibilityCoding.put("display", insuranceEligibilityDisplay(eligibilityCode));
		}
	}

	private String resolveInsuranceId(Transfer transfer) {
		if (transfer == null || transfer.getPatient() == null) {
			return null;
		}
		PatientInsuranceService insuranceService = getPatientInsuranceService();
		if (insuranceService == null) {
			return null;
		}
		try {
			return StringUtils.trimToNull(insuranceService.resolveInsuranceCardNumber(transfer.getPatient()));
		}
		catch (Exception ignored) {
			return null;
		}
	}

	/**
	 * ELIGIBLE when a non-NONE insurance type is present and an insurance ID is available;
	 * NOT_ELIGIBLE when type is NONE / blank with no ID; otherwise UNKNOWN.
	 */
	private static String resolveInsuranceEligibilityCode(String insuranceType, String insuranceId) {
		String type = StringUtils.trimToEmpty(insuranceType).toUpperCase(Locale.ENGLISH);
		boolean hasId = StringUtils.isNotBlank(insuranceId);
		if (TransferAppConstants.HEALTH_INSURANCE_NONE.equals(type)
				|| "NA".equals(type) || "N/A".equals(type)) {
			return "NOT_ELIGIBLE";
		}
		if (StringUtils.isNotBlank(type) && hasId) {
			return "ELIGIBLE";
		}
		if (StringUtils.isNotBlank(type) || hasId) {
			return "UNKNOWN";
		}
		return null;
	}

	private static String insuranceTypeDisplay(String insuranceType, String insuranceOther) {
		String type = StringUtils.trimToEmpty(insuranceType).toUpperCase(Locale.ENGLISH);
		if (TransferAppConstants.HEALTH_INSURANCE_CBHI.equals(type)) {
			return "CBHI (mutuelle)";
		}
		if (TransferAppConstants.HEALTH_INSURANCE_RSSB.equals(type)) {
			return "RSSB";
		}
		if (TransferAppConstants.HEALTH_INSURANCE_MMI.equals(type)) {
			return "MMI";
		}
		if (TransferAppConstants.HEALTH_INSURANCE_NONE.equals(type)) {
			return "None";
		}
		if (TransferAppConstants.HEALTH_INSURANCE_OTHER.equals(type)) {
			return StringUtils.isNotBlank(insuranceOther) ? insuranceOther.trim() : "Other";
		}
		return StringUtils.isNotBlank(insuranceType) ? insuranceType.trim() : "Unknown";
	}

	private static String insuranceEligibilityDisplay(String code) {
		if ("ELIGIBLE".equals(code)) {
			return "Eligible";
		}
		if ("NOT_ELIGIBLE".equals(code)) {
			return "Not eligible";
		}
		return "Unknown";
	}

	private PatientInsuranceService getPatientInsuranceService() {
		if (patientInsuranceService != null) {
			return patientInsuranceService;
		}
		try {
			patientInsuranceService = Context.getService(PatientInsuranceService.class);
			return patientInsuranceService;
		}
		catch (Exception ignored) {
			return null;
		}
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
			String transferBusinessId, String encounterId, User user, String license) {
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

		addCaregiverInfoExtension(nested, transfer);
		addStringExtension(nested, "http://example.org/fhir/StructureDefinition/vital-signs", formatVitals(transfer));
		addStringExtension(nested, "http://example.org/fhir/StructureDefinition/clinical-presentation",
				transfer.getClinicalPresentation());
		addStringExtension(nested, "http://example.org/fhir/StructureDefinition/disability-type",
				transfer.getDisabilityType());
		addStringExtension(nested, "http://example.org/fhir/StructureDefinition/lab-results",
				transfer.getLaboratory());
		addStringExtension(nested, "http://example.org/fhir/StructureDefinition/others-notes",
				transfer.getOtherNotes());
		addStringExtension(nested, "http://example.org/fhir/StructureDefinition/procedures-treatments",
				transfer.getProceduresTreatments());
		addAmbulanceProviderFacilityExtension(nested, transfer);
		addPatientDemographicsExtension(nested, transfer, upi);
		addStringExtension(nested, "http://example.org/fhir/StructureDefinition/receiving-province",
				transfer.getReceivingProvince());
		addStringExtension(nested, "http://example.org/fhir/StructureDefinition/receiving-district",
				transfer.getReceivingDistrict());
		addPatientAddressExtension(nested, transfer);
		addPractitionerInfoExtension(nested, transfer, user, license);

		addTransportExtension(nested, transfer);
	}

	private void addCaregiverInfoExtension(ArrayNode nested, Transfer transfer) {
		String name = StringUtils.trimToNull(transfer.getCaregiverName());
		String phone = StringUtils.trimToNull(transfer.getCaregiverTelephone());
		if (name == null && phone == null) {
			return;
		}
		ObjectNode extension = addObjectNode(nested);
		extension.put("url", "http://example.org/fhir/StructureDefinition/caregiver-info");
		ArrayNode caregiverNested = extension.putArray("extension");
		addNestedString(caregiverNested, "name", name);
		addNestedString(caregiverNested, "phone", phone);
	}

	private void addAmbulanceProviderFacilityExtension(ArrayNode nested, Transfer transfer) {
		String fosaId = StringUtils.trimToNull(transfer.getAmbulanceProviderFosaId());
		String name = StringUtils.trimToNull(transfer.getAmbulanceProviderName());
		if (fosaId == null && name == null) {
			return;
		}
		ObjectNode extension = addObjectNode(nested);
		extension.put("url", "http://example.org/fhir/StructureDefinition/ambulance-provider-facility");
		ArrayNode providerNested = extension.putArray("extension");
		addNestedString(providerNested, "fosaId", fosaId);
		addNestedString(providerNested, "name", name);
	}

	private void addPatientDemographicsExtension(ArrayNode nested, Transfer transfer, String upi) {
		String name = StringUtils.trimToNull(transfer.getClientName());
		String gender = StringUtils.trimToNull(transfer.getSex());
		String age = StringUtils.trimToNull(transfer.getAgeOrDob());
		String phone = StringUtils.trimToNull(transfer.getClientTelephone());
		String serial = firstNonBlank(upi, transfer.getEmrId());
		if (name == null && gender == null && age == null && phone == null && serial == null) {
			return;
		}
		ObjectNode extension = addObjectNode(nested);
		extension.put("url", "http://example.org/fhir/StructureDefinition/patient-demographics");
		ArrayNode demoNested = extension.putArray("extension");
		addNestedString(demoNested, "name", name);
		addNestedString(demoNested, "gender", gender);
		addNestedString(demoNested, "age", age);
		addNestedString(demoNested, "phone", phone);
		addNestedString(demoNested, "serial-number", serial);
	}

	private void addPatientAddressExtension(ArrayNode nested, Transfer transfer) {
		String district = StringUtils.trimToNull(transfer.getClientDistrict());
		String sector = StringUtils.trimToNull(transfer.getSector());
		String cell = StringUtils.trimToNull(transfer.getCell());
		String village = StringUtils.trimToNull(transfer.getVillage());
		if (district == null && sector == null && cell == null && village == null) {
			return;
		}
		ObjectNode extension = addObjectNode(nested);
		extension.put("url", "http://example.org/fhir/StructureDefinition/patient-address");
		ArrayNode addressNested = extension.putArray("extension");
		addNestedString(addressNested, "district", district);
		addNestedString(addressNested, "sector", sector);
		addNestedString(addressNested, "cell", cell);
		addNestedString(addressNested, "village", village);
	}

	private void addPractitionerInfoExtension(ArrayNode nested, Transfer transfer, User user, String license) {
		String name = resolveProviderDisplayName(transfer, user);
		String qualification = StringUtils.trimToNull(transfer.getProviderQualification());
		String phone = StringUtils.trimToNull(transfer.getProviderPhone());
		if (StringUtils.isBlank(name) && qualification == null && phone == null && StringUtils.isBlank(license)) {
			return;
		}
		ObjectNode extension = addObjectNode(nested);
		extension.put("url", "http://example.org/fhir/StructureDefinition/practitioner-info");
		ArrayNode practitionerNested = extension.putArray("extension");
		addNestedString(practitionerNested, "name",
				StringUtils.isNotBlank(license) && StringUtils.isNotBlank(name)
						? name + " (" + license.trim() + ")"
						: name);
		addNestedString(practitionerNested, "qualification", qualification);
		addNestedString(practitionerNested, "phone", phone);
		addNestedString(practitionerNested, "license-number", license);
	}

	private static String formatVitals(Transfer transfer) {
		if (transfer == null) {
			return null;
		}
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

	private void addNestedString(ArrayNode nested, String url, String value) {
		if (StringUtils.isBlank(value)) {
			return;
		}
		ObjectNode extension = addObjectNode(nested);
		extension.put("url", url);
		extension.put("valueString", value.trim());
	}

	private void addStringExtension(ArrayNode extensions, String url, String value) {
		if (StringUtils.isBlank(value) || StringUtils.isBlank(url)) {
			return;
		}
		ObjectNode extension = addObjectNode(extensions);
		extension.put("url", url);
		extension.put("valueString", value.trim());
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
		ObjectNode type = identifier.putObject("type");
		ObjectNode typeCoding = addObjectNode(type.putArray("coding"));
		typeCoding.put("code", "UPI");
		typeCoding.put("display", "UPI");
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

		ObjectNode admitSource = hospitalization.putObject("admitSource");
		ObjectNode admitCoding = addObjectNode(admitSource.putArray("coding"));
		admitCoding.put("system", "http://terminology.hl7.org/CodeSystem/admit-source");
		admitCoding.put("code", "hosp-trans");
		admitCoding.put("display", blankToDefault(transfer.getReferringUnit(), "Hospital Transfer"));

		ObjectNode destination = hospitalization.putObject("destination");
		populateFacilityReference(destination, "Location", transfer.getReceivingFacilityCode(),
				receivingFacilityLabel, "Receiving facility");

		ObjectNode dischargeDisposition = hospitalization.putObject("dischargeDisposition");
		ObjectNode dischargeCoding = addObjectNode(dischargeDisposition.putArray("coding"));
		dischargeCoding.put("system", "http://terminology.hl7.org/CodeSystem/discharge-disposition");
		dischargeCoding.put("code", "hosp");
		dischargeCoding.put("display", blankToDefault(transfer.getReceivingService(), "Hospital"));
	}

	private void addLocation(ObjectNode encounter, Transfer transfer, String receivingFacilityLabel,
			Date periodStart, Date periodEnd) {
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

	private String requireUpi(Transfer transfer) {
		String upi = StringUtils.trimToNull(transfer != null ? transfer.getEmrId() : null);
		if (upi == null && transfer != null && patientSnapshotResolver != null) {
			upi = StringUtils.trimToNull(patientSnapshotResolver.ensureEmrIdFromPatient(
					transfer, transfer.getPatient()));
		}
		if (StringUtils.isBlank(upi)) {
			throw new HieApiException(
					"Cannot submit transfer: patient UPID is missing. Register a UPID on the patient chart, then edit and resubmit the transfer.");
		}
		return upi.trim();
	}

	private static Date requireDecisionToTransferAt(Transfer transfer) {
		if (transfer == null || transfer.getDecisionToTransferAt() == null) {
			throw new HieApiException("Cannot submit transfer: Date and time of decision to transfer is required.");
		}
		return transfer.getDecisionToTransferAt();
	}

	private static Date plusOneMonth(Date start) {
		Calendar calendar = Calendar.getInstance(RWANDA);
		calendar.setTime(start);
		calendar.add(Calendar.MONTH, 1);
		return calendar.getTime();
	}

	private static String formatDateTime(Date date) {
		if (date == null) {
			return null;
		}
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.ENGLISH);
		format.setTimeZone(RWANDA);
		return format.format(date);
	}

	private static Date combineDateAndTime(Date day, String timeHHmmOrHHmmss) {
		if (day == null || StringUtils.isBlank(timeHHmmOrHHmmss)) {
			return null;
		}
		String raw = timeHHmmOrHHmmss.trim();
		String[] parts = raw.split(":");
		if (parts.length < 2) {
			return null;
		}
		try {
			Calendar calendar = Calendar.getInstance(RWANDA);
			calendar.setTime(day);
			calendar.set(Calendar.SECOND, 0);
			calendar.set(Calendar.MILLISECOND, 0);
			calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(parts[0]));
			calendar.set(Calendar.MINUTE, Integer.parseInt(parts[1]));
			if (parts.length >= 3) {
				calendar.set(Calendar.SECOND, Integer.parseInt(parts[2]));
			}
			return calendar.getTime();
		}
		catch (NumberFormatException ex) {
			return null;
		}
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
