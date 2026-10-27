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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Privilege;
import org.openmrs.Role;
import org.openmrs.api.UserService;
import org.openmrs.api.context.Context;
import org.openmrs.module.BaseModuleActivator;
import org.openmrs.scheduler.SchedulerService;
import org.openmrs.scheduler.TaskDefinition;

import java.util.Date;

/**
 * This class contains the logic that is run every time this module is either started or stopped.
 */
public class TransferAppActivator extends BaseModuleActivator {

	private static final Log log = LogFactory.getLog(TransferAppActivator.class);

	public static final String PRIVILEGE_DASHBOARD = "App: transferapp.dashboard";

	public static final String PRIVILEGE_LIST_TRANSFERS = "Task: transferapp.listTransfers";

	public static final String PRIVILEGE_CREATE_TRANSFER = "Task: transferapp.createTransfer";

	public static final String PRIVILEGE_LIST_PENDING = "Task: transferapp.listpending";

	public static final String PRIVILEGE_PAST_TRANSFERS = "View: transferapp.pasttransfers";

	public static final String PRIVILEGE_FEEDBACK = "Task: transferapp.feedback";

	public static final String PRIVILEGE_CONFIGURATION = "Task: transferapp.configuration";

	private static final String[] DEFAULT_PRIVILEGE_ROLES = new String[] { "System Developer", "System Administrator" };

	@Override
	public void started() {
		ensureDefaultPrivilegesForSystemRoles();
		ensureOrphanTransferBackfillTask();
		super.started();
	}

	/**
	 * Registers (or updates) the orphan Transfer Id backfill scheduler task to run every 50 minutes.
	 * Work is gated by {@code transferapp.orphanTransferBackfill.enabled} (default false).
	 */
	private void ensureOrphanTransferBackfillTask() {
		try {
			SchedulerService schedulerService = Context.getSchedulerService();
			if (schedulerService == null) {
				return;
			}
			TaskDefinition task = schedulerService.getTaskByName(
					TransferAppConstants.ORPHAN_TRANSFER_BACKFILL_TASK_NAME);
			boolean created = false;
			if (task == null) {
				task = new TaskDefinition();
				task.setName(TransferAppConstants.ORPHAN_TRANSFER_BACKFILL_TASK_NAME);
				task.setDescription(
						"Backfills transfers rows for Transfer Id obs missing local records by fetching HIE Encounter details");
				task.setTaskClass(TransferAppConstants.ORPHAN_TRANSFER_BACKFILL_TASK_CLASS);
				task.setStartTime(new Date());
				task.setCreator(Context.getAuthenticatedUser());
				task.setDateCreated(new Date());
				created = true;
			}
			task.setTaskClass(TransferAppConstants.ORPHAN_TRANSFER_BACKFILL_TASK_CLASS);
			task.setRepeatInterval(TransferAppConstants.ORPHAN_TRANSFER_BACKFILL_REPEAT_INTERVAL_SECONDS);
			task.setStartOnStartup(Boolean.TRUE);
			schedulerService.saveTaskDefinition(task);
			if (!Boolean.TRUE.equals(task.getStarted())) {
				try {
					schedulerService.scheduleTask(task);
				}
				catch (Exception scheduleEx) {
					log.warn("Could not schedule orphan Transfer Id backfill task on startup: "
							+ scheduleEx.getMessage());
				}
			}
			log.info((created ? "Registered" : "Updated") + " scheduler task '"
					+ TransferAppConstants.ORPHAN_TRANSFER_BACKFILL_TASK_NAME
					+ "' (every "
					+ TransferAppConstants.ORPHAN_TRANSFER_BACKFILL_REPEAT_INTERVAL_SECONDS
					+ "s)");
		}
		catch (Exception ex) {
			log.warn("Unable to ensure orphan Transfer Id backfill scheduler task: " + ex.getMessage(), ex);
		}
	}

	private void ensureDefaultPrivilegesForSystemRoles() {
		UserService userService = Context.getUserService();
		for (String privilegeName : new String[] {
				PRIVILEGE_DASHBOARD,
				PRIVILEGE_LIST_TRANSFERS,
				PRIVILEGE_CREATE_TRANSFER,
				PRIVILEGE_LIST_PENDING,
				PRIVILEGE_FEEDBACK,
				PRIVILEGE_CONFIGURATION }) {
			grantPrivilegeToRoles(userService, privilegeName, DEFAULT_PRIVILEGE_ROLES);
		}
	}

	private void grantPrivilegeToRoles(UserService userService, String privilegeName, String[] roleNames) {
		Privilege privilege = userService.getPrivilege(privilegeName);
		if (privilege == null) {
			return;
		}

		for (String roleName : roleNames) {
			Role role = userService.getRole(roleName);
			if (role != null && !role.getPrivileges().contains(privilege)) {
				role.addPrivilege(privilege);
				userService.saveRole(role);
			}
		}
	}

}
