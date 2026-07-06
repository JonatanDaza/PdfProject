package com.example.PruebaPDFBox.utilities.extractor.Auxiliaries;

import org.apache.pdfbox.pdmodel.PDDocument;

import java.awt.image.BufferedImage;
import java.util.*;

public class ImageTextExtractorAuxiliary {

    public boolean isValidPage(PDDocument doc, int page) {
        return page >= 0 && page < doc.getNumberOfPages();
    }

    public List<BufferedImage> splitIntoStrips(BufferedImage image, int numStrips, int overlap) {
        List<BufferedImage> strips = new ArrayList<>();
        int stripHeight = image.getHeight() / numStrips;
        for (int i = 0; i < numStrips; i++) {
            int y = Math.max(0, i * stripHeight - overlap);
            int end = Math.min(image.getHeight(), (i + 1) * stripHeight + overlap
            );
            strips.add(image.getSubimage(0, y, image.getWidth(), end - y)
            );        
        }
        return strips;
    }

    public void logExtractedText(String text) {
        System.out.println("=== TEXTO EXTRAÍDO POR OCR ===\n" + text + "\n==============================");
    }

    public Map<String, String> extractDataFromText(List<String> labels, String[] lines) {
        Map<String, String> data = new HashMap<>();
        for (String label : labels) {
            String value = findValueForLabelInLines(label, lines);
            if (isNotBlank(value)) data.put(label, value);
        }
        return data;
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
                if (i + 1 < tokens.length && tokens[i + 1].equals("-")) break;
                return clean(token);
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

    private String normalize(String text) {
        if (text == null) return "";
        return text.toUpperCase()
            .replaceAll("[.\\s]+", "")
            .trim();
    }

    public boolean isBlank(String s) { return s == null || s.isBlank(); }
    public boolean isNotBlank(String s) { return !isBlank(s); }

    private boolean isValidNumericValue(String token) {
        if (token == null || token.isEmpty()) return false;
        token = token.replace("*", "").trim();
        token = token
            .replaceAll("(mg/dl|g/dl|gr/dl|g%|%|mm3|ul|ml|mmhg)$", "")
            .trim();
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
}