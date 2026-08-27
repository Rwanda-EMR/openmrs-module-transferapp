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

import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.openmrs.api.context.Context;
import org.openmrs.module.transferapp.TransferAppActivator;
import org.openmrs.module.transferapp.TransferAppConstants;
import org.openmrs.module.transferapp.TransferAppMode;
import org.openmrs.module.transferapp.TransferPrivilegeHelper;
import org.openmrs.module.transferapp.PatientTransferIdentifierDetector;
import org.openmrs.module.transferapp.api.PendingTransferPatientStatusResolver;
import org.openmrs.module.transferapp.api.ClientRegistryRegistrationService;
import org.openmrs.module.transferapp.api.TransferAdminService;
import org.openmrs.module.transferapp.api.TransferAmbulanceVoucherService;
import org.openmrs.module.transferapp.api.TransferHieSearchService;
import org.openmrs.module.transferapp.api.TransferHieSubmissionService;
import org.openmrs.module.transferapp.api.TransferProfileService;
import org.openmrs.module.transferapp.api.TransferQrCodeService;
import org.openmrs.module.transferapp.api.TransferService;
import org.openmrs.module.transferapp.api.TransferVerificationUrlService;
import org.openmrs.module.transferapp.api.impl.PatientInsuranceServiceImpl;
import org.openmrs.module.transferapp.model.AmbulanceVoucherPreview;
import org.openmrs.module.transferapp.model.RegistryFacility;
import org.openmrs.module.transferapp.model.Transfer;
import org.openmrs.module.transferapp.model.TransferFormExtras;
import org.openmrs.module.transferapp.model.TransferProfile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletResponse;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class TransferSaveController {

	private static final String DATETIME_PATTERN = "dd.MMM.yyyy, HH:mm:ss";

	@Autowired
	private TransferService transferService;

	@Autowired
	private TransferAdminService transferAdminService;

	@Autowired
	private TransferAmbulanceVoucherService transferAmbulanceVoucherService;

	@Autowired(required = false)
	@Qualifier("transferAppClientRegistryRegistrationService")
	private ClientRegistryRegistrationService clientRegistryRegistrationService;

	private TransferHieSubmissionService getTransferHieSubmissionService() {
		return Context.getService(TransferHieSubmissionService.class);
	}

	private TransferHieSearchService getTransferHieSearchService() {
		return Context.getService(TransferHieSearchService.class);
	}

	private TransferVerificationUrlService getTransferVerificationUrlService() {
		return Context.getService(TransferVerificationUrlService.class);
	}

	private TransferQrCodeService getTransferQrCodeService() {
		return Context.getService(TransferQrCodeService.class);
	}

	@RequestMapping(value = "/module/transferapp/transfer/hiePatientRegistrationPreview.form", method = RequestMethod.GET)
	public void previewHiePatientRegistration(HttpServletResponse response,
			@RequestParam("upid") String upid) throws Exception {

		Map<String, Object> data = new HashMap<String, Object>();
		if (!TransferPrivilegeHelper.hasPrivilege(TransferAppActivator.PRIVILEGE_LIST_PENDING)
				&& !TransferPrivilegeHelper.hasPrivilege(TransferAppActivator.PRIVILEGE_LIST_TRANSFERS)) {
			writePrivilegeDenied(response, data, TransferAppActivator.PRIVILEGE_LIST_TRANSFERS);
			return;
		}
		if (clientRegistryRegistrationService == null) {
			data.put("status", "error");
			data.put("message", "Client Registry registration is not available on this server.");
			writeJson(response, data);
			return;
		}
		try {
			String normalizedUpid = StringUtils.trimToNull(upid);
			if (normalizedUpid == null) {
				data.put("status", "error");
				data.put("message", "UPID is required");
				writeJson(response, data);
				return;
			}
			Map<String, Object> patientDetails = clientRegistryRegistrationService
					.findRegistrationFieldsByUpid(normalizedUpid);
			if (patientDetails == null) {
				data.put("status", "error");
				data.put("message", "No patient was found in the HIE client registry for UPID " + normalizedUpid);
				writeJson(response, data);
				return;
			}
			data.put("status", "success");
			data.put("upid", normalizedUpid);
			data.put("upidIdentifierTypeUuid",
					nullToEmpty(clientRegistryRegistrationService.getUpidIdentifierTypeUuid()));
			data.put("patientDetails", patientDetails);
		}
		catch (Exception e) {
			putError(data, e, TransferAppActivator.PRIVILEGE_LIST_TRANSFERS,
					"Unable to load HIE patient registration preview");
		}
		writeJson(response, data);
	}

	@RequestMapping(value = "/module/transferapp/transfer/registerPatientFromHie.form", method = RequestMethod.POST)
	public void registerPatientFromHie(HttpServletResponse response,
			@RequestParam("upid") String upid) throws Exception {

		Map<String, Object> data = new HashMap<String, Object>();
		if (!TransferPrivilegeHelper.hasPrivilege(TransferAppActivator.PRIVILEGE_LIST_PENDING)
				&& !TransferPrivilegeHelper.hasPrivilege(TransferAppActivator.PRIVILEGE_LIST_TRANSFERS)) {
			writePrivilegeDenied(response, data, TransferAppActivator.PRIVILEGE_LIST_TRANSFERS);
			return;
		}
		if (clientRegistryRegistrationService == null) {
			data.put("status", "error");
			data.put("message", "Client Registry registration is not available on this server.");
			writeJson(response, data);
			return;
		}
		try {
			String normalizedUpid = StringUtils.trimToNull(upid);
			if (normalizedUpid == null) {
				data.put("status", "error");
				data.put("message", "UPID is required");
				writeJson(response, data);
				return;
			}
			org.openmrs.Location location = null;
			try {
				location = Context.getUserContext().getLocation();
			}
			catch (Exception ignored) {
				location = null;
			}
			if (location == null) {
				location = Context.getLocationService().getDefaultLocation();
			}
			org.openmrs.module.transferapp.api.HiePatientRegistrationResult result =
					clientRegistryRegistrationService.registerPatientByUpid(normalizedUpid, location);
			org.openmrs.Patient patient = result.getPatient();
			org.openmrs.PatientIdentifier preferredIdentifier = patient.getPatientIdentifier();
			String localIdentifier = preferredIdentifier != null ? preferredIdentifier.getIdentifier() : "";
			String patientUuid = patient.getUuid();
			String patientPagePath = "/coreapps/clinicianfacing/patient.page?patientId="
					+ java.net.URLEncoder.encode(patientUuid, "UTF-8");
			String redirectPath = "/registrationapp/registrationSummary.page?patientId="
					+ java.net.URLEncoder.encode(patientUuid, "UTF-8")
					+ "&appId=rwandaemr.registerPatient"
					+ "&returnUrl=" + java.net.URLEncoder.encode(patientPagePath, "UTF-8");

			data.put("status", "success");
			data.put("created", Boolean.valueOf(result.isCreated()));
			data.put("patientUuid", patientUuid);
			data.put("localIdentifier", localIdentifier);
			data.put("upid", normalizedUpid);
			data.put("redirectUrl", redirectPath);
			data.put("message", result.isCreated()
					? "Patient registered successfully"
					: "Patient already exists locally");
		}
		catch (Exception e) {
			putError(data, e, TransferAppActivator.PRIVILEGE_LIST_TRANSFERS,
					"Unable to register patient from HIE");
		}
		writeJson(response, data);
	}

	@RequestMapping(value = "/module/transferapp/transfer/submit.form", method = RequestMethod.POST)
	public void submitTransferToHie(HttpServletResponse response,
			@RequestParam("uuid") String uuid) throws Exception {

		Map<String, Object> data = new HashMap<String, Object>();

		if (!TransferPrivilegeHelper.hasPrivilege(TransferAppActivator.PRIVILEGE_CREATE_TRANSFER)) {
			writePrivilegeDenied(response, data, TransferAppActivator.PRIVILEGE_CREATE_TRANSFER);
			return;
		}

		if (!transferAdminService.isOutboundFacilityNameConfigured()) {
			data.put("status", "error");
			data.put("message", Context.getMessageSourceService().getMessage(
					"transferapp.patient.transfers.outboundFacilityRequired"));
			writeJson(response, data);
			return;
		}

		try {
			Transfer transfer = getTransferHieSubmissionService().submitTransferToHie(uuid);
			data.put("status", "success");
			data.put("uuid", transfer.getUuid());
			data.put("hieSent", transfer.isSentToHie());
			data.put("hieSentAt", formatDateTime(transfer.getHieSentAt()));
		}
		catch (Exception e) {
			putError(data, e, TransferAppActivator.PRIVILEGE_CREATE_TRANSFER, "Unable to submit transfer to HIE");
		}

		writeJson(response, data);
	}

	@RequestMapping(value = "/module/transferapp/transfer/save.form", method = RequestMethod.POST)
	public void saveReferralTransfer(HttpServletResponse response,
			@RequestParam("patientId") Integer patientId,
			@RequestParam(value = "transferUuid", required = false) String transferUuid,
			@RequestParam(value = "decisionToTransferAt", required = false) String decisionToTransferAt,
			@RequestParam(value = "callingTime", required = false) String callingTime,
			@RequestParam(value = "receivingFacilityCode", required = false) String receivingFacilityCode,
			@RequestParam(value = "receivingFacilityId", required = false) Integer receivingFacilityId,
			@RequestParam(value = "receivingService", required = false) String receivingService,
			@RequestParam(value = "staffContactedName", required = false) String staffContactedName,
			@RequestParam(value = "staffContactedPhone", required = false) String staffContactedPhone,
			@RequestParam(value = "transferType", required = false) String transferType,
			@RequestParam(value = "ambulanceCalledTime", required = false) String ambulanceCalledTime,
			@RequestParam(value = "departureFromReferringTime", required = false) String departureFromReferringTime,
			@RequestParam(value = "transportationType", required = false) String transportationType,
			@RequestParam(value = "transportationOtherSpec", required = false) String transportationOtherSpec,
			@RequestParam(value = "ambulanceProviderFosaId", required = false) String ambulanceProviderFosaId,
			@RequestParam(value = "ambulanceProviderName", required = false) String ambulanceProviderName,
			@RequestParam(value = "reasonForTransfer", required = false) String reasonForTransfer,
			@RequestParam(value = "clinicalPresentation", required = false) String clinicalPresentation,
			@RequestParam(value = "disabilityType", required = false) String disabilityType,
			@RequestParam(value = "laboratory", required = false) String laboratory,
			@RequestParam(value = "proceduresTreatments", required = false) String proceduresTreatments,
			@RequestParam(value = "otherNotes", required = false) String otherNotes,
			@RequestParam(value = "diagnosis", required = false) String diagnosis,
			@RequestParam(value = "caregiverName", required = false) String caregiverName,
			@RequestParam(value = "caregiverTelephone", required = false) String caregiverTelephone,
			@RequestParam(value = "providerQualification", required = false) String providerQualification,
			@RequestParam(value = "signedDate", required = false) String signedDate,
			@RequestParam(value = "signedTime", required = false) String signedTime) throws Exception {

		Map<String, Object> data = new HashMap<String, Object>();

		if (!TransferPrivilegeHelper.hasPrivilege(TransferAppActivator.PRIVILEGE_CREATE_TRANSFER)) {
			writePrivilegeDenied(response, data, TransferAppActivator.PRIVILEGE_CREATE_TRANSFER);
			return;
		}

		if (!transferAdminService.isOutboundFacilityNameConfigured()) {
			data.put("status", "error");
			data.put("message", Context.getMessageSourceService().getMessage(
					"transferapp.patient.transfers.outboundFacilityRequired"));
			writeJson(response, data);
			return;
		}

		try {
			TransferFormExtras formExtras = buildFormExtras(clinicalPresentation, disabilityType, laboratory,
					proceduresTreatments, otherNotes, diagnosis, caregiverName, caregiverTelephone,
					providerQualification, signedDate, signedTime);
			Transfer transfer = transferService.saveReferralTransfer(
					patientId,
					transferUuid,
					decisionToTransferAt,
					callingTime,
					receivingFacilityCode,
					receivingFacilityId,
					receivingService,
					staffContactedName,
					staffContactedPhone,
					transferType,
					ambulanceCalledTime,
					departureFromReferringTime,
					transportationType,
					transportationOtherSpec,
					ambulanceProviderFosaId,
					ambulanceProviderName,
					reasonForTransfer,
					formExtras);

			data.put("status", "success");
			data.put("transferId", transfer.getTransferId());
			data.put("uuid", transfer.getUuid());
			data.put("hieSent", transfer.isSentToHie());
			data.put("updated", StringUtils.isNotBlank(transferUuid));
		}
		catch (Exception e) {
			putError(data, e, TransferAppActivator.PRIVILEGE_CREATE_TRANSFER, "Unable to save transfer");
		}

		writeJson(response, data);
	}

	@RequestMapping(value = "/module/transferapp/transfer/preview.form", method = RequestMethod.GET)
	public void previewTransfer(HttpServletResponse response,
			@RequestParam("uuid") String uuid) throws Exception {

		Map<String, Object> data = new HashMap<String, Object>();

		if (!TransferPrivilegeHelper.hasPrivilege(TransferAppActivator.PRIVILEGE_LIST_TRANSFERS)) {
			writePrivilegeDenied(response, data, TransferAppActivator.PRIVILEGE_LIST_TRANSFERS);
			return;
		}

		try {
			Transfer transfer = transferService.getTransferByUuid(uuid);
			if (transfer == null) {
				data.put("status", "error");
				data.put("message", "Transfer not found");
			} else {
				data.put("status", "success");
				data.put("transfer", toPreviewMap(transfer));
			}
		}
		catch (Exception e) {
			putError(data, e, TransferAppActivator.PRIVILEGE_LIST_TRANSFERS, "Unable to load transfer");
		}

		writeJson(response, data);
	}

	@RequestMapping(value = "/module/transferapp/transfer/ambulanceProviderFacilities.form", method = RequestMethod.GET)
	public void listAmbulanceProviderFacilities(HttpServletResponse response) throws Exception {
		Map<String, Object> data = new HashMap<String, Object>();
		if (!TransferPrivilegeHelper.hasPrivilege(TransferAppActivator.PRIVILEGE_CREATE_TRANSFER)
				&& !TransferPrivilegeHelper.hasPrivilege(TransferAppActivator.PRIVILEGE_LIST_TRANSFERS)) {
			writePrivilegeDenied(response, data, TransferAppActivator.PRIVILEGE_CREATE_TRANSFER);
			return;
		}
		try {
			List<RegistryFacility> facilities = Context.getService(
					org.openmrs.module.transferapp.api.TransferFacilityRegistryService.class)
					.listAmbulanceProviderFacilitiesFromHie();
			List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
			if (facilities != null) {
				for (RegistryFacility facility : facilities) {
					if (facility == null) {
						continue;
					}
					Map<String, Object> row = new HashMap<String, Object>();
					row.put("code", nullToEmpty(facility.getCode()));
					row.put("name", nullToEmpty(facility.getName()));
					row.put("category", nullToEmpty(facility.getCategory()));
					rows.add(row);
				}
			}
			String currentFosaId = StringUtils.trimToEmpty(Context.getAdministrationService().getGlobalProperty(
					TransferAppConstants.GP_SENDING_FOSA_ID, TransferAppConstants.DEFAULT_SENDING_FOSA_ID));
			data.put("status", "success");
			data.put("facilities", rows);
			data.put("currentFosaId", currentFosaId);
		}
		catch (Exception e) {
			putError(data, e, TransferAppActivator.PRIVILEGE_CREATE_TRANSFER,
					"Unable to load ambulance provider facilities");
			data.put("facilities", new ArrayList<Map<String, Object>>());
		}
		writeJson(response, data);
	}

	@RequestMapping(value = "/module/transferapp/transfer/ambulanceVoucherPreview.form", method = RequestMethod.GET)
	public void previewAmbulanceVoucher(HttpServletResponse response,
			@RequestParam("uuid") String uuid) throws Exception {

		Map<String, Object> data = new HashMap<String, Object>();

		if (!TransferPrivilegeHelper.hasPrivilege(TransferAppActivator.PRIVILEGE_LIST_TRANSFERS)) {
			writePrivilegeDenied(response, data, TransferAppActivator.PRIVILEGE_LIST_TRANSFERS);
			return;
		}

		try {
			AmbulanceVoucherPreview preview = transferAmbulanceVoucherService.getVoucherPreview(uuid);
			data.put("status", "success");
			data.put("voucher", toAmbulanceVoucherMap(preview));
		}
		catch (Exception e) {
			putError(data, e, TransferAppActivator.PRIVILEGE_LIST_TRANSFERS, "Unable to load ambulance voucher");
		}

		writeJson(response, data);
	}

	@RequestMapping(value = "/module/transferapp/transfer/createAmbulanceVoucherFromHie.form", method = RequestMethod.POST)
	public void createAmbulanceVoucherFromHie(HttpServletResponse response,
			@RequestParam("patientId") Integer patientId,
			@RequestParam("hieTransferId") String hieTransferId,
			@RequestParam("kilometers") Integer kilometers,
			@RequestParam(value = "district", required = false) String district) throws Exception {

		Map<String, Object> data = new HashMap<String, Object>();

		if (!TransferPrivilegeHelper.hasPrivilege(TransferAppActivator.PRIVILEGE_CREATE_TRANSFER)
				&& !TransferPrivilegeHelper.hasPrivilege(TransferAppActivator.PRIVILEGE_LIST_TRANSFERS)) {
			writePrivilegeDenied(response, data, TransferAppActivator.PRIVILEGE_CREATE_TRANSFER);
			return;
		}

		if (kilometers == null || kilometers.intValue() <= 0) {
			data.put("status", "error");
			data.put("message", "Distance in kilometers must be greater than zero");
			writeJson(response, data);
			return;
		}
		if (StringUtils.isBlank(district)) {
			data.put("status", "error");
			data.put("message", "Covered district is required");
			writeJson(response, data);
			return;
		}

		try {
			Transfer transfer = transferAmbulanceVoucherService.createAmbulanceVoucherFromHie(
					patientId, hieTransferId, kilometers.intValue(), district);
			data.put("status", "success");
			data.put("uuid", transfer.getUuid());
			data.put("transferId", transfer.getTransferId());
			data.put("hieTransferId", transfer.getHieTransferId());
			data.put("ambulanceConsommationId", transfer.getAmbulanceConsommationId());
			data.put("message", "Ambulance voucher created successfully");
		}
		catch (Exception e) {
			putError(data, e, TransferAppActivator.PRIVILEGE_CREATE_TRANSFER,
					"Unable to create ambulance voucher from HIE transfer");
		}

		writeJson(response, data);
	}

	@RequestMapping(value = "/module/transferapp/transfer/hieTransfersByIdentifier.form", method = RequestMethod.GET)
	public void searchHieTransfersByIdentifier(HttpServletResponse response,
			@RequestParam("identifier") String identifier) throws Exception {

		Map<String, Object> data = new HashMap<String, Object>();

		if (!TransferPrivilegeHelper.hasPrivilege(TransferAppActivator.PRIVILEGE_LIST_TRANSFERS)) {
			writePrivilegeDenied(response, data, TransferAppActivator.PRIVILEGE_LIST_TRANSFERS);
			return;
		}

		try {
			String normalized = PatientTransferIdentifierDetector.normalize(identifier);
			PatientTransferIdentifierDetector.Kind kind = PatientTransferIdentifierDetector.detect(normalized);
			if (kind == PatientTransferIdentifierDetector.Kind.UNKNOWN || normalized == null) {
				data.put("status", "error");
				data.put("message",
						"Enter a 16-digit National ID or a UPID in 6-4-4 format (e.g. 260204-0023-8464).");
				data.put("transfers", new ArrayList<Map<String, Object>>());
				writeJson(response, data);
				return;
			}

			String upid;
			String identifierKind;
			if (kind == PatientTransferIdentifierDetector.Kind.UPID) {
				upid = normalized;
				identifierKind = "UPID";
			} else {
				identifierKind = "NID";
				if (clientRegistryRegistrationService == null) {
					data.put("status", "error");
					data.put("message", "Client Registry lookup is not available on this server.");
					data.put("transfers", new ArrayList<Map<String, Object>>());
					writeJson(response, data);
					return;
				}
				upid = clientRegistryRegistrationService.findUpidByNationalId(normalized);
				if (StringUtils.isBlank(upid)) {
					data.put("status", "error");
					data.put("message", "No patient found in Client Registry for National ID " + normalized + ".");
					data.put("transfers", new ArrayList<Map<String, Object>>());
					data.put("identifierKind", identifierKind);
					writeJson(response, data);
					return;
				}
			}

			Map<String, Object> searchResult = getTransferHieSearchService().searchTransfers(upid, null, false);
			@SuppressWarnings("unchecked")
			List<Map<String, Object>> rawTransfers = searchResult != null
					&& searchResult.get("data") instanceof List
					? (List<Map<String, Object>>) searchResult.get("data")
					: new ArrayList<Map<String, Object>>();

			if (searchResult != null && "error".equals(searchResult.get("status"))) {
				data.put("status", "error");
				data.put("message", searchResult.get("message") != null
						? String.valueOf(searchResult.get("message"))
						: "Unable to search transfers from HIE");
				data.put("transfers", new ArrayList<Map<String, Object>>());
				data.put("upid", upid);
				data.put("identifierKind", identifierKind);
				writeJson(response, data);
				return;
			}

			List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
			String currentFosaId = StringUtils.trimToEmpty(Context.getAdministrationService().getGlobalProperty(
					TransferAppConstants.GP_SENDING_FOSA_ID, TransferAppConstants.DEFAULT_SENDING_FOSA_ID));
			boolean productionMode = TransferAppMode.isProduction();
			boolean ambulanceProviderForAny = false;
			String currentFacilityName = "";
			if (transferAdminService != null) {
				currentFacilityName = StringUtils.trimToEmpty(transferAdminService.resolveOutboundFacilityName());
				if (StringUtils.isBlank(currentFacilityName)) {
					currentFacilityName = StringUtils.trimToEmpty(
							transferAdminService.resolveCurrentSendingFacilityName());
				}
			}

			org.openmrs.Patient localPatient = null;
			try {
				localPatient = PendingTransferPatientStatusResolver.findLocalPatientByUpid(
						Context.getPatientService(), upid);
			}
			catch (Exception ignored) {
				localPatient = null;
			}
			boolean existingPatient = localPatient != null;
			Integer localPatientId = localPatient != null ? localPatient.getPatientId() : null;
			String existingPatientReference = PendingTransferPatientStatusResolver.patientReference(localPatient);
			String insuranceCardNumber = null;
			if (existingPatient && localPatient != null) {
				try {
					org.openmrs.module.transferapp.api.PatientInsuranceService insuranceService =
							Context.getService(org.openmrs.module.transferapp.api.PatientInsuranceService.class);
					if (insuranceService != null) {
						insuranceCardNumber = insuranceService.resolveInsuranceCardNumber(localPatient);
					}
				}
				catch (Exception ignored) {
					insuranceCardNumber = null;
				}
			}
			boolean hasInsuranceOnRegistration = StringUtils.isNotBlank(insuranceCardNumber);
			boolean canRegisterPatient = !existingPatient && StringUtils.isNotBlank(upid);

			for (Map<String, Object> transfer : rawTransfers) {
				if (transfer == null) {
					continue;
				}
				Map<String, Object> row = toHieTransferSummaryRow(transfer);
				boolean providerMatch = isCurrentFacilityAmbulanceProvider(
						row.get("ambulanceProviderFosaId"),
						row.get("ambulanceProviderName"),
						currentFosaId,
						currentFacilityName);
				row.put("ambulanceProviderMatchesCurrent", providerMatch);
				if (providerMatch) {
					ambulanceProviderForAny = true;
				}

				String hieTransferId = firstNonBlank(row.get("uuid"), transfer.get("id"), transfer.get("uuid"));
				row.put("hieTransferId", hieTransferId);
				boolean canCreateVoucher = existingPatient && hasInsuranceOnRegistration
						&& StringUtils.isNotBlank(hieTransferId);
				// Production: only the configured outbound facility may create vouchers for
				// transfers that name it as ambulance provider. Non-production allows any.
				if (productionMode && !providerMatch) {
					canCreateVoucher = false;
				}
				boolean hasAmbulanceVoucher = false;
				String localTransferUuid = "";
				if (localPatientId != null && StringUtils.isNotBlank(hieTransferId)
						&& transferAmbulanceVoucherService != null) {
					Map<String, Object> link = transferAmbulanceVoucherService.resolveLocalVoucherLink(
							localPatientId, hieTransferId);
					localTransferUuid = link.get("localTransferUuid") != null
							? String.valueOf(link.get("localTransferUuid")) : "";
					hasAmbulanceVoucher = Boolean.TRUE.equals(link.get("hasAmbulanceVoucher"))
							|| "true".equalsIgnoreCase(String.valueOf(link.get("hasAmbulanceVoucher")));
				}
				if (hasAmbulanceVoucher) {
					canCreateVoucher = false;
				}
				row.put("localTransferUuid", localTransferUuid);
				row.put("hasAmbulanceVoucher", hasAmbulanceVoucher);
				row.put("canCreateAmbulanceVoucher", canCreateVoucher);
				rows.add(row);
			}

			data.put("status", "success");
			data.put("upid", upid);
			data.put("identifierKind", identifierKind);
			data.put("existingPatient", existingPatient);
			data.put("patientId", localPatientId);
			data.put("existingPatientReference", existingPatientReference);
			data.put("hasInsuranceOnRegistration", hasInsuranceOnRegistration);
			data.put("insuranceCardNumber", StringUtils.defaultString(insuranceCardNumber));
			data.put("currentFacilityName", currentFacilityName);
			data.put("currentFosaId", currentFosaId);
			data.put("productionMode", productionMode);
			data.put("ambulanceProviderForCurrentFacility", ambulanceProviderForAny);
			data.put("canRegisterPatient", canRegisterPatient);
			data.put("transfers", rows);
		}
		catch (Exception e) {
			putError(data, e, TransferAppActivator.PRIVILEGE_LIST_TRANSFERS, "Unable to search HIE transfers");
			if (!data.containsKey("transfers")) {
				data.put("transfers", new ArrayList<Map<String, Object>>());
			}
		}

		writeJson(response, data);
	}

	@RequestMapping(value = "/module/transferapp/transfer/verifyQr.form", method = RequestMethod.GET)
	public void verifyQr(HttpServletResponse response,
			@RequestParam(value = "uuid", required = false) String uuid,
			@RequestParam(value = "transferId", required = false) String transferId) throws Exception {
		if (!TransferPrivilegeHelper.hasPrivilege(TransferAppActivator.PRIVILEGE_LIST_TRANSFERS)) {
			response.sendError(HttpServletResponse.SC_FORBIDDEN,
					TransferPrivilegeHelper.requiredPrivilegeMessage(TransferAppActivator.PRIVILEGE_LIST_TRANSFERS));
			return;
		}

		try {
			TransferVerificationUrlService verificationUrlService = getTransferVerificationUrlService();
			String verifyUrl = null;

			if (StringUtils.isNotBlank(uuid)) {
				Transfer transfer = transferService.getTransferByUuid(uuid.trim());
				if (transfer != null && verificationUrlService.shouldShowVerificationQr(transfer)) {
					verifyUrl = verificationUrlService.buildRemoteVerifyUrl(transfer);
				}
			}

			if (StringUtils.isBlank(verifyUrl) && StringUtils.isNotBlank(transferId)) {
				verifyUrl = verificationUrlService.buildRemoteVerifyUrlForTransferId(transferId.trim());
			}

			if (StringUtils.isBlank(verifyUrl)) {
				response.sendError(HttpServletResponse.SC_NOT_FOUND);
				return;
			}

			byte[] png = getTransferQrCodeService().generatePng(verifyUrl);
			response.setContentType("image/png");
			response.setHeader("Cache-Control", "no-store");
			response.getOutputStream().write(png);
		}
		catch (Exception e) {
			if (TransferPrivilegeHelper.isPrivilegeException(e)) {
				response.sendError(HttpServletResponse.SC_FORBIDDEN,
						TransferPrivilegeHelper.resolveUserFacingMessage(
								e,
								TransferAppActivator.PRIVILEGE_LIST_TRANSFERS,
								TransferPrivilegeHelper.requiredPrivilegeMessage(TransferAppActivator.PRIVILEGE_LIST_TRANSFERS)));
				return;
			}
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
					e.getMessage() != null ? e.getMessage() : "Unable to generate QR code");
		}
	}

	private Map<String, Object> toAmbulanceVoucherMap(AmbulanceVoucherPreview preview) {
		Map<String, Object> map = new HashMap<String, Object>();
		if (preview == null) {
			return map;
		}
		map.put("transferUuid", nullToEmpty(preview.getTransferUuid()));
		map.put("organizationName", nullToEmpty(preview.getOrganizationName()));
		map.put("fax", nullToEmpty(preview.getFax()));
		map.put("title", nullToEmpty(preview.getTitle()));
		map.put("province", nullToEmpty(preview.getProvince()));
		map.put("district", nullToEmpty(preview.getDistrict()));
		map.put("sectionHospital", nullToEmpty(preview.getSectionHospital()));
		map.put("date", nullToEmpty(preview.getDate()));
		map.put("departureTime", nullToEmpty(preview.getDepartureTime()));
		map.put("patientCount", preview.getPatientCount() != null ? preview.getPatientCount() : 1);
		map.put("voucherId", nullToEmpty(preview.getVoucherId()));
		map.put("destination", nullToEmpty(preview.getDestination()));
		map.put("distanceKm", preview.getDistanceKm());
		map.put("patientName", nullToEmpty(preview.getPatientName()));
		map.put("affiliationNumber", nullToEmpty(preview.getAffiliationNumber()));
		map.put("driverName", nullToEmpty(preview.getDriverName()));
		map.put("accompanyingNurse", nullToEmpty(preview.getAccompanyingNurse()));
		map.put("arrivalDate", nullToEmpty(preview.getArrivalDate()));
		map.put("arrivalTime", nullToEmpty(preview.getArrivalTime()));
		map.put("cbhiAgentName", nullToEmpty(preview.getCbhiAgentName()));
		map.put("receivingClinicianName", nullToEmpty(preview.getReceivingClinicianName()));
		map.put("amount", preview.getAmount() != null ? preview.getAmount().toPlainString() : "");
		map.put("consommationId", preview.getConsommationId());
		map.put("noteReferral", nullToEmpty(preview.getNoteReferral()));
		map.put("noteInvoice", nullToEmpty(preview.getNoteInvoice()));
		return map;
	}

	private Map<String, Object> toPreviewMap(Transfer transfer) {
		Map<String, Object> preview = new HashMap<String, Object>();
		preview.put("uuid", transfer.getUuid());

		preview.put("province", nullToEmpty(transfer.getReceivingProvince()));
		preview.put("district", nullToEmpty(transfer.getReceivingDistrict()));
		preview.put("hospitalName", nullToEmpty(transfer.getSendingFacility()));
		preview.put("referringFacilityName", nullToEmpty(transfer.getSendingFacility()));
		preview.put("referringUnit", nullToEmpty(transfer.getReferringUnit()));
		preview.put("receivingClinicianPhone", nullToEmpty(transfer.getStaffContactedPhone()));

		preview.put("clientName", nullToEmpty(transfer.getClientName()));
		preview.put("emrId", nullToEmpty(transfer.getEmrId()));
		preview.put("serialNumberEmr", nullToEmpty(transfer.getEmrId()));
		preview.put("clientTelephone", nullToEmpty(transfer.getClientTelephone()));
		preview.put("ageOrDob", nullToEmpty(transfer.getAgeOrDob()));
		preview.put("sex", nullToEmpty(transfer.getSex()));
		preview.put("identifierType", nullToEmpty(transfer.getIdentifierType()));
		preview.put("identifierValue", nullToEmpty(transfer.getIdentifierValue()));
		preview.put("caregiverName", nullToEmpty(transfer.getCaregiverName()));
		preview.put("caregiverTelephone", nullToEmpty(transfer.getCaregiverTelephone()));
		preview.put("clientDistrict", nullToEmpty(transfer.getClientDistrict()));
		preview.put("sector", nullToEmpty(transfer.getSector()));
		preview.put("cell", nullToEmpty(transfer.getCell()));
		preview.put("village", nullToEmpty(transfer.getVillage()));
		preview.put("sendingFacility", nullToEmpty(transfer.getSendingFacility()));

		preview.put("admissionAt", formatDateTime(transfer.getAdmissionAt()));
		preview.put("decisionToTransferAt", formatDateTime(transfer.getDecisionToTransferAt()));
		preview.put("transferDecisionDatetime", formatDateTime(transfer.getDecisionToTransferAt()));
		preview.put("callingTime", nullToEmpty(transfer.getCallingTime()));
		preview.put("receivingFacility", resolveFacilityLabel(transfer.getReceivingFacilityCode()));
		preview.put("receivingFacilityCode", nullToEmpty(transfer.getReceivingFacilityCode()));
		preview.put("receivingService", nullToEmpty(transfer.getReceivingService()));
		preview.put("staffContactedName", nullToEmpty(transfer.getStaffContactedName()));
		preview.put("staffContactedAtReceivingFacility", nullToEmpty(transfer.getStaffContactedName()));
		preview.put("staffContactedPhone", nullToEmpty(transfer.getStaffContactedPhone()));

		String transferType = nullToEmpty(transfer.getTransferType());
		preview.put("transferType", transferType);
		preview.put("isEmergency", "EMERGENCY".equals(transferType));
		preview.put("isNonEmergency", "NOT_EMERGENCY".equals(transferType));
		preview.put("isFollowUp", "FOLLOW_UP".equals(transferType));
		preview.put("ambulanceCalledTime", nullToEmpty(transfer.getAmbulanceCallTime()));
		preview.put("departureFromReferringTime", nullToEmpty(transfer.getDepartRefTime()));
		preview.put("reasonForTransfer", nullToEmpty(transfer.getReasonForTransfer()));

		preview.put("clinicalPresentation", nullToEmpty(transfer.getClinicalPresentation()));
		preview.put("disabilityType", nullToEmpty(transfer.getDisabilityType()));
		preview.put("vitalTemp", nullToEmpty(transfer.getVitalTemp()));
		preview.put("vitalSpo2", nullToEmpty(transfer.getVitalSpo2()));
		preview.put("vitalRr", nullToEmpty(transfer.getVitalRr()));
		preview.put("vitalPulse", nullToEmpty(transfer.getVitalPulse()));
		preview.put("vitalBp", nullToEmpty(transfer.getVitalBp()));
		preview.put("vitalWeight", nullToEmpty(transfer.getVitalWt()));
		preview.put("vitalHeight", nullToEmpty(transfer.getVitalHt()));
		preview.put("vitalMuac", nullToEmpty(transfer.getVitalMuac()));
		preview.put("laboratory", nullToEmpty(transfer.getLaboratory()));
		preview.put("othersNotes", nullToEmpty(transfer.getOtherNotes()));
		preview.put("diagnosis", nullToEmpty(transfer.getDiagnosis()));
		preview.put("proceduresAndTreatments", nullToEmpty(transfer.getProceduresTreatments()));

		String transportType = nullToEmpty(transfer.getTransportType());
		preview.put("transportationType", transportType);
		preview.put("isAmbulanceTransport", "AMBULANCE".equals(transportType));
		preview.put("transportationOtherSpec", nullToEmpty(transfer.getTransportOther()));
		preview.put("isNaTransport", "NA".equals(transportType));
		preview.put("ambulanceProviderFosaId", nullToEmpty(transfer.getAmbulanceProviderFosaId()));
		preview.put("ambulanceProviderName", nullToEmpty(transfer.getAmbulanceProviderName()));

		String healthInsuranceType = nullToEmpty(transfer.getHealthInsuranceType()).trim();
		String healthInsuranceOther = nullToEmpty(transfer.getHealthInsuranceOther()).trim();
		boolean isNoInsurance = isNoInsurancePreview(healthInsuranceType, healthInsuranceOther);
		boolean isOtherInsurance = "OTHER".equalsIgnoreCase(healthInsuranceType) && !isNoInsurance;
		preview.put("healthInsuranceType", isNoInsurance ? "NONE" : healthInsuranceType);
		preview.put("isCbhiInsurance", "CBHI".equalsIgnoreCase(healthInsuranceType) && !isNoInsurance);
		preview.put("isRssbInsurance", "RSSB".equalsIgnoreCase(healthInsuranceType) && !isNoInsurance);
		preview.put("isMmiInsurance", "MMI".equalsIgnoreCase(healthInsuranceType) && !isNoInsurance);
		preview.put("healthInsuranceOtherSpec", isOtherInsurance ? healthInsuranceOther : "");
		preview.put("isNoInsurance", isNoInsurance);

		preview.put("referringProviderName", formatReferringProviderNameForPreview(transfer));
		preview.put("referringProviderQualification", nullToEmpty(transfer.getProviderQualification()));
		preview.put("referringSignedDate", formatDateOnly(transfer.getSignedDate()));
		preview.put("referringSignedTime", nullToEmpty(transfer.getSignedTime()));
		preview.put("referringProviderPhone", nullToEmpty(transfer.getProviderPhone()));
		preview.put("signatureAndStamp", "");
		preview.put("dateCreated", formatDateTime(transfer.getDateCreated()));
		preview.put("hieSent", transfer.isSentToHie());
		preview.put("hieSentAt", formatDateTime(transfer.getHieSentAt()));
		preview.put("hieSendError", nullToEmpty(transfer.getHieSendError()));
		preview.put("receivedFromHie", transfer.isReceivedFromHie());
		preview.put("hieTransferId", nullToEmpty(transfer.getHieTransferId()));
		org.openmrs.module.transferapp.model.TransferFormKind formKind = transfer.getFormKind();
		preview.put("formKind", formKind.name());
		preview.put("formKindCode", formKind.getCode());
		preview.put("formKindDisplay", formKind.getDisplay());

		TransferVerificationUrlService verificationUrlService = getTransferVerificationUrlService();
		boolean showVerificationQr = verificationUrlService.shouldShowVerificationQr(transfer);
		preview.put("showVerificationQr", showVerificationQr);
		if (showVerificationQr) {
			String verificationTransferId = verificationUrlService.resolveVerificationTransferId(transfer);
			preview.put("verificationTransferId", verificationTransferId);
			preview.put("verifyRemoteUrl", verificationUrlService.buildRemoteVerifyUrl(transfer));
			preview.put("verifyQrUrl", verificationUrlService.buildVerifyQrFormUrl(verificationTransferId));
		}
		return preview;
	}

	private String formatReferringProviderNameForPreview(Transfer transfer) {
		String name = nullToEmpty(transfer.getReferringProviderName());
		try {
			TransferProfileService profileService = Context.getService(TransferProfileService.class);
			org.openmrs.User owner = transfer.getCreator() != null
					? transfer.getCreator()
					: Context.getAuthenticatedUser();
			if (profileService != null && owner != null) {
				TransferProfile profile = profileService.getProfileForUser(owner);
				if (profile != null) {
					return TransferProfile.formatCareProviderName(name, profile.getLicenseNumber());
				}
			}
		}
		catch (Exception ignored) {
			// Fall back to the stored name when profile lookup is unavailable.
		}
		return name;
	}

	private String nullToEmpty(String value) {
		return value != null ? value : "";
	}

	private static String firstNonBlank(Object... values) {
		if (values == null) {
			return "";
		}
		for (Object value : values) {
			if (value == null) {
				continue;
			}
			String text = String.valueOf(value).trim();
			if (text.length() > 0 && !"null".equalsIgnoreCase(text)) {
				return text;
			}
		}
		return "";
	}

	private Map<String, Object> toHieTransferSummaryRow(Map<String, Object> transfer) {
		Map<String, Object> row = new HashMap<String, Object>();
		row.put("date", firstNonBlank(
				transfer.get("date"),
				transfer.get("transferDecisionDatetime"),
				transfer.get("decisionToTransferAt"),
				transfer.get("admissionDatetime"),
				transfer.get("admissionAt"),
				transfer.get("periodStart")));
		row.put("fromFacility", firstNonBlank(
				transfer.get("origin"),
				transfer.get("referringFacilityName"),
				transfer.get("hospitalName"),
				transfer.get("sendingFacility")));
		row.put("fromService", firstNonBlank(
				transfer.get("referringUnit"),
				transfer.get("admitSource")));
		row.put("toFacility", firstNonBlank(
				transfer.get("destinationDisplay"),
				transfer.get("destination"),
				transfer.get("receivingFacility")));
		row.put("toService", firstNonBlank(
				transfer.get("receivingService"),
				transfer.get("receivingDepartment")));
		row.put("clinician", firstNonBlank(
				transfer.get("staffContactedAtReceivingFacility"),
				transfer.get("staffContactedName"),
				transfer.get("receivingClinician"),
				transfer.get("referringProviderName")));
		row.put("ambulanceProviderFosaId", firstNonBlank(transfer.get("ambulanceProviderFosaId")));
		row.put("ambulanceProviderName", firstNonBlank(
				transfer.get("ambulanceProviderName"),
				transfer.get("ambulanceProviderFosaId")));
		row.put("district", firstNonBlank(
				transfer.get("district"),
				transfer.get("receivingDistrict"),
				transfer.get("patientDistrict")));
		row.put("uuid", firstNonBlank(transfer.get("uuid"), transfer.get("id")));
		return row;
	}

	private static boolean isCurrentFacilityAmbulanceProvider(Object providerFosaId, Object providerName,
			String currentFosaId, String outboundFacilityName) {
		String providerFosa = providerFosaId != null ? StringUtils.trimToNull(String.valueOf(providerFosaId)) : null;
		String currentFosa = StringUtils.trimToNull(currentFosaId);
		if (providerFosa != null && currentFosa != null && currentFosa.equalsIgnoreCase(providerFosa)) {
			return true;
		}
		String providerLabel = providerName != null ? StringUtils.trimToNull(String.valueOf(providerName)) : null;
		String outbound = StringUtils.trimToNull(outboundFacilityName);
		return providerLabel != null && outbound != null && outbound.equalsIgnoreCase(providerLabel);
	}

	/**
	 * Treats stored OTHER + otherSpec "NONE" (and similar labels) as no insurance so the
	 * preview checks None instead of filling Other (Specify).
	 */
	private static boolean isNoInsurancePreview(String healthInsuranceType, String healthInsuranceOther) {
		String type = StringUtils.trimToEmpty(healthInsuranceType);
		String other = StringUtils.trimToEmpty(healthInsuranceOther);
		if (TransferAppConstants.HEALTH_INSURANCE_NONE.equals(
				PatientInsuranceServiceImpl.matchCategoryByDisplayName(type))) {
			return true;
		}
		if (TransferAppConstants.HEALTH_INSURANCE_NONE.equalsIgnoreCase(type)) {
			return true;
		}
		if (TransferAppConstants.HEALTH_INSURANCE_OTHER.equalsIgnoreCase(type)
				|| StringUtils.isBlank(type)) {
			return TransferAppConstants.HEALTH_INSURANCE_NONE.equals(
					PatientInsuranceServiceImpl.matchCategoryByDisplayName(other));
		}
		return false;
	}

	private String resolveFacilityLabel(String facilityCode) {
		if (StringUtils.isBlank(facilityCode)) {
			return "";
		}
		if (transferAdminService != null) {
			Integer sendingLocationId = transferAdminService.resolveCurrentSendingLocationId();
			String label = transferAdminService.resolveReceivingFacilityName(sendingLocationId, facilityCode);
			if (!facilityCode.equals(label)) {
				return label;
			}
		}
		switch (facilityCode) {
			case "KUTH":
				return "Kigali University Teaching Hospital";
			case "RUHENGERI":
				return "Ruhengeri District Hospital";
			case "BUTARO":
				return "Butaro District Hospital";
			case "KFH":
				return "King Faisal Hospital";
			default:
				return facilityCode;
		}
	}

	private String formatDateTime(Date date) {
		if (date == null) {
			return "";
		}
		return new SimpleDateFormat(DATETIME_PATTERN).format(date);
	}

	private String formatDateOnly(Date date) {
		if (date == null) {
			return "";
		}
		return new SimpleDateFormat("dd.MMM.yyyy").format(date);
	}

	private TransferFormExtras buildFormExtras(String clinicalPresentation, String disabilityType, String laboratory,
			String proceduresTreatments, String otherNotes, String diagnosis, String caregiverName,
			String caregiverTelephone, String providerQualification, String signedDate, String signedTime) {
		if (StringUtils.isBlank(clinicalPresentation) && StringUtils.isBlank(disabilityType)
				&& StringUtils.isBlank(laboratory) && StringUtils.isBlank(proceduresTreatments)
				&& StringUtils.isBlank(otherNotes) && StringUtils.isBlank(diagnosis)
				&& StringUtils.isBlank(caregiverName) && StringUtils.isBlank(caregiverTelephone)
				&& StringUtils.isBlank(providerQualification)
				&& StringUtils.isBlank(signedDate) && StringUtils.isBlank(signedTime)) {
			return null;
		}
		TransferFormExtras extras = new TransferFormExtras();
		extras.setClinicalPresentation(clinicalPresentation);
		extras.setDisabilityType(disabilityType);
		extras.setLaboratory(laboratory);
		extras.setProceduresTreatments(proceduresTreatments);
		extras.setOtherNotes(otherNotes);
		extras.setDiagnosis(diagnosis);
		extras.setCaregiverName(caregiverName);
		extras.setCaregiverTelephone(caregiverTelephone);
		extras.setProviderQualification(providerQualification);
		extras.setSignedDate(signedDate);
		extras.setSignedTime(signedTime);
		return extras;
	}

	private String formatTimeOnly(Date date) {
		if (date == null) {
			return "";
		}
		return new SimpleDateFormat("HH:mm:ss").format(date);
	}

	private void writePrivilegeDenied(HttpServletResponse response, Map<String, Object> data, String privilege)
			throws Exception {
		data.put("status", "error");
		data.put("message", TransferPrivilegeHelper.requiredPrivilegeMessage(privilege));
		data.put("requiredPrivilege", privilege);
		response.setStatus(HttpServletResponse.SC_FORBIDDEN);
		writeJson(response, data);
	}

	private void putError(Map<String, Object> data, Exception exception, String requiredPrivilege, String fallback) {
		data.put("status", "error");
		data.put("message", TransferPrivilegeHelper.resolveUserFacingMessage(exception, requiredPrivilege, fallback));
		if (TransferPrivilegeHelper.isPrivilegeException(exception)) {
			data.put("requiredPrivilege", requiredPrivilege);
		}
	}

	private void writeJson(HttpServletResponse response, Map<String, Object> data) throws Exception {
		response.setContentType("application/json");
		new ObjectMapper().writeValue(response.getOutputStream(), data);
	}

}
