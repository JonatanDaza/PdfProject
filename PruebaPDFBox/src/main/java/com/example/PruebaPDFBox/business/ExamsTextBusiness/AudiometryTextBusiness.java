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
import com.example.PruebaPDFBox.utilities.mapper.MapperJsons.ObservationsMapper;
import com.example.PruebaPDFBox.utilities.mapper.MapperJsons.RecommendationMapper;
import com.example.PruebaPDFBox.utilities.mapper.MapperJsons.WeightedDecibelsMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@RequiredArgsConstructor
public class AudiometryTextBusiness {

    private final OcrFieldExtractor ocrFieldExtractor;
    private final OcrSubSectionExtractor ocrSubSectionExtractor;
    private final WeightedDecibelsMapper weightedDecibelsMapper;
    private final ObservationsMapper observationsMapper;
    private final RecommendationMapper recommendationMapper;
    private final ExamService examService;
    private final ClinicalHistoryService clinicalHistoryService;
    private final DataValueService dataValueService;

    public boolean process(OcrPageContext context, SectionArea section, Worker worker, String examName) {
        try {
            System.out.println("\n=== AUDIOMETRY - Iniciando proceso ===");

            Map<String, String> subSections = ocrSubSectionExtractor.extract(
                    context, section, SUB_SECTION_TITLES
            );

            subSections.replaceAll((k, v) -> ocrFieldExtractor.normalizeForExtraction(v));

            System.out.println("\n=== AUDIOMETRY - SubSecciones encontradas ===");
            subSections.forEach((k, v) -> System.out.println(
                    "  [" + k + "] -> " + (v != null && !v.isBlank() ? "✅" : "❌ VACÍO")
            ));

            // Fecha — busca en DATOS PERSONALES, si no existe busca en el texto completo
            String datosPersonales = subSections.get("DATOS PERSONALES");
            String examDate = null;

            if (datosPersonales != null && !datosPersonales.isBlank()) {
                String rawDate = ocrFieldExtractor.extractBetweenLabels(datosPersonales, "Fecha:", "Ciudad:");
                System.out.println("rawDate encontrado: '" + rawDate + "'");
                examDate = ocrFieldExtractor.extractDate(rawDate);
                System.out.println("examDate formateado: '" + examDate + "'");

                if (examDate == null || examDate.isBlank()) {
                    // Fallback: try without punctuation labels (OCR may drop ':')
                    rawDate = ocrFieldExtractor.extractBetweenLabels(datosPersonales, "Fecha", "Ciudad");
                    System.out.println("rawDate fallback (sin ':') encontrado: '" + rawDate + "'");
                    examDate = ocrFieldExtractor.extractDate(rawDate);
                    System.out.println("examDate fallback formateado: '" + examDate + "'");
                }
            }

            // If still missing, try to find any date pattern in the whole section text
            if (examDate == null || examDate.isBlank()) {
                String fullSection = context.textForSection(section.startPage(), section.endPage());
                System.out.println("Intentando extraer fecha desde texto completo de la sección...");
                examDate = ocrFieldExtractor.extractDate(fullSection);
                System.out.println("Fecha encontrada en texto completo: '" + examDate + "'");
            }

            if (examDate == null || examDate.isBlank()) {
                System.err.println("No se encontró fecha de examen");
                return false;
            }

            if (examService.examAlreadyExists(examDate, examName)) {
                System.out.println("Examen ya existe: " + examName + " - " + examDate);
                return false;
            }

            System.out.println("\n=== AUDIOMETRY - Construyendo valores... ===");
            Map<String, String> values = buildDataValues(subSections);

            System.out.println("\n=== AUDIOMETRY - Valores construidos ===");
            values.forEach((k, v) -> System.out.println("  [" + k + "] -> '" + v + "'"));

            ClinicalHistory clinicalHistory = clinicalHistoryService.findOrCreate(worker, examDate);
            Exam exam = examService.save(examName, examDate, worker, clinicalHistory);
            dataValueService.saveAll(values, exam);

            return true;

        } catch (Exception e) {
            System.err.println("ERROR en AudiometryTextBusiness: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private Map<String, String> buildDataValues(Map<String, String> subSections) {
        Map<String, String> values = new LinkedHashMap<>();

        // OTOSCOPIA
        String otoscopiaText = subSections.get("OTOSCOPIA");
        if (otoscopiaText != null && !otoscopiaText.isBlank()) {
            values.put("Oído derecho",
                    ocrFieldExtractor.extractAfterLabel(otoscopiaText, "Oído derecho:"));
            values.put("Oído izquierdo",
                    ocrFieldExtractor.extractAfterLabel(otoscopiaText, "Oído izquierdo:"));

            System.out.println("  [Oído derecho]   -> '" + values.get("Oído derecho") + "'");
            System.out.println("  [Oído izquierdo] -> '" + values.get("Oído izquierdo") + "'");
        }

        // DB(A) — todo el bloque entre "dB(A)" y "SEVERIDAD"
        String dbText = subSections.get("DB(A)");
        if (dbText != null && !dbText.isBlank()) {
            String rawDB = ocrFieldExtractor.extractBetweenLabels(dbText, "dB(A)", "SEVERIDAD");
            if (rawDB.isBlank()) rawDB = dbText;

            String parsed = weightedDecibelsMapper.parseToJson(rawDB);
            values.put("Ponderado Decibelios", parsed);

            System.out.println("  [Ponderado Decibelios] -> '" + parsed + "'");
        }

        // CÓDIGO / CUV
        String codigoText = subSections.get("CÓDIGO");
        if (codigoText != null && !codigoText.isBlank()) {
            values.put("Nombre del Diagnóstico",
                    ocrFieldExtractor.extractAfterLabel(codigoText, "Nombre del Diagnóstico"));
            values.put("Código de Examen",
                    ocrFieldExtractor.extractAfterLabel(codigoText, "Código"));

            System.out.println("  [Nombre del Diagnóstico] -> '" + values.get("Nombre del Diagnóstico") + "'");
            System.out.println("  [Código de Examen]       -> '" + values.get("Código de Examen") + "'");
        }

        // RECOMENDACION
        String recomText = subSections.get("RECOMENDACION");
        if (recomText != null && !recomText.isBlank()) {
            String rawRec = ocrFieldExtractor.extractBetweenLabels(recomText, "OBSERVACION", "Observaciones");
            if (rawRec.isBlank()) rawRec = recomText;

            String parsed = recommendationMapper.parseToJson(rawRec);
            values.put("Recomendaciones", parsed);

            System.out.println("  [Recomendaciones] -> '" + parsed + "'");
        }

        // OBSERVACIONES
        String obsText = subSections.get("OBSERVACIONES");
        if (obsText != null && !obsText.isBlank()) {
            String rawObs = ocrFieldExtractor.extractBetweenLabels(obsText, "Observaciones", "FONOAUDIÓLOGO");
            if (rawObs.isBlank()) rawObs = obsText;

            String parsed = observationsMapper.parseToJson(rawObs);
            values.put("Observaciones", parsed);

            System.out.println("  [Observaciones] -> '" + parsed + "'");
        }

        return values;
    }

    private static final Set<String> SUB_SECTION_TITLES = Set.of(
            "DATOS PERSONALES",
            "ANTECENDENTES AUDITIVOS LABORALES",
            "ANTECEDENTES FAMILIARES",
            "EXPOSICIÓN RUIDO EXTRALABORAL",
            "CONDICIONES DE TOMA DE LA PRUEBA",
            "OTOSCOPIA",
            "DB(A)",
        //     "EVERIDAD - CAOHC",
            "SEVERIDAD - CAOHC",
            "CÓDIGO",
            "RECOMENDACION",
            "OBSERVACIONES"
    );
}