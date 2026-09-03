(function() {
    jq(document).ready(function() {
        var filterConfig = window.transferRecordsFilterConfig || {};
        var filterMessages = filterConfig.messages || {};
        var maxDateRangeMonths = filterConfig.maxDateRangeMonths || 3;

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

        function parseIsoDate(value) {
            if (!value) {
                return null;
            }
            var parts = value.split("-");
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

        function formatIsoDate(date) {
            if (!date) {
                return "";
            }
            var month = String(date.getMonth() + 1);
            var day = String(date.getDate());
            if (month.length < 2) {
                month = "0" + month;
            }
            if (day.length < 2) {
                day = "0" + day;
            }
            return date.getFullYear() + "-" + month + "-" + day;
        }

        function addMonths(date, months) {
            var copy = new Date(date.getTime());
            copy.setMonth(copy.getMonth() + months);
            return copy;
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
            if (typeof emr !== "undefined" && typeof emr.errorMessage === "function") {
                emr.errorMessage(message);
            } else {
                window.alert(message);
            }
        }

        function validateDateRange(startValue, endValue) {
            var startDate = parseIsoDate(startValue);
            var endDate = parseIsoDate(endValue);
            if (!startDate || !endDate) {
                showFilterError("Please select valid start and end dates.");
                return false;
            }
            if (startDate > endDate) {
                showFilterError(filterMessages.invalidDateRange || "Start date must be on or before end date.");
                return false;
            }
            if (monthDiff(startDate, endDate) > maxDateRangeMonths) {
                showFilterError(filterMessages.dateRangeError || ("Date range cannot exceed " + maxDateRangeMonths + " months."));
                return false;
            }
            return true;
        }

        function initRecordsFilters() {
            var startInput = document.getElementById("records-filter-start-date");
            var endInput = document.getElementById("records-filter-end-date");
            var destinationSelect = jq("#records-filter-destination");
            var formTypeSelect = jq("#records-filter-form-type");
            var filterForm = jq("#transfer-records-filter-form");

            if (!filterForm.length) {
                return;
            }

            if (typeof flatpickr === "function") {
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
            }

            if (destinationSelect.length && ensureSelect2()) {
                destinationSelect.select2({
                    width: "100%",
                    placeholder: filterMessages.destinationPlaceholder || "All destinations",
                    allowClear: true
                });
                destinationSelect.on("change", function() {
                    if (!validateDateRange(
                            jq("#records-filter-start-date").val(),
                            jq("#records-filter-end-date").val())) {
                        return;
                    }
                    filterForm.trigger("submit");
                });
            }

            if (formTypeSelect.length) {
                formTypeSelect.on("change", function() {
                    if (!validateDateRange(
                            jq("#records-filter-start-date").val(),
                            jq("#records-filter-end-date").val())) {
                        return;
                    }
                    filterForm.trigger("submit");
                });
            }

            filterForm.on("submit", function(e) {
                if (!validateDateRange(
                        jq("#records-filter-start-date").val(),
                        jq("#records-filter-end-date").val())) {
                    e.preventDefault();
                }
            });
        }

        initRecordsFilters();

        if (jq.fn.dataTable && jq("#transfer-records-table").length) {
            jq("#transfer-records-table").dataTable({
                "sDom": "lfrtip",
                "bLengthChange": true,
                "iDisplayLength": 25,
                "aLengthMenu": [[10, 25, 50, 100, -1], [10, 25, 50, 100, "All"]],
                "sPaginationType": "full_numbers",
                "aaSorting": [[0, "desc"]],
                "aoColumnDefs": [
                    { "bSortable": false, "aTargets": [7] }
                ]
            });
        }

        var transferOpenmrsPath = (typeof openmrsContextPath !== "undefined" && openmrsContextPath)
            ? openmrsContextPath
            : (window.transferOpenmrsPath || "");
        window.transferOpenmrsPath = transferOpenmrsPath;
        var transferPreviewUrl = transferOpenmrsPath + "/module/transferapp/transfer/preview.form";
        var transferSubmitUrl = transferOpenmrsPath + "/module/transferapp/transfer/submit.form";
        var transferSubmitNeonatalUrl = transferOpenmrsPath + "/module/transferapp/transfer/submitNeonatal.form";
        var transferSubmitMaternityUrl = transferOpenmrsPath + "/module/transferapp/transfer/submitMaternity.form";
        var transferPreviewResourcesBase = transferOpenmrsPath + "/moduleResources/transferapp/scripts/";
        var transferPreviewScriptsLoading = null;
        var currentPreviewTransferUuid = null;
        var currentPreviewTransferSent = false;
        var currentPreviewFormType = "External";
        var currentPreviewIsHieUpdate = false;

        function syncTransferPreviewSubmitButton() {
            var submitBtn = jq("#transfer-preview-submit");
            if (!submitBtn.length) {
                return;
            }
            submitBtn.show();
            if (currentPreviewTransferSent) {
                submitBtn.prop("disabled", true).text("Sent to HIE");
            } else if (currentPreviewIsHieUpdate) {
                submitBtn.prop("disabled", false).text("Update on HIE");
            } else {
                submitBtn.prop("disabled", false).text("Submit");
            }
        }

        function markTransferRowHieStatus(transferUuid, sentToHie) {
            if (!transferUuid) {
                return;
            }
            var row = jq("tr.transfer-row").filter(function() {
                return jq(this).attr("data-transfer-id") === transferUuid
                    || jq(this).attr("data-uuid") === transferUuid;
            });
            if (!row.length) {
                return;
            }
            row.attr("data-hie-sent", sentToHie ? "true" : "false");
            if (sentToHie) {
                row.addClass("transfer-row-sent");
                row.find("td").eq(5).html("<span class='transfer-status-sent'>Sent</span>");
            } else {
                row.removeClass("transfer-row-sent");
                row.find("td").eq(5).html("<span class='transfer-status-pending'>Not sent</span>");
            }
        }

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
                .done(function() {
                    if (typeof buildTransferFormPreviewHtml === "function") {
                        callback();
                    } else {
                        jq("#transfer-preview-body").html("<p style='color:red;'>Preview renderer failed to initialize.</p>");
                    }
                })
                .fail(function() {
                    jq("#transfer-preview-body").html("<p style='color:red;'>Unable to load preview scripts.</p>");
                });
        }

        function renderTransferPreview(transfer) {
            var formType = (transfer && transfer.formType) || "External";
            currentPreviewFormType = (formType === "Maternity" || formType === "Neonatal") ? formType : "External";
            var previewHtml;
            if (currentPreviewFormType === "Maternity" && typeof buildMaternityTransferFormPreviewHtml === "function") {
                previewHtml = buildMaternityTransferFormPreviewHtml(transfer);
            } else if (currentPreviewFormType === "Neonatal" && typeof buildNeonatalTransferFormPreviewHtml === "function") {
                previewHtml = buildNeonatalTransferFormPreviewHtml(transfer);
            } else if (currentPreviewFormType === "External" && typeof buildTransferFormPreviewHtml === "function") {
                previewHtml = buildTransferFormPreviewHtml(transfer);
            } else {
                previewHtml = "<p style='color:red;'>Preview renderer not loaded.</p>";
            }
            jq("#transfer-preview-body").html(previewHtml);
            currentPreviewTransferSent = !!(transfer && (transfer.hieSent === true || transfer.hieSent === "true"));
            // Only External supports resubmitting an update to an already-sent HIE encounter.
            currentPreviewIsHieUpdate = currentPreviewFormType === "External"
                && !currentPreviewTransferSent && !!(transfer && String(transfer.hieTransferId || "").trim());
            syncTransferPreviewSubmitButton();
        }

        function showTransferPreviewDialog() {
            var dialog = jq("#transfer-preview-dialog");
            if (!dialog.length) {
                return;
            }
            if (dialog.parent()[0] !== document.body) {
                dialog.appendTo(document.body);
            }
            // Do not use emr.setupConfirmationDialog here; it can create a backdrop
            // above our modal in some UI Framework stacks.
            dialog.css({
                display: "flex",
                position: "fixed",
                top: "50%",
                left: "50%",
                transform: "translate(-50%, -50%)",
                zIndex: 20001
            }).show();
            if (!jq("#transfer-preview-overlay").length) {
                jq("body").append("<div id='transfer-preview-overlay'></div>");
            }
            jq("#transfer-preview-overlay").css({
                position: "fixed",
                top: 0,
                left: 0,
                right: 0,
                bottom: 0,
                background: "rgba(0,0,0,0.45)",
                zIndex: 20000
            }).show();
        }

        function hideTransferPreviewDialog() {
            jq("#transfer-preview-dialog").hide();
            jq("#transfer-preview-overlay").hide();
        }

        function showTransferPreview(transferUuid, formType) {
            if (!transferUuid) {
                if (typeof emr !== "undefined" && typeof emr.errorMessage === "function") {
                    emr.errorMessage("Transfer id is missing.");
                }
                return;
            }
            jq("#transfer-preview-body").html("<div style='padding:10px;'><i class='icon-spinner icon-spin'></i> Loading...</div>");
            currentPreviewTransferUuid = transferUuid;
            currentPreviewTransferSent = false;
            currentPreviewFormType = (formType === "Maternity" || formType === "Neonatal") ? formType : "External";
            currentPreviewIsHieUpdate = false;
            syncTransferPreviewSubmitButton();
            showTransferPreviewDialog();

            jq.ajax({
                url: transferPreviewUrl,
                type: "GET",
                data: { uuid: transferUuid, formType: currentPreviewFormType },
                dataType: "json"
            }).done(function(response) {
                if (response && response.status === "success" && response.transfer) {
                    ensureTransferPreviewRenderer(function() {
                        renderTransferPreview(response.transfer);
                    });
                    return;
                }
                var message = response && response.message ? response.message : "Unable to load transfer details.";
                jq("#transfer-preview-body").html("<p style='color:red;'>" + message + "</p>");
            }).fail(function(xhr) {
                var message = "Unable to load transfer details.";
                if (xhr && xhr.responseJSON && xhr.responseJSON.message) {
                    message = xhr.responseJSON.message;
                }
                jq("#transfer-preview-body").html("<p style='color:red;'>" + message + "</p>");
            });
        }

        jq(document).on("click", ".transfer-view-link", function(e) {
            e.preventDefault();
            e.stopPropagation();
            var link = jq(this);
            var $row = link.closest("tr.transfer-row");
            var transferUuid = link.attr("data-uuid")
                || link.attr("data-transfer-id")
                || $row.attr("data-uuid")
                || $row.attr("data-transfer-id");
            var formType = link.attr("data-form-type") || $row.attr("data-form-type") || "External";
            showTransferPreview(transferUuid, formType);
        });

        jq(document).on("click", "#transfer-preview-close, #transfer-preview-overlay", function(e) {
            e.preventDefault();
            hideTransferPreviewDialog();
        });

        jq(document).on("click", "#transfer-preview-submit", function(e) {
            e.preventDefault();
            if (!currentPreviewTransferUuid || currentPreviewTransferSent) {
                return;
            }
            var submitBtn = jq(this);
            submitBtn.prop("disabled", true);
            var submitUrl = currentPreviewFormType === "Neonatal" ? transferSubmitNeonatalUrl
                : currentPreviewFormType === "Maternity" ? transferSubmitMaternityUrl
                : transferSubmitUrl;
            jq.ajax({
                url: submitUrl,
                type: "POST",
                data: { uuid: currentPreviewTransferUuid },
                dataType: "json"
            }).done(function(response) {
                if (response && response.status === "success") {
                    var wasUpdate = currentPreviewIsHieUpdate;
                    currentPreviewTransferSent = true;
                    currentPreviewIsHieUpdate = false;
                    syncTransferPreviewSubmitButton();
                    markTransferRowHieStatus(currentPreviewTransferUuid, true);
                    if (typeof emr !== "undefined" && typeof emr.successMessage === "function") {
                        emr.successMessage(wasUpdate
                            ? "Transfer updated on HIE successfully. Insurance approval was preserved when present."
                            : "Transfer sent to HIE successfully.");
                    }
                    window.location.reload();
                    return;
                }
                var message = response && response.message ? response.message : "Unable to send transfer to HIE.";
                if (typeof emr !== "undefined" && typeof emr.errorMessage === "function") {
                    emr.errorMessage(message);
                }
                syncTransferPreviewSubmitButton();
            }).fail(function() {
                if (typeof emr !== "undefined" && typeof emr.errorMessage === "function") {
                    emr.errorMessage("Unable to send transfer to HIE.");
                }
                syncTransferPreviewSubmitButton();
            });
        });
    });
})();
