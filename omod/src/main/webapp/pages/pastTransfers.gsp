<%
    ui.decorateWith("appui", "standardEmrPage")
    ui.includeCss("transferapp", "dashboard.css")
    ui.includeCss("transferapp", "transferRecords.css")
    ui.includeCss("transferapp", "transferFormPreview.css")
    ui.includeCss("transferapp", "flatpickr.min.css")
    ui.includeJavascript("transferapp", "flatpickr/flatpickr.min.js")
    ui.includeJavascript("transferapp", "transferMohLogo.js")
    ui.includeJavascript("transferapp", "transferFormPreview.js")
    ui.includeJavascript("transferapp", "transferPreviewCommon.js")
    ui.includeJavascript("transferapp", "pastTransfers.js")
%>
<script type="text/javascript">
    var breadcrumbs = [
        { icon: "icon-home", link: '/' + OPENMRS_CONTEXT_PATH + '/index.htm' },
        { label: "${ ui.message("transferapp.pastTransfers.title") }" }
    ];
    var openmrsContextPath = (typeof openmrsContextPath !== "undefined" && openmrsContextPath)
        ? openmrsContextPath
        : "/${ ui.encodeJavaScript(contextPath) }";
    window.transferOpenmrsPath = openmrsContextPath;
    window.pastTransfersConfig = {
        previewUrl: openmrsContextPath + "/module/transferapp/transfer/preview.form",
        restUrl: openmrsContextPath + "/ws/rest/v1/transferapp/transfer",
        receiveUrl: openmrsContextPath + "/ws/rest/v1/transferapp/transfer/receive",
        validateUrl: openmrsContextPath + "/ws/rest/v1/transferapp/transfer/validate",
        listUrl: openmrsContextPath + "/module/transferapp/transfer/pastTransfersList.form",
        canCreateTransfer: ${ canCreateTransfer ? 'true' : 'false' },
        canListTransfers: ${ canListTransfers ? 'true' : 'false' },
        filterStartDate: "${ ui.encodeJavaScript(filterStartDate ?: '') }",
        filterEndDate: "${ ui.encodeJavaScript(filterEndDate ?: '') }",
        filterUpid: "${ ui.encodeJavaScript(filterUpid ?: '') }",
        pageSize: ${ pastTransfersPageSize ?: 100 },
        nextOffset: ${ pastTransfersNextOffset ?: 0 },
        totalCount: ${ pastTransfersTotalCount ?: 0 },
        loadedCount: ${ pastTransfersLoadedCount ?: 0 },
        hasMore: ${ pastTransfersHasMore ? 'true' : 'false' },
        messages: {
            loading: "${ ui.encodeJavaScript(ui.message('transferapp.pastTransfers.preview.loading')) }",
            previewError: "${ ui.encodeJavaScript(ui.message('transferapp.pastTransfers.preview.error')) }",
            missingIds: "${ ui.encodeJavaScript(ui.message('transferapp.pastTransfers.preview.missingIds')) }",
            saveSuccess: "${ ui.encodeJavaScript(ui.message('transferapp.pastTransfers.preview.saveSuccess')) }",
            saveError: "${ ui.encodeJavaScript(ui.message('transferapp.pastTransfers.preview.saveError')) }",
            alreadyLocal: "${ ui.encodeJavaScript(ui.message('transferapp.pastTransfers.preview.alreadyLocal')) }",
            listLoading: "${ ui.encodeJavaScript(ui.message('transferapp.pastTransfers.hieList.loading')) }",
            listError: "${ ui.encodeJavaScript(ui.message('transferapp.pastTransfers.hieList.error')) }",
            listEmpty: "${ ui.encodeJavaScript(ui.message('transferapp.pastTransfers.hieList.empty')) }",
            listMissing: "${ ui.encodeJavaScript(ui.message('transferapp.pastTransfers.hieList.missing')) }",
            previewTitle: "${ ui.encodeJavaScript(ui.message('transferapp.pastTransfers.action.preview')) }",
            downloadTitle: "${ ui.encodeJavaScript(ui.message('transferapp.pastTransfers.action.download')) }",
            visitOpen: "${ ui.encodeJavaScript(ui.message('transferapp.pastTransfers.visitOpen')) }",
            visitRangeLabel: "${ ui.encodeJavaScript(ui.message('transferapp.pastTransfers.hieList.visitRange')) }",
            loadMore: "${ ui.encodeJavaScript(ui.message('transferapp.pastTransfers.loadMore')) }",
            loadMoreLoading: "${ ui.encodeJavaScript(ui.message('transferapp.pastTransfers.loadMore.loading')) }",
            loadMoreError: "${ ui.encodeJavaScript(ui.message('transferapp.pastTransfers.loadMore.error')) }",
            showingStatus: "${ ui.encodeJavaScript(ui.message('transferapp.pastTransfers.showingStatus')) }",
            transferReady: "${ ui.encodeJavaScript(ui.message('transferapp.pastTransfers.transferRecords.ready')) }",
            transferCopy: "${ ui.encodeJavaScript(ui.message('transferapp.pastTransfers.transferRecords.copy')) }",
            transferCopied: "${ ui.encodeJavaScript(ui.message('transferapp.pastTransfers.transferRecords.copied')) }",
            transferUuidTitle: "${ ui.encodeJavaScript(ui.message('transferapp.pastTransfers.transferRecords.popoverTitle')) }"
        }
    };
    jq(function() {
        if (typeof flatpickr !== "function") {
            return;
        }
        var cfg = window.pastTransfersConfig || {};
        var common = {
            dateFormat: "Y-m-d",
            altInput: true,
            altFormat: "d M Y",
            allowInput: true,
            disableMobile: true
        };
        var startInput = document.getElementById("past-transfers-filter-start-date");
        var endInput = document.getElementById("past-transfers-filter-end-date");
        var startPicker = null;
        var endPicker = null;
        if (startInput) {
            startPicker = flatpickr(startInput, jq.extend({}, common, {
                defaultDate: cfg.filterStartDate || null,
                onChange: function(selectedDates) {
                    if (endPicker && selectedDates && selectedDates[0]) {
                        endPicker.set("minDate", selectedDates[0]);
                    }
                }
            }));
        }
        if (endInput) {
            endPicker = flatpickr(endInput, jq.extend({}, common, {
                defaultDate: cfg.filterEndDate || null,
                minDate: cfg.filterStartDate || null,
                onChange: function(selectedDates) {
                    if (startPicker && selectedDates && selectedDates[0]) {
                        startPicker.set("maxDate", selectedDates[0]);
                    }
                }
            }));
        }
        if (startPicker && cfg.filterStartDate) {
            startPicker.set("maxDate", cfg.filterEndDate || null);
        }
    });
