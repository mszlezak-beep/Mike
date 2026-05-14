package com.mike.pdfbox;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;
import javax.imageio.ImageIO;

/**
 * Converts a HEIC/HEIF image encoded as base64 into a PDFBox 2.x-compatible image format.
 * <p>
 * Supported output formats mirror the formats accepted by {@code PDImageXObject#createFromByteArray}
 * in PDFBox 2.x: JPEG, PNG, TIFF, GIF, and BMP.
 * </p>
 * <p>
 * The HEIC decoder relies on the {@code heif-convert} CLI from libheif. On Ubuntu this is
 * typically provided by the {@code libheif-examples} package.
 * </p>
 */
public final class HeicBase64ImageConverter {

    private static final String DEFAULT_OUTPUT_FORMAT = "png";
    private static final String HEIF_CONVERT_COMMAND = "heif-convert";

    private static final Map<String, String> FORMAT_ALIASES = Map.of(
            "jpg", "jpeg",
            "jpeg", "jpeg",
            "png", "png",
            "tif", "tiff",
            "tiff", "tiff",
            "gif", "gif",
            "bmp", "bmp"
    );

    private HeicBase64ImageConverter() {
    }

    /**
     * Converts a HEIC base64 string into PNG base64.
     *
     * @param heicBase64 base64 string for the input HEIC/HEIF image
     * @return base64 string for the converted PNG image
     * @throws IOException if the HEIC input cannot be decoded or the output image cannot be encoded
     */
    public static String convertHeicBase64ToPdfBoxImageBase64(String heicBase64) throws IOException {
        return convertHeicBase64ToPdfBoxImageBase64(heicBase64, DEFAULT_OUTPUT_FORMAT);
    }

    /**
     * Converts a HEIC base64 string into one of the image formats PDFBox 2.x can ingest directly.
     *
     * @param heicBase64 base64 string for the input HEIC/HEIF image
     * @param outputFormat target format: jpg/jpeg, png, tif/tiff, gif, or bmp
     * @return base64 string for the converted image in the requested output format
     * @throws IOException if the HEIC input cannot be decoded or the output image cannot be encoded
     */
    public static String convertHeicBase64ToPdfBoxImageBase64(String heicBase64,
                                                              String outputFormat) throws IOException {
        byte[] convertedBytes = convertHeicBase64ToPdfBoxImageBytes(heicBase64, outputFormat);
        return Base64.getEncoder().encodeToString(convertedBytes);
    }

    /**
     * Converts a HEIC base64 string into PDFBox 2.x-compatible image bytes.
     *
     * @param heicBase64 base64 string for the input HEIC/HEIF image
     * @param outputFormat target format: jpg/jpeg, png, tif/tiff, gif, or bmp
     * @return converted image bytes
     * @throws IOException if the HEIC input cannot be decoded or the output image cannot be encoded
     */
    public static byte[] convertHeicBase64ToPdfBoxImageBytes(String heicBase64,
                                                             String outputFormat) throws IOException {
        String normalizedFormat = normalizeOutputFormat(outputFormat);
        byte[] heicBytes = decodeBase64Image(heicBase64);
        BufferedImage sourceImage = decodeHeic(heicBytes);
        BufferedImage outputImage = prepareForOutput(sourceImage, normalizedFormat);
        return encode(outputImage, normalizedFormat);
    }

    private static String normalizeOutputFormat(String outputFormat) {
        Objects.requireNonNull(outputFormat, "outputFormat must not be null");

        String normalized = FORMAT_ALIASES.get(outputFormat.trim().toLowerCase());
        if (normalized == null) {
            throw new IllegalArgumentException(
                    "Unsupported PDFBox 2.x output format: " + outputFormat
                            + ". Supported formats are jpg/jpeg, png, tif/tiff, gif, and bmp."
            );
        }
        return normalized;
    }

    private static byte[] decodeBase64Image(String base64Value) {
        Objects.requireNonNull(base64Value, "heicBase64 must not be null");

        String normalized = base64Value.trim().replaceAll("\\s+", "");
        int dataUriSeparator = normalized.indexOf("base64,");
        if (dataUriSeparator >= 0) {
            normalized = normalized.substring(dataUriSeparator + "base64,".length());
        }

        try {
            return Base64.getDecoder().decode(normalized);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("heicBase64 is not valid base64 data", exception);
        }
    }

    private static BufferedImage decodeHeic(byte[] heicBytes) throws IOException {
        byte[] pngBytes = convertHeicToPng(heicBytes);
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(pngBytes)) {
            BufferedImage image = ImageIO.read(inputStream);
            if (image == null) {
                throw new IOException("Unable to decode intermediary PNG produced from HEIC input.");
            }
            return image;
        }
    }

    private static byte[] convertHeicToPng(byte[] heicBytes) throws IOException {
        Path inputFile = Files.createTempFile("heic-input-", ".heic");
        Path outputFile = Files.createTempFile("heic-output-", ".png");

        try {
            Files.write(inputFile, heicBytes);

            Process process;
            try {
                process = new ProcessBuilder(
                        HEIF_CONVERT_COMMAND,
                        inputFile.toString(),
                        outputFile.toString()
                ).redirectErrorStream(true).start();
            } catch (IOException exception) {
                throw new IOException(
                        "Unable to start heif-convert. Install libheif-examples and ensure heif-convert is on PATH.",
                        exception
                );
            }

            String processOutput;
            try (InputStream processStream = process.getInputStream()) {
                processOutput = new String(processStream.readAllBytes(), StandardCharsets.UTF_8);
            }

            try {
                int exitCode = process.waitFor();
                if (exitCode != 0) {
                    throw new IOException("heif-convert failed with exit code " + exitCode + ": " + processOutput.trim());
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted while waiting for heif-convert to finish", exception);
            }

            return Files.readAllBytes(outputFile);
        } finally {
            Files.deleteIfExists(inputFile);
            Files.deleteIfExists(outputFile);
        }
    }

    private static BufferedImage prepareForOutput(BufferedImage sourceImage, String outputFormat) {
        if ("jpeg".equals(outputFormat) || "bmp".equals(outputFormat)) {
            BufferedImage converted = new BufferedImage(
                    sourceImage.getWidth(),
                    sourceImage.getHeight(),
                    BufferedImage.TYPE_INT_RGB
            );

            Graphics2D graphics = converted.createGraphics();
            try {
                graphics.setColor(Color.WHITE);
                graphics.fillRect(0, 0, converted.getWidth(), converted.getHeight());
                graphics.setComposite(AlphaComposite.SrcOver);
                graphics.drawImage(sourceImage, 0, 0, null);
            } finally {
                graphics.dispose();
            }

            return converted;
        }

        if (sourceImage.getType() == BufferedImage.TYPE_CUSTOM) {
            BufferedImage converted = new BufferedImage(
                    sourceImage.getWidth(),
                    sourceImage.getHeight(),
                    sourceImage.getColorModel().hasAlpha()
                            ? BufferedImage.TYPE_INT_ARGB
                            : BufferedImage.TYPE_INT_RGB
            );

            Graphics2D graphics = converted.createGraphics();
            try {
                graphics.drawImage(sourceImage, 0, 0, null);
            } finally {
                graphics.dispose();
            }

            return converted;
        }

        return sourceImage;
    }

    private static byte[] encode(BufferedImage image, String outputFormat) throws IOException {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            boolean written = ImageIO.write(image, outputFormat, outputStream);
            if (!written) {
                throw new IOException("No ImageIO writer registered for output format: " + outputFormat);
            }
            return outputStream.toByteArray();
        }
    }
}
