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
        <h1 class="transfer-wizard-page-title">External Transfer Form</h1>
    </header>

    <div class="transfer-wizard-panel" style="padding: 0 5px;">
        <form id="moh-transfer-wizard-form" class="transfer-out-form" novalidate="novalidate">
            <input type="hidden" name="patientId" value="${ formData.patientId }" />
            <input type="hidden" id="receivingFacilityId" name="receivingFacilityId" value="" />

            <div class="transfer-wizard-section">
                <h2 class="transfer-wizard-section-title">Referral information</h2>
                <div class="transfer-wizard-row transfer-wizard-row-three-col"
                     style="display:grid !important;grid-template-columns:repeat(3,minmax(0,1fr)) !important;gap:12px !important;">
                    <div class="transfer-wizard-field">
                        <label for="decisionToTransferAt">Decision date &amp; time</label>
                        <input type="text" class="js-datetime-picker" id="decisionToTransferAt" name="decisionToTransferAt"
                               value="" required placeholder="Select date and time" autocomplete="off" />
                    </div>
                    <div class="transfer-wizard-field">
                        <label for="callingTime">Calling time</label>
                        <input type="text" class="js-time-picker" id="callingTime" name="callingTime"
                               value="" placeholder="Select time" autocomplete="off" />
                    </div>
                    <div class="transfer-wizard-field">
                        <label for="receivingFacilityCode">Receiving facility</label>
                        <select id="receivingFacilityCode" name="receivingFacilityCode" required>
                            <option value="">Select receiving facility</option>
                            <% formData.receivingFacilities.each { facility -> %>
                                <option value="${ ui.encodeHtmlAttribute(facility.value) }"
                                        data-receiving-facility-id="${ facility.receivingFacilityId ?: '' }">
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
                        </select>
                    </div>
                    <div class="transfer-wizard-field">
                        <label for="staffContactedName">Staff contacted</label>
                        <input type="text" id="staffContactedName" name="staffContactedName" value="" />
                    </div>
                    <div class="transfer-wizard-field">
                        <label for="staffContactedPhone">Contact phone</label>
                        <input type="tel" id="staffContactedPhone" name="staffContactedPhone" value="" />
                    </div>
                </div>
            </div>

            <div class="transfer-wizard-section">
                <h2 class="transfer-wizard-section-title">${ ui.message("transferapp.patient.transfers.transferType") }</h2>
                <div class="transfer-type-options">
                    <% formData.transferTypes.each { type -> %>
                    <span class="transfer-type-option">
                        <input type="radio" name="transferType" id="transferType_${ ui.encodeHtmlAttribute(type.value) }"
                               value="${ ui.encodeHtmlAttribute(type.value) }" required />
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
                               value="" placeholder="Select time" autocomplete="off" data-emergency-required="true" />
                    </div>
                    <div class="transfer-wizard-field">
                        <label for="departureFromReferringTime">${ ui.message("transferapp.patient.transfers.departureFromReferringTime") }</label>
                        <input type="text" class="js-time-picker" id="departureFromReferringTime" name="departureFromReferringTime"
                               value="" placeholder="Select time" autocomplete="off" data-emergency-required="true" />
                    </div>
                </div>
            </div>

            <div class="transfer-wizard-section transfer-wizard-field">
                <label for="reasonForTransfer" class="transfer-wizard-section-title">Reason for transfer</label>
                <textarea id="reasonForTransfer" name="reasonForTransfer" rows="4" required placeholder="Enter the reason for transfer"></textarea>
            </div>

            <div class="transfer-wizard-section">
                <h2 class="transfer-wizard-section-title">${ ui.message("transferapp.patient.transfers.transportationType") }</h2>
                <div class="transfer-transport-options">
                    <% formData.transportationTypes.each { transport -> %>
                    <span class="transfer-transport-option">
                        <input type="radio" name="transportationType"
                               id="transportationType_${ ui.encodeHtmlAttribute(transport.value) }"
                               value="${ ui.encodeHtmlAttribute(transport.value) }"
                               data-transport-value="${ ui.encodeHtmlAttribute(transport.value) }" />
                        <label for="transportationType_${ ui.encodeHtmlAttribute(transport.value) }">${ ui.format(transport.label) }</label>
                    </span>
                    <% } %>
                </div>
                <div id="transportOtherField" class="transfer-transport-other transfer-wizard-field">
                    <label for="transportationOtherSpec">${ ui.message("transferapp.patient.transfers.transportationOtherSpec") }</label>
                    <input type="text" id="transportationOtherSpec" name="transportationOtherSpec" maxlength="255"
                           placeholder="${ ui.message('transferapp.patient.transfers.transportationOtherSpec.placeholder') }" />
                </div>
            </div>

        </form>
    </div>
</div>
</g:if>
