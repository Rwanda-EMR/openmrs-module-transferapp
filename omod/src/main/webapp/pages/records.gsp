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
    ui.includeJavascript("transferapp", "transferRecords.js")
%>
<script type="text/javascript">
    var breadcrumbs = [
        { icon: "icon-home", link: '/' + OPENMRS_CONTEXT_PATH + '/index.htm' },
        { label: "${ ui.message("transferapp.records.title") }" }
    ];
    var openmrsContextPath = (typeof openmrsContextPath !== "undefined" && openmrsContextPath)
        ? openmrsContextPath
        : "/${ ui.encodeJavaScript(contextPath) }";
    window.transferOpenmrsPath = openmrsContextPath;
    window.transferRecordsFilterConfig = {
        startDate: "${ ui.encodeJavaScript(filterStartDate ?: '') }",
        endDate: "${ ui.encodeJavaScript(filterEndDate ?: '') }",
        receivingFacilityCode: "${ ui.encodeJavaScript(filterReceivingFacilityCode ?: '') }",
        maxDateRangeMonths: ${ maxDateRangeMonths ?: 3 },
        messages: {
            dateRangeError: "${ ui.encodeJavaScript(ui.message('transferapp.records.filter.dateRangeError', maxDateRangeMonths ?: 3)) }",
            invalidDateRange: "${ ui.encodeJavaScript(ui.message('transferapp.records.filter.invalidDateRange')) }",
            destinationPlaceholder: "${ ui.encodeJavaScript(ui.message('transferapp.records.filter.destination.placeholder')) }"
        }
    };
</script>

<div class="transfer-records-page">
${ ui.includeFragment("transferapp", "transfer/transferNav", [ activeTab: "records", app: appId ]) }

<h3 class="transfer-records-title">${ ui.message("transferapp.records.title") }</h3>

