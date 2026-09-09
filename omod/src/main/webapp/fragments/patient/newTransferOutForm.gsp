<%
ui.includeCss("transferapp", "styles/transferWizard.css")
ui.includeCss("transferapp", "styles/flatpickr.min.css")
ui.includeCss("transferapp", "styles/select2.min.css")
def ambulanceFacilitiesCtxPath = (ui.contextPath() ?: "openmrs").toString()
while (ambulanceFacilitiesCtxPath.startsWith("/")) {
	ambulanceFacilitiesCtxPath = ambulanceFacilitiesCtxPath.substring(1)
}
def ambulanceProviderFacilitiesUrl = "/" + ambulanceFacilitiesCtxPath + "/module/transferapp/transfer/ambulanceProviderFacilities.form"
%>

<g:if test="${error != null && error.trim().length() > 0}">
    <p style="color: red;">${ ui.format(error) }</p>
</g:if>

<g:if test="${formData != null}">
<div class="transfer-wizard-shell">
    <header class="transfer-wizard-page-header">
        <h1 class="transfer-wizard-page-title">
            <% if (formData.transferUuid) { %>
                ${ ui.message("transferapp.patient.transfers.editTransferOut") }
            <% } else { %>
                External Transfer Form
            <% } %>
        </h1>
    </header>

    <div class="transfer-wizard-panel" style="padding: 0 5px;">
        <form id="moh-transfer-wizard-form" class="transfer-out-form" novalidate="novalidate"
              data-editing="${ formData.transferUuid ? 'true' : 'false' }"
              data-preferred-receiving-service="${ ui.encodeHtmlAttribute(formData.receivingService ?: '') }">
            <input type="hidden" name="patientId" value="${ formData.patientId }" />
            <% if (formData.transferUuid) { %>
            <input type="hidden" name="transferUuid" value="${ ui.encodeHtmlAttribute(formData.transferUuid) }" />
            <% } %>
            <input type="hidden" id="receivingFacilityId" name="receivingFacilityId"
                   value="${ formData.receivingFacilityId != null ? formData.receivingFacilityId : '' }" />

            <div class="transfer-wizard-section">
                <h2 class="transfer-wizard-section-title">Referral information</h2>
                <div class="transfer-wizard-row transfer-wizard-row-three-col"
                     style="display:grid !important;grid-template-columns:repeat(3,minmax(0,1fr)) !important;gap:12px !important;">
                    <div class="transfer-wizard-field">
                        <label for="decisionToTransferAt">Decision date &amp; time</label>
                        <input type="text" class="js-datetime-picker" id="decisionToTransferAt" name="decisionToTransferAt"
                               value="${ ui.encodeHtmlAttribute(formData.decisionToTransferAt ?: '') }" required
                               placeholder="Select date and time" autocomplete="off" />
                    </div>
                    <div class="transfer-wizard-field">
                        <label for="callingTime">Calling time</label>
                        <input type="text" class="js-time-picker" id="callingTime" name="callingTime"
                               value="${ ui.encodeHtmlAttribute(formData.callingTime ?: '') }"
                               placeholder="Select time" autocomplete="off" />
                    </div>
                    <div class="transfer-wizard-field">
                        <label for="receivingFacilityCode">Receiving facility</label>
                        <select id="receivingFacilityCode" name="receivingFacilityCode" required>
                            <option value="">Select receiving facility</option>
                            <% formData.receivingFacilities.each { facility -> %>
                                <option value="${ ui.encodeHtmlAttribute(facility.value) }"
                                        data-receiving-facility-id="${ facility.receivingFacilityId ?: '' }"
                                        ${ formData.receivingFacilityCode == facility.value ? 'selected="selected"' : '' }>
                                    ${ ui.format(facility.label) }
                                </option>
                            <% } %>
                        </select>
                    </div>
                    <div class="transfer-wizard-field">
                        <label for="receivingService">${ ui.message("transferapp.patient.transfers.receivingService") }</label>
                        <select id="receivingService" name="receivingService" class="js-transfer-receiving-service-select" required
                                data-placeholder="${ ui.encodeHtmlAttribute(ui.message('transferapp.patient.transfers.receivingService.placeholder')) }">
                            <option value=""></option>
                            <% if (formData.receivingService) { %>
                                <option value="${ ui.encodeHtmlAttribute(formData.receivingService) }" selected="selected">
                                    ${ ui.encodeHtmlContent(formData.receivingService) }
                                </option>
                            <% } %>
                        </select>
                    </div>
                    <div class="transfer-wizard-field">
                        <label for="staffContactedName">Staff contacted</label>
                        <input type="text" id="staffContactedName" name="staffContactedName"
                               value="${ ui.encodeHtmlAttribute(formData.staffContactedName ?: '') }" />
                    </div>
                    <div class="transfer-wizard-field">
                        <label for="staffContactedPhone">Contact phone</label>
                        <input type="tel" id="staffContactedPhone" name="staffContactedPhone"
                               value="${ ui.encodeHtmlAttribute(formData.staffContactedPhone ?: '') }" />
                    </div>
                </div>
            </div>

            <div class="transfer-wizard-section">
                <h2 class="transfer-wizard-section-title">${ ui.message("transferapp.patient.transfers.transferType") }</h2>
                <div class="transfer-type-options">
                    <% formData.transferTypes.each { type -> %>
                    <span class="transfer-type-option">
                        <input type="radio" name="transferType" id="transferType_${ ui.encodeHtmlAttribute(type.value) }"
                               value="${ ui.encodeHtmlAttribute(type.value) }" required
                               ${ formData.transferType == type.value ? 'checked="checked"' : '' } />
                        <label for="transferType_${ ui.encodeHtmlAttribute(type.value) }">${ ui.format(type.label) }</label>
                    </span>
                    <% } %>
                </div>
            </div>

            <div id="emergencyFields" class="transfer-emergency-panel">
                <div class="transfer-wizard-row transfer-wizard-row-two-col"
                     style="display:grid !important;grid-template-columns:repeat(2,minmax(0,1fr)) !important;gap:12px !important;">
                    <div class="transfer-wizard-field">
                        <label for="ambulanceCalledTime">${ ui.message("transferapp.patient.transfers.ambulanceCalledTime") }</label>
                        <input type="text" class="js-time-picker" id="ambulanceCalledTime" name="ambulanceCalledTime"
                               value="${ ui.encodeHtmlAttribute(formData.ambulanceCalledTime ?: '') }"
                               placeholder="Select time" autocomplete="off" data-emergency-required="true" />
                    </div>
                    <div class="transfer-wizard-field">
                        <label for="departureFromReferringTime">${ ui.message("transferapp.patient.transfers.departureFromReferringTime") }</label>
                        <input type="text" class="js-time-picker" id="departureFromReferringTime" name="departureFromReferringTime"
                               value="${ ui.encodeHtmlAttribute(formData.departureFromReferringTime ?: '') }"
                               placeholder="Select time" autocomplete="off" data-emergency-required="true" />
                    </div>
                </div>
            </div>

            <div class="transfer-wizard-section">
                <h2 class="transfer-wizard-section-title">${ ui.message("transferapp.patient.transfers.caregiver") }</h2>
                <div class="transfer-wizard-row transfer-wizard-row-two-col"
                     style="display:grid !important;grid-template-columns:repeat(2,minmax(0,1fr)) !important;gap:12px !important;">
                    <div class="transfer-wizard-field">
                        <label for="caregiverName">${ ui.message("transferapp.patient.transfers.caregiverName") }</label>
                        <input type="text" id="caregiverName" name="caregiverName"
                               value="${ ui.encodeHtmlAttribute(formData.caregiverName ?: '') }"
                               placeholder="${ ui.encodeHtmlAttribute(ui.message('transferapp.patient.transfers.caregiverName.placeholder')) }" />
                    </div>
                    <div class="transfer-wizard-field">
                        <label for="caregiverTelephone">${ ui.message("transferapp.patient.transfers.caregiverTelephone") }</label>
                        <input type="tel" id="caregiverTelephone" name="caregiverTelephone"
                               value="${ ui.encodeHtmlAttribute(formData.caregiverTelephone ?: '') }"
                               placeholder="${ ui.encodeHtmlAttribute(ui.message('transferapp.patient.transfers.caregiverTelephone.placeholder')) }" />
                    </div>
                </div>
            </div>

            <div class="transfer-wizard-section">
                <h2 class="transfer-wizard-section-title">${ ui.message("transferapp.patient.transfers.clinicalInformation") }</h2>
                <div class="transfer-wizard-row transfer-wizard-row-three-col"
                     style="display:grid !important;grid-template-columns:repeat(3,minmax(0,1fr)) !important;gap:12px !important;">
                    <div class="transfer-wizard-field">
                        <label for="reasonForTransfer">${ ui.message("transferapp.patient.transfers.reason") }</label>
                        <textarea id="reasonForTransfer" name="reasonForTransfer" rows="4" required
                                  placeholder="${ ui.encodeHtmlAttribute(ui.message('transferapp.patient.transfers.reason.placeholder')) }">${ ui.encodeHtmlContent(formData.reasonForTransfer ?: "") }</textarea>
                    </div>
                    <div class="transfer-wizard-field">
                        <label for="clinicalPresentation">${ ui.message("transferapp.patient.transfers.clinicalPresentation") }</label>
                        <textarea id="clinicalPresentation" name="clinicalPresentation" rows="4" required
                                  placeholder="${ ui.encodeHtmlAttribute(ui.message('transferapp.patient.transfers.clinicalPresentation.placeholder')) }">${ ui.encodeHtmlContent(formData.clinicalPresentation ?: "") }</textarea>
                    </div>
                    <div class="transfer-wizard-field">
                        <label for="laboratory">${ ui.message("transferapp.patient.transfers.laboratory") }</label>
                        <textarea id="laboratory" name="laboratory" rows="4"
                                  placeholder="${ ui.encodeHtmlAttribute(ui.message('transferapp.patient.transfers.laboratory.placeholder')) }">${ ui.encodeHtmlContent(formData.laboratory ?: "") }</textarea>
                    </div>
                </div>
                <div class="transfer-wizard-row transfer-wizard-row-three-col"
                     style="display:grid !important;grid-template-columns:repeat(3,minmax(0,1fr)) !important;gap:12px !important;margin-top:12px;">
                    <div class="transfer-wizard-field">
                        <label for="otherNotes">${ ui.message("transferapp.patient.transfers.others") }</label>
                        <textarea id="otherNotes" name="otherNotes" rows="4"
                                  placeholder="${ ui.encodeHtmlAttribute(ui.message('transferapp.patient.transfers.others.placeholder')) }">${ ui.encodeHtmlContent(formData.othersNotes ?: "") }</textarea>
                    </div>
                    <div class="transfer-wizard-field">
                        <label for="proceduresTreatments">${ ui.message("transferapp.patient.transfers.proceduresAndTreatments") }</label>
                        <textarea id="proceduresTreatments" name="proceduresTreatments" rows="4"
                                  placeholder="${ ui.encodeHtmlAttribute(ui.message('transferapp.patient.transfers.proceduresAndTreatments.placeholder')) }">${ ui.encodeHtmlContent(formData.proceduresAndTreatments ?: "") }</textarea>
                    </div>
                    <div class="transfer-wizard-field">
                        <label for="diagnosis">${ ui.message("transferapp.patient.transfers.diagnosis") }</label>
                        <textarea id="diagnosis" name="diagnosis" rows="4" required
                                  placeholder="${ ui.encodeHtmlAttribute(ui.message('transferapp.patient.transfers.diagnosis.placeholder')) }">${ ui.encodeHtmlContent(formData.diagnosis ?: "") }</textarea>
                    </div>
                </div>
            </div>

            <div class="transfer-wizard-section">
                <h2 class="transfer-wizard-section-title">${ ui.message("transferapp.patient.transfers.transportationType") }</h2>
                <div class="transfer-transport-options">
                    <% formData.transportationTypes.each { transport -> %>
                    <span class="transfer-transport-option">
                        <input type="radio" name="transportationType"
                               id="transportationType_${ ui.encodeHtmlAttribute(transport.value) }"
                               value="${ ui.encodeHtmlAttribute(transport.value) }"
                               data-transport-value="${ ui.encodeHtmlAttribute(transport.value) }"
                               ${ formData.transportationType == transport.value ? 'checked="checked"' : '' } />
                        <label for="transportationType_${ ui.encodeHtmlAttribute(transport.value) }">${ ui.format(transport.label) }</label>
                    </span>
                    <% } %>
                    <div id="transportOtherField" class="transfer-transport-other transfer-wizard-field">
                        <label for="transportationOtherSpec">${ ui.message("transferapp.patient.transfers.transportationOtherSpec") }</label>
                        <input type="text" id="transportationOtherSpec" name="transportationOtherSpec" maxlength="255"
                               value="${ ui.encodeHtmlAttribute(formData.transportationOtherSpec ?: '') }"
                               placeholder="${ ui.message('transferapp.patient.transfers.transportationOtherSpec.placeholder') }" />
                    </div>
                    <div id="ambulanceProviderField" class="transfer-ambulance-provider transfer-wizard-field"
                         data-preferred-fosa-id="${ ui.encodeHtmlAttribute(formData.ambulanceProviderFosaId ?: '') }"
                         data-preferred-name="${ ui.encodeHtmlAttribute(formData.ambulanceProviderName ?: '') }"
                         data-current-fosa-id="${ ui.encodeHtmlAttribute(formData.currentSendingFosaId ?: '') }"
                         data-facilities-url="${ ui.encodeHtmlAttribute(ambulanceProviderFacilitiesUrl) }">
                        <label for="ambulanceProviderFosaId">${ ui.message("transferapp.patient.transfers.ambulanceProvider") }</label>
                        <input type="hidden" id="ambulanceProviderName" name="ambulanceProviderName"
                               value="${ ui.encodeHtmlAttribute(formData.ambulanceProviderName ?: '') }" />
                        <select id="ambulanceProviderFosaId" name="ambulanceProviderFosaId"
                                class="js-transfer-ambulance-provider-select"
                                data-placeholder="${ ui.encodeHtmlAttribute(ui.message('transferapp.patient.transfers.ambulanceProvider.placeholder')) }">
                            <option value=""></option>
                            <% if (formData.ambulanceProviderFosaId) { %>
                                <option value="${ ui.encodeHtmlAttribute(formData.ambulanceProviderFosaId) }" selected="selected">
                                    ${ ui.encodeHtmlContent(formData.ambulanceProviderName ?: formData.ambulanceProviderFosaId) }
                                </option>
                            <% } %>
                        </select>
                    </div>
                </div>
            </div>

        </form>
    </div>
</div>
</g:if>
