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
import org.openmrs.Concept;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.Person;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.ConceptService;
import org.openmrs.api.ObsService;
import org.openmrs.api.PatientService;
import org.openmrs.api.context.Context;
import org.openmrs.module.transferapp.TransferAppConstants;
import org.openmrs.module.transferapp.api.CounterReferralStatistics;
import org.openmrs.module.transferapp.api.MissingCounterReferralItem;
import org.openmrs.module.transferapp.api.TransferAdminService;
import org.openmrs.module.transferapp.api.TransferDashboardService;
import org.openmrs.module.transferapp.api.TransferPatientSnapshotResolver;
import org.openmrs.module.transferapp.api.TransferReceivedStatistics;
import org.openmrs.module.transferapp.api.dao.MaternityTransferDao;
import org.openmrs.module.transferapp.api.dao.NeonatalTransferDao;
import org.openmrs.module.transferapp.api.dao.TransferDao;
import org.openmrs.module.transferapp.api.dao.TransferReferralFeedbackDao;
import org.openmrs.module.transferapp.model.MaternityTransfer;
import org.openmrs.module.transferapp.model.NeonatalTransfer;
import org.openmrs.module.transferapp.model.Transfer;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Default implementation of {@link TransferDashboardService}.
 */
public class TransferDashboardServiceImpl implements TransferDashboardService {

