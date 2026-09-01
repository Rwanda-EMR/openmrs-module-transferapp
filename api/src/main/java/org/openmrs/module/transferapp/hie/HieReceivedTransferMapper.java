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
import org.openmrs.Patient;
import org.openmrs.module.transferapp.TransferAppConstants;
import org.openmrs.module.transferapp.api.impl.PatientInsuranceServiceImpl;
import org.openmrs.module.transferapp.model.Transfer;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

/**
 * Maps a parsed HIE transfer payload into a {@link Transfer} entity for inbound storage.
 */
public class HieReceivedTransferMapper {

	public Transfer mapToTransfer(Patient patient, Map<String, Object> hieData, String currentReceivingFacilityName) {
		Transfer transfer = new Transfer();
		transfer.setPatient(patient);
		transfer.setReceivedFromHie(true);
		transfer.setHieSent(false);
		transfer.setHieTransferId(truncate(stringValue(hieData.get("id")), 64));
		transfer.setFormKind(org.openmrs.module.transferapp.model.TransferFormKind.fromCodeOrLabel(
				firstNonBlank(stringValue(hieData.get("formKindCode")), stringValue(hieData.get("formKind")))));

		transfer.setSendingFacility(firstNonBlank(
				stringValue(hieData.get("origin")),
				stringValue(hieData.get("referringFacilityName"))));
		transfer.setReferringUnit(stringValue(hieData.get("referringUnit")));
		transfer.setReceivingFacilityCode(truncate(firstNonBlank(
				stringValue(hieData.get("destination")),
				stringValue(hieData.get("receivingFacility")),
				currentReceivingFacilityName), 64));
		transfer.setReceivingProvince(truncate(stringValue(hieData.get("province")), 120));
		transfer.setReceivingDistrict(truncate(stringValue(hieData.get("district")), 120));
		transfer.setReceivingService(truncate(stringValue(hieData.get("receivingService")), 255));

		transfer.setStaffContactedName(truncate(firstNonBlank(
				stringValue(hieData.get("staffContactedAtReceivingFacility")),
				stringValue(hieData.get("referringProviderName"))), 255));
		transfer.setStaffContactedPhone(truncate(firstNonBlank(
				stringValue(hieData.get("staffContactPhone")),
				stringValue(hieData.get("receivingClinicianPhone")),
				stringValue(hieData.get("providerPhone"))), 64));

		transfer.setDecisionToTransferAt(parseDateTime(stringValue(hieData.get("transferDecisionDatetime"))));
		transfer.setAdmissionAt(parseDateTime(stringValue(hieData.get("admissionDatetime"))));
		transfer.setCallingTime(truncate(extractTime(stringValue(hieData.get("callingTime"))), 8));

		transfer.setTransferType(resolveTransferType(hieData));
		if ("EMERGENCY".equals(transfer.getTransferType())) {
			transfer.setAmbulanceCallTime(truncate(extractTime(stringValue(hieData.get("ambulanceCalledTime"))), 8));
			transfer.setDepartRefTime(truncate(extractTime(stringValue(hieData.get("departureTime"))), 8));
		}
		applyTransport(transfer, hieData);
		transfer.setAmbulanceProviderFosaId(truncate(stringValue(hieData.get("ambulanceProviderFosaId")), 64));
		transfer.setAmbulanceProviderName(truncate(stringValue(hieData.get("ambulanceProviderName")), 255));

		transfer.setReasonForTransfer(StringUtils.trimToNull(stringValue(hieData.get("reasonForTransfer"))));
		transfer.setClinicalPresentation(StringUtils.trimToNull(stringValue(hieData.get("clinicalPresentation"))));
		transfer.setDisabilityType(truncate(stringValue(hieData.get("disabilityType")), 255));
		transfer.setDiagnosis(truncate(StringUtils.trimToNull(stringValue(hieData.get("diagnosis"))), 65535));
		transfer.setLaboratory(StringUtils.trimToNull(stringValue(hieData.get("laboratory"))));
		transfer.setProceduresTreatments(StringUtils.trimToNull(stringValue(hieData.get("proceduresAndTreatments"))));
		transfer.setOtherNotes(StringUtils.trimToNull(firstNonBlank(
				stringValue(hieData.get("others")),
				stringValue(hieData.get("othersNotes")))));

		transfer.setVitalTemp(truncate(stringValue(hieData.get("temperature")), 32));
		transfer.setVitalSpo2(truncate(stringValue(hieData.get("spo2")), 32));
		transfer.setVitalRr(truncate(stringValue(hieData.get("respiratoryRate")), 32));
		transfer.setVitalPulse(truncate(stringValue(hieData.get("pulse")), 32));
		transfer.setVitalBp(truncate(stringValue(hieData.get("bloodPressure")), 32));
		transfer.setVitalWt(truncate(stringValue(hieData.get("weight")), 32));
		transfer.setVitalHt(truncate(stringValue(hieData.get("height")), 32));
		transfer.setVitalMuac(truncate(stringValue(hieData.get("muac")), 32));

		transfer.setClientName(truncate(stringValue(hieData.get("clientName")), 255));
		transfer.setEmrId(truncate(firstNonBlank(
				stringValue(hieData.get("serialNumberOrEmrId")),
				stringValue(hieData.get("subject"))), 64));
		transfer.setClientTelephone(truncate(firstNonBlank(
				stringValue(hieData.get("clientTelephone")),
				stringValue(hieData.get("providerPhone"))), 64));
		transfer.setAgeOrDob(truncate(stringValue(hieData.get("ageDob")), 64));
		transfer.setSex(truncate(stringValue(hieData.get("sex")), 20));

		transfer.setCaregiverName(truncate(stringValue(hieData.get("caregiverName")), 255));
		transfer.setCaregiverTelephone(truncate(firstNonBlank(
				stringValue(hieData.get("caregiverTelephone")),
				stringValue(hieData.get("telephone"))), 64));
		transfer.setClientDistrict(truncate(stringValue(hieData.get("patientDistrict")), 120));
		transfer.setSector(truncate(stringValue(hieData.get("patientSector")), 120));
		transfer.setCell(truncate(stringValue(hieData.get("patientCell")), 120));
		transfer.setVillage(truncate(stringValue(hieData.get("patientVillage")), 120));

		transfer.setReferringProviderName(truncate(stringValue(hieData.get("referringProviderName")), 255));
		transfer.setProviderQualification(truncate(stringValue(hieData.get("referringProviderQualification")), 255));
		transfer.setProviderPhone(truncate(stringValue(hieData.get("providerPhone")), 64));
		transfer.setSignedDate(parseDateOnly(firstNonBlank(
				stringValue(hieData.get("formDate")),
				stringValue(hieData.get("referringSignedDate")))));
		transfer.setSignedTime(truncate(extractTime(firstNonBlank(
				stringValue(hieData.get("formTime")),
				stringValue(hieData.get("referringSignedTime")))), 8));
		applyInsurance(transfer, hieData);
		return transfer;
	}

