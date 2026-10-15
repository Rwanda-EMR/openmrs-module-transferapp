(function() {
    jq(document).ready(function() {
        var filterConfig = window.missingOutboundCounterReferralsConfig || {};
        var filterMessages = filterConfig.messages || {};
        var maxDateRangeMonths = filterConfig.maxDateRangeMonths || 6;

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
                showFilterError(filterMessages.invalidDateRange || "Please select a valid start and end date.");
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

        var startInput = document.getElementById("missing-outbound-cr-filter-start-date");
        var endInput = document.getElementById("missing-outbound-cr-filter-end-date");
        var filterForm = jq("#missing-outbound-cr-filter-form");

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

        if (filterForm.length) {
            filterForm.on("submit", function(e) {
                if (!validateDateRange(
                        jq("#missing-outbound-cr-filter-start-date").val(),
                        jq("#missing-outbound-cr-filter-end-date").val())) {
                    e.preventDefault();
                }
            });
        }

        function copyTextToClipboard(text) {
            var deferred = jq.Deferred();
            var value = String(text || "");
            if (!value) {
                deferred.reject();
                return deferred.promise();
            }
            if (navigator.clipboard && navigator.clipboard.writeText) {
                navigator.clipboard.writeText(value).then(function() {
                    deferred.resolve();
                }, function() {
                    deferred.reject();
                });
                return deferred.promise();
            }
            try {
                var area = document.createElement("textarea");
                area.value = value;
                area.setAttribute("readonly", "readonly");
                area.style.position = "absolute";
                area.style.left = "-9999px";
                document.body.appendChild(area);
                area.select();
                var ok = document.execCommand("copy");
                document.body.removeChild(area);
                if (ok) {
                    deferred.resolve();
                } else {
                    deferred.reject();
                }
            } catch (err) {
                deferred.reject();
            }
            return deferred.promise();
        }

        function showCopyMessage(message, isError) {
            if (typeof emr !== "undefined") {
                if (isError && typeof emr.errorMessage === "function") {
                    emr.errorMessage(message);
                    return;
                }
                if (!isError && typeof emr.successMessage === "function") {
                    emr.successMessage(message);
                    return;
                }
            }
            window.alert(message);
        }

        jq(document).on("click", ".missing-outbound-copy-id", function(e) {
            e.preventDefault();
            e.stopPropagation();
            var button = jq(this);
            var transferId = button.attr("data-transfer-id") || "";
            copyTextToClipboard(transferId).done(function() {
                showCopyMessage(filterMessages.copySuccess || "Transfer ID copied to clipboard.", false);
            }).fail(function() {
                showCopyMessage(filterMessages.copyError || "Unable to copy transfer ID.", true);
            });
        });

        var table = jq("#missing-outbound-counter-referrals-table");
        if (table.length && jq.fn.dataTable) {
            table.dataTable({
                sDom: "lfrtip",
                bFilter: true,
                bLengthChange: true,
                bPaginate: true,
                iDisplayLength: 25,
                aLengthMenu: [[10, 25, 50, 100, -1], [10, 25, 50, 100, "All"]],
                sPaginationType: "full_numbers",
                aaSorting: [[0, "desc"]],
                aoColumnDefs: [
                    { bSortable: false, aTargets: [6] }
                ]
            });
        }
    });
})();
