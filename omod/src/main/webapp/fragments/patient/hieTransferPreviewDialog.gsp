<div id="hie-transfer-preview-dialog" class="dialog transfer-preview-dialog" style="display: none">
    <div class="dialog-header">
        <i class="icon-random"></i>
        <h3>${ ui.message("transferapp.patient.hieTransfer.previewTitle") }</h3>
    </div>
    <div class="dialog-content">
        <div id="hie-transfer-preview-body"></div>
            <div id="hie-transfer-feedback-wrap" class="hie-transfer-feedback-wrap" style="display:none;">
            <h4 class="hie-transfer-feedback-title">${ ui.message("transferapp.patient.hieTransfer.feedback.title") }</h4>
            <form id="hie-transfer-feedback-form" autocomplete="off">
                <div class="hie-feedback-section">
                    <h5>${ ui.message("transferapp.patient.hieTransfer.feedback.section.feedback") }</h5>
                    <div class="hie-feedback-grid">
                        <label>${ ui.message("transferapp.patient.hieTransfer.feedback.admissionDate") }
                            <input type="date" id="hie-fb-admission" readonly="readonly" tabindex="-1"/>
                        </label>
                        <label>${ ui.message("transferapp.patient.hieTransfer.feedback.dischargeDate") }
                            <input type="date" id="hie-fb-discharge" required="required"/>
                        </label>
                        <label class="hie-feedback-span-2">${ ui.message("transferapp.patient.hieTransfer.feedback.finalDiagnosis") }
                            <input type="text" id="hie-fb-diagnosis" required="required"/>
                        </label>
                        <label class="hie-feedback-span-2">${ ui.message("transferapp.patient.hieTransfer.feedback.treatmentGiven") }
                            <textarea id="hie-fb-treatment" rows="3" required="required"></textarea>
                        </label>
                        <fieldset class="hie-feedback-span-2">
                            <legend>${ ui.message("transferapp.patient.hieTransfer.feedback.outcome") }</legend>
                            <div id="hie-fb-outcome-options" class="hie-feedback-outcomes"></div>
                        </fieldset>
                    </div>
                </div>
                <div class="hie-feedback-section">
                    <h5>${ ui.message("transferapp.patient.hieTransfer.feedback.section.counter") }</h5>
                    <div class="hie-feedback-grid">
                        <label class="hie-feedback-span-2">${ ui.message("transferapp.patient.hieTransfer.feedback.recommendations") }
                            <textarea id="hie-fb-recommendations" rows="3" required="required"></textarea>
                        </label>
                        <label>${ ui.message("transferapp.patient.hieTransfer.feedback.referBack") }
                            <select id="hie-fb-refer-back" required="required"
                                    data-placeholder="${ ui.encodeHtmlAttribute(ui.message('transferapp.patient.hieTransfer.feedback.referBack.placeholder')) }">
                                <option value=""></option>
                            </select>
                        </label>
                        <label>${ ui.message("transferapp.patient.hieTransfer.feedback.contactPerson") }
                            <input type="text" id="hie-fb-contact" required="required"/>
                        </label>
                        <div class="hie-feedback-span-2 hie-feedback-row-3">
                            <label>${ ui.message("transferapp.patient.hieTransfer.feedback.providerName") }
                                <input type="text" id="hie-fb-provider" readonly="readonly" tabindex="-1" required="required"/>
                            </label>
                            <label>${ ui.message("transferapp.patient.hieTransfer.feedback.qualification") }
                                <input type="text" id="hie-fb-qualification" readonly="readonly" tabindex="-1" required="required"/>
                            </label>
                            <label>${ ui.message("transferapp.patient.hieTransfer.feedback.phone") }
                                <input type="text" id="hie-fb-phone" required="required"/>
                            </label>
                        </div>
                        <label>${ ui.message("transferapp.patient.hieTransfer.feedback.signedDate") }
                            <input type="date" id="hie-fb-signed-date" required="required"/>
                        </label>
                        <label>${ ui.message("transferapp.patient.hieTransfer.feedback.signedTime") }
                            <input type="time" id="hie-fb-signed-time" required="required"/>
                        </label>
                    </div>
                </div>
                <div class="hie-feedback-actions">
                    <button type="submit" id="hie-fb-save" class="confirm">
                        ${ ui.message("transferapp.patient.hieTransfer.feedback.save") }
                    </button>
                    <span id="hie-fb-status" class="hie-transfer-status" style="display:none;"></span>
                </div>
            </form>
            <p id="hie-fb-preview-hint" class="hie-fb-preview-hint" style="display:none;">
                ${ ui.message("transferapp.patient.hieTransfer.feedback.previewHint") }
            </p>
        </div>
        <div class="transfer-preview-actions">
            <button type="button" id="hie-transfer-validate-btn" class="confirm" style="display:none;">
                ${ ui.message("transferapp.patient.hieTransfer.validateTransfer") }
            </button>
            <button type="button" id="hie-transfer-export-pdf-btn" class="confirm" style="display:none;">
                ${ ui.message("transferapp.patient.hieTransfer.exportPdf") }
            </button>
            <button type="button" id="hie-transfer-provide-feedback-btn" class="confirm" style="display:none;"
                    data-mode="provide"
                    data-label-provide="${ ui.encodeHtmlAttribute(ui.message('transferapp.patient.hieTransfer.feedback.provide')) }"
                    data-label-send="${ ui.encodeHtmlAttribute(ui.message('transferapp.patient.hieTransfer.feedback.send')) }"
                    data-label-sending="${ ui.encodeHtmlAttribute(ui.message('transferapp.patient.hieTransfer.feedback.sending')) }">
                ${ ui.message("transferapp.patient.hieTransfer.feedback.provide") }
            </button>
            <span id="hie-transfer-validate-status" class="hie-transfer-status" style="display:none;"></span>
            <button type="button" id="hie-transfer-preview-close" class="cancel">
                ${ ui.message("coreapps.close") }
            </button>
        </div>
    </div>
