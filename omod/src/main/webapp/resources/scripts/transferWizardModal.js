(function (jq) {
	'use strict';

	var TOTAL_STEPS = 6;
	var currentStep = 1;
	var REFERRAL_CAUSE_ONLY_MODE = false;

	var STEP_META = [
		{
			shortName: 'Client',
			lead: 'Step 1 — client identification, demographics, and admission.'
		},
		{
			shortName: 'Referral',
			leadWithClient: 'Step 2 — referral details for'
		},
		{
			shortName: 'Clinical',
			leadWithClient: 'Step 3 — type of transfer and clinical findings for'
		},
		{
			shortName: 'Vitals',
			leadWithClient: 'Step 4 — vital signs for'
		},
		{
			shortName: 'Findings',
			leadWithClient: 'Step 5 — laboratory, diagnosis, and transport for'
		},
		{
			shortName: 'Sign-off',
			leadWithClient: 'Step 6 — health insurance and referring provider for'
		}
	];

	function getForm() {
		return jq('#moh-transfer-wizard-form');
	}

	function normalizeRootUrl(path) {
		var p = String(path || '');
		if (p.indexOf('http://') === 0 || p.indexOf('https://') === 0) {
			return p;
		}
		while (p.indexOf('//') === 0) {
			p = p.substring(1);
		}
		if (p.charAt(0) !== '/') {
			p = '/' + p;
		}
		while (p.indexOf('//') !== -1) {
			p = p.split('//').join('/');
		}
		return p;
	}

	function resolveOpenmrsPath() {
		if (typeof window.transferOpenmrsPath === 'string' && window.transferOpenmrsPath) {
			return normalizeRootUrl(window.transferOpenmrsPath);
		}
		if (typeof openmrsContextPath !== 'undefined' && openmrsContextPath) {
			return normalizeRootUrl(openmrsContextPath);
		}
		return '/openmrs';
	}

	function getClientName() {
		var $form = getForm();
		var fromField = jq.trim(jq('#clientName').val() || '');
		if (fromField) {
			return fromField;
		}
		return jq.trim($form.attr('data-client-name') || '');
	}

	function getPanels() {
		return jq('.transfer-wizard-step-panel');
	}

	function isSingleReferralForm() {
		return getPanels().length === 0;
	}

	function getProgressItems() {
		return jq('.transfer-wizard-progress li');
	}

	function setFieldReadOnly(el) {
		var tag = (el.tagName || '').toLowerCase();
		var type = (el.type || '').toLowerCase();

		if (tag === 'input' && (type === 'radio' || type === 'checkbox')) {
			el.disabled = true;
			return;
		}
		if (tag === 'select') {
			el.disabled = true;
			return;
		}
		if (tag === 'textarea' || tag === 'input') {
			el.readOnly = true;
			return;
		}
		el.disabled = true;
	}

	function applyReferralCauseOnlyMode() {
		var $form = getForm();
		if (!$form.length) {
			return;
		}

		// Keep only "Reason for transfer" editable; everything else is prefilled/read-only.
		var allowEditableNames = {
			reasonForTransfer: true
		};

		$form.find('input, select, textarea').each(function () {
			var name = this.name || '';
			if (allowEditableNames[name]) {
				this.disabled = false;
				this.readOnly = false;
				this.required = true;
				return;
			}
			this.required = false;
			setFieldReadOnly(this);
		});

		var $step3 = getPanels().filter('[data-step="3"]');
		var $reasonSection = jq('#reasonForTransfer').closest('.transfer-wizard-section');
		if ($step3.length && $reasonSection.length) {
			$step3.children('.transfer-wizard-section').not($reasonSection).hide();
		}

		getProgressItems().hide();
		jq('.transfer-wizard-page-title').text('New transfer — Referral cause');
		jq('.transfer-wizard-page-lead').text('Only referral cause is entered manually. Other information is fetched from the database.');
		jq('#transfer-wizard-next').hide();
		jq('#transfer-wizard-back').hide();
		jq('#transfer-wizard-submit-btn').text('Save referral cause').show();
		jq('#transfer-wizard-cancel').show();
	}

	function buildPageLead(step) {
		var meta = STEP_META[step - 1];
		if (!meta) {
			return '';
		}
		if (meta.lead) {
			return meta.lead;
		}
		var clientName = getClientName() || 'client';
		return meta.leadWithClient + ' <strong>' + jq('<div/>').text(clientName).html() + '</strong>.';
	}

	function updateWizardHeader(step) {
		var meta = STEP_META[step - 1];
		if (!meta) {
			return;
		}

		jq('#wizard-page-title').text('New transfer — ' + meta.shortName);
		jq('#wizard-page-lead').html(buildPageLead(step));

		var modalTitle = jq('#new-transfer-out-title');
		if (modalTitle.length) {
			modalTitle.text('New transfer — ' + meta.shortName);
		}
	}

	function updateActionButtons(step) {
		var $next = jq('#transfer-wizard-next');
		var $back = jq('#transfer-wizard-back');
		var $submit = jq('#transfer-wizard-submit-btn');
		var $cancel = jq('#transfer-wizard-cancel');

		if (isSingleReferralForm()) {
			$next.hide();
			$back.hide();
			$submit.show();
			$cancel.show();
			return;
		}

		$next.toggle(step < TOTAL_STEPS);
		$submit.toggle(step === TOTAL_STEPS);
		$back.toggle(step > 1);
		$cancel.toggle(step === 1);

		if (step < TOTAL_STEPS) {
			$next.text('Continue to step ' + (step + 1));
		}
		if (step > 1) {
			$back.text('Back to step ' + (step - 1));
		}
	}

	function updateProgress(step) {
		getProgressItems().each(function (index) {
			var itemStep = index + 1;
			var $item = jq(this);
			$item.removeClass('active done');
			if (itemStep < step) {
				$item.addClass('done');
			} else if (itemStep === step) {
				$item.addClass('active');
			}
		});
	}

	function showStep(step) {
		currentStep = step;
		getPanels().removeClass('is-active');
		getPanels().filter('[data-step="' + step + '"]').addClass('is-active');
		updateProgress(step);
		updateWizardHeader(step);
		updateActionButtons(step);

		var scrollContainer = jq('.transfer-wizard-scroll');
		if (scrollContainer.length) {
			scrollContainer.scrollTop(0);
		}
	}

	function validateCurrentStep() {
		if (isSingleReferralForm()) {
			var $form = getForm();
			if (!$form.length || !$form[0].checkValidity()) {
				if ($form.length && typeof $form[0].reportValidity === 'function') {
					$form[0].reportValidity();
				}
				return false;
			}
			return true;
		}

		var $panel = getPanels().filter('[data-step="' + currentStep + '"]');
		var valid = true;
		$panel.find('input, select, textarea').each(function () {
			var el = this;
			if (el.disabled) {
				return;
			}
			if (!el.checkValidity()) {
				valid = false;
				if (typeof el.reportValidity === 'function') {
					el.reportValidity();
				}
				return false;
			}
		});
		return valid;
	}

	function syncEmergencyPanel() {
		var isEmergency = jq('input[name="transferType"]:checked').val() === 'EMERGENCY';
		var $panel = jq('#emergencyFields');
		$panel.toggleClass('is-visible', isEmergency);
		$panel.find('[data-emergency-required="true"]').prop('required', isEmergency);
		if (!isEmergency) {
			$panel.find('[data-emergency-required="true"]').val('');
		}
		syncTransportByTransferType();
	}

	function syncTransportByTransferType() {
		var transferType = jq('input[name="transferType"]:checked').val();
		var isEmergency = transferType === 'EMERGENCY';
		var $ambulance = jq('input[name="transportationType"][value="AMBULANCE"]');
		var $other = jq('input[name="transportationType"][value="OTHER"]');
		var $na = jq('input[name="transportationType"][value="NA"]');
		var $enabledTransport = jq('input[name="transportationType"]:not(:disabled)');

		if (isEmergency) {
			$ambulance.prop('checked', true).prop('disabled', false);
			$other.prop('checked', false).prop('disabled', true);
			$na.prop('checked', false).prop('disabled', true);
			$enabledTransport.prop('required', false);
		}
		else {
			$ambulance.prop('checked', false).prop('disabled', true);
			$other.prop('disabled', false);
			$na.prop('disabled', false);
			$enabledTransport.prop('required', transferType != null && transferType !== '');
		}
		syncTransportOther();
	}

	function syncTransportOther() {
		var isOther = jq('input[name="transportationType"]:checked').val() === 'OTHER';
		var $panel = jq('#transportOtherField');
		var $input = jq('#transportationOtherSpec');
		$panel.toggleClass('is-visible', isOther);
		$input.prop('required', isOther);
		if (!isOther) {
			$input.val('');
		}
		syncAmbulanceProvider();
	}

	function getAmbulanceProviderPanel() {
		return jq('#ambulanceProviderField');
	}

	function destroyAmbulanceProviderSelect2() {
		var $ = getWizardJq();
		var $select = $('#ambulanceProviderFosaId');
		if ($select.length && $select.hasClass('select2-hidden-accessible') && typeof $.fn.select2 === 'function') {
			$select.select2('destroy');
		}
	}

	function initAmbulanceProviderSelect2() {
		var $ = getWizardJq();
		if (typeof $.fn.select2 !== 'function') {
			return;
		}
		var $select = $('#ambulanceProviderFosaId');
		if (!$select.length) {
			return;
		}
		var $dropdownParent = $('#new-transfer-out-dialog');
		destroyAmbulanceProviderSelect2();
		$select.select2({
			width: '100%',
			placeholder: $select.attr('data-placeholder') || 'Select facility providing the ambulance',
			allowClear: true,
			dropdownParent: $dropdownParent.length ? $dropdownParent : $('body')
		});
		$select.off('change.ambulanceProvider').on('change.ambulanceProvider', function () {
			syncAmbulanceProviderNameFromSelect();
		});
	}

	var ambulanceProviderFacilitiesByCode = {};

	function normalizeFosaCode(code) {
		var value = String(code == null ? '' : code).trim();
		if (/^\d+$/.test(value) && value.length < 4) {
			while (value.length < 4) {
				value = '0' + value;
			}
		}
		return value;
	}

	function lookupAmbulanceProviderName(fosaId) {
		var code = String(fosaId == null ? '' : fosaId).trim();
		if (!code) {
			return '';
		}
		if (ambulanceProviderFacilitiesByCode[code]) {
			return ambulanceProviderFacilitiesByCode[code];
		}
		var normalized = normalizeFosaCode(code);
		if (ambulanceProviderFacilitiesByCode[normalized]) {
			return ambulanceProviderFacilitiesByCode[normalized];
		}
		var found = '';
		jq.each(ambulanceProviderFacilitiesByCode, function (key, name) {
			if (normalizeFosaCode(key) === normalized) {
				found = name;
				return false;
			}
		});
		return found;
	}

	function syncAmbulanceProviderNameFromSelect() {
		var $select = jq('#ambulanceProviderFosaId');
		var $name = jq('#ambulanceProviderName');
		if (!$select.length || !$name.length) {
			return;
		}
		var selected = $select.val();
		if (selected == null || selected === '') {
			$name.val('');
			return;
		}
		// Do not use option:selected — Select2 can leave stale selected attributes on
		// the previously chosen option, which caused the current facility name to stick.
		var selectedCode = String(selected);
		var label = lookupAmbulanceProviderName(selectedCode);
		if (!label) {
			var $option = $select.find('option').filter(function () {
				return String(jq(this).val()) === selectedCode
					|| normalizeFosaCode(jq(this).val()) === normalizeFosaCode(selectedCode);
			}).first();
			label = jq.trim($option.attr('data-facility-name') || $option.text() || '');
		}
		if (!label && $select.hasClass('select2-hidden-accessible') && typeof $select.select2 === 'function') {
			try {
				var data = $select.select2('data');
				if (data && data.length && data[0] && data[0].text) {
					label = jq.trim(String(data[0].text));
				}
			} catch (ignoreSelect2) {
				// ignore
			}
		}
		$name.val(label);
	}

	function clearAmbulanceProviderSelection() {
		var $select = jq('#ambulanceProviderFosaId');
		var $name = jq('#ambulanceProviderName');
		if ($name.length) {
			$name.val('');
		}
		if ($select.length) {
			$select.find('option').prop('selected', false);
			$select.val(null);
			if ($select.hasClass('select2-hidden-accessible')) {
				$select.trigger('change');
			}
		}
	}

	function updateAmbulanceProviderOptions(facilities, preferredFosaId, preferredName, currentFosaId) {
		var $select = jq('#ambulanceProviderFosaId');
		if (!$select.length) {
			return;
		}
		var preferred = jq.trim(preferredFosaId || '');
		var preferredNormalized = normalizeFosaCode(preferred);
		var preferredLabel = jq.trim(preferredName || '');
		var current = jq.trim(currentFosaId || '');
		var currentNormalized = normalizeFosaCode(current);
		destroyAmbulanceProviderSelect2();
		$select.empty();
		$select.append(jq('<option value="">'));
		ambulanceProviderFacilitiesByCode = {};
		var hasPreferred = false;
		var hasCurrent = false;
		jq.each(facilities || [], function (_, facility) {
			if (!facility || !facility.code) {
				return;
			}
			var code = String(facility.code).trim();
			var name = String(facility.name || code).trim();
			ambulanceProviderFacilitiesByCode[code] = name;
			var codeNormalized = normalizeFosaCode(code);
			var $option = jq('<option>')
				.attr('value', code)
				.attr('data-facility-name', name)
				.text(name);
			if (preferred && (preferred === code || preferredNormalized === codeNormalized)) {
				$option.prop('selected', true);
				hasPreferred = true;
				preferred = code;
				if (!preferredLabel) {
					preferredLabel = name;
				}
			}
			if (current && (current === code || currentNormalized === codeNormalized)) {
				hasCurrent = true;
				current = code;
			}
			$select.append($option);
		});
		if (preferred && !hasPreferred) {
			ambulanceProviderFacilitiesByCode[preferred] = preferredLabel || preferred;
			$select.append(jq('<option>')
				.attr('value', preferred)
				.attr('data-facility-name', preferredLabel || preferred)
				.text(preferredLabel || preferred)
				.prop('selected', true));
			hasPreferred = true;
		}
		$select.find('option').prop('selected', false);
		if (!hasPreferred && current && hasCurrent) {
			$select.val(current);
		}
		else if (hasPreferred) {
			$select.val(preferred);
		}
		else {
			$select.val(null);
		}
		initAmbulanceProviderSelect2();
		syncAmbulanceProviderNameFromSelect();
	}

	function getAmbulanceProviderFacilitiesUrl() {
		var $panel = getAmbulanceProviderPanel();
		var configured = jq.trim($panel.attr('data-facilities-url') || '');
		if (configured) {
			return normalizeRootUrl(configured);
		}
		return normalizeRootUrl(resolveOpenmrsPath() + '/module/transferapp/transfer/ambulanceProviderFacilities.form');
	}

	var ambulanceProviderFacilitiesLoaded = false;
	var ambulanceProviderFacilitiesLoading = false;

	function loadAmbulanceProviderFacilities() {
		var $panel = getAmbulanceProviderPanel();
		if (!$panel.length || ambulanceProviderFacilitiesLoading) {
			return;
		}
		if (ambulanceProviderFacilitiesLoaded) {
			initAmbulanceProviderSelect2();
			return;
		}
		ambulanceProviderFacilitiesLoading = true;
		var preferredFosaId = jq.trim($panel.attr('data-preferred-fosa-id') || '');
		var preferredName = jq.trim($panel.attr('data-preferred-name') || '');
		var currentFosaId = jq.trim($panel.attr('data-current-fosa-id') || '');
		jq.getJSON(getAmbulanceProviderFacilitiesUrl())
			.done(function (response) {
				if (!response || response.status !== 'success') {
					updateAmbulanceProviderOptions([], preferredFosaId, preferredName, currentFosaId);
					return;
				}
				var facilities = response.facilities ? response.facilities : [];
				if (response.currentFosaId) {
					currentFosaId = jq.trim(response.currentFosaId);
					$panel.attr('data-current-fosa-id', currentFosaId);
				}
				updateAmbulanceProviderOptions(facilities, preferredFosaId, preferredName, currentFosaId);
				ambulanceProviderFacilitiesLoaded = true;
			})
			.fail(function () {
				updateAmbulanceProviderOptions([], preferredFosaId, preferredName, currentFosaId);
			})
			.always(function () {
				ambulanceProviderFacilitiesLoading = false;
			});
	}

	function syncAmbulanceProvider() {
		var isAmbulance = jq('input[name="transportationType"]:checked').val() === 'AMBULANCE';
		var $panel = getAmbulanceProviderPanel();
		var $select = jq('#ambulanceProviderFosaId');
		$panel.toggleClass('is-visible', isAmbulance);
		$select.prop('required', isAmbulance);
		if (!isAmbulance) {
			clearAmbulanceProviderSelection();
			return;
		}
		loadAmbulanceProviderFacilities();
	}

	function syncInsuranceOther() {
		var isOther = jq('input[name="healthInsuranceType"]:checked').val() === 'OTHER';
		var $panel = jq('#insuranceOtherField');
		var $input = jq('#healthInsuranceOtherSpec');
		$panel.toggleClass('is-visible', isOther);
		$input.prop('required', isOther);
		if (!isOther) {
			$input.val('');
		}
	}

	function getSelectedReceivingFacilityId() {
		return jq.trim(jq('#receivingFacilityCode option:selected').attr('data-receiving-facility-id') || '');
	}

	function syncReceivingFacilityIdField() {
		jq('#receivingFacilityId').val(getSelectedReceivingFacilityId());
	}

	function getWizardJq() {
		return (typeof jq !== 'undefined') ? jq : jQuery;
	}

	function destroyReceivingServiceSelect2() {
		var $ = getWizardJq();
		var $select = $('#receivingService');
		if ($select.length && $select.hasClass('select2-hidden-accessible') && typeof $.fn.select2 === 'function') {
			$select.select2('destroy');
		}
	}

	function initReceivingServiceSelect2() {
		var $ = getWizardJq();
		if (typeof $.fn.select2 !== "function") {
			return;
		}
		var $select = $('#receivingService');
		if (!$select.length) {
			return;
		}
		var $dropdownParent = $('#new-transfer-out-dialog');
		destroyReceivingServiceSelect2();
		$select.select2({
			width: '100%',
			placeholder: $select.attr('data-placeholder') || 'Select or enter receiving service',
			allowClear: true,
			tags: true,
			dropdownParent: $dropdownParent.length ? $dropdownParent : $('body'),
			createTag: function (params) {
				var term = $.trim(params.term);
				if (term === '') {
					return null;
				}
				return {
					id: term,
					text: term,
					newTag: true
				};
			}
		});
	}

	function updateReceivingServiceOptions(serviceNames, preferredService) {
		var $select = jq('#receivingService');
		if (!$select.length) {
			return;
		}
		var preferred = preferredService;
		if (preferred == null || preferred === undefined) {
			preferred = jq.trim($select.closest('form').attr('data-preferred-receiving-service') || $select.val() || '');
		}
		destroyReceivingServiceSelect2();
		$select.empty();
		$select.append(jq('<option value="">'));
		var hasPreferred = false;
		jq.each(serviceNames || [], function (_, serviceName) {
			if (!serviceName) {
				return;
			}
			var $option = jq('<option>').attr('value', serviceName).text(serviceName);
			if (preferred && preferred === serviceName) {
				$option.attr('selected', 'selected');
				hasPreferred = true;
			}
			$select.append($option);
		});
		if (preferred && !hasPreferred) {
			$select.append(jq('<option>').attr('value', preferred).text(preferred).attr('selected', 'selected'));
			hasPreferred = true;
		}
		$select.val(hasPreferred ? preferred : null);
		initReceivingServiceSelect2();
	}

	function destroyDateTimePickers() {
		if (typeof flatpickr !== 'function') {
			return;
		}
		jq('.js-datetime-picker, .js-time-picker').each(function () {
			if (this._flatpickr) {
				this._flatpickr.destroy();
			}
		});
	}

	function initDateTimePickers() {
		if (typeof flatpickr !== 'function') {
			return;
		}
		destroyDateTimePickers();
		document.querySelectorAll('#moh-transfer-wizard-form .js-datetime-picker').forEach(function (el) {
			flatpickr(el, {
				enableTime: true,
				dateFormat: 'Y-m-d H:i',
				altInput: true,
				altFormat: 'd/m/Y H:i',
				time_24hr: true,
				allowInput: true,
				disableMobile: true
			});
		});
		document.querySelectorAll('#moh-transfer-wizard-form .js-time-picker').forEach(function (el) {
			flatpickr(el, {
				enableTime: true,
				noCalendar: true,
				dateFormat: 'H:i',
				time_24hr: true,
				allowInput: true,
				disableMobile: true
			});
		});
	}

	function bindReceivingFacilityServices() {
		jq(document).off('change.transferWizard', '#receivingFacilityCode');
		jq(document).on('change.transferWizard', '#receivingFacilityCode', function () {
			syncReceivingFacilityIdField();
			loadReceivingServicesForSelectedFacility();
		});
	}

	function getReceivingServicesUrl() {
		return normalizeRootUrl(resolveOpenmrsPath() + '/module/transferapp/admin/receivingServices.form');
	}

	function loadReceivingServicesForSelectedFacility() {
		var facilityId = getSelectedReceivingFacilityId();
		var preferredService = jq.trim(getForm().attr('data-preferred-receiving-service') || '');
		syncReceivingFacilityIdField();
		if (!facilityId) {
			updateReceivingServiceOptions([], preferredService);
			return;
		}

		jq.getJSON(getReceivingServicesUrl(), { receivingFacilityId: facilityId })
			.done(function (response) {
				if (response && response.status === 'success') {
					updateReceivingServiceOptions(response.services, preferredService);
				} else {
					updateReceivingServiceOptions([], preferredService);
				}
			})
			.fail(function () {
				updateReceivingServiceOptions([], preferredService);
			});
	}

	function bindToggles() {
		jq(document).off('change.transferWizard', 'input[name="transferType"]');
		jq(document).on('change.transferWizard', 'input[name="transferType"]', syncEmergencyPanel);

		jq(document).off('change.transferWizard', 'input[name="transportationType"]');
		jq(document).on('change.transferWizard', 'input[name="transportationType"]', syncTransportOther);

		jq(document).off('change.transferWizard', 'input[name="healthInsuranceType"]');
		jq(document).on('change.transferWizard', 'input[name="healthInsuranceType"]', syncInsuranceOther);

		jq(document).off('input.transferWizard', '#clientName');
		jq(document).on('input.transferWizard', '#clientName', function () {
			if (currentStep > 1) {
				updateWizardHeader(currentStep);
			}
		});
	}

	function bindNavigation() {
		jq(document).off('click.transferWizard', '#transfer-wizard-next');
		jq(document).on('click.transferWizard', '#transfer-wizard-next', function (e) {
			e.preventDefault();
			if (!validateCurrentStep()) {
				return;
			}
			if (currentStep < TOTAL_STEPS) {
				showStep(currentStep + 1);
			}
		});

		jq(document).off('click.transferWizard', '#transfer-wizard-back');
		jq(document).on('click.transferWizard', '#transfer-wizard-back', function (e) {
			e.preventDefault();
			if (currentStep > 1) {
				showStep(currentStep - 1);
			}
		});
	}

	window.initTransferWizardModal = function () {
		var $form = getForm();
		if (!$form.length) {
			return;
		}

		currentStep = 1;
		ambulanceProviderFacilitiesLoaded = false;
		ambulanceProviderFacilitiesLoading = false;
		ambulanceProviderFacilitiesByCode = {};
		bindToggles();
		bindReceivingFacilityServices();
		bindNavigation();
		syncReceivingFacilityIdField();
		initReceivingServiceSelect2();
		initDateTimePickers();
		syncEmergencyPanel();
		syncTransportByTransferType();
		syncTransportOther();
		syncAmbulanceProvider();
		syncInsuranceOther();
		showStep(REFERRAL_CAUSE_ONLY_MODE ? 3 : 1);
		if (REFERRAL_CAUSE_ONLY_MODE) {
			applyReferralCauseOnlyMode();
		}
		loadReceivingServicesForSelectedFacility();

		var isEditing = $form.attr('data-editing') === 'true';
		if (isEditing) {
			jq('.transfer-wizard-page-title').first().text('Edit Transfer Form');
			jq('#transfer-wizard-submit-btn').text('Update referral information');
		} else {
			jq('#transfer-wizard-submit-btn').text('Save referral information');
		}
	};

})(typeof jq !== 'undefined' ? jq : jQuery);
