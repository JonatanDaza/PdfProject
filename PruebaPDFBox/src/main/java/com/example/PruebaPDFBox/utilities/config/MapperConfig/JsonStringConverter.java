package com.example.PruebaPDFBox.utilities.config.MapperConfig;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class JsonStringConverter implements AttributeConverter<String, String> {

    @Override
    public String convertToDatabaseColumn(String jsonString) {
        return jsonString;
    }

    @Override
    public String convertToEntityAttribute(String jsonFromDb) {
        return jsonFromDb;
    }
}