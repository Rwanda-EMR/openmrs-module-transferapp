<%
    ui.decorateWith("appui", "standardEmrPage")
    ui.includeCss("transferapp", "dashboard.css")
    ui.includeCss("transferapp", "transferProfile.css")
    ui.includeJavascript("transferapp", "transferProfile.js")
    def licenseValidatedFlag = licenseValidated == true || licenseValidated == "true"
%>
<script type="text/javascript">
    var breadcrumbs = [
        { icon: "icon-home", link: '/' + OPENMRS_CONTEXT_PATH + '/index.htm' },
        { label: "${ ui.message("transferapp.nav.profile") }" }
    ];
</script>

<div class="transfer-profile">
${ ui.includeFragment("transferapp", "transfer/transferNav", [ activeTab: "profile", app: "transferapp.dashboard" ]) }

<div class="transfer-profile-card">
    <div class="transfer-profile-meta">
        <div><strong>${ ui.message("transferapp.profile.providerName") }:</strong> ${ ui.encodeHtmlContent(providerDisplayName ?: '') }</div>
        <div><strong>${ ui.message("transferapp.profile.username") }:</strong> ${ ui.encodeHtmlContent(username ?: '') }</div>
    </div>

    <form id="transfer-profile-form" class="transfer-profile-form">
        <div class="transfer-profile-field transfer-profile-license-field">
            <label for="licenseNumber">${ ui.message("transferapp.profile.licenseNumber") }</label>
            <div class="transfer-profile-license-row">
                <input type="text" id="licenseNumber" name="licenseNumber" maxlength="64" required
                       value="${ ui.encodeHtmlAttribute(licenseNumber ?: '') }"
                       data-validated-license="${ ui.encodeHtmlAttribute(licenseValidatedFlag ? (licenseNumber ?: '') : '') }" />
                <button type="button" id="transfer-license-validate-btn" class="button confirm transfer-license-validate-btn"
                        style="${ licenseValidatedFlag ? 'display:none;' : '' }">
                    ${ ui.message("transferapp.profile.license.checkValidity") }
                </button>
                <span id="transfer-license-validated-badge" class="transfer-license-validated-badge"
                      style="${ licenseValidatedFlag ? '' : 'display:none;' }">
                    <i class="icon-ok"></i> ${ ui.message("transferapp.profile.license.validated") }
                </span>
            </div>
            <p id="transfer-license-validate-hint" class="transfer-profile-hint">
                ${ ui.message("transferapp.profile.license.checkHint") }
            </p>
        </div>

        <div id="transfer-license-result-row" class="transfer-profile-license-result-row"
             style="${ licenseValidatedFlag ? '' : 'display:none;' }">
            <div class="transfer-profile-field">
                <label for="licenseStatusDisplay">${ ui.message("transferapp.profile.license.status") }</label>
                <input type="text" id="licenseStatusDisplay" readonly="readonly" tabindex="-1"
                       value="${ ui.encodeHtmlAttribute(licenseStatus ?: '') }" />
            </div>
            <div class="transfer-profile-field">
                <label for="licenseExpiryDisplay">${ ui.message("transferapp.profile.license.expiryDate") }</label>
                <input type="text" id="licenseExpiryDisplay" readonly="readonly" tabindex="-1"
                       value="${ ui.encodeHtmlAttribute(licenseExpiryDate ?: '') }" />
            </div>
        </div>

        <div class="transfer-profile-field">
            <label for="phoneNumber">${ ui.message("transferapp.profile.phoneNumber") }</label>
            <input type="tel" id="phoneNumber" name="phoneNumber" maxlength="64" required
                   value="${ ui.encodeHtmlAttribute(phoneNumber ?: '') }" />
        </div>
        <div class="transfer-profile-field">
            <label for="qualification">${ ui.message("transferapp.profile.qualification") }</label>
            <input type="text" id="qualification" name="qualification" maxlength="255" required
                   value="${ ui.encodeHtmlAttribute(qualification ?: '') }" />
        </div>
        <div class="transfer-profile-field">
            <label for="speciality">${ ui.message("transferapp.profile.speciality") }</label>
            <input type="text" id="speciality" name="speciality" maxlength="255"
                   value="${ ui.encodeHtmlAttribute(speciality ?: '') }" />
            <% if (qualificationDisplay) { %>
            <p class="transfer-profile-hint">${ ui.message("transferapp.profile.qualificationPreview") }:
                <strong>${ ui.encodeHtmlContent(qualificationDisplay) }</strong></p>
            <% } %>
        </div>

        <div class="transfer-profile-actions">
            <button type="submit" class="btn btn-primary">${ ui.message("transferapp.profile.save") }</button>
        </div>
    </form>
