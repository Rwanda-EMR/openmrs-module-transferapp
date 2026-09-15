(function(jq) {
    if (window.__hieTransferSectionBound) {
        return;
    }
    window.__hieTransferSectionBound = true;

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
        if (item.targetsCurrentFacility === true || item.targetsCurrentFacility === "true") {
            return true;
        }
        return false;
    }

    function initHieTransferSection() {
        var section = jq("#hie-transfer-section");
        var recordedCfg = jq("#transfer-recorded-hie-config");
        if (!section.length && !recordedCfg.length && !jq("#hie-transfer-preview-dialog").length) {
            return;
        }

        var previewDialogs = jq("#hie-transfer-preview-dialog");
        if (previewDialogs.length > 1) {
            previewDialogs.slice(1).remove();
        }

        var transferOpenmrsPath = (typeof openmrsContextPath !== "undefined" && openmrsContextPath)
            ? openmrsContextPath
            : "/openmrs";
        window.transferOpenmrsPath = transferOpenmrsPath;
        var runtime = {
            restUrl: normalizeRootUrl(transferOpenmrsPath + "/ws/rest/v1/transferapp/transfer"),
            validateUrl: normalizeRootUrl(transferOpenmrsPath + "/ws/rest/v1/transferapp/transfer/validate"),
            feedbackUrl: normalizeRootUrl(transferOpenmrsPath + "/ws/rest/v1/transferapp/transfer/feedback"),
            facilitiesUrl: normalizeRootUrl(transferOpenmrsPath + "/ws/rest/v1/transferapp/transfer/feedback/facilities"),
            feedbackPreviewUrl: normalizeRootUrl(transferOpenmrsPath + "/ws/rest/v1/transferapp/transfer/feedback/preview"),
            feedbackSubmitUrl: normalizeRootUrl(transferOpenmrsPath + "/ws/rest/v1/transferapp/transfer/feedback/submit"),
            pdfUrl: normalizeRootUrl(transferOpenmrsPath + "/module/transferapp/transfer/exportHiePdf.form"),
            upid: "",
            patientId: "",
            hasTransferId: false,
            listFromHie: false,
            canCreateTransfer: false,
            canValidate: false,
            canProvideFeedback: false,
            feedbackLoaded: false,
            feedbackCompleted: false,
            feedbackHieSent: false
        };
        var previewDialog = null;
        var previewScriptsLoading = null;
        var select2Loading = null;
        var facilitiesLoading = null;
        var counterReferralFacilities = null;
        var currentPreviewTransfer = null;

        function applyRuntimeConfig(link) {
            var source = link && link.length ? link : jq();
            var cfg = recordedCfg.length ? recordedCfg : jq();
            if (section.length) {
                runtime.restUrl = normalizeRootUrl(section.attr("data-rest-url") || runtime.restUrl);
                runtime.validateUrl = normalizeRootUrl(section.attr("data-validate-url") || runtime.validateUrl);
                runtime.feedbackUrl = normalizeRootUrl(section.attr("data-feedback-url") || runtime.feedbackUrl);
                runtime.facilitiesUrl = normalizeRootUrl(section.attr("data-facilities-url") || runtime.facilitiesUrl);
                runtime.feedbackPreviewUrl = normalizeRootUrl(section.attr("data-feedback-preview-url") || runtime.feedbackPreviewUrl);
                runtime.feedbackSubmitUrl = normalizeRootUrl(section.attr("data-feedback-submit-url") || runtime.feedbackSubmitUrl);
                runtime.pdfUrl = normalizeRootUrl(section.attr("data-pdf-url") || runtime.pdfUrl);
                runtime.upid = section.attr("data-upid") || runtime.upid;
                runtime.patientId = section.attr("data-patient-id") || runtime.patientId;
                runtime.hasTransferId = section.attr("data-has-transfer-id") === "true";
                runtime.listFromHie = section.attr("data-list-from-hie") === "true";
                runtime.canCreateTransfer = section.attr("data-can-validate") === "true";
                runtime.canProvideFeedback = section.attr("data-can-provide-feedback") === "true";
            }
            if (cfg.length) {
                runtime.restUrl = normalizeRootUrl(cfg.attr("data-rest-url") || runtime.restUrl);
                runtime.feedbackUrl = normalizeRootUrl(cfg.attr("data-feedback-url") || runtime.feedbackUrl);
                runtime.facilitiesUrl = normalizeRootUrl(cfg.attr("data-facilities-url") || runtime.facilitiesUrl);
                runtime.upid = cfg.attr("data-upid") || runtime.upid;
                runtime.patientId = cfg.attr("data-patient-id") || runtime.patientId;
                runtime.hasTransferId = cfg.attr("data-has-transfer-id") === "true" || runtime.hasTransferId;
                runtime.canProvideFeedback = cfg.attr("data-can-provide-feedback") === "true" || runtime.canProvideFeedback;
            }
            if (source.length) {
                runtime.restUrl = normalizeRootUrl(source.attr("data-rest-url") || runtime.restUrl);
                runtime.feedbackUrl = normalizeRootUrl(source.attr("data-feedback-url") || runtime.feedbackUrl);
                runtime.facilitiesUrl = normalizeRootUrl(source.attr("data-facilities-url") || runtime.facilitiesUrl);
                runtime.upid = source.attr("data-upid") || runtime.upid;
                runtime.patientId = source.attr("data-patient-id") || runtime.patientId;
                if (source.attr("data-can-provide-feedback") === "true") {
                    runtime.canProvideFeedback = true;
                } else if (source.attr("data-can-provide-feedback") === "false") {
                    runtime.canProvideFeedback = false;
                }
            }
            runtime.canValidate = runtime.canCreateTransfer && runtime.listFromHie && !runtime.hasTransferId;
        }

        applyRuntimeConfig(null);
        var upid = runtime.upid;
        var patientId = runtime.patientId;
        var hasTransferId = runtime.hasTransferId;
        var listFromHie = runtime.listFromHie;
        var showSection = section.length && section.attr("data-show-section") === "true";
        var canCreateTransfer = runtime.canCreateTransfer;
        var canValidate = runtime.canValidate;
        var canProvideFeedback = runtime.canProvideFeedback;
        var restUrl = runtime.restUrl;
        var validateUrl = runtime.validateUrl;
        var feedbackUrl = runtime.feedbackUrl;
        var facilitiesUrl = runtime.facilitiesUrl;
        var previewResourcesBase = normalizeRootUrl(transferOpenmrsPath + "/moduleResources/transferapp/scripts/");
        var resourcesBase = normalizeRootUrl(transferOpenmrsPath + "/moduleResources/transferapp/");

        function ensurePreviewDialog() {
            return jq("#hie-transfer-preview-dialog").length > 0;
        }

        function ensureTransferPreviewRenderer(callback) {
            if (typeof ensureTransferPreviewAssets === "function") {
                ensureTransferPreviewAssets(callback);
                return;
            }
            if (typeof buildTransferFormPreviewHtml === "function") {
                callback();
                return;
            }
            if (previewScriptsLoading) {
                previewScriptsLoading.done(callback);
                return;
            }
            previewScriptsLoading = jq.getScript(previewResourcesBase + "transferPreviewCommon.js")
                .done(callback)
                .fail(function() {
                    jq("#hie-transfer-preview-body").html("<p style='color:red;'>Unable to load transfer preview renderer.</p>");
                });
        }

        function showPreviewDialog() {
            var dialogEl = jq("#hie-transfer-preview-dialog");
            if (dialogEl.length && dialogEl.parent()[0] !== document.body) {
                dialogEl.appendTo(document.body);
            }
            if (previewDialog == null && typeof emr !== "undefined" && typeof emr.setupConfirmationDialog === "function") {
                previewDialog = emr.setupConfirmationDialog({
                    selector: "#hie-transfer-preview-dialog",
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
            jq("#hie-transfer-preview-dialog").hide();
        }

        function updateValidateButton(transfer) {
            var validateBtn = jq("#hie-transfer-validate-btn");
            var exportBtn = jq("#hie-transfer-export-pdf-btn");
            var validateStatus = jq("#hie-transfer-validate-status");
            validateStatus.hide().text("");
            currentPreviewTransfer = transfer || null;

            var transferUuid = transfer
                ? (transfer.uuid || transfer.id || transfer.hieTransferId || "")
                : "";
            var previewLoaded = !!(transfer && transferUuid);

            if (hasTransferId && previewLoaded) {
                exportBtn.show().prop("disabled", false);
            } else {
                exportBtn.hide().prop("disabled", false);
            }

            if (!canValidate || !transfer || !targetsCurrentFacility(transfer)) {
                validateBtn.hide().prop("disabled", false).text(validateBtn.data("default-label") || validateBtn.text());
                return;
            }

            if (!transferUuid) {
                validateBtn.hide();
                return;
            }

            if (!validateBtn.data("default-label")) {
                validateBtn.data("default-label", validateBtn.text());
            }
            validateBtn
                .show()
                .prop("disabled", false)
                .text(validateBtn.data("default-label"))
                .attr("data-transfer-id", transferUuid);
        }

        function exportCurrentPreviewPdf() {
            if (!hasTransferId || !currentPreviewTransfer) {
                return;
            }
            patientId = runtime.patientId || patientId;
            upid = runtime.upid || upid;
            var transferUuid = currentPreviewTransfer.uuid
                || currentPreviewTransfer.id
                || currentPreviewTransfer.hieTransferId
                || "";
            if (!patientId || !transferUuid) {
                jq("#hie-transfer-validate-status").show().css("color", "#a94442")
                    .text("Unable to export PDF: missing patient or transfer id.");
                return;
            }
            var pdfUrl = runtime.pdfUrl
                || normalizeRootUrl(transferOpenmrsPath + "/module/transferapp/transfer/exportHiePdf.form");
            var query = "patientId=" + encodeURIComponent(patientId)
                + "&hieTransferId=" + encodeURIComponent(transferUuid);
            if (upid) {
                query += "&upid=" + encodeURIComponent(upid);
            }
            window.location.href = pdfUrl + (pdfUrl.indexOf("?") >= 0 ? "&" : "?") + query;
        }

        function renderPreview(transfer) {
            function afterBasePreview() {
                updateValidateButton(transfer);
                runtime.feedbackLoaded = false;
                jq("#hie-transfer-preview-dialog").removeClass("has-feedback");
                jq("#hie-transfer-feedback-wrap").hide();
                jq("#hie-fb-status").hide().text("");
                jq("#hie-fb-preview-hint").hide();
                if (currentPreviewTransfer) {
                    delete currentPreviewTransfer.referralFeedback;
                }
                runtime.feedbackCompleted = false;
                runtime.feedbackHieSent = false;
                updateProvideFeedbackButton(transfer);
                loadSavedFeedbackIntoPreview(transfer);
            }

            if (typeof renderTransferPreviewInto === "function") {
                renderTransferPreviewInto("#hie-transfer-preview-body", transfer, afterBasePreview);
                return;
            }
            var previewHtml = typeof buildTransferFormPreviewHtml === "function"
                ? buildTransferFormPreviewHtml(transfer)
                : "<p style='color:red;'>Preview renderer not loaded.</p>";
            jq("#hie-transfer-preview-body").html(previewHtml);
            afterBasePreview();
        }

        function feedbackButtonLabels() {
            var btn = jq("#hie-transfer-provide-feedback-btn");
            return {
                provide: btn.attr("data-label-provide") || "Provide feedback",
                send: btn.attr("data-label-send") || "Send Feedback",
                sending: btn.attr("data-label-sending") || "Sending feedback..."
            };
        }

        function setFeedbackActionMode(mode) {
            var btn = jq("#hie-transfer-provide-feedback-btn");
            if (!btn.length) {
                return;
            }
            var labels = feedbackButtonLabels();
            var nextMode = mode === "send" ? "send" : "provide";
            btn.attr("data-mode", nextMode);
            if (nextMode === "send") {
                btn.addClass("hie-fb-mode-send").text(labels.send);
            } else {
                btn.removeClass("hie-fb-mode-send").text(labels.provide);
            }
        }

        function updateProvideFeedbackButton(transfer) {
            var provideBtn = jq("#hie-transfer-provide-feedback-btn");
            if (!provideBtn.length) {
                return;
            }
            provideBtn.hide().prop("disabled", false);
            if (!canProvideFeedback || !transfer || !isRecordedTransferPreview(
                    transfer.uuid || transfer.id || transfer.hieTransferId || "")) {
                return;
            }
            // Already sent to HIE — no action button.
            if (runtime.feedbackHieSent) {
                return;
            }
            if (runtime.feedbackCompleted) {
                setFeedbackActionMode("send");
                provideBtn.show();
                return;
            }
            // While entering feedback, keep the action button hidden.
            if (jq("#hie-transfer-feedback-wrap").is(":visible")) {
                return;
            }
            setFeedbackActionMode("provide");
            provideBtn.show();
        }

        function showReferralFeedbackForm() {
            if (!canProvideFeedback || !currentPreviewTransfer) {
                return;
            }
            loadReferralFeedback(currentPreviewTransfer, true);
        }

        function hideReferralFeedback() {
            jq("#hie-transfer-preview-dialog").removeClass("has-feedback");
            jq("#hie-transfer-feedback-wrap").hide();
            jq("#hie-fb-status").hide().text("");
            jq("#hie-fb-preview-hint").hide();
            jq("#hie-transfer-provide-feedback-btn").hide();
        }

        function selectedReferBackFacility() {
            var $select = jq("#hie-fb-refer-back");
            var $opt = $select.find("option:selected");
            var name = jq.trim(($opt.attr("data-name") || $opt.val() || ""));
            var code = jq.trim(($opt.attr("data-code") || ""));
            return { name: name, fosaId: code };
        }

        function feedbackSummaryFromResponse(response) {
            var summary = (response && response.summary) || {};
            var feedback = (response && response.feedback) || {};
            var defaults = (response && response.defaults) || {};
            return {
                finalDiagnosis: summary.finalDiagnosis || feedback.finalDiagnosis || defaults.finalDiagnosis || "",
                treatmentGiven: summary.treatmentGiven || feedback.treatmentGiven || defaults.treatmentGiven || "",
                outcome: summary.outcome || feedback.outcome || defaults.outcome || "",
                outcomeLabel: summary.outcomeLabel || feedback.outcomeLabel || "",
                recommendations: summary.recommendations || feedback.recommendations || defaults.recommendations || "",
                referBackToFacility: summary.referBackToFacility || feedback.referBackToFacility
                    || defaults.referBackToFacility || "",
                referBackToFacilityFosaId: summary.referBackToFacilityFosaId
                    || feedback.referBackToFacilityFosaId || defaults.referBackToFacilityFosaId || "",
                dateOfAdmissionOrSeen: summary.dateOfAdmissionOrSeen
                    || feedback.dateOfAdmissionOrSeen || defaults.dateOfAdmissionOrSeen || "",
                dateOfDischarge: summary.dateOfDischarge || feedback.dateOfDischarge
                    || defaults.dateOfDischarge || "",
                contactPerson: summary.contactPerson || feedback.contactPerson || defaults.contactPerson || "",
                providerName: summary.providerName || feedback.providerName || defaults.providerName || "",
                qualification: summary.qualification || feedback.qualification || defaults.qualification || "",
                signedDate: summary.signedDate || feedback.signedDate || defaults.signedDate || "",
                signedTime: summary.signedTime || feedback.signedTime || defaults.signedTime || "",
                phone: summary.phone || feedback.phone || defaults.phone || "",
                clientName: summary.clientName || "",
                sex: summary.sex || "",
                ageOrDob: summary.ageOrDob || ""
            };
        }

        function hasCompletedFeedbackData(response) {
            if (!response) {
                return false;
            }
            if (response.completed === true || response.completed === "true") {
                return true;
            }
            var summary = feedbackSummaryFromResponse(response);
            return !!(summary.finalDiagnosis || summary.treatmentGiven || summary.outcome
                || summary.recommendations || summary.dateOfDischarge);
        }

        function refreshTransferPreviewWithFeedback(response) {
            if (!canProvideFeedback || !currentPreviewTransfer) {
                return;
            }
            if (!hasCompletedFeedbackData(response)) {
                return;
            }
            var summary = feedbackSummaryFromResponse(response);
            currentPreviewTransfer.referralFeedback = summary;
            var previewHtml = typeof buildTransferFormPreviewHtml === "function"
                ? buildTransferFormPreviewHtml(currentPreviewTransfer)
                : "";
            if (previewHtml) {
                jq("#hie-transfer-preview-body").html(previewHtml);
            }
            var hieSent = response && (response.hieSent === true || response.hieSent === "true");
            runtime.feedbackCompleted = true;
            runtime.feedbackHieSent = hieSent;
            runtime.feedbackLoaded = true;
            if (hieSent) {
                jq("#hie-fb-preview-hint").hide();
                jq("#hie-transfer-feedback-wrap").hide();
                jq("#hie-transfer-preview-dialog").removeClass("has-feedback");
            } else if (jq("#hie-transfer-feedback-wrap").is(":visible")) {
                jq("#hie-fb-preview-hint").show();
            }
            updateProvideFeedbackButton(currentPreviewTransfer);
        }

        /**
         * When preview opens for a recorded inbound transfer, fetch saved feedback (if any)
         * and append REFERRAL FEEDBACK / COUNTER-REFERRAL onto the paper form.
         */
        function loadSavedFeedbackIntoPreview(transfer) {
            if (!canProvideFeedback || !patientId || !transfer) {
                return;
            }
            var transferUuid = transfer.uuid || transfer.id || transfer.hieTransferId || "";
            if (!isRecordedTransferPreview(transferUuid)) {
                return;
            }
            feedbackUrl = runtime.feedbackUrl || feedbackUrl;
            jq.ajax({
                url: feedbackUrl,
                type: "GET",
                data: {
                    patientId: patientId,
                    hieTransferId: transferUuid
                },
                dataType: "json",
                headers: { "Accept": "application/json" }
            }).done(function(response) {
                if (!response || response.status === "error") {
                    return;
                }
                // Ignore stale responses if the user opened another transfer.
                var currentId = currentPreviewTransfer
                    ? (currentPreviewTransfer.uuid || currentPreviewTransfer.id
                        || currentPreviewTransfer.hieTransferId || "")
                    : "";
                if (currentId && currentId !== transferUuid) {
                    return;
                }
                refreshTransferPreviewWithFeedback(response);
            });
        }

        function originFacilityName(transfer) {
            if (!transfer) {
                return "";
            }
            return transfer.origin || transfer.referringFacilityName || transfer.hospitalName
                || transfer.sendingFacility || "";
        }

        function normalizeFacilityKey(value) {
            return String(value || "").trim().toLowerCase().replace(/\s+/g, " ");
        }

        function bridgeSelect2() {
            if (typeof jq.fn.select2 === "function") {
                return true;
            }
            if (typeof jQuery !== "undefined" && typeof jQuery.fn.select2 === "function") {
                jq.fn.select2 = jQuery.fn.select2;
                if (jQuery.fn.select2.defaults) {
                    jq.fn.select2.defaults = jQuery.fn.select2.defaults;
                }
                return true;
            }
            return false;
        }

        function ensureSelect2Css() {
            if (document.getElementById("hie-feedback-select2-css")) {
                return;
            }
            jq("head").append(
                "<link id='hie-feedback-select2-css' rel='stylesheet' type='text/css' href='"
                + resourcesBase + "styles/select2.min.css' />"
            );
        }

        function ensureSelect2(callback) {
            ensureSelect2Css();
            if (bridgeSelect2()) {
                callback(true);
                return;
            }
            if (select2Loading) {
                select2Loading.done(function() {
                    callback(bridgeSelect2());
                }).fail(function() {
                    callback(false);
                });
                return;
            }
            select2Loading = jq.getScript(resourcesBase + "scripts/select2/select2.min.js");
            select2Loading.done(function() {
                callback(bridgeSelect2());
            }).fail(function() {
                callback(false);
            });
        }

        function destroyReferBackSelect2() {
            var $select = jq("#hie-fb-refer-back");
            if ($select.length && $select.hasClass("select2-hidden-accessible") && typeof jq.fn.select2 === "function") {
                $select.select2("destroy");
            }
        }

        function applyReferBackSelect2() {
            var $select = jq("#hie-fb-refer-back");
            if (!$select.length || typeof jq.fn.select2 !== "function") {
                return;
            }
            destroyReferBackSelect2();
            $select.select2({
                width: "100%",
                placeholder: $select.attr("data-placeholder") || "Search and select a facility",
                allowClear: true,
                dropdownParent: jq("body")
            });
        }

        function appendReferBackOption($select, name, code, category) {
            var facilityName = jq.trim(name || "");
            if (!facilityName) {
                return;
            }
            var label = facilityName;
            if (category) {
                label += " — " + category;
            }
            $select.append(
                jq("<option></option>")
                    .val(facilityName)
                    .text(label)
                    .attr("data-name", facilityName)
                    .attr("data-code", code || "")
                    .attr("data-category", category || "")
            );
        }

        function populateReferBackOptions(facilities, preferredName) {
            var $select = jq("#hie-fb-refer-back");
            if (!$select.length) {
                return;
            }
            destroyReferBackSelect2();
            $select.empty().append(jq("<option value=''></option>"));
            var preferred = jq.trim(preferredName || "");
            var preferredKey = normalizeFacilityKey(preferred);
            var matched = false;
            jq.each(facilities || [], function(_, facility) {
                var name = (facility && facility.name) || "";
                appendReferBackOption($select, name, facility && facility.code, facility && facility.category);
                if (preferredKey && normalizeFacilityKey(name) === preferredKey) {
                    matched = true;
                }
            });
            if (preferred && !matched) {
                appendReferBackOption($select, preferred, "", "");
            }
            if (preferredKey) {
                var selectedVal = "";
                $select.find("option").each(function() {
                    if (normalizeFacilityKey(jq(this).attr("data-name") || jq(this).val()) === preferredKey) {
                        selectedVal = jq(this).val();
                        return false;
                    }
                });
                $select.val(selectedVal);
            }
        }

        function loadCounterReferralFacilities(callback) {
            if (counterReferralFacilities) {
                callback(counterReferralFacilities);
                return;
            }
            if (facilitiesLoading) {
                facilitiesLoading.done(function(response) {
                    callback((response && response.facilities) || counterReferralFacilities || []);
                }).fail(function() {
                    callback([]);
                });
                return;
            }
            facilitiesUrl = runtime.facilitiesUrl || facilitiesUrl;
            facilitiesLoading = jq.ajax({
                url: facilitiesUrl,
                type: "GET",
                dataType: "json",
                headers: { "Accept": "application/json" }
            }).done(function(response) {
                if (response && response.status === "success") {
                    counterReferralFacilities = response.facilities || [];
                } else {
                    counterReferralFacilities = [];
                    jq("#hie-fb-status").show().css("color", "#a94442")
                        .text((response && response.message) || "Unable to load facilities from the registry.");
                }
            }).fail(function() {
                counterReferralFacilities = [];
                jq("#hie-fb-status").show().css("color", "#a94442")
                    .text("Unable to load facilities from the registry.");
            }).always(function() {
                callback(counterReferralFacilities || []);
            });
        }

        function ensureReferBackFacilitySelect(preferredName, enabled) {
            var $select = jq("#hie-fb-refer-back");
            if (!$select.length) {
                return;
            }
            loadCounterReferralFacilities(function(facilities) {
                populateReferBackOptions(facilities, preferredName);
                ensureSelect2(function(ready) {
                    if (ready) {
                        applyReferBackSelect2();
                    }
                    $select.prop("disabled", !enabled);
                    if ($select.hasClass("select2-hidden-accessible") && typeof jq.fn.select2 === "function") {
                        $select.trigger("change.select2");
                    }
                });
            });
        }

        function setFeedbackFormEnabled(enabled) {
            var form = jq("#hie-transfer-feedback-form");
            form.find("input, textarea, button, select").each(function() {
                var el = jq(this);
                var fieldId = el.attr("id");
                if (fieldId === "hie-fb-admission" || fieldId === "hie-fb-provider"
                    || fieldId === "hie-fb-qualification") {
                    el.prop("readonly", true);
                    return;
                }
                el.prop("disabled", !enabled);
            });
            var $referBack = jq("#hie-fb-refer-back");
            if ($referBack.hasClass("select2-hidden-accessible") && typeof jq.fn.select2 === "function") {
                $referBack.trigger("change.select2");
            }
        }

        function renderOutcomeOptions(outcomes, selected) {
            var wrap = jq("#hie-fb-outcome-options");
            var html = [];
            var list = outcomes && outcomes.length ? outcomes : [
                { code: "STABILIZED_CURED", label: "Stabilized/Cured" },
                { code: "DIED", label: "Died" },
                { code: "ESCAPED", label: "Escaped" },
                { code: "TO_BE_FOLLOWED_UP", label: "To be followed up" },
                { code: "REFERRED_TO_HIGH_LEVEL", label: "Referred to high level" }
            ];
            for (var i = 0; i < list.length; i++) {
                var code = list[i].code || "";
                var label = list[i].label || code;
                html.push(
                    "<label><input type='radio' name='hie-fb-outcome' value='" + esc(code) + "'"
                    + (selected && selected === code ? " checked='checked'" : "")
                    + " required='required'/> " + esc(label) + "</label>"
                );
            }
            wrap.html(html.join(""));
        }

        function fillFeedbackForm(defaults, transfer, profileDefaults) {
            defaults = defaults || {};
            profileDefaults = profileDefaults || {};
            jq("#hie-fb-admission").val(defaults.dateOfAdmissionOrSeen || "");
            jq("#hie-fb-discharge").val(defaults.dateOfDischarge || "");
            jq("#hie-fb-diagnosis").val(defaults.finalDiagnosis || "");
            jq("#hie-fb-treatment").val(defaults.treatmentGiven || "");
            jq("#hie-fb-recommendations").val(defaults.recommendations || "");
            jq("#hie-fb-contact").val(defaults.contactPerson || "");
            jq("#hie-fb-provider").val(defaults.providerName || profileDefaults.providerName || "");
            jq("#hie-fb-qualification").val(defaults.qualification || profileDefaults.qualification || "");
            jq("#hie-fb-signed-date").val(defaults.signedDate || "");
            jq("#hie-fb-signed-time").val(defaults.signedTime || "");
            jq("#hie-fb-phone").val(defaults.phone || profileDefaults.phone || "");
        }

        function recordedTransferId() {
            return jq("#patient-open-recorded-transfer").attr("data-transfer-id")
                || section.attr("data-transfer-id")
                || "";
        }

        function isRecordedTransferPreview(transferUuid) {
            if (!transferUuid) {
                return false;
            }
            if (hasTransferId) {
                return true;
            }
            var recordedId = recordedTransferId();
            return !!recordedId && recordedId === transferUuid;
        }

        function loadReferralFeedback(transfer, showForm) {
            if (!patientId || !transfer || !canProvideFeedback) {
                hideReferralFeedback();
                return;
            }
            var transferUuid = transfer.uuid || transfer.id || transfer.hieTransferId || "";
            if (!isRecordedTransferPreview(transferUuid)) {
                hideReferralFeedback();
                return;
            }
            if (!showForm) {
                return;
            }

            jq("#hie-transfer-preview-dialog").addClass("has-feedback");
            jq("#hie-transfer-feedback-wrap").show();
            jq("#hie-transfer-provide-feedback-btn").hide();
            jq("#hie-fb-status").hide().text("");
            setFeedbackFormEnabled(false);
            facilitiesUrl = runtime.facilitiesUrl || facilitiesUrl;

            jq.ajax({
                url: feedbackUrl,
                type: "GET",
                data: {
                    patientId: patientId,
                    hieTransferId: transferUuid
                },
                dataType: "json",
                headers: { "Accept": "application/json" }
            }).done(function(response) {
                if (response && response.status === "error") {
                    jq("#hie-fb-status").show().css("color", "#a94442")
                        .text(response.message || "Unable to load referral feedback.");
                    return;
                }
                renderOutcomeOptions(response.outcomes, (response.defaults && response.defaults.outcome) || "");
                fillFeedbackForm(response.defaults, transfer, response.profileDefaults);
                var canSubmit = response.canSubmit === true || response.canSubmit === "true";
                setFeedbackFormEnabled(canSubmit);
                ensureReferBackFacilitySelect(
                    (response.defaults && response.defaults.referBackToFacility) || originFacilityName(transfer),
                    canSubmit
                );
                if (response.completed) {
                    refreshTransferPreviewWithFeedback({
                        feedback: response.feedback || response.defaults,
                        hieSent: response.hieSent
                    });
                    if (!canSubmit) {
                        jq("#hie-fb-status").show().css("color", "#0f766e")
                            .text("Feedback already saved for this transfer.");
                    }
                } else {
                    runtime.feedbackCompleted = false;
                    runtime.feedbackHieSent = false;
                    jq("#hie-fb-preview-hint").hide();
                    updateProvideFeedbackButton(transfer);
                }
            }).fail(function() {
                jq("#hie-fb-status").show().css("color", "#a94442").text("Unable to load referral feedback.");
            });
        }

        function selectedOutcome() {
            return jq("input[name='hie-fb-outcome']:checked").val() || "";
        }

        function saveReferralFeedback(e) {
            if (e) {
                e.preventDefault();
            }
            canProvideFeedback = runtime.canProvideFeedback || canProvideFeedback;
            patientId = runtime.patientId || patientId;
            feedbackUrl = runtime.feedbackUrl || feedbackUrl;
            if (!canProvideFeedback || !currentPreviewTransfer || !patientId) {
                jq("#hie-fb-status").show().css("color", "#a94442")
                    .text("You do not have permission to save referral feedback.");
                return;
            }
            var transferUuid = currentPreviewTransfer.uuid
                || currentPreviewTransfer.id
                || currentPreviewTransfer.hieTransferId
                || "";
            if (!transferUuid) {
                return;
            }
            var saveBtn = jq("#hie-fb-save");
            var actionBtn = jq("#hie-transfer-provide-feedback-btn");
            var statusEl = jq("#hie-fb-status");
            var validateStatus = jq("#hie-transfer-validate-status");
            var referBack = selectedReferBackFacility();
            saveBtn.prop("disabled", true).text("Saving feedback...");
            actionBtn.prop("disabled", true);
            statusEl.hide().text("");
            validateStatus.hide().text("");

            jq.ajax({
                url: feedbackUrl,
                type: "POST",
                data: {
                    patientId: patientId,
                    hieTransferId: transferUuid,
                    dateOfDischarge: jq("#hie-fb-discharge").val(),
                    dateOfAdmissionOrSeen: jq("#hie-fb-admission").val(),
                    finalDiagnosis: jq("#hie-fb-diagnosis").val(),
                    treatmentGiven: jq("#hie-fb-treatment").val(),
                    outcome: selectedOutcome(),
                    recommendations: jq("#hie-fb-recommendations").val(),
                    referBackToFacility: referBack.name,
                    referBackToFacilityFosaId: referBack.fosaId,
                    contactPerson: jq("#hie-fb-contact").val(),
                    providerName: jq("#hie-fb-provider").val(),
                    qualification: jq("#hie-fb-qualification").val(),
                    signedDate: jq("#hie-fb-signed-date").val(),
                    signedTime: jq("#hie-fb-signed-time").val(),
                    phone: jq("#hie-fb-phone").val()
                },
                dataType: "json",
                headers: { "Accept": "application/json" }
            }).done(function(response) {
                if (response && response.status === "success") {
                    var savedMsg = response.message
                        || "Referral feedback saved. Review the form above, then click Send Feedback.";
                    saveBtn.prop("disabled", false).text("Save feedback");
                    actionBtn.prop("disabled", false);
                    jq("#hie-transfer-feedback-wrap").hide();
                    jq("#hie-transfer-preview-dialog").removeClass("has-feedback");
                    jq("#hie-fb-preview-hint").hide();
                    jq("#hie-transfer-validate-status").show().css("color", "#0f766e").text(savedMsg);
                    refreshTransferPreviewWithFeedback(response);
                    return;
                }
                saveBtn.prop("disabled", false).text("Save feedback");
                actionBtn.prop("disabled", false);
                statusEl.show().css("color", "#a94442")
                    .text((response && response.message) || "Unable to save referral feedback.");
            }).fail(function(xhr) {
                saveBtn.prop("disabled", false).text("Save feedback");
                actionBtn.prop("disabled", false);
                var message = "Unable to save referral feedback.";
                if (xhr && xhr.responseJSON && xhr.responseJSON.message) {
                    message = xhr.responseJSON.message;
                }
                statusEl.show().css("color", "#a94442").text(message);
            });
        }

        function submitReferralFeedbackToHie() {
            canProvideFeedback = runtime.canProvideFeedback || canProvideFeedback;
            patientId = runtime.patientId || patientId;
            var submitUrl = runtime.feedbackSubmitUrl || (feedbackUrl + "/submit");
            if (!canProvideFeedback || !currentPreviewTransfer || !patientId) {
                jq("#hie-transfer-validate-status").show().css("color", "#a94442")
                    .text("You do not have permission to send referral feedback.");
                return;
            }
            var transferUuid = currentPreviewTransfer.uuid
                || currentPreviewTransfer.id
                || currentPreviewTransfer.hieTransferId
                || "";
            if (!transferUuid) {
                return;
            }
            if (!runtime.feedbackCompleted || runtime.feedbackHieSent) {
                return;
            }
            var actionBtn = jq("#hie-transfer-provide-feedback-btn");
            var saveBtn = jq("#hie-fb-save");
            var statusEl = jq("#hie-fb-status");
            var validateStatus = jq("#hie-transfer-validate-status");
            var labels = feedbackButtonLabels();
            actionBtn.prop("disabled", true).text(labels.sending);
            saveBtn.prop("disabled", true);
            statusEl.hide().text("");
            validateStatus.hide().text("");

            jq.ajax({
                url: submitUrl,
                type: "POST",
                data: {
                    patientId: patientId,
                    hieTransferId: transferUuid
                },
                dataType: "json",
                headers: { "Accept": "application/json" }
            }).done(function(response) {
                if (response && response.status === "success") {
                    var okMessage = response.message || "Referral feedback sent to HIE.";
                    statusEl.hide().text("");
                    validateStatus.hide().text("");
                    if (typeof emr !== "undefined" && typeof emr.successMessage === "function") {
                        emr.successMessage(okMessage);
                    } else if (typeof toastr !== "undefined" && typeof toastr.success === "function") {
                        toastr.success(okMessage);
                    } else {
                        validateStatus.show().css("color", "#0f766e").text(okMessage);
                    }
                    setFeedbackFormEnabled(false);
                    refreshTransferPreviewWithFeedback(jq.extend({}, response, { hieSent: true }));
                    actionBtn.prop("disabled", false);
                    saveBtn.prop("disabled", true).text("Save feedback");
                    return;
                }
                actionBtn.prop("disabled", false);
                setFeedbackActionMode("send");
                saveBtn.prop("disabled", false);
                var err = (response && response.message) || "Unable to send referral feedback to HIE.";
                statusEl.show().css("color", "#a94442").text(err);
                validateStatus.show().css("color", "#a94442").text(err);
            }).fail(function(xhr) {
                actionBtn.prop("disabled", false);
                setFeedbackActionMode("send");
                saveBtn.prop("disabled", false);
                var message = "Unable to send referral feedback to HIE.";
                if (xhr && xhr.responseJSON && xhr.responseJSON.message) {
                    message = xhr.responseJSON.message;
                }
                statusEl.show().css("color", "#a94442").text(message);
                validateStatus.show().css("color", "#a94442").text(message);
            });
        }

        function onFeedbackActionClick(e) {
            if (e) {
                e.preventDefault();
            }
            var mode = jq("#hie-transfer-provide-feedback-btn").attr("data-mode") || "provide";
            if (mode === "send") {
                submitReferralFeedbackToHie();
                return;
            }
            showReferralFeedbackForm();
        }

        function loadTransferPreview(transferId, patientUpid, link) {
            applyRuntimeConfig(link || jq());
            upid = runtime.upid || patientUpid;
            patientId = runtime.patientId || patientId;
            hasTransferId = runtime.hasTransferId;
            canValidate = runtime.canValidate;
            canProvideFeedback = runtime.canProvideFeedback;
            canCreateTransfer = runtime.canCreateTransfer;
            restUrl = runtime.restUrl;
            validateUrl = runtime.validateUrl;
            feedbackUrl = runtime.feedbackUrl;
            var effectiveUpid = patientUpid || upid;

            if (!ensurePreviewDialog()) {
                return;
            }
            if (!transferId || !effectiveUpid) {
                jq("#hie-transfer-preview-body").html("<p style='color:red;'>Missing transfer UUID or UPID.</p>");
                updateValidateButton(null);
                hideReferralFeedback();
                showPreviewDialog();
                return;
            }

            jq("#hie-transfer-preview-body").html(
                "<div style='padding:10px;'><i class='icon-spinner icon-spin'></i> Loading transfer information...</div>"
            );
            updateValidateButton(null);
            hideReferralFeedback();
            showPreviewDialog();

            jq.ajax({
                url: restUrl,
                type: "GET",
                data: {
                    upid: effectiveUpid,
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
                        jq("#hie-transfer-preview-body").html("<p style='color:red;'>Transfer endpoint returned non-JSON response.</p>");
                        updateValidateButton(null);
                        hideReferralFeedback();
                        return;
                    }
                }
                if (response && response.status === "error") {
                    jq("#hie-transfer-preview-body").html(
                        "<p style='color:red;'>" + esc(response.message || "Unable to load transfer.") + "</p>"
                    );
                    updateValidateButton(null);
                    hideReferralFeedback();
                    return;
                }
                var items = response && response.data ? response.data : [];
                if (items.length) {
                    ensureTransferPreviewRenderer(function() {
                        renderPreview(items[0]);
                    });
                    return;
                }
                jq("#hie-transfer-preview-body").html("<p style='color:red;'>No matching transfer found in HIE.</p>");
                updateValidateButton(null);
                hideReferralFeedback();
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
                jq("#hie-transfer-preview-body").html("<p style='color:red;'>" + esc(message) + "</p>");
                updateValidateButton(null);
                hideReferralFeedback();
            });
        }

        function transferListRowHtml(item) {
            var uuid = item.uuid || item.id || "";
            var date = item.date || item.transferDecisionDatetime || item.admissionDatetime || "";
            var from = item.origin || item.referringFacilityName || item.hospitalName || "";
            var destination = resolveDestination(item);
            var status = item.status || "";
            var statusClass = "transfer-status-pending";
            if (item.reuseScheduled === true || item.reuseScheduled === "true") {
                statusClass = "transfer-status-approved";
                if (!status) {
                    status = "Scheduled reuse";
                }
            } else if (item.agentRejected === true || item.agentRejected === "true") {
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
                + "<tr class='hie-transfer-row' data-uuid='" + esc(uuid) + "' data-upid='" + esc(upid) + "'>"
                + "<td>" + esc(date) + statusHtml + "</td>"
                + "<td>" + esc(from) + "</td>"
                + "<td>" + esc(destination) + "</td>"
                + "<td><a href='javascript:void(0);' class='hie-transfer-view-link' "
                + "data-transfer-id='" + esc(uuid) + "' data-upid='" + esc(upid) + "' "
                + "title='Open transfer'><i class='icon-eye-open'></i></a></td>"
                + "</tr>";
        }

        function loadHieTransferList() {
            var statusEl = jq("#hie-transfer-list-status");
            var wrapEl = jq("#hie-transfer-list-wrap");
            var bodyEl = jq("#hie-transfer-list-body");

            if (!upid) {
                statusEl.html("<span style='color:#a94442;'>No UPID found for this patient.</span>");
                return;
            }

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
                    statusEl.html("<span style='color:#a94442;'>" + esc(response.message || "Unable to load transfers.") + "</span>");
                    return;
                }
                var items = response && response.data ? response.data : [];
                if (!items.length) {
                    statusEl.html("No inbound HIE transfers found for this patient.");
                    return;
                }
                var rows = [];
                for (var i = 0; i < items.length; i++) {
                    rows.push(transferListRowHtml(items[i]));
                }
                bodyEl.html(rows.join(""));
                statusEl.hide();
                wrapEl.show();
            }).fail(function(xhr) {
                var message = "Unable to load transfers from HIE.";
                if (xhr && xhr.responseJSON && xhr.responseJSON.message) {
                    message = xhr.responseJSON.message;
                }
                statusEl.html("<span style='color:#a94442;'>" + esc(message) + "</span>");
            });
        }

        function validateCurrentTransfer() {
            if (!canValidate || !patientId) {
                return;
            }
            var transfer = currentPreviewTransfer;
            if (!transfer || !targetsCurrentFacility(transfer)) {
                return;
            }
            var hieTransferId = transfer.uuid || transfer.id || transfer.hieTransferId || "";
            if (!hieTransferId) {
                return;
            }

            var validateBtn = jq("#hie-transfer-validate-btn");
            var validateStatus = jq("#hie-transfer-validate-status");
            validateBtn.prop("disabled", true).html("<i class='icon-spinner icon-spin'></i> Saving...");
            validateStatus.hide().text("");

            jq.ajax({
                url: validateUrl,
                type: "POST",
                data: {
                    patientId: patientId,
                    hieTransferId: hieTransferId
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
                        validateBtn.prop("disabled", false).text(validateBtn.data("default-label") || "Yes Transfer is valid");
                        validateStatus.show().css("color", "#a94442").text("Unexpected response from server.");
                        return;
                    }
                }
                if (response && response.status === "success") {
                    validateStatus.show().css("color", "#0f766e").text(response.message || "Transfer validated. Refreshing...");
                    window.location.reload();
                    return;
                }
                validateBtn.prop("disabled", false).text(validateBtn.data("default-label") || "Yes Transfer is valid");
                validateStatus.show().css("color", "#a94442").text(
                    response && response.message ? response.message : "Unable to validate transfer."
                );
            }).fail(function(xhr) {
                var message = "Unable to validate transfer.";
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
                validateBtn.prop("disabled", false).text(validateBtn.data("default-label") || "Yes Transfer is valid");
                validateStatus.show().css("color", "#a94442").text(message);
            });
        }

        jq(document).off("click.hieTransfer", ".hie-transfer-view-link");
        jq(document).on("click.hieTransfer", ".hie-transfer-view-link", function(e) {
            e.preventDefault();
            e.stopPropagation();
            var link = jq(this);
            var transferId = link.attr("data-transfer-id") || link.data("transfer-id") || "";
            var patientUpid = link.attr("data-upid") || link.data("upid") || upid || "";
            loadTransferPreview(transferId, patientUpid, link);
        });

        jq(document).off("click.hieTransferClose", "#hie-transfer-preview-close");
        jq(document).on("click.hieTransferClose", "#hie-transfer-preview-close", function(e) {
            e.preventDefault();
            hidePreviewDialog();
        });

        jq(document).off("click.hieTransferValidate", "#hie-transfer-validate-btn");
        jq(document).on("click.hieTransferValidate", "#hie-transfer-validate-btn", function(e) {
            e.preventDefault();
            validateCurrentTransfer();
        });

        jq(document).off("click.hieTransferExportPdf", "#hie-transfer-export-pdf-btn");
        jq(document).on("click.hieTransferExportPdf", "#hie-transfer-export-pdf-btn", function(e) {
            e.preventDefault();
            exportCurrentPreviewPdf();
        });

        jq(document).off("submit.hieTransferFeedback", "#hie-transfer-feedback-form");
        jq(document).on("submit.hieTransferFeedback", "#hie-transfer-feedback-form", saveReferralFeedback);

        jq(document).off("click.hieTransferProvideFeedback", "#hie-transfer-provide-feedback-btn");
        jq(document).on("click.hieTransferProvideFeedback", "#hie-transfer-provide-feedback-btn", onFeedbackActionClick);

        if (section.length && section.attr("data-can-list") === "true"
                && showSection && listFromHie && !hasTransferId) {
            loadHieTransferList();
        }
    }

    jq(document).ready(initHieTransferSection);
})(typeof jq !== "undefined" ? jq : jQuery);
