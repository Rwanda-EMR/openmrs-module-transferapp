(function(global) {
	"use strict";

	var jq = global.jq || global.jQuery;
	var assetsLoading = null;

	function resolveOpenmrsPath() {
		if (typeof resolveOpenmrsContextPath === "function") {
			return resolveOpenmrsContextPath();
		}
		var path = global.transferOpenmrsPath;
		if (path === undefined || path === null || String(path).trim() === "") {
			path = (typeof openmrsContextPath !== "undefined") ? openmrsContextPath : "";
		}
		if (path === undefined || path === null || String(path).trim() === "") {
			path = "/openmrs";
		}
		path = String(path).trim();
		if (path.charAt(0) !== "/") {
			path = "/" + path;
		}
		while (path.length > 1 && path.charAt(path.length - 1) === "/") {
			path = path.substring(0, path.length - 1);
		}
		if (path === "/" || path === "") {
			return "";
		}
		return path;
	}

	function scriptsBase() {
		var base = global.transferPreviewResourcesBase;
		if (!base || String(base).trim() === "") {
			base = resolveOpenmrsPath() + "/moduleResources/transferapp/scripts/";
		}
		base = String(base).trim();
		if (base.charAt(base.length - 1) !== "/") {
			base += "/";
		}
		return base;
	}

	function ensurePreviewStyles() {
		if (document.getElementById("transfer-form-preview-css")) {
			return;
		}
		var cssHref = resolveOpenmrsPath() + "/moduleResources/transferapp/styles/transferFormPreview.css";
		if (!jq) {
			return;
		}
		jq("head").append(
			"<link id='transfer-form-preview-css' rel='stylesheet' type='text/css' href='" + cssHref + "' />"
		);
	}

	function previewAssetsReady() {
		return typeof global.buildTransferFormPreviewHtml === "function"
			&& !!global.transferMohLogoDataUri;
	}

	/**
	 * Loads MOH logo + transferFormPreview.js once, then invokes callback.
	 */
	function ensureTransferPreviewAssets(callback) {
		if (typeof callback !== "function") {
			return;
		}
		ensurePreviewStyles();
		if (previewAssetsReady()) {
			callback();
			return;
		}
		if (!jq) {
			callback();
			return;
		}
		if (assetsLoading) {
			assetsLoading.done(callback);
			return;
		}
		var base = scriptsBase();
		assetsLoading = jq.getScript(base + "transferMohLogo.js")
			.then(function() {
				return jq.getScript(base + "transferFormPreview.js");
			})
			.done(function() {
				callback();
			})
			.fail(function() {
				callback();
			});
	}

	/**
	 * Renders the standard MOH transfer form preview (with QR when a transfer UUID is available).
	 *
	 * @param {string|jQuery} target preview body selector or element
	 * @param {Object} transfer raw transfer map from API / HIE
	 * @param {Function} [afterRender] optional callback receiving normalized preview data
	 */
	function renderTransferPreviewInto(target, transfer, afterRender) {
		ensureTransferPreviewAssets(function() {
			if (!jq) {
				return;
			}
			var $target = typeof target === "string" ? jq(target) : jq(target);
			if (!$target.length) {
				return;
			}
			if (typeof global.buildTransferFormPreviewHtml !== "function") {
				$target.html("<p style='color:red;'>Preview renderer not loaded.</p>");
				return;
			}
			var normalized = typeof global.enrichTransferPreviewData === "function"
				? global.enrichTransferPreviewData(transfer)
				: transfer;
			$target.html(global.buildTransferFormPreviewHtml(normalized));
			if (typeof afterRender === "function") {
				afterRender(normalized, transfer);
			}
		});
	}

	global.ensureTransferPreviewAssets = ensureTransferPreviewAssets;
	global.renderTransferPreviewInto = renderTransferPreviewInto;
})(window);
