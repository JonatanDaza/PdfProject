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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
            values.put("Oído derecho", ocrFieldExtractor.extractEarResult(otoscopiaText, "Oído derecho:"));
            values.put("Oído izquierdo", ocrFieldExtractor.extractEarResult(otoscopiaText, "Oido izquierdo:"));
        }

        // DB(A) — todo el bloque entre "dB(A)" y "SEVERIDAD"
        String dbText = subSections.get("DB(A)");
        if (dbText == null || dbText.isBlank()) dbText = subSections.get("dB(A)");
        if (dbText != null && !dbText.isBlank()) {
            String vaodLine = extractVaLine(dbText, "VAOD");
            String vaoiLine = extractVaLine(dbText, "VAOI");

            if (!vaodLine.isBlank() || !vaoiLine.isBlank()) {
                // Construye el texto en formato que WeightedDecibelsMapper puede procesar
                String formattedDb = (vaodLine.isBlank() ? "" : "VAOD " + vaodLine + "\n")
                                + (vaoiLine.isBlank() ? "" : "VAOI " + vaoiLine);
                values.put("Ponderado Decibelios", weightedDecibelsMapper.parseToJson(formattedDb));
            } else {
                // Fallback: pasa el bloque completo limpio
                String cleanDb = dbText
                        .replaceAll("(?m)^\\s*-?\\d+\\s*$", "")
                        .replaceAll("[|()\\[\\]<>xX]", " ")
                        .replaceAll("[ \\t]{2,}", " ")
                        .trim();
                values.put("Ponderado Decibelios", weightedDecibelsMapper.parseToJson(cleanDb));
            }
            System.out.println("  [Ponderado Decibelios] -> '" + values.get("Ponderado Decibelios") + "'");
        }

        // CÓDIGO / CUV
        String codigoText = subSections.get("CODIGO");
        if (codigoText == null || codigoText.isBlank())
            codigoText = subSections.get("CÓDIGO");
        if (codigoText != null && !codigoText.isBlank()) {
            values.put("Código de Examen",   ocrFieldExtractor.extractDiagnosisCode(codigoText));
            values.put("Nombre del Diagnóstico", ocrFieldExtractor.extractDiagnosisName(codigoText));
        }

        // RECOMENDACION
        String recomText = subSections.get("RECOMENDACION");
        if (recomText != null && !recomText.isBlank()) {
            String rawRec = recomText;

            rawRec = rawRec.replaceFirst(
                    "(?i)^RECOMENDACION\\s*OBSERVACION",
                    "");

            rawRec = rawRec.replaceAll(
                    "(?m)^\\d+\\s*\\.?$",
                    "");

            rawRec = rawRec.trim();

            String parsed =
                    recommendationMapper.parseToJson(rawRec);

            values.put("Recomendaciones", parsed);
        }

        // OBSERVACIONES
        String obsText = subSections.get("OBSERVACIONES");
        if (obsText == null || obsText.isBlank()) obsText = subSections.get("Observaciones");
        if (obsText != null && !obsText.isBlank()) {
            String rawObs = ocrFieldExtractor.extractAudiometryObservations(obsText);
            if (!rawObs.isBlank()) {
                values.put("Observaciones", observationsMapper.parseToJson(rawObs));
                System.out.println("  [Observaciones] -> '" + values.get("Observaciones") + "'");
            }
        }

        return values;
    }

    private String extractVaLine(String text, String vaLabel) {
        Pattern p = Pattern.compile(
                "(?i)" + vaLabel + "\\s+([\\d\\s]+)",
                Pattern.CASE_INSENSITIVE
        );
        Matcher m = p.matcher(text);
        if (m.find()) {
            return m.group(1).trim().replaceAll("\\s+", " ");
        }
        return "";
    }

    private static final Set<String> SUB_SECTION_TITLES = Set.of(
            "DATOS PERSONALES",
            "ANTECENDENTES AUDITIVOS LABORALES",
            "ANTECEDENTES FAMILIARES",
            "EXPOSICIÓN RUIDO EXTRALABORAL",
            "CONDICIONES DE TOMA DE LA PRUEBA",
            "OTOSCOPIA",
            "DB(A)",
            "dB(A)",
        //     "EVERIDAD - CAOHC",
            "SEVERIDAD - CAOHC",
            "CÓDIGO",
            "Código",
            "RECOMENDACION",
            "RECOMENDACION OBSERVACION",
            "OBSERVACIONES",
            "Observaciones"
    );
}