	private static void applyTransport(Transfer transfer, Map<String, Object> hieData) {
		if (isTrue(hieData.get("isAmbulanceTransport"))) {
			transfer.setTransportType("AMBULANCE");
			return;
		}
		if (isTrue(hieData.get("isNaTransport"))) {
			transfer.setTransportType("NA");
			return;
		}
		String transportType = stringValue(hieData.get("transportType"));
		if (StringUtils.isNotBlank(transportType)) {
			if (transportType.toLowerCase().contains("ambulance")) {
				transfer.setTransportType("AMBULANCE");
			}
			else if (transportType.toLowerCase().contains("n/a") || transportType.toLowerCase().contains("na")) {
				transfer.setTransportType("NA");
			}
			else {
				transfer.setTransportType("OTHER");
				transfer.setTransportOther(truncate(firstNonBlank(
						stringValue(hieData.get("otherTransportType")),
						transportType), 255));
			}
		}
	}

	private static void applyInsurance(Transfer transfer, Map<String, Object> hieData) {
		if (isTrue(hieData.get("isCbhiInsurance"))) {
			transfer.setHealthInsuranceType(TransferAppConstants.HEALTH_INSURANCE_CBHI);
			return;
		}
		if (isTrue(hieData.get("isRssbInsurance"))) {
			transfer.setHealthInsuranceType(TransferAppConstants.HEALTH_INSURANCE_RSSB);
			return;
		}
		if (isTrue(hieData.get("isMmiInsurance"))) {
			transfer.setHealthInsuranceType(TransferAppConstants.HEALTH_INSURANCE_MMI);
			return;
		}
		if (isTrue(hieData.get("isNoInsurance"))) {
			transfer.setHealthInsuranceType(TransferAppConstants.HEALTH_INSURANCE_NONE);
			return;
		}
		String insurance = stringValue(hieData.get("healthInsurance"));
		String byDisplay = PatientInsuranceServiceImpl.matchCategoryByDisplayName(insurance);
		if (TransferAppConstants.HEALTH_INSURANCE_NONE.equals(byDisplay)) {
			transfer.setHealthInsuranceType(TransferAppConstants.HEALTH_INSURANCE_NONE);
			return;
		}
		if (TransferAppConstants.HEALTH_INSURANCE_CBHI.equals(byDisplay)
				|| TransferAppConstants.HEALTH_INSURANCE_RSSB.equals(byDisplay)
				|| TransferAppConstants.HEALTH_INSURANCE_MMI.equals(byDisplay)) {
			transfer.setHealthInsuranceType(byDisplay);
			return;
		}
		if (StringUtils.isNotBlank(insurance)) {
			transfer.setHealthInsuranceType(TransferAppConstants.HEALTH_INSURANCE_OTHER);
			transfer.setHealthInsuranceOther(truncate(firstNonBlank(
					stringValue(hieData.get("otherInsurance")),
					insurance), 255));
		}
	}

