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
import org.openmrs.User;
import org.openmrs.api.context.Context;

/**
 * Helpers for privilege checks and user-facing access-denied messages.
 */
public final class TransferPrivilegeHelper {

	private TransferPrivilegeHelper() {
	}

	public static boolean hasPrivilege(String privilege) {
		User user = Context.getAuthenticatedUser();
		return user != null && StringUtils.isNotBlank(privilege) && user.hasPrivilege(privilege);
	}

	public static String requiredPrivilegeMessage(String privilege) {
		if (StringUtils.isBlank(privilege)) {
			return "You do not have permission to access this feature.";
		}
		return "You do not have permission to access this feature. Required privilege: " + privilege;
	}

	public static boolean isPrivilegeException(Throwable throwable) {
		Throwable current = throwable;
		while (current != null) {
			String className = current.getClass().getName();
			if (className.contains("APIAuthenticationException")
					|| className.contains("ContextAuthenticationException")
					|| className.contains("AccessDeniedException")) {
				return true;
			}
			String message = current.getMessage();
			if (message != null) {
				String lower = message.toLowerCase();
				if (lower.contains("privileges required")
						|| lower.contains("privilege required")
						|| lower.contains("user is not logged in")
						|| lower.contains("authentication required")) {
					return true;
				}
			}
			current = current.getCause();
		}
		return false;
	}

	public static String resolveUserFacingMessage(Throwable throwable, String requiredPrivilege, String fallback) {
		if (isPrivilegeException(throwable)) {
			String extracted = extractRequiredPrivilege(throwable);
			if (StringUtils.isNotBlank(extracted)) {
				return requiredPrivilegeMessage(extracted);
			}
			if (StringUtils.isNotBlank(requiredPrivilege)) {
				return requiredPrivilegeMessage(requiredPrivilege);
			}
			return "You do not have permission to access this feature.";
		}

		// Prefer nested business/API messages over Spring transaction-framework wording
		// such as "Transaction silently rolled back because it has been marked as rollback-only".
		String businessMessage = findPreferredBusinessMessage(throwable);
		if (StringUtils.isNotBlank(businessMessage)) {
			return businessMessage;
		}

		Throwable current = throwable;
		while (current != null) {
			if (StringUtils.isNotBlank(current.getMessage())
					&& !isTransactionFrameworkMessage(current.getMessage())) {
				return current.getMessage().trim();
			}
			current = current.getCause();
		}
		return fallback != null ? fallback : "An unexpected error occurred";
	}

	/**
	 * Walks the cause chain and returns the deepest meaningful API / billing message,
	 * skipping Spring transaction rollback wrappers.
	 */
	private static String findPreferredBusinessMessage(Throwable throwable) {
		String deepest = null;
		Throwable current = throwable;
		while (current != null) {
			String message = current.getMessage();
			if (StringUtils.isNotBlank(message) && !isTransactionFrameworkMessage(message)) {
				String typeName = current.getClass().getName();
				boolean apiLike = typeName.contains("APIException")
						|| typeName.contains("HieApiException")
						|| typeName.contains("HieConfigurationException")
						|| typeName.contains("ValidationException");
				if (apiLike || deepest == null) {
					deepest = message.trim();
				}
			}
			current = current.getCause();
		}
		return deepest;
	}

	private static boolean isTransactionFrameworkMessage(String message) {
		if (StringUtils.isBlank(message)) {
			return false;
		}
		String lower = message.toLowerCase();
		return lower.contains("rollback-only")
				|| lower.contains("transaction silently rolled back")
				|| lower.contains("unexpectedrollback")
				|| lower.contains("no transaction is in progress")
				|| lower.contains("no hibernate session")
				|| lower.contains("could not obtain transaction-synchronized session")
				|| lower.contains("transaction was marked for rollback");
	}

	private static String extractRequiredPrivilege(Throwable throwable) {
		Throwable current = throwable;
		while (current != null) {
			String message = current.getMessage();
			if (message != null) {
				String marker = "Privileges required:";
				int idx = message.indexOf(marker);
				if (idx < 0) {
					idx = message.toLowerCase().indexOf("privileges required:");
					if (idx >= 0) {
						marker = message.substring(idx, idx + "privileges required:".length());
					}
				}
				if (idx >= 0) {
					String remainder = message.substring(idx + marker.length()).trim();
					if (remainder.length() > 0) {
						int end = remainder.indexOf('.');
						if (end > 0) {
							remainder = remainder.substring(0, end).trim();
						}
						return remainder;
					}
				}
			}
			current = current.getCause();
		}
		return null;
	}

}
