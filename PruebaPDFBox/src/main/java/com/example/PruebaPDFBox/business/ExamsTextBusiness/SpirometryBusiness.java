package com.example.PruebaPDFBox.business.ExamsTextBusiness;

import java.io.IOException;
import java.util.*;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.stereotype.Component;

import com.example.PruebaPDFBox.model.*;
import com.example.PruebaPDFBox.record.AreaConfig;
import com.example.PruebaPDFBox.record.FieldExtractionConfig;
import com.example.PruebaPDFBox.record.SectionArea;
import com.example.PruebaPDFBox.record.SubSectionArea;
import com.example.PruebaPDFBox.service.ServiceEntity.ClinicalHistoryService;
import com.example.PruebaPDFBox.service.ServiceEntity.DataValueService;
import com.example.PruebaPDFBox.service.ServiceEntity.ExamService;
import com.example.PruebaPDFBox.utilities.extractor.SectionExtractor.PdfSubSectionExtractor;
import com.example.PruebaPDFBox.utilities.extractor.TextExtractor.PlainTextExtractor;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SpirometryBusiness {

    private final PlainTextExtractor plainTextExtractor;
    private final ExamService examService;
    private final ClinicalHistoryService clinicalHistoryService;
    private final DataValueService dataValueService;
    private final PdfSubSectionExtractor pdfSubSectionExtractor;

    public boolean process(PDDocument document, SectionArea section, Worker worker, String examName) {
        try {
            // Extraer subsecciones primero
            Map<String, SubSectionArea> subSections = pdfSubSectionExtractor.extract(
                    document, section, SUB_SECTION_TITLES
            );

            // Extraer fecha desde DATOS PERSONALES
            SubSectionArea personalDataArea = subSections.get("DATOS PERSONALES");
            String examDate = null;

            if (personalDataArea != null) {
                Map<String, AreaConfig> config = buildSpirometryConfig();
                List<FieldExtractionConfig> dateFields = new ArrayList<>();
                dateFields.add(new FieldExtractionConfig(
                        "Fecha:", "Fecha:",
                        config.get("Fecha:"),
                        FieldExtractionConfig.ExtractionStrategy.EXACT_MATCH
                ));

                Map<String, String> dateData = plainTextExtractor.extractMultipleValues(
                        document, dateFields, section, personalDataArea
                );

                String rawDate = dateData.get("Fecha:");
                examDate = plainTextExtractor.extractDate(rawDate);
            }

            if (examDate == null || examDate.isEmpty()) {
                System.err.println("No se encontró fecha de examen en DATOS PERSONALES");
                return false;
            }

            if (examService.examAlreadyExists(examDate, examName)) {
                System.out.println("Examen ya existe: " + examName + " - " + examDate);
                return false;
            }

            Map<String, String> values = buildDataValues(document, section, subSections);

            ClinicalHistory clinicalHistory = clinicalHistoryService.findOrCreate(worker, examDate);
            Exam exam = examService.save(examName, examDate, worker, clinicalHistory);
            dataValueService.saveAll(values, exam);

            return true;
        } catch (IOException e) {
            System.err.println("Error procesando medicina ocupacional: " + e.getMessage());
            return false;
        }
    }

    private Map<String, String> buildDataValues(
            PDDocument document,
            SectionArea section,
            Map<String, SubSectionArea> subSections
    ) throws IOException {

        Map<String, String> values = new LinkedHashMap<>();
        Map<String, AreaConfig> config = buildSpirometryConfig();

        SubSectionArea spirometryArea = subSections.get("INFORME DE ESPIROMETRÍA");
        if (spirometryArea != null) {
            List<FieldExtractionConfig> fields = buildSpirometryField(config);
            Map<String, String> extracted = plainTextExtractor.extractMultipleValues(
                    document, fields, section, spirometryArea
            );

            values.putAll(extracted);
        }

        return values;
    }

    private List<FieldExtractionConfig> buildSpirometryField(Map<String, AreaConfig> config) {
        List<FieldExtractionConfig> fields = new ArrayList<>();

        fields.add(new FieldExtractionConfig("Interpretación", "Interpretacion",
                config.get("Interpretación"), FieldExtractionConfig.ExtractionStrategy.HORIZONTAL));
        return fields;
    }

    private Map<String, AreaConfig> buildSpirometryConfig() {
        Map<String, AreaConfig> config = new HashMap<>();
        config.put("Interpretación", new AreaConfig(0, 50, 50));

        return config;
    }

    private static final Set<String> SUB_SECTION_TITLES = Set.of();
}
