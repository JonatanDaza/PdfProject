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
    protected ObservationsDto parseLine(String line) {
        ObservationsDto dto = new ObservationsDto();
        dto.setObservationText(line);
        return dto;
    }

    @Override
    protected TypeReference<List<ObservationsDto>> getTypeReference() {
        return new TypeReference<>() {};
    }


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

        // Deduplica ANTES de fixOcrErrors (texto crudo)
        List<String> deduped = deduplicate(lines);

        // Aplica correcciones OCR
        List<String> fixed = deduped.stream()
                .map(this::fixOcrErrors)
                .toList();

        // Deduplica DE NUEVO después de fixOcrErrors
        // (líneas distintas antes pueden ser iguales después de corregir tildes/errores)
        List<String> dedupedFixed = deduplicate(fixed);

        String combinedText = String.join(" \n", dedupedFixed);
        if (combinedText.isEmpty()) return "[]";

        ObservationsDto dto = new ObservationsDto();
        dto.setObservationText(combinedText);
        return toJson(List.of(dto));
    }

    private List<String> deduplicate(List<String> lines) {
        List<String> result = new ArrayList<>();
        for (String line : lines) {
            String normCurr = normalize(line);
            boolean isDuplicate = result.stream()
                    .anyMatch(prev -> {
                        String normPrev = normalize(prev);
                        return normPrev.equals(normCurr)
                                || normPrev.contains(normCurr)
                                || normCurr.contains(normPrev);
                    });
            if (!isDuplicate) result.add(line);
        }
        return result;
    }

    private String normalize(String text) {
        return text.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
    }

    private boolean isOcrNoise(String line) {
        String clean = line.replaceAll("[^A-Za-zÁÉÍÓÚáéíóúÑñ]", "").toLowerCase();

        if (clean.isEmpty()) return true;

        List<String> noise = List.of(
                "oo", "ooo", "oooo", "ee", "eee", "eeo", "ob", "bn",
                "a", "e", "i", "le", "oetho", "oethe", "oethe"
        );
        if (noise.contains(clean)) return true;

        if (clean.matches("(.)\\1{2,}")) return true;

        // Detecta basura tipo "OE THe" — muy pocas letras reales sin sentido
        if (clean.length() <= 5 && !clean.matches(".*(normal|bilateral|auditiv|umbrales).*")) return true;

        return false;
    }

    private String cleanRaw(String rawText) {
        return rawText
                .replaceAll("(?i)FONOAUDI[ÓO]LOGO.*", "")
                .replaceAll("(?i)FIRMA.*", "")
                .replaceAll("(?i)CODIGO DE SEGURIDAD.*", "")
                .replaceAll("(?i)IMPRESO EL.*", "")
                .replaceAll("(?i)R\\.M\\..*", "")
                .replaceAll("(?m)^\\s*[oe_\\-]{1,6}\\s*$\\n?", "")
                .replaceAll("(?m)^\\s*oo\\s*oo.*$\\n?", "")
                .replaceAll("[\\[\\]()|]", "")
                .replaceAll("[ \\t]{2,}", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }

    private String fixOcrErrors(String text) {
        return text
                .replaceAll("(?i)R[IL]{1,2}\\s*ATERAIL", "BILATERAL")
                .replaceAll("(?i)RILATERAL", "BILATERAL")
                .replaceAll("(?i)\\bDTA\\b", "PTA")
                .replace("AUDICION", "AUDICIÓN")
                .replace("PARAMETROS", "PARÁMETROS")
                .replace("CONSERVACION", "CONSERVACIÓN")
                .replace("PERIODICO", "PERIÓDICO")
                .replace("SE BRINDAN", "\nSE BRINDAN")
                .replace("EN CASO DE", "\nEN CASO DE")
                .replace("REPOSO AUDITIVO", "\nREPOSO AUDITIVO")
                .replace("CONTROL PERIODICO", "\nCONTROL PERIÓDICO")
                .replace("CONTROL PERIÓDICO", "\nCONTROL PERIÓDICO");
    }
}
