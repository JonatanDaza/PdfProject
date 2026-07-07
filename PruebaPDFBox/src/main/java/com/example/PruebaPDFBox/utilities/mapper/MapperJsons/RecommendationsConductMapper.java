package com.example.PruebaPDFBox.utilities.mapper.MapperJsons;

import com.example.PruebaPDFBox.dto.DTOJson.RecommendationsConductDto;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class RecommendationsConductMapper extends GenericJsonMapper<RecommendationsConductDto> {

    @Override
    public String parseToJson(String rawText) {
        if (isNullOrEmpty(rawText)) {
            return "[]";
        }

        List<String> recommendations = Arrays.stream(rawText.split("\\r?\\n"))
                .map(String::trim)

                // eliminar caracteres basura al inicio
                .map(line -> line.replaceFirst("^[\\[\\]{}()|_\\-*•]+\\s*", ""))

                // quitar espacios repetidos
                .map(line -> line.replaceAll("\\s{2,}", " ").trim())

                // eliminar líneas vacías
                .filter(line -> !line.isBlank())

                // eliminar líneas sin letras
                .filter(line -> line.matches(".*[A-Za-zÁÉÍÓÚáéíóúÑñ].*"))

                // eliminar líneas de solo símbolos/números
                .filter(line -> !line.matches("^[\\d\\W_]+$"))

                // exigir contenido alfabético real
                .filter(line ->
                        line.replaceAll("[^A-Za-zÁÉÍÓÚáéíóúÑñ]", "").length() >= 4
                )

                // eliminar líneas muy cortas
                .filter(line -> line.length() > 4)

                // eliminar duplicados ignorando mayúsculas y tildes
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(
                                this::normalize,
                                line -> line,
                                (a, b) -> a
                        ),
                        map -> new ArrayList<>(map.values())
                ));

        if (recommendations.isEmpty()) {
            return "[]";
        }

        RecommendationsConductDto dto = new RecommendationsConductDto();
        dto.setRecommendationConductText(String.join(", \n", recommendations));

        return toJson(List.of(dto));
    }

    private String normalize(String text) {
        return java.text.Normalizer.normalize(text, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toUpperCase()
                .trim();
    }

    @Override
    protected RecommendationsConductDto parseLine(String line) {
        RecommendationsConductDto dto = new RecommendationsConductDto();
        dto.setRecommendationConductText(line);
        return dto;
    }

    @Override
    protected TypeReference<List<RecommendationsConductDto>> getTypeReference() {
        return new TypeReference<>() {};
    }
}
