(function() {
    var jq = (typeof jQuery !== "undefined") ? jQuery
        : ((typeof $ !== "undefined") ? $ : null);
    if (!jq) {
        return;
    }

    var pendingReuse = null;

    function config() {
        return window.transferHistoryConfig || {};
    }

    function messages() {
        return config().messages || {};
    }

    function esc(value) {
        return jq("<div/>").text(value == null ? "" : String(value)).html();
    }

    function restUrl() {
        if (config().restUrl) {
            return config().restUrl;
        }
        var base = window.transferOpenmrsPath || openmrsContextPath || "";
        return base + "/ws/rest/v1/transferapp/transfer";
    }

    function reuseUrl() {
        if (config().reuseUrl) {
            return config().reuseUrl;
        }
        return restUrl() + "/reuseRendezvous";
    }

    function todayIso() {
        var now = new Date();
        var month = String(now.getMonth() + 1);
        var day = String(now.getDate());
        if (month.length < 2) {
            month = "0" + month;
        }
        if (day.length < 2) {
            day = "0" + day;
        }
        return now.getFullYear() + "-" + month + "-" + day;
    }

    function showPreviewDialog() {
        var dialog = jq("#transfer-history-preview-dialog");
        if (!dialog.length) {
            return;
        }
        if (dialog.parent()[0] !== document.body) {
            dialog.appendTo(document.body);
        }
        dialog.css({
            display: "flex",
            position: "fixed",
            top: "50%",
            left: "50%",
            transform: "translate(-50%, -50%)",
            zIndex: 20001,
            maxWidth: "95%",
            maxHeight: "92vh",
            width: "1400px",
            margin: 0,
            flexDirection: "column",
            background: "#fff",
            border: "1px solid #00473f",
            borderRadius: "4px",
            boxShadow: "0 4px 20px rgba(0,0,0,0.3)"
        }).show();

        if (!jq("#transfer-history-preview-overlay").length) {
            jq("body").append("<div id='transfer-history-preview-overlay'></div>");
        }
        jq("#transfer-history-preview-overlay").css({
            position: "fixed",
            top: 0,
            left: 0,
            right: 0,
            bottom: 0,
            background: "rgba(0,0,0,0.45)",
            zIndex: 20000
        }).show();
        jq("body").addClass("transfer-history-preview-open");
    }

    function hidePreviewDialog() {
        jq("#transfer-history-preview-dialog").hide();
        jq("#transfer-history-preview-overlay").hide();
        jq("body").removeClass("transfer-history-preview-open");
    }

    function showReuseDialog(context) {
        pendingReuse = context || null;
        var dialog = jq("#transfer-history-reuse-dialog");
        var overlay = jq("#transfer-history-reuse-overlay");
        if (!dialog.length) {
            return;
        }
        if (dialog.parent()[0] !== document.body) {
            dialog.appendTo(document.body);
        }
        if (overlay.parent()[0] !== document.body) {
            overlay.appendTo(document.body);
        }
        var minDate = todayIso();
        var dateInput = jq("#transfer-history-reuse-date");
        dateInput.attr("min", minDate);
        dateInput.val(context && context.reuseDate ? context.reuseDate : minDate);
        jq("#transfer-history-reuse-patient").text(
            (context && context.patientName ? context.patientName : "")
            + (context && context.upid ? " (" + context.upid + ")" : "")
        );
        jq("#transfer-history-reuse-status").hide().text("");
        overlay.css({
            position: "fixed",
            top: 0,
            left: 0,
            right: 0,
            bottom: 0,
            background: "rgba(0,0,0,0.45)",
            zIndex: 20010
        }).show();
        dialog.css({
            display: "block",
            position: "fixed",
            top: "50%",
            left: "50%",
            transform: "translate(-50%, -50%)",
            zIndex: 20011,
            width: "420px",
            maxWidth: "95%",
            background: "#fff",
            border: "1px solid #00473f",
            borderRadius: "4px",
            boxShadow: "0 4px 20px rgba(0,0,0,0.3)",
            padding: 0
        }).show();
    }

    function hideReuseDialog() {
        pendingReuse = null;
        jq("#transfer-history-reuse-dialog").hide();
        jq("#transfer-history-reuse-overlay").hide();
    }

    function updateRowReuseDate(patientId, transferId, reuseDate) {
        var rows = jq("#transfer-history-table tr.transfer-history-row").filter(function() {
            return String(jq(this).attr("data-patient-id") || "") === String(patientId || "")
                && String(jq(this).attr("data-transfer-id") || "") === String(transferId || "");
        });
        rows.each(function() {
            var row = jq(this);
            row.attr("data-reuse-date", reuseDate || "");
            row.find(".transfer-history-reuse-link").attr("data-reuse-date", reuseDate || "");
            var cell = row.find(".transfer-history-reuse-date-cell .transfer-history-reuse-date-value");
            if (!cell.length) {
                return;
            }
            if (reuseDate) {
                cell.removeClass("transfer-history-reuse-none").text(reuseDate);
            } else {
                cell.addClass("transfer-history-reuse-none")
                    .text(messages().reuseNone || "Not scheduled");
            }
        });
    }

    function saveReuseDate(clear) {
        if (!pendingReuse || !pendingReuse.patientId || !pendingReuse.transferId) {
            return;
        }
        var reuseDate = clear ? "" : jq.trim(jq("#transfer-history-reuse-date").val() || "");
        if (!clear) {
            if (!reuseDate) {
                jq("#transfer-history-reuse-status").show().css("color", "#a94442")
                    .text(messages().reusePastDate || "Please choose today or a future date.");
                return;
            }
            if (reuseDate < todayIso()) {
                jq("#transfer-history-reuse-status").show().css("color", "#a94442")
                    .text(messages().reusePastDate || "Please choose today or a future date.");
                return;
            }
        }
        var saveBtn = jq("#transfer-history-reuse-save");
        var clearBtn = jq("#transfer-history-reuse-clear");
        saveBtn.prop("disabled", true);
        clearBtn.prop("disabled", true);
        jq("#transfer-history-reuse-status").show().css("color", "#334155")
            .text(messages().loading || "Saving...");

        jq.ajax({
            url: reuseUrl(),
            type: "POST",
            data: {
                patientId: pendingReuse.patientId,
                hieTransferId: pendingReuse.transferId,
                reuseDate: reuseDate
            },
            dataType: "json",
            headers: {
                "Accept": "application/json"
            }
        }).done(function(response) {
            if (typeof response === "string") {
                try {
                    response = jq.parseJSON(response);
                } catch (err) {
                    response = null;
                }
            }
            if (response && response.status === "success") {
                var savedDate = response.reuseRendezvousDate || "";
                updateRowReuseDate(pendingReuse.patientId, pendingReuse.transferId, savedDate);
                if (typeof emr !== "undefined" && typeof emr.successMessage === "function") {
                    emr.successMessage(savedDate
                        ? (messages().reuseSuccess || "Reuse rendez-vous date saved.")
                        : (messages().reuseCleared || "Reuse rendez-vous date cleared."));
                }
                hideReuseDialog();
                return;
            }
            jq("#transfer-history-reuse-status").show().css("color", "#a94442")
                .text((response && response.message) || messages().reuseError || "Unable to save reuse rendez-vous date.");
        }).fail(function(xhr) {
            var message = messages().reuseError || "Unable to save reuse rendez-vous date.";
            if (xhr && xhr.responseJSON && xhr.responseJSON.message) {
                message = xhr.responseJSON.message;
            }
            jq("#transfer-history-reuse-status").show().css("color", "#a94442").text(message);
        }).always(function() {
            saveBtn.prop("disabled", false);
            clearBtn.prop("disabled", false);
        });
    }

    function previewUrl() {
        if (config().previewUrl) {
            return config().previewUrl;
        }
        var base = window.transferOpenmrsPath || openmrsContextPath || "";
        return base + "/module/transferapp/transfer/preview.form";
    }

    function formatProgress(template, current, total) {
        var text = template || "Building combined PDF ({0} of {1})...";
        return String(text).replace("{0}", String(current)).replace("{1}", String(total));
    }

    function listExportRows() {
        var table = jq("#transfer-history-table");
        if (!table.length) {
            return [];
        }
        var rows = [];
        if (jq.fn && jq.fn.dataTable && jq.fn.dataTable.fnIsDataTable
                && jq.fn.dataTable.fnIsDataTable(table[0])) {
            var api = table.dataTable();
            var nodes = typeof api.fnGetFilteredNodes === "function"
                ? api.fnGetFilteredNodes()
                : api.fnGetNodes();
            jq(nodes).filter(".transfer-history-row").each(function() {
                rows.push(jq(this));
            });
        } else {
            table.find("tbody tr.transfer-history-row").each(function() {
                rows.push(jq(this));
            });
        }
        return rows;
    }

    function fetchLocalPreview(localUuid) {
        var deferred = jq.Deferred();
        if (!localUuid) {
            deferred.resolve(null);
            return deferred.promise();
        }
        jq.ajax({
            url: previewUrl(),
            type: "GET",
            data: { uuid: localUuid },
            dataType: "json",
            headers: { "Accept": "application/json" }
        }).done(function(response) {
            if (typeof response === "string") {
                try {
                    response = jq.parseJSON(response);
                } catch (err) {
                    deferred.resolve(null);
                    return;
                }
            }
            if (response && response.status === "success" && response.transfer) {
                deferred.resolve(response.transfer);
                return;
            }
            deferred.resolve(null);
        }).fail(function() {
            deferred.resolve(null);
        });
        return deferred.promise();
    }

    function fetchHiePreview(transferId, upid) {
        var deferred = jq.Deferred();
        if (!transferId || !upid) {
            deferred.resolve(null);
            return deferred.promise();
        }
        jq.ajax({
            url: restUrl(),
            type: "GET",
            data: {
                upid: upid,
                transferId: transferId,
                activeOnly: false
            },
            dataType: "json",
            headers: { "Accept": "application/json" }
        }).done(function(response) {
            if (typeof response === "string") {
                try {
                    response = jq.parseJSON(response);
                } catch (err) {
                    deferred.resolve(null);
                    return;
                }
            }
            if (response && response.status === "error") {
                deferred.resolve(null);
                return;
            }
            var items = response && response.data ? response.data : [];
            deferred.resolve(items.length ? items[0] : null);
        }).fail(function() {
            deferred.resolve(null);
        });
        return deferred.promise();
    }

    function loadPreviewDataForRow(row) {
        var localUuid = jq.trim(row.attr("data-local-uuid") || "");
        var transferId = jq.trim(row.attr("data-transfer-id") || "");
        var upid = jq.trim(row.attr("data-upid") || "");
        var deferred = jq.Deferred();
        fetchLocalPreview(localUuid).done(function(localTransfer) {
            if (localTransfer) {
                deferred.resolve(localTransfer);
                return;
            }
            fetchHiePreview(transferId, upid).done(function(hieTransfer) {
                deferred.resolve(hieTransfer || null);
            });
        });
        return deferred.promise();
    }

    function buildPreviewHtml(transfer) {
        if (!transfer) {
            return "";
        }
        var normalized = typeof enrichTransferPreviewData === "function"
            ? enrichTransferPreviewData(transfer)
            : transfer;
        if (typeof buildTransferFormPreviewHtml === "function") {
            return buildTransferFormPreviewHtml(normalized);
        }
        return "";
    }

    function showExportStatus(text, isError) {
        var status = jq("#transfer-history-export-status");
        if (!status.length) {
            jq(".transfer-history-filters-grid").after(
                "<p id='transfer-history-export-status' class='transfer-history-export-status'></p>"
            );
            status = jq("#transfer-history-export-status");
        }
        status.css("color", isError ? "#a94442" : "#334155").text(text || "").show();
    }

    function hideExportStatus() {
        jq("#transfer-history-export-status").hide().text("");
    }

    function exportCombinedPdf() {
        var rows = listExportRows();
        var exportBtn = jq("#transfer-history-export-pdf");
        if (!rows.length) {
            showExportStatus(messages().exportPdfEmpty || "No transfers in the list to export.", true);
            return;
        }
        if (typeof buildTransferFormPreviewHtml !== "function") {
            showExportStatus(messages().exportPdfError || "Unable to build the combined PDF export.", true);
            return;
        }

        exportBtn.prop("disabled", true);
        var pages = [];
        var failed = 0;
        var index = 0;
        var total = rows.length;

        function finish() {
            exportBtn.prop("disabled", false);
            if (!pages.length) {
                showExportStatus(messages().exportPdfError || "Unable to build the combined PDF export.", true);
                return;
            }
            var combined = pages.join("");
            var ok = typeof exportTransferFormPreviewPdf === "function"
                && exportTransferFormPreviewPdf(combined, {
                    fileName: messages().exportPdfTitle || "Transfer-History-Export"
                });
            if (!ok) {
                showExportStatus(messages().exportPdfError || "Unable to build the combined PDF export.", true);
                return;
            }
            if (failed > 0) {
                var partial = messages().exportPdfPartial
                    || "Exported {0} of {1} transfers. Some could not be loaded.";
                showExportStatus(formatProgress(partial, pages.length, total), true);
            } else {
                hideExportStatus();
            }
        }

        function next() {
            if (index >= total) {
                finish();
                return;
            }
            showExportStatus(formatProgress(
                messages().exportPdfProgress || "Building combined PDF ({0} of {1})...",
                index + 1,
                total
            ), false);
            var row = rows[index];
            index += 1;
            loadPreviewDataForRow(row).done(function(transfer) {
                var html = buildPreviewHtml(transfer);
                if (html) {
                    pages.push("<div class='transfer-pdf-page'>" + html + "</div>");
                } else {
                    failed += 1;
                }
                next();
            });
        }

        next();
    }

    function renderPreview(transfer) {
        if (typeof renderTransferPreviewInto === "function") {
            renderTransferPreviewInto("#transfer-history-preview-body", transfer);
            return;
        }
        var previewHtml = typeof buildTransferFormPreviewHtml === "function"
            ? buildTransferFormPreviewHtml(transfer)
            : "<p style='color:red;'>Preview renderer not loaded.</p>";
        jq("#transfer-history-preview-body").html(previewHtml);
    }

    function loadTransferPreview(transferId, upid, localUuid) {
        if (!transferId || !upid) {
            jq("#transfer-history-preview-body").html(
                "<p style='color:red;'>" + esc(messages().missingIds || "Missing transfer UUID or UPID.") + "</p>"
            );
            showPreviewDialog();
            return;
        }

        jq("#transfer-history-preview-body").html(
            "<div style='padding:10px;'><i class='icon-spinner icon-spin'></i> "
            + esc(messages().loading || "Loading transfer information...") + "</div>"
        );
        showPreviewDialog();

        var rowLike = {
            attr: function(name) {
                if (name === "data-local-uuid") {
                    return localUuid || "";
                }
                if (name === "data-transfer-id") {
                    return transferId || "";
                }
                if (name === "data-upid") {
                    return upid || "";
                }
                return "";
            }
        };

        loadPreviewDataForRow(rowLike).done(function(transfer) {
            if (transfer) {
                renderPreview(transfer);
                return;
            }
            jq("#transfer-history-preview-body").html(
                "<p style='color:red;'>" + esc(messages().empty || "No matching transfer found in HIE.") + "</p>"
            );
        });
    }

    jq(function() {
        var table = jq("#transfer-history-table");
        if (table.length && jq.fn && jq.fn.dataTable) {
            table.dataTable({
                bFilter: true,
                bInfo: true,
                bPaginate: true,
                bLengthChange: true,
                sPaginationType: "full_numbers",
                iDisplayLength: 25,
                aaSorting: [[0, "desc"]],
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

        jq("#transfer-history-export-pdf").on("click", function(event) {
            event.preventDefault();
            exportCombinedPdf();
        });

        jq(document).on("click", ".transfer-history-view-link", function(event) {
            event.preventDefault();
            event.stopPropagation();
            var link = jq(this);
            var row = link.closest("tr.transfer-history-row");
            loadTransferPreview(
                link.attr("data-transfer-id"),
                link.attr("data-upid"),
                row.attr("data-local-uuid") || ""
            );
        });

        jq(document).on("click", ".transfer-history-reuse-link", function(event) {
            event.preventDefault();
            event.stopPropagation();
            if (!config().canCreateTransfer) {
                return;
            }
            var link = jq(this);
            showReuseDialog({
                patientId: link.attr("data-patient-id"),
                transferId: link.attr("data-transfer-id"),
                upid: link.attr("data-upid"),
                reuseDate: link.attr("data-reuse-date") || "",
                patientName: link.attr("data-patient-name") || ""
            });
        });

        jq("#transfer-history-preview-close").on("click", function(event) {
            event.preventDefault();
            hidePreviewDialog();
        });

        jq(document).on("click", "#transfer-history-preview-overlay", function() {
            hidePreviewDialog();
        });

        jq("#transfer-history-reuse-save").on("click", function(event) {
            event.preventDefault();
            saveReuseDate(false);
        });
        jq("#transfer-history-reuse-clear").on("click", function(event) {
            event.preventDefault();
            saveReuseDate(true);
        });
        jq("#transfer-history-reuse-cancel").on("click", function(event) {
            event.preventDefault();
            hideReuseDialog();
        });
        jq(document).on("click", "#transfer-history-reuse-overlay", function() {
            hideReuseDialog();
        });
    });
}());
