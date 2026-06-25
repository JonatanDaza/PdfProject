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
        String exposicionText = subSections.get("ANTECEDENTES DE EXPOSICIÓN LABORAL");
        if (exposicionText != null && !exposicionText.isBlank()) {
            String tiempoRaw = ocrFieldExtractor.extractAfterLabel(exposicionText, "Tiempo de Exposición");
            String tiempo = ocrFieldExtractor.extractOnlyNumbers(tiempoRaw);
            values.put("Tiempo de Exposición", tiempo);

            System.out.println("  [Tiempo de Exposición] -> '" + tiempo + "'");
        }

        // ACTIVIDAD FÍSICA
        String actividadText = subSections.get("ACTIVIDAD FÍSICA");
        if (actividadText != null && !actividadText.isBlank()) {
            values.put("Actividades Manuales",
                    ocrFieldExtractor.extractAfterLabel(actividadText, "ACTIVIDADES MANUALES"));
            values.put("Ejercicios o Deportes",
                    ocrFieldExtractor.extractAfterLabel(actividadText, "EJERCICIOS O DEPORTES"));

            System.out.println("  [Actividades Manuales] -> '" + values.get("Actividades Manuales") + "'");
            System.out.println("  [Ejercicios o Deportes] -> '" + values.get("Ejercicios o Deportes") + "'");
        }

        // HÁBITOS TÓXICOS
        String habitosText = subSections.get("HÁBITOS TÓXICOS");
        if (habitosText != null && !habitosText.isBlank()) {
            values.put("Consumidor de Alcohol",
                    ocrFieldExtractor.extractAfterLabel(habitosText, "CONSUMIDOR DE ALCOHOL"));
            values.put("Fumador Actual",
                    ocrFieldExtractor.extractAfterLabel(habitosText, "FUMADOR ACTUAL"));

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
                            ocrFieldExtractor.extractAfterLabel(examenText, "Frecuencia Cardiaca:")));
            values.put("Frecuencia Respiratoria",
                    ocrFieldExtractor.extractOnlyNumbers(
                            ocrFieldExtractor.extractAfterLabel(examenText, "Frecuencia Respiratoria:")));
            values.put("Lateralidad Dominante",
                    ocrFieldExtractor.extractAfterLabel(examenText, "Lateralidad Dominante:"));
            values.put("Perímetro Abdominal",
                    ocrFieldExtractor.extractOnlyNumbers(
                            ocrFieldExtractor.extractAfterLabel(examenText, "Perimetro Abdominal:")));
            values.put("Peso",
                    ocrFieldExtractor.extractOnlyNumbers(
                            ocrFieldExtractor.extractAfterLabel(examenText, "Peso:")));
            values.put("Talla",
                    ocrFieldExtractor.extractOnlyNumbers(
                            ocrFieldExtractor.extractAfterLabel(examenText, "Talla:")));
            values.put("IMC",
                    ocrFieldExtractor.extractAfterLabel(examenText, "IMC:"));

            System.out.println("  [Tensión Arterial]       -> '" + values.get("Tensión Arterial") + "'");
            System.out.println("  [Frecuencia Cardíaca]    -> '" + values.get("Frecuencia Cardíaca") + "'");
            System.out.println("  [Frecuencia Respiratoria]-> '" + values.get("Frecuencia Respiratoria") + "'");
            System.out.println("  [Lateralidad Dominante]  -> '" + values.get("Lateralidad Dominante") + "'");
            System.out.println("  [Perímetro Abdominal]    -> '" + values.get("Perímetro Abdominal") + "'");
            System.out.println("  [Peso]                   -> '" + values.get("Peso") + "'");
            System.out.println("  [Talla]                  -> '" + values.get("Talla") + "'");
            System.out.println("  [IMC]                    -> '" + values.get("IMC") + "'");
        }

        // CONCEPTO OCUPACIONAL
        String conceptoText = subSections.get("CONCEPTO OCUPACIONAL");
        if (conceptoText != null && !conceptoText.isBlank()) {
            // El concepto es todo el bloque — no hay label interno, es el texto mismo
            values.put("Concepto Ocupacional", conceptoText.trim());
            System.out.println("  [Concepto Ocupacional] -> '" + conceptoText.trim() + "'");
        }

        // CÓDIGO / CUV
        String codigoText = subSections.get("CÓDIGO");
        if (codigoText != null && !codigoText.isBlank()) {
            values.put("Nombre del Diagnóstico",
                    ocrFieldExtractor.extractAfterLabel(codigoText, "Diagnósticos"));
            values.put("Código de Examen",
                    ocrFieldExtractor.extractAfterLabel(codigoText, "Código"));

            System.out.println("  [Nombre del Diagnóstico] -> '" + values.get("Nombre del Diagnóstico") + "'");
            System.out.println("  [Código de Examen]       -> '" + values.get("Código de Examen") + "'");
        }

        // REQUISITO SALUD
        String requisitoText = subSections.get("REQUISITO SALUD");
        if (requisitoText != null && !requisitoText.isBlank()) {

            String rawHealth;

            if (requisitoText.contains("SIN REQUISITOS DE SALUD")) {
                // Extrae todo hasta "SIN REQUISITOS DE SALUD" (incluyéndolo)
                int index = requisitoText.indexOf("SIN REQUISITOS DE SALUD");
                rawHealth = requisitoText.substring(0, index + "SIN REQUISITOS DE SALUD".length());
            } else {
                // Lógica actual
                rawHealth = ocrFieldExtractor.extractBetweenLabels(
                        requisitoText,
                        "Requisito Salud Cumple",
                        "Restricciones Laborales"
                );

                // Si no encontró el recorte, usa el bloque completo
                if (rawHealth.isBlank()) {
                    rawHealth = requisitoText;
                }
            }

            values.put("Requisitos de Salud", healthRequirementMapper.parseToJson(rawHealth));
            System.out.println("  [Requisitos de Salud] -> '" + values.get("Requisitos de Salud") + "'");
        }
        return values;
    }

    // Los títulos en mayúsculas — OcrSubSectionExtractor los busca con toUpperCase()
    private static final Set<String> SUB_SECTION_TITLES = Set.of(
            "DATOS PERSONALES",
            "Motivo Consulta",
            "Elementos de Protección Personal (En la última labor o labor actual)",
            "Antecedentes de Exposición Laboral (El primero corresponde a la última labor o labor actual)",
            "MOTIVO CONSULTA",
            "ANTECEDENTES DE EXPOSICIÓN LABORAL",
            "ANTECEDENTES FAMILIARES",
            "ANTECEDENTES PERSONALES",
            "Antecedentes Personales por Sistemas",
            "Observaciones Adicionales a los Antecedente",
            "Revisión Por Sistemas",
            "ANTECEDENTES PERSONALES POR SISTEMAS",
            "REVISIÓN POR SISTEMAS",
            "ACTIVIDAD FÍSICA",
            "HÁBITOS TÓXICOS",
            "Examen Fisico",
            "Ampliación de los Hallazgos",
            "EXAMEN FISICO",
            "AMPLIACIÓN DE LOS HALLAZGOS",
            "CONSULTAS REALIZADOS",
            "EXÁMENES REALIZADOS",
            "LABORATORIOS REALIZADOS",
            "Observaciones de los Exámenes Paraclínicos y/o Procedimientos Realizados en Ésta Atención",
            "Código",
            "Ánalisis y Plan de Tratamiento",
            "Concepto Ocupacional",
            "Requisito Salud",
            "CÓDIGO",
            "CONCEPTO OCUPACIONAL",
            "REQUISITO SALUD"
    );
}