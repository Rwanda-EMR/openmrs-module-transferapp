(function (jq) {
	'use strict';

	var TOTAL_STEPS = 5;
	var currentStep = 1;

	var STEP_META = [
		{
			shortName: 'Client & Referral',
			lead: 'Step 1 — client identification, demographics, and referral.'
		},
		{
			shortName: 'Obstetric History',
			leadWithClient: 'Step 2 — obstetric history and current pregnancy for'
		},
		{
			shortName: 'Clinical Findings',
			leadWithClient: 'Step 3 — vitals, exam findings, and investigations for'
		},
		{
			shortName: 'Treatment & Transport',
			leadWithClient: 'Step 4 — treatment given, transportation, and insurance for'
		},
		{
			shortName: 'Sign-off',
			leadWithClient: 'Step 5 — referring provider sign-off for'
		}
	];

	function getForm() {
		return jq('#moh-maternity-transfer-wizard-form');
	}

	function getClientName() {
		var $form = getForm();
		var fromField = jq.trim(jq('#maternityClientName').val() || '');
		if (fromField) {
			return fromField;
		}
		return jq.trim($form.attr('data-client-name') || '');
	}

	function getPanels() {
		return getForm().find('.transfer-wizard-step-panel');
	}

	function getProgressItems() {
		return jq('.transfer-wizard-progress-5 li');
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

		jq('#maternity-wizard-page-title').text('Maternity transfer — ' + meta.shortName);
		jq('#maternity-wizard-page-lead').html(buildPageLead(step));

		var modalTitle = jq('#new-maternity-transfer-out-title');
		if (modalTitle.length) {
			modalTitle.text('Maternity transfer — ' + meta.shortName);
		}
	}

	function updateActionButtons(step) {
		var $next = jq('#maternity-wizard-next');
		var $back = jq('#maternity-wizard-back');
		var $submit = jq('#maternity-wizard-submit-btn');
		var $cancel = jq('#maternity-wizard-cancel');

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

		var scrollContainer = jq('#new-maternity-transfer-out-data .transfer-wizard-scroll');
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

	function syncEmergencyPanel() {
		var isEmergency = jq('#moh-maternity-transfer-wizard-form input[name="transferType"]:checked').val() === 'EMERGENCY';
		var $panel = jq('#maternityEmergencyFields');
		$panel.toggleClass('is-visible', isEmergency);
		$panel.find('[data-emergency-required="true"]').prop('required', isEmergency);
		if (!isEmergency) {
			$panel.find('[data-emergency-required="true"]').val('');
		}
		syncTransportByTransferType();
	}

	function syncTransportByTransferType() {
		var transferType = jq('#moh-maternity-transfer-wizard-form input[name="transferType"]:checked').val();
		var isEmergency = transferType === 'EMERGENCY';
		var $ambulance = jq('#moh-maternity-transfer-wizard-form input[name="transportationType"][value="AMBULANCE"]');
		var $other = jq('#moh-maternity-transfer-wizard-form input[name="transportationType"][value="OTHER"]');
		var $na = jq('#moh-maternity-transfer-wizard-form input[name="transportationType"][value="NA"]');
		var $enabledTransport = jq('#moh-maternity-transfer-wizard-form input[name="transportationType"]:not(:disabled)');

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
		var isOther = jq('#moh-maternity-transfer-wizard-form input[name="transportationType"]:checked').val() === 'OTHER';
		var $panel = jq('#maternityTransportOtherField');
		var $input = jq('#maternityTransportationOtherSpec');
		$panel.toggleClass('is-visible', isOther);
		$input.prop('required', isOther);
		if (!isOther) {
			$input.val('');
		}
	}

	function syncInsuranceOther() {
		var isOther = jq('#moh-maternity-transfer-wizard-form input[name="healthInsuranceType"]:checked').val() === 'OTHER';
		var $panel = jq('#maternityInsuranceOtherField');
		var $input = jq('#maternityHealthInsuranceOtherSpec');
		$panel.toggleClass('is-visible', isOther);
		$input.prop('required', isOther);
		if (!isOther) {
			$input.val('');
		}
	}

	function getSelectedReceivingFacilityId() {
		return jq.trim(jq('#maternityReceivingFacilityCode option:selected').attr('data-receiving-facility-id') || '');
	}

	function syncReceivingFacilityIdField() {
		jq('#maternityReceivingFacilityId').val(getSelectedReceivingFacilityId());
	}

	function getWizardJq() {
		return (typeof jq !== 'undefined') ? jq : jQuery;
	}

	function destroyReceivingServiceSelect2() {
		var $ = getWizardJq();
		var $select = $('#maternityReceivingService');
		if ($select.length && $select.hasClass('select2-hidden-accessible') && typeof $.fn.select2 === 'function') {
			$select.select2('destroy');
		}
	}

	function initReceivingServiceSelect2() {
		var $ = getWizardJq();
		if (typeof $.fn.select2 !== 'function') {
			return;
		}
		var $select = $('#maternityReceivingService');
		if (!$select.length) {
			return;
		}
		var $dropdownParent = $('#new-maternity-transfer-out-dialog');
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
		var $select = jq('#maternityReceivingService');
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
		jq(document).off('change.maternityTransferWizard', '#maternityReceivingFacilityCode');
		jq(document).on('change.maternityTransferWizard', '#maternityReceivingFacilityCode', function () {
			syncReceivingFacilityIdField();
			loadReceivingServicesForSelectedFacility();
		});
	}

	function zeroPad(value) {
		return value < 10 ? '0' + value : String(value);
	}

	function formatDateYmd(date) {
		return date.getFullYear() + '-' + zeroPad(date.getMonth() + 1) + '-' + zeroPad(date.getDate());
	}

	function calculateEddAndGestationFromLmp(lmpDate) {
		if (!lmpDate) {
			return;
		}

		var lmpMidnight = new Date(lmpDate.getFullYear(), lmpDate.getMonth(), lmpDate.getDate());

		var eddDate = new Date(lmpMidnight.getTime());
		eddDate.setDate(eddDate.getDate() + 280);
		var eddEl = document.getElementById('eddDate');
		if (eddEl && eddEl._flatpickr) {
			eddEl._flatpickr.setDate(eddDate, true);
		} else if (eddEl) {
			eddEl.value = formatDateYmd(eddDate);
		}

		var today = new Date();
		var todayMidnight = new Date(today.getFullYear(), today.getMonth(), today.getDate());
		var diffDays = Math.round((todayMidnight - lmpMidnight) / (1000 * 60 * 60 * 24));
		if (diffDays < 0) {
			diffDays = 0;
		}
		var weeks = Math.floor(diffDays / 7);
		var days = diffDays % 7;

		var gestationEl = document.getElementById('gestationAge');
		if (gestationEl) {
			gestationEl.value = days > 0 ? (weeks + ' weeks ' + days + ' days') : (weeks + ' weeks');
		}
	}

	function destroyDateTimePickers() {
		if (typeof flatpickr !== 'function') {
			return;
		}
		jq('#moh-maternity-transfer-wizard-form .js-datetime-picker, #moh-maternity-transfer-wizard-form .js-time-picker, #moh-maternity-transfer-wizard-form .js-date-picker')
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
		document.querySelectorAll('#moh-maternity-transfer-wizard-form .js-datetime-picker').forEach(function (el) {
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
		document.querySelectorAll('#moh-maternity-transfer-wizard-form .js-time-picker').forEach(function (el) {
			flatpickr(el, {
				enableTime: true,
				noCalendar: true,
				dateFormat: 'H:i',
				time_24hr: true,
				allowInput: true,
				disableMobile: true
			});
		});
		document.querySelectorAll('#moh-maternity-transfer-wizard-form .js-date-picker').forEach(function (el) {
			var options = {
				enableTime: false,
				dateFormat: 'Y-m-d',
				altInput: true,
				altFormat: 'd/m/Y',
				allowInput: true,
				disableMobile: true
			};
			if (el.id === 'lmpDate') {
				options.onChange = function (selectedDates) {
					calculateEddAndGestationFromLmp(selectedDates[0]);
				};
			}
			flatpickr(el, options);
		});
	}

	function nextTreatmentRowIndex() {
		return jq('#maternity-treatment-table-body tr.maternity-treatment-row').length;
	}

	function addTreatmentRow() {
		var $tbody = jq('#maternity-treatment-table-body');
		if (!$tbody.length) {
			return;
		}
		var cellStyle = "padding:0.3rem;border-bottom:1px solid #eef2f6;";
		var treatmentOptions = ["IV Fluids", "Dexamethasone", "Magnesium sulphate", "Nifedipine", "Oxytocin", "ATBs"];
		var treatmentSelectHtml = '<select name="treatmentName"><option value="">Select treatment</option>';
		treatmentOptions.forEach(function (option) {
			treatmentSelectHtml += '<option value="' + option + '">' + option + '</option>';
		});
		treatmentSelectHtml += '</select>';
		var $row = jq(
			'<tr class="maternity-treatment-row">'
			+ '<td style="' + cellStyle + '">' + treatmentSelectHtml + '</td>'
			+ '<td style="' + cellStyle + '"><input type="text" name="treatmentDose" value="" /></td>'
			+ '<td style="' + cellStyle + '"><input type="text" class="js-date-picker" name="treatmentGivenDate" value="" placeholder="Date" autocomplete="off" /></td>'
			+ '<td style="' + cellStyle + '"><input type="text" class="js-time-picker" name="treatmentGivenTime" value="" placeholder="Time" autocomplete="off" /></td>'
			+ '<td style="' + cellStyle + '"><button type="button" class="transfer-wizard-btn transfer-wizard-btn-outline js-remove-treatment-row">Remove</button></td>'
			+ '</tr>'
		);
		$tbody.append($row);
		if (typeof flatpickr === 'function') {
			$row.find('.js-date-picker').each(function () {
				flatpickr(this, { enableTime: false, dateFormat: 'Y-m-d', altInput: true, altFormat: 'd/m/Y', allowInput: true, disableMobile: true });
			});
			$row.find('.js-time-picker').each(function () {
				flatpickr(this, { enableTime: true, noCalendar: true, dateFormat: 'H:i', time_24hr: true, allowInput: true, disableMobile: true });
			});
		}
	}

	function bindTreatmentTable() {
		jq(document).off('click.maternityTransferWizard', '#maternity-add-treatment-row');
		jq(document).on('click.maternityTransferWizard', '#maternity-add-treatment-row', function (e) {
			e.preventDefault();
			addTreatmentRow();
		});

		jq(document).off('click.maternityTransferWizard', '.js-remove-treatment-row');
		jq(document).on('click.maternityTransferWizard', '.js-remove-treatment-row', function (e) {
			e.preventDefault();
			jq(this).closest('tr.maternity-treatment-row').remove();
		});
	}

	function bindToggles() {
		jq(document).off('change.maternityTransferWizard', '#moh-maternity-transfer-wizard-form input[name="transferType"]');
		jq(document).on('change.maternityTransferWizard', '#moh-maternity-transfer-wizard-form input[name="transferType"]', syncEmergencyPanel);

		jq(document).off('change.maternityTransferWizard', '#moh-maternity-transfer-wizard-form input[name="transportationType"]');
		jq(document).on('change.maternityTransferWizard', '#moh-maternity-transfer-wizard-form input[name="transportationType"]', syncTransportOther);

		jq(document).off('change.maternityTransferWizard', '#moh-maternity-transfer-wizard-form input[name="healthInsuranceType"]');
		jq(document).on('change.maternityTransferWizard', '#moh-maternity-transfer-wizard-form input[name="healthInsuranceType"]', syncInsuranceOther);

		jq(document).off('input.maternityTransferWizard', '#maternityClientName');
		jq(document).on('input.maternityTransferWizard', '#maternityClientName', function () {
			if (currentStep > 1) {
				updateWizardHeader(currentStep);
			}
		});
	}

	function bindNavigation() {
		jq(document).off('click.maternityTransferWizard', '#maternity-wizard-next');
		jq(document).on('click.maternityTransferWizard', '#maternity-wizard-next', function (e) {
			e.preventDefault();
			if (!validateCurrentStep()) {
				return;
			}
			if (currentStep < TOTAL_STEPS) {
				showStep(currentStep + 1);
			}
		});

		jq(document).off('click.maternityTransferWizard', '#maternity-wizard-back');
		jq(document).on('click.maternityTransferWizard', '#maternity-wizard-back', function (e) {
			e.preventDefault();
			if (currentStep > 1) {
				showStep(currentStep - 1);
			}
		});
	}

	function getSaveUrl() {
		var path = (typeof openmrsContextPath !== 'undefined' ? openmrsContextPath : '');
		return path + '/module/transferapp/transfer/saveMaternity.form';
	}

	function bindSubmit() {
		jq(document).off('click.maternityTransferWizard', '#maternity-wizard-submit-btn');
		jq(document).on('click.maternityTransferWizard', '#maternity-wizard-submit-btn', function (e) {
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
						emr.successMessage(isEditing ? 'Maternity transfer referral updated.' : 'Maternity transfer referral saved.');
					}
					if (typeof window.closeNewMaternityTransferOutDialog === 'function') {
						window.closeNewMaternityTransferOutDialog();
					}
					if (response.uuid) {
						try {
							sessionStorage.setItem('transferapp.previewTransferUuid', response.uuid);
							sessionStorage.setItem('transferapp.previewFormType', 'Maternity');
						} catch (ignoreStorage) {}
					}
					window.location.reload();
					return;
				}
				var message = response && response.message ? response.message : 'Unable to save maternity transfer.';
				if (typeof emr !== 'undefined' && typeof emr.errorMessage === 'function') {
					emr.errorMessage(message);
				}
			}).fail(function () {
				if (typeof emr !== 'undefined' && typeof emr.errorMessage === 'function') {
					emr.errorMessage('Unable to save maternity transfer.');
				}
			}).always(function () {
				$submitBtn.prop('disabled', false);
			});
		});
	}

	window.initMaternityTransferWizardModal = function () {
		var $form = getForm();
		if (!$form.length) {
			return;
		}

		currentStep = 1;
		bindToggles();
		bindReceivingFacilityServices();
		bindNavigation();
		bindTreatmentTable();
		bindSubmit();
		syncReceivingFacilityIdField();
		initReceivingServiceSelect2();
		initDateTimePickers();
		syncEmergencyPanel();
		syncTransportByTransferType();
		syncTransportOther();
		syncInsuranceOther();
		showStep(1);
		loadReceivingServicesForSelectedFacility();
	};

})(typeof jq !== 'undefined' ? jq : jQuery);
