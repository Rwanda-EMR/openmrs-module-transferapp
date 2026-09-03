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

    function loadTransferPreview(transferId, upid) {
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

        jq.ajax({
            url: restUrl(),
            type: "GET",
            data: {
                upid: upid,
                transferId: transferId,
                activeOnly: false
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
                    jq("#transfer-history-preview-body").html(
                        "<p style='color:red;'>" + esc(messages().loadError || "Unable to load transfer.") + "</p>"
                    );
                    return;
                }
            }
            if (response && response.status === "error") {
                jq("#transfer-history-preview-body").html(
                    "<p style='color:red;'>" + esc(response.message || messages().loadError || "Unable to load transfer.") + "</p>"
                );
                return;
            }
            var items = response && response.data ? response.data : [];
            if (items.length) {
                renderPreview(items[0]);
                return;
            }
            jq("#transfer-history-preview-body").html(
                "<p style='color:red;'>" + esc(messages().empty || "No matching transfer found in HIE.") + "</p>"
            );
        }).fail(function(xhr) {
            var message = messages().loadError || "Unable to load transfer details.";
            if (xhr && xhr.responseJSON && xhr.responseJSON.message) {
                message = xhr.responseJSON.message;
            }
            jq("#transfer-history-preview-body").html("<p style='color:red;'>" + esc(message) + "</p>");
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

        jq(document).on("click", ".transfer-history-view-link", function(event) {
            event.preventDefault();
            event.stopPropagation();
            var link = jq(this);
            loadTransferPreview(link.attr("data-transfer-id"), link.attr("data-upid"));
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
