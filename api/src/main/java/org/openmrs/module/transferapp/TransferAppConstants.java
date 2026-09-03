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

/**
 * Constants for the Transfer App module.
 */
public final class TransferAppConstants {

	private TransferAppConstants() {
	}

	public static final String GP_RECEIVED_TRANSFER_CONCEPT_UUID = "transferapp.receivedTransferConceptUuid";

	public static final String DEFAULT_RECEIVED_TRANSFER_CONCEPT_UUID = "076b04e5-d62d-47da-83e2-0da6e7a119b4";

	/** Fallback Transfer Id concept GP from rwandaemr (same UUID as receivedTransferConceptUuid). */
	public static final String GP_RWANDAEMR_TRANSFER_ID_CONCEPT_UUID = "rwandaemr.hie.transfer_id";

	public static final String GP_REGISTRATION_ENCOUNTER_TYPE_ID = "transferapp.registrationEncounterTypeId";

	/** Prefer rwandaemr registration encounter type when transferapp GP is blank. */
	public static final String GP_RWANDAEMR_REGISTRATION_ENCOUNTER_TYPE_ID = "rwandaemr.hie.registration_encounter_type_id";

	public static final String DEFAULT_REGISTRATION_ENCOUNTER_TYPE_ID = "5";

	public static final String GP_INSURANCE_TYPE_CONCEPT_UUID = "transferapp.insuranceTypeConceptUuid";

	public static final String DEFAULT_INSURANCE_TYPE_CONCEPT_UUID = "8da67e73-776c-43f6-9758-79d1f6786db3";

	public static final String GP_INSURANCE_NUMBER_CONCEPT_UUID = "transferapp.insuranceNumberConceptUuid";

	public static final String DEFAULT_INSURANCE_NUMBER_CONCEPT_UUID = "5775fd72-b120-40e1-84a4-a2554a781bb2";

	public static final String GP_INSURANCE_CBHI = "transferapp.insuranceCBHI";

	public static final String GP_INSURANCE_RSSB = "transferapp.insuranceRSSB";

	public static final String GP_INSURANCE_MMI = "transferapp.insuranceMMI";

	public static final String GP_INSURANCE_OTHER = "transferapp.insuranceOther";

	public static final String GP_INSURANCE_NONE = "transferapp.insuranceNone";

	public static final String HEALTH_INSURANCE_CBHI = "CBHI";

	public static final String HEALTH_INSURANCE_RSSB = "RSSB";

	public static final String HEALTH_INSURANCE_MMI = "MMI";

	public static final String HEALTH_INSURANCE_OTHER = "OTHER";

	public static final String HEALTH_INSURANCE_NONE = "NONE";

	public static final String GP_DIAGNOSIS_CONCEPT_UUID = "transferapp.diagnosisConceptUuid";

	/** Primary ICD, Secondary ICD, then CIEL Coded Diagnosis (Visit Diagnoses construct). */
	public static final String DEFAULT_DIAGNOSIS_CONCEPT_UUID =
			"2dce81f9-3874-4247-b441-6369ca0725c2,afb8006f-e7c4-45bd-82bd-16f6e4b4b51d,3cd94c66-26fe-102b-80cb-0017a47871b2";

	public static final String GP_CLINICAL_PRESENTATION_CONCEPT_UUID = "transferapp.ClinicalPresentationConceptUuid";

	public static final String DEFAULT_CLINICAL_PRESENTATION_CONCEPT_UUID = "3ce2b170-26fe-102b-80cb-0017a47871b2";

	public static final String GP_HEIGHT_CONCEPT_UUID = "transferapp.heightConceptUuid";

	public static final String DEFAULT_HEIGHT_CONCEPT_UUID = "3ce93cf2-26fe-102b-80cb-0017a47871b2";

	public static final String GP_WEIGHT_CONCEPT_UUID = "transferapp.weightConceptUuid";

	public static final String DEFAULT_WEIGHT_CONCEPT_UUID = "3ce93b62-26fe-102b-80cb-0017a47871b2";

	public static final String GP_TEMPERATURE_CONCEPT_UUID = "transferapp.temperatureConceptUuid";

	public static final String DEFAULT_TEMPERATURE_CONCEPT_UUID = "3ce939d2-26fe-102b-80cb-0017a47871b2";

	public static final String GP_PULSE_CONCEPT_UUID = "transferapp.pulseConceptUuid";

	public static final String DEFAULT_PULSE_CONCEPT_UUID = "3ce93824-26fe-102b-80cb-0017a47871b2";

	public static final String GP_BLOOD_PRESSURE_CONCEPT_UUID = "transferapp.bloodPressureConceptUuid";

	public static final String DEFAULT_BLOOD_PRESSURE_CONCEPT_UUID = "3ce934fa-26fe-102b-80cb-0017a47871b2/3ce93694-26fe-102b-80cb-0017a47871b2";

