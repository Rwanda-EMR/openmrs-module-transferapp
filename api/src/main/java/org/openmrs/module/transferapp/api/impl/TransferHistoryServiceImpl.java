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
import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.EncounterType;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifierType;
import org.openmrs.Person;
import org.openmrs.api.APIException;
import org.openmrs.api.PatientService;
import org.openmrs.api.context.Context;
import org.openmrs.module.transferapp.TransferAppConstants;
import org.openmrs.module.transferapp.api.TransferHieReceiveService;
import org.openmrs.module.transferapp.api.TransferHistoryService;
import org.openmrs.module.transferapp.api.TransferPatientSnapshotResolver;
import org.openmrs.module.transferapp.api.TransferVerificationUrlService;
import org.openmrs.module.transferapp.api.dao.TransferDao;
import org.openmrs.module.transferapp.model.Transfer;
import org.openmrs.module.transferapp.model.TransferHistoryItem;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TransferHistoryServiceImpl implements TransferHistoryService {

	private static final int MAX_RESULTS = 500;

	private PatientService patientService;

	private TransferDao transferDao;

	private TransferHieReceiveService transferHieReceiveService;

	private TransferPatientSnapshotResolver patientSnapshotResolver = new TransferPatientSnapshotResolver();

	public void setPatientService(PatientService patientService) {
		this.patientService = patientService;
	}

	public void setTransferDao(TransferDao transferDao) {
		this.transferDao = transferDao;
	}

	public void setTransferHieReceiveService(TransferHieReceiveService transferHieReceiveService) {
		this.transferHieReceiveService = transferHieReceiveService;
	}

	public void setPatientSnapshotResolver(TransferPatientSnapshotResolver patientSnapshotResolver) {
		this.patientSnapshotResolver = patientSnapshotResolver != null
				? patientSnapshotResolver
				: new TransferPatientSnapshotResolver();
	}

	@Override
	public List<TransferHistoryItem> findHistory(String upid, String yearMonth) {
		Concept transferIdConcept = resolveTransferIdConcept();
		if (transferIdConcept == null) {
			return Collections.emptyList();
		}
		Integer registrationTypeId = resolveRegistrationEncounterTypeId();
		if (registrationTypeId == null) {
			return Collections.emptyList();
		}

		String normalizedUpid = StringUtils.trimToNull(upid);
		Patient patient = null;
		if (normalizedUpid != null) {
			patient = findPatientByUpid(normalizedUpid);
			if (patient == null) {
				return Collections.emptyList();
			}
		}

		Date[] range = resolveDateRange(normalizedUpid != null, StringUtils.trimToNull(yearMonth));
		List<Person> persons = null;
		if (patient != null) {
			persons = Collections.<Person>singletonList(patient);
		}

		List<Obs> observations = Context.getObsService().getObservations(
				persons,
				null,
				Collections.singletonList(transferIdConcept),
				null,
				null,
				null,
				Collections.singletonList("obsDatetime"),
				MAX_RESULTS,
				null,
				range[0],
				range[1],
				false);

		if (observations == null || observations.isEmpty()) {
			return Collections.emptyList();
		}

		TransferVerificationUrlService verificationUrlService =
				Context.getService(TransferVerificationUrlService.class);
		List<TransferHistoryItem> items = new ArrayList<TransferHistoryItem>();
		for (Obs obs : observations) {
			if (obs == null || Boolean.TRUE.equals(obs.getVoided())) {
				continue;
			}
			Encounter encounter = obs.getEncounter();
			if (encounter == null || Boolean.TRUE.equals(encounter.getVoided())) {
				continue;
			}
			EncounterType type = encounter.getEncounterType();
			if (type == null || type.getEncounterTypeId() == null
					|| !registrationTypeId.equals(type.getEncounterTypeId())) {
				continue;
			}
			String transferId = StringUtils.trimToNull(obs.getValueText());
			if (transferId == null) {
				continue;
			}
			if (verificationUrlService != null
					&& !verificationUrlService.isValidVerificationTransferId(transferId)) {
				continue;
			}

			Patient rowPatient = encounter.getPatient();
			if (rowPatient == null && obs.getPerson() instanceof Patient) {
				rowPatient = (Patient) obs.getPerson();
			}
			if (rowPatient == null) {
				continue;
			}

			TransferHistoryItem item = new TransferHistoryItem();
			item.setPatientId(rowPatient.getPatientId());
			if (rowPatient.getPersonName() != null) {
				item.setPatientName(rowPatient.getPersonName().getFullName());
			}
			item.setUpid(patientSnapshotResolver.resolveUpid(rowPatient));
			item.setPhoneNumber(patientSnapshotResolver.resolvePatientPhone(rowPatient));
			item.setEncounterId(encounter.getEncounterId());
			item.setEncounterDatetime(encounter.getEncounterDatetime() != null
					? encounter.getEncounterDatetime()
					: obs.getObsDatetime());
			item.setTransferId(transferId);
			if (encounter.getLocation() != null) {
				item.setLocationName(encounter.getLocation().getName());
			}
			item.setReuseRendezvousDate(resolveReuseRendezvousDate(rowPatient.getPatientId(), transferId));
			items.add(item);
		}

		Collections.sort(items, new Comparator<TransferHistoryItem>() {
			@Override
			public int compare(TransferHistoryItem left, TransferHistoryItem right) {
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
		return items;
	}

	@Override
	public Transfer setReuseRendezvousDate(Integer patientId, String hieTransferId, String reuseDateYyyyMmDd) {
		if (patientId == null) {
			throw new APIException("Patient is required");
		}
		String transferId = StringUtils.trimToNull(hieTransferId);
		if (transferId == null) {
			throw new APIException("HIE transfer id is required");
		}
		TransferVerificationUrlService verificationUrlService =
				Context.getService(TransferVerificationUrlService.class);
		if (verificationUrlService == null
				|| !verificationUrlService.isValidVerificationTransferId(transferId)) {
			throw new APIException("A valid HIE transfer UUID is required");
		}

		Transfer transfer = transferDao != null
				? transferDao.getTransferByHieTransferId(patientId, transferId)
				: null;
		if (transfer == null) {
			if (transferHieReceiveService == null) {
				throw new APIException("Unable to store transfer from HIE for reuse scheduling");
			}
			transfer = transferHieReceiveService.receiveTransferFromHie(patientId, transferId);
		}
		if (transfer == null) {
			throw new APIException("Unable to load transfer for reuse scheduling");
		}

		String dateText = StringUtils.trimToNull(reuseDateYyyyMmDd);
		if (dateText == null) {
			transfer.setReuseRendezvousDate(null);
		}
		else {
			Date reuseDate = parseReuseDate(dateText);
			assertReuseDateNotBeforeToday(reuseDate);
			transfer.setReuseRendezvousDate(reuseDate);
		}
		transfer.setChangedBy(Context.getAuthenticatedUser());
		transfer.setDateChanged(new Date());
		return transferDao.saveTransfer(transfer);
	}

	private String resolveReuseRendezvousDate(Integer patientId, String hieTransferId) {
		if (transferDao == null || patientId == null || StringUtils.isBlank(hieTransferId)) {
			return null;
		}
		Transfer local = transferDao.getTransferByHieTransferId(patientId, hieTransferId.trim());
		if (local == null || local.getReuseRendezvousDate() == null) {
			return null;
		}
		return new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(local.getReuseRendezvousDate());
	}

	private static Date parseReuseDate(String value) {
		try {
			SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
			format.setLenient(false);
			Date parsed = format.parse(value.trim());
			Calendar calendar = Calendar.getInstance();
			calendar.setTime(parsed);
			clearTimeStatic(calendar);
			return calendar.getTime();
		}
		catch (ParseException ex) {
			throw new APIException("Reuse date must use yyyy-MM-dd format");
		}
	}

	private static void assertReuseDateNotBeforeToday(Date reuseDate) {
		Calendar today = Calendar.getInstance();
		clearTimeStatic(today);
		if (reuseDate.before(today.getTime())) {
			throw new APIException("Reuse rendez-vous date must be today or a future date");
		}
	}

	private static void clearTimeStatic(Calendar calendar) {
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
	}

	/**
	 * No filters → today. Month set → that month. UPID only → all time for that patient.
	 */
	private Date[] resolveDateRange(boolean hasPatientFilter, String yearMonth) {
		if (StringUtils.isNotBlank(yearMonth)) {
			try {
				Date monthStart = new SimpleDateFormat("yyyy-MM", Locale.ENGLISH).parse(yearMonth.trim());
				Calendar calendar = Calendar.getInstance();
				calendar.setTime(monthStart);
				clearTime(calendar);
				calendar.set(Calendar.DAY_OF_MONTH, 1);
				Date start = calendar.getTime();
				calendar.add(Calendar.MONTH, 1);
				calendar.add(Calendar.MILLISECOND, -1);
				Date end = calendar.getTime();
				return new Date[] { start, end };
			}
			catch (ParseException ex) {
				throw new APIException("Invalid month filter. Use yyyy-MM.");
			}
		}
		if (hasPatientFilter) {
			return new Date[] { null, null };
		}
		Calendar calendar = Calendar.getInstance();
		clearTime(calendar);
		Date start = calendar.getTime();
		calendar.add(Calendar.DAY_OF_MONTH, 1);
		calendar.add(Calendar.MILLISECOND, -1);
		Date end = calendar.getTime();
		return new Date[] { start, end };
	}

	private Patient findPatientByUpid(String upid) {
		if (patientService == null || StringUtils.isBlank(upid)) {
			return null;
		}
		PatientIdentifierType upidType = null;
		List<PatientIdentifierType> types = patientService.getAllPatientIdentifierTypes(false);
		if (types != null) {
			for (PatientIdentifierType type : types) {
				if (type != null && type.getName() != null
						&& TransferAppConstants.UPID_IDENTIFIER_TYPE_NAME.equalsIgnoreCase(type.getName().trim())) {
					upidType = type;
					break;
				}
			}
		}
		List<Patient> matches;
		if (upidType != null) {
			matches = patientService.getPatients(null, upid.trim(), Collections.singletonList(upidType), true);
		}
		else {
			matches = patientService.getPatients(null, upid.trim(), null, false);
		}
		if (matches == null || matches.isEmpty()) {
			return null;
		}
		return matches.get(0);
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

	private void clearTime(Calendar calendar) {
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
	}
}
