jQuery(function($) {
    "use strict";

    var config = window.approvalsConfig || {};
    var messages = config.messages || {};
    var currentUuid = null;
    var jq = $;

    if (!$("#approvals-table").length) {
        return;
    }

    function showMessage(text, isError) {
        var $el = $("#approvals-message");
        if (!$el.length) {
            return;
        }
        $el.text(text || "")
            .toggle(!!text)
            .css({
                color: isError ? "#a94442" : "#3c763d",
                margin: "8px 0"
            });
    }

    function ensureOverlay() {
        var overlay = $("#approvals-preview-overlay");
        if (!overlay.length) {
            $("body").append("<div id='approvals-preview-overlay' class='approvals-overlay' style='display:none;'></div>");
            overlay = $("#approvals-preview-overlay");
        }
        return overlay;
    }

    function showDialog() {
        ensureOverlay().show();
        $("#approvals-preview-dialog").css("display", "flex").show();
        $("body").addClass("approvals-modal-open");
    }

    function hideDialog() {
        $("#approvals-preview-dialog").hide();
        $("#approvals-preview-overlay").hide();
        $("body").removeClass("approvals-modal-open");
    }

    function closePreview() {
        currentUuid = null;
        $("#approvals-reject-panel").hide();
        $("#approvals-reject-reason").val("");
        $("#approvals-approve-btn, #approvals-reject-btn").prop("disabled", false);
        hideDialog();
        $("#approvals-preview-body").empty();
    }

    function removeRow(uuid) {
        var $row = $("#approvals-table tbody tr[data-uuid='" + uuid + "']");
        $row.remove();
        if (!$("#approvals-table tbody tr[data-uuid]").length) {
            $("#approvals-table tbody").html(
                '<tr class="transfer-approvals-empty-row"><td colspan="7">'
                + $("<div/>").text(messages.empty || "").html()
                + "</td></tr>"
            );
        }
    }

    function openPreview(uuid, upid) {
        currentUuid = uuid;
        $("#approvals-reject-panel").hide();
        $("#approvals-reject-reason").val("");
        $("#approvals-approve-btn, #approvals-reject-btn").prop("disabled", false);
        $("#approvals-preview-body").html(
            "<div style='padding:10px;'><i class='icon-spinner icon-spin'></i> "
            + $("<div/>").text(messages.loading || "Loading...").html()
            + "</div>"
        );
        showDialog();

        $.ajax({
            url: config.previewUrl,
            type: "GET",
            dataType: "json",
            data: { uuid: uuid, upid: upid || "", formType: "External" }
        }).done(function(response) {
            var transfer = null;
            if (response && response.status === "success" && response.transfer) {
                transfer = response.transfer;
            } else if (response && response.transfer) {
                transfer = response.transfer;
            } else if (response && !response.status) {
                transfer = response;
            }
            if (!transfer || (response && response.status === "error")) {
                $("#approvals-preview-body").html(
                    "<p style='color:red;'>"
                    + $("<div/>").text((response && response.message) || messages.previewError || "Unable to load preview").html()
                    + "</p>"
                );
                return;
            }
            if (typeof buildTransferFormPreviewHtml === "function") {
                $("#approvals-preview-body").html(buildTransferFormPreviewHtml(transfer));
            } else {
                $("#approvals-preview-body").html(
                    "<p style='color:red;'>"
                    + $("<div/>").text(messages.previewError || "Preview renderer not loaded.").html()
                    + "</p>"
                );
            }
        }).fail(function() {
            $("#approvals-preview-body").html(
                "<p style='color:red;'>"
                + $("<div/>").text(messages.previewError || "Unable to load preview").html()
                + "</p>"
            );
        });
    }

    $(document).on("click", ".approvals-preview-btn", function(event) {
        event.preventDefault();
        var uuid = $(this).attr("data-uuid");
        var upid = $(this).attr("data-upid");
        if (uuid) {
            openPreview(uuid, upid);
        }
    });

    $(document).on("click", ".approvals-preview-close, #approvals-preview-overlay", function(event) {
        event.preventDefault();
        closePreview();
    });

    $("#approvals-approve-btn").on("click", function(event) {
        event.preventDefault();
        if (!currentUuid) {
            return;
        }
        var $btn = $(this);
        $btn.prop("disabled", true);
        $("#approvals-reject-btn").prop("disabled", true);
        $.ajax({
            url: config.approveUrl,
            type: "POST",
            dataType: "json",
            data: { uuid: currentUuid }
        }).done(function(response) {
            if (!response || response.status !== "success") {
                showMessage((response && response.message) || messages.approveError, true);
                $btn.prop("disabled", false);
                $("#approvals-reject-btn").prop("disabled", false);
                return;
            }
            var approvedUuid = currentUuid;
            removeRow(approvedUuid);
            closePreview();
            showMessage(messages.approveSuccess || "Approved", false);
        }).fail(function(xhr) {
            var msg = messages.approveError;
            try {
                var body = xhr && xhr.responseText ? JSON.parse(xhr.responseText) : null;
                if (body && body.message) {
                    msg = body.message;
                }
            } catch (ignoreParse) {
            }
            showMessage(msg, true);
            $btn.prop("disabled", false);
            $("#approvals-reject-btn").prop("disabled", false);
        });
    });

    $("#approvals-reject-btn").on("click", function(event) {
        event.preventDefault();
        $("#approvals-reject-panel").show();
        $("#approvals-reject-reason").focus();
    });

    $("#approvals-reject-cancel-btn").on("click", function(event) {
        event.preventDefault();
        $("#approvals-reject-panel").hide();
        $("#approvals-reject-reason").val("");
    });

    $("#approvals-reject-confirm-btn").on("click", function(event) {
        event.preventDefault();
        if (!currentUuid) {
            return;
        }
        var reason = $.trim($("#approvals-reject-reason").val() || "");
        if (!reason) {
            showMessage(messages.rejectReasonRequired || "Rejection reason is required", true);
            return;
        }
        var $btn = $(this);
        $btn.prop("disabled", true);
        $.ajax({
            url: config.rejectUrl,
            type: "POST",
            dataType: "json",
            data: { uuid: currentUuid, reason: reason }
        }).done(function(response) {
            if (!response || response.status !== "success") {
                showMessage((response && response.message) || messages.rejectError, true);
                $btn.prop("disabled", false);
                return;
            }
            var rejectedUuid = currentUuid;
            removeRow(rejectedUuid);
            closePreview();
            showMessage(messages.rejectSuccess || "Rejected", false);
        }).fail(function(xhr) {
            var msg = messages.rejectError;
            try {
                var body = xhr && xhr.responseText ? JSON.parse(xhr.responseText) : null;
                if (body && body.message) {
                    msg = body.message;
                }
            } catch (ignoreParse) {
            }
            showMessage(msg, true);
            $btn.prop("disabled", false);
        });
    });
});
