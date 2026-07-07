package com.example.PruebaPDFBox.utilities.mapper.MapperJsons;

import com.fasterxml.jackson.core.type.TypeReference;

import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class WeightedDecibelsMapper extends GenericJsonMapper<Map<String, String>> {

    public static final List<String> FREQUENCIES = Arrays.asList("250", "500", "1000", "2000", "3000", "4000", "6000", "8000");

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
            return buildFallback(rawText);
        }

        // Extraer los valores de VAOD (entre VAOD y VAOI)
        String vaodSection = rawText.substring(vaodIndex + 4, vaoiIndex).trim();
        List<String> vaodValues = extractNumericValues(vaodSection);

        // Extraer los valores de VAOI (después de VAOI)
        String vaoiSection = rawText.substring(vaoiIndex + 4).trim();
        List<String> vaoiValues = extractNumericValues(vaoiSection);

        // Verificar que tengamos exactamente 8 valores para cada uno
        if (vaodValues.size() < 8 || vaoiValues.size() < 8) {
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

        List<String> values = new ArrayList<>();

        Matcher matcher = Pattern.compile("\\b\\d+\\b").matcher(text);

        while (matcher.find()) {

            String value = matcher.group();

            // Ignorar las frecuencias del encabezado
            if (FREQUENCIES.contains(value)) {
                continue;
            }

            values.add(value);

            // Solo tomar los primeros 8 valores
            if (values.size() == 8) {
                break;
            }
        }

        return values;
    }

    @Override
    protected Map<String, String> parseLine(String line) {
        return null;
    }

    @Override
    protected TypeReference<List<Map<String, String>>> getTypeReference() {
        return new TypeReference<>() {};
    }

    private List<Map<String, String>> buildFallback(String rawText) {

        List<String> numbers = Arrays.stream(rawText.split("\\s+"))
                .filter(s -> s.matches("\\d+"))
                .collect(Collectors.toList());

        if (numbers.size() < 16) return null;

        Map<String, String> map = new LinkedHashMap<>();

        for (int i = 0; i < Math.min(8, numbers.size()); i++) {
            map.put("VAOD" + FREQUENCIES.get(i), numbers.get(i));
        }

        for (int i = 0; i < Math.min(8, numbers.size() - 8); i++) {
            map.put("VAOI" + FREQUENCIES.get(i), numbers.get(i + 8));
        }

        return List.of(map);
    }
}