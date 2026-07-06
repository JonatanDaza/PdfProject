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
import com.example.PruebaPDFBox.utilities.mapper.MapperJsons.HealthRequirementMapper;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class OccupationalMedicineTextBusiness {

    private final OcrFieldExtractor ocrFieldExtractor;
    private final OcrSubSectionExtractor ocrSubSectionExtractor;
    private final HealthRequirementMapper healthRequirementMapper;
    private final ExamService examService;
    private final ClinicalHistoryService clinicalHistoryService;
    private final DataValueService dataValueService;

    public boolean process(OcrPageContext context, SectionArea section, Worker worker, String examName) {
        try {
            System.out.println("\n=== OCCUPATIONAL - Iniciando proceso ===");

            Map<String, String> subSections = ocrSubSectionExtractor.extract(
                    context, section, SUB_SECTION_TITLES
            );

            subSections.replaceAll((k, v) -> ocrFieldExtractor.normalizeForExtraction(v));

            System.out.println("\n=== OCCUPATIONAL - SubSecciones encontradas ===");
            subSections.forEach((k, v) -> System.out.println(
                    "  [" + k + "] -> " + (v != null && !v.isBlank() ? "✅" : "❌ VACÍO")
            ));

            // Fecha desde DATOS PERSONALES
            String datosPersonales = subSections.get("DATOS PERSONALES");
            String examDate = null;

            if (datosPersonales != null && !datosPersonales.isBlank()) {
                String rawDate = ocrFieldExtractor.extractAfterLabel(datosPersonales, "Fecha:");
                System.out.println("rawDate encontrado: '" + rawDate + "'");
                examDate = ocrFieldExtractor.extractDate(rawDate);
                System.out.println("examDate formateado: '" + examDate + "'");
            }

            if (examDate == null || examDate.isBlank()) {
                String sectionText = context.textForSection(section.startPage(), section.endPage());
                String fallbackRawDate = ocrFieldExtractor.extractAfterLabel(sectionText, "Fecha:");
                if (!fallbackRawDate.isBlank()) {
                    System.out.println("rawDate fallback sección: '" + fallbackRawDate + "'");
                    examDate = ocrFieldExtractor.extractDate(fallbackRawDate);
                    System.out.println("examDate formateado fallback: '" + examDate + "'");
                }
            }

            if ((examDate == null || examDate.isBlank())) {
                String sectionText = context.textForSection(section.startPage(), section.endPage());
                Matcher dateMatcher = Pattern.compile("\\b\\d{2}/\\d{2}/\\d{4}\\b").matcher(sectionText);
                if (dateMatcher.find()) {
                    examDate = dateMatcher.group();
                    System.out.println("examDate encontrado por regex: '" + examDate + "'");
                }
            }

            if (examDate == null || examDate.isBlank()) {
                System.err.println("No se encontró fecha de examen en DATOS PERSONALES");
                return false;
            }

            if (examService.examAlreadyExists(examDate, examName)) {
                System.out.println("Examen ya existe: " + examName + " - " + examDate);
                return false;
            }

            System.out.println("\n=== OCCUPATIONAL - Construyendo valores... ===");
            Map<String, String> values = buildDataValues(subSections);

            System.out.println("\n=== OCCUPATIONAL - Valores construidos ===");
            values.forEach((k, v) -> System.out.println(
                    "  [" + k + "] -> '" + v + "' " + (v == null ? "❌ NULL" : "✅")
            ));

            ClinicalHistory clinicalHistory = clinicalHistoryService.findOrCreate(worker, examDate);
            Exam exam = examService.save(examName, examDate, worker, clinicalHistory);
            dataValueService.saveAll(values, exam);

            return true;

        } catch (Exception e) {
            System.err.println("Error procesando medicina ocupacional: " + e.getMessage());
            return false;
        }
    }

    private Map<String, String> buildDataValues(Map<String, String> subSections) {
        Map<String, String> values = new LinkedHashMap<>();

        // ANTECEDENTES DE EXPOSICIÓN LABORAL
        String exposicionText = subSections.get("ANTECEDENTES DE EXPOSICION LABORAL");
        if (exposicionText == null || exposicionText.isBlank())
            exposicionText = subSections.get("ANTECEDENTES DE EXPOSICIÓN LABORAL");
        if (exposicionText != null && !exposicionText.isBlank()) {
            values.put("Tiempo de Exposición", ocrFieldExtractor.extractExposureTime(exposicionText));
        }

        // ACTIVIDAD FÍSICA
        String actividadText = subSections.get("ACTIVIDAD FÍSICA");
        if (actividadText != null && !actividadText.isBlank()) {
            values.put("Actividades Manuales",
                ocrFieldExtractor.extractRowAnswer(actividadText, "ACTIVIDADES MANUALES"));
            values.put("Ejercicios o Deportes",
                ocrFieldExtractor.extractRowAnswer(actividadText, "EJERCICIOS O DEPORTES"));

            System.out.println("  [Actividades Manuales] -> '" + values.get("Actividades Manuales") + "'");
            System.out.println("  [Ejercicios o Deportes] -> '" + values.get("Ejercicios o Deportes") + "'");
        }

        // HÁBITOS TÓXICOS
        String habitosText = subSections.get("HÁBITOS TÓXICOS");
        if (habitosText != null && !habitosText.isBlank()) {
            values.put("Consumidor de Alcohol",
                    ocrFieldExtractor.extractRowAnswer(habitosText, "CONSUMIDOR DE ALCOHOL"));
            values.put("Fumador Actual",
                    ocrFieldExtractor.extractRowAnswer(habitosText, "FUMADOR ACTUAL"));

            System.out.println("  [Consumidor de Alcohol] -> '" + values.get("Consumidor de Alcohol") + "'");
            System.out.println("  [Fumador Actual] -> '" + values.get("Fumador Actual") + "'");
        }

        // EXAMEN FISICO
        String examenText = subSections.get("EXAMEN FISICO");
        if (examenText != null && !examenText.isBlank()) {
            values.put("Tensión Arterial",
                    ocrFieldExtractor.extractAfterLabel(examenText, "Tension Arterial:"));
            values.put("Frecuencia Cardíaca",
                    ocrFieldExtractor.extractOnlyNumbers(
                            ocrFieldExtractor.extractNumberAfterLabel(examenText, "Frecuencia Cardiaca:")));
            values.put("Frecuencia Respiratoria",
                    ocrFieldExtractor.extractOnlyNumbers(
                            ocrFieldExtractor.extractNumberAfterLabel(examenText, "Frecuencia Respiratoria:")));
            values.put("Lateralidad Dominante",
                    ocrFieldExtractor.extractAfterLabel(examenText, "Lateralidad Dominante:"));
            values.put("Peso",
                    ocrFieldExtractor.extractOnlyNumbers(
                            ocrFieldExtractor.extractNumberAfterLabel(examenText, "Peso:")));
            values.put("Talla",
                    ocrFieldExtractor.extractOnlyNumbers(
                            ocrFieldExtractor.extractNumberAfterLabel(examenText, "Talla:")));
            values.put("IMC",
                    ocrFieldExtractor.extractAfterLabel(examenText, "IMC:"));
            String perimetro = ocrFieldExtractor.extractOnlyNumbers(
                    ocrFieldExtractor.extractAfterLabel(examenText, "Perimetro Abdominal:"));
            if (!perimetro.isBlank()) values.put("Perímetro Abdominal", perimetro);
            System.out.println("  [Tensión Arterial]       -> '" + values.get("Tensión Arterial") + "'");
            System.out.println("  [Frecuencia Cardíaca]    -> '" + values.get("Frecuencia Cardíaca") + "'");
            System.out.println("  [Frecuencia Respiratoria]-> '" + values.get("Frecuencia Respiratoria") + "'");
            System.out.println("  [Lateralidad Dominante]  -> '" + values.get("Lateralidad Dominante") + "'");
            System.out.println("  [Peso]                   -> '" + values.get("Peso") + "'");
            System.out.println("  [Talla]                  -> '" + values.get("Talla") + "'");
            System.out.println("  [IMC]                    -> '" + values.get("IMC") + "'");
            System.out.println("  [Perímetro Abdominal]    -> '" + values.get("Perímetro Abdominal") + "'");
        }

        // CONCEPTO OCUPACIONAL
        String conceptoText = subSections.get("CONCEPTO OCUPACIONAL");
        if (conceptoText != null && !conceptoText.isBlank()) {
            values.put("Concepto Ocupacional",
                    ocrFieldExtractor.extractOccupationalConcept(conceptoText));
        }

        // CÓDIGO / CUV
        String codigoText = subSections.get("CODIGO");
        if (codigoText == null || codigoText.isBlank())
            codigoText = subSections.get("CÓDIGO");
        if (codigoText != null && !codigoText.isBlank()) {
            values.put("Código de Examen",   ocrFieldExtractor.extractDiagnosisCode(codigoText));
            values.put("Nombre del Diagnóstico", ocrFieldExtractor.extractDiagnosisName(codigoText));
        }

        // REQUISITO SALUD
        String requisitoText = subSections.get("REQUISITO SALUD");
        if (requisitoText != null && !requisitoText.isBlank()) {
            System.out.println("===== TEXTO OCR =====");
            System.out.println(requisitoText);
            System.out.println("=====================");
            String rawHealth = ocrFieldExtractor.extractHealthRequirements(requisitoText);
            System.out.println("===== RAW HEALTH =====");
            System.out.println(rawHealth);
            System.out.println("======================");
            values.put("Requisitos de Salud", healthRequirementMapper.parseToJson(rawHealth));
        }

        return values;
    }

    // Los títulos en mayúsculas — OcrSubSectionExtractor los busca con toUpperCase()
    private static final Set<String> SUB_SECTION_TITLES = Set.of(
            "DATOS PERSONALES",
            "Antecedentes de Exposición Laboral (El primero corresponde a la última labor o labor actual)",
            "ANTECEDENTES DE EXPOSICION LABORAL",
            "Motivo Consulta",
            "MOTIVO CONSULTA",
            "ANTECEDENTES FAMILIARES",
            "ANTECEDENTES PERSONALES",
            "Antecedentes Personales por Sistemas",
            "ANTECEDENTES PERSONALES POR SISTEMAS",
            "Requisito Salud",
            "REQUISITO SALUD",
            "Concepto Ocupacional",
            "CONCEPTO OCUPACIONAL",
            "Revisión Por Sistemas",
            "REVISIÓN POR SISTEMAS",
            "REVISION POR SISTEMAS",
            "Examen Fisico",
            "EXAMEN FISICO",
            "ACTIVIDAD FÍSICA",
            "ACTIVIDAD FISICA",
            "HÁBITOS TÓXICOS",
            "HABITOS TOXICOS",
            "Código",
            "CÓDIGO",
            "CODIGO"
            // "ELEMENTOS DE PROTECCION PERSONAL",
            // "Elementos de Protección Personal (En la última labor o labor actual)",
            // "OBSERVACIONES ADICIONALES A LOS ANTECEDENTES",
            // "Observaciones Adicionales a los Antecedente"
            // "CONSULTAS REALIZADOS",
            // "LABORATORIOS REALIZADOS",
            // "Observaciones de los Exámenes Paraclínicos y/o Procedimientos Realizados en Ésta Atención",
            // "EXÁMENES REALIZADOS",
            // "EXAMENES REALIZADOS",
            // "Ánalisis y Plan de Tratamiento",
            // "ANALISIS Y PLAN DE TRATAMIENTO",
            // "Ampliación de los Hallazgos",
            // "AMPLIACIÓN DE LOS HALLAZGOS",
            // "AMPLIACION DE LOS HALLAZGOS",
    );
}