package com.example.pdf;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

/**
 * Composes PDFs with Apache PDFBox.
 *
 * <p>The coordinate system uses PDF points and starts at the bottom-left corner
 * of the page. Letter pages are used for newly appended pages.</p>
 */
public final class PdfBoxDocumentComposer {
    private static final float DEFAULT_MARGIN = 54.0F;
    private static final float HEADER_FONT_SIZE = 16.0F;
    private static final PDType1Font HEADER_FONT =
            new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

    /**
     * Merges existing PDF byte arrays and appends newly generated pages.
     *
     * @param sourcePdfs PDF documents to merge in order
     * @param newPages pages to append after the merged source PDFs
     * @return the complete PDF document as bytes
     */
    public byte[] mergeAndAppendPages(List<byte[]> sourcePdfs, List<NewPage> newPages) throws IOException {
        Objects.requireNonNull(sourcePdfs, "sourcePdfs must not be null");
        Objects.requireNonNull(newPages, "newPages must not be null");

        if (sourcePdfs.isEmpty() && newPages.isEmpty()) {
            throw new IllegalArgumentException("At least one source PDF or new page is required.");
        }

        try (PDDocument document = loadMergedSources(sourcePdfs)) {
            for (NewPage page : newPages) {
                appendPage(document, page);
            }

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            document.save(output);
            return output.toByteArray();
        }
    }

    /**
     * Merges PDFs from disk, appends generated pages, and writes the output PDF.
     */
    public void mergeAndAppendPages(List<Path> sourcePdfPaths, Path outputPdfPath, List<NewPage> newPages)
            throws IOException {
        Objects.requireNonNull(sourcePdfPaths, "sourcePdfPaths must not be null");
        Objects.requireNonNull(outputPdfPath, "outputPdfPath must not be null");

        List<byte[]> sourcePdfs = sourcePdfPaths.stream()
                .map(PdfBoxDocumentComposer::readAllBytesUnchecked)
                .toList();
        byte[] output = mergeAndAppendPages(sourcePdfs, newPages);

        Path parent = outputPdfPath.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.write(outputPdfPath, output);
    }

    private static PDDocument loadMergedSources(List<byte[]> sourcePdfs) throws IOException {
        if (sourcePdfs.isEmpty()) {
            return new PDDocument();
        }

        PDFMergerUtility merger = new PDFMergerUtility();
        List<RandomAccessReadBuffer> randomAccessSources = new ArrayList<>();
        ByteArrayOutputStream mergedOutput = new ByteArrayOutputStream();
        merger.setDestinationStream(mergedOutput);

        try {
            for (byte[] sourcePdf : sourcePdfs) {
                if (sourcePdf == null || sourcePdf.length == 0) {
                    throw new IllegalArgumentException("Source PDFs must not be null or empty.");
                }
                RandomAccessReadBuffer source = new RandomAccessReadBuffer(sourcePdf);
                randomAccessSources.add(source);
                merger.addSource(source);
            }

            merger.mergeDocuments(IOUtils.createMemoryOnlyStreamCache());
            return Loader.loadPDF(mergedOutput.toByteArray());
        } finally {
            for (RandomAccessReadBuffer source : randomAccessSources) {
                source.close();
            }
        }
    }

    private static void appendPage(PDDocument document, NewPage newPage) throws IOException {
        Objects.requireNonNull(newPage, "newPage must not be null");

        PDPage page = new PDPage(PDRectangle.LETTER);
        document.addPage(page);

        PDRectangle mediaBox = page.getMediaBox();
        try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
            drawHeader(contentStream, newPage.headerText(), mediaBox);

            for (Base64Image image : newPage.images()) {
                drawImage(document, contentStream, image);
            }
        }
    }

    private static void drawHeader(PDPageContentStream contentStream, String headerText, PDRectangle mediaBox)
            throws IOException {
        if (headerText == null || headerText.isBlank()) {
            return;
        }

        contentStream.beginText();
        contentStream.setFont(HEADER_FONT, HEADER_FONT_SIZE);
        contentStream.newLineAtOffset(DEFAULT_MARGIN, mediaBox.getHeight() - DEFAULT_MARGIN);
        contentStream.showText(headerText);
        contentStream.endText();
    }

    private static void drawImage(PDDocument document, PDPageContentStream contentStream, Base64Image image)
            throws IOException {
        Objects.requireNonNull(image, "image must not be null");

        byte[] imageBytes = decodeBase64Image(image.base64());
        PDImageXObject pdImage = PDImageXObject.createFromByteArray(document, imageBytes, "base64-image");
        contentStream.drawImage(pdImage, image.x(), image.y(), image.width(), image.height());
    }

    private static byte[] decodeBase64Image(String base64Image) {
        if (base64Image == null || base64Image.isBlank()) {
            throw new IllegalArgumentException("Base64 image must not be null or blank.");
        }

        String normalized = base64Image.trim();
        int dataUriSeparator = normalized.indexOf(',');
        if (normalized.startsWith("data:") && dataUriSeparator >= 0) {
            normalized = normalized.substring(dataUriSeparator + 1);
        }

        return Base64.getMimeDecoder().decode(normalized);
    }

    private static byte[] readAllBytesUnchecked(Path path) {
        try {
            return Files.readAllBytes(path);
        } catch (IOException exception) {
            throw new IllegalArgumentException("Unable to read PDF source: " + path, exception);
        }
    }

    public record NewPage(String headerText, List<Base64Image> images) {
        public NewPage {
            images = List.copyOf(Objects.requireNonNull(images, "images must not be null"));
        }
    }

    public record Base64Image(String base64, float x, float y, float width, float height) {
        public Base64Image {
            if (width <= 0.0F || height <= 0.0F) {
                throw new IllegalArgumentException("Image width and height must be positive.");
            }
        }
    }
}
