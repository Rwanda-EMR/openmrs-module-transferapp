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
        <h1 class="transfer-wizard-page-title" id="neonatal-wizard-page-title">Neonatal Transfer Form</h1>
        <p class="transfer-wizard-page-lead" id="neonatal-wizard-page-lead">Step 1 — baby identification, mother details, and referral.</p>
    </header>

    <div class="transfer-wizard-panel" style="padding: 0 5px;">
        <ul class="transfer-wizard-progress transfer-wizard-progress-7">
            <li data-step="1"><span class="transfer-wizard-step-num">1</span><span class="transfer-wizard-step-label">Baby &amp; Referral</span></li>
            <li data-step="2"><span class="transfer-wizard-step-num">2</span><span class="transfer-wizard-step-label">Maternal History</span></li>
            <li data-step="3"><span class="transfer-wizard-step-num">3</span><span class="transfer-wizard-step-label">Labor Details</span></li>
            <li data-step="4"><span class="transfer-wizard-step-num">4</span><span class="transfer-wizard-step-label">Neonatal History &amp; Drugs</span></li>
            <li data-step="5"><span class="transfer-wizard-step-num">5</span><span class="transfer-wizard-step-label">Chief Complaint &amp; Diagnoses</span></li>
            <li data-step="6"><span class="transfer-wizard-step-num">6</span><span class="transfer-wizard-step-label">Management</span></li>
            <li data-step="7"><span class="transfer-wizard-step-num">7</span><span class="transfer-wizard-step-label">Summary &amp; Sign-off</span></li>
        </ul>

        <div class="transfer-wizard-scroll">
        <form id="moh-neonatal-transfer-wizard-form" class="transfer-out-form" novalidate="novalidate"
              data-editing="${ formData.transferUuid ? 'true' : 'false' }"
              data-client-name="${ ui.encodeHtmlAttribute(formData.babyName ?: '') }">
            <input type="hidden" name="patientId" value="${ formData.patientId }" />
            <input type="hidden" name="transferUuid" value="${ ui.encodeHtmlAttribute(formData.transferUuid ?: '') }" />
            <input type="hidden" id="neonatalReceivingFacilityId" name="receivingFacilityId" value="" />

            <!-- Step 1: Baby & Referral Info -->
            <div class="transfer-wizard-step-panel is-active" data-step="1">
                <div class="transfer-wizard-section">
                    <h2 class="transfer-wizard-section-title">Facility details</h2>
                    <div class="transfer-wizard-row transfer-wizard-row-three-col">
                        <div class="transfer-wizard-field">
                            <label for="neonatalProvince">${ ui.message("transferapp.patient.transfers.province") }</label>
                            <input type="text" id="neonatalProvince" name="province" value="${ ui.encodeHtmlAttribute(formData.province ?: '') }" />
                        </div>
                        <div class="transfer-wizard-field">
                            <label for="neonatalDistrict">${ ui.message("transferapp.patient.transfers.district") }</label>
                            <input type="text" id="neonatalDistrict" name="district" value="${ ui.encodeHtmlAttribute(formData.district ?: '') }" />
                        </div>
                        <div class="transfer-wizard-field">
                            <label for="neonatalHospitalName">${ ui.message("transferapp.patient.transfers.hospitalName") }</label>
                            <input type="text" id="neonatalHospitalName" name="hospitalName" value="${ ui.encodeHtmlAttribute(formData.hospitalName ?: '') }" />
                        </div>
                        <div class="transfer-wizard-field">
                            <label for="neonatalReferringFacilityName">${ ui.message("transferapp.patient.transfers.referringFacilityName") }</label>
                            <input type="text" id="neonatalReferringFacilityName" name="referringFacilityName" value="${ ui.encodeHtmlAttribute(formData.referringFacilityName ?: '') }" />
                        </div>
                        <div class="transfer-wizard-field">
                            <label for="neonatalReferringUnit">${ ui.message("transferapp.patient.transfers.referringUnit") }</label>
                            <input type="text" id="neonatalReferringUnit" name="referringUnit" value="${ ui.encodeHtmlAttribute(formData.referringUnit ?: '') }" />
                        </div>
                    </div>
                </div>

                <div class="transfer-wizard-section">
                    <h2 class="transfer-wizard-section-title">Baby information</h2>
                    <div class="transfer-wizard-row transfer-wizard-row-three-col">
                        <div class="transfer-wizard-field">
                            <label for="neonatalBabyName">${ ui.message("transferapp.patient.transfers.babyName") }</label>
                            <input type="text" id="neonatalBabyName" name="babyName" value="${ ui.encodeHtmlAttribute(formData.babyName ?: '') }" required />
                        </div>
                        <div class="transfer-wizard-field">
                            <label for="neonatalSex">${ ui.message("transferapp.patient.transfers.sex") }</label>
                            <select id="neonatalSex" name="sex">
                                <option value="">Select sex</option>
                                <option value="M" <% if (formData.sex == "M") { %>selected="selected"<% } %>>Male</option>
                                <option value="F" <% if (formData.sex == "F") { %>selected="selected"<% } %>>Female</option>
                                <option value="Unknown" <% if (formData.sex == "Unknown") { %>selected="selected"<% } %>>Unknown</option>
                            </select>
                        </div>
                        <div class="transfer-wizard-field">
                            <label for="neonatalDob">${ ui.message("transferapp.patient.transfers.dob") }</label>
                            <input type="text" class="js-date-picker" id="neonatalDob" name="dob"
                                   value="${ ui.encodeHtmlAttribute(formData.dob ?: '') }" placeholder="Select date" autocomplete="off" />
                        </div>
                        <div class="transfer-wizard-field">
                            <label for="neonatalGestationalAgeWeeks">${ ui.message("transferapp.patient.transfers.gestationalAgeWeeks") }</label>
                            <input type="text" id="neonatalGestationalAgeWeeks" name="gestationalAgeWeeks"  value="${ ui.encodeHtmlAttribute(formData.gestationalAgeWeeks ?: '') }" />
                        </div>
                        <div class="transfer-wizard-field">
                            <label for="neonatalBirthWeightG">${ ui.message("transferapp.patient.transfers.birthWeightG") }</label>
                            <input type="text" id="neonatalBirthWeightG" name="birthWeightG"  value="${ ui.encodeHtmlAttribute(formData.birthWeightG ?: '') }" />
                        </div>
                        <div class="transfer-wizard-field">
                            <label for="neonatalCurrentWeightG">${ ui.message("transferapp.patient.transfers.currentWeightG") }</label>
                            <input type="text" id="neonatalCurrentWeightG" name="currentWeightG"  value="${ ui.encodeHtmlAttribute(formData.currentWeightG ?: '') }" />
                        </div>
                        <div class="transfer-wizard-field">
                            <label for="neonatalCurrentAgeDays">${ ui.message("transferapp.patient.transfers.currentAgeDays") }</label>
                            <input type="text" id="neonatalCurrentAgeDays" name="currentAgeDays"  value="${ ui.encodeHtmlAttribute(formData.currentAgeDays ?: '') }" />
                        </div>
                    </div>
                </div>

                <div class="transfer-wizard-section">
                    <h2 class="transfer-wizard-section-title">Mother information</h2>
                    <div class="transfer-wizard-row transfer-wizard-row-three-col">
                        <div class="transfer-wizard-field">
                            <label for="neonatalMotherName">${ ui.message("transferapp.patient.transfers.motherName") }</label>
                            <input type="text" id="neonatalMotherName" name="motherName" value="${ ui.encodeHtmlAttribute(formData.motherName ?: '') }" />
                        </div>
                        <div class="transfer-wizard-field">
                            <label for="neonatalMotherAge">${ ui.message("transferapp.patient.transfers.motherAge") }</label>
                            <input type="text" id="neonatalMotherAge" name="motherAge"  value="${ ui.encodeHtmlAttribute(formData.motherAge ?: '') }" />
                        </div>
                        <div class="transfer-wizard-field">
                            <label for="neonatalMotherCaregiverPhone">${ ui.message("transferapp.patient.transfers.motherCaregiverPhone") }</label>
                            <input type="tel" id="neonatalMotherCaregiverPhone" name="motherCaregiverPhone" value="${ ui.encodeHtmlAttribute(formData.motherCaregiverPhone ?: '') }" />
                        </div>
                    </div>
                </div>

                <div class="transfer-wizard-section">
                    <h2 class="transfer-wizard-section-title">Referral information</h2>
                    <div class="transfer-wizard-row transfer-wizard-row-three-col">
                        <div class="transfer-wizard-field">
                            <label for="neonatalPlaceOfBirth">${ ui.message("transferapp.patient.transfers.placeOfBirth") }</label>
                            <select id="neonatalPlaceOfBirth" name="placeOfBirth">
                                <option value="">Select place of birth</option>
                                <option value="Home" <% if (formData.placeOfBirth == "Home") { %>selected="selected"<% } %>>Home</option>
                                <option value="Private facility" <% if (formData.placeOfBirth == "Private facility") { %>selected="selected"<% } %>>Private facility</option>
                                <option value="En-route" <% if (formData.placeOfBirth == "En-route") { %>selected="selected"<% } %>>En-route</option>
                                <option value="Public facility" <% if (formData.placeOfBirth == "Public facility") { %>selected="selected"<% } %>>Public facility</option>
                            </select>
                        </div>
                        <div class="transfer-wizard-field">
                            <label for="neonatalDecisionToTransferAt">${ ui.message("transferapp.patient.transfers.decisionToTransferAt") }</label>
                            <input type="text" class="js-datetime-picker" id="neonatalDecisionToTransferAt" name="decisionToTransferAt"
                                   value="${ ui.encodeHtmlAttribute(formData.decisionToTransferAt ?: '') }" required placeholder="Select date and time" autocomplete="off" />
                        </div>
                        <div class="transfer-wizard-field">
                            <label for="neonatalCallingTime">${ ui.message("transferapp.patient.transfers.callingTime") }</label>
                            <input type="text" class="js-time-picker" id="neonatalCallingTime" name="callingTime"
                                   value="${ ui.encodeHtmlAttribute(formData.callingTime ?: '') }" placeholder="Select time" autocomplete="off" />
                        </div>
                        <div class="transfer-wizard-field">
                            <label for="neonatalReceivingFacilityCode">${ ui.message("transferapp.patient.transfers.receivingFacilityCode") }</label>
                            <select id="neonatalReceivingFacilityCode" name="receivingFacilityCode" required>
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
                            <label for="neonatalReceivingService">${ ui.message("transferapp.patient.transfers.receivingService") }</label>
                            <select id="neonatalReceivingService" name="receivingService" class="js-neonatal-receiving-service-select" required
                                    data-placeholder="${ ui.encodeHtmlAttribute(ui.message('transferapp.patient.transfers.receivingService.placeholder')) }">
                                <option value=""></option>
                            </select>
                        </div>
                        <div class="transfer-wizard-field">
                            <label for="neonatalStaffContactedName">${ ui.message("transferapp.patient.transfers.staffContactedName") }</label>
                            <input type="text" id="neonatalStaffContactedName" name="staffContactedName" value="${ ui.encodeHtmlAttribute(formData.staffContactedName ?: '') }" />
                        </div>
                        <div class="transfer-wizard-field">
                            <label for="neonatalStaffContactedPhone">${ ui.message("transferapp.patient.transfers.staffContactedPhone") }</label>
                            <input type="tel" id="neonatalStaffContactedPhone" name="staffContactedPhone" value="${ ui.encodeHtmlAttribute(formData.staffContactedPhone ?: '') }" />
                        </div>
                    </div>
                </div>

                <div class="transfer-wizard-section">
                    <h2 class="transfer-wizard-section-title">${ ui.message("transferapp.patient.transfers.transferType") }</h2>
                    <div class="transfer-type-options">
                        <% formData.transferTypes.each { type -> %>
                        <span class="transfer-type-option">
                            <input type="radio" name="transferType" id="neonatalTransferType_${ ui.encodeHtmlAttribute(type.value) }"
                                   value="${ ui.encodeHtmlAttribute(type.value) }" <% if (formData.transferType == type.value) { %>checked="checked"<% } %> required />
                            <label for="neonatalTransferType_${ ui.encodeHtmlAttribute(type.value) }">${ ui.format(type.label) }</label>
                        </span>
                        <% } %>
                    </div>
                </div>

                <div class="transfer-wizard-section">
                    <h2 class="transfer-wizard-section-title">Mode of transport</h2>
                    <div class="transfer-transport-options">
                        <% formData.transportTypes.each { transport -> %>
                        <span class="transfer-transport-option">
                            <input type="radio" name="modeOfTransport"
                                   id="neonatalModeOfTransport_${ ui.encodeHtmlAttribute(transport.value) }"
                                   value="${ ui.encodeHtmlAttribute(transport.value) }" <% if (formData.modeOfTransport == transport.value) { %>checked="checked"<% } %> />
                            <label for="neonatalModeOfTransport_${ ui.encodeHtmlAttribute(transport.value) }">${ ui.format(transport.label) }</label>
                        </span>
                        <% } %>
                    </div>
                    <div id="neonatalTransportOtherField" class="transfer-transport-other transfer-wizard-field">
                        <label for="neonatalTransportOther">${ ui.message("transferapp.patient.transfers.transportOther") }</label>
                        <input type="text" id="neonatalTransportOther" name="transportOther" maxlength="255"  value="${ ui.encodeHtmlAttribute(formData.transportOther ?: '') }" />
                    </div>
                </div>

                <div class="transfer-wizard-section transfer-wizard-field">
                    <label for="neonatalReasonForTransfer" class="transfer-wizard-section-title">${ ui.message("transferapp.patient.transfers.reasonForTransfer") }</label>
                    <textarea id="neonatalReasonForTransfer" name="reasonForTransfer" rows="4" required placeholder="Enter the reason for transfer">${ ui.format(formData.reasonForTransfer ?: '') }</textarea>
                </div>
            </div>

            <!-- Step 2: Maternal History -->
            <div class="transfer-wizard-step-panel" data-step="2">
                <div class="transfer-wizard-section">
                    <h2 class="transfer-wizard-section-title">Maternal history</h2>
                    <div class="transfer-wizard-row transfer-wizard-row-three-col">
                        <div class="transfer-wizard-field">
                            <label for="neonatalMotherAlive">${ ui.message("transferapp.patient.transfers.motherAlive") }</label>
                            <select id="neonatalMotherAlive" name="motherAlive">
                                <option value="">Select status</option>
                                <option value="Alive" <% if (formData.motherAlive == 'Alive') { %>selected="selected"<% } %>>Alive</option>
                                <option value="Deceased" <% if (formData.motherAlive == 'Deceased') { %>selected="selected"<% } %>>Deceased</option>
                                <option value="Unknown" <% if (formData.motherAlive == 'Unknown') { %>selected="selected"<% } %>>Unknown</option>
                            </select>
                        </div>
                        <div class="transfer-wizard-field">
                            <label for="neonatalObstetricGravida">${ ui.message("transferapp.patient.transfers.obstetricGravida") }</label>
                            <input type="text" id="neonatalObstetricGravida" name="obstetricGravida"  value="${ ui.encodeHtmlAttribute(formData.obstetricGravida ?: '') }" />
                        </div>
                        <div class="transfer-wizard-field">
                            <label for="neonatalObstetricParity">${ ui.message("transferapp.patient.transfers.obstetricParity") }</label>
                            <input type="text" id="neonatalObstetricParity" name="obstetricParity"  value="${ ui.encodeHtmlAttribute(formData.obstetricParity ?: '') }" />
                        </div>
                        <div class="transfer-wizard-field">
                            <label for="neonatalPregnancyType">${ ui.message("transferapp.patient.transfers.pregnancyType") }</label>
                            <select id="neonatalPregnancyType" name="pregnancyType">
                                <option value="">Select type</option>
                                <option value="Singleton" <% if (formData.pregnancyType == 'Singleton') { %>selected="selected"<% } %>>Singleton</option>
                                <option value="Twin" <% if (formData.pregnancyType == 'Twin') { %>selected="selected"<% } %>>Twin</option>
                                <option value="Multiple" <% if (formData.pregnancyType == 'Multiple') { %>selected="selected"<% } %>>Multiple</option>
                            </select>
                        </div>
                        <div class="transfer-wizard-field">
                            <label for="neonatalBloodGroup">${ ui.message("transferapp.patient.transfers.bloodGroup") }</label>
                            <input type="text" id="neonatalBloodGroup" name="bloodGroup" placeholder="e.g. A+, or UnK"  value="${ ui.encodeHtmlAttribute(formData.bloodGroup ?: '') }" />
                        </div>
                        <div class="transfer-wizard-field">
                            <label for="neonatalRhFactor">${ ui.message("transferapp.patient.transfers.rhFactor") }</label>
                            <input type="text" id="neonatalRhFactor" name="rhFactor" placeholder="e.g. Positive, or UnK"  value="${ ui.encodeHtmlAttribute(formData.rhFactor ?: '') }" />
                        </div>
                        <div class="transfer-wizard-field">
                            <label for="neonatalTetanusVaccineDoses">${ ui.message("transferapp.patient.transfers.tetanusVaccineDoses") }</label>
                            <input type="text" id="neonatalTetanusVaccineDoses" name="tetanusVaccineDoses"  value="${ ui.encodeHtmlAttribute(formData.tetanusVaccineDoses ?: '') }" />
                        </div>
                    </div>
                    <div class="transfer-wizard-field">
                        <label class="transfer-wizard-section-title" style="font-size:1rem;">${ ui.message("transferapp.patient.transfers.ancScreening.label") }</label>
                        <div class="transfer-wizard-row transfer-wizard-row-three-col">
                            <% def ancScreeningValues = (formData.ancScreening ?: '').split('; ') as List %>
                            <% ["Toxoplasmosis", "Rubella", "Syphilis", "Hep B & C", "U/S", "Other"].each { option -> %>
                            <label style="font-weight:normal;">
                                <input type="checkbox" name="ancScreening" value="${ ui.encodeHtmlAttribute(option) }" <% if (ancScreeningValues.contains(option)) { %>checked="checked"<% } %> style="width:auto;display:inline-block;margin-right:0.4rem;" /> ${ ui.format(option) }
                            </label>
                            <% } %>
                        </div>
                    </div>
                    <div class="transfer-wizard-field">
                        <label class="transfer-wizard-section-title" style="font-size:1rem;">${ ui.message("transferapp.patient.transfers.pathologiesDuringPregnancy.label") }</label>
                        <div class="transfer-wizard-row transfer-wizard-row-three-col">
                            <% def pathologiesDuringPregnancyValues = (formData.pathologiesDuringPregnancy ?: '').split('; ') as List %>
                            <% ["Anemia", "Pre-eclampsia", "TB", "Diabetes", "Asthma"].each { option -> %>
                            <label style="font-weight:normal;">
                                <input type="checkbox" name="pathologiesDuringPregnancy" value="${ ui.encodeHtmlAttribute(option) }" <% if (pathologiesDuringPregnancyValues.contains(option)) { %>checked="checked"<% } %> style="width:auto;display:inline-block;margin-right:0.4rem;" /> ${ ui.format(option) }
                            </label>
                            <% } %>
                        </div>
                    </div>
                    <div class="transfer-wizard-field">
                        <label for="neonatalPregnancyOtherPathologies">${ ui.message("transferapp.patient.transfers.pregnancyOtherPathologies") }</label>
                        <input type="text" id="neonatalPregnancyOtherPathologies" name="pregnancyOtherPathologies"  value="${ ui.encodeHtmlAttribute(formData.pregnancyOtherPathologies ?: '') }" />
                    </div>
                    <div class="transfer-wizard-field">
                        <label for="neonatalPregnancyTreatment">${ ui.message("transferapp.patient.transfers.pregnancyTreatment") }</label>
                        <textarea id="neonatalPregnancyTreatment" name="pregnancyTreatment" rows="2">${ ui.format(formData.pregnancyTreatment ?: '') }</textarea>
                    </div>
                </div>

                <div class="transfer-wizard-section">
                    <h2 class="transfer-wizard-section-title">HIV status</h2>
                    <div class="transfer-wizard-row transfer-wizard-row-three-col">
                        <div class="transfer-wizard-field">
                            <label for="neonatalHivStatus">${ ui.message("transferapp.patient.transfers.hivStatus") }</label>
                            <select id="neonatalHivStatus" name="hivStatus">
                                <option value="">Select HIV status</option>
                                <option value="Eligible" <% if (formData.hivStatus == 'Eligible') { %>selected="selected"<% } %>>Eligible</option>
                                <option value="Non-eligible" <% if (formData.hivStatus == 'Non-eligible') { %>selected="selected"<% } %>>Non-eligible</option>
                                <option value="Unknown" <% if (formData.hivStatus == 'Unknown') { %>selected="selected"<% } %>>Unknown</option>
                            </select>
                        </div>
                        <div class="transfer-wizard-field">
                            <label for="neonatalHivRegimen">${ ui.message("transferapp.patient.transfers.hivRegimen") }</label>
                            <input type="text" id="neonatalHivRegimen" name="hivRegimen"  value="${ ui.encodeHtmlAttribute(formData.hivRegimen ?: '') }" />
                        </div>
                        <div class="transfer-wizard-field">
                            <label for="neonatalHivRecentVl">${ ui.message("transferapp.patient.transfers.hivRecentVl") }</label>
                            <input type="text" id="neonatalHivRecentVl" name="hivRecentVl"  value="${ ui.encodeHtmlAttribute(formData.hivRecentVl ?: '') }" />
                        </div>
                        <div class="transfer-wizard-field">
                            <label for="neonatalHivCd4Count">${ ui.message("transferapp.patient.transfers.hivCd4Count") }</label>
                            <input type="text" id="neonatalHivCd4Count" name="hivCd4Count"  value="${ ui.encodeHtmlAttribute(formData.hivCd4Count ?: '') }" />
                        </div>
                    </div>
                    <div class="transfer-wizard-field">
                        <label for="neonatalHivOpportunisticInfections">${ ui.message("transferapp.patient.transfers.hivOpportunisticInfections") }</label>
                        <textarea id="neonatalHivOpportunisticInfections" name="hivOpportunisticInfections" rows="2">${ ui.format(formData.hivOpportunisticInfections ?: '') }</textarea>
                    </div>
                    <div class="transfer-wizard-field">
                        <label for="neonatalMaternalIllicitDrugHistory">${ ui.message("transferapp.patient.transfers.maternalIllicitDrugHistory") }</label>
                        <textarea id="neonatalMaternalIllicitDrugHistory" name="maternalIllicitDrugHistory" rows="2">${ ui.format(formData.maternalIllicitDrugHistory ?: '') }</textarea>
                    </div>
                </div>
            </div>

            <!-- Step 3: Labor Details -->
            <div class="transfer-wizard-step-panel" data-step="3">
                <div class="transfer-wizard-section">
                    <h2 class="transfer-wizard-section-title">Rupture of membranes &amp; fever</h2>
                    <div class="transfer-wizard-row transfer-wizard-row-three-col">
                        <div class="transfer-wizard-field">
                            <label for="neonatalRomAt">${ ui.message("transferapp.patient.transfers.romAt") }</label>
                            <input type="text" class="js-datetime-picker" id="neonatalRomAt" name="romAt" placeholder="Select date and time" autocomplete="off"  value="${ ui.encodeHtmlAttribute(formData.romAt ?: '') }" />
                        </div>
                        <div class="transfer-wizard-field">
                            <label for="neonatalAfQuality">${ ui.message("transferapp.patient.transfers.afQuality") }</label>
                            <select id="neonatalAfQuality" name="afQuality">
                                <option value="">Select AF quality</option>
                                <option value="Clear" <% if (formData.afQuality == 'Clear') { %>selected="selected"<% } %>>Clear</option>
                                <option value="Blood-stained" <% if (formData.afQuality == 'Blood-stained') { %>selected="selected"<% } %>>Blood-stained</option>
                                <option value="Meconium-stained (light)" <% if (formData.afQuality == 'Meconium-stained (light)') { %>selected="selected"<% } %>>Meconium-stained (light)</option>
                                <option value="Meconium-stained (thick)" <% if (formData.afQuality == 'Meconium-stained (thick)') { %>selected="selected"<% } %>>Meconium-stained (thick)</option>
                                <option value="Unknown" <% if (formData.afQuality == 'Unknown') { %>selected="selected"<% } %>>Unknown</option>
                            </select>
                        </div>
                        <div class="transfer-wizard-field">
                            <label for="neonatalAfQuantity">${ ui.message("transferapp.patient.transfers.afQuantity") }</label>
                            <select id="neonatalAfQuantity" name="afQuantity">
                                <option value="">Select AF quantity</option>
                                <option value="Normal" <% if (formData.afQuantity == 'Normal') { %>selected="selected"<% } %>>Normal</option>
                                <option value="Oligohydramnios" <% if (formData.afQuantity == 'Oligohydramnios') { %>selected="selected"<% } %>>Oligohydramnios</option>
                                <option value="Polyhydramnios" <% if (formData.afQuantity == 'Polyhydramnios') { %>selected="selected"<% } %>>Polyhydramnios</option>
                                <option value="Unknown" <% if (formData.afQuantity == 'Unknown') { %>selected="selected"<% } %>>Unknown</option>
                            </select>
                        </div>
                        <div class="transfer-wizard-field">
                            <label for="neonatalFeverTiming">${ ui.message("transferapp.patient.transfers.feverTiming") }</label>
                            <select id="neonatalFeverTiming" name="feverTiming">
                                <option value="">Select fever timing</option>
                                <option value="None" <% if (formData.feverTiming == 'None') { %>selected="selected"<% } %>>None</option>
                                <option value="Antepartum" <% if (formData.feverTiming == 'Antepartum') { %>selected="selected"<% } %>>Antepartum</option>
                                <option value="Intrapartum" <% if (formData.feverTiming == 'Intrapartum') { %>selected="selected"<% } %>>Intrapartum</option>
                                <option value="Postpartum" <% if (formData.feverTiming == 'Postpartum') { %>selected="selected"<% } %>>Postpartum</option>
                            </select>
                        </div>
                    </div>
                </div>

                <div class="transfer-wizard-section">
                    <h2 class="transfer-wizard-section-title">Steroids &amp; MgSO4</h2>
                    <div class="transfer-wizard-row transfer-wizard-row-three-col">
                        <div class="transfer-wizard-field">
                            <label for="neonatalSteroidDoses">${ ui.message("transferapp.patient.transfers.steroidDoses") }</label>
                            <select id="neonatalSteroidDoses" name="steroidDoses">
                                <option value="">Select</option>
                                <option value="1" <% if (formData.steroidDoses == '1') { %>selected="selected"<% } %>>1</option>
                                <option value="2" <% if (formData.steroidDoses == '2') { %>selected="selected"<% } %>>2</option>
                                <option value="3" <% if (formData.steroidDoses == '3') { %>selected="selected"<% } %>>3</option>
                                <option value="4" <% if (formData.steroidDoses == '4') { %>selected="selected"<% } %>>4</option>
                                <option value="Unknown" <% if (formData.steroidDoses == 'Unknown') { %>selected="selected"<% } %>>Unknown</option>
                                <option value="NA" <% if (formData.steroidDoses == 'NA') { %>selected="selected"<% } %>>NA</option>
                            </select>
                        </div>
                        <div class="transfer-wizard-field">
                            <label for="neonatalLastSteroidDoseAt">${ ui.message("transferapp.patient.transfers.lastSteroidDoseAt") }</label>
                            <input type="text" class="js-datetime-picker" id="neonatalLastSteroidDoseAt" name="lastSteroidDoseAt" placeholder="Select date and time" autocomplete="off"  value="${ ui.encodeHtmlAttribute(formData.lastSteroidDoseAt ?: '') }" />
                        </div>
                        <div class="transfer-wizard-field">
                            <label for="neonatalMgso4At">${ ui.message("transferapp.patient.transfers.mgso4At") }</label>
                            <input type="text" class="js-datetime-picker" id="neonatalMgso4At" name="mgso4At" placeholder="Select date and time" autocomplete="off"  value="${ ui.encodeHtmlAttribute(formData.mgso4At ?: '') }" />
                        </div>
                    </div>
                </div>

                <div class="transfer-wizard-section">
                    <h2 class="transfer-wizard-section-title">Delivery &amp; complications</h2>
                    <div class="transfer-wizard-row transfer-wizard-row-three-col">
                        <div class="transfer-wizard-field">
                            <label for="neonatalModeOfDelivery">${ ui.message("transferapp.patient.transfers.modeOfDelivery") }</label>
                            <select id="neonatalModeOfDelivery" name="modeOfDelivery">
                                <option value="">Select mode of delivery</option>
                                <option value="Vaginal Delivery" <% if (formData.modeOfDelivery == 'Vaginal Delivery') { %>selected="selected"<% } %>>Vaginal Delivery</option>
                                <option value="Assisted Vaginal Delivery" <% if (formData.modeOfDelivery == 'Assisted Vaginal Delivery') { %>selected="selected"<% } %>>Assisted Vaginal Delivery</option>
                                <option value="Vacuum" <% if (formData.modeOfDelivery == 'Vacuum') { %>selected="selected"<% } %>>Vacuum</option>
                                <option value="Caesarean Section" <% if (formData.modeOfDelivery == 'Caesarean Section') { %>selected="selected"<% } %>>Caesarean Section</option>
                                <option value="Other" <% if (formData.modeOfDelivery == 'Other') { %>selected="selected"<% } %>>Other</option>
                            </select>
                        </div>
                        <div class="transfer-wizard-field">
                            <label for="neonatalMaternalAnesthesia">${ ui.message("transferapp.patient.transfers.maternalAnesthesia") }</label>
                            <select id="neonatalMaternalAnesthesia" name="maternalAnesthesia">
                                <option value="">Select anesthesia</option>
                                <option value="None" <% if (formData.maternalAnesthesia == 'None') { %>selected="selected"<% } %>>None</option>
                                <option value="Local" <% if (formData.maternalAnesthesia == 'Local') { %>selected="selected"<% } %>>Local</option>
                                <option value="Spinal" <% if (formData.maternalAnesthesia == 'Spinal') { %>selected="selected"<% } %>>Spinal</option>
                                <option value="Epidural" <% if (formData.maternalAnesthesia == 'Epidural') { %>selected="selected"<% } %>>Epidural</option>
                                <option value="General" <% if (formData.maternalAnesthesia == 'General') { %>selected="selected"<% } %>>General</option>
                                <option value="Other" <% if (formData.maternalAnesthesia == 'Other') { %>selected="selected"<% } %>>Other (specify)</option>
                            </select>
                        </div>
                        <div class="transfer-wizard-field">
                            <label for="neonatalMaternalAnesthesiaOther">${ ui.message("transferapp.patient.transfers.maternalAnesthesiaOther") }</label>
                            <input type="text" id="neonatalMaternalAnesthesiaOther" name="maternalAnesthesiaOther"  value="${ ui.encodeHtmlAttribute(formData.maternalAnesthesiaOther ?: '') }" />
                        </div>
                    </div>
                    <div class="transfer-wizard-field">
                        <label class="transfer-wizard-section-title" style="font-size:1rem;">${ ui.message("transferapp.patient.transfers.laborComplications.label") }</label>
                        <div class="transfer-wizard-row transfer-wizard-row-three-col">
                            <% def laborComplicationsValues = (formData.laborComplications ?: '').split('; ') as List %>
                            <% ["Prolonged labor", "Obstructed labor", "Cord prolapse", "Placental abruption", "Placenta previa", "Uterine rupture", "Shoulder dystocia", "Fetal distress", "Postpartum hemorrhage", "None"].each { option -> %>
                            <label style="font-weight:normal;">
                                <input type="checkbox" name="laborComplications" value="${ ui.encodeHtmlAttribute(option) }" <% if (laborComplicationsValues.contains(option)) { %>checked="checked"<% } %> style="width:auto;display:inline-block;margin-right:0.4rem;" /> ${ ui.format(option) }
                            </label>
                            <% } %>
                        </div>
                    </div>
                    <div class="transfer-wizard-field">
                        <label for="neonatalLaborComplicationsOther">${ ui.message("transferapp.patient.transfers.laborComplicationsOther") }</label>
                        <input type="text" id="neonatalLaborComplicationsOther" name="laborComplicationsOther"  value="${ ui.encodeHtmlAttribute(formData.laborComplicationsOther ?: '') }" />
                    </div>
                    <div class="transfer-wizard-row transfer-wizard-row-three-col">
                        <div class="transfer-wizard-field">
                            <label for="neonatalMaternalAntibiotics">${ ui.message("transferapp.patient.transfers.maternalAntibiotics") }</label>
                            <select id="neonatalMaternalAntibiotics" name="maternalAntibiotics">
                                <option value="">Select</option>
                                <option value="Yes" <% if (formData.maternalAntibiotics == 'Yes') { %>selected="selected"<% } %>>Yes</option>
                                <option value="No" <% if (formData.maternalAntibiotics == 'No') { %>selected="selected"<% } %>>No</option>
                                <option value="Unknown" <% if (formData.maternalAntibiotics == 'Unknown') { %>selected="selected"<% } %>>Unknown</option>
                            </select>
                        </div>
                        <div class="transfer-wizard-field">
                            <label for="neonatalOtherDrugs">${ ui.message("transferapp.patient.transfers.otherDrugs") }</label>
                            <select id="neonatalOtherDrugs" name="otherDrugs">
                                <option value="">Select</option>
                                <option value="Yes" <% if (formData.otherDrugs == 'Yes') { %>selected="selected"<% } %>>Yes</option>
                                <option value="No" <% if (formData.otherDrugs == 'No') { %>selected="selected"<% } %>>No</option>
                                <option value="Unknown" <% if (formData.otherDrugs == 'Unknown') { %>selected="selected"<% } %>>Unknown</option>
                            </select>
                        </div>
                    </div>
                    <div class="transfer-wizard-field">
                        <label class="transfer-wizard-section-title" style="font-size:1rem;">${ ui.message("transferapp.patient.transfers.sepsisRiskFactors.label") }</label>
                        <div class="transfer-wizard-row transfer-wizard-row-three-col">
                            <% def sepsisRiskFactorsValues = (formData.sepsisRiskFactors ?: '').split('; ') as List %>
                            <% ["PROM", "Maternal fever", "Prematurity", "Maternal infection", "Born en-route/home", "Others"].each { option -> %>
                            <label style="font-weight:normal;">
                                <input type="checkbox" name="sepsisRiskFactors" value="${ ui.encodeHtmlAttribute(option) }" <% if (sepsisRiskFactorsValues.contains(option)) { %>checked="checked"<% } %> style="width:auto;display:inline-block;margin-right:0.4rem;" /> ${ ui.format(option) }
                            </label>
                            <% } %>
                        </div>
                    </div>
                </div>
            </div>

            <!-- Step 4: Neonatal History & Drugs -->
            <div class="transfer-wizard-step-panel" data-step="4">
                <div class="transfer-wizard-section">
                    <h2 class="transfer-wizard-section-title">Resuscitation &amp; APGAR</h2>
                    <div class="transfer-wizard-row transfer-wizard-row-three-col">
                        <div class="transfer-wizard-field">
                            <label for="neonatalResuscitationAtBirth">${ ui.message("transferapp.patient.transfers.resuscitationAtBirth") }</label>
                            <select id="neonatalResuscitationAtBirth" name="resuscitationAtBirth">
                                <option value="">Select</option>
                                <option value="Yes" <% if (formData.resuscitationAtBirth == 'Yes') { %>selected="selected"<% } %>>Yes</option>
                                <option value="No" <% if (formData.resuscitationAtBirth == 'No') { %>selected="selected"<% } %>>No</option>
                                <option value="Unknown" <% if (formData.resuscitationAtBirth == 'Unknown') { %>selected="selected"<% } %>>Unknown</option>
                            </select>
                        </div>
                        <div class="transfer-wizard-field">
                            <label for="neonatalApgar1min">${ ui.message("transferapp.patient.transfers.apgar1min") }</label>
                            <input type="text" id="neonatalApgar1min" name="apgar1min"  value="${ ui.encodeHtmlAttribute(formData.apgar1min ?: '') }" />
                        </div>
                        <div class="transfer-wizard-field">
                            <label for="neonatalApgar5min">${ ui.message("transferapp.patient.transfers.apgar5min") }</label>
                            <input type="text" id="neonatalApgar5min" name="apgar5min"  value="${ ui.encodeHtmlAttribute(formData.apgar5min ?: '') }" />
                        </div>
                        <div class="transfer-wizard-field">
                            <label for="neonatalApgar10min">${ ui.message("transferapp.patient.transfers.apgar10min") }</label>
                            <input type="text" id="neonatalApgar10min" name="apgar10min"  value="${ ui.encodeHtmlAttribute(formData.apgar10min ?: '') }" />
                        </div>
                    </div>
                    <div class="transfer-wizard-field" id="neonatalResuscitationMethodsPanel" style="display:none;">
                        <label class="transfer-wizard-section-title" style="font-size:1rem;">${ ui.message("transferapp.patient.transfers.resuscitationMethods.label") }</label>
                        <div class="transfer-wizard-row transfer-wizard-row-three-col">
                            <% def resuscitationMethodsValues = (formData.resuscitationMethods ?: '').split('; ') as List %>
                            <% ["Stimulation", "Suctioning", "BMV", "Oxygen", "Intubation", "Chest compressions"].each { option -> %>
                            <label style="font-weight:normal;">
                                <input type="checkbox" name="resuscitationMethods" value="${ ui.encodeHtmlAttribute(option) }" <% if (resuscitationMethodsValues.contains(option)) { %>checked="checked"<% } %> style="width:auto;display:inline-block;margin-right:0.4rem;" /> ${ ui.format(option) }
                            </label>
                            <% } %>
                        </div>
                    </div>
                </div>

                <div class="transfer-wizard-section">
                    <h2 class="transfer-wizard-section-title">HIE &amp; allergies</h2>
                    <div class="transfer-wizard-row transfer-wizard-row-three-col">
                        <div class="transfer-wizard-field">
                            <label for="neonatalHie">${ ui.message("transferapp.patient.transfers.hie") }</label>
                            <select id="neonatalHie" name="hie">
                                <option value="">Select</option>
                                <option value="Yes" <% if (formData.hie == 'Yes') { %>selected="selected"<% } %>>Yes</option>
                                <option value="No" <% if (formData.hie == 'No') { %>selected="selected"<% } %>>No</option>
                                <option value="Unknown" <% if (formData.hie == 'Unknown') { %>selected="selected"<% } %>>Unknown</option>
                            </select>
                        </div>
                        <div class="transfer-wizard-field" id="neonatalHieGradePanel" style="display:none;">
                            <label for="neonatalHieGrade">${ ui.message("transferapp.patient.transfers.hieGrade") }</label>
                            <select id="neonatalHieGrade" name="hieGrade">
                                <option value="">Select grade</option>
                                <option value="Mild" <% if (formData.hieGrade == 'Mild') { %>selected="selected"<% } %>>Mild</option>
                                <option value="Moderate" <% if (formData.hieGrade == 'Moderate') { %>selected="selected"<% } %>>Moderate</option>
                                <option value="Severe" <% if (formData.hieGrade == 'Severe') { %>selected="selected"<% } %>>Severe</option>
                            </select>
                        </div>
                        <div class="transfer-wizard-field">
                            <label for="neonatalAllergies">${ ui.message("transferapp.patient.transfers.allergies") }</label>
                            <select id="neonatalAllergies" name="allergies">
                                <option value="">Select</option>
                                <option value="Yes" <% if (formData.allergies == 'Yes') { %>selected="selected"<% } %>>Yes</option>
                                <option value="No" <% if (formData.allergies == 'No') { %>selected="selected"<% } %>>No</option>
                                <option value="Unknown" <% if (formData.allergies == 'Unknown') { %>selected="selected"<% } %>>Unknown</option>
                            </select>
                        </div>
                    </div>
                </div>

                <div class="transfer-wizard-section">
                    <h2 class="transfer-wizard-section-title">Immunization &amp; prophylaxis</h2>
                    <div class="transfer-wizard-row transfer-wizard-row-three-col">
                        <div class="transfer-wizard-field">
                            <label for="neonatalImmunization">${ ui.message("transferapp.patient.transfers.immunization") }</label>
                            <select id="neonatalImmunization" name="immunization">
                                <option value="">Select</option>
                                <option value="Yes" <% if (formData.immunization == 'Yes') { %>selected="selected"<% } %>>Yes</option>
                                <option value="No" <% if (formData.immunization == 'No') { %>selected="selected"<% } %>>No</option>
                                <option value="Unknown" <% if (formData.immunization == 'Unknown') { %>selected="selected"<% } %>>Unknown</option>
                            </select>
                        </div>
                        <div class="transfer-wizard-field">
                            <label for="neonatalVitaminK">${ ui.message("transferapp.patient.transfers.vitaminK") }</label>
                            <select id="neonatalVitaminK" name="vitaminK">
                                <option value="">Select</option>
                                <option value="Yes" <% if (formData.vitaminK == 'Yes') { %>selected="selected"<% } %>>Yes</option>
                                <option value="No" <% if (formData.vitaminK == 'No') { %>selected="selected"<% } %>>No</option>
                                <option value="Unknown" <% if (formData.vitaminK == 'Unknown') { %>selected="selected"<% } %>>Unknown</option>
                            </select>
                        </div>
                        <div class="transfer-wizard-field">
                            <label for="neonatalTetracyclineEyeOintment">${ ui.message("transferapp.patient.transfers.tetracyclineEyeOintment") }</label>
                            <select id="neonatalTetracyclineEyeOintment" name="tetracyclineEyeOintment">
                                <option value="">Select</option>
                                <option value="Yes" <% if (formData.tetracyclineEyeOintment == 'Yes') { %>selected="selected"<% } %>>Yes</option>
                                <option value="No" <% if (formData.tetracyclineEyeOintment == 'No') { %>selected="selected"<% } %>>No</option>
                                <option value="Unknown" <% if (formData.tetracyclineEyeOintment == 'Unknown') { %>selected="selected"<% } %>>Unknown</option>
                            </select>
                        </div>
                        <div class="transfer-wizard-field">
                            <label for="neonatalSurfactant">${ ui.message("transferapp.patient.transfers.surfactant") }</label>
                            <select id="neonatalSurfactant" name="surfactant">
                                <option value="">Select</option>
                                <option value="Yes" <% if (formData.surfactant == 'Yes') { %>selected="selected"<% } %>>Yes</option>
                                <option value="No" <% if (formData.surfactant == 'No') { %>selected="selected"<% } %>>No</option>
                                <option value="Unknown" <% if (formData.surfactant == 'Unknown') { %>selected="selected"<% } %>>Unknown</option>
                            </select>
                        </div>
                    </div>
                    <div class="transfer-wizard-field" id="neonatalImmunizationDetailsPanel" style="display:none;">
                        <label for="neonatalImmunizationDetails">${ ui.message("transferapp.patient.transfers.immunizationDetails") }</label>
                        <textarea id="neonatalImmunizationDetails" name="immunizationDetails" rows="2">${ ui.format(formData.immunizationDetails ?: '') }</textarea>
                    </div>
                </div>
            </div>

            <!-- Step 5: Chief Complaint & Diagnoses -->
            <div class="transfer-wizard-step-panel" data-step="5">
                <div class="transfer-wizard-section">
                    <h2 class="transfer-wizard-section-title">Chief complaint</h2>
                    <div class="transfer-wizard-field">
                        <label for="neonatalChiefComplaintDetails">${ ui.message("transferapp.patient.transfers.chiefComplaintDetails") }</label>
                        <textarea id="neonatalChiefComplaintDetails" name="chiefComplaintDetails" rows="3">${ ui.format(formData.chiefComplaintDetails ?: '') }</textarea>
                    </div>
                </div>

                <div class="transfer-wizard-section">
                    <h2 class="transfer-wizard-section-title">Clinical condition</h2>
                    <div class="transfer-vitals-grid">
                        <div class="transfer-wizard-field transfer-vital-field">
                            <label for="neonatalSpo2Preductal">${ ui.message("transferapp.patient.transfers.spo2Preductal") }</label>
                            <input type="text" id="neonatalSpo2Preductal" name="spo2Preductal"  value="${ ui.encodeHtmlAttribute(formData.spo2Preductal ?: '') }" />
                        </div>
                        <div class="transfer-wizard-field transfer-vital-field">
                            <label for="neonatalSpo2Postductal">${ ui.message("transferapp.patient.transfers.spo2Postductal") }</label>
                            <input type="text" id="neonatalSpo2Postductal" name="spo2Postductal"  value="${ ui.encodeHtmlAttribute(formData.spo2Postductal ?: '') }" />
                        </div>
                        <div class="transfer-wizard-field transfer-vital-field">
                            <label for="neonatalConditionTemp">${ ui.message("transferapp.patient.transfers.conditionTemp") }</label>
                            <input type="text" id="neonatalConditionTemp" name="conditionTemp"  value="${ ui.encodeHtmlAttribute(formData.conditionTemp ?: '') }" />
                        </div>
                        <div class="transfer-wizard-field transfer-vital-field">
                            <label for="neonatalConditionHr">${ ui.message("transferapp.patient.transfers.conditionHr") }</label>
                            <input type="text" id="neonatalConditionHr" name="conditionHr"  value="${ ui.encodeHtmlAttribute(formData.conditionHr ?: '') }" />
                        </div>
                        <div class="transfer-wizard-field transfer-vital-field">
                            <label for="neonatalConditionRr">${ ui.message("transferapp.patient.transfers.conditionRr") }</label>
                            <input type="text" id="neonatalConditionRr" name="conditionRr"  value="${ ui.encodeHtmlAttribute(formData.conditionRr ?: '') }" />
                        </div>
                        <div class="transfer-wizard-field transfer-vital-field">
                            <label for="neonatalConditionBp">${ ui.message("transferapp.patient.transfers.conditionBp") }</label>
                            <input type="text" id="neonatalConditionBp" name="conditionBp"  value="${ ui.encodeHtmlAttribute(formData.conditionBp ?: '') }" />
                        </div>
                    </div>
                    <div class="transfer-wizard-field" style="margin-top:0.75rem;">
                        <label class="transfer-wizard-section-title" style="font-size:1rem;">${ ui.message("transferapp.patient.transfers.neurologicalStatus.label") }</label>
                        <div class="transfer-wizard-row transfer-wizard-row-three-col">
                            <% def neurologicalStatusValues = (formData.neurologicalStatus ?: '').split('; ') as List %>
                            <% ["Active", "Lethargic", "Unresponsive", "Seizures"].each { option -> %>
                            <label style="font-weight:normal;">
                                <input type="checkbox" name="neurologicalStatus" value="${ ui.encodeHtmlAttribute(option) }" <% if (neurologicalStatusValues.contains(option)) { %>checked="checked"<% } %> style="width:auto;display:inline-block;margin-right:0.4rem;" /> ${ ui.format(option) }
                            </label>
                            <% } %>
                        </div>
                    </div>
                    <div class="transfer-wizard-field">
                        <label for="neonatalAdverseEvents24h">${ ui.message("transferapp.patient.transfers.adverseEvents24h") }</label>
                        <select id="neonatalAdverseEvents24h" name="adverseEvents24h">
                            <option value="">Select</option>
                            <option value="Yes" <% if (formData.adverseEvents24h == 'Yes') { %>selected="selected"<% } %>>Yes</option>
                            <option value="No" <% if (formData.adverseEvents24h == 'No') { %>selected="selected"<% } %>>No</option>
                            <option value="Unknown" <% if (formData.adverseEvents24h == 'Unknown') { %>selected="selected"<% } %>>Unknown</option>
                        </select>
                    </div>
                </div>

                <div class="transfer-wizard-section">
                    <h2 class="transfer-wizard-section-title">Diagnoses</h2>
                    <div class="transfer-wizard-row transfer-wizard-row-two-col">
                        <div class="transfer-wizard-field">
                            <label for="neonatalDiagnosis1">${ ui.message("transferapp.patient.transfers.diagnosis1") }</label>
                            <input type="text" id="neonatalDiagnosis1" name="diagnosis1"  value="${ ui.encodeHtmlAttribute(formData.diagnosis1 ?: '') }" />
                        </div>
                        <div class="transfer-wizard-field">
                            <label for="neonatalDiagnosis2">${ ui.message("transferapp.patient.transfers.diagnosis2") }</label>
                            <input type="text" id="neonatalDiagnosis2" name="diagnosis2"  value="${ ui.encodeHtmlAttribute(formData.diagnosis2 ?: '') }" />
                        </div>
                        <div class="transfer-wizard-field">
                            <label for="neonatalDiagnosis3">${ ui.message("transferapp.patient.transfers.diagnosis3") }</label>
                            <input type="text" id="neonatalDiagnosis3" name="diagnosis3"  value="${ ui.encodeHtmlAttribute(formData.diagnosis3 ?: '') }" />
                        </div>
                        <div class="transfer-wizard-field">
                            <label for="neonatalDiagnosis4">${ ui.message("transferapp.patient.transfers.diagnosis4") }</label>
                            <input type="text" id="neonatalDiagnosis4" name="diagnosis4"  value="${ ui.encodeHtmlAttribute(formData.diagnosis4 ?: '') }" />
                        </div>
                    </div>
                </div>
            </div>

            <!-- Step 6: Management at Referring Facility -->
            <div class="transfer-wizard-step-panel" data-step="6">
                <div class="transfer-wizard-section">
                    <h2 class="transfer-wizard-section-title">Airway, breathing &amp; circulation</h2>
                    <div class="transfer-wizard-row transfer-wizard-row-three-col">
                        <div class="transfer-wizard-field">
                            <label for="neonatalRespiratorySupport">${ ui.message("transferapp.patient.transfers.respiratorySupport") }</label>
                            <select id="neonatalRespiratorySupport" name="respiratorySupport">
                                <option value="">Select</option>
                                <option value="None" <% if (formData.respiratorySupport == 'None') { %>selected="selected"<% } %>>None</option>
                                <option value="Low flow O2" <% if (formData.respiratorySupport == 'Low flow O2') { %>selected="selected"<% } %>>Low flow O2</option>
                                <option value="HFT" <% if (formData.respiratorySupport == 'HFT') { %>selected="selected"<% } %>>HFT</option>
                                <option value="CPAP" <% if (formData.respiratorySupport == 'CPAP') { %>selected="selected"<% } %>>CPAP</option>
                                <option value="Mechanical Ventilation" <% if (formData.respiratorySupport == 'Mechanical Ventilation') { %>selected="selected"<% } %>>Mechanical Ventilation</option>
                            </select>
                        </div>
                        <div class="transfer-wizard-field">
                            <label for="neonatalBloodGasAnalysis">${ ui.message("transferapp.patient.transfers.bloodGasAnalysis") }</label>
                            <select id="neonatalBloodGasAnalysis" name="bloodGasAnalysis">
                                <option value="">Select</option>
                                <option value="Yes" <% if (formData.bloodGasAnalysis == 'Yes') { %>selected="selected"<% } %>>Yes</option>
                                <option value="No" <% if (formData.bloodGasAnalysis == 'No') { %>selected="selected"<% } %>>No</option>
                            </select>
                        </div>
                        <div class="transfer-wizard-field">
                            <label for="neonatalIvFluidVol">${ ui.message("transferapp.patient.transfers.ivFluidVol") }</label>
                            <input type="text" id="neonatalIvFluidVol" name="ivFluidVol"  value="${ ui.encodeHtmlAttribute(formData.ivFluidVol ?: '') }" />
                        </div>
                        <div class="transfer-wizard-field">
                            <label for="neonatalPassedUrine">${ ui.message("transferapp.patient.transfers.passedUrine") }</label>
                            <select id="neonatalPassedUrine" name="passedUrine">
                                <option value="">Select</option>
                                <option value="Yes" <% if (formData.passedUrine == 'Yes') { %>selected="selected"<% } %>>Yes</option>
                                <option value="No" <% if (formData.passedUrine == 'No') { %>selected="selected"<% } %>>No</option>
                                <option value="Unknown" <% if (formData.passedUrine == 'Unknown') { %>selected="selected"<% } %>>Unknown</option>
                            </select>
                        </div>
                        <div class="transfer-wizard-field">
                            <label for="neonatalInotropes">${ ui.message("transferapp.patient.transfers.inotropes") }</label>
                            <select id="neonatalInotropes" name="inotropes">
                                <option value="">Select</option>
                                <option value="No" <% if (formData.inotropes == 'No') { %>selected="selected"<% } %>>No</option>
                                <option value="Unknown" <% if (formData.inotropes == 'Unknown') { %>selected="selected"<% } %>>Unknown</option>
                                <option value="Yes" <% if (formData.inotropes == 'Yes') { %>selected="selected"<% } %>>Yes - specify</option>
                            </select>
                        </div>
                        <div class="transfer-wizard-field" id="neonatalInotropesSpecifyPanel" style="display:none;">
                            <label for="neonatalInotropesSpecify">${ ui.message("transferapp.patient.transfers.inotropesSpecify") }</label>
                            <input type="text" id="neonatalInotropesSpecify" name="inotropesSpecify"  value="${ ui.encodeHtmlAttribute(formData.inotropesSpecify ?: '') }" />
                        </div>
                        <div class="transfer-wizard-field">
                            <label for="neonatalPeripheralIv">${ ui.message("transferapp.patient.transfers.peripheralIv") }</label>
                            <select id="neonatalPeripheralIv" name="peripheralIv">
                                <option value="">Select</option>
                                <option value="Yes" <% if (formData.peripheralIv == 'Yes') { %>selected="selected"<% } %>>Yes</option>
                                <option value="No" <% if (formData.peripheralIv == 'No') { %>selected="selected"<% } %>>No</option>
                                <option value="Unknown" <% if (formData.peripheralIv == 'Unknown') { %>selected="selected"<% } %>>Unknown</option>
                            </select>
                        </div>
                        <div class="transfer-wizard-field">
                            <label for="neonatalCentralIv">${ ui.message("transferapp.patient.transfers.centralIv") }</label>
                            <select id="neonatalCentralIv" name="centralIv">
                                <option value="">Select</option>
                                <option value="Yes" <% if (formData.centralIv == 'Yes') { %>selected="selected"<% } %>>Yes</option>
                                <option value="No" <% if (formData.centralIv == 'No') { %>selected="selected"<% } %>>No</option>
                                <option value="Unknown" <% if (formData.centralIv == 'Unknown') { %>selected="selected"<% } %>>Unknown</option>
                            </select>
                        </div>
                        <div class="transfer-wizard-field">
                            <label for="neonatalIntraosseousLine">${ ui.message("transferapp.patient.transfers.intraosseousLine") }</label>
                            <select id="neonatalIntraosseousLine" name="intraosseousLine">
                                <option value="">Select</option>
                                <option value="Yes" <% if (formData.intraosseousLine == 'Yes') { %>selected="selected"<% } %>>Yes</option>
                                <option value="No" <% if (formData.intraosseousLine == 'No') { %>selected="selected"<% } %>>No</option>
                                <option value="Unknown" <% if (formData.intraosseousLine == 'Unknown') { %>selected="selected"<% } %>>Unknown</option>
                            </select>
                        </div>
                    </div>
                    <div class="transfer-wizard-field">
                        <label for="neonatalVentilationSettings">${ ui.message("transferapp.patient.transfers.ventilationSettings") }</label>
                        <textarea id="neonatalVentilationSettings" name="ventilationSettings" rows="2">${ ui.format(formData.ventilationSettings ?: '') }</textarea>
                    </div>
                </div>

                <div class="transfer-wizard-section">
                    <h2 class="transfer-wizard-section-title">Infectious &amp; antibiotics</h2>
                    <div style="overflow-x:auto;">
                        <table style="border-collapse:collapse;width:100%;">
                            <thead>
                                <tr>
                                    <th style="text-align:left;padding:0.4rem;border-bottom:2px solid #e2e8f0;">Antibiotic</th>
                                    <th style="text-align:left;padding:0.4rem;border-bottom:2px solid #e2e8f0;">Name</th>
                                    <th style="text-align:left;padding:0.4rem;border-bottom:2px solid #e2e8f0;">Doses</th>
                                    <th style="text-align:left;padding:0.4rem;border-bottom:2px solid #e2e8f0;">Duration</th>
                                </tr>
                            </thead>
                            <tbody>
                                <tr>
                                    <td style="padding:0.3rem;border-bottom:1px solid #eef2f6;">1</td>
                                    <td style="padding:0.3rem;border-bottom:1px solid #eef2f6;"><input type="text" id="neonatalAntibiotic1Name" name="antibiotic1Name"  value="${ ui.encodeHtmlAttribute(formData.antibiotic1Name ?: '') }" /></td>
                                    <td style="padding:0.3rem;border-bottom:1px solid #eef2f6;"><input type="text" id="neonatalAntibiotic1Doses" name="antibiotic1Doses"  value="${ ui.encodeHtmlAttribute(formData.antibiotic1Doses ?: '') }" /></td>
                                    <td style="padding:0.3rem;border-bottom:1px solid #eef2f6;"><input type="text" id="neonatalAntibiotic1Durations" name="antibiotic1Durations"  value="${ ui.encodeHtmlAttribute(formData.antibiotic1Durations ?: '') }" /></td>
                                </tr>
                                <tr>
                                    <td style="padding:0.3rem;border-bottom:1px solid #eef2f6;">2</td>
                                    <td style="padding:0.3rem;border-bottom:1px solid #eef2f6;"><input type="text" id="neonatalAntibiotic2Name" name="antibiotic2Name"  value="${ ui.encodeHtmlAttribute(formData.antibiotic2Name ?: '') }" /></td>
                                    <td style="padding:0.3rem;border-bottom:1px solid #eef2f6;"><input type="text" id="neonatalAntibiotic2Doses" name="antibiotic2Doses"  value="${ ui.encodeHtmlAttribute(formData.antibiotic2Doses ?: '') }" /></td>
                                    <td style="padding:0.3rem;border-bottom:1px solid #eef2f6;"><input type="text" id="neonatalAntibiotic2Durations" name="antibiotic2Durations"  value="${ ui.encodeHtmlAttribute(formData.antibiotic2Durations ?: '') }" /></td>
                                </tr>
                            </tbody>
                        </table>
                    </div>
                    <div class="transfer-wizard-field" style="margin-top:0.75rem;">
                        <label for="neonatalArvs">${ ui.message("transferapp.patient.transfers.arvs") }</label>
                        <select id="neonatalArvs" name="arvs">
                            <option value="">Select</option>
                            <option value="Yes" <% if (formData.arvs == 'Yes') { %>selected="selected"<% } %>>Yes</option>
                            <option value="No" <% if (formData.arvs == 'No') { %>selected="selected"<% } %>>No</option>
                            <option value="NA" <% if (formData.arvs == 'NA') { %>selected="selected"<% } %>>NA</option>
                        </select>
                    </div>
                </div>

                <div class="transfer-wizard-section">
                    <h2 class="transfer-wizard-section-title">Feeding &amp; GIT</h2>
                    <div class="transfer-wizard-row transfer-wizard-row-three-col">
                        <div class="transfer-wizard-field">
                            <label for="neonatalNpo">${ ui.message("transferapp.patient.transfers.npo") }</label>
                            <select id="neonatalNpo" name="npo">
                                <option value="">Select</option>
                                <option value="Yes" <% if (formData.npo == 'Yes') { %>selected="selected"<% } %>>Yes</option>
                                <option value="No" <% if (formData.npo == 'No') { %>selected="selected"<% } %>>No</option>
                            </select>
                        </div>
                        <div class="transfer-wizard-field">
                            <label for="neonatalLastFeedTime">${ ui.message("transferapp.patient.transfers.lastFeedTime") }</label>
                            <input type="text" class="js-time-picker" id="neonatalLastFeedTime" name="lastFeedTime" placeholder="Select time" autocomplete="off"  value="${ ui.encodeHtmlAttribute(formData.lastFeedTime ?: '') }" />
                        </div>
                        <div class="transfer-wizard-field">
                            <label for="neonatalLastFeedAmount">${ ui.message("transferapp.patient.transfers.lastFeedAmount") }</label>
                            <input type="text" id="neonatalLastFeedAmount" name="lastFeedAmount"  value="${ ui.encodeHtmlAttribute(formData.lastFeedAmount ?: '') }" />
                        </div>
                        <div class="transfer-wizard-field">
                            <label for="neonatalFeedVol">${ ui.message("transferapp.patient.transfers.feedVol") }</label>
                            <input type="text" id="neonatalFeedVol" name="feedVol"  value="${ ui.encodeHtmlAttribute(formData.feedVol ?: '') }" />
                        </div>
                        <div class="transfer-wizard-field">
                            <label for="neonatalFeedType">${ ui.message("transferapp.patient.transfers.feedType") }</label>
                            <select id="neonatalFeedType" name="feedType">
                                <option value="">Select</option>
                                <option value="Breastmilk" <% if (formData.feedType == 'Breastmilk') { %>selected="selected"<% } %>>Breastmilk</option>
                                <option value="Other" <% if (formData.feedType == 'Other') { %>selected="selected"<% } %>>Other</option>
                            </select>
                        </div>
                        <div class="transfer-wizard-field">
                            <label for="neonatalPassedStool">${ ui.message("transferapp.patient.transfers.passedStool") }</label>
                            <select id="neonatalPassedStool" name="passedStool">
                                <option value="">Select</option>
                                <option value="Yes" <% if (formData.passedStool == 'Yes') { %>selected="selected"<% } %>>Yes</option>
                                <option value="No" <% if (formData.passedStool == 'No') { %>selected="selected"<% } %>>No</option>
                                <option value="Unknown" <% if (formData.passedStool == 'Unknown') { %>selected="selected"<% } %>>Unknown</option>
                            </select>
                        </div>
                        <div class="transfer-wizard-field">
                            <label for="neonatalNasogastricTube">${ ui.message("transferapp.patient.transfers.nasogastricTube") }</label>
                            <select id="neonatalNasogastricTube" name="nasogastricTube">
                                <option value="">Select</option>
                                <option value="Yes" <% if (formData.nasogastricTube == 'Yes') { %>selected="selected"<% } %>>Yes</option>
                                <option value="No" <% if (formData.nasogastricTube == 'No') { %>selected="selected"<% } %>>No</option>
                            </select>
                        </div>
                    </div>
                </div>

                <div class="transfer-wizard-section">
                    <h2 class="transfer-wizard-section-title">Latest labs &amp; imaging</h2>
                    <div class="transfer-vitals-grid">
                        <div class="transfer-wizard-field transfer-vital-field">
                            <label for="neonatalLabGlucose">${ ui.message("transferapp.patient.transfers.labGlucose") }</label>
                            <input type="text" id="neonatalLabGlucose" name="labGlucose"  value="${ ui.encodeHtmlAttribute(formData.labGlucose ?: '') }" />
                        </div>
                        <div class="transfer-wizard-field transfer-vital-field">
                            <label for="neonatalLabFbc">${ ui.message("transferapp.patient.transfers.fbcDone") }</label>
                            <select id="neonatalLabFbc" name="fbcDone">
                                <option value="">Select</option>
                                <option value="Yes" <% if (formData.fbcDone == 'Yes') { %>selected="selected"<% } %>>Yes</option>
                                <option value="No" <% if (formData.fbcDone == 'No') { %>selected="selected"<% } %>>No</option>
                            </select>
                        </div>
                        <div class="transfer-wizard-field transfer-vital-field" id="neonatalLabHbPanel" style="display:none;">
                            <label for="neonatalLabHb">${ ui.message("transferapp.patient.transfers.labHb") }</label>
                            <input type="text" id="neonatalLabHb" name="labHb"  value="${ ui.encodeHtmlAttribute(formData.labHb ?: '') }" />
                        </div>
                        <div class="transfer-wizard-field transfer-vital-field" id="neonatalLabWbcPanel" style="display:none;">
                            <label for="neonatalLabWbc">${ ui.message("transferapp.patient.transfers.labWbc") }</label>
                            <input type="text" id="neonatalLabWbc" name="labWbc"  value="${ ui.encodeHtmlAttribute(formData.labWbc ?: '') }" />
                        </div>
                        <div class="transfer-wizard-field transfer-vital-field" id="neonatalLabPlateletsPanel" style="display:none;">
                            <label for="neonatalLabPlatelets">${ ui.message("transferapp.patient.transfers.labPlatelets") }</label>
                            <input type="text" id="neonatalLabPlatelets" name="labPlatelets"  value="${ ui.encodeHtmlAttribute(formData.labPlatelets ?: '') }" />
                        </div>
                        <div class="transfer-wizard-field transfer-vital-field">
                            <label for="neonatalLabCrp">${ ui.message("transferapp.patient.transfers.labCrp") }</label>
                            <input type="text" id="neonatalLabCrp" name="labCrp"  value="${ ui.encodeHtmlAttribute(formData.labCrp ?: '') }" />
                        </div>
                        <div class="transfer-wizard-field transfer-vital-field">
                            <label for="neonatalLabBiliTotal">${ ui.message("transferapp.patient.transfers.labBiliTotal") }</label>
                            <input type="text" id="neonatalLabBiliTotal" name="labBiliTotal"  value="${ ui.encodeHtmlAttribute(formData.labBiliTotal ?: '') }" />
                        </div>
                        <div class="transfer-wizard-field transfer-vital-field">
                            <label for="neonatalLabBiliDirect">${ ui.message("transferapp.patient.transfers.labBiliDirect") }</label>
                            <input type="text" id="neonatalLabBiliDirect" name="labBiliDirect"  value="${ ui.encodeHtmlAttribute(formData.labBiliDirect ?: '') }" />
                        </div>
                        <div class="transfer-wizard-field transfer-vital-field">
                            <label for="neonatalLabUe">${ ui.message("transferapp.patient.transfers.labUe") }</label>
                            <input type="text" id="neonatalLabUe" name="labUe"  value="${ ui.encodeHtmlAttribute(formData.labUe ?: '') }" />
                        </div>
                        <div class="transfer-wizard-field transfer-vital-field">
                            <label for="neonatalLabCultures">${ ui.message("transferapp.patient.transfers.labCultures") }</label>
                            <input type="text" id="neonatalLabCultures" name="labCultures"  value="${ ui.encodeHtmlAttribute(formData.labCultures ?: '') }" />
                        </div>
                    </div>
                    <div class="transfer-wizard-field">
                        <label for="neonatalImagingResultsAvailable">${ ui.message("transferapp.patient.transfers.imagingResultsAvailable") }</label>
                        <select id="neonatalImagingResultsAvailable" name="imagingResultsAvailable">
                            <option value="">Select</option>
                            <option value="Yes" <% if (formData.imagingResultsAvailable == 'Yes') { %>selected="selected"<% } %>>Yes</option>
                            <option value="No" <% if (formData.imagingResultsAvailable == 'No') { %>selected="selected"<% } %>>No</option>
                        </select>
                    </div>
                    <div class="transfer-wizard-field" id="neonatalImagingResultsPanel" style="display:none;">
                        <label for="neonatalImagingResults">${ ui.message("transferapp.patient.transfers.imagingResults") }</label>
                        <textarea id="neonatalImagingResults" name="imagingResults" rows="2">${ ui.format(formData.imagingResults ?: '') }</textarea>
                    </div>
                    <div class="transfer-wizard-field">
                        <label for="neonatalPainSedationDrugs">${ ui.message("transferapp.patient.transfers.painSedationDrugs") }</label>
                        <textarea id="neonatalPainSedationDrugs" name="painSedationDrugs" rows="2">${ ui.format(formData.painSedationDrugs ?: '') }</textarea>
                    </div>
                    <div class="transfer-wizard-row transfer-wizard-row-two-col">
                        <div class="transfer-wizard-field">
                            <label><input type="checkbox" id="neonatalImagingReportAttached" name="imagingReportAttached" value="true" <% if (formData.imagingReportAttached == 'true') { %>checked="checked"<% } %> style="width:auto;display:inline-block;margin-right:0.4rem;" /> ${ ui.message("transferapp.patient.transfers.imagingReportAttached") }</label>
                        </div>
                        <div class="transfer-wizard-field">
                            <label><input type="checkbox" id="neonatalLabReportsAttached" name="labReportsAttached" value="true" <% if (formData.labReportsAttached == 'true') { %>checked="checked"<% } %> style="width:auto;display:inline-block;margin-right:0.4rem;" /> ${ ui.message("transferapp.patient.transfers.labReportsAttached") }</label>
                        </div>
                    </div>
                </div>
            </div>

            <!-- Step 7: Summary & Sign-off -->
            <div class="transfer-wizard-step-panel" data-step="7">
                <div class="transfer-wizard-section">
                    <h2 class="transfer-wizard-section-title">Clinical management summary</h2>
                    <div class="transfer-wizard-field">
                        <label for="neonatalClinicalManagementSummary">${ ui.message("transferapp.patient.transfers.clinicalManagementSummary") }</label>
                        <textarea id="neonatalClinicalManagementSummary" name="clinicalManagementSummary" rows="5">${ ui.format(formData.clinicalManagementSummary ?: '') }</textarea>
                    </div>
                </div>

                <div class="transfer-wizard-section">
                    <h2 class="transfer-wizard-section-title">Referring provider sign-off</h2>
                    <div class="transfer-wizard-row transfer-wizard-row-three-col">
                        <div class="transfer-wizard-field">
                            <label for="neonatalReferringProviderName">${ ui.message("transferapp.patient.transfers.referringProviderName") }</label>
                            <input type="text" id="neonatalReferringProviderName" name="referringProviderName"
                                   value="${ ui.encodeHtmlAttribute(formData.referringProviderName ?: '') }" />
                        </div>
                        <div class="transfer-wizard-field">
                            <label for="neonatalReferringProviderQualification">${ ui.message("transferapp.patient.transfers.referringProviderQualification") }</label>
                            <input type="text" id="neonatalReferringProviderQualification" name="referringProviderQualification"  value="${ ui.encodeHtmlAttribute(formData.referringProviderQualification ?: '') }" />
                        </div>
                        <div class="transfer-wizard-field">
                            <label for="neonatalReferringProviderPhone">${ ui.message("transferapp.patient.transfers.referringProviderPhone") }</label>
                            <input type="tel" id="neonatalReferringProviderPhone" name="referringProviderPhone" />
                        </div>
                        <div class="transfer-wizard-field">
                            <label for="neonatalReferringSignedDate">${ ui.message("transferapp.patient.transfers.referringSignedDate") }</label>
                            <input type="text" class="js-date-picker" id="neonatalReferringSignedDate" name="referringSignedDate"
                                   value="${ ui.encodeHtmlAttribute(formData.referringSignedDate ?: '') }" placeholder="Select date" autocomplete="off" />
                        </div>
                        <div class="transfer-wizard-field">
                            <label for="neonatalReferringSignedTime">${ ui.message("transferapp.patient.transfers.referringSignedTime") }</label>
                            <input type="text" class="js-time-picker" id="neonatalReferringSignedTime" name="referringSignedTime"
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
