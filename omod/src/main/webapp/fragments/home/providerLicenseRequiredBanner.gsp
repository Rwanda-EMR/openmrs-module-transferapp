<%
    ui.includeCss("transferapp", "providerLicenseRequiredBanner.css")
%>
<% if (showBanner) { %>
<div class="note-container transferapp-license-banner-wrap">
    <div class="note error transferapp-license-banner">
        <div class="text">
            <i class="icon-warning-sign" style="vertical-align: middle;"></i>
            <a class="transferapp-license-banner-link"
               href="${ ui.pageLink("transferapp", "transferProfile") }?app=transferapp.dashboard">
                ${ ui.message("transferapp.profile.license.requiredBanner") }
            </a>
        </div>
    </div>
</div>
<% } %>
