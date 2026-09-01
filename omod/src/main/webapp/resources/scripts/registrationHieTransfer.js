(function(jq) {
    if (window.__registrationHieTransferBound) {
        return;
    }
    window.__registrationHieTransferBound = true;

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

    function esc(value) {
        if (value === null || value === undefined) {
            return "";
        }
        return String(value)
            .split("&").join("&amp;")
            .split("<").join("&lt;")
            .split(">").join("&gt;")
            .split("\"").join("&quot;");
    }

    function resolveDestination(item) {
        if (!item) {
            return "";
        }
        return item.destinationDisplay || item.destination || item.receivingFacility || item.hospitalName || "";
    }

    function targetsCurrentFacility(item) {
        if (!item) {
            return false;
        }
        return item.targetsCurrentFacility === true || item.targetsCurrentFacility === "true";
    }

    function resolveTransferIdInput() {
        var selectors = [
            "#hie_transfer_id input[type='text']",
            "#hie_transfer_id input",
            "#hie_transfer_id",
            "#hie_transfer_id\\.value",
            "input[name='hie_transfer_id']",
            "input[name='hie_transfer_id.value']"
        ];
        for (var i = 0; i < selectors.length; i++) {
            var target = jq(selectors[i]);
            if (target.length) {
                return target.first();
            }
        }
        return null;
    }

    function loadPreviewStylesheet() {
        if (jq("#registration-transfer-preview-css").length) {
            return;
        }
        var transferOpenmrsPath = (typeof openmrsContextPath !== "undefined" ? openmrsContextPath : "/openmrs");
        var href = normalizeRootUrl(transferOpenmrsPath + "/moduleResources/transferapp/styles/transferFormPreview.css");
        var link = document.createElement("link");
        link.id = "registration-transfer-preview-css";
        link.rel = "stylesheet";
        link.type = "text/css";
        link.href = href;
        document.getElementsByTagName("head")[0].appendChild(link);
    }

    function initRegistrationHieTransfer() {
        loadPreviewStylesheet();
        var container = jq("#transfer_from_hie");
        if (!container.length) {
            return;
        }

        var upid = jq(".patientUpid").text();
        if (upid) {
            upid = upid.trim();
        }
        if (!upid) {
            upid = jq(".patientUpid").html();
            if (upid) {
                upid = upid.replace(/<[^>]*>/g, "").trim();
            }
        }

        var transferOpenmrsPath = (typeof openmrsContextPath !== "undefined" ? openmrsContextPath : "/openmrs");
        var restUrl = normalizeRootUrl(transferOpenmrsPath + "/ws/rest/v1/transferapp/transfer");
        var previewDialog = null;
        var currentPreviewTransfer = null;

        function ensurePreviewModal() {
            if (jq("#registration-hie-transfer-preview-dialog").length) {
                return;
            }
            jq("body").append(
                "<div id='registration-hie-transfer-preview-dialog' class='dialog transfer-preview-dialog' style='display:none;'>"
                + "<div class='dialog-header'><i class='icon-random'></i><h3>Available Transfer Preview</h3></div>"
                + "<div class='dialog-content'>"
                + "<div id='registration-hie-transfer-preview-body'></div>"
                + "<div class='transfer-preview-actions'>"
                + "<button type='button' id='registration-hie-transfer-validate-btn' class='confirm' style='display:none;'>Yes Transfer is valid</button>"
                + "<button type='button' id='registration-hie-transfer-export-pdf-btn' class='confirm' style='display:none;'>Export PDF</button>"
                + "<span id='registration-hie-transfer-validate-status' class='hie-transfer-status' style='display:none;'></span>"
                + "<button type='button' id='registration-hie-transfer-preview-close' class='cancel'>Close</button>"
                + "</div></div></div>"
            );
            if (!jq("#registration-hie-transfer-preview-styles").length) {
                jq("head").append(
                    "<style id='registration-hie-transfer-preview-styles'>"
                    + "#registration-hie-transfer-preview-dialog.transfer-preview-dialog.dialog{position:fixed;top:50%;left:50%;transform:translate(-50%,-50%);z-index:10001;background:#fff;border:1px solid #00473f;border-radius:4px;box-shadow:0 4px 20px rgba(0,0,0,.3);max-width:95%;max-height:92vh;width:1400px;display:flex;flex-direction:column;margin:0;}"
                    + "#registration-hie-transfer-preview-dialog .dialog-header{padding:15px 20px;border-bottom:1px solid #00473f;background:#00473f;border-radius:4px 4px 0 0;color:#fff;}"
                    + "#registration-hie-transfer-preview-dialog .dialog-header h3{margin:0;font-size:18px;font-weight:bold;color:#fff;}"
                    + "#registration-hie-transfer-preview-dialog .dialog-content{padding:20px;overflow-y:auto;flex:1;min-height:0;}"
                    + "#registration-hie-transfer-preview-body{max-height:70vh;overflow-y:auto;margin-bottom:15px;}"
                    + "#registration-hie-transfer-preview-dialog .transfer-preview-actions{display:flex;align-items:center;gap:10px;flex-wrap:wrap;}"
                    + "#registration-hie-transfer-validate-btn.confirm{background:#0f766e;color:#fff;border:1px solid #0d9488;padding:8px 14px;border-radius:4px;font-weight:600;cursor:pointer;}"
                    + "#registration-hie-transfer-export-pdf-btn.confirm{background:#1d4ed8;color:#fff;border:1px solid #1e40af;padding:8px 14px;border-radius:4px;font-weight:600;cursor:pointer;}"
                    + "a.registration-hie-transfer-view-link{display:inline-flex;align-items:center;color:#0f766e;font-weight:600;text-decoration:none;}"
                    + "a.registration-hie-transfer-view-link:hover{color:#0d9488;text-decoration:underline;}"
                    + "</style>"
                );
            }
        }

        function showPreviewDialog() {
            ensurePreviewModal();
            var dialogEl = jq("#registration-hie-transfer-preview-dialog");
            if (previewDialog == null && typeof emr !== "undefined" && typeof emr.setupConfirmationDialog === "function") {
                previewDialog = emr.setupConfirmationDialog({
                    selector: "#registration-hie-transfer-preview-dialog",
                    actions: {
                        confirm: function() {},
                        cancel: function() {
                            dialogEl.hide();
                        }
                    }
                });
                if (previewDialog && typeof previewDialog.close === "function") {
                    previewDialog.close();
                }
            }
            if (previewDialog && typeof previewDialog.show === "function") {
                previewDialog.show();
            } else {
                dialogEl.show();
            }
        }

        function hidePreviewDialog() {
            if (previewDialog && typeof previewDialog.close === "function") {
                previewDialog.close();
            }
            jq("#registration-hie-transfer-preview-dialog").hide();
        }

        function getCurrentTransferIdFieldValue() {
            var target = resolveTransferIdInput();
            if (!target || !target.length) {
                return "";
            }
            return String(target.val() || "").trim();
        }

        function isPreviewTransferValidated(transfer) {
            if (!transfer) {
                return false;
            }
            var transferUuid = String(transfer.uuid || transfer.id || transfer.hieTransferId || "").trim();
            if (!transferUuid) {
                return false;
            }
            var fieldValue = getCurrentTransferIdFieldValue();
            return fieldValue.length > 0 && fieldValue.toLowerCase() === transferUuid.toLowerCase();
        }

        function updateValidateButton(transfer) {
            ensurePreviewModal();
            var validateBtn = jq("#registration-hie-transfer-validate-btn");
            var exportBtn = jq("#registration-hie-transfer-export-pdf-btn");
            var validateStatus = jq("#registration-hie-transfer-validate-status");
            validateStatus.hide().text("");
            currentPreviewTransfer = transfer || null;

            var validated = isPreviewTransferValidated(transfer);
            if (validated) {
                exportBtn.show().prop("disabled", false);
                validateBtn.hide();
                return;
            }

            exportBtn.hide();
            if (!transfer || !targetsCurrentFacility(transfer)) {
                validateBtn.hide();
                return;
            }
            validateBtn.show().prop("disabled", false);
        }

        function exportCurrentPreviewPdf() {
            if (!currentPreviewTransfer || !isPreviewTransferValidated(currentPreviewTransfer)) {
                return;
            }
            var transferUuid = currentPreviewTransfer.uuid
                || currentPreviewTransfer.id
                || currentPreviewTransfer.hieTransferId
                || "transfer";
            var ok = typeof exportTransferFormPreviewPdf === "function"
                && exportTransferFormPreviewPdf("#registration-hie-transfer-preview-body", {
                    fileName: "External-Transfer-Form-" + transferUuid
                });
            if (!ok) {
                jq("#registration-hie-transfer-validate-status").show().css("color", "#a94442")
                    .text("Unable to open PDF export. Allow pop-ups and try again.");
            }
        }

        function renderPreview(transfer) {
            if (typeof renderTransferPreviewInto === "function") {
                renderTransferPreviewInto("#registration-hie-transfer-preview-body", transfer, function() {
                    updateValidateButton(transfer);
                });
                return;
            }
            var previewHtml = typeof buildTransferFormPreviewHtml === "function"
                ? buildTransferFormPreviewHtml(transfer)
                : "<p style='color:red;'>Preview renderer not loaded.</p>";
            jq("#registration-hie-transfer-preview-body").html(previewHtml);
            updateValidateButton(transfer);
        }

        function loadTransferPreview(transferId, patientUpid) {
            ensurePreviewModal();
            if (!transferId || !patientUpid) {
                jq("#registration-hie-transfer-preview-body").html("<p style='color:red;'>Missing transfer UUID or UPID.</p>");
                updateValidateButton(null);
                showPreviewDialog();
                return;
            }

            jq("#registration-hie-transfer-preview-body").html(
                "<div style='padding:10px;'><i class='icon-spinner icon-spin'></i> Loading transfer information...</div>"
            );
            updateValidateButton(null);
            showPreviewDialog();

            jq.ajax({
                url: restUrl,
                type: "GET",
                data: {
                    upid: patientUpid,
                    transferId: transferId,
                    activeOnly: false
                },
                dataType: "json",
                headers: {
                    "Accept": "application/json"
                }
            }).done(function(response) {
                if (response && response.status === "error") {
                    jq("#registration-hie-transfer-preview-body").html(
                        "<p style='color:red;'>" + esc(response.message || "Unable to load transfer.") + "</p>"
                    );
                    updateValidateButton(null);
                    return;
                }
                var items = response && response.data ? response.data : [];
                if (items.length) {
                    renderPreview(items[0]);
                    return;
                }
                jq("#registration-hie-transfer-preview-body").html("<p style='color:red;'>No matching transfer found in HIE.</p>");
                updateValidateButton(null);
            }).fail(function(xhr) {
                var message = "Unable to load transfer details.";
                if (xhr && xhr.responseJSON && xhr.responseJSON.message) {
                    message = xhr.responseJSON.message;
                }
                jq("#registration-hie-transfer-preview-body").html("<p style='color:red;'>" + esc(message) + "</p>");
                updateValidateButton(null);
            });
        }

        function transferListRowHtml(item) {
            var uuid = item.uuid || item.id || "";
            var date = item.date || item.transferDecisionDatetime || item.admissionDatetime || "";
            var from = item.origin || item.referringFacilityName || item.hospitalName || "";
            var destination = resolveDestination(item);
            var status = item.status || "";
            var statusClass = "transfer-status-pending";
            if (item.agentRejected === true || item.agentRejected === "true") {
                statusClass = "transfer-status-rejected";
            } else if (item.agentDecisionApproved === true || item.agentDecisionApproved === "true") {
                statusClass = "transfer-status-approved";
            } else if (item.needsInsuranceApproval === true || item.needsInsuranceApproval === "true") {
                statusClass = "transfer-status-awaiting";
            }
            var statusHtml = status
                ? "<div class='" + statusClass + "' style='font-size:12px;margin-top:4px;'>" + esc(status) + "</div>"
                : "";
            return ""
                + "<tr>"
                + "<td>" + esc(date) + statusHtml + "</td>"
                + "<td>" + esc(from) + "</td>"
                + "<td>" + esc(destination) + "</td>"
                + "<td><a href='javascript:void(0);' class='registration-hie-transfer-view-link' "
                + "data-transfer-id='" + esc(uuid) + "' data-upid='" + esc(upid) + "' "
                + "title='Open transfer'><i class='icon-eye-open'></i></a></td>"
                + "</tr>";
        }

        function loadTransferList() {
            if (!upid || !/^\d{6}-\d{4}-\d{4}$/.test(upid)) {
                return;
            }

            container.html("<span><i class='icon-spinner icon-spin'></i> Loading transfers from HIE...</span>");

            jq.ajax({
                url: restUrl,
                type: "GET",
                data: {
                    upid: upid,
                    activeOnly: true
                },
                dataType: "json",
                headers: {
                    "Accept": "application/json"
                }
            }).done(function(response) {
                if (response && response.status === "error") {
                    container.html("<span style='color:#a94442;'>" + esc(response.message || "Unable to load transfers.") + "</span>");
                    return;
                }
                var items = response && response.data ? response.data : [];
                if (!items.length) {
                    container.html("No inbound HIE transfers found for this patient.");
                    return;
                }
                var rows = [];
                for (var i = 0; i < items.length; i++) {
                    rows.push(transferListRowHtml(items[i]));
                }
                container.html(
                    "<table class='table table-striped transfer-datatable'>"
                    + "<thead><tr><th>Date</th><th>From</th><th>Destination</th><th>Action</th></tr></thead>"
                    + "<tbody>" + rows.join("") + "</tbody></table>"
                );
            }).fail(function() {
                container.html("<span style='color:#a94442;'>Unable to load transfers from HIE.</span>");
            });
        }

        function applyValidatedTransfer() {
            var transfer = currentPreviewTransfer;
            if (!transfer || !targetsCurrentFacility(transfer)) {
                return;
            }
            var hieTransferId = transfer.uuid || transfer.id || transfer.hieTransferId || "";
            if (!hieTransferId) {
                return;
            }
            var target = resolveTransferIdInput();
            if (!target || !target.length) {
                jq("#registration-hie-transfer-validate-status").show().css("color", "#a94442")
                    .text("Transfer ID field not found on form.");
                return;
            }
            target.val(hieTransferId).trigger("change");
            jq("#registration-hie-transfer-validate-status").show().css("color", "#0f766e")
                .text("Transfer ID recorded on form. Save registration to persist.");
            updateValidateButton(currentPreviewTransfer);
            hidePreviewDialog();
        }

        jq(document).off("click.registrationHieTransfer", ".registration-hie-transfer-view-link");
        jq(document).on("click.registrationHieTransfer", ".registration-hie-transfer-view-link", function(e) {
            e.preventDefault();
            e.stopPropagation();
            var link = jq(this);
            var transferId = link.attr("data-transfer-id") || "";
            var patientUpid = link.attr("data-upid") || upid || "";
            loadTransferPreview(transferId, patientUpid);
        });

        jq(document).off("click.registrationHieTransferClose", "#registration-hie-transfer-preview-close");
        jq(document).on("click.registrationHieTransferClose", "#registration-hie-transfer-preview-close", function(e) {
            e.preventDefault();
            hidePreviewDialog();
        });

        jq(document).off("click.registrationHieTransferValidate", "#registration-hie-transfer-validate-btn");
        jq(document).on("click.registrationHieTransferValidate", "#registration-hie-transfer-validate-btn", function(e) {
            e.preventDefault();
            applyValidatedTransfer();
        });

        jq(document).off("click.registrationHieTransferExportPdf", "#registration-hie-transfer-export-pdf-btn");
        jq(document).on("click.registrationHieTransferExportPdf", "#registration-hie-transfer-export-pdf-btn", function(e) {
            e.preventDefault();
            exportCurrentPreviewPdf();
        });

        loadTransferList();
    }

    jq(document).ready(function() {
        if (typeof renderTransferPreviewInto === "function" || typeof buildTransferFormPreviewHtml === "function") {
            initRegistrationHieTransfer();
            return;
        }
        var transferOpenmrsPath = (typeof openmrsContextPath !== "undefined" ? openmrsContextPath : "/openmrs");
        var previewResourcesBase = normalizeRootUrl(transferOpenmrsPath + "/moduleResources/transferapp/scripts/");
        jq.getScript(previewResourcesBase + "transferPreviewCommon.js")
            .done(initRegistrationHieTransfer)
            .fail(function() {
                jq("#transfer_from_hie").html("<span style='color:#a94442;'>Unable to load transfer preview renderer.</span>");
            });
    });
})(typeof jq !== "undefined" ? jq : jQuery);
