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
import org.openmrs.api.APIException;
import org.openmrs.api.context.Context;
import org.openmrs.module.mohbilling.model.Consommation;
import org.openmrs.module.mohbilling.model.PatientServiceBill;
import org.openmrs.module.mohbilling.service.BillingService;
import org.openmrs.module.transferapp.api.PatientInsuranceService;
import org.openmrs.module.transferapp.api.TransferAdminService;
import org.openmrs.module.transferapp.api.TransferAmbulanceBillingService;
import org.openmrs.module.transferapp.api.TransferAmbulanceVoucherService;
import org.openmrs.module.transferapp.api.TransferHieReceiveService;
import org.openmrs.module.transferapp.api.dao.TransferDao;
import org.openmrs.module.transferapp.model.AmbulanceVoucherItem;
import org.openmrs.module.transferapp.model.AmbulanceVoucherPage;
import org.openmrs.module.transferapp.model.AmbulanceVoucherPreview;
import org.openmrs.module.transferapp.model.ReceivingFacility;
import org.openmrs.module.transferapp.model.Transfer;
import org.openmrs.Patient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class TransferAmbulanceVoucherServiceImpl implements TransferAmbulanceVoucherService {

	private static final Log log = LogFactory.getLog(TransferAmbulanceVoucherServiceImpl.class);

	private static final String TRANSPORT_TYPE_AMBULANCE = "AMBULANCE";

	private TransferDao transferDao;

	private TransferAdminService transferAdminService;

	private PatientInsuranceService patientInsuranceService;

	private TransferHieReceiveService transferHieReceiveService;

	private TransferAmbulanceBillingService transferAmbulanceBillingService;

	public void setTransferDao(TransferDao transferDao) {
		this.transferDao = transferDao;
	}

	public void setTransferAdminService(TransferAdminService transferAdminService) {
		this.transferAdminService = transferAdminService;
	}

	public void setPatientInsuranceService(PatientInsuranceService patientInsuranceService) {
		this.patientInsuranceService = patientInsuranceService;
	}

	public void setTransferHieReceiveService(TransferHieReceiveService transferHieReceiveService) {
		this.transferHieReceiveService = transferHieReceiveService;
	}

	public void setTransferAmbulanceBillingService(TransferAmbulanceBillingService transferAmbulanceBillingService) {
		this.transferAmbulanceBillingService = transferAmbulanceBillingService;
	}

	@Override
	public Map<String, Object> resolveLocalVoucherLink(Integer patientId, String hieTransferId) {
		Map<String, Object> link = new HashMap<String, Object>();
		link.put("localTransferUuid", "");
		link.put("hasAmbulanceVoucher", Boolean.FALSE);
		link.put("ambulanceConsommationId", null);
		if (patientId == null || StringUtils.isBlank(hieTransferId) || transferDao == null) {
			return link;
		}
		Transfer local = transferDao.getTransferByHieTransferId(patientId, hieTransferId.trim());
		if (local == null) {
			return link;
		}
		link.put("localTransferUuid", StringUtils.defaultString(local.getUuid()));
		boolean hasVoucher = local.getAmbulanceConsommationId() != null;
		link.put("hasAmbulanceVoucher", Boolean.valueOf(hasVoucher));
		link.put("ambulanceConsommationId", local.getAmbulanceConsommationId());
		return link;
	}

	@Override
	public Transfer createAmbulanceVoucherFromHie(Integer patientId, String hieTransferId, int kilometers,
			String coveredDistrict, String province) {
		if (patientId == null) {
			throw new APIException("Patient is required");
		}
		if (StringUtils.isBlank(hieTransferId)) {
			throw new APIException("HIE transfer id is required");
		}
		if (kilometers <= 0) {
			throw new APIException("Distance in kilometers must be greater than zero");
		}
		String district = StringUtils.trimToNull(coveredDistrict);
		if (district == null) {
			throw new APIException("Covered district is required");
		}
		if (transferHieReceiveService == null) {
			throw new APIException("HIE receive service is not available");
		}
		if (transferAmbulanceBillingService == null) {
			throw new APIException("Ambulance billing service is not available");
		}

		Patient patient = Context.getPatientService().getPatient(patientId);
		if (patient == null) {
			throw new APIException("Patient not found");
		}
		String insuranceCard = patientInsuranceService != null
				? patientInsuranceService.resolveInsuranceCardNumber(patient)
				: null;
		if (StringUtils.isBlank(insuranceCard)) {
			throw new APIException(
					"Insurance card/policy number not found on the current registration encounter");
		}

		Transfer transfer = transferHieReceiveService.receiveTransferFromHie(patientId, hieTransferId.trim());
		if (transfer == null) {
			throw new APIException("Unable to store transfer from HIE");
		}
		if (transfer.getAmbulanceConsommationId() != null) {
			return transfer;
		}

		// In production, only allow voucher creation when HIE names this facility as provider.
		// Must check before overwriting provider fields for local billing.
		assertAllowedToCreateAmbulanceVoucher(transfer);

		if (!TRANSPORT_TYPE_AMBULANCE.equals(StringUtils.trimToEmpty(transfer.getTransportType()))) {
			transfer.setTransportType(TRANSPORT_TYPE_AMBULANCE);
		}
		transfer.setReceivingDistrict(StringUtils.left(district, 120));
		ensureReceivingProvince(transfer, province);
		// Creating the voucher at this facility implies we are providing the ambulance.
		String currentFosaId = StringUtils.trimToNull(Context.getAdministrationService().getGlobalProperty(
				org.openmrs.module.transferapp.TransferAppConstants.GP_SENDING_FOSA_ID,
				org.openmrs.module.transferapp.TransferAppConstants.DEFAULT_SENDING_FOSA_ID));
		if (currentFosaId != null) {
			transfer.setAmbulanceProviderFosaId(currentFosaId);
		}
		if (transferAdminService != null) {
			String name = StringUtils.trimToNull(transferAdminService.resolveOutboundFacilityName());
			if (name == null) {
				name = StringUtils.trimToNull(transferAdminService.resolveCurrentSendingFacilityName());
			}
			if (name != null) {
				transfer.setAmbulanceProviderName(name);
			}
		}
		if (transferDao != null) {
			transfer = transferDao.saveTransfer(transfer);
		}

		String description = buildReceivedAmbulanceBillDescription(transfer);
		transfer = transferAmbulanceBillingService.createAmbulanceBillWithRoute(
				transfer, kilometers, description);
		if (transfer.getAmbulanceConsommationId() == null) {
			throw new APIException("Unable to create ambulance voucher");
		}
		return transfer;
	}

	/**
	 * Keeps province from the HIE transfer when present; otherwise uses the create-dialog
	 * value or the destination facility registry province.
	 */
	private void ensureReceivingProvince(Transfer transfer, String provinceFromRequest) {
		if (transfer == null) {
			return;
		}
		if (StringUtils.isNotBlank(transfer.getReceivingProvince())) {
			return;
		}
		String province = StringUtils.trimToNull(provinceFromRequest);
		if (province == null && transferAdminService != null) {
			Integer sendingLocationId = transferAdminService.resolveCurrentSendingLocationId();
			province = StringUtils.trimToNull(resolveProvinceFromDestination(transfer, sendingLocationId));
		}
		if (province != null) {
			transfer.setReceivingProvince(StringUtils.left(province, 120));
		}
	}

	private String resolveProvince(Transfer transfer, Integer sendingLocationId) {
		String province = StringUtils.trimToNull(transfer != null ? transfer.getReceivingProvince() : null);
		if (province != null) {
			return province;
		}
		province = StringUtils.trimToNull(resolveProvinceFromDestination(transfer, sendingLocationId));
		return province != null ? province : "";
	}

	private String resolveProvinceFromDestination(Transfer transfer, Integer sendingLocationId) {
		if (transfer == null || transferAdminService == null || sendingLocationId == null) {
			return null;
		}
		String code = StringUtils.trimToNull(transfer.getReceivingFacilityCode());
		if (code == null) {
			return null;
		}
		ReceivingFacility facility = transferAdminService.getReceivingFacilityByCode(sendingLocationId, code);
		if (facility == null) {
			return null;
		}
		return StringUtils.trimToNull(facility.getProvince());
	}

	/**
	 * Production requires the HIE transfer's ambulance provider to match this facility
	 * ({@code transferapp.sendingFosaId} or {@code transferapp.outboundFacilityName}).
	 * Non-production allows any transfer.
	 */
	private void assertAllowedToCreateAmbulanceVoucher(Transfer transfer) {
		if (!org.openmrs.module.transferapp.TransferAppMode.isProduction()) {
			return;
		}
		String providerFosaId = StringUtils.trimToNull(transfer != null ? transfer.getAmbulanceProviderFosaId() : null);
		String providerName = StringUtils.trimToNull(transfer != null ? transfer.getAmbulanceProviderName() : null);
		String currentFosaId = StringUtils.trimToNull(Context.getAdministrationService().getGlobalProperty(
				org.openmrs.module.transferapp.TransferAppConstants.GP_SENDING_FOSA_ID,
				org.openmrs.module.transferapp.TransferAppConstants.DEFAULT_SENDING_FOSA_ID));
		String outboundName = null;
		if (transferAdminService != null) {
			outboundName = StringUtils.trimToNull(transferAdminService.resolveOutboundFacilityName());
			if (outboundName == null) {
				outboundName = StringUtils.trimToNull(transferAdminService.resolveCurrentSendingFacilityName());
			}
		}
		boolean fosaMatch = providerFosaId != null && currentFosaId != null
				&& currentFosaId.equalsIgnoreCase(providerFosaId);
		boolean nameMatch = providerName != null && outboundName != null
				&& outboundName.equalsIgnoreCase(providerName);
		if (!fosaMatch && !nameMatch) {
			throw new APIException(
					"Ambulance voucher can only be created when this facility ("
							+ (outboundName != null ? outboundName : "outbound facility")
							+ ") is the ambulance provider, or when transferapp.production=false");
		}
	}

	/**
	 * My Location (outbound/current facility) via transfer origin to receiving facility.
	 */
	private String buildReceivedAmbulanceBillDescription(Transfer transfer) {
		String myLocation = "";
		if (transferAdminService != null) {
			myLocation = StringUtils.trimToEmpty(transferAdminService.resolveOutboundFacilityName());
			if (StringUtils.isBlank(myLocation)) {
				myLocation = StringUtils.trimToEmpty(transferAdminService.resolveCurrentSendingFacilityName());
			}
		}
		String viaFacility = StringUtils.trimToEmpty(transfer != null ? transfer.getSendingFacility() : null);
		String toFacility = "";
		if (transfer != null) {
			toFacility = StringUtils.trimToEmpty(transfer.getReceivingFacilityCode());
			if (transferAdminService != null && StringUtils.isNotBlank(toFacility)) {
				Integer sendingLocationId = transferAdminService.resolveCurrentSendingLocationId();
				String resolved = transferAdminService.resolveReceivingFacilityName(sendingLocationId, toFacility);
				if (StringUtils.isNotBlank(resolved)) {
					toFacility = resolved;
				}
			}
		}
		return blankAsNa(myLocation) + " via " + blankAsNa(viaFacility) + " to " + blankAsNa(toFacility);
	}

	private static String blankAsNa(String value) {
		return StringUtils.isNotBlank(value) ? value.trim() : "N/A";
	}

	@Override
	public AmbulanceVoucherPage getVouchers(Date startDate, Date endDate) {
		AmbulanceVoucherPage result = new AmbulanceVoucherPage();
		result.setPage(1);
		result.setPageSize(0);
		result.setTotalPages(0);

		Date rangeStart = startOfDay(startDate != null ? startDate : new Date());
		Date rangeEnd = endOfDay(endDate != null ? endDate : new Date());
		if (rangeStart.after(rangeEnd)) {
			Date swap = rangeStart;
			rangeStart = startOfDay(rangeEnd);
			rangeEnd = endOfDay(swap);
		}

		if (transferDao == null || transferAdminService == null) {
			result.setTotalCount(0);
			result.setItems(Collections.<AmbulanceVoucherItem>emptyList());
			return result;
		}

		String sendingFacility = transferAdminService.resolveOutboundFacilityName();
		if (StringUtils.isBlank(sendingFacility)) {
			sendingFacility = transferAdminService.resolveCurrentSendingFacilityName();
		}
		String ambulanceProviderFosaId = StringUtils.trimToNull(Context.getAdministrationService().getGlobalProperty(
				org.openmrs.module.transferapp.TransferAppConstants.GP_SENDING_FOSA_ID,
				org.openmrs.module.transferapp.TransferAppConstants.DEFAULT_SENDING_FOSA_ID));
		if (StringUtils.isBlank(sendingFacility) && StringUtils.isBlank(ambulanceProviderFosaId)) {
			result.setTotalCount(0);
			result.setItems(Collections.<AmbulanceVoucherItem>emptyList());
			return result;
		}

		List<Transfer> transfers = transferDao.getAmbulanceVoucherTransfers(
				StringUtils.trimToNull(sendingFacility), ambulanceProviderFosaId, rangeStart, rangeEnd, null, null);
		if (transfers == null || transfers.isEmpty()) {
			result.setTotalCount(0);
			result.setItems(Collections.<AmbulanceVoucherItem>emptyList());
			return result;
		}

		Integer sendingLocationId = transferAdminService.resolveCurrentSendingLocationId();
		BillingService billingService = resolveBillingService();
		List<AmbulanceVoucherItem> items = new ArrayList<AmbulanceVoucherItem>();
		int rowNumber = 1;
		for (Transfer transfer : transfers) {
			items.add(toItem(transfer, rowNumber++, sendingLocationId, billingService));
		}
		result.setTotalCount(items.size());
		result.setItems(items);
		return result;
	}

	@Override
	public AmbulanceVoucherPreview getVoucherPreview(String transferUuid) {
		if (StringUtils.isBlank(transferUuid) || transferDao == null) {
			throw new APIException("Transfer UUID is required");
		}
		Transfer transfer = transferDao.getTransferByUuid(transferUuid.trim());
		if (transfer == null || transfer.isVoided()) {
			throw new APIException("Transfer not found");
		}
		if (transfer.getAmbulanceConsommationId() == null) {
			throw new APIException("This transfer has no linked ambulance voucher");
		}

		Integer sendingLocationId = transferAdminService != null
				? transferAdminService.resolveCurrentSendingLocationId()
				: null;
		BillingService billingService = resolveBillingService();

		AmbulanceVoucherPreview preview = new AmbulanceVoucherPreview();
		preview.setTransferUuid(transfer.getUuid());
		preview.setConsommationId(transfer.getAmbulanceConsommationId());
		preview.setProvince(resolveProvince(transfer, sendingLocationId));
		preview.setDistrict(StringUtils.defaultString(transfer.getReceivingDistrict()));
		preview.setSectionHospital(StringUtils.defaultString(transfer.getSendingFacility()));

		// Persist province onto the transfer when preview resolves it from destination registry
		// so later voucher views keep the value without re-deriving.
		if (StringUtils.isNotBlank(preview.getProvince())
				&& StringUtils.isBlank(transfer.getReceivingProvince())
				&& transferDao != null) {
			transfer.setReceivingProvince(StringUtils.left(preview.getProvince(), 120));
			try {
				transferDao.saveTransfer(transfer);
			}
			catch (Exception ex) {
				log.warn("Unable to persist resolved province on transfer " + transfer.getUuid()
						+ ": " + ex.getMessage());
			}
		}

		Date transferDate = transfer.getDecisionToTransferAt() != null
				? transfer.getDecisionToTransferAt()
				: transfer.getDateCreated();
		preview.setDate(formatDateOnly(transferDate));
		preview.setDepartureTime(firstNonBlank(transfer.getDepartRefTime(),
				firstNonBlank(transfer.getAmbulanceCallTime(), transfer.getCallingTime())));
		preview.setPatientCount(Integer.valueOf(1));
		preview.setVoucherId(firstNonBlank(transfer.getEmrId(), transfer.getIdentifierValue()));
		preview.setDestination(resolveDestinationName(transfer, sendingLocationId));

		Integer distance = resolveConfiguredDistance(transfer, sendingLocationId);
		BigDecimal amount = null;
		if (billingService != null) {
			try {
				Consommation consommation = billingService.getConsommation(transfer.getAmbulanceConsommationId());
				BillTotals totals = resolveBillTotals(consommation);
				if (distance == null && totals.distance != null) {
					distance = totals.distance;
				}
				amount = totals.amount;
			}
			catch (Exception ex) {
				log.warn("Unable to load ambulance consommation " + transfer.getAmbulanceConsommationId()
						+ " for preview: " + ex.getMessage());
			}
		}
		preview.setDistanceKm(distance);
		if (amount != null) {
			preview.setAmount(amount.setScale(2, RoundingMode.HALF_UP));
		}

		preview.setPatientName(StringUtils.defaultString(transfer.getClientName()));
		String affiliation = null;
		if (patientInsuranceService != null && transfer.getPatient() != null) {
			affiliation = patientInsuranceService.resolveInsuranceCardNumber(transfer.getPatient());
		}
		preview.setAffiliationNumber(StringUtils.defaultString(affiliation));
		preview.setDriverName("");
		preview.setAccompanyingNurse(StringUtils.defaultString(transfer.getReferringProviderName()));
		preview.setArrivalDate("");
		preview.setArrivalTime("");
		preview.setCbhiAgentName("");
		preview.setReceivingClinicianName(StringUtils.defaultString(transfer.getStaffContactedName()));
		return preview;
	}

	private AmbulanceVoucherItem toItem(Transfer transfer, int rowNumber, Integer sendingLocationId,
			BillingService billingService) {
		AmbulanceVoucherItem item = new AmbulanceVoucherItem();
		item.setRowNumber(rowNumber);
		item.setTransferUuid(transfer.getUuid());
		item.setTransferDate(transfer.getDecisionToTransferAt() != null
				? transfer.getDecisionToTransferAt()
				: transfer.getDateCreated());
		item.setPatientUpid(firstNonBlank(transfer.getEmrId(), transfer.getIdentifierValue()));
		item.setPatientName(StringUtils.defaultString(transfer.getClientName()));
		item.setFromHospital(StringUtils.defaultString(transfer.getSendingFacility()));
		item.setDestinationHospital(resolveDestinationName(transfer, sendingLocationId));
		item.setAmbulanceConsommationId(transfer.getAmbulanceConsommationId());

		Integer distance = resolveConfiguredDistance(transfer, sendingLocationId);
		BigDecimal amount = null;
		if (billingService != null && transfer.getAmbulanceConsommationId() != null) {
			try {
				Consommation consommation = billingService.getConsommation(transfer.getAmbulanceConsommationId());
				BillTotals totals = resolveBillTotals(consommation);
				if (distance == null && totals.distance != null) {
					distance = totals.distance;
				}
				amount = totals.amount;
			}
			catch (Exception ex) {
				log.warn("Unable to load ambulance consommation " + transfer.getAmbulanceConsommationId()
						+ " for transfer " + transfer.getUuid() + ": " + ex.getMessage());
			}
		}
		item.setDistance(distance);
		if (amount != null) {
			item.setAmount(amount.setScale(2, RoundingMode.HALF_UP));
		}
		return item;
	}

	private String resolveDestinationName(Transfer transfer, Integer sendingLocationId) {
		String code = StringUtils.trimToNull(transfer.getReceivingFacilityCode());
		if (code != null && sendingLocationId != null) {
			String name = transferAdminService.resolveReceivingFacilityName(sendingLocationId, code);
			if (StringUtils.isNotBlank(name)) {
				return name.trim();
			}
		}
		return StringUtils.defaultString(code);
	}

	private Integer resolveConfiguredDistance(Transfer transfer, Integer sendingLocationId) {
		String code = StringUtils.trimToNull(transfer.getReceivingFacilityCode());
		if (code == null || sendingLocationId == null) {
			return null;
		}
		ReceivingFacility facility = transferAdminService.getReceivingFacilityByCode(sendingLocationId, code);
		if (facility == null || facility.getDistance() == null || facility.getDistance().intValue() <= 0) {
			return null;
		}
		return facility.getDistance();
	}

	private BillTotals resolveBillTotals(Consommation consommation) {
		BillTotals totals = new BillTotals();
		if (consommation == null || Boolean.TRUE.equals(consommation.getVoided())) {
			return totals;
		}
		BigDecimal sum = BigDecimal.ZERO;
		Integer quantityKm = null;
		Set<PatientServiceBill> items = consommation.getBillItems();
		if (items != null) {
			for (PatientServiceBill billItem : items) {
				if (billItem == null || Boolean.TRUE.equals(billItem.getVoided())) {
					continue;
				}
				BigDecimal qty = billItem.getQuantity();
				BigDecimal unit = billItem.getUnitPrice();
				if (qty != null && unit != null) {
					sum = sum.add(qty.multiply(unit));
				}
				if (quantityKm == null && qty != null) {
					quantityKm = Integer.valueOf(qty.intValue());
				}
			}
		}
		if (sum.compareTo(BigDecimal.ZERO) > 0) {
			totals.amount = sum;
		}
		else if (consommation.getPatientBill() != null && consommation.getPatientBill().getAmount() != null) {
			totals.amount = consommation.getPatientBill().getAmount();
		}
		totals.distance = quantityKm;
		return totals;
	}

	private String firstNonBlank(String first, String second) {
		String value = StringUtils.trimToNull(first);
		if (value != null) {
			return value;
		}
		return StringUtils.defaultString(StringUtils.trimToNull(second));
	}

	private String formatDateOnly(Date date) {
		if (date == null) {
			return "";
		}
		return new SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH).format(date);
	}

	private static Date startOfDay(Date date) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		return calendar.getTime();
	}

	private static Date endOfDay(Date date) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		calendar.set(Calendar.HOUR_OF_DAY, 23);
		calendar.set(Calendar.MINUTE, 59);
		calendar.set(Calendar.SECOND, 59);
		calendar.set(Calendar.MILLISECOND, 999);
		return calendar.getTime();
	}

	protected BillingService resolveBillingService() {
		try {
			return Context.getService(BillingService.class);
		}
		catch (Exception ex) {
			return null;
		}
	}

	private static class BillTotals {
		private Integer distance;
		private BigDecimal amount;
	}

}
