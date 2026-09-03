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
import org.openmrs.api.context.Context;

/**
 * Runtime mode helpers backed by {@link TransferAppConstants#GP_PRODUCTION}.
 */
public final class TransferAppMode {

	private TransferAppMode() {
	}

	/**
	 * @return {@code true} unless the global property is explicitly {@code false}
	 *         (case-insensitive). Missing/blank values use the production default.
	 */
	public static boolean isProduction() {
		String raw = Context.getAdministrationService().getGlobalProperty(
				TransferAppConstants.GP_PRODUCTION, TransferAppConstants.DEFAULT_PRODUCTION);
		return !"false".equalsIgnoreCase(StringUtils.trimToEmpty(raw));
	}
}
