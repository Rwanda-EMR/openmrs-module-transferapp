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
import org.openmrs.api.AdministrationService;
import org.openmrs.api.context.Context;
import org.openmrs.module.transferapp.TransferAppConstants;
import org.openmrs.module.transferapp.api.TransferVerificationUrlService;
import org.openmrs.module.transferapp.model.Transfer;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;
import java.util.regex.Pattern;

public class TransferVerificationUrlServiceImpl implements TransferVerificationUrlService {

	private static final Pattern UUID_PATTERN = Pattern.compile(
			"^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

	private static final String VERIFY_QR_FORM_PATH = "/module/transferapp/transfer/verifyQr.form";

	@Override
	public String resolveVerificationTransferId(Transfer transfer) {
		if (transfer == null) {
			return null;
		}
		if (StringUtils.isNotBlank(transfer.getHieTransferId())) {
			return transfer.getHieTransferId().trim();
		}
		return StringUtils.trimToNull(transfer.getUuid());
	}

	@Override
	public String buildRemoteVerifyUrl(Transfer transfer) {
		return buildRemoteVerifyUrlForTransferId(resolveVerificationTransferId(transfer));
	}

	@Override
	public boolean shouldShowVerificationQr(Transfer transfer) {
		if (transfer == null) {
			return false;
		}
		return isValidVerificationTransferId(resolveVerificationTransferId(transfer));
	}

	@Override
	public boolean isValidVerificationTransferId(String transferId) {
		if (StringUtils.isBlank(transferId)) {
			return false;
		}
		String normalized = transferId.trim();
		if (normalized.startsWith("{") && normalized.endsWith("}") && normalized.length() > 2) {
			normalized = normalized.substring(1, normalized.length() - 1).trim();
		}
		return UUID_PATTERN.matcher(normalized).matches();
	}

	@Override
	public String buildRemoteVerifyUrlForTransferId(String transferId) {
		if (!isValidVerificationTransferId(transferId)) {
			return null;
		}
		return resolveVerifyBaseUrl() + "/verify/transfer/" + transferId.trim() + "/remote";
	}

	@Override
	public String buildVerifyQrFormUrl(String transferId) {
		if (!isValidVerificationTransferId(transferId)) {
			return null;
		}
		try {
			return VERIFY_QR_FORM_PATH + "?transferId=" + URLEncoder.encode(transferId.trim(), "UTF-8");
		}
		catch (UnsupportedEncodingException ex) {
			return VERIFY_QR_FORM_PATH + "?transferId=" + transferId.trim();
		}
	}

	@Override
	public void enrichPreviewVerificationFields(Map<String, Object> preview) {
		if (preview == null) {
			return;
		}

		String transferId = resolveVerificationTransferIdFromPreview(preview);
		if (!isValidVerificationTransferId(transferId)) {
			preview.put("showVerificationQr", Boolean.FALSE);
			return;
		}

		preview.put("showVerificationQr", Boolean.TRUE);
		preview.put("verificationTransferId", transferId);
		preview.put("verifyRemoteUrl", buildRemoteVerifyUrlForTransferId(transferId));

		Object existingQrUrl = preview.get("verifyQrUrl");
		if (existingQrUrl == null || StringUtils.isBlank(String.valueOf(existingQrUrl))) {
			preview.put("verifyQrUrl", buildVerifyQrFormUrl(transferId));
		}
	}

	private String resolveVerificationTransferIdFromPreview(Map<String, Object> preview) {
		String[] keys = new String[] { "verificationTransferId", "hieTransferId", "uuid", "id" };
		for (String key : keys) {
			Object value = preview.get(key);
			if (value == null) {
				continue;
			}
			String candidate = String.valueOf(value).trim();
			if (isValidVerificationTransferId(candidate)) {
				return candidate;
			}
		}
		return null;
	}

	private String resolveVerifyBaseUrl() {
		AdministrationService adminService = Context.getAdministrationService();
		String baseUrl = adminService.getGlobalProperty(
				TransferAppConstants.GP_VERIFY_BASE_URL,
				TransferAppConstants.DEFAULT_VERIFY_BASE_URL);
		return normalizeVerifyBaseUrl(baseUrl);
	}

	static String normalizeVerifyBaseUrl(String baseUrl) {
		if (baseUrl == null) {
			return "";
		}
		String trimmed = baseUrl.trim();
		while (trimmed.endsWith("/")) {
			trimmed = trimmed.substring(0, trimmed.length() - 1);
		}
		return trimmed;
	}

}
