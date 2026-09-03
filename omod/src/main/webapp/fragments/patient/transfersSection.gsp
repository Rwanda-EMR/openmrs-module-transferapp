<% if (!(canListTransfers || canCreateTransfer)) { %>
<div class="info-section">
    <div class="info-header">
        <i class="icon-retweet"></i>
        <h3>${ ui.message("transferapp.patient.transfers.title") }</h3>
    </div>
    <div class="info-body">
        <p>${ ui.encodeHtmlContent(accessDeniedMessage ?: ui.message("transferapp.patient.transfers.accessDenied", requiredListPrivilege, requiredCreatePrivilege)) }</p>
    </div>
</div>
<% } else { %>
<% if (canCreateTransfer) { %>
<div id="new-transfer-out-dialog" class="dialog transfer-wizard-dialog" style="display: none"
     data-provider-profile-complete="${ providerProfileComplete ? 'true' : 'false' }"
     data-profile-incomplete-message="${ ui.encodeHtmlAttribute(ui.message('transferapp.patient.transfers.profileIncomplete')) }">
    <div class="dialog-header" style="display: none;">
        <i class="icon-retweet"></i>
        <h3 id="new-transfer-out-title">
            ${ ui.message("transferapp.patient.transfers.newTransferOut") }
        </h3>
    </div>
    <div class="dialog-content">
        <div id="new-transfer-out-data">
            <div style="padding: 10px;"><i class="icon-spinner icon-spin"></i> ${ ui.message("transferapp.patient.transfers.wizard.loading") }</div>
        </div>
    </div>
    <div class="dialog-footer">
        <button type="button" id="transfer-wizard-submit-btn" class="transfer-wizard-btn transfer-wizard-btn-primary">
            Save referral information
        </button>
        <button type="button" id="transfer-wizard-cancel" class="transfer-wizard-btn transfer-wizard-btn-outline">
            ${ ui.message("coreapps.cancel") }
        </button>
    </div>
</div>

<div id="transfer-profile-incomplete-overlay" class="transfer-notice-overlay" style="display: none;"></div>
<div id="transfer-profile-incomplete-dialog" class="dialog transfer-notice-dialog" style="display: none;"
     data-profile-url="${ ui.encodeHtmlAttribute(ui.pageLink('transferapp', 'transferProfile') + '?app=transferapp.dashboard') }">
    <div class="dialog-header">
        <i class="icon-user"></i>
        <h3>${ ui.message("transferapp.patient.transfers.profileIncompleteTitle") }</h3>
    </div>
    <div class="dialog-content">
        <p id="transfer-profile-incomplete-message" class="transfer-notice-message"></p>
        <div class="transfer-notice-actions">
            <a id="transfer-profile-incomplete-goto" class="button confirm" href="javascript:void(0);">
                ${ ui.message("transferapp.patient.transfers.goToProfile") }
            </a>
            <button type="button" id="transfer-profile-incomplete-close" class="cancel">
                ${ ui.message("coreapps.close") }
            </button>
        </div>
    </div>
</div>

<style>
#new-transfer-out-dialog.transfer-wizard-dialog.dialog {
  position: fixed !important;
  top: 50% !important;
  left: 50% !important;
  transform: translate(-50%, -50%) !important;
  z-index: 10000 !important;
  margin: 0 !important;
  padding: 0 !important;
  background: #eef2f6;
  border: 1px solid #d8e0ea;
  border-radius: 12px;
  box-shadow: 0 12px 40px rgba(15, 23, 42, 0.18);
  width: 1100px !important;
  max-width: 95% !important;
  height: 94vh !important;
  max-height: 94vh !important;
  flex-direction: column !important;
  overflow: hidden !important;
}

/* Only show when explicitly opened — never override jQuery .hide() with display:!important */
#new-transfer-out-dialog.transfer-wizard-dialog.dialog.transfer-wizard-open {
  display: flex !important;
}

#new-transfer-out-dialog .dialog-content {
  padding: 0 !important;
  flex: 1 1 0 !important;
  min-height: 0 !important;
  overflow-x: hidden !important;
  overflow-y: auto !important;
  -webkit-overflow-scrolling: touch;
  display: block !important;
  background: #fff;
}

#new-transfer-out-data {
  min-height: 0;
  display: block;
}

#new-transfer-out-data .transfer-wizard-shell {
  height: auto !important;
  min-height: 0 !important;
  max-height: none !important;
  overflow: visible !important;
  display: block !important;
}

#new-transfer-out-data .transfer-wizard-page-header {
  position: sticky;
  top: 0;
  z-index: 2;
}

#new-transfer-out-data .transfer-wizard-panel {
  overflow: visible !important;
  max-height: none !important;
  padding-right: 0.35rem;
  padding-bottom: 0.75rem;
}

#new-transfer-out-dialog .dialog-footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 0.5rem;
  flex: 0 0 auto !important;
  padding: 0.5rem 0.75rem;
  border-top: 1px solid #e2e8f0;
  background: #fff;
}

.transfer-notice-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  z-index: 10010;
}

#transfer-profile-incomplete-dialog.transfer-notice-dialog.dialog {
  position: fixed;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  z-index: 10011;
  background: #fff;
  border: 1px solid #00473f;
  border-radius: 4px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.3);
  width: 480px;
  max-width: 92%;
  margin: 0;
  display: none;
  flex-direction: column;
}

#transfer-profile-incomplete-dialog.transfer-notice-dialog.is-open {
  display: flex !important;
}

#transfer-profile-incomplete-dialog .dialog-header {
  padding: 14px 18px;
  border-bottom: 1px solid #00473f;
  background: #00473f;
  border-radius: 4px 4px 0 0;
  color: #fff;
  display: flex;
  align-items: center;
  gap: 10px;
}

#transfer-profile-incomplete-dialog .dialog-header i,
#transfer-profile-incomplete-dialog .dialog-header h3 {
  margin: 0;
  color: #fff;
  font-size: 17px;
  font-weight: bold;
}

#transfer-profile-incomplete-dialog .dialog-content {
  padding: 20px 18px 16px;
}

#transfer-profile-incomplete-dialog .transfer-notice-message {
  margin: 0 0 18px;
  color: #334155;
  font-size: 14px;
  line-height: 1.5;
}

#transfer-profile-incomplete-dialog .transfer-notice-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  flex-wrap: wrap;
}

#transfer-profile-incomplete-dialog .transfer-notice-actions .confirm {
  background: #00473f;
  color: #fff;
  border: 1px solid #00352f;
  text-decoration: none;
}

#transfer-profile-incomplete-dialog .transfer-notice-actions .confirm:hover {
  background: #0a5c52;
  color: #fff;
}
</style>

<div id="new-maternity-transfer-out-dialog" class="dialog transfer-wizard-dialog" style="display: none">
    <div class="dialog-header" style="display: none;">
        <i class="icon-retweet"></i>
        <h3 id="new-maternity-transfer-out-title">
            Maternity Transfer
        </h3>
    </div>
    <div class="dialog-content">
        <div id="new-maternity-transfer-out-data">
            <div style="padding: 10px;"><i class="icon-spinner icon-spin"></i> ${ ui.message("transferapp.patient.transfers.wizard.loading") }</div>
        </div>
    </div>
    <div class="dialog-footer">
        <button type="button" id="maternity-wizard-back" class="transfer-wizard-btn transfer-wizard-btn-outline">
            Back
        </button>
        <button type="button" id="maternity-wizard-next" class="transfer-wizard-btn transfer-wizard-btn-primary">
            Continue
        </button>
        <button type="button" id="maternity-wizard-submit-btn" class="transfer-wizard-btn transfer-wizard-btn-primary">
            Save maternity transfer
        </button>
        <button type="button" id="maternity-wizard-cancel" class="transfer-wizard-btn transfer-wizard-btn-outline">
            ${ ui.message("coreapps.cancel") }
        </button>
    </div>
