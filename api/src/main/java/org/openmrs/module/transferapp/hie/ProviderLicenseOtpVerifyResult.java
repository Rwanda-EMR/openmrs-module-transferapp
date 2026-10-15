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
 * Parsed response from provider-registry {@code POST .../licenses/validate-otp}.
 */
public class ProviderLicenseOtpVerifyResult {

	private final int statusCode;

	private final boolean success;

	private final String message;

	private final String licenseNumber;

	private final String licenseExpiryDate;

	private final String specialty;

	private final String qualification;

	private final Boolean providerActive;

	private final String rawBody;

	public ProviderLicenseOtpVerifyResult(int statusCode, boolean success, String message, String licenseNumber,
			String licenseExpiryDate, String specialty, String qualification, Boolean providerActive, String rawBody) {
		this.statusCode = statusCode;
		this.success = success;
		this.message = message;
		this.licenseNumber = licenseNumber;
		this.licenseExpiryDate = licenseExpiryDate;
		this.specialty = specialty;
		this.qualification = qualification;
		this.providerActive = providerActive;
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

	public String getLicenseNumber() {
		return licenseNumber;
	}

	public String getLicenseExpiryDate() {
		return licenseExpiryDate;
	}

	public String getSpecialty() {
		return specialty;
	}

	public String getQualification() {
		return qualification;
	}

	public Boolean getProviderActive() {
		return providerActive;
	}

	public String getRawBody() {
		return rawBody;
	}

	/**
	 * Maps {@code providerActive} to a display/status string for License Status attribute.
	 */
	public String resolveLicenseStatus() {
		if (providerActive == null) {
			return null;
		}
		return Boolean.TRUE.equals(providerActive) ? "active" : "inactive";
	}

}
