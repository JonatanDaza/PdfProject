package com.example.PruebaPDFBox.utilities.mapper.MapperJsons;

import com.example.PruebaPDFBox.dto.DTOJson.ObservationsDto;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ObservationsMapper extends GenericJsonMapper<ObservationsDto> {

    @Override
    public String parseToJson(String rawText) {
        if (isNullOrEmpty(rawText)) return "[]";

        String cleaned = cleanRaw(rawText);

        List<String> lines = Arrays.stream(cleaned.split("\\r?\\n"))
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .filter(line -> !isOcrNoise(line))
                .filter(line -> line.replaceAll("[^A-Za-zÁÉÍÓÚáéíóúÑñ]", "").length() >= 5)
                .toList();

        // Deduplicar ANTES de fixOcrErrors — compara texto crudo normalizado
        List<String> deduped = new ArrayList<>();
        for (String line : lines) {
            String normCurr = line.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
            boolean isDuplicate = deduped.stream().anyMatch(prev -> {
                String normPrev = prev.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
                return normPrev.equals(normCurr)
                        || normPrev.contains(normCurr)
                        || normCurr.contains(normPrev);
            });
            if (!isDuplicate) deduped.add(line);
        }

        // Aplica correcciones OCR DESPUÉS de deduplicar
        List<String> fixed = deduped.stream()
                .map(this::fixOcrErrors)
                .toList();

        String combinedText = String.join(" \n", fixed);
        if (combinedText.isEmpty()) return "[]";

        ObservationsDto dto = new ObservationsDto();
        dto.setObservationText(combinedText);
        return toJson(List.of(dto));
    }

    @Override
    protected ObservationsDto parseLine(String line) {
        ObservationsDto dto = new ObservationsDto();
        dto.setObservationText(line);
        return dto;
    }

    @Override
    protected TypeReference<List<ObservationsDto>> getTypeReference() {
        return new TypeReference<>() {};
    }

    private boolean isOcrNoise(String line) {

    String clean = line
            .replaceAll("[^A-Za-zÁÉÍÓÚáéíóúÑñ]", "")
            .toLowerCase();

        if (clean.isEmpty()) {
            return true;
        }

        // líneas OCR basura
        List<String> noise = List.of(
                "oo",
                "ooo",
                "ee",
                "eee",
                "eeo",
                "ob",
                "bn",
                "a",
                "e"
        );

        if (noise.contains(clean)) {
            return true;
        }

        // muchas letras repetidas: eeee, oooo, bbbb
        if (clean.matches("(.)\\1{2,}")) {
            return true;
        }

        // demasiadas letras pequeñas sin palabras reales
        return clean.length() <= 3;
    }

    private String cleanRaw(String rawText) {
        return rawText
                .replaceAll("(?i)FONOAUDI[ÓO]LOGO.*", "")
                .replaceAll("(?i)FIRMA.*", "")
                .replaceAll("(?i)CODIGO DE SEGURIDAD.*", "")
                .replaceAll("[\\[\\]()|]", "")
                .replaceAll("[ \\t]{2,}", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }

    private String fixOcrErrors(String text) {

        return text
                .replace("RIL ATERAIL", "BILATERAL")
                .replace("RILATERAL", "BILATERAL")
                .replace("AUDICION", "AUDICIÓN")
                .replace("PARAMETROS", "PARÁMETROS")
                .replace("CONSERVACION", "CONSERVACIÓN")
                .replace("SE BRINDAN", "\nSE BRINDAN")
                .replace("EN CASO DE", "\nEN CASO DE")
                .replace("REPOSO AUDITIVO", "\nREPOSO AUDITIVO")
                .replace("CONTROL PERIODICO", "\nCONTROL PERIÓDICO");
    }
}
