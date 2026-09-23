(function() {
    var jq = (typeof jQuery !== "undefined") ? jQuery : (typeof $ !== "undefined" ? $ : null);
    if (!jq) {
        return;
    }

    function config() {
        return window.pastTransfersConfig || {};
    }

    function messages() {
        return config().messages || {};
    }

    function esc(value) {
        return jq("<div/>").text(value == null ? "" : String(value)).html();
    }

    function displayValue(value) {
        var text = value == null ? "" : String(value).trim();
        return text ? esc(text) : "—";
    }

    function previewUrl() {
        return config().previewUrl
            || ((window.transferOpenmrsPath || "") + "/module/transferapp/transfer/preview.form");
    }

    function restUrl() {
        return config().restUrl
            || ((window.transferOpenmrsPath || "") + "/ws/rest/v1/transferapp/transfer");
    }

    function receiveUrl() {
        return config().receiveUrl
            || ((window.transferOpenmrsPath || "") + "/ws/rest/v1/transferapp/transfer/receive");
    }

    function validateUrl() {
        return config().validateUrl
            || ((window.transferOpenmrsPath || "") + "/ws/rest/v1/transferapp/transfer/validate");
    }

    var previewContext = {
        patientId: null,
        visitId: null,
        upid: null,
        transferId: null,
        localUuid: null,
        fromHie: false,
        canConfirm: false,
        saveWithObs: false,
        sourceRow: null
    };

    var hieListContext = {
        patientId: null,
        visitId: null,
        upid: null,
        patientName: "",
        visitStart: "",
        visitEnd: ""
    };

    function ensureOverlay(id) {
        var overlay = jq("#" + id);
        if (!overlay.length) {
            jq("body").append("<div id='" + id + "' class='past-transfers-overlay' style='display:none;'></div>");
            overlay = jq("#" + id);
        }
        return overlay;
    }

    function showDialog(dialogSelector, overlayId) {
        ensureOverlay(overlayId).css({
            position: "fixed",
            top: 0,
            left: 0,
            right: 0,
            bottom: 0,
            background: "rgba(0,0,0,0.45)",
            zIndex: 20000,
            display: "block"
        }).show();
        var dialog = jq(dialogSelector);
        dialog.css("display", "flex").show();
        jq("body").addClass("past-transfers-modal-open");
    }

    function hideDialog(dialogSelector, overlayId) {
        jq(dialogSelector).hide();
        jq("#" + overlayId).hide();
        if (!jq("#past-transfers-preview-dialog").is(":visible")
                && !jq("#past-transfers-hie-dialog").is(":visible")) {
            jq("body").removeClass("past-transfers-modal-open");
        }
    }

    function setPreviewStatus(text, isError) {
        var status = jq("#past-transfers-preview-status");
        if (!status.length) {
            return;
        }
        if (!text) {
            status.hide().text("");
            return;
        }
        status.css("color", isError ? "#a94442" : "#0f766e").text(text).show();
    }

    function updateSaveButtonVisibility() {
        var saveBtn = jq("#past-transfers-preview-save");
        if (!saveBtn.length) {
            return;
        }
        var canCreate = config().canCreateTransfer === true || config().canCreateTransfer === "true";
        var show = canCreate && previewContext.fromHie && previewContext.canConfirm
            && previewContext.patientId
            && previewContext.transferId;
        saveBtn.toggle(!!show).prop("disabled", false);
    }

    function renderPreview(transfer) {
        if (typeof renderTransferPreviewInto === "function") {
            renderTransferPreviewInto("#past-transfers-preview-body", transfer);
            return;
        }
        var previewHtml = typeof buildTransferFormPreviewHtml === "function"
            ? buildTransferFormPreviewHtml(transfer)
            : "<p style='color:red;'>Preview renderer not loaded.</p>";
        jq("#past-transfers-preview-body").html(previewHtml);
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

    function openTransferPreview(options) {
        options = options || {};
        previewContext.patientId = options.patientId || null;
        previewContext.visitId = options.visitId || null;
        previewContext.upid = options.upid || "";
        previewContext.transferId = options.transferId || "";
        previewContext.localUuid = options.localUuid || "";
        previewContext.fromHie = false;
        previewContext.canConfirm = options.canConfirm === true;
        previewContext.saveWithObs = options.saveWithObs === true;
        previewContext.sourceRow = options.sourceRow || null;

        if (!previewContext.transferId || !previewContext.upid) {
            jq("#past-transfers-preview-body").html(
                "<p style='color:red;'>" + esc(messages().missingIds || "Missing transfer UUID or UPID.") + "</p>"
            );
            updateSaveButtonVisibility();
            setPreviewStatus("", false);
            showDialog("#past-transfers-preview-dialog", "past-transfers-preview-overlay");
            return;
        }

        jq("#past-transfers-preview-body").html(
            "<div style='padding:10px;'><i class='icon-spinner icon-spin'></i> "
            + esc(messages().loading || "Loading transfer information...") + "</div>"
        );
        updateSaveButtonVisibility();
        setPreviewStatus("", false);
        showDialog("#past-transfers-preview-dialog", "past-transfers-preview-overlay");

        fetchLocalPreview(previewContext.localUuid).done(function(localTransfer) {
            if (localTransfer) {
                previewContext.fromHie = false;
                previewContext.canConfirm = false;
                renderPreview(localTransfer);
                updateSaveButtonVisibility();
                setPreviewStatus(messages().alreadyLocal || "This transfer is already saved.", false);
                return;
            }
            fetchHiePreview(previewContext.transferId, previewContext.upid).done(function(hieTransfer) {
                if (!hieTransfer) {
                    jq("#past-transfers-preview-body").html(
                        "<p style='color:red;'>" + esc(messages().previewError || "Unable to load transfer preview.") + "</p>"
                    );
                    previewContext.fromHie = false;
                    previewContext.canConfirm = false;
                    updateSaveButtonVisibility();
                    return;
                }
                previewContext.fromHie = true;
                if (!previewContext.transferId) {
                    previewContext.transferId = hieTransfer.id || hieTransfer.uuid || hieTransfer.hieTransferId || "";
                }
                if (options.canConfirm == null) {
                    previewContext.canConfirm = hieTransfer.targetsCurrentFacility === true
                        || hieTransfer.targetsCurrentFacility === "true";
                }
                renderPreview(hieTransfer);
                updateSaveButtonVisibility();
            });
        });
    }

    function openTransferPreviewFromVisitRow(row) {
        openTransferPreview({
            patientId: jq.trim(row.attr("data-patient-id") || "") || null,
            visitId: jq.trim(row.attr("data-visit-id") || "") || null,
            upid: jq.trim(row.attr("data-upid") || ""),
            transferId: jq.trim(row.attr("data-transfer-id") || ""),
            localUuid: jq.trim(row.attr("data-local-uuid") || ""),
            canConfirm: true,
            sourceRow: row
        });
    }

    function markVisitRowAfterSave(transferId, localUuid) {
        var row = previewContext.sourceRow;
        if (!row || !row.length) {
            row = jq("tr.past-transfers-row").filter(function() {
                return String(jq(this).attr("data-visit-id") || "") === String(previewContext.visitId || "")
                    && String(jq(this).attr("data-patient-id") || "") === String(previewContext.patientId || "");
            });
        }
        if (!row.length) {
            return;
        }
        row.attr("data-transfer-id", transferId || "");
        row.attr("data-local-uuid", localUuid || "");
        var recordsCell = row.find("td.past-transfers-records-cell");
        if (!recordsCell.length) {
            recordsCell = row.find("td").eq(7);
        }
        if (recordsCell.length && transferId) {
            recordsCell.replaceWith(buildTransferRecordsCellHtml(transferId));
        }
        var actionCell = row.find("td.transfer-past-action");
        if (actionCell.length) {
            actionCell.html(
                "<a href='javascript:void(0);' class='button past-transfers-preview-link' title='"
                + esc(messages().previewTitle || "Preview transfer") + "'>"
                + "<i class='icon-eye-open'></i></a>"
            );
        }
    }

    function saveTransferAsCorrect() {
        var saveBtn = jq("#past-transfers-preview-save");
        if (!previewContext.patientId || !previewContext.transferId) {
            setPreviewStatus(messages().missingIds || "Missing transfer UUID or patient.", true);
            return;
        }
        saveBtn.prop("disabled", true);
        setPreviewStatus(messages().loading || "Saving...", false);

        var useValidate = previewContext.saveWithObs && !!previewContext.visitId;
        var request = {
            url: useValidate ? validateUrl() : receiveUrl(),
            type: "POST",
            data: {
                patientId: previewContext.patientId,
                hieTransferId: previewContext.transferId
            },
            dataType: "json",
            headers: { "Accept": "application/json" }
        };
        if (useValidate) {
            request.data.visitId = previewContext.visitId;
        }

        jq.ajax(request).done(function(response) {
            if (typeof response === "string") {
                try {
                    response = jq.parseJSON(response);
                } catch (err) {
                    setPreviewStatus(messages().saveError || "Unable to save transfer locally.", true);
                    saveBtn.prop("disabled", false);
                    return;
                }
            }
            if (response && response.status === "success") {
                previewContext.fromHie = false;
                previewContext.canConfirm = false;
                previewContext.localUuid = response.uuid || response.localTransferUuid || previewContext.localUuid;
                updateSaveButtonVisibility();
                setPreviewStatus(
                    response.message || messages().saveSuccess || "Transfer saved locally.",
                    false
                );
                markVisitRowAfterSave(previewContext.transferId, previewContext.localUuid || "");
                return;
            }
            setPreviewStatus(
                (response && response.message) || messages().saveError || "Unable to save transfer locally.",
                true
            );
            saveBtn.prop("disabled", false);
        }).fail(function(xhr) {
            var message = messages().saveError || "Unable to save transfer locally.";
            if (xhr && xhr.responseJSON && xhr.responseJSON.message) {
                message = xhr.responseJSON.message;
            }
            setPreviewStatus(message, true);
            saveBtn.prop("disabled", false);
        });
    }

    function formatIsoDate(date) {
        var year = date.getFullYear();
        var month = String(date.getMonth() + 1);
        if (month.length < 2) {
            month = "0" + month;
        }
        var day = String(date.getDate());
        if (day.length < 2) {
            day = "0" + day;
        }
        return year + "-" + month + "-" + day;
    }

    function resolveHieRange(visitDateIso) {
        if (!visitDateIso || !/^\d{4}-\d{2}-\d{2}$/.test(visitDateIso)) {
            return null;
        }
        var parts = visitDateIso.split("-");
        var visitDate = new Date(parseInt(parts[0], 10), parseInt(parts[1], 10) - 1, parseInt(parts[2], 10));
        if (isNaN(visitDate.getTime())) {
            return null;
        }
        var endDate = new Date(visitDate.getTime());
        endDate.setDate(endDate.getDate() + 1);
        var fromDate = new Date(endDate.getTime());
        fromDate.setDate(fromDate.getDate() - 31);
        return {
            fromDate: formatIsoDate(fromDate),
            endDate: formatIsoDate(endDate)
        };
    }

    function fromFacility(row) {
        return row.origin || row.referringFacilityName || row.hospitalName || row.sendingFacility || row.fromFacility || "";
    }

    function toFacility(row) {
        return row.destinationDisplay || row.destination || row.receivingFacility || row.toFacility || "";
    }

    function isTargetFacility(row) {
        return row.targetsCurrentFacility === true || row.targetsCurrentFacility === "true";
    }

    function formatVisitRangeLabel(visitStart, visitEnd) {
        var start = visitStart || "";
        var end = visitEnd || (messages().visitOpen || "Open");
        var prefix = messages().visitRangeLabel || "Visit";
        return prefix + ": " + start + " → " + end;
    }

    function renderHieList(transfers) {
        var meta = jq("#past-transfers-hie-meta");
        var visitLabel = formatVisitRangeLabel(hieListContext.visitStart, hieListContext.visitEnd);
        meta.text((hieListContext.patientName ? (hieListContext.patientName + " — ") : "") + visitLabel);

        if (!transfers || !transfers.length) {
            jq("#past-transfers-hie-body").html(
                "<p>" + esc(messages().listEmpty || "No transfers were found in HIE for this date range.") + "</p>"
            );
            return;
        }

        var rowsHtml = "";
        jq.each(transfers, function(index, row) {
            var transferId = row.hieTransferId || row.id || row.uuid || "";
            var canConfirm = isTargetFacility(row);
            rowsHtml += "<tr class='past-transfers-hie-row'"
                + " data-transfer-id='" + esc(transferId) + "'"
                + " data-can-confirm='" + (canConfirm ? "true" : "false") + "'>"
                + "<td>" + (index + 1) + "</td>"
                + "<td>" + displayValue(row.date) + "</td>"
                + "<td>" + displayValue(fromFacility(row)) + "</td>"
                + "<td>" + displayValue(toFacility(row)) + "</td>"
                + "<td class='past-transfers-hie-action'>"
                + "<a href='javascript:void(0);' class='button past-transfers-hie-preview-link' title='"
                + esc(messages().previewTitle || "Preview transfer") + "'>"
                + "<i class='icon-eye-open'></i></a>"
                + "</td>"
                + "</tr>";
        });

        jq("#past-transfers-hie-body").html(
            "<div class='transfer-table-wrapper past-transfers-hie-table-wrap'>"
            + "<table class='transfer-datatable display past-transfers-hie-table'>"
            + "<thead><tr>"
            + "<th>#</th><th>Date</th><th>From facility</th><th>To Facility</th><th>Action</th>"
            + "</tr></thead><tbody>" + rowsHtml + "</tbody></table></div>"
        );
    }

    function openHieDownloadList(row) {
        var upid = jq.trim(row.attr("data-upid") || "");
        var visitDate = jq.trim(row.attr("data-visit-date") || "");
        var visitEnd = jq.trim(row.attr("data-visit-end-date") || "");
        var patientName = jq.trim(row.attr("data-patient-name") || "");
        var range = resolveHieRange(visitDate);

        hieListContext.patientId = jq.trim(row.attr("data-patient-id") || "") || null;
        hieListContext.visitId = jq.trim(row.attr("data-visit-id") || "") || null;
        hieListContext.upid = upid;
        hieListContext.patientName = patientName;
        hieListContext.visitStart = visitDate;
        hieListContext.visitEnd = visitEnd;

        if (!upid || !range) {
            jq("#past-transfers-hie-meta").text("");
            jq("#past-transfers-hie-body").html(
                "<p style='color:red;'>" + esc(messages().listMissing || "UPID and visit date are required.") + "</p>"
            );
            showDialog("#past-transfers-hie-dialog", "past-transfers-hie-overlay");
            return;
        }

        jq("#past-transfers-hie-meta").text(
            (patientName ? (patientName + " — ") : "") + formatVisitRangeLabel(visitDate, visitEnd)
        );
        jq("#past-transfers-hie-body").html(
            "<div style='padding:10px;'><i class='icon-spinner icon-spin'></i> "
            + esc(messages().listLoading || "Loading transfers from HIE...") + "</div>"
        );
        showDialog("#past-transfers-hie-dialog", "past-transfers-hie-overlay");

        jq.ajax({
            url: restUrl(),
            type: "GET",
            data: {
                upid: upid,
                fromDate: range.fromDate,
                endDate: range.endDate,
                activeOnly: false
            },
            dataType: "json",
            headers: { "Accept": "application/json" }
        }).done(function(response) {
            if (typeof response === "string") {
                try {
                    response = jq.parseJSON(response);
                } catch (err) {
                    jq("#past-transfers-hie-body").html(
                        "<p style='color:red;'>" + esc(messages().listError || "Unable to load transfers from HIE.") + "</p>"
                    );
                    return;
                }
            }
            if (response && response.status === "error") {
                jq("#past-transfers-hie-body").html(
                    "<p style='color:red;'>" + esc(response.message || messages().listError || "Unable to load transfers from HIE.") + "</p>"
                );
                return;
            }
            renderHieList(response && response.data ? response.data : []);
        }).fail(function(xhr) {
            var message = messages().listError || "Unable to load transfers from HIE.";
            if (xhr && xhr.responseJSON && xhr.responseJSON.message) {
                message = xhr.responseJSON.message;
            }
            jq("#past-transfers-hie-body").html("<p style='color:red;'>" + esc(message) + "</p>");
        });
    }

    function listUrl() {
        return config().listUrl
            || ((window.transferOpenmrsPath || "") + "/module/transferapp/transfer/pastTransfersList.form");
    }

    function formatShowingStatus(loaded, total) {
        var template = messages().showingStatus || "Showing {0} of {1} visits";
        return String(template)
            .replace("{0}", String(loaded))
            .replace("{1}", String(total));
    }

    function updateListStatus(loaded, total, hasMore, nextOffset) {
        config().loadedCount = loaded;
        config().totalCount = total;
        config().hasMore = !!hasMore;
        config().nextOffset = nextOffset || loaded;
        jq("#past-transfers-status").text(formatShowingStatus(loaded, total));
        var loadMoreBtn = jq("#past-transfers-load-more");
        if (hasMore) {
            loadMoreBtn.show();
        } else {
            loadMoreBtn.hide();
        }
    }

    function buildActionHtml(row) {
        var hasTransfer = jq.trim(row.primaryTransferId || row.transferRecords || "") !== "";
        if (hasTransfer) {
            return "<a href='javascript:void(0);' class='button past-transfers-preview-link' title='"
                + esc(messages().previewTitle || "Preview transfer") + "'>"
                + "<i class='icon-eye-open'></i></a>";
        }
        return "<a href='javascript:void(0);' class='button past-transfers-download-link' title='"
            + esc(messages().downloadTitle || "Find transfers in HIE") + "'>"
            + "<i class='icon-download-alt'></i></a>";
    }

    function splitTransferRecordIds(raw) {
        var text = jq.trim(raw || "");
        if (!text) {
            return [];
        }
        var parts = text.split(",");
        var ids = [];
        for (var i = 0; i < parts.length; i++) {
            var id = jq.trim(parts[i] || "");
            if (id && ids.indexOf(id) < 0) {
                ids.push(id);
            }
        }
        return ids;
    }

    function buildTransferRecordsCellHtml(transferRecords) {
        var ids = splitTransferRecordIds(transferRecords);
        if (!ids.length) {
            return "<td class='past-transfers-records-cell'></td>";
        }
        return "<td class='past-transfers-records-cell'>"
            + "<a href='javascript:void(0);' class='past-transfers-ready-link' role='button' aria-haspopup='true'"
            + " data-transfer-records='" + esc(ids.join(", ")) + "'>"
            + esc(messages().transferReady || "Ready")
            + "</a></td>";
    }

    function closeTransferUuidPopover() {
        jq(".past-transfers-uuid-popover").remove();
        jq(".past-transfers-ready-link.is-open").removeClass("is-open");
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

    function openTransferUuidPopover(link) {
        closeTransferUuidPopover();
        var ids = splitTransferRecordIds(link.attr("data-transfer-records"));
        if (!ids.length) {
            return;
        }
        var title = messages().transferUuidTitle || "Transfer UUID";
        var copyLabel = messages().transferCopy || "Copy";
        var rowsHtml = "";
        for (var i = 0; i < ids.length; i++) {
            rowsHtml += "<div class='past-transfers-uuid-row'>"
                + "<code class='past-transfers-uuid-value'>" + esc(ids[i]) + "</code>"
                + "<button type='button' class='button past-transfers-uuid-copy' data-uuid='"
                + esc(ids[i]) + "'>" + esc(copyLabel) + "</button>"
                + "</div>";
        }
        var popover = jq(
            "<div class='past-transfers-uuid-popover' role='dialog'>"
            + "<div class='past-transfers-uuid-popover-title'>" + esc(title) + "</div>"
            + rowsHtml
            + "</div>"
        );
        jq("body").append(popover);
        link.addClass("is-open");

        var offset = link.offset();
        var top = offset.top + link.outerHeight() + 8;
        var left = offset.left;
        var maxLeft = jq(window).scrollLeft() + jq(window).width() - popover.outerWidth() - 12;
        if (left > maxLeft) {
            left = Math.max(12, maxLeft);
        }
        popover.css({ top: top, left: left });
    }

    function appendPastTransferRows(items) {
        var tbody = jq("#past-transfers-table tbody");
        if (!tbody.length || !items || !items.length) {
            return;
        }
        var html = "";
        jq.each(items, function(_, row) {
            var endCell = row.visitOpen
                ? ("<span class='transfer-past-open-visit'>" + esc(messages().visitOpen || "Open") + "</span>")
                : esc(row.visitEndDateDisplay || "");
            html += "<tr class='past-transfers-row'"
                + " data-patient-id='" + esc(row.patientId || "") + "'"
                + " data-visit-id='" + esc(row.visitId || "") + "'"
                + " data-upid='" + esc(row.upid || "") + "'"
                + " data-visit-date='" + esc(row.visitDateIso || "") + "'"
                + " data-visit-end-date='" + esc(row.visitEndDateIso || "") + "'"
                + " data-transfer-id='" + esc(row.primaryTransferId || "") + "'"
                + " data-local-uuid='" + esc(row.localTransferUuid || "") + "'"
                + " data-patient-name='" + esc(row.patientName || "") + "'>"
                + "<td>" + esc(row.rowNumber || "") + "</td>"
                + "<td>" + esc(row.upid || "") + "</td>"
                + "<td>" + esc(row.patientName || "") + "</td>"
                + "<td>" + esc(row.visitDateDisplay || "") + "</td>"
                + "<td>" + endCell + "</td>"
                + "<td>" + esc(row.insuranceType || "") + "</td>"
                + "<td>" + esc(row.insuranceId || "") + "</td>"
                + buildTransferRecordsCellHtml(row.transferRecords || row.primaryTransferId || "")
                + "<td class='transfer-past-action'>" + buildActionHtml(row) + "</td>"
                + "</tr>";
        });
        tbody.append(html);
    }

    function setLoadMoreStatus(text, isError) {
        var status = jq("#past-transfers-load-more-status");
        if (!status.length) {
            return;
        }
        if (!text) {
            status.hide().text("");
            return;
        }
        status.css("color", isError ? "#a94442" : "#334155").text(text).show();
    }

    function loadMorePastTransfers() {
        var btn = jq("#past-transfers-load-more");
        if (!btn.length || btn.prop("disabled")) {
            return;
        }
        var startDate = config().filterStartDate || "";
        var endDate = config().filterEndDate || "";
        var upid = jq.trim(config().filterUpid || "");
        var offset = config().nextOffset || 0;
        var limit = config().pageSize || 100;
        if (!startDate || !endDate) {
            setLoadMoreStatus(messages().loadMoreError || "Unable to load more visits.", true);
            return;
        }
        btn.prop("disabled", true);
        setLoadMoreStatus(messages().loadMoreLoading || "Loading more visits...", false);
        var requestData = {
            startDate: startDate,
            endDate: endDate,
            offset: offset,
            limit: limit
        };
        if (upid) {
            requestData.upid = upid;
        }
        jq.ajax({
            url: listUrl(),
            type: "GET",
            data: requestData,
            dataType: "json",
            headers: { "Accept": "application/json" }
        }).done(function(response) {
            if (typeof response === "string") {
                try {
                    response = jq.parseJSON(response);
                } catch (err) {
                    setLoadMoreStatus(messages().loadMoreError || "Unable to load more visits.", true);
                    btn.prop("disabled", false);
                    return;
                }
            }
            if (!response || response.status !== "success") {
                setLoadMoreStatus(
                    (response && response.message) || messages().loadMoreError || "Unable to load more visits.",
                    true
                );
                btn.prop("disabled", false);
                return;
            }
            appendPastTransferRows(response.items || []);
            updateListStatus(
                response.loadedCount || 0,
                response.totalCount || 0,
                response.hasMore === true || response.hasMore === "true",
                response.nextOffset || response.loadedCount || 0
            );
            setLoadMoreStatus("", false);
            btn.prop("disabled", false);
        }).fail(function(xhr) {
            var message = messages().loadMoreError || "Unable to load more visits.";
            if (xhr && xhr.responseJSON && xhr.responseJSON.message) {
                message = xhr.responseJSON.message;
            }
            setLoadMoreStatus(message, true);
            btn.prop("disabled", false);
        });
    }

    jq(function() {
        jq(document).on("click", "#past-transfers-load-more", function(event) {
            event.preventDefault();
            loadMorePastTransfers();
        });

        jq(document).on("click", ".past-transfers-ready-link", function(event) {
            event.preventDefault();
            event.stopPropagation();
            var link = jq(this);
            if (link.hasClass("is-open")) {
                closeTransferUuidPopover();
                return;
            }
            openTransferUuidPopover(link);
        });

        jq(document).on("click", ".past-transfers-uuid-copy", function(event) {
            event.preventDefault();
            event.stopPropagation();
            var btn = jq(this);
            var uuid = btn.attr("data-uuid") || "";
            copyTextToClipboard(uuid).done(function() {
                var original = messages().transferCopy || "Copy";
                btn.addClass("is-copied").text(messages().transferCopied || "Copied");
                window.setTimeout(function() {
                    btn.removeClass("is-copied").text(original);
                }, 1400);
            });
        });

        jq(document).on("click", ".past-transfers-uuid-popover", function(event) {
            event.stopPropagation();
        });

        jq(document).on("click", function() {
            closeTransferUuidPopover();
        });

        jq(window).on("resize scroll", function() {
            closeTransferUuidPopover();
        });

        jq(document).on("click", ".past-transfers-preview-link", function(event) {
            event.preventDefault();
            event.stopPropagation();
            closeTransferUuidPopover();
            openTransferPreviewFromVisitRow(jq(this).closest("tr.past-transfers-row"));
        });

        jq(document).on("click", ".past-transfers-download-link", function(event) {
            event.preventDefault();
            event.stopPropagation();
            closeTransferUuidPopover();
            openHieDownloadList(jq(this).closest("tr.past-transfers-row"));
        });

        jq(document).on("click", ".past-transfers-hie-preview-link", function(event) {
            event.preventDefault();
            event.stopPropagation();
            var hieRow = jq(this).closest("tr.past-transfers-hie-row");
            hideDialog("#past-transfers-hie-dialog", "past-transfers-hie-overlay");
            openTransferPreview({
                patientId: hieListContext.patientId,
                visitId: hieListContext.visitId,
                upid: hieListContext.upid,
                transferId: jq.trim(hieRow.attr("data-transfer-id") || ""),
                localUuid: "",
                canConfirm: hieRow.attr("data-can-confirm") === "true",
                saveWithObs: hieRow.attr("data-can-confirm") === "true",
                sourceRow: jq("tr.past-transfers-row").filter(function() {
                    return String(jq(this).attr("data-visit-id") || "") === String(hieListContext.visitId || "");
                }).first()
            });
        });

        jq(document).on("click", "#past-transfers-preview-close, #past-transfers-preview-overlay", function(event) {
            event.preventDefault();
            hideDialog("#past-transfers-preview-dialog", "past-transfers-preview-overlay");
        });

        jq(document).on("click", "#past-transfers-hie-close, #past-transfers-hie-overlay", function(event) {
            event.preventDefault();
            hideDialog("#past-transfers-hie-dialog", "past-transfers-hie-overlay");
        });

        jq(document).on("click", "#past-transfers-preview-save", function(event) {
            event.preventDefault();
            saveTransferAsCorrect();
        });
    });
})();
