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
package org.openmrs.module.transferapp.hie;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.openmrs.module.transferapp.TransferAppConstants;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Calls HIE Provider Registry license APIs under {@code /provider-registry/api/v1/pr/...}.
 */
public class HieProviderRegistryClient {

	private static final Log log = LogFactory.getLog(HieProviderRegistryClient.class);

	private final ObjectMapper objectMapper = new ObjectMapper();

	/**
	 * {@code POST /provider-registry/api/v1/pr/licenses/validate} with
	 * {@code {"licenseNumber":"..."}}.
	 */
	public ProviderLicenseValidateResult validateLicense(HieBasicConnection connection, String licenseNumber) {
		if (connection == null) {
			throw new HieConfigurationException("HIE connection is not configured");
		}
		String license = StringUtils.trimToNull(licenseNumber);
		if (license == null) {
			throw new HieApiException("License number is required");
		}

		Map<String, String> payload = new LinkedHashMap<String, String>();
		payload.put("licenseNumber", license);
		HttpJsonResponse response = postJson(connection,
				TransferAppConstants.HIE_PROVIDER_REGISTRY_LICENSE_VALIDATE_PATH, payload);
		return parseValidateResponse(response.statusCode, response.body);
	}

	/**
	 * {@code POST /provider-registry/api/v1/pr/licenses/validate-otp} with
	 * {@code {"licenseNumber":"...","otp":"..."}}.
	 */
	public ProviderLicenseOtpVerifyResult validateLicenseOtp(HieBasicConnection connection, String licenseNumber,
			String otp) {
		if (connection == null) {
			throw new HieConfigurationException("HIE connection is not configured");
		}
		String license = StringUtils.trimToNull(licenseNumber);
		String otpCode = StringUtils.trimToNull(otp);
		if (license == null) {
			throw new HieApiException("License number is required");
		}
		if (otpCode == null) {
			throw new HieApiException("OTP is required");
		}

		Map<String, String> payload = new LinkedHashMap<String, String>();
		payload.put("licenseNumber", license);
		payload.put("otp", otpCode);
		HttpJsonResponse response = postJson(connection,
				TransferAppConstants.HIE_PROVIDER_REGISTRY_LICENSE_VALIDATE_OTP_PATH, payload);
		return parseOtpVerifyResponse(response.statusCode, response.body, license);
	}

	private HttpJsonResponse postJson(HieBasicConnection connection, String path, Map<String, String> payload) {
		String url = connection.getBaseUrl() + path;
		HttpURLConnection httpConnection = null;
		try {
			String requestBody = objectMapper.writeValueAsString(payload);
			httpConnection = (HttpURLConnection) new URL(url).openConnection();
			httpConnection.setRequestMethod("POST");
			httpConnection.setConnectTimeout(30000);
			httpConnection.setReadTimeout(60000);
			httpConnection.setDoOutput(true);
			httpConnection.setRequestProperty("Accept", "application/json");
			httpConnection.setRequestProperty("Content-Type", "application/json");

			String credentials = connection.getUsername() + ":" + connection.getPassword();
			String encoded = Base64.getEncoder().encodeToString(credentials.getBytes("UTF-8"));
			httpConnection.setRequestProperty("Authorization", "Basic " + encoded);

			OutputStream outputStream = httpConnection.getOutputStream();
			outputStream.write(requestBody.getBytes("UTF-8"));
			outputStream.flush();
			outputStream.close();

			int statusCode = httpConnection.getResponseCode();
			String responseBody = readStream(
					statusCode >= 400 ? httpConnection.getErrorStream() : httpConnection.getInputStream());
			if (responseBody == null) {
				responseBody = "";
			}
			return new HttpJsonResponse(statusCode, responseBody);
		}
		catch (HieApiException ex) {
			throw ex;
		}
		catch (HieConfigurationException ex) {
			throw ex;
		}
		catch (Exception ex) {
			log.error("Provider Registry request failed for " + url, ex);
			throw new HieApiException(
					"Could not reach Provider Registry POST " + url + ": " + ex.getMessage(), ex);
		}
		finally {
			if (httpConnection != null) {
				httpConnection.disconnect();
			}
		}
	}

	private ProviderLicenseValidateResult parseValidateResponse(int statusCode, String responseBody) {
		boolean success = false;
		String message = null;
		String maskedPhone = null;
		JsonNode root = readJsonTree(responseBody);
		if (root != null) {
			success = readBoolean(root, "success", false);
			message = firstText(root, "message", "error", "errorMessage", "detail", "title");
			maskedPhone = firstText(root, "maskedPhone", "masked_phone", "phone");
		}

		if (statusCode >= 200 && statusCode < 300 && success) {
			if (StringUtils.isBlank(message)) {
				message = "License verified. OTP generated successfully";
			}
			return new ProviderLicenseValidateResult(statusCode, true, message, maskedPhone, responseBody);
		}
		return new ProviderLicenseValidateResult(statusCode, false,
				fallbackErrorMessage(message, responseBody, statusCode, "License validation failed"),
				maskedPhone, responseBody);
	}

