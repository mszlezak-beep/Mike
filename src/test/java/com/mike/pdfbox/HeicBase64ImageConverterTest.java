package com.mike.pdfbox;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Iterator;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.junit.jupiter.api.Test;

class HeicBase64ImageConverterTest {

    @Test
    void convertsHeicBase64ToPngAcceptedByPdfBox() throws IOException {
        String heicBase64 = "data:image/heic;base64," + Base64.getEncoder().encodeToString(createHeicBytes());

        String convertedBase64 = HeicBase64ImageConverter.convertHeicBase64ToPdfBoxImageBase64(heicBase64, "png");

        assertAcceptableByPdfBox(Base64.getDecoder().decode(convertedBase64), "converted.png");
    }

    @Test
    void convertsHeicBase64ToTiffAcceptedByPdfBox() throws IOException {
        String heicBase64 = Base64.getEncoder().encodeToString(createHeicBytes());

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

    private static byte[] createHeicBytes() throws IOException {
        ImageIO.scanForPlugins();

        BufferedImage image = new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB);
        image.setRGB(0, 0, Color.RED.getRGB());
        image.setRGB(1, 0, Color.GREEN.getRGB());
        image.setRGB(2, 0, Color.BLUE.getRGB());
        image.setRGB(3, 0, Color.WHITE.getRGB());

        for (String formatName : new String[]{"heic", "heif"}) {
            Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName(formatName);
            if (!writers.hasNext()) {
                continue;
            }

            ImageWriter writer = writers.next();
            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                 ImageOutputStream imageOutputStream = ImageIO.createImageOutputStream(outputStream)) {
                writer.setOutput(imageOutputStream);
                writer.write(null, new IIOImage(image, null, null), writer.getDefaultWriteParam());
                writer.dispose();
                return outputStream.toByteArray();
            }
        }

        throw new IOException("No HEIC/HEIF ImageIO writer was registered for the test runtime.");
    }

    private static void assertAcceptableByPdfBox(byte[] imageBytes, String name) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDImageXObject imageXObject = PDImageXObject.createFromByteArray(document, imageBytes, name);
            assertNotNull(imageXObject);
        }
    }
}
