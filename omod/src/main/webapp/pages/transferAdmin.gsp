<%
    ui.decorateWith("appui", "standardEmrPage")
    ui.includeCss("transferapp", "dashboard.css")
    ui.includeCss("transferapp", "transferAdmin.css")
    ui.includeCss("transferapp", "styles/select2.min.css")
    ui.includeJavascript("transferapp", "transferAdmin.js")
%>
<script type="text/javascript">
    var breadcrumbs = [
        { icon: "icon-home", link: '/' + OPENMRS_CONTEXT_PATH + '/index.htm' },
        { label: "${ ui.message("transferapp.nav.admin") }" }
    ];
</script>

<div class="transfer-admin">
${ ui.includeFragment("transferapp", "transfer/transferNav", [ activeTab: "admin", app: "transferapp.dashboard" ]) }

<% if (!canAccessAdmin) { %>
<div class="transfer-admin-empty" style="margin-top: 16px; padding: 12px; background: #fff8e6; border: 1px solid #f0d78c;">
    ${ ui.encodeHtmlContent(adminAccessDeniedMessage) }
</div>
<% } else { %>
<div class="transfer-admin-location">
    <label for="transfer-admin-location-select">${ ui.message("transferapp.admin.sendingFacility") }</label>
    <select id="transfer-admin-location-select" class="transfer-admin-location-select">
        <option value="">${ ui.message("transferapp.admin.selectSendingFacility") }</option>
        <% sendingLocations.each { location -> %>
        <option value="${ location.locationId }"
            <% if (selectedLocationId != null && selectedLocationId == location.locationId) { %>selected<% } %>>
            ${ ui.encodeHtmlContent(location.name) }
        </option>
        <% } %>
    </select>
</div>

<% if (selectedLocationId == null) { %>
<div class="transfer-admin-empty">${ ui.message("transferapp.admin.noSendingFacility") }</div>
<% } else { %>

<div class="transfer-admin-section">
    <h3 class="transfer-admin-section-title">${ ui.message("transferapp.admin.receivingFacilities.title") }</h3>
    <form id="transfer-admin-add-facility-form" class="transfer-admin-add-form">
        <input type="hidden" name="sendingLocationId" value="${ selectedLocationId }" />
        <div class="transfer-admin-form-row">
            <div class="transfer-admin-field transfer-admin-field-registry">
                <label for="facilityRegistrySelect">${ ui.message("transferapp.admin.receivingFacilities.registry") }</label>
                <select id="facilityRegistrySelect" class="transfer-admin-facility-registry-select">
                    <option value="">${ ui.message("transferapp.admin.receivingFacilities.registry.placeholder") }</option>
                </select>
                <input type="hidden" id="facilityCode" name="facilityCode" />
                <input type="hidden" id="facilityName" name="facilityName" />
            </div>
            <div class="transfer-admin-field transfer-admin-field-location">
                <label for="province">${ ui.message("transferapp.admin.receivingFacilities.province") }</label>
                <input type="text" id="province" name="province" maxlength="120" required />
            </div>
            <div class="transfer-admin-field transfer-admin-field-location">
                <label for="district">${ ui.message("transferapp.admin.receivingFacilities.district") }</label>
                <input type="text" id="district" name="district" maxlength="120" required />
            </div>
            <div class="transfer-admin-field transfer-admin-field-distance">
                <label for="distance">${ ui.message("transferapp.admin.receivingFacilities.distance") }</label>
                <input type="number" id="distance" name="distance" min="0" step="1" placeholder="${ ui.message('transferapp.admin.receivingFacilities.distance.placeholder') }" />
            </div>
            <div class="transfer-admin-field transfer-admin-field-external">
                <label for="external">${ ui.message("transferapp.admin.receivingFacilities.external") }</label>
                <label class="transfer-admin-checkbox">
                    <input type="checkbox" id="external" name="external" value="true" />
                </label>
            </div>
            <div class="transfer-admin-field transfer-admin-field-action">
                <label>&nbsp;</label>
                <div class="transfer-admin-form-actions">
                    <button type="submit" id="facility-form-submit-btn" class="btn btn-primary">${ ui.message("transferapp.admin.add") }</button>
                    <button type="button" id="facility-form-cancel-btn" class="btn btn-default" style="display:none;">
                        ${ ui.message("transferapp.admin.cancelEdit") }
                    </button>
                </div>
            </div>
        </div>
    </form>

    <table class="transfer-admin-table" id="transfer-admin-facilities-table">
        <thead>
            <tr>
                <th>${ ui.message("transferapp.admin.receivingFacilities.code") }</th>
                <th>${ ui.message("transferapp.admin.receivingFacilities.name") }</th>
                <th>${ ui.message("transferapp.admin.receivingFacilities.province") }</th>
                <th>${ ui.message("transferapp.admin.receivingFacilities.district") }</th>
                <th>${ ui.message("transferapp.admin.receivingFacilities.distance") }</th>
                <th>${ ui.message("transferapp.admin.receivingFacilities.external") }</th>
                <th class="transfer-admin-col-action">${ ui.message("transferapp.admin.action") }</th>
            </tr>
        </thead>
        <tbody>
            <% if (receivingFacilities == null || receivingFacilities.isEmpty()) { %>
            <tr class="transfer-admin-empty-row">
                <td colspan="7">${ ui.message("transferapp.admin.receivingFacilities.empty") }</td>
            </tr>
            <% } else { receivingFacilities.each { facility -> %>
            <tr data-facility-id="${ facility.receivingFacilityId }"
                data-facility-code="${ ui.encodeHtmlAttribute(facility.facilityCode) }"
                data-facility-name="${ ui.encodeHtmlAttribute(facility.facilityName) }"
                data-province="${ ui.encodeHtmlAttribute(facility.province ?: '') }"
                data-district="${ ui.encodeHtmlAttribute(facility.district ?: '') }"
                data-distance="${ facility.distance != null ? facility.distance : '' }"
                data-external="${ facility.external ? 'true' : 'false' }"
                class="${ selectedReceivingFacilityId != null && selectedReceivingFacilityId == facility.receivingFacilityId ? 'is-selected' : '' }">
                <td>${ ui.encodeHtmlContent(facility.facilityCode) }</td>
                <td>${ ui.encodeHtmlContent(facility.facilityName) }</td>
                <td>${ ui.encodeHtmlContent(facility.province ?: '') }</td>
                <td>${ ui.encodeHtmlContent(facility.district ?: '') }</td>
                <td><% if (facility.distance != null) { %>${ facility.distance } ${ ui.message("transferapp.admin.receivingFacilities.distance.unit") }<% } else { %>—<% } %></td>
                <td>${ facility.external ? ui.message("transferapp.admin.receivingFacilities.external.yes") : ui.message("transferapp.admin.receivingFacilities.external.no") }</td>
                <td class="transfer-admin-col-action">
                    <button type="button"
                            class="btn btn-link transfer-admin-edit-facility"
                            data-facility-id="${ facility.receivingFacilityId }">
                        <i class="icon-pencil"></i> ${ ui.message("transferapp.admin.edit") }
                    </button>
                    <a href="${ ui.pageLink('transferapp', 'transferAdmin') }?app=transferapp.dashboard&amp;locationId=${ selectedLocationId }&amp;receivingFacilityId=${ facility.receivingFacilityId }"
                       class="btn btn-link transfer-admin-manage-services">
                        ${ ui.message("transferapp.admin.manageServices") }
                    </a>
                    <button type="button"
                            class="btn btn-link transfer-admin-remove-facility"
                            data-facility-id="${ facility.receivingFacilityId }">
                        ${ ui.message("transferapp.admin.remove") }
                    </button>
                </td>
            </tr>
            <% } } %>
        </tbody>
    </table>
</div>

<div class="transfer-admin-section">
    <h3 class="transfer-admin-section-title">${ ui.message("transferapp.admin.receivingServices.title") }</h3>
    <% if (receivingFacilities == null || receivingFacilities.isEmpty()) { %>
    <div class="transfer-admin-empty">${ ui.message("transferapp.admin.receivingServices.noFacility") }</div>
    <% } else { %>

    <div class="transfer-admin-location">
        <label for="transfer-admin-receiving-facility-select">${ ui.message("transferapp.admin.receivingServices.facility") }</label>
        <select id="transfer-admin-receiving-facility-select" class="transfer-admin-location-select">
            <% receivingFacilities.each { facility -> %>
            <option value="${ facility.receivingFacilityId }"
                <% if (selectedReceivingFacilityId != null && selectedReceivingFacilityId == facility.receivingFacilityId) { %>selected<% } %>>
                ${ ui.encodeHtmlContent(facility.facilityName) } (${ ui.encodeHtmlContent(facility.facilityCode) })
            </option>
            <% } %>
        </select>
    </div>

    <form id="transfer-admin-add-service-form" class="transfer-admin-add-form">
        <input type="hidden" name="receivingFacilityId" value="${ selectedReceivingFacilityId ?: '' }" />
        <input type="hidden" id="receivingServiceId" name="receivingServiceId" value="" disabled="disabled" />
        <div class="transfer-admin-form-row">
            <div class="transfer-admin-field transfer-admin-field-grow">
                <label for="serviceName">${ ui.message("transferapp.admin.receivingServices.name") }</label>
                <input type="text" id="serviceName" name="serviceName" maxlength="255" required />
            </div>
            <div class="transfer-admin-field transfer-admin-field-action">
                <label>&nbsp;</label>
                <div class="transfer-admin-form-actions">
                    <button type="submit" id="service-form-submit-btn" class="btn btn-primary">${ ui.message("transferapp.admin.add") }</button>
                    <button type="button" id="service-form-cancel-btn" class="btn btn-default" style="display:none;">
                        ${ ui.message("transferapp.admin.cancelEdit") }
                    </button>
                </div>
            </div>
        </div>
    </form>

    <table class="transfer-admin-table" id="transfer-admin-services-table">
        <thead>
            <tr>
                <th>${ ui.message("transferapp.admin.receivingServices.name") }</th>
                <th class="transfer-admin-col-action">${ ui.message("transferapp.admin.action") }</th>
            </tr>
        </thead>
        <tbody>
            <% if (selectedReceivingFacilityId == null) { %>
            <tr class="transfer-admin-empty-row">
                <td colspan="2">${ ui.message("transferapp.admin.receivingServices.selectFacility") }</td>
            </tr>
            <% } else if (receivingServices == null || receivingServices.isEmpty()) { %>
            <tr class="transfer-admin-empty-row">
                <td colspan="2">${ ui.message("transferapp.admin.receivingServices.empty") }</td>
            </tr>
            <% } else { receivingServices.each { service -> %>
            <tr data-service-id="${ service.receivingServiceId }"
                data-service-name="${ ui.encodeHtmlAttribute(service.serviceName) }">
                <td>${ ui.encodeHtmlContent(service.serviceName) }</td>
                <td class="transfer-admin-col-action">
                    <button type="button"
                            class="btn btn-link transfer-admin-edit-service"
                            data-service-id="${ service.receivingServiceId }">
                        <i class="icon-pencil"></i> ${ ui.message("transferapp.admin.edit") }
                    </button>
                    <button type="button"
                            class="btn btn-link transfer-admin-remove-service"
                            data-service-id="${ service.receivingServiceId }">
                        ${ ui.message("transferapp.admin.remove") }
                    </button>
                </td>
            </tr>
            <% } } %>
        </tbody>
    </table>
    <% } %>
</div>

<% } %>
<% } %>
</div>