</div>

<style>
#new-maternity-transfer-out-dialog.transfer-wizard-dialog.dialog {
  position: fixed !important;
  top: 50% !important;
  left: 50% !important;
  transform: translate(-50%, -50%) !important;
  z-index: 10000 !important;
  margin: 0 !important;
  padding: 0 !important;
  background: #eef2f6;
  border: 1px solid #d8e0ea;
  border-radius: 12px;
  box-shadow: 0 12px 40px rgba(15, 23, 42, 0.18);
  max-width: 95% !important;
  height: 94vh !important;
  max-height: 94vh !important;
  width: 1100px !important;
  flex-direction: column !important;
  overflow: hidden !important;
}

/* Only show when explicitly opened — never override jQuery .hide() with display:!important */
#new-maternity-transfer-out-dialog.transfer-wizard-dialog.dialog.transfer-wizard-open {
  display: flex !important;
}

#new-maternity-transfer-out-dialog .dialog-content {
  padding: 0 !important;
  flex: 1 1 0 !important;
  min-height: 0 !important;
  overflow-x: hidden !important;
  overflow-y: auto !important;
  -webkit-overflow-scrolling: touch;
  display: block !important;
  background: #fff;
}

#new-maternity-transfer-out-data {
  min-height: 0;
  display: block;
}

#new-maternity-transfer-out-data .transfer-wizard-shell {
  height: auto !important;
  min-height: 0 !important;
  max-height: none !important;
  overflow: visible !important;
  display: block !important;
}

#new-maternity-transfer-out-data .transfer-wizard-panel {
  overflow: visible !important;
  max-height: none !important;
  padding-right: 0.35rem;
}

#new-maternity-transfer-out-dialog .dialog-footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 0.5rem;
  padding: 0.5rem 0.75rem;
  border-top: 1px solid #e2e8f0;
  background: #fff;
}
</style>

<div id="new-neonatal-transfer-out-dialog" class="dialog transfer-wizard-dialog" style="display: none">
    <div class="dialog-header" style="display: none;">
        <i class="icon-retweet"></i>
        <h3 id="new-neonatal-transfer-out-title">
            Neonatal Transfer
        </h3>
    </div>
    <div class="dialog-content">
        <div id="new-neonatal-transfer-out-data">
            <div style="padding: 10px;"><i class="icon-spinner icon-spin"></i> ${ ui.message("transferapp.patient.transfers.wizard.loading") }</div>
        </div>
    </div>
    <div class="dialog-footer">
        <button type="button" id="neonatal-wizard-back" class="transfer-wizard-btn transfer-wizard-btn-outline">
            Back
        </button>
        <button type="button" id="neonatal-wizard-next" class="transfer-wizard-btn transfer-wizard-btn-primary">
            Continue
        </button>
        <button type="button" id="neonatal-wizard-submit-btn" class="transfer-wizard-btn transfer-wizard-btn-primary">
            Save neonatal transfer
        </button>
        <button type="button" id="neonatal-wizard-cancel" class="transfer-wizard-btn transfer-wizard-btn-outline">
            ${ ui.message("coreapps.cancel") }
        </button>
    </div>
</div>

<style>
#new-neonatal-transfer-out-dialog.transfer-wizard-dialog.dialog {
  position: fixed !important;
  top: 50% !important;
  left: 50% !important;
  transform: translate(-50%, -50%) !important;
  z-index: 10000 !important;
  margin: 0 !important;
  padding: 0 !important;
  background: #eef2f6;
  border: 1px solid #d8e0ea;
  border-radius: 12px;
  box-shadow: 0 12px 40px rgba(15, 23, 42, 0.18);
  max-width: 95% !important;
  height: 94vh !important;
  max-height: 94vh !important;
  width: 1100px !important;
  flex-direction: column !important;
  overflow: hidden !important;
}

/* Only show when explicitly opened — never override jQuery .hide() with display:!important */
#new-neonatal-transfer-out-dialog.transfer-wizard-dialog.dialog.transfer-wizard-open {
  display: flex !important;
}

#new-neonatal-transfer-out-dialog .dialog-content {
  padding: 0 !important;
  flex: 1 1 0 !important;
  min-height: 0 !important;
  overflow-x: hidden !important;
  overflow-y: auto !important;
  -webkit-overflow-scrolling: touch;
  display: block !important;
  background: #fff;
}

#new-neonatal-transfer-out-data {
  min-height: 0;
  display: block;
}

#new-neonatal-transfer-out-data .transfer-wizard-shell {
  height: auto !important;
  min-height: 0 !important;
  max-height: none !important;
  overflow: visible !important;
  display: block !important;
}

#new-neonatal-transfer-out-data .transfer-wizard-panel {
  overflow: visible !important;
  max-height: none !important;
  padding-right: 0.35rem;
}

#new-neonatal-transfer-out-dialog .dialog-footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 0.5rem;
  padding: 0.5rem 0.75rem;
  border-top: 1px solid #e2e8f0;
  background: #fff;
}
</style>
<% } %>

<% if (canListTransfers) { %>
<div id="transfer-preview-dialog" class="dialog transfer-preview-dialog" style="display: none">
    <div class="dialog-header">
        <i class="icon-retweet"></i>
        <h3>${ ui.message("transferapp.patient.transfers.previewTitle") }</h3>
    </div>
    <div class="dialog-content">
        <div id="transfer-preview-body"></div>
        <div class="transfer-preview-actions">
            <% if (canCreateTransfer) { %>
                <button type="button" id="transfer-preview-submit" class="confirm">
                    ${ ui.message("transferapp.patient.transfers.submitToHie") }
                </button>
            <% } %>
            <button type="button" id="transfer-preview-close" class="cancel">
                ${ ui.message("coreapps.close") }
            </button>
        </div>
    </div>
</div>

<style>
#transfer-preview-dialog .transfer-preview-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 4px;
}

#transfer-preview-dialog #transfer-preview-submit {
  background: #0f766e;
  color: #fff;
  border: 1px solid #0a5c52;
}

#transfer-preview-dialog #transfer-preview-submit:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
#transfer-preview-dialog.transfer-preview-dialog.dialog {
  position: fixed;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  z-index: 10001;
  background: white;
  border: 1px solid #00473f;
  border-radius: 4px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.3);
  max-width: 95%;
  max-height: 92vh;
  width: 1400px;
  display: flex;
  flex-direction: column;
  margin: 0;
}

#transfer-preview-dialog .dialog-header {
  padding: 15px 20px;
  border-bottom: 1px solid #00473f;
  background: #00473f;
  border-radius: 4px 4px 0 0;
  flex-shrink: 0;
  color: #fff;
}

#transfer-preview-dialog .dialog-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: bold;
  color: #fff;
}

