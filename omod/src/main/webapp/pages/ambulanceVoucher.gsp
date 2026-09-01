<%
    ui.decorateWith("appui", "standardEmrPage")
    ui.includeCss("transferapp", "dashboard.css")
    ui.includeCss("transferapp", "transferSection.css")
    ui.includeCss("transferapp", "transferRecords.css")
    ui.includeCss("transferapp", "ambulanceVoucherPreview.css")
    ui.includeCss("transferapp", "hiePatientPreview.css")
    ui.includeCss("transferapp", "transferFormPreview.css")
    ui.includeCss("transferapp", "flatpickr.min.css")
    ui.includeCss("uicommons", "datatables/dataTables_jui.css")
    ui.includeJavascript("uicommons", "datatables/jquery.dataTables.min.js")
    ui.includeJavascript("transferapp", "flatpickr/flatpickr.min.js")
    ui.includeJavascript("transferapp", "transferMohLogo.js")
    ui.includeJavascript("transferapp", "transferFormPreview.js")
    ui.includeJavascript("transferapp", "transferPreviewCommon.js")
    ui.includeJavascript("transferapp", "transferAmbulanceVoucher.js")
%>
<script type="text/javascript">
    var breadcrumbs = [
        { icon: "icon-home", link: '/' + OPENMRS_CONTEXT_PATH + '/index.htm' },
        { label: "${ ui.message("transferapp.ambulanceVoucher.title") }" }
    ];
    var openmrsContextPath = (typeof openmrsContextPath !== "undefined" && openmrsContextPath)
        ? openmrsContextPath
        : "/${ ui.encodeJavaScript(contextPath) }";
    window.transferOpenmrsPath = openmrsContextPath;
    window.transferAmbulanceVoucherFilterConfig = {
        startDate: "${ ui.encodeJavaScript(filterStartDate ?: '') }",
        endDate: "${ ui.encodeJavaScript(filterEndDate ?: '') }",
        maxDateRangeMonths: ${ maxDateRangeMonths ?: 3 },
        previewUrl: openmrsContextPath + "/module/transferapp/transfer/ambulanceVoucherPreview.form",
        transferPreviewUrl: openmrsContextPath + "/module/transferapp/transfer/preview.form",
        transferPreviewResourcesBase: openmrsContextPath + "/moduleResources/transferapp/scripts/",
        hieSearchUrl: openmrsContextPath + "/module/transferapp/transfer/hieTransfersByIdentifier.form",
        registerPatientUrl: openmrsContextPath + "/transferapp/registerPatientFromHie.page",
        registerPreviewUrl: openmrsContextPath + "/module/transferapp/transfer/hiePatientRegistrationPreview.form",
        registerConfirmUrl: openmrsContextPath + "/module/transferapp/transfer/registerPatientFromHie.form",
        createVoucherUrl: openmrsContextPath + "/module/transferapp/transfer/createAmbulanceVoucherFromHie.form",
        ambulanceVoucherReturnUrl: "/transferapp/ambulanceVoucher.page?app=transferapp.dashboard",
        messages: {
            dateRangeError: "${ ui.encodeJavaScript(ui.message('transferapp.ambulanceVoucher.filter.dateRangeError', maxDateRangeMonths ?: 3)) }",
            invalidDateRange: "${ ui.encodeJavaScript(ui.message('transferapp.ambulanceVoucher.filter.invalidDateRange')) }",
            previewLoading: "${ ui.encodeJavaScript(ui.message('transferapp.ambulanceVoucher.preview.loading')) }",
            previewError: "${ ui.encodeJavaScript(ui.message('transferapp.ambulanceVoucher.preview.error')) }",
            previewClose: "${ ui.encodeJavaScript(ui.message('coreapps.close')) }",
            previewPrint: "${ ui.encodeJavaScript(ui.message('transferapp.ambulanceVoucher.preview.print')) }",
            searchInvalid: "${ ui.encodeJavaScript(ui.message('transferapp.ambulanceVoucher.search.invalid')) }",
            searchLoading: "${ ui.encodeJavaScript(ui.message('transferapp.ambulanceVoucher.search.loading')) }",
            searchError: "${ ui.encodeJavaScript(ui.message('transferapp.ambulanceVoucher.search.error')) }",
            searchEmpty: "${ ui.encodeJavaScript(ui.message('transferapp.ambulanceVoucher.search.empty')) }",
            searchTitle: "${ ui.encodeJavaScript(ui.message('transferapp.ambulanceVoucher.search.resultsTitle')) }",
            patientExisting: "${ ui.encodeJavaScript(ui.message('transferapp.pending.patient.existing')) }",
            patientNew: "${ ui.encodeJavaScript(ui.message('transferapp.pending.patient.new')) }",
            registerPatient: "${ ui.encodeJavaScript(ui.message('transferapp.pending.action.register')) }",
            registerPreviewTitle: "${ ui.encodeJavaScript(ui.message('transferapp.pending.registration.preview.title')) }",
            registerPreviewSubtitle: "${ ui.encodeJavaScript(ui.message('transferapp.pending.registration.preview.subtitle')) }",
            registerPreviewLoading: "${ ui.encodeJavaScript(ui.message('transferapp.ambulanceVoucher.search.register.loading')) }",
            registerPreviewError: "${ ui.encodeJavaScript(ui.message('transferapp.ambulanceVoucher.search.register.error')) }",
            registerConfirm: "${ ui.encodeJavaScript(ui.message('transferapp.pending.registration.preview.confirm')) }",
            registerCancel: "${ ui.encodeJavaScript(ui.message('transferapp.pending.registration.preview.cancel')) }",
            registerNotProvided: "${ ui.encodeJavaScript(ui.message('transferapp.pending.registration.preview.notProvided')) }",
            registerIdentifiers: "${ ui.encodeJavaScript(ui.message('transferapp.pending.registration.preview.identifiers')) }",
            registerDemographics: "${ ui.encodeJavaScript(ui.message('transferapp.pending.registration.preview.demographics')) }",
            registerContact: "${ ui.encodeJavaScript(ui.message('transferapp.pending.registration.preview.contact')) }",
            registerAttributes: "${ ui.encodeJavaScript(ui.message('transferapp.pending.registration.preview.attributes')) }",
            registerNationalId: "${ ui.encodeJavaScript(ui.message('transferapp.pending.registration.preview.nationalId')) }",
            registerUpid: "${ ui.encodeJavaScript(ui.message('transferapp.pending.registration.preview.upid')) }",
            registerApplicationNumber: "${ ui.encodeJavaScript(ui.message('transferapp.pending.registration.preview.applicationNumber')) }",
            registerNin: "${ ui.encodeJavaScript(ui.message('transferapp.pending.registration.preview.nin')) }",
            registerPassport: "${ ui.encodeJavaScript(ui.message('transferapp.pending.registration.preview.passportNumber')) }",
            registerGivenName: "${ ui.encodeJavaScript(ui.message('transferapp.pending.registration.preview.givenName')) }",
            registerMiddleName: "${ ui.encodeJavaScript(ui.message('transferapp.pending.registration.preview.middleName')) }",
            registerFamilyName: "${ ui.encodeJavaScript(ui.message('transferapp.pending.registration.preview.familyName')) }",
            registerGender: "${ ui.encodeJavaScript(ui.message('transferapp.pending.registration.preview.gender')) }",
            registerBirthdate: "${ ui.encodeJavaScript(ui.message('transferapp.pending.registration.preview.birthdate')) }",
            registerPhoto: "${ ui.encodeJavaScript(ui.message('transferapp.pending.registration.preview.photo')) }",
            registerPhotoNone: "${ ui.encodeJavaScript(ui.message('transferapp.pending.registration.preview.photo.none')) }",
            registerPhone: "${ ui.encodeJavaScript(ui.message('transferapp.pending.registration.preview.phoneNumber')) }",
            registerCountry: "${ ui.encodeJavaScript(ui.message('transferapp.pending.registration.preview.country')) }",
            registerProvince: "${ ui.encodeJavaScript(ui.message('transferapp.pending.registration.preview.province')) }",
            registerDistrict: "${ ui.encodeJavaScript(ui.message('transferapp.pending.registration.preview.district')) }",
            registerSector: "${ ui.encodeJavaScript(ui.message('transferapp.pending.registration.preview.sector')) }",
            registerCell: "${ ui.encodeJavaScript(ui.message('transferapp.pending.registration.preview.cell')) }",
            registerVillage: "${ ui.encodeJavaScript(ui.message('transferapp.pending.registration.preview.village')) }",
            registerMothersName: "${ ui.encodeJavaScript(ui.message('transferapp.pending.registration.preview.mothersName')) }",
            registerFathersName: "${ ui.encodeJavaScript(ui.message('transferapp.pending.registration.preview.fathersName')) }",
            registerEducation: "${ ui.encodeJavaScript(ui.message('transferapp.pending.registration.preview.education')) }",
            registerProfession: "${ ui.encodeJavaScript(ui.message('transferapp.pending.registration.preview.profession')) }",
            registerReligion: "${ ui.encodeJavaScript(ui.message('transferapp.pending.registration.preview.religion')) }",
            createVoucher: "${ ui.encodeJavaScript(ui.message('transferapp.ambulanceVoucher.search.createVoucher')) }",
            createVoucherLoading: "${ ui.encodeJavaScript(ui.message('transferapp.ambulanceVoucher.search.createVoucher.loading')) }",
            createVoucherError: "${ ui.encodeJavaScript(ui.message('transferapp.ambulanceVoucher.search.createVoucher.error')) }",
            createVoucherSuccess: "${ ui.encodeJavaScript(ui.message('transferapp.ambulanceVoucher.search.createVoucher.success')) }",
            createVoucherTitle: "${ ui.encodeJavaScript(ui.message('transferapp.ambulanceVoucher.search.createVoucher.title')) }",
            createVoucherInsuranceNumber: "${ ui.encodeJavaScript(ui.message('transferapp.ambulanceVoucher.search.createVoucher.insuranceNumber')) }",
            createVoucherInsuranceMissing: "${ ui.encodeJavaScript(ui.message('transferapp.ambulanceVoucher.search.createVoucher.insuranceMissing')) }",
            createVoucherDistrict: "${ ui.encodeJavaScript(ui.message('transferapp.ambulanceVoucher.search.createVoucher.district')) }",
            createVoucherDistrictPlaceholder: "${ ui.encodeJavaScript(ui.message('transferapp.ambulanceVoucher.search.createVoucher.districtPlaceholder')) }",
            createVoucherDistrictRequired: "${ ui.encodeJavaScript(ui.message('transferapp.ambulanceVoucher.search.createVoucher.districtRequired')) }",
            createVoucherDistance: "${ ui.encodeJavaScript(ui.message('transferapp.ambulanceVoucher.search.createVoucher.distance')) }",
            createVoucherDistancePrompt: "${ ui.encodeJavaScript(ui.message('transferapp.ambulanceVoucher.search.createVoucher.distancePrompt')) }",
            createVoucherDistanceInvalid: "${ ui.encodeJavaScript(ui.message('transferapp.ambulanceVoucher.search.createVoucher.distanceInvalid')) }",
            createVoucherRoute: "${ ui.encodeJavaScript(ui.message('transferapp.ambulanceVoucher.search.createVoucher.route')) }",
            createVoucherRouteTemplate: "${ ui.encodeJavaScript(ui.message('transferapp.ambulanceVoucher.search.createVoucher.routeTemplate')) }",
            createVoucherConfirm: "${ ui.encodeJavaScript(ui.message('transferapp.ambulanceVoucher.search.createVoucher.confirm')) }",
            createVoucherCancel: "${ ui.encodeJavaScript(ui.message('transferapp.ambulanceVoucher.search.createVoucher.cancel')) }",
            previewAction: "${ ui.encodeJavaScript(ui.message('transferapp.ambulanceVoucher.preview.action')) }",
            previewTransferAction: "${ ui.encodeJavaScript(ui.message('transferapp.ambulanceVoucher.preview.transfer')) }",
            previewTransferTitle: "${ ui.encodeJavaScript(ui.message('transferapp.ambulanceVoucher.preview.transferTitle')) }",
            previewTransferLoading: "${ ui.encodeJavaScript(ui.message('transferapp.ambulanceVoucher.preview.transferLoading')) }",
            previewTransferError: "${ ui.encodeJavaScript(ui.message('transferapp.ambulanceVoucher.preview.transferError')) }",
            printList: "${ ui.encodeJavaScript(ui.message('transferapp.ambulanceVoucher.printList')) }",
            printListTitle: "${ ui.encodeJavaScript(ui.message('transferapp.ambulanceVoucher.printList.title')) }",
            printListPeriod: "${ ui.encodeJavaScript(ui.message('transferapp.ambulanceVoucher.printList.period')) }",
            printListGenerated: "${ ui.encodeJavaScript(ui.message('transferapp.ambulanceVoucher.printList.generated')) }",
            printListEmpty: "${ ui.encodeJavaScript(ui.message('transferapp.ambulanceVoucher.printList.empty')) }",
            ambulanceProviderColumn: "${ ui.encodeJavaScript(ui.message('transferapp.ambulanceVoucher.search.column.ambulanceProvider')) }",
            ambulanceProviderOurs: "${ ui.encodeJavaScript(ui.message('transferapp.ambulanceVoucher.search.ambulanceProviderOurs')) }",
            actionColumn: "${ ui.encodeJavaScript(ui.message('transferapp.ambulanceVoucher.column.action')) }",
            columnNumber: "${ ui.encodeJavaScript(ui.message('transferapp.ambulanceVoucher.column.number')) }",
            columnDate: "${ ui.encodeJavaScript(ui.message('transferapp.ambulanceVoucher.column.date')) }",
            columnUpid: "${ ui.encodeJavaScript(ui.message('transferapp.ambulanceVoucher.column.upid')) }",
            columnPatient: "${ ui.encodeJavaScript(ui.message('transferapp.ambulanceVoucher.column.patient')) }",
            columnFrom: "${ ui.encodeJavaScript(ui.message('transferapp.ambulanceVoucher.column.from')) }",
            columnDestination: "${ ui.encodeJavaScript(ui.message('transferapp.ambulanceVoucher.column.destination')) }",
            columnDistance: "${ ui.encodeJavaScript(ui.message('transferapp.ambulanceVoucher.column.distance')) }",
            columnAmount: "${ ui.encodeJavaScript(ui.message('transferapp.ambulanceVoucher.column.amount')) }"
        }
    };
