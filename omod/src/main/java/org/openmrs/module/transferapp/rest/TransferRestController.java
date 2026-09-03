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
package org.openmrs.module.transferapp.rest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.api.context.Context;
import org.openmrs.module.transferapp.TransferAppActivator;
import org.openmrs.module.transferapp.TransferPrivilegeHelper;
import org.openmrs.module.transferapp.api.TransferFacilityRegistryService;
import org.openmrs.module.transferapp.api.TransferHieReceiveService;
import org.openmrs.module.transferapp.api.TransferHieSearchService;
import org.openmrs.module.transferapp.api.TransferReferralFeedbackService;
import org.openmrs.module.transferapp.api.TransferRegistrationObsService;
import org.openmrs.module.transferapp.model.RegistryFacility;
import org.openmrs.module.transferapp.model.Transfer;
import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.module.webservices.rest.web.response.IllegalRequestException;
import org.openmrs.module.webservices.rest.web.response.ResponseException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * REST API endpoint for searching patient transfers received from HIE.
 */
@Controller("transferAppRestController")
public class TransferRestController {

	protected final Log log = LogFactory.getLog(getClass());

	@RequestMapping(value = "/rest/v1/transferapp/transfer", method = RequestMethod.GET)
	@ResponseBody
	public Object getTransfer(HttpServletRequest request, HttpServletResponse response) throws ResponseException {
		String upid = request.getParameter("upid");

		if (upid == null || upid.trim().isEmpty()) {
			log.warn("Transfer REST API called without UPID parameter");
			throw new IllegalRequestException(
					"UPID parameter is required. Usage: /rest/v1/transferapp/transfer?upid=<patient-upid>[&activeOnly=true]");
		}

		if (!TransferPrivilegeHelper.hasPrivilege(TransferAppActivator.PRIVILEGE_LIST_TRANSFERS)) {
			SimpleObject denied = new SimpleObject();
			denied.put("status", "error");
			denied.put("message", TransferPrivilegeHelper.requiredPrivilegeMessage(
					TransferAppActivator.PRIVILEGE_LIST_TRANSFERS));
			denied.put("requiredPrivilege", TransferAppActivator.PRIVILEGE_LIST_TRANSFERS);
			response.setStatus(HttpServletResponse.SC_FORBIDDEN);
			return denied;
		}

		upid = upid.trim();
		String transferId = request.getParameter("transferId");
		if (transferId != null) {
			transferId = transferId.trim();
			if (transferId.isEmpty()) {
				transferId = null;
			}
		}

		String activeOnlyParam = request.getParameter("activeOnly");
		boolean activeOnly = false;
		if (activeOnlyParam != null && !activeOnlyParam.trim().isEmpty()) {
			activeOnly = Boolean.parseBoolean(activeOnlyParam.trim());
		}

		log.info("Transfer REST API endpoint called for UPID: " + upid + ", activeOnly: " + activeOnly);

		try {
			Map<String, Object> searchResult = getTransferHieSearchService().searchTransfers(upid, transferId, activeOnly);
			return toSimpleObject(searchResult);
		}
		catch (Exception ex) {
			log.error("Unable to search transfers from HIE", ex);
			SimpleObject error = new SimpleObject();
			error.put("status", "error");
			error.put("message", TransferPrivilegeHelper.resolveUserFacingMessage(
					ex,
					TransferAppActivator.PRIVILEGE_LIST_TRANSFERS,
					"Unable to search transfers"));
			if (TransferPrivilegeHelper.isPrivilegeException(ex)) {
				error.put("requiredPrivilege", TransferAppActivator.PRIVILEGE_LIST_TRANSFERS);
				response.setStatus(HttpServletResponse.SC_FORBIDDEN);
			} else {
				response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			}
			return error;
		}
	}

