/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package org.openmrs.module.transferapp.pdf;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Map;

/**
 * Controlled server-side PDF renderer matching the MoH External Transfer Form
 * (Facility_transfer_forms_2020). Labels stay regular weight; filled data is bold.
 * Empty values stay blank — no dashed/dotted fill lines.
 */
public class TransferFormPdfRenderer {

	private static final Log log = LogFactory.getLog(TransferFormPdfRenderer.class);

	private static final String MOH_LOGO_RESOURCE = "/pdf/rwanda-moh-logo.png";
	private static final float MOH_LOGO_MAX_WIDTH = 72f;
	private static final float MOH_LOGO_MAX_HEIGHT = 78f;

	private static final Font BRAND = FontFactory.getFont(FontFactory.TIMES_BOLD, 11);
	private static final Font TITLE = FontFactory.getFont(FontFactory.TIMES_BOLD, 13);
	private static final Font LABEL = FontFactory.getFont(FontFactory.TIMES_ROMAN, 8.5f);
	private static final Font VALUE = FontFactory.getFont(FontFactory.TIMES_BOLD, 9);
	private static final Font HEADER_LABEL = FontFactory.getFont(FontFactory.TIMES_BOLD, 9);
	private static final Font HEADER_VALUE = FontFactory.getFont(FontFactory.TIMES_BOLD, 9);

	private static volatile byte[] cachedMohLogoBytes;

	public byte[] render(Map<String, Object> transfer, Map<String, Object> feedback) throws DocumentException {
		return render(transfer, feedback, null);
	}

	public byte[] render(Map<String, Object> transfer, Map<String, Object> feedback, byte[] qrPng)
			throws DocumentException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		Document document = new Document(PageSize.A4, 28, 28, 28, 28);
		PdfWriter writer = PdfWriter.getInstance(document, out);
		document.open();

		PdfPTable outer = new PdfPTable(1);
		outer.setWidthPercentage(100);
		PdfPCell border = new PdfPCell();
		border.setBorder(Rectangle.BOX);
		border.setBorderWidth(1.8f);
		border.setPadding(8f);
		border.setPaddingTop(6f);

		border.addElement(buildHeader(transfer));

		Paragraph title = new Paragraph(coalesce(first(transfer, "formTitle"), "EXTERNAL TRANSFER FORM"), TITLE);
		title.setAlignment(Element.ALIGN_CENTER);
		title.setSpacingBefore(4f);
		title.setSpacingAfter(6f);
		border.addElement(title);

		border.addElement(spacedRow(
				pair("Client Name:", first(transfer, "clientName", "patientName")),
				pair("Serial number in register/EMR ID:",
						first(transfer, "serialNumberOrEmrId", "serialNumberEmr", "upid", "subject"))));
		border.addElement(spacedRow(
				pair("Age(DOB):", first(transfer, "ageDob", "ageOrDob")),
				pair("Sex:", text(transfer, "sex")),
				pair("Name of caregiver:", text(transfer, "caregiverName")),
				pair("Telephone:", first(transfer, "caregiverTelephone", "clientTelephone", "patientPhone",
						"telephone"))));
		border.addElement(spacedRow(
				pair("District:", first(transfer, "patientDistrict", "clientDistrict")),
				pair("Sector:", first(transfer, "patientSector", "sector")),
				pair("Cell:", first(transfer, "patientCell", "cell")),
				pair("Village:", first(transfer, "patientVillage", "village"))));
		border.addElement(spacedRow(
				pair("Date and time of Admission:", first(transfer, "admissionDatetime", "admissionAt")),
				pair("Date and Time of decision to transfer:",
						first(transfer, "transferDecisionDatetime", "decisionToTransferAt", "periodStart"))));
		border.addElement(spacedRow(
				pair("Receiving Facility:", first(transfer, "receivingFacility", "destination", "destinationDisplay")),
				pair("Receiving Service:", text(transfer, "receivingService")),
				pair("Calling Time:", text(transfer, "callingTime"))));
		border.addElement(spacedRow(
				pair("Staff contacted at receiving facility:",
						first(transfer, "staffContactedAtReceivingFacility", "staffContactedName")),
				pair("Phone:", first(transfer, "staffContactPhone", "receivingClinicianPhone",
						"staffContactedPhone"))));

