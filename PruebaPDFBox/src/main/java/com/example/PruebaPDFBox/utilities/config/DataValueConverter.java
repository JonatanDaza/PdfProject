package com.example.PruebaPDFBox.utilities.config;

import org.mapstruct.Named;
import org.springframework.stereotype.Component;

@Component
public class DataValueConverter {

    @Named("stringToObject")
    public Object stringToObject(String value) {
        return value;
    }

    @Named("objectToString")
    public String objectToString(Object value) {
        if (value == null) return null;
        return value.toString();
    }
}