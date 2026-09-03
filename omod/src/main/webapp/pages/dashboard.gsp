<%
    ui.decorateWith("appui", "standardEmrPage")
    ui.includeCss("transferapp", "dashboard.css")
%>
<script type="text/javascript">
    var breadcrumbs = [
        { icon: "icon-home", link: '/' + OPENMRS_CONTEXT_PATH + '/index.htm' },
        { label: "${ ui.message("transferapp.dashboard.title") }" }
    ];
</script>

<div class="transfer-dashboard">
${ ui.includeFragment("transferapp", "transfer/transferNav", [ activeTab: "dashboard", app: "transferapp.dashboard" ]) }

<% if (!canAccessDashboard) { %>
<div class="transfer-records-empty" style="margin-top: 16px; padding: 12px; background: #fff8e6; border: 1px solid #f0d78c;">
    ${ ui.encodeHtmlContent(dashboardAccessDeniedMessage) }
</div>
<% } else { %>

<div class="transfer-dashboard-section">
    <h3 class="transfer-dashboard-section-title">${ ui.message("transferapp.dashboard.received.title") }</h3>
    <div class="transfer-stat-grid transfer-stat-grid-3">
        <div class="transfer-stat-card transfer-stat-card-received">
            <div class="transfer-stat-label">${ ui.message("transferapp.dashboard.received.today") }</div>
            <div class="transfer-stat-value">${ transfersReceivedToday }</div>
        </div>
        <div class="transfer-stat-card transfer-stat-card-received">
            <div class="transfer-stat-label">${ ui.message("transferapp.dashboard.received.thisWeek") }</div>
            <div class="transfer-stat-value">${ transfersReceivedThisWeek }</div>
        </div>
        <div class="transfer-stat-card transfer-stat-card-received">
            <div class="transfer-stat-label">${ ui.message("transferapp.dashboard.received.total") }</div>
            <div class="transfer-stat-value">${ transfersReceivedTotal }</div>
        </div>
    </div>
</div>

<div class="transfer-dashboard-section">
    <h3 class="transfer-dashboard-section-title">${ ui.message("transferapp.dashboard.sent.title") }</h3>
    <div class="transfer-stat-grid">
        <div class="transfer-stat-card transfer-stat-card-sent">
            <div class="transfer-stat-label">${ ui.message("transferapp.dashboard.sent.today") }</div>
            <div class="transfer-stat-value">${ transfersSentToday }</div>
        </div>
        <div class="transfer-stat-card transfer-stat-card-sent">
            <div class="transfer-stat-label">${ ui.message("transferapp.dashboard.sent.thisWeek") }</div>
            <div class="transfer-stat-value">${ transfersSentThisWeek }</div>
        </div>
        <div class="transfer-stat-card transfer-stat-card-sent">
            <div class="transfer-stat-label">${ ui.message("transferapp.dashboard.sent.total") }</div>
            <div class="transfer-stat-value">${ transfersSentTotal }</div>
        </div>
        <div class="transfer-stat-card transfer-stat-card-sent transfer-stat-card-pending">
            <div class="transfer-stat-label">${ ui.message("transferapp.dashboard.sent.pending") }</div>
            <div class="transfer-stat-value">${ transfersSentPending }</div>
        </div>
    </div>
</div>

<div class="transfer-dashboard-section">
    <h3 class="transfer-dashboard-section-title">${ ui.message("transferapp.dashboard.ambulanceVoucher.title") }</h3>
    <div class="transfer-stat-grid transfer-stat-grid-3">
        <div class="transfer-stat-card transfer-stat-card-voucher">
            <div class="transfer-stat-label">${ ui.message("transferapp.dashboard.ambulanceVoucher.today") }</div>
            <div class="transfer-stat-value">${ ambulanceVouchersToday }</div>
        </div>
        <div class="transfer-stat-card transfer-stat-card-voucher">
            <div class="transfer-stat-label">${ ui.message("transferapp.dashboard.ambulanceVoucher.thisWeek") }</div>
            <div class="transfer-stat-value">${ ambulanceVouchersThisWeek }</div>
        </div>
        <div class="transfer-stat-card transfer-stat-card-voucher">
            <div class="transfer-stat-label">${ ui.message("transferapp.dashboard.ambulanceVoucher.total") }</div>
            <div class="transfer-stat-value">${ ambulanceVouchersTotal }</div>
        </div>
    </div>
</div>
<% } %>
</div>
