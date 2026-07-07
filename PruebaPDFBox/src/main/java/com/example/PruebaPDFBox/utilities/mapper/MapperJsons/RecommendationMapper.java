package com.example.PruebaPDFBox.utilities.mapper.MapperJsons;

import com.example.PruebaPDFBox.dto.DTOJson.RecommendationDto;
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

        String cleaned = cleanRaw(rawText)
                .replaceAll("(?i)^RECOMENDACION\\s*OBSERVACION", "")
                .replaceAll("(?i)^OBSERVACION", "")
                .replaceAll("[\\[\\]()]", "");

        String combinedText = Arrays.stream(cleaned.split("\\r?\\n"))
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .filter(line -> !line.matches("(?i)RECOMENDACION|OBSERVACION"))
                .collect(Collectors.joining(" / "));

        if (combinedText.isEmpty()) return "[]";

        RecommendationDto dto = new RecommendationDto();
        dto.setRecommendationText(combinedText);

        return toJson(List.of(dto));
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

    private String cleanRaw(String rawText) {
        return rawText
                .replaceAll("(?m)^\\s*[Oo0a]{1,3}\\s*$", "")   // ruido OCR
                .replaceAll("[\\[\\]()|]", "")                // símbolos basura
                .replaceAll("\\s{2,}", " ")                   // espacios múltiples
                .replaceAll("\\n{2,}", "\n")
                .trim();
    }
}
