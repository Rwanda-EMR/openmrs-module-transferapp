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

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.api.context.Context;
import org.openmrs.module.mohbilling.model.Consommation;
import org.openmrs.module.mohbilling.service.BillingService;
import org.openmrs.module.transferapp.api.PatientInsuranceService;
import org.openmrs.module.transferapp.api.TransferAdminService;
import org.openmrs.module.transferapp.api.TransferAmbulanceBillingService;
import org.openmrs.module.transferapp.api.dao.TransferDao;
import org.openmrs.module.transferapp.model.ReceivingFacility;
import org.openmrs.module.transferapp.model.Transfer;

/**
 * Ambulance billing on local transfer save via mohbilling
 * {@code createAmbulanceBill} / {@code updateAmbulanceBill} / {@code deleteAmbulanceBill}.
 */
public class TransferAmbulanceBillingServiceImpl implements TransferAmbulanceBillingService {

	private static final Log log = LogFactory.getLog(TransferAmbulanceBillingServiceImpl.class);

	private static final String TRANSPORT_TYPE_AMBULANCE = "AMBULANCE";

	private static final String DELETE_VOID_REASON = "Ambulance transport removed from transfer";

	private TransferAdminService transferAdminService;

	private PatientInsuranceService patientInsuranceService;

	private TransferDao transferDao;

	public void setTransferAdminService(TransferAdminService transferAdminService) {
		this.transferAdminService = transferAdminService;
	}

	public void setPatientInsuranceService(PatientInsuranceService patientInsuranceService) {
		this.patientInsuranceService = patientInsuranceService;
	}

	public void setTransferDao(TransferDao transferDao) {
		this.transferDao = transferDao;
	}

	@Override
	public Transfer syncAmbulanceBill(Transfer transfer, String previousReceivingFacilityCode,
			String previousTransportType) {
		if (transfer == null) {
			return transfer;
		}

		boolean nowAmbulance = TRANSPORT_TYPE_AMBULANCE.equals(StringUtils.trimToEmpty(transfer.getTransportType()));
		Integer existingConsommationId = transfer.getAmbulanceConsommationId();
		String previousCode = StringUtils.trimToNull(previousReceivingFacilityCode);
		String currentCode = StringUtils.trimToNull(transfer.getReceivingFacilityCode());
		boolean destinationChanged = !StringUtils.equals(previousCode, currentCode)
				&& (previousCode != null || currentCode != null);
		boolean providedByCurrentFacility = isAmbulanceProvidedByCurrentFacility(transfer);

		if (!nowAmbulance || !providedByCurrentFacility) {
			if (existingConsommationId != null) {
				return deleteAmbulanceBillForTransfer(transfer, existingConsommationId);
			}
			if (nowAmbulance && !providedByCurrentFacility) {
				log.info("Skipping ambulance bill for transfer " + transfer.getUuid()
						+ ": ambulance provider FOSA "
						+ StringUtils.defaultString(transfer.getAmbulanceProviderFosaId())
						+ " is not the current facility");
			}
			return transfer;
		}

		if (existingConsommationId != null) {
			if (destinationChanged) {
				return updateAmbulanceBillForTransfer(transfer, existingConsommationId);
			}
			return transfer;
		}

		return createAmbulanceBillForTransfer(transfer);
	}