#transfer-preview-dialog .dialog-content {
  padding: 20px;
  overflow-y: auto;
  overflow-x: hidden;
  flex: 1;
  min-height: 0;
}

#transfer-preview-body {
  max-height: 70vh;
  overflow-y: auto;
  overflow-x: hidden;
  margin-bottom: 15px;
}
</style>
<% } %>

<div class="info-section transfer-section">
    <div class="info-header">
        <i class="icon-retweet"></i>
        <h3>${ ui.message(config.label ? config.label : "transferapp.patient.transfers.title").toUpperCase() }</h3>
    </div>
    <div class="info-body">
        <% if (canCreateTransfer && outboundFacilityConfigured && patientInsuranceAvailable) { %>
            <div class="transfer-new-btn-wrap">
                <div class="transfer-new-menu-wrap" style="position: relative; display: inline-block;">
                    <a id="open-new-transfer-menu"
                       class="transfer-new-btn"
                       href="javascript:void(0);"
                       role="button"
                       title="${ ui.encodeHtmlAttribute(ui.message('transferapp.patient.transfers.newTransferOut')) }">
                        ${ ui.message("transferapp.patient.transfers.newTransferOut") }
                    </a>
                    <div id="new-transfer-type-menu" class="transfer-new-type-menu" style="display:none; position:absolute; top:100%; left:0; z-index:50; min-width:220px; background:#fff; border:1px solid #d8e0ea; border-radius:6px; box-shadow:0 4px 12px rgba(15,23,42,0.15); margin-top:4px;">
                        <a id="open-new-transfer-out"
                           class="transfer-new-type-menu-item"
                           href="javascript:void(0);"
                           role="button"
                           style="display:block; padding:0.6rem 0.9rem; text-decoration:none; color:#1e293b;"
                           data-load-url="${ ui.encodeHtmlAttribute(ui.pageLink('transferapp', 'patient/newTransferOutForm') + '?patientId=' + patient.patient.patientId) }">
                            External Transfer
                        </a>
                        <a id="open-new-transfer-out-maternity"
                           class="transfer-new-type-menu-item"
                           href="javascript:void(0);"
                           role="button"
                           style="display:block; padding:0.6rem 0.9rem; text-decoration:none; color:#1e293b; border-top:1px solid #eef2f6;"
                           data-load-url="${ ui.encodeHtmlAttribute(ui.pageLink('transferapp', 'patient/newMaternityTransferOutForm') + '?patientId=' + patient.patient.patientId) }">
                            Maternity Transfer
                        </a>
                        <a id="open-new-transfer-out-neonatal"
                           class="transfer-new-type-menu-item"
                           href="javascript:void(0);"
                           role="button"
                           style="display:block; padding:0.6rem 0.9rem; text-decoration:none; color:#1e293b; border-top:1px solid #eef2f6;"
                           data-load-url="${ ui.encodeHtmlAttribute(ui.pageLink('transferapp', 'patient/newNeonatalTransferOutForm') + '?patientId=' + patient.patient.patientId) }">
                            Neonatal Transfer
                        </a>
                    </div>
                </div>
                <span class="transfer-insurance-info">
                    <strong>${ ui.message("transferapp.patient.transfers.insurance") }:</strong>
                    ${ ui.encodeHtmlContent(patientInsuranceType) }
                    <span class="transfer-insurance-separator">|</span>
                    ${ ui.encodeHtmlContent(patientInsuranceNumber) }
                </span>
            </div>
        <% } else if (canCreateTransfer && !outboundFacilityConfigured) { %>
            <p class="transfer-insurance-missing">${ ui.message("transferapp.patient.transfers.outboundFacilityRequired") }</p>
        <% } else if (canCreateTransfer) { %>
            <p class="transfer-insurance-missing">${ ui.message("transferapp.patient.transfers.insuranceRequired") }</p>
        <% } %>

        <% if (hasTransfers) { %>
            <div class="transfer-table-wrapper">
                <table id="patient-transfers-table" class="transfer-datatable">
                    <thead>
                        <tr>
                            <th>${ ui.message("transferapp.patient.transfers.column.date") }</th>
                            <th>${ ui.message("transferapp.patient.transfers.column.to") }</th>
                            <th>${ ui.message("transferapp.patient.transfers.column.service") }</th>
                            <th>${ ui.message("transferapp.patient.transfers.column.status") }</th>
                            <th>${ ui.message("transferapp.patient.transfers.column.action") }</th>
                        </tr>
                    </thead>
                    <tbody>
                        <% transfers.each { transfer -> %>
                            <tr class="transfer-row${ transfer.hieSent ? ' transfer-row-sent' : '' }" data-transfer-id="${ ui.encodeHtmlAttribute(transfer.id) }" data-hie-sent="${ transfer.hieSent ? 'true' : 'false' }" data-form-type="${ ui.encodeHtmlAttribute(transfer.formType) }">
                                <td>${ ui.format(transfer.transferDate) }</td>
                                <td>${ ui.format(transfer.toFacility) }</td>
                                <td>${ ui.format(transfer.service) }</td>
                                <td>
                                    <% if (transfer.hieSent) { %>
                                        <span class="transfer-status-sent">${ ui.message("transferapp.patient.transfers.statusSent") }</span>
                                    <% } else { %>
                                        <span class="transfer-status-pending">${ ui.message("transferapp.patient.transfers.statusPending") }</span>
                                    <% } %>
                                </td>
                                <td class="transfer-action-cell">
                                    <a class="transfer-view-link transfer-action-icon"
                                       href="javascript:void(0);"
                                       data-transfer-id="${ ui.encodeHtmlAttribute(transfer.id) }"
                                       data-form-type="${ ui.encodeHtmlAttribute(transfer.formType) }"
                                       title="${ ui.encodeHtmlAttribute(ui.message('transferapp.patient.transfers.view')) }"
                                       aria-label="${ ui.encodeHtmlAttribute(ui.message('transferapp.patient.transfers.view')) }">
                                        <i class="icon-eye-open"></i>
                                    </a>
                                    <% if (canCreateTransfer && outboundFacilityConfigured) {
                                        def editFormPage = transfer.formType == "Neonatal" ? "patient/newNeonatalTransferOutForm"
                                            : transfer.formType == "Maternity" ? "patient/newMaternityTransferOutForm"
                                            : "patient/newTransferOutForm"
                                    %>
                                    <a class="transfer-edit-link transfer-action-icon"
                                       href="javascript:void(0);"
                                       data-transfer-id="${ ui.encodeHtmlAttribute(transfer.id) }"
                                       data-form-type="${ ui.encodeHtmlAttribute(transfer.formType ?: 'External') }"
                                       data-load-url="${ ui.encodeHtmlAttribute(ui.pageLink('transferapp', editFormPage) + '?patientId=' + patient.patient.patientId + '&transferUuid=' + transfer.id) }"
                                       title="${ ui.encodeHtmlAttribute(ui.message('transferapp.patient.transfers.edit')) }"
                                       aria-label="${ ui.encodeHtmlAttribute(ui.message('transferapp.patient.transfers.edit')) }">
                                        <i class="icon-pencil"></i>
                                    </a>
                                    <% } %>
                                </td>
                            </tr>
                        <% } %>
                    </tbody>
                </table>
                <% if (hasMorePatientTransfers) { %>
                <div class="transfer-records-link-wrap">
                    <a href="${ ui.encodeHtmlAttribute(recordsPageUrl) }">
                        ${ ui.message("transferapp.records.viewAllForPatient", totalPatientTransfers) }
                    </a>
                </div>
                <% } %>
            </div>
        <% } %>
        <% if (canListTransfers && (hasRecordedHieTransfer || (historyPageUrl != null && historyPageUrl.trim().length() > 0))) {
            def recordedCtxPath = (ui.contextPath() ?: "").toString()
            while (recordedCtxPath.startsWith("/")) {
                recordedCtxPath = recordedCtxPath.substring(1)
            }
            def recordedHieRestUrl = "/" + recordedCtxPath + "/ws/rest/v1/transferapp/transfer"
            def recordedFeedbackRestUrl = "/" + recordedCtxPath + "/ws/rest/v1/transferapp/transfer/feedback"
        %>
            <% if (hasRecordedHieTransfer) { %>
            <div id="transfer-recorded-hie-config" style="display:none;"
                 data-patient-id="${ ui.encodeHtmlAttribute((patientId ?: '') as String) }"
                 data-upid="${ ui.encodeHtmlAttribute(recordedHieTransferUpid) }"
                 data-transfer-id="${ ui.encodeHtmlAttribute(recordedHieTransferId) }"
                 data-rest-url="${ ui.encodeHtmlAttribute(recordedHieRestUrl) }"
                 data-feedback-url="${ ui.encodeHtmlAttribute(recordedFeedbackRestUrl) }"
                 data-facilities-url="${ ui.encodeHtmlAttribute(recordedFeedbackRestUrl + '/facilities') }"
                 data-has-transfer-id="true"
                 data-can-provide-feedback="${ canProvideFeedback ? 'true' : 'false' }"></div>
            <% } %>
            <div class="transfer-open-recorded-wrap">
                <% if (hasRecordedHieTransfer) { %>
                <a id="patient-open-recorded-transfer"
                   class="transfer-open-recorded-link hie-transfer-view-link"
                   href="javascript:void(0);"
                   role="button"
                   data-transfer-id="${ ui.encodeHtmlAttribute(recordedHieTransferId) }"
                   data-upid="${ ui.encodeHtmlAttribute(recordedHieTransferUpid) }"
                   data-patient-id="${ ui.encodeHtmlAttribute((patientId ?: '') as String) }"
                   data-rest-url="${ ui.encodeHtmlAttribute(recordedHieRestUrl) }"
                   data-feedback-url="${ ui.encodeHtmlAttribute(recordedFeedbackRestUrl) }"
                   data-facilities-url="${ ui.encodeHtmlAttribute(recordedFeedbackRestUrl + '/facilities') }"
                   data-can-provide-feedback="${ canProvideFeedback ? 'true' : 'false' }"
                   title="${ ui.encodeHtmlAttribute(ui.message('transferapp.patient.hieTransfer.openTransfer')) }">
                    <i class="icon-eye-open"></i> ${ ui.message("transferapp.patient.hieTransfer.openTransfer") }
                </a>
                <% } %>
                <% if (historyPageUrl != null && historyPageUrl.trim().length() > 0) { %>
                <a id="patient-transfer-history-link"
                   class="transfer-open-recorded-link transfer-history-link"
                   href="${ ui.encodeHtmlAttribute(historyPageUrl) }"
                   title="${ ui.encodeHtmlAttribute(ui.message('transferapp.patient.transfers.history')) }">
                    <i class="icon-time"></i> ${ ui.message("transferapp.patient.transfers.history") }
                </a>
                <% } %>
            </div>
            <% if (hasRecordedHieTransfer) { %>
            ${ ui.includeFragment("transferapp", "patient/hieTransferPreviewDialog") }
            <% } %>
        <% } %>
    </div>
