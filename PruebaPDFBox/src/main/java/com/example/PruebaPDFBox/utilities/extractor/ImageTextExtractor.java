
/// version 4 refactorizacion de version 2
package com.example.PruebaPDFBox.utilities.extractor;

import com.example.PruebaPDFBox.record.SectionArea;
import com.example.PruebaPDFBox.utilities.Infrastructure.exception.CustomException;
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
    private final SectionImgExtractor sectionImgExtractor;

    public ImageTextExtractor(
            ObjectProvider<Tesseract> tesseractProvider,
            SectionImgExtractor sectionImgExtractor
    ) {
        this.tesseractThreadLocal = ThreadLocal.withInitial(tesseractProvider::getObject);
        this.sectionImgExtractor = sectionImgExtractor;
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

            List<BufferedImage> images = sectionImgExtractor.extractImages(
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

            List<BufferedImage> images = sectionImgExtractor.extractImages(
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

//// version 3 daña la extraccion del label: RTO.PLAQUETAS
//package com.example.PruebaPDFBox.utilities.extractor;
//
//import com.example.PruebaPDFBox.record.SectionArea;
//import com.example.PruebaPDFBox.utilities.exception.CustomException;
//import net.sourceforge.tess4j.Tesseract;
//import net.sourceforge.tess4j.Word;
//import org.apache.pdfbox.pdmodel.PDDocument;
//import org.springframework.beans.factory.ObjectProvider;
//import org.springframework.http.HttpStatus;
//import org.springframework.stereotype.Component;
//
//import java.awt.*;
//import java.awt.image.BufferedImage;
//import java.util.*;
//import java.util.List;
//import java.util.stream.Collectors;
//
//@Component
//public class ImageTextExtractor {
//
//    private static final int CONFIDENCE_THRESHOLD = 15;
//    private static final int VERTICAL_SPACING = 50; // Espaciado vertical para considerar nueva línea
//    private static final int HORIZONTAL_SPACING = 200; // Distancia máxima horizontal para valores relacionados
//
//    private final ThreadLocal<Tesseract> tesseractThreadLocal;
//    private final SectionImgExtractor sectionImgExtractor;
//
//    public ImageTextExtractor(
//            ObjectProvider<Tesseract> tesseractProvider,
//            SectionImgExtractor sectionImgExtractor
//    ) {
//        this.tesseractThreadLocal = ThreadLocal.withInitial(tesseractProvider::getObject);
//        this.sectionImgExtractor = sectionImgExtractor;
//    }
//
//    private Tesseract getTesseract() {
//        return tesseractThreadLocal.get();
//    }
//
//    public Map<String, String> extractTextFromTable(
//            PDDocument document,
//            List<Integer> targetPages,
//            List<String> labels
//    ) {
//
//        Map<String, String> extractedData = new HashMap<>();
//
//        for (int pageNum : targetPages) {
//            if (!isValidPage(document, pageNum)) continue;
//
//            List<BufferedImage> images = extractPageImages(document, pageNum);
//
//            for (BufferedImage image : images) {
//                if (!isValidTableImage(image)) continue;
//
//                BufferedImage optimizedImage = optimizeImageForOCR(image);
//
//                Tesseract tesseract = getTesseract();
//
//                Map<String, String> data = tryExtractWithMultiplePSM(
//                        optimizedImage, labels, tesseract
//                );
//
//                extractedData.putAll(data);
//            }
//        }
//
//        return extractedData;
//    }
//
//    private Map<String, String> tryExtractWithMultiplePSM(
//            BufferedImage image,
//            List<String> labels,
//            Tesseract tesseract
//    ) {
//        tesseract.setOcrEngineMode(1);
//        tesseract.setVariable("tessedit_char_whitelist",
//                "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789.,%-:/()*+ ");
//
//        int[] psmModes = {6, 11, 4, 3};
//
//        for (int psm : psmModes) {
//            tesseract.setPageSegMode(psm);
//
//            System.out.println("=== Intentando con PSM: " + psm + " ===");
//
//            Map<String, String> data = extractUsingImprovedPositionalAnalysis(image, labels, tesseract);
//
//            if (!data.isEmpty()) {
//                System.out.println("✓ PSM " + psm + " funcionó, encontrados " + data.size() + " valores");
//                return data;
//            }
//        }
//
//        return new HashMap<>();
//    }
//
//    /* ===================== IMAGE OPTIMIZATION ===================== */
//
//    private BufferedImage optimizeImageForOCR(BufferedImage original) {
//        int width = original.getWidth();
//        int height = original.getHeight();
//
//        if (width < 1500) {
//            double scale = 1500.0 / width;
//            int targetWidth = (int) (width * scale);
//            int targetHeight = (int) (height * scale);
//
//            BufferedImage scaled = new BufferedImage(targetWidth, targetHeight,
//                    BufferedImage.TYPE_BYTE_GRAY);
//            Graphics2D g = scaled.createGraphics();
//
//            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
//                    RenderingHints.VALUE_INTERPOLATION_BICUBIC);
//
//            g.drawImage(original, 0, 0, targetWidth, targetHeight, null);
//            g.dispose();
//
//            return scaled;
//        }
//
//        BufferedImage grayscale = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
//        Graphics2D g = grayscale.createGraphics();
//        g.drawImage(original, 0, 0, null);
//        g.dispose();
//
//        return grayscale;
//    }
//
//    /* ===================== IMPROVED POSITIONAL ANALYSIS ===================== */
//
//    private Map<String, String> extractUsingImprovedPositionalAnalysis(
//            BufferedImage image,
//            List<String> labels,
//            Tesseract tesseract
//    ) {
//        Map<String, String> data = new HashMap<>();
//
//        try {
//            List<Word> words = tesseract.getWords(image, 0);
//
//            if (words == null || words.isEmpty()) return data;
//
//            System.out.println("=== PALABRAS DETECTADAS (con posición) ===");
//            for (Word word : words) {
//                System.out.printf("'%s' (conf: %.1f, x:%d, y:%d, w:%d, h:%d)%n",
//                        word.getText(),
//                        word.getConfidence(),
//                        word.getBoundingBox().x,
//                        word.getBoundingBox().y,
//                        word.getBoundingBox().width,
//                        word.getBoundingBox().height
//                );
//            }
//            System.out.println("==========================================");
//
//            List<Word> validWords = words.stream()
//                    .filter(w -> w.getConfidence() > CONFIDENCE_THRESHOLD)
//                    .sorted(Comparator.comparingInt(w -> w.getBoundingBox().y)) // Ordenar por Y primero
//                    .toList();
//
//            // Crear estructura de datos espacial
//            List<TextBlock> textBlocks = createTextBlocks(validWords);
//
//            System.out.println("\n=== BLOQUES DE TEXTO DETECTADOS ===");
//            for (int i = 0; i < textBlocks.size(); i++) {
//                TextBlock block = textBlocks.get(i);
//                System.out.printf("Bloque %d: '%s' (x:%d, y:%d)%n",
//                        i, block.text, block.x, block.y);
//            }
//            System.out.println("====================================\n");
//
//            // Buscar valores para cada label
//            for (String label : labels) {
//                String value = findValueUsingBlocks(label, textBlocks);
//                if (isNotBlank(value)) {
//                    System.out.println("✓ Label: '" + label + "' -> Value: '" + value + "'");
//                    data.put(label, value);
//                } else {
//                    System.out.println("✗ Label: '" + label + "' -> NO ENCONTRADO");
//                }
//            }
//
//        } catch (Exception e) {
//            throw new CustomException("Error en análisis posicional: " + e.getMessage(), HttpStatus.BAD_REQUEST);
//        }
//
//        return data;
//    }
//
//    /* ===================== TEXT BLOCK STRUCTURE ===================== */
//
//    private static class TextBlock {
//        String text;
//        int x, y;
//        int width, height;
//        List<Word> words;
//
//        TextBlock(String text, int x, int y, int width, int height, List<Word> words) {
//            this.text = text;
//            this.x = x;
//            this.y = y;
//            this.width = width;
//            this.height = height;
//            this.words = words;
//        }
//    }
//
//    /**
//     * Agrupa palabras en bloques de texto considerando su posición espacial
//     * Las palabras muy cercanas verticalmente se agrupan en el mismo bloque
//     */
//    private List<TextBlock> createTextBlocks(List<Word> words) {
//        List<TextBlock> blocks = new ArrayList<>();
//
//        List<List<Word>> columns = new ArrayList<>();
//
//        for (Word word : words) {
//            boolean added = false;
//
//            // Intentar agregar a una columna existente (misma región X)
//            for (List<Word> column : columns) {
//                Word firstInColumn = column.get(0);
//                int xDiff = Math.abs(word.getBoundingBox().x - firstInColumn.getBoundingBox().x);
//
//                // Si está en la misma columna (diferencia X pequeña)
//                if (xDiff < 100) {
//                    column.add(word);
//                    added = true;
//                    break;
//                }
//            }
//
//            if (!added) {
//                List<Word> newColumn = new ArrayList<>();
//                newColumn.add(word);
//                columns.add(newColumn);
//            }
//        }
//
//        // Ordenar columnas por X
//        columns.sort(Comparator.comparingInt(col -> col.get(0).getBoundingBox().x));
//
//        // Para cada columna, agrupar palabras verticalmente cercanas
//        for (List<Word> column : columns) {
//            // Ordenar por Y dentro de la columna
//            column.sort(Comparator.comparingInt(w -> w.getBoundingBox().y));
//
//            List<Word> currentGroup = new ArrayList<>();
//
//            for (Word word : column) {
//                if (currentGroup.isEmpty()) {
//                    currentGroup.add(word);
//                } else {
//                    Word lastWord = currentGroup.get(currentGroup.size() - 1);
//                    int yDiff = word.getBoundingBox().y - (lastWord.getBoundingBox().y + lastWord.getBoundingBox().height);
//
//                    // Si está muy cerca verticalmente, es parte del mismo bloque
//                    if (yDiff < VERTICAL_SPACING) {
//                        currentGroup.add(word);
//                    } else {
//                        // Crear bloque con el grupo actual
//                        blocks.add(createBlockFromWords(currentGroup));
//                        currentGroup = new ArrayList<>();
//                        currentGroup.add(word);
//                    }
//                }
//            }
//
//            // Agregar el último grupo
//            if (!currentGroup.isEmpty()) {
//                blocks.add(createBlockFromWords(currentGroup));
//            }
//        }
//
//        // Ordenar bloques por posición (primero Y, luego X)
//        blocks.sort((a, b) -> {
//            int yDiff = a.y - b.y;
//            if (Math.abs(yDiff) < VERTICAL_SPACING) {
//                return a.x - b.x;
//            }
//            return yDiff;
//        });
//
//        return blocks;
//    }
//
//    private TextBlock createBlockFromWords(List<Word> words) {
//        if (words.isEmpty()) return null;
//
//        StringBuilder text = new StringBuilder();
//        int minX = Integer.MAX_VALUE;
//        int minY = Integer.MAX_VALUE;
//        int maxX = 0;
//        int maxY = 0;
//
//        for (Word word : words) {
//            if (text.length() > 0) text.append(" ");
//            text.append(word.getText());
//
//            Rectangle bbox = word.getBoundingBox();
//            minX = Math.min(minX, bbox.x);
//            minY = Math.min(minY, bbox.y);
//            maxX = Math.max(maxX, bbox.x + bbox.width);
//            maxY = Math.max(maxY, bbox.y + bbox.height);
//        }
//
//        return new TextBlock(
//                text.toString(),
//                minX,
//                minY,
//                maxX - minX,
//                maxY - minY,
//                words
//        );
//    }
//
//    /* ===================== VALUE FINDING USING BLOCKS ===================== */
//
//    private String findValueUsingBlocks(String label, List<TextBlock> blocks) {
//        String normalizedLabel = normalize(label);
//        String[] labelParts = label.split("\\s+");
//
//        System.out.println("\n--- Buscando label: '" + label + "' (normalizado: '" + normalizedLabel + "') ---");
//
//        for (int i = 0; i < blocks.size(); i++) {
//            TextBlock block = blocks.get(i);
//            String normalizedBlock = normalize(block.text);
//
//            System.out.printf("  Comparando con bloque %d: '%s' (normalizado: '%s')%n",
//                    i, block.text, normalizedBlock);
//
//            // CASO 1: El label completo está en este bloque
//            if (normalizedBlock.contains(normalizedLabel)) {
//                System.out.println("  ✓ Label encontrado COMPLETO en bloque " + i);
//
//                // Buscar valor en el mismo bloque (horizontal)
//                String value = extractValueFromSameBlock(block, normalizedLabel);
//                if (value != null) {
//                    System.out.println("  ✓ Valor encontrado en MISMO bloque: " + value);
//                    return value;
//                }
//
//                // Buscar en bloques a la derecha (misma altura Y)
//                value = findValueToRight(blocks, i);
//                if (value != null) {
//                    System.out.println("  ✓ Valor encontrado a la DERECHA: " + value);
//                    return value;
//                }
//
//                // Buscar en bloque siguiente (abajo)
//                value = findValueBelow(blocks, i);
//                if (value != null) {
//                    System.out.println("  ✓ Valor encontrado ABAJO: " + value);
//                    return value;
//                }
//            }
//
//            // CASO 2: El label está dividido en varios bloques verticales
//            if (labelParts.length > 1) {
//                String value = tryMatchMultiLineLabel(blocks, i, labelParts);
//                if (value != null) {
//                    System.out.println("  ✓ Label MULTI-LÍNEA encontrado, valor: " + value);
//                    return value;
//                }
//            }
//        }
//
//        System.out.println("  ✗ No se encontró el label\n");
//        return null;
//    }
//
//    /**
//     * Intenta encontrar un label que está dividido en múltiples bloques verticales
//     * Ejemplo: "COLESTEROL" en un bloque, "TOTAL" en el siguiente
//     */
//    private String tryMatchMultiLineLabel(List<TextBlock> blocks, int startIdx, String[] labelParts) {
//        if (startIdx + labelParts.length > blocks.size()) return null;
//
//        List<Integer> matchedIndices = new ArrayList<>();
//
//        // Verificar si los siguientes bloques coinciden con las partes del label
//        for (int i = 0; i < labelParts.length; i++) {
//            if (startIdx + i >= blocks.size()) return null;
//
//            TextBlock block = blocks.get(startIdx + i);
//            String normalizedPart = normalize(labelParts[i]);
//            String normalizedBlock = normalize(block.text);
//
//            // El bloque debe contener esta parte del label
//            if (normalizedBlock.contains(normalizedPart)) {
//                matchedIndices.add(startIdx + i);
//            } else {
//                return null; // No coincide, no es este label
//            }
//        }
//
//        // Si todas las partes coincidieron
//        if (matchedIndices.size() == labelParts.length) {
//            System.out.println("  ✓ Label multi-línea detectado en bloques: " + matchedIndices);
//
//            int lastLabelIdx = matchedIndices.get(matchedIndices.size() - 1);
//            TextBlock lastBlock = blocks.get(lastLabelIdx);
//
//            // Buscar valor a la derecha del último bloque del label
//            String value = findValueToRight(blocks, lastLabelIdx);
//            if (value != null) return value;
//
//            // Buscar valor en el último bloque después del texto del label
//            value = extractValueFromSameBlock(lastBlock, normalize(labelParts[labelParts.length - 1]));
//            if (value != null) return value;
//
//            // Buscar valor en el siguiente bloque (abajo)
//            value = findValueBelow(blocks, lastLabelIdx);
//            if (value != null) return value;
//        }
//
//        return null;
//    }
//
//    /**
//     * Busca un valor numérico en el mismo bloque, después del label
//     */
//    private String extractValueFromSameBlock(TextBlock block, String normalizedLabel) {
//        List<Word> words = block.words;
//
//        // Encontrar dónde termina el label en las palabras
//        boolean labelEnded = false;
//
//        for (int i = 0; i < words.size(); i++) {
//            Word word = words.get(i);
//
//            if (normalize(word.getText()).contains(normalizedLabel.substring(Math.max(0, normalizedLabel.length() - 5)))) {
//                labelEnded = true;
//                continue;
//            }
//
//            if (labelEnded) {
//                String text = word.getText().replace("*", "").trim();
//                if (isNumeric(text)) {
//                    return buildValueWithUnit(words, i);
//                }
//            }
//        }
//
//        return null;
//    }
//
//    /**
//     * Busca un bloque a la derecha con un valor numérico (misma altura Y)
//     */
//    private String findValueToRight(List<TextBlock> blocks, int currentIdx) {
//        TextBlock currentBlock = blocks.get(currentIdx);
//
//        for (int i = currentIdx + 1; i < blocks.size(); i++) {
//            TextBlock candidate = blocks.get(i);
//
//            // Debe estar a la derecha y a una altura similar
//            if (candidate.x > currentBlock.x &&
//                    Math.abs(candidate.y - currentBlock.y) < VERTICAL_SPACING) {
//
//                String value = extractFirstNumericFromBlock(candidate);
//                if (value != null) return value;
//            }
//        }
//
//        return null;
//    }
//
//    /**
//     * Busca un valor en el bloque inmediatamente debajo
//     */
//    private String findValueBelow(List<TextBlock> blocks, int currentIdx) {
//        if (currentIdx + 1 >= blocks.size()) return null;
//
//        TextBlock nextBlock = blocks.get(currentIdx + 1);
//        return extractFirstNumericFromBlock(nextBlock);
//    }
//
//    /**
//     * Extrae el primer valor numérico de un bloque
//     */
//    private String extractFirstNumericFromBlock(TextBlock block) {
//        for (int i = 0; i < block.words.size(); i++) {
//            Word word = block.words.get(i);
//            String text = word.getText().replace("*", "").trim();
//
//            if (isNumeric(text)) {
//                return buildValueWithUnit(block.words, i);
//            }
//        }
//        return null;
//    }
//
//    /**
//     * Construye el valor con su unidad si existe
//     */
//    private String buildValueWithUnit(List<Word> words, int valueIndex) {
//        String value = words.get(valueIndex).getText().replace("*", "");
//
//        // Verificar si el siguiente elemento es una unidad
//        if (valueIndex + 1 < words.size() && isUnit(words.get(valueIndex + 1).getText())) {
//            value += " " + words.get(valueIndex + 1).getText();
//        }
//
//        return clean(value);
//    }
//
//    /* ===================== UTILS ===================== */
//
//    private boolean isValidPage(PDDocument doc, int page) {
//        return page >= 0 && page < doc.getNumberOfPages();
//    }
//
//    private List<BufferedImage> extractPageImages(PDDocument doc, int page) {
//        return sectionImgExtractor.extractImages(
//                doc, new SectionArea(page, 0, page, Float.MAX_VALUE)
//        );
//    }
//
//    private boolean isValidTableImage(BufferedImage img) {
//        return img.getWidth() >= 500 && img.getHeight() >= 200;
//    }
//
//    private boolean isUnit(String text) {
//        return text != null && text.toLowerCase()
//                .matches(".*(g%|%|gr/dl|mg/dl|mm3|mm/h|x|ul|ml|mmhg).*");
//    }
//
//    private String normalize(String text) {
//        if (text == null) return "";
//        return text.toUpperCase()
//                .replaceAll("[.\\s]+", "");
//    }
//
//    private boolean isBlank(String s) {
//        return s == null || s.isBlank();
//    }
//
//    private boolean isNotBlank(String s) {
//        return !isBlank(s);
//    }
//
//    private boolean isValidNumericValue(String token) {
//        if (token == null || token.isEmpty()) return false;
//
//        token = token.replace("*", "").trim();
//        token = token.replaceAll("(mg/dl|g/dl|gr/dl|g%|%|mm3|ul|ml|mmhg)$", "").trim();
//
//        if (!token.matches(".*\\d.*")) return false;
//
//        return token.matches("^[0-9]+([.,\\s][0-9]+)*$");
//    }
//
//    private String clean(String value) {
//        if (value == null) return "";
//
//        return value.trim()
//                .replaceAll("(mm3|x\\s*mm3|mg/dl|g/dl|g%|%|ul|ml|mmhg|gr/dl)$", "")
//                .replaceAll("\\s+", "")
//                .replaceAll("^[^0-9]+", "")
//                .replaceAll("[^0-9.,]+$", "")
//                .trim();
//    }
//
//    private boolean isNumeric(String text) {
//        return isValidNumericValue(text);
//    }
//}

/// version 2 solo falta la extraccion del grupo de colesterol
//package com.example.PruebaPDFBox.utilities.extractor;
//
//import com.example.PruebaPDFBox.record.SectionArea;
//import com.example.PruebaPDFBox.utilities.exception.CustomException;
//import net.sourceforge.tess4j.Tesseract;
//import net.sourceforge.tess4j.Word;
//import org.apache.pdfbox.pdmodel.PDDocument;
//import org.springframework.beans.factory.ObjectProvider;
//import org.springframework.http.HttpStatus;
//import org.springframework.stereotype.Component;
//
//import java.awt.*;
//import java.awt.image.BufferedImage;
//import java.util.*;
//import java.util.List;
//import java.util.stream.Collectors;
//
//@Component
//public class ImageTextExtractor {
//
//    private static final int CONFIDENCE_THRESHOLD = 15;
//    private static final int ROW_THRESHOLD = 15;
//
//    private final ThreadLocal<Tesseract> tesseractThreadLocal;
//    private final SectionImgExtractor sectionImgExtractor;
//
//    public ImageTextExtractor(
//            ObjectProvider<Tesseract> tesseractProvider,
//            SectionImgExtractor sectionImgExtractor
//    ) {
//        // Usar ThreadLocal para reutilizar instancias de Tesseract
//        this.tesseractThreadLocal = ThreadLocal.withInitial(tesseractProvider::getObject);
//        this.sectionImgExtractor = sectionImgExtractor;
//    }
//
//    private Tesseract getTesseract() {
//        return tesseractThreadLocal.get();
//    }
//
//    public Map<String, String> extractTextFromTable(
//            PDDocument document,
//            List<Integer> targetPages,
//            List<String> labels
//    ) {
//
//        Map<String, String> extractedData = new HashMap<>();
//
//        for (int pageNum : targetPages) {
//            if (!isValidPage(document, pageNum)) continue;
//
//            List<BufferedImage> images = extractPageImages(document, pageNum);
//
//            for (BufferedImage image : images) {
//                if (!isValidTableImage(image)) continue;
//
//                BufferedImage optimizedImage = optimizeImageForOCR(image);
//
//                Tesseract tesseract = getTesseract();
//
//                // PROBAR MÚLTIPLES PSMs si el primero falla
//                Map<String, String> data = tryExtractWithMultiplePSM(
//                        optimizedImage, labels, tesseract
//                );
//
//                extractedData.putAll(data);
//            }
//        }
//
//        return extractedData;
//    }
//
//    private Map<String, String> tryExtractWithMultiplePSM(
//            BufferedImage image,
//            List<String> labels,
//            Tesseract tesseract
//    ) {
//        // Configuraciones adicionales para mejorar la detección
//        tesseract.setOcrEngineMode(1); // LSTM OCR Engine mode
//        tesseract.setVariable("tessedit_char_whitelist",
//                "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789.,%-:/()*+ ");
//
//        // Intentar con diferentes modos de segmentación
//        int[] psmModes = {6, 11, 4, 3};
//
//        for (int psm : psmModes) {
//            tesseract.setPageSegMode(psm);
//
//            System.out.println("=== Intentando con PSM: " + psm + " ===");
//
//            Map<String, String> data = extractUsingTextAnalysis(image, labels, tesseract);
//
//            if (data.isEmpty()) {
//                data = extractUsingPositionalAnalysis(image, labels, tesseract);
//            }
//
//            if (!data.isEmpty()) {
//                System.out.println("✓ PSM " + psm + " funcionó, encontrados " + data.size() + " valores");
//                return data;
//            }
//        }
//
//        return new HashMap<>();
//    }
//
//
//    /* ===================== IMAGE OPTIMIZATION ===================== */
//
//    private BufferedImage optimizeImageForOCR(BufferedImage original) {
//        int width = original.getWidth();
//        int height = original.getHeight();
//
//        // Si la imagen es muy pequeña, aumentarla
//        if (width < 1500) {
//            double scale = 1500.0 / width;
//            int targetWidth = (int) (width * scale);
//            int targetHeight = (int) (height * scale);
//
//            BufferedImage scaled = new BufferedImage(targetWidth, targetHeight,
//                    BufferedImage.TYPE_BYTE_GRAY);
//            Graphics2D g = scaled.createGraphics();
//
//            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
//                    RenderingHints.VALUE_INTERPOLATION_BICUBIC);
//
//            g.drawImage(original, 0, 0, targetWidth, targetHeight, null);
//            g.dispose();
//
//            return scaled;
//        }
//
//        // Si ya es grande, solo convertir a escala de grises
//        BufferedImage grayscale = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
//        Graphics2D g = grayscale.createGraphics();
//        g.drawImage(original, 0, 0, null);
//        g.dispose();
//
//        return grayscale;
//    }
//
//    /* ===================== OCR STRATEGIES ===================== */
//
//    private Map<String, String> extractUsingPositionalAnalysis(
//            BufferedImage image,
//            List<String> labels,
//            Tesseract tesseract
//    ) {
//        Map<String, String> data = new HashMap<>();
//
//        try {
//            List<Word> words = tesseract.getWords(image, 0);
//
//            if (words == null || words.isEmpty()) return data;
//
//            // **LOG PARA VER TODAS LAS PALABRAS DETECTADAS**
//            System.out.println("=== PALABRAS DETECTADAS (con confianza) ===");
//            for (Word word : words) {
//                System.out.printf("'%s' (conf: %.1f, x:%d, y:%d)%n",
//                        word.getText(),
//                        word.getConfidence(),
//                        word.getBoundingBox().x,
//                        word.getBoundingBox().y
//                );
//            }
//            System.out.println("==========================================");
//
//            List<Word> validWords = words.stream()
//                    .filter(w -> w.getConfidence() > CONFIDENCE_THRESHOLD)
//                    .toList();
//
//            Map<Integer, List<Word>> rows = organizeWordsIntoRows(validWords);
//
//            for (String label : labels) {
//                String value = findValueInRows(label, rows);
//                if (isNotBlank(value)) {
//                    System.out.println("Label: " + label + " -> Value: " + value); // LOG
//                    data.put(label, value);
//                }
//            }
//
//        } catch (Exception e) {
//            throw new CustomException("Error en analisis posicional: " + e.getMessage(), HttpStatus.BAD_REQUEST);
//        }
//
//        return data;
//    }
//
//    private Map<String, String> extractUsingTextAnalysis(
//            BufferedImage image,
//            List<String> labels,
//            Tesseract tesseract
//    ) {
//        Map<String, String> data = new HashMap<>();
//
//        try {
//            String text = tesseract.doOCR(image);
//
//            // **AGREGAR ESTE LOG PARA VER QUÉ LEE TESSERACT**
//            System.out.println("=== TEXTO COMPLETO EXTRAÍDO POR OCR ===");
//            System.out.println(text);
//            System.out.println("=======================================");
//
//            if (isBlank(text)) return data;
//
//            String[] lines = text.split("\n");
//
//            for (String label : labels) {
//                String value = findValueInLines(label, lines);
//                if (isNotBlank(value)) {
//                    System.out.println("Label: " + label + " -> Value: " + value); // LOG
//                    data.put(label, value);
//                }
//            }
//
//        } catch (Exception e) {
//            throw new CustomException("Error al analizar el texto: " + e.getMessage(), HttpStatus.BAD_REQUEST);
//        }
//
//        return data;
//    }
//
//    /* ===================== ROW / VALUE LOGIC ===================== */
//
//    private Map<Integer, List<Word>> organizeWordsIntoRows(List<Word> words) {
//        Map<Integer, List<Word>> rows = new TreeMap<>();
//
//        for (Word word : words) {
//            int y = word.getBoundingBox().y;
//
//            Integer rowKey = rows.keySet().stream()
//                    .filter(k -> Math.abs(k - y) < ROW_THRESHOLD)
//                    .findFirst()
//                    .orElse(y);
//
//            rows.computeIfAbsent(rowKey, k -> new ArrayList<>()).add(word);
//        }
//
//        rows.values()
//                .forEach(r -> r.sort(Comparator.comparingInt(w -> w.getBoundingBox().x)));
//
//        return rows;
//    }
//
//    private String findValueInRows(String label, Map<Integer, List<Word>> rows) {
//        String normalizedLabel = normalize(label);
//
//        for (List<Word> row : rows.values()) {
//            int labelIndex = findLabelInRow(row, normalizedLabel);
//            if (labelIndex == -1) continue;
//
//            // Buscar el valor INMEDIATAMENTE después del label específico
//            String value = extractNumericValueAfterLabel(row, labelIndex);
//            if (value != null) {
//                // Validar que el valor esté cerca del label (mismo contexto)
//                if (isValueNearLabel(row, labelIndex)) {
//                    return value;
//                }
//            }
//        }
//        return null;
//    }
//
//    private boolean isValueNearLabel(List<Word> row, int labelIndex) {
//        if (labelIndex + 1 >= row.size()) return false;
//
//        int labelX = row.get(labelIndex).getBoundingBox().x;
//        int valueX = row.get(labelIndex + 1).getBoundingBox().x;
//
//        // Si la distancia horizontal es muy grande, probablemente no es el valor correcto
//        return (valueX - labelX) < 300;
//    }
//
//    private int findLabelInRow(List<Word> row, String normalizedLabel) {
//        String rowText = normalize(
//                row.stream().map(Word::getText).collect(Collectors.joining(" "))
//        );
//
//        if (!rowText.contains(normalizedLabel)) return -1;
//
//        // Para labels compuestos (ej: "COLESTEROLHDL"), buscar coincidencia exacta
//        if (normalizedLabel.length() > 10) {
//            for (int i = 0; i < row.size() - 1; i++) {
//                String combined = normalize(row.get(i).getText() + row.get(i + 1).getText());
//                if (combined.equals(normalizedLabel)) {
//                    return i + 1; // Retornar el índice DESPUÉS del label
//                }
//            }
//        }
//
//        // Búsqueda estándar
//        int bestMatch = -1;
//        int longestMatch = 0;
//
//        for (int i = 0; i < row.size(); i++) {
//            String word = normalize(row.get(i).getText());
//
//            if (normalizedLabel.contains(word) && word.length() > longestMatch) {
//                bestMatch = i;
//                longestMatch = word.length();
//            }
//        }
//
//        return bestMatch;
//    }
//
//    private String extractNumericValueAfterLabel(List<Word> row, int index) {
//        // Buscar el primer valor numérico después del label
//        for (int i = index + 1; i < Math.min(index + 4, row.size()); i++) {
//            String text = row.get(i).getText().replace("*", ""); // Quitar asteriscos
//
//            if (isNumeric(text)) {
//                String value = text;
//
//                // Verificar si el siguiente elemento es una unidad
//                if (i + 1 < row.size() && isUnit(row.get(i + 1).getText())) {
//                    value += " " + row.get(i + 1).getText();
//                }
//
//                return clean(value);
//            }
//        }
//        return null;
//    }
//
//    /* ===================== UTILS ===================== */
//
//    private boolean isValidPage(PDDocument doc, int page) {
//        return page >= 0 && page < doc.getNumberOfPages();
//    }
//
//    private List<BufferedImage> extractPageImages(PDDocument doc, int page) {
//        return sectionImgExtractor.extractImages(
//                doc, new SectionArea(page, 0, page, Float.MAX_VALUE)
//        );
//    }
//
//    private boolean isValidTableImage(BufferedImage img) {
//        // Reducir el mínimo ya que ahora podríamos tener páginas completas renderizadas
//        return img.getWidth() >= 500 && img.getHeight() >= 200;
//    }
//
//    private boolean isUnit(String text) {
//        return text != null && text.toLowerCase()
//                .matches(".*(g%|%|gr/dl|mg/dl|mm3|mm/h|x|ul|ml|mmhg).*");
//    }
//
//    private String normalize(String text) {
//        if (text == null) return "";
//
//        // Normalizar: quitar puntos, espacios, y convertir a mayúsculas
//        // "RTO.BLANCOS" → "RTOBLANCOS"
//        // "RTO BLANCOS" → "RTOBLANCOS"
//        return text.toUpperCase()
//                .replaceAll("[.\\s]+", "");
//    }
//
//    private boolean isBlank(String s) {
//        return s == null || s.isBlank();
//    }
//
//    private boolean isNotBlank(String s) {
//        return !isBlank(s);
//    }
//
//    private String findValueInLines(String label, String[] lines) {
//        String normalizedLabel = normalize(label);
//
//        for (int i = 0; i < lines.length; i++) {
//            String normalizedLine = normalize(lines[i]);
//
//            if (!normalizedLine.contains(normalizedLabel)) continue;
//
//            // Encontramos la línea con el label
//            String currentLine = lines[i];
//
//            // Extraer el valor de la línea actual
//            String value = extractValueAfterLabel(currentLine, label);
//            if (value != null) return value;
//
//            // Si no se encuentra, buscar en la siguiente línea
//            if (i + 1 < lines.length) {
//                value = extractFirstNumeric(lines[i + 1]);
//                if (value != null) return value;
//            }
//        }
//        return null;
//    }
//
//    private String extractValueAfterLabel(String line, String label) {
//        int labelPos = line.toUpperCase().indexOf(label.toUpperCase());
//        if (labelPos == -1) return null;
//
//        String afterLabel = line.substring(labelPos + label.length());
//
//        // Dividir en tokens
//        String[] tokens = afterLabel.trim().split("\\s+");
//
//        // Buscar el primer número válido después del label
//        for (int i = 0; i < Math.min(tokens.length, 5); i++) {
//            String token = tokens[i].trim();
//
//            if (token.isEmpty()) continue;
//
//            // Verificar si es numérico válido
//            if (isValidNumericValue(token)) {
//                String cleaned = clean(token);
//
//                // Validación: evitar rangos (números seguidos de guión)
//                if (i + 1 < tokens.length && tokens[i + 1].equals("-")) {
//                    break; // Es un rango de referencia
//                }
//
//                return cleaned;
//            }
//        }
//
//        return null;
//    }
//
//    private boolean isValidNumericValue(String token) {
//        if (token == null || token.isEmpty()) return false;
//
//        token = token.replace("*", "").trim();
//        token = token.replaceAll("(mg/dl|g/dl|gr/dl|g%|%|mm3|ul|ml|mmhg)$", "").trim();
//
//        if (!token.matches(".*\\d.*")) return false;
//
//        // Aceptar números con puntos, comas y espacios (como 150.000 o 150 000)
//        return token.matches("^[0-9]+([.,\\s][0-9]+)*$");
//    }
//
//    private String clean(String value) {
//        if (value == null) return "";
//
//        return value.trim()
//                .replaceAll("(mm3|x\\s*mm3|mg/dl|g/dl|g%|%|ul|ml|mmhg|gr/dl)$", "")
//                .replaceAll("\\s+", "") // Eliminar espacios internos
//                .replaceAll("^[^0-9]+", "")
//                .replaceAll("[^0-9.,]+$", "")
//                .trim();
//    }
//
//    private String extractFirstNumeric(String line) {
//        // Dividir por espacios
//        String[] tokens = line.split("\\s+");
//
//        for (String token : tokens) {
//            if (isValidNumericValue(token)) {
//                return clean(token);
//            }
//        }
//
//        return null;
//    }
//
//    private boolean isNumeric(String text) {
//        return isValidNumericValue(text);
//    }
//}

//// version 1
//package com.example.PruebaPDFBox.utilities.extractor;
//
//import com.example.PruebaPDFBox.record.SectionArea;
//import com.example.PruebaPDFBox.utilities.exception.CustomException;
//import net.sourceforge.tess4j.Tesseract;
//import net.sourceforge.tess4j.Word;
//import org.apache.pdfbox.pdmodel.PDDocument;
//import org.springframework.beans.factory.ObjectProvider;
//import org.springframework.http.HttpStatus;
//import org.springframework.stereotype.Component;
//
//import java.awt.*;
//import java.awt.image.BufferedImage;
//import java.io.IOException;
//import java.util.*;
//import java.util.List;
//import java.util.concurrent.ConcurrentHashMap;
//import java.util.stream.Collectors;
//@Component
//public class ImageTextExtractor {
//
//    private static final int MIN_IMG_WIDTH = 700;
//    private static final int MIN_IMG_HEIGHT = 300;
//    private static final int CONFIDENCE_THRESHOLD = 15;
//    private static final int ROW_THRESHOLD = 15;
//
//    private final ObjectProvider<Tesseract> tesseractProvider;
//    private final SectionImgExtractor sectionImgExtractor;
//
//    public ImageTextExtractor(
//            ObjectProvider<Tesseract> tesseractProvider,
//            SectionImgExtractor sectionImgExtractor
//    ) {
//        this.tesseractProvider = tesseractProvider;
//        this.sectionImgExtractor = sectionImgExtractor;
//    }
//
//
//    private Tesseract getTesseract() {
//        return tesseractProvider.getObject();
//    }
//
//    public Map<String, String> extractTextFromTable(
//            PDDocument document,
//            List<Integer> targetPages,
//            List<String> labels
//    ) {
//        // Cambiar a ConcurrentHashMap para thread-safety
//        Map<String, String> extractedData = new ConcurrentHashMap<>();
//
//        // Procesar páginas en paralelo
//        targetPages.parallelStream().forEach(pageNum -> {
//            if (!isValidPage(document, pageNum)) return;
//
//            try {
//                List<BufferedImage> images = extractPageImages(document, pageNum);
//
//                for (BufferedImage image : images) {
//                    if (!isValidTableImage(image)) continue;
//
//                    // Optimizar imagen antes de OCR
//                    BufferedImage optimizedImage = optimizeImageForOCR(image);
//
//                    Tesseract tesseract = getTesseract();
//
//                    Map<String, String> data =
//                            extractUsingTextAnalysis(image, labels, tesseract);
//
//                    if (data.isEmpty()) {
//                        data = extractUsingPositionalAnalysis(image, labels, tesseract);
//                    }
//
//                    // putAll es thread-safe en ConcurrentHashMap
//                    extractedData.putAll(data);
//                }
//            } catch (IOException e) {
//                // Convertir a RuntimeException para poder lanzarla desde lambda
//                throw new CustomException(
//                        "Error procesando página " + pageNum + ": " + e.getMessage(),
//                        HttpStatus.BAD_REQUEST
//                );
//            }
//        });
//
//        return extractedData;
//    }
//
//
//    private BufferedImage optimizeImageForOCR(BufferedImage original) {
//        int width = original.getWidth();
//        int height = original.getHeight();
//
//        // Tesseract works best at ~300 DPI
//        // If image is too large, scale down
//        if (width > 3000 || height > 3000) {
//            double scale = Math.min(3000.0 / width, 3000.0 / height);
//            int newWidth = (int) (width * scale);
//            int newHeight = (int) (height * scale);
//
//            BufferedImage scaled = new BufferedImage(newWidth, newHeight,
//                    BufferedImage.TYPE_BYTE_GRAY);
//            Graphics2D g = scaled.createGraphics();
//            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
//                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
//            g.drawImage(original, 0, 0, newWidth, newHeight, null);
//            g.dispose();
//            return scaled;
//        }
//
//        return original;
//    }
//
//    /* ===================== OCR STRATEGIES ===================== */
//
//    private Map<String, String> extractUsingPositionalAnalysis(
//            BufferedImage image,
//            List<String> labels,
//            Tesseract tesseract
//    ) {
//        Map<String, String> data = new HashMap<>();
//
//        try {
//            List<Word> words = tesseract.getWords(image, 0);
//
//            if (words == null || words.isEmpty()) return data;
//
//            List<Word> validWords = words.stream()
//                    .filter(w -> w.getConfidence() > CONFIDENCE_THRESHOLD)
//                    .toList();
//
//            Map<Integer, List<Word>> rows = organizeWordsIntoRows(validWords);
//
//            for (String label : labels) {
//                String value = findValueInRows(label, rows);
//                if (isNotBlank(value)) {
//                    data.put(label, value);
//                }
//            }
//
//        } catch (Exception e) {
//            throw new CustomException("Error en analisis posicional" + e.getMessage(), HttpStatus.BAD_REQUEST);
//        }
//
//        return data;
//    }
//
//    private Map<String, String> extractUsingTextAnalysis(
//            BufferedImage image,
//            List<String> labels,
//            Tesseract tesseract
//    ) {
//        Map<String, String> data = new HashMap<>();
//
//        try {
//            String text = tesseract.doOCR(image);
//
//            if (isBlank(text)) return data;
//
//            String[] lines = text.split("\n");
//
//            for (String label : labels) {
//                String value = findValueInLines(label, lines);
//                if (isNotBlank(value)) {
//                    data.put(label, value);
//                }
//            }
//
//        } catch (Exception e) {
//            throw new CustomException("Error al analizar el texto" + e.getMessage(), HttpStatus.BAD_REQUEST);
//        }
//
//        return data;
//    }
//
//    /* ===================== ROW / VALUE LOGIC ===================== */
//
//    private Map<Integer, List<Word>> organizeWordsIntoRows(List<Word> words) {
//        Map<Integer, List<Word>> rows = new TreeMap<>();
//
//        for (Word word : words) {
//            int y = word.getBoundingBox().y;
//
//            Integer rowKey = rows.keySet().stream()
//                    .filter(k -> Math.abs(k - y) < ROW_THRESHOLD)
//                    .findFirst()
//                    .orElse(y);
//
//            rows.computeIfAbsent(rowKey, k -> new ArrayList<>()).add(word);
//        }
//
//        rows.values()
//                .forEach(r -> r.sort(Comparator.comparingInt(w -> w.getBoundingBox().x)));
//
//        return rows;
//    }
//
//    private String findValueInRows(String label, Map<Integer, List<Word>> rows) {
//        String normalizedLabel = normalize(label);
//
//        for (List<Word> row : rows.values()) {
//            int labelIndex = findLabelInRow(row, normalizedLabel);
//            if (labelIndex == -1) continue;
//
//            String value = extractNumericValueAfterLabel(row, labelIndex);
//            if (value != null) return value;
//        }
//        return null;
//    }
//
//    private int findLabelInRow(List<Word> row, String normalizedLabel) {
//        String rowText = normalize(
//                row.stream().map(Word::getText).collect(Collectors.joining(" "))
//        );
//
//        if (!rowText.contains(normalizedLabel)) return -1;
//
//        for (int i = 0; i < row.size(); i++) {
//            String word = normalize(row.get(i).getText());
//            if (normalizedLabel.contains(word) && word.length() > 2) return i;
//        }
//        return -1;
//    }
//
//    private String extractNumericValueAfterLabel(List<Word> row, int index) {
//        for (int i = index + 1; i < row.size(); i++) {
//            String text = row.get(i).getText();
//            if (!isNumeric(text)) continue;
//
//            String value = text;
//            if (i + 1 < row.size() && isUnit(row.get(i + 1).getText())) {
//                value += " " + row.get(i + 1).getText();
//            }
//            return clean(value);
//        }
//        return null;
//    }
//
//    /* ===================== FALLBACK ===================== */
//
//    private String findValueInLines(String label, String[] lines) {
//        String normalizedLabel = normalize(label);
//
//        for (int i = 0; i < lines.length; i++) {
//            if (!normalize(lines[i]).contains(normalizedLabel)) continue;
//
//            String value = extractFirstNumeric(lines[i]);
//            if (value != null) return value;
//
//            if (i + 1 < lines.length) {
//                value = extractFirstNumeric(lines[i + 1]);
//                if (value != null) return value;
//            }
//        }
//        return null;
//    }
//
//    private String extractFirstNumeric(String line) {
//        return Arrays.stream(line.split("\\s+"))
//                .filter(this::isNumeric)
//                .findFirst()
//                .map(this::clean)
//                .orElse(null);
//    }
//
//    /* ===================== UTILS ===================== */
//
//    private boolean isValidPage(PDDocument doc, int page) {
//        return page >= 0 && page < doc.getNumberOfPages();
//    }
//
//    private List<BufferedImage> extractPageImages(PDDocument doc, int page) throws IOException {
//        return sectionImgExtractor.extractImages(
//                doc, new SectionArea(page, 0, page, Float.MAX_VALUE)
//        );
//    }
//
//    private boolean isValidTableImage(BufferedImage img) {
//        return img.getWidth() >= MIN_IMG_WIDTH && img.getHeight() >= MIN_IMG_HEIGHT;
//    }
//
//    private boolean isNumeric(String text) {
//        return text != null && text.matches(".*\\d.*") && text.matches("^[0-9.,\\-]+$");
//    }
//
//    private boolean isUnit(String text) {
//        return text != null && text.toLowerCase()
//                .matches(".*(g%|%|gr/dl|mg/dl|mm3|mm/h|x|ul|ml|mmhg).*");
//    }
//
//    private String clean(String value) {
//        return value.trim()
//                .replaceAll("^[^0-9]+", "")
//                .replaceAll("[^0-9.,\\-/%a-zA-Z]+$", "");
//    }
//
//    private String normalize(String text) {
//        return text == null ? "" : text.toUpperCase().replaceAll("[.\\s]+", "");
//    }
//
//    private boolean isBlank(String s) {
//        return s == null || s.isBlank();
//    }
//
//    private boolean isNotBlank(String s) {
//        return !isBlank(s);
//    }
//}