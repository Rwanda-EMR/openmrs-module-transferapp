<%
    ui.decorateWith("appui", "standardEmrPage")
    ui.includeCss("transferapp", "hiePatientPreview.css")
    def missingValue = ui.message("transferapp.pending.registration.preview.notProvided")
    def showValue = { value -> value == null || value.toString().trim().isEmpty() ? missingValue : value.toString() }
    def patientPhoto = patientDetails.photo == null ? "" : patientDetails.photo.toString().trim()
%>

<script type="text/javascript">
    var breadcrumbs = [
        { icon: "icon-home", link: '/' + OPENMRS_CONTEXT_PATH + '/index.htm' },
        { label: "${ ui.encodeJavaScript(ui.message('transferapp.pending.title')) }", link: "${ ui.encodeJavaScript(cancelUrl) }" },
        { label: "${ ui.encodeJavaScript(ui.message('transferapp.pending.registration.preview.title')) }" }
    ];
</script>

<div class="transfer-hie-preview">
    <header class="transfer-hie-preview-header">
        <div>
            <h1>${ ui.message("transferapp.pending.registration.preview.title") }</h1>
            <p>${ ui.message("transferapp.pending.registration.preview.subtitle") }</p>
        </div>
        <span class="transfer-hie-preview-upid">${ ui.encodeHtmlContent(upid) }</span>
    </header>

    <section class="transfer-hie-preview-section" aria-labelledby="preview-identifiers-title">
        <h2 id="preview-identifiers-title">${ ui.message("transferapp.pending.registration.preview.identifiers") }</h2>
        <dl class="transfer-hie-preview-grid">
            <div><dt>${ ui.message("transferapp.pending.registration.preview.nationalId") }</dt><dd>${ ui.encodeHtmlContent(showValue(patientDetails.nationalId)) }</dd></div>
            <div><dt>${ ui.message("transferapp.pending.registration.preview.upid") }</dt><dd>${ ui.encodeHtmlContent(showValue(patientDetails.upid)) }</dd></div>
            <div><dt>${ ui.message("transferapp.pending.registration.preview.applicationNumber") }</dt><dd>${ ui.encodeHtmlContent(showValue(patientDetails.applicationNumber)) }</dd></div>
            <div><dt>${ ui.message("transferapp.pending.registration.preview.nin") }</dt><dd>${ ui.encodeHtmlContent(showValue(patientDetails.nin)) }</dd></div>
            <div><dt>${ ui.message("transferapp.pending.registration.preview.passportNumber") }</dt><dd>${ ui.encodeHtmlContent(showValue(patientDetails.passportNumber)) }</dd></div>
        </dl>
    </section>

    <section class="transfer-hie-preview-section" aria-labelledby="preview-demographics-title">
        <h2 id="preview-demographics-title">${ ui.message("transferapp.pending.registration.preview.demographics") }</h2>
        <div class="transfer-hie-preview-demographics">
            <figure class="transfer-hie-preview-photo">
                <div class="transfer-hie-preview-photo-frame">
                    <img id="transfer-hie-preview-photo-image"
                         alt="${ ui.encodeHtmlAttribute(ui.message('transferapp.pending.registration.preview.photo')) }" />
                    <span id="transfer-hie-preview-photo-placeholder"
                          class="transfer-hie-preview-photo-placeholder">
                        <i class="icon-user"></i>
                        <span>${ ui.message("transferapp.pending.registration.preview.photo.loading") }</span>
                    </span>
                </div>
                <figcaption>${ ui.message("transferapp.pending.registration.preview.photo") }</figcaption>
            </figure>
            <dl class="transfer-hie-preview-grid">
                <div><dt>${ ui.message("transferapp.pending.registration.preview.givenName") }</dt><dd>${ ui.encodeHtmlContent(showValue(patientDetails.givenName)) }</dd></div>
                <div><dt>${ ui.message("transferapp.pending.registration.preview.middleName") }</dt><dd>${ ui.encodeHtmlContent(showValue(patientDetails.middleName)) }</dd></div>
                <div><dt>${ ui.message("transferapp.pending.registration.preview.familyName") }</dt><dd>${ ui.encodeHtmlContent(showValue(patientDetails.familyName)) }</dd></div>
                <div><dt>${ ui.message("transferapp.pending.registration.preview.gender") }</dt><dd>${ ui.encodeHtmlContent(showValue(patientDetails.gender)) }</dd></div>
                <div><dt>${ ui.message("transferapp.pending.registration.preview.birthdate") }</dt><dd>${ ui.encodeHtmlContent(showValue(patientDetails.birthdate)) }</dd></div>
            </dl>
        </div>
    </section>

    <section class="transfer-hie-preview-section" aria-labelledby="preview-contact-title">
        <h2 id="preview-contact-title">${ ui.message("transferapp.pending.registration.preview.contact") }</h2>
        <dl class="transfer-hie-preview-grid">
            <div><dt>${ ui.message("transferapp.pending.registration.preview.phoneNumber") }</dt><dd>${ ui.encodeHtmlContent(showValue(patientDetails.phoneNumber)) }</dd></div>
            <div><dt>${ ui.message("transferapp.pending.registration.preview.country") }</dt><dd>${ ui.encodeHtmlContent(showValue(patientDetails.country)) }</dd></div>
            <div><dt>${ ui.message("transferapp.pending.registration.preview.province") }</dt><dd>${ ui.encodeHtmlContent(showValue(patientDetails.stateProvince)) }</dd></div>
            <div><dt>${ ui.message("transferapp.pending.registration.preview.district") }</dt><dd>${ ui.encodeHtmlContent(showValue(patientDetails.countyDistrict)) }</dd></div>
            <div><dt>${ ui.message("transferapp.pending.registration.preview.sector") }</dt><dd>${ ui.encodeHtmlContent(showValue(patientDetails.cityVillage)) }</dd></div>
            <div><dt>${ ui.message("transferapp.pending.registration.preview.cell") }</dt><dd>${ ui.encodeHtmlContent(showValue(patientDetails.address3)) }</dd></div>
            <div><dt>${ ui.message("transferapp.pending.registration.preview.village") }</dt><dd>${ ui.encodeHtmlContent(showValue(patientDetails.address1)) }</dd></div>
        </dl>
    </section>

    <section class="transfer-hie-preview-section" aria-labelledby="preview-attributes-title">
        <h2 id="preview-attributes-title">${ ui.message("transferapp.pending.registration.preview.attributes") }</h2>
        <dl class="transfer-hie-preview-grid">
            <div><dt>${ ui.message("transferapp.pending.registration.preview.mothersName") }</dt><dd>${ ui.encodeHtmlContent(showValue(patientDetails.mothersName)) }</dd></div>
            <div><dt>${ ui.message("transferapp.pending.registration.preview.fathersName") }</dt><dd>${ ui.encodeHtmlContent(showValue(patientDetails.fathersName)) }</dd></div>
            <div><dt>${ ui.message("transferapp.pending.registration.preview.education") }</dt><dd>${ ui.encodeHtmlContent(showValue(patientDetails.educationLevel)) }</dd></div>
            <div><dt>${ ui.message("transferapp.pending.registration.preview.profession") }</dt><dd>${ ui.encodeHtmlContent(showValue(patientDetails.profession)) }</dd></div>
            <div><dt>${ ui.message("transferapp.pending.registration.preview.religion") }</dt><dd>${ ui.encodeHtmlContent(showValue(patientDetails.religion)) }</dd></div>
        </dl>
    </section>

    <% if (!canConfirmRegistration) { %>
    <div class="transfer-hie-preview-warning" role="alert">
        <i class="icon-warning-sign"></i>
        <span>${ ui.message("transferapp.pending.registration.preview.incomplete") }</span>
    </div>
    <% } %>

    <div class="transfer-hie-preview-actions">
        <a class="button cancel" href="${ ui.encodeHtmlAttribute(cancelUrl) }">
            ${ ui.message("transferapp.pending.registration.preview.cancel") }
        </a>
        <% if (canConfirmRegistration) { %>
        <form method="post" action="${ ui.pageLink('transferapp', 'registerPatientFromHie') }">
            <input type="hidden" name="upid" value="${ ui.encodeHtmlAttribute(upid) }" />
            <input type="hidden" name="returnUrl" value="${ ui.encodeHtmlAttribute(returnUrl) }" />
            <button type="submit" class="button confirm">
                <i class="icon-ok"></i> ${ ui.message("transferapp.pending.registration.preview.confirm") }
            </button>
        </form>
        <% } %>
    </div>
