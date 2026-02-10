package com.example.PruebaPDFBox.utilities.mapper.MapperJsons;

import com.example.PruebaPDFBox.dto.DTOJson.MorphologyStudyDto;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class MorphologyStudyMapper extends GenericJsonMapper<MorphologyStudyDto> {

    @Override
    public String parseToJson(String rawText) {
        if (isNullOrEmpty(rawText)) return "[]";

        List<MorphologyStudyDto> result = new ArrayList<>();

        String[] lines = rawText.split("\\r?\\n");

        for (String line : lines) {
            MorphologyStudyDto dto = parseLine(line);
            if (dto != null) {
                result.add(dto);
            }
        }

        return toJson(result);
    }

    @Override
    protected MorphologyStudyDto parseLine(String line) {
        if (line == null || !line.contains(":")) return null;

        String[] parts = line.split(":", 2);

        String campo = parts[0].trim();
        String valor = parts[1].trim();

        if (campo.isEmpty() || valor.isEmpty()) return null;

        return new MorphologyStudyDto(campo, valor);
    }

    @Override
    protected TypeReference<List<MorphologyStudyDto>> getTypeReference() {
        return new TypeReference<>() {};
    }
}
