<%
    ui.decorateWith("appui", "standardEmrPage")
    ui.includeCss("transferapp", "dashboard.css")
    ui.includeCss("transferapp", "transferRecords.css")
    ui.includeCss("transferapp", "transferFormPreview.css")
    ui.includeCss("uicommons", "datatables/dataTables_jui.css")
    ui.includeJavascript("uicommons", "datatables/jquery.dataTables.min.js")
    ui.includeJavascript("transferapp", "transferMohLogo.js")
    ui.includeJavascript("transferapp", "transferFormPreview.js")
    ui.includeJavascript("transferapp", "transferPreviewCommon.js")
    ui.includeJavascript("transferapp", "transferHistory.js")
%>
<script type="text/javascript">
    var breadcrumbs = [
        { icon: "icon-home", link: '/' + OPENMRS_CONTEXT_PATH + '/index.htm' },
        { label: "${ ui.message("transferapp.history.title") }" }
    ];
    var openmrsContextPath = (typeof openmrsContextPath !== "undefined" && openmrsContextPath)
        ? openmrsContextPath
        : "/${ ui.encodeJavaScript(contextPath) }";
    window.transferOpenmrsPath = openmrsContextPath;
    window.transferHistoryConfig = {
        restUrl: openmrsContextPath + "/ws/rest/v1/transferapp/transfer",
        reuseUrl: openmrsContextPath + "/ws/rest/v1/transferapp/transfer/reuseRendezvous",
        canCreateTransfer: ${ canCreateTransfer ? 'true' : 'false' },
        messages: {
            loading: "${ ui.encodeJavaScript(ui.message('transferapp.history.preview.loading')) }",
            missingIds: "${ ui.encodeJavaScript(ui.message('transferapp.history.preview.missingIds')) }",
            loadError: "${ ui.encodeJavaScript(ui.message('transferapp.history.preview.loadError')) }",
            empty: "${ ui.encodeJavaScript(ui.message('transferapp.history.preview.empty')) }",
            reuseTitle: "${ ui.encodeJavaScript(ui.message('transferapp.history.reuse.title')) }",
            reuseHint: "${ ui.encodeJavaScript(ui.message('transferapp.history.reuse.hint')) }",
            reuseSave: "${ ui.encodeJavaScript(ui.message('transferapp.history.reuse.save')) }",
            reuseClear: "${ ui.encodeJavaScript(ui.message('transferapp.history.reuse.clear')) }",
            reuseCancel: "${ ui.encodeJavaScript(ui.message('coreapps.cancel')) }",
            reuseSuccess: "${ ui.encodeJavaScript(ui.message('transferapp.history.reuse.success')) }",
            reuseCleared: "${ ui.encodeJavaScript(ui.message('transferapp.history.reuse.cleared')) }",
            reuseError: "${ ui.encodeJavaScript(ui.message('transferapp.history.reuse.error')) }",
            reusePastDate: "${ ui.encodeJavaScript(ui.message('transferapp.history.reuse.pastDate')) }",
            reuseAction: "${ ui.encodeJavaScript(ui.message('transferapp.history.action.reuse')) }",
            reuseNone: "${ ui.encodeJavaScript(ui.message('transferapp.history.reuse.none')) }"
        }
    };
</script>

<div class="transfer-records-page transfer-history-page">
${ ui.includeFragment("transferapp", "transfer/transferNav", [ activeTab: "history", app: appId ]) }

