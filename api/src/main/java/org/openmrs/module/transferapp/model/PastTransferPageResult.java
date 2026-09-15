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

import java.util.Collections;
import java.util.List;

/**
 * One page of Past Transfers visit rows.
 */
public class PastTransferPageResult {

	private List<PastTransferItem> items = Collections.emptyList();

	private int totalCount;

	private int offset;

	private int limit;

	public List<PastTransferItem> getItems() {
		return items;
	}

	public void setItems(List<PastTransferItem> items) {
		this.items = items != null ? items : Collections.<PastTransferItem>emptyList();
	}

	public int getTotalCount() {
		return totalCount;
	}

	public void setTotalCount(int totalCount) {
		this.totalCount = totalCount;
	}

	public int getOffset() {
		return offset;
	}

	public void setOffset(int offset) {
		this.offset = offset;
	}

	public int getLimit() {
		return limit;
	}

	public void setLimit(int limit) {
		this.limit = limit;
	}

	public int getLoadedCount() {
		return offset + (items != null ? items.size() : 0);
	}

	public boolean isHasMore() {
		return getLoadedCount() < totalCount;
	}

	public int getNextOffset() {
		return getLoadedCount();
	}
}
