package com.mike.pdfbox;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.junit.jupiter.api.Test;

class HeicBase64ImageConverterTest {

    @Test
    void convertsHeicBase64ToPngAcceptedByPdfBox() throws IOException {
        String heicBase64 = "data:image/heic;base64," + Base64.getEncoder().encodeToString(readSampleHeicBytes());

        String convertedBase64 = HeicBase64ImageConverter.convertHeicBase64ToPdfBoxImageBase64(heicBase64, "png");

        assertAcceptableByPdfBox(Base64.getDecoder().decode(convertedBase64), "converted.png");
    }

    @Test
    void convertsHeicBase64ToTiffAcceptedByPdfBox() throws IOException {
        String heicBase64 = Base64.getEncoder().encodeToString(readSampleHeicBytes());

        String convertedBase64 = HeicBase64ImageConverter.convertHeicBase64ToPdfBoxImageBase64(heicBase64, "tif");

        assertAcceptableByPdfBox(Base64.getDecoder().decode(convertedBase64), "converted.tif");
    }

    @Test
    void rejectsUnsupportedOutputFormat() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> HeicBase64ImageConverter.convertHeicBase64ToPdfBoxImageBase64("aGVsbG8=", "webp")
        );

        assertNotNull(exception.getMessage());
    }

    private static byte[] readSampleHeicBytes() throws IOException {
        try (InputStream inputStream = HeicBase64ImageConverterTest.class.getResourceAsStream("/sample.heic.base64")) {
            if (inputStream == null) {
                throw new IOException("Missing test resource: sample.heic.base64");
            }
            String base64 = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8).replaceAll("\\s+", "");
            return Base64.getDecoder().decode(base64);
        }
    }

    private static void assertAcceptableByPdfBox(byte[] imageBytes, String name) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDImageXObject imageXObject = PDImageXObject.createFromByteArray(document, imageBytes, name);
            assertNotNull(imageXObject);
        }
    }
}
