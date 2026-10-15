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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.map.ObjectMapper;
import org.openmrs.User;
import org.openmrs.api.context.Context;
import org.openmrs.module.transferapp.api.ProviderProfileAttributes;
import org.openmrs.module.transferapp.api.TransferProfileService;
import org.openmrs.module.transferapp.hie.HieConnectionResolver;
import org.openmrs.module.transferapp.hie.HieProviderRegistryClient;
import org.openmrs.module.transferapp.hie.ProviderLicenseOtpVerifyResult;
import org.openmrs.module.transferapp.hie.ProviderLicenseValidateResult;
import org.openmrs.module.transferapp.model.TransferProfile;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

@Controller
public class TransferProfileController {

	private static final Log log = LogFactory.getLog(TransferProfileController.class);

	private TransferProfileService getTransferProfileService() {
		return Context.getService(TransferProfileService.class);
	}

	@RequestMapping(value = "/module/transferapp/profile/save.form", method = RequestMethod.POST)
	public void saveProfile(HttpServletResponse response,
			@RequestParam("licenseNumber") String licenseNumber,
			@RequestParam("phoneNumber") String phoneNumber,
			@RequestParam("qualification") String qualification,
			@RequestParam(value = "speciality", required = false) String speciality) throws Exception {

		Map<String, Object> data = new HashMap<String, Object>();
		try {
			User user = Context.getAuthenticatedUser();
			String license = StringUtils.trimToNull(licenseNumber);
			if (license == null) {
				data.put("status", "error");
				data.put("message", "License number is required");
				writeJson(response, data);
				return;
			}
			if (!ProviderProfileAttributes.isLicenseValidatedForUser(user, license)) {
				data.put("status", "error");
				data.put("message", "License number must be validated before the profile can be saved");
				writeJson(response, data);
				return;
			}

			TransferProfile profile = getTransferProfileService().saveProfileForCurrentUser(
					license, phoneNumber, qualification, speciality);
			data.put("status", "success");
			data.put("licenseNumber", profile.getLicenseNumber());
			data.put("phoneNumber", profile.getPhoneNumber());
			data.put("qualification", profile.getQualification());
			data.put("speciality", profile.getSpeciality());
			data.put("qualificationDisplay", profile.getQualificationWithSpeciality());
		}
		catch (Exception e) {
			log.error("Unable to save transfer profile", e);
			data.put("status", "error");
			data.put("message", resolveErrorMessage(e, "Unable to save profile"));
		}
		writeJson(response, data);
	}

	/**
	 * Calls Provider Registry {@code POST .../pr/licenses/validate}. On success the UI opens the OTP dialog.
	 */
	@RequestMapping(value = "/module/transferapp/profile/license/startValidation.form", method = RequestMethod.POST)
	public void startLicenseValidation(HttpServletResponse response,
			@RequestParam("licenseNumber") String licenseNumber) throws Exception {

		Map<String, Object> data = new HashMap<String, Object>();
		try {
			String license = StringUtils.trimToNull(licenseNumber);
			if (license == null) {
				data.put("status", "error");
				data.put("message", "License number is required");
				writeJson(response, data);
				return;
			}

			HieConnectionResolver connectionResolver = new HieConnectionResolver();
			if (!connectionResolver.isHieConfigured()) {
				data.put("status", "error");
				data.put("message", "HIE is not configured. Set HIE URL and credentials before validating a license.");
				writeJson(response, data);
				return;
			}

			ProviderLicenseValidateResult result = new HieProviderRegistryClient()
					.validateLicense(connectionResolver.resolveConnection(), license);
			data.put("licenseNumber", license);
			data.put("maskedPhone", StringUtils.defaultString(result.getMaskedPhone()));
			data.put("httpStatus", result.getStatusCode());
			if (result.isSuccess()) {
				data.put("status", "success");
				data.put("needsOtp", true);
				data.put("message", buildOtpSentMessage(result.getMaskedPhone(), result.getMessage()));
			}
			else {
				data.put("status", "error");
				data.put("needsOtp", false);
				data.put("message", result.getMessage());
			}
		}
		catch (Exception e) {
			log.error("Unable to start license validation", e);
			data.put("status", "error");
			data.put("needsOtp", false);
			data.put("message", resolveErrorMessage(e, "Unable to start license validation"));
		}
		writeJson(response, data);
	}

