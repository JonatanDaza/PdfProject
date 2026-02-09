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
        if (isNullOrEmpty(rawText)) return "[]";

        // Separar por líneas y unir con " / "
        String combinedText = Arrays.stream(rawText.split("\\r?\\n"))
            .map(String::trim)
            .filter(line -> !line.isEmpty())
            .collect(Collectors.joining(" / "));

        if (combinedText.isEmpty()) {
            return "[]";
        }

        // Crear un solo objeto con todas las recomendaciones de conducta separadas
        RecommendationsConductDto dto = new RecommendationsConductDto();
        dto.setRecommendationConductText(combinedText);

        List<RecommendationsConductDto> result = new ArrayList<>();
        result.add(dto);

        return toJson(result);
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
