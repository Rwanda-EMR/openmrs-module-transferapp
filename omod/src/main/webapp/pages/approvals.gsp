<%
    ui.decorateWith("appui", "standardEmrPage")
    ui.includeCss("transferapp", "dashboard.css")
    ui.includeCss("transferapp", "transferRecords.css")
    ui.includeCss("transferapp", "transferFormPreview.css")
    ui.includeCss("transferapp", "approvals.css")
    ui.includeJavascript("transferapp", "transferMohLogo.js")
    ui.includeJavascript("transferapp", "transferFormPreview.js")
    ui.includeJavascript("transferapp", "transferPreviewCommon.js")
    ui.includeJavascript("transferapp", "approvals.js")
%>
<script type="text/javascript">
    var breadcrumbs = [
        { icon: "icon-home", link: '/' + OPENMRS_CONTEXT_PATH + '/index.htm' },
        { label: "${ ui.message("transferapp.nav.approvals") }" }
    ];
</script>

<div class="transfer-records-page">
${ ui.includeFragment("transferapp", "transfer/transferNav", [ activeTab: "approvals", app: appId ]) }

<h3 class="transfer-records-title">${ ui.message("transferapp.approvals.title") }</h3>
<p class="transfer-records-filter">${ ui.message("transferapp.approvals.subtitle") }</p>

<% if (!canAccessApprovals) { %>
<div class="transfer-admin-empty" style="margin-top: 16px; padding: 12px; background: #fff8e6; border: 1px solid #f0d78c;">
    ${ ui.encodeHtmlContent(approvalsAccessDeniedMessage) }
</div>
<% } else { %>

<% if (approvalListError) { %>
<div class="transfer-admin-empty" style="margin-top: 12px; padding: 12px; background: #fdecea; border: 1px solid #f5c2c0; color: #a94442;">
    ${ ui.encodeHtmlContent(approvalListError) }
</div>
<% } %>

<div id="approvals-message" class="transfer-approvals-message" style="display:none;"></div>

<table class="transfer-records-table" id="approvals-table">
    <thead>
        <tr>
            <th>${ ui.message("transferapp.approvals.column.upid") }</th>
            <th>${ ui.message("transferapp.approvals.column.patient") }</th>
            <th>${ ui.message("transferapp.approvals.column.destination") }</th>
            <th>${ ui.message("transferapp.approvals.column.service") }</th>
            <th>${ ui.message("transferapp.approvals.column.insurance") }</th>
            <th>${ ui.message("transferapp.approvals.column.decision") }</th>
            <th>${ ui.message("transferapp.approvals.column.action") }</th>
        </tr>
    </thead>
    <tbody>
        <% if (approvalItems == null || approvalItems.isEmpty()) { %>
        <tr class="transfer-approvals-empty-row">
            <td colspan="7">${ ui.message("transferapp.approvals.empty") }</td>
        </tr>
        <% } else { approvalItems.each { item -> %>
        <tr data-uuid="${ ui.encodeHtmlAttribute(item.uuid) }">
            <td>${ ui.encodeHtmlContent(item.upid ?: '') }</td>
            <td>${ ui.encodeHtmlContent(item.patientName ?: '') }</td>
            <td>${ ui.encodeHtmlContent(item.receivingFacilityName ?: item.receivingFacilityCode ?: '') }</td>
            <td>${ ui.encodeHtmlContent(item.receivingService ?: '') }</td>
            <td>${ ui.encodeHtmlContent(item.healthInsuranceType ?: '') }</td>
            <td>${ item.decisionToTransferAt ? ui.format(item.decisionToTransferAt) : '' }</td>
            <td>
                <button type="button" class="btn btn-primary approvals-preview-btn"
                        data-uuid="${ ui.encodeHtmlAttribute(item.uuid) }"
                        data-upid="${ ui.encodeHtmlAttribute(item.upid ?: '') }">
                    ${ ui.message("transferapp.approvals.preview") }
                </button>
            </td>
        </tr>
        <% } } %>
    </tbody>
</table>

<% } %>
</div>

<div id="approvals-preview-overlay" class="approvals-overlay" style="display:none;"></div>
<div id="approvals-preview-dialog" class="dialog transfer-preview-dialog" style="display:none;">
    <div class="dialog-header">
        <h3>${ ui.message("transferapp.approvals.preview.title") }</h3>
        <i class="icon-remove approvals-preview-close" title="${ ui.encodeHtmlAttribute(ui.message('transferapp.approvals.close')) }"></i>
    </div>
    <div class="dialog-content">
        <div id="approvals-preview-body" class="transfer-form-preview-host"></div>
        <div class="approvals-preview-actions">
            <button type="button" id="approvals-approve-btn" class="confirm">
                ${ ui.message("transferapp.approvals.approve") }
            </button>
            <button type="button" id="approvals-reject-btn" class="cancel">
                ${ ui.message("transferapp.approvals.reject") }
            </button>
            <button type="button" id="approvals-preview-close" class="cancel approvals-preview-close">
                ${ ui.message("transferapp.approvals.close") }
            </button>
        </div>
        <div id="approvals-reject-panel" class="approvals-reject-panel" style="display:none;">
            <label for="approvals-reject-reason">${ ui.message("transferapp.approvals.rejectReason") }</label>
            <textarea id="approvals-reject-reason" rows="3" maxlength="1000"></textarea>
            <div class="approvals-reject-actions">
                <button type="button" id="approvals-reject-confirm-btn" class="confirm">
                    ${ ui.message("transferapp.approvals.rejectConfirm") }
                </button>
                <button type="button" id="approvals-reject-cancel-btn" class="cancel">
                    ${ ui.message("transferapp.approvals.cancel") }
                </button>
            </div>
        </div>
    </div>
</div>

<script type="text/javascript">
    window.approvalsConfig = {
        previewUrl: (typeof openmrsContextPath !== "undefined" ? openmrsContextPath : "")
            + "/module/transferapp/transfer/preview.form",
        approveUrl: (typeof openmrsContextPath !== "undefined" ? openmrsContextPath : "")
            + "/module/transferapp/approvals/approve.form",
        rejectUrl: (typeof openmrsContextPath !== "undefined" ? openmrsContextPath : "")
            + "/module/transferapp/approvals/reject.form",
        messages: {
            previewError: "${ ui.encodeJavaScript(ui.message('transferapp.approvals.previewError')) }",
            approveSuccess: "${ ui.encodeJavaScript(ui.message('transferapp.approvals.approveSuccess')) }",
            approveError: "${ ui.encodeJavaScript(ui.message('transferapp.approvals.approveError')) }",
            rejectSuccess: "${ ui.encodeJavaScript(ui.message('transferapp.approvals.rejectSuccess')) }",
            rejectError: "${ ui.encodeJavaScript(ui.message('transferapp.approvals.rejectError')) }",
            rejectReasonRequired: "${ ui.encodeJavaScript(ui.message('transferapp.approvals.rejectReasonRequired')) }",
            empty: "${ ui.encodeJavaScript(ui.message('transferapp.approvals.empty')) }",
            loading: "${ ui.encodeJavaScript(ui.message('transferapp.patient.transfers.previewLoading')) }"
        }
    };
</script>
