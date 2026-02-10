package com.example.PruebaPDFBox.utilities.config.MapperConfig;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mapstruct.Named;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class JsonMapperConfig {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Named("objectToJson")
    public static String objectToJson(Object object) {
        if (object == null) {
            return "[]"; // Retornar JSON array vacío en lugar de null
        }
        try {
            String json = objectMapper.writeValueAsString(object);
            // Si el resultado es null o vacío, retornar []
            return (json == null || json.trim().isEmpty()) ? "[]" : json;
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error converting object to JSON", e);
        }
    }

    @Named("jsonToObject")
    public static Object jsonToObject(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, Object.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error converting JSON to object", e);
        }
    }

    public static <T> T jsonToList(String json, TypeReference<T> typeReference) {
        if (json == null || json.trim().isEmpty() || "[]".equals(json.trim())) {
            try {
                // Retornar una lista vacía en lugar de null
                return objectMapper.readValue("[]", typeReference);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Error creating empty list", e);
            }
        }
        try {
            return objectMapper.readValue(json, typeReference);
        } catch (JsonProcessingException e) {
            // En caso de error, intentar retornar una lista vacía
            try {
                return objectMapper.readValue("[]", typeReference);
            } catch (JsonProcessingException ex) {
                throw new RuntimeException("Error converting JSON to List: " + json, e);
            }
        }
    }

    @Named("jsonToMap")
    public static Map<String, Object> jsonToMap(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error converting JSON to Map", e);
        }
    }

    @Named("mapToJson")
    public static String mapToJson(Map<String, Object> map) {
        if (map == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error converting Map to JSON", e);
        }
    }
}