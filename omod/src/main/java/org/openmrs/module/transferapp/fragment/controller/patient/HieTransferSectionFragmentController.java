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
package org.openmrs.module.transferapp.fragment.controller.patient;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.EncounterType;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.Visit;
import org.openmrs.api.EncounterService;
import org.openmrs.api.context.Context;
import org.openmrs.module.appframework.domain.AppDescriptor;
import org.openmrs.module.emrapi.patient.PatientDomainWrapper;
import org.openmrs.module.transferapp.TransferAppActivator;
import org.openmrs.module.transferapp.TransferAppConstants;
import org.openmrs.module.transferapp.TransferPrivilegeHelper;
import org.openmrs.module.transferapp.api.TransferPatientSnapshotResolver;
import org.openmrs.module.transferapp.api.TransferRegistrationObsService;
import org.openmrs.module.transferapp.hie.HieConnectionResolver;
import org.openmrs.ui.framework.UiUtils;
import org.openmrs.ui.framework.annotation.FragmentParam;
import org.openmrs.ui.framework.annotation.InjectBeans;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.fragment.FragmentConfiguration;
import org.openmrs.ui.framework.fragment.FragmentModel;
import org.openmrs.ui.framework.page.PageModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * Patient dashboard fragment for inbound HIE transfers on an active visit.
 * When the latest registration encounter has a Transfer Id obs, offers preview by that UUID.
 * Otherwise lists transfers from HIE via the same REST search as /ws/rest/v1/transferapp/transfer.
 */
public class HieTransferSectionFragmentController {

	protected final Log log = LogFactory.getLog(getClass());

	private final TransferPatientSnapshotResolver patientSnapshotResolver = new TransferPatientSnapshotResolver();

	private final HieConnectionResolver hieConnectionResolver = new HieConnectionResolver();