<% if (!canListTransfers) { %>
<div class="transfer-records-empty">${ ui.encodeHtmlContent(listAccessDeniedMessage ?: ui.message("transferapp.patient.transfers.listNotAllowed")) }</div>
<% } else { %>

<form id="transfer-records-filter-form"
      class="transfer-records-filters"
      method="get"
      action="${ ui.pageLink('transferapp', 'records') }">
    <input type="hidden" name="app" value="${ ui.encodeHtmlAttribute(appId) }" />
    <% if (filteredPatientId != null) { %>
    <input type="hidden" name="patientId" value="${ ui.encodeHtmlAttribute(filteredPatientId.toString()) }" />
    <% } %>

    <div class="transfer-records-filters-grid">
        <div class="transfer-records-filter-field">
            <label for="records-filter-start-date">${ ui.message("transferapp.records.filter.startDate") }</label>
            <input type="text"
                   id="records-filter-start-date"
                   name="startDate"
                   class="transfer-records-date-input"
                   value="${ ui.encodeHtmlAttribute(filterStartDate ?: '') }"
                   autocomplete="off" />
        </div>
        <div class="transfer-records-filter-field">
            <label for="records-filter-end-date">${ ui.message("transferapp.records.filter.endDate") }</label>
            <input type="text"
                   id="records-filter-end-date"
                   name="endDate"
                   class="transfer-records-date-input"
                   value="${ ui.encodeHtmlAttribute(filterEndDate ?: '') }"
                   autocomplete="off" />
        </div>
        <div class="transfer-records-filter-field transfer-records-filter-field-destination">
            <label for="records-filter-destination">${ ui.message("transferapp.records.filter.destination") }</label>
            <select id="records-filter-destination" name="receivingFacilityCode" class="transfer-records-destination-select">
                <option value="">${ ui.message("transferapp.records.filter.destination.placeholder") }</option>
                <% if (receivingFacilities != null) { receivingFacilities.each { facility -> %>
                <option value="${ ui.encodeHtmlAttribute(facility.facilityCode) }"
                    <% if (filterReceivingFacilityCode != null && filterReceivingFacilityCode == facility.facilityCode) { %>selected="selected"<% } %>>
                    ${ ui.encodeHtmlContent(facility.facilityName) }
                </option>
                <% } } %>
            </select>
        </div>
        <div class="transfer-records-filter-field transfer-records-filter-field-formtype">
            <label for="records-filter-form-type">${ ui.message("transferapp.records.filter.formType") }</label>
            <select id="records-filter-form-type" name="formType" class="transfer-records-formtype-select">
                <option value="" <% if (filterFormType == null || filterFormType.length() == 0) { %>selected="selected"<% } %>>${ ui.message("transferapp.records.filter.formType.all") }</option>
                <option value="External" <% if (filterFormType == "External") { %>selected="selected"<% } %>>${ ui.message("transferapp.records.filter.formType.external") }</option>
                <option value="Maternity" <% if (filterFormType == "Maternity") { %>selected="selected"<% } %>>${ ui.message("transferapp.records.filter.formType.maternity") }</option>
                <option value="Neonatal" <% if (filterFormType == "Neonatal") { %>selected="selected"<% } %>>${ ui.message("transferapp.records.filter.formType.neonatal") }</option>
            </select>
        </div>
        <div class="transfer-records-filter-actions">
            <button type="submit" id="records-filter-apply" class="confirm">
                ${ ui.message("transferapp.records.filter.apply") }
            </button>
        </div>
    </div>
</form>

<% if (filteredPatientId != null && filteredPatientName != null) { %>
<p class="transfer-records-patient-filter">
    ${ ui.message("transferapp.records.filteredByPatient") }:
    <strong>${ ui.encodeHtmlContent(filteredPatientName) }</strong>
    <a href="${ ui.pageLink('transferapp', 'records') }?app=${ ui.encodeHtmlAttribute(appId) }&amp;startDate=${ ui.encodeHtmlAttribute(filterStartDate ?: '') }&amp;endDate=${ ui.encodeHtmlAttribute(filterEndDate ?: '') }<% if (filterReceivingFacilityCode != null && filterReceivingFacilityCode.length() > 0) { %>&amp;receivingFacilityCode=${ ui.encodeHtmlAttribute(filterReceivingFacilityCode) }<% } %><% if (filterFormType != null && filterFormType.length() > 0) { %>&amp;formType=${ ui.encodeHtmlAttribute(filterFormType) }<% } %>">
        ${ ui.message("transferapp.records.clearFilter") }
    </a>
</p>
<% } %>

<div id="transfer-preview-dialog" class="dialog transfer-preview-dialog" style="display: none;">
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

<% if (!hasRecords) { %>
<div class="transfer-records-empty">${ ui.message("transferapp.records.empty") }</div>
<% } else { %>
<div class="transfer-table-wrapper">
    <table id="transfer-records-table" class="transfer-datatable display">
        <thead>
            <tr>
                <th>${ ui.message("transferapp.patient.transfers.column.date") }</th>
                <th>${ ui.message("transferapp.records.column.patient") }</th>
                <th>${ ui.message("transferapp.records.column.emrId") }</th>
                <th>${ ui.message("transferapp.records.column.to") }</th>
                <th>${ ui.message("transferapp.patient.transfers.column.service") }</th>
                <th>${ ui.message("transferapp.records.column.formType") }</th>
                <th>${ ui.message("transferapp.patient.transfers.column.status") }</th>
                <th>${ ui.message("transferapp.patient.transfers.column.action") }</th>
            </tr>
        </thead>
        <tbody>
            <% records.each { record -> %>
            <tr class="transfer-row${ record.hieSent ? ' transfer-row-sent' : '' }"
                data-transfer-id="${ ui.encodeHtmlAttribute(record.id) }"
                data-uuid="${ ui.encodeHtmlAttribute(record.id) }"
                data-form-type="${ ui.encodeHtmlAttribute(record.formType) }">
                <td>${ ui.format(record.transferDate) }</td>
                <td>${ ui.format(record.clientName) }</td>
                <td>${ ui.format(record.emrId) }</td>
                <td>${ ui.format(record.receivingFacility) }</td>
                <td>${ ui.format(record.service) }</td>
                <td>${ ui.format(record.formType) }</td>
                <td>
                    <% if (record.hieSent) { %>
                        <span class="transfer-status-sent">${ ui.message("transferapp.patient.transfers.statusSent") }</span>
                    <% } else { %>
                        <span class="transfer-status-pending">${ ui.message("transferapp.patient.transfers.statusPending") }</span>
                    <% } %>
                </td>
                <td>
                    <a class="transfer-view-link"
                       href="#"
                       data-transfer-id="${ ui.encodeHtmlAttribute(record.id) }"
                       data-uuid="${ ui.encodeHtmlAttribute(record.id) }"
                       data-form-type="${ ui.encodeHtmlAttribute(record.formType) }">
                        <i class="icon-share-alt"></i> ${ ui.message("transferapp.patient.transfers.view") }
                    </a>
                    <% if (canCreateTransfer && record.patientId != null) { %>
                    <a class="transfer-edit-link"
                       href="${ ui.pageLink('coreapps', 'clinicianfacing/patient') }?patientId=${ record.patientId }"
                       title="${ ui.encodeHtmlAttribute(ui.message('transferapp.patient.transfers.edit')) }">
                        <i class="icon-pencil"></i> ${ ui.message("transferapp.patient.transfers.edit") }
                    </a>
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
