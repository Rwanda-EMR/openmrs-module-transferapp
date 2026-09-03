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
import org.openmrs.module.transferapp.api.FacilityTransferRecordsService;
import org.openmrs.module.transferapp.api.TransferAdminService;
import org.openmrs.module.transferapp.api.dao.MaternityTransferDao;
import org.openmrs.module.transferapp.api.dao.NeonatalTransferDao;
import org.openmrs.module.transferapp.api.dao.TransferDao;
import org.openmrs.module.transferapp.model.FacilityTransferRecordItem;
import org.openmrs.module.transferapp.model.MaternityTransfer;
import org.openmrs.module.transferapp.model.NeonatalTransfer;
import org.openmrs.module.transferapp.model.Transfer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public class FacilityTransferRecordsServiceImpl implements FacilityTransferRecordsService {

	private static final String FORM_TYPE_EXTERNAL = "External";

	private static final String FORM_TYPE_MATERNITY = "Maternity";

	private static final String FORM_TYPE_NEONATAL = "Neonatal";

	private TransferDao transferDao;

	private MaternityTransferDao maternityTransferDao;

	private NeonatalTransferDao neonatalTransferDao;

	private TransferAdminService transferAdminService;

	public void setTransferDao(TransferDao transferDao) {
		this.transferDao = transferDao;
	}

	public void setMaternityTransferDao(MaternityTransferDao maternityTransferDao) {
		this.maternityTransferDao = maternityTransferDao;
	}

	public void setNeonatalTransferDao(NeonatalTransferDao neonatalTransferDao) {
		this.neonatalTransferDao = neonatalTransferDao;
	}

	public void setTransferAdminService(TransferAdminService transferAdminService) {
		this.transferAdminService = transferAdminService;
	}

	@Override
	public List<FacilityTransferRecordItem> getOutboundTransferRecords(Integer patientId, Date startDate, Date endDate,
			String receivingFacilityCode) {
		return getOutboundTransferRecords(patientId, startDate, endDate, receivingFacilityCode, null);
	}

	@Override
	public List<FacilityTransferRecordItem> getOutboundTransferRecords(Integer patientId, Date startDate, Date endDate,
			String receivingFacilityCode, String formType) {
		if (transferAdminService == null) {
			return Collections.emptyList();
		}

		String sendingFacility = transferAdminService.resolveCurrentSendingFacilityName();
		if (StringUtils.isBlank(sendingFacility)) {
			return Collections.emptyList();
		}

		String normalizedFormType = StringUtils.trimToNull(formType);
		String trimmedFacility = sendingFacility.trim();
		String trimmedReceivingFacilityCode = StringUtils.trimToNull(receivingFacilityCode);

		List<FacilityTransferRecordItem> items = new ArrayList<FacilityTransferRecordItem>();

		if (transferDao != null && (normalizedFormType == null || FORM_TYPE_EXTERNAL.equalsIgnoreCase(normalizedFormType))) {
			List<Transfer> transfers = transferDao.getOutboundTransfersBySendingFacility(
					trimmedFacility, patientId, null, startDate, endDate, trimmedReceivingFacilityCode);
			if (transfers != null) {
				for (Transfer transfer : transfers) {
					items.add(toRecordItem(transfer));
				}
			}
		}

		if (maternityTransferDao != null
				&& (normalizedFormType == null || FORM_TYPE_MATERNITY.equalsIgnoreCase(normalizedFormType))) {
			List<MaternityTransfer> maternityTransfers = maternityTransferDao.getOutboundMaternityTransfersBySendingFacility(
					trimmedFacility, patientId, null, startDate, endDate, trimmedReceivingFacilityCode);
			if (maternityTransfers != null) {
				for (MaternityTransfer maternityTransfer : maternityTransfers) {
					items.add(toRecordItem(maternityTransfer));
				}
			}
		}

		if (neonatalTransferDao != null
				&& (normalizedFormType == null || FORM_TYPE_NEONATAL.equalsIgnoreCase(normalizedFormType))) {
			List<NeonatalTransfer> neonatalTransfers = neonatalTransferDao.getOutboundNeonatalTransfersBySendingFacility(
					trimmedFacility, patientId, null, startDate, endDate, trimmedReceivingFacilityCode);
			if (neonatalTransfers != null) {
				for (NeonatalTransfer neonatalTransfer : neonatalTransfers) {
					items.add(toRecordItem(neonatalTransfer));
				}
			}
		}

		Collections.sort(items, new Comparator<FacilityTransferRecordItem>() {
			@Override
			public int compare(FacilityTransferRecordItem a, FacilityTransferRecordItem b) {
				Date dateA = a.getTransferDate();
				Date dateB = b.getTransferDate();
				if (dateA == null && dateB == null) {
					return 0;
				}
				if (dateA == null) {
					return 1;
				}
				if (dateB == null) {
					return -1;
				}
				return dateB.compareTo(dateA);
			}
		});

		return items;
	}

	private FacilityTransferRecordItem toRecordItem(Transfer transfer) {
		FacilityTransferRecordItem item = new FacilityTransferRecordItem();
		item.setId(transfer.getUuid());
		item.setTransferDate(transfer.getDecisionToTransferAt() != null
				? transfer.getDecisionToTransferAt()
				: transfer.getDateCreated());
		if (transfer.getPatient() != null) {
			item.setPatientId(transfer.getPatient().getPatientId());
		}
		item.setClientName(StringUtils.defaultString(transfer.getClientName()));
		item.setEmrId(StringUtils.defaultString(transfer.getEmrId()));
		item.setReceivingFacility(resolveReceivingFacilityLabel(transfer.getReceivingFacilityCode()));
		item.setService(StringUtils.defaultString(transfer.getReceivingService()));
		item.setHieSent(transfer.isSentToHie());
		item.setFormType(FORM_TYPE_EXTERNAL);
		return item;
	}

	private FacilityTransferRecordItem toRecordItem(MaternityTransfer transfer) {
		FacilityTransferRecordItem item = new FacilityTransferRecordItem();
		item.setId(transfer.getUuid());
		item.setTransferDate(transfer.getDecisionToTransferAt() != null
				? transfer.getDecisionToTransferAt()
				: transfer.getDateCreated());
		if (transfer.getPatient() != null) {
			item.setPatientId(transfer.getPatient().getPatientId());
		}
		item.setClientName(StringUtils.defaultString(transfer.getClientName()));
		item.setEmrId(StringUtils.defaultString(transfer.getSerialNumberEmr()));
		item.setReceivingFacility(resolveReceivingFacilityLabel(transfer.getReceivingFacilityCode()));
		item.setService(StringUtils.defaultString(transfer.getReceivingService()));
		item.setHieSent(transfer.isSentToHie());
		item.setFormType(FORM_TYPE_MATERNITY);
		return item;
	}

	private FacilityTransferRecordItem toRecordItem(NeonatalTransfer transfer) {
		FacilityTransferRecordItem item = new FacilityTransferRecordItem();
		item.setId(transfer.getUuid());
		item.setTransferDate(transfer.getDecisionToTransferAt() != null
				? transfer.getDecisionToTransferAt()
				: transfer.getDateCreated());
		if (transfer.getPatient() != null) {
			item.setPatientId(transfer.getPatient().getPatientId());
		}
		item.setClientName(StringUtils.defaultString(transfer.getBabyName()));
		item.setEmrId("");
		item.setReceivingFacility(resolveReceivingFacilityLabel(transfer.getReceivingFacilityCode()));
		item.setService(StringUtils.defaultString(transfer.getReceivingService()));
		item.setHieSent(transfer.isSentToHie());
		item.setFormType(FORM_TYPE_NEONATAL);
		return item;
	}

	private String resolveReceivingFacilityLabel(String facilityCode) {
		if (StringUtils.isBlank(facilityCode)) {
			return "";
		}
		if (transferAdminService != null) {
			Integer sendingLocationId = transferAdminService.resolveCurrentSendingLocationId();
			String label = transferAdminService.resolveReceivingFacilityName(sendingLocationId, facilityCode);
			if (!facilityCode.equals(label)) {
				return label;
			}
		}
		return defaultFacilityLabel(facilityCode);
	}

	private static String defaultFacilityLabel(String facilityCode) {
		if ("KUTH".equals(facilityCode)) {
			return "Kigali University Teaching Hospital";
		}
		if ("RUHENGERI".equals(facilityCode)) {
			return "Ruhengeri District Hospital";
		}
		if ("BUTARO".equals(facilityCode)) {
			return "Butaro District Hospital";
		}
		if ("KFH".equals(facilityCode)) {
			return "King Faisal Hospital";
		}
		return facilityCode;
	}

}