	public void controller(FragmentConfiguration config,
			PageModel pageModel,
			FragmentModel model,
			UiUtils ui,
			@FragmentParam("app") AppDescriptor appDescriptor,
			@InjectBeans PatientDomainWrapper patientWrapper,
			@SpringBean("encounterService") EncounterService encounterService,
			@SpringBean("transferAppRegistrationObsService") TransferRegistrationObsService registrationObsService) {

		config.require("patient");
		Object patient = config.get("patient");
		if (patient instanceof Patient) {
			patientWrapper.setPatient((Patient) patient);
			config.addAttribute("patient", patientWrapper);
		} else if (patient instanceof PatientDomainWrapper) {
			patientWrapper = (PatientDomainWrapper) patient;
		}

		model.addAttribute("canListTransfers", false);
		model.addAttribute("showSection", false);
		model.addAttribute("hasActiveVisit", false);
		model.addAttribute("hasRegistrationEncounter", false);
		model.addAttribute("hasTransferIdObs", false);
		model.addAttribute("listFromHie", false);
		model.addAttribute("canProvideFeedback", false);
		model.addAttribute("transferId", "");
		model.addAttribute("upid", "");
		model.addAttribute("patientId", null);
		model.addAttribute("registrationEncounterId", null);
		model.addAttribute("currentFacilityName", "");
		model.addAttribute("canValidateTransfer", false);
		model.addAttribute("statusMessage", "");
		model.addAttribute("accessDeniedMessage", "");

		boolean canListTransfers = TransferPrivilegeHelper.hasPrivilege(TransferAppActivator.PRIVILEGE_LIST_TRANSFERS);
		model.addAttribute("canListTransfers", canListTransfers);
		if (!canListTransfers) {
			model.addAttribute("accessDeniedMessage", ui.message("transferapp.patient.hieTransfer.listNotAllowed",
					TransferAppActivator.PRIVILEGE_LIST_TRANSFERS));
			return;
		}

		boolean canCreateTransfer = TransferPrivilegeHelper.hasPrivilege(TransferAppActivator.PRIVILEGE_CREATE_TRANSFER);
		boolean canProvideFeedback = TransferPrivilegeHelper.hasPrivilege(TransferAppActivator.PRIVILEGE_FEEDBACK);
		model.addAttribute("canValidateTransfer", canCreateTransfer);
		model.addAttribute("currentFacilityName", registrationObsService.resolveCurrentFacilityName());

		try {
			ui.includeCss("transferapp", "transferFormPreview.css");
			ui.includeCss("transferapp", "transferSection.css");
			ui.includeJavascript("transferapp", "transferMohLogo.js");
			ui.includeJavascript("transferapp", "transferFormPreview.js");
			ui.includeJavascript("transferapp", "transferPreviewCommon.js");
			ui.includeJavascript("transferapp", "hieTransferSection.js");
		}
		catch (Exception ex) {
			log.warn("Unable to include HIE transfer section resources", ex);
		}

		if (!hieConnectionResolver.isHieConfigured()) {
			model.addAttribute("statusMessage", ui.message("transferapp.patient.hieTransfer.hieNotConfigured"));
			return;
		}

		Patient currentPatient = patientWrapper.getPatient();
		if (currentPatient == null) {
			model.addAttribute("statusMessage", ui.message("transferapp.patient.hieTransfer.noPatient"));
			return;
		}
		model.addAttribute("patientId", currentPatient.getPatientId());

		String upid = patientSnapshotResolver.resolveUpid(currentPatient);
		if (StringUtils.isBlank(upid)) {
			model.addAttribute("statusMessage", ui.message("transferapp.patient.hieTransfer.noUpid"));
			return;
		}
		model.addAttribute("upid", upid.trim());

		Visit activeVisit = resolveActiveVisit(currentPatient);
		if (activeVisit == null) {
			model.addAttribute("statusMessage", ui.message("transferapp.patient.hieTransfer.noActiveVisit"));
			return;
		}
		model.addAttribute("hasActiveVisit", true);

		Integer registrationTypeId = resolveRegistrationEncounterTypeId();
		if (registrationTypeId == null) {
			model.addAttribute("statusMessage", ui.message("transferapp.patient.hieTransfer.registrationTypeMissing"));
			return;
		}

		Encounter latestRegistration = findLatestRegistrationEncounterOnVisit(
				encounterService, currentPatient, registrationTypeId, activeVisit);
		if (latestRegistration == null) {
			model.addAttribute("statusMessage", ui.message("transferapp.patient.hieTransfer.noRegistration"));
			return;
		}
		model.addAttribute("hasRegistrationEncounter", true);
		model.addAttribute("registrationEncounterId", latestRegistration.getEncounterId());
		model.addAttribute("showSection", true);

		Concept transferIdConcept = resolveTransferIdConcept();
		if (transferIdConcept == null) {
			// Still list from HIE when Transfer Id concept is missing.
			model.addAttribute("listFromHie", true);
			return;
		}

		String transferId = findTransferIdValueText(latestRegistration, transferIdConcept);
		if (StringUtils.isNotBlank(transferId)) {
			model.addAttribute("hasTransferIdObs", true);
			model.addAttribute("transferId", transferId.trim());
			model.addAttribute("listFromHie", false);
			model.addAttribute("canProvideFeedback", canProvideFeedback);
			return;
		}

		model.addAttribute("hasTransferIdObs", false);
		model.addAttribute("listFromHie", true);
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

	private Integer resolveRegistrationEncounterTypeId() {
		String raw = StringUtils.trimToNull(Context.getAdministrationService().getGlobalProperty(
				TransferAppConstants.GP_REGISTRATION_ENCOUNTER_TYPE_ID,
				TransferAppConstants.DEFAULT_REGISTRATION_ENCOUNTER_TYPE_ID));
		if (raw == null) {
			raw = StringUtils.trimToNull(Context.getAdministrationService().getGlobalProperty(
					TransferAppConstants.GP_RWANDAEMR_REGISTRATION_ENCOUNTER_TYPE_ID));
		}
		if (raw == null) {
			return null;
		}
		try {
			int id = Integer.parseInt(raw);
			return id > 0 ? Integer.valueOf(id) : null;
		}
		catch (NumberFormatException ex) {
			log.warn("Invalid registration encounter type id: " + raw);
			return null;
		}
	}

	private Encounter findLatestRegistrationEncounterOnVisit(EncounterService encounterService, Patient patient,
			Integer registrationTypeId, Visit activeVisit) {
		List<Encounter> encounters = encounterService.getEncountersByPatient(patient);
		if (encounters == null || encounters.isEmpty() || activeVisit == null || activeVisit.getVisitId() == null) {
			return null;
		}

		List<Encounter> registrationEncounters = new ArrayList<Encounter>();
		for (Encounter encounter : encounters) {
			if (encounter == null || Boolean.TRUE.equals(encounter.getVoided())) {
				continue;
			}
			if (encounter.getVisit() == null || encounter.getVisit().getVisitId() == null) {
				continue;
			}
			if (!activeVisit.getVisitId().equals(encounter.getVisit().getVisitId())) {
				continue;
			}
			EncounterType type = encounter.getEncounterType();
			if (type == null || type.getEncounterTypeId() == null) {
				continue;
			}
			if (registrationTypeId.equals(type.getEncounterTypeId())) {
				registrationEncounters.add(encounter);
			}
		}
		if (registrationEncounters.isEmpty()) {
			return null;
		}

		Collections.sort(registrationEncounters, new Comparator<Encounter>() {
			@Override
			public int compare(Encounter left, Encounter right) {
				Date leftDate = left != null ? left.getEncounterDatetime() : null;
				Date rightDate = right != null ? right.getEncounterDatetime() : null;
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
		return registrationEncounters.get(0);
	}

	private Concept resolveTransferIdConcept() {
		String conceptUuid = StringUtils.trimToNull(Context.getAdministrationService().getGlobalProperty(
				TransferAppConstants.GP_RECEIVED_TRANSFER_CONCEPT_UUID,
				TransferAppConstants.DEFAULT_RECEIVED_TRANSFER_CONCEPT_UUID));
		if (conceptUuid == null) {
			conceptUuid = StringUtils.trimToNull(Context.getAdministrationService().getGlobalProperty(
					TransferAppConstants.GP_RWANDAEMR_TRANSFER_ID_CONCEPT_UUID));
		}
		if (conceptUuid == null) {
			return null;
		}
		return Context.getConceptService().getConceptByUuid(conceptUuid);
	}

	private String findTransferIdValueText(Encounter registration, Concept transferIdConcept) {
		Set<Obs> obsSet = registration.getAllObs(false);
		if (obsSet == null || obsSet.isEmpty()) {
			return null;
		}
		for (Obs obs : obsSet) {
			if (obs == null || Boolean.TRUE.equals(obs.getVoided()) || obs.getConcept() == null) {
				continue;
			}
			if (!obs.getConcept().equals(transferIdConcept)) {
				continue;
			}
			String valueText = StringUtils.trimToNull(obs.getValueText());
			if (valueText != null) {
				return valueText;
			}
			String asString = StringUtils.trimToNull(obs.getValueAsString(Context.getLocale()));
			if (asString != null) {
				return asString;
			}
		}
		return null;
	}
}
