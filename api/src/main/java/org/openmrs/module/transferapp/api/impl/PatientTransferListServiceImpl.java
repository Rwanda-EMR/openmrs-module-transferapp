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
import org.openmrs.Patient;
import org.openmrs.module.transferapp.api.MaternityTransferService;
import org.openmrs.module.transferapp.api.NeonatalTransferService;
import org.openmrs.module.transferapp.api.PatientTransferListService;
import org.openmrs.module.transferapp.api.TransferAdminService;
import org.openmrs.module.transferapp.api.TransferService;
import org.openmrs.module.transferapp.model.MaternityTransfer;
import org.openmrs.module.transferapp.model.NeonatalTransfer;
import org.openmrs.module.transferapp.model.PatientTransferListItem;
import org.openmrs.module.transferapp.model.Transfer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public class PatientTransferListServiceImpl implements PatientTransferListService {

	private static final String FORM_TYPE_EXTERNAL = "External";

	private static final String FORM_TYPE_MATERNITY = "Maternity";

	private static final String FORM_TYPE_NEONATAL = "Neonatal";

	private TransferService transferService;

	private MaternityTransferService maternityTransferService;

	private NeonatalTransferService neonatalTransferService;

	private TransferAdminService transferAdminService;

	public void setTransferService(TransferService transferService) {
		this.transferService = transferService;
	}

	public void setMaternityTransferService(MaternityTransferService maternityTransferService) {
		this.maternityTransferService = maternityTransferService;
	}

	public void setNeonatalTransferService(NeonatalTransferService neonatalTransferService) {
		this.neonatalTransferService = neonatalTransferService;
	}

	public void setTransferAdminService(TransferAdminService transferAdminService) {
		this.transferAdminService = transferAdminService;
	}

	@Override
	public List<PatientTransferListItem> getPatientTransfers(Patient patient) {
		return getPatientTransfers(patient, null);
	}

	@Override
	public List<PatientTransferListItem> getPatientTransfers(Patient patient, Integer limit) {
		List<PatientTransferListItem> items = new ArrayList<PatientTransferListItem>();

		List<Transfer> transfers = transferService.getTransfersByPatient(patient, limit);
		if (transfers != null) {
			for (Transfer transfer : transfers) {
				items.add(toItem(transfer));
			}
		}

		if (maternityTransferService != null) {
			List<MaternityTransfer> maternityTransfers = maternityTransferService.getMaternityTransfersByPatient(patient, limit);
			if (maternityTransfers != null) {
				for (MaternityTransfer maternityTransfer : maternityTransfers) {
					items.add(toItem(maternityTransfer));
				}
			}
		}

		if (neonatalTransferService != null) {
			List<NeonatalTransfer> neonatalTransfers = neonatalTransferService.getNeonatalTransfersByPatient(patient, limit);
			if (neonatalTransfers != null) {
				for (NeonatalTransfer neonatalTransfer : neonatalTransfers) {
					items.add(toItem(neonatalTransfer));
				}
			}
		}

		Collections.sort(items, new Comparator<PatientTransferListItem>() {
			@Override
			public int compare(PatientTransferListItem a, PatientTransferListItem b) {
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

		if (limit != null && limit.intValue() > 0 && items.size() > limit.intValue()) {
			items = items.subList(0, limit.intValue());
		}

		return items;
	}

	@Override
	public int countPatientTransfers(Patient patient) {
		int total = transferService.countTransfersByPatient(patient);
		if (maternityTransferService != null) {
			total += maternityTransferService.countMaternityTransfersByPatient(patient);
		}
		if (neonatalTransferService != null) {
			total += neonatalTransferService.countNeonatalTransfersByPatient(patient);
		}
		return total;
	}

	private PatientTransferListItem toItem(Transfer transfer) {
		PatientTransferListItem item = new PatientTransferListItem();
		item.setId(transfer.getUuid());
		item.setTransferDate(transfer.getDecisionToTransferAt() != null
				? transfer.getDecisionToTransferAt()
				: transfer.getDateCreated());
		// Destination only: receiving facility selected on the transfer form.
		item.setToFacility(resolveSelectedDestinationName(transfer.getReceivingFacilityCode()));
		item.setService(StringUtils.defaultString(transfer.getReceivingService()));
		item.setClientName(transfer.getClientName());
		item.setEmrId(transfer.getEmrId());
		item.setHieSent(transfer.isSentToHie());
		item.setFormType(FORM_TYPE_EXTERNAL);
		return item;
	}

	private PatientTransferListItem toItem(MaternityTransfer transfer) {
		PatientTransferListItem item = new PatientTransferListItem();
		item.setId(transfer.getUuid());
		item.setTransferDate(transfer.getDecisionToTransferAt() != null
				? transfer.getDecisionToTransferAt()
				: transfer.getDateCreated());
		item.setToFacility(resolveSelectedDestinationName(transfer.getReceivingFacilityCode()));
		item.setService(StringUtils.defaultString(transfer.getReceivingService()));
		item.setClientName(transfer.getClientName());
		item.setEmrId(StringUtils.defaultString(transfer.getSerialNumberEmr()));
		item.setHieSent(transfer.isSentToHie());
		item.setFormType(FORM_TYPE_MATERNITY);
		return item;
	}

	private PatientTransferListItem toItem(NeonatalTransfer transfer) {
		PatientTransferListItem item = new PatientTransferListItem();
		item.setId(transfer.getUuid());
		item.setTransferDate(transfer.getDecisionToTransferAt() != null
				? transfer.getDecisionToTransferAt()
				: transfer.getDateCreated());
		item.setToFacility(resolveSelectedDestinationName(transfer.getReceivingFacilityCode()));
		item.setService(StringUtils.defaultString(transfer.getReceivingService()));
		item.setClientName(transfer.getBabyName());
		item.setEmrId("");
		item.setHieSent(transfer.isSentToHie());
		item.setFormType(FORM_TYPE_NEONATAL);
		return item;
	}

	/**
	 * Display name for the destination facility chosen on the form ({@code receivingFacilityCode}).
	 * Never uses sending / outbound facility name.
	 */
	private String resolveSelectedDestinationName(String receivingFacilityCode) {
		if (StringUtils.isBlank(receivingFacilityCode)) {
			return "";
		}
		String code = receivingFacilityCode.trim();
		if (transferAdminService == null) {
			return code;
		}
		Integer sendingLocationId = transferAdminService.resolveCurrentSendingLocationId();
		String name = transferAdminService.resolveReceivingFacilityName(sendingLocationId, code);
		return StringUtils.isNotBlank(name) ? name : code;
	}

}
