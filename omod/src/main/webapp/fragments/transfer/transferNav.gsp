<%
    import org.openmrs.api.context.Context
    def activeTab = config.activeTab ?: "dashboard"
    def appParam = config.app ?: "transferapp.dashboard"
    def dashboardUrl = ui.pageLink("transferapp", "dashboard") + "?app=" + appParam
    def historyUrl = ui.pageLink("transferapp", "history") + "?app=" + appParam
    def recordsUrl = ui.pageLink("transferapp", "records") + "?app=" + appParam
    def pendingUrl = ui.pageLink("transferapp", "pending") + "?app=" + appParam
    def ambulanceVoucherUrl = ui.pageLink("transferapp", "ambulanceVoucher") + "?app=" + appParam
    def adminUrl = ui.pageLink("transferapp", "transferAdmin") + "?app=" + appParam
    def pastTransfersUrl = ui.pageLink("transferapp", "pastTransfers") + "?app=" + appParam
    def profileUrl = ui.pageLink("transferapp", "transferProfile") + "?app=" + appParam
    def authUser = Context.getAuthenticatedUser()
    def canPastTransfers = authUser != null && authUser.hasPrivilege("View: transferapp.pasttransfers")
%>
<nav class="transfer-app-nav" aria-label="Transfer app navigation">
    <a href="${ dashboardUrl }"
       class="transfer-app-nav-link ${ activeTab == 'dashboard' ? 'active' : '' }">${ ui.message("transferapp.nav.dashboard") }</a>
    <span class="transfer-app-nav-separator">|</span>
    <a href="${ historyUrl }"
       class="transfer-app-nav-link ${ activeTab == 'history' ? 'active' : '' }">${ ui.message("transferapp.nav.history") }</a>
    <span class="transfer-app-nav-separator">|</span>
    <a href="${ recordsUrl }"
       class="transfer-app-nav-link ${ activeTab == 'records' ? 'active' : '' }">${ ui.message("transferapp.nav.records") }</a>
    <span class="transfer-app-nav-separator">|</span>
    <a href="${ pendingUrl }"
       class="transfer-app-nav-link ${ activeTab == 'pending' ? 'active' : '' }">${ ui.message("transferapp.nav.pending") }</a>
    <span class="transfer-app-nav-separator">|</span>
    <a href="${ ambulanceVoucherUrl }"
       class="transfer-app-nav-link ${ activeTab == 'ambulanceVoucher' ? 'active' : '' }">${ ui.message("transferapp.nav.ambulanceVoucher") }</a>
    <span class="transfer-app-nav-separator">|</span>
    <a href="${ adminUrl }"
       class="transfer-app-nav-link ${ activeTab == 'admin' ? 'active' : '' }">${ ui.message("transferapp.nav.admin") }</a>
    <% if (canPastTransfers) { %>
    <span class="transfer-app-nav-separator">|</span>
    <a href="${ pastTransfersUrl }"
       class="transfer-app-nav-link ${ activeTab == 'pastTransfers' ? 'active' : '' }">${ ui.message("transferapp.nav.pastTransfers") }</a>
    <% } %>
    <span class="transfer-app-nav-separator">|</span>
    <a href="${ profileUrl }"
       class="transfer-app-nav-link ${ activeTab == 'profile' ? 'active' : '' }">${ ui.message("transferapp.nav.profile") }</a>
</nav>