	/**
	 * Calls Provider Registry {@code POST .../pr/licenses/validate-otp} and persists returned fields.
	 */
	@RequestMapping(value = "/module/transferapp/profile/license/verifyOtp.form", method = RequestMethod.POST)
	public void verifyLicenseOtp(HttpServletResponse response,
			@RequestParam("licenseNumber") String licenseNumber,
			@RequestParam("otp") String otp) throws Exception {

		Map<String, Object> data = new HashMap<String, Object>();
		try {
			String license = StringUtils.trimToNull(licenseNumber);
			String otpCode = StringUtils.trimToNull(otp);
			if (license == null) {
				data.put("status", "error");
				data.put("message", "License number is required");
				writeJson(response, data);
				return;
			}
			if (otpCode == null) {
				data.put("status", "error");
				data.put("message", "OTP is required");
				writeJson(response, data);
				return;
			}

			HieConnectionResolver connectionResolver = new HieConnectionResolver();
			if (!connectionResolver.isHieConfigured()) {
				data.put("status", "error");
				data.put("message", "HIE is not configured. Set HIE URL and credentials before verifying a license.");
				writeJson(response, data);
				return;
			}

			ProviderLicenseOtpVerifyResult result = new HieProviderRegistryClient()
					.validateLicenseOtp(connectionResolver.resolveConnection(), license, otpCode);
			data.put("message", result.getMessage());
			data.put("httpStatus", result.getStatusCode());
			data.put("licenseNumber", StringUtils.defaultString(result.getLicenseNumber(), license));
			data.put("licenseExpiryDate", StringUtils.defaultString(result.getLicenseExpiryDate()));
			data.put("specialty", StringUtils.defaultString(result.getSpecialty()));
			data.put("speciality", StringUtils.defaultString(result.getSpecialty()));
			data.put("qualification", StringUtils.defaultString(result.getQualification()));
			data.put("providerActive", result.getProviderActive());
			String licenseStatus = result.resolveLicenseStatus();
			data.put("licenseStatus", StringUtils.defaultString(licenseStatus));

			boolean activeLicense = result.isSuccess() && !Boolean.FALSE.equals(result.getProviderActive());
			if (!activeLicense) {
				data.put("status", "error");
				data.put("licenseValidated", false);
				if (result.isSuccess() && Boolean.FALSE.equals(result.getProviderActive())) {
					data.put("message", StringUtils.isNotBlank(result.getMessage())
							? result.getMessage()
							: "License is not active and cannot be saved");
				}
				writeJson(response, data);
				return;
			}

			User user = Context.getAuthenticatedUser();
			String verifiedLicense = StringUtils.isNotBlank(result.getLicenseNumber())
					? result.getLicenseNumber()
					: license;
			ProviderProfileAttributes.applyOtpVerificationResult(user, verifiedLicense,
					result.getLicenseExpiryDate(), licenseStatus, result.getQualification(), result.getSpecialty());

			data.put("status", "success");
			data.put("licenseValidated", true);
		}
		catch (Exception e) {
			log.error("Unable to verify license OTP", e);
			data.put("status", "error");
			data.put("licenseValidated", false);
			data.put("message", resolveErrorMessage(e, "Unable to verify OTP"));
		}
		writeJson(response, data);
	}

	/**
	 * UI copy for OTP dialog: {@code License verified. OTP sent to 078****270}.
	 */
	private static String buildOtpSentMessage(String maskedPhone, String fallbackMessage) {
		String displayPhone = formatMaskedPhoneForDisplay(maskedPhone);
		if (StringUtils.isNotBlank(displayPhone)) {
			return "License verified. OTP sent to " + displayPhone;
		}
		return StringUtils.isNotBlank(fallbackMessage)
				? fallbackMessage
				: "License verified. OTP generated successfully";
	}

	private static String formatMaskedPhoneForDisplay(String maskedPhone) {
		String phone = StringUtils.trimToNull(maskedPhone);
		if (phone == null) {
			return null;
		}
		while (phone.startsWith("+")) {
			phone = phone.substring(1);
		}
		return StringUtils.trimToNull(phone);
	}

	private String resolveErrorMessage(Exception exception, String fallback) {
		Throwable current = exception;
		while (current != null) {
			if (current.getMessage() != null && current.getMessage().trim().length() > 0) {
				return current.getMessage();
			}
			current = current.getCause();
		}
		return fallback;
	}

	private void writeJson(HttpServletResponse response, Map<String, Object> data) throws Exception {
		response.setContentType("application/json");
		new ObjectMapper().writeValue(response.getOutputStream(), data);
	}

}
