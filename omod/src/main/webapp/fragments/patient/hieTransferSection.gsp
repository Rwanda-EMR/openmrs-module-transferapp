<%
def ctxPath = (ui.contextPath() ?: "").toString()
while (ctxPath.startsWith("/")) {
	ctxPath = ctxPath.substring(1)
}
def hieRestUrl = "/" + ctxPath + "/ws/rest/v1/transferapp/transfer"
def validateRestUrl = "/" + ctxPath + "/ws/rest/v1/transferapp/transfer/validate"
def feedbackRestUrl = "/" + ctxPath + "/ws/rest/v1/transferapp/transfer/feedback"
%>

<div class="info-section transfer-section hie-transfer-section"
     id="hie-transfer-section"
     data-upid="${ ui.encodeHtmlAttribute(upid ?: '') }"
     data-patient-id="${ ui.encodeHtmlAttribute((patientId ?: '') as String) }"
     data-registration-encounter-id="${ ui.encodeHtmlAttribute((registrationEncounterId ?: '') as String) }"
     data-transfer-id="${ ui.encodeHtmlAttribute(transferId ?: '') }"
     data-has-transfer-id="${ hasTransferIdObs ? 'true' : 'false' }"
     data-list-from-hie="${ listFromHie ? 'true' : 'false' }"
     data-show-section="${ showSection ? 'true' : 'false' }"
     data-can-list="${ canListTransfers ? 'true' : 'false' }"
     data-can-validate="${ canValidateTransfer ? 'true' : 'false' }"
     data-can-provide-feedback="${ canProvideFeedback ? 'true' : 'false' }"
     data-current-facility="${ ui.encodeHtmlAttribute(currentFacilityName ?: '') }"
     data-rest-url="${ ui.encodeHtmlAttribute(hieRestUrl) }"
     data-validate-url="${ ui.encodeHtmlAttribute(validateRestUrl) }"
     data-feedback-url="${ ui.encodeHtmlAttribute(feedbackRestUrl) }"
     data-facilities-url="${ ui.encodeHtmlAttribute(feedbackRestUrl + '/facilities') }">

    <div class="info-header">
        <i class="icon-random"></i>
        <h3>${ ui.message(config.label ? config.label : "transferapp.patient.hieTransfer.title").toUpperCase() }</h3>
        <div class="transfer-section-subtitle">
            ${ ui.message("transferapp.patient.hieTransfer.subtitle") }
        </div>
    </div>

    <div class="info-body">
        <% if (!canListTransfers) { %>
            <p>${ ui.encodeHtmlContent(accessDeniedMessage ?: ui.message("transferapp.patient.hieTransfer.listNotAllowed", "Task: transferapp.listTransfers")) }</p>
        <% } else if (statusMessage) { %>
            <p class="hie-transfer-status">${ ui.encodeHtmlContent(statusMessage) }</p>
        <% } else if (showSection && hasTransferIdObs) { %>
            <div class="hie-transfer-recorded">
                <span>${ ui.message("transferapp.patient.hieTransfer.recorded") }</span>
                <a id="hie-open-transfer-link"
                   class="hie-transfer-view-link"
                   href="javascript:void(0);"
                   role="button"
                   data-transfer-id="${ ui.encodeHtmlAttribute(transferId) }"
                   data-upid="${ ui.encodeHtmlAttribute(upid) }"
                   title="${ ui.encodeHtmlAttribute(ui.message('transferapp.patient.hieTransfer.openTransfer')) }">
                    ${ ui.message("transferapp.patient.hieTransfer.openTransfer") }
                    <i class="icon-eye-open"></i>
                </a>
            </div>
        <% } else if (showSection && listFromHie) { %>
            <div id="hie-transfer-list-status" class="hie-transfer-status">
                <i class="icon-spinner icon-spin"></i>
                ${ ui.message("transferapp.patient.hieTransfer.loadingList") }
            </div>
            <div id="hie-transfer-list-wrap" class="transfer-table-wrapper" style="display:none;">
                <table id="hie-transfer-list-table" class="transfer-datatable">
                    <thead>
                        <tr>
                            <th>${ ui.message("transferapp.patient.transfers.column.date") }</th>
                            <th>${ ui.message("transferapp.patient.transfers.column.from") }</th>
                            <th>${ ui.message("transferapp.patient.transfers.column.destination") }</th>
                            <th>${ ui.message("transferapp.patient.transfers.column.action") }</th>
                        </tr>
                    </thead>
                    <tbody id="hie-transfer-list-body"></tbody>
                </table>
            </div>
        <% } else { %>
            <p class="hie-transfer-status">${ ui.message("transferapp.patient.hieTransfer.empty") }</p>
        <% } %>
    </div>
</div>

<% if (canListTransfers) { %>
${ ui.includeFragment("transferapp", "patient/hieTransferPreviewDialog") }

<style>
.hie-transfer-recorded {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}
a.hie-transfer-view-link {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-weight: 600;
  color: #0f766e;
  text-decoration: none;
  cursor: pointer;
  pointer-events: auto;
  position: relative;
  z-index: 2;
}
a.hie-transfer-view-link:hover {
  color: #0d9488;
  text-decoration: underline;
}
.hie-transfer-status {
  margin: 0;
}
.transfer-used-badge {
  display: inline-block;
  margin-left: 8px;
  padding: 1px 8px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.02em;
  color: #9a3412;
  background: #ffedd5;
  border: 1px solid #fdba74;
  vertical-align: middle;
}
</style>
<% } %>