	@RequestMapping(value = "/rest/v1/transferapp/transfer/receive", method = RequestMethod.POST)
	@ResponseBody
	public Object receiveTransfer(HttpServletResponse response,
			@RequestParam("patientId") Integer patientId,
			@RequestParam("hieTransferId") String hieTransferId) throws ResponseException {

		if (patientId == null) {
			throw new IllegalRequestException("patientId parameter is required");
		}
		if (hieTransferId == null || hieTransferId.trim().isEmpty()) {
			throw new IllegalRequestException("hieTransferId parameter is required");
		}

		SimpleObject result = new SimpleObject();
		if (!TransferPrivilegeHelper.hasPrivilege(TransferAppActivator.PRIVILEGE_CREATE_TRANSFER)) {
			result.put("status", "error");
			result.put("message", TransferPrivilegeHelper.requiredPrivilegeMessage(
					TransferAppActivator.PRIVILEGE_CREATE_TRANSFER));
			result.put("requiredPrivilege", TransferAppActivator.PRIVILEGE_CREATE_TRANSFER);
			response.setStatus(HttpServletResponse.SC_FORBIDDEN);
			return result;
		}

		try {
			Transfer transfer = getTransferHieReceiveService().receiveTransferFromHie(patientId, hieTransferId.trim());
			result.put("status", "success");
			result.put("uuid", transfer.getUuid());
			result.put("transferId", transfer.getTransferId());
			result.put("hieTransferId", transfer.getHieTransferId());
			result.put("message", "Transfer saved successfully");
		}
		catch (Exception ex) {
			log.error("Unable to store received transfer from HIE", ex);
			result.put("status", "error");
			result.put("message", TransferPrivilegeHelper.resolveUserFacingMessage(
					ex,
					TransferAppActivator.PRIVILEGE_CREATE_TRANSFER,
					"Unable to store received transfer"));
			if (TransferPrivilegeHelper.isPrivilegeException(ex)) {
				result.put("requiredPrivilege", TransferAppActivator.PRIVILEGE_CREATE_TRANSFER);
				response.setStatus(HttpServletResponse.SC_FORBIDDEN);
			}
		}
		return result;
	}

	@RequestMapping(value = "/rest/v1/transferapp/transfer/validate", method = RequestMethod.POST)
	@ResponseBody
	public Object validateTransfer(HttpServletResponse response,
			@RequestParam("patientId") Integer patientId,
			@RequestParam("hieTransferId") String hieTransferId) throws ResponseException {

		if (patientId == null) {
			throw new IllegalRequestException("patientId parameter is required");
		}
		if (hieTransferId == null || hieTransferId.trim().isEmpty()) {
			throw new IllegalRequestException("hieTransferId parameter is required");
		}

		SimpleObject result = new SimpleObject();
		if (!TransferPrivilegeHelper.hasPrivilege(TransferAppActivator.PRIVILEGE_CREATE_TRANSFER)) {
			result.put("status", "error");
			result.put("message", TransferPrivilegeHelper.requiredPrivilegeMessage(
					TransferAppActivator.PRIVILEGE_CREATE_TRANSFER));
			result.put("requiredPrivilege", TransferAppActivator.PRIVILEGE_CREATE_TRANSFER);
			response.setStatus(HttpServletResponse.SC_FORBIDDEN);
			return result;
		}

		try {
			Map<String, Object> serviceResult = getTransferRegistrationObsService()
					.validateAndSaveTransferId(patientId, hieTransferId.trim());
			return toSimpleObject(serviceResult);
		}
		catch (Exception ex) {
			log.error("Unable to validate and record HIE transfer on registration", ex);
			result.put("status", "error");
			result.put("message", TransferPrivilegeHelper.resolveUserFacingMessage(
					ex,
					TransferAppActivator.PRIVILEGE_CREATE_TRANSFER,
					"Unable to validate transfer"));
			if (TransferPrivilegeHelper.isPrivilegeException(ex)) {
				result.put("requiredPrivilege", TransferAppActivator.PRIVILEGE_CREATE_TRANSFER);
				response.setStatus(HttpServletResponse.SC_FORBIDDEN);
			}
		}
		return result;
	}

