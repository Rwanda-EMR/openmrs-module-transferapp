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
package org.openmrs.module.transferapp.model;

import org.apache.commons.lang.StringUtils;
import org.openmrs.BaseOpenmrsData;

/**
 * In-memory view of a provider's transfer profile fields.
 * Persisted on OpenMRS {@code provider_attribute} (not a custom table).
 */
public class TransferProfile extends BaseOpenmrsData {

	private static final long serialVersionUID = 1L;

	private Integer transferProfileId;

	private Integer userId;

	private String licenseNumber;

	private String phoneNumber;

	private String qualification;

	private String speciality;

	@Override
	public Integer getId() {
		return getTransferProfileId();
	}

	@Override
	public void setId(Integer id) {
		setTransferProfileId(id);
	}

	public Integer getTransferProfileId() {
		return transferProfileId;
	}

	public void setTransferProfileId(Integer transferProfileId) {
		this.transferProfileId = transferProfileId;
	}

	public Integer getUserId() {
		return userId;
	}

	public void setUserId(Integer userId) {
		this.userId = userId;
	}

	public String getLicenseNumber() {
		return licenseNumber;
	}

	public void setLicenseNumber(String licenseNumber) {
		this.licenseNumber = licenseNumber;
	}

	public String getPhoneNumber() {
		return phoneNumber;
	}

	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}

	public String getQualification() {
		return qualification;
	}

	public void setQualification(String qualification) {
		this.qualification = qualification;
	}

	public String getSpeciality() {
		return speciality;
	}

	public void setSpeciality(String speciality) {
		this.speciality = speciality;
	}

	/**
	 * Returns qualification formatted with optional speciality, e.g. {@code Nurse A2(Midwife)}.
	 */
	public String getQualificationWithSpeciality() {
		String qualificationValue = StringUtils.trimToNull(qualification);
		if (qualificationValue == null) {
			return null;
		}
		String specialityValue = StringUtils.trimToNull(speciality);
		if (specialityValue == null) {
			return qualificationValue;
		}
		return qualificationValue + "(" + specialityValue + ")";
	}

	/**
	 * True when phone, qualification, and speciality are all set for transfer creation.
	 */
	public boolean isCompleteForTransfer() {
		return StringUtils.isNotBlank(phoneNumber)
				&& StringUtils.isNotBlank(qualification)
				&& StringUtils.isNotBlank(speciality);
	}

	/**
	 * Preview / HIE display: {@code Name(LICENSE)} when a license number is available.
	 * Does not append another {@code (…)} when the name already ends with one (avoids
	 * {@code Name(refLicense)(approverLicense)} after approval → HIE submit).
	 */
	public static String formatCareProviderName(String name, String licenseNumber) {
		String displayName = StringUtils.trimToEmpty(name);
		String license = StringUtils.trimToEmpty(licenseNumber);
		if (StringUtils.isBlank(displayName)) {
			return StringUtils.isNotBlank(license) ? "(" + license + ")" : "";
		}
		if (StringUtils.isBlank(license)) {
			return displayName;
		}
		String suffix = "(" + license + ")";
		if (displayName.endsWith(suffix) || displayName.contains("(" + license + ")")) {
			return displayName;
		}
		// Already has a trailing parenthetical (typically the referring license).
		if (displayName.matches(".*\\([^)]+\\)\\s*$")) {
			return displayName;
		}
		return displayName + suffix;
	}

	/**
	 * Keeps at most one trailing {@code (…)} license suffix on a referring-provider display name.
	 */
	public static String sanitizeReferringProviderDisplayName(String name) {
		String displayName = StringUtils.trimToEmpty(name);
		if (StringUtils.isBlank(displayName)) {
			return "";
		}
		// Name(license1)(license2)... → Name(license1)
		while (displayName.matches(".*\\([^)]+\\)\\([^)]+\\)\\s*$")) {
			int lastOpen = displayName.lastIndexOf('(');
			if (lastOpen <= 0) {
				break;
			}
			displayName = displayName.substring(0, lastOpen).trim();
		}
		return displayName;
	}

}
