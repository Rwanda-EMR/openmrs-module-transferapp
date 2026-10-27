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
package org.openmrs.module.transferapp.task;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.api.context.Context;
import org.openmrs.module.transferapp.TransferAppActivator;
import org.openmrs.module.transferapp.api.OrphanTransferBackfillService;
import org.openmrs.module.transferapp.model.OrphanTransferIdObs;
import org.openmrs.module.transferapp.model.Transfer;
import org.openmrs.scheduler.tasks.AbstractTask;

import java.util.List;

/**
 * Finds Transfer Id obs with no {@code transfers} row (within the lookback window), loads each
 * Encounter from HIE, and inserts the missing local transfer. Controlled by
 * {@code transferapp.orphanTransferBackfill.*} global properties. Runs every 50 minutes when
 * scheduled; does nothing while the enable flag is false.
 */
public class OrphanTransferIdBackfillTask extends AbstractTask {

	private static final Log log = LogFactory.getLog(OrphanTransferIdBackfillTask.class);

	@Override
	public void execute() {
		// Scheduler framework authenticates as the daemon user before execute().
		OrphanTransferBackfillService service = Context.getService(OrphanTransferBackfillService.class);
		if (service == null) {
			log.warn("Orphan Transfer Id backfill: service is not available");
			return;
		}
		if (!service.isBackfillEnabled()) {
			log.debug("Orphan Transfer Id backfill is disabled "
					+ "(transferapp.orphanTransferBackfill.enabled=false)");
			return;
		}

		List<OrphanTransferIdObs> orphans;
		try {
			orphans = service.findOrphanTransferIdObs();
		}
		catch (Exception ex) {
			log.error("Orphan Transfer Id backfill: failed to list orphan obs", ex);
			return;
		}
		if (orphans == null || orphans.isEmpty()) {
			log.info("Orphan Transfer Id backfill: no orphan Transfer Id obs in lookback window");
			return;
		}

		int delaySeconds = service.getDelaySeconds();
		int created = 0;
		int skipped = 0;
		int failed = 0;
		log.info("Orphan Transfer Id backfill: processing " + orphans.size() + " orphan obs"
				+ " (delay " + delaySeconds + "s between HIE calls)");

		for (int i = 0; i < orphans.size(); i++) {
			if (i > 0 && delaySeconds > 0) {
				try {
					Thread.sleep(delaySeconds * 1000L);
				}
				catch (InterruptedException ie) {
					Thread.currentThread().interrupt();
					log.warn("Orphan Transfer Id backfill interrupted during delay");
					break;
				}
			}
			OrphanTransferIdObs orphan = orphans.get(i);
			if (orphan == null || orphan.getPatientId() == null
					|| orphan.getHieTransferId() == null) {
				skipped++;
				continue;
			}
			Context.addProxyPrivilege(TransferAppActivator.PRIVILEGE_CREATE_TRANSFER);
			try {
				Transfer transfer = service.backfillOne(orphan.getPatientId(), orphan.getHieTransferId());
				if (transfer == null) {
					skipped++;
					log.info("Orphan Transfer Id backfill: HIE miss for "
							+ orphan.getHieTransferId() + " (patient " + orphan.getPatientId() + ")");
				} else {
					created++;
					log.info("Orphan Transfer Id backfill: stored transfer_id="
							+ transfer.getTransferId() + " for HIE id " + orphan.getHieTransferId()
							+ " (patient " + orphan.getPatientId() + ")");
				}
			}
			catch (Exception ex) {
				failed++;
				log.warn("Orphan Transfer Id backfill failed for HIE id " + orphan.getHieTransferId()
						+ " (patient " + orphan.getPatientId() + "): "
						+ (ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName()), ex);
			}
			finally {
				Context.removeProxyPrivilege(TransferAppActivator.PRIVILEGE_CREATE_TRANSFER);
			}
		}

		log.info("Orphan Transfer Id backfill finished: storedOrExisting=" + created
				+ ", skipped=" + skipped + ", failed=" + failed);
	}
}
