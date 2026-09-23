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
package org.openmrs.module.transferapp.web;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.map.ObjectMapper;
import org.openmrs.api.context.Context;
import org.openmrs.module.transferapp.TransferAppActivator;
import org.openmrs.module.transferapp.TransferPrivilegeHelper;
import org.openmrs.module.transferapp.api.TransferAdminService;
import org.openmrs.module.transferapp.api.TransferFacilityRegistryService;
import org.openmrs.module.transferapp.model.ReceivingFacility;
import org.openmrs.module.transferapp.model.ReceivingService;
import org.openmrs.module.transferapp.model.RegistryFacility;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class TransferAdminController {

	private static final Log log = LogFactory.getLog(TransferAdminController.class);

	private TransferAdminService getTransferAdminService() {
		return Context.getService(TransferAdminService.class);
	}

	private TransferFacilityRegistryService getTransferFacilityRegistryService() {
		return Context.getService(TransferFacilityRegistryService.class);
	}

	@RequestMapping(value = "/module/transferapp/admin/facilityRegistry.form", method = RequestMethod.GET)
	public void listFacilityRegistry(HttpServletResponse response) throws Exception {
		Map<String, Object> data = new HashMap<String, Object>();
		if (!requireDashboardPrivilege(response, data)) {
			return;
		}
		try {
			List<RegistryFacility> facilities = getTransferFacilityRegistryService().listReceivingFacilitiesFromHie();
			data.put("status", "success");
			data.put("facilities", toFacilityMaps(facilities));
		}
		catch (Exception e) {
			log.error("Unable to load facilities from HIE registry", e);
			data.put("status", "error");
			data.put("message", resolveErrorMessage(e, TransferAppActivator.PRIVILEGE_CONFIGURATION,
					"Unable to load facilities from HIE registry"));
			data.put("facilities", new ArrayList<Map<String, Object>>());
		}
		writeJson(response, data);
	}

	@RequestMapping(value = "/module/transferapp/admin/saveFacility.form", method = RequestMethod.POST)
	public void saveFacility(HttpServletResponse response,
			@RequestParam("sendingLocationId") Integer sendingLocationId,
			@RequestParam("facilityCode") String facilityCode,
			@RequestParam("facilityName") String facilityName,
			@RequestParam(value = "distance", required = false) Integer distance,
			@RequestParam("province") String province,
			@RequestParam("district") String district,
			@RequestParam(value = "external", required = false) Boolean external) throws Exception {

		Map<String, Object> data = new HashMap<String, Object>();
		if (!requireDashboardPrivilege(response, data)) {
			return;
		}
		try {
			ReceivingFacility facility = getTransferAdminService().saveReceivingFacility(
					sendingLocationId, facilityCode, facilityName, distance, province, district, external);
			data.put("status", "success");
			data.put("receivingFacilityId", facility.getReceivingFacilityId());
			data.put("facilityCode", facility.getFacilityCode());
			data.put("facilityName", facility.getFacilityName());
			data.put("distance", facility.getDistance());
			data.put("province", facility.getProvince());
			data.put("district", facility.getDistrict());
			data.put("external", facility.isExternal());
		}
		catch (Exception e) {
			log.error("Unable to save receiving facility", e);
			data.put("status", "error");
			data.put("message", resolveErrorMessage(e, TransferAppActivator.PRIVILEGE_CONFIGURATION,
					"Unable to save receiving facility"));
		}
		writeJson(response, data);
	}

	@RequestMapping(value = "/module/transferapp/admin/voidFacility.form", method = RequestMethod.POST)
	public void voidFacility(HttpServletResponse response,
			@RequestParam("receivingFacilityId") Integer receivingFacilityId) throws Exception {

		Map<String, Object> data = new HashMap<String, Object>();
		if (!requireDashboardPrivilege(response, data)) {
			return;
		}
		try {
			getTransferAdminService().voidReceivingFacility(receivingFacilityId, null);
			data.put("status", "success");
		}
		catch (Exception e) {
			log.error("Unable to remove receiving facility", e);
			data.put("status", "error");
			data.put("message", resolveErrorMessage(e, TransferAppActivator.PRIVILEGE_CONFIGURATION,
					"Unable to remove receiving facility"));
		}
		writeJson(response, data);
	}

	@RequestMapping(value = "/module/transferapp/admin/saveService.form", method = RequestMethod.POST)
	public void saveService(HttpServletResponse response,
			@RequestParam("receivingFacilityId") Integer receivingFacilityId,
			@RequestParam("serviceName") String serviceName,
			@RequestParam(value = "receivingServiceId", required = false) Integer receivingServiceId) throws Exception {

		Map<String, Object> data = new HashMap<String, Object>();
		if (!requireDashboardPrivilege(response, data)) {
			return;
		}
		try {
			ReceivingService service = getTransferAdminService().saveReceivingService(
					receivingFacilityId, serviceName, receivingServiceId);
			data.put("status", "success");
			data.put("receivingServiceId", service.getReceivingServiceId());
			data.put("serviceName", service.getServiceName());
			data.put("updated", receivingServiceId != null);
		}
		catch (Exception e) {
			log.error("Unable to save receiving service", e);
			data.put("status", "error");
			data.put("message", resolveErrorMessage(e, TransferAppActivator.PRIVILEGE_CONFIGURATION,
					"Unable to save receiving service"));
		}
		writeJson(response, data);
	}

	@RequestMapping(value = "/module/transferapp/admin/voidService.form", method = RequestMethod.POST)
	public void voidService(HttpServletResponse response,
			@RequestParam("receivingServiceId") Integer receivingServiceId) throws Exception {

		Map<String, Object> data = new HashMap<String, Object>();
		if (!requireDashboardPrivilege(response, data)) {
			return;
		}
		try {
			getTransferAdminService().voidReceivingService(receivingServiceId, null);
			data.put("status", "success");
		}
		catch (Exception e) {
			log.error("Unable to remove receiving service", e);
			data.put("status", "error");
			data.put("message", resolveErrorMessage(e, TransferAppActivator.PRIVILEGE_CONFIGURATION,
					"Unable to remove receiving service"));
		}
		writeJson(response, data);
	}

	@RequestMapping(value = "/module/transferapp/admin/receivingServices.form", method = RequestMethod.GET)
	public void listReceivingServices(HttpServletResponse response,
			@RequestParam("receivingFacilityId") Integer receivingFacilityId) throws Exception {

		Map<String, Object> data = new HashMap<String, Object>();
		// Used by transfer wizards (clinicians) as well as admin UI.
		if (!TransferPrivilegeHelper.hasPrivilege(TransferAppActivator.PRIVILEGE_CREATE_TRANSFER)
				&& !TransferPrivilegeHelper.hasPrivilege(TransferAppActivator.PRIVILEGE_LIST_TRANSFERS)
				&& !TransferPrivilegeHelper.hasPrivilege(TransferAppActivator.PRIVILEGE_CONFIGURATION)
				&& !TransferPrivilegeHelper.hasPrivilege(TransferAppActivator.PRIVILEGE_DASHBOARD)) {
			writePrivilegeDenied(response, data, TransferAppActivator.PRIVILEGE_CREATE_TRANSFER);
			return;
		}
		try {
			List<String> services = getTransferAdminService().getReceivingServiceNamesByFacility(receivingFacilityId);
			data.put("status", "success");
			data.put("services", services != null ? services : new ArrayList<String>());
		}
		catch (Exception e) {
			log.error("Unable to load receiving services", e);
			data.put("status", "error");
			data.put("message", resolveErrorMessage(e, TransferAppActivator.PRIVILEGE_CREATE_TRANSFER,
					"Unable to load receiving services"));
			data.put("services", new ArrayList<String>());
		}
		writeJson(response, data);
	}

	private boolean requireDashboardPrivilege(HttpServletResponse response, Map<String, Object> data) throws Exception {
		if (TransferPrivilegeHelper.hasPrivilege(TransferAppActivator.PRIVILEGE_CONFIGURATION)) {
			return true;
		}
		writePrivilegeDenied(response, data, TransferAppActivator.PRIVILEGE_CONFIGURATION);
		return false;
	}

	private List<Map<String, Object>> toFacilityMaps(List<RegistryFacility> facilities) {
		List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
		if (facilities == null) {
			return rows;
		}
		for (RegistryFacility facility : facilities) {
			if (facility == null) {
				continue;
			}
			Map<String, Object> row = new HashMap<String, Object>();
			row.put("code", facility.getCode());
			row.put("name", facility.getName());
			row.put("category", facility.getCategory());
			rows.add(row);
		}
		return rows;
	}

	private String resolveErrorMessage(Exception exception, String requiredPrivilege, String fallback) {
		return TransferPrivilegeHelper.resolveUserFacingMessage(exception, requiredPrivilege, fallback);
	}

	private void writePrivilegeDenied(HttpServletResponse response, Map<String, Object> data, String privilege)
			throws Exception {
		data.put("status", "error");
		data.put("message", TransferPrivilegeHelper.requiredPrivilegeMessage(privilege));
		data.put("requiredPrivilege", privilege);
		data.put("facilities", new ArrayList<Map<String, Object>>());
		data.put("services", new ArrayList<String>());
		response.setStatus(HttpServletResponse.SC_FORBIDDEN);
		writeJson(response, data);
	}

	private void writeJson(HttpServletResponse response, Map<String, Object> data) throws Exception {
		response.setContentType("application/json");
		new ObjectMapper().writeValue(response.getOutputStream(), data);
	}

}
