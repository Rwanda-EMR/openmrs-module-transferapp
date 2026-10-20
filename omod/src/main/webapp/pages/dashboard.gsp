<%
    ui.decorateWith("appui", "standardEmrPage")
    ui.includeCss("transferapp", "dashboard.css")
    ui.includeCss("transferapp", "flatpickr.min.css")
    ui.includeJavascript("transferapp", "flatpickr/flatpickr.min.js")
    ui.includeJavascript("transferapp", "transferDashboard.js")
%>
<script type="text/javascript">
    var breadcrumbs = [
        { icon: "icon-home", link: '/' + OPENMRS_CONTEXT_PATH + '/index.htm' },
        { label: "${ ui.message("transferapp.dashboard.title") }" }
    ];
    window.transferDashboardFilterConfig = {
        startDate: "${ ui.encodeJavaScript(filterStartDate ?: '') }",
        endDate: "${ ui.encodeJavaScript(filterEndDate ?: '') }",
        maxDateRangeMonths: ${ maxDateRangeMonths ?: 6 },
        messages: {
            dateRangeError: "${ ui.encodeJavaScript(ui.message('transferapp.dashboard.filter.dateRangeError', maxDateRangeMonths ?: 6)) }",
            invalidDateRange: "${ ui.encodeJavaScript(ui.message('transferapp.dashboard.filter.invalidDateRange')) }"
        }
    };
</script>

<div class="transfer-dashboard">
${ ui.includeFragment("transferapp", "home/providerLicenseRequiredBanner") }
${ ui.includeFragment("transferapp", "transfer/transferNav", [ activeTab: "dashboard", app: "transferapp.dashboard" ]) }

