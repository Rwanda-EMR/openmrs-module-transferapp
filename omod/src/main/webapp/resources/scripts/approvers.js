jQuery(function($) {
    "use strict";

    var config = window.approversConfig || {};
    var messages = config.messages || {};
    var resourcesBase = config.resourcesBase || "";
    var select2Loading = null;

    if (!$("#approvers-add-form").length) {
        return;
    }

    function showMessage(text, isError) {
        var $el = $("#approvers-message");
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

    function bridgeSelect2ToCurrentJquery() {
        if (typeof $.fn.select2 === "function") {
            return true;
        }
        if (typeof jQuery !== "undefined" && typeof jQuery.fn.select2 === "function") {
            $.fn.select2 = jQuery.fn.select2;
            if (jQuery.fn.select2.defaults) {
                $.fn.select2.defaults = jQuery.fn.select2.defaults;
            }
            return true;
        }
        if (typeof jq !== "undefined" && typeof jq.fn.select2 === "function") {
            $.fn.select2 = jq.fn.select2;
            if (jq.fn.select2.defaults) {
                $.fn.select2.defaults = jq.fn.select2.defaults;
            }
            return true;
        }
        return false;
    }

    function isSelect2Ready() {
        bridgeSelect2ToCurrentJquery();
        return typeof $.fn.select2 === "function";
    }

    function ensureSelect2Css() {
        if (document.getElementById("approvers-select2-css") || !resourcesBase) {
            return;
        }
        $("head").append(
            "<link id='approvers-select2-css' rel='stylesheet' type='text/css' href='"
            + resourcesBase + "styles/select2.min.css' />"
        );
    }

    function ensureSelect2(callback) {
        ensureSelect2Css();
        if (isSelect2Ready()) {
            callback(true);
            return;
        }
        if (select2Loading) {
            select2Loading.done(function() {
                callback(isSelect2Ready());
            }).fail(function() {
                callback(false);
            });
            return;
        }
        if (!resourcesBase) {
            callback(false);
            return;
        }
        select2Loading = $.getScript(resourcesBase + "scripts/select2/select2.min.js");
        select2Loading.done(function() {
            bridgeSelect2ToCurrentJquery();
            callback(isSelect2Ready());
        }).fail(function() {
            callback(false);
        });
    }

    function applySelect2($select) {
        if (!isSelect2Ready() || !$select.length) {
            return;
        }
        if ($select.hasClass("select2-hidden-accessible")) {
            $select.select2("destroy");
        }
        $select.select2({
            width: "100%",
            placeholder: $select.find("option[value='']").text() || "",
            allowClear: true
        });
    }

    function tbody() {
        return $("#approvers-table tbody");
    }

    function ensureEmptyRow() {
        var $tbody = tbody();
        if ($tbody.find("tr[data-approver-id]").length === 0) {
            $tbody.html(
                '<tr class="transfer-admin-empty-row"><td colspan="4">'
                + $("<div/>").text(messages.empty || "").html()
                + "</td></tr>"
            );
        }
    }

    function removeEmptyRow() {
        tbody().find("tr.transfer-admin-empty-row").remove();
    }

    function removeUserOption(userId) {
        var $select = $("#approverUserSelect");
        $select.find("option[value='" + userId + "']").remove();
        if ($select.hasClass("select2-hidden-accessible") && typeof $.fn.select2 === "function") {
            $select.val("").trigger("change");
        } else {
            $select.val("");
        }
    }

    function restoreUserOption(userId, label) {
        if (!userId) {
            return;
        }
        var $select = $("#approverUserSelect");
        if ($select.find("option[value='" + userId + "']").length) {
            return;
        }
        $("<option/>").attr("value", userId).text(label || String(userId)).appendTo($select);
    }

    function appendRow(payload) {
        removeEmptyRow();
        var $row = $("<tr/>")
            .attr("data-approver-id", payload.approverId)
            .attr("data-user-id", payload.userId);
        $row.append($("<td class='approver-name'/>").text(payload.displayName || ""));
        $row.append($("<td class='approver-username'/>").text(payload.username || ""));
        $row.append($("<td class='approver-position'/>").text(payload.position || ""));
        var $action = $("<td class='transfer-admin-col-action'/>");
        $("<button type='button' class='btn btn-default approver-remove-btn'/>")
            .attr("data-approver-id", payload.approverId)
            .text(messages.remove || "Remove")
            .appendTo($action);
        $row.append($action);
        tbody().append($row);
    }

    $("#approvers-add-form").on("submit", function(event) {
        event.preventDefault();
        event.stopPropagation();

        var saveUrl = config.saveUrl;
        if (!saveUrl) {
            showMessage(messages.addError || "Save URL is not configured", true);
            return false;
        }

        var $select = $("#approverUserSelect");
        var userId = $.trim($select.val() || "");
        var position = $.trim($("#approverPosition").val() || "");
        if (!userId) {
            showMessage(messages.userRequired || "User is required", true);
            return false;
        }
        if (!position) {
            showMessage(messages.positionRequired || "Position is required", true);
            return false;
        }

        var $submit = $(this).find("button[type='submit']");
        $submit.prop("disabled", true);

        $.ajax({
            url: saveUrl,
            type: "POST",
            dataType: "json",
            data: {
                userId: userId,
                position: position
            }
        }).done(function(response) {
            if (!response || response.status !== "success") {
                showMessage((response && response.message) || messages.addError, true);
                return;
            }
            appendRow(response);
            removeUserOption(response.userId || userId);
            $("#approverPosition").val("");
            showMessage(messages.addSuccess || "Saved", false);
        }).fail(function(xhr) {
            var msg = messages.addError;
            try {
                var body = xhr && xhr.responseText ? JSON.parse(xhr.responseText) : null;
                if (body && body.message) {
                    msg = body.message;
                }
            } catch (ignoreParse) {
            }
            showMessage(msg, true);
        }).always(function() {
            $submit.prop("disabled", false);
        });

        return false;
    });

    $(document).on("click", ".approver-remove-btn", function(event) {
        event.preventDefault();
        var $btn = $(this);
        var approverId = $btn.attr("data-approver-id");
        if (!approverId) {
            return;
        }
        if (!window.confirm(messages.removeConfirm || "Remove this approver?")) {
            return;
        }
        var $row = $btn.closest("tr");
        var userId = $row.attr("data-user-id");
        var name = $.trim($row.find(".approver-name").text() || "");
        var username = $.trim($row.find(".approver-username").text() || "");
        var label = name;
        if (username && username !== name) {
            label = name + " (" + username + ")";
        }

        $.ajax({
            url: config.voidUrl,
            type: "POST",
            dataType: "json",
            data: { approverId: approverId }
        }).done(function(response) {
            if (!response || response.status !== "success") {
                showMessage((response && response.message) || messages.removeError, true);
                return;
            }
            $row.remove();
            restoreUserOption(userId, label);
            ensureEmptyRow();
            showMessage(messages.removeSuccess || "Removed", false);
        }).fail(function(xhr) {
            var msg = messages.removeError;
            try {
                var body = xhr && xhr.responseText ? JSON.parse(xhr.responseText) : null;
                if (body && body.message) {
                    msg = body.message;
                }
            } catch (ignoreParse) {
            }
            showMessage(msg, true);
        });
    });

    ensureSelect2(function(ready) {
        if (!ready) {
            showMessage(messages.select2Error || "Unable to load select2", true);
            return;
        }
        applySelect2($("#approverUserSelect"));
    });
});
