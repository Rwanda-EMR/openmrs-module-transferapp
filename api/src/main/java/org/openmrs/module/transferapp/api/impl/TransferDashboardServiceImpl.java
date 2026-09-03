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
import org.openmrs.Obs;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.ConceptService;
import org.openmrs.api.ObsService;
import org.openmrs.api.context.Context;
import org.openmrs.module.transferapp.TransferAppConstants;
import org.openmrs.module.transferapp.api.TransferAdminService;
import org.openmrs.module.transferapp.api.TransferDashboardService;
import org.openmrs.module.transferapp.api.TransferReceivedStatistics;
import org.openmrs.module.transferapp.api.dao.MaternityTransferDao;
import org.openmrs.module.transferapp.api.dao.NeonatalTransferDao;
import org.openmrs.module.transferapp.api.dao.TransferDao;

import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Default implementation of {@link TransferDashboardService}.
 */
public class TransferDashboardServiceImpl implements TransferDashboardService {

	private static final Pattern UUID_PATTERN = Pattern.compile(
			"^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

	private TransferDao transferDao;

	private MaternityTransferDao maternityTransferDao;

	private NeonatalTransferDao neonatalTransferDao;

	private TransferAdminService transferAdminService;

	public void setTransferDao(TransferDao transferDao) {
		this.transferDao = transferDao;
	}

	public void setMaternityTransferDao(MaternityTransferDao maternityTransferDao) {
		this.maternityTransferDao = maternityTransferDao;
	}

	public void setNeonatalTransferDao(NeonatalTransferDao neonatalTransferDao) {
		this.neonatalTransferDao = neonatalTransferDao;
	}

	public void setTransferAdminService(TransferAdminService transferAdminService) {
		this.transferAdminService = transferAdminService;
	}

	@Override
	public TransferReceivedStatistics getReceivedTransferStatistics() {
		TransferReceivedStatistics statistics = new TransferReceivedStatistics();

		Concept concept = getReceivedTransferConcept();
		if (concept == null) {
			return statistics;
		}

		Date startOfToday = startOfToday();
		Date startOfWeek = startOfThisWeek();

		statistics.setToday(countObservations(concept, startOfToday, null));
		statistics.setThisWeek(countObservations(concept, startOfWeek, null));
		statistics.setTotal(countObservations(concept, null, null));
		statistics.setPending(0);

		return statistics;
	}

	@Override
	public TransferReceivedStatistics getSentTransferStatistics() {
		TransferReceivedStatistics statistics = new TransferReceivedStatistics();

		String sendingFacility = transferAdminService != null
				? transferAdminService.resolveCurrentSendingFacilityName()
				: null;
		if (StringUtils.isBlank(sendingFacility)) {
			return statistics;
		}

		String facilityName = sendingFacility.trim();
		Date startOfToday = startOfToday();
		Date startOfWeek = startOfThisWeek();

		int maternityToday = maternityTransferDao != null
				? maternityTransferDao.countOutboundMaternityTransfers(facilityName, startOfToday) : 0;
		int maternityThisWeek = maternityTransferDao != null
				? maternityTransferDao.countOutboundMaternityTransfers(facilityName, startOfWeek) : 0;
		int maternityTotal = maternityTransferDao != null
				? maternityTransferDao.countOutboundMaternityTransfers(facilityName, null) : 0;
		int maternityPending = maternityTransferDao != null
				? maternityTransferDao.countOutboundMaternityTransfers(facilityName, null, Boolean.FALSE) : 0;

		int neonatalToday = neonatalTransferDao != null
				? neonatalTransferDao.countOutboundNeonatalTransfers(facilityName, startOfToday) : 0;
		int neonatalThisWeek = neonatalTransferDao != null
				? neonatalTransferDao.countOutboundNeonatalTransfers(facilityName, startOfWeek) : 0;
		int neonatalTotal = neonatalTransferDao != null
				? neonatalTransferDao.countOutboundNeonatalTransfers(facilityName, null) : 0;
		int neonatalPending = neonatalTransferDao != null
				? neonatalTransferDao.countOutboundNeonatalTransfers(facilityName, null, Boolean.FALSE) : 0;

		statistics.setToday(transferDao.countOutboundTransfers(facilityName, startOfToday, null)
				+ maternityToday + neonatalToday);
		statistics.setThisWeek(transferDao.countOutboundTransfers(facilityName, startOfWeek, null)
				+ maternityThisWeek + neonatalThisWeek);
		statistics.setTotal(transferDao.countOutboundTransfers(facilityName, null, null)
				+ maternityTotal + neonatalTotal);
		statistics.setPending(transferDao.countOutboundTransfers(facilityName, null, Boolean.FALSE)
				+ maternityPending + neonatalPending);

		return statistics;
	}

	@Override
	public Concept getReceivedTransferConcept() {
		String conceptUuid = getAdministrationService().getGlobalProperty(
				TransferAppConstants.GP_RECEIVED_TRANSFER_CONCEPT_UUID,
				TransferAppConstants.DEFAULT_RECEIVED_TRANSFER_CONCEPT_UUID);

		if (StringUtils.isBlank(conceptUuid)) {
			return null;
		}

		return getConceptService().getConceptByUuid(conceptUuid.trim());
	}

	protected int countObservations(Concept concept, Date fromDate, Date toDate) {
		List<Obs> observations = getObsService().getObservations(
				null,
				null,
				Collections.singletonList(concept),
				null,
				null,
				null,
				null,
				null,
				null,
				fromDate,
				toDate,
				false);

		if (observations == null || observations.isEmpty()) {
			return 0;
		}

		int count = 0;
		for (Obs observation : observations) {
			if (observation != null && isValidUuid(observation.getValueText())) {
				count++;
			}
		}
		return count;
	}

	/**
	 * Accepts standard UUID text (with or without braces), case-insensitive.
	 */
	protected boolean isValidUuid(String value) {
		if (StringUtils.isBlank(value)) {
			return false;
		}
		String normalized = value.trim();
		if (normalized.startsWith("{") && normalized.endsWith("}") && normalized.length() > 2) {
			normalized = normalized.substring(1, normalized.length() - 1).trim();
		}
		return UUID_PATTERN.matcher(normalized).matches();
	}

	protected Date startOfToday() {
		Calendar calendar = Calendar.getInstance();
		clearTime(calendar);
		return calendar.getTime();
	}

	protected Date startOfThisWeek() {
		Calendar calendar = Calendar.getInstance();
		clearTime(calendar);
		calendar.setFirstDayOfWeek(Calendar.MONDAY);
		calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
		return calendar.getTime();
	}

	private void clearTime(Calendar calendar) {
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
	}

	protected AdministrationService getAdministrationService() {
		return Context.getAdministrationService();
	}

	protected ConceptService getConceptService() {
		return Context.getConceptService();
	}

	protected ObsService getObsService() {
		return Context.getObsService();
	}

}
