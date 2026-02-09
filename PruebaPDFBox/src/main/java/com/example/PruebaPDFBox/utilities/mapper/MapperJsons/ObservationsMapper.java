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

        String combinedText = Arrays.stream(rawText.split("\\r?\\n"))
            .map(String::trim)
            .filter(line -> !line.isEmpty())
            .collect(Collectors.joining(" / "));

        if (combinedText.isEmpty()) {
            return "[]";
        }

        // Crear un solo objeto con todas las observaciones separadas
        ObservationsDto dto = new ObservationsDto();
        dto.setObservationText(combinedText);

        List<ObservationsDto> result = new ArrayList<>();
        result.add(dto);

        return toJson(result);
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
}
