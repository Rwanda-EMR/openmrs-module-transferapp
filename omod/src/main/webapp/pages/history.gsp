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
        messages: {
            loading: "${ ui.encodeJavaScript(ui.message('transferapp.history.preview.loading')) }",
            missingIds: "${ ui.encodeJavaScript(ui.message('transferapp.history.preview.missingIds')) }",
            loadError: "${ ui.encodeJavaScript(ui.message('transferapp.history.preview.loadError')) }",
            empty: "${ ui.encodeJavaScript(ui.message('transferapp.history.preview.empty')) }"
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
                <th>${ ui.message("transferapp.history.column.action") }</th>
            </tr>
        </thead>
        <tbody>
            <% historyItems.each { item -> %>
            <tr class="transfer-history-row"
                data-transfer-id="${ ui.encodeHtmlAttribute(item.transferId ?: '') }"
                data-upid="${ ui.encodeHtmlAttribute(item.upid ?: '') }">
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
                <td>
                    <% if (item.transferId && item.upid) { %>
                    <a class="transfer-history-view-link"
                       href="javascript:void(0);"
                       data-transfer-id="${ ui.encodeHtmlAttribute(item.transferId) }"
                       data-upid="${ ui.encodeHtmlAttribute(item.upid) }"
                       title="${ ui.encodeHtmlAttribute(ui.message('transferapp.history.action.preview')) }">
                        <i class="icon-eye-open"></i> ${ ui.message("transferapp.history.action.preview") }
                    </a>
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
