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
package org.openmrs.module.transferapp.api;

import org.apache.commons.lang.StringUtils;
import org.openmrs.Provider;
import org.openmrs.ProviderAttribute;
import org.openmrs.ProviderAttributeType;
import org.openmrs.User;
import org.openmrs.api.APIException;
import org.openmrs.api.ProviderService;
import org.openmrs.api.context.Context;
import org.openmrs.module.transferapp.model.TransferProfile;

import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Reads/writes My Profile fields on OpenMRS {@link Provider} attributes by type name.
 */
public final class ProviderProfileAttributes {

	public static final String TYPE_PROVIDER_LICENSE = "Provider License";

	public static final String TYPE_PHONE_NUMBER = "Phone Number";

	public static final String TYPE_QUALIFICATION = "Qualification";

	public static final String TYPE_SPECIALITY = "Speciality";

	public static final String TYPE_LICENSE_VALIDATED = "License Validated";

	public static final String TYPE_LICENSE_EXPIRY = "License Expiry Date";

	public static final String TYPE_LICENSE_STATUS = "License Status";

	private static final String DATE_PATTERN = "yyyy-MM-dd";

	private ProviderProfileAttributes() {
	}

	/**
	 * Builds an in-memory {@link TransferProfile} from provider attributes for the user.
	 * Returns {@code null} when none of the core profile attributes are present.
	 */
	public static TransferProfile readProfileForUser(User user) {
		Provider provider = findProviderForUser(user);
		if (provider == null) {
			return null;
		}
		String license = readText(provider, TYPE_PROVIDER_LICENSE);
		String phone = readText(provider, TYPE_PHONE_NUMBER);
		String qualification = readText(provider, TYPE_QUALIFICATION);
		String speciality = readText(provider, TYPE_SPECIALITY);
		if (license == null && phone == null && qualification == null && speciality == null) {
			return null;
		}
		TransferProfile profile = new TransferProfile();
		if (user != null) {
			profile.setUserId(user.getUserId());
		}
		profile.setLicenseNumber(license);
		profile.setPhoneNumber(phone);
		profile.setQualification(qualification);
		profile.setSpeciality(speciality);
		profile.setVoided(false);
		return profile;
	}

	public static Map<String, String> readLicenseValidationForUser(User user) {
		Map<String, String> values = new HashMap<String, String>();
		values.put("licenseValidated", "false");
		values.put("licenseExpiryDate", "");
		values.put("licenseStatus", "");
		Provider provider = findProviderForUser(user);
		if (provider == null) {
			return values;
		}
		Boolean validated = readBoolean(provider, TYPE_LICENSE_VALIDATED);
		values.put("licenseValidated", Boolean.TRUE.equals(validated) ? "true" : "false");
		Date expiry = readDate(provider, TYPE_LICENSE_EXPIRY);
		if (expiry != null) {
			values.put("licenseExpiryDate", new SimpleDateFormat(DATE_PATTERN).format(expiry));
		}
		String status = readText(provider, TYPE_LICENSE_STATUS);
		if (status != null) {
			values.put("licenseStatus", status);
		}
		return values;
	}

	public static TransferProfile saveProfileForUser(User user, String licenseNumber, String phoneNumber,
			String qualification, String speciality) {
		if (user == null || user.getUserId() == null) {
			throw new APIException("You must be logged in to save your profile");
		}
		String license = requireText(licenseNumber, "License number is required");
		String phone = requireText(phoneNumber, "Phone number is required");
		String qualificationValue = requireText(qualification, "Qualification is required");
		String specialityValue = StringUtils.trimToNull(speciality);

		Provider provider = ensureProviderForUser(user);
		upsertText(provider, TYPE_PROVIDER_LICENSE, license);
		upsertText(provider, TYPE_PHONE_NUMBER, phone);
		upsertText(provider, TYPE_QUALIFICATION, qualificationValue);
		if (specialityValue != null) {
			upsertText(provider, TYPE_SPECIALITY, specialityValue);
		}
		else {
			clearText(provider, TYPE_SPECIALITY);
		}
		Context.getProviderService().saveProvider(provider);

		TransferProfile profile = new TransferProfile();
		profile.setUserId(user.getUserId());
		profile.setLicenseNumber(license);
		profile.setPhoneNumber(phone);
		profile.setQualification(qualificationValue);
		profile.setSpeciality(specialityValue);
		profile.setVoided(false);
		return profile;
	}

	public static void saveValidationResult(User user, boolean validated, String expiryDate, String status) {
		Provider provider = ensureProviderForUser(user);
		upsertBoolean(provider, TYPE_LICENSE_VALIDATED, validated);
		upsertDate(provider, TYPE_LICENSE_EXPIRY, parseDate(expiryDate));
		upsertText(provider, TYPE_LICENSE_STATUS, status);
		Context.getProviderService().saveProvider(provider);
	}

	private static Provider ensureProviderForUser(User user) {
		Provider existing = findProviderForUser(user);
		if (existing != null) {
			return existing;
		}
		if (user == null || user.getPerson() == null) {
			throw new APIException("Cannot save provider profile: user has no person record");
		}
		Provider provider = new Provider();
		provider.setPerson(user.getPerson());
		provider.setIdentifier(buildProviderIdentifier(user));
		provider.setCreator(user);
		provider.setDateCreated(new Date());
		provider.setRetired(false);
		provider.setUuid(UUID.randomUUID().toString());
		return Context.getProviderService().saveProvider(provider);
	}

