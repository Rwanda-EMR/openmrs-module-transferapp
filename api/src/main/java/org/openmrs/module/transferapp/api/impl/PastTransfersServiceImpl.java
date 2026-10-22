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
import org.openmrs.Patient;
import org.openmrs.api.APIException;
import org.openmrs.api.context.Context;
import org.openmrs.module.transferapp.PatientTransferIdentifierDetector;
import org.openmrs.module.transferapp.TransferAppConstants;
import org.openmrs.module.transferapp.api.PastTransfersService;
import org.openmrs.module.transferapp.api.TransferPatientSnapshotResolver;
import org.openmrs.module.transferapp.api.TransferVerificationUrlService;
import org.openmrs.module.transferapp.api.dao.PastTransfersDao;
import org.openmrs.module.transferapp.model.PastTransferItem;
import org.openmrs.module.transferapp.model.PastTransferPageResult;
import org.openmrs.module.transferapp.model.Transfer;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class PastTransfersServiceImpl implements PastTransfersService {

	private TransferPatientSnapshotResolver patientSnapshotResolver = new TransferPatientSnapshotResolver();

	private PastTransfersDao pastTransfersDao;

	public void setPatientSnapshotResolver(TransferPatientSnapshotResolver patientSnapshotResolver) {
		this.patientSnapshotResolver = patientSnapshotResolver != null
				? patientSnapshotResolver
				: new TransferPatientSnapshotResolver();
	}

	public void setPastTransfersDao(PastTransfersDao pastTransfersDao) {
		this.pastTransfersDao = pastTransfersDao;
	}

	@Override
	public List<PastTransferItem> findVisitsForMonth(String yearMonth) {
		PastTransferPageResult page = findVisitsForMonth(yearMonth, null, 0, DEFAULT_PAGE_SIZE);
		return page != null ? page.getItems() : Collections.<PastTransferItem>emptyList();
	}

	@Override
	public PastTransferPageResult findVisitsForMonth(String yearMonth, int offset, int limit) {
		return findVisitsForMonth(yearMonth, null, offset, limit);
	}

	@Override
	public PastTransferPageResult findVisitsForMonth(String yearMonth, String upid, int offset, int limit) {
		String normalizedMonth = StringUtils.trimToNull(yearMonth);
		if (normalizedMonth == null || !normalizedMonth.matches("\\d{4}-\\d{2}")) {
			throw new APIException("Invalid month filter. Use yyyy-MM.");
		}
		Date[] range = resolveMonthRange(normalizedMonth);
		SimpleDateFormat dayFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
		return findVisitsInRange(dayFormat.format(range[0]), dayFormat.format(range[1]), upid, offset, limit);
	}

	@Override
	public PastTransferPageResult findVisitsInRange(String startDate, String endDate, String upid, int offset,
			int limit) {
		return findVisitsInRange(startDate, endDate, upid, null, null, offset, limit);
	}

	@Override
	public PastTransferPageResult findVisitsInRange(String startDate, String endDate, String upid, String insuranceType,
			int offset, int limit) {
		return findVisitsInRange(startDate, endDate, upid, insuranceType, null, offset, limit);
	}

	@Override
	public PastTransferPageResult findVisitsInRange(String startDate, String endDate, String upid, String insuranceType,
			String primaryCareId, int offset, int limit) {
		if (pastTransfersDao == null) {
			throw new APIException("Past transfers data access is not configured");
		}

		int safeOffset = offset < 0 ? 0 : offset;
		int safeLimit = limit <= 0 ? DEFAULT_PAGE_SIZE : Math.min(limit, 500);

		PastTransferPageResult page = new PastTransferPageResult();
		page.setOffset(safeOffset);
		page.setLimit(safeLimit);

		Integer registrationTypeId = resolveRegistrationEncounterTypeId();
		if (registrationTypeId == null) {
			page.setItems(Collections.<PastTransferItem>emptyList());
			page.setTotalCount(0);
			return page;
		}

		Integer patientIdFilter = null;
		String normalizedUpid = PatientTransferIdentifierDetector.normalize(upid);
		if (StringUtils.isNotBlank(normalizedUpid)) {
			patientIdFilter = pastTransfersDao.findPatientIdByUpid(normalizedUpid);
			if (patientIdFilter == null) {
				page.setItems(Collections.<PastTransferItem>emptyList());
				page.setTotalCount(0);
				return page;
			}
		}
		String normalizedPrimaryCareId = StringUtils.trimToNull(primaryCareId);
		if (normalizedPrimaryCareId != null) {
			Integer primaryCarePatientId = pastTransfersDao.findPatientIdByPrimaryCareId(normalizedPrimaryCareId);
			if (primaryCarePatientId == null) {
				page.setItems(Collections.<PastTransferItem>emptyList());
				page.setTotalCount(0);
				return page;
			}
			if (patientIdFilter != null && !patientIdFilter.equals(primaryCarePatientId)) {
				page.setItems(Collections.<PastTransferItem>emptyList());
				page.setTotalCount(0);
				return page;
			}
			patientIdFilter = primaryCarePatientId;
		}

		InsuranceTypeFilter insuranceFilter = resolveInsuranceTypeFilter(insuranceType);
		Date[] range = resolveInclusiveDayRange(startDate, endDate);
		int totalCount = pastTransfersDao.countVisitsWithRegistrationInRange(
				range[0], range[1], registrationTypeId, patientIdFilter,
				insuranceFilter.questionConceptId, insuranceFilter.valueCodedIds,
				insuranceFilter.namePatterns, insuranceFilter.includeMissing);
		page.setTotalCount(totalCount);
		if (totalCount <= 0 || safeOffset >= totalCount) {
			page.setItems(Collections.<PastTransferItem>emptyList());
			return page;
		}

		List<Object[]> visitRows = pastTransfersDao.findVisitsWithRegistrationInRange(
				range[0], range[1], registrationTypeId, patientIdFilter,
				insuranceFilter.questionConceptId, insuranceFilter.valueCodedIds,
				insuranceFilter.namePatterns, insuranceFilter.includeMissing, safeOffset, safeLimit);
		if (visitRows == null || visitRows.isEmpty()) {
			page.setItems(Collections.<PastTransferItem>emptyList());
			return page;
		}

		List<Integer> visitIds = new ArrayList<Integer>();
		Set<Integer> patientIds = new LinkedHashSet<Integer>();
		for (Object[] row : visitRows) {
			if (row == null || row.length < 4 || row[0] == null || row[1] == null) {
				continue;
			}
			visitIds.add((Integer) row[0]);
			patientIds.add((Integer) row[1]);
		}

		Concept transferIdConcept = resolveTransferIdConcept();
		Integer transferIdConceptId = transferIdConcept != null ? transferIdConcept.getConceptId() : null;
		Map<Integer, List<String>> transferIdsByVisit = transferIdConceptId != null
				? pastTransfersDao.findTransferIdValuesByVisitIds(visitIds, transferIdConceptId)
				: Collections.<Integer, List<String>>emptyMap();

		Integer insuranceTypeConceptId = resolveConceptId(
				TransferAppConstants.GP_INSURANCE_TYPE_CONCEPT_UUID,
				TransferAppConstants.DEFAULT_INSURANCE_TYPE_CONCEPT_UUID);
		Integer insuranceNumberConceptId = resolveConceptId(
				TransferAppConstants.GP_INSURANCE_NUMBER_CONCEPT_UUID,
				TransferAppConstants.DEFAULT_INSURANCE_NUMBER_CONCEPT_UUID);
		Map<Integer, String[]> insuranceByVisit = pastTransfersDao.findInsuranceByVisitIds(
				visitIds, insuranceTypeConceptId, insuranceNumberConceptId, registrationTypeId);

		TransferVerificationUrlService verificationUrlService =
				Context.getService(TransferVerificationUrlService.class);
		Map<Integer, Patient> patientsById = pastTransfersDao.findPatientsByIds(patientIds);
		Map<String, String> localUuidByPatientAndHieId = buildLocalUuidIndex(transferIdsByVisit,
				verificationUrlService);

		List<PastTransferItem> items = new ArrayList<PastTransferItem>(visitRows.size());
		for (Object[] row : visitRows) {
			if (row == null || row.length < 4 || row[0] == null || row[1] == null) {
				continue;
			}
			Integer visitId = (Integer) row[0];
			Integer patientId = (Integer) row[1];
			Date start = row[2] instanceof Date ? (Date) row[2] : null;
			Date stop = row[3] instanceof Date ? (Date) row[3] : null;
			if (start == null || start.before(range[0]) || start.after(range[1])) {
				continue;
			}
			Patient patient = patientsById.get(patientId);
			if (patient == null || Boolean.TRUE.equals(patient.getVoided())) {
				continue;
			}

			PastTransferItem item = new PastTransferItem();
			item.setVisitId(visitId);
			item.setPatientId(patientId);
			if (patient.getPersonName() != null) {
				item.setPatientName(patient.getPersonName().getFullName());
			}
			item.setUpid(patientSnapshotResolver.resolveUpid(patient));
			item.setPrimaryCareId(patientSnapshotResolver.resolvePrimaryCareId(patient));
			item.setVisitStartDatetime(start);
			item.setVisitStopDatetime(stop);

			String[] insurance = insuranceByVisit.get(visitId);
			if (insurance != null) {
				item.setInsuranceType(formatInsuranceTypeDisplay(insurance[0]));
				item.setInsuranceId(StringUtils.trimToEmpty(insurance[1]));
			}

			String transferRecords = formatTransferRecords(transferIdsByVisit.get(visitId), verificationUrlService);
			item.setTransferRecords(transferRecords);
			String primaryTransferId = firstTransferId(transferRecords);
			item.setPrimaryTransferId(primaryTransferId);
			if (primaryTransferId != null) {
				item.setLocalTransferUuid(localUuidByPatientAndHieId.get(localKey(patientId, primaryTransferId)));
			}
			items.add(item);
		}
		page.setItems(items);
		return page;
	}

	private Map<String, String> buildLocalUuidIndex(Map<Integer, List<String>> transferIdsByVisit,
			TransferVerificationUrlService verificationUrlService) {
		Set<String> hieIds = new LinkedHashSet<String>();
		for (List<String> values : transferIdsByVisit.values()) {
			if (values == null) {
				continue;
			}
			for (String value : values) {
				String transferId = StringUtils.trimToNull(value);
				if (transferId == null) {
					continue;
				}
				if (verificationUrlService != null
						&& !verificationUrlService.isValidVerificationTransferId(transferId)) {
					continue;
				}
				hieIds.add(transferId);
			}
		}
		if (hieIds.isEmpty()) {
			return Collections.emptyMap();
		}

		List<Transfer> locals = pastTransfersDao.findTransfersByHieTransferIds(hieIds);
		Map<String, String> index = new HashMap<String, String>();
		for (Transfer local : locals) {
			if (local == null || local.getPatient() == null || local.getPatient().getPatientId() == null
					|| StringUtils.isBlank(local.getHieTransferId()) || StringUtils.isBlank(local.getUuid())) {
				continue;
			}
			index.put(localKey(local.getPatient().getPatientId(), local.getHieTransferId().trim()), local.getUuid());
		}
		return index;
	}

	private String formatTransferRecords(List<String> rawValues,
			TransferVerificationUrlService verificationUrlService) {
		if (rawValues == null || rawValues.isEmpty()) {
			return "";
		}
		Set<String> transferIds = new LinkedHashSet<String>();
		for (String raw : rawValues) {
			String transferId = StringUtils.trimToNull(raw);
			if (transferId == null) {
				continue;
			}
			if (verificationUrlService != null
					&& !verificationUrlService.isValidVerificationTransferId(transferId)) {
				continue;
			}
			transferIds.add(transferId);
		}
		if (transferIds.isEmpty()) {
			return "";
		}
		StringBuilder builder = new StringBuilder();
		for (String id : transferIds) {
			if (builder.length() > 0) {
				builder.append(", ");
			}
			builder.append(id);
		}
		return builder.toString();
	}

	private String localKey(Integer patientId, String hieTransferId) {
		return String.valueOf(patientId) + "|" + hieTransferId.trim().toLowerCase(Locale.ENGLISH);
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

	private String firstTransferId(String transferRecords) {
		String value = StringUtils.trimToNull(transferRecords);
		if (value == null) {
			return null;
		}
		String[] parts = value.split(",");
		for (String part : parts) {
			String candidate = StringUtils.trimToNull(part);
			if (candidate != null) {
				return candidate;
			}
		}
		return null;
	}

	private Date[] resolveMonthRange(String yearMonth) {
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

	/**
	 * Inclusive day bounds: start at 00:00:00.000, end at 23:59:59.999.
	 * If end is before start, the dates are swapped.
	 */
	private Date[] resolveInclusiveDayRange(String startDate, String endDate) {
		Date startDay = parseDay(startDate, "Start date");
		Date endDay = parseDay(endDate, "End date");
		if (endDay.before(startDay)) {
			Date swap = startDay;
			startDay = endDay;
			endDay = swap;
		}
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(startDay);
		clearTime(calendar);
		Date start = calendar.getTime();
		calendar.setTime(endDay);
		clearTime(calendar);
		calendar.add(Calendar.DAY_OF_MONTH, 1);
		calendar.add(Calendar.MILLISECOND, -1);
		Date end = calendar.getTime();
		return new Date[] { start, end };
	}

	private Date parseDay(String value, String label) {
		String normalized = StringUtils.trimToNull(value);
		if (normalized == null || !normalized.matches("\\d{4}-\\d{2}-\\d{2}")) {
			throw new APIException(label + " is required. Use yyyy-MM-dd.");
		}
		try {
			SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
			format.setLenient(false);
			return format.parse(normalized);
		}
		catch (ParseException ex) {
			throw new APIException(label + " is invalid. Use yyyy-MM-dd.");
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

	private Integer resolveConceptId(String globalProperty, String defaultUuid) {
		String conceptUuid = StringUtils.trimToNull(Context.getAdministrationService().getGlobalProperty(
				globalProperty, defaultUuid));
		if (conceptUuid == null) {
			return null;
		}
		Concept concept = Context.getConceptService().getConceptByUuid(conceptUuid);
		return concept != null ? concept.getConceptId() : null;
	}

	/**
	 * Prefer short category labels (CBHI/RSSB/MMI/NONE/OTHER) when the registration display
	 * matches; otherwise keep the concept name.
	 */
	private String formatInsuranceTypeDisplay(String rawDisplay) {
		String display = StringUtils.trimToNull(rawDisplay);
		if (display == null) {
			return "";
		}
		String category = PatientInsuranceServiceImpl.matchCategoryByDisplayName(display);
		if (StringUtils.isNotBlank(category)) {
			return category;
		}
		return display;
	}

	private InsuranceTypeFilter resolveInsuranceTypeFilter(String insuranceType) {
		String category = StringUtils.trimToNull(insuranceType);
		if (category == null) {
			return InsuranceTypeFilter.none();
		}
		category = category.toUpperCase(Locale.ENGLISH);
		String gpName = null;
		List<String> namePatterns = Collections.emptyList();
		boolean includeMissing = false;
		if (TransferAppConstants.HEALTH_INSURANCE_CBHI.equals(category)) {
			gpName = TransferAppConstants.GP_INSURANCE_CBHI;
			namePatterns = Arrays.asList("%cbhi%", "%mutuelle%");
		} else if (TransferAppConstants.HEALTH_INSURANCE_RSSB.equals(category)) {
			gpName = TransferAppConstants.GP_INSURANCE_RSSB;
			namePatterns = Collections.singletonList("%rssb%");
		} else if (TransferAppConstants.HEALTH_INSURANCE_MMI.equals(category)) {
			gpName = TransferAppConstants.GP_INSURANCE_MMI;
			namePatterns = Collections.singletonList("%mmi%");
		} else if (TransferAppConstants.HEALTH_INSURANCE_OTHER.equals(category)) {
			gpName = TransferAppConstants.GP_INSURANCE_OTHER;
			namePatterns = Collections.singletonList("%other%");
		} else if (TransferAppConstants.HEALTH_INSURANCE_NONE.equals(category)) {
			gpName = TransferAppConstants.GP_INSURANCE_NONE;
			namePatterns = Arrays.asList("none", "n/a", "na", "no", "%no insurance%", "%without insurance%",
					"%uninsured%");
			includeMissing = true;
		} else {
			return InsuranceTypeFilter.none();
		}

		Integer questionConceptId = resolveConceptId(
				TransferAppConstants.GP_INSURANCE_TYPE_CONCEPT_UUID,
				TransferAppConstants.DEFAULT_INSURANCE_TYPE_CONCEPT_UUID);
		if (questionConceptId == null) {
			return InsuranceTypeFilter.none();
		}
		Set<Integer> codedIds = loadConceptIdsFromGlobalProperty(gpName);
		List<String> sqlPatterns = new ArrayList<String>();
		for (String pattern : namePatterns) {
			if (pattern.contains("%")) {
				sqlPatterns.add(pattern.toLowerCase(Locale.ENGLISH));
			} else {
				sqlPatterns.add(pattern.toLowerCase(Locale.ENGLISH));
			}
		}
		// Exact label matches use equality-style LIKE without wildcards when no %.
		return new InsuranceTypeFilter(questionConceptId, codedIds, sqlPatterns, includeMissing);
	}

	private Set<Integer> loadConceptIdsFromGlobalProperty(String globalPropertyName) {
		String rawValue = Context.getAdministrationService().getGlobalProperty(globalPropertyName);
		if (StringUtils.isBlank(rawValue)) {
			return Collections.emptySet();
		}
		Set<Integer> conceptIds = new HashSet<Integer>();
		for (String token : rawValue.split(",")) {
			String trimmed = StringUtils.trimToNull(token);
			if (trimmed == null) {
				continue;
			}
			try {
				conceptIds.add(Integer.valueOf(trimmed));
			}
			catch (NumberFormatException ignored) {
				Concept concept = Context.getConceptService().getConceptByUuid(trimmed);
				if (concept != null && concept.getConceptId() != null) {
					conceptIds.add(concept.getConceptId());
				}
			}
		}
		return conceptIds;
	}

	private void clearTime(Calendar calendar) {
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
	}

	private static final class InsuranceTypeFilter {
		private final Integer questionConceptId;
		private final Collection<Integer> valueCodedIds;
		private final Collection<String> namePatterns;
		private final boolean includeMissing;

		private InsuranceTypeFilter(Integer questionConceptId, Collection<Integer> valueCodedIds,
				Collection<String> namePatterns, boolean includeMissing) {
			this.questionConceptId = questionConceptId;
			this.valueCodedIds = valueCodedIds != null ? valueCodedIds : Collections.<Integer>emptySet();
			this.namePatterns = namePatterns != null ? namePatterns : Collections.<String>emptyList();
			this.includeMissing = includeMissing;
		}

		private static InsuranceTypeFilter none() {
			return new InsuranceTypeFilter(null, Collections.<Integer>emptySet(),
					Collections.<String>emptyList(), false);
		}
	}
}
