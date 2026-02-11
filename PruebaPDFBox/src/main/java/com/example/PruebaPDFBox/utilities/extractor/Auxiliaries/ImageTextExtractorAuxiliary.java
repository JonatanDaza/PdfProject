package com.example.PruebaPDFBox.utilities.extractor.Auxiliaries;

import net.sourceforge.tess4j.Tesseract;
import org.apache.pdfbox.pdmodel.PDDocument;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

import static com.example.PruebaPDFBox.utilities.extractor.TextExtractor.ImageTextExtractor.MIN_VALID_IMAGE_HEIGHT;
import static com.example.PruebaPDFBox.utilities.extractor.TextExtractor.ImageTextExtractor.MIN_VALID_IMAGE_WIDTH;

public class ImageTextExtractorAuxiliary {

    private static final String TESSERACT_CHAR_WHITELIST =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789.,%-:/()*+ ";

    // ===================== IMAGE PROCESSING METHODS =====================

    public BufferedImage optimizeImageForOCR(BufferedImage original, int minImageWidth) {
        int width = original.getWidth();

        if (width < minImageWidth) {
            return scaleImage(original, minImageWidth);
        }

        return convertToGrayscale(original);
    }

    public BufferedImage scaleImage(BufferedImage original, int targetMinWidth) {
        double scale = (double) targetMinWidth / original.getWidth();
        int targetWidth = (int) (original.getWidth() * scale);
        int targetHeight = (int) (original.getHeight() * scale);

        BufferedImage scaled = new BufferedImage(targetWidth, targetHeight,
                BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g = scaled.createGraphics();

        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.drawImage(original, 0, 0, targetWidth, targetHeight, null);
        g.dispose();

        return scaled;
    }

    public BufferedImage convertToGrayscale(BufferedImage image) {
        BufferedImage grayscale = new BufferedImage(
                image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_GRAY
        );
        Graphics2D g = grayscale.createGraphics();
        g.drawImage(image, 0, 0, null);
        g.dispose();
        return grayscale;
    }

    // ===================== TESSERACT CONFIGURATION METHODS =====================

    public void configureTesseract(Tesseract tesseract) {
        tesseract.setOcrEngineMode(1); // LSTM OCR Engine mode
        tesseract.setVariable("tessedit_char_whitelist", TESSERACT_CHAR_WHITELIST);
    }

    // ===================== VALIDATION METHODS =====================

    public boolean isValidPage(PDDocument doc, int page) {
        return page >= 0 && page < doc.getNumberOfPages();
    }

    public boolean isValidTableImage(BufferedImage img) {
        return img != null &&
                img.getWidth() >= MIN_VALID_IMAGE_WIDTH &&
                img.getHeight() >= MIN_VALID_IMAGE_HEIGHT;
    }

    // ===================== LOGGING METHODS =====================

    public void logAttempt(int psm) {
        System.out.println("=== Intentando con PSM: " + psm + " ===");
    }

    public void logSuccess(int psm, int dataSize) {
        System.out.println("✓ PSM " + psm + " funcionó, encontrados " + dataSize + " valores");
    }

    public void logExtractedText(String text) {
        System.out.println("=== TEXTO COMPLETO EXTRAÍDO POR OCR ===");
        System.out.println(text);
        System.out.println("=======================================");
    }

    public void logExtractedValue(String label, String value) {
        System.out.println("Label: " + label + " -> Value: " + value);
    }

    // ===================== TEXT EXTRACTION METHODS =====================

    public Map<String, String> extractDataFromText(List<String> labels, String[] lines) {
        Map<String, String> data = new HashMap<>();

        for (String label : labels) {
            String value = findValueForLabelInLines(label, lines);
            if (isNotBlank(value)) {
                logExtractedValue(label, value);
                data.put(label, value);
            }
        }

        return data;
    }

    public String findValueForLabelInLines(String label, String[] lines) {
        String normalizedLabel = normalize(label);

        for (int i = 0; i < lines.length; i++) {
            if (!normalize(lines[i]).contains(normalizedLabel)) continue;

            String value = extractValueAfterLabel(lines[i], label);
            if (value != null) return value;

            if (i + 1 < lines.length) {
                value = extractFirstNumeric(lines[i + 1]);
                if (value != null) return value;
            }
        }
        return null;
    }

    public String extractValueAfterLabel(String line, String label) {
        int labelPos = line.toUpperCase().indexOf(label.toUpperCase());
        if (labelPos == -1) return null;

        String afterLabel = line.substring(labelPos + label.length());
        String[] tokens = afterLabel.trim().split("\\s+");

        for (int i = 0; i < Math.min(tokens.length, 5); i++) {
            String token = tokens[i].trim();
            if (token.isEmpty()) continue;

            if (isValidNumericValue(token)) {
                String cleaned = clean(token);

                if (i + 1 < tokens.length && tokens[i + 1].equals("-")) {
                    break;
                }

                return cleaned;
            }
        }

        return null;
    }

    public String extractFirstNumeric(String line) {
        return Arrays.stream(line.split("\\s+"))
                .filter(this::isValidNumericValue)
                .findFirst()
                .map(this::clean)
                .orElse(null);
    }

    // ===================== UTILITY METHODS =====================

    public String normalize(String text) {
        if (text == null) return "";
        return text.toUpperCase().replaceAll("[.\\s]+", "");
    }

    public boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    public boolean isNotBlank(String s) {
        return !isBlank(s);
    }

    public boolean isValidNumericValue(String token) {
        if (token == null || token.isEmpty()) return false;

        token = token.replace("*", "").trim();
        token = token.replaceAll("(mg/dl|g/dl|gr/dl|g%|%|mm3|ul|ml|mmhg)$", "").trim();

        if (!token.matches(".*\\d.*")) return false;

        return token.matches("^[0-9]+([.,\\s][0-9]+)*$");
    }

    public String clean(String value) {
        if (value == null) return "";

        return value.trim()
                .replaceAll("(mm3|x\\s*mm3|mg/dl|g/dl|g%|%|ul|ml|mmhg|gr/dl)$", "")
                .replaceAll("\\s+", "")
                .replaceAll("^[^0-9]+", "")
                .replaceAll("[^0-9.,]+$", "")
                .trim();
    }
}