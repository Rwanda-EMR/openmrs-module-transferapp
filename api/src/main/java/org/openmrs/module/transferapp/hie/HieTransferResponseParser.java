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
                extractNestedExtensionValue(resource, EXT_PATIENT_DEMOGRAPHICS, "name"),
                asString(transfer.get("clientName"))));
        transfer.put("serialNumberOrEmrId", firstNonBlank(
                extractNestedExtensionValue(resource, EXT_PATIENT_DEMOGRAPHICS, "serial-number"),
                asString(transfer.get("serialNumberOrEmrId"))));
        String patientDob = extractNestedExtensionValue(resource, EXT_PATIENT_DEMOGRAPHICS, "dob");
        String patientAge = extractNestedExtensionValue(resource, EXT_PATIENT_DEMOGRAPHICS, "age");
        transfer.put("ageDob", formatAgeOrDob(patientAge, patientDob));
        transfer.put("sex", extractNestedExtensionValue(resource, EXT_PATIENT_DEMOGRAPHICS, "gender"));
        String patientPhone = extractNestedExtensionValue(resource, EXT_PATIENT_DEMOGRAPHICS, "phone");
        transfer.put("clientTelephone", patientPhone);

        transfer.put("caregiverName", extractNestedExtensionValue(resource, EXT_CAREGIVER_INFO, "name"));
        String caregiverPhone = extractNestedExtensionValue(resource, EXT_CAREGIVER_INFO, "phone");
        transfer.put("caregiverTelephone", caregiverPhone);
        transfer.put("telephone", firstNonBlank(patientPhone, caregiverPhone));

        transfer.put("province", stripCodePrefix(extractExtensionValue(resource, EXT_RECEIVING_PROVINCE)));
        transfer.put("district", stripCodePrefix(extractExtensionValue(resource, EXT_RECEIVING_DISTRICT)));

        transfer.put("patientDistrict", stripCodePrefix(extractNestedExtensionValue(resource, EXT_PATIENT_ADDRESS, "district")));
        transfer.put("patientSector", stripCodePrefix(extractNestedExtensionValue(resource, EXT_PATIENT_ADDRESS, "sector")));
        transfer.put("patientCell", stripCodePrefix(extractNestedExtensionValue(resource, EXT_PATIENT_ADDRESS, "cell")));
        transfer.put("patientVillage", stripCodePrefix(extractNestedExtensionValue(resource, EXT_PATIENT_ADDRESS, "village")));

        JsonNode period = resource.get("period");
        String periodStart = "";
        if (period != null && !period.isNull()) {
            periodStart = textOrDefault(period.get("start"), "");
        }
		transfer.put("admissionDatetime", firstNonBlank(
				extractExtensionDateTime(resource, EXT_ADMISSION_DATETIME),
				extractNestedExtensionValue(resource, EXT_TRANSFER_FLAGS, "admission-date"),
				periodStart));
		transfer.put("transferDecisionDatetime", firstNonBlank(
				extractExtensionDateTime(resource, EXT_DECISION_TO_TRANSFER_DATETIME),
				periodStart));
		transfer.put("departureTime", extractExtensionDateTime(resource, EXT_DEPARTURE_TIME));
        transfer.put("ambulanceCalledTime", extractExtensionDateTime(resource, EXT_AMBULANCE_CALL_TIME));
        transfer.put("callingTime", firstNonBlank(
                extractExtensionDateTime(resource, EXT_CALLING_TIME),
                extractNestedExtensionValue(resource, EXT_RECEIVING_CLINICIAN_CONTACT, "calling-time")));

        String transferType = extractExtensionDisplay(resource, EXT_TRANSFER_TYPE);
        transfer.put("transferType", transferType);
        String transferTypeLower = transferType.toLowerCase();
        transfer.put("isEmergency", String.valueOf(transferTypeLower.contains("emergency") && !transferTypeLower.contains("non")));
        transfer.put("isNonEmergency", String.valueOf(transferTypeLower.contains("non-emergency") || transferTypeLower.contains("non emergency")));
        transfer.put("isFollowUp", String.valueOf(transferTypeLower.contains("follow")));

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
        transfer.put("referringFacilityName", originDisplay);
        transfer.put("hospitalName", destinationDisplay);
        transfer.put("receivingFacility", destinationDisplay);
        transfer.put("referringUnit", firstNonBlank(
                extractExtensionValue(resource, EXT_REFERRING_DEPARTMENT),
                admitSourceDisplay));
        transfer.put("receivingService", firstNonBlank(
                extractExtensionValue(resource, EXT_RECEIVING_DEPARTMENT),
                serviceTypeDisplay,
                dischargeDispositionDisplay));

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

        transfer.put("reasonForTransfer", firstNonBlank(
                extractNestedExtensionValue(resource, EXT_CLINICAL_PRESENTATION, "immediate-condition"),
                extractNestedExtensionValue(resource, EXT_TRANSFER_FLAGS, "consultation-motif"),
                extractExtensionValue(resource, EXT_CLINICAL_PRESENTATION),
                reasonCodeText));
        transfer.put("clinicalPresentation", firstNonBlank(
                extractNestedExtensionValue(resource, EXT_CLINICAL_PRESENTATION, "presentation"),
                extractExtensionValue(resource, EXT_CLINICAL_PRESENTATION),
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
                        extractNestedExtensionValue(resource, EXT_PRACTITIONER_INFO, "name"),
                        referringProviderName),
                firstNonBlank(
                        extractNestedExtensionValue(resource, EXT_PRACTITIONER_INFO, "license-number"),
                        referringProviderLicense)));
        transfer.put("referringProviderQualification", extractNestedExtensionValue(resource, EXT_PRACTITIONER_INFO, "qualification"));
        transfer.put("providerPhone", extractNestedExtensionValue(resource, EXT_PRACTITIONER_INFO, "phone"));
        transfer.put("formDate", firstNonBlank(
                extractNestedExtensionValue(resource, EXT_PRACTITIONER_INFO, "signed-date"),
                toDateOnly(periodStart)));
        transfer.put("formTime", firstNonBlank(
                extractNestedExtensionValue(resource, EXT_PRACTITIONER_INFO, "signed-time"),
                toTimeOnly(periodStart)));

        String transportType = extractExtensionDisplay(resource, EXT_TRANSPORT_TYPE);
        transfer.put("transportType", transportType);
        String transportTypeLower = transportType.toLowerCase();
        transfer.put("isAmbulanceTransport", String.valueOf(transportTypeLower.contains("ambulance")));
        transfer.put("otherTransportType", transportTypeLower.contains("ambulance") ? "" : transportType);
        transfer.put("ambulanceProviderFosaId", extractNestedExtensionValue(resource,
                TransferAppConstants.EXT_AMBULANCE_PROVIDER_FACILITY,
                TransferAppConstants.EXT_AMBULANCE_PROVIDER_FOSA_ID));
        transfer.put("ambulanceProviderName", extractNestedExtensionValue(resource,
                TransferAppConstants.EXT_AMBULANCE_PROVIDER_FACILITY,
                TransferAppConstants.EXT_AMBULANCE_PROVIDER_NAME));

        String insurance = extractExtensionDisplay(resource, EXT_INSURANCE_TYPE);
        transfer.put("healthInsurance", insurance);
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

        transfer.put("laboratory", extractExtensionValue(resource, EXT_LAB_RESULTS));
        transfer.put("others", firstNonBlank(
                extractExtensionValue(resource, EXT_OTHERS),
                extractExtensionValue(resource, EXT_ADDITIONAL_NOTES)));
        transfer.put("proceduresAndTreatments", firstNonBlank(
                extractExtensionValue(resource, EXT_PROCEDURES_AND_TREATMENTS),
                extractNestedExtensionValue(resource, EXT_TRANSFER_FLAGS, "prescriptions")));
        String vitals = extractExtensionValue(resource, EXT_VITAL_SIGNS);
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
        transfer.put("referringProviderName", TransferProfile.formatCareProviderName(
                asString(transfer.get("referringProviderName")),
                firstNonBlank(
                        extractNestedExtensionValue(resource, EXT_PRACTITIONER_INFO, "license-number"),
                        referringProviderLicense)));
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
        JsonNode extensions = resource.get("extension");
        if (extensions == null || !extensions.isArray()) {
            return "";
        }

        Iterator<JsonNode> extensionIterator = extensions.getElements();
        while (extensionIterator.hasNext()) {
            JsonNode ext = extensionIterator.next();
            if (extensionUrl.equals(textOrDefault(ext.get("url"), ""))) {
                JsonNode valueString = ext.get("valueString");
                if (valueString != null && !valueString.isNull()) {
                    return textOrDefault(valueString, "");
                }

                JsonNode valueDateTime = ext.get("valueDateTime");
                if (valueDateTime != null && !valueDateTime.isNull()) {
                    return textOrDefault(valueDateTime, "");
                }

                JsonNode valueCodeableConcept = ext.get("valueCodeableConcept");
                if (valueCodeableConcept != null && !valueCodeableConcept.isNull()) {
                    JsonNode coding = valueCodeableConcept.get("coding");
                    JsonNode firstCoding = firstArrayElement(coding);
                    if (firstCoding != null) {
                        String display = textOrDefault(firstCoding.get("display"), "");
                        if (!display.trim().isEmpty()) {
                            return display;
                        }
                    }
                    return textOrDefault(valueCodeableConcept.get("text"), "");
                }
            }
        }
        return "";
    }

    private String extractExtensionDisplay(JsonNode resource, String extensionUrl) {
        return extractExtensionValue(resource, extensionUrl);
    }

    private String extractExtensionCode(JsonNode resource, String extensionUrl) {
        JsonNode extensions = resource.get("extension");
        if (extensions == null || !extensions.isArray() || extensionUrl == null) {
            return "";
        }
        Iterator<JsonNode> extensionIterator = extensions.getElements();
        while (extensionIterator.hasNext()) {
            JsonNode ext = extensionIterator.next();
            if (!extensionUrl.equals(textOrDefault(ext.get("url"), ""))) {
                continue;
            }
            JsonNode valueCodeableConcept = ext.get("valueCodeableConcept");
            if (valueCodeableConcept == null || valueCodeableConcept.isNull()) {
                continue;
            }
            JsonNode firstCoding = firstArrayElement(valueCodeableConcept.get("coding"));
            if (firstCoding != null) {
                String code = textOrDefault(firstCoding.get("code"), "");
                if (!code.trim().isEmpty()) {
                    return code;
                }
            }
        }
        return "";
    }

    private String extractExtensionDateTime(JsonNode resource, String extensionUrl) {
        return extractExtensionValue(resource, extensionUrl);
    }

    private String extractNestedExtensionValue(JsonNode resource, String parentUrl, String childUrl) {
        JsonNode extensions = resource.get("extension");
        if (extensions == null || !extensions.isArray()) {
            return "";
        }

        Iterator<JsonNode> extensionIterator = extensions.getElements();
        while (extensionIterator.hasNext()) {
            JsonNode ext = extensionIterator.next();
            if (!parentUrl.equals(textOrDefault(ext.get("url"), ""))) {
                continue;
            }

            JsonNode nestedExtensions = ext.get("extension");
            if (nestedExtensions == null || !nestedExtensions.isArray()) {
                continue;
            }

            Iterator<JsonNode> nestedIterator = nestedExtensions.getElements();
            while (nestedIterator.hasNext()) {
                JsonNode nested = nestedIterator.next();
                if (!childUrl.equals(textOrDefault(nested.get("url"), ""))) {
                    continue;
                }

                JsonNode valueString = nested.get("valueString");
                if (valueString != null && !valueString.isNull()) {
                    return textOrDefault(valueString, "");
                }

                JsonNode valueDateTime = nested.get("valueDateTime");
                if (valueDateTime != null && !valueDateTime.isNull()) {
                    return textOrDefault(valueDateTime, "");
                }
            }
        }
        return "";
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
            if (value != null && value.trim().length() > 0) {
                return value;
            }
        }
        return "";
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
}
