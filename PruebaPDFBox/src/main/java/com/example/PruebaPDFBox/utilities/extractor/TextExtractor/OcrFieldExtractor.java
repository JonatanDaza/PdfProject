package com.example.PruebaPDFBox.utilities.extractor.TextExtractor;

import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.*;

@Component
public class OcrFieldExtractor {

    public String extractAfterLabel(String text, String label) {

        if (text == null || label == null)
            return "";
        
        int start = text.toUpperCase().indexOf(label.toUpperCase());
        
        if (start == -1)
            return "";

        start += label.length();

        String rest = text.substring(start);
        Pattern nextLabel = Pattern.compile("\\s+[A-Za-zÁÉÍÓÚÑÜ()/. ]{2,50}:");
        Matcher matcher = nextLabel.matcher(rest);

        if (matcher.find()) {
            return rest.substring(0, matcher.start()).trim();
        }

        return rest.trim();
    }

    public String extractBetweenLabels(String text, String startLabel, String endLabel) {
        if (text == null) return "";

        String upper = text.toUpperCase();
        String upperStart = startLabel.toUpperCase();
        String upperEnd = endLabel.toUpperCase();

        int startIdx = upper.indexOf(upperStart);
        if (startIdx == -1) return "";

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

    public String extractRowAnswer(String text, String rowLabel) {

        if (text == null || rowLabel == null)
            return "";

        Pattern pattern = Pattern.compile(
                Pattern.quote(rowLabel) + ".*?\\b(SI|NO)\\b",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

        Matcher matcher = pattern.matcher(text);

        if (matcher.find()) {
            return matcher.group(1).toUpperCase();
        }

        return "";
    }

    public String extractExposureTime(String text) {
        if (text == null) return "";

        // Patrón 1: "1 año(s) 0 mes(es)" explícito
        Matcher m1 = Pattern.compile("(\\d+)\\s*[Aa][Ññ][Oo][Ss]?\\s+(\\d+)\\s*[Mm][Ee][Ss]").matcher(text);
        if (m1.find()) return m1.group(1) + " años " + m1.group(2) + " meses";

        // Patrón 2: columnas de tabla "| 1 | 0" al final de línea (años | meses)
        Matcher m2 = Pattern.compile("[|\\s]+(\\d+)\\s*[|]\\s*(\\d+)\\s*$", Pattern.MULTILINE).matcher(text);
        if (m2.find()) return m2.group(1) + " años " + m2.group(2) + " meses";

        // Patrón 3: dos números al final de línea "1 0"
        Matcher m3 = Pattern.compile("(\\d+)\\s+(\\d+)\\s*$", Pattern.MULTILINE).matcher(text);
        if (m3.find()) return m3.group(1) + " años " + m3.group(2) + " meses";

        return "";
    }

    public String extractOccupationalConcept(String text){

        if(text==null)
            return "";

        Matcher m = Pattern.compile(
                "\\b(APTO|NO APTO|APLAZADO|REUBICAR|REUBICACION)\\b",
                Pattern.CASE_INSENSITIVE)
                .matcher(text);

        if(m.find())
            return m.group(1).toUpperCase();

        return "";
    }

    public String extractDiagnosisCode(String text){

        Matcher m = Pattern.compile("\\b([A-Z]\\d{3})\\b")
                .matcher(text);

        return m.find()
                ? m.group(1)
                : "";
    }

    public String extractDiagnosisName(String text){

        Matcher m = Pattern.compile(
                "[A-Z]\\d{3}\\s+([A-ZÁÉÍÓÚÑ ]+)",
                Pattern.CASE_INSENSITIVE)
                .matcher(text);

        if(!m.find())
            return "";

        String diagnosis = m.group(1);

        diagnosis = diagnosis.replaceAll(
                "\\b(COMUN|COMÚN|ORIGEN|TIPO|IMPRESION|IMPRESIÓN|DIAGNOSTICA|DIAGNÓSTICA).*",
                "");

        return diagnosis.trim();
    }

    public String extractHealthRequirements(String text) {

        if (text == null || text.isBlank()) {
            return "";
        }

        // Normalizar espacios
        String normalized = text.replaceAll("[\\r\\n]+", " ")
                                .replaceAll("\\s+", " ")
                                .trim();

        // Caso: NO SE EVALUARÓN REQUISITOS DE SALUD (...) N/A
        Pattern p = Pattern.compile(
                "(NO SE EVALUAR[ÓO]N REQUISITOS DE SALUD.*?\\))(?:\\s+)(N/?A)",
                Pattern.CASE_INSENSITIVE);

        Matcher m = p.matcher(normalized);

        if (m.find()) {
            return m.group(1).trim() + ", Cumple: " + m.group(2).toUpperCase();
        }

        // Si no encontró el caso anterior, intenta entre etiquetas
        String between = extractBetweenLabels(
                text,
                "Cumple",
                "Restricciones Laborales");

        if (!between.isBlank()) {
            return between.replaceAll("\\s+", " ").trim();
        }

        // Solo como último recurso
        if (normalized.toUpperCase().contains("SIN REQUISITOS DE SALUD")) {
            return "SIN REQUISITOS DE SALUD";
        }

        return normalized;
    }

    public String extractNumberAfterLabel(String text, String label) {

        Pattern pattern = Pattern.compile(
                Pattern.quote(label) + "\\s*(\\d+(?:[.,]\\d+)?)",
                Pattern.CASE_INSENSITIVE);

        Matcher matcher = pattern.matcher(text);

        return matcher.find() ? matcher.group(1) : "";
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

        Matcher m1 = Pattern.compile("\\d{2}/\\d{2}/\\d{4}").matcher(value);
        if (m1.find()) return m1.group();

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

    public String normalizeForExtraction(String text) {
        if (text == null) return "";
        return text
                .replaceAll("(?m)^\\s*[oO0]\\s*$\\n?", "")
                .replaceAll("(?m)^[a-zA-Z]\\s+(?=[A-ZÁÉÍÓÚÑ])", "")
                .replaceAll("[ \\t]{2,}", " ")
                .replaceAll("\\r\\n", "\n")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }
}