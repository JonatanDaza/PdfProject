package com.example.PruebaPDFBox.utilities.mapper.MapperJsons;

import com.example.PruebaPDFBox.dto.DTOJson.HealthRequirementDto;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class HealthRequirementMapper extends GenericJsonMapper<HealthRequirementDto> {

    private static final List<String> KEYWORDS = Arrays.asList("NO APTO", "APTO", "NO CUMPLE", "CUMPLE", "SÍ", "SI", "NO");
    private static final Set<String> EXCLUDE_WORDS = new HashSet<>(Arrays.asList("APTO", "NO APTO", "CUMPLE", "NO CUMPLE", "PARA", "EN"));
    private static final Pattern MULTI_PATTERN = Pattern.compile("(.*?)\\s+(APTO|NO\\s+APTO|CUMPLE|NO\\s+CUMPLE)(?=\\s+[A-ZÁÉÍÓÚÑ]|\\s*$)", Pattern.CASE_INSENSITIVE);
    private static final Pattern COLON_PATTERN = Pattern.compile("^(.+?)[:：]\\s*(.+?)$");
    private static final int MIN_LENGTH = 5, MIN_WORDS = 2, MIN_MATCHES = 2;

    private static final Pattern STATUS_PATTERN = Pattern.compile(
        "\\b(NO\\s+APTO|APTO|NO\\s+CUMPLE|CUMPLE|SÍ|SI|NO)\\b",
            Pattern.CASE_INSENSITIVE);
        
    private static final Pattern CUMPLE_PATTERN =
        Pattern.compile("^(.*?)\\s*,?\\s*Cumple\\s*:\\s*(.+)$",
            Pattern.CASE_INSENSITIVE);

    public String parseToJson(String rawText) {
        if (isNullOrEmpty(rawText)) return "[]";

        List<HealthRequirementDto> reqs = tryParseMultiple(rawText);
        if (reqs.isEmpty()) {
            reqs = Arrays.stream(rawText.split("\\r?\\n"))
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .map(this::parseLine)
                .filter(dto -> !isNullOrEmpty(dto.getRequisito()))
                .collect(Collectors.toList());
        }

        return toJson(reqs);
    }

    @Override
    protected TypeReference<List<HealthRequirementDto>> getTypeReference() {
        return new TypeReference<>() {};
    }

    private List<HealthRequirementDto> tryParseMultiple(String text) {
        List<HealthRequirementDto> result = new ArrayList<>();
        Matcher matcher = MULTI_PATTERN.matcher(text.toUpperCase());

        while (matcher.find()) {
            String req = matcher.group(1).trim();
            String status = matcher.group(2).trim();

            if (isValid(req)) {
                result.add(createDto(format(req), normalize(status)));
            }
        }

        return result.size() >= MIN_MATCHES ? result : new ArrayList<>();
    }

    @Override
    protected HealthRequirementDto parseLine(String line) {

        line = line.replaceFirst("^\\d+\\.\\s*", "").trim();

        if (line.isBlank()) {
            return createDto("", "");
        }

        // Caso: "..., Cumple: N/A"
        Matcher cumpleMatcher = CUMPLE_PATTERN.matcher(line);
        if (cumpleMatcher.matches()) {
            return createDto(
                    cumpleMatcher.group(1).trim(),
                    normalize(cumpleMatcher.group(2).trim())
            );
        }

        // Caso: "Etiqueta: Valor"
        Matcher colonMatcher = COLON_PATTERN.matcher(line);
        if (colonMatcher.matches()) {
            return createDto(
                    colonMatcher.group(1).trim(),
                    normalize(colonMatcher.group(2).trim())
            );
        }

        // Buscar la última palabra de estado como palabra completa
        Matcher statusMatcher = STATUS_PATTERN.matcher(line);

        int start = -1;
        int end = -1;

        while (statusMatcher.find()) {
            start = statusMatcher.start();
            end = statusMatcher.end();
        }

        if (start > 0) {
            return createDto(
                    line.substring(0, start).trim(),
                    normalize(line.substring(start, end).trim())
            );
        }

        return createDto(line, "");
    }

    private HealthRequirementDto createDto(String requisito, String cumple) {
        HealthRequirementDto dto = new HealthRequirementDto();
        dto.setRequisito(requisito);
        dto.setCumple(cumple);
        return dto;
    }

    private boolean isValid(String text) {
        return text.length() >= MIN_LENGTH
            && !EXCLUDE_WORDS.contains(text)
            && text.split("\\s+").length >= MIN_WORDS;
    }

    private String format(String text) {
        text = text.replaceAll("(?i)\\s+PARA\\s*$", "");
        return Arrays.stream(text.toLowerCase().split("\\s+"))
            .map(w -> w.length() <= 2 && !w.matches("\\d+") ? w :
                     Character.toUpperCase(w.charAt(0)) + (w.length() > 1 ? w.substring(1) : ""))
            .collect(Collectors.joining(" "));
    }
    
    private String normalize(String status) {

        if (isNullOrEmpty(status)) {return "";}

        String s = status.toUpperCase().trim();

        if (s.equals("N/A") || s.equals("NA")) {return "N/A";}
        if (s.equals("NO APTO")) {return "NO APTO";}
        if (s.equals("APTO")) {return "APTO";}
        if (s.equals("NO CUMPLE")) {return "NO CUMPLE";}
        if (s.equals("CUMPLE")) {return "CUMPLE";}
        if (s.equals("SI") || s.equals("SÍ")) {return "SÍ";}
        if (s.equals("NO")) {return "NO";}

        return status.trim();
    }
}
