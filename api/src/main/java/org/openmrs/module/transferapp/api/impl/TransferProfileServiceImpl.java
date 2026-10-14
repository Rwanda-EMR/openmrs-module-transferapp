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

import org.openmrs.User;
import org.openmrs.api.APIException;
import org.openmrs.api.context.Context;
import org.openmrs.module.transferapp.api.ProviderProfileAttributes;
import org.openmrs.module.transferapp.api.TransferProfileService;
import org.openmrs.module.transferapp.model.TransferProfile;

/**
 * My Profile and transfer provider details are stored on OpenMRS provider attributes
 * ({@code Provider License}, {@code Phone Number}, {@code Qualification}, {@code Speciality}).
 */
public class TransferProfileServiceImpl implements TransferProfileService {

	@Override
	public TransferProfile getProfileForUser(User user) {
		if (user == null || user.getUserId() == null) {
			return null;
		}
		return ProviderProfileAttributes.readProfileForUser(user);
	}

	@Override
	public TransferProfile saveProfileForCurrentUser(String licenseNumber, String phoneNumber, String qualification,
			String speciality) {
		User user = Context.getAuthenticatedUser();
		if (user == null || user.getUserId() == null) {
			throw new APIException("You must be logged in to save your profile");
		}
		return ProviderProfileAttributes.saveProfileForUser(user, licenseNumber, phoneNumber, qualification, speciality);
	}

}
