package com.example.PruebaPDFBox.utilities.extractor.TextExtractor;

import com.example.PruebaPDFBox.record.SectionArea;
import com.example.PruebaPDFBox.utilities.Infrastructure.exception.CustomException;
import com.example.PruebaPDFBox.utilities.extractor.SectionExtractor.PdfImgSectionExtractor;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.Word;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ImageTextExtractor {

    private static final int CONFIDENCE_THRESHOLD = 15;
    private static final int ROW_THRESHOLD = 15;
    private static final int MAX_HORIZONTAL_DISTANCE = 300;
    private static final int MIN_IMAGE_WIDTH = 1500;
    private static final int MIN_VALID_IMAGE_WIDTH = 500;
    private static final int MIN_VALID_IMAGE_HEIGHT = 200;

    private static final int[] PSM_MODES = {6, 11, 4, 3};
    private static final String TESSERACT_CHAR_WHITELIST =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789.,%-:/()*+ ";

    private final ThreadLocal<Tesseract> tesseractThreadLocal;
    private final PdfImgSectionExtractor pdfImgSectionExtractor;

    public ImageTextExtractor(
            ObjectProvider<Tesseract> tesseractProvider,
            PdfImgSectionExtractor pdfImgSectionExtractor
    ) {
        this.tesseractThreadLocal = ThreadLocal.withInitial(tesseractProvider::getObject);
        this.pdfImgSectionExtractor = pdfImgSectionExtractor;
    }

    private Tesseract getTesseract() {
        return tesseractThreadLocal.get();
    }

    public Map<String, String> extractTextFromTable(
            PDDocument document,
            List<Integer> targetPages,
            List<String> labels
    ) {
        Map<String, String> extractedData = new HashMap<>();

        for (int pageNum : targetPages) {
            if (!isValidPage(document, pageNum)) continue;

            List<BufferedImage> images = pdfImgSectionExtractor.extractImages(
                    document, new SectionArea(pageNum, 0, pageNum, Float.MAX_VALUE)
            );

            for (BufferedImage image : images) {
                if (!isValidTableImage(image)) continue;

                BufferedImage optimizedImage = optimizeImageForOCR(image);
                Map<String, String> data = tryExtractWithMultiplePSM(optimizedImage, labels);
                extractedData.putAll(data);
            }
        }

        return extractedData;
    }

    private Map<String, String> tryExtractWithMultiplePSM(
            BufferedImage image,
            List<String> labels
    ) {
        Tesseract tesseract = getTesseract();
        configureTesseract(tesseract);

        for (int psm : PSM_MODES) {
            tesseract.setPageSegMode(psm);
            logAttempt(psm);

            Map<String, String> data = extractUsingTextAnalysis(image, labels, tesseract);
            if (data.isEmpty()) {
                data = extractUsingPositionalAnalysis(image, labels, tesseract);
            }

            if (!data.isEmpty()) {
                logSuccess(psm, data.size());
                return data;
            }
        }

        return new HashMap<>();
    }

    private void configureTesseract(Tesseract tesseract) {
        tesseract.setOcrEngineMode(1); // LSTM OCR Engine mode
        tesseract.setVariable("tessedit_char_whitelist", TESSERACT_CHAR_WHITELIST);
    }

    private void logAttempt(int psm) {
        System.out.println("=== Intentando con PSM: " + psm + " ===");
    }

    private void logSuccess(int psm, int dataSize) {
        System.out.println("✓ PSM " + psm + " funcionó, encontrados " + dataSize + " valores");
    }

    private BufferedImage optimizeImageForOCR(BufferedImage original) {
        int width = original.getWidth();
        int height = original.getHeight();

        if (width < MIN_IMAGE_WIDTH) {
            return scaleImage(original, MIN_IMAGE_WIDTH);
        }

        return convertToGrayscale(original);
    }

    private BufferedImage scaleImage(BufferedImage original, int targetMinWidth) {
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

    private BufferedImage convertToGrayscale(BufferedImage image) {
        BufferedImage grayscale = new BufferedImage(
                image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_GRAY
        );
        Graphics2D g = grayscale.createGraphics();
        g.drawImage(image, 0, 0, null);
        g.dispose();
        return grayscale;
    }

    private Map<String, String> extractUsingPositionalAnalysis(
            BufferedImage image,
            List<String> labels,
            Tesseract tesseract
    ) {
        try {
            List<Word> words = tesseract.getWords(image, 0);
            if (words == null || words.isEmpty()) return new HashMap<>();

            logDetectedWords(words);

            List<Word> validWords = words.stream()
                    .filter(w -> w.getConfidence() > CONFIDENCE_THRESHOLD)
                    .toList();

            Map<Integer, List<Word>> rows = organizeWordsIntoRows(validWords);
            return extractDataFromRows(labels, rows);

        } catch (Exception e) {
            throw new CustomException("Error en análisis posicional: " + e.getMessage(),
                    HttpStatus.BAD_REQUEST);
        }
    }

    private Map<String, String> extractUsingTextAnalysis(
            BufferedImage image,
            List<String> labels,
            Tesseract tesseract
    ) {
        try {
            String text = tesseract.doOCR(image);
            logExtractedText(text);

            if (isBlank(text)) return new HashMap<>();

            return extractDataFromText(labels, text.split("\n"));

        } catch (Exception e) {
            throw new CustomException("Error al analizar el texto: " + e.getMessage(),
                    HttpStatus.BAD_REQUEST);
        }
    }

    private void logDetectedWords(List<Word> words) {
        System.out.println("=== PALABRAS DETECTADAS (con confianza) ===");
        for (Word word : words) {
            System.out.printf("'%s' (conf: %.1f, x:%d, y:%d)%n",
                    word.getText(),
                    word.getConfidence(),
                    word.getBoundingBox().x,
                    word.getBoundingBox().y
            );
        }
        System.out.println("==========================================");
    }

    private void logExtractedText(String text) {
        System.out.println("=== TEXTO COMPLETO EXTRAÍDO POR OCR ===");
        System.out.println(text);
        System.out.println("=======================================");
    }

    private Map<Integer, List<Word>> organizeWordsIntoRows(List<Word> words) {
        Map<Integer, List<Word>> rows = new TreeMap<>();

        for (Word word : words) {
            int y = word.getBoundingBox().y;
            Integer rowKey = rows.keySet().stream()
                    .filter(k -> Math.abs(k - y) < ROW_THRESHOLD)
                    .findFirst()
                    .orElse(y);

            rows.computeIfAbsent(rowKey, k -> new ArrayList<>()).add(word);
        }

        rows.values().forEach(r -> r.sort(Comparator.comparingInt(w -> w.getBoundingBox().x)));
        return rows;
    }

    private Map<String, String> extractDataFromRows(List<String> labels, Map<Integer, List<Word>> rows) {
        Map<String, String> data = new HashMap<>();

        for (String label : labels) {
            String value = findValueForLabelInRows(label, rows);
            if (isNotBlank(value)) {
                logExtractedValue(label, value);
                data.put(label, value);
            }
        }

        return data;
    }

    private Map<String, String> extractDataFromText(List<String> labels, String[] lines) {
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

    private void logExtractedValue(String label, String value) {
        System.out.println("Label: " + label + " -> Value: " + value);
    }

    private String findValueForLabelInRows(String label, Map<Integer, List<Word>> rows) {
        String normalizedLabel = normalize(label);

        for (List<Word> row : rows.values()) {
            int labelIndex = findLabelIndexInRow(row, normalizedLabel);
            if (labelIndex == -1) continue;

            String value = extractNumericValueAfterLabel(row, labelIndex);
            if (value != null && isValueNearLabel(row, labelIndex)) {
                return value;
            }
        }
        return null;
    }

    private boolean isValueNearLabel(List<Word> row, int labelIndex) {
        if (labelIndex + 1 >= row.size()) return false;

        int labelX = row.get(labelIndex).getBoundingBox().x;
        int valueX = row.get(labelIndex + 1).getBoundingBox().x;

        return (valueX - labelX) < MAX_HORIZONTAL_DISTANCE;
    }

    private int findLabelIndexInRow(List<Word> row, String normalizedLabel) {
        String rowText = normalize(row.stream()
                .map(Word::getText)
                .collect(Collectors.joining(" ")));

        if (!rowText.contains(normalizedLabel)) return -1;

        // Para labels compuestos
        if (normalizedLabel.length() > 10) {
            for (int i = 0; i < row.size() - 1; i++) {
                String combined = normalize(row.get(i).getText() + row.get(i + 1).getText());
                if (combined.equals(normalizedLabel)) {
                    return i + 1;
                }
            }
        }

        int bestMatch = -1;
        int longestMatch = 0;

        for (int i = 0; i < row.size(); i++) {
            String word = normalize(row.get(i).getText());
            if (normalizedLabel.contains(word) && word.length() > longestMatch) {
                bestMatch = i;
                longestMatch = word.length();
            }
        }

        return bestMatch;
    }

    private String extractNumericValueAfterLabel(List<Word> row, int index) {
        for (int i = index + 1; i < Math.min(index + 4, row.size()); i++) {
            String text = row.get(i).getText().replace("*", "");

            if (isNumeric(text)) {
                String value = text;

                if (i + 1 < row.size() && isUnit(row.get(i + 1).getText())) {
                    value += " " + row.get(i + 1).getText();
                }

                return clean(value);
            }
        }
        return null;
    }

    private String findValueForLabelInLines(String label, String[] lines) {
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

    private String extractValueAfterLabel(String line, String label) {
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

    private String extractFirstNumeric(String line) {
        return Arrays.stream(line.split("\\s+"))
                .filter(this::isValidNumericValue)
                .findFirst()
                .map(this::clean)
                .orElse(null);
    }

    // ===================== UTILITY METHODS =====================

    private boolean isValidPage(PDDocument doc, int page) {
        return page >= 0 && page < doc.getNumberOfPages();
    }

    private boolean isValidTableImage(BufferedImage img) {
        return img != null &&
                img.getWidth() >= MIN_VALID_IMAGE_WIDTH &&
                img.getHeight() >= MIN_VALID_IMAGE_HEIGHT;
    }

    private boolean isUnit(String text) {
        return text != null && text.toLowerCase()
                .matches(".*(g%|%|gr/dl|mg/dl|mm3|mm/h|x|ul|ml|mmhg).*");
    }

    private String normalize(String text) {
        if (text == null) return "";
        return text.toUpperCase().replaceAll("[.\\s]+", "");
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private boolean isNotBlank(String s) {
        return !isBlank(s);
    }

    private boolean isValidNumericValue(String token) {
        if (token == null || token.isEmpty()) return false;

        token = token.replace("*", "").trim();
        token = token.replaceAll("(mg/dl|g/dl|gr/dl|g%|%|mm3|ul|ml|mmhg)$", "").trim();

        if (!token.matches(".*\\d.*")) return false;

        return token.matches("^[0-9]+([.,\\s][0-9]+)*$");
    }

    private String clean(String value) {
        if (value == null) return "";

        return value.trim()
                .replaceAll("(mm3|x\\s*mm3|mg/dl|g/dl|g%|%|ul|ml|mmhg|gr/dl)$", "")
                .replaceAll("\\s+", "")
                .replaceAll("^[^0-9]+", "")
                .replaceAll("[^0-9.,]+$", "")
                .trim();
    }

    private boolean isNumeric(String text) {
        return isValidNumericValue(text);
    }

    public boolean hasValidTableImage(PDDocument document, List<Integer> targetPages) {
        for (int pageNum : targetPages) {
            if (!isValidPage(document, pageNum)) continue;

            List<BufferedImage> images = pdfImgSectionExtractor.extractImages(
                    document, new SectionArea(pageNum, 0, pageNum, Float.MAX_VALUE)
            );

            for (BufferedImage image : images) {
                if (isValidTableImage(image)) {
                    return true;
                }
            }
        }
        return false;
    }
}