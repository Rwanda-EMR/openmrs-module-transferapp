<%
    ui.decorateWith("appui", "standardEmrPage")
    ui.includeCss("transferapp", "dashboard.css")
    ui.includeCss("transferapp", "transferAdmin.css")
    ui.includeCss("transferapp", "select2.min.css")
    ui.includeJavascript("transferapp", "select2/select2.min.js")
    ui.includeJavascript("transferapp", "approvers.js")
%>
<script type="text/javascript">
    var breadcrumbs = [
        { icon: "icon-home", link: '/' + OPENMRS_CONTEXT_PATH + '/index.htm' },
        { label: "${ ui.message("transferapp.nav.approvers") }" }
    ];
</script>

<div class="transfer-admin">
${ ui.includeFragment("transferapp", "transfer/transferNav", [ activeTab: "approvers", app: "transferapp.dashboard" ]) }

<% if (!canAccessApprovers) { %>
<div class="transfer-admin-empty" style="margin-top: 16px; padding: 12px; background: #fff8e6; border: 1px solid #f0d78c;">
    ${ ui.encodeHtmlContent(approversAccessDeniedMessage) }
</div>
<% } else { %>

<div class="transfer-admin-section">
    <h3 class="transfer-admin-section-title">${ ui.message("transferapp.approvers.title") }</h3>
    <form id="approvers-add-form" class="transfer-admin-add-form">
        <div class="transfer-admin-form-row">
            <div class="transfer-admin-field transfer-admin-field-registry">
                <label for="approverUserSelect">${ ui.message("transferapp.approvers.user") }</label>
                <select id="approverUserSelect" name="userId" class="transfer-admin-facility-registry-select">
                    <option value="">${ ui.message("transferapp.approvers.user.placeholder") }</option>
                    <% clinicianOptions.each { option -> %>
                    <option value="${ option.userId }">${ ui.encodeHtmlContent(option.label) }</option>
                    <% } %>
                </select>
            </div>
            <div class="transfer-admin-field">
                <label for="approverPosition">${ ui.message("transferapp.approvers.position") }</label>
                <input type="text" id="approverPosition" name="position" maxlength="255" required
                       placeholder="${ ui.message('transferapp.approvers.position.placeholder') }" />
            </div>
            <div class="transfer-admin-field transfer-admin-field-action">
                <label>&nbsp;</label>
                <div class="transfer-admin-form-actions">
                    <button type="submit" class="btn btn-primary">${ ui.message("transferapp.approvers.add") }</button>
                </div>
            </div>
        </div>
    </form>

    <div id="approvers-message" class="transfer-admin-message" style="display:none;"></div>

    <table class="transfer-admin-table" id="approvers-table">
        <thead>
            <tr>
                <th>${ ui.message("transferapp.approvers.column.name") }</th>
                <th>${ ui.message("transferapp.approvers.column.username") }</th>
                <th>${ ui.message("transferapp.approvers.column.position") }</th>
                <th class="transfer-admin-col-action">${ ui.message("transferapp.admin.action") }</th>
            </tr>
        </thead>
        <tbody>
            <% if (approverRows == null || approverRows.isEmpty()) { %>
            <tr class="transfer-admin-empty-row">
                <td colspan="4">${ ui.message("transferapp.approvers.empty") }</td>
            </tr>
            <% } else { approverRows.each { row -> %>
            <tr data-approver-id="${ row.approverId }" data-user-id="${ row.userId }">
                <td class="approver-name">${ ui.encodeHtmlContent(row.displayName ?: '') }</td>
                <td class="approver-username">${ ui.encodeHtmlContent(row.username ?: '') }</td>
                <td class="approver-position">${ ui.encodeHtmlContent(row.position ?: '') }</td>
                <td class="transfer-admin-col-action">
                    <button type="button" class="btn btn-default approver-remove-btn"
                            data-approver-id="${ row.approverId }">
                        ${ ui.message("transferapp.approvers.remove") }
                    </button>
                </td>
            </tr>
            <% } } %>
        </tbody>
    </table>
</div>

<% } %>
</div>

<script type="text/javascript">
    window.approversConfig = {
        saveUrl: (typeof openmrsContextPath !== "undefined" ? openmrsContextPath : "/${ ui.encodeJavaScript(contextPath) }")
            + "/module/transferapp/approvers/save.form",
        voidUrl: (typeof openmrsContextPath !== "undefined" ? openmrsContextPath : "/${ ui.encodeJavaScript(contextPath) }")
            + "/module/transferapp/approvers/void.form",
        resourcesBase: (typeof openmrsContextPath !== "undefined" ? openmrsContextPath : "/${ ui.encodeJavaScript(contextPath) }")
            + "/moduleResources/transferapp/",
        messages: {
            addSuccess: "${ ui.encodeJavaScript(ui.message('transferapp.approvers.addSuccess')) }",
            addError: "${ ui.encodeJavaScript(ui.message('transferapp.approvers.addError')) }",
            removeSuccess: "${ ui.encodeJavaScript(ui.message('transferapp.approvers.removeSuccess')) }",
            removeError: "${ ui.encodeJavaScript(ui.message('transferapp.approvers.removeError')) }",
            removeConfirm: "${ ui.encodeJavaScript(ui.message('transferapp.approvers.removeConfirm')) }",
            userRequired: "${ ui.encodeJavaScript(ui.message('transferapp.approvers.userRequired')) }",
            positionRequired: "${ ui.encodeJavaScript(ui.message('transferapp.approvers.positionRequired')) }",
            empty: "${ ui.encodeJavaScript(ui.message('transferapp.approvers.empty')) }",
            remove: "${ ui.encodeJavaScript(ui.message('transferapp.approvers.remove')) }",
            select2Error: "${ ui.encodeJavaScript(ui.message('transferapp.admin.receivingFacilities.registry.select2Error')) }"
        }
    };
</script>
