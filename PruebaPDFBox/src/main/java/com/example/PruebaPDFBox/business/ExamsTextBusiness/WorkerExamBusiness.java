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

        SubSectionArea workerArea = subSections.get("DATOS PERSONALES");

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

        String rawCC = extractedData.get("CC:");
        String cc = plainTextExtractor.extractFirstNumber(rawCC);

        if (cc == null || cc.trim().isEmpty()) {
            System.err.println("No se encontró CC válido");
            return null;
        }

        String rawAge = extractedData.get("Edad:");
        String age = plainTextExtractor.extractFirstNumber(rawAge);
        extractedData.put("Edad:", age != null ? age : "");

        return workerService.buildFromExtractedData(extractedData, cc);
    }

    private List<FieldExtractionConfig> buildWorkerFieldConfigurations(Map<String, AreaConfig> config) {
        List<FieldExtractionConfig> fields = new ArrayList<>();

        fields.add(new FieldExtractionConfig("CC:", "CC:",
                config.get("CC:"), FieldExtractionConfig.ExtractionStrategy.EXACT_MATCH));
        fields.add(new FieldExtractionConfig("Genero:", "Genero:",
                config.get("Genero:"), FieldExtractionConfig.ExtractionStrategy.EXACT_MATCH));
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
        config.put("Edad:", new AreaConfig(0, 192, 12));
        config.put("Estado Civil:", new AreaConfig(40, 80, 12));
        config.put("Nivel Educativo:", new AreaConfig(40, 120, 12));

        return config;
    }

}