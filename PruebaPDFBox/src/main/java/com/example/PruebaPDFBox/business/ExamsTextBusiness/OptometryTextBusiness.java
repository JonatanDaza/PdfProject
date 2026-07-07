package com.example.PruebaPDFBox.business.ExamsTextBusiness;

import com.example.PruebaPDFBox.model.ClinicalHistory;
import com.example.PruebaPDFBox.model.Exam;
import com.example.PruebaPDFBox.model.Worker;
import com.example.PruebaPDFBox.record.OcrPageContext;
import com.example.PruebaPDFBox.record.SectionArea;
import com.example.PruebaPDFBox.service.ServiceEntity.ClinicalHistoryService;
import com.example.PruebaPDFBox.service.ServiceEntity.DataValueService;
import com.example.PruebaPDFBox.service.ServiceEntity.ExamService;
import com.example.PruebaPDFBox.utilities.extractor.SectionExtractor.OcrSubSectionExtractor;
import com.example.PruebaPDFBox.utilities.extractor.TextExtractor.OcrFieldExtractor;
import com.example.PruebaPDFBox.utilities.mapper.MapperJsons.RecommendationsConductMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@RequiredArgsConstructor
public class OptometryTextBusiness {

    private final OcrFieldExtractor ocrFieldExtractor;
    private final OcrSubSectionExtractor ocrSubSectionExtractor;
    private final RecommendationsConductMapper recommendationsConductMapper;
    private final ExamService examService;
    private final ClinicalHistoryService clinicalHistoryService;
    private final DataValueService dataValueService;

    public boolean process(OcrPageContext context, SectionArea section, Worker worker, String examName) {
        try {
            System.out.println("\n=== OPTOMETRY - Iniciando proceso ===");

            Map<String, String> subSections = ocrSubSectionExtractor.extract(
                    context, section, SUB_SECTION_TITLES
            );
            
            subSections.replaceAll((k, v) -> ocrFieldExtractor.normalizeForExtraction(v));

            System.out.println("\n=== OPTOMETRY - SubSecciones encontradas ===");
            subSections.forEach((k, v) -> System.out.println(
                    "  [" + k + "] -> " + (v != null && !v.isBlank() ? "✅" : "❌ VACÍO")
            ));

            // Fecha — busca entre "Fecha:" y "Municipio:" en DATOS PERSONALES
            String examDate = null;
            String datosPersonales = subSections.get("DATOS PERSONALES");

            if (datosPersonales != null && !datosPersonales.isBlank()) {
                String rawDate = ocrFieldExtractor.extractBetweenLabels(datosPersonales, "Fecha:", "Municipio:");
                if (rawDate.isBlank()) {
                    rawDate = ocrFieldExtractor.extractAfterLabel(datosPersonales, "Fecha:");
                }
                System.out.println("rawDate encontrado: '" + rawDate + "'");
                examDate = ocrFieldExtractor.extractDate(rawDate);
                System.out.println("examDate formateado: '" + examDate + "'");
            }

            // Fallback: buscar en el texto completo de la sección
            if (examDate == null || examDate.isBlank()) {
                String fullText = context.textForSection(section.startPage(), section.endPage());
                String rawDate = ocrFieldExtractor.extractBetweenLabels(fullText, "Fecha:", "Municipio:");
                if (rawDate.isBlank()) {
                    rawDate = ocrFieldExtractor.extractAfterLabel(fullText, "Fecha:");
                }
                examDate = ocrFieldExtractor.extractDate(rawDate);
                System.out.println("examDate (fallback) formateado: '" + examDate + "'");
            }

            if (examDate == null || examDate.isBlank()) {
                System.err.println("No se encontró fecha de examen en DATOS PERSONALES");
                return false;
            }

            if (examService.examAlreadyExists(examDate, examName)) {
                System.out.println("Examen ya existe: " + examName + " - " + examDate);
                return false;
            }

            System.out.println("\n=== OPTOMETRY - Construyendo valores... ===");
            Map<String, String> values = buildDataValues(subSections);

            System.out.println("\n=== OPTOMETRY - Valores construidos ===");
            values.forEach((k, v) -> System.out.println(
                    "  [" + k + "] -> '" + v + "' " + (v == null ? "❌ NULL" : "✅")
            ));

            System.out.println("\n=== OPTOMETRY - Guardando en BD... ===");

            ClinicalHistory clinicalHistory = clinicalHistoryService.findOrCreate(worker, examDate);
            Exam exam = examService.save(examName, examDate, worker, clinicalHistory);
            dataValueService.saveAll(values, exam);

            return true;

        } catch (Exception e) {
            System.err.println("ERROR en OptometryTextBusiness: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private Map<String, String> buildDataValues(Map<String, String> subSections) {
        Map<String, String> values = new LinkedHashMap<>();

        // CÓDIGO / CUV
        String codigoText = subSections.get("CODIGO");
        if (codigoText == null || codigoText.isBlank())
            codigoText = subSections.get("CÓDIGO");
        if (codigoText != null && !codigoText.isBlank()) {
            values.put("Código de Examen",   ocrFieldExtractor.extractDiagnosisCode(codigoText));
            values.put("Nombre del Diagnóstico", ocrFieldExtractor.extractDiagnosisName(codigoText));
        }

        // CONDUCTA Y/O RECOMENDACIONES
        String conductaText = subSections.get("CONDUCTA Y/O RECOMENDACIONES");
        if (conductaText != null && !conductaText.isBlank()) {
            // Recorta hasta "OTRAS OBSERVACIONES" si aparece dentro del bloque
            String rawConduct = ocrFieldExtractor.extractBetweenLabels(
                    conductaText, "CONDUCTA Y/O RECOMENDACIONES", "OTRAS OBSERVACIONES Y RECOMENDACIONES:"
            );
            // Si no encontró el recorte, usa el bloque completo
            if (rawConduct.isBlank()) rawConduct = conductaText;

            String parsed = recommendationsConductMapper.parseToJson(rawConduct);
            values.put("Conducta Y/O Recomendaciones", parsed);

            System.out.println("\n=== CONDUCTA Y/O RECOMENDACIONES - Valores extraídos ===");
            System.out.println("  [rawConduct]                      -> '" + rawConduct + "'");
            System.out.println("  [Conducta Y/O Recomendaciones JSON]-> '" + parsed + "'");
        }

        System.out.println("\n=== TOTAL VALUES ACUMULADOS ===");
        values.forEach((k, v) -> System.out.println("  [" + k + "] -> '" + v + "'"));

        return values;
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