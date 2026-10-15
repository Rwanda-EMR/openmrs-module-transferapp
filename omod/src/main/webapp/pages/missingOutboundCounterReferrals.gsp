<%
    ui.decorateWith("appui", "standardEmrPage")
    ui.includeCss("transferapp", "dashboard.css")
    ui.includeCss("transferapp", "transferSection.css")
    ui.includeCss("transferapp", "transferRecords.css")
    ui.includeCss("transferapp", "transferFormPreview.css")
    ui.includeCss("transferapp", "flatpickr.min.css")
    ui.includeCss("transferapp", "select2.min.css")
    ui.includeCss("uicommons", "datatables/dataTables_jui.css")
    ui.includeJavascript("uicommons", "datatables/jquery.dataTables.min.js")
    ui.includeJavascript("transferapp", "flatpickr/flatpickr.min.js")
    ui.includeJavascript("transferapp", "select2/select2.min.js")
    ui.includeJavascript("transferapp", "transferMohLogo.js")
    ui.includeJavascript("transferapp", "transferFormPreview.js")
    ui.includeJavascript("transferapp", "transferPreviewCommon.js")
    ui.includeJavascript("transferapp", "hieTransferSection.js")
    ui.includeJavascript("transferapp", "missingOutboundCounterReferrals.js")
%>
<script type="text/javascript">
    var breadcrumbs = [
        { icon: "icon-home", link: '/' + OPENMRS_CONTEXT_PATH + '/index.htm' },
        { label: "${ ui.message("transferapp.dashboard.title") }", link: '${ ui.pageLink("transferapp", "dashboard") }?app=${ ui.encodeJavaScript(appId) }&startDate=${ ui.encodeJavaScript(filterStartDate ?: "") }&endDate=${ ui.encodeJavaScript(filterEndDate ?: "") }' },
        { label: "${ ui.message("transferapp.missingOutboundCounterReferrals.title") }" }
    ];
    var openmrsContextPath = (typeof openmrsContextPath !== "undefined" && openmrsContextPath)
        ? openmrsContextPath
        : "/${ ui.encodeJavaScript(contextPath) }";
    window.transferOpenmrsPath = openmrsContextPath;
    window.missingOutboundCounterReferralsConfig = {
        startDate: "${ ui.encodeJavaScript(filterStartDate ?: '') }",
        endDate: "${ ui.encodeJavaScript(filterEndDate ?: '') }",
        maxDateRangeMonths: ${ maxDateRangeMonths ?: 6 },
        canListTransfers: ${ canListTransfers ? 'true' : 'false' },
        importUrl: openmrsContextPath + "/ws/rest/v1/transferapp/transfer/feedback/import",
        messages: {
            dateRangeError: "${ ui.encodeJavaScript(ui.message('transferapp.dashboard.filter.dateRangeError', maxDateRangeMonths ?: 6)) }",
            invalidDateRange: "${ ui.encodeJavaScript(ui.message('transferapp.dashboard.filter.invalidDateRange')) }",
            importSuccess: "${ ui.encodeJavaScript(ui.message('transferapp.missingOutboundCounterReferrals.importSuccess')) }",
            importSkipped: "${ ui.encodeJavaScript(ui.message('transferapp.missingOutboundCounterReferrals.importSkipped')) }",
            importError: "${ ui.encodeJavaScript(ui.message('transferapp.missingOutboundCounterReferrals.importError')) }",
            copyId: "${ ui.encodeJavaScript(ui.message('transferapp.missingOutboundCounterReferrals.copyId')) }",
            copySuccess: "${ ui.encodeJavaScript(ui.message('transferapp.missingOutboundCounterReferrals.copySuccess')) }",
            copyError: "${ ui.encodeJavaScript(ui.message('transferapp.missingOutboundCounterReferrals.copyError')) }"
        }
    };
</script>

<div class="transfer-records-page">
${ ui.includeFragment("transferapp", "transfer/transferNav", [ activeTab: "dashboard", app: appId ]) }

<h3 class="transfer-records-title">${ ui.message("transferapp.missingOutboundCounterReferrals.title") }</h3>
<p class="transfer-records-filter">${ ui.message("transferapp.missingOutboundCounterReferrals.subtitle") }</p>