<h3 class="transfer-records-title">${ ui.message("transferapp.history.title") }</h3>
<% if (!canListTransfers) { %>
<div class="transfer-records-empty">${ ui.encodeHtmlContent(listAccessDeniedMessage ?: ui.message("transferapp.patient.transfers.listNotAllowed")) }</div>
<% } else { %>

<form id="transfer-history-filter-form"
      class="transfer-records-filters"
      method="get"
      action="${ ui.pageLink('transferapp', 'history') }">
    <input type="hidden" name="app" value="${ ui.encodeHtmlAttribute(appId) }" />
    <div class="transfer-history-filters-grid">
        <div class="transfer-records-filter-field">
            <label for="history-filter-upid">${ ui.message("transferapp.history.filter.upid") }</label>
            <input type="text"
                   id="history-filter-upid"
                   name="upid"
                   value="${ ui.encodeHtmlAttribute(filterUpid ?: '') }"
                   placeholder="${ ui.encodeHtmlAttribute(ui.message('transferapp.history.filter.upid.placeholder')) }"
                   autocomplete="off" />
        </div>
        <div class="transfer-records-filter-field">
            <label for="history-filter-month">${ ui.message("transferapp.history.filter.month") }</label>
            <select id="history-filter-month" name="month">
                <option value="">${ ui.message("transferapp.history.filter.month.all") }</option>
                <% if (monthOptions != null) { monthOptions.each { option -> %>
                <option value="${ ui.encodeHtmlAttribute(option.value) }"
                    <% if (filterMonth != null && filterMonth == option.value) { %>selected="selected"<% } %>>
                    ${ ui.encodeHtmlContent(option.label) }
                </option>
                <% } } %>
            </select>
        </div>
        <div class="transfer-records-filter-actions">
            <button type="submit" class="confirm">${ ui.message("transferapp.history.filter.apply") }</button>
            <a class="button"
               href="${ ui.pageLink('transferapp', 'history') }?app=${ ui.encodeHtmlAttribute(appId) }">
                ${ ui.message("transferapp.history.filter.clear") }
            </a>
        </div>
    </div>
</form>

<div id="transfer-history-preview-dialog" class="dialog transfer-preview-dialog" style="display: none;">
    <div class="dialog-header">
        <i class="icon-retweet"></i>
        <h3>${ ui.message("transferapp.history.previewTitle") }</h3>
    </div>
    <div class="dialog-content">
        <div id="transfer-history-preview-body"></div>
        <div class="transfer-preview-actions">
            <button type="button" id="transfer-history-preview-close" class="cancel">
                ${ ui.message("coreapps.close") }
            </button>
        </div>
    </div>
</div>

<div id="transfer-history-reuse-overlay" style="display:none;"></div>
<div id="transfer-history-reuse-dialog" class="dialog" style="display:none;">
    <div class="dialog-header">
        <i class="icon-calendar"></i>
        <h3>${ ui.message("transferapp.history.reuse.title") }</h3>
    </div>
    <div class="dialog-content">
        <p id="transfer-history-reuse-hint">${ ui.message("transferapp.history.reuse.hint") }</p>
        <p id="transfer-history-reuse-patient" style="margin:8px 0;font-weight:600;"></p>
        <label for="transfer-history-reuse-date">${ ui.message("transferapp.history.reuse.dateLabel") }</label>
        <input type="date" id="transfer-history-reuse-date" />
        <p id="transfer-history-reuse-status" class="transfer-history-reuse-status" style="display:none;margin-top:8px;"></p>
        <div class="transfer-preview-actions" style="margin-top:14px;">
            <button type="button" id="transfer-history-reuse-save" class="confirm">
                ${ ui.message("transferapp.history.reuse.save") }
            </button>
            <button type="button" id="transfer-history-reuse-clear" class="button">
                ${ ui.message("transferapp.history.reuse.clear") }
            </button>
            <button type="button" id="transfer-history-reuse-cancel" class="cancel">
                ${ ui.message("coreapps.cancel") }
            </button>
        </div>
    </div>
</div>

<% if (!hasHistory) { %>
<div class="transfer-records-empty">${ ui.message("transferapp.history.empty") }</div>
<% } else { %>
<div class="transfer-table-wrapper">
    <table id="transfer-history-table" class="transfer-datatable display">
        <thead>
            <tr>
                <th>${ ui.message("transferapp.history.column.date") }</th>
                <th>${ ui.message("transferapp.history.column.patient") }</th>
                <th>${ ui.message("transferapp.history.column.upid") }</th>
                <th>${ ui.message("transferapp.history.column.location") }</th>
                <th>${ ui.message("transferapp.history.column.phone") }</th>
                <th>${ ui.message("transferapp.history.column.reuseDate") }</th>
                <th>${ ui.message("transferapp.history.column.action") }</th>
            </tr>
        </thead>
        <tbody>
            <% historyItems.each { item -> %>
            <tr class="transfer-history-row"
                data-transfer-id="${ ui.encodeHtmlAttribute(item.transferId ?: '') }"
                data-upid="${ ui.encodeHtmlAttribute(item.upid ?: '') }"
                data-patient-id="${ item.patientId != null ? item.patientId : '' }"
                data-reuse-date="${ ui.encodeHtmlAttribute(item.reuseRendezvousDate ?: '') }"
                data-patient-name="${ ui.encodeHtmlAttribute(item.patientName ?: '') }">
                <td>${ ui.format(item.encounterDatetime) }</td>
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
                <td>${ ui.encodeHtmlContent(item.locationName ?: '') }</td>
                <td>${ ui.encodeHtmlContent(item.phoneNumber ?: '') }</td>
                <td class="transfer-history-reuse-date-cell">
                    <% if (item.reuseRendezvousDate) { %>
                    <span class="transfer-history-reuse-date-value">${ ui.encodeHtmlContent(item.reuseRendezvousDate) }</span>
                    <% } else { %>
                    <span class="transfer-history-reuse-date-value transfer-history-reuse-none">${ ui.message("transferapp.history.reuse.none") }</span>
                    <% } %>
                </td>
                <td>
                    <% if (item.transferId && item.upid) { %>
                    <a class="transfer-history-view-link"
                       href="javascript:void(0);"
                       data-transfer-id="${ ui.encodeHtmlAttribute(item.transferId) }"
                       data-upid="${ ui.encodeHtmlAttribute(item.upid) }"
                       title="${ ui.encodeHtmlAttribute(ui.message('transferapp.history.action.preview')) }">
                        <i class="icon-eye-open"></i> ${ ui.message("transferapp.history.action.preview") }
                    </a>
                    <% if (canCreateTransfer && item.patientId != null) { %>
                    &nbsp;
                    <a class="transfer-history-reuse-link"
                       href="javascript:void(0);"
                       data-transfer-id="${ ui.encodeHtmlAttribute(item.transferId) }"
                       data-upid="${ ui.encodeHtmlAttribute(item.upid) }"
                       data-patient-id="${ item.patientId }"
                       data-reuse-date="${ ui.encodeHtmlAttribute(item.reuseRendezvousDate ?: '') }"
                       data-patient-name="${ ui.encodeHtmlAttribute(item.patientName ?: '') }"
                       title="${ ui.encodeHtmlAttribute(ui.message('transferapp.history.action.reuse')) }">
                        <i class="icon-calendar"></i> ${ ui.message("transferapp.history.action.reuse") }
                    </a>
                    <% } %>
                    <% } else { %>
                    <span class="transfer-history-missing">${ ui.message("transferapp.history.action.unavailable") }</span>
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
