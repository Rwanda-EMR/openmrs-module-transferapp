<%
    ui.decorateWith("appui", "standardEmrPage")
    ui.includeCss("transferapp", "dashboard.css")
    ui.includeCss("transferapp", "transferSection.css")
    ui.includeCss("transferapp", "transferRecords.css")
    ui.includeCss("transferapp", "transferFormPreview.css")
    ui.includeCss("transferapp", "select2.min.css")
    ui.includeCss("uicommons", "datatables/dataTables_jui.css")
    ui.includeJavascript("uicommons", "datatables/jquery.dataTables.min.js")
    ui.includeJavascript("transferapp", "select2/select2.min.js")
    ui.includeJavascript("transferapp", "transferMohLogo.js")
    ui.includeJavascript("transferapp", "transferFormPreview.js")
    ui.includeJavascript("transferapp", "transferPreviewCommon.js")
    ui.includeJavascript("transferapp", "transferPending.js")
%>
<script type="text/javascript">
    var breadcrumbs = [
        { icon: "icon-home", link: '/' + OPENMRS_CONTEXT_PATH + '/index.htm' },
        { label: "${ ui.message("transferapp.pending.title") }" }
    ];
</script>

<div class="transfer-records-page">
${ ui.includeFragment("transferapp", "transfer/transferNav", [ activeTab: "pending", app: appId ]) }

<h3 class="transfer-records-title">${ ui.message("transferapp.pending.title") }</h3>
<% if (targetOrg) { %>
<p class="transfer-records-filter">
    ${ ui.message("transferapp.pending.targetOrg") }:
    <strong>${ ui.encodeHtmlContent(targetOrg) }</strong>
</p>
<% } %>

<% if (canListPending) { %>
<form id="transfer-pending-filter-form"
      class="transfer-records-filters transfer-pending-filters"
      method="get"
      action="${ ui.encodeHtmlAttribute(ui.pageLink('transferapp', 'pending')) }">
    <input type="hidden" name="app" value="${ ui.encodeHtmlAttribute(appId) }" />
    <div class="transfer-records-filter-field transfer-pending-period-filter-field">
        <label for="pending-week-filter">${ ui.message("transferapp.pending.filter.period") }</label>
        <select id="pending-week-filter" name="weeks" class="transfer-pending-period-select">
            <option value="1" <% if (selectedWeeks == 1) { %>selected="selected"<% } %>>${ ui.message("transferapp.pending.filter.period.week1") }</option>
            <option value="2" <% if (selectedWeeks == 2) { %>selected="selected"<% } %>>${ ui.message("transferapp.pending.filter.period.week2") }</option>
            <option value="3" <% if (selectedWeeks == 3) { %>selected="selected"<% } %>>${ ui.message("transferapp.pending.filter.period.week3") }</option>
            <option value="4" <% if (selectedWeeks == 4) { %>selected="selected"<% } %>>${ ui.message("transferapp.pending.filter.period.week4") }</option>
        </select>
    </div>
    <div id="pending-client-filters"
         class="transfer-pending-client-filters${ pendingErrorMessage || !hasPendingTransfers ? ' is-empty' : '' }">
    <% if (!pendingErrorMessage && hasPendingTransfers) { %>
    <div class="transfer-records-filter-field transfer-pending-date-filter-field">
        <label for="pending-date-filter">${ ui.message("transferapp.pending.filter.date") }</label>
        <select id="pending-date-filter" class="transfer-pending-date-select">
            <option value="">${ ui.message("transferapp.pending.filter.date.all") }</option>
            <% pendingDates.each { date -> %>
            <option value="${ ui.encodeHtmlAttribute(date) }">${ ui.encodeHtmlContent(date) }</option>
            <% } %>
        </select>
    </div>
    <div class="transfer-records-filter-field transfer-pending-service-filter-field">
        <label for="pending-service-filter">${ ui.message("transferapp.pending.filter.service") }</label>
        <select id="pending-service-filter"
                class="transfer-pending-service-select"
                multiple="multiple"
                data-placeholder="${ ui.encodeHtmlAttribute(ui.message('transferapp.pending.filter.service.all')) }">
            <% pendingServices.each { service -> %>
            <option value="${ ui.encodeHtmlAttribute(service) }">${ ui.encodeHtmlContent(service) }</option>
            <% } %>
        </select>
    </div>
    <% } %>
    </div>
    <span id="pending-week-loading" class="transfer-pending-period-loading" style="display: none;">
        <i class="icon-spinner icon-spin"></i>
        ${ ui.message("transferapp.pending.filter.period.loading") }
    </span>
</form>
<% } %>

<% if (canListPending) { %>
<div id="transfer-preview-dialog" class="dialog transfer-preview-dialog" style="display: none">
    <div class="dialog-header">
        <i class="icon-retweet"></i>
        <h3>${ ui.message("transferapp.patient.transfers.previewTitle") }</h3>
    </div>
    <div class="dialog-content">
        <div id="transfer-preview-body"></div>
        <div class="transfer-preview-actions">
            <button type="button" id="transfer-preview-close" class="cancel">
                ${ ui.message("coreapps.close") }
            </button>
        </div>
    </div>
</div>
<% } %>