<% if (!canAccessDashboard) { %>
<div class="transfer-records-empty" style="margin-top: 16px; padding: 12px; background: #fff8e6; border: 1px solid #f0d78c;">
    ${ ui.encodeHtmlContent(dashboardAccessDeniedMessage) }
</div>
<% } else { %>

<form id="missing-outbound-cr-filter-form"
      class="transfer-dashboard-filters"
      method="get"
      action="${ ui.pageLink('transferapp', 'missingOutboundCounterReferrals') }">
    <input type="hidden" name="app" value="${ ui.encodeHtmlAttribute(appId) }" />
    <div class="transfer-dashboard-filters-grid">
        <div class="transfer-dashboard-filter-field">
            <label for="missing-outbound-cr-filter-start-date">${ ui.message("transferapp.dashboard.filter.startDate") }</label>
            <input type="text"
                   id="missing-outbound-cr-filter-start-date"
                   name="startDate"
                   class="transfer-dashboard-date-input"
                   value="${ ui.encodeHtmlAttribute(filterStartDate ?: '') }"
                   autocomplete="off" />
        </div>
        <div class="transfer-dashboard-filter-field">
            <label for="missing-outbound-cr-filter-end-date">${ ui.message("transferapp.dashboard.filter.endDate") }</label>
            <input type="text"
                   id="missing-outbound-cr-filter-end-date"
                   name="endDate"
                   class="transfer-dashboard-date-input"
                   value="${ ui.encodeHtmlAttribute(filterEndDate ?: '') }"
                   autocomplete="off" />
        </div>
        <div class="transfer-dashboard-filter-actions">
            <button type="submit" id="missing-outbound-cr-filter-apply" class="confirm">
                ${ ui.message("transferapp.dashboard.filter.apply") }
            </button>
            <a class="button"
               href="${ ui.pageLink('transferapp', 'dashboard') }?app=${ ui.encodeHtmlAttribute(appId) }&amp;startDate=${ ui.encodeHtmlAttribute(filterStartDate ?: '') }&amp;endDate=${ ui.encodeHtmlAttribute(filterEndDate ?: '') }">
                ${ ui.message("transferapp.missingCounterReferrals.backToDashboard") }
            </a>
        </div>
    </div>
</form>

<%
    def ctxPath = (ui.contextPath() ?: "").toString()
    while (ctxPath.startsWith("/")) {
        ctxPath = ctxPath.substring(1)
    }
    def hieRestUrl = "/" + ctxPath + "/ws/rest/v1/transferapp/transfer"
    def feedbackRestUrl = "/" + ctxPath + "/ws/rest/v1/transferapp/transfer/feedback"
%>
<div id="transfer-recorded-hie-config" style="display:none;"
     data-has-transfer-id="true"
     data-can-provide-feedback="false"
     data-import-counter-referral="true"
     data-rest-url="${ ui.encodeHtmlAttribute(hieRestUrl) }"
     data-feedback-url="${ ui.encodeHtmlAttribute(feedbackRestUrl) }"
     data-facilities-url="${ ui.encodeHtmlAttribute(feedbackRestUrl + '/facilities') }"></div>

${ ui.includeFragment("transferapp", "patient/hieTransferPreviewDialog") }

<% if (!hasMissingOutboundCounterReferrals) { %>
<div class="transfer-records-empty">${ ui.message("transferapp.missingOutboundCounterReferrals.empty") }</div>
<% } else { %>
<div class="transfer-table-wrapper">
    <table id="missing-outbound-counter-referrals-table" class="transfer-datatable display">
        <thead>
            <tr>
                <th>${ ui.message("transferapp.missingOutboundCounterReferrals.column.transferDate") }</th>
                <th>${ ui.message("transferapp.missingCounterReferrals.column.patient") }</th>
                <th>${ ui.message("transferapp.missingCounterReferrals.column.upid") }</th>
                <th>${ ui.message("transferapp.missingOutboundCounterReferrals.column.destination") }</th>
                <th>${ ui.message("transferapp.records.column.formType") }</th>
                <th>${ ui.message("transferapp.missingOutboundCounterReferrals.column.insuranceType") }</th>
                <th>${ ui.message("transferapp.patient.transfers.column.action") }</th>
            </tr>
        </thead>
        <tbody>
            <% missingOutboundCounterReferrals.each { item -> %>
            <tr>
                <td>${ ui.format(item.receivedDate) }</td>
                <td>
                    <% if (item.patientId != null) { %>
                    <a href="${ ui.pageLink('coreapps', 'clinicianfacing/patient') }?patientId=${ item.patientId }">
                        ${ ui.encodeHtmlContent(item.patientName ?: '') }
                    </a>
                    <% } else { %>
                    ${ ui.encodeHtmlContent(item.patientName ?: '') }
                    <% } %>
                </td>
                <td>${ ui.encodeHtmlContent(item.upid ?: '') }</td>
                <td>${ ui.encodeHtmlContent(item.destinationFacility ?: '') }</td>
                <td>${ ui.encodeHtmlContent(item.formType ?: '') }</td>
                <td>${ ui.encodeHtmlContent(item.insuranceType ?: '') }</td>
                <td class="missing-outbound-actions">
                    <% if (item.upid) { %>
                    <a class="hie-transfer-view-link transfer-view-link"
                       href="javascript:void(0);"
                       data-transfer-id="${ ui.encodeHtmlAttribute(item.hieTransferId ?: '') }"
                       data-upid="${ ui.encodeHtmlAttribute(item.upid ?: '') }"
                       data-patient-id="${ item.patientId != null ? item.patientId : '' }"
                       data-has-transfer-id="true"
                       data-import-counter-referral="true"
                       data-can-provide-feedback="false"
                       data-rest-url="${ ui.encodeHtmlAttribute(hieRestUrl) }"
                       data-feedback-url="${ ui.encodeHtmlAttribute(feedbackRestUrl) }"
                       data-facilities-url="${ ui.encodeHtmlAttribute(feedbackRestUrl + '/facilities') }">
                        <i class="icon-eye-open"></i> ${ ui.message("transferapp.patient.transfers.view") }
                    </a>
                    <% } else { %>
                    <span class="transfer-records-filter">${ ui.message("transferapp.missingCounterReferrals.missingUpid") }</span>
                    <% } %>
                    <% if (item.hieTransferId) { %>
                    <button type="button"
                            class="button missing-outbound-copy-id"
                            data-transfer-id="${ ui.encodeHtmlAttribute(item.hieTransferId) }"
                            title="${ ui.encodeHtmlAttribute(ui.message('transferapp.missingOutboundCounterReferrals.copyId')) }">
                        <i class="icon-copy"></i> ${ ui.message("transferapp.missingOutboundCounterReferrals.copyId") }
                    </button>
                    <% } %>
                </td>
            </tr>
            <% } %>
        </tbody>
    </table>
</div>
<% } %>
<% } %>
</div>
