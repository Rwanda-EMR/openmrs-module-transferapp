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
import org.openmrs.Patient;
import org.openmrs.Obs;
import org.openmrs.User;
import org.openmrs.Visit;
import org.openmrs.api.APIException;
import org.openmrs.api.PatientService;
import org.openmrs.api.context.Context;
import org.openmrs.module.transferapp.api.TransferProfileService;
import org.openmrs.module.transferapp.api.TransferReferralFeedbackService;
import org.openmrs.module.transferapp.api.TransferRegistrationObsService;
import org.openmrs.module.transferapp.api.dao.TransferDao;
import org.openmrs.module.transferapp.api.dao.TransferReferralFeedbackDao;
import org.openmrs.module.transferapp.hie.HieApiException;
import org.openmrs.module.transferapp.hie.HieBasicConnection;
import org.openmrs.module.transferapp.hie.HieConnectionResolver;
import org.openmrs.module.transferapp.hie.HieShrClient;
import org.openmrs.module.transferapp.hie.ReferralFeedbackEncounterPatcher;
import org.openmrs.module.transferapp.model.ReferralFeedbackOutcome;
import org.openmrs.module.transferapp.model.Transfer;
import org.openmrs.module.transferapp.model.TransferProfile;
import org.openmrs.module.transferapp.model.TransferReferralFeedback;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TransferReferralFeedbackServiceImpl implements TransferReferralFeedbackService {

	private static final Log log = LogFactory.getLog(TransferReferralFeedbackServiceImpl.class);

	private static final String DATE_PATTERN = "yyyy-MM-dd";
	private static final String TIME_PATTERN = "HH:mm";

	private TransferReferralFeedbackDao transferReferralFeedbackDao;

	private TransferDao transferDao;

	private PatientService patientService;

	private TransferProfileService transferProfileService;

	private TransferRegistrationObsService registrationObsService;

	private HieConnectionResolver hieConnectionResolver = new HieConnectionResolver();

	private HieShrClient hieShrClient = new HieShrClient();

	private ReferralFeedbackEncounterPatcher encounterPatcher = new ReferralFeedbackEncounterPatcher();

	public void setTransferReferralFeedbackDao(TransferReferralFeedbackDao transferReferralFeedbackDao) {
		this.transferReferralFeedbackDao = transferReferralFeedbackDao;
	}

	public void setTransferDao(TransferDao transferDao) {
		this.transferDao = transferDao;
	}

	public void setPatientService(PatientService patientService) {
		this.patientService = patientService;
	}

	public void setTransferProfileService(TransferProfileService transferProfileService) {
		this.transferProfileService = transferProfileService;
	}

	public void setRegistrationObsService(TransferRegistrationObsService registrationObsService) {
		this.registrationObsService = registrationObsService;
	}

	public void setHieConnectionResolver(HieConnectionResolver hieConnectionResolver) {
		this.hieConnectionResolver = hieConnectionResolver != null ? hieConnectionResolver : new HieConnectionResolver();
	}

	public void setHieShrClient(HieShrClient hieShrClient) {
		this.hieShrClient = hieShrClient != null ? hieShrClient : new HieShrClient();
	}

	public void setEncounterPatcher(ReferralFeedbackEncounterPatcher encounterPatcher) {
		this.encounterPatcher = encounterPatcher != null ? encounterPatcher : new ReferralFeedbackEncounterPatcher();
	}

	@Override
	public Map<String, Object> getFeedbackForm(Integer patientId, String hieTransferId) {
		Patient patient = requirePatient(patientId);
		String transferId = requireHieTransferId(hieTransferId);
		TransferReferralFeedback existing = transferReferralFeedbackDao.getByPatientAndHieTransferId(
				patient, transferId);

		Map<String, Object> result = new LinkedHashMap<String, Object>();
		result.put("status", "success");
		result.put("hieTransferId", transferId);
		result.put("patientId", patient.getPatientId());
		result.put("completed", existing != null && existing.isCompleted());
		result.put("hieSent", existing != null && existing.isHieSent());
		result.put("defaults", buildDefaults(patient, transferId, existing));
		result.put("profileDefaults", resolveProfileDefaultsForCurrentUser());
		result.put("feedback", existing != null ? toMap(existing) : null);
		result.put("outcomes", outcomeOptions());
		return result;
	}

	@Override
	public Map<String, Object> saveFeedback(Integer patientId, String hieTransferId,
			String dateOfDischarge, String finalDiagnosis, String treatmentGiven, String outcome,
			String recommendations, String referBackToFacility, String referBackToFacilityFosaId,
			String contactPerson, String providerName, String qualification, String signedDate,
			String signedTime, String phone) {

		Patient patient = requirePatient(patientId);
		String transferId = requireHieTransferId(hieTransferId);
		User user = Context.getAuthenticatedUser();
		if (user == null) {
			throw new APIException("You must be logged in to save referral feedback");
		}

		Date admissionDate = resolveAdmissionDate(patient, transferId);
		if (admissionDate == null) {
			admissionDate = toDateOnly(new Date());
		}
		Date discharge = parseDate(requireText(dateOfDischarge, "Date of discharge"), "Date of discharge");
		if (discharge.before(admissionDate)) {
			throw new APIException("Date of discharge cannot be before the date of admission or client seen.");
		}
		ReferralFeedbackOutcome parsedOutcome = ReferralFeedbackOutcome.fromStoredValue(outcome);
		if (parsedOutcome == null) {
			throw new APIException("Select the patient outcome.");
		}
		Date signed = parseDate(requireText(signedDate, "Date"), "Date");
		String time = normalizeTime(requireText(signedTime, "Time"));

		TransferReferralFeedback feedback = transferReferralFeedbackDao.getByPatientAndHieTransferId(
				patient, transferId);
		Date now = new Date();
		if (feedback == null) {
			feedback = new TransferReferralFeedback();
			feedback.setUuid(UUID.randomUUID().toString());
			feedback.setPatient(patient);
			feedback.setHieTransferId(transferId);
			feedback.setCreator(user);
			feedback.setDateCreated(now);
			feedback.setVoided(false);
			feedback.setHieSent(Boolean.FALSE);
		}
		else {
			if (feedback.isHieSent()) {
				throw new APIException("This referral feedback has already been sent to HIE and cannot be changed.");
			}
			feedback.setChangedBy(user);
			feedback.setDateChanged(now);
		}

		Visit activeVisit = resolveActiveVisit(patient);
		if (activeVisit != null) {
			feedback.setVisitId(activeVisit.getVisitId());
		}
		try {
			Transfer localTransfer = transferDao.getTransferByHieTransferId(patient.getPatientId(), transferId);
			if (localTransfer != null) {
				feedback.setLocalTransferId(localTransfer.getTransferId());
			}
		}
		catch (RuntimeException ignored) {
			// Inbound HIE transfers may not have a local transfers row.
		}
		if (registrationObsService != null) {
			Obs transferIdObs = registrationObsService.findMatchingTransferIdObs(patient, transferId);
			if (transferIdObs != null && transferIdObs.getEncounter() != null) {
				feedback.setRegistrationEncounterId(transferIdObs.getEncounter().getEncounterId());
			}
		}

		feedback.setDateOfAdmissionOrSeen(admissionDate);
		feedback.setDateOfDischarge(discharge);
		feedback.setFinalDiagnosis(requireText(finalDiagnosis, "Final diagnosis"));
		feedback.setTreatmentGiven(requireText(treatmentGiven, "Treatment given"));
		feedback.setOutcome(parsedOutcome.name());
		feedback.setRecommendations(requireText(recommendations, "Recommendations (follow up care)"));
		feedback.setReferBackToFacility(requireText(referBackToFacility, "Refer back to facility"));
		feedback.setReferBackToFacilityFosaId(StringUtils.trimToNull(referBackToFacilityFosaId));
		feedback.setContactPerson(requireText(contactPerson, "Contact person"));
		Map<String, String> profileDefaults = resolveProfileDefaultsForCurrentUser();
		feedback.setProviderName(requireText(
				firstNonBlank(profileDefaults.get("providerName"), providerName),
				"Names of health care provider"));
		feedback.setQualification(requireText(
				firstNonBlank(profileDefaults.get("qualification"), qualification),
				"Qualification"));
		feedback.setSignedDate(signed);
		feedback.setSignedTime(time);
		feedback.setPhone(requireText(firstNonBlank(phone, profileDefaults.get("phone")), "Phone"));
		feedback.setCompleted(Boolean.TRUE);
		feedback.setCompletedAt(now);
		feedback.setCompletedBy(user.getUserId());
		feedback.setHieSendErr(null);

		transferReferralFeedbackDao.save(feedback);

		Map<String, Object> result = new LinkedHashMap<String, Object>();
		result.put("status", "success");
		result.put("message", "Referral feedback and counter-referral saved. Review the HIE payload, then send.");
		result.put("completed", Boolean.TRUE);
		result.put("hieSent", Boolean.FALSE);
		result.put("feedback", toMap(feedback));
		result.putAll(buildPreviewResult(feedback, false));
		return result;
	}

	@Override
	public Map<String, Object> previewFeedbackHiePayload(Integer patientId, String hieTransferId) {
		TransferReferralFeedback feedback = requireSavedFeedback(patientId, hieTransferId);
		return buildPreviewResult(feedback, true);
	}

	@Override
	public Map<String, Object> submitFeedbackToHie(Integer patientId, String hieTransferId) {
		TransferReferralFeedback feedback = requireSavedFeedback(patientId, hieTransferId);
		if (feedback.isHieSent()) {
			Map<String, Object> already = new LinkedHashMap<String, Object>();
			already.put("status", "success");
			already.put("message", "Referral feedback was already sent to HIE.");
			already.put("hieSent", Boolean.TRUE);
			already.put("feedback", toMap(feedback));
			return already;
		}

		HieBasicConnection connection;
		try {
			connection = hieConnectionResolver.resolveConnection();
		}
		catch (RuntimeException ex) {
			throw new APIException("HIE is not configured: " + ex.getMessage(), ex);
		}

		String existingJson;
		try {
			existingJson = hieShrClient.fetchEncounterById(connection, feedback.getHieTransferId());
		}
		catch (HieApiException ex) {
			feedback.setHieSendErr(truncate(ex.getMessage(), 500));
			transferReferralFeedbackDao.save(feedback);
			throw new APIException("Unable to load the existing transfer from HIE: " + ex.getMessage(), ex);
		}
		if (StringUtils.isBlank(existingJson)) {
			feedback.setHieSendErr("Existing transfer Encounter was not found in HIE.");
			transferReferralFeedbackDao.save(feedback);
			throw new APIException("Existing transfer Encounter was not found in HIE for id "
					+ feedback.getHieTransferId());
		}

		String mergedJson;
		try {
			mergedJson = encounterPatcher.mergeFeedbackIntoEncounter(existingJson, feedback);
			hieShrClient.updateTransferEncounter(connection, mergedJson);
		}
		catch (HieApiException ex) {
			feedback.setHieSendErr(truncate(ex.getMessage(), 500));
			transferReferralFeedbackDao.save(feedback);
			throw new APIException("Unable to send referral feedback to HIE: " + ex.getMessage(), ex);
		}
		catch (RuntimeException ex) {
			feedback.setHieSendErr(truncate(ex.getMessage(), 500));
			transferReferralFeedbackDao.save(feedback);
			throw new APIException("Unable to send referral feedback to HIE: " + ex.getMessage(), ex);
		}

		Date now = new Date();
		feedback.setHieSent(Boolean.TRUE);
		feedback.setHieSentAt(now);
		feedback.setHieSendErr(null);
		User user = Context.getAuthenticatedUser();
		if (user != null) {
			feedback.setChangedBy(user);
			feedback.setDateChanged(now);
		}
		transferReferralFeedbackDao.save(feedback);

		Map<String, Object> result = new LinkedHashMap<String, Object>();
		result.put("status", "success");
		result.put("message", "Referral feedback and counter-referral sent to HIE.");
		result.put("hieSent", Boolean.TRUE);
		result.put("hieSentAt", formatDate(now));
		result.put("feedback", toMap(feedback));
		result.put("summary", encounterPatcher.buildPreviewSummary(feedback));
		try {
			result.put("encounterPayload", encounterPatcher.prettyPrintJson(mergedJson));
		}
		catch (RuntimeException ignored) {
			result.put("encounterPayload", mergedJson);
		}
		return result;
	}

	private Map<String, Object> buildPreviewResult(TransferReferralFeedback feedback, boolean wrapStatus) {
		Map<String, Object> result = new LinkedHashMap<String, Object>();
		if (wrapStatus) {
			result.put("status", "success");
		}
		result.put("summary", encounterPatcher.buildPreviewSummary(feedback));
		try {
			result.put("referralFeedbackExtension",
					encounterPatcher.prettyPrint(encounterPatcher.buildReferralFeedbackExtension(feedback)));
			result.put("counterReferralExtension",
					encounterPatcher.prettyPrint(encounterPatcher.buildCounterReferralExtension(feedback)));
		}
		catch (RuntimeException ex) {
			log.warn("Unable to pretty-print feedback extensions", ex);
		}

		String encounterPayload = null;
		String warning = null;
		try {
			HieBasicConnection connection = hieConnectionResolver.resolveConnection();
			String existingJson = hieShrClient.fetchEncounterById(connection, feedback.getHieTransferId());
			if (StringUtils.isBlank(existingJson)) {
				warning = "Existing transfer was not found in HIE yet; extensions below show what will be merged when it is available.";
			}
			else {
				encounterPayload = encounterPatcher.prettyPrintJson(
						encounterPatcher.mergeFeedbackIntoEncounter(existingJson, feedback));
			}
		}
		catch (RuntimeException ex) {
			warning = "Unable to load existing HIE Encounter for full preview: " + ex.getMessage();
			log.warn(warning, ex);
		}
		if (encounterPayload != null) {
			result.put("encounterPayload", encounterPayload);
		}
		if (warning != null) {
			result.put("previewWarning", warning);
		}
		result.put("canSend", !feedback.isHieSent());
		result.put("hieSent", feedback.isHieSent());
		return result;
	}

	private TransferReferralFeedback requireSavedFeedback(Integer patientId, String hieTransferId) {
		Patient patient = requirePatient(patientId);
		String transferId = requireHieTransferId(hieTransferId);
		TransferReferralFeedback feedback = transferReferralFeedbackDao.getByPatientAndHieTransferId(
				patient, transferId);
		if (feedback == null || !feedback.isCompleted()) {
			throw new APIException("Save referral feedback before previewing or sending to HIE.");
		}
		return feedback;
	}

	private static String truncate(String value, int max) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		if (trimmed.length() <= max) {
			return trimmed;
		}
		return trimmed.substring(0, max);
	}

	private Map<String, Object> buildDefaults(Patient patient, String hieTransferId,
			TransferReferralFeedback existing) {
		Map<String, Object> defaults = new LinkedHashMap<String, Object>();
		defaults.put("dateOfAdmissionOrSeen", formatDate(resolveAdmissionDate(patient, hieTransferId)));
		Map<String, String> profileDefaults = resolveProfileDefaultsForCurrentUser();

		if (existing != null && existing.isCompleted()) {
			defaults.put("dateOfDischarge", formatDate(existing.getDateOfDischarge()));
			defaults.put("finalDiagnosis", StringUtils.trimToEmpty(existing.getFinalDiagnosis()));
			defaults.put("treatmentGiven", StringUtils.trimToEmpty(existing.getTreatmentGiven()));
			defaults.put("outcome", StringUtils.trimToEmpty(existing.getOutcome()));
			defaults.put("recommendations", StringUtils.trimToEmpty(existing.getRecommendations()));
			defaults.put("referBackToFacility", StringUtils.trimToEmpty(existing.getReferBackToFacility()));
			defaults.put("referBackToFacilityFosaId", StringUtils.trimToEmpty(existing.getReferBackToFacilityFosaId()));
			defaults.put("contactPerson", StringUtils.trimToEmpty(existing.getContactPerson()));
			defaults.put("providerName", StringUtils.trimToEmpty(existing.getProviderName()));
			defaults.put("qualification", StringUtils.trimToEmpty(existing.getQualification()));
			defaults.put("signedDate", formatDate(existing.getSignedDate()));
			defaults.put("signedTime", StringUtils.trimToEmpty(existing.getSignedTime()));
			defaults.put("phone", StringUtils.trimToEmpty(existing.getPhone()));
		}
		else {
			defaults.put("dateOfDischarge", "");
			defaults.put("finalDiagnosis", "");
			defaults.put("treatmentGiven", "");
			defaults.put("outcome", "");
			defaults.put("recommendations", "");
			defaults.put("referBackToFacility", "");
			defaults.put("referBackToFacilityFosaId", "");
			defaults.put("contactPerson", "");
			defaults.put("providerName", profileDefaults.get("providerName"));
			defaults.put("qualification", profileDefaults.get("qualification"));
			defaults.put("signedDate", formatDate(new Date()));
			defaults.put("signedTime", new SimpleDateFormat(TIME_PATTERN).format(new Date()));
			defaults.put("phone", profileDefaults.get("phone"));
		}
		return defaults;
	}

	private Map<String, String> resolveProfileDefaultsForCurrentUser() {
		Map<String, String> defaults = new LinkedHashMap<String, String>();
		User user = Context.getAuthenticatedUser();
		TransferProfile profile = user != null ? transferProfileService.getProfileForUser(user) : null;
		String providerName = "";
		String qualification = "";
		String phone = "";
		if (user != null && user.getPerson() != null && user.getPerson().getPersonName() != null) {
			providerName = StringUtils.trimToEmpty(user.getPerson().getPersonName().getFullName());
		}
		if (profile != null) {
			providerName = TransferProfile.formatCareProviderName(providerName, profile.getLicenseNumber());
			qualification = StringUtils.trimToEmpty(profile.getQualificationWithSpeciality());
			phone = StringUtils.trimToEmpty(profile.getPhoneNumber());
		}
		defaults.put("providerName", providerName);
		defaults.put("qualification", qualification);
		defaults.put("phone", phone);
		return defaults;
	}

	private Map<String, Object> toMap(TransferReferralFeedback feedback) {
		Map<String, Object> map = new LinkedHashMap<String, Object>();
		map.put("uuid", feedback.getUuid());
		map.put("hieTransferId", feedback.getHieTransferId());
		map.put("dateOfAdmissionOrSeen", formatDate(feedback.getDateOfAdmissionOrSeen()));
		map.put("dateOfDischarge", formatDate(feedback.getDateOfDischarge()));
		map.put("finalDiagnosis", feedback.getFinalDiagnosis());
		map.put("treatmentGiven", feedback.getTreatmentGiven());
		map.put("outcome", feedback.getOutcome());
		ReferralFeedbackOutcome parsed = ReferralFeedbackOutcome.fromStoredValue(feedback.getOutcome());
		map.put("outcomeLabel", parsed != null ? parsed.getLabel() : feedback.getOutcome());
		map.put("recommendations", feedback.getRecommendations());
		map.put("referBackToFacility", feedback.getReferBackToFacility());
		map.put("referBackToFacilityFosaId", feedback.getReferBackToFacilityFosaId());
		map.put("contactPerson", feedback.getContactPerson());
		map.put("providerName", feedback.getProviderName());
		map.put("qualification", feedback.getQualification());
		map.put("signedDate", formatDate(feedback.getSignedDate()));
		map.put("signedTime", feedback.getSignedTime());
		map.put("phone", feedback.getPhone());
		map.put("completed", feedback.isCompleted());
		map.put("hieSent", feedback.isHieSent());
		return map;
	}

	private List<Map<String, String>> outcomeOptions() {
		List<Map<String, String>> options = new ArrayList<Map<String, String>>();
		ReferralFeedbackOutcome[] values = ReferralFeedbackOutcome.values();
		for (int i = 0; i < values.length; i++) {
			Map<String, String> option = new LinkedHashMap<String, String>();
			option.put("code", values[i].name());
			option.put("label", values[i].getLabel());
			options.add(option);
		}
		return options;
	}

	private Patient requirePatient(Integer patientId) {
		if (patientId == null) {
			throw new APIException("patientId is required");
		}
		Patient patient = patientService.getPatient(patientId);
		if (patient == null) {
			throw new APIException("Patient not found");
		}
		return patient;
	}

	private String requireHieTransferId(String hieTransferId) {
		String transferId = StringUtils.trimToNull(hieTransferId);
		if (transferId == null) {
			throw new APIException("hieTransferId is required");
		}
		return transferId;
	}

	private String firstNonBlank(String first, String second) {
		if (StringUtils.isNotBlank(first)) {
			return first.trim();
		}
		if (StringUtils.isNotBlank(second)) {
			return second.trim();
		}
		return "";
	}

	private String requireText(String value, String label) {
		String trimmed = StringUtils.trimToNull(value);
		if (trimmed == null) {
			throw new APIException(label + " is required.");
		}
		return trimmed;
	}

	private Date parseDate(String value, String label) {
		try {
			SimpleDateFormat format = new SimpleDateFormat(DATE_PATTERN);
			format.setLenient(false);
			return format.parse(value.trim());
		}
		catch (ParseException ex) {
			throw new APIException(label + " must be yyyy-MM-dd.");
		}
	}

	private String normalizeTime(String value) {
		String trimmed = value.trim();
		if (trimmed.length() >= 5 && trimmed.charAt(2) == ':') {
			return trimmed.substring(0, 5);
		}
		try {
			SimpleDateFormat format = new SimpleDateFormat(TIME_PATTERN);
			format.setLenient(false);
			return format.format(format.parse(trimmed));
		}
		catch (ParseException ex) {
			throw new APIException("Time must be HH:mm.");
		}
	}

	private String formatDate(Date date) {
		if (date == null) {
			return "";
		}
		return new SimpleDateFormat(DATE_PATTERN).format(date);
	}

	private Date resolveAdmissionDate(Patient patient, String hieTransferId) {
		if (registrationObsService != null && StringUtils.isNotBlank(hieTransferId)) {
			Date obsDate = registrationObsService.findTransferIdObsDatetimeOnActiveVisit(patient, hieTransferId);
			if (obsDate != null) {
				return toDateOnly(obsDate);
			}
		}
		Visit visit = resolveActiveVisit(patient);
		if (visit != null && visit.getStartDatetime() != null) {
			return toDateOnly(visit.getStartDatetime());
		}
		return toDateOnly(new Date());
	}

	private Date toDateOnly(Date source) {
		if (source == null) {
			return null;
		}
		try {
			SimpleDateFormat format = new SimpleDateFormat(DATE_PATTERN);
			return format.parse(format.format(source));
		}
		catch (ParseException ex) {
			return source;
		}
	}

	private Visit resolveActiveVisit(Patient patient) {
		List<Visit> visits = Context.getVisitService().getActiveVisitsByPatient(patient);
		if (visits == null || visits.isEmpty()) {
			return null;
		}
		Visit latest = null;
		Date latestStart = null;
		for (Visit visit : visits) {
			if (visit == null || Boolean.TRUE.equals(visit.getVoided()) || visit.getStopDatetime() != null) {
				continue;
			}
			Date start = visit.getStartDatetime();
			if (start == null) {
				continue;
			}
			if (latestStart == null || start.after(latestStart)) {
				latestStart = start;
				latest = visit;
			}
		}
		return latest;
	}
}