</script>

<div class="transfer-records-page transfer-ambulance-voucher-page">
${ ui.includeFragment("transferapp", "transfer/transferNav", [ activeTab: "ambulanceVoucher", app: appId ]) }

<h3 class="transfer-records-title">${ ui.message("transferapp.ambulanceVoucher.title") }</h3>
<% if (!canListTransfers) { %>
<div class="transfer-records-empty">${ ui.encodeHtmlContent(listAccessDeniedMessage ?: ui.message("transferapp.patient.transfers.listNotAllowed")) }</div>
<% } else { %>

<div class="transfer-ambulance-voucher-toolbar">
    <form id="transfer-ambulance-voucher-filter-form"
          class="transfer-records-filters transfer-ambulance-voucher-date-half"
          method="get"
          action="${ ui.pageLink('transferapp', 'ambulanceVoucher') }">
        <input type="hidden" name="app" value="${ ui.encodeHtmlAttribute(appId) }" />
        <div class="transfer-ambulance-voucher-panel-title">${ ui.message("transferapp.ambulanceVoucher.filter.title") }</div>
        <div class="transfer-ambulance-voucher-filters-grid">
            <div class="transfer-records-filter-field">
                <label for="ambulance-voucher-filter-start-date">${ ui.message("transferapp.ambulanceVoucher.filter.startDate") }</label>
                <input type="text"
                       id="ambulance-voucher-filter-start-date"
                       name="startDate"
                       class="transfer-records-date-input"
                       value="${ ui.encodeHtmlAttribute(filterStartDate ?: '') }"
                       autocomplete="off" />
            </div>
            <div class="transfer-records-filter-field">
                <label for="ambulance-voucher-filter-end-date">${ ui.message("transferapp.ambulanceVoucher.filter.endDate") }</label>
                <input type="text"
                       id="ambulance-voucher-filter-end-date"
                       name="endDate"
                       class="transfer-records-date-input"
                       value="${ ui.encodeHtmlAttribute(filterEndDate ?: '') }"
                       autocomplete="off" />
            </div>
            <div class="transfer-records-filter-actions">
                <button type="submit" id="ambulance-voucher-filter-apply" class="confirm">
                    ${ ui.message("transferapp.ambulanceVoucher.filter.apply") }
                </button>
            </div>
        </div>
        <p id="ambulance-voucher-filter-error" class="transfer-records-filter-error" style="display:none;"></p>
    </form>

    <div class="transfer-records-filters transfer-ambulance-voucher-search-half">
        <div class="transfer-ambulance-voucher-panel-title">${ ui.message("transferapp.ambulanceVoucher.search.title") }</div>
        <div class="transfer-ambulance-voucher-search-grid">
            <div class="transfer-records-filter-field transfer-ambulance-voucher-search-field">
                <label for="ambulance-voucher-identifier">${ ui.message("transferapp.ambulanceVoucher.search.identifier") }</label>
                <input type="text"
                       id="ambulance-voucher-identifier"
                       class="transfer-records-date-input"
                       placeholder="${ ui.encodeHtmlAttribute(ui.message('transferapp.ambulanceVoucher.search.identifier.placeholder')) }"
                       autocomplete="off" />
            </div>
            <div class="transfer-records-filter-actions">
                <button type="button" id="ambulance-voucher-identifier-search" class="confirm">
                    ${ ui.message("transferapp.ambulanceVoucher.search.button") }
                </button>
            </div>
        </div>
        <p id="ambulance-voucher-search-error" class="transfer-records-filter-error" style="display:none;"></p>    </div>
</div>

<div id="ambulance-voucher-preview-overlay" class="ambulance-voucher-preview-overlay" style="display:none;"></div>
<div id="ambulance-voucher-preview-dialog" class="dialog ambulance-voucher-preview-dialog" style="display:none;">
    <div class="dialog-header">
        <i class="icon-file-alt"></i>
        <h3>${ ui.message("transferapp.ambulanceVoucher.preview.title") }</h3>
    </div>
    <div class="dialog-content">
        <div id="ambulance-voucher-preview-body"></div>
        <div class="ambulance-voucher-preview-actions">
            <button type="button" id="ambulance-voucher-preview-print" class="confirm">
                ${ ui.message("transferapp.ambulanceVoucher.preview.print") }
            </button>
            <button type="button" id="ambulance-voucher-preview-close" class="cancel">
                ${ ui.message("coreapps.close") }
            </button>
        </div>
    </div>
</div>

<div id="ambulance-voucher-transfer-preview-overlay" class="ambulance-voucher-preview-overlay ambulance-voucher-transfer-preview-overlay" style="display:none;"></div>
<div id="ambulance-voucher-transfer-preview-dialog" class="dialog transfer-preview-dialog ambulance-voucher-transfer-preview-dialog" style="display:none;">
    <div class="dialog-header">
        <i class="icon-retweet"></i>
        <h3>${ ui.message("transferapp.ambulanceVoucher.preview.transferTitle") }</h3>
    </div>
    <div class="dialog-content">
        <div id="ambulance-voucher-transfer-preview-body"></div>
        <div class="transfer-preview-actions ambulance-voucher-preview-actions">
            <button type="button" id="ambulance-voucher-transfer-preview-print" class="confirm">
                ${ ui.message("transferapp.ambulanceVoucher.preview.print") }
            </button>
            <button type="button" id="ambulance-voucher-transfer-preview-close" class="cancel">
                ${ ui.message("coreapps.close") }
            </button>
        </div>
    </div>
</div>

<div id="ambulance-voucher-hie-search-overlay" class="ambulance-voucher-preview-overlay" style="display:none;"></div>
<div id="ambulance-voucher-hie-search-dialog" class="dialog ambulance-voucher-hie-search-dialog" style="display:none;">
    <div class="dialog-header">
        <i class="icon-search"></i>
        <h3 id="ambulance-voucher-hie-search-title">${ ui.message("transferapp.ambulanceVoucher.search.resultsTitle") }</h3>
    </div>
    <div class="dialog-content">
        <div id="ambulance-voucher-hie-search-body"></div>
        <div class="ambulance-voucher-preview-actions">
            <button type="button" id="ambulance-voucher-hie-search-close" class="cancel">
                ${ ui.message("coreapps.close") }
            </button>
        </div>
    </div>
</div>

<div id="ambulance-voucher-register-overlay" class="ambulance-voucher-preview-overlay ambulance-voucher-register-overlay" style="display:none;"></div>
<div id="ambulance-voucher-register-dialog" class="dialog ambulance-voucher-hie-search-dialog ambulance-voucher-register-dialog" style="display:none;">
    <div class="dialog-header">
        <i class="icon-user"></i>
        <h3 id="ambulance-voucher-register-title">${ ui.message("transferapp.pending.registration.preview.title") }</h3>
    </div>
    <div class="dialog-content">
        <div id="ambulance-voucher-register-body"></div>
        <div class="ambulance-voucher-preview-actions">
            <button type="button" id="ambulance-voucher-register-cancel" class="cancel">
                ${ ui.message("transferapp.pending.registration.preview.cancel") }
            </button>
            <button type="button" id="ambulance-voucher-register-confirm" class="confirm" style="display:none;">
                <i class="icon-ok"></i> ${ ui.message("transferapp.pending.registration.preview.confirm") }
            </button>
        </div>
    </div>
</div>

<div id="ambulance-voucher-create-overlay" class="ambulance-voucher-preview-overlay ambulance-voucher-create-overlay" style="display:none;"></div>
<div id="ambulance-voucher-create-dialog" class="dialog ambulance-voucher-create-dialog" style="display:none;">
    <div class="dialog-header">
        <i class="icon-file"></i>
        <h3>${ ui.message("transferapp.ambulanceVoucher.search.createVoucher.title") }</h3>
    </div>
    <div class="dialog-content">
        <div id="ambulance-voucher-create-body" class="ambulance-voucher-create-form">
            <div class="ambulance-voucher-create-field">
                <label>${ ui.message("transferapp.ambulanceVoucher.search.createVoucher.insuranceNumber") }</label>
                <div id="ambulance-voucher-create-insurance" class="ambulance-voucher-create-readonly">&nbsp;</div>
            </div>
            <div class="ambulance-voucher-create-field">
                <label for="ambulance-voucher-create-district">${ ui.message("transferapp.ambulanceVoucher.search.createVoucher.district") }</label>
                <input type="text"
                       id="ambulance-voucher-create-district"
                       maxlength="120"
                       autocomplete="off"
                       placeholder="${ ui.encodeHtmlAttribute(ui.message('transferapp.ambulanceVoucher.search.createVoucher.districtPlaceholder')) }" />
            </div>
            <div class="ambulance-voucher-create-field">
                <label for="ambulance-voucher-create-kilometers">${ ui.message("transferapp.ambulanceVoucher.search.createVoucher.distance") }</label>
                <input type="number"
                       id="ambulance-voucher-create-kilometers"
                       min="1"
                       step="1"
                       autocomplete="off" />
            </div>
            <div class="ambulance-voucher-create-field">
                <label>${ ui.message("transferapp.ambulanceVoucher.search.createVoucher.route") }</label>
                <p id="ambulance-voucher-create-route" class="ambulance-voucher-create-route"></p>
            </div>
            <p id="ambulance-voucher-create-error" class="bon-error" style="display:none;"></p>
        </div>
        <div class="ambulance-voucher-preview-actions">
            <button type="button" id="ambulance-voucher-create-cancel" class="cancel">
                ${ ui.message("transferapp.ambulanceVoucher.search.createVoucher.cancel") }
            </button>
            <button type="button" id="ambulance-voucher-create-confirm" class="confirm">
                <i class="icon-ok"></i> ${ ui.message("transferapp.ambulanceVoucher.search.createVoucher.confirm") }
            </button>
        </div>
    </div>
</div>

<% if (!hasVouchers) { %>
<div class="transfer-records-empty">${ ui.message("transferapp.ambulanceVoucher.empty") }</div>
<% } else { %>
<div class="transfer-ambulance-voucher-list-toolbar">
    <button type="button" id="ambulance-voucher-print-list" class="confirm">
        <i class="icon-print"></i> ${ ui.message("transferapp.ambulanceVoucher.printList") }
    </button>
</div>
<div class="transfer-table-wrapper">
    <table id="transfer-ambulance-voucher-table" class="transfer-datatable display">
        <thead>
            <tr>
                <th>${ ui.message("transferapp.ambulanceVoucher.column.number") }</th>
                <th>${ ui.message("transferapp.ambulanceVoucher.column.date") }</th>
                <th>${ ui.message("transferapp.ambulanceVoucher.column.upid") }</th>
                <th>${ ui.message("transferapp.ambulanceVoucher.column.patient") }</th>
                <th>${ ui.message("transferapp.ambulanceVoucher.column.from") }</th>
                <th>${ ui.message("transferapp.ambulanceVoucher.column.destination") }</th>
                <th>${ ui.message("transferapp.ambulanceVoucher.column.distance") }</th>
                <th>${ ui.message("transferapp.ambulanceVoucher.column.amount") }</th>
                <th>${ ui.message("transferapp.ambulanceVoucher.column.action") }</th>
            </tr>
        </thead>
        <tbody>
            <% vouchers.each { voucher -> %>
            <tr>
                <td>${ voucher.rowNumber }</td>
                <td>${ ui.format(voucher.transferDate) }</td>
                <td>${ ui.encodeHtmlContent(voucher.patientUpid ?: '') }</td>
                <td>${ ui.encodeHtmlContent(voucher.patientName ?: '') }</td>
                <td>${ ui.encodeHtmlContent(voucher.fromHospital ?: '') }</td>
                <td>${ ui.encodeHtmlContent(voucher.destinationHospital ?: '') }</td>
                <td>${ voucher.distance != null ? voucher.distance : '' }</td>
                <td>${ voucher.amount != null ? voucher.amount : '' }</td>
                <td class="ambulance-voucher-actions">
                    <a class="ambulance-voucher-preview-link"
                       href="javascript:void(0);"
                       data-uuid="${ ui.encodeHtmlAttribute(voucher.transferUuid ?: '') }"
                       title="${ ui.encodeHtmlAttribute(ui.message('transferapp.ambulanceVoucher.preview.action')) }">
                        <i class="icon-eye-open"></i> ${ ui.message("transferapp.ambulanceVoucher.preview.action") }
                    </a>
                    <a class="ambulance-voucher-transfer-preview-link"
                       href="javascript:void(0);"
                       data-uuid="${ ui.encodeHtmlAttribute(voucher.transferUuid ?: '') }"
                       title="${ ui.encodeHtmlAttribute(ui.message('transferapp.ambulanceVoucher.preview.transfer')) }">
                        <i class="icon-retweet"></i> ${ ui.message("transferapp.ambulanceVoucher.preview.transfer") }
                    </a>
                </td>
            </tr>
            <% } %>
        </tbody>
    </table>
</div>
<% } %>
<% } %>
</div>
