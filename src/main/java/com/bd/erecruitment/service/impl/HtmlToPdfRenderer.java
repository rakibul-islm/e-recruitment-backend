package com.bd.erecruitment.service.impl;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;

// Shared HTML -> PDF rendering, used by both CvGenerationServiceImpl and OfferServiceImpl (offer
// letters). The renderer requires well-formed XML input (it parses via TRaX), not just valid
// HTML - callers must build markup with escaped text and no unescaped named entities (e.g. use
// the literal "·" character rather than "&middot;").
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