	@RequestMapping(value = "/rest/v1/transferapp/transfer/reuseRendezvous", method = RequestMethod.POST)
	@ResponseBody
	public Object setReuseRendezvous(HttpServletResponse response,
			@RequestParam("patientId") Integer patientId,
			@RequestParam("hieTransferId") String hieTransferId,
			@RequestParam(value = "reuseDate", required = false) String reuseDate) throws ResponseException {

		if (patientId == null) {
			throw new IllegalRequestException("patientId parameter is required");
		}
		if (hieTransferId == null || hieTransferId.trim().isEmpty()) {
			throw new IllegalRequestException("hieTransferId parameter is required");
		}

		SimpleObject result = new SimpleObject();
		if (!TransferPrivilegeHelper.hasPrivilege(TransferAppActivator.PRIVILEGE_CREATE_TRANSFER)) {
			result.put("status", "error");
			result.put("message", TransferPrivilegeHelper.requiredPrivilegeMessage(
					TransferAppActivator.PRIVILEGE_CREATE_TRANSFER));
			result.put("requiredPrivilege", TransferAppActivator.PRIVILEGE_CREATE_TRANSFER);
			response.setStatus(HttpServletResponse.SC_FORBIDDEN);
			return result;
		}

		try {
			Transfer transfer = Context.getService(
					org.openmrs.module.transferapp.api.TransferHistoryService.class)
					.setReuseRendezvousDate(patientId, hieTransferId.trim(), reuseDate);
			result.put("status", "success");
			result.put("message", transfer.getReuseRendezvousDate() != null
					? "Reuse rendez-vous date saved"
					: "Reuse rendez-vous date cleared");
			result.put("hieTransferId", transfer.getHieTransferId());
			result.put("localTransferUuid", transfer.getUuid());
			if (transfer.getReuseRendezvousDate() != null) {
				result.put("reuseRendezvousDate",
						new java.text.SimpleDateFormat("yyyy-MM-dd").format(transfer.getReuseRendezvousDate()));
			}
			else {
				result.put("reuseRendezvousDate", "");
			}
			return result;
		}
		catch (Exception ex) {
			log.error("Unable to set reuse rendez-vous date", ex);
			result.put("status", "error");
			result.put("message", TransferPrivilegeHelper.resolveUserFacingMessage(
					ex,
					TransferAppActivator.PRIVILEGE_CREATE_TRANSFER,
					"Unable to save reuse rendez-vous date"));
			if (TransferPrivilegeHelper.isPrivilegeException(ex)) {
				result.put("requiredPrivilege", TransferAppActivator.PRIVILEGE_CREATE_TRANSFER);
				response.setStatus(HttpServletResponse.SC_FORBIDDEN);
			}
		}
		return result;
	}

	@RequestMapping(value = "/rest/v1/transferapp/transfer/feedback/facilities", method = RequestMethod.GET)
	@ResponseBody
	public Object listCounterReferralFacilities(HttpServletResponse response) throws ResponseException {
		SimpleObject result = new SimpleObject();
		if (!TransferPrivilegeHelper.hasPrivilege(TransferAppActivator.PRIVILEGE_LIST_TRANSFERS)) {
			result.put("status", "error");
			result.put("message", TransferPrivilegeHelper.requiredPrivilegeMessage(
					TransferAppActivator.PRIVILEGE_LIST_TRANSFERS));
			result.put("requiredPrivilege", TransferAppActivator.PRIVILEGE_LIST_TRANSFERS);
			response.setStatus(HttpServletResponse.SC_FORBIDDEN);
			return result;
		}
		try {
			List<RegistryFacility> facilities = getTransferFacilityRegistryService()
					.listCounterReferralFacilitiesFromHie();
			SimpleObject success = new SimpleObject();
			success.put("status", "success");
			success.put("facilities", toFacilityMaps(facilities));
			return success;
		}
		catch (Exception ex) {
			log.error("Unable to load counter-referral facilities", ex);
			result.put("status", "error");
			result.put("message", TransferPrivilegeHelper.resolveUserFacingMessage(
					ex,
					TransferAppActivator.PRIVILEGE_LIST_TRANSFERS,
					"Unable to load facilities from the registry"));
			result.put("facilities", new ArrayList<SimpleObject>());
			if (TransferPrivilegeHelper.isPrivilegeException(ex)) {
				result.put("requiredPrivilege", TransferAppActivator.PRIVILEGE_LIST_TRANSFERS);
				response.setStatus(HttpServletResponse.SC_FORBIDDEN);
			}
			return result;
		}
	}

