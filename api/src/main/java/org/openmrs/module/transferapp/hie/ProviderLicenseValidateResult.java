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

/**
 * Parsed response from provider-registry {@code POST .../licenses/validate}.
 */
public class ProviderLicenseValidateResult {

	private final int statusCode;

	private final boolean success;

	private final String message;

	private final String maskedPhone;

	private final String rawBody;

	public ProviderLicenseValidateResult(int statusCode, boolean success, String message, String maskedPhone,
			String rawBody) {
		this.statusCode = statusCode;
		this.success = success;
		this.message = message;
		this.maskedPhone = maskedPhone;
		this.rawBody = rawBody;
	}

	public int getStatusCode() {
		return statusCode;
	}

	public boolean isSuccess() {
		return success;
	}

	public String getMessage() {
		return message;
	}

	public String getMaskedPhone() {
		return maskedPhone;
	}

	public String getRawBody() {
		return rawBody;
	}

}
