<%
ui.includeCss("transferapp", "styles/transferWizard.css")
ui.includeCss("transferapp", "styles/flatpickr.min.css")
ui.includeCss("transferapp", "styles/select2.min.css")
%>

<g:if test="${error != null && error.trim().length() > 0}">
    <p style="color: red;">${ ui.format(error) }</p>
</g:if>

<g:if test="${formData}">
<div class="transfer-wizard-shell">
    <header class="transfer-wizard-page-header">
        <h1 class="transfer-wizard-page-title" id="maternity-wizard-page-title">Maternity Transfer Form</h1>
        <p class="transfer-wizard-page-lead" id="maternity-wizard-page-lead">Step 1 — client identification, demographics, and referral.</p>
    </header>

    <div class="transfer-wizard-panel" style="padding: 0 5px;">
        <ul class="transfer-wizard-progress transfer-wizard-progress-5">
            <li data-step="1"><span class="transfer-wizard-step-num">1</span><span class="transfer-wizard-step-label">Client &amp; Referral</span></li>
            <li data-step="2"><span class="transfer-wizard-step-num">2</span><span class="transfer-wizard-step-label">Obstetric History</span></li>
            <li data-step="3"><span class="transfer-wizard-step-num">3</span><span class="transfer-wizard-step-label">Clinical Findings</span></li>
            <li data-step="4"><span class="transfer-wizard-step-num">4</span><span class="transfer-wizard-step-label">Treatment &amp; Transport</span></li>
            <li data-step="5"><span class="transfer-wizard-step-num">5</span><span class="transfer-wizard-step-label">Sign-off</span></li>
        </ul>

        <div class="transfer-wizard-scroll">
        <form id="moh-maternity-transfer-wizard-form" class="transfer-out-form" novalidate="novalidate"
              data-editing="${ formData.transferUuid ? 'true' : 'false' }"
              data-client-name="${ ui.encodeHtmlAttribute(formData.clientName ?: '') }">
            <input type="hidden" name="patientId" value="${ formData.patientId }" />
            <input type="hidden" name="transferUuid" value="${ ui.encodeHtmlAttribute(formData.transferUuid ?: '') }" />
            <input type="hidden" id="maternityReceivingFacilityId" name="receivingFacilityId" value="" />

            <!-- Step 1: Client & Referral Info -->
            <div class="transfer-wizard-step-panel is-active" data-step="1">
                <div class="transfer-wizard-section">
                    <h2 class="transfer-wizard-section-title">Facility details</h2>
                    <div class="transfer-wizard-row transfer-wizard-row-three-col">
                        <div class="transfer-wizard-field">
                            <label for="maternityProvince">Province</label>
                            <input type="text" id="maternityProvince" name="province" value="${ ui.encodeHtmlAttribute(formData.province ?: '') }" />
                        </div>
                        <div class="transfer-wizard-field">
                            <label for="maternityFacilityDistrict">District</label>
                            <input type="text" id="maternityFacilityDistrict" name="district" value="${ ui.encodeHtmlAttribute(formData.district ?: '') }" />
                        </div>
                        <div class="transfer-wizard-field">
                            <label for="maternityHospitalName">Name of hospital</label>
                            <input type="text" id="maternityHospitalName" name="hospitalName" value="${ ui.encodeHtmlAttribute(formData.hospitalName ?: '') }" />
                        </div>
                        <div class="transfer-wizard-field">
                            <label for="maternityReferringFacilityName">Name of referring facility</label>
                            <input type="text" id="maternityReferringFacilityName" name="referringFacilityName" value="${ ui.encodeHtmlAttribute(formData.referringFacilityName ?: '') }" />
                        </div>
                        <div class="transfer-wizard-field">
                            <label for="maternityReferringUnit">Referring unit</label>
                            <input type="text" id="maternityReferringUnit" name="referringUnit" value="${ ui.encodeHtmlAttribute(formData.referringUnit ?: '') }" />
                        </div>
                    </div>
                </div>

                <div class="transfer-wizard-section">
                    <h2 class="transfer-wizard-section-title">Client information</h2>
                    <div class="transfer-wizard-row transfer-wizard-row-three-col">
                        <div class="transfer-wizard-field">
                            <label for="maternityClientName">Client name</label>
                            <input type="text" id="maternityClientName" name="clientName" value="${ ui.encodeHtmlAttribute(formData.clientName ?: '') }" required />
                        </div>
                        <div class="transfer-wizard-field">
                            <label for="maternitySerialNumberEmr">UPID</label>
                            <input type="text" id="maternitySerialNumberEmr" name="serialNumberEmr" value="${ ui.encodeHtmlAttribute(formData.serialNumberEmr ?: '') }" />
                        </div>
                        <div class="transfer-wizard-field">
                            <label for="maternityAgeOrDob">Age / DOB</label>
                            <input type="text" id="maternityAgeOrDob" name="ageOrDob" value="${ ui.encodeHtmlAttribute(formData.ageOrDob ?: '') }" />
                        </div>
                        <div class="transfer-wizard-field">
                            <label for="maternityNextOfKinName">Next of kin name</label>
                            <input type="text" id="maternityNextOfKinName" name="nextOfKinName" value="${ ui.encodeHtmlAttribute(formData.nextOfKinName ?: '') }" />
                        </div>
                        <div class="transfer-wizard-field">
                            <label for="maternityNextOfKinTelephone">Next of kin telephone</label>
                            <input type="tel" id="maternityNextOfKinTelephone" name="nextOfKinTelephone" value="${ ui.encodeHtmlAttribute(formData.nextOfKinTelephone ?: '') }" />
                        </div>
                        <div class="transfer-wizard-field">
                            <label for="maternityClientDistrict">District</label>
                            <input type="text" id="maternityClientDistrict" name="clientDistrict" value="${ ui.encodeHtmlAttribute(formData.clientDistrict ?: '') }" />
                        </div>
                        <div class="transfer-wizard-field">
                            <label for="maternitySector">Sector</label>
                            <input type="text" id="maternitySector" name="sector" value="${ ui.encodeHtmlAttribute(formData.sector ?: '') }" />
                        </div>
                        <div class="transfer-wizard-field">
                            <label for="maternityCell">Cell</label>
                            <input type="text" id="maternityCell" name="cell" value="${ ui.encodeHtmlAttribute(formData.cell ?: '') }" />
                        </div>
                        <div class="transfer-wizard-field">
                            <label for="maternityVillage">Village</label>
                            <input type="text" id="maternityVillage" name="village" value="${ ui.encodeHtmlAttribute(formData.village ?: '') }" />
                        </div>
                    </div>
                </div>

                <div class="transfer-wizard-section">
                    <h2 class="transfer-wizard-section-title">Referral information</h2>
                    <div class="transfer-wizard-row transfer-wizard-row-three-col">
                        <div class="transfer-wizard-field">
                            <label for="maternityAdmissionAt">Date &amp; time of admission</label>
                            <input type="text" class="js-datetime-picker" id="maternityAdmissionAt" name="admissionAt"
                                   value="${ ui.encodeHtmlAttribute(formData.admissionAt ?: '') }" placeholder="Select date and time" autocomplete="off" />
                        </div>
                        <div class="transfer-wizard-field">
                            <label for="maternityDecisionToTransferAt">Decision date &amp; time</label>
                            <input type="text" class="js-datetime-picker" id="maternityDecisionToTransferAt" name="decisionToTransferAt"
                                   value="${ ui.encodeHtmlAttribute(formData.decisionToTransferAt ?: '') }" required placeholder="Select date and time" autocomplete="off" />
                        </div>
                        <div class="transfer-wizard-field">
                            <label for="maternityCallingTime">Calling time</label>
                            <input type="text" class="js-time-picker" id="maternityCallingTime" name="callingTime"
                                   value="${ ui.encodeHtmlAttribute(formData.callingTime ?: '') }" placeholder="Select time" autocomplete="off" />
                        </div>
                        <div class="transfer-wizard-field">
                            <label for="maternityReceivingFacilityCode">Receiving facility</label>
                            <select id="maternityReceivingFacilityCode" name="receivingFacilityCode" required>
                                <option value="">Select receiving facility</option>
                                <% formData.receivingFacilities.each { facility -> %>
                                    <option value="${ ui.encodeHtmlAttribute(facility.value) }"
                                            data-receiving-facility-id="${ facility.receivingFacilityId ?: '' }"
                                            <% if (formData.receivingFacilityCode == facility.value) { %>selected="selected"<% } %>>
                                        ${ ui.format(facility.label) }
                                    </option>
                                <% } %>
                            </select>
                        </div>
                        <div class="transfer-wizard-field">
                            <label for="maternityReceivingService">${ ui.message("transferapp.patient.transfers.receivingService") }</label>
                            <select id="maternityReceivingService" name="receivingService" class="js-maternity-receiving-service-select" required
                                    data-placeholder="${ ui.encodeHtmlAttribute(ui.message('transferapp.patient.transfers.receivingService.placeholder')) }">
                                <option value=""></option>
                            </select>
                        </div>
                        <div class="transfer-wizard-field">
                            <label for="maternityStaffContactedName">Staff contacted</label>
                            <input type="text" id="maternityStaffContactedName" name="staffContactedName" value="${ ui.encodeHtmlAttribute(formData.staffContactedName ?: '') }" />
                        </div>
                        <div class="transfer-wizard-field">
                            <label for="maternityStaffContactedPhone">Contact phone</label>
                            <input type="tel" id="maternityStaffContactedPhone" name="staffContactedPhone" value="${ ui.encodeHtmlAttribute(formData.staffContactedPhone ?: '') }" />
                        </div>
                    </div>
                </div>

                <div class="transfer-wizard-section">
                    <h2 class="transfer-wizard-section-title">${ ui.message("transferapp.patient.transfers.transferType") }</h2>
                    <div class="transfer-type-options">
                        <% formData.transferTypes.each { type -> %>
                        <span class="transfer-type-option">
                            <input type="radio" name="transferType" id="maternityTransferType_${ ui.encodeHtmlAttribute(type.value) }"
                                   value="${ ui.encodeHtmlAttribute(type.value) }" <% if (formData.transferType == type.value) { %>checked="checked"<% } %> required />
                            <label for="maternityTransferType_${ ui.encodeHtmlAttribute(type.value) }">${ ui.format(type.label) }</label>
                        </span>
                        <% } %>
                    </div>
                </div>

                <div id="maternityEmergencyFields" class="transfer-emergency-panel">
                    <div class="transfer-wizard-row transfer-wizard-row-two-col">
                        <div class="transfer-wizard-field">
                            <label for="maternityAmbulanceCalledTime">${ ui.message("transferapp.patient.transfers.ambulanceCalledTime") }</label>
                            <input type="text" class="js-time-picker" id="maternityAmbulanceCalledTime" name="ambulanceCalledTime"
                                   value="" placeholder="Select time" autocomplete="off" data-emergency-required="true" />
                        </div>
                        <div class="transfer-wizard-field">
                            <label for="maternityDepartureFromReferringTime">${ ui.message("transferapp.patient.transfers.departureFromReferringTime") }</label>
                            <input type="text" class="js-time-picker" id="maternityDepartureFromReferringTime" name="departureFromReferringTime"
                                   value="" placeholder="Select time" autocomplete="off" data-emergency-required="true" />
                        </div>
                    </div>
                </div>

                <div class="transfer-wizard-section transfer-wizard-field">
                    <label for="maternityReasonForTransfer" class="transfer-wizard-section-title">Reason for transfer</label>
                    <textarea id="maternityReasonForTransfer" name="reasonForTransfer" rows="4" required placeholder="Enter the reason for transfer">${ ui.format(formData.reasonForTransfer ?: '') }</textarea>
                </div>

                <div class="transfer-wizard-section">
                    <div class="transfer-wizard-row transfer-wizard-row-two-col">
                        <div class="transfer-wizard-field">
                            <label><input type="checkbox" name="partographAttached" value="true" style="width:auto;display:inline-block;margin-right:0.4rem;" /> Partograph attached</label>
                        </div>
                        <div class="transfer-wizard-field">
                            <label for="maternityDisabilityType">If person with disability, type of disability</label>
                            <input type="text" id="maternityDisabilityType" name="disabilityType" value="${ ui.encodeHtmlAttribute(formData.disabilityType ?: '') }" />
                        </div>
                    </div>
                    <div class="transfer-wizard-field">
                        <label for="maternityClinicalPresentation">Clinical presentation</label>
                        <textarea id="maternityClinicalPresentation" name="clinicalPresentation" rows="3" placeholder="Enter clinical presentation">${ ui.format(formData.clinicalPresentation ?: '') }</textarea>
                    </div>
                </div>
            </div>

            <!-- Step 2: Obstetric History & Current Pregnancy -->
            <div class="transfer-wizard-step-panel" data-step="2">
                <div class="transfer-wizard-section">
                    <h2 class="transfer-wizard-section-title">Obstetric history</h2>
                    <div class="transfer-vitals-grid">
                        <div class="transfer-wizard-field transfer-vital-field">
                            <label for="obstetricGravida">Gravida</label>
                            <input type="text" id="obstetricGravida" name="obstetricGravida"  value="${ ui.encodeHtmlAttribute(formData.obstetricGravida ?: '') }" />
                        </div>
                        <div class="transfer-wizard-field transfer-vital-field">
                            <label for="obstetricParity">Parity</label>
                            <input type="text" id="obstetricParity" name="obstetricParity"  value="${ ui.encodeHtmlAttribute(formData.obstetricParity ?: '') }" />
                        </div>
                        <div class="transfer-wizard-field transfer-vital-field">
                            <label for="obstetricLivingChildren">Living children</label>
                            <input type="text" id="obstetricLivingChildren" name="obstetricLivingChildren"  value="${ ui.encodeHtmlAttribute(formData.obstetricLivingChildren ?: '') }" />
                        </div>
                        <div class="transfer-wizard-field transfer-vital-field">
                            <label for="obstetricAbortion">Abortion</label>
                            <input type="text" id="obstetricAbortion" name="obstetricAbortion"  value="${ ui.encodeHtmlAttribute(formData.obstetricAbortion ?: '') }" />
                        </div>
                        <div class="transfer-wizard-field transfer-vital-field">
                            <label for="obstetricStillbirth">Stillbirth</label>
                            <input type="text" id="obstetricStillbirth" name="obstetricStillbirth"  value="${ ui.encodeHtmlAttribute(formData.obstetricStillbirth ?: '') }" />
                        </div>
                        <div class="transfer-wizard-field transfer-vital-field">
                            <label for="obstetricNeonatalDeath">Neonatal death</label>
                            <input type="text" id="obstetricNeonatalDeath" name="obstetricNeonatalDeath"  value="${ ui.encodeHtmlAttribute(formData.obstetricNeonatalDeath ?: '') }" />
                        </div>
                        <div class="transfer-wizard-field transfer-vital-field">
                            <label for="obstetricPretermBirth">Preterm birth</label>
                            <input type="text" id="obstetricPretermBirth" name="obstetricPretermBirth"  value="${ ui.encodeHtmlAttribute(formData.obstetricPretermBirth ?: '') }" />
                        </div>
                    </div>
                </div>

                <div class="transfer-wizard-section">
                    <h2 class="transfer-wizard-section-title">Current pregnancy</h2>
                    <div class="transfer-wizard-row transfer-wizard-row-three-col">
                        <div class="transfer-wizard-field">
                            <label for="lmpDate">LMP date</label>
                            <input type="text" class="js-date-picker" id="lmpDate" name="lmpDate" placeholder="Select date" autocomplete="off"  value="${ ui.encodeHtmlAttribute(formData.lmpDate ?: '') }" />
                        </div>
                        <div class="transfer-wizard-field">
                            <label for="eddDate">EDD date</label>
                            <input type="text" class="js-date-picker" id="eddDate" name="eddDate" placeholder="Select date" autocomplete="off"  value="${ ui.encodeHtmlAttribute(formData.eddDate ?: '') }" />
                        </div>
                        <div class="transfer-wizard-field">
                            <label for="gestationAge">Gestation age</label>
                            <input type="text" id="gestationAge" name="gestationAge" placeholder="e.g. 34 weeks"  value="${ ui.encodeHtmlAttribute(formData.gestationAge ?: '') }" />
                        </div>
                        <div class="transfer-wizard-field">
                            <label for="obstetricMuac">MUAC</label>
                            <input type="text" id="obstetricMuac" name="muac"  value="${ ui.encodeHtmlAttribute(formData.muac ?: '') }" />
                        </div>
                        <div class="transfer-wizard-field">
                            <label for="ancCompletedCount">ANC visits completed</label>
                            <input type="text" id="ancCompletedCount" name="ancCompletedCount"  value="${ ui.encodeHtmlAttribute(formData.ancCompletedCount ?: '') }" />
                        </div>
                        <div class="transfer-wizard-field">
                            <label for="tetanusVaccineDoses">Tetanus vaccine doses</label>
                            <input type="text" id="tetanusVaccineDoses" name="tetanusVaccineDoses"  value="${ ui.encodeHtmlAttribute(formData.tetanusVaccineDoses ?: '') }" />
                        </div>
                    </div>
                    <div class="transfer-wizard-field">
                        <label for="previousSignificantHistory">Previous significant history</label>
                        <textarea id="previousSignificantHistory" name="previousSignificantHistory" rows="3">${ ui.format(formData.previousSignificantHistory ?: '') }</textarea>
                    </div>
                    <div class="transfer-wizard-field">
                        <label for="multiPregnanciesAndKnownHiv">Multi pregnancies and known HIV</label>
                        <textarea id="multiPregnanciesAndKnownHiv" name="multiPregnanciesAndKnownHiv" rows="2">${ ui.format(formData.multiPregnanciesAndKnownHiv ?: '') }</textarea>
                    </div>
                    <div class="transfer-wizard-field">
                        <label for="currentPregnancyComplications">Current pregnancy complications</label>
                        <textarea id="currentPregnancyComplications" name="currentPregnancyComplications" rows="3">${ ui.format(formData.currentPregnancyComplications ?: '') }</textarea>
                    </div>
                </div>
            </div>

            <!-- Step 3: Clinical Findings -->
            <div class="transfer-wizard-step-panel" data-step="3">
                <div class="transfer-wizard-section">
                    <h2 class="transfer-wizard-section-title">Latest results</h2>
                    <div class="transfer-wizard-row transfer-wizard-row-three-col">
                        <div class="transfer-wizard-field">
                            <label for="latestHemoglobin">Hemoglobin</label>
                            <input type="text" id="latestHemoglobin" name="latestHemoglobin"  value="${ ui.encodeHtmlAttribute(formData.latestHemoglobin ?: '') }" />
                        </div>
                        <div class="transfer-wizard-field">
                            <label for="latestHivStatus">HIV status</label>
                            <input type="text" id="latestHivStatus" name="latestHivStatus"  value="${ ui.encodeHtmlAttribute(formData.latestHivStatus ?: '') }" />
                        </div>
                        <div class="transfer-wizard-field">
                            <label for="latestBloodGroup">Blood group</label>
                            <input type="text" id="latestBloodGroup" name="latestBloodGroup"  value="${ ui.encodeHtmlAttribute(formData.latestBloodGroup ?: '') }" />
                        </div>
                    </div>
                    <div class="transfer-wizard-field">
                        <label for="latestOtherResults">Other results</label>
                        <textarea id="latestOtherResults" name="latestOtherResults" rows="2">${ ui.format(formData.latestOtherResults ?: '') }</textarea>
                    </div>
                </div>

                <div class="transfer-wizard-section">
                    <h2 class="transfer-wizard-section-title">Vital signs</h2>
                    <div class="transfer-vitals-grid">
                        <div class="transfer-wizard-field transfer-vital-field">
                            <label for="maternityVitalBp">BP</label>
                            <input type="text" id="maternityVitalBp" name="vitalBp"  value="${ ui.encodeHtmlAttribute(formData.vitalBp ?: '') }" />
                        </div>
                        <div class="transfer-wizard-field transfer-vital-field">
                            <label for="maternityVitalTemp">Temp</label>
                            <input type="text" id="maternityVitalTemp" name="vitalTemp"  value="${ ui.encodeHtmlAttribute(formData.vitalTemp ?: '') }" />
                        </div>
                        <div class="transfer-wizard-field transfer-vital-field">
                            <label for="maternityVitalSpo2">SpO2</label>
                            <input type="text" id="maternityVitalSpo2" name="vitalSpo2"  value="${ ui.encodeHtmlAttribute(formData.vitalSpo2 ?: '') }" />
                        </div>
                        <div class="transfer-wizard-field transfer-vital-field">
                            <label for="maternityVitalRr">RR</label>
                            <input type="text" id="maternityVitalRr" name="vitalRr"  value="${ ui.encodeHtmlAttribute(formData.vitalRr ?: '') }" />
                        </div>
                        <div class="transfer-wizard-field transfer-vital-field">
                            <label for="maternityVitalPulse">Pulse</label>
                            <input type="text" id="maternityVitalPulse" name="vitalPulse"  value="${ ui.encodeHtmlAttribute(formData.vitalPulse ?: '') }" />
                        </div>
                        <div class="transfer-wizard-field transfer-vital-field">
                            <label for="maternityVitalWeight">Weight</label>
                            <input type="text" id="maternityVitalWeight" name="vitalWeight"  value="${ ui.encodeHtmlAttribute(formData.vitalWeight ?: '') }" />
                        </div>
                        <div class="transfer-wizard-field transfer-vital-field">
                            <label for="maternityVitalHeight">Height</label>
                            <input type="text" id="maternityVitalHeight" name="vitalHeight"  value="${ ui.encodeHtmlAttribute(formData.vitalHeight ?: '') }" />
                        </div>
                    </div>
                </div>

                <div class="transfer-wizard-section">
                    <h2 class="transfer-wizard-section-title">Abdominal &amp; vaginal exam</h2>
                    <div class="transfer-wizard-row transfer-wizard-row-three-col">
                        <div class="transfer-wizard-field">
                            <label for="fetalPresentation">Fetal presentation</label>
                            <select id="fetalPresentation" name="fetalPresentation">
                                <option value="">Select presentation</option>
                                <option value="Cephalic" <% if (formData.fetalPresentation == 'Cephalic') { %>selected="selected"<% } %>>Cephalic</option>
                                <option value="Breech" <% if (formData.fetalPresentation == 'Breech') { %>selected="selected"<% } %>>Breech</option>
                                <option value="Transverse" <% if (formData.fetalPresentation == 'Transverse') { %>selected="selected"<% } %>>Transverse</option>
                                <option value="Oblique" <% if (formData.fetalPresentation == 'Oblique') { %>selected="selected"<% } %>>Oblique</option>
                                <option value="Face" <% if (formData.fetalPresentation == 'Face') { %>selected="selected"<% } %>>Face</option>
                                <option value="Brow" <% if (formData.fetalPresentation == 'Brow') { %>selected="selected"<% } %>>Brow</option>
                                <option value="Compound" <% if (formData.fetalPresentation == 'Compound') { %>selected="selected"<% } %>>Compound</option>
                                <option value="Unknown" <% if (formData.fetalPresentation == 'Unknown') { %>selected="selected"<% } %>>Unknown / Not assessed</option>
                            </select>
                        </div>
                        <div class="transfer-wizard-field">
                            <label for="fundalHeight">Fundal height</label>
                            <input type="text" id="fundalHeight" name="fundalHeight"  value="${ ui.encodeHtmlAttribute(formData.fundalHeight ?: '') }" />
                        </div>
                        <div class="transfer-wizard-field">
                            <label for="fetalHeartRate">Fetal heart rate</label>
                            <input type="text" id="fetalHeartRate" name="fetalHeartRate"  value="${ ui.encodeHtmlAttribute(formData.fetalHeartRate ?: '') }" />
                        </div>
                        <div class="transfer-wizard-field">
                            <label for="contractions">Contractions</label>
                            <input type="text" id="contractions" name="contractions"  value="${ ui.encodeHtmlAttribute(formData.contractions ?: '') }" />
                        </div>
                        <div class="transfer-wizard-field">
                            <label for="vaginalExamAt">Vaginal exam date &amp; time</label>
                            <input type="text" class="js-datetime-picker" id="vaginalExamAt" name="vaginalExamAt" placeholder="Select date and time" autocomplete="off"  value="${ ui.encodeHtmlAttribute(formData.vaginalExamAt ?: '') }" />
                        </div>
                        <div class="transfer-wizard-field">
                            <label for="dilation">Dilation</label>
                            <input type="text" id="dilation" name="dilation"  value="${ ui.encodeHtmlAttribute(formData.dilation ?: '') }" />
                        </div>
                        <div class="transfer-wizard-field">
                            <label for="effacement">Effacement</label>
                            <input type="text" id="effacement" name="effacement"  value="${ ui.encodeHtmlAttribute(formData.effacement ?: '') }" />
                        </div>
                        <div class="transfer-wizard-field">
                            <label for="descent">Descent</label>
                            <input type="text" id="descent" name="descent"  value="${ ui.encodeHtmlAttribute(formData.descent ?: '') }" />
                        </div>
                        <div class="transfer-wizard-field">
                            <label for="consistency">Consistency</label>
                            <select id="consistency" name="consistency">
                                <option value="">Select consistency</option>
                                <option value="Firm" <% if (formData.consistency == 'Firm') { %>selected="selected"<% } %>>Firm</option>
                                <option value="Medium" <% if (formData.consistency == 'Medium') { %>selected="selected"<% } %>>Medium</option>
                                <option value="Soft" <% if (formData.consistency == 'Soft') { %>selected="selected"<% } %>>Soft</option>
                            </select>
                        </div>
                        <div class="transfer-wizard-field">
                            <label for="maternityPosition">Position</label>
                            <select id="maternityPosition" name="position">
                                <option value="">Select position</option>
                                <option value="Occipito-Anterior (OA)" <% if (formData.position == 'Occipito-Anterior (OA)') { %>selected="selected"<% } %>>Occipito-Anterior (OA)</option>
                                <option value="Left Occipito-Anterior (LOA)" <% if (formData.position == 'Left Occipito-Anterior (LOA)') { %>selected="selected"<% } %>>Left Occipito-Anterior (LOA)</option>
                                <option value="Right Occipito-Anterior (ROA)" <% if (formData.position == 'Right Occipito-Anterior (ROA)') { %>selected="selected"<% } %>>Right Occipito-Anterior (ROA)</option>
                                <option value="Occipito-Transverse (OT)" <% if (formData.position == 'Occipito-Transverse (OT)') { %>selected="selected"<% } %>>Occipito-Transverse (OT)</option>
                                <option value="Left Occipito-Transverse (LOT)" <% if (formData.position == 'Left Occipito-Transverse (LOT)') { %>selected="selected"<% } %>>Left Occipito-Transverse (LOT)</option>
                                <option value="Right Occipito-Transverse (ROT)" <% if (formData.position == 'Right Occipito-Transverse (ROT)') { %>selected="selected"<% } %>>Right Occipito-Transverse (ROT)</option>
                                <option value="Occipito-Posterior (OP)" <% if (formData.position == 'Occipito-Posterior (OP)') { %>selected="selected"<% } %>>Occipito-Posterior (OP)</option>
                                <option value="Left Occipito-Posterior (LOP)" <% if (formData.position == 'Left Occipito-Posterior (LOP)') { %>selected="selected"<% } %>>Left Occipito-Posterior (LOP)</option>
                                <option value="Right Occipito-Posterior (ROP)" <% if (formData.position == 'Right Occipito-Posterior (ROP)') { %>selected="selected"<% } %>>Right Occipito-Posterior (ROP)</option>
                                <option value="Unknown" <% if (formData.position == 'Unknown') { %>selected="selected"<% } %>>Unknown / Not assessed</option>
                            </select>
                        </div>
                    </div>
                    <div class="transfer-wizard-row transfer-wizard-row-two-col" style="margin-top:0.75rem;">
                        <div class="transfer-wizard-field">
                            <label for="caput">Caput</label>
                            <select id="caput" name="caput">
                                <option value="">Select</option>
                                <option value="true" <% if (formData.caput == 'true') { %>selected="selected"<% } %>>Yes</option>
                                <option value="false" <% if (formData.caput == 'false') { %>selected="selected"<% } %>>No</option>
                            </select>
                        </div>
                        <div class="transfer-wizard-field">
                            <label for="moulding">Moulding</label>
                            <select id="moulding" name="moulding">
                                <option value="">Select</option>
                                <option value="true" <% if (formData.moulding == 'true') { %>selected="selected"<% } %>>Yes</option>
                                <option value="false" <% if (formData.moulding == 'false') { %>selected="selected"<% } %>>No</option>
                            </select>
                        </div>
                        <div class="transfer-wizard-field">
                            <label for="membranesRuptured">Membranes ruptured</label>
                            <select id="membranesRuptured" name="membranesRuptured">
                                <option value="">Select</option>
                                <option value="true" <% if (formData.membranesRuptured == 'true') { %>selected="selected"<% } %>>Yes</option>
                                <option value="false" <% if (formData.membranesRuptured == 'false') { %>selected="selected"<% } %>>No</option>
                            </select>
                        </div>
                        <div class="transfer-wizard-field">
                            <label for="membranesRupturedAt">If yes: date and time</label>
                            <input type="text" class="js-datetime-picker" id="membranesRupturedAt" name="membranesRupturedAt" placeholder="Select date and time" autocomplete="off"  value="${ ui.encodeHtmlAttribute(formData.membranesRupturedAt ?: '') }" />
                        </div>
                        <div class="transfer-wizard-field">
                            <label for="amnioticFluidColor">Color/character of amniotic fluid</label>
                            <select id="amnioticFluidColor" name="amnioticFluidColor">
                                <option value="">Select</option>
                                <option value="Clear" <% if (formData.amnioticFluidColor == 'Clear') { %>selected="selected"<% } %>>Clear</option>
                                <option value="Meconium" <% if (formData.amnioticFluidColor == 'Meconium') { %>selected="selected"<% } %>>Meconium</option>
                                <option value="Bloody" <% if (formData.amnioticFluidColor == 'Bloody') { %>selected="selected"<% } %>>Bloody</option>
                                <option value="Offensive" <% if (formData.amnioticFluidColor == 'Offensive') { %>selected="selected"<% } %>>Offensive</option>
                            </select>
                        </div>
                        <div class="transfer-wizard-field">
                            <label for="estimatedBloodLossMl">If bloody: estimated blood loss (mL)</label>
                            <input type="text" id="estimatedBloodLossMl" name="estimatedBloodLossMl"  value="${ ui.encodeHtmlAttribute(formData.estimatedBloodLossMl ?: '') }" />
                        </div>
                    </div>
                </div>

                <div class="transfer-wizard-section">
                    <h2 class="transfer-wizard-section-title">Investigations &amp; diagnosis</h2>
                    <div class="transfer-wizard-row transfer-wizard-row-three-col">
                        <div class="transfer-wizard-field">
                            <label for="investigationHgb">Investigation HGB</label>
                            <input type="text" id="investigationHgb" name="investigationHgb"  value="${ ui.encodeHtmlAttribute(formData.investigationHgb ?: '') }" />
                        </div>
                        <div class="transfer-wizard-field">
                            <label for="investigationUrineTest">Urine test</label>
                            <input type="text" id="investigationUrineTest" name="investigationUrineTest"  value="${ ui.encodeHtmlAttribute(formData.investigationUrineTest ?: '') }" />
                        </div>
                        <div class="transfer-wizard-field">
                            <label for="investigationOtherTest">Other test</label>
                            <input type="text" id="investigationOtherTest" name="investigationOtherTest"  value="${ ui.encodeHtmlAttribute(formData.investigationOtherTest ?: '') }" />
                        </div>
                    </div>
                    <div class="transfer-wizard-field">
                        <label for="imagingInvestigations">Imaging investigations</label>
                        <textarea id="imagingInvestigations" name="imagingInvestigations" rows="2">${ ui.format(formData.imagingInvestigations ?: '') }</textarea>
                    </div>
                    <div class="transfer-wizard-field">
                        <label for="maternityDiagnosis">Diagnosis</label>
                        <textarea id="maternityDiagnosis" name="diagnosis" rows="3">${ ui.format(formData.diagnosis ?: '') }</textarea>
                    </div>
                    <div class="transfer-wizard-field">
                        <label for="maternityProcedures">Procedures</label>
                        <textarea id="maternityProcedures" name="procedures" rows="3">${ ui.format(formData.procedures ?: '') }</textarea>
                    </div>
                    <div class="transfer-wizard-row transfer-wizard-row-two-col">
                        <div class="transfer-wizard-field">
                            <label for="attachedLabTests">Lab tests attached</label>
                            <select id="attachedLabTests" name="attachedLabTests">
                                <option value="">Select</option>
                                <option value="true" <% if (formData.attachedLabTests == 'true') { %>selected="selected"<% } %>>Yes</option>
                                <option value="false" <% if (formData.attachedLabTests == 'false') { %>selected="selected"<% } %>>No</option>
                            </select>
                        </div>
                        <div class="transfer-wizard-field">
                            <label for="attachedImaging">Imaging attached</label>
                            <select id="attachedImaging" name="attachedImaging">
                                <option value="">Select</option>
                                <option value="true" <% if (formData.attachedImaging == 'true') { %>selected="selected"<% } %>>Yes</option>
                                <option value="false" <% if (formData.attachedImaging == 'false') { %>selected="selected"<% } %>>No</option>
                            </select>
                        </div>
                    </div>
                    <div class="transfer-wizard-field">
                        <label for="attachedOther">Other attachments</label>
                        <input type="text" id="attachedOther" name="attachedOther"  value="${ ui.encodeHtmlAttribute(formData.attachedOther ?: '') }" />
                    </div>
                </div>
            </div>

            <!-- Step 4: Treatment & Transport -->
            <div class="transfer-wizard-step-panel" data-step="4">
                <div class="transfer-wizard-section">
                    <h2 class="transfer-wizard-section-title">Treatment given</h2>
                    <div style="overflow-x:auto;">
                        <table id="maternity-treatment-table" style="border-collapse:collapse;width:100%;">
                            <thead>
                                <tr>
                                    <th style="text-align:left;padding:0.4rem;border-bottom:2px solid #e2e8f0;">Treatment</th>
                                    <th style="text-align:left;padding:0.4rem;border-bottom:2px solid #e2e8f0;">Dose</th>
                                    <th style="text-align:left;padding:0.4rem;border-bottom:2px solid #e2e8f0;">Date</th>
                                    <th style="text-align:left;padding:0.4rem;border-bottom:2px solid #e2e8f0;">Time</th>
                                    <th style="border-bottom:2px solid #e2e8f0;"></th>
                                </tr>
                            </thead>
                            <tbody id="maternity-treatment-table-body">
                                <% formData.defaultTreatmentRows.each { row -> %>
                                <tr class="maternity-treatment-row">
                                    <td style="padding:0.3rem;border-bottom:1px solid #eef2f6;">
                                        <select name="treatmentName">
                                            <option value="">Select treatment</option>
                                            <% ["IV Fluids", "Dexamethasone", "Magnesium sulphate", "Nifedipine", "Oxytocin", "ATBs"].each { option -> %>
                                                <option value="${ ui.encodeHtmlAttribute(option) }"${ option == row.treatmentName ? ' selected="selected"' : '' }>${ ui.format(option) }</option>
                                            <% } %>
                                        </select>
                                    </td>
                                    <td style="padding:0.3rem;border-bottom:1px solid #eef2f6;"><input type="text" name="treatmentDose" value="${ ui.encodeHtmlAttribute(row.dose ?: '') }" /></td>
                                    <td style="padding:0.3rem;border-bottom:1px solid #eef2f6;"><input type="text" class="js-date-picker" name="treatmentGivenDate" value="${ ui.encodeHtmlAttribute(row.givenDate ?: '') }" placeholder="Date" autocomplete="off" /></td>
                                    <td style="padding:0.3rem;border-bottom:1px solid #eef2f6;"><input type="text" class="js-time-picker" name="treatmentGivenTime" value="${ ui.encodeHtmlAttribute(row.givenTime ?: '') }" placeholder="Time" autocomplete="off" /></td>
                                    <td style="padding:0.3rem;border-bottom:1px solid #eef2f6;"><button type="button" class="transfer-wizard-btn transfer-wizard-btn-outline js-remove-treatment-row">Remove</button></td>
                                </tr>
                                <% } %>
                            </tbody>
                        </table>
                    </div>
                    <button type="button" id="maternity-add-treatment-row" class="transfer-wizard-btn transfer-wizard-btn-outline" style="margin-top:0.5rem;">Add treatment row</button>
                </div>

                <div class="transfer-wizard-section">
                    <h2 class="transfer-wizard-section-title">${ ui.message("transferapp.patient.transfers.transportationType") }</h2>
                    <div class="transfer-transport-options">
                        <% formData.transportationTypes.each { transport -> %>
                        <span class="transfer-transport-option">
                            <input type="radio" name="transportationType"
                                   id="maternityTransportationType_${ ui.encodeHtmlAttribute(transport.value) }"
                                   value="${ ui.encodeHtmlAttribute(transport.value) }"
                                   data-transport-value="${ ui.encodeHtmlAttribute(transport.value) }"
                                   <% if (formData.transportationType == transport.value) { %>checked="checked"<% } %> />
                            <label for="maternityTransportationType_${ ui.encodeHtmlAttribute(transport.value) }">${ ui.format(transport.label) }</label>
                        </span>
                        <% } %>
                    </div>
                    <div id="maternityTransportOtherField" class="transfer-transport-other transfer-wizard-field">
                        <label for="maternityTransportationOtherSpec">${ ui.message("transferapp.patient.transfers.transportationOtherSpec") }</label>
                        <input type="text" id="maternityTransportationOtherSpec" name="transportationOtherSpec" maxlength="255"
                               placeholder="${ ui.message('transferapp.patient.transfers.transportationOtherSpec.placeholder') }"  value="${ ui.encodeHtmlAttribute(formData.transportationOtherSpec ?: '') }" />
                    </div>
                </div>

                <div class="transfer-wizard-section">
                    <h2 class="transfer-wizard-section-title">Health insurance</h2>
                    <div class="transfer-insurance-options">
                        <% formData.healthInsuranceTypes.each { insurance -> %>
                        <span class="transfer-insurance-option">
                            <input type="radio" name="healthInsuranceType"
                                   id="maternityHealthInsuranceType_${ ui.encodeHtmlAttribute(insurance.value) }"
                                   value="${ ui.encodeHtmlAttribute(insurance.value) }" <% if (formData.healthInsuranceType == insurance.value) { %>checked="checked"<% } %> required />
                            <label for="maternityHealthInsuranceType_${ ui.encodeHtmlAttribute(insurance.value) }">${ ui.format(insurance.label) }</label>
                        </span>
                        <% } %>
                    </div>
                    <div id="maternityInsuranceOtherField" class="transfer-insurance-other transfer-wizard-field">
                        <label for="maternityHealthInsuranceOtherSpec">Other (specify)</label>
                        <input type="text" id="maternityHealthInsuranceOtherSpec" name="healthInsuranceOtherSpec" maxlength="255"  value="${ ui.encodeHtmlAttribute(formData.healthInsuranceOtherSpec ?: '') }" />
                    </div>
                </div>
            </div>

            <!-- Step 5: Sign-off -->
            <div class="transfer-wizard-step-panel" data-step="5">
                <div class="transfer-wizard-section">
                    <h2 class="transfer-wizard-section-title">Referring provider sign-off</h2>
                    <div class="transfer-wizard-row transfer-wizard-row-three-col">
                        <div class="transfer-wizard-field">
                            <label for="maternityReferringProviderName">Provider name</label>
                            <input type="text" id="maternityReferringProviderName" name="referringProviderName"
                                   value="${ ui.encodeHtmlAttribute(formData.referringProviderName ?: '') }" />
                        </div>
                        <div class="transfer-wizard-field">
                            <label for="maternityReferringProviderQualification">Qualification</label>
                            <input type="text" id="maternityReferringProviderQualification" name="referringProviderQualification"  value="${ ui.encodeHtmlAttribute(formData.referringProviderQualification ?: '') }" />
                        </div>
                        <div class="transfer-wizard-field">
                            <label for="maternityReferringProviderPhone">Provider phone</label>
                            <input type="tel" id="maternityReferringProviderPhone" name="referringProviderPhone" />
                        </div>
                        <div class="transfer-wizard-field">
                            <label for="maternityReferringSignedDate">Date</label>
                            <input type="text" class="js-date-picker" id="maternityReferringSignedDate" name="referringSignedDate"
                                   value="${ ui.encodeHtmlAttribute(formData.referringSignedDate ?: '') }" placeholder="Select date" autocomplete="off" />
                        </div>
                        <div class="transfer-wizard-field">
                            <label for="maternityReferringSignedTime">Time</label>
                            <input type="text" class="js-time-picker" id="maternityReferringSignedTime" name="referringSignedTime"
                                   value="${ ui.encodeHtmlAttribute(formData.referringSignedTime ?: '') }" placeholder="Select time" autocomplete="off" />
                        </div>
                    </div>
                </div>
            </div>

        </form>
        </div>
    </div>
</div>
</g:if>
