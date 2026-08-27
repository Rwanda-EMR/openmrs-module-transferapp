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
package org.openmrs.module.transferapp;

import org.apache.commons.lang.StringUtils;

/**
 * Detects whether a free-text patient identifier is a National ID (16 digits)
 * or a UPID (6-4-4).
 */
public final class PatientTransferIdentifierDetector {

	public enum Kind {
		NATIONAL_ID,
		UPID,
		UNKNOWN
	}

	private PatientTransferIdentifierDetector() {
	}

	public static Kind detect(String raw) {
		String normalized = normalize(raw);
		if (normalized == null) {
			return Kind.UNKNOWN;
		}
		if (normalized.matches("\\d{16}")) {
			return Kind.NATIONAL_ID;
		}
		if (normalized.matches("\\d{6}-\\d{4}-\\d{4}")) {
			return Kind.UPID;
		}
		if (normalized.matches("\\d{14}")) {
			return Kind.UPID;
		}
		return Kind.UNKNOWN;
	}

	/**
	 * Digits-only for National ID; 6-4-4 for UPID (inserts dashes when 14 digits).
	 */
	public static String normalize(String raw) {
		if (raw == null) {
			return null;
		}
		String trimmed = raw.trim().replaceAll("\\s+", "");
		if (trimmed.isEmpty()) {
			return null;
		}
		if (trimmed.matches("\\d{16}")) {
			return trimmed;
		}
		if (trimmed.matches("\\d{6}-\\d{4}-\\d{4}")) {
			return trimmed;
		}
		String digitsOnly = trimmed.replace("-", "");
		if (digitsOnly.matches("\\d{14}")) {
			return digitsOnly.substring(0, 6) + "-" + digitsOnly.substring(6, 10) + "-"
					+ digitsOnly.substring(10, 14);
		}
		if (digitsOnly.matches("\\d{16}")) {
			return digitsOnly;
		}
		return StringUtils.trimToNull(trimmed);
	}
}
