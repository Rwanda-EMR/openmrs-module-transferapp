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

import org.openmrs.module.transferapp.model.PastTransferItem;
import org.openmrs.module.transferapp.model.PastTransferPageResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Lists patient visits that started in a selected calendar month, with Transfer Id obs when present.
 */
@Transactional
public interface PastTransfersService {

	int DEFAULT_PAGE_SIZE = 100;

	/**
	 * First page ({@link #DEFAULT_PAGE_SIZE} rows) for the month.
	 */
	@Transactional(readOnly = true)
	List<PastTransferItem> findVisitsForMonth(String yearMonth);

	/**
	 * Paged visits for the month. {@code limit} defaults to {@link #DEFAULT_PAGE_SIZE} when &lt;= 0.
	 */
	@Transactional(readOnly = true)
	PastTransferPageResult findVisitsForMonth(String yearMonth, int offset, int limit);
}
