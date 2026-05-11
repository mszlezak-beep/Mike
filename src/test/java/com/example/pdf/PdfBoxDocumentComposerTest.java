package com.example.pdf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

class PdfBoxDocumentComposerTest {
    private static final PDType1Font TEST_FONT =
            new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private static final String ONE_PIXEL_PNG =
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+/p9sAAAAASUVORK5CYII=";

    private final PdfBoxDocumentComposer composer = new PdfBoxDocumentComposer();

    @Test
    void mergesPdfsAndAppendsPageWithHeaderAndBase64Image() throws IOException {
        byte[] firstPdf = onePagePdf("First source PDF");
        byte[] secondPdf = onePagePdf("Second source PDF");
        PdfBoxDocumentComposer.NewPage newPage = new PdfBoxDocumentComposer.NewPage(
                "Header test",
                List.of(new PdfBoxDocumentComposer.Base64Image(ONE_PIXEL_PNG, 72.0F, 500.0F, 64.0F, 64.0F)));

        byte[] output = composer.mergeAndAppendPages(List.of(firstPdf, secondPdf), List.of(newPage));

        try (PDDocument document = Loader.loadPDF(output)) {
            assertEquals(3, document.getNumberOfPages());

            String text = new PDFTextStripper().getText(document);
            assertTrue(text.contains("First source PDF"));
            assertTrue(text.contains("Second source PDF"));
            assertTrue(text.contains("Header test"));
            assertTrue(document.getPage(2).getResources().getXObjectNames().iterator().hasNext());
        }
    }

    @Test
    void canCreateDocumentFromOnlyNewPages() throws IOException {
        PdfBoxDocumentComposer.NewPage newPage = new PdfBoxDocumentComposer.NewPage(
                "Only generated page",
                List.of(new PdfBoxDocumentComposer.Base64Image(
                        "data:image/png;base64," + ONE_PIXEL_PNG,
                        72.0F,
                        500.0F,
                        32.0F,
                        32.0F)));

        byte[] output = composer.mergeAndAppendPages(List.of(), List.of(newPage));

        try (PDDocument document = Loader.loadPDF(output)) {
            assertEquals(1, document.getNumberOfPages());
            assertTrue(new PDFTextStripper().getText(document).contains("Only generated page"));
        }
    }

    @Test
    void rejectsRequestsWithoutSourcePdfsOrNewPages() {
        assertThrows(IllegalArgumentException.class, () -> composer.mergeAndAppendPages(List.of(), List.of()));
    }

    private static byte[] onePagePdf(String text) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.LETTER);
            document.addPage(page);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.beginText();
                contentStream.setFont(TEST_FONT, 12.0F);
                contentStream.newLineAtOffset(72.0F, 720.0F);
                contentStream.showText(text);
                contentStream.endText();
            }

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            document.save(output);
            return output.toByteArray();
        }
    }
}
