jQuery(function($) {
    var config = window.transferProfileConfig || {};
    var saveUrl = config.saveUrl;
    var startValidationUrl = config.startValidationUrl;
    var verifyOtpUrl = config.verifyOtpUrl;
    var messages = config.messages || {};
    var pendingLicenseNumber = "";

    function showProfileMessage(message, isError) {
        if (typeof emr !== "undefined") {
            if (isError) {
                emr.errorMessage(message);
            } else {
                emr.successMessage(message);
            }
            return;
        }
        if (isError) {
            alert(message);
        }
    }

    function trimValue(value) {
        return $.trim(value || "");
    }

    function isLicenseValidatedForCurrentValue() {
        var current = trimValue($("#licenseNumber").val());
        var validatedLicense = trimValue($("#licenseNumber").attr("data-validated-license"));
        return !!current && !!validatedLicense && current === validatedLicense;
    }

    function syncLicenseValidationUi() {
        var validated = isLicenseValidatedForCurrentValue();
        $("#transfer-license-validate-btn").toggle(!validated);
        $("#transfer-license-validated-badge").toggle(validated);
        $("#transfer-license-result-row").toggle(validated);
        $("#transfer-license-validate-hint").toggle(!validated);
    }

    function applyValidatedLicense(result) {
        var licenseNumber = trimValue((result && result.licenseNumber) || $("#licenseNumber").val());
        var status = trimValue(result && result.licenseStatus);
        var expiry = trimValue(result && result.licenseExpiryDate);
        $("#licenseNumber").val(licenseNumber).attr("data-validated-license", licenseNumber);
        $("#licenseStatusDisplay").val(status);
        $("#licenseExpiryDisplay").val(expiry);
        syncLicenseValidationUi();
    }

    function openOtpDialog() {
        $("#transfer-license-otp-error").hide().text("");
        $("#transfer-license-otp-input").val("");
        $("#transfer-license-otp-overlay").show();
        $("#transfer-license-otp-dialog").addClass("is-open").css("display", "flex").show();
        setTimeout(function() {
            $("#transfer-license-otp-input").focus();
        }, 50);
    }

    function closeOtpDialog() {
        $("#transfer-license-otp-overlay").hide();
        $("#transfer-license-otp-dialog").removeClass("is-open").hide();
        $("#transfer-license-otp-error").hide().text("");
        $("#transfer-license-otp-input").val("");
        pendingLicenseNumber = "";
    }

    function setOtpBusy(busy) {
        $("#transfer-license-otp-submit").prop("disabled", !!busy);
        $("#transfer-license-otp-cancel").prop("disabled", !!busy);
        $("#transfer-license-otp-input").prop("disabled", !!busy);
    }

    $("#licenseNumber").on("input change", function() {
        syncLicenseValidationUi();
    });

    $("#transfer-license-validate-btn").on("click", function() {
        var licenseNumber = trimValue($("#licenseNumber").val());
        if (!licenseNumber) {
            showProfileMessage(messages.licenseRequired || "License number is required", true);
            $("#licenseNumber").focus();
            return;
        }

        var $btn = $(this);
        $btn.prop("disabled", true);
        $.post(startValidationUrl, { licenseNumber: licenseNumber }, function(response) {
            if (response && response.status === "success" && (response.needsOtp === true || response.needsOtp === "true")) {
                pendingLicenseNumber = licenseNumber;
                openOtpDialog();
                return;
            }
            var message = response && response.message ? response.message : messages.startError;
            showProfileMessage(message, true);
        }, "json").fail(function() {
            showProfileMessage(messages.startError || "Unable to start license validation", true);
        }).always(function() {
            $btn.prop("disabled", false);
        });
    });

    $("#transfer-license-otp-cancel, #transfer-license-otp-overlay").on("click", function() {
        closeOtpDialog();
    });

    $("#transfer-license-otp-submit").on("click", function() {
        var otp = trimValue($("#transfer-license-otp-input").val());
        if (!otp) {
            $("#transfer-license-otp-error").text(messages.otpRequired || "OTP is required").show();
            $("#transfer-license-otp-input").focus();
            return;
        }
        if (!pendingLicenseNumber) {
            closeOtpDialog();
            return;
        }

        setOtpBusy(true);
        $("#transfer-license-otp-error").hide().text("");
        $.post(verifyOtpUrl, {
            licenseNumber: pendingLicenseNumber,
            otp: otp
        }, function(response) {
            if (response && response.status === "success") {
                applyValidatedLicense(response);
                closeOtpDialog();
                showProfileMessage(messages.otpSuccess || "License verified successfully", false);
                return;
            }
            var message = response && response.message ? response.message : messages.otpError;
            $("#transfer-license-otp-error").text(message).show();
        }, "json").fail(function() {
            $("#transfer-license-otp-error").text(messages.otpError || "Unable to verify OTP").show();
        }).always(function() {
            setOtpBusy(false);
        });
    });

    $("#transfer-license-otp-input").on("keydown", function(event) {
        if (event.key === "Enter" || event.keyCode === 13) {
            event.preventDefault();
            $("#transfer-license-otp-submit").click();
        }
    });

    $("#transfer-profile-form").on("submit", function(event) {
        event.preventDefault();
        var $form = $(this);
        var $submit = $form.find("button[type='submit']");
        $submit.prop("disabled", true);

        $.post(saveUrl, $form.serialize(), function(response) {
            if (response && response.status === "success") {
                showProfileMessage(messages.saveSuccess, false);
            } else {
                var message = response && response.message ? response.message : messages.saveError;
                showProfileMessage(message, true);
            }
        }, "json").fail(function() {
            showProfileMessage(messages.saveError, true);
        }).always(function() {
            $submit.prop("disabled", false);
        });
    });

    syncLicenseValidationUi();
});
