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
import org.openmrs.module.transferapp.TransferPrivilegeHelper;
import org.openmrs.module.transferapp.api.MaternityTransferService;
import org.openmrs.module.transferapp.api.NeonatalTransferService;
import org.openmrs.module.transferapp.api.TransferAdminService;
import org.openmrs.module.transferapp.api.TransferAmbulanceVoucherService;
import org.openmrs.module.transferapp.api.MaternityTransferHieSubmissionService;
import org.openmrs.module.transferapp.api.NeonatalTransferHieSubmissionService;
import org.openmrs.module.transferapp.api.TransferHieSubmissionService;
import org.openmrs.module.transferapp.api.TransferProfileService;
import org.openmrs.module.transferapp.api.TransferQrCodeService;
import org.openmrs.module.transferapp.api.TransferService;
import org.openmrs.module.transferapp.api.TransferVerificationUrlService;
import org.openmrs.module.transferapp.api.impl.PatientInsuranceServiceImpl;
import org.openmrs.module.transferapp.model.AmbulanceVoucherPreview;
import org.openmrs.module.transferapp.model.MaternityTransfer;
import org.openmrs.module.transferapp.model.MaternityTransferFormData;
import org.openmrs.module.transferapp.model.MaternityTransferTreatment;
import org.openmrs.module.transferapp.model.MaternityTransferTreatmentRow;
import org.openmrs.module.transferapp.model.NeonatalTransfer;
import org.openmrs.module.transferapp.model.NeonatalTransferFormData;
import org.openmrs.module.transferapp.model.Transfer;
import org.openmrs.module.transferapp.model.TransferFormExtras;
import org.openmrs.module.transferapp.model.TransferProfile;
import org.springframework.beans.factory.annotation.Autowired;
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

	private static final String FORM_TYPE_EXTERNAL = "External";

	private static final String FORM_TYPE_MATERNITY = "Maternity";

	private static final String FORM_TYPE_NEONATAL = "Neonatal";

	@Autowired
	private TransferService transferService;

	@Autowired
	private MaternityTransferService maternityTransferService;

	@Autowired
	private NeonatalTransferService neonatalTransferService;

	@Autowired
	private TransferAdminService transferAdminService;

	@Autowired
	private TransferAmbulanceVoucherService transferAmbulanceVoucherService;

	private TransferHieSubmissionService getTransferHieSubmissionService() {
		return Context.getService(TransferHieSubmissionService.class);
	}

	private NeonatalTransferHieSubmissionService getNeonatalTransferHieSubmissionService() {
		return Context.getService(NeonatalTransferHieSubmissionService.class);
	}

	private MaternityTransferHieSubmissionService getMaternityTransferHieSubmissionService() {
		return Context.getService(MaternityTransferHieSubmissionService.class);
	}

	private TransferVerificationUrlService getTransferVerificationUrlService() {
		return Context.getService(TransferVerificationUrlService.class);
	}

	private TransferQrCodeService getTransferQrCodeService() {
		return Context.getService(TransferQrCodeService.class);
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

	@RequestMapping(value = "/module/transferapp/transfer/submitNeonatal.form", method = RequestMethod.POST)
	public void submitNeonatalTransferToHie(HttpServletResponse response,
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
			NeonatalTransfer transfer = getNeonatalTransferHieSubmissionService().submitNeonatalTransferToHie(uuid);
			data.put("status", "success");
			data.put("uuid", transfer.getUuid());
			data.put("hieSent", transfer.isSentToHie());
			data.put("hieSentAt", formatDateTime(transfer.getHieSentAt()));
		}
		catch (Exception e) {
			putError(data, e, TransferAppActivator.PRIVILEGE_CREATE_TRANSFER, "Unable to submit neonatal transfer to HIE");
		}

		writeJson(response, data);
	}

	@RequestMapping(value = "/module/transferapp/transfer/submitMaternity.form", method = RequestMethod.POST)
	public void submitMaternityTransferToHie(HttpServletResponse response,
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
			MaternityTransfer transfer = getMaternityTransferHieSubmissionService().submitMaternityTransferToHie(uuid);
			data.put("status", "success");
			data.put("uuid", transfer.getUuid());
			data.put("hieSent", transfer.isSentToHie());
			data.put("hieSentAt", formatDateTime(transfer.getHieSentAt()));
		}
		catch (Exception e) {
			putError(data, e, TransferAppActivator.PRIVILEGE_CREATE_TRANSFER, "Unable to submit maternity transfer to HIE");
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
			@RequestParam("uuid") String uuid,
			@RequestParam(value = "formType", required = false) String formType) throws Exception {

		Map<String, Object> data = new HashMap<String, Object>();

		if (!TransferPrivilegeHelper.hasPrivilege(TransferAppActivator.PRIVILEGE_LIST_TRANSFERS)) {
			writePrivilegeDenied(response, data, TransferAppActivator.PRIVILEGE_LIST_TRANSFERS);
			return;
		}

		try {
			if (FORM_TYPE_MATERNITY.equalsIgnoreCase(StringUtils.trimToNull(formType))) {
				MaternityTransfer maternityTransfer = maternityTransferService.getMaternityTransferByUuid(uuid);
				if (maternityTransfer == null) {
					data.put("status", "error");
					data.put("message", "Transfer not found");
				} else {
					data.put("status", "success");
					data.put("transfer", toMaternityPreviewMap(maternityTransfer));
				}
			} else if (FORM_TYPE_NEONATAL.equalsIgnoreCase(StringUtils.trimToNull(formType))) {
				NeonatalTransfer neonatalTransfer = neonatalTransferService.getNeonatalTransferByUuid(uuid);
				if (neonatalTransfer == null) {
					data.put("status", "error");
					data.put("message", "Transfer not found");
				} else {
					data.put("status", "success");
					data.put("transfer", toNeonatalPreviewMap(neonatalTransfer));
				}
			} else {
				Transfer transfer = transferService.getTransferByUuid(uuid);
				if (transfer == null) {
					data.put("status", "error");
					data.put("message", "Transfer not found");
				} else {
					data.put("status", "success");
					data.put("transfer", toPreviewMap(transfer));
				}
			}
		}
		catch (Exception e) {
			putError(data, e, TransferAppActivator.PRIVILEGE_LIST_TRANSFERS, "Unable to load transfer");
		}

		writeJson(response, data);
	}

	@RequestMapping(value = "/module/transferapp/transfer/saveMaternity.form", method = RequestMethod.POST)
	public void saveMaternityTransfer(HttpServletResponse response,
			@RequestParam("patientId") Integer patientId,
			@RequestParam(value = "receivingFacilityId", required = false) Integer receivingFacilityId,
			@RequestParam(value = "transferUuid", required = false) String transferUuid,
			// Facility details (header)
			@RequestParam(value = "province", required = false) String province,
			@RequestParam(value = "district", required = false) String district,
			@RequestParam(value = "hospitalName", required = false) String hospitalName,
			@RequestParam(value = "referringFacilityName", required = false) String referringFacilityName,
			@RequestParam(value = "referringUnit", required = false) String referringUnit,
			// Step 1 — client & referral info
			@RequestParam(value = "clientName", required = false) String clientName,
			@RequestParam(value = "serialNumberEmr", required = false) String serialNumberEmr,
			@RequestParam(value = "ageOrDob", required = false) String ageOrDob,
			@RequestParam(value = "nextOfKinName", required = false) String nextOfKinName,
			@RequestParam(value = "nextOfKinTelephone", required = false) String nextOfKinTelephone,
			@RequestParam(value = "clientDistrict", required = false) String clientDistrict,
			@RequestParam(value = "sector", required = false) String sector,
			@RequestParam(value = "cell", required = false) String cell,
			@RequestParam(value = "village", required = false) String village,
			@RequestParam(value = "admissionAt", required = false) String admissionAt,
			@RequestParam(value = "decisionToTransferAt", required = false) String decisionToTransferAt,
			@RequestParam(value = "receivingFacilityCode", required = false) String receivingFacilityCode,
			@RequestParam(value = "receivingService", required = false) String receivingService,
			@RequestParam(value = "callingTime", required = false) String callingTime,
			@RequestParam(value = "staffContactedName", required = false) String staffContactedName,
			@RequestParam(value = "staffContactedPhone", required = false) String staffContactedPhone,
			@RequestParam(value = "reasonForTransfer", required = false) String reasonForTransfer,
			@RequestParam(value = "transferType", required = false) String transferType,
			@RequestParam(value = "ambulanceCalledTime", required = false) String ambulanceCalledTime,
			@RequestParam(value = "departureFromReferringTime", required = false) String departureFromReferringTime,
			@RequestParam(value = "partographAttached", required = false) String partographAttached,
			@RequestParam(value = "clinicalPresentation", required = false) String clinicalPresentation,
			@RequestParam(value = "disabilityType", required = false) String disabilityType,
			// Step 2 — obstetric history & current pregnancy
			@RequestParam(value = "obstetricGravida", required = false) String obstetricGravida,
			@RequestParam(value = "obstetricParity", required = false) String obstetricParity,
			@RequestParam(value = "obstetricLivingChildren", required = false) String obstetricLivingChildren,
			@RequestParam(value = "obstetricAbortion", required = false) String obstetricAbortion,
			@RequestParam(value = "obstetricStillbirth", required = false) String obstetricStillbirth,
			@RequestParam(value = "obstetricNeonatalDeath", required = false) String obstetricNeonatalDeath,
			@RequestParam(value = "obstetricPretermBirth", required = false) String obstetricPretermBirth,
			@RequestParam(value = "lmpDate", required = false) String lmpDate,
			@RequestParam(value = "eddDate", required = false) String eddDate,
			@RequestParam(value = "gestationAge", required = false) String gestationAge,
			@RequestParam(value = "muac", required = false) String muac,
			@RequestParam(value = "ancCompletedCount", required = false) String ancCompletedCount,
			@RequestParam(value = "tetanusVaccineDoses", required = false) String tetanusVaccineDoses,
			@RequestParam(value = "previousSignificantHistory", required = false) String previousSignificantHistory,
			@RequestParam(value = "multiPregnanciesAndKnownHiv", required = false) String multiPregnanciesAndKnownHiv,
			@RequestParam(value = "currentPregnancyComplications", required = false) String currentPregnancyComplications,
			// Step 3 — clinical findings
			@RequestParam(value = "latestHemoglobin", required = false) String latestHemoglobin,
			@RequestParam(value = "latestHivStatus", required = false) String latestHivStatus,
			@RequestParam(value = "latestBloodGroup", required = false) String latestBloodGroup,
			@RequestParam(value = "latestOtherResults", required = false) String latestOtherResults,
			@RequestParam(value = "vitalBp", required = false) String vitalBp,
			@RequestParam(value = "vitalTemp", required = false) String vitalTemp,
			@RequestParam(value = "vitalSpo2", required = false) String vitalSpo2,
			@RequestParam(value = "vitalRr", required = false) String vitalRr,
			@RequestParam(value = "vitalPulse", required = false) String vitalPulse,
			@RequestParam(value = "vitalWeight", required = false) String vitalWeight,
			@RequestParam(value = "vitalHeight", required = false) String vitalHeight,
			@RequestParam(value = "fetalPresentation", required = false) String fetalPresentation,
			@RequestParam(value = "fundalHeight", required = false) String fundalHeight,
			@RequestParam(value = "fetalHeartRate", required = false) String fetalHeartRate,
			@RequestParam(value = "contractions", required = false) String contractions,
			@RequestParam(value = "vaginalExamAt", required = false) String vaginalExamAt,
			@RequestParam(value = "dilation", required = false) String dilation,
			@RequestParam(value = "effacement", required = false) String effacement,
			@RequestParam(value = "descent", required = false) String descent,
			@RequestParam(value = "consistency", required = false) String consistency,
			@RequestParam(value = "position", required = false) String position,
			@RequestParam(value = "caput", required = false) String caput,
			@RequestParam(value = "moulding", required = false) String moulding,
			@RequestParam(value = "membranesRuptured", required = false) String membranesRuptured,
			@RequestParam(value = "membranesRupturedAt", required = false) String membranesRupturedAt,
			@RequestParam(value = "amnioticFluidColor", required = false) String amnioticFluidColor,
			@RequestParam(value = "estimatedBloodLossMl", required = false) String estimatedBloodLossMl,
			@RequestParam(value = "investigationHgb", required = false) String investigationHgb,
			@RequestParam(value = "investigationUrineTest", required = false) String investigationUrineTest,
			@RequestParam(value = "investigationOtherTest", required = false) String investigationOtherTest,
			@RequestParam(value = "imagingInvestigations", required = false) String imagingInvestigations,
			@RequestParam(value = "diagnosis", required = false) String diagnosis,
			@RequestParam(value = "procedures", required = false) String procedures,
			@RequestParam(value = "attachedLabTests", required = false) String attachedLabTests,
			@RequestParam(value = "attachedImaging", required = false) String attachedImaging,
			@RequestParam(value = "attachedOther", required = false) String attachedOther,
			// Step 4 — treatment & transport
			@RequestParam(value = "transportationType", required = false) String transportationType,
			@RequestParam(value = "transportationOtherSpec", required = false) String transportationOtherSpec,
			@RequestParam(value = "treatmentName", required = false) String[] treatmentNames,
			@RequestParam(value = "treatmentDose", required = false) String[] treatmentDoses,
			@RequestParam(value = "treatmentGivenDate", required = false) String[] treatmentGivenDates,
			@RequestParam(value = "treatmentGivenTime", required = false) String[] treatmentGivenTimes,
			// Step 5 — sign-off
			@RequestParam(value = "healthInsuranceType", required = false) String healthInsuranceType,
			@RequestParam(value = "healthInsuranceOtherSpec", required = false) String healthInsuranceOtherSpec,
			@RequestParam(value = "referringProviderName", required = false) String referringProviderName,
			@RequestParam(value = "referringProviderQualification", required = false) String referringProviderQualification,
			@RequestParam(value = "referringSignedDate", required = false) String referringSignedDate,
			@RequestParam(value = "referringSignedTime", required = false) String referringSignedTime,
			@RequestParam(value = "referringProviderPhone", required = false) String referringProviderPhone) throws Exception {

		Map<String, Object> data = new HashMap<String, Object>();

		if (!TransferPrivilegeHelper.hasPrivilege(TransferAppActivator.PRIVILEGE_CREATE_TRANSFER)) {
			writePrivilegeDenied(response, data, TransferAppActivator.PRIVILEGE_CREATE_TRANSFER);
			return;
		}

		try {
			MaternityTransferFormData formData = new MaternityTransferFormData();
			formData.setTransferUuid(transferUuid);
			formData.setProvince(province);
			formData.setDistrict(district);
			formData.setHospitalName(hospitalName);
			formData.setReferringFacilityName(referringFacilityName);
			formData.setReferringUnit(referringUnit);
			formData.setClientName(clientName);
			formData.setSerialNumberEmr(serialNumberEmr);
			formData.setAgeOrDob(ageOrDob);
			formData.setNextOfKinName(nextOfKinName);
			formData.setNextOfKinTelephone(nextOfKinTelephone);
			formData.setClientDistrict(clientDistrict);
			formData.setSector(sector);
			formData.setCell(cell);
			formData.setVillage(village);
			formData.setAdmissionAt(admissionAt);
			formData.setDecisionToTransferAt(decisionToTransferAt);
			formData.setReceivingFacilityCode(receivingFacilityCode);
			formData.setReceivingService(receivingService);
			formData.setCallingTime(callingTime);
			formData.setStaffContactedName(staffContactedName);
			formData.setStaffContactedPhone(staffContactedPhone);
			formData.setReasonForTransfer(reasonForTransfer);
			formData.setTransferType(transferType);
			formData.setAmbulanceCalledTime(ambulanceCalledTime);
			formData.setDepartureFromReferringTime(departureFromReferringTime);
			formData.setPartographAttached(partographAttached);
			formData.setClinicalPresentation(clinicalPresentation);
			formData.setDisabilityType(disabilityType);

			formData.setObstetricGravida(obstetricGravida);
			formData.setObstetricParity(obstetricParity);
			formData.setObstetricLivingChildren(obstetricLivingChildren);
			formData.setObstetricAbortion(obstetricAbortion);
			formData.setObstetricStillbirth(obstetricStillbirth);
			formData.setObstetricNeonatalDeath(obstetricNeonatalDeath);
			formData.setObstetricPretermBirth(obstetricPretermBirth);
			formData.setLmpDate(lmpDate);
			formData.setEddDate(eddDate);
			formData.setGestationAge(gestationAge);
			formData.setMuac(muac);
			formData.setAncCompletedCount(ancCompletedCount);
			formData.setTetanusVaccineDoses(tetanusVaccineDoses);
			formData.setPreviousSignificantHistory(previousSignificantHistory);
			formData.setMultiPregnanciesAndKnownHiv(multiPregnanciesAndKnownHiv);
			formData.setCurrentPregnancyComplications(currentPregnancyComplications);

			formData.setLatestHemoglobin(latestHemoglobin);
			formData.setLatestHivStatus(latestHivStatus);
			formData.setLatestBloodGroup(latestBloodGroup);
			formData.setLatestOtherResults(latestOtherResults);
			formData.setVitalBp(vitalBp);
			formData.setVitalTemp(vitalTemp);
			formData.setVitalSpo2(vitalSpo2);
			formData.setVitalRr(vitalRr);
			formData.setVitalPulse(vitalPulse);
			formData.setVitalWeight(vitalWeight);
			formData.setVitalHeight(vitalHeight);
			formData.setFetalPresentation(fetalPresentation);
			formData.setFundalHeight(fundalHeight);
			formData.setFetalHeartRate(fetalHeartRate);
			formData.setContractions(contractions);
			formData.setVaginalExamAt(vaginalExamAt);
			formData.setDilation(dilation);
			formData.setEffacement(effacement);
			formData.setDescent(descent);
			formData.setConsistency(consistency);
			formData.setPosition(position);
			formData.setCaput(caput);
			formData.setMoulding(moulding);
			formData.setMembranesRuptured(membranesRuptured);
			formData.setMembranesRupturedAt(membranesRupturedAt);
			formData.setAmnioticFluidColor(amnioticFluidColor);
			formData.setEstimatedBloodLossMl(estimatedBloodLossMl);
			formData.setInvestigationHgb(investigationHgb);
			formData.setInvestigationUrineTest(investigationUrineTest);
			formData.setInvestigationOtherTest(investigationOtherTest);
			formData.setImagingInvestigations(imagingInvestigations);
			formData.setDiagnosis(diagnosis);
			formData.setProcedures(procedures);
			formData.setAttachedLabTests(attachedLabTests);
			formData.setAttachedImaging(attachedImaging);
			formData.setAttachedOther(attachedOther);

			formData.setTransportationType(transportationType);
			formData.setTransportationOtherSpec(transportationOtherSpec);

			formData.setHealthInsuranceType(healthInsuranceType);
			formData.setHealthInsuranceOtherSpec(healthInsuranceOtherSpec);
			formData.setReferringProviderName(referringProviderName);
			formData.setReferringProviderQualification(referringProviderQualification);
			formData.setReferringSignedDate(referringSignedDate);
			formData.setReferringSignedTime(referringSignedTime);
			formData.setReferringProviderPhone(referringProviderPhone);

			List<MaternityTransferTreatmentRow> treatmentRows = buildTreatmentRows(
					treatmentNames, treatmentDoses, treatmentGivenDates, treatmentGivenTimes);

			MaternityTransfer transfer = maternityTransferService.saveMaternityTransfer(
					patientId, receivingFacilityId, formData, treatmentRows);

			data.put("status", "success");
			data.put("transferId", transfer.getMaternityTransferId());
			data.put("uuid", transfer.getUuid());
		}
		catch (Exception e) {
			putError(data, e, TransferAppActivator.PRIVILEGE_CREATE_TRANSFER, "Unable to save maternity transfer");
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

	@RequestMapping(value = "/module/transferapp/transfer/saveNeonatal.form", method = RequestMethod.POST)
	public void saveNeonatalTransfer(HttpServletResponse response,
			@RequestParam("patientId") Integer patientId,
			@RequestParam(value = "receivingFacilityId", required = false) Integer receivingFacilityId,
			@RequestParam(value = "transferUuid", required = false) String transferUuid,
			// Facility details (header)
			@RequestParam(value = "province", required = false) String province,
			@RequestParam(value = "district", required = false) String district,
			@RequestParam(value = "hospitalName", required = false) String hospitalName,
			@RequestParam(value = "referringFacilityName", required = false) String referringFacilityName,
			@RequestParam(value = "referringUnit", required = false) String referringUnit,
			// Step 1 — baby & referral info
			@RequestParam(value = "babyName", required = false) String babyName,
			@RequestParam(value = "sex", required = false) String sex,
			@RequestParam(value = "dob", required = false) String dob,
			@RequestParam(value = "gestationalAgeWeeks", required = false) String gestationalAgeWeeks,
			@RequestParam(value = "birthWeightG", required = false) String birthWeightG,
			@RequestParam(value = "currentWeightG", required = false) String currentWeightG,
			@RequestParam(value = "currentAgeDays", required = false) String currentAgeDays,
			@RequestParam(value = "motherName", required = false) String motherName,
			@RequestParam(value = "motherAge", required = false) String motherAge,
			@RequestParam(value = "motherCaregiverPhone", required = false) String motherCaregiverPhone,
			@RequestParam(value = "placeOfBirth", required = false) String placeOfBirth,
			@RequestParam(value = "reasonForTransfer", required = false) String reasonForTransfer,
			@RequestParam(value = "modeOfTransport", required = false) String modeOfTransport,
			@RequestParam(value = "transportOther", required = false) String transportOther,
			@RequestParam(value = "transferType", required = false) String transferType,
			@RequestParam(value = "receivingFacilityCode", required = false) String receivingFacilityCode,
			@RequestParam(value = "receivingService", required = false) String receivingService,
			@RequestParam(value = "callingTime", required = false) String callingTime,
			@RequestParam(value = "staffContactedName", required = false) String staffContactedName,
			@RequestParam(value = "staffContactedPhone", required = false) String staffContactedPhone,
			@RequestParam(value = "decisionToTransferAt", required = false) String decisionToTransferAt,
			// Step 2 — maternal history
			@RequestParam(value = "motherAlive", required = false) String motherAlive,
			@RequestParam(value = "obstetricGravida", required = false) String obstetricGravida,
			@RequestParam(value = "obstetricParity", required = false) String obstetricParity,
			@RequestParam(value = "pregnancyType", required = false) String pregnancyType,
			@RequestParam(value = "ancScreening", required = false) String[] ancScreening,
			@RequestParam(value = "pathologiesDuringPregnancy", required = false) String[] pathologiesDuringPregnancy,
			@RequestParam(value = "pregnancyOtherPathologies", required = false) String pregnancyOtherPathologies,
			@RequestParam(value = "pregnancyTreatment", required = false) String pregnancyTreatment,
			@RequestParam(value = "bloodGroup", required = false) String bloodGroup,
			@RequestParam(value = "rhFactor", required = false) String rhFactor,
			@RequestParam(value = "hivStatus", required = false) String hivStatus,
			@RequestParam(value = "hivRegimen", required = false) String hivRegimen,
			@RequestParam(value = "hivRecentVl", required = false) String hivRecentVl,
			@RequestParam(value = "hivCd4Count", required = false) String hivCd4Count,
			@RequestParam(value = "hivOpportunisticInfections", required = false) String hivOpportunisticInfections,
			@RequestParam(value = "tetanusVaccineDoses", required = false) String tetanusVaccineDoses,
			@RequestParam(value = "maternalIllicitDrugHistory", required = false) String maternalIllicitDrugHistory,
			// Step 3 — labor details
			@RequestParam(value = "romAt", required = false) String romAt,
			@RequestParam(value = "afQuality", required = false) String afQuality,
			@RequestParam(value = "afQuantity", required = false) String afQuantity,
			@RequestParam(value = "feverTiming", required = false) String feverTiming,
			@RequestParam(value = "steroidDoses", required = false) String steroidDoses,
			@RequestParam(value = "lastSteroidDoseAt", required = false) String lastSteroidDoseAt,
			@RequestParam(value = "mgso4At", required = false) String mgso4At,
			@RequestParam(value = "modeOfDelivery", required = false) String modeOfDelivery,
			@RequestParam(value = "laborComplications", required = false) String[] laborComplications,
			@RequestParam(value = "laborComplicationsOther", required = false) String laborComplicationsOther,
			@RequestParam(value = "maternalAnesthesia", required = false) String maternalAnesthesia,
			@RequestParam(value = "maternalAnesthesiaOther", required = false) String maternalAnesthesiaOther,
			@RequestParam(value = "maternalAntibiotics", required = false) String maternalAntibiotics,
			@RequestParam(value = "otherDrugs", required = false) String otherDrugs,
			@RequestParam(value = "sepsisRiskFactors", required = false) String[] sepsisRiskFactors,
			// Step 4 — neonatal history & drugs
			@RequestParam(value = "resuscitationAtBirth", required = false) String resuscitationAtBirth,
			@RequestParam(value = "resuscitationMethods", required = false) String[] resuscitationMethods,
			@RequestParam(value = "apgar1min", required = false) String apgar1min,
			@RequestParam(value = "apgar5min", required = false) String apgar5min,
			@RequestParam(value = "apgar10min", required = false) String apgar10min,
			@RequestParam(value = "hie", required = false) String hie,
			@RequestParam(value = "hieGrade", required = false) String hieGrade,
			@RequestParam(value = "allergies", required = false) String allergies,
			@RequestParam(value = "immunization", required = false) String immunization,
			@RequestParam(value = "immunizationDetails", required = false) String immunizationDetails,
			@RequestParam(value = "vitaminK", required = false) String vitaminK,
			@RequestParam(value = "tetracyclineEyeOintment", required = false) String tetracyclineEyeOintment,
			@RequestParam(value = "surfactant", required = false) String surfactant,
			// Step 5 — chief complaint & diagnoses
			@RequestParam(value = "chiefComplaintDetails", required = false) String chiefComplaintDetails,
			@RequestParam(value = "spo2Preductal", required = false) String spo2Preductal,
			@RequestParam(value = "spo2Postductal", required = false) String spo2Postductal,
			@RequestParam(value = "conditionTemp", required = false) String conditionTemp,
			@RequestParam(value = "conditionHr", required = false) String conditionHr,
			@RequestParam(value = "conditionRr", required = false) String conditionRr,
			@RequestParam(value = "conditionBp", required = false) String conditionBp,
			@RequestParam(value = "neurologicalStatus", required = false) String[] neurologicalStatus,
			@RequestParam(value = "seizures", required = false) String seizures,
			@RequestParam(value = "adverseEvents24h", required = false) String adverseEvents24h,
			@RequestParam(value = "diagnosis1", required = false) String diagnosis1,
			@RequestParam(value = "diagnosis2", required = false) String diagnosis2,
			@RequestParam(value = "diagnosis3", required = false) String diagnosis3,
			@RequestParam(value = "diagnosis4", required = false) String diagnosis4,
			// Step 6 — management at referring facility
			@RequestParam(value = "respiratorySupport", required = false) String respiratorySupport,
			@RequestParam(value = "ventilationSettings", required = false) String ventilationSettings,
			@RequestParam(value = "bloodGasAnalysis", required = false) String bloodGasAnalysis,
			@RequestParam(value = "ivFluidVol", required = false) String ivFluidVol,
			@RequestParam(value = "passedUrine", required = false) String passedUrine,
			@RequestParam(value = "inotropes", required = false) String inotropes,
			@RequestParam(value = "inotropesSpecify", required = false) String inotropesSpecify,
			@RequestParam(value = "peripheralIv", required = false) String peripheralIv,
			@RequestParam(value = "centralIv", required = false) String centralIv,
			@RequestParam(value = "intraosseousLine", required = false) String intraosseousLine,
			@RequestParam(value = "antibiotic1Name", required = false) String antibiotic1Name,
			@RequestParam(value = "antibiotic1Doses", required = false) String antibiotic1Doses,
			@RequestParam(value = "antibiotic1Durations", required = false) String antibiotic1Durations,
			@RequestParam(value = "antibiotic2Name", required = false) String antibiotic2Name,
			@RequestParam(value = "antibiotic2Doses", required = false) String antibiotic2Doses,
			@RequestParam(value = "antibiotic2Durations", required = false) String antibiotic2Durations,
			@RequestParam(value = "arvs", required = false) String arvs,
			@RequestParam(value = "npo", required = false) String npo,
			@RequestParam(value = "lastFeedTime", required = false) String lastFeedTime,
			@RequestParam(value = "lastFeedAmount", required = false) String lastFeedAmount,
			@RequestParam(value = "feedVol", required = false) String feedVol,
			@RequestParam(value = "feedType", required = false) String feedType,
			@RequestParam(value = "passedStool", required = false) String passedStool,
			@RequestParam(value = "nasogastricTube", required = false) String nasogastricTube,
			@RequestParam(value = "labGlucose", required = false) String labGlucose,
			@RequestParam(value = "labHb", required = false) String labHb,
			@RequestParam(value = "labWbc", required = false) String labWbc,
			@RequestParam(value = "labPlatelets", required = false) String labPlatelets,
			@RequestParam(value = "labCrp", required = false) String labCrp,
			@RequestParam(value = "labBiliTotal", required = false) String labBiliTotal,
			@RequestParam(value = "labBiliDirect", required = false) String labBiliDirect,
			@RequestParam(value = "labUe", required = false) String labUe,
			@RequestParam(value = "labCultures", required = false) String labCultures,
			@RequestParam(value = "fbcDone", required = false) String fbcDone,
			@RequestParam(value = "imagingResultsAvailable", required = false) String imagingResultsAvailable,
			@RequestParam(value = "imagingResults", required = false) String imagingResults,
			@RequestParam(value = "painSedationDrugs", required = false) String painSedationDrugs,
			@RequestParam(value = "imagingReportAttached", required = false) String imagingReportAttached,
			@RequestParam(value = "labReportsAttached", required = false) String labReportsAttached,
			// Step 7 — summary & sign-off
			@RequestParam(value = "clinicalManagementSummary", required = false) String clinicalManagementSummary,
			@RequestParam(value = "referringProviderName", required = false) String referringProviderName,
			@RequestParam(value = "referringProviderQualification", required = false) String referringProviderQualification,
			@RequestParam(value = "referringSignedDate", required = false) String referringSignedDate,
			@RequestParam(value = "referringSignedTime", required = false) String referringSignedTime,
			@RequestParam(value = "referringProviderPhone", required = false) String referringProviderPhone) throws Exception {

		Map<String, Object> data = new HashMap<String, Object>();

		if (!TransferPrivilegeHelper.hasPrivilege(TransferAppActivator.PRIVILEGE_CREATE_TRANSFER)) {
			writePrivilegeDenied(response, data, TransferAppActivator.PRIVILEGE_CREATE_TRANSFER);
			return;
		}

		try {
			NeonatalTransferFormData formData = new NeonatalTransferFormData();
			formData.setTransferUuid(transferUuid);
			formData.setProvince(province);
			formData.setDistrict(district);
			formData.setHospitalName(hospitalName);
			formData.setReferringFacilityName(referringFacilityName);
			formData.setReferringUnit(referringUnit);
			formData.setBabyName(babyName);
			formData.setSex(sex);
			formData.setDob(dob);
			formData.setGestationalAgeWeeks(gestationalAgeWeeks);
			formData.setBirthWeightG(birthWeightG);
			formData.setCurrentWeightG(currentWeightG);
			formData.setCurrentAgeDays(currentAgeDays);
			formData.setMotherName(motherName);
			formData.setMotherAge(motherAge);
			formData.setMotherCaregiverPhone(motherCaregiverPhone);
			formData.setPlaceOfBirth(placeOfBirth);
			formData.setReasonForTransfer(reasonForTransfer);
			formData.setModeOfTransport(modeOfTransport);
			formData.setTransportOther(transportOther);
			formData.setTransferType(transferType);
			formData.setReceivingFacilityCode(receivingFacilityCode);
			formData.setReceivingService(receivingService);
			formData.setCallingTime(callingTime);
			formData.setStaffContactedName(staffContactedName);
			formData.setStaffContactedPhone(staffContactedPhone);
			formData.setDecisionToTransferAt(decisionToTransferAt);

			formData.setMotherAlive(motherAlive);
			formData.setObstetricGravida(obstetricGravida);
			formData.setObstetricParity(obstetricParity);
			formData.setPregnancyType(pregnancyType);
			formData.setAncScreening(ancScreening == null ? null : StringUtils.join(ancScreening, "; "));
			formData.setPathologiesDuringPregnancy(
					pathologiesDuringPregnancy == null ? null : StringUtils.join(pathologiesDuringPregnancy, "; "));
			formData.setPregnancyOtherPathologies(pregnancyOtherPathologies);
			formData.setPregnancyTreatment(pregnancyTreatment);
			formData.setBloodGroup(bloodGroup);
			formData.setRhFactor(rhFactor);
			formData.setHivStatus(hivStatus);
			formData.setHivRegimen(hivRegimen);
			formData.setHivRecentVl(hivRecentVl);
			formData.setHivCd4Count(hivCd4Count);
			formData.setHivOpportunisticInfections(hivOpportunisticInfections);
			formData.setTetanusVaccineDoses(tetanusVaccineDoses);
			formData.setMaternalIllicitDrugHistory(maternalIllicitDrugHistory);

			formData.setRomAt(romAt);
			formData.setAfQuality(afQuality);
			formData.setAfQuantity(afQuantity);
			formData.setFeverTiming(feverTiming);
			formData.setSteroidDoses(steroidDoses);
			formData.setLastSteroidDoseAt(lastSteroidDoseAt);
			formData.setMgso4At(mgso4At);
			formData.setModeOfDelivery(modeOfDelivery);
			formData.setLaborComplications(laborComplications == null ? null : StringUtils.join(laborComplications, "; "));
			formData.setLaborComplicationsOther(laborComplicationsOther);
			formData.setMaternalAnesthesia(maternalAnesthesia);
			formData.setMaternalAnesthesiaOther(maternalAnesthesiaOther);
			formData.setMaternalAntibiotics(maternalAntibiotics);
			formData.setOtherDrugs(otherDrugs);
			formData.setSepsisRiskFactors(sepsisRiskFactors == null ? null : StringUtils.join(sepsisRiskFactors, "; "));

			formData.setResuscitationAtBirth(resuscitationAtBirth);
			formData.setResuscitationMethods(
					resuscitationMethods == null ? null : StringUtils.join(resuscitationMethods, "; "));
			formData.setApgar1min(apgar1min);
			formData.setApgar5min(apgar5min);
			formData.setApgar10min(apgar10min);
			formData.setHie(hie);
			formData.setHieGrade(hieGrade);
			formData.setAllergies(allergies);
			formData.setImmunization(immunization);
			formData.setImmunizationDetails(immunizationDetails);
			formData.setVitaminK(vitaminK);
			formData.setTetracyclineEyeOintment(tetracyclineEyeOintment);
			formData.setSurfactant(surfactant);

			formData.setChiefComplaintDetails(chiefComplaintDetails);
			formData.setSpo2Preductal(spo2Preductal);
			formData.setSpo2Postductal(spo2Postductal);
			formData.setConditionTemp(conditionTemp);
			formData.setConditionHr(conditionHr);
			formData.setConditionRr(conditionRr);
			formData.setConditionBp(conditionBp);
			formData.setNeurologicalStatus(neurologicalStatus == null ? null : StringUtils.join(neurologicalStatus, "; "));
			formData.setSeizures(seizures);
			formData.setAdverseEvents24h(adverseEvents24h);
			formData.setDiagnosis1(diagnosis1);
			formData.setDiagnosis2(diagnosis2);
			formData.setDiagnosis3(diagnosis3);
			formData.setDiagnosis4(diagnosis4);

			formData.setRespiratorySupport(respiratorySupport);
			formData.setVentilationSettings(ventilationSettings);
			formData.setBloodGasAnalysis(bloodGasAnalysis);
			formData.setIvFluidVol(ivFluidVol);
			formData.setPassedUrine(passedUrine);
			formData.setInotropes(inotropes);
			formData.setInotropesSpecify(inotropesSpecify);
			formData.setPeripheralIv(peripheralIv);
			formData.setCentralIv(centralIv);
			formData.setIntraosseousLine(intraosseousLine);
			formData.setAntibiotic1Name(antibiotic1Name);
			formData.setAntibiotic1Doses(antibiotic1Doses);
			formData.setAntibiotic1Durations(antibiotic1Durations);
			formData.setAntibiotic2Name(antibiotic2Name);
			formData.setAntibiotic2Doses(antibiotic2Doses);
			formData.setAntibiotic2Durations(antibiotic2Durations);
			formData.setArvs(arvs);
			formData.setNpo(npo);
			formData.setLastFeedTime(lastFeedTime);
			formData.setLastFeedAmount(lastFeedAmount);
			formData.setFeedVol(feedVol);
			formData.setFeedType(feedType);
			formData.setPassedStool(passedStool);
			formData.setNasogastricTube(nasogastricTube);
			formData.setLabGlucose(labGlucose);
			formData.setLabHb(labHb);
			formData.setLabWbc(labWbc);
			formData.setLabPlatelets(labPlatelets);
			formData.setLabCrp(labCrp);
			formData.setLabBiliTotal(labBiliTotal);
			formData.setLabBiliDirect(labBiliDirect);
			formData.setLabUe(labUe);
			formData.setLabCultures(labCultures);
			formData.setFbcDone(fbcDone);
			formData.setImagingResultsAvailable(imagingResultsAvailable);
			formData.setImagingResults(imagingResults);
			formData.setPainSedationDrugs(painSedationDrugs);
			formData.setImagingReportAttached(imagingReportAttached);
			formData.setLabReportsAttached(labReportsAttached);

			formData.setClinicalManagementSummary(clinicalManagementSummary);
			formData.setReferringProviderName(referringProviderName);
			formData.setReferringProviderQualification(referringProviderQualification);
			formData.setReferringSignedDate(referringSignedDate);
			formData.setReferringSignedTime(referringSignedTime);
			formData.setReferringProviderPhone(referringProviderPhone);

			NeonatalTransfer transfer = neonatalTransferService.saveNeonatalTransfer(
					patientId, receivingFacilityId, formData);

			data.put("status", "success");
			data.put("transferId", transfer.getNeonatalTransferId());
			data.put("uuid", transfer.getUuid());
		}
		catch (Exception e) {
			putError(data, e, TransferAppActivator.PRIVILEGE_CREATE_TRANSFER, "Unable to save neonatal transfer");
		}

		writeJson(response, data);
	}

	private List<MaternityTransferTreatmentRow> buildTreatmentRows(String[] treatmentNames, String[] doses,
			String[] givenDates, String[] givenTimes) {
		List<MaternityTransferTreatmentRow> rows = new ArrayList<MaternityTransferTreatmentRow>();
		if (treatmentNames == null) {
			return rows;
		}
		for (int i = 0; i < treatmentNames.length; i++) {
			MaternityTransferTreatmentRow row = new MaternityTransferTreatmentRow();
			row.setTreatmentName(treatmentNames[i]);
			row.setDose(doses != null && i < doses.length ? doses[i] : null);
			row.setGivenDate(givenDates != null && i < givenDates.length ? givenDates[i] : null);
			row.setGivenTime(givenTimes != null && i < givenTimes.length ? givenTimes[i] : null);
			rows.add(row);
		}
		return rows;
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
		preview.put("formType", FORM_TYPE_EXTERNAL);

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

	private Map<String, Object> toMaternityPreviewMap(MaternityTransfer transfer) {
		Map<String, Object> preview = new HashMap<String, Object>();
		preview.put("uuid", transfer.getUuid());
		preview.put("formType", FORM_TYPE_MATERNITY);
		preview.put("hieSent", transfer.isSentToHie());
		preview.put("hieSentAt", formatDateTime(transfer.getHieSentAt()));
		preview.put("hieTransferId", nullToEmpty(transfer.getHieTransferId()));

		preview.put("province", nullToEmpty(transfer.getProvince()));
		preview.put("district", nullToEmpty(transfer.getDistrict()));
		preview.put("hospitalName", nullToEmpty(transfer.getHospitalName()));
		preview.put("referringFacilityName", nullToEmpty(transfer.getReferringFacilityName()));
		preview.put("referringUnit", nullToEmpty(transfer.getReferringUnit()));

		preview.put("clientName", nullToEmpty(transfer.getClientName()));
		preview.put("serialNumberEmr", nullToEmpty(transfer.getSerialNumberEmr()));
		preview.put("ageOrDob", nullToEmpty(transfer.getAgeOrDob()));
		preview.put("nextOfKinName", nullToEmpty(transfer.getNextOfKinName()));
		preview.put("nextOfKinTelephone", nullToEmpty(transfer.getNextOfKinTelephone()));
		preview.put("clientDistrict", nullToEmpty(transfer.getClientDistrict()));
		preview.put("sector", nullToEmpty(transfer.getSector()));
		preview.put("cell", nullToEmpty(transfer.getCell()));
		preview.put("village", nullToEmpty(transfer.getVillage()));

		preview.put("admissionAt", formatDateTime(transfer.getAdmissionAt()));
		preview.put("decisionToTransferAt", formatDateTime(transfer.getDecisionToTransferAt()));
		preview.put("receivingFacility", resolveFacilityLabel(transfer.getReceivingFacilityCode()));
		preview.put("receivingFacilityCode", nullToEmpty(transfer.getReceivingFacilityCode()));
		preview.put("receivingService", nullToEmpty(transfer.getReceivingService()));
		preview.put("callingTime", nullToEmpty(transfer.getCallingTime()));
		preview.put("staffContactedName", nullToEmpty(transfer.getStaffContactedName()));
		preview.put("staffContactedPhone", nullToEmpty(transfer.getStaffContactedPhone()));
		preview.put("reasonForTransfer", nullToEmpty(transfer.getReasonForTransfer()));

		String transferType = nullToEmpty(transfer.getTransferType());
		preview.put("transferType", transferType);
		preview.put("isEmergency", "EMERGENCY".equals(transferType));
		preview.put("isNonEmergency", "NOT_EMERGENCY".equals(transferType));
		preview.put("isFollowUp", "FOLLOW_UP".equals(transferType));
		preview.put("ambulanceCalledTime", nullToEmpty(transfer.getAmbulanceCalledTime()));
		preview.put("departureFromReferringTime", nullToEmpty(transfer.getDepartureFromReferringTime()));
		preview.put("partographAttached", Boolean.TRUE.equals(transfer.getPartographAttached()));
		preview.put("clinicalPresentation", nullToEmpty(transfer.getClinicalPresentation()));
		preview.put("disabilityType", nullToEmpty(transfer.getDisabilityType()));

		preview.put("obstetricGravida", nullToEmpty(transfer.getObstetricGravida()));
		preview.put("obstetricParity", nullToEmpty(transfer.getObstetricParity()));
		preview.put("obstetricLivingChildren", nullToEmpty(transfer.getObstetricLivingChildren()));
		preview.put("obstetricAbortion", nullToEmpty(transfer.getObstetricAbortion()));
		preview.put("obstetricStillbirth", nullToEmpty(transfer.getObstetricStillbirth()));
		preview.put("obstetricNeonatalDeath", nullToEmpty(transfer.getObstetricNeonatalDeath()));
		preview.put("obstetricPretermBirth", nullToEmpty(transfer.getObstetricPretermBirth()));
		preview.put("lmpDate", formatDateOnly(transfer.getLmpDate()));
		preview.put("eddDate", formatDateOnly(transfer.getEddDate()));
		preview.put("gestationAge", nullToEmpty(transfer.getGestationAge()));
		preview.put("muac", nullToEmpty(transfer.getMuac()));
		preview.put("ancCompletedCount", nullToEmpty(transfer.getAncCompletedCount()));
		preview.put("tetanusVaccineDoses", nullToEmpty(transfer.getTetanusVaccineDoses()));
		preview.put("previousSignificantHistory", nullToEmpty(transfer.getPreviousSignificantHistory()));
		preview.put("multiPregnanciesAndKnownHiv", nullToEmpty(transfer.getMultiPregnanciesAndKnownHiv()));
		preview.put("currentPregnancyComplications", nullToEmpty(transfer.getCurrentPregnancyComplications()));

		preview.put("latestHemoglobin", nullToEmpty(transfer.getLatestHemoglobin()));
		preview.put("latestHivStatus", nullToEmpty(transfer.getLatestHivStatus()));
		preview.put("latestBloodGroup", nullToEmpty(transfer.getLatestBloodGroup()));
		preview.put("latestOtherResults", nullToEmpty(transfer.getLatestOtherResults()));
		preview.put("vitalBp", nullToEmpty(transfer.getVitalBp()));
		preview.put("vitalTemp", nullToEmpty(transfer.getVitalTemp()));
		preview.put("vitalSpo2", nullToEmpty(transfer.getVitalSpo2()));
		preview.put("vitalRr", nullToEmpty(transfer.getVitalRr()));
		preview.put("vitalPulse", nullToEmpty(transfer.getVitalPulse()));
		preview.put("vitalWeight", nullToEmpty(transfer.getVitalWeight()));
		preview.put("vitalHeight", nullToEmpty(transfer.getVitalHeight()));
		preview.put("fetalPresentation", nullToEmpty(transfer.getFetalPresentation()));
		preview.put("fundalHeight", nullToEmpty(transfer.getFundalHeight()));
		preview.put("fetalHeartRate", nullToEmpty(transfer.getFetalHeartRate()));
		preview.put("contractions", nullToEmpty(transfer.getContractions()));
		preview.put("vaginalExamAt", formatDateTime(transfer.getVaginalExamAt()));
		preview.put("dilation", nullToEmpty(transfer.getDilation()));
		preview.put("effacement", nullToEmpty(transfer.getEffacement()));
		preview.put("descent", nullToEmpty(transfer.getDescent()));
		preview.put("consistency", nullToEmpty(transfer.getConsistency()));
		preview.put("position", nullToEmpty(transfer.getPosition()));
		preview.put("caput", Boolean.TRUE.equals(transfer.getCaput()));
		preview.put("moulding", Boolean.TRUE.equals(transfer.getMoulding()));
		preview.put("membranesRuptured", Boolean.TRUE.equals(transfer.getMembranesRuptured()));
		preview.put("membranesRupturedAt", formatDateTime(transfer.getMembranesRupturedAt()));
		preview.put("amnioticFluidColor", nullToEmpty(transfer.getAmnioticFluidColor()));
		preview.put("estimatedBloodLossMl", nullToEmpty(transfer.getEstimatedBloodLossMl()));
		preview.put("investigationHgb", nullToEmpty(transfer.getInvestigationHgb()));
		preview.put("investigationUrineTest", nullToEmpty(transfer.getInvestigationUrineTest()));
		preview.put("investigationOtherTest", nullToEmpty(transfer.getInvestigationOtherTest()));
		preview.put("imagingInvestigations", nullToEmpty(transfer.getImagingInvestigations()));
		preview.put("diagnosis", nullToEmpty(transfer.getDiagnosis()));
		preview.put("procedures", nullToEmpty(transfer.getProcedures()));
		preview.put("attachedLabTests", Boolean.TRUE.equals(transfer.getAttachedLabTests()));
		preview.put("attachedImaging", Boolean.TRUE.equals(transfer.getAttachedImaging()));
		preview.put("attachedOther", nullToEmpty(transfer.getAttachedOther()));

		String transportType = nullToEmpty(transfer.getTransportType());
		preview.put("transportationType", transportType);
		preview.put("isAmbulanceTransport", "AMBULANCE".equals(transportType));
		preview.put("transportationOtherSpec", nullToEmpty(transfer.getTransportOther()));
		preview.put("isNaTransport", "NA".equals(transportType));

		List<Map<String, Object>> treatmentRows = new ArrayList<Map<String, Object>>();
		if (transfer.getTreatments() != null) {
			for (MaternityTransferTreatment treatment : transfer.getTreatments()) {
				Map<String, Object> row = new HashMap<String, Object>();
				row.put("treatmentName", nullToEmpty(treatment.getTreatmentName()));
				row.put("dose", nullToEmpty(treatment.getDose()));
				row.put("givenDate", formatDateOnly(treatment.getGivenDate()));
				row.put("givenTime", nullToEmpty(treatment.getGivenTime()));
				treatmentRows.add(row);
			}
		}
		preview.put("treatments", treatmentRows);

		String healthInsuranceType = nullToEmpty(transfer.getHealthInsuranceType());
		preview.put("healthInsuranceType", healthInsuranceType);
		preview.put("isCbhiInsurance", "CBHI".equals(healthInsuranceType));
		preview.put("isRssbInsurance", "RSSB".equals(healthInsuranceType));
		preview.put("isMmiInsurance", "MMI".equals(healthInsuranceType));
		preview.put("healthInsuranceOtherSpec", nullToEmpty(transfer.getHealthInsuranceOther()));
		preview.put("isNoInsurance", "NONE".equals(healthInsuranceType));

		preview.put("referringProviderName", nullToEmpty(transfer.getReferringProviderName()));
		preview.put("referringProviderQualification", nullToEmpty(transfer.getReferringProviderQualification()));
		preview.put("referringSignedDate", formatDateOnly(transfer.getReferringSignedDate()));
		preview.put("referringSignedTime", nullToEmpty(transfer.getReferringSignedTime()));
		preview.put("referringProviderPhone", nullToEmpty(transfer.getReferringProviderPhone()));
		preview.put("dateCreated", formatDateTime(transfer.getDateCreated()));

		return preview;
	}

	private Map<String, Object> toNeonatalPreviewMap(NeonatalTransfer transfer) {
		Map<String, Object> preview = new HashMap<String, Object>();
		preview.put("uuid", transfer.getUuid());
		preview.put("formType", FORM_TYPE_NEONATAL);
		preview.put("hieSent", transfer.isSentToHie());
		preview.put("hieSentAt", formatDateTime(transfer.getHieSentAt()));
		preview.put("hieTransferId", nullToEmpty(transfer.getHieTransferId()));

		preview.put("province", nullToEmpty(transfer.getProvince()));
		preview.put("district", nullToEmpty(transfer.getDistrict()));
		preview.put("hospitalName", nullToEmpty(transfer.getHospitalName()));
		preview.put("referringFacilityName", nullToEmpty(transfer.getReferringFacilityName()));
		preview.put("referringUnit", nullToEmpty(transfer.getReferringUnit()));

		preview.put("babyName", nullToEmpty(transfer.getBabyName()));
		preview.put("sex", nullToEmpty(transfer.getSex()));
		preview.put("dob", formatDateOnly(transfer.getDob()));
		preview.put("gestationalAgeWeeks", nullToEmpty(transfer.getGestationalAgeWeeks()));
		preview.put("birthWeightG", nullToEmpty(transfer.getBirthWeightG()));
		preview.put("currentWeightG", nullToEmpty(transfer.getCurrentWeightG()));
		preview.put("currentAgeDays", nullToEmpty(transfer.getCurrentAgeDays()));
		preview.put("motherName", nullToEmpty(transfer.getMotherName()));
		preview.put("motherAge", nullToEmpty(transfer.getMotherAge()));
		preview.put("motherCaregiverPhone", nullToEmpty(transfer.getMotherCaregiverPhone()));
		preview.put("placeOfBirth", nullToEmpty(transfer.getPlaceOfBirth()));
		preview.put("reasonForTransfer", nullToEmpty(transfer.getReasonForTransfer()));
		preview.put("modeOfTransport", nullToEmpty(transfer.getModeOfTransport()));
		preview.put("transportOther", nullToEmpty(transfer.getTransportOther()));

		String transferType = nullToEmpty(transfer.getTransferType());
		preview.put("transferType", transferType);
		preview.put("isEmergency", "EMERGENCY".equals(transferType));
		preview.put("isNonEmergency", "NOT_EMERGENCY".equals(transferType));
		preview.put("isFollowUp", "FOLLOW_UP".equals(transferType));

		preview.put("receivingFacility", resolveFacilityLabel(transfer.getReceivingFacilityCode()));
		preview.put("receivingFacilityCode", nullToEmpty(transfer.getReceivingFacilityCode()));
		preview.put("receivingService", nullToEmpty(transfer.getReceivingService()));
		preview.put("callingTime", nullToEmpty(transfer.getCallingTime()));
		preview.put("staffContactedName", nullToEmpty(transfer.getStaffContactedName()));
		preview.put("staffContactedPhone", nullToEmpty(transfer.getStaffContactedPhone()));
		preview.put("decisionToTransferAt", formatDateTime(transfer.getDecisionToTransferAt()));

		preview.put("motherAlive", nullToEmpty(transfer.getMotherAlive()));
		preview.put("obstetricGravida", nullToEmpty(transfer.getObstetricGravida()));
		preview.put("obstetricParity", nullToEmpty(transfer.getObstetricParity()));
		preview.put("pregnancyType", nullToEmpty(transfer.getPregnancyType()));
		preview.put("ancScreening", nullToEmpty(transfer.getAncScreening()));
		preview.put("pathologiesDuringPregnancy", nullToEmpty(transfer.getPathologiesDuringPregnancy()));
		preview.put("pregnancyOtherPathologies", nullToEmpty(transfer.getPregnancyOtherPathologies()));
		preview.put("pregnancyTreatment", nullToEmpty(transfer.getPregnancyTreatment()));
		preview.put("bloodGroup", nullToEmpty(transfer.getBloodGroup()));
		preview.put("rhFactor", nullToEmpty(transfer.getRhFactor()));
		preview.put("hivStatus", nullToEmpty(transfer.getHivStatus()));
		preview.put("hivRegimen", nullToEmpty(transfer.getHivRegimen()));
		preview.put("hivRecentVl", nullToEmpty(transfer.getHivRecentVl()));
		preview.put("hivCd4Count", nullToEmpty(transfer.getHivCd4Count()));
		preview.put("hivOpportunisticInfections", nullToEmpty(transfer.getHivOpportunisticInfections()));
		preview.put("tetanusVaccineDoses", nullToEmpty(transfer.getTetanusVaccineDoses()));
		preview.put("maternalIllicitDrugHistory", nullToEmpty(transfer.getMaternalIllicitDrugHistory()));

		preview.put("romAt", formatDateTime(transfer.getRomAt()));
		preview.put("afQuality", nullToEmpty(transfer.getAfQuality()));
		preview.put("afQuantity", nullToEmpty(transfer.getAfQuantity()));
		preview.put("feverTiming", nullToEmpty(transfer.getFeverTiming()));
		preview.put("steroidDoses", nullToEmpty(transfer.getSteroidDoses()));
		preview.put("lastSteroidDoseAt", formatDateTime(transfer.getLastSteroidDoseAt()));
		preview.put("mgso4At", formatDateTime(transfer.getMgso4At()));
		preview.put("modeOfDelivery", nullToEmpty(transfer.getModeOfDelivery()));
		preview.put("laborComplications", nullToEmpty(transfer.getLaborComplications()));
		preview.put("laborComplicationsOther", nullToEmpty(transfer.getLaborComplicationsOther()));
		preview.put("maternalAnesthesia", nullToEmpty(transfer.getMaternalAnesthesia()));
		preview.put("maternalAnesthesiaOther", nullToEmpty(transfer.getMaternalAnesthesiaOther()));
		preview.put("maternalAntibiotics", nullToEmpty(transfer.getMaternalAntibiotics()));
		preview.put("otherDrugs", nullToEmpty(transfer.getOtherDrugs()));
		preview.put("sepsisRiskFactors", nullToEmpty(transfer.getSepsisRiskFactors()));

		preview.put("resuscitationAtBirth", nullToEmpty(transfer.getResuscitationAtBirth()));
		preview.put("resuscitationMethods", nullToEmpty(transfer.getResuscitationMethods()));
		preview.put("apgar1min", nullToEmpty(transfer.getApgar1min()));
		preview.put("apgar5min", nullToEmpty(transfer.getApgar5min()));
		preview.put("apgar10min", nullToEmpty(transfer.getApgar10min()));
		preview.put("hie", nullToEmpty(transfer.getHie()));
		preview.put("hieGrade", nullToEmpty(transfer.getHieGrade()));
		preview.put("allergies", nullToEmpty(transfer.getAllergies()));
		preview.put("immunization", nullToEmpty(transfer.getImmunization()));
		preview.put("immunizationDetails", nullToEmpty(transfer.getImmunizationDetails()));
		preview.put("vitaminK", nullToEmpty(transfer.getVitaminK()));
		preview.put("tetracyclineEyeOintment", nullToEmpty(transfer.getTetracyclineEyeOintment()));
		preview.put("surfactant", nullToEmpty(transfer.getSurfactant()));

		preview.put("chiefComplaintDetails", nullToEmpty(transfer.getChiefComplaintDetails()));
		preview.put("spo2Preductal", nullToEmpty(transfer.getSpo2Preductal()));
		preview.put("spo2Postductal", nullToEmpty(transfer.getSpo2Postductal()));
		preview.put("conditionTemp", nullToEmpty(transfer.getConditionTemp()));
		preview.put("conditionHr", nullToEmpty(transfer.getConditionHr()));
		preview.put("conditionRr", nullToEmpty(transfer.getConditionRr()));
		preview.put("conditionBp", nullToEmpty(transfer.getConditionBp()));
		preview.put("neurologicalStatus", nullToEmpty(transfer.getNeurologicalStatus()));
		preview.put("seizures", Boolean.TRUE.equals(transfer.getSeizures()));
		preview.put("adverseEvents24h", nullToEmpty(transfer.getAdverseEvents24h()));
		preview.put("diagnosis1", nullToEmpty(transfer.getDiagnosis1()));
		preview.put("diagnosis2", nullToEmpty(transfer.getDiagnosis2()));
		preview.put("diagnosis3", nullToEmpty(transfer.getDiagnosis3()));
		preview.put("diagnosis4", nullToEmpty(transfer.getDiagnosis4()));

		preview.put("respiratorySupport", nullToEmpty(transfer.getRespiratorySupport()));
		preview.put("bloodGasAnalysis", nullToEmpty(transfer.getBloodGasAnalysis()));
		preview.put("ventilationSettings", nullToEmpty(transfer.getVentilationSettings()));
		preview.put("ivFluidVol", nullToEmpty(transfer.getIvFluidVol()));
		preview.put("passedUrine", nullToEmpty(transfer.getPassedUrine()));
		preview.put("inotropes", nullToEmpty(transfer.getInotropes()));
		preview.put("inotropesSpecify", nullToEmpty(transfer.getInotropesSpecify()));
		preview.put("peripheralIv", nullToEmpty(transfer.getPeripheralIv()));
		preview.put("centralIv", nullToEmpty(transfer.getCentralIv()));
		preview.put("intraosseousLine", nullToEmpty(transfer.getIntraosseousLine()));
		preview.put("antibiotic1Name", nullToEmpty(transfer.getAntibiotic1Name()));
		preview.put("antibiotic1Doses", nullToEmpty(transfer.getAntibiotic1Doses()));
		preview.put("antibiotic1Durations", nullToEmpty(transfer.getAntibiotic1Durations()));
		preview.put("antibiotic2Name", nullToEmpty(transfer.getAntibiotic2Name()));
		preview.put("antibiotic2Doses", nullToEmpty(transfer.getAntibiotic2Doses()));
		preview.put("antibiotic2Durations", nullToEmpty(transfer.getAntibiotic2Durations()));
		preview.put("arvs", nullToEmpty(transfer.getArvs()));
		preview.put("npo", nullToEmpty(transfer.getNpo()));
		preview.put("lastFeedTime", nullToEmpty(transfer.getLastFeedTime()));
		preview.put("lastFeedAmount", nullToEmpty(transfer.getLastFeedAmount()));
		preview.put("feedVol", nullToEmpty(transfer.getFeedVol()));
		preview.put("feedType", nullToEmpty(transfer.getFeedType()));
		preview.put("passedStool", nullToEmpty(transfer.getPassedStool()));
		preview.put("nasogastricTube", nullToEmpty(transfer.getNasogastricTube()));
		preview.put("labGlucose", nullToEmpty(transfer.getLabGlucose()));
		preview.put("fbcDone", nullToEmpty(transfer.getFbcDone()));
		preview.put("labHb", nullToEmpty(transfer.getLabHb()));
		preview.put("labWbc", nullToEmpty(transfer.getLabWbc()));
		preview.put("labPlatelets", nullToEmpty(transfer.getLabPlatelets()));
		preview.put("labCrp", nullToEmpty(transfer.getLabCrp()));
		preview.put("labBiliTotal", nullToEmpty(transfer.getLabBiliTotal()));
		preview.put("labBiliDirect", nullToEmpty(transfer.getLabBiliDirect()));
		preview.put("labUe", nullToEmpty(transfer.getLabUe()));
		preview.put("labCultures", nullToEmpty(transfer.getLabCultures()));
		preview.put("imagingResultsAvailable", nullToEmpty(transfer.getImagingResultsAvailable()));
		preview.put("imagingResults", nullToEmpty(transfer.getImagingResults()));
		preview.put("painSedationDrugs", nullToEmpty(transfer.getPainSedationDrugs()));
		preview.put("imagingReportAttached", Boolean.TRUE.equals(transfer.getImagingReportAttached()));
		preview.put("labReportsAttached", Boolean.TRUE.equals(transfer.getLabReportsAttached()));

		preview.put("clinicalManagementSummary", nullToEmpty(transfer.getClinicalManagementSummary()));
		preview.put("referringProviderName", nullToEmpty(transfer.getReferringProviderName()));
		preview.put("referringProviderQualification", nullToEmpty(transfer.getReferringProviderQualification()));
		preview.put("referringSignedDate", formatDateOnly(transfer.getReferringSignedDate()));
		preview.put("referringSignedTime", nullToEmpty(transfer.getReferringSignedTime()));
		preview.put("referringProviderPhone", nullToEmpty(transfer.getReferringProviderPhone()));
		preview.put("dateCreated", formatDateTime(transfer.getDateCreated()));

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
