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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.ui.framework.page.PageRequest;
import org.openmrs.ui.framework.page.PageRequestMapper;
import org.springframework.stereotype.Component;

/**
 * Renders Find Patient through transferapp so the approver pending banner can appear
 * (coreapps findPatient has no fragment extension point like the home page).
 */
@Component
public class FindPatientPageRequestMapper implements PageRequestMapper {

	private static final Log log = LogFactory.getLog(FindPatientPageRequestMapper.class);

	@Override
	public boolean mapRequest(PageRequest request) {
		if (request == null || request.getProviderName() == null || request.getPageName() == null) {
			return false;
		}
		if (!"coreapps".equals(request.getProviderName())) {
			return false;
		}
		String pageName = request.getPageName();
		if (!"findpatient/findPatient".equals(pageName) && !"findPatient".equals(pageName)) {
			return false;
		}
		request.setProviderNameOverride("transferapp");
		request.setPageNameOverride("findpatient/findPatient");
		if (log.isDebugEnabled()) {
			log.debug("Mapped coreapps findPatient to transferapp findpatient/findPatient");
		}
		return true;
	}
}