	public static final String GP_OXYGEN_SATURATION_CONCEPT_UUID = "transferapp.oxygenSaturationConceptUuid";

	public static final String DEFAULT_OXYGEN_SATURATION_CONCEPT_UUID = "3ce9401c-26fe-102b-80cb-0017a47871b2";

	public static final String GP_MUAC_CONCEPT_UUID = "transferapp.muacConceptUuid";

	public static final String DEFAULT_MUAC_CONCEPT_UUID = "4326b04b-3158-417a-bb8d-ad022295b0f4";

	public static final String GP_RESPIRATORY_RATE_CONCEPT_UUID = "transferapp.respiratoryRateConceptUuid";

	public static final String DEFAULT_RESPIRATORY_RATE_CONCEPT_UUID = "3ceb11f8-26fe-102b-80cb-0017a47871b2";

	public static final String GP_RWANDAEMR_HIE_URL = "rwandaemr.hie.url";

	public static final String GP_RWANDAEMR_HIE_USERNAME = "rwandaemr.hie.username";

	public static final String GP_RWANDAEMR_HIE_PASSWORD = "rwandaemr.hie.password";

	public static final String GP_HIE_URL = "transferapp.hie.url";

	public static final String GP_HIE_USERNAME = "transferapp.hie.username";

	public static final String GP_HIE_PASSWORD = "transferapp.hie.password";

	public static final String GP_HIE_VALIDATE_PATIENT_ADDRESS = "transferapp.hie.validatePatientAddress";

	public static final String GP_FR_TOKEN = "transferapp.fr_token";

	public static final String GP_VERIFY_BASE_URL = "transferapp.verify_base_url";

	public static final String DEFAULT_VERIFY_BASE_URL = "http://197.243.24.138:8081";

	public static final String GP_SENDING_FOSA_ID = "transferapp.sendingFosaId";

	public static final String DEFAULT_SENDING_FOSA_ID = "";

	/** Comma-separated aliases for inbound destination matching and pending targetOrg (old/alternate names). */
	public static final String GP_SENDING_FACILITY_NAME = "transferapp.sendingFacilityName";

	/** Normalized facility display name used on outbound HIE payloads (sendingFacility). */
	public static final String GP_OUTBOUND_FACILITY_NAME = "transferapp.outboundFacilityName";

	/** When true and credentials are set, send patient SMS after HIE accepts the transfer. */
	public static final String GP_SMS_INTOUCH_ENABLED = "transferapp.sms.intouch.enabled";

	public static final String GP_SMS_INTOUCH_BASE_URL = "transferapp.sms.intouch.base_url";

	public static final String DEFAULT_SMS_INTOUCH_BASE_URL = "https://www.intouchsms.co.rw:1402";

	public static final String GP_SMS_INTOUCH_SEND_PATH = "transferapp.sms.intouch.send_path";

	public static final String DEFAULT_SMS_INTOUCH_SEND_PATH = "/send";

	public static final String GP_SMS_INTOUCH_USERNAME = "transferapp.sms.intouch.username";

	public static final String GP_SMS_INTOUCH_PASSWORD = "transferapp.sms.intouch.password";

	public static final String GP_SMS_INTOUCH_SENDER_ID = "transferapp.sms.intouch.sender_id";

	public static final String DEFAULT_SMS_INTOUCH_SENDER_ID = "eBuzima";

	public static final String GP_SMS_INTOUCH_CODING = "transferapp.sms.intouch.coding";

	public static final String GP_SMS_INTOUCH_DLR_LEVEL = "transferapp.sms.intouch.dlr_level";

	public static final String GP_SMS_INTOUCH_DLR_URL = "transferapp.sms.intouch.dlr_url";

	public static final String GP_SMS_INTOUCH_LOG_OUTBOUND = "transferapp.sms.intouch.log_outbound_message";

	public static final String UPID_IDENTIFIER_TYPE_NAME = "UPID";

	public static final String PENDING_TRANSFER_EXISTING_PATIENT_KEY = "existingPatient";

	public static final String PENDING_TRANSFER_PATIENT_REFERENCE_KEY = "existingPatientReference";

	public static final String RWANDAEMR_MODULE_ID = "rwandaemr";

	public static final String REQUEST_APPOINTMENT_PAGE = "appointment/requestAppointment";

	public static final String HIE_TRANSFER_ENCOUNTER_PATH = "/shr/Encounter/transfer";

	public static final String HIE_LIST_TRANSFERS_PATH = "/shr/Encounter/$list-transfers";

	public static final String HIE_ENCOUNTER_BY_ID_PATH = "/shr/Encounter/";

	public static final String HIE_CLIENT_REGISTRY_PATIENT_PATH = "/clientregistry/Patient";

	public static final String HIE_FACILITY_REGISTRY_PATH = "/facility-registry/fhir";

	public static final String HIE_AUTH_TOKEN_HEADER = "X-Auth-Token";

	public static final int PATIENT_DASHBOARD_TRANSFER_LIMIT = 3;

}