	private static final Pattern UUID_PATTERN = Pattern.compile(
			"^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

	private TransferDao transferDao;

	private MaternityTransferDao maternityTransferDao;

	private NeonatalTransferDao neonatalTransferDao;

	private TransferAdminService transferAdminService;

	private TransferReferralFeedbackDao transferReferralFeedbackDao;

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

	public void setTransferReferralFeedbackDao(TransferReferralFeedbackDao transferReferralFeedbackDao) {
		this.transferReferralFeedbackDao = transferReferralFeedbackDao;
	}

	@Override
	public TransferReceivedStatistics getReceivedTransferStatistics(Date periodStart, Date periodEnd) {
		TransferReceivedStatistics statistics = new TransferReceivedStatistics();

		Concept concept = getReceivedTransferConcept();
		if (concept == null) {
			return statistics;
		}

		Date startOfToday = startOfToday();
		Date startOfWeek = startOfThisWeek();

		statistics.setToday(countObservations(concept, startOfToday, null));
		statistics.setThisWeek(countObservations(concept, startOfWeek, null));
		statistics.setTotal(countObservations(concept, periodStart, periodEnd));
		statistics.setPending(0);
		statistics.setMissingCounterReferral(listMissingCounterReferrals(periodStart, periodEnd).size());

		return statistics;
	}

	@Override
	public TransferReceivedStatistics getSentTransferStatistics(Date periodStart, Date periodEnd) {
		TransferReceivedStatistics statistics = new TransferReceivedStatistics();

		String sendingFacility = transferAdminService != null
				? transferAdminService.resolveCurrentSendingFacilityName()
				: null;
		if (StringUtils.isBlank(sendingFacility)) {
			return statistics;
		}

		String facilityName = sendingFacility.trim();
		Date startOfToday = startOfToday();
		Date startOfWeek = startOfThisWeek();

		int maternityToday = maternityTransferDao != null
				? maternityTransferDao.countOutboundMaternityTransfers(facilityName, startOfToday) : 0;
		int maternityThisWeek = maternityTransferDao != null
				? maternityTransferDao.countOutboundMaternityTransfers(facilityName, startOfWeek) : 0;
		int maternityPeriod = maternityTransferDao != null
				? maternityTransferDao.countOutboundMaternityTransfers(facilityName, periodStart, periodEnd, null) : 0;
		int maternityPending = maternityTransferDao != null
				? maternityTransferDao.countOutboundMaternityTransfers(facilityName, null, Boolean.FALSE) : 0;

		int neonatalToday = neonatalTransferDao != null
				? neonatalTransferDao.countOutboundNeonatalTransfers(facilityName, startOfToday) : 0;
		int neonatalThisWeek = neonatalTransferDao != null
				? neonatalTransferDao.countOutboundNeonatalTransfers(facilityName, startOfWeek) : 0;
		int neonatalPeriod = neonatalTransferDao != null
				? neonatalTransferDao.countOutboundNeonatalTransfers(facilityName, periodStart, periodEnd, null) : 0;
		int neonatalPending = neonatalTransferDao != null
				? neonatalTransferDao.countOutboundNeonatalTransfers(facilityName, null, Boolean.FALSE) : 0;

		statistics.setToday(transferDao.countOutboundTransfers(facilityName, startOfToday, null)
				+ maternityToday + neonatalToday);
		statistics.setThisWeek(transferDao.countOutboundTransfers(facilityName, startOfWeek, null)
				+ maternityThisWeek + neonatalThisWeek);
		statistics.setTotal(transferDao.countOutboundTransfers(facilityName, periodStart, periodEnd, null)
				+ maternityPeriod + neonatalPeriod);
		statistics.setPending(transferDao.countOutboundTransfers(facilityName, null, Boolean.FALSE)
				+ maternityPending + neonatalPending);
		statistics.setMissingCounterReferral(listMissingOutboundCounterReferrals(periodStart, periodEnd).size());

		return statistics;
	}

	@Override
	public TransferReceivedStatistics getAmbulanceVoucherStatistics(Date periodStart, Date periodEnd) {
		TransferReceivedStatistics statistics = new TransferReceivedStatistics();
		if (transferDao == null || transferAdminService == null) {
			return statistics;
		}

		String sendingFacility = transferAdminService.resolveOutboundFacilityName();
		if (StringUtils.isBlank(sendingFacility)) {
			sendingFacility = transferAdminService.resolveCurrentSendingFacilityName();
		}
		String ambulanceProviderFosaId = StringUtils.trimToNull(getAdministrationService().getGlobalProperty(
				TransferAppConstants.GP_SENDING_FOSA_ID,
				TransferAppConstants.DEFAULT_SENDING_FOSA_ID));
		sendingFacility = StringUtils.trimToNull(sendingFacility);
		if (sendingFacility == null && ambulanceProviderFosaId == null) {
			return statistics;
		}

		Date startOfToday = startOfToday();
		Date startOfWeek = startOfThisWeek();
		statistics.setToday(transferDao.countAmbulanceVoucherTransfers(
				sendingFacility, ambulanceProviderFosaId, startOfToday, null));
		statistics.setThisWeek(transferDao.countAmbulanceVoucherTransfers(
				sendingFacility, ambulanceProviderFosaId, startOfWeek, null));
		statistics.setTotal(transferDao.countAmbulanceVoucherTransfers(
				sendingFacility, ambulanceProviderFosaId, periodStart, periodEnd));
		return statistics;
	}

	@Override
	public CounterReferralStatistics getCounterReferralStatistics(Date periodStart, Date periodEnd) {
		CounterReferralStatistics statistics = new CounterReferralStatistics();
		if (transferReferralFeedbackDao != null) {
			statistics.setSent(transferReferralFeedbackDao.countFeedback(true, periodStart, periodEnd));
		}

		String sendingFacility = transferAdminService != null
				? transferAdminService.resolveCurrentSendingFacilityName()
				: null;
		if (StringUtils.isBlank(sendingFacility) || transferDao == null) {
			return statistics;
		}

		List<String> outboundIds = transferDao.listOutboundHieTransferIds(sendingFacility.trim());
		if (outboundIds == null || outboundIds.isEmpty()) {
			return statistics;
		}

		Set<String> outboundIdSet = new HashSet<String>();
		for (String id : outboundIds) {
			if (StringUtils.isNotBlank(id)) {
				outboundIdSet.add(id.trim());
			}
		}
		if (outboundIdSet.isEmpty()) {
			return statistics;
		}

		Concept concept = getReceivedTransferConcept();
		if (concept == null) {
			return statistics;
		}

		List<Obs> observations = getObsService().getObservations(
				null,
				null,
				Collections.singletonList(concept),
				null,
				null,
				null,
				null,
				null,
				null,
				periodStart,
				periodEnd,
				false);
		if (observations == null || observations.isEmpty()) {
			return statistics;
		}

		int received = 0;
		Set<String> counted = new HashSet<String>();
		for (Obs observation : observations) {
			if (observation == null || StringUtils.isBlank(observation.getValueText())) {
				continue;
			}
			String transferId = observation.getValueText().trim();
			if (transferId.startsWith("{") && transferId.endsWith("}") && transferId.length() > 2) {
				transferId = transferId.substring(1, transferId.length() - 1).trim();
			}
			if (!outboundIdSet.contains(transferId) || !counted.add(transferId)) {
				continue;
			}
			received++;
		}
		statistics.setReceived(received);
		return statistics;
	}

	@Override
	public List<MissingCounterReferralItem> listMissingCounterReferrals(Date periodStart, Date periodEnd) {
		List<MissingCounterReferralItem> items = new ArrayList<MissingCounterReferralItem>();
		Concept concept = getReceivedTransferConcept();
		if (concept == null) {
			return items;
		}

		List<Obs> observations = getObsService().getObservations(
				null,
				null,
				Collections.singletonList(concept),
				null,
				null,
				null,
				null,
				null,
				null,
				periodStart,
				periodEnd,
				false);
		if (observations == null || observations.isEmpty()) {
			return items;
		}

		Set<String> sentKeys = transferReferralFeedbackDao != null
				? transferReferralFeedbackDao.listHieSentFeedbackKeys()
				: Collections.<String>emptySet();
		if (sentKeys == null) {
			sentKeys = Collections.emptySet();
		}

		TransferPatientSnapshotResolver snapshotResolver = new TransferPatientSnapshotResolver();
		Set<String> listed = new HashSet<String>();
		PatientService patientService = getPatientService();

		for (Obs observation : observations) {
			if (observation == null || Boolean.TRUE.equals(observation.getVoided())) {
				continue;
			}
			String transferId = normalizeTransferId(observation.getValueText());
			if (!isValidUuid(transferId)) {
				continue;
			}

			Patient patient = resolvePatient(observation, patientService);
			if (patient == null || patient.getPatientId() == null) {
				continue;
			}

			String key = patient.getPatientId() + "|" + transferId;
			if (sentKeys.contains(key) || !listed.add(key)) {
				continue;
			}

			MissingCounterReferralItem item = new MissingCounterReferralItem();
			item.setPatientId(patient.getPatientId());
			item.setHieTransferId(transferId);
			item.setReceivedDate(observation.getObsDatetime() != null
					? observation.getObsDatetime()
					: observation.getDateCreated());
			if (patient.getPersonName() != null) {
				item.setPatientName(patient.getPersonName().getFullName());
			}
			item.setUpid(StringUtils.defaultString(snapshotResolver.resolveUpid(patient)));
			items.add(item);
		}

		Collections.sort(items, new Comparator<MissingCounterReferralItem>() {
			@Override
			public int compare(MissingCounterReferralItem left, MissingCounterReferralItem right) {
				Date leftDate = left != null ? left.getReceivedDate() : null;
				Date rightDate = right != null ? right.getReceivedDate() : null;
				if (leftDate == null && rightDate == null) {
					return 0;
				}
				if (leftDate == null) {
					return 1;
				}
				if (rightDate == null) {
					return -1;
				}
				return rightDate.compareTo(leftDate);
			}
		});
		return items;
	}

	@Override
	public List<MissingCounterReferralItem> listMissingOutboundCounterReferrals(Date periodStart, Date periodEnd) {
		List<MissingCounterReferralItem> items = new ArrayList<MissingCounterReferralItem>();
		String sendingFacility = transferAdminService != null
				? transferAdminService.resolveCurrentSendingFacilityName()
				: null;
		if (StringUtils.isBlank(sendingFacility) || transferDao == null) {
			return items;
		}

		Set<String> feedbackKeys = transferReferralFeedbackDao != null
				? transferReferralFeedbackDao.listFeedbackKeys()
				: Collections.<String>emptySet();
		if (feedbackKeys == null) {
			feedbackKeys = Collections.emptySet();
		}

		TransferPatientSnapshotResolver snapshotResolver = new TransferPatientSnapshotResolver();
		Set<String> listed = new HashSet<String>();
		String facilityName = sendingFacility.trim();

		List<Transfer> outbound = transferDao.getOutboundTransfersBySendingFacility(
				facilityName, null, null, periodStart, periodEnd, null);
		if (outbound != null) {
			for (Transfer transfer : outbound) {
				MissingCounterReferralItem item = toMissingOutboundItem(transfer, feedbackKeys, listed,
						snapshotResolver);
				if (item != null) {
					items.add(item);
				}
			}
		}

		if (maternityTransferDao != null) {
			List<MaternityTransfer> maternity = maternityTransferDao.getOutboundMaternityTransfersBySendingFacility(
					facilityName, null, null, periodStart, periodEnd, null);
			if (maternity != null) {
				for (MaternityTransfer transfer : maternity) {
					MissingCounterReferralItem item = toMissingOutboundMaternityItem(transfer, feedbackKeys, listed,
							snapshotResolver);
					if (item != null) {
						items.add(item);
					}
				}
			}
		}

		if (neonatalTransferDao != null) {
			List<NeonatalTransfer> neonatal = neonatalTransferDao.getOutboundNeonatalTransfersBySendingFacility(
					facilityName, null, null, periodStart, periodEnd, null);
			if (neonatal != null) {
				for (NeonatalTransfer transfer : neonatal) {
					MissingCounterReferralItem item = toMissingOutboundNeonatalItem(transfer, feedbackKeys, listed,
							snapshotResolver);
					if (item != null) {
						items.add(item);
					}
				}
			}
		}

		Collections.sort(items, new Comparator<MissingCounterReferralItem>() {
			@Override
			public int compare(MissingCounterReferralItem left, MissingCounterReferralItem right) {
				Date leftDate = left != null ? left.getReceivedDate() : null;
				Date rightDate = right != null ? right.getReceivedDate() : null;
				if (leftDate == null && rightDate == null) {
					return 0;
				}
				if (leftDate == null) {
					return 1;
				}
				if (rightDate == null) {
					return -1;
				}
				return rightDate.compareTo(leftDate);
			}
		});
		return items;
	}

	@Override
	public Concept getReceivedTransferConcept() {
		String conceptUuid = getAdministrationService().getGlobalProperty(
				TransferAppConstants.GP_RECEIVED_TRANSFER_CONCEPT_UUID,
				TransferAppConstants.DEFAULT_RECEIVED_TRANSFER_CONCEPT_UUID);

		if (StringUtils.isBlank(conceptUuid)) {
			return null;
		}

		return getConceptService().getConceptByUuid(conceptUuid.trim());
	}

	private MissingCounterReferralItem toMissingOutboundItem(Transfer transfer, Set<String> feedbackKeys,
			Set<String> listed, TransferPatientSnapshotResolver snapshotResolver) {
		if (transfer == null || !transfer.isSentToHie() || transfer.isReceivedFromHie()) {
			return null;
		}
		String transferId = StringUtils.trimToNull(transfer.getHieTransferId());
		Patient patient = transfer.getPatient();
		if (transferId == null || patient == null || patient.getPatientId() == null) {
			return null;
		}
		String key = patient.getPatientId() + "|" + transferId;
		if (feedbackKeys.contains(key) || !listed.add(key)) {
			return null;
		}
		MissingCounterReferralItem item = new MissingCounterReferralItem();
		item.setPatientId(patient.getPatientId());
		item.setHieTransferId(transferId);
		item.setReceivedDate(transfer.getDecisionToTransferAt() != null
				? transfer.getDecisionToTransferAt()
				: transfer.getDateCreated());
		item.setPatientName(StringUtils.defaultString(transfer.getClientName()));
		if (StringUtils.isBlank(item.getPatientName()) && patient.getPersonName() != null) {
			item.setPatientName(patient.getPersonName().getFullName());
		}
		item.setUpid(StringUtils.defaultString(firstNonBlank(transfer.getEmrId(),
				snapshotResolver.resolveUpid(patient))));
		item.setDestinationFacility(resolveDestinationName(transfer.getReceivingFacilityCode()));
		item.setService(StringUtils.defaultString(transfer.getReceivingService()));
		item.setFormType("External");
		item.setInsuranceType(formatInsuranceType(transfer.getHealthInsuranceType(),
				transfer.getHealthInsuranceOther()));
		return item;
	}

	private MissingCounterReferralItem toMissingOutboundMaternityItem(MaternityTransfer transfer,
			Set<String> feedbackKeys, Set<String> listed, TransferPatientSnapshotResolver snapshotResolver) {
		if (transfer == null || !transfer.isSentToHie()) {
			return null;
		}
		String transferId = StringUtils.trimToNull(transfer.getHieTransferId());
		Patient patient = transfer.getPatient();
		if (transferId == null || patient == null || patient.getPatientId() == null) {
			return null;
		}
		String key = patient.getPatientId() + "|" + transferId;
		if (feedbackKeys.contains(key) || !listed.add(key)) {
			return null;
		}
		MissingCounterReferralItem item = new MissingCounterReferralItem();
		item.setPatientId(patient.getPatientId());
		item.setHieTransferId(transferId);
		item.setReceivedDate(transfer.getDecisionToTransferAt() != null
				? transfer.getDecisionToTransferAt()
				: transfer.getDateCreated());
		item.setPatientName(StringUtils.defaultString(transfer.getClientName()));
		if (StringUtils.isBlank(item.getPatientName()) && patient.getPersonName() != null) {
			item.setPatientName(patient.getPersonName().getFullName());
		}
		item.setUpid(StringUtils.defaultString(snapshotResolver.resolveUpid(patient)));
		item.setDestinationFacility(resolveDestinationName(transfer.getReceivingFacilityCode()));
		item.setService(StringUtils.defaultString(transfer.getReceivingService()));
		item.setFormType("Maternity");
		item.setInsuranceType(formatInsuranceType(transfer.getHealthInsuranceType(),
				transfer.getHealthInsuranceOther()));
		return item;
	}

	private MissingCounterReferralItem toMissingOutboundNeonatalItem(NeonatalTransfer transfer,
			Set<String> feedbackKeys, Set<String> listed, TransferPatientSnapshotResolver snapshotResolver) {
		if (transfer == null || !transfer.isSentToHie()) {
			return null;
		}
		String transferId = StringUtils.trimToNull(transfer.getHieTransferId());
		Patient patient = transfer.getPatient();
		if (transferId == null || patient == null || patient.getPatientId() == null) {
			return null;
		}
		String key = patient.getPatientId() + "|" + transferId;
		if (feedbackKeys.contains(key) || !listed.add(key)) {
			return null;
		}
		MissingCounterReferralItem item = new MissingCounterReferralItem();
		item.setPatientId(patient.getPatientId());
		item.setHieTransferId(transferId);
		item.setReceivedDate(transfer.getDecisionToTransferAt() != null
				? transfer.getDecisionToTransferAt()
				: transfer.getDateCreated());
		item.setPatientName(StringUtils.defaultString(transfer.getBabyName()));
		if (StringUtils.isBlank(item.getPatientName()) && patient.getPersonName() != null) {
			item.setPatientName(patient.getPersonName().getFullName());
		}
		item.setUpid(StringUtils.defaultString(snapshotResolver.resolveUpid(patient)));
		item.setDestinationFacility(resolveDestinationName(transfer.getReceivingFacilityCode()));
		item.setService(StringUtils.defaultString(transfer.getReceivingService()));
		item.setFormType("Neonatal");
		item.setInsuranceType("");
		return item;
	}

	private String formatInsuranceType(String insuranceType, String insuranceOther) {
		String type = StringUtils.trimToNull(insuranceType);
		if (type == null) {
			return "";
		}
		String other = StringUtils.trimToNull(insuranceOther);
		if (other != null && ("OTHER".equalsIgnoreCase(type) || "Other".equalsIgnoreCase(type))) {
			return "Other (" + other + ")";
		}
		return type;
	}

	private String resolveDestinationName(String receivingFacilityCode) {
		if (StringUtils.isBlank(receivingFacilityCode)) {
			return "";
		}
		String code = receivingFacilityCode.trim();
		if (transferAdminService == null) {
			return code;
		}
		Integer sendingLocationId = transferAdminService.resolveCurrentSendingLocationId();
		if (sendingLocationId == null) {
			return code;
		}
		org.openmrs.module.transferapp.model.ReceivingFacility facility =
				transferAdminService.getReceivingFacilityByCode(sendingLocationId, code);
		if (facility != null && StringUtils.isNotBlank(facility.getFacilityName())) {
			return facility.getFacilityName().trim();
		}
		return code;
	}

	private String firstNonBlank(String first, String second) {
		if (StringUtils.isNotBlank(first)) {
			return first.trim();
		}
		if (StringUtils.isNotBlank(second)) {
			return second.trim();
		}
		return null;
	}

	private Patient resolvePatient(Obs observation, PatientService patientService) {
		if (observation == null || patientService == null) {
			return null;
		}
		Person person = observation.getPerson();
		if (person instanceof Patient) {
			return (Patient) person;
		}
		Integer personId = observation.getPersonId();
		if (personId == null && person != null) {
			personId = person.getPersonId();
		}
		if (personId == null) {
			return null;
		}
		return patientService.getPatient(personId);
	}

	private String normalizeTransferId(String value) {
		if (StringUtils.isBlank(value)) {
			return null;
		}
		String normalized = value.trim();
		if (normalized.startsWith("{") && normalized.endsWith("}") && normalized.length() > 2) {
			normalized = normalized.substring(1, normalized.length() - 1).trim();
		}
		return normalized;
	}

	protected int countObservations(Concept concept, Date fromDate, Date toDate) {
		List<Obs> observations = getObsService().getObservations(
				null,
				null,
				Collections.singletonList(concept),
				null,
				null,
				null,
				null,
				null,
				null,
				fromDate,
				toDate,
				false);

		if (observations == null || observations.isEmpty()) {
			return 0;
		}

		int count = 0;
		for (Obs observation : observations) {
			if (observation != null && isValidUuid(observation.getValueText())) {
				count++;
			}
		}
		return count;
	}

	/**
	 * Accepts standard UUID text (with or without braces), case-insensitive.
	 */
	protected boolean isValidUuid(String value) {
		if (StringUtils.isBlank(value)) {
			return false;
		}
		String normalized = value.trim();
		if (normalized.startsWith("{") && normalized.endsWith("}") && normalized.length() > 2) {
			normalized = normalized.substring(1, normalized.length() - 1).trim();
		}
		return UUID_PATTERN.matcher(normalized).matches();
	}

	protected Date startOfToday() {
		Calendar calendar = Calendar.getInstance();
		clearTime(calendar);
		return calendar.getTime();
	}

	protected Date startOfThisWeek() {
		Calendar calendar = Calendar.getInstance();
		clearTime(calendar);
		calendar.setFirstDayOfWeek(Calendar.MONDAY);
		calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
		return calendar.getTime();
	}

	private void clearTime(Calendar calendar) {
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
	}

	protected AdministrationService getAdministrationService() {
		return Context.getAdministrationService();
	}

	protected ConceptService getConceptService() {
		return Context.getConceptService();
	}

	protected ObsService getObsService() {
		return Context.getObsService();
	}

	protected PatientService getPatientService() {
		return Context.getPatientService();
	}

}
