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
package org.openmrs.module.transferapp.api.impl;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.EncounterType;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.Visit;
import org.openmrs.api.ObsService;
import org.openmrs.api.PatientService;
import org.openmrs.api.context.Context;
import org.openmrs.module.transferapp.TransferAppConstants;
import org.openmrs.module.transferapp.api.TransferHieReceiveService;
import org.openmrs.module.transferapp.api.TransferHieSearchService;
import org.openmrs.module.transferapp.api.TransferPatientSnapshotResolver;
import org.openmrs.module.transferapp.api.TransferRegistrationObsService;
import org.openmrs.module.transferapp.api.TransferSendingLocationResolver;
import org.openmrs.module.transferapp.api.TransferVerificationUrlService;
import org.openmrs.module.transferapp.model.Transfer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TransferRegistrationObsServiceImpl implements TransferRegistrationObsService {

	private static final Log log = LogFactory.getLog(TransferRegistrationObsServiceImpl.class);

	private PatientService patientService;

	private ObsService obsService;

	private TransferHieSearchService transferHieSearchService;

	private TransferHieReceiveService transferHieReceiveService;

	private TransferSendingLocationResolver sendingLocationResolver = new TransferSendingLocationResolver();

	private TransferPatientSnapshotResolver patientSnapshotResolver = new TransferPatientSnapshotResolver();

	public void setPatientService(PatientService patientService) {
		this.patientService = patientService;
	}

	public void setObsService(ObsService obsService) {
		this.obsService = obsService;
	}

	public void setTransferHieSearchService(TransferHieSearchService transferHieSearchService) {
		this.transferHieSearchService = transferHieSearchService;
	}

	public void setTransferHieReceiveService(TransferHieReceiveService transferHieReceiveService) {
		this.transferHieReceiveService = transferHieReceiveService;
	}

	@Override
	public String resolveCurrentFacilityName() {
		return sendingLocationResolver.resolveCurrentSendingFacilityName();
	}

	@Override
	public boolean destinationMatchesCurrentFacility(String destination) {
		return destinationMatchesAnyFacility(destination, sendingLocationResolver.resolveCurrentSendingFacilityNames());
	}

	/**
	 * True when {@code destination} equals / contains / is contained by any configured facility alias.
	 */
	public static boolean destinationMatchesAnyFacility(String destination, List<String> facilityNames) {
		String normalizedDestination = normalizeFacilityName(destination);
		if (StringUtils.isBlank(normalizedDestination) || facilityNames == null || facilityNames.isEmpty()) {
			return false;
		}
		for (String facilityName : facilityNames) {
			String currentFacility = normalizeFacilityName(facilityName);
			if (StringUtils.isBlank(currentFacility)) {
				continue;
			}
			if (normalizedDestination.equals(currentFacility)
					|| normalizedDestination.contains(currentFacility)
					|| currentFacility.contains(normalizedDestination)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public Encounter findRegistrationEncounterMissingTransferId(Patient patient) {
		if (patient == null) {
			return null;
		}

		Visit activeVisit = resolveActiveVisit(patient);
		if (activeVisit == null) {
			return null;
		}

		Integer registrationTypeId = resolveRegistrationEncounterTypeId();
		if (registrationTypeId == null) {
			return null;
		}

		Concept transferIdConcept = resolveTransferIdConcept();
		if (transferIdConcept == null) {
			return null;
		}

		List<Encounter> registrationEncounters = findRegistrationEncountersOnVisit(
				Context.getEncounterService().getEncountersByPatient(patient),
				registrationTypeId,
				activeVisit);
		if (registrationEncounters.isEmpty()) {
			return null;
		}

		for (Encounter encounter : registrationEncounters) {
			if (findTransferIdValueText(encounter, transferIdConcept) == null) {
				return encounter;
			}
		}
		return null;
	}

	@Override
	public String findRecordedHieTransferIdOnActiveVisit(Patient patient) {
		if (patient == null) {
			return null;
		}
		Visit activeVisit = resolveActiveVisit(patient);
		if (activeVisit == null) {
			return null;
		}
		Integer registrationTypeId = resolveRegistrationEncounterTypeId();
		if (registrationTypeId == null) {
			return null;
		}
		Concept transferIdConcept = resolveTransferIdConcept();
		if (transferIdConcept == null) {
			return null;
		}
		List<Encounter> registrationEncounters = findRegistrationEncountersOnVisit(
				Context.getEncounterService().getEncountersByPatient(patient),
				registrationTypeId,
				activeVisit);
		if (registrationEncounters.isEmpty()) {
			return null;
		}
		TransferVerificationUrlService verificationUrlService = Context.getService(TransferVerificationUrlService.class);
		if (verificationUrlService == null) {
			return null;
		}
		for (Encounter registration : registrationEncounters) {
			String transferId = findTransferIdValueText(registration, transferIdConcept);
			if (StringUtils.isBlank(transferId)) {
				continue;
			}
			if (verificationUrlService.isValidVerificationTransferId(transferId)) {
				return transferId.trim();
			}
		}
		return null;
	}

	@Override
	public Date findTransferIdObsDatetimeOnActiveVisit(Patient patient, String hieTransferId) {
		Obs obs = findMatchingTransferIdObs(patient, hieTransferId);
		return obs != null ? obs.getObsDatetime() : null;
	}

	@Override
	public Obs findMatchingTransferIdObs(Patient patient, String hieTransferId) {
		if (patient == null || StringUtils.isBlank(hieTransferId)) {
			return null;
		}
		Concept transferIdConcept = resolveTransferIdConcept();
		if (transferIdConcept == null) {
			return null;
		}
		String normalizedTransferId = normalizeTransferUuid(hieTransferId);
		Visit activeVisit = resolveActiveVisit(patient);
		Integer registrationTypeId = resolveRegistrationEncounterTypeId();
		if (activeVisit != null && registrationTypeId != null) {
			List<Encounter> registrationEncounters = findRegistrationEncountersOnVisit(
					Context.getEncounterService().getEncountersByPatient(patient),
					registrationTypeId,
					activeVisit);
			for (Encounter registration : registrationEncounters) {
				Obs transferIdObs = findTransferIdObs(registration, transferIdConcept, normalizedTransferId, null);
				if (transferIdObs != null) {
					return transferIdObs;
				}
			}
		}
		List<Obs> matches = obsService.getObservationsByPersonAndConcept(patient, transferIdConcept);
		if (matches == null) {
			return null;
		}
		for (Obs obs : matches) {
			if (obs == null || Boolean.TRUE.equals(obs.getVoided())) {
				continue;
			}
			String valueText = StringUtils.trimToNull(obs.getValueText());
			if (valueText == null) {
				valueText = StringUtils.trimToNull(obs.getValueAsString(Context.getLocale()));
			}
			if (valueText != null && normalizedTransferId.equals(normalizeTransferUuid(valueText))) {
				return obs;
			}
		}
		return null;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Map<String, Object> validateAndSaveTransferId(Integer patientId, String hieTransferId) {
		Map<String, Object> result = new LinkedHashMap<String, Object>();
		result.put("status", "error");

		if (patientId == null) {
			result.put("message", "Patient is required");
			return result;
		}
		if (!Context.getService(TransferVerificationUrlService.class).isValidVerificationTransferId(hieTransferId)) {
			result.put("message", "A valid HIE transfer UUID is required");
			return result;
		}

		Patient patient = patientService.getPatient(patientId);
		if (patient == null) {
			result.put("message", "Patient not found");
			return result;
		}

		Encounter registrationEncounter = findRegistrationEncounterMissingTransferId(patient);
		if (registrationEncounter == null) {
			result.put("message", "No active registration encounter missing Transfer Id was found");
			return result;
		}

		String upid = patientSnapshotResolver.resolveUpid(patient);
		if (StringUtils.isBlank(upid)) {
			result.put("message", "Patient UPID is required");
			return result;
		}

		Map<String, Object> searchResult = transferHieSearchService.searchTransfers(
				upid.trim(), hieTransferId.trim(), false);
		if (searchResult == null || !"success".equals(searchResult.get("status"))) {
			result.put("message", searchResult != null && searchResult.get("message") != null
					? String.valueOf(searchResult.get("message"))
					: "Unable to load transfer from HIE");
			return result;
		}

		List<Map<String, Object>> items = (List<Map<String, Object>>) searchResult.get("data");
		if (items == null || items.isEmpty()) {
			result.put("message", "Transfer not found in HIE");
			return result;
		}

		Map<String, Object> hieTransfer = items.get(0);
		String destination = resolveDestination(hieTransfer);
		if (!destinationMatchesCurrentFacility(destination)) {
			result.put("message", "This transfer is not addressed to the current facility");
			return result;
		}

		Concept transferIdConcept = resolveTransferIdConcept();
		if (transferIdConcept == null) {
			result.put("message", "Transfer Id concept is not configured");
			return result;
		}

		if (findTransferIdValueText(registrationEncounter, transferIdConcept) != null) {
			result.put("message", "Transfer Id is already recorded on this registration encounter");
			return result;
		}

		Obs obs = new Obs();
		obs.setPerson(patient);
		obs.setConcept(transferIdConcept);
		obs.setEncounter(registrationEncounter);
		obs.setObsDatetime(new Date());
		obs.setLocation(registrationEncounter.getLocation());
		obs.setValueText(hieTransferId.trim());
		obsService.saveObs(obs, null);

		log.info("Saved Transfer Id obs on registration encounter "
				+ registrationEncounter.getEncounterId() + " for patient " + patientId);

		Transfer localTransfer = null;
		if (transferHieReceiveService != null) {
			localTransfer = transferHieReceiveService.storeReceivedTransfer(patientId, hieTransfer);
		}

		result.put("status", "success");
		result.put("message", localTransfer != null
				? "Transfer validated, recorded on registration, and saved locally"
				: "Transfer validated and recorded on registration");
		result.put("encounterId", registrationEncounter.getEncounterId());
		result.put("transferId", hieTransferId.trim());
		result.put("destination", destination);
		if (localTransfer != null) {
			result.put("localTransferUuid", localTransfer.getUuid());
			result.put("localTransferId", localTransfer.getTransferId());
			result.put("receivedFromHie", Boolean.TRUE);
		}
		return result;
	}

	static String resolveDestination(Map<String, Object> transfer) {
		if (transfer == null) {
			return "";
		}
		String destination = asString(transfer.get("destination"));
		if (StringUtils.isNotBlank(destination)) {
			return destination;
		}
		destination = asString(transfer.get("receivingFacility"));
		if (StringUtils.isNotBlank(destination)) {
			return destination;
		}
		return asString(transfer.get("hospitalName"));
	}

	static String normalizeFacilityName(String value) {
		if (value == null) {
			return "";
		}
		return value.trim().toLowerCase().replaceAll("\\s+", " ");
	}

	private Visit resolveActiveVisit(Patient patient) {
		List<Visit> visits = Context.getVisitService().getActiveVisitsByPatient(patient);
		if (visits == null || visits.isEmpty()) {
			return null;
		}
		Visit latest = null;
		Date latestStart = null;
		for (Visit visit : visits) {
			if (visit == null || Boolean.TRUE.equals(visit.getVoided()) || visit.getStopDatetime() != null) {
				continue;
			}
			Date start = visit.getStartDatetime();
			if (start == null) {
				continue;
			}
			if (latestStart == null || start.after(latestStart)) {
				latestStart = start;
				latest = visit;
			}
		}
		return latest;
	}

	private Integer resolveRegistrationEncounterTypeId() {
		String raw = StringUtils.trimToNull(Context.getAdministrationService().getGlobalProperty(
				TransferAppConstants.GP_REGISTRATION_ENCOUNTER_TYPE_ID,
				TransferAppConstants.DEFAULT_REGISTRATION_ENCOUNTER_TYPE_ID));
		if (raw == null) {
			raw = StringUtils.trimToNull(Context.getAdministrationService().getGlobalProperty(
					TransferAppConstants.GP_RWANDAEMR_REGISTRATION_ENCOUNTER_TYPE_ID));
		}
		if (raw == null) {
			return null;
		}
		try {
			int id = Integer.parseInt(raw);
			return id > 0 ? Integer.valueOf(id) : null;
		}
		catch (NumberFormatException ex) {
			return null;
		}
	}

	private List<Encounter> findRegistrationEncountersOnVisit(List<Encounter> encounters,
			Integer registrationTypeId, Visit activeVisit) {
		List<Encounter> matches = new ArrayList<Encounter>();
		if (encounters == null || activeVisit == null || activeVisit.getVisitId() == null) {
			return matches;
		}
		for (Encounter encounter : encounters) {
			if (encounter == null || Boolean.TRUE.equals(encounter.getVoided())) {
				continue;
			}
			if (encounter.getVisit() == null || encounter.getVisit().getVisitId() == null) {
				continue;
			}
			if (!activeVisit.getVisitId().equals(encounter.getVisit().getVisitId())) {
				continue;
			}
			EncounterType type = encounter.getEncounterType();
			if (type == null || type.getEncounterTypeId() == null) {
				continue;
			}
			if (registrationTypeId.equals(type.getEncounterTypeId())) {
				matches.add(encounter);
			}
		}
		Collections.sort(matches, new Comparator<Encounter>() {
			@Override
			public int compare(Encounter left, Encounter right) {
				Date leftDate = left != null ? left.getEncounterDatetime() : null;
				Date rightDate = right != null ? right.getEncounterDatetime() : null;
				if (leftDate == null && rightDate == null) {
					return 0;
				}
				if (leftDate == null) {
					return 1;
				}
				if (rightDate == null) {
					return -1;
				}
				return rightDate.compareTo(leftDate);
			}
		});
		return matches;
	}

	private Concept resolveTransferIdConcept() {
		String conceptUuid = StringUtils.trimToNull(Context.getAdministrationService().getGlobalProperty(
				TransferAppConstants.GP_RECEIVED_TRANSFER_CONCEPT_UUID,
				TransferAppConstants.DEFAULT_RECEIVED_TRANSFER_CONCEPT_UUID));
		if (conceptUuid == null) {
			conceptUuid = StringUtils.trimToNull(Context.getAdministrationService().getGlobalProperty(
					TransferAppConstants.GP_RWANDAEMR_TRANSFER_ID_CONCEPT_UUID));
		}
		if (conceptUuid == null) {
			return null;
		}
		return Context.getConceptService().getConceptByUuid(conceptUuid);
	}

	private String findTransferIdValueText(Encounter registration, Concept transferIdConcept) {
		Obs obs = findTransferIdObs(registration, transferIdConcept, null, null);
		if (obs == null) {
			return null;
		}
		String valueText = StringUtils.trimToNull(obs.getValueText());
		if (valueText != null) {
			return valueText;
		}
		return StringUtils.trimToNull(obs.getValueAsString(Context.getLocale()));
	}

	private Obs findTransferIdObs(Encounter registration, Concept transferIdConcept, String requiredTransferId,
			TransferVerificationUrlService verificationUrlService) {
		Set<Obs> obsSet = registration.getAllObs(false);
		if (obsSet == null || obsSet.isEmpty()) {
			return null;
		}
		for (Obs obs : obsSet) {
			if (obs == null || Boolean.TRUE.equals(obs.getVoided()) || obs.getConcept() == null) {
				continue;
			}
			if (!obs.getConcept().equals(transferIdConcept)) {
				continue;
			}
			String valueText = StringUtils.trimToNull(obs.getValueText());
			if (valueText == null) {
				valueText = StringUtils.trimToNull(obs.getValueAsString(Context.getLocale()));
			}
			if (valueText == null) {
				continue;
			}
			if (requiredTransferId != null) {
				if (!normalizeTransferUuid(requiredTransferId).equals(normalizeTransferUuid(valueText))) {
					continue;
				}
			}
			return obs;
		}
		return null;
	}

	private static String normalizeTransferUuid(String value) {
		if (value == null) {
			return "";
		}
		String normalized = value.trim();
		if (normalized.startsWith("{") && normalized.endsWith("}") && normalized.length() > 2) {
			normalized = normalized.substring(1, normalized.length() - 1).trim();
		}
		return normalized.toLowerCase();
	}

	private static String asString(Object value) {
		return value == null ? "" : String.valueOf(value).trim();
	}

}
