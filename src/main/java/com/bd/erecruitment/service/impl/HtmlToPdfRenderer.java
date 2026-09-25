package com.bd.erecruitment.service.impl;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;

// Input must be well-formed XML: escape text and use literal characters (e.g. "·"), not named entities like &middot;.
@Component
public class HtmlToPdfRenderer {

	public byte[] render(String html) {
		try {
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			PdfRendererBuilder builder = new PdfRendererBuilder();
			builder.useFastMode();
			builder.withHtmlContent(html, null);
			builder.toStream(os);
			builder.run();
			return os.toByteArray();
		} catch (Exception e) {
			throw new IllegalStateException("Failed to render PDF", e);
		}
	}
}
