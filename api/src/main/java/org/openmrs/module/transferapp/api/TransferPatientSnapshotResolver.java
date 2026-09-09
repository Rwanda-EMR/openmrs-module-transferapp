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
package org.openmrs.module.transferapp.api;

import org.apache.commons.lang.StringUtils;
import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.Location;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.PersonAddress;
import org.openmrs.PersonAttribute;
import org.openmrs.User;
import org.openmrs.Visit;
import org.openmrs.api.context.Context;
import org.openmrs.module.transferapp.TransferAppConstants;
import org.openmrs.module.transferapp.api.dao.TransferDao;
import org.openmrs.module.transferapp.model.Transfer;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * Resolves patient and session context fields stored on a transfer for preview.
 */
public class TransferPatientSnapshotResolver {

	private static final String DATE_PATTERN = "yyyy-MM-dd";

	private static final String[] PHONE_ATTRIBUTE_NAMES = new String[] {
			"Telephone Number",
			"Telephone number",
			"PhoneNumber",
			"Phone Number",
			"Telephone",
			"Phone",
			"Mobile Number",
			"Mobile"
	};

	private static final String[] PHONE_ATTRIBUTE_KEYWORDS = new String[] {
			"telephone",
			"phone",
			"mobile"
	};

	public void applyPatientSnapshot(Transfer transfer, Patient patient) {
		applyPatientSnapshot(transfer, patient, null);
	}

	public void applyPatientSnapshot(Transfer transfer, Patient patient, TransferDao transferDao) {
		if (transfer == null || patient == null) {
			return;
		}

		if (patient.getPersonName() != null) {
			transfer.setClientName(patient.getPersonName().getFullName());
		}

		ensureEmrIdFromPatient(transfer, patient);
		transfer.setClientTelephone(resolvePatientPhone(patient, transferDao));
		transfer.setAgeOrDob(resolveAgeOrDob(patient));
		transfer.setSex(mapGender(patient.getGender()));

		PatientIdentifier nationalId = resolveNationalIdentifier(patient);
		if (nationalId != null && nationalId.getIdentifierType() != null) {
			transfer.setIdentifierType(nationalId.getIdentifierType().getName());
			transfer.setIdentifierValue(nationalId.getIdentifier());
		}

		transfer.setCaregiverName(resolveCaregiverName(patient));
		transfer.setCaregiverTelephone(resolveCaregiverTelephone(patient));

		transfer.setAdmissionAt(resolveAdmissionDatetime(patient));
		transfer.setDiagnosis(resolveDiagnosis(patient));
		transfer.setClinicalPresentation(resolveClinicalPresentation(patient));
		applyVitalSignsSnapshot(transfer, patient);
		transfer.setSendingFacility(resolveCurrentFacilityName());
		transfer.setReferringUnit(resolveReferringUnit());
		transfer.setReferringProviderName(resolveReferringProviderName());
	}

	/**
	 * Copies the patient's OpenMRS UPID onto {@code transfer.emrId} when available.
	 * Returns the UPID used (existing or freshly resolved), or {@code null} when neither
	 * the transfer nor the patient has a UPID.
	 */
	public String ensureEmrIdFromPatient(Transfer transfer, Patient patient) {
		if (transfer == null) {
			return null;
		}
		String existing = StringUtils.trimToNull(transfer.getEmrId());
		String fromPatient = resolveUpid(patient);
		if (StringUtils.isNotBlank(fromPatient)) {
			transfer.setEmrId(fromPatient.trim());
			return fromPatient.trim();
		}
		return existing;
	}

	public boolean patientHasUpid(Patient patient) {
		return StringUtils.isNotBlank(resolveUpid(patient));
	}

	public PersonAddress resolveActivePersonAddress(Patient patient) {
		if (patient == null) {
			return null;
		}

		// Touch the collection so OpenMRS loads rows from person_address for this patient.
		Set<PersonAddress> addresses = patient.getAddresses();
		if (addresses != null) {
			for (PersonAddress address : addresses) {
				if (isActiveAddress(address) && Boolean.TRUE.equals(address.getPreferred())) {
					return address;
				}
			}
			for (PersonAddress address : addresses) {
				if (isActiveAddress(address)) {
					return address;
				}
			}
		}

		PersonAddress preferred = patient.getPersonAddress();
		return isActiveAddress(preferred) ? preferred : null;
	}

