package com.cengage.contenttaggingservice.unibookstore.service;

import com.cengage.contenttaggingservice.unibookstore.dto.OrderSnapshot;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

@Slf4j
@Component
public class PdfReceiptGenerator {

    private static final float MARGIN = 50f;
    private static final float LINE_HEIGHT = 18f;
    private static final float START_Y = 760f;

    public byte[] render(OrderSnapshot snapshot, String tagline) {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                PDType1Font regular = font(Standard14Fonts.FontName.HELVETICA);
                PDType1Font bold = font(Standard14Fonts.FontName.HELVETICA_BOLD);
                float y = START_Y;
                y = writeLine(content, bold, 18f, "Order receipt #" + snapshot.orderId(), y);
                y -= LINE_HEIGHT;
                y = writeLine(content, regular, 11f, "Customer: " + snapshot.customerId(), y);
                y = writeLine(content, regular, 11f, "Placed at: " + snapshot.createdAt(), y);
                y -= LINE_HEIGHT;

                for (var line : snapshot.lines()) {
                    String text = String.format("%d x %s (%s) @ %s",
                            line.count(), line.title(), line.author(), line.unitPrice());
                    y = writeLine(content, regular, 11f, text, y);
                }

                y -= LINE_HEIGHT;
                y = writeLine(content, bold, 12f, "Total: " + snapshot.grandTotal(), y);
                y -= LINE_HEIGHT;
                writeLine(content, regular, 10f, tagline, y);
            }

            document.save(out);
            log.info("Rendered receipt PDF for order {}", snapshot.orderId());
            return out.toByteArray();
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to render receipt PDF for order " + snapshot.orderId(), ex);
        }
    }

    private PDType1Font font(Standard14Fonts.FontName name) {
        return new PDType1Font(name);
    }

    private float writeLine(PDPageContentStream content, PDType1Font font, float size, String text, float y)
            throws IOException {
        content.beginText();
        content.setFont(font, size);
        content.newLineAtOffset(MARGIN, y);
        content.showText(text == null ? "" : text);
        content.endText();
        return y - LINE_HEIGHT;
    }
}
