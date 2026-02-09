package com.example.PruebaPDFBox.utilities.mapper.MapperJsons;

import com.example.PruebaPDFBox.dto.DTOJson.WeightedDecibelsDto;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class WeightedDecibelsMapper extends GenericJsonMapper<WeightedDecibelsDto> {

    private static final List<String> FREQUENCIES = Arrays.asList("250", "500", "1000", "2000", "3000", "4000", "6000", "8000");

    @Override
    public String parseToJson(String rawText) {
        if (isNullOrEmpty(rawText)) return "[]";

        WeightedDecibelsDto dto = parseWeightedDecibels(rawText);

        if (dto != null) {
            List<WeightedDecibelsDto> result = new ArrayList<>();
            result.add(dto);
            return toJson(result);
        }

        return "[]";
    }

    private WeightedDecibelsDto parseWeightedDecibels(String rawText) {
        String upperText = rawText.toUpperCase();

        // Buscar las posiciones de VAOD y VAOI
        int vaodIndex = upperText.indexOf("VAOD");
        int vaoiIndex = upperText.indexOf("VAOI");

        if (vaodIndex == -1 || vaoiIndex == -1) {
            return null;
        }

        // Extraer los valores de VAOD (entre VAOD y VAOI)
        String vaodSection = rawText.substring(vaodIndex + 4, vaoiIndex).trim();
        List<String> vaodValues = extractNumericValues(vaodSection);

        // Extraer los valores de VAOI (después de VAOI)
        String vaoiSection = rawText.substring(vaoiIndex + 4).trim();
        List<String> vaoiValues = extractNumericValues(vaoiSection);

        // Verificar que tengamos exactamente 8 valores para cada uno
        if (vaodValues.size() != 8 || vaoiValues.size() != 8) {
            return null;
        }

        // Crear el DTO y asignar valores por frecuencia
        WeightedDecibelsDto dto = new WeightedDecibelsDto();

        dto.setVAOD250(vaodValues.get(0));
        dto.setVAOI250(vaoiValues.get(0));

        dto.setVAOD500(vaodValues.get(1));
        dto.setVAOI500(vaoiValues.get(1));

        dto.setVAOD1000(vaodValues.get(2));
        dto.setVAOI1000(vaoiValues.get(2));

        dto.setVAOD2000(vaodValues.get(3));
        dto.setVAOI2000(vaoiValues.get(3));

        dto.setVAOD3000(vaodValues.get(4));
        dto.setVAOI3000(vaoiValues.get(4));

        dto.setVAOD4000(vaodValues.get(5));
        dto.setVAOI4000(vaoiValues.get(5));

        dto.setVAOD6000(vaodValues.get(6));
        dto.setVAOI6000(vaoiValues.get(6));

        dto.setVAOD8000(vaodValues.get(7));
        dto.setVAOI8000(vaoiValues.get(7));

        return dto;
    }

    private List<String> extractNumericValues(String text) {
        if (text == null || text.trim().isEmpty()) {
            return new ArrayList<>();
        }

        return Arrays.stream(text.split("\\s+"))
            .map(String::trim)
            .filter(s -> !s.isEmpty() && s.matches("\\d+"))
            .filter(s -> !FREQUENCIES.contains(s)) // Excluir frecuencias
            .collect(Collectors.toList());
    }

    @Override
    protected WeightedDecibelsDto parseLine(String line) {
        return null;
    }

    @Override
    protected TypeReference<List<WeightedDecibelsDto>> getTypeReference() {
        return new TypeReference<>() {};
    }
}