	public void applyPersonAddressSnapshot(Transfer transfer, PersonAddress address) {
		if (transfer == null || address == null) {
			return;
		}
		transfer.setClientDistrict(resolveDistrict(address));
		transfer.setSector(resolveSector(address));
		transfer.setCell(resolveCell(address));
		transfer.setVillage(resolveVillage(address));
	}

	public String resolveDistrict(PersonAddress address) {
		if (address == null) {
			return null;
		}
		return StringUtils.trimToNull(address.getCountyDistrict());
	}

	public String resolveSector(PersonAddress address) {
		if (address == null) {
			return null;
		}
		return StringUtils.trimToNull(address.getCityVillage());
	}

	public String resolveCell(PersonAddress address) {
		if (address == null) {
			return null;
		}
		return StringUtils.trimToNull(address.getAddress3());
	}

	public String resolveVillage(PersonAddress address) {
		if (address == null) {
			return null;
		}
		return StringUtils.trimToNull(address.getAddress1());
	}

	private boolean isActiveAddress(PersonAddress address) {
		return address != null && !Boolean.TRUE.equals(address.getVoided());
	}

	public String resolveUpid(Patient patient) {
		for (PatientIdentifier identifier : patient.getActiveIdentifiers()) {
			if (identifier == null || identifier.getIdentifierType() == null) {
				continue;
			}
			String typeName = identifier.getIdentifierType().getName();
			if (typeName != null && "UPID".equalsIgnoreCase(typeName.trim())) {
				return identifier.getIdentifier();
			}
		}

		for (PatientIdentifier identifier : patient.getActiveIdentifiers()) {
			if (identifier == null || identifier.getIdentifierType() == null) {
				continue;
			}
			String typeName = identifier.getIdentifierType().getName();
			if (typeName != null && typeName.toUpperCase().contains("UPID")) {
				return identifier.getIdentifier();
			}
		}

		return null;
	}

	/**
	 * Name of the person helping the patient (not the referring clinician).
	 * Prefers explicit caregiver attributes, then mother/father/next-of-kin.
	 */
	public String resolveCaregiverName(Patient patient) {
		return resolvePersonAttribute(patient,
				"Caregiver Name",
				"CaregiverName",
				"Name of caregiver",
				"Mother's Name",
				"Mothers Name",
				"Mother Name",
				"Father's Name",
				"Fathers Name",
				"Father Name",
				"Next of Kin",
				"Next of Kin Name",
				"Contact Name",
				"Contact Person");
	}

	/**
	 * Telephone for the caregiver / helper (not the referring clinician's phone).
	 */
	public String resolveCaregiverTelephone(Patient patient) {
		return resolvePersonAttribute(patient,
				"Caregiver Telephone",
				"Caregiver Phone",
				"CaregiverPhone",
				"Contact Telephone",
				"Contact Phone",
				"Next of Kin Phone",
				"Next of Kin Telephone");
	}

	public PatientIdentifier resolveNationalIdentifier(Patient patient) {
		for (PatientIdentifier identifier : patient.getActiveIdentifiers()) {
			if (identifier == null || identifier.getIdentifierType() == null) {
				continue;
			}
			String typeName = identifier.getIdentifierType().getName();
			if (typeName != null) {
				String upper = typeName.toUpperCase();
				if (upper.contains("NID") || upper.contains("NATIONAL")) {
					return identifier;
				}
			}
		}
		return null;
	}

	public String resolvePatientPhone(Patient patient) {
		return resolvePatientPhone(patient, null);
	}