	@RequestMapping(value = "/rest/v1/transferapp/transfer/feedback", method = RequestMethod.GET)
	@ResponseBody
	public Object getReferralFeedback(HttpServletResponse response,
			@RequestParam("patientId") Integer patientId,
			@RequestParam("hieTransferId") String hieTransferId) throws ResponseException {

		SimpleObject result = new SimpleObject();
		if (!TransferPrivilegeHelper.hasPrivilege(TransferAppActivator.PRIVILEGE_LIST_TRANSFERS)) {
			result.put("status", "error");
			result.put("message", TransferPrivilegeHelper.requiredPrivilegeMessage(
					TransferAppActivator.PRIVILEGE_LIST_TRANSFERS));
			result.put("requiredPrivilege", TransferAppActivator.PRIVILEGE_LIST_TRANSFERS);
			response.setStatus(HttpServletResponse.SC_FORBIDDEN);
			return result;
		}
		if (patientId == null) {
			throw new IllegalRequestException("patientId parameter is required");
		}
		if (hieTransferId == null || hieTransferId.trim().isEmpty()) {
			throw new IllegalRequestException("hieTransferId parameter is required");
		}
		try {
			Map<String, Object> serviceResult = getTransferReferralFeedbackService()
					.getFeedbackForm(patientId, hieTransferId.trim());
			boolean hieSent = Boolean.TRUE.equals(serviceResult.get("hieSent"));
			serviceResult.put("canSubmit", TransferPrivilegeHelper.hasPrivilege(
					TransferAppActivator.PRIVILEGE_CREATE_TRANSFER) && !hieSent);
			return toSimpleObject(serviceResult);
		}
		catch (Exception ex) {
			log.error("Unable to load referral feedback", ex);
			result.put("status", "error");
			result.put("message", TransferPrivilegeHelper.resolveUserFacingMessage(
					ex,
					TransferAppActivator.PRIVILEGE_LIST_TRANSFERS,
					"Unable to load referral feedback"));
			if (TransferPrivilegeHelper.isPrivilegeException(ex)) {
				result.put("requiredPrivilege", TransferAppActivator.PRIVILEGE_LIST_TRANSFERS);
				response.setStatus(HttpServletResponse.SC_FORBIDDEN);
			}
			return result;
		}
	}