		border.addElement(spacedRow(
				labelOnly("Type of transfer:"),
				choice("Emergency", flag(transfer, "isEmergency")),
				choice("Not- Emergency", flag(transfer, "isNonEmergency")),
				choice("Follow up", flag(transfer, "isFollowUp"))));
		border.addElement(spacedRow(
				pair("If emergency: Time ambulance called:", text(transfer, "ambulanceCalledTime")),
				pair("Time of departure from referring facility:",
						first(transfer, "departureTime", "departureFromReferringTime"))));
		border.addElement(block("Reason for Transfer:", text(transfer, "reasonForTransfer")));

		border.addElement(sectionTitle("Significant Findings:"));
		border.addElement(block("Clinical Presentation:",
				first(transfer, "clinicalPresentation", "significantFindings")));
		border.addElement(block("If person with disability, record the type of disability:",
				text(transfer, "disabilityType")));

		border.addElement(vitalsRow(transfer));

		border.addElement(block("Laboratory:", text(transfer, "laboratory")));
		border.addElement(block("Others:", first(transfer, "others", "othersNotes")));
		border.addElement(block("Diagnosis:", text(transfer, "diagnosis")));
		border.addElement(block("Procedures and Treatments:", text(transfer, "proceduresAndTreatments")));

		boolean naTransport = flag(transfer, "isNaTransport")
				|| "NA".equalsIgnoreCase(text(transfer, "transportType"));
		String otherTransport = first(transfer, "otherTransportType", "transportationOtherSpec");
		if (naTransport && "NA".equalsIgnoreCase(otherTransport)) {
			otherTransport = "";
		}
		border.addElement(spacedRow(
				labelOnly("Type of Transportation:"),
				choice("Ambulance", flag(transfer, "isAmbulanceTransport")
						|| "AMBULANCE".equalsIgnoreCase(text(transfer, "transportType"))),
				pair("Other (specify):", otherTransport),
				choice("NA", naTransport)));

		boolean noInsurance = flag(transfer, "isNoInsurance");
		String otherInsurance = text(transfer, "otherInsurance");
		if (noInsurance && "None".equalsIgnoreCase(otherInsurance)) {
			otherInsurance = "";
		}
		border.addElement(spacedRow(
				labelOnly("Health insurance:"),
				choice("CBHI (mutuelle)", flag(transfer, "isCbhiInsurance")),
				choice("RSSB", flag(transfer, "isRssbInsurance")),
				choice("MMI", flag(transfer, "isMmiInsurance")),
				pair("Other (Specify):", otherInsurance),
				choice("None", noInsurance)));

		boolean withFeedback = hasFeedback(feedback);
		if (withFeedback) {
			border.addElement(spacedRow(new float[] { 1.7f, 1f },
					pair("Names of referring health care provider:", text(transfer, "referringProviderName")),
					pair("Qualification:",
							first(transfer, "referringProviderQualification", "providerSpecialty"))));
			border.addElement(spacedRow(
					pair("Date:", first(transfer, "formDate", "referringSignedDate")),
					pair("Time:", first(transfer, "formTime", "referringSignedTime")),
					pair("Phone:", first(transfer, "providerPhone", "referringProviderPhone"))));

			border.addElement(sectionTitle("REFERRAL FEEDBACK"));
			border.addElement(spacedRow(
					pair("Client name:", coalesce(text(feedback, "clientName"), text(transfer, "clientName"))),
					pair("Sex:", coalesce(text(feedback, "sex"), text(transfer, "sex"))),
					pair("Age (DOB):", coalesce(text(feedback, "ageOrDob"),
							first(transfer, "ageDob", "ageOrDob")))));
			border.addElement(spacedRow(
					pair("Date of admission or client seen at receiving facility:",
							text(feedback, "dateOfAdmissionOrSeen")),
					pair("Date of Discharge:", text(feedback, "dateOfDischarge"))));
			border.addElement(block("Final Diagnosis:", text(feedback, "finalDiagnosis")));
			border.addElement(block("Treatment at the receiving facility:", text(feedback, "treatmentGiven")));
			border.addElement(outcomeRow(text(feedback, "outcome"), text(feedback, "outcomeLabel")));

			border.addElement(sectionTitle("COUNTER-REFERRAL"));
			border.addElement(block("Recommendations (follow up care):", text(feedback, "recommendations")));
			border.addElement(spacedRow(
					pair("Refer back to: Name of facility:", text(feedback, "referBackToFacility")),
					pair("Contact person:", text(feedback, "contactPerson"))));
			border.addElement(bottomWithQr(
					text(feedback, "providerName"),
					text(feedback, "qualification"),
					text(feedback, "signedDate"),
					text(feedback, "signedTime"),
					text(feedback, "phone"),
					qrPng));
		} else {
			border.addElement(bottomWithQr(
					text(transfer, "referringProviderName"),
					first(transfer, "referringProviderQualification", "providerSpecialty"),
					first(transfer, "formDate", "referringSignedDate"),
					first(transfer, "formTime", "referringSignedTime"),
					first(transfer, "providerPhone", "referringProviderPhone"),
					qrPng));
		}