	public String resolvePatientPhone(Patient patient, TransferDao transferDao) {
		String phone = resolvePersonAttributeValue(
				patient != null ? patient.getAttributes() : null,
				PHONE_ATTRIBUTE_NAMES,
				PHONE_ATTRIBUTE_KEYWORDS);

		if (phone != null || transferDao == null || patient == null) {
			return StringUtils.trimToNull(phone);
		}

		return StringUtils.trimToNull(resolvePersonAttributeValue(
				transferDao.getPersonAttributes(patient.getPatientId()),
				PHONE_ATTRIBUTE_NAMES,
				PHONE_ATTRIBUTE_KEYWORDS));
	}

	public String resolvePersonAttribute(Patient patient, String... attributeNames) {
		if (patient == null || attributeNames == null) {
			return null;
		}

		for (String attributeName : attributeNames) {
			PersonAttribute attribute = patient.getAttribute(attributeName);
			if (isActivePersonAttribute(attribute) && StringUtils.isNotBlank(attribute.getValue())) {
				return attribute.getValue().trim();
			}
		}

		return resolvePersonAttributeValue(patient.getAttributes(), attributeNames, null);
	}

	private String resolvePersonAttributeValue(Collection<PersonAttribute> attributes,
			String[] exactNames,
			String[] keywords) {
		if (attributes == null || attributes.isEmpty()) {
			return null;
		}

		if (exactNames != null) {
			for (String attributeName : exactNames) {
				for (PersonAttribute attribute : attributes) {
					if (!isActivePersonAttribute(attribute) || attribute.getAttributeType() == null) {
						continue;
					}
					if (attributeName.equalsIgnoreCase(attribute.getAttributeType().getName())
							&& StringUtils.isNotBlank(attribute.getValue())) {
						return attribute.getValue().trim();
					}
				}
			}
		}

		if (keywords != null) {
			for (PersonAttribute attribute : attributes) {
				if (!isActivePersonAttribute(attribute) || attribute.getAttributeType() == null) {
					continue;
				}
				String typeName = attribute.getAttributeType().getName();
				if (StringUtils.isBlank(typeName) || StringUtils.isBlank(attribute.getValue())) {
					continue;
				}
				String lowerTypeName = typeName.toLowerCase();
				for (String keyword : keywords) {
					if (lowerTypeName.contains(keyword) && StringUtils.isNotBlank(attribute.getValue())) {
						return attribute.getValue().trim();
					}
				}
			}
		}

		return null;
	}

	private boolean isActivePersonAttribute(PersonAttribute attribute) {
		return attribute != null && !Boolean.TRUE.equals(attribute.getVoided());
	}

	public String resolveAgeOrDob(Patient patient) {
		Integer age = patient.getAge();
		if (patient.getBirthdate() != null) {
			if (age != null) {
				return age + " years";
			}
			return formatDate(patient.getBirthdate());
		}
		if (age != null) {
			return age + " years";
		}
		return null;
	}

	protected String resolveCurrentFacilityName() {
		return new TransferSendingLocationResolver().resolveOutboundFacilityName();
	}

	protected String resolveReferringUnit() {
		Location sessionLocation = Context.getUserContext().getLocation();
		if (sessionLocation == null) {
			return null;
		}
		return sessionLocation.getName();
	}

	public Date resolveAdmissionDatetime(Patient patient) {
		if (patient == null) {
			return null;
		}

		List<Visit> visits = Context.getVisitService().getVisitsByPatient(patient);
		if (visits == null || visits.isEmpty()) {
			return null;
		}

		Date latestStart = null;
		for (Visit visit : visits) {
			if (visit == null || Boolean.TRUE.equals(visit.getVoided())) {
				continue;
			}
			Date startDatetime = visit.getStartDatetime();
			if (startDatetime == null) {
				continue;
			}
			if (latestStart == null || startDatetime.after(latestStart)) {
				latestStart = startDatetime;
			}
		}
		return latestStart;
	}