</div>
</div>

<div id="transfer-license-otp-overlay" class="transfer-license-otp-overlay" style="display:none;"></div>
<div id="transfer-license-otp-dialog" class="dialog transfer-license-otp-dialog" style="display:none;" role="dialog"
     aria-labelledby="transfer-license-otp-title" aria-modal="true">
    <div class="dialog-header">
        <i class="icon-key"></i>
        <h3 id="transfer-license-otp-title">${ ui.message("transferapp.profile.license.otpTitle") }</h3>
    </div>
    <div class="dialog-content">
        <p class="transfer-license-otp-message">${ ui.message("transferapp.profile.license.otpMessage") }</p>
        <div class="transfer-profile-field">
            <label for="transfer-license-otp-input">${ ui.message("transferapp.profile.license.otpLabel") }</label>
            <input type="text" id="transfer-license-otp-input" maxlength="12" autocomplete="one-time-code"
                   placeholder="${ ui.encodeHtmlAttribute(ui.message('transferapp.profile.license.otpPlaceholder')) }" />
        </div>
        <p id="transfer-license-otp-error" class="transfer-license-otp-error" style="display:none;"></p>
        <div class="transfer-license-otp-actions">
            <button type="button" id="transfer-license-otp-submit" class="button confirm">
                ${ ui.message("transferapp.profile.license.otpSubmit") }
            </button>
            <button type="button" id="transfer-license-otp-cancel" class="button cancel">
                ${ ui.message("coreapps.cancel") }
            </button>
        </div>
    </div>
</div>

<script type="text/javascript">
    window.transferProfileConfig = {
        saveUrl: (typeof openmrsContextPath !== "undefined" ? openmrsContextPath : "/${ ui.encodeJavaScript(contextPath) }")
            + "/module/transferapp/profile/save.form",
        startValidationUrl: (typeof openmrsContextPath !== "undefined" ? openmrsContextPath : "/${ ui.encodeJavaScript(contextPath) }")
            + "/module/transferapp/profile/license/startValidation.form",
        verifyOtpUrl: (typeof openmrsContextPath !== "undefined" ? openmrsContextPath : "/${ ui.encodeJavaScript(contextPath) }")
            + "/module/transferapp/profile/license/verifyOtp.form",
        licenseValidated: ${ licenseValidatedFlag ? 'true' : 'false' },
        messages: {
            saveSuccess: "${ ui.encodeJavaScript(ui.message('transferapp.profile.saveSuccess')) }",
            saveError: "${ ui.encodeJavaScript(ui.message('transferapp.profile.saveError')) }",
            licenseRequired: "${ ui.encodeJavaScript(ui.message('transferapp.profile.license.required')) }",
            startError: "${ ui.encodeJavaScript(ui.message('transferapp.profile.license.startError')) }",
            otpRequired: "${ ui.encodeJavaScript(ui.message('transferapp.profile.license.otpRequired')) }",
            otpError: "${ ui.encodeJavaScript(ui.message('transferapp.profile.license.otpError')) }",
            otpSuccess: "${ ui.encodeJavaScript(ui.message('transferapp.profile.license.otpSuccess')) }",
            validated: "${ ui.encodeJavaScript(ui.message('transferapp.profile.license.validated')) }"
        }
    };
</script>