</script>

<div class="transfer-records-page transfer-past-transfers-page">
${ ui.includeFragment("transferapp", "transfer/transferNav", [ activeTab: "pastTransfers", app: appId ]) }

<h3 class="transfer-records-title">${ ui.message("transferapp.pastTransfers.title") }</h3>

<% if (!canAccessPastTransfers) { %>
<div class="transfer-records-empty">${ ui.encodeHtmlContent(accessDeniedMessage) }</div>
<% } else { %>

<form id="past-transfers-filter-form"
      class="transfer-records-filters"
      method="get"
      action="${ ui.pageLink('transferapp', 'pastTransfers') }">
    <input type="hidden" name="app" value="${ ui.encodeHtmlAttribute(appId) }" />
    <div class="transfer-past-transfers-filters-grid">
        <div class="transfer-records-filter-field transfer-records-filter-field-start-date">
            <label for="past-transfers-filter-start-date">${ ui.message("transferapp.pastTransfers.filter.startDate") }</label>
            <input type="text"
                   id="past-transfers-filter-start-date"
                   name="startDate"
                   value="${ ui.encodeHtmlAttribute(filterStartDate ?: '') }"
                   placeholder="YYYY-MM-DD"
                   autocomplete="off"
                   required="required" />
        </div>
        <div class="transfer-records-filter-field transfer-records-filter-field-end-date">
            <label for="past-transfers-filter-end-date">${ ui.message("transferapp.pastTransfers.filter.endDate") }</label>
            <input type="text"
                   id="past-transfers-filter-end-date"
                   name="endDate"
                   value="${ ui.encodeHtmlAttribute(filterEndDate ?: '') }"
                   placeholder="YYYY-MM-DD"
                   autocomplete="off"
                   required="required" />
        </div>
        <div class="transfer-records-filter-field transfer-records-filter-field-upid">
            <label for="past-transfers-filter-upid">${ ui.message("transferapp.pastTransfers.filter.upid") }</label>
            <input type="text"
                   id="past-transfers-filter-upid"
                   name="upid"
                   value="${ ui.encodeHtmlAttribute(filterUpid ?: '') }"
                   placeholder="${ ui.encodeHtmlAttribute(ui.message('transferapp.pastTransfers.filter.upid.placeholder')) }"
                   autocomplete="off" />
        </div>
        <div class="transfer-records-filter-actions">
            <button type="submit" class="confirm">${ ui.message("transferapp.pastTransfers.filter.apply") }</button>
        </div>
    </div>
</form>

<div id="past-transfers-preview-overlay" class="past-transfers-overlay" style="display:none;"></div>
<div id="past-transfers-preview-dialog" class="dialog transfer-preview-dialog past-transfers-preview-dialog" style="display:none;">
    <div class="dialog-header">
        <i class="icon-eye-open"></i>
        <h3>${ ui.message("transferapp.pastTransfers.preview.title") }</h3>
    </div>
    <div class="dialog-content">
        <div id="past-transfers-preview-body"></div>
        <p id="past-transfers-preview-status" class="past-transfers-status" style="display:none;"></p>
        <div class="transfer-preview-actions">
            <% if (canCreateTransfer) { %>
            <button type="button" id="past-transfers-preview-save" class="confirm" style="display:none;">
                ${ ui.message("transferapp.pastTransfers.preview.saveCorrect") }
            </button>
            <% } %>
            <button type="button" id="past-transfers-preview-close" class="cancel">
                ${ ui.message("coreapps.close") }
            </button>
        </div>
    </div>
</div>

<div id="past-transfers-hie-overlay" class="past-transfers-overlay" style="display:none;"></div>
<div id="past-transfers-hie-dialog" class="dialog past-transfers-hie-dialog" style="display:none;">
    <div class="dialog-header">
        <i class="icon-download-alt"></i>
        <h3>${ ui.message("transferapp.pastTransfers.hieList.title") }</h3>
    </div>
    <div class="dialog-content">
        <p id="past-transfers-hie-meta" class="past-transfers-hie-meta"></p>
        <div id="past-transfers-hie-body"></div>
        <div class="transfer-preview-actions">
            <button type="button" id="past-transfers-hie-close" class="cancel">
                ${ ui.message("coreapps.close") }
            </button>
        </div>
    </div>
</div>

<% if (listError != null && listError.trim().length() > 0) { %>
<div class="transfer-records-empty" style="color:#b94a48;">${ ui.encodeHtmlContent(listError) }</div>
<% } else if (!hasPastTransfers) { %>
<div class="transfer-records-empty">${ ui.message("transferapp.pastTransfers.empty") }</div>
<% } else { %>
<div class="transfer-table-wrapper">
    <p id="past-transfers-status" class="past-transfers-status-line">
        ${ ui.message("transferapp.pastTransfers.showingStatus", pastTransfersLoadedCount ?: 0, pastTransfersTotalCount ?: 0) }
    </p>
    <table id="past-transfers-table" class="transfer-datatable display">
        <thead>
            <tr>
                <th>${ ui.message("transferapp.pastTransfers.column.row") }</th>
                <th>${ ui.message("transferapp.pastTransfers.column.upid") }</th>
                <th>${ ui.message("transferapp.pastTransfers.column.patientName") }</th>
                <th>${ ui.message("transferapp.pastTransfers.column.visitDate") }</th>
                <th>${ ui.message("transferapp.pastTransfers.column.visitEndDate") }</th>
                <th>${ ui.message("transferapp.pastTransfers.column.insuranceType") }</th>
                <th>${ ui.message("transferapp.pastTransfers.column.insuranceId") }</th>
                <th>${ ui.message("transferapp.pastTransfers.column.transferRecords") }</th>
                <th>${ ui.message("transferapp.pastTransfers.column.action") }</th>
            </tr>
        </thead>
        <tbody>
        <% pastTransferItems.eachWithIndex { item, index -> %>
            <%
                def visitDateIso = ""
                def visitEndIso = ""
                if (item.visitStartDatetime != null) {
                    visitDateIso = new java.text.SimpleDateFormat("yyyy-MM-dd").format(item.visitStartDatetime)
                }
                if (item.visitStopDatetime != null) {
                    visitEndIso = new java.text.SimpleDateFormat("yyyy-MM-dd").format(item.visitStopDatetime)
                }
                def hasTransferRecord = item.primaryTransferId != null && item.primaryTransferId.trim().length() > 0
            %>
            <tr class="past-transfers-row"
                data-patient-id="${ item.patientId != null ? item.patientId : '' }"
                data-visit-id="${ item.visitId != null ? item.visitId : '' }"
                data-upid="${ ui.encodeHtmlAttribute(item.upid ?: '') }"
                data-visit-date="${ ui.encodeHtmlAttribute(visitDateIso) }"
                data-visit-end-date="${ ui.encodeHtmlAttribute(visitEndIso) }"
                data-transfer-id="${ ui.encodeHtmlAttribute(item.primaryTransferId ?: '') }"
                data-local-uuid="${ ui.encodeHtmlAttribute(item.localTransferUuid ?: '') }"
                data-patient-name="${ ui.encodeHtmlAttribute(item.patientName ?: '') }">
                <td>${ index + 1 }</td>
                <td>${ ui.encodeHtmlContent(item.upid ?: '') }</td>
                <td>${ ui.encodeHtmlContent(item.patientName ?: '') }</td>
                <td>
                    <% if (item.visitStartDatetime) { %>
                        ${ ui.format(item.visitStartDatetime) }
                    <% } %>
                </td>
                <td>
                    <% if (item.visitStopDatetime) { %>
                        ${ ui.format(item.visitStopDatetime) }
                    <% } else { %>
                        <span class="transfer-past-open-visit">${ ui.message("transferapp.pastTransfers.visitOpen") }</span>
                    <% } %>
                </td>
                <td>${ ui.encodeHtmlContent(item.insuranceType ?: '') }</td>
                <td>${ ui.encodeHtmlContent(item.insuranceId ?: '') }</td>
                <td class="past-transfers-records-cell">
                    <% if (hasTransferRecord) { %>
                    <a href="javascript:void(0);"
                       class="past-transfers-ready-link"
                       data-transfer-records="${ ui.encodeHtmlAttribute(item.transferRecords ?: item.primaryTransferId ?: '') }"
                       role="button"
                       aria-haspopup="true">
                        ${ ui.message("transferapp.pastTransfers.transferRecords.ready") }
                    </a>
                    <% } %>
                </td>
                <td class="transfer-past-action">
                    <% if (hasTransferRecord) { %>
                    <a href="javascript:void(0);"
                       class="button past-transfers-preview-link"
                       title="${ ui.encodeHtmlAttribute(ui.message('transferapp.pastTransfers.action.preview')) }">
                        <i class="icon-eye-open"></i>
                    </a>
                    <% } else { %>
                    <a href="javascript:void(0);"
                       class="button past-transfers-download-link"
                       title="${ ui.encodeHtmlAttribute(ui.message('transferapp.pastTransfers.action.download')) }">
                        <i class="icon-download-alt"></i>
                    </a>
                    <% } %>
                </td>
            </tr>
        <% } %>
        </tbody>
    </table>
    <div class="past-transfers-load-more-wrap">
        <button type="button"
                id="past-transfers-load-more"
                class="confirm"
                style="${ pastTransfersHasMore ? '' : 'display:none;' }">
            ${ ui.message("transferapp.pastTransfers.loadMore") }
        </button>
        <p id="past-transfers-load-more-status" class="past-transfers-status" style="display:none;"></p>
    </div>
</div>
<% } %>
<% } %>
</div>
