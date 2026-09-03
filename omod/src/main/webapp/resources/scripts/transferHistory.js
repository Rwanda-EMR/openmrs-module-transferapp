(function() {
    var jq = (typeof jQuery !== "undefined") ? jQuery
        : ((typeof $ !== "undefined") ? $ : null);
    if (!jq) {
        return;
    }

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

        jq("#transfer-history-preview-close").on("click", function(event) {
            event.preventDefault();
            hidePreviewDialog();
        });

        jq(document).on("click", "#transfer-history-preview-overlay", function() {
            hidePreviewDialog();
        });
    });
}());
