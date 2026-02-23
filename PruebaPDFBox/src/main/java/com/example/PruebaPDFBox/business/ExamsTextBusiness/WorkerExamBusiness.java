package com.example.PruebaPDFBox.business.ExamsTextBusiness;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    public static final Set<String> WORKER_SUB_SECTION_TITLES = Set.of("DATOS PERSONALES");

    public Worker processWorker(PDDocument document, SectionArea section) throws IOException {

        // Resolver SubSection DATOS PERSONALES dentro del examen
        Map<String, SubSectionArea> subSections =
                pdfSubSectionExtractor.extract(
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
        Map<String, String> extractedData =
                plainTextExtractor.extractMultipleValues(
                        document,
                        fields,
                        section,
                        workerArea
                );

        // Map<String, AreaConfig> config = buildDataConfig();
        // Map<String, String> extractedData = new HashMap<>();
        // for (Map.Entry<String, SectionArea> entry : sections.entrySet()) {
        //     SectionArea section = entry.getValue();
        //     String sectionName = entry.getKey();
        //     try {
        //         List<FieldExtractionConfig> fields = buildWorkerFieldConfigurations(config);
        //         Map<String, String> sectionData = plainTextExtractor.extractMultipleValues(document, fields, section, subSection);
        //         for (Map.Entry<String, String> dataEntry : sectionData.entrySet()) {
        //             String key = dataEntry.getKey();
        //             String value = dataEntry.getValue();
        //             if (value != null && !value.trim().isEmpty()) {
        //                 if (!extractedData.containsKey(key) || extractedData.get(key).trim().isEmpty()) {
        //                     extractedData.put(key, value);
        //                 }
        //             }
        //         }
        //     } catch (Exception e) {
        //         throw new RuntimeException("Error procesando sección " + sectionName + ": " + e.getMessage());
        //     }
        // }

        String cc = extractedData.get("CC:");
        if (cc == null || cc.trim().isEmpty()) {
            System.err.println("No se encontró CC válido");
            return null;
        }

        Worker worker = new Worker();
        worker.setCC(plainTextExtractor.extractFirstNumber(cc));
        worker.setGender(extractedData.get("Genero:"));
        worker.setAge(plainTextExtractor.extractFirstNumber(extractedData.get("Edad:")));
        worker.setMaritalStatus(extractedData.get("Estado Civil:"));
        worker.setEducationalLevel(extractedData.get("Nivel Educativo:"));

//        worker.setName(extractedData.get("Nombre:"));

        return worker;
    }

    private List<FieldExtractionConfig> buildWorkerFieldConfigurations(Map<String, AreaConfig> config) {
        List<FieldExtractionConfig> fields = new ArrayList<>();

        fields.add(new FieldExtractionConfig("CC:", "CC:", config.get("CC:"), FieldExtractionConfig.ExtractionStrategy.EXACT_MATCH));
        fields.add(new FieldExtractionConfig("Genero:", "Genero:", config.get("Genero:"), FieldExtractionConfig.ExtractionStrategy.EXACT_MATCH));
        fields.add(new FieldExtractionConfig("Edad:", "Edad:", config.get("Edad:"), FieldExtractionConfig.ExtractionStrategy.EXACT_MATCH));
        fields.add(new FieldExtractionConfig("Estado Civil:", "Estado Civil:", config.get("Estado Civil:"), FieldExtractionConfig.ExtractionStrategy.EXACT_MATCH));
        fields.add(new FieldExtractionConfig("Nivel Educativo:", "Nivel Educativo:", config.get("Nivel Educativo:"), FieldExtractionConfig.ExtractionStrategy.EXACT_MATCH));

//        fields.add(new FieldExtractionConfig("Nombre:", "Nombre:", config.get("Nombre:"), FieldExtractionConfig.ExtractionStrategy.EXACT_MATCH));

        return fields;
    }

    private Map<String, AreaConfig> buildDataConfig() {
        Map<String, AreaConfig> config = new HashMap<>();

        config.put("CC:", new AreaConfig(0, 172, 12));
        config.put("Genero:", new AreaConfig(0, 192, 12));
        config.put("Edad:", new AreaConfig(0, 192, 12));
        config.put("Estado Civil:", new AreaConfig(40, 80, 12));
        config.put("Nivel Educativo:", new AreaConfig(40, 120, 12));

//        config.put("Nombre:", new AreaConfig(0, 160, 12));

        return config;
    }

}