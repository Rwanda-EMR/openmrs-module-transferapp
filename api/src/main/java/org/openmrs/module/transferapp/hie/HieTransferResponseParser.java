package org.openmrs.module.transferapp.hie;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.openmrs.module.transferapp.TransferAppConstants;
import org.openmrs.module.transferapp.model.TransferProfile;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class HieTransferResponseParser {

    private static final String EXT_RECEIVING_CLINICIAN_CONTACT =
            "http://example.org/fhir/StructureDefinition/receiving-clinician-contact";
    private static final String EXT_REFERRING_DEPARTMENT =
            "http://example.org/fhir/StructureDefinition/referring-department";
    private static final String EXT_RECEIVING_DEPARTMENT =
            "http://example.org/fhir/StructureDefinition/receiving-department";
    private static final String EXT_AMBULANCE_CALL_TIME =
            "http://example.org/fhir/StructureDefinition/ambulance-call-time";
    private static final String EXT_CALLING_TIME =
            "http://example.org/fhir/StructureDefinition/calling-time";
    private static final String EXT_ADMISSION_DATETIME =
            "http://example.org/fhir/StructureDefinition/admission-datetime";
    private static final String EXT_DECISION_TO_TRANSFER_DATETIME =
            "http://example.org/fhir/StructureDefinition/decision-to-transfer-datetime";
    /** Alternate decision URL used by some sending EMRs (e.g. Nyanza sample). */
    private static final String EXT_TRANSFER_DECISION =
            "http://example.org/fhir/StructureDefinition/transfer-decision";
    private static final String EXT_PRACTITIONER_INFO =
            "http://example.org/fhir/StructureDefinition/practitioner-info";
    private static final String EXT_ETRANSFER_FORM =
            "http://moh.gov.rw/fhir/StructureDefinition/etransfer-transfer-form";

    private static final String EXT_PATIENT_DEMOGRAPHICS =
            "http://example.org/fhir/StructureDefinition/patient-demographics";
    private static final String EXT_CAREGIVER_INFO =
            "http://example.org/fhir/StructureDefinition/caregiver-info";
    private static final String EXT_RECEIVING_PROVINCE =
            "http://example.org/fhir/StructureDefinition/receiving-province";
    private static final String EXT_RECEIVING_DISTRICT =
            "http://example.org/fhir/StructureDefinition/receiving-district";
    private static final String EXT_PATIENT_ADDRESS =
            "http://example.org/fhir/StructureDefinition/patient-address";
    private static final String EXT_TRANSFER_FLAGS =
            "http://example.org/fhir/StructureDefinition/transfer-flags";
    private static final String EXT_DEPARTURE_TIME =
            "http://example.org/fhir/StructureDefinition/departure-time";
    private static final String EXT_TRANSFER_TYPE =
            "http://example.org/fhir/StructureDefinition/transfer-type";
    private static final String EXT_CLINICAL_PRESENTATION =
            "http://example.org/fhir/StructureDefinition/clinical-presentation";
    private static final String EXT_TRANSPORT_TYPE =
            "http://example.org/fhir/StructureDefinition/transport-type";
    private static final String EXT_INSURANCE_TYPE =
            "http://example.org/fhir/StructureDefinition/insurance-type";
    private static final String EXT_LAB_RESULTS =
            "http://example.org/fhir/StructureDefinition/lab-results";
    private static final String EXT_OTHERS =
            "http://example.org/fhir/StructureDefinition/others-notes";
    private static final String EXT_PROCEDURES_AND_TREATMENTS =
            "http://example.org/fhir/StructureDefinition/procedures-treatments";
    private static final String EXT_ADDITIONAL_NOTES =
            "http://example.org/fhir/StructureDefinition/additional-notes";
    private static final String EXT_VITAL_SIGNS =
            "http://example.org/fhir/StructureDefinition/vital-signs";
    private static final String EXT_EXTENDED_VITALS =
            "http://example.org/fhir/StructureDefinition/extended-vitals";
    private static final String EXT_REQUIRES_INSURANCE_AGENT_VERIFICATION =
            "http://example.org/fhir/StructureDefinition/requires-insurance-agent-verification";
    private static final String EXT_AGENT_APPROVED =
            "http://example.org/fhir/StructureDefinition/agent-approved";
    private static final String EXT_AGENT_COMMENT =
            "http://example.org/fhir/StructureDefinition/agent-comment";
    private static final String EXT_TRANSFER_FORM_KIND =
            "http://example.org/fhir/StructureDefinition/transfer-form-kind";

    /** New External Transfer payload ({@code devs/transfer.json}). */
    private static final String EXT_PATIENT_PHONE = "patient-phone";
    private static final String EXT_DOCTOR_DETAILS =
            "http://example.rw/fhir/StructureDefinition/doctor-details";
    private static final String EXT_TRANSFER_DETAILS =
            "http://example.rw/fhir/StructureDefinition/transfer-details";
    private static final String EXT_INSURANCE_DETAILS =
            "http://example.rw/fhir/StructureDefinition/insurance-details";

    public List<Map<String, Object>> parse(String jsonData) throws Exception {
        return parsePage(jsonData).getTransfers();
    }

    public HieTransferResponsePage parsePage(String jsonData) throws Exception {
        if (jsonData == null || jsonData.trim().isEmpty()) {
            return emptyPage();
        }

        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readTree(jsonData);

        JsonNode resourceType = rootNode.get("resourceType");
        if (resourceType == null || resourceType.isNull() || !"Parameters".equals(resourceType.getTextValue())) {
            return emptyPage();
        }

        JsonNode parameters = rootNode.get("parameter");
        if (parameters == null || !parameters.isArray()) {
            return emptyPage();
        }

        JsonNode bundleNode = null;
        boolean hasMore = false;
        Integer page = null;
        Integer size = null;
        Integer total = null;
        Iterator<JsonNode> parameterIterator = parameters.getElements();
        while (parameterIterator.hasNext()) {
            JsonNode param = parameterIterator.next();
            JsonNode nameNode = param.get("name");
            if (nameNode == null || nameNode.isNull()) {
                continue;
            }
            String parameterName = nameNode.getTextValue();
            if ("bundle".equals(parameterName)) {
                bundleNode = param.get("resource");
            } else if ("hasMore".equals(parameterName)) {
                JsonNode value = param.get("valueBoolean");
                hasMore = value != null && !value.isNull() && value.getBooleanValue();
            } else if ("page".equals(parameterName)) {
                page = integerValue(param.get("valueInteger"));
            } else if ("size".equals(parameterName)) {
                size = integerValue(param.get("valueInteger"));
            } else if ("total".equals(parameterName)) {
                total = integerValue(param.get("valueInteger"));
            }
        }

        if (bundleNode == null || bundleNode.isNull()) {
            return new HieTransferResponsePage(new ArrayList<Map<String, Object>>(), hasMore, page, size, total);
        }
        if (total == null) {
            total = integerValue(bundleNode.get("total"));
        }

        JsonNode entries = bundleNode.get("entry");
        if (entries == null || !entries.isArray()) {
            return new HieTransferResponsePage(new ArrayList<Map<String, Object>>(), hasMore, page, size, total);
        }

        Iterator<JsonNode> entriesIterator = entries.getElements();
        if (!entriesIterator.hasNext()) {
            return new HieTransferResponsePage(new ArrayList<Map<String, Object>>(), hasMore, page, size, total);
        }

        List<Map<String, Object>> transferList = new ArrayList<Map<String, Object>>();
        while (entriesIterator.hasNext()) {
            JsonNode entry = entriesIterator.next();
            JsonNode resource = entry.get("resource");
            if (resource == null || resource.isNull()) {
                continue;
            }

            Map<String, Object> transfer = new LinkedHashMap<String, Object>();
            initializeTransferFormPlaceholders(transfer);

            transfer.put("id", textOrDefault(resource.get("id"), ""));
            transfer.put("status", firstNonBlank(textOrDefault(resource.get("status"), ""), "unknown"));

            JsonNode subject = resource.get("subject");
            if (subject != null && !subject.isNull()) {
                JsonNode subjectIdentifier = subject.get("identifier");
                if (subjectIdentifier != null && !subjectIdentifier.isNull()) {
                    transfer.put("subject", textOrDefault(subjectIdentifier.get("value"), ""));
                } else {
                    transfer.put("subject", "");
                }
            } else {
                transfer.put("subject", "");
            }

            JsonNode period = resource.get("period");
            String startDateTime = "";
            if (period != null && !period.isNull()) {
                // List "date" column uses period.start only — never period.end (+1 month window).
                startDateTime = textOrDefault(period.get("start"), "");
            }
            transfer.put("date", toDateOnly(startDateTime));

            JsonNode hospitalization = resource.get("hospitalization");
            if (hospitalization != null && !hospitalization.isNull()) {
                JsonNode origin = hospitalization.get("origin");
                if (origin != null && !origin.isNull()) {
                    transfer.put("origin", textOrDefault(origin.get("display"), ""));
                } else {
                    transfer.put("origin", "");
                }

                JsonNode destination = hospitalization.get("destination");
                if (destination != null && !destination.isNull()) {
                    transfer.put("destination", textOrDefault(destination.get("display"), ""));
                } else {
                    transfer.put("destination", "");
                }

                JsonNode admitSource = hospitalization.get("admitSource");
                transfer.put("admitSource", codingDisplay(admitSource));
            } else {
                transfer.put("origin", "");
                transfer.put("destination", "");
                transfer.put("admitSource", "");
            }

            mapTransferFormFields(resource, transfer);
            transferList.add(transfer);
        }

        return new HieTransferResponsePage(transferList, hasMore, page, size, total);
    }

    private HieTransferResponsePage emptyPage() {
        return new HieTransferResponsePage(new ArrayList<Map<String, Object>>(), false, null, null, null);
    }

    private Integer integerValue(JsonNode node) {
        return node == null || node.isNull() ? null : node.getIntValue();
    }

    /**
     * Initializes placeholders aligned with the transfer form so fields can be
     * populated incrementally without breaking clients expecting these keys.
     */
    private void initializeTransferFormPlaceholders(Map<String, Object> transfer) {
        transfer.put("province", "");
        transfer.put("district", "");
        transfer.put("hospitalName", "");
        transfer.put("referringFacilityName", "");
        transfer.put("referringUnit", "");
        transfer.put("receivingClinicianPhone", "");

        transfer.put("clientName", "");
        transfer.put("serialNumberOrEmrId", "");
        transfer.put("ageDob", "");
        transfer.put("sex", "");
        transfer.put("caregiverName", "");
        transfer.put("telephone", "");
        transfer.put("clientTelephone", "");
        transfer.put("caregiverTelephone", "");
        transfer.put("patientDistrict", "");
        transfer.put("patientSector", "");
        transfer.put("patientCell", "");
        transfer.put("patientVillage", "");

        transfer.put("admissionDatetime", "");
        transfer.put("transferDecisionDatetime", "");
        transfer.put("receivingFacility", "");
        transfer.put("receivingService", "");
        transfer.put("callingTime", "");
        transfer.put("staffContactedAtReceivingFacility", "");
        transfer.put("staffContactPhone", "");

        transfer.put("transferType", "");
        transfer.put("formKind", "GENERAL");
        transfer.put("formKindCode", "external");
        transfer.put("formKindDisplay", "External transfer form");
        transfer.put("isEmergency", "");
        transfer.put("isNonEmergency", "");
        transfer.put("isFollowUp", "");
        transfer.put("ambulanceCalledTime", "");
        transfer.put("departureTime", "");

        transfer.put("reasonForTransfer", "");
        transfer.put("significantFindings", "");
        transfer.put("clinicalPresentation", "");
        transfer.put("disabilityType", "");

        transfer.put("temperature", "");
        transfer.put("spo2", "");
        transfer.put("respiratoryRate", "");
        transfer.put("pulse", "");
        transfer.put("bloodPressure", "");
        transfer.put("weight", "");
        transfer.put("height", "");
        transfer.put("muac", "");
        transfer.put("laboratory", "");
        transfer.put("others", "");
        transfer.put("diagnosis", "");
        transfer.put("proceduresAndTreatments", "");

        transfer.put("transportType", "");
        transfer.put("isAmbulanceTransport", "");
        transfer.put("otherTransportType", "");
        transfer.put("isNaTransport", "");
        transfer.put("ambulanceProviderFosaId", "");
        transfer.put("ambulanceProviderName", "");
        transfer.put("healthInsurance", "");
        transfer.put("insuranceId", "");
        transfer.put("insuranceEligibilityStatus", "");
        transfer.put("isCbhiInsurance", "");
        transfer.put("isRssbInsurance", "");
        transfer.put("isMmiInsurance", "");
        transfer.put("otherInsurance", "");
        transfer.put("isNoInsurance", "");

        transfer.put("referringProviderName", "");
        transfer.put("referringProviderQualification", "");
        transfer.put("formDate", "");
        transfer.put("formTime", "");
        transfer.put("providerPhone", "");
        transfer.put("signatureAndStamp", "");

        transfer.put("requiresInsuranceAgentVerification", "false");
        transfer.put("hasAgentApprovedExtension", "false");
        transfer.put("agentApproved", "false");
        transfer.put("agentComment", "");
        transfer.put("agentRejected", "false");
        transfer.put("agentDecisionApproved", "false");
        transfer.put("needsInsuranceApproval", "false");
    }

    private void mapTransferFormFields(JsonNode resource, Map<String, Object> transfer) {
        JsonNode subject = resource.get("subject");
        if (subject != null && !subject.isNull()) {
            transfer.put("clientName", textOrDefault(subject.get("display"), ""));
            JsonNode identifier = subject.get("identifier");
            if (identifier != null && !identifier.isNull()) {
                transfer.put("serialNumberOrEmrId", textOrDefault(identifier.get("value"), ""));
            }
        }

        transfer.put("clientName", firstNonBlank(
                extractNestedPreferTopThenDetails(resource, EXT_PATIENT_DEMOGRAPHICS, "name"),
                asString(transfer.get("clientName"))));
        transfer.put("serialNumberOrEmrId", firstNonBlank(
                extractNestedPreferTopThenDetails(resource, EXT_PATIENT_DEMOGRAPHICS, "serial-number"),
                asString(transfer.get("serialNumberOrEmrId"))));
        String patientDob = extractNestedPreferTopThenDetails(resource, EXT_PATIENT_DEMOGRAPHICS, "dob");
        String patientAge = extractNestedPreferTopThenDetails(resource, EXT_PATIENT_DEMOGRAPHICS, "age");
        transfer.put("ageDob", formatAgeOrDob(patientAge, patientDob));
        transfer.put("sex", extractNestedPreferTopThenDetails(resource, EXT_PATIENT_DEMOGRAPHICS, "gender"));
        String patientPhone = firstNonBlank(
                extractExtensionLeafValue(resource, EXT_PATIENT_PHONE),
                extractNestedPreferTopThenDetails(resource, EXT_PATIENT_DEMOGRAPHICS, "phone"));
        transfer.put("clientTelephone", patientPhone);

        transfer.put("caregiverName", extractNestedPreferTopThenDetails(resource, EXT_CAREGIVER_INFO, "name"));
        String caregiverPhone = extractNestedPreferTopThenDetails(resource, EXT_CAREGIVER_INFO, "phone");
        transfer.put("caregiverTelephone", caregiverPhone);
        transfer.put("telephone", firstNonBlank(patientPhone, caregiverPhone));
        transfer.put("patientPhone", patientPhone);

        transfer.put("province", stripCodePrefix(extractLeafPreferTopThenDetails(resource, EXT_RECEIVING_PROVINCE)));
        transfer.put("district", stripCodePrefix(extractLeafPreferTopThenDetails(resource, EXT_RECEIVING_DISTRICT)));

        transfer.put("patientDistrict", stripCodePrefix(
                extractNestedPreferTopThenDetails(resource, EXT_PATIENT_ADDRESS, "district")));
        transfer.put("patientSector", stripCodePrefix(
                extractNestedPreferTopThenDetails(resource, EXT_PATIENT_ADDRESS, "sector")));
        transfer.put("patientCell", stripCodePrefix(
                extractNestedPreferTopThenDetails(resource, EXT_PATIENT_ADDRESS, "cell")));
        transfer.put("patientVillage", stripCodePrefix(
                extractNestedPreferTopThenDetails(resource, EXT_PATIENT_ADDRESS, "village")));

        JsonNode period = resource.get("period");
        String periodStart = "";
        String periodEnd = "";
        if (period != null && !period.isNull()) {
            periodStart = textOrDefault(period.get("start"), "");
            periodEnd = textOrDefault(period.get("end"), "");
        }
        // Expose both for debugging. NEVER use period.end as clinical decision/admission time —
        // outbound payloads set period.end = decision + 1 month (active window), which caused
        // receivers to show decision datetime shifted by one month when they mistook end for start.
        transfer.put("periodStart", periodStart);
        transfer.put("periodEnd", periodEnd);
        // Clinical timestamps from dedicated extensions only here.
        // Do NOT fall back to period.start yet — that would block etransfer-transfer-form
        // admissionAt / decisionToTransferAt (Nyanza payloads use transfer-decision + form,
        // not decision-to-transfer-datetime / admission-datetime). Never use period.end.
        transfer.put("admissionDatetime", firstNonBlank(
                extractExtensionDateTime(resource, EXT_ADMISSION_DATETIME),
                extractNestedExtensionValue(resource, EXT_TRANSFER_FLAGS, "admission-date")));
        transfer.put("transferDecisionDatetime", firstNonBlank(
                extractExtensionDateTime(resource, EXT_DECISION_TO_TRANSFER_DATETIME),
                extractExtensionDateTime(resource, EXT_TRANSFER_DECISION)));
		transfer.put("departureTime", extractExtensionDateTime(resource, EXT_DEPARTURE_TIME));
        transfer.put("ambulanceCalledTime", extractExtensionDateTime(resource, EXT_AMBULANCE_CALL_TIME));
        transfer.put("callingTime", firstNonBlank(
                extractExtensionDateTime(resource, EXT_CALLING_TIME),
                extractNestedExtensionValue(resource, EXT_RECEIVING_CLINICIAN_CONTACT, "calling-time")));

        // Urgency type from legacy example.org transfer-type; workflow type from transfer-details.
        String transferTypeCode = firstNonBlank(
                extractExtensionCode(resource, EXT_TRANSFER_TYPE),
                extractNestedCodingCode(resource, EXT_TRANSFER_DETAILS, "transfer-type"));
        String transferTypeDisplay = firstNonBlank(
                extractExtensionDisplay(resource, EXT_TRANSFER_TYPE),
                extractNestedCodingDisplay(resource, EXT_TRANSFER_DETAILS, "transfer-type"),
                transferTypeCode);
        String transferType = firstNonBlank(transferTypeDisplay, transferTypeCode);
        transfer.put("transferType", transferType);
        transfer.put("workflowTransferType", firstNonBlank(
                extractNestedCodingCode(resource, EXT_TRANSFER_DETAILS, "transfer-type"),
                ""));
        transfer.put("transferBusinessId", extractNestedExtensionValue(resource, EXT_TRANSFER_DETAILS, "transfer-id"));
        transfer.put("qrCode", extractNestedExtensionValue(resource, EXT_TRANSFER_DETAILS, "qr-code"));
        applyTransferTypeFlags(transfer, transferTypeCode, transferTypeDisplay);

        applyTransferFormKind(resource, transfer);

        JsonNode hospitalization = resource.get("hospitalization");
        String originDisplay = "";
        String destinationDisplay = "";
        String admitSourceDisplay = "";
        String dischargeDispositionDisplay = "";

        if (hospitalization != null && !hospitalization.isNull()) {
            JsonNode origin = hospitalization.get("origin");
            if (origin != null && !origin.isNull()) {
                originDisplay = textOrDefault(origin.get("display"), "");
            }
            JsonNode destination = hospitalization.get("destination");
            if (destination != null && !destination.isNull()) {
                destinationDisplay = textOrDefault(destination.get("display"), "");
            }
            admitSourceDisplay = codingDisplay(hospitalization.get("admitSource"));
            dischargeDispositionDisplay = codingDisplay(hospitalization.get("dischargeDisposition"));
        }

        if (destinationDisplay.trim().isEmpty()) {
            JsonNode locations = resource.get("location");
            JsonNode firstLocation = firstArrayElement(locations);
            if (firstLocation != null) {
                JsonNode locationNode = firstLocation.get("location");
                if (locationNode != null && !locationNode.isNull()) {
                    destinationDisplay = textOrDefault(locationNode.get("display"), "");
                }
            }
        }

        String serviceTypeDisplay = codingDisplay(resource.get("serviceType"));
        String serviceProviderDisplay = referenceDisplay(resource.get("serviceProvider"));
        String locationDisplay = "";
        JsonNode locationsForService = resource.get("location");
        JsonNode firstLocForService = firstArrayElement(locationsForService);
        if (firstLocForService != null) {
            JsonNode locationNode = firstLocForService.get("location");
            if (locationNode != null && !locationNode.isNull()) {
                locationDisplay = textOrDefault(locationNode.get("display"), "");
            }
        }
        transfer.put("referringFacilityName", originDisplay);
        transfer.put("hospitalName", firstNonBlank(destinationDisplay, serviceProviderDisplay));
        transfer.put("receivingFacility", firstNonBlank(destinationDisplay, serviceProviderDisplay, locationDisplay));
        transfer.put("referringUnit", firstNonBlank(
                extractExtensionValue(resource, EXT_REFERRING_DEPARTMENT),
                admitSourceDisplay));
        transfer.put("receivingService", firstNonBlank(
                extractExtensionValue(resource, EXT_RECEIVING_DEPARTMENT),
                serviceTypeDisplay,
                dischargeDispositionDisplay,
                locationDisplay));

        String receivingClinicianContact = extractExtensionValue(resource, EXT_RECEIVING_CLINICIAN_CONTACT);
        String[] clinicianContactParts = parseReceivingClinicianContact(receivingClinicianContact);
        String nestedStaffName = extractNestedExtensionValue(resource, EXT_RECEIVING_CLINICIAN_CONTACT, "name");
        String nestedStaffPhone = extractNestedExtensionValue(resource, EXT_RECEIVING_CLINICIAN_CONTACT, "phone");
        transfer.put("receivingClinicianPhone", firstNonBlank(nestedStaffPhone, receivingClinicianContact));
        transfer.put("staffContactedAtReceivingFacility", firstNonBlank(nestedStaffName, clinicianContactParts[0]));
        transfer.put("staffContactPhone", firstNonBlank(nestedStaffPhone, clinicianContactParts[1]));

        String reasonCodeText = "";
        JsonNode reasonCode = resource.get("reasonCode");
        JsonNode firstReasonCode = firstArrayElement(reasonCode);
        if (firstReasonCode != null) {
            reasonCodeText = textOrDefault(firstReasonCode.get("text"), "");
        }

        String diagnosisDisplay = "";
        JsonNode diagnosis = resource.get("diagnosis");
        JsonNode firstDiagnosis = firstArrayElement(diagnosis);
        if (firstDiagnosis != null) {
            JsonNode condition = firstDiagnosis.get("condition");
            if (condition != null && !condition.isNull()) {
                diagnosisDisplay = textOrDefault(condition.get("display"), "");
            }
        }

        String clinicalPresentationLeaf = extractLeafPreferTopThenDetails(resource, EXT_CLINICAL_PRESENTATION);
        transfer.put("reasonForTransfer", firstNonBlank(
                extractNestedPreferTopThenDetails(resource, EXT_CLINICAL_PRESENTATION, "immediate-condition"),
                extractNestedExtensionValue(resource, EXT_TRANSFER_FLAGS, "consultation-motif"),
                clinicalPresentationLeaf,
                reasonCodeText));
        transfer.put("clinicalPresentation", firstNonBlank(
                extractNestedPreferTopThenDetails(resource, EXT_CLINICAL_PRESENTATION, "presentation"),
                clinicalPresentationLeaf,
                diagnosisDisplay));
        transfer.put("diagnosis", diagnosisDisplay);

        String referringProviderName = "";
        String referringProviderLicense = "";
        JsonNode participants = resource.get("participant");
        JsonNode firstParticipant = firstArrayElement(participants);
        if (firstParticipant != null) {
            JsonNode individual = firstParticipant.get("individual");
            if (individual != null && !individual.isNull()) {
                referringProviderName = textOrDefault(individual.get("display"), "");
                JsonNode practitionerIdentifier = individual.get("identifier");
                if (practitionerIdentifier != null && !practitionerIdentifier.isNull()) {
                    referringProviderLicense = textOrDefault(practitionerIdentifier.get("value"), "");
                }
            }
        }
        transfer.put("referringProviderName", TransferProfile.formatCareProviderName(
                firstNonBlank(
                        extractNestedPreferTopThenDetails(resource, EXT_PRACTITIONER_INFO, "name"),
                        referringProviderName),
                firstNonBlank(
                        extractNestedExtensionValue(resource, EXT_DOCTOR_DETAILS, "license-number"),
                        extractNestedPreferTopThenDetails(resource, EXT_PRACTITIONER_INFO, "license-number"),
                        referringProviderLicense)));
        transfer.put("referringProviderQualification", firstNonBlank(
                extractNestedCodingDisplay(resource, EXT_DOCTOR_DETAILS, "qualification"),
                extractNestedPreferTopThenDetails(resource, EXT_PRACTITIONER_INFO, "qualification")));
        transfer.put("providerPhone", firstNonBlank(
                extractNestedExtensionValue(resource, EXT_DOCTOR_DETAILS, "phone-number"),
                extractNestedPreferTopThenDetails(resource, EXT_PRACTITIONER_INFO, "phone")));
        transfer.put("providerSpecialty", extractNestedCodingDisplay(resource, EXT_DOCTOR_DETAILS, "specialty"));
        transfer.put("formDate", firstNonBlank(
                extractNestedPreferTopThenDetails(resource, EXT_PRACTITIONER_INFO, "signed-date"),
                toDateOnly(periodStart)));
        transfer.put("formTime", firstNonBlank(
                extractNestedPreferTopThenDetails(resource, EXT_PRACTITIONER_INFO, "signed-time"),
                toTimeOnly(periodStart)));

        applyTransportFields(resource, transfer);

        String insurance = firstNonBlank(
                extractNestedCodingDisplay(resource, EXT_INSURANCE_DETAILS, "insurance-type"),
                extractExtensionDisplay(resource, EXT_INSURANCE_TYPE));
        transfer.put("healthInsurance", insurance);
        transfer.put("insuranceId", firstNonBlank(
                extractNestedExtensionValue(resource, EXT_INSURANCE_DETAILS, "insurance-id"),
                ""));
        transfer.put("insuranceEligibilityStatus", firstNonBlank(
                extractNestedCodingCode(resource, EXT_INSURANCE_DETAILS, "eligibility-status"),
                extractNestedCodingDisplay(resource, EXT_INSURANCE_DETAILS, "eligibility-status"),
                ""));
        String insuranceLower = insurance == null ? "" : insurance.toLowerCase().trim();
        boolean isCbhi = insuranceLower.contains("cbhi") || insuranceLower.contains("mutuelle");
        boolean isRssb = insuranceLower.contains("rssb");
        boolean isMmi = insuranceLower.contains("mmi");
        boolean isNone = insuranceLower.isEmpty()
                || "none".equals(insuranceLower)
                || "n/a".equals(insuranceLower)
                || "na".equals(insuranceLower)
                || "no".equals(insuranceLower)
                || "no insurance".equals(insuranceLower)
                || "without insurance".equals(insuranceLower)
                || "uninsured".equals(insuranceLower);
        boolean isKnownNamed = isCbhi || isRssb || isMmi || isNone;
        transfer.put("isCbhiInsurance", String.valueOf(isCbhi));
        transfer.put("isRssbInsurance", String.valueOf(isRssb));
        transfer.put("isMmiInsurance", String.valueOf(isMmi));
        transfer.put("otherInsurance", isKnownNamed ? "" : insurance);
        transfer.put("isNoInsurance", String.valueOf(isNone));
        if (isNone) {
            transfer.put("healthInsuranceType", "NONE");
        }

        transfer.put("laboratory", extractLeafPreferTopThenDetails(resource, EXT_LAB_RESULTS));
        transfer.put("others", firstNonBlank(
                extractLeafPreferTopThenDetails(resource, EXT_OTHERS),
                extractLeafPreferTopThenDetails(resource, EXT_ADDITIONAL_NOTES)));
        transfer.put("proceduresAndTreatments", firstNonBlank(
                extractLeafPreferTopThenDetails(resource, EXT_PROCEDURES_AND_TREATMENTS),
                extractNestedExtensionValue(resource, EXT_TRANSFER_FLAGS, "prescriptions")));
        String vitals = extractLeafPreferTopThenDetails(resource, EXT_VITAL_SIGNS);
        parseVitalSignsIntoTransfer(vitals, transfer);

        String extWeight = extractNestedExtensionValue(resource, EXT_EXTENDED_VITALS, "weight");
        String extHeight = extractNestedExtensionValue(resource, EXT_EXTENDED_VITALS, "height");
        String extMuac = extractNestedExtensionValue(resource, EXT_EXTENDED_VITALS, "muac");
        if (extWeight != null && extWeight.trim().length() > 0) {
            transfer.put("weight", extWeight.trim());
        }
        if (extHeight != null && extHeight.trim().length() > 0) {
            transfer.put("height", extHeight.trim());
        }
        if (extMuac != null && extMuac.trim().length() > 0) {
            transfer.put("muac", extMuac.trim());
        }

        applyInsuranceAgentVerificationFlags(resource, transfer);
        applyEtransferFormFallback(transfer, extractEtransferFormNode(resource));
        applyPeriodDatetimeFallbacks(transfer, periodStart, periodEnd);
        applyReferralFeedbackFromEncounter(resource, transfer);
        transfer.put("referringProviderName", TransferProfile.formatCareProviderName(
                asString(transfer.get("referringProviderName")),
                firstNonBlank(
                        extractNestedExtensionValue(resource, EXT_DOCTOR_DETAILS, "license-number"),
                        extractNestedPreferTopThenDetails(resource, EXT_PRACTITIONER_INFO, "license-number"),
                        referringProviderLicense)));
    }

    /**
     * Reads {@code referral-feedback} / {@code counter-referral} from under transfer-details
     * (same shape {@link ReferralFeedbackEncounterPatcher} writes) or top-level if present.
     * Populates {@code referralFeedback} for paper-form preview when HIE already has feedback.
     */
    private void applyReferralFeedbackFromEncounter(JsonNode resource, Map<String, Object> transfer) {
        if (transfer.get("referralFeedback") instanceof Map) {
            return;
        }
        JsonNode feedbackExt = findNestedExtensionNode(resource, EXT_TRANSFER_DETAILS, "referral-feedback");
        if (feedbackExt == null) {
            feedbackExt = findExtensionNode(resource, "referral-feedback");
        }
        JsonNode counterExt = findNestedExtensionNode(resource, EXT_TRANSFER_DETAILS, "counter-referral");
        if (counterExt == null) {
            counterExt = findExtensionNode(resource, "counter-referral");
        }
        if (feedbackExt == null && counterExt == null) {
            return;
        }

        Map<String, Object> summary = new LinkedHashMap<String, Object>();
        String finalDiagnosis = childLeaf(feedbackExt, "final-diagnosis-comment");
        String treatmentGiven = firstNonBlank(
                deepChildLeaf(feedbackExt, "treatment-given", "description"),
                childLeaf(feedbackExt, "treatment-given"));
        String outcomeRaw = firstNonBlank(
                childCodingCode(feedbackExt, "outcome"),
                childCodingDisplay(feedbackExt, "outcome"));
        org.openmrs.module.transferapp.model.ReferralFeedbackOutcome outcome =
                org.openmrs.module.transferapp.model.ReferralFeedbackOutcome.fromStoredValue(outcomeRaw);
        String recommendations = firstNonBlank(
                childLeaf(feedbackExt, "comments"),
                childLeaf(counterExt, "recommendation"));
        String referBackFacility = firstNonBlank(
                deepChildLeaf(counterExt, "referred-back-to", "facility-name"),
                childLeaf(counterExt, "referred-back-to"));
        String referBackFosa = deepChildLeaf(counterExt, "referred-back-to", "fosa-id");

        summary.put("finalDiagnosis", finalDiagnosis);
        summary.put("treatmentGiven", treatmentGiven);
        summary.put("outcome", outcome != null ? outcome.name() : outcomeRaw);
        summary.put("outcomeLabel", outcome != null ? outcome.getLabel() : outcomeRaw);
        summary.put("outcomeHieCode", outcome != null ? outcome.getHieCode() : outcomeRaw);
        summary.put("recommendations", recommendations);
        summary.put("referBackToFacility", referBackFacility);
        summary.put("referBackToFacilityFosaId", referBackFosa);
        summary.put("dateOfAdmissionOrSeen", childLeaf(feedbackExt, "date-of-admission"));
        summary.put("dateOfDischarge", childLeaf(feedbackExt, "date-of-discharge"));
        summary.put("followUpDate", firstNonBlank(
                childLeaf(counterExt, "follow-up-date"),
                childLeaf(feedbackExt, "signed-date"),
                childLeaf(feedbackExt, "date-of-discharge")));
        summary.put("contactPerson", firstNonBlank(
                childLeaf(feedbackExt, "contact-person"),
                childLeaf(counterExt, "contact-person")));
        summary.put("providerName", firstNonBlank(
                childLeaf(feedbackExt, "provider-name"),
                childLeaf(counterExt, "provider-name")));
        summary.put("qualification", firstNonBlank(
                childLeaf(feedbackExt, "qualification"),
                childLeaf(counterExt, "qualification")));
        summary.put("signedDate", firstNonBlank(
                childLeaf(feedbackExt, "signed-date"),
                childLeaf(counterExt, "signed-date")));
        summary.put("signedTime", firstNonBlank(
                childLeaf(feedbackExt, "signed-time"),
                childLeaf(counterExt, "signed-time")));
        summary.put("phone", firstNonBlank(
                childLeaf(feedbackExt, "phone"),
                childLeaf(counterExt, "phone")));
        summary.put("clientName", asString(transfer.get("clientName")));
        summary.put("sex", asString(transfer.get("sex")));
        summary.put("ageOrDob", asString(transfer.get("ageDob")));
        summary.put("transferType", firstNonBlank(
                extractNestedCodingCode(resource, EXT_TRANSFER_DETAILS, "transfer-type"),
                "COUNTER_REFERRAL"));
        summary.put("fromHie", Boolean.TRUE);

        boolean hasContent = !isPlaceholderEmpty(finalDiagnosis)
                || !isPlaceholderEmpty(treatmentGiven)
                || !isPlaceholderEmpty(outcomeRaw)
                || !isPlaceholderEmpty(recommendations)
                || !isPlaceholderEmpty(referBackFacility)
                || !isPlaceholderEmpty(asString(summary.get("dateOfDischarge")))
                || !isPlaceholderEmpty(asString(summary.get("dateOfAdmissionOrSeen")));
        if (hasContent) {
            transfer.put("referralFeedback", summary);
        }
    }

    private String childLeaf(JsonNode parentExt, String childUrl) {
        return leafExtensionValue(findChildExtensionNode(parentExt, childUrl));
    }

    private String deepChildLeaf(JsonNode parentExt, String midUrl, String childUrl) {
        JsonNode mid = findChildExtensionNode(parentExt, midUrl);
        return leafExtensionValue(findChildExtensionNode(mid, childUrl));
    }

    private String childCodingCode(JsonNode parentExt, String childUrl) {
        return codingCode(findChildExtensionNode(parentExt, childUrl));
    }

    private String childCodingDisplay(JsonNode parentExt, String childUrl) {
        JsonNode child = findChildExtensionNode(parentExt, childUrl);
        String display = codingDisplayFromExtension(child);
        if (display != null && display.trim().length() > 0) {
            return display;
        }
        return leafExtensionValue(child);
    }

    /**
     * Last-resort clinical datetime fallbacks after extensions + etransfer form.
     * period.start may equal decision on outbound payloads; period.end is always the
     * +1 month active window and must never be used here.
     */
    private void applyPeriodDatetimeFallbacks(Map<String, Object> transfer, String periodStart, String periodEnd) {
        putIfBlank(transfer, "transferDecisionDatetime", periodStart);
        String admission = asString(transfer.get("admissionDatetime"));
        if (admission.trim().isEmpty()) {
            if (periodEnd.trim().isEmpty() || periodStart.equals(periodEnd)) {
                putIfBlank(transfer, "admissionDatetime", periodStart);
            }
        }
    }

    private void applyTransferFormKind(JsonNode resource, Map<String, Object> transfer) {
        String formKindCode = firstNonBlank(
                extractExtensionCode(resource, EXT_TRANSFER_FORM_KIND),
                extractExtensionDisplay(resource, EXT_TRANSFER_FORM_KIND));
        org.openmrs.module.transferapp.model.TransferFormKind kind =
                org.openmrs.module.transferapp.model.TransferFormKind.fromCodeOrLabel(formKindCode);
        transfer.put("formKind", kind.name());
        transfer.put("formKindCode", kind.getCode());
        transfer.put("formKindDisplay", kind.getDisplay());
        if (kind.isMaternityTransferForm()) {
            transfer.put("formType", "Maternity");
        } else if (kind.isNeonatalTransferForm()) {
            transfer.put("formType", "Neonatal");
        } else {
            transfer.put("formType", "External");
        }
    }

    /**
     * Reads agent-approved / agent-comment / requires-insurance-agent-verification
     * the same way etransfer does for pending and receiving previews.
     */
    private void applyInsuranceAgentVerificationFlags(JsonNode resource, Map<String, Object> transfer) {
        boolean requiresVerification = extractExtensionBooleanTrue(resource, EXT_REQUIRES_INSURANCE_AGENT_VERIFICATION);
        boolean hasAgentApprovedExtension = hasExtension(resource, EXT_AGENT_APPROVED);
        boolean agentApproved = hasAgentApprovedExtension
                && extractExtensionPresentAndNotFalse(resource, EXT_AGENT_APPROVED);
        String agentComment = extractExtensionValue(resource, EXT_AGENT_COMMENT);
        boolean agentRejected = hasAgentApprovedExtension && !agentApproved;
        boolean agentDecisionApproved = hasAgentApprovedExtension && agentApproved;
        boolean needsInsuranceApproval = requiresVerification && !hasAgentApprovedExtension;

        transfer.put("requiresInsuranceAgentVerification", String.valueOf(requiresVerification));
        transfer.put("hasAgentApprovedExtension", String.valueOf(hasAgentApprovedExtension));
        transfer.put("agentApproved", String.valueOf(agentApproved));
        transfer.put("agentComment", agentComment);
        transfer.put("agentRejected", String.valueOf(agentRejected));
        transfer.put("agentDecisionApproved", String.valueOf(agentDecisionApproved));
        transfer.put("needsInsuranceApproval", String.valueOf(needsInsuranceApproval));

        if (agentRejected) {
            transfer.put("status", "Rejected by insurance");
        } else if (agentDecisionApproved) {
            transfer.put("status", "Approved by insurance");
        } else if (needsInsuranceApproval) {
            transfer.put("status", "Awaiting insurance approval");
        }
    }

    private JsonNode findExtensionNode(JsonNode resource, String extensionUrl) {
        JsonNode extensions = resource.get("extension");
        if (extensions == null || !extensions.isArray() || extensionUrl == null) {
            return null;
        }
        String expected = extensionUrl.trim();
        Iterator<JsonNode> extensionIterator = extensions.getElements();
        while (extensionIterator.hasNext()) {
            JsonNode ext = extensionIterator.next();
            String url = textOrDefault(ext.get("url"), "").trim();
            if (url.equals(expected) || url.endsWith("/" + lastUrlSegment(expected))) {
                return ext;
            }
            if (lastUrlSegment(url).equals(lastUrlSegment(expected))) {
                return ext;
            }
        }
        return null;
    }

    private static String lastUrlSegment(String url) {
        if (url == null || url.isEmpty()) {
            return "";
        }
        int slash = url.lastIndexOf('/');
        return slash >= 0 ? url.substring(slash + 1) : url;
    }

    private boolean hasExtension(JsonNode resource, String extensionUrl) {
        return findExtensionNode(resource, extensionUrl) != null;
    }

    private boolean extractExtensionBooleanTrue(JsonNode resource, String extensionUrl) {
        JsonNode ext = findExtensionNode(resource, extensionUrl);
        if (ext == null) {
            return false;
        }
        JsonNode valueBoolean = ext.get("valueBoolean");
        return valueBoolean != null && !valueBoolean.isNull() && valueBoolean.getBooleanValue();
    }

    /**
     * Matches etransfer: extension present and not explicitly false counts as approved.
     */
    private boolean extractExtensionPresentAndNotFalse(JsonNode resource, String extensionUrl) {
        JsonNode ext = findExtensionNode(resource, extensionUrl);
        if (ext == null) {
            return false;
        }
        JsonNode valueBoolean = ext.get("valueBoolean");
        if (valueBoolean == null || valueBoolean.isNull()) {
            return true;
        }
        return valueBoolean.getBooleanValue();
    }

    /**
     * Fills transfer form fields from etransfer-transfer-form only when FHIR/native
     * mapping left them empty. Not all HIE sources include this extension.
     */
    private void applyEtransferFormFallback(Map<String, Object> transfer, JsonNode form) {
        if (form == null || form.isNull()) {
            return;
        }

        putIfBlank(transfer, "province", jsonText(form, "province"));
        putIfBlank(transfer, "district", jsonText(form, "district"));
        putIfBlank(transfer, "hospitalName", jsonText(form, "hospitalName"));
        putIfBlank(transfer, "referringFacilityName", jsonText(form, "referringFacilityName"));
        putIfBlank(transfer, "referringUnit", jsonText(form, "referringUnit"));
        putIfBlank(transfer, "receivingFacility", jsonText(form, "receivingFacility"));
        putIfBlank(transfer, "receivingService", jsonText(form, "receivingService"));
        putIfBlank(transfer, "receivingClinicianPhone", jsonText(form, "receivingClinicianPhone"));
        putIfBlank(transfer, "staffContactedAtReceivingFacility", jsonText(form, "staffContactedName"));
        putIfBlank(transfer, "staffContactPhone", jsonText(form, "staffContactedPhone"));
        putIfBlank(transfer, "callingTime", jsonText(form, "callingTime"));
        putIfBlank(transfer, "ambulanceCalledTime", jsonText(form, "ambulanceCalledTime"));
        putIfBlank(transfer, "departureTime", jsonText(form, "departureFromReferringTime"));

        putIfBlank(transfer, "clientName", jsonText(form, "clientName"));
        putIfBlank(transfer, "serialNumberOrEmrId", jsonText(form, "serialNumberEmr"));
        putIfBlank(transfer, "ageDob", jsonText(form, "ageOrDob"));
        putIfBlank(transfer, "sex", jsonText(form, "sex"));
        putIfBlank(transfer, "caregiverName", jsonText(form, "caregiverName"));
        putIfBlank(transfer, "caregiverTelephone", jsonText(form, "caregiverTelephone"));
        putIfBlank(transfer, "clientTelephone", jsonText(form, "clientTelephone"));
        putIfBlank(transfer, "telephone", firstNonBlank(
                jsonText(form, "caregiverTelephone"),
                jsonText(form, "clientTelephone")));

        putIfBlank(transfer, "providerPhone", jsonText(form, "referringProviderPhone"));
        putIfBlank(transfer, "patientDistrict", jsonText(form, "clientDistrict"));
        putIfBlank(transfer, "patientSector", jsonText(form, "sector"));
        putIfBlank(transfer, "patientCell", jsonText(form, "cell"));
        putIfBlank(transfer, "patientVillage", jsonText(form, "village"));

        putIfBlank(transfer, "admissionDatetime", jsonText(form, "admissionAt"));
        putIfBlank(transfer, "transferDecisionDatetime", jsonText(form, "decisionToTransferAt"));
        putIfBlank(transfer, "reasonForTransfer", jsonText(form, "reasonForTransfer"));
        putIfBlank(transfer, "clinicalPresentation", jsonText(form, "clinicalPresentation"));
        putIfBlank(transfer, "disabilityType", jsonText(form, "disabilityType"));
        putIfBlank(transfer, "diagnosis", jsonText(form, "diagnosis"));
        putIfBlank(transfer, "proceduresAndTreatments", jsonText(form, "proceduresAndTreatments"));
        putIfBlank(transfer, "laboratory", jsonText(form, "laboratory"));
        putIfBlank(transfer, "others", jsonText(form, "othersNotes"));

        putIfBlank(transfer, "referringProviderName", jsonText(form, "referringProviderName"));
        putIfBlank(transfer, "referringProviderQualification", jsonText(form, "referringProviderQualification"));
        putIfBlank(transfer, "formDate", jsonText(form, "referringSignedDate"));
        putIfBlank(transfer, "formTime", jsonText(form, "referringSignedTime"));

        putIfBlank(transfer, "temperature", jsonText(form, "vitalTemp"));
        putIfBlank(transfer, "spo2", jsonText(form, "vitalSpo2"));
        putIfBlank(transfer, "respiratoryRate", jsonText(form, "vitalRr"));
        putIfBlank(transfer, "pulse", jsonText(form, "vitalPulse"));
        putIfBlank(transfer, "bloodPressure", jsonText(form, "vitalBp"));
        putIfBlank(transfer, "weight", jsonText(form, "vitalWeight"));
        putIfBlank(transfer, "height", jsonText(form, "vitalHeight"));
        putIfBlank(transfer, "muac", jsonText(form, "vitalMuac"));
    }

    private void putIfBlank(Map<String, Object> transfer, String key, String value) {
        if (value == null || value.trim().isEmpty()) {
            return;
        }
        Object existing = transfer.get(key);
        if (existing == null || String.valueOf(existing).trim().isEmpty()) {
            transfer.put(key, value);
        }
    }

    private String extractExtensionValue(JsonNode resource, String extensionUrl) {
        return leafExtensionValue(findExtensionNode(resource, extensionUrl));
    }

    private String extractExtensionDisplay(JsonNode resource, String extensionUrl) {
        JsonNode ext = findExtensionNode(resource, extensionUrl);
        String display = codingDisplayFromExtension(ext);
        if (display != null && display.trim().length() > 0) {
            return display;
        }
        return leafExtensionValue(ext);
    }

    private String extractExtensionCode(JsonNode resource, String extensionUrl) {
        return codingCode(findExtensionNode(resource, extensionUrl));
    }

    private String extractExtensionDateTime(JsonNode resource, String extensionUrl) {
        return extractExtensionValue(resource, extensionUrl);
    }

    private String extractNestedExtensionValue(JsonNode resource, String parentUrl, String childUrl) {
        JsonNode nested = findNestedExtensionNode(resource, parentUrl, childUrl);
        return leafExtensionValue(nested);
    }

    private String stripCodePrefix(String value) {
        if (value == null) {
            return "";
        }
        int idx = value.indexOf('#');
        return idx >= 0 && idx + 1 < value.length() ? value.substring(idx + 1).trim() : value.trim();
    }

    private String textOrDefault(JsonNode node, String defaultValue) {
        if (node == null || node.isNull()) {
            return defaultValue;
        }
        String value = node.getTextValue();
        return value == null ? defaultValue : sanitizeFrontendText(value);
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (!isPlaceholderEmpty(value)) {
                return value.trim();
            }
        }
        return "";
    }

    /**
     * Empty optional fields in some HIE samples are encoded as a lone "," (or ".").
     * Treat those as absent so they do not render on the transfer form.
     */
    private boolean isPlaceholderEmpty(String value) {
        if (value == null) {
            return true;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() || ",".equals(trimmed) || ".".equals(trimmed);
    }

    /**
     * Prefer a top-level Encounter extension leaf; fall back to the same URL nested under
     * {@code transfer-details} (outbound External Transfer / sample_transfer_gahini shape).
     */
    private String extractLeafPreferTopThenDetails(JsonNode resource, String extensionUrl) {
        return firstNonBlank(
                extractExtensionValue(resource, extensionUrl),
                extractNestedExtensionValue(resource, EXT_TRANSFER_DETAILS, extensionUrl));
    }

    /**
     * Prefer parent/child nesting at Encounter top level; fall back under {@code transfer-details}.
     */
    private String extractNestedPreferTopThenDetails(JsonNode resource, String parentUrl, String childUrl) {
        return firstNonBlank(
                extractNestedExtensionValue(resource, parentUrl, childUrl),
                extractDeepNestedString(resource, EXT_TRANSFER_DETAILS, parentUrl, childUrl));
    }

    private JsonNode extractEtransferFormNode(JsonNode resource) {
        String raw = extractExtensionValue(resource, EXT_ETRANSFER_FORM);
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        try {
            return new ObjectMapper().readTree(raw);
        } catch (Exception e) {
            return null;
        }
    }

    private String jsonText(JsonNode node, String field) {
        if (node == null || field == null) {
            return "";
        }
        JsonNode valueNode = node.get(field);
        if (valueNode == null || valueNode.isNull()) {
            return "";
        }
        String text = valueNode.getTextValue();
        if (text == null || "null".equalsIgnoreCase(text.trim())) {
            return "";
        }
        return sanitizeFrontendText(text);
    }

    private String codingDisplay(JsonNode codeableConceptNode) {
        if (codeableConceptNode == null || codeableConceptNode.isNull()) {
            return "";
        }

        JsonNode coding = codeableConceptNode.get("coding");
        JsonNode firstCoding = firstArrayElement(coding);
        if (firstCoding != null) {
            String display = textOrDefault(firstCoding.get("display"), "");
            if (!display.trim().isEmpty()) {
                return display;
            }
            return textOrDefault(firstCoding.get("code"), "");
        }
        return textOrDefault(codeableConceptNode.get("text"), "");
    }

    /** Splits "Name - Phone" or "Name / Phone" from receiving-clinician-contact. */
    private String[] parseReceivingClinicianContact(String contact) {
        String[] result = new String[]{"", ""};
        if (contact == null || contact.trim().isEmpty()) {
            return result;
        }
        String normalized = contact.trim();
        if (normalized.contains(" - ")) {
            int sep = normalized.indexOf(" - ");
            result[0] = normalized.substring(0, sep).trim();
            result[1] = normalized.substring(sep + 3).trim();
        } else if (normalized.contains("/")) {
            int sep = normalized.indexOf('/');
            result[0] = normalized.substring(0, sep).trim();
            result[1] = normalized.substring(sep + 1).trim();
        } else {
            result[0] = normalized;
        }
        return result;
    }

    /**
     * Normalizes HIE text values for frontend rendering by removing HTML tags and
     * common attribute remnants, then collapsing whitespace.
     */
    private String sanitizeFrontendText(String input) {
        if (input == null) {
            return "";
        }
        String text = input;

        text = text.replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&amp;", "&")
                .replace("&nbsp;", " ");

        text = text.replaceAll("(?is)<[^>]*>", " ");
        text = text.replaceAll("(?i)\\b[a-zA-Z_:][-a-zA-Z0-9_:.]*\\s*=\\s*\"[^\"]*\"\\s*>?", " ");
        text = text.replaceAll("(?i)\\b[a-zA-Z_:][-a-zA-Z0-9_:.]*\\s*=\\s*'[^']*'\\s*>?", " ");
        text = text.replaceAll("\\s+", " ").trim();
        text = normalizeIsoDateTimeForFrontend(text);
        return text;
    }

    /**
     * Converts ISO datetime values from HIE to frontend-friendly format:
     * - removes timezone suffix (+02:00 or Z)
     * - replaces "T" with a space
     * Non-datetime strings are returned unchanged.
     */
    private String normalizeIsoDateTimeForFrontend(String value) {
        if (value == null) {
            return "";
        }
        String text = value.trim();
        if (text.matches("^[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}(?:[+-][0-9]{2}:[0-9]{2}|Z)?$")) {
            text = text.replaceAll("([T ][0-9]{2}:[0-9]{2}:[0-9]{2})(?:[+-][0-9]{2}:[0-9]{2}|Z)$", "$1");
            text = text.replace("T", " ");
        }
        return text;
    }

    private void parseVitalSignsIntoTransfer(String vitals, Map<String, Object> transfer) {
        if (vitals == null || vitals.trim().isEmpty()) {
            return;
        }
        String[] segments = vitals.split(",");
        for (String rawSegment : segments) {
            if (rawSegment == null) {
                continue;
            }
            String segment = rawSegment.trim();
            int idx = segment.indexOf(':');
            if (idx <= 0) {
                continue;
            }

            String key = segment.substring(0, idx).trim().toLowerCase();
            String value = segment.substring(idx + 1).trim();
            if (value.endsWith("%")) {
                value = value.substring(0, value.length() - 1).trim();
            }

            if ("t".equals(key) || "temp".equals(key) || "temperature".equals(key)) {
                transfer.put("temperature", value);
            } else if ("spo2".equals(key) || "sp02".equals(key) || "o2sat".equals(key)) {
                transfer.put("spo2", value);
            } else if ("rr".equals(key) || "respiratory rate".equals(key) || "respiratoryrate".equals(key)) {
                transfer.put("respiratoryRate", value);
            } else if ("pulse".equals(key) || "pr".equals(key) || "heart rate".equals(key) || "heartrate".equals(key)) {
                transfer.put("pulse", value);
            } else if ("bp".equals(key) || "blood pressure".equals(key) || "bloodpressure".equals(key)) {
                transfer.put("bloodPressure", value);
            } else if ("weight".equals(key)) {
                transfer.put("weight", value);
            } else if ("height".equals(key)) {
                transfer.put("height", value);
            } else if ("muac".equals(key)) {
                transfer.put("muac", value);
            }
        }
    }

    private JsonNode firstArrayElement(JsonNode node) {
        if (node == null || !node.isArray()) {
            return null;
        }
        Iterator<JsonNode> iterator = node.getElements();
        if (iterator.hasNext()) {
            return iterator.next();
        }
        return null;
    }

    private String asString(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String formatAgeOrDob(String age, String dob) {
        String ageText = age == null ? "" : age.trim();
        String dobText = dob == null ? "" : dob.trim();
        if (!ageText.isEmpty() && !dobText.isEmpty()) {
            return ageText + " (" + dobText + ")";
        }
        return firstNonBlank(ageText, dobText);
    }

    private String toDateOnly(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "";
        }
        String text = value.trim();
        int tIndex = text.indexOf('T');
        if (tIndex > 0) {
            return text.substring(0, tIndex);
        }
        if (text.length() >= 10) {
            return text.substring(0, 10);
        }
        return text;
    }

    private String toTimeOnly(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "";
        }
        String text = value.trim();
        int tIndex = text.indexOf('T');
        if (tIndex < 0 || tIndex + 1 >= text.length()) {
            return "";
        }
        String time = text.substring(tIndex + 1);
        int plus = time.indexOf('+');
        int zIndex = time.indexOf('Z');
        int cut = time.length();
        if (plus > 0) {
            cut = Math.min(cut, plus);
        }
        if (zIndex > 0) {
            cut = Math.min(cut, zIndex);
        }
        time = time.substring(0, cut);
        if (time.length() >= 8) {
            return time.substring(0, 8);
        }
        if (time.length() >= 5) {
            return time.substring(0, 5);
        }
        return time;
    }

    /**
     * Classifies FHIR transfer-type code/display into Emergency / Not-Emergency / Follow-up flags.
     * Prefers the coding code (e.g. {@code not-emergency}) over display text. Display labels such as
     * "Not emergency" must not be treated as Emergency: naive {@code contains("emergency")} matching
     * wrongly matches that phrase.
     */
    private void applyTransferTypeFlags(Map<String, Object> transfer, String code, String display) {
        String kind = classifyTransferType(code);
        if (kind == null) {
            kind = classifyTransferType(display);
        }
        transfer.put("isEmergency", String.valueOf("EMERGENCY".equals(kind)));
        transfer.put("isNonEmergency", String.valueOf("NOT_EMERGENCY".equals(kind)));
        transfer.put("isFollowUp", String.valueOf("FOLLOW_UP".equals(kind)));
    }

    /**
     * @return EMERGENCY, NOT_EMERGENCY, FOLLOW_UP, or null when unrecognized/blank
     */
    public static String classifyTransferType(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        String normalized = raw.trim().toUpperCase()
                .replace('-', '_')
                .replace(' ', '_')
                .replaceAll("_+", "_");
        // New workflow transfer-types are not urgency flags.
        if ("NORMAL_TRANSFER".equals(normalized) || "REFERRAL".equals(normalized)
                || "COUNTER_REFERRAL".equals(normalized) || "COUNTERREFERRAL".equals(normalized)) {
            return null;
        }
        if ("NOT_EMERGENCY".equals(normalized) || "NON_EMERGENCY".equals(normalized)
                || "NOT_EMERGENT".equals(normalized) || "NON_EMERGENT".equals(normalized)
                || normalized.contains("NOT_EMERGENCY") || normalized.contains("NON_EMERGENCY")
                || normalized.contains("NOT_EMERG") || normalized.contains("NON_EMERG")) {
            return "NOT_EMERGENCY";
        }
        if ("FOLLOW_UP".equals(normalized) || "FOLLOWUP".equals(normalized)
                || normalized.contains("FOLLOW_UP") || normalized.contains("FOLLOWUP")
                || normalized.startsWith("FOLLOW")) {
            return "FOLLOW_UP";
        }
        if ("EMERGENCY".equals(normalized) || "EMER".equals(normalized)
                || normalized.equals("EMERG")) {
            return "EMERGENCY";
        }
        // Exact-ish emergency only — avoid matching "not emergency" / "non-emergency".
        if (normalized.contains("EMERGENCY") || normalized.contains("EMERG")) {
            if (normalized.contains("NOT_") || normalized.contains("NON_")
                    || normalized.startsWith("NOT") || normalized.startsWith("NON")) {
                return "NOT_EMERGENCY";
            }
            return "EMERGENCY";
        }
        return null;
    }

    private void applyTransportFields(JsonNode resource, Map<String, Object> transfer) {
        String legacyTransport = extractExtensionDisplay(resource, EXT_TRANSPORT_TYPE);
        String nestedTransportCode = extractDeepNestedCodingCode(resource, EXT_TRANSFER_DETAILS, "transport",
                "transport-type");
        String nestedTransportDisplay = extractDeepNestedCodingDisplay(resource, EXT_TRANSFER_DETAILS, "transport",
                "transport-type");
        String transportType = firstNonBlank(nestedTransportDisplay, nestedTransportCode, legacyTransport);
        transfer.put("transportType", transportType);
        String transportTypeLower = transportType == null ? "" : transportType.toLowerCase();
        boolean ambulance = transportTypeLower.contains("ambulance");
        transfer.put("isAmbulanceTransport", String.valueOf(ambulance));
        transfer.put("otherTransportType", ambulance ? "" : firstNonBlank(transportType));
        transfer.put("isNaTransport", String.valueOf(
                "na".equals(transportTypeLower) || "n/a".equals(transportTypeLower)));

        String nestedProviderFosa = extractDeepNestedString(resource, EXT_TRANSFER_DETAILS, "transport", "fosa-id");
        String nestedProviderName = extractDeepNestedString(resource, EXT_TRANSFER_DETAILS, "transport", "facility-name");
        String transportComments = extractDeepNestedString(resource, EXT_TRANSFER_DETAILS, "transport",
                "transport-comments");
        transfer.put("ambulanceProviderFosaId", firstNonBlank(
                nestedProviderFosa,
                extractNestedExtensionValue(resource,
                        TransferAppConstants.EXT_AMBULANCE_PROVIDER_FACILITY,
                        TransferAppConstants.EXT_AMBULANCE_PROVIDER_FOSA_ID)));
        transfer.put("ambulanceProviderName", firstNonBlank(
                nestedProviderName,
                extractNestedExtensionValue(resource,
                        TransferAppConstants.EXT_AMBULANCE_PROVIDER_FACILITY,
                        TransferAppConstants.EXT_AMBULANCE_PROVIDER_NAME)));
        if (!isPlaceholderEmpty(transportComments)) {
            transfer.put("transportComments", transportComments.trim());
            if (asString(transfer.get("others")).trim().isEmpty() && !ambulance) {
                transfer.put("others", transportComments.trim());
            }
        }
    }

    private String referenceDisplay(JsonNode referenceNode) {
        if (referenceNode == null || referenceNode.isNull()) {
            return "";
        }
        return textOrDefault(referenceNode.get("display"), "");
    }

    private String extractExtensionLeafValue(JsonNode resource, String extensionUrl) {
        return leafExtensionValue(findExtensionNode(resource, extensionUrl));
    }

    private String extractNestedCodingCode(JsonNode resource, String parentUrl, String childUrl) {
        JsonNode nested = findNestedExtensionNode(resource, parentUrl, childUrl);
        return codingCode(nested);
    }

    private String extractNestedCodingDisplay(JsonNode resource, String parentUrl, String childUrl) {
        JsonNode nested = findNestedExtensionNode(resource, parentUrl, childUrl);
        String display = codingDisplayFromExtension(nested);
        if (display != null && display.trim().length() > 0) {
            return display;
        }
        return leafExtensionValue(nested);
    }

    private String extractDeepNestedString(JsonNode resource, String parentUrl, String midUrl, String childUrl) {
        JsonNode mid = findNestedExtensionNode(resource, parentUrl, midUrl);
        if (mid == null) {
            return "";
        }
        JsonNode child = findChildExtensionNode(mid, childUrl);
        return leafExtensionValue(child);
    }

    private String extractDeepNestedCodingCode(JsonNode resource, String parentUrl, String midUrl, String childUrl) {
        JsonNode mid = findNestedExtensionNode(resource, parentUrl, midUrl);
        if (mid == null) {
            return "";
        }
        return codingCode(findChildExtensionNode(mid, childUrl));
    }

    private String extractDeepNestedCodingDisplay(JsonNode resource, String parentUrl, String midUrl, String childUrl) {
        JsonNode mid = findNestedExtensionNode(resource, parentUrl, midUrl);
        if (mid == null) {
            return "";
        }
        JsonNode child = findChildExtensionNode(mid, childUrl);
        String display = codingDisplayFromExtension(child);
        if (display != null && display.trim().length() > 0) {
            return display;
        }
        return leafExtensionValue(child);
    }

    private JsonNode findNestedExtensionNode(JsonNode resource, String parentUrl, String childUrl) {
        JsonNode parent = findExtensionNode(resource, parentUrl);
        return findChildExtensionNode(parent, childUrl);
    }

    private JsonNode findChildExtensionNode(JsonNode parentExt, String childUrl) {
        if (parentExt == null || childUrl == null) {
            return null;
        }
        JsonNode nestedExtensions = parentExt.get("extension");
        if (nestedExtensions == null || !nestedExtensions.isArray()) {
            return null;
        }
        String expected = childUrl.trim();
        Iterator<JsonNode> nestedIterator = nestedExtensions.getElements();
        while (nestedIterator.hasNext()) {
            JsonNode nested = nestedIterator.next();
            String url = textOrDefault(nested.get("url"), "").trim();
            if (url.equals(expected) || lastUrlSegment(url).equals(lastUrlSegment(expected))) {
                return nested;
            }
        }
        return null;
    }

    private String leafExtensionValue(JsonNode ext) {
        if (ext == null || ext.isNull()) {
            return "";
        }
        JsonNode valueString = ext.get("valueString");
        if (valueString != null && !valueString.isNull()) {
            return textOrDefault(valueString, "");
        }
        JsonNode valueDateTime = ext.get("valueDateTime");
        if (valueDateTime != null && !valueDateTime.isNull()) {
            return textOrDefault(valueDateTime, "");
        }
        JsonNode valueDate = ext.get("valueDate");
        if (valueDate != null && !valueDate.isNull()) {
            return textOrDefault(valueDate, "");
        }
        String codingDisplay = codingDisplayFromExtension(ext);
        if (codingDisplay != null && codingDisplay.trim().length() > 0) {
            return codingDisplay;
        }
        String codingCode = codingCode(ext);
        if (codingCode != null && codingCode.trim().length() > 0) {
            return codingCode;
        }
        JsonNode valueReference = ext.get("valueReference");
        if (valueReference != null && !valueReference.isNull()) {
            return textOrDefault(valueReference.get("display"),
                    textOrDefault(valueReference.get("reference"), ""));
        }
        return "";
    }

    private String codingCode(JsonNode ext) {
        if (ext == null || ext.isNull()) {
            return "";
        }
        JsonNode valueCoding = ext.get("valueCoding");
        if (valueCoding != null && !valueCoding.isNull()) {
            return textOrDefault(valueCoding.get("code"), "");
        }
        JsonNode valueCodeableConcept = ext.get("valueCodeableConcept");
        if (valueCodeableConcept != null && !valueCodeableConcept.isNull()) {
            JsonNode firstCoding = firstArrayElement(valueCodeableConcept.get("coding"));
            if (firstCoding != null) {
                return textOrDefault(firstCoding.get("code"), "");
            }
        }
        return "";
    }

    private String codingDisplayFromExtension(JsonNode ext) {
        if (ext == null || ext.isNull()) {
            return "";
        }
        JsonNode valueCoding = ext.get("valueCoding");
        if (valueCoding != null && !valueCoding.isNull()) {
            return firstNonBlank(
                    textOrDefault(valueCoding.get("display"), ""),
                    textOrDefault(valueCoding.get("code"), ""));
        }
        JsonNode valueCodeableConcept = ext.get("valueCodeableConcept");
        if (valueCodeableConcept != null && !valueCodeableConcept.isNull()) {
            JsonNode firstCoding = firstArrayElement(valueCodeableConcept.get("coding"));
            if (firstCoding != null) {
                return firstNonBlank(
                        textOrDefault(firstCoding.get("display"), ""),
                        textOrDefault(firstCoding.get("code"), ""));
            }
            return textOrDefault(valueCodeableConcept.get("text"), "");
        }
        return "";
    }
}