		outer.addCell(border);
		document.add(outer);

		// Inner double-border stroke like the paper form
		drawInnerBorder(writer, document);

		document.close();
		return out.toByteArray();
	}

	private void drawInnerBorder(PdfWriter writer, Document document) {
		PdfContentByte canvas = writer.getDirectContent();
		Rectangle page = document.getPageSize();
		float inset = 22f;
		canvas.saveState();
		canvas.setColorStroke(BaseColor.BLACK);
		canvas.setLineWidth(0.6f);
		canvas.rectangle(page.getLeft() + inset, page.getBottom() + inset,
				page.getWidth() - (inset * 2), page.getHeight() - (inset * 2));
		canvas.stroke();
		canvas.restoreState();
	}

	/**
	 * Preview-style bottom table: stacked provider + date/time/phone on the left,
	 * QR code on the right so the QR does not stretch vertical gaps between text rows.
	 */
	private PdfPTable bottomWithQr(String providerName, String qualification, String date, String time,
			String phone, byte[] qrPng) throws DocumentException {
		PdfPTable leftFields = new PdfPTable(1);
		leftFields.setWidthPercentage(100);
		leftFields.setSpacingBefore(0f);
		leftFields.setSpacingAfter(0f);

		PdfPCell providerCell = new PdfPCell();
		providerCell.setBorder(Rectangle.NO_BORDER);
		providerCell.setPadding(0f);
		providerCell.setPaddingBottom(3f);
		providerCell.addElement(spacedRow(new float[] { 1.7f, 1f },
				pair("Names of health care provider:", providerName),
				pair("Qualification:", qualification)));
		leftFields.addCell(providerCell);

		PdfPCell dateCell = new PdfPCell();
		dateCell.setBorder(Rectangle.NO_BORDER);
		dateCell.setPadding(0f);
		dateCell.addElement(spacedRow(new float[] { 1f, 0.9f, 1.3f },
				pair("Date:", date),
				pair("Time:", time),
				pair("Phone:", phone)));
		leftFields.addCell(dateCell);

		Image qrImage = loadQrImage(qrPng);
		if (qrImage == null) {
			return leftFields;
		}

		PdfPTable wrap = new PdfPTable(2);
		wrap.setWidthPercentage(100);
		wrap.setWidths(new float[] { 3.4f, 1f });
		wrap.setSpacingBefore(2f);
		wrap.setSpacingAfter(1f);

		PdfPCell fieldsCell = new PdfPCell();
		fieldsCell.setBorder(Rectangle.NO_BORDER);
		fieldsCell.setVerticalAlignment(Element.ALIGN_TOP);
		fieldsCell.setPadding(0f);
		fieldsCell.setPaddingRight(6f);
		fieldsCell.addElement(leftFields);

		PdfPCell qrCell = new PdfPCell();
		qrCell.setBorder(Rectangle.BOX);
		qrCell.setBorderWidth(1f);
		qrCell.setPadding(1.5f);
		qrCell.setHorizontalAlignment(Element.ALIGN_CENTER);
		qrCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
		qrImage.setAlignment(Image.ALIGN_CENTER);
		qrCell.addElement(qrImage);

		wrap.addCell(fieldsCell);
		wrap.addCell(qrCell);
		return wrap;
	}

	private static Image loadQrImage(byte[] qrPng) {
		if (qrPng == null || qrPng.length == 0) {
			return null;
		}
		try {
			Image image = Image.getInstance(qrPng);
			image.scaleToFit(110f, 110f);
			return image;
		}
		catch (Exception ex) {
			log.warn("Unable to embed verification QR in transfer PDF", ex);
			return null;
		}
	}

	private PdfPTable buildHeader(Map<String, Object> transfer) throws DocumentException {
		PdfPTable table = new PdfPTable(2);
		table.setWidthPercentage(100);
		table.setWidths(new float[] { 1f, 1.15f });
		table.setSpacingAfter(4f);

		PdfPTable brand = new PdfPTable(1);
		brand.setWidthPercentage(100);

		Paragraph republic = new Paragraph("REPUBLIC OF RWANDA", BRAND);
		republic.setAlignment(Element.ALIGN_CENTER);
		PdfPCell republicCell = new PdfPCell(republic);
		republicCell.setBorder(Rectangle.NO_BORDER);
		republicCell.setHorizontalAlignment(Element.ALIGN_CENTER);
		republicCell.setPaddingBottom(2f);
		brand.addCell(republicCell);

		PdfPCell logoCell = new PdfPCell();
		logoCell.setBorder(Rectangle.NO_BORDER);
		logoCell.setHorizontalAlignment(Element.ALIGN_CENTER);
		logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
		logoCell.setPaddingTop(2f);
		logoCell.setPaddingBottom(2f);
		Image logo = loadMohLogo();
		if (logo != null) {
			logo.setAlignment(Image.ALIGN_CENTER);
			logoCell.setImage(logo);
			logoCell.setFixedHeight(MOH_LOGO_MAX_HEIGHT + 4f);
		} else {
			logoCell.setPhrase(new Phrase(" ", BRAND));
			logoCell.setFixedHeight(24f);
		}
		brand.addCell(logoCell);

		Paragraph moh = new Paragraph("MINISTRY OF HEALTH", BRAND);
		moh.setAlignment(Element.ALIGN_CENTER);
		PdfPCell mohCell = new PdfPCell(moh);
		mohCell.setBorder(Rectangle.NO_BORDER);
		mohCell.setHorizontalAlignment(Element.ALIGN_CENTER);
		mohCell.setPaddingTop(4f);
		brand.addCell(mohCell);

		PdfPCell leftCell = new PdfPCell(brand);
		leftCell.setBorder(Rectangle.NO_BORDER);
		leftCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
		leftCell.setPaddingRight(10f);
		leftCell.setPaddingTop(2f);
		leftCell.setPaddingBottom(2f);

		PdfPCell box = new PdfPCell();
		box.setBorder(Rectangle.BOX);
		box.setBorderWidth(1.2f);
		box.setPadding(6f);
		box.setPaddingLeft(8f);
		Paragraph right = new Paragraph();
		right.setLeading(12.5f);
		right.add(headerField("Province:", text(transfer, "province")));
		right.add(Chunk.NEWLINE);
		right.add(headerField("District:", text(transfer, "district")));
		right.add(Chunk.NEWLINE);
		right.add(headerField("Name of Hospital:",
				first(transfer, "hospitalName", "receivingFacility", "destination")));
		right.add(Chunk.NEWLINE);
		right.add(headerField("Name of Referring Facility:",
				first(transfer, "referringFacilityName", "origin", "hospitalName")));
		right.add(Chunk.NEWLINE);
		right.add(headerField("Referring Unit:", first(transfer, "referringUnit", "admitSource")));
		right.add(Chunk.NEWLINE);
		right.add(headerField("Receiving Clinician /Phone:",
				first(transfer, "receivingClinicianPhone", "staffContactPhone")));
		box.addElement(right);

		PdfPCell rightCell = new PdfPCell();
		rightCell.setBorder(Rectangle.NO_BORDER);
		rightCell.setPadding(0);
		rightCell.setVerticalAlignment(Element.ALIGN_TOP);
		PdfPTable rightWrap = new PdfPTable(1);
		rightWrap.setWidthPercentage(100);
		rightWrap.addCell(box);
		rightCell.addElement(rightWrap);

		table.addCell(leftCell);
		table.addCell(rightCell);
		return table;
	}

	private Phrase headerField(String label, String value) {
		Phrase phrase = new Phrase();
		phrase.add(new Chunk(label + " ", HEADER_LABEL));
		if (StringUtils.isNotBlank(value)) {
			phrase.add(new Chunk(value, HEADER_VALUE));
		}
		return phrase;
	}

	private static Image loadMohLogo() {
		try {
			byte[] bytes = cachedMohLogoBytes;
			if (bytes == null) {
				InputStream in = TransferFormPdfRenderer.class.getResourceAsStream(MOH_LOGO_RESOURCE);
				if (in == null) {
					log.warn("MOH logo resource missing: " + MOH_LOGO_RESOURCE);
					return null;
				}
				try {
					bytes = readAllBytes(in);
				} finally {
					try {
						in.close();
					} catch (Exception ignored) {
						// ignore
					}
				}
				cachedMohLogoBytes = bytes;
			}
			Image logo = Image.getInstance(bytes);
			logo.scaleToFit(MOH_LOGO_MAX_WIDTH, MOH_LOGO_MAX_HEIGHT);
			return logo;
		}
		catch (Exception ex) {
			log.warn("Unable to load MOH logo for transfer PDF export", ex);
			return null;
		}
	}

	private static byte[] readAllBytes(InputStream in) throws Exception {
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		byte[] chunk = new byte[4096];
		int read;
		while ((read = in.read(chunk)) != -1) {
			buffer.write(chunk, 0, read);
		}
		return buffer.toByteArray();
	}

	private Paragraph sectionTitle(String title) {
		Font underlined = FontFactory.getFont(FontFactory.TIMES_BOLD, 11, Font.UNDERLINE);
		Paragraph p = new Paragraph(title, underlined);
		p.setSpacingBefore(7f);
		p.setSpacingAfter(3f);
		p.setAlignment(Element.ALIGN_CENTER);
		return p;
	}

	private PdfPTable vitalsRow(Map<String, Object> transfer) throws DocumentException {
		return spacedRow(new float[] { 1.15f, 0.7f, 0.85f, 0.7f, 0.85f, 1f, 0.9f, 0.9f, 0.85f },
				labelOnly("Vital Signs:"),
				pair("T°:", first(transfer, "temperature", "vitalTemp")),
				pair("SpO2:", first(transfer, "spo2", "vitalSpo2")),
				pair("RR:", first(transfer, "respiratoryRate", "vitalRr")),
				pair("Pulse:", first(transfer, "pulse", "vitalPulse")),
				pair("BP:", first(transfer, "bloodPressure", "vitalBp")),
				pair("Weight:", first(transfer, "weight", "vitalWeight")),
				pair("Height:", first(transfer, "height", "vitalHeight")),
				pair("MUAC:", first(transfer, "muac", "vitalMuac")));
	}

	/**
	 * Spreads fields across the full page width so labels/values sit on one horizontal line
	 * with clear gaps (e.g. Client Name .... Serial number).
	 */
	private PdfPTable spacedRow(Phrase... parts) throws DocumentException {
		return spacedRow(null, parts);
	}

	private PdfPTable spacedRow(float[] widths, Phrase... parts) throws DocumentException {
		int cols = Math.max(1, parts == null ? 0 : parts.length);
		PdfPTable table = new PdfPTable(cols);
		table.setWidthPercentage(100);
		table.setSpacingAfter(2.5f);
		table.setWidths(widths != null && widths.length == cols ? widths : evenWidths(cols));
		if (parts != null) {
			for (Phrase part : parts) {
				PdfPCell cell = new PdfPCell(part == null ? new Phrase("") : part);
				cell.setBorder(Rectangle.NO_BORDER);
				cell.setPadding(0f);
				cell.setPaddingRight(8f);
				cell.setPaddingBottom(1.5f);
				cell.setVerticalAlignment(Element.ALIGN_TOP);
				table.addCell(cell);
			}
		}
		return table;
	}

	private static float[] evenWidths(int cols) {
		float[] widths = new float[cols];
		for (int i = 0; i < cols; i++) {
			widths[i] = 1f;
		}
		return widths;
	}

	private Paragraph block(String label, String value) {
		Paragraph p = new Paragraph();
		p.setSpacingAfter(3f);
		p.setLeading(11.5f);
		p.add(new Chunk(label + " ", LABEL));
		if (StringUtils.isNotBlank(value)) {
			p.add(new Chunk(value, VALUE));
		}
		return p;
	}

	private PdfPTable outcomeRow(String outcomeCode, String outcomeLabel) throws DocumentException {
		String code = StringUtils.upperCase(StringUtils.trimToEmpty(outcomeCode));
		String label = StringUtils.lowerCase(StringUtils.trimToEmpty(outcomeLabel));
		return spacedRow(
				labelOnly("Outcome:"),
				choice("Stabilized/Cured", "STABILIZED_CURED".equals(code) || label.contains("stabilized")),
				choice("Died", "DIED".equals(code) || label.equals("died")),
				choice("Escaped", "ESCAPED".equals(code) || label.equals("escaped")),
				choice("To be followed up", "TO_BE_FOLLOWED_UP".equals(code) || label.contains("followed")),
				choice("Referred to high level",
						"REFERRED_TO_HIGH_LEVEL".equals(code) || label.contains("high level")));
	}

	private static Phrase pair(String label, String value) {
		Phrase phrase = new Phrase();
		phrase.add(new Chunk(label + " ", LABEL));
		if (StringUtils.isNotBlank(value)) {
			phrase.add(new Chunk(value, VALUE));
		}
		return phrase;
	}

	private static Phrase labelOnly(String label) {
		return new Phrase(new Chunk(label, LABEL));
	}

	private static Phrase choice(String label, boolean selected) {
		Phrase phrase = new Phrase();
		phrase.add(new Chunk((selected ? "● " : "○ ") + label, selected ? VALUE : LABEL));
		return phrase;
	}

	private static boolean hasFeedback(Map<String, Object> feedback) {
		if (feedback == null || feedback.isEmpty()) {
			return false;
		}
		return StringUtils.isNotBlank(text(feedback, "finalDiagnosis"))
				|| StringUtils.isNotBlank(text(feedback, "treatmentGiven"))
				|| StringUtils.isNotBlank(text(feedback, "outcome"))
				|| StringUtils.isNotBlank(text(feedback, "recommendations"))
				|| StringUtils.isNotBlank(text(feedback, "dateOfDischarge"));
	}

	private static boolean flag(Map<String, Object> map, String key) {
		Object value = map == null ? null : map.get(key);
		if (value instanceof Boolean) {
			return ((Boolean) value).booleanValue();
		}
		if (value == null) {
			return false;
		}
		String text = String.valueOf(value).trim();
		return "true".equalsIgnoreCase(text) || "1".equals(text) || "yes".equalsIgnoreCase(text);
	}

	private static String text(Map<String, Object> map, String key) {
		if (map == null || key == null) {
			return "";
		}
		Object value = map.get(key);
		return value == null ? "" : StringUtils.trimToEmpty(String.valueOf(value));
	}

	/** First non-blank map value for the given keys; empty string if none found. */
	private static String first(Map<String, Object> map, String... keys) {
		if (map == null || keys == null) {
			return "";
		}
		for (String key : keys) {
			String value = text(map, key);
			if (StringUtils.isNotBlank(value)) {
				return value;
			}
		}
		return "";
	}

	private static String coalesce(String... values) {
		if (values == null) {
			return "";
		}
		for (String value : values) {
			if (StringUtils.isNotBlank(value)) {
				return value.trim();
			}
		}
		return "";
	}
}