	private static String resolveTransferType(Map<String, Object> hieData) {
		if (isTrue(hieData.get("isEmergency"))) {
			return "EMERGENCY";
		}
		if (isTrue(hieData.get("isNonEmergency"))) {
			return "NOT_EMERGENCY";
		}
		if (isTrue(hieData.get("isFollowUp"))) {
			return "FOLLOW_UP";
		}
		String transferType = stringValue(hieData.get("transferType"));
		if (StringUtils.isBlank(transferType)) {
			return null;
		}
		String lower = transferType.toLowerCase();
		if (lower.contains("emergency") && !lower.contains("non")) {
			return "EMERGENCY";
		}
		if (lower.contains("non-emergency") || lower.contains("non emergency")) {
			return "NOT_EMERGENCY";
		}
		if (lower.contains("follow")) {
			return "FOLLOW_UP";
		}
		return null;
	}

	private static boolean isTrue(Object value) {
		return "true".equalsIgnoreCase(stringValue(value));
	}

	private static String stringValue(Object value) {
		if (value == null) {
			return "";
		}
		String text = String.valueOf(value).trim();
		return "null".equalsIgnoreCase(text) ? "" : text;
	}

	private static String firstNonBlank(String... values) {
		if (values == null) {
			return "";
		}
		for (String value : values) {
			if (StringUtils.isNotBlank(value)) {
				return value.trim();
			}
		}
		return "";
	}

	private static String truncate(String value, int maxLength) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		if (trimmed.length() <= maxLength) {
			return trimmed;
		}
		return trimmed.substring(0, maxLength);
	}

	private static Date parseDateTime(String value) {
		if (StringUtils.isBlank(value)) {
			return null;
		}
		String normalized = value.trim().replace('T', ' ');
		String[] patterns = new String[] {
				"yyyy-MM-dd HH:mm:ss",
				"yyyy-MM-dd HH:mm",
				"dd.MMM.yyyy, HH:mm:ss",
				"dd.MMM.yyyy, HH:mm",
				"yyyy-MM-dd"
		};
		for (String pattern : patterns) {
			try {
				return new SimpleDateFormat(pattern).parse(normalized);
			}
			catch (ParseException ignored) {
				// try next pattern
			}
		}
		return null;
	}

	private static Date parseDateOnly(String value) {
		Date parsed = parseDateTime(value);
		if (parsed != null) {
			return parsed;
		}
		if (StringUtils.isBlank(value)) {
			return null;
		}
		String trimmed = value.trim();
		try {
			return new SimpleDateFormat("dd.MMM.yyyy").parse(trimmed);
		}
		catch (ParseException ignored) {
			return null;
		}
	}

	private static String extractTime(String value) {
		if (StringUtils.isBlank(value)) {
			return null;
		}
		String normalized = value.trim().replace('T', ' ');
		if (normalized.length() >= 16 && normalized.charAt(10) == ' ') {
			return normalized.substring(11, Math.min(16, normalized.length()));
		}
		if (normalized.matches("^\\d{1,2}:\\d{2}(:\\d{2})?$")) {
			return normalized.length() >= 5 ? normalized.substring(0, 5) : normalized;
		}
		return normalized;
	}

}