<script type="text/javascript">
    window.transferAdminConfig = {
        adminBaseUrl: (typeof openmrsContextPath !== "undefined" ? openmrsContextPath : "/${ ui.encodeJavaScript(contextPath) }") + "/module/transferapp/admin",
        adminPageUrl: "${ ui.encodeJavaScript(ui.pageLink('transferapp', 'transferAdmin') + '?app=transferapp.dashboard') }",
        resourcesBase: (typeof openmrsContextPath !== "undefined" ? openmrsContextPath : "/${ ui.encodeJavaScript(contextPath) }") + "/moduleResources/transferapp/",
        selectedLocationId: ${ selectedLocationId != null ? selectedLocationId : 'null' },
        selectedReceivingFacilityId: ${ selectedReceivingFacilityId != null ? selectedReceivingFacilityId : 'null' },
        messages: {
            remove: "${ ui.encodeJavaScript(ui.message('transferapp.admin.remove')) }",
            edit: "${ ui.encodeJavaScript(ui.message('transferapp.admin.edit')) }",
            add: "${ ui.encodeJavaScript(ui.message('transferapp.admin.add')) }",
            update: "${ ui.encodeJavaScript(ui.message('transferapp.admin.update')) }",
            cancelEdit: "${ ui.encodeJavaScript(ui.message('transferapp.admin.cancelEdit')) }",
            saveSuccess: "${ ui.encodeJavaScript(ui.message('transferapp.admin.saveSuccess')) }",
            saveError: "${ ui.encodeJavaScript(ui.message('transferapp.admin.saveError')) }",
            removeSuccess: "${ ui.encodeJavaScript(ui.message('transferapp.admin.removeSuccess')) }",
            removeError: "${ ui.encodeJavaScript(ui.message('transferapp.admin.removeError')) }",
            confirmRemove: "${ ui.encodeJavaScript(ui.message('transferapp.admin.confirmRemove')) }",
            registryLoading: "${ ui.encodeJavaScript(ui.message('transferapp.admin.receivingFacilities.registry.loading')) }",
            registryError: "${ ui.encodeJavaScript(ui.message('transferapp.admin.receivingFacilities.registry.error')) }",
            registryEmpty: "${ ui.encodeJavaScript(ui.message('transferapp.admin.receivingFacilities.registry.empty')) }",
            registryRequired: "${ ui.encodeJavaScript(ui.message('transferapp.admin.receivingFacilities.registry.required')) }",
            registryPlaceholder: "${ ui.encodeJavaScript(ui.message('transferapp.admin.receivingFacilities.registry.placeholder')) }",
            select2Error: "${ ui.encodeJavaScript(ui.message('transferapp.admin.receivingFacilities.registry.select2Error')) }"
        }
    };
</script>
