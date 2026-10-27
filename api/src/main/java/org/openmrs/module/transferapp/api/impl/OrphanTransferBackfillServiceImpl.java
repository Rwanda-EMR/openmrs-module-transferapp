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
import org.openmrs.api.APIException;
import org.openmrs.api.context.Context;
import org.openmrs.module.transferapp.TransferAppConstants;
import org.openmrs.module.transferapp.api.OrphanTransferBackfillService;
import org.openmrs.module.transferapp.api.TransferHieReceiveService;
import org.openmrs.module.transferapp.api.dao.PastTransfersDao;
import org.openmrs.module.transferapp.hie.HieBasicConnection;
import org.openmrs.module.transferapp.hie.HieConnectionResolver;
import org.openmrs.module.transferapp.hie.HieShrClient;
import org.openmrs.module.transferapp.hie.HieTransferResponseParser;
import org.openmrs.module.transferapp.model.OrphanTransferIdObs;
import org.openmrs.module.transferapp.model.Transfer;

import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class OrphanTransferBackfillServiceImpl implements OrphanTransferBackfillService {

	private static final Log log = LogFactory.getLog(OrphanTransferBackfillServiceImpl.class);

	private PastTransfersDao pastTransfersDao;

	private TransferHieReceiveService transferHieReceiveService;

	private HieConnectionResolver hieConnectionResolver = new HieConnectionResolver();

	private HieShrClient hieShrClient = new HieShrClient();

	private HieTransferResponseParser responseParser = new HieTransferResponseParser();

	public void setPastTransfersDao(PastTransfersDao pastTransfersDao) {
		this.pastTransfersDao = pastTransfersDao;
	}

	public void setTransferHieReceiveService(TransferHieReceiveService transferHieReceiveService) {
		this.transferHieReceiveService = transferHieReceiveService;
	}

	void setHieConnectionResolver(HieConnectionResolver hieConnectionResolver) {
		this.hieConnectionResolver = hieConnectionResolver;
	}

	void setHieShrClient(HieShrClient hieShrClient) {
		this.hieShrClient = hieShrClient;
	}

	void setResponseParser(HieTransferResponseParser responseParser) {
		this.responseParser = responseParser;
	}

	@Override
	public boolean isBackfillEnabled() {
		String value = Context.getAdministrationService().getGlobalProperty(
				TransferAppConstants.GP_ORPHAN_TRANSFER_BACKFILL_ENABLED,
				TransferAppConstants.DEFAULT_ORPHAN_TRANSFER_BACKFILL_ENABLED);
		return "true".equalsIgnoreCase(StringUtils.trimToEmpty(value));
	}

	@Override
	public int getBatchSize() {
		return parsePositiveInt(
				Context.getAdministrationService().getGlobalProperty(
						TransferAppConstants.GP_ORPHAN_TRANSFER_BACKFILL_BATCH_SIZE,
						TransferAppConstants.DEFAULT_ORPHAN_TRANSFER_BACKFILL_BATCH_SIZE),
				50);
	}

	@Override
	public int getDelaySeconds() {
		return parseNonNegativeInt(
				Context.getAdministrationService().getGlobalProperty(
						TransferAppConstants.GP_ORPHAN_TRANSFER_BACKFILL_DELAY_SECONDS,
						TransferAppConstants.DEFAULT_ORPHAN_TRANSFER_BACKFILL_DELAY_SECONDS),
				30);
	}

	@Override
	public int getLookbackDays() {
		return parsePositiveInt(
				Context.getAdministrationService().getGlobalProperty(
						TransferAppConstants.GP_ORPHAN_TRANSFER_BACKFILL_LOOKBACK_DAYS,
						TransferAppConstants.DEFAULT_ORPHAN_TRANSFER_BACKFILL_LOOKBACK_DAYS),
				1);
	}

	@Override
	public List<OrphanTransferIdObs> findOrphanTransferIdObs() {
		if (pastTransfersDao == null) {
			return Collections.emptyList();
		}
		Concept concept = resolveTransferIdConcept();
		if (concept == null || concept.getConceptId() == null) {
			log.warn("Orphan transfer backfill: Transfer Id concept is not configured or missing");
			return Collections.emptyList();
		}
		Date createdOnOrAfter = lookbackStart(getLookbackDays());
		return pastTransfersDao.findOrphanTransferIdObs(concept.getConceptId(), createdOnOrAfter, getBatchSize());
	}

	@Override
	public Transfer backfillOne(Integer patientId, String hieTransferId) {
		if (patientId == null) {
			throw new APIException("Patient is required");
		}
		String normalizedId = StringUtils.trimToNull(hieTransferId);
		if (normalizedId == null) {
			throw new APIException("HIE transfer id is required");
		}
		if (transferHieReceiveService == null) {
			throw new APIException("Transfer HIE receive service is not configured");
		}
		if (!hieConnectionResolver.isHieConfigured()) {
			throw new APIException("HIE connection is not configured");
		}

		HieBasicConnection connection = hieConnectionResolver.resolveConnection();
		String json;
		try {
			json = hieShrClient.fetchEncounterById(connection, normalizedId);
		}
		catch (Exception ex) {
			throw new APIException("Failed to fetch HIE Encounter " + normalizedId + ": "
					+ (ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName()), ex);
		}
		if (StringUtils.isBlank(json)) {
			log.info("Orphan transfer backfill: no HIE Encounter for id " + normalizedId
					+ " (patient " + patientId + ")");
			return null;
		}

		Map<String, Object> hieTransfer;
		try {
			hieTransfer = responseParser.parseEncounter(json);
		}
		catch (Exception ex) {
			throw new APIException("Failed to parse HIE Encounter " + normalizedId + ": "
					+ (ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName()), ex);
		}
		if (hieTransfer == null || hieTransfer.isEmpty()) {
			log.info("Orphan transfer backfill: empty parse result for HIE id " + normalizedId);
			return null;
		}
		if (StringUtils.isBlank(String.valueOf(hieTransfer.get("id")))
				&& StringUtils.isBlank(String.valueOf(hieTransfer.get("uuid")))) {
			hieTransfer.put("id", normalizedId);
		}
		return transferHieReceiveService.storeReceivedTransfer(patientId, hieTransfer);
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

	private static Date lookbackStart(int lookbackDays) {
		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.DAY_OF_MONTH, -Math.max(1, lookbackDays));
		return calendar.getTime();
	}

	private static int parsePositiveInt(String raw, int defaultValue) {
		try {
			int value = Integer.parseInt(StringUtils.trimToEmpty(raw));
			return value > 0 ? value : defaultValue;
		}
		catch (Exception ignored) {
			return defaultValue;
		}
	}

	private static int parseNonNegativeInt(String raw, int defaultValue) {
		try {
			int value = Integer.parseInt(StringUtils.trimToEmpty(raw));
			return value >= 0 ? value : defaultValue;
		}
		catch (Exception ignored) {
			return defaultValue;
		}
	}
}