	public String resolveDiagnosis(Patient patient) {
		Visit activeVisit = resolveActiveVisit(patient);
		if (activeVisit == null) {
			return null;
		}

		String conceptUuids = Context.getAdministrationService().getGlobalProperty(
				TransferAppConstants.GP_DIAGNOSIS_CONCEPT_UUID,
				TransferAppConstants.DEFAULT_DIAGNOSIS_CONCEPT_UUID);
		if (StringUtils.isBlank(conceptUuids)) {
			return null;
		}

		List<String> diagnosisNames = new ArrayList<String>();
		for (String token : conceptUuids.split(",")) {
			String conceptUuid = StringUtils.trimToNull(token);
			if (conceptUuid == null) {
				continue;
			}
			Concept questionConcept = Context.getConceptService().getConceptByUuid(conceptUuid);
			if (questionConcept == null) {
				continue;
			}
			String diagnosisName = resolveDiagnosisNameForConcept(patient, activeVisit, questionConcept);
			if (diagnosisName != null && !diagnosisNames.contains(diagnosisName)) {
				diagnosisNames.add(diagnosisName);
			}
		}

		if (diagnosisNames.isEmpty()) {
			return null;
		}
		return joinDiagnosisNames(diagnosisNames);
	}

	/**
	 * Picks the latest valid disease name for a question concept on the active visit.
	 * Scans several recent obs in case the newest row is empty / order-only.
	 */
	private String resolveDiagnosisNameForConcept(Patient patient, Visit visit, Concept questionConcept) {
		List<Obs> candidates = getRecentObservationsForVisit(patient, visit, questionConcept, 10);
		for (Obs obs : candidates) {
			String diagnosisName = extractCodedDiagnosisName(obs);
			if (diagnosisName != null) {
				return diagnosisName;
			}
		}
		return null;
	}

	/**
	 * Prefer value_coded preferred name (e.g. "Malaria"), then value_text.
	 * Skip diagnosis-order / certainty labels. For obs groups, prefer coded/non-coded
	 * diagnosis members over order/certainty members.
	 */
	private String extractCodedDiagnosisName(Obs obs) {
		if (obs == null || Boolean.TRUE.equals(obs.getVoided())) {
			return null;
		}

		if (isDiagnosisOrderOrCertaintyQuestion(obs.getConcept())) {
			return null;
		}

		String fromCoded = conceptPreferredName(obs.getValueCoded());
		if (fromCoded != null && !isDiagnosisMetadataLabel(fromCoded)) {
			return fromCoded;
		}

		String fromText = StringUtils.trimToNull(obs.getValueText());
		if (fromText != null && !isDiagnosisMetadataLabel(fromText)) {
			return fromText;
		}

		if (obs.hasGroupMembers(false)) {
			Obs codedMember = null;
			Obs nonCodedMember = null;
			List<Obs> otherMembers = new ArrayList<Obs>();
			for (Obs member : obs.getGroupMembers(false)) {
				if (member == null || Boolean.TRUE.equals(member.getVoided())) {
					continue;
				}
				if (isCodedDiagnosisQuestion(member.getConcept())) {
					codedMember = member;
				} else if (isNonCodedDiagnosisQuestion(member.getConcept())) {
					nonCodedMember = member;
				} else if (!isDiagnosisOrderOrCertaintyQuestion(member.getConcept())) {
					otherMembers.add(member);
				}
			}
			if (codedMember != null) {
				String nested = extractCodedDiagnosisName(codedMember);
				if (nested != null) {
					return nested;
				}
			}
			if (nonCodedMember != null) {
				String nested = extractCodedDiagnosisName(nonCodedMember);
				if (nested != null) {
					return nested;
				}
			}
			for (Obs member : otherMembers) {
				String nested = extractCodedDiagnosisName(member);
				if (nested != null) {
					return nested;
				}
			}
		}
		return null;
	}

	private String conceptPreferredName(Concept concept) {
		if (concept == null) {
			return null;
		}
		if (concept.getName(Context.getLocale()) != null
				&& StringUtils.isNotBlank(concept.getName(Context.getLocale()).getName())) {
			return concept.getName(Context.getLocale()).getName().trim();
		}
		if (concept.getName() != null && StringUtils.isNotBlank(concept.getName().getName())) {
			return concept.getName().getName().trim();
		}
		return StringUtils.trimToNull(concept.getDisplayString());
	}

