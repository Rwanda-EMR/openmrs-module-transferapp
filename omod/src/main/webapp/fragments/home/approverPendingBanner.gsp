<%
    ui.includeCss("transferapp", "approverPendingBanner.css")
%>
<% if (showBanner) { %>
<div class="note-container transferapp-approver-banner-wrap">
    <div class="note warning transferapp-approver-banner">
        <div class="text">
            <i class="icon-ok-sign" style="vertical-align: middle;"></i>
            <a class="transferapp-approver-banner-link"
               href="${ ui.pageLink("transferapp", "approvals") }?app=transferapp.dashboard">
                ${ ui.message("transferapp.approver.banner.pendingApproval") }
                <span class="transferapp-approver-banner-badge">${ pendingApprovalCount }</span>
            </a>
        </div>
    </div>
</div>
<% } %>
