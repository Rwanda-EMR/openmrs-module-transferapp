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
import org.openmrs.Patient;
import org.openmrs.api.context.Context;
import org.openmrs.module.transferapp.TransferAppConstants;
import org.openmrs.module.transferapp.api.PendingTransferPatientStatusResolver;
import org.openmrs.module.transferapp.api.TransferHieSearchService;
import org.openmrs.module.transferapp.api.TransferRegistrationObsService;
import org.openmrs.module.transferapp.api.TransferVerificationUrlService;
import org.openmrs.module.transferapp.api.TransferSendingLocationResolver;
import org.openmrs.module.transferapp.api.dao.TransferDao;
import org.openmrs.module.transferapp.hie.HieApiException;
import org.openmrs.module.transferapp.hie.HieBasicConnection;
import org.openmrs.module.transferapp.hie.HieConnectionResolver;
import org.openmrs.module.transferapp.hie.HieShrClient;
import org.openmrs.module.transferapp.hie.HieTransferResponsePage;
import org.openmrs.module.transferapp.hie.HieTransferResponseParser;
import org.openmrs.module.transferapp.model.Transfer;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class TransferHieSearchServiceImpl implements TransferHieSearchService {

	private static final Log log = LogFactory.getLog(TransferHieSearchServiceImpl.class);

	static final int HIE_PAGE_SIZE = 100;

	static final int HIE_MAX_PAGES = 10000;

	private HieConnectionResolver hieConnectionResolver = new HieConnectionResolver();

	private HieShrClient hieShrClient = new HieShrClient();

	private HieTransferResponseParser responseParser = new HieTransferResponseParser();

	private TransferSendingLocationResolver sendingLocationResolver = new TransferSendingLocationResolver();

	private TransferDao transferDao;

	public void setTransferDao(TransferDao transferDao) {
		this.transferDao = transferDao;
	}

	@Override
	public Map<String, Object> searchTransfers(String upid, String transferId, boolean activeOnly) {
		String fromDate = null;
		String endDate = null;
		if (activeOnly) {
			LocalDate today = LocalDate.now();
			fromDate = today.minusDays(31).format(DateTimeFormatter.ISO_LOCAL_DATE);
			endDate = today.plusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE);
		}
		return searchTransfersInternal(upid, transferId, fromDate, endDate, activeOnly);
	}

	@Override
	public Map<String, Object> searchTransfers(String upid, String transferId, String fromDate, String endDate) {
		String normalizedFrom = StringUtils.trimToNull(fromDate);
		String normalizedEnd = StringUtils.trimToNull(endDate);
		if (normalizedFrom == null || normalizedEnd == null) {
			normalizedFrom = null;
			normalizedEnd = null;
		}
		return searchTransfersInternal(upid, transferId, normalizedFrom, normalizedEnd, false);
	}

	private Map<String, Object> searchTransfersInternal(String upid, String transferId, String fromDate,
			String endDate, boolean mergeScheduledReuse) {
		Map<String, Object> result = new LinkedHashMap<String, Object>();

		if (StringUtils.isBlank(upid)) {
			result.put("status", "error");
			result.put("message", "UPID parameter is required");
			result.put("data", Collections.emptyList());
			return result;
		}

		if (!hieConnectionResolver.isHieConfigured()) {
			log.warn("HIE is not configured for transfer search");
			result.put("status", "error");
			result.put("message", "HIE is not enabled");
			result.put("data", Collections.emptyList());
			return result;
		}

		try {
			HieBasicConnection connection = hieConnectionResolver.resolveConnection();
			String pathWithQuery = buildPatientListTransfersPath(upid.trim(), fromDate, endDate);
			log.info("Requesting transfers from HIE: " + connection.getBaseUrl() + pathWithQuery);

			List<Map<String, Object>> transfers = enrichTransfers(
					fetchAllTransferPages(connection, pathWithQuery));
			if (StringUtils.isNotBlank(transferId)) {
				transfers = filterByTransferId(transfers, transferId.trim());
			}
			if (mergeScheduledReuse) {
				transfers = mergeScheduledReuseTransfers(transfers, upid.trim(), fromDate, endDate);
			}

			result.put("status", "success");
			result.put("fromDate", fromDate != null ? fromDate : "");
			result.put("endDate", endDate != null ? endDate : "");
			result.put("data", transfers);
			return result;
		}
		catch (Exception ex) {
			log.error("Error fetching transfers from HIE for UPID: " + upid, ex);
			result.put("status", "error");
			result.put("message", ex.getMessage() != null ? ex.getMessage() : "Failed to fetch transfers from HIE");
			result.put("data", Collections.emptyList());
			return result;
		}
	}

	@Override
	public Map<String, Object> listPendingTransfersForCurrentFacility() {
		return listPendingTransfersForCurrentFacility(DEFAULT_PENDING_WEEKS);
	}

	@Override
	public Map<String, Object> listPendingTransfersForCurrentFacility(int weeks) {
		Map<String, Object> result = new LinkedHashMap<String, Object>();
		int selectedWeeks = normalizePendingWeeks(weeks);

		List<String> targetOrgs = sendingLocationResolver.resolveCurrentSendingFacilityNames();
		if (targetOrgs == null || targetOrgs.isEmpty()) {
			result.put("status", "error");
			result.put("message", "Current facility location is not available");
			result.put("targetOrg", "");
			result.put("data", Collections.emptyList());
			return result;
		}
		String targetOrgDisplay = joinFacilityNames(targetOrgs);

		if (!hieConnectionResolver.isHieConfigured()) {
			log.warn("HIE is not configured for pending transfer list");
			result.put("status", "error");
			result.put("message", "HIE is not enabled");
			result.put("targetOrg", targetOrgDisplay);
			result.put("data", Collections.emptyList());
			return result;
		}

		try {
			LocalDate today = LocalDate.now();
			String fromDate = calculatePendingFromDate(today, selectedWeeks)
					.format(DateTimeFormatter.ISO_LOCAL_DATE);
			String endDate = today.plusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE);

			HieBasicConnection connection = hieConnectionResolver.resolveConnection();
			List<Map<String, Object>> transfers = new ArrayList<Map<String, Object>>();
			Set<String> seenIds = new HashSet<String>();
			for (String targetOrg : targetOrgs) {
				String pathWithQuery = buildTargetOrgListTransfersPath(targetOrg, fromDate, endDate);
				log.info("Requesting pending transfers from HIE: " + connection.getBaseUrl() + pathWithQuery);
				List<Map<String, Object>> pageTransfers = fetchAllTransferPages(connection, pathWithQuery);
				if (pageTransfers == null) {
					continue;
				}
				for (Map<String, Object> transfer : pageTransfers) {
					if (transfer == null) {
						continue;
					}
					String id = asString(transfer.get("id"));
					if (StringUtils.isNotBlank(id) && !seenIds.add(id)) {
						continue;
					}
					transfers.add(transfer);
				}
			}
			result.put("status", "success");
			result.put("targetOrg", targetOrgDisplay);
			result.put("fromDate", fromDate);
			result.put("endDate", endDate);
			result.put("weeks", selectedWeeks);
			result.put("data", enrichTransfers(transfers));
			return result;
		}
		catch (Exception ex) {
			log.error("Error fetching pending transfers from HIE for targetOrg: " + targetOrgDisplay, ex);
			result.put("status", "error");
			result.put("message", ex.getMessage() != null ? ex.getMessage() : "Failed to fetch pending transfers from HIE");
			result.put("targetOrg", targetOrgDisplay);
			result.put("data", Collections.emptyList());
			return result;
		}
	}

	private static String joinFacilityNames(List<String> names) {
		if (names == null || names.isEmpty()) {
			return "";
		}
		StringBuilder builder = new StringBuilder();
		for (String name : names) {
			if (StringUtils.isBlank(name)) {
				continue;
			}
			if (builder.length() > 0) {
				builder.append(", ");
			}
			builder.append(name.trim());
		}
		return builder.toString();
	}

	static int normalizePendingWeeks(int weeks) {
		return weeks >= DEFAULT_PENDING_WEEKS && weeks <= MAX_PENDING_WEEKS
				? weeks : DEFAULT_PENDING_WEEKS;
	}

	static LocalDate calculatePendingFromDate(LocalDate today, int weeks) {
		return today.minusDays(normalizePendingWeeks(weeks) * 7L);
	}

	List<Map<String, Object>> fetchAllTransferPages(HieBasicConnection connection, String pathWithQuery)
			throws Exception {
		List<Map<String, Object>> allTransfers = new ArrayList<Map<String, Object>>();
		Set<String> seenTransferIds = new HashSet<String>();
		int rawLoadedCount = 0;

		for (int page = 1; page <= HIE_MAX_PAGES; page++) {
			String pagedPath = appendPagination(pathWithQuery, page, HIE_PAGE_SIZE);
			String responseJson = hieShrClient.get(connection, pagedPath);
			validateHieResponse(responseJson);

			HieTransferResponsePage responsePage = responseParser.parsePage(responseJson);
			List<Map<String, Object>> batch = responsePage.getTransfers();
			if (batch == null || batch.isEmpty()) {
				log.info("HIE transfer list loaded " + allTransfers.size() + " unique entries across "
						+ page + " page(s)");
				return allTransfers;
			}

			rawLoadedCount += batch.size();
			for (Map<String, Object> transfer : batch) {
				String transferId = transfer != null ? asString(transfer.get("id")) : "";
				if (StringUtils.isBlank(transferId) || seenTransferIds.add(transferId)) {
					allTransfers.add(transfer);
				}
			}

			if (!shouldFetchNextPage(responsePage, batch.size(), rawLoadedCount)) {
				log.info("HIE transfer list loaded " + allTransfers.size() + " unique entries across "
						+ page + " page(s)");
				return allTransfers;
			}
		}

		throw new HieApiException("HIE transfer pagination exceeded the safety limit of "
				+ HIE_MAX_PAGES + " pages");
	}

	static boolean shouldFetchNextPage(HieTransferResponsePage responsePage, int batchSize, int loadedCount) {
		if (batchSize <= 0) {
			return false;
		}
		if (responsePage.hasMore()) {
			return true;
		}
		Integer total = responsePage.getTotal();
		if (total != null) {
			return loadedCount < total;
		}
		return batchSize >= HIE_PAGE_SIZE;
	}

	private static String appendPagination(String pathWithQuery, int page, int size) {
		String separator = pathWithQuery.contains("?") ? "&" : "?";
		return pathWithQuery + separator + "page=" + page + "&size=" + size;
	}

	private static String buildPatientListTransfersPath(String upid, String fromDate, String endDate)
			throws UnsupportedEncodingException {
		StringBuilder path = new StringBuilder(TransferAppConstants.HIE_LIST_TRANSFERS_PATH);
		path.append("?patient=").append(URLEncoder.encode(upid, "UTF-8"));
		if (fromDate != null && endDate != null) {
			path.append("&fromDate=").append(URLEncoder.encode(fromDate, "UTF-8"));
			path.append("&endDate=").append(URLEncoder.encode(endDate, "UTF-8"));
		}
		return path.toString();
	}

	private static String buildTargetOrgListTransfersPath(String targetOrg, String fromDate, String endDate)
			throws UnsupportedEncodingException {
		StringBuilder path = new StringBuilder(TransferAppConstants.HIE_LIST_TRANSFERS_PATH);
		path.append("?targetOrg=").append(URLEncoder.encode(targetOrg, "UTF-8"));
		path.append("&fromDate=").append(URLEncoder.encode(fromDate, "UTF-8"));
		path.append("&endDate=").append(URLEncoder.encode(endDate, "UTF-8"));
		return path.toString();
	}

	private List<Map<String, Object>> mergeScheduledReuseTransfers(List<Map<String, Object>> hieTransfers,
			String upid, String fromDateIso, String endDateIso) {
		List<Map<String, Object>> merged = new ArrayList<Map<String, Object>>();
		Set<String> seenIds = new HashSet<String>();
		if (hieTransfers != null) {
			for (Map<String, Object> item : hieTransfers) {
				if (item == null) {
					continue;
				}
				String id = firstNonBlank(asString(item.get("id")), asString(item.get("uuid")),
						asString(item.get("hieTransferId")));
				if (StringUtils.isNotBlank(id)) {
					seenIds.add(id.toLowerCase(Locale.ENGLISH));
				}
				merged.add(item);
			}
		}
		if (transferDao == null || StringUtils.isBlank(upid)) {
			return merged;
		}
		Patient patient;
		try {
			patient = PendingTransferPatientStatusResolver.findLocalPatientByUpid(
					Context.getPatientService(), upid);
		}
		catch (Exception ex) {
			log.warn("Unable to resolve patient for reuse rendez-vous merge: " + upid, ex);
			return merged;
		}
		if (patient == null || patient.getPatientId() == null) {
			return merged;
		}
		Date fromDate = parseIsoDateStart(fromDateIso);
		Date toDate = parseIsoDateStart(endDateIso);
		if (fromDate == null && toDate == null) {
			return merged;
		}
		List<Transfer> scheduled = transferDao.getTransfersByReuseRendezvousDate(
				patient.getPatientId(), fromDate, toDate);
		if (scheduled == null || scheduled.isEmpty()) {
			return merged;
		}
		SimpleDateFormat dayFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
		for (Transfer transfer : scheduled) {
			if (transfer == null || StringUtils.isBlank(transfer.getHieTransferId())) {
				continue;
			}
			String hieId = transfer.getHieTransferId().trim();
			if (seenIds.contains(hieId.toLowerCase(Locale.ENGLISH))) {
				continue;
			}
			seenIds.add(hieId.toLowerCase(Locale.ENGLISH));
			merged.add(toScheduledReusePreviewMap(transfer, upid, dayFormat));
		}
		return merged;
	}

	private Map<String, Object> toScheduledReusePreviewMap(Transfer transfer, String upid,
			SimpleDateFormat dayFormat) {
		Map<String, Object> map = new LinkedHashMap<String, Object>();
		String hieId = transfer.getHieTransferId().trim();
		String rendezvous = transfer.getReuseRendezvousDate() != null
				? dayFormat.format(transfer.getReuseRendezvousDate())
				: "";
		map.put("id", hieId);
		map.put("uuid", hieId);
		map.put("hieTransferId", hieId);
		map.put("subject", upid);
		map.put("upid", upid);
		map.put("date", rendezvous);
		map.put("origin", StringUtils.defaultString(transfer.getSendingFacility()));
		map.put("referringFacilityName", StringUtils.defaultString(transfer.getSendingFacility()));
		map.put("destination", StringUtils.defaultString(transfer.getReceivingFacilityCode()));
		map.put("receivingFacility", StringUtils.defaultString(transfer.getReceivingFacilityCode()));
		map.put("hospitalName", StringUtils.defaultString(transfer.getReceivingFacilityCode()));
		map.put("receivedFromHie", Boolean.TRUE);
		map.put("reuseScheduled", Boolean.TRUE);
		map.put("reuseRendezvousDate", rendezvous);
		map.put("status", "Scheduled reuse (" + rendezvous + ")");
		map.put("targetsCurrentFacility", Boolean.TRUE);
		TransferVerificationUrlService verificationUrlService = Context.getService(TransferVerificationUrlService.class);
		if (verificationUrlService != null) {
			verificationUrlService.enrichPreviewVerificationFields(map);
		}
		return map;
	}

	private static Date parseIsoDateStart(String isoDate) {
		if (StringUtils.isBlank(isoDate)) {
			return null;
		}
		try {
			Date parsed = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).parse(isoDate.trim());
			Calendar calendar = Calendar.getInstance();
			calendar.setTime(parsed);
			calendar.set(Calendar.HOUR_OF_DAY, 0);
			calendar.set(Calendar.MINUTE, 0);
			calendar.set(Calendar.SECOND, 0);
			calendar.set(Calendar.MILLISECOND, 0);
			return calendar.getTime();
		}
		catch (Exception ex) {
			return null;
		}
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

	private static List<Map<String, Object>> enrichTransfers(List<Map<String, Object>> transfers) {
		if (transfers == null) {
			return Collections.emptyList();
		}
		TransferVerificationUrlService verificationUrlService = Context.getService(TransferVerificationUrlService.class);
		TransferRegistrationObsService registrationObsService = Context.getService(TransferRegistrationObsService.class);
		for (Map<String, Object> transfer : transfers) {
			if (transfer == null) {
				continue;
			}
			String uuid = asString(transfer.get("id"));
			String upid = asString(transfer.get("subject"));
			transfer.put("uuid", uuid);
			transfer.put("upid", upid);
			transfer.put("hieTransferId", uuid);
			transfer.put("receivedFromHie", Boolean.TRUE);
			String destination = TransferRegistrationObsServiceImpl.resolveDestination(transfer);
			transfer.put("destinationDisplay", destination);
			String toFacility = firstNonBlank(
					asString(transfer.get("destinationDisplay")),
					asString(transfer.get("destination")),
					asString(transfer.get("receivingFacility")),
					asString(transfer.get("toFacility")));
			if (registrationObsService != null) {
				boolean targetsCurrent = registrationObsService.destinationMatchesSendingFacilityName(toFacility)
						|| registrationObsService.destinationMatchesSendingFacilityName(destination);
				transfer.put("targetsCurrentFacility", targetsCurrent);
			}
			else {
				transfer.put("targetsCurrentFacility", Boolean.FALSE);
			}
			if (verificationUrlService != null) {
				verificationUrlService.enrichPreviewVerificationFields(transfer);
			}
		}
		return transfers;
	}

	private static String asString(Object value) {
		return value == null ? "" : String.valueOf(value).trim();
	}

	private static void validateHieResponse(String responseJson) {
		if (responseJson == null || responseJson.trim().isEmpty()) {
			return;
		}
		if (responseJson.contains("\"resourceType\":\"OperationOutcome\"")) {
			String errorMessage = "HIE returned OperationOutcome error";
			if (responseJson.length() < 500) {
				errorMessage += ". Response: " + responseJson;
			}
			throw new HieApiException(errorMessage);
		}
	}

	private static List<Map<String, Object>> filterByTransferId(List<Map<String, Object>> transfers, String transferId) {
		List<Map<String, Object>> filtered = new ArrayList<Map<String, Object>>();
		for (Map<String, Object> item : transfers) {
			Object idObj = item.get("id");
			if (idObj != null && transferId.equals(String.valueOf(idObj))) {
				filtered.add(item);
				break;
			}
		}
		return filtered;
	}

	void setHieShrClient(HieShrClient hieShrClient) {
		this.hieShrClient = hieShrClient;
	}

}