	private ProviderLicenseOtpVerifyResult parseOtpVerifyResponse(int statusCode, String responseBody,
			String requestedLicense) {
		boolean success = false;
		String message = null;
		String licenseNumber = requestedLicense;
		String licenseExpiryDate = null;
		String specialty = null;
		String qualification = null;
		Boolean providerActive = null;

		JsonNode root = readJsonTree(responseBody);
		if (root != null) {
			success = readBoolean(root, "success", false);
			message = firstText(root, "message", "error", "errorMessage", "detail", "title");
			String responseLicense = firstText(root, "licenseNumber");
			if (responseLicense != null) {
				licenseNumber = responseLicense;
			}
			licenseExpiryDate = firstText(root, "licenseExpiryDate", "expiryDate", "licenseExpiry");
			specialty = firstText(root, "specialty", "speciality", "specialityName");
			qualification = firstText(root, "qualification");
			providerActive = readBooleanObject(root, "providerActive");
		}

		if (statusCode >= 200 && statusCode < 300 && success) {
			if (StringUtils.isBlank(message)) {
				message = "License verified successfully";
			}
			return new ProviderLicenseOtpVerifyResult(statusCode, true, message, licenseNumber, licenseExpiryDate,
					specialty, qualification, providerActive, responseBody);
		}
		return new ProviderLicenseOtpVerifyResult(statusCode, false,
				fallbackErrorMessage(message, responseBody, statusCode, "OTP verification failed"),
				licenseNumber, licenseExpiryDate, specialty, qualification, providerActive, responseBody);
	}

	private JsonNode readJsonTree(String responseBody) {
		if (StringUtils.isBlank(responseBody)) {
			return null;
		}
		try {
			JsonNode root = objectMapper.readTree(responseBody);
			return root != null && !root.isNull() ? root : null;
		}
		catch (Exception parseEx) {
			log.warn("Unable to parse Provider Registry response as JSON: " + responseBody, parseEx);
			return null;
		}
	}

	private static boolean readBoolean(JsonNode root, String fieldName, boolean defaultValue) {
		Boolean value = readBooleanObject(root, fieldName);
		return value != null ? value.booleanValue() : defaultValue;
	}

	private static Boolean readBooleanObject(JsonNode root, String fieldName) {
		if (root == null || StringUtils.isBlank(fieldName)) {
			return null;
		}
		JsonNode node = root.get(fieldName);
		if (node == null || node.isNull()) {
			return null;
		}
		if (node.isBoolean()) {
			return Boolean.valueOf(node.getBooleanValue());
		}
		String text = StringUtils.trimToNull(node.asText());
		if (text == null) {
			return null;
		}
		if ("true".equalsIgnoreCase(text) || "1".equals(text) || "yes".equalsIgnoreCase(text)) {
			return Boolean.TRUE;
		}
		if ("false".equalsIgnoreCase(text) || "0".equals(text) || "no".equalsIgnoreCase(text)) {
			return Boolean.FALSE;
		}
		return null;
	}

	private static String firstText(JsonNode root, String... fieldNames) {
		if (root == null || fieldNames == null) {
			return null;
		}
		for (String fieldName : fieldNames) {
			JsonNode node = root.get(fieldName);
			if (node != null && !node.isNull()) {
				String text = StringUtils.trimToNull(node.asText());
				if (text != null) {
					return text;
				}
			}
		}
		return null;
	}

	private static String fallbackErrorMessage(String message, String responseBody, int statusCode, String prefix) {
		if (StringUtils.isNotBlank(message)) {
			return message;
		}
		if (StringUtils.isNotBlank(responseBody)) {
			String trimmed = responseBody.trim();
			if (trimmed.length() > 400) {
				return trimmed.substring(0, 400) + "...";
			}
			return trimmed;
		}
		return prefix + " (HTTP " + statusCode + ")";
	}

	private static String readStream(InputStream inputStream) throws Exception {
		if (inputStream == null) {
			return "";
		}
		BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
		StringBuilder builder = new StringBuilder();
		String line;
		while ((line = reader.readLine()) != null) {
			if (builder.length() > 0) {
				builder.append('\n');
			}
			builder.append(line);
		}
		reader.close();
		return builder.toString();
	}

	private static final class HttpJsonResponse {
		private final int statusCode;
		private final String body;

		private HttpJsonResponse(int statusCode, String body) {
			this.statusCode = statusCode;
			this.body = body;
		}
	}

}
