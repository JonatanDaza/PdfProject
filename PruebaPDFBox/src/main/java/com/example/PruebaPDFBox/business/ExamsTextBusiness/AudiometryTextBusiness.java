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
import com.example.PruebaPDFBox.utilities.mapper.MapperJsons.ObservationsMapper;
import com.example.PruebaPDFBox.utilities.mapper.MapperJsons.RecommendationMapper;
import com.example.PruebaPDFBox.utilities.mapper.MapperJsons.WeightedDecibelsMapper;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AudiometryTextBusiness {

    private final PlainTextExtractor plainTextExtractor;
    private final WeightedDecibelsMapper weightedDecibelsMapper;
    private final ObservationsMapper observationsMapper;
    private final RecommendationMapper recommendationMapper;
    private final ExamService examService;
    private final ClinicalHistoryService clinicalHistoryService;
    private final DataValueService dataValueService;
    private final PdfSubSectionExtractor pdfSubSectionExtractor;

    public boolean process(PDDocument document, SectionArea section, Worker worker, String examName) throws IOException {
        try {
            // Extraer subsecciones primero
            System.out.println("\n=== AUDIOMETRY - Iniciando proceso ===");

            Map<String, SubSectionArea> subSections = pdfSubSectionExtractor.extract(
                    document, section, SUB_SECTION_TITLES
            );

            System.out.println("\n=== AUDIOMETRY - SubSecciones encontradas ===");
            subSections.forEach((k, v) -> System.out.println("  [" + k + "] -> " + (v != null ? "✅" : "❌ NULL")));

            // --- OTOSCOPIA ---
            SubSectionArea otoscopiaArea = subSections.get("OTOSCOPIA");
            System.out.println("\n=== AUDIOMETRY - OTOSCOPIA -> " + (otoscopiaArea != null ? "✅" : "❌ NULL"));

            // --- DB(A) ---
            SubSectionArea dBArea = subSections.get("DB(A)");
            System.out.println("=== AUDIOMETRY - DB(A) -> " + (dBArea != null ? "✅" : "❌ NULL"));

            // --- CÓDIGO ---
            SubSectionArea codigoArea = subSections.get("CÓDIGO");
            System.out.println("=== AUDIOMETRY - CÓDIGO -> " + (codigoArea != null ? "✅" : "❌ NULL"));

            // --- RECOMENDACION ---
            SubSectionArea recomendacionArea = subSections.get("RECOMENDACION");
            System.out.println("=== AUDIOMETRY - RECOMENDACION -> " + (recomendacionArea != null ? "✅" : "❌ NULL"));

            // --- OBSERVACIONES ---
            SubSectionArea observacionesArea = subSections.get("OBSERVACIONES");
            System.out.println("=== AUDIOMETRY - OBSERVACIONES -> " + (observacionesArea != null ? "✅" : "❌ NULL"));

            // Extraer fecha desde DATOS PERSONALES
            SubSectionArea personalDataArea = subSections.get("DATOS PERSONALES");
            String examDate = null;

            if (personalDataArea != null) {
                String rawDate = plainTextExtractor.extractTextBetweenLabels(
                        document, "Fecha:", "Ciudad:", section
                );

                System.out.println("rawDate encontrado: '" + rawDate + "'");
                examDate = plainTextExtractor.extractDate(rawDate);
                System.out.println("examDate formateado: '" + examDate + "'");
            } else {
                Map<String, AreaConfig> config = buildAudiometryConfig();
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

            System.out.println("\n=== AUDIOMETRY - Construyendo valores... ===");
            Map<String, String> values = buildDataValues(document, section, subSections);

            System.out.println("\n=== AUDIOMETRY - Valores construidos ===");
            values.forEach((k, v) -> System.out.println("  [" + k + "] -> '" + v + "' " + (v == null ? "❌ NULL" : "✅")));

            System.out.println("\n=== AUDIOMETRY - Guardando en BD... ===");

            ClinicalHistory clinicalHistory = clinicalHistoryService.findOrCreate(worker, examDate);
            Exam exam = examService.save(examName, examDate, worker, clinicalHistory);
            dataValueService.saveAll(values, exam);

            return true;

        } catch (Exception e) {
            System.err.println("❌ ERROR en AudiometryTextBusiness: " + e.getMessage());
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
        Map<String, AreaConfig> config = buildAudiometryConfig();

        // OTOSCOPIA
        SubSectionArea otoscopiaArea = subSections.get("OTOSCOPIA");
        if (otoscopiaArea != null) {
            List<FieldExtractionConfig> fields = buildOtoscopiaFields(config);
            Map<String, String> extracted = plainTextExtractor.extractMultipleValues(
                    document, fields, section, otoscopiaArea
            );

            // LOG: ver qué encontró
            System.out.println("\n=== OTOSCOPIA - Valores extraídos ===");
            extracted.forEach((k, v) ->
                    System.out.println("  [" + k + "] -> '" + v + "' " + (v == null || v.trim().isEmpty() ? "❌ VACÍO" : "✅"))
            );

            values.putAll(extracted);
        }

        // DB(A)
        SubSectionArea dBArea = subSections.get("DB(A)");
        if (dBArea != null) {
            String rawWeightedDecibels = plainTextExtractor.extractTextBetweenLabels(
                    document, "dB(A)", "SEVERIDAD", section
            );


            // Debug
            System.out.println("\n=== DB(A) - Valores extraídos ===");
            System.out.println("  [rawWeightedDecibels] -> '" + rawWeightedDecibels + "' "
                    + (rawWeightedDecibels == null || rawWeightedDecibels.trim().isEmpty() ? "❌ VACÍO" : "✅"));

            String parsed = weightedDecibelsMapper.parseToJson(rawWeightedDecibels);
            System.out.println("  [Ponderado Decibelios (JSON)] -> '" + parsed + "' "
                    + (parsed == null || parsed.trim().isEmpty() ? "❌ VACÍO" : "✅"));

            values.put("Ponderado Decibelios", parsed);
        }

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

        // RECOMENDACION
        SubSectionArea recomendacionArea = subSections.get("RECOMENDACION");
        if (recomendacionArea != null) {
            String rawRecommendations = plainTextExtractor.extractTextBetweenLabels(
                    document, "OBSERVACION", "Observaciones", section
            );

            // Debug
            System.out.println("\n=== RECOMENDACION - Valores extraídos ===");
            System.out.println("  [rawRecommendations] -> '" + rawRecommendations + "' "
                    + (rawRecommendations == null || rawRecommendations.trim().isEmpty() ? "❌ VACÍO" : "✅"));

            String parsed = recommendationMapper.parseToJson(rawRecommendations);
            System.out.println("  [Recomendaciones (JSON)] -> '" + parsed + "' "
                    + (parsed == null || parsed.trim().isEmpty() ? "❌ VACÍO" : "✅"));

            values.put("Recomendaciones", parsed);
        }

        // OBSERVACIONES
        SubSectionArea observacionesArea  = subSections.get("OBSERVACIONES");
        if (observacionesArea != null) {
             String rawObservations = plainTextExtractor.extractTextBetweenLabels(
                    document, "Observaciones", "FONOAUDIÓLOGO", section
            );

            // Debug
            System.out.println("\n=== OBSERVACIONES - Valores extraídos ===");
            System.out.println("  [rawObservations] -> '" + rawObservations + "' "
                    + (rawObservations == null || rawObservations.trim().isEmpty() ? "❌ VACÍO" : "✅"));

            String parsed = observationsMapper.parseToJson(rawObservations);
            System.out.println("  [Observaciones (JSON)] -> '" + parsed + "' "
                    + (parsed == null || parsed.trim().isEmpty() ? "❌ VACÍO" : "✅"));

            values.put("Observaciones", parsed);
        }

        System.out.println("\n=== TOTAL VALUES ACUMULADOS ===");
        values.forEach((k, v) ->
                System.out.println("  [" + k + "] -> '" + v + "'")
        );

        return values;
    }

    private List<FieldExtractionConfig> buildOtoscopiaFields(Map<String, AreaConfig> config) {
        List<FieldExtractionConfig> fields = new ArrayList<>();
        fields.add(new FieldExtractionConfig("Oído derecho:", "Oído derecho",
                config.get("Oído derecho:"), FieldExtractionConfig.ExtractionStrategy.HORIZONTAL));
        fields.add(new FieldExtractionConfig("Oído izquierdo:", "Oído izquierdo",
                config.get("Oído izquierdo:"), FieldExtractionConfig.ExtractionStrategy.HORIZONTAL));
        return fields;
    }

    private List<FieldExtractionConfig> buildCodigoFields(Map<String, AreaConfig> config) {
        List<FieldExtractionConfig> fields = new ArrayList<>();
        fields.add(new FieldExtractionConfig("Nombre del Diagnóstico", "Nombre del Diagnóstico",
                config.get("Nombre del Diagnóstico"), FieldExtractionConfig.ExtractionStrategy.VERTICAL));
        fields.add(new FieldExtractionConfig("Código", "Código de Examen",
                config.get("Código"), FieldExtractionConfig.ExtractionStrategy.VERTICAL));
        return fields;
    }

    private Map<String, AreaConfig> buildAudiometryConfig() {

        Map<String, AreaConfig> config = new HashMap<>();

        config.put("Fecha:", new AreaConfig(0, 100, 10));

        config.put("Oído derecho:", new AreaConfig(0, 200, 21));
        config.put("Oído izquierdo:", new AreaConfig(0, 200, 21));
        config.put("Nombre del Diagnóstico", new AreaConfig(0, 114, 6));
        config.put("Código", new AreaConfig(0, 20, 20));

        return config;
    }

    private static final Set<String> SUB_SECTION_TITLES = Set.of(
            "DATOS PERSONALES",
            "ANTECENDENTES AUDITIVOS LABORALES",
            "ANTECEDENTES FAMILIARES",
            "EXPOSICIÓN RUIDO EXTRALABORAL",
            "CONDICIONES DE TOMA DE LA PRUEBA",
            "OTOSCOPIA",
            "DB(A)",
            "EVERIDAD - CAOHC",
            "CÓDIGO",
            "RECOMENDACION",
            "OBSERVACIONES"
    );
}