</div>

<% if (canCreateTransfer) { %>
<script type="text/javascript">
    var newTransferOutDialog = null;
    var transferWizardOpenmrsPath = (typeof openmrsContextPath !== "undefined" ? openmrsContextPath : "/${ ui.encodeJavaScript(contextPath) }");
    var transferWizardResourcesBase = transferWizardOpenmrsPath + "/moduleResources/transferapp/";
    var transferWizardScriptsLoading = null;

    function ensureTransferWizardAssets(callback) {
        if (!document.getElementById("transfer-wizard-flatpickr-css")) {
            jq("head").append(
                "<link id='transfer-wizard-flatpickr-css' rel='stylesheet' type='text/css' href='"
                + transferWizardResourcesBase + "styles/flatpickr.min.css' />"
            );
        }
        if (!document.getElementById("transfer-wizard-select2-css")) {
            jq("head").append(
                "<link id='transfer-wizard-select2-css' rel='stylesheet' type='text/css' href='"
                + transferWizardResourcesBase + "styles/select2.min.css' />"
            );
        }
        if (!document.getElementById("transfer-wizard-css")) {
            jq("head").append(
                "<link id='transfer-wizard-css' rel='stylesheet' type='text/css' href='"
                + transferWizardResourcesBase + "styles/transferWizard.css' />"
            );
        }

        var wizardReady = (typeof flatpickr === "function")
            && (typeof jq.fn.select2 === "function" || (typeof jQuery !== "undefined" && typeof jQuery.fn.select2 === "function"))
            && typeof initTransferWizardModal === "function";
        if (wizardReady) {
            callback();
            return;
        }
        if (transferWizardScriptsLoading) {
            transferWizardScriptsLoading.done(callback);
            return;
        }

        transferWizardScriptsLoading = jq.getScript(transferWizardResourcesBase + "scripts/flatpickr/flatpickr.min.js")
            .then(function() {
                return jq.getScript(transferWizardResourcesBase + "scripts/select2/select2.min.js");
            })
            .then(function() {
                if (typeof jq !== "undefined" && typeof jQuery !== "undefined"
                    && typeof jq.fn.select2 !== "function" && typeof jQuery.fn.select2 === "function") {
                    jq.fn.select2 = jQuery.fn.select2;
                }
                return jq.getScript(transferWizardResourcesBase + "scripts/transferWizardModal.js");
            })
            .done(function() {
                if (typeof initTransferWizardModal === "function") {
                    callback();
                } else {
                    jq("#new-transfer-out-data").html("<p style='color:red;'>${ ui.encodeJavaScript(ui.message('transferapp.patient.transfers.wizard.loadError')) }</p>");
                }
            })
            .fail(function() {
                jq("#new-transfer-out-data").html("<p style='color:red;'>${ ui.encodeJavaScript(ui.message('transferapp.patient.transfers.wizard.loadError')) }</p>");
            });
    }

    function closeNewTransferOutDialog() {
        try {
            if (newTransferOutDialog && typeof newTransferOutDialog.close === "function") {
                newTransferOutDialog.close();
            }
        } catch (ignoreClose) {}
        jq("#new-transfer-out-dialog").removeClass("transfer-wizard-open").hide();
    }

    function showNewTransferOutDialog(loadUrl) {
        if (newTransferOutDialog == null && typeof emr !== "undefined" && typeof emr.setupConfirmationDialog === "function") {
            newTransferOutDialog = emr.setupConfirmationDialog({
                selector: "#new-transfer-out-dialog",
                actions: {
                    confirm: function() {},
                    cancel: function() { closeNewTransferOutDialog(); }
                }
            });
            newTransferOutDialog.close();
        }

        jq("#new-transfer-out-data").html("<div style='padding: 10px;'><i class='icon-spinner icon-spin'></i> ${ ui.encodeJavaScript(ui.message('transferapp.patient.transfers.wizard.loading')) }</div>");

        if (!loadUrl) {
            jq("#new-transfer-out-data").html("<p style='color:red;'>${ ui.encodeJavaScript(ui.message('transferapp.patient.transfers.wizard.loadError')) }</p>");
        } else {
            var fullUrl = loadUrl;
            if (fullUrl.indexOf("http") !== 0) {
                fullUrl = window.location.origin + fullUrl;
            }
            jq.ajax({
                url: fullUrl,
                type: "GET",
                dataType: "html",
                timeout: 30000
            }).done(function(html) {
                jq("#new-transfer-out-data").html(html);
                ensureTransferWizardAssets(function() {
                    if (typeof initTransferWizardModal === "function") {
                        initTransferWizardModal();
                    }
                    jq("#new-transfer-out-dialog .dialog-content").scrollTop(0);
                });
            }).fail(function() {
                jq("#new-transfer-out-data").html("<p style='color:red;'>${ ui.encodeJavaScript(ui.message('transferapp.patient.transfers.wizard.loadError')) }</p>");
            });
        }

        jq("#new-transfer-out-dialog").addClass("transfer-wizard-open").show().css("display", "flex");
        if (newTransferOutDialog && typeof newTransferOutDialog.show === "function") {
            newTransferOutDialog.show();
            jq("#new-transfer-out-dialog").addClass("transfer-wizard-open").css("display", "flex");
        }
    }

    var transferSaveUrl = (typeof openmrsContextPath !== "undefined" ? openmrsContextPath : "/${ ui.encodeJavaScript(contextPath) }")
        + "/module/transferapp/transfer/save.form";

    var newMaternityTransferOutDialog = null;

    function ensureMaternityTransferWizardAssets(callback) {
        if (!document.getElementById("transfer-wizard-flatpickr-css")) {
            jq("head").append(
                "<link id='transfer-wizard-flatpickr-css' rel='stylesheet' type='text/css' href='"
                + transferWizardResourcesBase + "styles/flatpickr.min.css' />"
            );
        }
        if (!document.getElementById("transfer-wizard-select2-css")) {
            jq("head").append(
                "<link id='transfer-wizard-select2-css' rel='stylesheet' type='text/css' href='"
                + transferWizardResourcesBase + "styles/select2.min.css' />"
            );
        }
        if (!document.getElementById("transfer-wizard-css")) {
            jq("head").append(
                "<link id='transfer-wizard-css' rel='stylesheet' type='text/css' href='"
                + transferWizardResourcesBase + "styles/transferWizard.css' />"
            );
        }

        var wizardReady = (typeof flatpickr === "function")
            && (typeof jq.fn.select2 === "function" || (typeof jQuery !== "undefined" && typeof jQuery.fn.select2 === "function"))
            && typeof initMaternityTransferWizardModal === "function";
        if (wizardReady) {
            callback();
            return;
        }
        if (transferWizardScriptsLoading) {
            transferWizardScriptsLoading.done(function() {
                if (typeof initMaternityTransferWizardModal === "function") {
                    callback();
                    return;
                }
                jq.getScript(transferWizardResourcesBase + "scripts/maternityTransferWizardModal.js").done(callback);
            });
            return;
        }

        transferWizardScriptsLoading = jq.getScript(transferWizardResourcesBase + "scripts/flatpickr/flatpickr.min.js")
            .then(function() {
                return jq.getScript(transferWizardResourcesBase + "scripts/select2/select2.min.js");
            })
            .then(function() {
                if (typeof jq !== "undefined" && typeof jQuery !== "undefined"
                    && typeof jq.fn.select2 !== "function" && typeof jQuery.fn.select2 === "function") {
                    jq.fn.select2 = jQuery.fn.select2;
                }
                return jq.getScript(transferWizardResourcesBase + "scripts/maternityTransferWizardModal.js");
            })
            .done(function() {
                if (typeof initMaternityTransferWizardModal === "function") {
                    callback();
                } else {
                    jq("#new-maternity-transfer-out-data").html("<p style='color:red;'>${ ui.encodeJavaScript(ui.message('transferapp.patient.transfers.wizard.loadError')) }</p>");
                }
            })
            .fail(function() {
                jq("#new-maternity-transfer-out-data").html("<p style='color:red;'>${ ui.encodeJavaScript(ui.message('transferapp.patient.transfers.wizard.loadError')) }</p>");
            });
    }

    window.closeNewMaternityTransferOutDialog = function closeNewMaternityTransferOutDialog() {
        try {
            if (newMaternityTransferOutDialog && typeof newMaternityTransferOutDialog.close === "function") {
                newMaternityTransferOutDialog.close();
            }
        } catch (ignoreClose) {}
        jq("#new-maternity-transfer-out-dialog").removeClass("transfer-wizard-open").hide();
    };

    function showNewMaternityTransferOutDialog(loadUrl) {
        if (newMaternityTransferOutDialog == null && typeof emr !== "undefined" && typeof emr.setupConfirmationDialog === "function") {
            newMaternityTransferOutDialog = emr.setupConfirmationDialog({
                selector: "#new-maternity-transfer-out-dialog",
                actions: {
                    confirm: function() {},
                    cancel: function() { window.closeNewMaternityTransferOutDialog(); }
                }
            });
            newMaternityTransferOutDialog.close();
        }

        jq("#new-maternity-transfer-out-data").html("<div style='padding: 10px;'><i class='icon-spinner icon-spin'></i> ${ ui.encodeJavaScript(ui.message('transferapp.patient.transfers.wizard.loading')) }</div>");

        if (loadUrl) {
            var fullUrl = loadUrl;
            if (fullUrl.indexOf("http") !== 0) {
                fullUrl = window.location.origin + fullUrl;
            }
            jq("#new-maternity-transfer-out-data").load(fullUrl, function(response, status) {
                if (status === "error") {
                    jq("#new-maternity-transfer-out-data").html("<p style='color:red;'>${ ui.encodeJavaScript(ui.message('transferapp.patient.transfers.wizard.loadError')) }</p>");
                } else {
                    ensureMaternityTransferWizardAssets(function() {
                        initMaternityTransferWizardModal();
                        jq("#new-maternity-transfer-out-dialog .dialog-content").scrollTop(0);
                    });
                }
            });
        }

        jq("#new-maternity-transfer-out-dialog").addClass("transfer-wizard-open").show().css("display", "flex");
        if (newMaternityTransferOutDialog && typeof newMaternityTransferOutDialog.show === "function") {
            newMaternityTransferOutDialog.show();
            jq("#new-maternity-transfer-out-dialog").addClass("transfer-wizard-open").css("display", "flex");
        }
    }

    var newNeonatalTransferOutDialog = null;

    function ensureNeonatalTransferWizardAssets(callback) {
        if (!document.getElementById("transfer-wizard-flatpickr-css")) {
            jq("head").append(
                "<link id='transfer-wizard-flatpickr-css' rel='stylesheet' type='text/css' href='"
                + transferWizardResourcesBase + "styles/flatpickr.min.css' />"
            );
        }
        if (!document.getElementById("transfer-wizard-select2-css")) {
            jq("head").append(
                "<link id='transfer-wizard-select2-css' rel='stylesheet' type='text/css' href='"
                + transferWizardResourcesBase + "styles/select2.min.css' />"
            );
        }
        if (!document.getElementById("transfer-wizard-css")) {
            jq("head").append(
                "<link id='transfer-wizard-css' rel='stylesheet' type='text/css' href='"
                + transferWizardResourcesBase + "styles/transferWizard.css' />"
            );
        }

        var wizardReady = (typeof flatpickr === "function")
            && (typeof jq.fn.select2 === "function" || (typeof jQuery !== "undefined" && typeof jQuery.fn.select2 === "function"))
            && typeof initNeonatalTransferWizardModal === "function";
        if (wizardReady) {
            callback();
            return;
        }
        if (transferWizardScriptsLoading) {
            transferWizardScriptsLoading.done(function() {
                if (typeof initNeonatalTransferWizardModal === "function") {
                    callback();
                    return;
                }
                jq.getScript(transferWizardResourcesBase + "scripts/neonatalTransferWizardModal.js").done(callback);
            });
            return;
        }

        transferWizardScriptsLoading = jq.getScript(transferWizardResourcesBase + "scripts/flatpickr/flatpickr.min.js")
            .then(function() {
                return jq.getScript(transferWizardResourcesBase + "scripts/select2/select2.min.js");
            })
            .then(function() {
                if (typeof jq !== "undefined" && typeof jQuery !== "undefined"
                    && typeof jq.fn.select2 !== "function" && typeof jQuery.fn.select2 === "function") {
                    jq.fn.select2 = jQuery.fn.select2;
                }
                return jq.getScript(transferWizardResourcesBase + "scripts/neonatalTransferWizardModal.js");
            })
            .done(function() {
                if (typeof initNeonatalTransferWizardModal === "function") {
                    callback();
                } else {
                    jq("#new-neonatal-transfer-out-data").html("<p style='color:red;'>${ ui.encodeJavaScript(ui.message('transferapp.patient.transfers.wizard.loadError')) }</p>");
                }
            })
            .fail(function() {
                jq("#new-neonatal-transfer-out-data").html("<p style='color:red;'>${ ui.encodeJavaScript(ui.message('transferapp.patient.transfers.wizard.loadError')) }</p>");
            });
    }

    window.closeNewNeonatalTransferOutDialog = function closeNewNeonatalTransferOutDialog() {
        try {
            if (newNeonatalTransferOutDialog && typeof newNeonatalTransferOutDialog.close === "function") {
                newNeonatalTransferOutDialog.close();
            }
        } catch (ignoreClose) {}
        jq("#new-neonatal-transfer-out-dialog").removeClass("transfer-wizard-open").hide();
    };

    function showNewNeonatalTransferOutDialog(loadUrl) {
        if (newNeonatalTransferOutDialog == null && typeof emr !== "undefined" && typeof emr.setupConfirmationDialog === "function") {
            newNeonatalTransferOutDialog = emr.setupConfirmationDialog({
                selector: "#new-neonatal-transfer-out-dialog",
                actions: {
                    confirm: function() {},
                    cancel: function() { window.closeNewNeonatalTransferOutDialog(); }
                }
            });
            newNeonatalTransferOutDialog.close();
        }

        jq("#new-neonatal-transfer-out-data").html("<div style='padding: 10px;'><i class='icon-spinner icon-spin'></i> ${ ui.encodeJavaScript(ui.message('transferapp.patient.transfers.wizard.loading')) }</div>");

        if (loadUrl) {
            var fullUrl = loadUrl;
            if (fullUrl.indexOf("http") !== 0) {
                fullUrl = window.location.origin + fullUrl;
            }
            jq("#new-neonatal-transfer-out-data").load(fullUrl, function(response, status) {
                if (status === "error") {
                    jq("#new-neonatal-transfer-out-data").html("<p style='color:red;'>${ ui.encodeJavaScript(ui.message('transferapp.patient.transfers.wizard.loadError')) }</p>");
                } else {
                    ensureNeonatalTransferWizardAssets(function() {
                        initNeonatalTransferWizardModal();
                        jq("#new-neonatal-transfer-out-dialog .dialog-content").scrollTop(0);
                    });
                }
            });
        }

        jq("#new-neonatal-transfer-out-dialog").addClass("transfer-wizard-open").show().css("display", "flex");
        if (newNeonatalTransferOutDialog && typeof newNeonatalTransferOutDialog.show === "function") {
            newNeonatalTransferOutDialog.show();
            jq("#new-neonatal-transfer-out-dialog").addClass("transfer-wizard-open").css("display", "flex");
        }
    }

    jq(document).ready(function() {
        jq(document).on("click", "#open-new-transfer-menu", function(e) {
            e.preventDefault();
            e.stopPropagation();
            jq("#new-transfer-type-menu").toggle();
        });

        jq(document).on("click", function(e) {
            if (!jq(e.target).closest("#new-transfer-type-menu, #open-new-transfer-menu").length) {
                jq("#new-transfer-type-menu").hide();
            }
        });

        function closeProfileIncompleteDialog() {
            jq("#transfer-profile-incomplete-dialog").removeClass("is-open").hide();
            jq("#transfer-profile-incomplete-overlay").hide();
        }

        function showProfileIncompleteDialog(message) {
            var dialog = jq("#transfer-profile-incomplete-dialog");
            if (!dialog.length) {
                window.alert(message);
                return;
            }
            jq("#transfer-profile-incomplete-message").text(message || "");
            jq("#transfer-profile-incomplete-overlay").show();
            dialog.addClass("is-open").show().css("display", "flex");
        }

        jq(document).on("click", "#transfer-profile-incomplete-close, #transfer-profile-incomplete-overlay", function(e) {
            e.preventDefault();
            closeProfileIncompleteDialog();
        });

        jq(document).on("click", "#transfer-profile-incomplete-goto", function(e) {
            e.preventDefault();
            var profileUrl = jq("#transfer-profile-incomplete-dialog").attr("data-profile-url");
            if (profileUrl) {
                window.location.href = profileUrl;
                return;
            }
            closeProfileIncompleteDialog();
        });

        jq(document).on("click", "#open-new-transfer-out", function(e) {
            e.preventDefault();
            jq("#new-transfer-type-menu").hide();
            var gateEl = jq("#new-transfer-out-dialog");
            if (gateEl.attr("data-provider-profile-complete") === "false") {
                var incompleteMessage = gateEl.attr("data-profile-incomplete-message")
                    || "${ ui.encodeJavaScript(ui.message('transferapp.patient.transfers.profileIncomplete')) }";
                showProfileIncompleteDialog(incompleteMessage);
                return;
            }
            var loadUrl = jq(this).attr("data-load-url");
            showNewTransferOutDialog(loadUrl);
        });

        jq(document).on("click", ".transfer-edit-link", function(e) {
            e.preventDefault();
            var editLink = jq(this);
            var formType = editLink.attr("data-form-type") || "External";
            var loadUrl = editLink.attr("data-load-url");

            if (formType === "Neonatal") {
                showNewNeonatalTransferOutDialog(loadUrl);
                return;
            }
            if (formType === "Maternity") {
                showNewMaternityTransferOutDialog(loadUrl);
                return;
            }

            var gateEl = jq("#new-transfer-out-dialog");
            if (gateEl.attr("data-provider-profile-complete") === "false") {
                var incompleteMessage = gateEl.attr("data-profile-incomplete-message")
                    || "${ ui.encodeJavaScript(ui.message('transferapp.patient.transfers.profileIncomplete')) }";
                showProfileIncompleteDialog(incompleteMessage);
                return;
            }
            showNewTransferOutDialog(loadUrl);
        });

        jq(document).on("click", "#open-new-transfer-out-maternity", function(e) {
            e.preventDefault();
            jq("#new-transfer-type-menu").hide();
            var loadUrl = jq(this).attr("data-load-url");
            showNewMaternityTransferOutDialog(loadUrl);
        });

        jq(document).on("click", "#open-new-transfer-out-neonatal", function(e) {
            e.preventDefault();
            jq("#new-transfer-type-menu").hide();
            var loadUrl = jq(this).attr("data-load-url");
            showNewNeonatalTransferOutDialog(loadUrl);
        });

        jq(document).on("click", "#transfer-wizard-cancel", function(e) {
            e.preventDefault();
            closeNewTransferOutDialog();
        });

        jq(document).on("click", "#maternity-wizard-cancel", function(e) {
            e.preventDefault();
            window.closeNewMaternityTransferOutDialog();
        });

        jq(document).on("click", "#neonatal-wizard-cancel", function(e) {
            e.preventDefault();
            window.closeNewNeonatalTransferOutDialog();
        });

        jq(document).on("click", "#transfer-wizard-submit-btn", function(e) {
            e.preventDefault();
            var wizardForm = jq("#moh-transfer-wizard-form");
            if (wizardForm.length && !wizardForm[0].checkValidity()) {
                wizardForm[0].reportValidity();
                return;
            }

            var submitBtn = jq(this);
            submitBtn.prop("disabled", true);
            var isEditing = wizardForm.attr("data-editing") === "true";
            var successMessage = isEditing
                ? "${ ui.encodeJavaScript(ui.message('transferapp.patient.transfers.updateSuccess')) }"
                : "${ ui.encodeJavaScript(ui.message('transferapp.patient.transfers.submitSuccess')) }";

            jq.ajax({
                url: transferSaveUrl,
                type: "POST",
                data: wizardForm.serialize(),
                dataType: "json"
            }).done(function(response) {
                if (response && response.status === "success") {
                    if (typeof emr !== "undefined" && typeof emr.successMessage === "function") {
                        emr.successMessage(successMessage);
                    }
                    closeNewTransferOutDialog();
                    if (response.uuid) {
                        try {
                            sessionStorage.setItem("transferapp.previewTransferUuid", response.uuid);
                            sessionStorage.setItem("transferapp.previewFormType", "External");
                        } catch (ignoreStorage) {}
                    }
                    window.location.reload();
                    return;
                } else {
                    var message = response && response.message
                        ? response.message
                        : "${ ui.encodeJavaScript(ui.message('transferapp.patient.transfers.submitError')) }";
                    if (typeof emr !== "undefined" && typeof emr.errorMessage === "function") {
                        emr.errorMessage(message);
                    }
                }
            }).fail(function() {
                if (typeof emr !== "undefined" && typeof emr.errorMessage === "function") {
                    emr.errorMessage("${ ui.encodeJavaScript(ui.message('transferapp.patient.transfers.submitError')) }");
                }
            }).always(function() {
                submitBtn.prop("disabled", false);
            });
        });
    });
</script>
<% } %>