	@RequestMapping(value = "/rest/v1/transferapp/transfer/feedback", method = RequestMethod.POST)
	@ResponseBody
	public Object saveReferralFeedback(HttpServletResponse response,
			@RequestParam("patientId") Integer patientId,
			@RequestParam("hieTransferId") String hieTransferId,
			@RequestParam("dateOfDischarge") String dateOfDischarge,
			@RequestParam("finalDiagnosis") String finalDiagnosis,
			@RequestParam("treatmentGiven") String treatmentGiven,
			@RequestParam("outcome") String outcome,
			@RequestParam("recommendations") String recommendations,
			@RequestParam("referBackToFacility") String referBackToFacility,
			@RequestParam("contactPerson") String contactPerson,
			@RequestParam("providerName") String providerName,
			@RequestParam("qualification") String qualification,
			@RequestParam("signedDate") String signedDate,
			@RequestParam("signedTime") String signedTime,
			@RequestParam("phone") String phone) throws ResponseException {

		SimpleObject result = new SimpleObject();
		if (!TransferPrivilegeHelper.hasPrivilege(TransferAppActivator.PRIVILEGE_CREATE_TRANSFER)) {
			result.put("status", "error");
			result.put("message", TransferPrivilegeHelper.requiredPrivilegeMessage(
					TransferAppActivator.PRIVILEGE_CREATE_TRANSFER));
			result.put("requiredPrivilege", TransferAppActivator.PRIVILEGE_CREATE_TRANSFER);
			response.setStatus(HttpServletResponse.SC_FORBIDDEN);
			return result;
		}
		try {
			return toSimpleObject(getTransferReferralFeedbackService().saveFeedback(
					patientId, hieTransferId, dateOfDischarge, finalDiagnosis, treatmentGiven, outcome,
					recommendations, referBackToFacility, contactPerson, providerName, qualification,
					signedDate, signedTime, phone));
		}
		catch (Exception ex) {
			log.error("Unable to save referral feedback", ex);
			result.put("status", "error");
			result.put("message", TransferPrivilegeHelper.resolveUserFacingMessage(
					ex,
					TransferAppActivator.PRIVILEGE_CREATE_TRANSFER,
					"Unable to save referral feedback"));
			if (TransferPrivilegeHelper.isPrivilegeException(ex)) {
				result.put("requiredPrivilege", TransferAppActivator.PRIVILEGE_CREATE_TRANSFER);
				response.setStatus(HttpServletResponse.SC_FORBIDDEN);
			}
			return result;
		}
	}

	private TransferHieSearchService getTransferHieSearchService() {
		return Context.getService(TransferHieSearchService.class);
	}

	private TransferHieReceiveService getTransferHieReceiveService() {
		return Context.getService(TransferHieReceiveService.class);
	}

	private TransferRegistrationObsService getTransferRegistrationObsService() {
		return Context.getService(TransferRegistrationObsService.class);
	}

	private TransferFacilityRegistryService getTransferFacilityRegistryService() {
		return Context.getService(TransferFacilityRegistryService.class);
	}

	private TransferReferralFeedbackService getTransferReferralFeedbackService() {
		return Context.getService(TransferReferralFeedbackService.class);
	}

	private List<SimpleObject> toFacilityMaps(List<RegistryFacility> facilities) {
		List<SimpleObject> rows = new ArrayList<SimpleObject>();
		if (facilities == null) {
			return rows;
		}
		for (RegistryFacility facility : facilities) {
			if (facility == null) {
				continue;
			}
			SimpleObject row = new SimpleObject();
			row.put("code", facility.getCode());
			row.put("name", facility.getName());
			row.put("category", facility.getCategory());
			rows.add(row);
		}
		return rows;
	}

	@SuppressWarnings("unchecked")
	private SimpleObject toSimpleObject(Map<String, Object> source) {
		SimpleObject result = new SimpleObject();
		if (source == null) {
			return result;
		}
		for (Map.Entry<String, Object> entry : source.entrySet()) {
			String key = entry.getKey();
			Object value = entry.getValue();
			if ("data".equals(key) && value instanceof List) {
				result.put(key, toSimpleObjectList((List<Map<String, Object>>) value));
			}
			else if ("defaults".equals(key) && value instanceof Map) {
				result.put(key, toSimpleObject((Map<String, Object>) value));
			}
			else if ("feedback".equals(key) && value instanceof Map) {
				result.put(key, toSimpleObject((Map<String, Object>) value));
			}
			else if ("outcomes".equals(key) && value instanceof List) {
				result.put(key, toSimpleObjectList((List<Map<String, Object>>) value));
			}
			else {
				result.put(key, value);
			}
		}
		return result;
	}

	private List<SimpleObject> toSimpleObjectList(List<Map<String, Object>> items) {
		List<SimpleObject> converted = new ArrayList<SimpleObject>();
		if (items == null) {
			return converted;
		}
		for (Map<String, Object> item : items) {
			SimpleObject row = new SimpleObject();
			if (item != null) {
				for (Map.Entry<String, Object> entry : item.entrySet()) {
					row.put(entry.getKey(), entry.getValue());
				}
			}
			converted.add(row);
		}
		return converted;
	}

}