</div>

<style>
#hie-transfer-preview-dialog .transfer-preview-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 4px;
  flex-wrap: wrap;
}
#hie-transfer-validate-btn.confirm,
#hie-transfer-export-pdf-btn.confirm,
#hie-transfer-provide-feedback-btn.confirm {
  background: #0f766e;
  color: #fff;
  border: 1px solid #0d9488;
  padding: 8px 14px;
  border-radius: 4px;
  font-weight: 600;
  cursor: pointer;
}
#hie-transfer-export-pdf-btn.confirm {
  background: #1d4ed8;
  border-color: #1e40af;
}
#hie-transfer-provide-feedback-btn.confirm.hie-fb-mode-send {
  background: #1d4ed8;
  border-color: #1e40af;
}
#hie-transfer-validate-btn.confirm:disabled,
#hie-transfer-export-pdf-btn.confirm:disabled,
#hie-transfer-provide-feedback-btn.confirm:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
#hie-transfer-preview-dialog.transfer-preview-dialog.dialog {
  position: fixed;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  z-index: 10001;
  background: white;
  border: 1px solid #00473f;
  border-radius: 4px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.3);
  max-width: 95%;
  max-height: 92vh;
  width: 1400px;
  display: flex;
  flex-direction: column;
  margin: 0;
}
#hie-transfer-preview-dialog .dialog-header {
  padding: 15px 20px;
  border-bottom: 1px solid #00473f;
  background: #00473f;
  border-radius: 4px 4px 0 0;
  flex-shrink: 0;
  color: #fff;
}
#hie-transfer-preview-dialog .dialog-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: bold;
  color: #fff;
}
#hie-transfer-preview-dialog .dialog-content {
  padding: 20px;
  overflow-y: auto;
  overflow-x: hidden;
  flex: 1;
  min-height: 0;
}
#hie-transfer-preview-body {
  max-height: 70vh;
  overflow-y: auto;
  overflow-x: hidden;
  margin-bottom: 15px;
}
.hie-transfer-feedback-wrap {
  margin-top: 16px;
  padding-top: 14px;
  border-top: 1px solid #cbd5e1;
}
.hie-transfer-feedback-title {
  margin: 0 0 12px;
  font-size: 16px;
  color: #00473f;
}
.hie-feedback-section {
  margin-bottom: 16px;
}
.hie-feedback-section h5 {
  margin: 0 0 10px;
  font-size: 14px;
  color: #0f766e;
}
.hie-feedback-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px 14px;
}
.hie-feedback-grid label,
.hie-feedback-grid fieldset {
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-weight: 600;
  color: #2c3e66;
  font-size: 13px;
  margin: 0;
  border: 0;
  padding: 0;
}
.hie-feedback-grid .select2-container {
  width: 100% !important;
}
.hie-feedback-grid .select2-container .select2-selection--single {
  height: 34px;
  border: 1px solid #ccc;
  border-radius: 3px;
}
.hie-feedback-grid .select2-container .select2-selection__rendered {
  line-height: 32px;
  padding-left: 8px;
}
.hie-feedback-grid .select2-container .select2-selection__arrow {
  height: 32px;
}
.select2-container--open {
  z-index: 10050 !important;
}
#hie-fb-admission[readonly],
#hie-fb-provider[readonly],
#hie-fb-qualification[readonly] {
  background: #f3f4f6;
  color: #374151;
  cursor: not-allowed;
}
.hie-feedback-span-2 {
  grid-column: 1 / -1;
  width: 100%;
  min-width: 0;
}
.hie-feedback-row-3 {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(0, 1fr) minmax(0, 0.9fr);
  gap: 10px 14px;
}
.hie-feedback-grid fieldset.hie-feedback-span-2 {
  width: 100%;
}
.hie-feedback-grid input[type="radio"] {
  width: auto;
  flex: 0 0 auto;
  margin: 0;
}
.hie-feedback-outcomes {
  display: flex;
  flex-wrap: nowrap;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  gap: 8px 12px;
  font-weight: 400;
}
.hie-feedback-outcomes label {
  flex: 1 1 0;
  flex-direction: row;
  align-items: center;
  white-space: nowrap;
  gap: 6px;
  font-weight: 500;
  min-width: 0;
}
.hie-feedback-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  margin-top: 8px;
}
#hie-fb-save.confirm {
  background: #0f766e;
  color: #fff;
  border: 1px solid #0d9488;
  padding: 8px 14px;
  border-radius: 4px;
  font-weight: 600;
  cursor: pointer;
}
#hie-fb-save.confirm:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.hie-fb-preview-hint {
  margin: 10px 0 0;
  color: #475569;
  font-size: 13px;
}
#hie-transfer-preview-dialog.has-feedback #hie-transfer-preview-body {
  max-height: 38vh;
}
</style>