<% if (canListTransfers) { %>
<script type="text/javascript">
    var transferOpenmrsPath = (typeof openmrsContextPath !== "undefined" ? openmrsContextPath : "/${ ui.encodeJavaScript(contextPath) }");
    window.transferOpenmrsPath = transferOpenmrsPath;
    var transferPreviewUrl = transferOpenmrsPath + "/module/transferapp/transfer/preview.form";
    var transferSubmitUrl = transferOpenmrsPath + "/module/transferapp/transfer/submit.form";
    var transferSubmitNeonatalUrl = transferOpenmrsPath + "/module/transferapp/transfer/submitNeonatal.form";
    var transferSubmitMaternityUrl = transferOpenmrsPath + "/module/transferapp/transfer/submitMaternity.form";
    var transferPreviewResourcesBase = transferOpenmrsPath + "/moduleResources/transferapp/scripts/";
    var transferPreviewStorageKey = "transferapp.previewTransferUuid";
    var transferPreviewFormTypeStorageKey = "transferapp.previewFormType";
    var transferPreviewDialog = null;
    var transferPreviewScriptsLoading = null;
    var currentPreviewTransferUuid = null;
    var currentPreviewTransferSent = false;
    var currentPreviewIsHieUpdate = false;
    var currentPreviewFormType = "External";

    function syncTransferPreviewSubmitButton() {
        var submitBtn = jq("#transfer-preview-submit");
        if (!submitBtn.length) {
            return;
        }
        submitBtn.show();
        if (currentPreviewTransferSent) {
            submitBtn.prop("disabled", true).text("${ ui.encodeJavaScript(ui.message('transferapp.patient.transfers.alreadySent')) }");
        } else if (currentPreviewIsHieUpdate) {
            submitBtn.prop("disabled", false).text("${ ui.encodeJavaScript(ui.message('transferapp.patient.transfers.updateOnHie')) }");
        } else {
            submitBtn.prop("disabled", false).text("${ ui.encodeJavaScript(ui.message('transferapp.patient.transfers.submitToHie')) }");
        }
    }

    function markTransferRowHieStatus(transferUuid, sentToHie) {
        if (!transferUuid) {
            return;
        }
        var row = jq("tr.transfer-row[data-transfer-id='" + transferUuid + "']");
        if (!row.length) {
            return;
        }
        row.attr("data-hie-sent", sentToHie ? "true" : "false");
        if (sentToHie) {
            row.addClass("transfer-row-sent");
            row.find("td").eq(3).html("<span class='transfer-status-sent'>${ ui.encodeJavaScript(ui.message('transferapp.patient.transfers.statusSent')) }</span>");
        } else {
            row.removeClass("transfer-row-sent");
            row.find("td").eq(3).html("<span class='transfer-status-pending'>${ ui.encodeJavaScript(ui.message('transferapp.patient.transfers.statusPending')) }</span>");
        }
    }

    function ensureTransferPreviewRenderer(callback) {
        if (typeof buildTransferFormPreviewHtml === "function") {
            callback();
            return;
        }
        if (!document.getElementById("transfer-form-preview-css")) {
            jq("head").append(
                "<link id='transfer-form-preview-css' rel='stylesheet' type='text/css' href='"
                + transferOpenmrsPath + "/moduleResources/transferapp/styles/transferFormPreview.css' />"
            );
        }
        if (transferPreviewScriptsLoading) {
            transferPreviewScriptsLoading.done(callback);
            return;
        }
        transferPreviewScriptsLoading = jq.getScript(transferPreviewResourcesBase + "transferMohLogo.js")
            .then(function() {
                return jq.getScript(transferPreviewResourcesBase + "transferFormPreview.js");
            })
            .done(function() {
                if (typeof buildTransferFormPreviewHtml === "function") {
                    callback();
                } else {
                    jq("#transfer-preview-body").html("<p style='color:red;'>Preview renderer failed to initialize.</p>");
                }
            })
            .fail(function() {
                jq("#transfer-preview-body").html("<p style='color:red;'>Unable to load preview scripts.</p>");
            });
    }

    function renderTransferPreview(transfer) {
        var formType = (transfer && transfer.formType) || "External";
        currentPreviewFormType = (formType === "Maternity" || formType === "Neonatal") ? formType : "External";
        var previewHtml;
        if (currentPreviewFormType === "Maternity" && typeof buildMaternityTransferFormPreviewHtml === "function") {
            previewHtml = buildMaternityTransferFormPreviewHtml(transfer);
        } else if (currentPreviewFormType === "Neonatal" && typeof buildNeonatalTransferFormPreviewHtml === "function") {
            previewHtml = buildNeonatalTransferFormPreviewHtml(transfer);
        } else if (currentPreviewFormType === "External" && typeof buildTransferFormPreviewHtml === "function") {
            previewHtml = buildTransferFormPreviewHtml(transfer);
        } else {
            previewHtml = "<p style='color:red;'>Preview renderer not loaded.</p>";
        }
        jq("#transfer-preview-body").html(previewHtml);
        currentPreviewTransferSent = !!(transfer && (transfer.hieSent === true || transfer.hieSent === "true"));
        // Only External supports resubmitting an update to an already-sent HIE encounter.
        currentPreviewIsHieUpdate = currentPreviewFormType === "External"
            && !currentPreviewTransferSent && !!(transfer && String(transfer.hieTransferId || "").trim());
        syncTransferPreviewSubmitButton();
    }

    function showTransferPreviewDialog() {
        if (transferPreviewDialog == null && typeof emr !== "undefined" && typeof emr.setupConfirmationDialog === "function") {
            transferPreviewDialog = emr.setupConfirmationDialog({
                selector: "#transfer-preview-dialog",
                actions: {
                    confirm: function() {},
                    cancel: function() { jq("#transfer-preview-dialog").hide(); }
                }
            });
            transferPreviewDialog.close();
        }
        if (transferPreviewDialog) {
            transferPreviewDialog.show();
        } else {
            jq("#transfer-preview-dialog").show();
        }
    }

    function showTransferPreview(transferUuid, formType) {
        if (!transferUuid) {
            return;
        }

        jq("#transfer-preview-body").html("<div style='padding: 10px;'><i class='icon-spinner icon-spin'></i> ${ ui.encodeJavaScript(ui.message('transferapp.patient.transfers.previewLoading')) }</div>");
        currentPreviewTransferUuid = transferUuid;
        currentPreviewTransferSent = false;
        currentPreviewFormType = (formType === "Maternity" || formType === "Neonatal") ? formType : "External";
        currentPreviewIsHieUpdate = false;
        syncTransferPreviewSubmitButton();
        showTransferPreviewDialog();

        jq.ajax({
            url: transferPreviewUrl,
            type: "GET",
            data: { uuid: transferUuid, formType: currentPreviewFormType },
            dataType: "json"
        }).done(function(response) {
            if (response && response.status === "success" && response.transfer) {
                ensureTransferPreviewRenderer(function() {
                    renderTransferPreview(response.transfer);
                });
                return;
            }
            var message = response && response.message
                ? response.message
                : "${ ui.encodeJavaScript(ui.message('transferapp.patient.transfers.previewError')) }";
            var esc = typeof escTransferPreview === "function" ? escTransferPreview : function(v) { return v || ""; };
            jq("#transfer-preview-body").html("<p style='color:red;'>" + esc(message) + "</p>");
        }).fail(function() {
            jq("#transfer-preview-body").html("<p style='color:red;'>${ ui.encodeJavaScript(ui.message('transferapp.patient.transfers.previewError')) }</p>");
        });
    }

    jq(document).ready(function() {
        jq(document).on("click", "#transfer-preview-submit", function(e) {
            e.preventDefault();
            if (!currentPreviewTransferUuid || currentPreviewTransferSent) {
                return;
            }
            var submitBtn = jq(this);
            submitBtn.prop("disabled", true);
            var submitUrl = currentPreviewFormType === "Neonatal" ? transferSubmitNeonatalUrl
                : currentPreviewFormType === "Maternity" ? transferSubmitMaternityUrl
                : transferSubmitUrl;
            jq.ajax({
                url: submitUrl,
                type: "POST",
                data: { uuid: currentPreviewTransferUuid },
                dataType: "json"
            }).done(function(response) {
                if (response && response.status === "success") {
                    var wasUpdate = currentPreviewIsHieUpdate;
                    currentPreviewTransferSent = true;
                    currentPreviewIsHieUpdate = false;
                    syncTransferPreviewSubmitButton();
                    markTransferRowHieStatus(currentPreviewTransferUuid, true);
                    if (typeof emr !== "undefined" && typeof emr.successMessage === "function") {
                        emr.successMessage(wasUpdate
                            ? "${ ui.encodeJavaScript(ui.message('transferapp.patient.transfers.updateOnHieSuccess')) }"
                            : "${ ui.encodeJavaScript(ui.message('transferapp.patient.transfers.submitToHieSuccess')) }");
                    }
                    window.location.reload();
                    return;
                }
                var message = response && response.message
                    ? response.message
                    : "${ ui.encodeJavaScript(ui.message('transferapp.patient.transfers.submitToHieError')) }";
                if (typeof emr !== "undefined" && typeof emr.errorMessage === "function") {
                    emr.errorMessage(message);
                }
                syncTransferPreviewSubmitButton();
            }).fail(function() {
                if (typeof emr !== "undefined" && typeof emr.errorMessage === "function") {
                    emr.errorMessage("${ ui.encodeJavaScript(ui.message('transferapp.patient.transfers.submitToHieError')) }");
                }
                syncTransferPreviewSubmitButton();
            });
        });

        jq(document).on("click", "#transfer-preview-close", function(e) {
            e.preventDefault();
            if (transferPreviewDialog && typeof transferPreviewDialog.close === "function") {
                transferPreviewDialog.close();
            }
            jq("#transfer-preview-dialog").hide();
        });

        jq(document).on("click", ".transfer-view-link", function(e) {
            e.preventDefault();
            var clickedLink = jq(this);
            var parentRow = clickedLink.closest("tr.transfer-row");
            var transferUuid = clickedLink.attr("data-transfer-id") || parentRow.attr("data-transfer-id");
            var formType = clickedLink.attr("data-form-type") || parentRow.attr("data-form-type") || "External";
            showTransferPreview(transferUuid, formType);
        });

        var pendingPreviewUuid = null;
        var pendingPreviewFormType = "External";
        try {
            pendingPreviewUuid = sessionStorage.getItem(transferPreviewStorageKey);
            pendingPreviewFormType = sessionStorage.getItem(transferPreviewFormTypeStorageKey) || "External";
        } catch (ignoreStorage) {}
        if (pendingPreviewUuid) {
            try {
                sessionStorage.removeItem(transferPreviewStorageKey);
                sessionStorage.removeItem(transferPreviewFormTypeStorageKey);
            } catch (ignoreStorage) {}
            showTransferPreview(pendingPreviewUuid, pendingPreviewFormType);
        }
    });
</script>
<% } %>
<% } %>
