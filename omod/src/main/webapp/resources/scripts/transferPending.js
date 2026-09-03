(function(jq) {
    if (window.__transferPendingBound) {
        return;
    }
    window.__transferPendingBound = true;

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

    function escapeRegex(value) {
        return String(value || "").replace(/([.*+?^=!:${}()|\[\]\/\\])/g, "\\$1");
    }

    function ensureSelect2() {
        if (typeof jq.fn.select2 === "function") {
            return true;
        }
        if (typeof jQuery !== "undefined" && typeof jQuery.fn.select2 === "function") {
            jq.fn.select2 = jQuery.fn.select2;
            return true;
        }
        return false;
    }

    jq(document).ready(function() {
        jq("#pending-week-filter").on("change.transferPending", function() {
            var form = jq("#transfer-pending-filter-form");
            if (!form.length || form.attr("aria-busy") === "true") {
                return;
            }
            form.attr("aria-busy", "true");
            jq("#pending-week-loading").show();
            if (form[0] && typeof form[0].submit === "function") {
                form[0].submit();
            }
        });

        var pendingTable = null;
        var pendingRefreshInProgress = false;

        function getPendingFilterState() {
            var selectedServices = jq("#pending-service-filter").val() || [];
            if (typeof selectedServices === "string") {
                selectedServices = [selectedServices];
            }
            return {
                date: jq.trim(jq("#pending-date-filter").val() || ""),
                services: selectedServices
            };
        }

        function initializePendingTable(filterState) {
            var tableElement = jq("#transfer-pending-table");
            if (!jq.fn.dataTable || !tableElement.length) {
                pendingTable = null;
                return;
            }

            var dateFilter = jq("#pending-date-filter");
            var serviceFilter = jq("#pending-service-filter");
            if (filterState) {
                dateFilter.val(filterState.date || "");
                serviceFilter.val(filterState.services || []);
            }

            pendingTable = tableElement.dataTable({
                "sDom": "lfrtip",
                "bLengthChange": true,
                "iDisplayLength": 25,
                "aLengthMenu": [[10, 25, 50, 100, -1], [10, 25, 50, 100, "All"]],
                "sPaginationType": "full_numbers",
                "aaSorting": [[0, "desc"]],
                "aoColumnDefs": [
                    { "bSortable": false, "aTargets": [6] }
                ]
            });

            if (serviceFilter.length && ensureSelect2()) {
                serviceFilter.select2({
                    width: "100%",
                    placeholder: serviceFilter.attr("data-placeholder") || "All services",
                    allowClear: true,
                    closeOnSelect: false
                });
            }

            serviceFilter.off("change.transferPending").on("change.transferPending", function() {
                var selectedServices = jq(this).val() || [];
                if (typeof selectedServices === "string") {
                    selectedServices = [selectedServices];
                }
                var escapedServices = jq.map(selectedServices, function(service) {
                    return escapeRegex(jq.trim(service));
                });
                var filterExpression = escapedServices.length > 0
                    ? "^(?:" + escapedServices.join("|") + ")$"
                    : "";
                pendingTable.fnFilter(filterExpression, 4, escapedServices.length > 0, false);
            });

            dateFilter.off("change.transferPending").on("change.transferPending", function() {
                var selectedDate = jq.trim(jq(this).val() || "");
                var filterExpression = selectedDate
                    ? "^" + escapeRegex(selectedDate) + "$"
                    : "";
                pendingTable.fnFilter(filterExpression, 0, selectedDate.length > 0, false);
            });

            serviceFilter.trigger("change.transferPending");
            dateFilter.trigger("change.transferPending");
        }

        function destroyPendingTable() {
            var serviceFilter = jq("#pending-service-filter");
            if (serviceFilter.hasClass("select2-hidden-accessible") && ensureSelect2()) {
                serviceFilter.select2("destroy");
            }
            if (pendingTable && typeof pendingTable.fnDestroy === "function") {
                pendingTable.fnDestroy();
            }
            pendingTable = null;
        }

        function parsePendingResponse(html) {
            var parsedNodes = typeof jq.parseHTML === "function"
                ? jq.parseHTML(html, document, false)
                : jq(html);
            return jq("<div></div>").append(parsedNodes);
        }

        function refreshPendingTransfers() {
            var form = jq("#transfer-pending-filter-form");
            if (!form.length || pendingRefreshInProgress) {
                return;
            }

            pendingRefreshInProgress = true;
            var filterState = getPendingFilterState();
            jq.ajax({
                url: form.attr("action") || window.location.pathname,
                type: "GET",
                data: form.serialize(),
                dataType: "html",
                cache: false
            }).done(function(html) {
                var response = parsePendingResponse(html);
                var refreshedFilters = response.find("#pending-client-filters").first();
                var refreshedResults = response.find("#transfer-pending-results").first();
                if (!refreshedFilters.length || !refreshedResults.length
                        || refreshedResults.attr("data-refresh-status") === "error") {
                    return;
                }

                destroyPendingTable();
                jq("#pending-client-filters")
                    .toggleClass("is-empty", refreshedFilters.hasClass("is-empty"))
                    .html(refreshedFilters.html());
                jq("#transfer-pending-results")
                    .attr("data-refresh-status", refreshedResults.attr("data-refresh-status"))
                    .html(refreshedResults.html());
                initializePendingTable(filterState);
            }).always(function() {
                pendingRefreshInProgress = false;
            });
        }

        initializePendingTable();
        window.refreshPendingTransfers = refreshPendingTransfers;
        window.__transferPendingRefreshTimer = window.setInterval(
            refreshPendingTransfers,
            5 * 60 * 1000
        );

        var transferOpenmrsPath = (typeof openmrsContextPath !== "undefined" && openmrsContextPath)
            ? openmrsContextPath
            : "/openmrs";
        window.transferOpenmrsPath = transferOpenmrsPath;
        var transferHiePreviewUrl = normalizeRootUrl(transferOpenmrsPath + "/ws/rest/v1/transferapp/transfer");
        var transferPreviewResourcesBase = normalizeRootUrl(transferOpenmrsPath + "/moduleResources/transferapp/scripts/");
        var transferPreviewDialog = null;
        var transferPreviewScriptsLoading = null;

        function ensureTransferPreviewRenderer(callback) {
            if (typeof buildTransferFormPreviewHtml === "function") {
                callback();
                return;
            }
            if (transferPreviewScriptsLoading) {
                transferPreviewScriptsLoading.done(callback);
                return;
            }
            transferPreviewScriptsLoading = jq.getScript(transferPreviewResourcesBase + "transferMohLogo.js")
                .then(function() {
                    return jq.getScript(transferPreviewResourcesBase + "transferFormPreview.js");
                })
                .done(callback)
                .fail(function() {
                    jq("#transfer-preview-body").html("<p style='color:red;'>Unable to load transfer preview renderer.</p>");
                });
        }

        function showTransferPreviewDialog() {
            var dialogEl = jq("#transfer-preview-dialog");
            if (!dialogEl.length) {
                return;
            }
            if (dialogEl.parent()[0] !== document.body) {
                dialogEl.appendTo(document.body);
            }
            if (transferPreviewDialog == null && typeof emr !== "undefined" && typeof emr.setupConfirmationDialog === "function") {
                transferPreviewDialog = emr.setupConfirmationDialog({
                    selector: "#transfer-preview-dialog",
                    actions: {
                        confirm: function() {},
                        cancel: function() {
                            dialogEl.hide();
                        }
                    }
                });
                if (transferPreviewDialog && typeof transferPreviewDialog.close === "function") {
                    transferPreviewDialog.close();
                }
            }
            if (transferPreviewDialog && typeof transferPreviewDialog.show === "function") {
                transferPreviewDialog.show();
            } else {
                dialogEl.show();
            }
        }

        function hideTransferPreviewDialog() {
            if (transferPreviewDialog && typeof transferPreviewDialog.close === "function") {
                transferPreviewDialog.close();
            }
            jq("#transfer-preview-dialog").hide();
        }

        function renderTransferPreview(transfer) {
            if (typeof renderTransferPreviewInto === "function") {
                renderTransferPreviewInto("#transfer-preview-body", transfer);
                return;
            }
            var previewHtml = typeof buildTransferFormPreviewHtml === "function"
                ? buildTransferFormPreviewHtml(transfer)
                : "<p style='color:red;'>Preview renderer not loaded.</p>";
            jq("#transfer-preview-body").html(previewHtml);
        }

        function showPendingTransferPreview(uuid, upid) {
            if (!uuid || !upid) {
                jq("#transfer-preview-body").html("<p style='color:red;'>Missing transfer UUID or UPID.</p>");
                showTransferPreviewDialog();
                return;
            }

            jq("#transfer-preview-body").html(
                "<div style='padding:10px;'><i class='icon-spinner icon-spin'></i> Loading transfer information...</div>"
            );
            showTransferPreviewDialog();

            jq.ajax({
                url: transferHiePreviewUrl,
                type: "GET",
                data: {
                    upid: upid,
                    transferId: uuid,
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
                        jq("#transfer-preview-body").html("<p style='color:red;'>Transfer endpoint returned non-JSON response.</p>");
                        return;
                    }
                }
                if (response && response.status === "error") {
                    jq("#transfer-preview-body").html(
                        "<p style='color:red;'>" + esc(response.message || "Unable to load transfer.") + "</p>"
                    );
                    return;
                }
                var items = response && response.data ? response.data : [];
                if (items.length) {
                    if (typeof renderTransferPreviewInto === "function") {
                        renderTransferPreviewInto("#transfer-preview-body", items[0]);
                    } else {
                        ensureTransferPreviewRenderer(function() {
                            renderTransferPreview(items[0]);
                        });
                    }
                    return;
                }
                jq("#transfer-preview-body").html("<p style='color:red;'>Transfer not found in HIE.</p>");
            }).fail(function(xhr) {
                var message = "Unable to load transfer details.";
                if (xhr && xhr.responseJSON && xhr.responseJSON.message) {
                    message = xhr.responseJSON.message;
                } else if (xhr && xhr.responseText) {
                    try {
                        var parsed = jq.parseJSON(xhr.responseText);
                        if (parsed && parsed.message) {
                            message = parsed.message;
                        }
                    } catch (ignore) {}
                }
                jq("#transfer-preview-body").html("<p style='color:red;'>" + esc(message) + "</p>");
            });
        }

        jq(document).off("click.transferPending", ".transfer-pending-view-link");
        jq(document).on("click.transferPending", ".transfer-pending-view-link", function(e) {
            e.preventDefault();
            e.stopPropagation();
            var link = jq(this);
            var row = link.closest("tr.transfer-row");
            var uuid = link.attr("data-uuid") || row.attr("data-uuid") || "";
            var upid = link.attr("data-upid") || row.attr("data-upid") || "";
            showPendingTransferPreview(uuid, upid);
        });

        jq(document).off("click.transferPendingClose", "#transfer-preview-close");
        jq(document).on("click.transferPendingClose", "#transfer-preview-close", function(e) {
            e.preventDefault();
            hideTransferPreviewDialog();
        });
    });
})(typeof jq !== "undefined" ? jq : jQuery);
