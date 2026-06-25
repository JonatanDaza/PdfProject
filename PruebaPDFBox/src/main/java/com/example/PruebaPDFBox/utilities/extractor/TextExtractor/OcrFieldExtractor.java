package com.example.PruebaPDFBox.utilities.extractor.TextExtractor;

import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.*;

@Component
public class OcrFieldExtractor {

    public String extractAfterLabel(String text, String label) {
        if (text == null || label == null) return "";

        String escapedLabel = Pattern.quote(label);

        // Captura hasta el siguiente label conocido (dos puntos seguido de texto)
        // o hasta 80 caracteres máximo para valores de campo simples
        Pattern pattern = Pattern.compile(
                escapedLabel + "\\s*([^\\n]{1,120})(?:\\n|$)",
                Pattern.CASE_INSENSITIVE
        );
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            String value = matcher.group(1).trim();
            // Corta si encuentra otro "Label:" dentro del valor capturado
            int nextLabelIdx = value.indexOf(":");
            if (nextLabelIdx > 0 && nextLabelIdx < value.length() - 1) {
                // Verifica que no sea parte de un valor (ej: "119/78")
                String beforeColon = value.substring(0, nextLabelIdx).trim();
                if (!beforeColon.isEmpty() && !beforeColon.matches(".*\\d.*")) {
                    value = beforeColon;
                }
            }
            return value.trim();
        }
        return "";
    }

    public String extractBetweenLabels(String text, String startLabel, String endLabel) {
        if (text == null) return "";

        String upper = text.toUpperCase();
        String upperStart = startLabel.toUpperCase();
        String upperEnd = endLabel.toUpperCase();

        int startIdx = upper.indexOf(upperStart);
        if (startIdx == -1) return "";

        // Avanzar al final del label de inicio
        startIdx += upperStart.length();

        int endIdx = upper.indexOf(upperEnd, startIdx);
        String section = endIdx == -1
                ? text.substring(startIdx)
                : text.substring(startIdx, endIdx);

        return section.trim();
    }

    public String extractSubSection(String text, String subTitle, Set<String> nextTitles) {
        if (text == null) return "";

        String upper = text.toUpperCase();
        String upperSub = subTitle.toUpperCase();

        int startIdx = upper.indexOf(upperSub);
        if (startIdx == -1) return "";

        startIdx += upperSub.length();

        // Buscar cuál de los nextTitles aparece primero
        int endIdx = -1;
        for (String next : nextTitles) {
            int idx = upper.indexOf(next.toUpperCase(), startIdx);
            if (idx != -1 && (endIdx == -1 || idx < endIdx)) {
                endIdx = idx;
            }
        }

        String result = endIdx == -1
                ? text.substring(startIdx)
                : text.substring(startIdx, endIdx);

        return result.trim();
    }

    // ── Utilidades de limpieza ──────────────────────────────────────────────

    public String extractOnlyNumbers(String value) {
        if (value == null || value.isEmpty()) return "";
        return value.replaceAll("[^0-9.,]", "").trim();
    }

    public String extractFirstNumber(String value) {
        if (value == null || value.isEmpty()) return "";
        Matcher matcher = Pattern.compile("\\d+").matcher(value);
        return matcher.find() ? matcher.group() : "";
    }

    public String extractDate(String value) {
        if (value == null || value.isEmpty()) return "";

        // Formato dd/MM/yyyy
        Matcher m1 = Pattern.compile("\\d{2}/\\d{2}/\\d{4}").matcher(value);
        if (m1.find()) return m1.group();

        // Formato "12 de enero de 2024"
        return extractLongFormatDate(value);
    }

    private String extractLongFormatDate(String value) {
        Map<String, String> months = Map.ofEntries(
                Map.entry("enero","01"), Map.entry("febrero","02"), Map.entry("marzo","03"),
                Map.entry("abril","04"), Map.entry("mayo","05"), Map.entry("junio","06"),
                Map.entry("julio","07"), Map.entry("agosto","08"), Map.entry("septiembre","09"),
                Map.entry("octubre","10"), Map.entry("noviembre","11"), Map.entry("diciembre","12")
        );

        Pattern p = Pattern.compile(
                "(?:\\w+,\\s*)?(\\d{1,2})\\s+de\\s+(\\w+)\\s+de\\s+(\\d{4})",
                Pattern.CASE_INSENSITIVE
        );
        Matcher m = p.matcher(value.toLowerCase());
        if (m.find()) {
            String day   = m.group(1).length() == 1 ? "0" + m.group(1) : m.group(1);
            String month = months.get(m.group(2).toLowerCase());
            String year  = m.group(3);
            if (month != null) return day + "/" + month + "/" + year;
        }
        return "";
    }

    /** Normaliza texto OCR: elimina tildes, mayúsculas, espacios extra */
    public String normalize(String text) {
        if (text == null) return "";
        return text.toUpperCase()
                .replace("Á","A").replace("É","E").replace("Í","I")
                .replace("Ó","O").replace("Ú","U").replace("Ü","U")
                .replace("Ñ","N")
                .replaceAll("[^A-Z0-9\\s:/.-]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
}