	@Override
	public Transfer createAmbulanceBillWithRoute(Transfer transfer, int kilometers, String description) {
		if (transfer == null) {
			throw new org.openmrs.api.APIException("Transfer is required");
		}
		if (kilometers <= 0) {
			throw new org.openmrs.api.APIException("Distance in kilometers must be greater than zero");
		}
		String billDescription = StringUtils.trimToNull(description);
		if (billDescription == null) {
			throw new org.openmrs.api.APIException("Ambulance bill description is required");
		}
		if (transfer.getAmbulanceConsommationId() != null) {
			return transfer;
		}
		if (!TRANSPORT_TYPE_AMBULANCE.equals(StringUtils.trimToEmpty(transfer.getTransportType()))) {
			transfer.setTransportType(TRANSPORT_TYPE_AMBULANCE);
		}
		if (!isAmbulanceProvidedByCurrentFacility(transfer)) {
			throw new org.openmrs.api.APIException(
					"Ambulance voucher can only be created when this facility is the ambulance provider"
							+ " (or transferapp.production=false)");
		}

		String insuranceCardNumber = patientInsuranceService != null
				? patientInsuranceService.resolveInsuranceCardNumber(transfer.getPatient())
				: null;
		if (StringUtils.isBlank(insuranceCardNumber)) {
			throw new org.openmrs.api.APIException(
					"Insurance card/policy number not found on the current registration encounter");
		}

		BillingService billingService = resolveBillingService();
		if (billingService == null) {
			throw new org.openmrs.api.APIException("mohbilling BillingService is not available");
		}

		try {
			Consommation consommation = billingService.createAmbulanceBill(
					insuranceCardNumber, kilometers, billDescription);
			if (consommation == null || consommation.getConsommationId() == null) {
				throw new org.openmrs.api.APIException("createAmbulanceBill returned no consommation");
			}
			transfer.setAmbulanceConsommationId(consommation.getConsommationId());
			transfer = saveTransfer(transfer);
			log.info("Created ambulance bill consommationId=" + consommation.getConsommationId()
					+ " for transfer " + transfer.getUuid() + " distance=" + kilometers
					+ " description=" + billDescription);
			return transfer;
		}
		catch (org.openmrs.api.APIException ex) {
			throw ex;
		}
		catch (Exception ex) {
			throw new org.openmrs.api.APIException(
					"Unable to create ambulance bill: " + StringUtils.defaultString(ex.getMessage()), ex);
		}
	}

	private boolean isAmbulanceProvidedByCurrentFacility(Transfer transfer) {
		// Testing mode: skip ambulance-provider filter so any facility can voucher.
		if (!org.openmrs.module.transferapp.TransferAppMode.isProduction()) {
			return true;
		}
		String providerFosaId = StringUtils.trimToNull(transfer != null ? transfer.getAmbulanceProviderFosaId() : null);
		String providerName = StringUtils.trimToNull(transfer != null ? transfer.getAmbulanceProviderName() : null);
		String currentFosaId = StringUtils.trimToNull(Context.getAdministrationService().getGlobalProperty(
				org.openmrs.module.transferapp.TransferAppConstants.GP_SENDING_FOSA_ID,
				org.openmrs.module.transferapp.TransferAppConstants.DEFAULT_SENDING_FOSA_ID));
		if (providerFosaId != null && currentFosaId != null && currentFosaId.equalsIgnoreCase(providerFosaId)) {
			return true;
		}
		String outboundName = null;
		if (transferAdminService != null) {
			outboundName = StringUtils.trimToNull(transferAdminService.resolveOutboundFacilityName());
			if (outboundName == null) {
				outboundName = StringUtils.trimToNull(transferAdminService.resolveCurrentSendingFacilityName());
			}
		}
		return providerName != null && outboundName != null && outboundName.equalsIgnoreCase(providerName);
	}

	private Transfer createAmbulanceBillForTransfer(Transfer transfer) {
		AmbulanceBillParams params = resolveBillParams(transfer, "create");
		if (params == null) {
			return transfer;
		}
		try {
			return createAmbulanceBillWithRoute(transfer, params.kilometers, params.description);
		}
		catch (Exception ex) {
			log.warn("Skipping ambulance bill create for transfer " + transfer.getUuid() + ": " + ex.getMessage(), ex);
			return transfer;
		}
	}

