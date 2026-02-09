package com.example.PruebaPDFBox.utilities.mapper.MapperJsons;

import com.example.PruebaPDFBox.utilities.config.JsonMapperConfig;
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class GenericJsonMapper<T> {

    public String parseToJson(String rawText) {
        if (isNullOrEmpty(rawText)) return "[]";

        List<T> items = Arrays.stream(rawText.split("\\r?\\n"))
            .map(String::trim)
            .filter(line -> !line.isEmpty())
            .map(this::parseLine)
            .filter(item -> item != null)
            .collect(Collectors.toList());

        return toJson(items);
    }

    public String parseToJson(String rawText, Function<String, T> customParser) {
        if (isNullOrEmpty(rawText)) return "[]";

        List<T> items = Arrays.stream(rawText.split("\\r?\\n"))
            .map(String::trim)
            .filter(line -> !line.isEmpty())
            .map(customParser)
            .filter(item -> item != null)
            .collect(Collectors.toList());

        return toJson(items);
    }

    public String toJson(List<T> items) {
        if (items == null || items.isEmpty()) return "[]";
        return JsonMapperConfig.objectToJson(items);
    }

    public List<T> fromJson(String json) {
        if (isNullOrEmpty(json) || "[]".equals(json)) return new ArrayList<>();

        try {
            return JsonMapperConfig.jsonToList(json, getTypeReference());
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    protected abstract T parseLine(String line);

    protected abstract TypeReference<List<T>> getTypeReference();

    protected boolean isNullOrEmpty(String text) {
        return text == null || text.trim().isEmpty();
    }
}