	private boolean isDiagnosisMetadataLabel(String name) {
		if (StringUtils.isBlank(name)) {
			return true;
		}
		String normalized = name.trim().toLowerCase();
		return "primary diagnosis".equals(normalized)
				|| "secondary diagnosis".equals(normalized)
				|| "primary".equals(normalized)
				|| "secondary".equals(normalized)
				|| "confirmed".equals(normalized)
				|| "presumed".equals(normalized)
				|| "provisional".equals(normalized)
				|| normalized.contains("diagnosis order")
				|| normalized.contains("diagnosis certainty");
	}

	private boolean isDiagnosisOrderOrCertaintyQuestion(Concept concept) {
		String name = conceptPreferredName(concept);
		if (name == null) {
			return false;
		}
		String normalized = name.trim().toLowerCase();
		return normalized.contains("diagnosis order")
				|| normalized.contains("diagnosis certainty")
				|| "order".equals(normalized)
				|| "certainty".equals(normalized);
	}

	private boolean isCodedDiagnosisQuestion(Concept concept) {
		String name = conceptPreferredName(concept);
		if (name == null) {
			return false;
		}
		String normalized = name.trim().toLowerCase();
		if (normalized.contains("non-coded") || normalized.contains("non coded")) {
			return false;
		}
		return "coded diagnosis".equals(normalized) || normalized.contains("coded diagnosis");
	}

	private boolean isNonCodedDiagnosisQuestion(Concept concept) {
		String name = conceptPreferredName(concept);
		if (name == null) {
			return false;
		}
		String normalized = name.trim().toLowerCase();
		return normalized.contains("non-coded diagnosis") || normalized.contains("non coded diagnosis");
	}

	/**
	 * Resolves clinical presentation (chief complaint) from the latest observation
	 * on the active visit for the configured concept UUID (value_text).
	 */
	public String resolveClinicalPresentation(Patient patient) {
		Visit activeVisit = resolveActiveVisit(patient);
		if (activeVisit == null) {
			return null;
		}

		Concept questionConcept = getConceptByGlobalProperty(
				TransferAppConstants.GP_CLINICAL_PRESENTATION_CONCEPT_UUID,
				TransferAppConstants.DEFAULT_CLINICAL_PRESENTATION_CONCEPT_UUID);
		if (questionConcept == null) {
			return null;
		}

		Obs latestObs = getLatestObservationForVisit(patient, activeVisit, questionConcept);
		if (latestObs == null) {
			return null;
		}
		return StringUtils.trimToNull(latestObs.getValueText());
	}

	public void applyVitalSignsSnapshot(Transfer transfer, Patient patient) {
		if (transfer == null || patient == null) {
			return;
		}

		Visit activeVisit = resolveActiveVisit(patient);
		if (activeVisit == null) {
			return;
		}

		transfer.setVitalHt(resolveVitalSignValue(patient, activeVisit,
				TransferAppConstants.GP_HEIGHT_CONCEPT_UUID,
				TransferAppConstants.DEFAULT_HEIGHT_CONCEPT_UUID));
		transfer.setVitalWt(resolveVitalSignValue(patient, activeVisit,
				TransferAppConstants.GP_WEIGHT_CONCEPT_UUID,
				TransferAppConstants.DEFAULT_WEIGHT_CONCEPT_UUID));
		transfer.setVitalTemp(resolveVitalSignValue(patient, activeVisit,
				TransferAppConstants.GP_TEMPERATURE_CONCEPT_UUID,
				TransferAppConstants.DEFAULT_TEMPERATURE_CONCEPT_UUID));
		transfer.setVitalPulse(resolveVitalSignValue(patient, activeVisit,
				TransferAppConstants.GP_PULSE_CONCEPT_UUID,
				TransferAppConstants.DEFAULT_PULSE_CONCEPT_UUID));
		transfer.setVitalBp(resolveBloodPressureValue(patient, activeVisit));
		transfer.setVitalSpo2(resolveVitalSignValue(patient, activeVisit,
				TransferAppConstants.GP_OXYGEN_SATURATION_CONCEPT_UUID,
				TransferAppConstants.DEFAULT_OXYGEN_SATURATION_CONCEPT_UUID));
		transfer.setVitalMuac(resolveVitalSignValue(patient, activeVisit,
				TransferAppConstants.GP_MUAC_CONCEPT_UUID,
				TransferAppConstants.DEFAULT_MUAC_CONCEPT_UUID));
		transfer.setVitalRr(resolveVitalSignValue(patient, activeVisit,
				TransferAppConstants.GP_RESPIRATORY_RATE_CONCEPT_UUID,
				TransferAppConstants.DEFAULT_RESPIRATORY_RATE_CONCEPT_UUID));
	}