	private Transfer updateAmbulanceBillForTransfer(Transfer transfer, Integer consommationId) {
		AmbulanceBillParams params = resolveBillParams(transfer, "update");
		if (params == null) {
			return transfer;
		}

		BillingService billingService = resolveBillingService();
		if (billingService == null) {
			log.warn("Skipping ambulance bill update for transfer " + transfer.getUuid()
					+ ": mohbilling BillingService is not available");
			return transfer;
		}

		try {
			billingService.updateAmbulanceBill(consommationId, params.kilometers, params.description);
			log.info("Updated ambulance bill consommationId=" + consommationId
					+ " for transfer " + transfer.getUuid() + " distance=" + params.kilometers
					+ " description=" + params.description);
		}
		catch (Exception ex) {
			log.warn("Skipping ambulance bill update for transfer " + transfer.getUuid()
					+ " (consommationId=" + consommationId + "): " + ex.getMessage(), ex);
		}
		return transfer;
	}

	private Transfer deleteAmbulanceBillForTransfer(Transfer transfer, Integer consommationId) {
		BillingService billingService = resolveBillingService();
		if (billingService == null) {
			log.warn("Skipping ambulance bill delete for transfer " + transfer.getUuid()
					+ ": mohbilling BillingService is not available");
			return transfer;
		}

		try {
			billingService.deleteAmbulanceBill(consommationId, DELETE_VOID_REASON);
			transfer.setAmbulanceConsommationId(null);
			transfer = saveTransfer(transfer);
			log.info("Deleted ambulance bill consommationId=" + consommationId
					+ " for transfer " + transfer.getUuid());
		}
		catch (Exception ex) {
			log.warn("Skipping ambulance bill delete for transfer " + transfer.getUuid()
					+ " (consommationId=" + consommationId + "): " + ex.getMessage(), ex);
		}
		return transfer;
	}

	private AmbulanceBillParams resolveBillParams(Transfer transfer, String action) {
		ReceivingFacility destination = resolveDestination(transfer);
		if (destination == null) {
			log.warn("Skipping ambulance bill " + action + " for transfer " + transfer.getUuid()
					+ ": receiving facility configuration not found");
			return null;
		}

		Integer distance = destination.getDistance();
		if (distance == null || distance.intValue() <= 0) {
			log.warn("Skipping ambulance bill " + action + " for transfer " + transfer.getUuid()
					+ ": destination distance is missing or not greater than zero for "
					+ destination.getFacilityName());
			return null;
		}

		String insuranceCardNumber = null;
		if ("create".equals(action)) {
			insuranceCardNumber = patientInsuranceService != null
					? patientInsuranceService.resolveInsuranceCardNumber(transfer.getPatient())
					: null;
			if (StringUtils.isBlank(insuranceCardNumber)) {
				log.warn("Skipping ambulance bill create for transfer " + transfer.getUuid()
						+ ": insurance card/policy number not found on registration encounter");
				return null;
			}
		}

		String destinationName = StringUtils.trimToNull(destination.getFacilityName());
		if (destinationName == null) {
			destinationName = StringUtils.defaultString(transfer.getReceivingFacilityCode(), "destination");
		}

		AmbulanceBillParams params = new AmbulanceBillParams();
		params.insuranceCardNumber = StringUtils.trimToNull(insuranceCardNumber);
		params.kilometers = distance.intValue();
		params.description = "Transfer to " + destinationName;
		return params;
	}

	private Transfer saveTransfer(Transfer transfer) {
		if (transferDao != null) {
			return transferDao.saveTransfer(transfer);
		}
		return transfer;
	}

	private ReceivingFacility resolveDestination(Transfer transfer) {
		if (transferAdminService == null || transfer == null) {
			return null;
		}
		Integer sendingLocationId = transferAdminService.resolveCurrentSendingLocationId();
		if (sendingLocationId == null) {
			return null;
		}
		String code = StringUtils.trimToNull(transfer.getReceivingFacilityCode());
		if (code == null) {
			return null;
		}
		return transferAdminService.getReceivingFacilityByCode(sendingLocationId, code);
	}

	protected BillingService resolveBillingService() {
		try {
			return Context.getService(BillingService.class);
		}
		catch (Exception ex) {
			return null;
		}
	}

	private static class AmbulanceBillParams {
		private String insuranceCardNumber;
		private int kilometers;
		private String description;
	}

}
