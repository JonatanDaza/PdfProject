package com.example.PruebaPDFBox.business.ExamsTextBusiness;

import java.io.IOException;
import java.util.*;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.stereotype.Component;

import com.example.PruebaPDFBox.model.ClinicalHistory;
import com.example.PruebaPDFBox.model.Exam;
import com.example.PruebaPDFBox.model.Worker;
import com.example.PruebaPDFBox.record.AreaConfig;
import com.example.PruebaPDFBox.record.FieldExtractionConfig;
import com.example.PruebaPDFBox.record.SectionArea;
import com.example.PruebaPDFBox.record.SubSectionArea;
import com.example.PruebaPDFBox.service.ServiceEntity.ClinicalHistoryService;
import com.example.PruebaPDFBox.service.ServiceEntity.DataValueService;
import com.example.PruebaPDFBox.service.ServiceEntity.ExamService;
import com.example.PruebaPDFBox.utilities.extractor.SectionExtractor.PdfSubSectionExtractor;
import com.example.PruebaPDFBox.utilities.extractor.TextExtractor.PlainTextExtractor;
import com.example.PruebaPDFBox.utilities.mapper.MapperJsons.RecommendationsConductMapper;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OptometryTextBusiness {

    private final PlainTextExtractor plainTextExtractor;
    private final RecommendationsConductMapper recommendationsConductMapper;
    private final ExamService examService;
    private final ClinicalHistoryService clinicalHistoryService;
    private final DataValueService dataValueService;
    private final PdfSubSectionExtractor pdfSubSectionExtractor;

    public boolean process(PDDocument document, SectionArea section, Worker worker, String examName) throws IOException {
        try {
            // Extraer subsecciones primero
            System.out.println("\n=== OPTOMETRY - Iniciando proceso ===");

            Map<String, SubSectionArea> subSections = pdfSubSectionExtractor.extract(
                    document, section, SUB_SECTION_TITLES
            );

            System.out.println("\n=== OPTOMETRY - SubSecciones encontradas ===");
            subSections.forEach((k, v) -> System.out.println("  [" + k + "] -> " + (v != null ? "✅" : "❌ NULL")));

            // --- CÓDIGO (CUV) ---
            SubSectionArea codigoArea = subSections.get("CÓDIGO");
            System.out.println("\n=== OPTOMETRY - CÓDIGO -> "
                    + (codigoArea != null ? "✅" : "❌ NULL"));

            // --- CONDUCTA Y/O RECOMENDACIONES ---
            SubSectionArea recommendationsConductArea =
                    subSections.get("CONDUCTA Y/O RECOMENDACIONES");
            System.out.println("\n=== OPTOMETRY - CONDUCTA Y/O RECOMENDACIONES -> "
                    + (recommendationsConductArea != null ? "✅" : "❌ NULL"));

            // Extraer fecha desde DATOS PERSONALES
            SubSectionArea personalDataArea = subSections.get("DATOS PERSONALES");
            String examDate = null;

            if (personalDataArea != null) {
                String rawDate = plainTextExtractor.extractTextBetweenLabels(
                        document, "Fecha:", "Municipio:", section
                );

                System.out.println("rawDate encontrado: '" + rawDate + "'");
                examDate = plainTextExtractor.extractDate(rawDate);
                System.out.println("examDate formateado: '" + examDate + "'");
            }else {
                Map<String, AreaConfig> config = buildOptometryConfig();
                List<FieldExtractionConfig> dateFields = new ArrayList<>();
                dateFields.add(new FieldExtractionConfig(
                        "Fecha:", "Fecha:",
                        config.get("Fecha:"),
                        FieldExtractionConfig.ExtractionStrategy.HORIZONTAL
                ));

                Map<String, String> dateData = plainTextExtractor.extractMultipleValues(
                        document, dateFields, section, personalDataArea
                );

                String rawDate = dateData.get("Fecha:");
                System.out.println("rawDate encontrado: '" + rawDate + "'");

                examDate = plainTextExtractor.extractDate(rawDate);
                System.out.println("examDate formateado: '" + examDate + "'");
            }

            if (examDate == null || examDate.isEmpty()) {
                System.err.println("No se encontró fecha de examen en DATOS PERSONALES");
                return false;
            }

            if (examService.examAlreadyExists(examDate, examName)) {
                System.out.println("Examen ya existe: " + examName + " - " + examDate);
                return false;
            }

            System.out.println("\n=== OPTOMETRY - Construyendo valores... ===");
            Map<String, String> values = buildDataValues(document, section, subSections);

            System.out.println("\n=== OPTOMETRY - Valores construidos ===");
            values.forEach((k, v) -> System.out.println("  [" + k + "] -> '" + v + "' " + (v == null ? "❌ NULL" : "✅")));

            System.out.println("\n=== OPTOMETRY - Guardando en BD... ===");

            ClinicalHistory clinicalHistory = clinicalHistoryService.findOrCreate(worker, examDate);
            Exam exam = examService.save(examName, examDate, worker, clinicalHistory);
            dataValueService.saveAll(values, exam);

            return true;

        } catch (Exception e) {
            System.err.println("ERROR en OptometryTextBusiness: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }


    private Map<String, String> buildDataValues(
            PDDocument document,
            SectionArea section,
            Map<String, SubSectionArea> subSections
    ) throws IOException {

        Map<String, String> values = new LinkedHashMap<>();
        Map<String, AreaConfig> config = buildOptometryConfig();

        // CUV
        SubSectionArea codigoArea = subSections.get("CÓDIGO");
        if (codigoArea != null) {
            List<FieldExtractionConfig> fields = buildCodigoFields(config);
            Map<String, String> extracted = plainTextExtractor.extractMultipleValues(
                    document, fields, section, codigoArea
            );

            // LOG: ver qué encontró
            System.out.println("\n=== CÓDIGO - Valores extraídos ===");
            extracted.forEach((k, v) ->
                    System.out.println("  [" + k + "] -> '" + v + "' " + (v == null || v.trim().isEmpty() ? "❌ VACÍO" : "✅"))
            );

            values.putAll(extracted);
        }

        // CONDUCTA Y/O RECOMENDACIONES
        SubSectionArea recommendationsConductArea = subSections.get("CONDUCTA Y/O RECOMENDACIONES");
        if (recommendationsConductArea != null) {
            String rawRecommendationsConduct = plainTextExtractor.extractTextBetweenLabels(
                    document, "CONDUCTA Y/O RECOMENDACIONES", "OTRAS OBSERVACIONES Y RECOMENDACIONES:", section
            );

            // Debug
            System.out.println("\n=== CONDUCTA Y/O RECOMENDACIONES - Valores extraídos ===");
            System.out.println("  [rawRecommendationsConduct] -> '" + rawRecommendationsConduct + "' "
                    + (rawRecommendationsConduct == null || rawRecommendationsConduct.trim().isEmpty() ? "❌ VACÍO" : "✅"));

            String parsed = recommendationsConductMapper.parseToJson(rawRecommendationsConduct);
            System.out.println("  [CONDUCTA Y/O RECOMENDACIONES (JSON)] -> '" + parsed + "' "
                    + (parsed == null || parsed.trim().isEmpty() ? "❌ VACÍO" : "✅"));

            values.put("Conducta Y/O Recomendaciones", parsed);
        }

        System.out.println("\n=== TOTAL VALUES ACUMULADOS ===");
        values.forEach((k, v) ->
                System.out.println("  [" + k + "] -> '" + v + "'")
        );

        return values;
    }

    private List<FieldExtractionConfig> buildCodigoFields(Map<String, AreaConfig> config) {
        List<FieldExtractionConfig> fields = new ArrayList<>();
        fields.add(new FieldExtractionConfig("DIAGNÓSTICO", "Nombre del Diagnóstico",
                config.get("DIAGNÓSTICO"), FieldExtractionConfig.ExtractionStrategy.VERTICAL));
        fields.add(new FieldExtractionConfig("CÓDIGO", "Código de Examen",
                config.get("CÓDIGO"), FieldExtractionConfig.ExtractionStrategy.VERTICAL));
        return fields;
    }

    private Map<String, AreaConfig> buildOptometryConfig() {
        Map<String, AreaConfig> config = new HashMap<>();

        config.put("Fecha:", new AreaConfig(0, 100, 12));
        config.put("DIAGNÓSTICO", new AreaConfig(0, 100, 21));
        config.put("CÓDIGO", new AreaConfig(0, 20, 5));

        return config;
    }

    private static final Set<String> SUB_SECTION_TITLES = Set.of(
            "DATOS PERSONALES",
            "ANTECEDENTES VISUALES",
            "SINTOMATOLOGÍA",
            "EXAMEN EXTERNO",
            "REFLEJOS",
            "COVER TEST",
            "MOTILIDAD OCULAR",
            "PUNTO PRÓXIMO DE CONVERGENCIA",
            "OFTALMOSCOPÍA",
            "QUERATROMETRÍA",
            "REFRACCIÓN",
            "RX FINAL",
            "VISIÓN COLOR",
            "ESTEREOPSIS",
            "CÓDIGO",
            "CONDUCTA Y/O RECOMENDACIONES",
            "OTRAS OBSERVACIONES Y RECOMENDACIONES:"
    );
}