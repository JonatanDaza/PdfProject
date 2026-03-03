package com.example.PruebaPDFBox.business.ExamsTextBusiness;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.example.PruebaPDFBox.service.ServiceEntity.WorkerService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.stereotype.Component;

import com.example.PruebaPDFBox.model.Worker;
import com.example.PruebaPDFBox.record.AreaConfig;
import com.example.PruebaPDFBox.record.FieldExtractionConfig;
import com.example.PruebaPDFBox.record.SectionArea;
import com.example.PruebaPDFBox.record.SubSectionArea;
import com.example.PruebaPDFBox.utilities.extractor.SectionExtractor.PdfSubSectionExtractor;
import com.example.PruebaPDFBox.utilities.extractor.TextExtractor.PlainTextExtractor;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class WorkerExamBusiness {

    private final PlainTextExtractor plainTextExtractor;
    private final PdfSubSectionExtractor pdfSubSectionExtractor;
    private final WorkerService workerService;

    public static final Set<String> WORKER_SUB_SECTION_TITLES = Set.of("DATOS PERSONALES");

    public Worker processWorker(PDDocument document, SectionArea section) throws IOException {

        // Resolver SubSection DATOS PERSONALES dentro del examen
        Map<String, SubSectionArea> subSections = pdfSubSectionExtractor.extract(
                document,
                section,
                WORKER_SUB_SECTION_TITLES
        );

        // DEBUG: ver subsecciones encontradas
        System.out.println("\n=== WORKER - SubSecciones encontradas ===");
        subSections.forEach((k, v) -> System.out.println("  [" + k + "] -> " + (v != null ? "✅" : "❌ NULL")));
        System.out.println("=== SECTION DEBUG ===");
        System.out.println("  startPage: " + section.startPage() + ", startY: " + section.startY());
        System.out.println("  endPage:   " + section.endPage()   + ", endY:   " + section.endY());
        System.out.println("  examName:  " + section.examName());

        SubSectionArea workerArea = subSections.get("DATOS PERSONALES");
        if (workerArea != null) {
            System.out.println("=== SUBSECTION DEBUG ===");
            System.out.println("  page:   " + workerArea.page());
            System.out.println("  startY: " + workerArea.startY());
            System.out.println("  endY:   " + workerArea.endY());
        }

        if (workerArea == null) {
            System.err.println("No se encontró subsección DATOS PERSONALES");
            return null;
        }

        // Construir config y fields
        Map<String, AreaConfig> config = buildDataConfig();
        List<FieldExtractionConfig> fields = buildWorkerFieldConfigurations(config);

        // Extraer solo dentro de DATOS PERSONALES
        Map<String, String> extractedData = plainTextExtractor.extractMultipleValues(
                document,
                fields,
                section,
                workerArea
        );

        // DEBUG: ver qué se extrajo
        System.out.println("\n=== WORKER - Datos extraídos ===");
        extractedData.forEach((k, v) ->
                System.out.println("  [" + k + "] -> '" + v + "' " + (v == null || v.trim().isEmpty() ? "❌ VACÍO" : "✅"))
        );

        String rawCC = extractedData.get("CC:");
        String cc = plainTextExtractor.extractFirstNumber(rawCC);

        System.out.println("\n=== WORKER - CC ===");
        System.out.println("  rawCC    -> '" + rawCC + "'");
        System.out.println("  cc (num) -> '" + cc + "' " + (cc == null || cc.trim().isEmpty() ? "❌ VACÍO" : "✅"));

        // Validar solo campos obligatorios
        if (cc == null || cc.trim().isEmpty()) {
            System.err.println("❌ CC obligatorio no encontrado");
            return null;
        }

        String rawGenero = extractedData.getOrDefault("Genero:", "");
        if (rawGenero.trim().isEmpty()) rawGenero = extractedData.getOrDefault("Género:", "");
        if (rawGenero.trim().isEmpty()) rawGenero = extractedData.getOrDefault("Sexo:", "");

        if (rawGenero == null || rawGenero.trim().isEmpty()) {
            System.err.println("❌ Género obligatorio no encontrado");
            return null;
        }
        extractedData.put("Genero:", rawGenero);

        String rawAge = extractedData.get("Edad:");
        String age = plainTextExtractor.extractFirstNumber(rawAge);

        System.out.println("  rawAge   -> '" + rawAge + "'");
        System.out.println("  age(num) -> '" + age + "'");

        if (age == null || age.trim().isEmpty()) {
            System.err.println("❌ Edad obligatoria no encontrada");
            return null;
        }

        // Campos opcionales: Estado Civil y Nivel Educativo pueden quedar vacíos
        extractedData.put("Edad:", age);

        return workerService.buildFromExtractedData(extractedData, cc);
    }

    private List<FieldExtractionConfig> buildWorkerFieldConfigurations(Map<String, AreaConfig> config) {
        List<FieldExtractionConfig> fields = new ArrayList<>();

        fields.add(new FieldExtractionConfig("CC:", "CC:",
                config.get("CC:"), FieldExtractionConfig.ExtractionStrategy.EXACT_MATCH));
        fields.add(new FieldExtractionConfig("Genero:", "Genero:",
                config.get("Genero:"), FieldExtractionConfig.ExtractionStrategy.EXACT_MATCH));
        fields.add(new FieldExtractionConfig("Género:", "Género:",
                config.get("Género:"), FieldExtractionConfig.ExtractionStrategy.EXACT_MATCH));
        fields.add(new FieldExtractionConfig("Sexo:", "Sexo:",
                config.get("Sexo:"), FieldExtractionConfig.ExtractionStrategy.EXACT_MATCH));
        fields.add(new FieldExtractionConfig("Edad:", "Edad:",
                config.get("Edad:"), FieldExtractionConfig.ExtractionStrategy.EXACT_MATCH));
        fields.add(new FieldExtractionConfig("Estado Civil:", "Estado Civil:",
                config.get("Estado Civil:"), FieldExtractionConfig.ExtractionStrategy.EXACT_MATCH));
        fields.add(new FieldExtractionConfig("Nivel Educativo:", "Nivel Educativo:",
                config.get("Nivel Educativo:"), FieldExtractionConfig.ExtractionStrategy.EXACT_MATCH));

        return fields;
    }

    private Map<String, AreaConfig> buildDataConfig() {
        Map<String, AreaConfig> config = new HashMap<>();

        config.put("CC:", new AreaConfig(0, 172, 12));
        config.put("Genero:", new AreaConfig(0, 192, 12));
        config.put("Género:", new AreaConfig(0, 192, 12));
        config.put("Sexo:", new AreaConfig(0, 192, 12));
        config.put("Edad:", new AreaConfig(0, 192, 12));
        config.put("Estado Civil:", new AreaConfig(40, 80, 12));
        config.put("Nivel Educativo:", new AreaConfig(40, 120, 12));

        return config;
    }

}