	private static String buildProviderIdentifier(User user) {
		// Stable unique id; avoid colliding with existing provider.identifier values.
		return "TP-" + user.getUserId();
	}

	private static Provider findProviderForUser(User user) {
		if (user == null || user.getPerson() == null) {
			return null;
		}
		ProviderService providerService = Context.getProviderService();
		Collection<Provider> providers = providerService.getProvidersByPerson(user.getPerson(), false);
		if (providers == null || providers.isEmpty()) {
			return null;
		}
		return providers.iterator().next();
	}

	private static ProviderAttributeType findType(String name) {
		ProviderService providerService = Context.getProviderService();
		List<ProviderAttributeType> types = providerService.getAllProviderAttributeTypes(false);
		if (types == null) {
			return null;
		}
		for (ProviderAttributeType type : types) {
			if (type != null && name.equals(type.getName())) {
				return type;
			}
		}
		return null;
	}

	private static ProviderAttribute findActiveAttribute(Provider provider, ProviderAttributeType type) {
		if (provider == null || type == null) {
			return null;
		}
		Collection<ProviderAttribute> attributes = provider.getActiveAttributes(type);
		if (attributes == null || attributes.isEmpty()) {
			return null;
		}
		return attributes.iterator().next();
	}

	private static Boolean readBoolean(Provider provider, String typeName) {
		ProviderAttributeType type = findType(typeName);
		ProviderAttribute attr = findActiveAttribute(provider, type);
		if (attr == null || attr.getValue() == null) {
			return null;
		}
		Object value = attr.getValue();
		if (value instanceof Boolean) {
			return (Boolean) value;
		}
		return Boolean.valueOf(String.valueOf(value));
	}

	private static Date readDate(Provider provider, String typeName) {
		ProviderAttributeType type = findType(typeName);
		ProviderAttribute attr = findActiveAttribute(provider, type);
		if (attr == null || attr.getValue() == null) {
			return null;
		}
		Object value = attr.getValue();
		if (value instanceof Date) {
			return (Date) value;
		}
		return parseDate(String.valueOf(value));
	}

	private static String readText(Provider provider, String typeName) {
		ProviderAttributeType type = findType(typeName);
		ProviderAttribute attr = findActiveAttribute(provider, type);
		if (attr == null || attr.getValue() == null) {
			return null;
		}
		return StringUtils.trimToNull(String.valueOf(attr.getValue()));
	}

	private static void upsertBoolean(Provider provider, String typeName, boolean value) {
		ProviderAttributeType type = requireType(typeName);
		ProviderAttribute attr = findActiveAttribute(provider, type);
		if (attr == null) {
			attr = new ProviderAttribute();
			attr.setAttributeType(type);
			provider.addAttribute(attr);
		}
		attr.setValue(Boolean.valueOf(value));
	}

	private static void upsertDate(Provider provider, String typeName, Date value) {
		ProviderAttributeType type = requireType(typeName);
		if (value == null) {
			return;
		}
		ProviderAttribute attr = findActiveAttribute(provider, type);
		if (attr == null) {
			attr = new ProviderAttribute();
			attr.setAttributeType(type);
			provider.addAttribute(attr);
		}
		attr.setValue(value);
	}

	private static void upsertText(Provider provider, String typeName, String value) {
		ProviderAttributeType type = requireType(typeName);
		String trimmed = StringUtils.trimToNull(value);
		if (trimmed == null) {
			return;
		}
		ProviderAttribute attr = findActiveAttribute(provider, type);
		if (attr == null) {
			attr = new ProviderAttribute();
			attr.setAttributeType(type);
			provider.addAttribute(attr);
		}
		attr.setValue(trimmed);
	}

	private static void clearText(Provider provider, String typeName) {
		ProviderAttributeType type = findType(typeName);
		ProviderAttribute attr = findActiveAttribute(provider, type);
		if (attr != null) {
			attr.setVoided(true);
			attr.setDateVoided(new Date());
			attr.setVoidReason("Cleared from transfer profile");
			User user = Context.getAuthenticatedUser();
			if (user != null) {
				attr.setVoidedBy(user);
			}
		}
	}

	private static ProviderAttributeType requireType(String typeName) {
		ProviderAttributeType type = findType(typeName);
		if (type == null) {
			throw new APIException("Provider attribute type '" + typeName
					+ "' is missing. Run the transferapp Liquibase updates.");
		}
		return type;
	}

	private static String requireText(String value, String message) {
		String trimmed = StringUtils.trimToNull(value);
		if (trimmed == null) {
			throw new APIException(message);
		}
		return trimmed;
	}

	private static Date parseDate(String value) {
		String trimmed = StringUtils.trimToNull(value);
		if (trimmed == null) {
			return null;
		}
		try {
			return new SimpleDateFormat(DATE_PATTERN).parse(trimmed);
		}
		catch (Exception ignored) {
			return null;
		}
	}
}