	private String resolveVitalSignValue(Patient patient, Visit activeVisit, String globalPropertyName,
			String defaultConceptUuid) {
		Concept questionConcept = getConceptByGlobalProperty(globalPropertyName, defaultConceptUuid);
		if (questionConcept == null) {
			return null;
		}
		return formatObsValue(getLatestObservationForVisit(patient, activeVisit, questionConcept));
	}

	private String resolveBloodPressureValue(Patient patient, Visit activeVisit) {
		String conceptUuids = Context.getAdministrationService().getGlobalProperty(
				TransferAppConstants.GP_BLOOD_PRESSURE_CONCEPT_UUID,
				TransferAppConstants.DEFAULT_BLOOD_PRESSURE_CONCEPT_UUID);
		if (StringUtils.isBlank(conceptUuids)) {
			return null;
		}

		String[] parts = conceptUuids.split("/");
		if (parts.length != 2) {
			return null;
		}

		Concept systolicConcept = getConceptByUuid(StringUtils.trimToNull(parts[0]));
		Concept diastolicConcept = getConceptByUuid(StringUtils.trimToNull(parts[1]));
		if (systolicConcept == null || diastolicConcept == null) {
			return null;
		}

		String systolic = formatObsValue(getLatestObservationForVisit(patient, activeVisit, systolicConcept));
		String diastolic = formatObsValue(getLatestObservationForVisit(patient, activeVisit, diastolicConcept));
		if (systolic == null && diastolic == null) {
			return null;
		}
		if (systolic == null) {
			return diastolic;
		}
		if (diastolic == null) {
			return systolic;
		}
		return systolic + "/" + diastolic;
	}

	private Concept getConceptByGlobalProperty(String globalPropertyName, String defaultConceptUuid) {
		String conceptUuid = Context.getAdministrationService().getGlobalProperty(globalPropertyName, defaultConceptUuid);
		return getConceptByUuid(StringUtils.trimToNull(conceptUuid));
	}

	private Concept getConceptByUuid(String conceptUuid) {
		if (conceptUuid == null) {
			return null;
		}
		return Context.getConceptService().getConceptByUuid(conceptUuid);
	}

	private String formatObsValue(Obs obs) {
		if (obs == null) {
			return null;
		}
		if (obs.getValueNumeric() != null) {
			Double numericValue = obs.getValueNumeric();
			if (numericValue.doubleValue() == Math.rint(numericValue.doubleValue())) {
				return String.valueOf(numericValue.intValue());
			}
			return String.valueOf(numericValue);
		}
		String textValue = StringUtils.trimToNull(obs.getValueText());
		if (textValue != null) {
			return textValue;
		}
		if (obs.getValueCoded() != null) {
			return StringUtils.trimToNull(obs.getValueCoded().getDisplayString());
		}
		return StringUtils.trimToNull(obs.getValueAsString(Context.getLocale()));
	}

