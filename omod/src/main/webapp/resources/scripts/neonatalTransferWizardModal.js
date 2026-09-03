(function (jq) {
	'use strict';

	var TOTAL_STEPS = 7;
	var currentStep = 1;

	var STEP_META = [
		{
			shortName: 'Baby & Referral',
			lead: 'Step 1 — baby identification, mother details, and referral.'
		},
		{
			shortName: 'Maternal History',
			leadWithClient: 'Step 2 — maternal history for'
		},
		{
			shortName: 'Labor Details',
			leadWithClient: 'Step 3 — labor details for'
		},
		{
			shortName: 'Neonatal History & Drugs',
			leadWithClient: 'Step 4 — neonatal history and drugs for'
		},
		{
			shortName: 'Chief Complaint & Diagnoses',
			leadWithClient: 'Step 5 — chief complaint, clinical condition, and diagnoses for'
		},
		{
			shortName: 'Management',
			leadWithClient: 'Step 6 — management at the referring facility for'
		},
		{
			shortName: 'Summary & Sign-off',
			leadWithClient: 'Step 7 — clinical management summary and referring provider sign-off for'
		}
	];

	function getForm() {
		return jq('#moh-neonatal-transfer-wizard-form');
	}

	function getClientName() {
		var $form = getForm();
		var fromField = jq.trim(jq('#neonatalBabyName').val() || '');
		if (fromField) {
			return fromField;
		}
		return jq.trim($form.attr('data-client-name') || '');
	}

	function getPanels() {
		return getForm().find('.transfer-wizard-step-panel');
	}

	function getProgressItems() {
		return jq('.transfer-wizard-progress-7 li');
	}

	function buildPageLead(step) {
		var meta = STEP_META[step - 1];
		if (!meta) {
			return '';
		}
		if (meta.lead) {
			return meta.lead;
		}
		var clientName = getClientName() || 'baby';
		return meta.leadWithClient + ' <strong>' + jq('<div/>').text(clientName).html() + '</strong>.';
	}

	function updateWizardHeader(step) {
		var meta = STEP_META[step - 1];
		if (!meta) {
			return;
		}

		jq('#neonatal-wizard-page-title').text('Neonatal transfer — ' + meta.shortName);
		jq('#neonatal-wizard-page-lead').html(buildPageLead(step));

		var modalTitle = jq('#new-neonatal-transfer-out-title');
		if (modalTitle.length) {
			modalTitle.text('Neonatal transfer — ' + meta.shortName);
		}
	}

	function updateActionButtons(step) {
		var $next = jq('#neonatal-wizard-next');
		var $back = jq('#neonatal-wizard-back');
		var $submit = jq('#neonatal-wizard-submit-btn');
		var $cancel = jq('#neonatal-wizard-cancel');

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

		var scrollContainer = jq('#new-neonatal-transfer-out-data .transfer-wizard-scroll');
		if (scrollContainer.length) {
			scrollContainer.scrollTop(0);
		}
	}

	function validateCurrentStep() {
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

	function syncTransportOther() {
		var isOther = jq('#moh-neonatal-transfer-wizard-form input[name="modeOfTransport"]:checked').val() === 'OTHER';
		var $panel = jq('#neonatalTransportOtherField');
		var $input = jq('#neonatalTransportOther');
		$panel.toggleClass('is-visible', isOther);
		$input.prop('required', isOther);
		if (!isOther) {
			$input.val('');
		}
	}

	function getSelectedReceivingFacilityId() {
		return jq.trim(jq('#neonatalReceivingFacilityCode option:selected').attr('data-receiving-facility-id') || '');
	}

	function syncReceivingFacilityIdField() {
		jq('#neonatalReceivingFacilityId').val(getSelectedReceivingFacilityId());
	}

	function getWizardJq() {
		return (typeof jq !== 'undefined') ? jq : jQuery;
	}

	function destroyReceivingServiceSelect2() {
		var $ = getWizardJq();
		var $select = $('#neonatalReceivingService');
		if ($select.length && $select.hasClass('select2-hidden-accessible') && typeof $.fn.select2 === 'function') {
			$select.select2('destroy');
		}
	}

	function initReceivingServiceSelect2() {
		var $ = getWizardJq();
		if (typeof $.fn.select2 !== 'function') {
			return;
		}
		var $select = $('#neonatalReceivingService');
		if (!$select.length) {
			return;
		}
		var $dropdownParent = $('#new-neonatal-transfer-out-dialog');
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

	function updateReceivingServiceOptions(serviceNames) {
		var $select = jq('#neonatalReceivingService');
		if (!$select.length) {
			return;
		}
		destroyReceivingServiceSelect2();
		$select.empty();
		$select.append(jq('<option value="">'));
		jq.each(serviceNames || [], function (_, serviceName) {
			if (!serviceName) {
				return;
			}
			$select.append(jq('<option>').attr('value', serviceName).text(serviceName));
		});
		$select.val(null);
		initReceivingServiceSelect2();
	}

	function getReceivingServicesUrl() {
		var path = (typeof openmrsContextPath !== 'undefined' ? openmrsContextPath : '');
		return path + '/module/transferapp/admin/receivingServices.form';
	}

	function loadReceivingServicesForSelectedFacility() {
		var facilityId = getSelectedReceivingFacilityId();
		syncReceivingFacilityIdField();
		if (!facilityId) {
			updateReceivingServiceOptions([]);
			return;
		}

		jq.getJSON(getReceivingServicesUrl(), { receivingFacilityId: facilityId })
			.done(function (response) {
				if (response && response.status === 'success') {
					updateReceivingServiceOptions(response.services);
				} else {
					updateReceivingServiceOptions([]);
				}
			})
			.fail(function () {
				updateReceivingServiceOptions([]);
			});
	}

	function bindReceivingFacilityServices() {
		jq(document).off('change.neonatalTransferWizard', '#neonatalReceivingFacilityCode');
		jq(document).on('change.neonatalTransferWizard', '#neonatalReceivingFacilityCode', function () {
			syncReceivingFacilityIdField();
			loadReceivingServicesForSelectedFacility();
		});
	}

	function destroyDateTimePickers() {
		if (typeof flatpickr !== 'function') {
			return;
		}
		jq('#moh-neonatal-transfer-wizard-form .js-datetime-picker, #moh-neonatal-transfer-wizard-form .js-time-picker, #moh-neonatal-transfer-wizard-form .js-date-picker')
			.each(function () {
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
		document.querySelectorAll('#moh-neonatal-transfer-wizard-form .js-datetime-picker').forEach(function (el) {
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
		document.querySelectorAll('#moh-neonatal-transfer-wizard-form .js-time-picker').forEach(function (el) {
			flatpickr(el, {
				enableTime: true,
				noCalendar: true,
				dateFormat: 'H:i',
				time_24hr: true,
				allowInput: true,
				disableMobile: true
			});
		});
		document.querySelectorAll('#moh-neonatal-transfer-wizard-form .js-date-picker').forEach(function (el) {
			flatpickr(el, {
				enableTime: false,
				dateFormat: 'Y-m-d',
				altInput: true,
				altFormat: 'd/m/Y',
				allowInput: true,
				disableMobile: true
			});
		});
	}

	function syncConditionalPanel(triggerSelector, panelSelector, requiredValue) {
		var value = jq(triggerSelector).val();
		var isVisible = value === requiredValue;
		jq(panelSelector).toggle(isVisible);
		if (!isVisible) {
			jq(panelSelector).find('input, select, textarea').val('').prop('checked', false);
		}
	}

	function syncNeonatalConditionalPanels() {
		syncConditionalPanel('#neonatalResuscitationAtBirth', '#neonatalResuscitationMethodsPanel', 'Yes');
		syncConditionalPanel('#neonatalHie', '#neonatalHieGradePanel', 'Yes');
		syncConditionalPanel('#neonatalImmunization', '#neonatalImmunizationDetailsPanel', 'Yes');
		syncConditionalPanel('#neonatalInotropes', '#neonatalInotropesSpecifyPanel', 'Yes');
		syncConditionalPanel('#neonatalImagingResultsAvailable', '#neonatalImagingResultsPanel', 'Yes');

		var fbcDone = jq('#neonatalLabFbc').val();
		var fbcVisible = fbcDone === 'Yes';
		jq('#neonatalLabHbPanel, #neonatalLabWbcPanel, #neonatalLabPlateletsPanel').toggle(fbcVisible);
		if (!fbcVisible) {
			jq('#neonatalLabHbPanel, #neonatalLabWbcPanel, #neonatalLabPlateletsPanel').find('input').val('');
		}
	}

	function bindToggles() {
		jq(document).off('change.neonatalTransferWizard', '#moh-neonatal-transfer-wizard-form input[name="modeOfTransport"]');
		jq(document).on('change.neonatalTransferWizard', '#moh-neonatal-transfer-wizard-form input[name="modeOfTransport"]', syncTransportOther);

		jq(document).off('input.neonatalTransferWizard', '#neonatalBabyName');
		jq(document).on('input.neonatalTransferWizard', '#neonatalBabyName', function () {
			if (currentStep > 1) {
				updateWizardHeader(currentStep);
			}
		});

		jq(document).off('change.neonatalTransferWizard',
			'#neonatalResuscitationAtBirth, #neonatalHie, #neonatalImmunization, #neonatalInotropes, #neonatalImagingResultsAvailable, #neonatalLabFbc');
		jq(document).on('change.neonatalTransferWizard',
			'#neonatalResuscitationAtBirth, #neonatalHie, #neonatalImmunization, #neonatalInotropes, #neonatalImagingResultsAvailable, #neonatalLabFbc',
			syncNeonatalConditionalPanels);
	}

	function bindNavigation() {
		jq(document).off('click.neonatalTransferWizard', '#neonatal-wizard-next');
		jq(document).on('click.neonatalTransferWizard', '#neonatal-wizard-next', function (e) {
			e.preventDefault();
			if (!validateCurrentStep()) {
				return;
			}
			if (currentStep < TOTAL_STEPS) {
				showStep(currentStep + 1);
			}
		});

		jq(document).off('click.neonatalTransferWizard', '#neonatal-wizard-back');
		jq(document).on('click.neonatalTransferWizard', '#neonatal-wizard-back', function (e) {
			e.preventDefault();
			if (currentStep > 1) {
				showStep(currentStep - 1);
			}
		});
	}

	function getSaveUrl() {
		var path = (typeof openmrsContextPath !== 'undefined' ? openmrsContextPath : '');
		return path + '/module/transferapp/transfer/saveNeonatal.form';
	}

	function bindSubmit() {
		jq(document).off('click.neonatalTransferWizard', '#neonatal-wizard-submit-btn');
		jq(document).on('click.neonatalTransferWizard', '#neonatal-wizard-submit-btn', function (e) {
			e.preventDefault();
			var $form = getForm();
			if (!$form.length) {
				return;
			}
			if (!validateCurrentStep()) {
				return;
			}
			if (!$form[0].checkValidity()) {
				$form[0].reportValidity();
				return;
			}

			var $submitBtn = jq(this);
			$submitBtn.prop('disabled', true);
			var isEditing = $form.attr('data-editing') === 'true';

			jq.ajax({
				url: getSaveUrl(),
				type: 'POST',
				data: $form.serialize(),
				dataType: 'json'
			}).done(function (response) {
				if (response && response.status === 'success') {
					if (typeof emr !== 'undefined' && typeof emr.successMessage === 'function') {
						emr.successMessage(isEditing ? 'Neonatal transfer referral updated.' : 'Neonatal transfer referral saved.');
					}
					if (typeof window.closeNewNeonatalTransferOutDialog === 'function') {
						window.closeNewNeonatalTransferOutDialog();
					}
					if (response.uuid) {
						try {
							sessionStorage.setItem('transferapp.previewTransferUuid', response.uuid);
							sessionStorage.setItem('transferapp.previewFormType', 'Neonatal');
						} catch (ignoreStorage) {}
					}
					window.location.reload();
					return;
				}
				var message = response && response.message ? response.message : 'Unable to save neonatal transfer.';
				if (typeof emr !== 'undefined' && typeof emr.errorMessage === 'function') {
					emr.errorMessage(message);
				}
			}).fail(function () {
				if (typeof emr !== 'undefined' && typeof emr.errorMessage === 'function') {
					emr.errorMessage('Unable to save neonatal transfer.');
				}
			}).always(function () {
				$submitBtn.prop('disabled', false);
			});
		});
	}

	window.initNeonatalTransferWizardModal = function () {
		var $form = getForm();
		if (!$form.length) {
			return;
		}

		currentStep = 1;
		bindToggles();
		bindReceivingFacilityServices();
		bindNavigation();
		bindSubmit();
		syncReceivingFacilityIdField();
		initReceivingServiceSelect2();
		initDateTimePickers();
		syncTransportOther();
		syncNeonatalConditionalPanels();
		showStep(1);
		loadReceivingServicesForSelectedFacility();
	};

})(typeof jq !== 'undefined' ? jq : jQuery);
