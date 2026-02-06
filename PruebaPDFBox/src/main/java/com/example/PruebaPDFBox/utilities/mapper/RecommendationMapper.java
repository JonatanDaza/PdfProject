package com.example.PruebaPDFBox.utilities.mapper;

import com.example.PruebaPDFBox.dto.RecommendationDto;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class RecommendationMapper extends GenericJsonMapper<RecommendationDto> {

    @Override
    public String parseToJson(String rawText) {
        if (isNullOrEmpty(rawText)) return "[]";

        // Separar por líneas y unir con " / "
        String combinedText = Arrays.stream(rawText.split("\\r?\\n"))
            .map(String::trim)
            .filter(line -> !line.isEmpty())
            .collect(Collectors.joining(" / "));

        if (combinedText.isEmpty()) {
            return "[]";
        }

        // Crear un solo objeto con todas las recomendaciones separadas
        RecommendationDto dto = new RecommendationDto();
        dto.setRecommendationText(combinedText);

        List<RecommendationDto> result = new ArrayList<>();
        result.add(dto);

        return toJson(result);
    }

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
}