</div>

<script type="text/javascript">
    jq(function() {
        var registryPhoto = jq("#transfer-hie-preview-photo-image");
        var registryPhotoPlaceholder = jq("#transfer-hie-preview-photo-placeholder");
        var initialPhoto = "${ ui.encodeJavaScript(patientPhoto) }";
        var patientUpid = "${ ui.encodeJavaScript(upid) }";
        var upidIdentifierTypeUuid = "${ ui.encodeJavaScript(upidIdentifierTypeUuid) }";
        var registrySearchUrl = "${ ui.encodeJavaScript(ui.actionLink('rwandaemr', 'field/searchClientRegistry', 'findByIdentifier')) }";
        var noPhotoMessage = "${ ui.encodeJavaScript(ui.message('transferapp.pending.registration.preview.photo.none')) }";
        var loadingPhotoMessage = "${ ui.encodeJavaScript(ui.message('transferapp.pending.registration.preview.photo.loading')) }";
        var unavailablePhotoMessage = "${ ui.encodeJavaScript(ui.message('transferapp.pending.registration.preview.photo.unavailable')) }";

        function clearRegistryPhoto(message) {
            registryPhoto.hide().removeAttr("src");
            registryPhotoPlaceholder.removeClass("is-hidden").find("span").text(message || noPhotoMessage);
        }

        function showRegistryPhoto(photo) {
            var photoSrc = getRegistryPhotoSrc(photo);
            if (!photoSrc) {
                clearRegistryPhoto(noPhotoMessage);
                return;
            }
            registryPhoto
                .off("load error")
                .on("load", function() {
                    registryPhotoPlaceholder.addClass("is-hidden");
                    registryPhoto.show();
                })
                .on("error", function() {
                    clearRegistryPhoto(unavailablePhotoMessage);
                })
                .attr("src", photoSrc);
        }

        function getRegistryPhotoSrc(photo) {
            if (!photo) {
                return null;
            }
            var trimmed = jq.trim(photo);
            if (!trimmed) {
                return null;
            }
            var lower = trimmed.toLowerCase();
            if (lower.indexOf("data:image/") === 0
                    || lower.indexOf("http://") === 0
                    || lower.indexOf("https://") === 0) {
                return trimmed;
            }
            var compact = trimmed
                .split(" ").join("")
                .split(String.fromCharCode(9)).join("")
                .split(String.fromCharCode(10)).join("")
                .split(String.fromCharCode(13)).join("");
            if (isBase64PhotoValue(compact)) {
                return "data:image/jpeg;base64," + compact;
            }
            if (trimmed.charAt(0) === "/") {
                return trimmed;
            }
            return null;
        }

        function isBase64PhotoValue(value) {
            if (!value || value.length < 100) {
                return false;
            }
            if (value.indexOf("/9j/") !== 0 && value.indexOf("iVBOR") !== 0) {
                return false;
            }
            var validChars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789+/=";
            for (var i = 0; i < value.length; i++) {
                if (validChars.indexOf(value.charAt(i)) === -1) {
                    return false;
                }
            }
            return true;
        }

        if (initialPhoto) {
            showRegistryPhoto(initialPhoto);
        } else {
            clearRegistryPhoto(loadingPhotoMessage);
        }

        if (!patientUpid || !upidIdentifierTypeUuid) {
            if (!initialPhoto) {
                clearRegistryPhoto(noPhotoMessage);
            }
            return;
        }

        var searchParams = {};
        searchParams["identifier_" + upidIdentifierTypeUuid] = patientUpid;
        jq.ajax({
            url: registrySearchUrl,
            dataType: "json",
            data: searchParams
        }).done(function(data) {
            if (data && data.patient && data.patient.photo) {
                showRegistryPhoto(data.patient.photo);
            } else if (!initialPhoto) {
                clearRegistryPhoto(noPhotoMessage);
            }
        }).fail(function() {
            if (!initialPhoto) {
                clearRegistryPhoto(unavailablePhotoMessage);
            }
        });
    });
</script>