<% if (!canAccessDashboard) { %>
<div class="transfer-records-empty" style="margin-top: 16px; padding: 12px; background: #fff8e6; border: 1px solid #f0d78c;">
    ${ ui.encodeHtmlContent(dashboardAccessDeniedMessage) }
</div>
<% } else { %>

<form id="transfer-dashboard-filter-form"
      class="transfer-dashboard-filters"
      method="get"
      action="${ ui.pageLink('transferapp', 'dashboard') }">
    <input type="hidden" name="app" value="${ ui.encodeHtmlAttribute(appId) }" />
    <div class="transfer-dashboard-filters-grid">
        <div class="transfer-dashboard-filter-field">
            <label for="dashboard-filter-start-date">${ ui.message("transferapp.dashboard.filter.startDate") }</label>
            <input type="text"
                   id="dashboard-filter-start-date"
                   name="startDate"
                   class="transfer-dashboard-date-input"
                   value="${ ui.encodeHtmlAttribute(filterStartDate ?: '') }"
                   autocomplete="off" />
        </div>
        <div class="transfer-dashboard-filter-field">
            <label for="dashboard-filter-end-date">${ ui.message("transferapp.dashboard.filter.endDate") }</label>
            <input type="text"
                   id="dashboard-filter-end-date"
                   name="endDate"
                   class="transfer-dashboard-date-input"
                   value="${ ui.encodeHtmlAttribute(filterEndDate ?: '') }"
                   autocomplete="off" />
        </div>
        <div class="transfer-dashboard-filter-actions">
            <button type="submit" id="dashboard-filter-apply" class="confirm">
                ${ ui.message("transferapp.dashboard.filter.apply") }
            </button>
        </div>
    </div>
</form>

<div class="transfer-dashboard-section">
    <h3 class="transfer-dashboard-section-title">${ ui.message("transferapp.dashboard.received.title") }</h3>
    <div class="transfer-stat-grid">
        <div class="transfer-stat-card transfer-stat-card-received">
            <div class="transfer-stat-label">${ ui.message("transferapp.dashboard.received.today") }</div>
            <div class="transfer-stat-value">${ transfersReceivedToday }</div>
        </div>
        <div class="transfer-stat-card transfer-stat-card-received">
            <div class="transfer-stat-label">${ ui.message("transferapp.dashboard.received.thisWeek") }</div>
            <div class="transfer-stat-value">${ transfersReceivedThisWeek }</div>
        </div>
        <div class="transfer-stat-card transfer-stat-card-received">
            <div class="transfer-stat-label">${ ui.message("transferapp.dashboard.received.period") }</div>
            <div class="transfer-stat-value">${ transfersReceivedPeriod }</div>
        </div>
        <a class="transfer-stat-card transfer-stat-card-received transfer-stat-card-missing transfer-stat-card-link"
           href="${ ui.pageLink('transferapp', 'missingCounterReferrals') }?app=${ ui.encodeHtmlAttribute(appId) }&amp;startDate=${ ui.encodeHtmlAttribute(filterStartDate ?: '') }&amp;endDate=${ ui.encodeHtmlAttribute(filterEndDate ?: '') }">
            <div class="transfer-stat-label">${ ui.message("transferapp.dashboard.received.missingCounterReferral") }</div>
            <div class="transfer-stat-value">${ transfersReceivedMissingCounterReferral }</div>
        </a>
    </div>
</div>

<div class="transfer-dashboard-section">
    <h3 class="transfer-dashboard-section-title">${ ui.message("transferapp.dashboard.sent.title") }</h3>
    <div class="transfer-stat-grid transfer-stat-grid-5">
        <div class="transfer-stat-card transfer-stat-card-sent">
            <div class="transfer-stat-label">${ ui.message("transferapp.dashboard.sent.today") }</div>
            <div class="transfer-stat-value">${ transfersSentToday }</div>
        </div>
        <div class="transfer-stat-card transfer-stat-card-sent">
            <div class="transfer-stat-label">${ ui.message("transferapp.dashboard.sent.thisWeek") }</div>
            <div class="transfer-stat-value">${ transfersSentThisWeek }</div>
        </div>
        <div class="transfer-stat-card transfer-stat-card-sent">
            <div class="transfer-stat-label">${ ui.message("transferapp.dashboard.sent.period") }</div>
            <div class="transfer-stat-value">${ transfersSentPeriod }</div>
        </div>
        <div class="transfer-stat-card transfer-stat-card-sent transfer-stat-card-pending">
            <div class="transfer-stat-label">${ ui.message("transferapp.dashboard.sent.pending") }</div>
            <div class="transfer-stat-value">${ transfersSentPending }</div>
        </div>
        <a class="transfer-stat-card transfer-stat-card-sent transfer-stat-card-missing transfer-stat-card-link"
           href="${ ui.pageLink('transferapp', 'missingOutboundCounterReferrals') }?app=${ ui.encodeHtmlAttribute(appId) }&amp;startDate=${ ui.encodeHtmlAttribute(filterStartDate ?: '') }&amp;endDate=${ ui.encodeHtmlAttribute(filterEndDate ?: '') }">
            <div class="transfer-stat-label">${ ui.message("transferapp.dashboard.sent.missingCounterReferral") }</div>
            <div class="transfer-stat-value">${ transfersSentMissingCounterReferral }</div>
        </a>
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
            <div class="transfer-stat-label">${ ui.message("transferapp.dashboard.ambulanceVoucher.period") }</div>
            <div class="transfer-stat-value">${ ambulanceVouchersPeriod }</div>
        </div>
    </div>
</div>

<div class="transfer-dashboard-section">
    <h3 class="transfer-dashboard-section-title">${ ui.message("transferapp.dashboard.counterReferral.title") }</h3>
    <div class="transfer-stat-grid transfer-stat-grid-2">
        <div class="transfer-stat-card transfer-stat-card-counter-received">
            <div class="transfer-stat-label">${ ui.message("transferapp.dashboard.counterReferral.received") }</div>
            <div class="transfer-stat-value">${ counterReferralsReceived }</div>
        </div>
        <div class="transfer-stat-card transfer-stat-card-counter-sent">
            <div class="transfer-stat-label">${ ui.message("transferapp.dashboard.counterReferral.sent") }</div>
            <div class="transfer-stat-value">${ counterReferralsSent }</div>
        </div>
    </div>
</div>
<% } %>
</div>
