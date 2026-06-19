package com.example.PruebaPDFBox.utilities.mapper.MapperJsons;

import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class WeightedDecibelsMapper extends GenericJsonMapper<Map<String, String>> {

    private static final List<String> FREQUENCIES = Arrays.asList("250", "500", "1000", "2000", "3000", "4000", "6000", "8000");

    @Override
    public String parseToJson(String rawText) {
        if (isNullOrEmpty(rawText)) return "[]";

        List<Map<String, String>> result = parseWeightedDecibels(rawText);

        return result != null ? toJson(result) : "[]";
    }

    private List<Map<String, String>> parseWeightedDecibels(String rawText) {
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

        Map<String, String> vaoiMap = new LinkedHashMap<>();
        Map<String, String> vaodMap = new LinkedHashMap<>();

        for (int i = 0; i < FREQUENCIES.size(); i++) {
            String freq = FREQUENCIES.get(i);
            vaoiMap.put("VAOI" + freq, vaoiValues.get(i));
            vaodMap.put("VAOD" + freq, vaodValues.get(i));
        }

        List<Map<String, String>> result = new ArrayList<>();
        result.add(vaoiMap);
        result.add(vaodMap);

        return result;
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
    protected Map<String, String> parseLine(String line) {
        return null;
    }

    @Override
    protected TypeReference<List<Map<String, String>>> getTypeReference() {
        return new TypeReference<>() {};
    }
}