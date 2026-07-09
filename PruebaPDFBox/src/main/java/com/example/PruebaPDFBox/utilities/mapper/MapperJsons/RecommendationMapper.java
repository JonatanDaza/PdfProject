package com.example.PruebaPDFBox.utilities.mapper.MapperJsons;

import com.example.PruebaPDFBox.dto.DTOJson.RecommendationDto;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class RecommendationMapper extends GenericJsonMapper<RecommendationDto> {

    @Override
    protected RecommendationDto parseLine(String line) {
        RecommendationDto dto = new RecommendationDto();
        dto.setRecommendationText(line);
        return dto;
    }

    @Override
    protected TypeReference<List<RecommendationDto>> getTypeReference() {
        return new TypeReference<>() {};
    }

    @Override
    public String parseToJson(String rawText) {
        if (isNullOrEmpty(rawText)) return "[]";

        String cleaned = cleanRaw(rawText);

        String combinedText = Arrays.stream(cleaned.split("\\r?\\n"))
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .filter(line -> !line.matches("(?i)RECOMENDACION|OBSERVACION"))
                .collect(Collectors.joining(", \n"));

        if (combinedText.isEmpty()) return "[]";

        RecommendationDto dto = new RecommendationDto();
        dto.setRecommendationText(combinedText);

        return toJson(List.of(dto));
    }

    private String cleanRaw(String rawText) {
        return rawText
                .replaceAll("(?i)OBSERVACIONES.*$(?s)", "") // corta en "Observaciones"
                .replaceAll("(?i)FONOAUDI[ÓO]LOGO.*", "")
                .replaceAll("[\\[\\]()|]", "")
                .replaceAll("[ \\t]{2,}", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .replaceAll("ANO", "AÑO")
                .trim();
    }
}
