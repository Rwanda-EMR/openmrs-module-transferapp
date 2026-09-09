(function() {
    var jq = (typeof jQuery !== "undefined") ? jQuery : (typeof $ !== "undefined" ? $ : null);
    if (!jq) {
        return;
    }

    jq(function() {
        var filterConfig = window.transferAmbulanceVoucherFilterConfig || {};
        var filterMessages = filterConfig.messages || {};
        var maxDateRangeMonths = filterConfig.maxDateRangeMonths || 3;
        var filterForm = jq("#transfer-ambulance-voucher-filter-form");
        var startInput = document.getElementById("ambulance-voucher-filter-start-date");
        var endInput = document.getElementById("ambulance-voucher-filter-end-date");
        var errorEl = jq("#ambulance-voucher-filter-error");
        var searchErrorEl = jq("#ambulance-voucher-search-error");
        var previewUrl = filterConfig.previewUrl
            || ((window.transferOpenmrsPath || "") + "/module/transferapp/transfer/ambulanceVoucherPreview.form");
        var transferPreviewUrl = filterConfig.transferPreviewUrl
            || ((window.transferOpenmrsPath || "") + "/module/transferapp/transfer/preview.form");
        var transferPreviewResourcesBase = filterConfig.transferPreviewResourcesBase
            || ((window.transferOpenmrsPath || "") + "/moduleResources/transferapp/scripts/");
        var transferPreviewScriptsLoading = null;
        var hieSearchUrl = filterConfig.hieSearchUrl
            || ((window.transferOpenmrsPath || "") + "/module/transferapp/transfer/hieTransfersByIdentifier.form");
        var registerPatientUrl = filterConfig.registerPatientUrl
            || ((window.transferOpenmrsPath || "") + "/transferapp/registerPatientFromHie.page");
        var registerPreviewUrl = filterConfig.registerPreviewUrl
            || ((window.transferOpenmrsPath || "") + "/module/transferapp/transfer/hiePatientRegistrationPreview.form");
        var registerConfirmUrl = filterConfig.registerConfirmUrl
            || ((window.transferOpenmrsPath || "") + "/module/transferapp/transfer/registerPatientFromHie.form");
        var createVoucherUrl = filterConfig.createVoucherUrl
            || ((window.transferOpenmrsPath || "") + "/module/transferapp/transfer/createAmbulanceVoucherFromHie.form");
        var ambulanceVoucherReturnUrl = filterConfig.ambulanceVoucherReturnUrl
            || "/transferapp/ambulanceVoucher.page?app=transferapp.dashboard";
        var pendingRegisterUpid = null;
        var pendingCreateVoucher = null;
        var lastHieSearchContext = {
            patientId: null,
            upid: null,
            identifierKind: null,
            existingPatient: false,
            hasInsuranceOnRegistration: false,
            insuranceCardNumber: "",
            currentFacilityName: "",
            transfers: []
        };

        function parseYmd(value) {
            if (!value) {
                return null;
            }
            var parts = String(value).split("-");
            if (parts.length !== 3) {
                return null;
            }
            var year = parseInt(parts[0], 10);
            var month = parseInt(parts[1], 10) - 1;
            var day = parseInt(parts[2], 10);
            if (isNaN(year) || isNaN(month) || isNaN(day)) {
                return null;
            }
            return new Date(year, month, day);
        }

        function monthDiff(startDate, endDate) {
            var months = (endDate.getFullYear() - startDate.getFullYear()) * 12;
            months += endDate.getMonth() - startDate.getMonth();
            if (endDate.getDate() < startDate.getDate()) {
                months -= 1;
            }
            return months;
        }

        function showFilterError(message) {
            if (!errorEl.length) {
                window.alert(message);
                return;
            }
            errorEl.text(message || "").show();
        }

        function clearFilterError() {
            if (errorEl.length) {
                errorEl.hide().text("");
            }
        }

        function showSearchError(message) {
            if (!searchErrorEl.length) {
                window.alert(message);
                return;
            }
            searchErrorEl.text(message || "").show();
        }

        function clearSearchError() {
            if (searchErrorEl.length) {
                searchErrorEl.hide().text("");
            }
        }

        function validateDateRange(startValue, endValue) {
            clearFilterError();
            var startDate = parseYmd(startValue);
            var endDate = parseYmd(endValue);
            if (!startDate || !endDate) {
                showFilterError(filterMessages.invalidDateRange || "Please select valid start and end dates.");
                return false;
            }
            if (startDate > endDate) {
                showFilterError(filterMessages.invalidDateRange || "Start date must be on or before end date.");
                return false;
            }
            if (monthDiff(startDate, endDate) > maxDateRangeMonths) {
                showFilterError(filterMessages.dateRangeError
                    || ("Date range cannot exceed " + maxDateRangeMonths + " months."));
                return false;
            }
            return true;
        }

        function escapeHtml(value) {
            return String(value == null ? "" : value)
                .replace(/&/g, "&amp;")
                .replace(/</g, "&lt;")
                .replace(/>/g, "&gt;")
                .replace(/"/g, "&quot;")
                .replace(/'/g, "&#39;");
        }

        function displayValue(value) {
            var text = value == null ? "" : String(value).trim();
            return text.length ? escapeHtml(text) : "&nbsp;";
        }

        function detectIdentifierKind(raw) {
            var value = String(raw || "").trim().replace(/\s+/g, "");
            if (/^\d{16}$/.test(value)) {
                return "NID";
            }
            if (/^\d{6}-\d{4}-\d{4}$/.test(value)) {
                return "UPID";
            }
            var digits = value.replace(/-/g, "");
            if (/^\d{14}$/.test(digits)) {
                return "UPID";
            }
            return null;
        }

        function buildVoucherHtml(v) {
            var distanceLabel = (v.distanceKm != null && v.distanceKm !== "")
                ? (escapeHtml(v.distanceKm) + " Km")
                : "&nbsp;";
            var amountLabel = v.amount ? escapeHtml(v.amount) + " RWF" : "&nbsp;";
            return ""
                + "<div class='bon-ambulance'>"
                + "  <div class='bon-ambulance-header'>"
                + "    <div class='bon-ambulance-brand'>" + displayValue(v.organizationName) + "</div>"
                + "    <div class='bon-ambulance-fax'>FAX: " + displayValue(v.fax) + "</div>"
                + "    <h2 class='bon-ambulance-title'>" + displayValue(v.title || "BON D'AMBULANCE") + "</h2>"
                + "  </div>"
                + "  <div class='bon-ambulance-location'>"
                + "    <div><span>Province</span><strong>" + displayValue(v.province) + "</strong></div>"
                + "    <div><span>District</span><strong>" + displayValue(v.district) + "</strong></div>"
                + "    <div><span>Section / Hôpital</span><strong>" + displayValue(v.sectionHospital) + "</strong></div>"
                + "  </div>"
                + "  <div class='bon-ambulance-grid'>"
                + "    <div class='bon-field'><label>1. Date</label><div>" + displayValue(v.date) + "</div></div>"
                + "    <div class='bon-field'><label>2. Heure de départ</label><div>" + displayValue(v.departureTime) + "</div></div>"
                + "    <div class='bon-field'><label>3. Nombre de patients à bord</label><div>" + displayValue(v.patientCount) + "</div></div>"
                + "    <div class='bon-field'><label>4. ID</label><div>" + displayValue(v.voucherId) + "</div></div>"
                + "    <div class='bon-field bon-field-wide'><label>5. Destination</label><div>" + displayValue(v.destination) + "</div></div>"
                + "    <div class='bon-field'><label>Distance parcourue (Allée et Retour)</label><div>" + distanceLabel + "</div></div>"
                + "    <div class='bon-field'><label>Montant facture</label><div>" + amountLabel + "</div></div>"
                + "  </div>"
                + "  <table class='bon-patients'>"
                + "    <thead><tr>"
                + "      <th>#</th>"
                + "      <th>Nom et prénom du (des) patient(s)</th>"
                + "      <th>Numéro d'affiliation du (des) patient(s)</th>"
                + "    </tr></thead>"
                + "    <tbody>"
                + "      <tr><td>1</td><td>" + displayValue(v.patientName) + "</td><td>" + displayValue(v.affiliationNumber) + "</td></tr>"
                + "      <tr><td>2</td><td>&nbsp;</td><td>&nbsp;</td></tr>"
                + "      <tr><td>3</td><td>&nbsp;</td><td>&nbsp;</td></tr>"
                + "    </tbody>"
                + "  </table>"
                + "  <div class='bon-notes'>"
                + "    <p><strong>N.B:</strong> " + displayValue(v.noteReferral) + "</p>"
                + "    <p>" + displayValue(v.noteInvoice) + "</p>"
                + "  </div>"
                + "</div>";
        }

        function normalizeRootUrl(path) {
            var p = String(path || "");
            if (p.indexOf("http://") === 0 || p.indexOf("https://") === 0) {
                return p;
            }
            while (p.indexOf("//") === 0) {
                p = p.substring(1);
            }
            if (p.charAt(0) !== "/") {
                p = "/" + p;
            }
            while (p.indexOf("//") !== -1) {
                p = p.split("//").join("/");
            }
            return p;
        }

        function anyAmbulanceModalOpen() {
            return jq("#ambulance-voucher-register-dialog").hasClass("is-open")
                || jq("#ambulance-voucher-preview-dialog").hasClass("is-open")
                || jq("#ambulance-voucher-create-dialog").hasClass("is-open")
                || jq("#ambulance-voucher-hie-search-dialog").hasClass("is-open")
                || jq("#ambulance-voucher-transfer-preview-dialog").is(":visible");
        }

        function closePreview() {
            jq("#ambulance-voucher-preview-dialog").hide().removeClass("is-open");
            jq("#ambulance-voucher-preview-overlay").hide();
            if (!anyAmbulanceModalOpen()) {
                jq("body").removeClass("ambulance-voucher-preview-open");
            }
        }

        function showPreview(html) {
            closeHieSearchDialog();
            closeCreateVoucherDialog();
            closeTransferPreviewDialog();
            jq("#ambulance-voucher-preview-body").html(html);
            jq("#ambulance-voucher-preview-overlay").show();
            jq("#ambulance-voucher-preview-dialog").addClass("is-open").show().css("display", "flex");
            jq("body").addClass("ambulance-voucher-preview-open");
        }

        function closeTransferPreviewDialog() {
            jq("#ambulance-voucher-transfer-preview-dialog").hide();
            jq("#ambulance-voucher-transfer-preview-overlay").hide();
            if (!anyAmbulanceModalOpen()) {
                jq("body").removeClass("ambulance-voucher-preview-open");
            }
        }

        function showTransferPreviewDialog() {
            var $dialog = jq("#ambulance-voucher-transfer-preview-dialog");
            if (!$dialog.length) {
                return;
            }
            if ($dialog.parent()[0] !== document.body) {
                $dialog.appendTo(document.body);
            }
            closeHieSearchDialog();
            closeCreateVoucherDialog();
            closePreview();
            jq("#ambulance-voucher-transfer-preview-overlay").show();
            $dialog.css({
                display: "flex",
                position: "fixed",
                top: "50%",
                left: "50%",
                transform: "translate(-50%, -50%)",
                zIndex: 20001
            }).show();
            jq("body").addClass("ambulance-voucher-preview-open");
        }

        function ensureTransferPreviewRenderer(callback) {
            if (typeof ensureTransferPreviewAssets === "function") {
                ensureTransferPreviewAssets(callback);
                return;
            }
            if (typeof buildTransferFormPreviewHtml === "function") {
                callback();
                return;
            }
            if (transferPreviewScriptsLoading) {
                transferPreviewScriptsLoading.done(callback);
                return;
            }
            var base = normalizeRootUrl(transferPreviewResourcesBase);
            if (base.charAt(base.length - 1) !== "/") {
                base += "/";
            }
            transferPreviewScriptsLoading = jq.getScript(base + "transferPreviewCommon.js").done(callback);
        }

        function loadTransferPreview(uuid) {
            if (!uuid) {
                return;
            }
            jq("#ambulance-voucher-transfer-preview-body").html(
                "<p class='bon-loading'><i class='icon-spinner icon-spin'></i> "
                + escapeHtml(filterMessages.previewTransferLoading || "Loading transfer…")
                + "</p>"
            );
            showTransferPreviewDialog();
            jq.ajax({
                url: normalizeRootUrl(transferPreviewUrl),
                type: "GET",
                dataType: "json",
                data: { uuid: uuid },
                timeout: 30000
            }).done(function(response) {
                if (response && response.status === "success" && response.transfer) {
                    ensureTransferPreviewRenderer(function() {
                        if (typeof renderTransferPreviewInto === "function") {
                            renderTransferPreviewInto("#ambulance-voucher-transfer-preview-body", response.transfer);
                            return;
                        }
                        var previewHtml = buildTransferFormPreviewHtml(response.transfer);
                        jq("#ambulance-voucher-transfer-preview-body").html(previewHtml);
                    });
                    return;
                }
                var message = (response && response.message)
                    ? response.message
                    : (filterMessages.previewTransferError || "Unable to load transfer form.");
                jq("#ambulance-voucher-transfer-preview-body").html(
                    "<p class='bon-error'>" + escapeHtml(message) + "</p>"
                );
            }).fail(function() {
                jq("#ambulance-voucher-transfer-preview-body").html(
                    "<p class='bon-error'>"
                    + escapeHtml(filterMessages.previewTransferError || "Unable to load transfer form.")
                    + "</p>"
                );
            });
        }

        function printTransferPreview() {
            var $body = jq("#ambulance-voucher-transfer-preview-body");
            if (!$body.length || !$body.html()) {
                return;
            }
            if (typeof exportTransferFormPreviewPdf === "function") {
                exportTransferFormPreviewPdf($body, {
                    fileName: filterMessages.previewTransferTitle || "Transfer-form"
                });
                return;
            }
            ensureTransferPreviewRenderer(function() {
                if (typeof exportTransferFormPreviewPdf === "function") {
                    exportTransferFormPreviewPdf($body, {
                        fileName: filterMessages.previewTransferTitle || "Transfer-form"
                    });
                }
            });
        }

        function cellText(value) {
            if (value == null) {
                return "";
            }
            var tmp = jq("<div/>").html(String(value));
            return jq.trim(tmp.text());
        }

        function collectVoucherListRows() {
            var rows = [];
            var $table = jq("#transfer-ambulance-voucher-table");
            if (!$table.length) {
                return rows;
            }
            // Use the full DataTables dataset (all server-returned rows), ignoring
            // client-side search filter and pagination.
            if (jq.fn.dataTable && jq.fn.dataTable.fnIsDataTable
                    && jq.fn.dataTable.fnIsDataTable($table[0])) {
                var allData = $table.dataTable().fnGetData() || [];
                jq.each(allData, function(_, data) {
                    if (!data || !data.length) {
                        return;
                    }
                    rows.push({
                        number: cellText(data[0]),
                        date: cellText(data[1]),
                        upid: cellText(data[2]),
                        patient: cellText(data[3]),
                        from: cellText(data[4]),
                        destination: cellText(data[5]),
                        distance: cellText(data[6]),
                        amount: cellText(data[7])
                    });
                });
                return rows;
            }
            $table.find("tbody tr").each(function() {
                var cells = jq(this).find("td");
                if (cells.length < 8) {
                    return;
                }
                rows.push({
                    number: cellText(cells.eq(0).html()),
                    date: cellText(cells.eq(1).html()),
                    upid: cellText(cells.eq(2).html()),
                    patient: cellText(cells.eq(3).html()),
                    from: cellText(cells.eq(4).html()),
                    destination: cellText(cells.eq(5).html()),
                    distance: cellText(cells.eq(6).html()),
                    amount: cellText(cells.eq(7).html())
                });
            });
            return rows;
        }

        function printVoucherListPdf() {
            var rows = collectVoucherListRows();
            if (!rows.length) {
                window.alert(filterMessages.printListEmpty
                    || "No vouchers to print for the selected date range.");
                return;
            }
            var title = filterMessages.printListTitle || "Ambulance Voucher List";
            var periodLabel = filterMessages.printListPeriod || "Period";
            var generatedLabel = filterMessages.printListGenerated || "Generated";
            var startDate = filterConfig.startDate || "";
            var endDate = filterConfig.endDate || "";
            var period = startDate && endDate ? (startDate + " — " + endDate) : (startDate || endDate || "");
            var generated = new Date().toLocaleString();
            var bodyRows = "";
            jq.each(rows, function(i, row) {
                bodyRows += "<tr>"
                    + "<td>" + escapeHtml(row.number || String(i + 1)) + "</td>"
                    + "<td>" + escapeHtml(row.date) + "</td>"
                    + "<td>" + escapeHtml(row.upid) + "</td>"
                    + "<td>" + escapeHtml(row.patient) + "</td>"
                    + "<td>" + escapeHtml(row.from) + "</td>"
                    + "<td>" + escapeHtml(row.destination) + "</td>"
                    + "<td class='num'>" + escapeHtml(row.distance) + "</td>"
                    + "<td class='num'>" + escapeHtml(row.amount) + "</td>"
                    + "</tr>";
            });
            var html = "<!DOCTYPE html><html><head><meta charset='utf-8'/>"
                + "<title>" + escapeHtml(title) + "</title>"
                + "<style>"
                + "@page{size:A4 portrait;margin:12mm 10mm;}"
                + "html,body{margin:0;padding:0;background:#fff;color:#12332f;"
                + "font-family:'Segoe UI','Helvetica Neue',Arial,sans-serif;font-size:11pt;}"
                + ".sheet{width:100%;}"
                + "h1{margin:0 0 6px;font-size:16pt;color:#0b4f49;}"
                + ".meta{margin:0 0 14px;font-size:10pt;color:#334155;}"
                + ".meta div{margin:2px 0;}"
                + "table{width:100%;border-collapse:collapse;table-layout:fixed;}"
                + "th,td{border:1px solid #94a3b8;padding:5px 6px;vertical-align:top;word-wrap:break-word;}"
                + "th{background:#e8f4f1;color:#0b4f49;font-size:9pt;text-align:left;}"
                + "td{font-size:9pt;}"
                + "td.num{text-align:right;white-space:nowrap;}"
                + "@media print{body{-webkit-print-color-adjust:exact;print-color-adjust:exact;}"
                + ".no-print{display:none !important;}}"
                + "</style></head><body>"
                + "<p class='no-print' style='font-size:12px;color:#334155;margin:0 0 10px;'>"
                + "Use your browser print dialog and choose <strong>Save as PDF</strong> "
                + "(A4 portrait).</p>"
                + "<div class='sheet'>"
                + "<h1>" + escapeHtml(title) + "</h1>"
                + "<div class='meta'>"
                + (period ? ("<div><strong>" + escapeHtml(periodLabel) + ":</strong> "
                    + escapeHtml(period) + "</div>") : "")
                + "<div><strong>" + escapeHtml(generatedLabel) + ":</strong> "
                + escapeHtml(generated) + "</div>"
                + "<div><strong>Total:</strong> " + rows.length + "</div>"
                + "</div>"
                + "<table><thead><tr>"
                + "<th style='width:6%;'>" + escapeHtml(filterMessages.columnNumber || "#") + "</th>"
                + "<th style='width:14%;'>" + escapeHtml(filterMessages.columnDate || "Date") + "</th>"
                + "<th style='width:14%;'>" + escapeHtml(filterMessages.columnUpid || "UPID") + "</th>"
                + "<th style='width:16%;'>" + escapeHtml(filterMessages.columnPatient || "Patient") + "</th>"
                + "<th style='width:16%;'>" + escapeHtml(filterMessages.columnFrom || "From") + "</th>"
                + "<th style='width:16%;'>" + escapeHtml(filterMessages.columnDestination || "Destination") + "</th>"
                + "<th style='width:8%;'>" + escapeHtml(filterMessages.columnDistance || "Distance") + "</th>"
                + "<th style='width:10%;'>" + escapeHtml(filterMessages.columnAmount || "Amount") + "</th>"
                + "</tr></thead><tbody>" + bodyRows + "</tbody></table>"
                + "</div>"
                + "<script>window.onload=function(){setTimeout(function(){window.focus();window.print();},250);};<\/script>"
                + "</body></html>";
            var printWindow = window.open("", "_blank", "width=900,height=1100");
            if (!printWindow) {
                window.alert("Please allow pop-ups to print the voucher list.");
                return;
            }
            printWindow.document.open();
            printWindow.document.write(html);
            printWindow.document.close();
        }

        function loadPreview(uuid) {
            if (!uuid) {
                showPreview("<p class='bon-error'>" + escapeHtml(filterMessages.previewError || "Unable to load voucher") + "</p>");
                return;
            }
            closeHieSearchDialog();
            closeCreateVoucherDialog();
            closeTransferPreviewDialog();
            showPreview("<p class='bon-loading'><i class='icon-spinner icon-spin'></i> "
                + escapeHtml(filterMessages.previewLoading || "Loading…") + "</p>");
            jq.ajax({
                url: previewUrl,
                type: "GET",
                dataType: "json",
                data: { uuid: uuid },
                timeout: 30000
            }).done(function(response) {
                if (response && response.status === "success" && response.voucher) {
                    showPreview(buildVoucherHtml(response.voucher));
                } else {
                    var message = (response && response.message)
                        ? response.message
                        : (filterMessages.previewError || "Unable to load voucher");
                    showPreview("<p class='bon-error'>" + escapeHtml(message) + "</p>");
                }
            }).fail(function() {
                showPreview("<p class='bon-error'>" + escapeHtml(filterMessages.previewError || "Unable to load voucher") + "</p>");
            });
        }

        function rememberPreviewAfterReload(uuid) {
            if (!uuid) {
                return;
            }
            try {
                window.sessionStorage.setItem("transferapp.ambulanceVoucher.previewUuid", String(uuid));
            } catch (ignore) {
                // sessionStorage may be unavailable
            }
        }

        function openRememberedPreview() {
            var previewUuid = null;
            try {
                previewUuid = window.sessionStorage.getItem("transferapp.ambulanceVoucher.previewUuid");
                if (previewUuid) {
                    window.sessionStorage.removeItem("transferapp.ambulanceVoucher.previewUuid");
                }
            } catch (ignore) {
                previewUuid = null;
            }
            if (previewUuid) {
                loadPreview(previewUuid);
            }
        }

        function closeHieSearchDialog() {
            jq("#ambulance-voucher-hie-search-dialog").hide().removeClass("is-open");
            jq("#ambulance-voucher-hie-search-overlay").hide();
            if (!anyAmbulanceModalOpen()) {
                jq("body").removeClass("ambulance-voucher-preview-open");
            }
        }

        function closeRegisterDialog() {
            pendingRegisterUpid = null;
            jq("#ambulance-voucher-register-dialog").hide().removeClass("is-open");
            jq("#ambulance-voucher-register-overlay").hide();
            jq("#ambulance-voucher-register-confirm").hide().prop("disabled", false);
            jq("#ambulance-voucher-register-body").empty();
            if (!anyAmbulanceModalOpen()) {
                jq("body").removeClass("ambulance-voucher-preview-open");
            }
        }

        function closeCreateVoucherDialog() {
            pendingCreateVoucher = null;
            jq("#ambulance-voucher-create-dialog").hide().removeClass("is-open");
            jq("#ambulance-voucher-create-overlay").hide();
            jq("#ambulance-voucher-create-error").hide().text("");
            jq("#ambulance-voucher-create-district").val("");
            jq("#ambulance-voucher-create-kilometers").val("");
            jq("#ambulance-voucher-create-province").text("");
            jq("#ambulance-voucher-create-confirm").prop("disabled", false);
            if (!anyAmbulanceModalOpen()) {
                jq("body").removeClass("ambulance-voucher-preview-open");
            }
        }

        function showCreateVoucherError(message) {
            var $error = jq("#ambulance-voucher-create-error");
            if (!$error.length) {
                window.alert(message);
                return;
            }
            $error.text(message || "").show();
        }

        function formatRouteDescription(myLocation, fromFacility, toFacility) {
            var template = filterMessages.createVoucherRouteTemplate
                || "{0} via {1} to {2}";
            var myLoc = String(myLocation || "").trim() || "N/A";
            var from = String(fromFacility || "").trim() || "N/A";
            var to = String(toFacility || "").trim() || "N/A";
            return template
                .replace("{0}", myLoc)
                .replace("{1}", from)
                .replace("{2}", to);
        }

        function findHieTransferRow(hieTransferId) {
            var found = null;
            jq.each(lastHieSearchContext.transfers || [], function(_, row) {
                var rowId = row.hieTransferId || row.uuid || "";
                if (String(rowId) === String(hieTransferId)) {
                    found = row;
                    return false;
                }
            });
            return found;
        }

        function openCreateVoucherDialog(patientId, hieTransferId, $button) {
            if (!patientId || !hieTransferId) {
                return;
            }
            var transferRow = findHieTransferRow(hieTransferId) || {};
            var insuranceNumber = String(lastHieSearchContext.insuranceCardNumber || "").trim();
            var myLocation = String(lastHieSearchContext.currentFacilityName || "").trim();
            var fromFacility = String(transferRow.fromFacility || "").trim();
            var toFacility = String(transferRow.toFacility || "").trim();
            var districtDefault = String(transferRow.district || "").trim();
            var provinceFromTransfer = String(transferRow.province || "").trim();

            pendingCreateVoucher = {
                patientId: patientId,
                hieTransferId: hieTransferId,
                province: provinceFromTransfer,
                $button: $button,
                originalHtml: $button ? $button.html() : ""
            };

            jq("#ambulance-voucher-create-error").hide().text("");
            if (insuranceNumber) {
                jq("#ambulance-voucher-create-insurance").text(insuranceNumber);
            } else {
                jq("#ambulance-voucher-create-insurance").text(
                    filterMessages.createVoucherInsuranceMissing
                        || "No insurance number found on registration."
                );
            }
            jq("#ambulance-voucher-create-province").text(
                provinceFromTransfer
                    || (filterMessages.createVoucherProvinceMissing || "Not provided on transfer")
            );
            jq("#ambulance-voucher-create-district").val(districtDefault);
            jq("#ambulance-voucher-create-kilometers").val("");
            jq("#ambulance-voucher-create-route").text(
                formatRouteDescription(myLocation, fromFacility, toFacility)
            );
            jq("#ambulance-voucher-create-confirm").prop("disabled", !insuranceNumber);
            jq("#ambulance-voucher-create-overlay").show();
            jq("#ambulance-voucher-create-dialog").addClass("is-open").show().css("display", "flex");
            jq("body").addClass("ambulance-voucher-preview-open");
            setTimeout(function() {
                jq("#ambulance-voucher-create-district").focus();
            }, 50);
        }

        function showRegisterDialog(html) {
            jq("#ambulance-voucher-register-title").text(
                filterMessages.registerPreviewTitle || "Patient Registration Preview"
            );
            jq("#ambulance-voucher-register-body").html(html);
            jq("#ambulance-voucher-register-overlay").show();
            jq("#ambulance-voucher-register-dialog").addClass("is-open").show().css("display", "flex");
            jq("body").addClass("ambulance-voucher-preview-open");
        }

        function previewValue(value) {
            var text = value == null ? "" : String(value).trim();
            return text ? escapeHtml(text) : escapeHtml(filterMessages.registerNotProvided || "Not provided");
        }

        function previewField(label, value) {
            return "<div><dt>" + escapeHtml(label) + "</dt><dd>" + previewValue(value) + "</dd></div>";
        }

        function getRegistryPhotoSrc(photo) {
            if (!photo) {
                return "";
            }
            var value = String(photo).trim();
            if (!value) {
                return "";
            }
            if (value.indexOf("data:image/") === 0) {
                return value;
            }
            if (value.indexOf("/9j/") === 0) {
                return "data:image/jpeg;base64," + value;
            }
            if (value.indexOf("iVBOR") === 0) {
                return "data:image/png;base64," + value;
            }
            return value;
        }

        function buildRegistrationPreviewHtml(upid, details) {
            details = details || {};
            var photoSrc = getRegistryPhotoSrc(details.photo);
            var photoHtml = photoSrc
                ? "<img src='" + escapeHtml(photoSrc) + "' alt='"
                    + escapeHtml(filterMessages.registerPhoto || "Patient photo") + "' />"
                : "<span class='transfer-hie-preview-photo-placeholder'><i class='icon-user'></i><span>"
                    + escapeHtml(filterMessages.registerPhotoNone || "No registry photo")
                    + "</span></span>";
            return ""
                + "<div class='transfer-hie-preview'>"
                + "<header class='transfer-hie-preview-header'>"
                + "<div><h1>" + escapeHtml(filterMessages.registerPreviewTitle || "Patient Registration Preview") + "</h1>"
                + "<p>" + escapeHtml(filterMessages.registerPreviewSubtitle || "") + "</p></div>"
                + "<span class='transfer-hie-preview-upid'>" + escapeHtml(upid || "") + "</span>"
                + "</header>"
                + "<section class='transfer-hie-preview-section'><h2>"
                + escapeHtml(filterMessages.registerIdentifiers || "Identifiers") + "</h2>"
                + "<dl class='transfer-hie-preview-grid'>"
                + previewField(filterMessages.registerNationalId || "National ID", details.nationalId)
                + previewField(filterMessages.registerUpid || "UPID", details.upid || upid)
                + previewField(filterMessages.registerApplicationNumber || "Application Number", details.applicationNumber)
                + previewField(filterMessages.registerNin || "NIN", details.nin)
                + previewField(filterMessages.registerPassport || "Passport Number", details.passportNumber)
                + "</dl></section>"
                + "<section class='transfer-hie-preview-section'><h2>"
                + escapeHtml(filterMessages.registerDemographics || "Demographics") + "</h2>"
                + "<div class='transfer-hie-preview-demographics'>"
                + "<figure class='transfer-hie-preview-photo'><div class='transfer-hie-preview-photo-frame'>"
                + photoHtml + "</div><figcaption>"
                + escapeHtml(filterMessages.registerPhoto || "Patient photo")
                + "</figcaption></figure>"
                + "<dl class='transfer-hie-preview-grid'>"
                + previewField(filterMessages.registerGivenName || "Given Name", details.givenName)
                + previewField(filterMessages.registerMiddleName || "Middle Name", details.middleName)
                + previewField(filterMessages.registerFamilyName || "Family Name", details.familyName)
                + previewField(filterMessages.registerGender || "Gender", details.gender)
                + previewField(filterMessages.registerBirthdate || "Birthdate", details.birthdate)
                + "</dl></div></section>"
                + "<section class='transfer-hie-preview-section'><h2>"
                + escapeHtml(filterMessages.registerContact || "Contact and Address") + "</h2>"
                + "<dl class='transfer-hie-preview-grid'>"
                + previewField(filterMessages.registerPhone || "Phone Number", details.phoneNumber)
                + previewField(filterMessages.registerCountry || "Country", details.country)
                + previewField(filterMessages.registerProvince || "Province", details.stateProvince)
                + previewField(filterMessages.registerDistrict || "District", details.countyDistrict)
                + previewField(filterMessages.registerSector || "Sector", details.cityVillage)
                + previewField(filterMessages.registerCell || "Cell", details.address3)
                + previewField(filterMessages.registerVillage || "Village", details.address1)
                + "</dl></section>"
                + "<section class='transfer-hie-preview-section'><h2>"
                + escapeHtml(filterMessages.registerAttributes || "Additional Information") + "</h2>"
                + "<dl class='transfer-hie-preview-grid'>"
                + previewField(filterMessages.registerMothersName || "Mother's Name", details.mothersName)
                + previewField(filterMessages.registerFathersName || "Father's Name", details.fathersName)
                + previewField(filterMessages.registerEducation || "Education Level", details.educationLevel)
                + previewField(filterMessages.registerProfession || "Profession", details.profession)
                + previewField(filterMessages.registerReligion || "Religion", details.religion)
                + "</dl></section>"
                + "</div>";
        }

        function openRegistrationPreviewModal(upid) {
            if (!upid) {
                return;
            }
            closeHieSearchDialog();
            pendingRegisterUpid = upid;
            jq("#ambulance-voucher-register-confirm").hide().prop("disabled", true);
            showRegisterDialog(
                "<p class='bon-loading'><i class='icon-spinner icon-spin'></i> "
                + escapeHtml(filterMessages.registerPreviewLoading || "Loading patient registration preview…")
                + "</p>"
            );
            jq.ajax({
                url: registerPreviewUrl,
                type: "GET",
                dataType: "json",
                data: { upid: upid },
                timeout: 60000
            }).done(function(response) {
                if (response && response.status === "success") {
                    showRegisterDialog(buildRegistrationPreviewHtml(response.upid || upid, response.patientDetails));
                    jq("#ambulance-voucher-register-confirm").show().prop("disabled", false);
                    return;
                }
                var message = (response && response.message)
                    ? response.message
                    : (filterMessages.registerPreviewError || "Unable to load patient registration preview.");
                showRegisterDialog("<p class='bon-error'>" + escapeHtml(message) + "</p>");
            }).fail(function() {
                showRegisterDialog(
                    "<p class='bon-error'>"
                    + escapeHtml(filterMessages.registerPreviewError || "Unable to load patient registration preview.")
                    + "</p>"
                );
            });
        }

        function confirmRegistrationFromModal() {
            if (!pendingRegisterUpid) {
                return;
            }
            var $confirm = jq("#ambulance-voucher-register-confirm");
            $confirm.prop("disabled", true).html(
                "<i class='icon-spinner icon-spin'></i> "
                + escapeHtml(filterMessages.registerPreviewLoading || "Loading…")
            );
            jq.ajax({
                url: registerConfirmUrl,
                type: "POST",
                dataType: "json",
                data: { upid: pendingRegisterUpid },
                timeout: 90000
            }).done(function(response) {
                if (response && response.status === "success" && response.redirectUrl) {
                    var redirectUrl = response.redirectUrl;
                    if (redirectUrl.indexOf("http") !== 0 && redirectUrl.charAt(0) === "/") {
                        redirectUrl = (window.transferOpenmrsPath || "") + redirectUrl;
                    }
                    window.location.href = redirectUrl;
                    return;
                }
                var message = (response && response.message)
                    ? response.message
                    : (filterMessages.registerPreviewError || "Unable to register patient.");
                window.alert(message);
                $confirm.prop("disabled", false).html(
                    "<i class='icon-ok'></i> "
                    + escapeHtml(filterMessages.registerConfirm || "Confirm Registration")
                );
            }).fail(function() {
                window.alert(filterMessages.registerPreviewError || "Unable to register patient.");
                $confirm.prop("disabled", false).html(
                    "<i class='icon-ok'></i> "
                    + escapeHtml(filterMessages.registerConfirm || "Confirm Registration")
                );
            });
        }

        function showHieSearchDialog(html, title) {
            if (title) {
                jq("#ambulance-voucher-hie-search-title").text(title);
            }
            jq("#ambulance-voucher-hie-search-body").html(html);
            jq("#ambulance-voucher-hie-search-overlay").show();
            jq("#ambulance-voucher-hie-search-dialog").addClass("is-open").show().css("display", "flex");
            jq("body").addClass("ambulance-voucher-preview-open");
        }

        function buildHieTransfersTable(context) {
            var transfers = context.transfers || [];
            var upid = context.upid || "";
            var identifierKind = context.identifierKind || "";
            var existingPatient = !!context.existingPatient;
            var patientBadgeClass = existingPatient
                ? "transfer-patient-badge transfer-patient-badge-existing"
                : "transfer-patient-badge transfer-patient-badge-new";
            var patientBadgeLabel = existingPatient
                ? (filterMessages.patientExisting || "Existing")
                : (filterMessages.patientNew || "New");
            var workflowHtml = "";
            if (!existingPatient && upid) {
                workflowHtml = " <button type='button' class='button confirm transfer-pending-workflow-button js-open-hie-register-modal'"
                    + " data-upid='" + escapeHtml(upid) + "'>"
                    + "<i class='icon-user'></i> "
                    + escapeHtml(filterMessages.registerPatient || "Register")
                    + "</button>";
            }
            var meta = "<div class='ambulance-voucher-hie-search-meta'>"
                + "<div><strong>UPID:</strong> " + escapeHtml(upid || "")
                + (identifierKind ? (" &nbsp;|&nbsp; <strong>Lookup:</strong> " + escapeHtml(identifierKind)) : "")
                + " <span class='" + patientBadgeClass + "'>" + escapeHtml(patientBadgeLabel) + "</span>"
                + workflowHtml
                + "</div></div>";
            if (!transfers.length) {
                return meta + "<p>" + escapeHtml(filterMessages.searchEmpty || "No transfers found for this patient in HIE.") + "</p>";
            }
            var rows = "";
            jq.each(transfers, function(_, row) {
                var providerLabel = displayValue(row.ambulanceProviderName || row.ambulanceProviderFosaId);
                if (row.ambulanceProviderMatchesCurrent === true || row.ambulanceProviderMatchesCurrent === "true") {
                    providerLabel += " <span class='transfer-patient-badge transfer-patient-badge-existing'>"
                        + escapeHtml(filterMessages.ambulanceProviderOurs || "Our facility")
                        + "</span>";
                }
                var actionHtml = "—";
                var hieTransferId = row.hieTransferId || row.uuid || "";
                var hasVoucher = row.hasAmbulanceVoucher === true || row.hasAmbulanceVoucher === "true";
                var canCreate = row.canCreateAmbulanceVoucher === true || row.canCreateAmbulanceVoucher === "true";
                if (hasVoucher && row.localTransferUuid) {
                    actionHtml = "<a class='button transfer-ambulance-voucher-preview' href='javascript:void(0);' data-uuid='"
                        + escapeHtml(row.localTransferUuid)
                        + "' title='" + escapeHtml(filterMessages.previewPrint ? filterMessages.previewLoading : "Preview") + "'>"
                        + "<i class='icon-eye-open'></i> "
                        + escapeHtml(filterMessages.previewAction || "Preview")
                        + "</a>";
                } else if (canCreate && context.patientId && hieTransferId) {
                    actionHtml = "<button type='button' class='button confirm js-create-ambulance-voucher-from-hie'"
                        + " data-patient-id='" + escapeHtml(String(context.patientId)) + "'"
                        + " data-hie-transfer-id='" + escapeHtml(String(hieTransferId)) + "'>"
                        + "<i class='icon-file'></i> "
                        + escapeHtml(filterMessages.createVoucher || "Create voucher")
                        + "</button>";
                }
                rows += "<tr>"
                    + "<td>" + displayValue(row.date) + "</td>"
                    + "<td>" + displayValue(row.fromFacility) + "</td>"
                    + "<td>" + displayValue(row.fromService) + "</td>"
                    + "<td>" + displayValue(row.toFacility) + "</td>"
                    + "<td>" + displayValue(row.toService) + "</td>"
                    + "<td>" + displayValue(row.clinician) + "</td>"
                    + "<td>" + providerLabel + "</td>"
                    + "<td>" + actionHtml + "</td>"
                    + "</tr>";
            });
            return meta
                + "<div class='transfer-table-wrapper'>"
                + "<table class='ambulance-voucher-hie-search-table'>"
                + "<thead><tr>"
                + "<th>Date</th>"
                + "<th>From</th>"
                + "<th>From Service</th>"
                + "<th>To facility</th>"
                + "<th>To service</th>"
                + "<th>Clinician</th>"
                + "<th>" + escapeHtml(filterMessages.ambulanceProviderColumn || "Ambulance provider") + "</th>"
                + "<th>" + escapeHtml(filterMessages.actionColumn || "Action") + "</th>"
                + "</tr></thead>"
                + "<tbody>" + rows + "</tbody>"
                + "</table></div>";
        }

        function refreshHieSearchDialog() {
            showHieSearchDialog(
                buildHieTransfersTable(lastHieSearchContext),
                filterMessages.searchTitle || "HIE transfers"
            );
        }

        function createAmbulanceVoucherFromHie(patientId, hieTransferId, $button) {
            openCreateVoucherDialog(patientId, hieTransferId, $button);
        }

        function submitCreateVoucherFromModal() {
            if (!pendingCreateVoucher || !pendingCreateVoucher.patientId || !pendingCreateVoucher.hieTransferId) {
                return;
            }
            jq("#ambulance-voucher-create-error").hide().text("");
            var district = String(jq("#ambulance-voucher-create-district").val() || "").trim();
            if (!district) {
                showCreateVoucherError(filterMessages.createVoucherDistrictRequired
                    || "Enter the district that will be covered.");
                jq("#ambulance-voucher-create-district").focus();
                return;
            }
            var rawDistance = String(jq("#ambulance-voucher-create-kilometers").val() || "").trim();
            var kilometers = parseInt(rawDistance, 10);
            if (!kilometers || kilometers <= 0 || isNaN(kilometers)) {
                showCreateVoucherError(filterMessages.createVoucherDistanceInvalid
                    || "Enter a distance greater than zero (kilometers).");
                jq("#ambulance-voucher-create-kilometers").focus();
                return;
            }

            var patientId = pendingCreateVoucher.patientId;
            var hieTransferId = pendingCreateVoucher.hieTransferId;
            var province = String(pendingCreateVoucher.province || "").trim();
            var $button = pendingCreateVoucher.$button;
            var originalHtml = pendingCreateVoucher.originalHtml;
            var $confirm = jq("#ambulance-voucher-create-confirm");
            $confirm.prop("disabled", true).html(
                "<i class='icon-spinner icon-spin'></i> "
                + escapeHtml(filterMessages.createVoucherLoading || "Creating voucher…")
            );
            if ($button && $button.length) {
                $button.prop("disabled", true).html(
                    "<i class='icon-spinner icon-spin'></i> "
                    + escapeHtml(filterMessages.createVoucherLoading || "Creating voucher…")
                );
            }

            jq.ajax({
                url: createVoucherUrl,
                type: "POST",
                dataType: "json",
                data: {
                    patientId: patientId,
                    hieTransferId: hieTransferId,
                    kilometers: kilometers,
                    district: district,
                    province: province
                },
                timeout: 90000
            }).done(function(response) {
                if (response && response.status === "success") {
                    closeCreateVoucherDialog();
                    closeHieSearchDialog();
                    if (response.uuid) {
                        rememberPreviewAfterReload(response.uuid);
                    }
                    window.location.reload();
                    return;
                }
                var message = (response && response.message)
                    ? response.message
                    : (filterMessages.createVoucherError || "Unable to create ambulance voucher.");
                showCreateVoucherError(message);
                $confirm.prop("disabled", false).html(
                    "<i class='icon-ok'></i> "
                    + escapeHtml(filterMessages.createVoucherConfirm || "Create voucher")
                );
                if ($button && $button.length) {
                    $button.prop("disabled", false).html(originalHtml);
                }
            }).fail(function() {
                showCreateVoucherError(filterMessages.createVoucherError || "Unable to create ambulance voucher.");
                $confirm.prop("disabled", false).html(
                    "<i class='icon-ok'></i> "
                    + escapeHtml(filterMessages.createVoucherConfirm || "Create voucher")
                );
                if ($button && $button.length) {
                    $button.prop("disabled", false).html(originalHtml);
                }
            });
        }

        function searchHieTransfers() {
            clearSearchError();
            var identifier = jq("#ambulance-voucher-identifier").val();
            if (!detectIdentifierKind(identifier)) {
                showSearchError(filterMessages.searchInvalid
                    || "Enter a 16-digit National ID or a UPID in 6-4-4 format.");
                return;
            }
            var searchBtn = jq("#ambulance-voucher-identifier-search");
            searchBtn.prop("disabled", true);
            showHieSearchDialog(
                "<p class='bon-loading'><i class='icon-spinner icon-spin'></i> "
                + escapeHtml(filterMessages.searchLoading || "Searching HIE transfers…") + "</p>",
                filterMessages.searchTitle || "HIE transfers"
            );
            jq.ajax({
                url: hieSearchUrl,
                type: "GET",
                dataType: "json",
                data: { identifier: String(identifier).trim() },
                timeout: 60000
            }).done(function(response) {
                if (response && response.status === "success") {
                    lastHieSearchContext = {
                        patientId: response.patientId || null,
                        upid: response.upid || "",
                        identifierKind: response.identifierKind || "",
                        existingPatient: response.existingPatient === true || response.existingPatient === "true",
                        hasInsuranceOnRegistration: response.hasInsuranceOnRegistration === true
                            || response.hasInsuranceOnRegistration === "true",
                        insuranceCardNumber: response.insuranceCardNumber || "",
                        currentFacilityName: response.currentFacilityName || "",
                        transfers: response.transfers || []
                    };
                    showHieSearchDialog(
                        buildHieTransfersTable(lastHieSearchContext),
                        filterMessages.searchTitle || "HIE transfers"
                    );
                    return;
                }
                var message = (response && response.message)
                    ? response.message
                    : (filterMessages.searchError || "Unable to search HIE transfers.");
                showSearchError(message);
                showHieSearchDialog("<p class='bon-error'>" + escapeHtml(message) + "</p>");
            }).fail(function() {
                var message = filterMessages.searchError || "Unable to search HIE transfers.";
                showSearchError(message);
                showHieSearchDialog("<p class='bon-error'>" + escapeHtml(message) + "</p>");
            }).always(function() {
                searchBtn.prop("disabled", false);
            });
        }

        if (filterForm.length && typeof flatpickr === "function") {
            if (startInput) {
                flatpickr(startInput, {
                    dateFormat: "Y-m-d",
                    allowInput: true,
                    defaultDate: filterConfig.startDate || null
                });
            }
            if (endInput) {
                flatpickr(endInput, {
                    dateFormat: "Y-m-d",
                    allowInput: true,
                    defaultDate: filterConfig.endDate || null
                });
            }
            filterForm.on("submit", function(e) {
                if (!validateDateRange(
                        jq("#ambulance-voucher-filter-start-date").val(),
                        jq("#ambulance-voucher-filter-end-date").val())) {
                    e.preventDefault();
                }
            });
        }

        if (jq.fn.dataTable && jq("#transfer-ambulance-voucher-table").length) {
            jq("#transfer-ambulance-voucher-table").dataTable({
                bFilter: true,
                bInfo: true,
                bPaginate: true,
                bLengthChange: true,
                sPaginationType: "full_numbers",
                iDisplayLength: 25,
                aLengthMenu: [[10, 25, 50, 100, -1], [10, 25, 50, 100, "All"]],
                aaSorting: [[1, "desc"]],
                aoColumnDefs: [
                    { bSortable: false, aTargets: [8] }
                ],
                oLanguage: {
                    sSearch: "Filter:",
                    oPaginate: {
                        sFirst: "First",
                        sPrevious: "Previous",
                        sNext: "Next",
                        sLast: "Last"
                    }
                }
            });
        }

        jq(document).on("click", ".ambulance-voucher-preview-link, .transfer-ambulance-voucher-preview", function(e) {
            e.preventDefault();
            loadPreview(jq(this).attr("data-uuid"));
        });

        jq(document).on("click", ".ambulance-voucher-transfer-preview-link", function(e) {
            e.preventDefault();
            loadTransferPreview(jq(this).attr("data-uuid"));
        });

        jq(document).on("click", "#ambulance-voucher-print-list", function(e) {
            e.preventDefault();
            printVoucherListPdf();
        });

        jq(document).on("click", "#ambulance-voucher-transfer-preview-close, #ambulance-voucher-transfer-preview-overlay", function(e) {
            e.preventDefault();
            closeTransferPreviewDialog();
        });

        jq(document).on("click", "#ambulance-voucher-transfer-preview-print", function(e) {
            e.preventDefault();
            printTransferPreview();
        });

        jq(document).on("click", ".js-create-ambulance-voucher-from-hie", function(e) {
            e.preventDefault();
            var $button = jq(this);
            createAmbulanceVoucherFromHie(
                $button.attr("data-patient-id"),
                $button.attr("data-hie-transfer-id"),
                $button
            );
        });

        jq(document).on("click", "#ambulance-voucher-create-cancel, #ambulance-voucher-create-overlay", function(e) {
            e.preventDefault();
            if (pendingCreateVoucher && pendingCreateVoucher.$button && pendingCreateVoucher.$button.length) {
                pendingCreateVoucher.$button.prop("disabled", false).html(pendingCreateVoucher.originalHtml);
            }
            closeCreateVoucherDialog();
        });

        jq(document).on("click", "#ambulance-voucher-create-confirm", function(e) {
            e.preventDefault();
            submitCreateVoucherFromModal();
        });

        jq(document).on("keydown", "#ambulance-voucher-create-district, #ambulance-voucher-create-kilometers", function(e) {
            if (e.key === "Enter" || e.keyCode === 13) {
                e.preventDefault();
                submitCreateVoucherFromModal();
            }
        });

        jq(document).on("click", "#ambulance-voucher-preview-close, #ambulance-voucher-preview-overlay", function(e) {
            e.preventDefault();
            closePreview();
        });

        jq(document).on("click", "#ambulance-voucher-identifier-search", function(e) {
            e.preventDefault();
            searchHieTransfers();
        });

        jq(document).on("keydown", "#ambulance-voucher-identifier", function(e) {
            if (e.key === "Enter" || e.keyCode === 13) {
                e.preventDefault();
                searchHieTransfers();
            }
        });

        jq(document).on("click", ".js-open-hie-register-modal", function(e) {
            e.preventDefault();
            openRegistrationPreviewModal(jq(this).attr("data-upid"));
        });

        jq(document).on("click", "#ambulance-voucher-register-cancel, #ambulance-voucher-register-overlay", function(e) {
            e.preventDefault();
            closeRegisterDialog();
        });

        jq(document).on("click", "#ambulance-voucher-register-confirm", function(e) {
            e.preventDefault();
            confirmRegistrationFromModal();
        });

        jq(document).on("click", "#ambulance-voucher-hie-search-close, #ambulance-voucher-hie-search-overlay", function(e) {
            e.preventDefault();
            closeHieSearchDialog();
        });

        jq(document).on("click", "#ambulance-voucher-preview-print", function(e) {
            e.preventDefault();
            var content = jq("#ambulance-voucher-preview-body").html();
            if (!content) {
                return;
            }
            var printWindow = window.open("", "_blank", "width=900,height=1000");
            if (!printWindow) {
                window.print();
                return;
            }
            printWindow.document.write("<!DOCTYPE html><html><head><title>BON D'AMBULANCE</title>");
            printWindow.document.write("<link rel='stylesheet' href='"
                + (window.transferOpenmrsPath || "")
                + "/moduleResources/transferapp/styles/ambulanceVoucherPreview.css' />");
            printWindow.document.write("</head><body class='bon-print-body'>");
            printWindow.document.write(content);
            printWindow.document.write("</body></html>");
            printWindow.document.close();
            printWindow.focus();
            setTimeout(function() {
                printWindow.print();
            }, 300);
        });

        openRememberedPreview();
    });
}());