<div id="transfer-pending-results" data-refresh-status="${ pendingErrorMessage ? 'error' : 'success' }">
<% if (!canListPending) { %>
<div class="transfer-records-empty">${ ui.encodeHtmlContent(pendingAccessDeniedMessage ?: ui.message("transferapp.pending.notAllowed")) }</div>
<% } else if (pendingErrorMessage) { %>
<div class="transfer-records-empty" style="color:#a94442;">${ ui.encodeHtmlContent(pendingErrorMessage) }</div>
<% } else if (!hasPendingTransfers) { %>
<div class="transfer-records-empty">${ ui.message("transferapp.pending.empty") }</div>
<% } else { %>

<div class="transfer-table-wrapper">
    <table id="transfer-pending-table" class="transfer-datatable display">
        <thead>
            <tr>
                <th>${ ui.message("transferapp.patient.transfers.column.date") }</th>
                <th>${ ui.message("transferapp.records.column.patient") }</th>
                <th>${ ui.message("transferapp.pending.column.upid") }</th>
                <th>${ ui.message("transferapp.patient.transfers.column.from") }</th>
                <th>${ ui.message("transferapp.patient.transfers.column.service") }</th>
                <th>${ ui.message("transferapp.patient.transfers.column.status") }</th>
                <th>${ ui.message("transferapp.patient.transfers.column.action") }</th>
            </tr>
        </thead>
        <tbody>
            <% pendingTransfers.each { transfer ->
                def uuid = (transfer.uuid ?: transfer.id ?: "") as String
                def upid = (transfer.upid ?: transfer.subject ?: "") as String
                def existingPatient = transfer.existingPatient == true
                def existingPatientReference = (transfer.existingPatientReference ?: "") as String
                def pendingReturnUrl = ui.pageLinkWithoutContextPath("transferapp", "pending", [app: appId, weeks: selectedWeeks])
                def agentRejected = transfer.agentRejected == true || transfer.agentRejected == "true"
                def agentApproved = transfer.agentDecisionApproved == true || transfer.agentDecisionApproved == "true"
                def needsApproval = transfer.needsInsuranceApproval == true || transfer.needsInsuranceApproval == "true"
                def statusClass = agentRejected ? "transfer-status-rejected"
                        : (agentApproved ? "transfer-status-approved"
                        : (needsApproval ? "transfer-status-awaiting" : "transfer-status-pending"))
                def statusLabel = (transfer.status ?: "pending") as String
            %>
            <tr class="transfer-row"
                data-uuid="${ ui.encodeHtmlAttribute(uuid) }"
                data-upid="${ ui.encodeHtmlAttribute(upid) }"
                data-agent-approved="${ ui.encodeHtmlAttribute(String.valueOf(transfer.agentApproved == true || transfer.agentApproved == 'true')) }"
                data-agent-comment="${ ui.encodeHtmlAttribute((transfer.agentComment ?: '') as String) }">
                <td>${ ui.encodeHtmlContent((transfer.date ?: "") as String) }</td>
                <td>
                    <span>${ ui.encodeHtmlContent((transfer.clientName ?: "") as String) }</span>
                    <span class="transfer-patient-badge ${ existingPatient ? 'transfer-patient-badge-existing' : 'transfer-patient-badge-new' }">
                        ${ existingPatient ? ui.message("transferapp.pending.patient.existing") : ui.message("transferapp.pending.patient.new") }
                    </span>
                </td>
                <td>${ ui.encodeHtmlContent(upid) }</td>
                <td>${ ui.encodeHtmlContent((transfer.origin ?: transfer.referringFacilityName ?: "") as String) }</td>
                <td>${ ui.encodeHtmlContent((transfer.receivingService ?: "") as String) }</td>
                <td>
                    <span class="${ statusClass }">${ ui.encodeHtmlContent(statusLabel) }</span>
                </td>
                <td>
                    <div class="transfer-pending-actions">
                        <a class="transfer-pending-view-link"
                           href="javascript:void(0);"
                           data-uuid="${ ui.encodeHtmlAttribute(uuid) }"
                           data-upid="${ ui.encodeHtmlAttribute(upid) }">
                            <i class="icon-share-alt"></i> ${ ui.message("transferapp.patient.transfers.view") }
                        </a>
                        <% if (existingPatient && existingPatientReference) { %>
                            <a class="button transfer-pending-workflow-button"
                               href="${ ui.pageLink(rwandaEmrModuleId, requestAppointmentPage, [patientId: existingPatientReference]) }">
                                <i class="icon-calendar"></i> ${ ui.message("transferapp.pending.action.schedule") }
                            </a>
                        <% } else if (!existingPatient) { %>
                            <a class="button confirm transfer-pending-workflow-button transfer-pending-register-link"
                               href="${ ui.pageLink('transferapp', 'registerPatientFromHie', [upid: upid, returnUrl: pendingReturnUrl]) }"
                               target="_blank"
                               rel="noopener">
                                <i class="icon-user"></i> ${ ui.message("transferapp.pending.action.register") }
                            </a>
                        <% } %>
                    </div>
                </td>
            </tr>
            <% } %>
        </tbody>
    </table>
</div>
<% } %>
</div>
</div>