	public Visit resolveActiveVisit(Patient patient) {
		if (patient == null) {
			return null;
		}

		List<Visit> visits = Context.getVisitService().getActiveVisitsByPatient(patient);
		if (visits == null || visits.isEmpty()) {
			return null;
		}

		Visit latestActiveVisit = null;
		Date latestStart = null;
		for (Visit visit : visits) {
			if (visit == null || Boolean.TRUE.equals(visit.getVoided())) {
				continue;
			}
			if (visit.getStopDatetime() != null) {
				continue;
			}
			Date startDatetime = visit.getStartDatetime();
			if (startDatetime == null) {
				continue;
			}
			if (latestStart == null || startDatetime.after(latestStart)) {
				latestStart = startDatetime;
				latestActiveVisit = visit;
			}
		}
		return latestActiveVisit;
	}

	private Obs getLatestObservationForVisit(Patient patient, Visit visit, Concept questionConcept) {
		List<Obs> observations = getRecentObservationsForVisit(patient, visit, questionConcept, 1);
		if (observations.isEmpty()) {
			return null;
		}
		return observations.get(0);
	}

	/**
	 * Load recent obs for a question on the active visit.
	 * Uses EncounterService (visit.getEncounters() is often empty / lazy), sorts by
	 * obsDatetime descending, and falls back to visit start/stop date window.
	 */
	private List<Obs> getRecentObservationsForVisit(Patient patient, Visit visit, Concept questionConcept,
			int mostRecentN) {
		if (patient == null || visit == null || questionConcept == null) {
			return Collections.emptyList();
		}

		List<String> sort = Collections.singletonList("obsDatetime");
		List<Encounter> encounters = collectActiveEncounters(visit);
		List<Obs> observations = null;
		if (!encounters.isEmpty()) {
			observations = Context.getObsService().getObservations(
					Collections.singletonList(patient),
					encounters,
					Collections.singletonList(questionConcept),
					null,
					null,
					null,
					sort,
					mostRecentN,
					null,
					null,
					null,
					false);
		}

		if (observations == null || observations.isEmpty()) {
			observations = Context.getObsService().getObservations(
					Collections.singletonList(patient),
					null,
					Collections.singletonList(questionConcept),
					null,
					null,
					null,
					sort,
					mostRecentN,
					null,
					visit.getStartDatetime(),
					visit.getStopDatetime(),
					false);
		}

		if (observations == null || observations.isEmpty()) {
			return Collections.emptyList();
		}
		return observations;
	}

	private List<Encounter> collectActiveEncounters(Visit visit) {
		List<Encounter> encounters = new ArrayList<Encounter>();
		if (visit == null) {
			return encounters;
		}

		// Prefer explicit service load — Visit.encounters is often uninitialized.
		List<Encounter> byVisit = Context.getEncounterService().getEncountersByVisit(visit, false);
		if (byVisit != null) {
			for (Encounter encounter : byVisit) {
				if (encounter != null && !Boolean.TRUE.equals(encounter.getVoided())) {
					encounters.add(encounter);
				}
			}
		}

		if (!encounters.isEmpty()) {
			return encounters;
		}

		if (visit.getEncounters() != null) {
			for (Encounter encounter : visit.getEncounters()) {
				if (encounter != null && !Boolean.TRUE.equals(encounter.getVoided())) {
					encounters.add(encounter);
				}
			}
		}
		return encounters;
	}

	private String joinDiagnosisNames(List<String> diagnosisNames) {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < diagnosisNames.size(); i++) {
			if (i > 0) {
				builder.append("; ");
			}
			builder.append(diagnosisNames.get(i));
		}
		return builder.toString();
	}

	protected String resolveReferringProviderName() {
		User user = Context.getAuthenticatedUser();
		if (user != null && user.getPerson() != null && user.getPerson().getPersonName() != null) {
			return user.getPerson().getPersonName().getFullName();
		}
		return null;
	}

	public String mapGender(String gender) {
		if (gender == null) {
			return null;
		}
		if ("M".equalsIgnoreCase(gender)) {
			return "MALE";
		}
		if ("F".equalsIgnoreCase(gender)) {
			return "FEMALE";
		}
		return "OTHER";
	}

	protected String formatDate(Date date) {
		return new SimpleDateFormat(DATE_PATTERN).format(date);
	}

}
