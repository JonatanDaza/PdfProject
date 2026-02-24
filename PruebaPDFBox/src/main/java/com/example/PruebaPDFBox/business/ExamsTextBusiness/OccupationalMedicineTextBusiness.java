package com.example.PruebaPDFBox.business.ExamsTextBusiness;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import com.example.PruebaPDFBox.utilities.mapper.MapperJsons.HealthRequirementMapper;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OccupationalMedicineTextBusiness {

    private final PlainTextExtractor plainTextExtractor;
    private final HealthRequirementMapper healthRequirementMapper;
    private final ExamService examService;
    private final ClinicalHistoryService clinicalHistoryService;
    private final DataValueService dataValueService;
    private final PdfSubSectionExtractor pdfSubSectionExtractor;

    public boolean process(PDDocument document, SectionArea section, Worker worker) {
        try {
            // Extraer subsecciones primero
            Map<String, SubSectionArea> subSections = pdfSubSectionExtractor.extract(
                    document, section, SUB_SECTION_TITLES
            );

            // Extraer fecha desde DATOS PERSONALES
            SubSectionArea datosPersonalesArea = subSections.get("DATOS PERSONALES");
            String examDate = null;

            if (datosPersonalesArea != null) {
                Map<String, AreaConfig> config = buildEvaluationConfig();
                List<FieldExtractionConfig> dateFields = new ArrayList<>();
                dateFields.add(new FieldExtractionConfig(
                        "Fecha:", "Fecha:",
                        config.get("Fecha:"),
                        FieldExtractionConfig.ExtractionStrategy.EXACT_MATCH
                ));

                Map<String, String> dateData = plainTextExtractor.extractMultipleValues(
                        document, dateFields, section, datosPersonalesArea
                );

                String rawDate = dateData.get("Fecha:");
                examDate = plainTextExtractor.extractDate(rawDate);
            }

            if (examDate == null || examDate.isEmpty()) {
                System.err.println("No se encontró fecha de examen en DATOS PERSONALES");
                return false;
            }

            String examName = "EVALUACIÓN MEDICA OCUPACIONAL";
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
        Map<String, AreaConfig> config = buildEvaluationConfig();

        // ANTECEDENTES DE EXPOSICIÓN LABORAL
        SubSectionArea exposicionArea = subSections.get("ANTECEDENTES DE EXPOSICIÓN LABORAL (EL PRIMERO CORRESPONDE A LA ÚLTIMA LABOR O LABOR ACTUAL)");
        if (exposicionArea != null) {
            List<FieldExtractionConfig> fields = buildExposicionFields(config);
            Map<String, String> extracted = plainTextExtractor.extractMultipleValues(
                    document, fields, section, exposicionArea
            );
            applyNumberTransformations(extracted);

            values.putAll(extracted);
        }

        // ACTIVIDAD FÍSICA
        SubSectionArea actividadFisicaArea = subSections.get("ACTIVIDAD FÍSICA");
        if (actividadFisicaArea != null) {
            List<FieldExtractionConfig> fields = buildActividadFisicaFields(config);
            Map<String, String> extracted = plainTextExtractor.extractMultipleValues(
                    document, fields, section, actividadFisicaArea
            );

            values.putAll(extracted);
        }

        // HÁBITOS TÓXICOS
        SubSectionArea habitosArea = subSections.get("HÁBITOS TÓXICOS");
        if (habitosArea != null) {
            List<FieldExtractionConfig> fields = buildHabitosToxicosFields(config);
            Map<String, String> extracted = plainTextExtractor.extractMultipleValues(
                    document, fields, section, habitosArea
            );

            values.putAll(extracted);
        }

        // EXAMEN FISICO
        SubSectionArea examenFisicoArea = subSections.get("EXAMEN FISICO");
        if (examenFisicoArea != null) {
            List<FieldExtractionConfig> fields = buildExamenFisicoFields(config);
            Map<String, String> extracted = plainTextExtractor.extractMultipleValues(
                    document, fields, section, examenFisicoArea
            );
            applyNumberTransformations(extracted);

            values.putAll(extracted);
        }

        // CONCEPTO OCUPACIONAL
        SubSectionArea conceptoArea = subSections.get("CONCEPTO OCUPACIONAL");
        if (conceptoArea != null) {
            List<FieldExtractionConfig> fields = buildConceptoOcupacionalFields(config);
            Map<String, String> extracted = plainTextExtractor.extractMultipleValues(
                    document, fields, section, conceptoArea
            );

            values.putAll(extracted);
        }

        // CUV
        SubSectionArea cuvArea = subSections.get("CÓDIGO");
        if (cuvArea != null) {
            List<FieldExtractionConfig> fields = buildCuvFields(config);
            Map<String, String> extracted = plainTextExtractor.extractMultipleValues(
                    document, fields, section, cuvArea
            );

            values.putAll(extracted);
        }

        // REQUISITO SALUD
        SubSectionArea requisitoArea = subSections.get("REQUISITO SALUD");
        if (requisitoArea != null) {
            String rawHealthRequirements = plainTextExtractor.extractTextBetweenLabels(
                    document, "Cumple", "Restricciones Laborales", section
            );
            values.put("Requisitos de Salud", healthRequirementMapper.parseToJson(rawHealthRequirements));
        }

        return values;
    }

    private List<FieldExtractionConfig> buildExposicionFields(Map<String, AreaConfig> config) {
        List<FieldExtractionConfig> fields = new ArrayList<>();
        fields.add(new FieldExtractionConfig("Tiempo Exposición", "Tiempo de Exposición",
                config.get("Tiempo Exposición"), FieldExtractionConfig.ExtractionStrategy.VERTICAL));
        return fields;
    }

    private List<FieldExtractionConfig> buildActividadFisicaFields(Map<String, AreaConfig> config) {
        List<FieldExtractionConfig> fields = new ArrayList<>();
        fields.add(new FieldExtractionConfig("ACTIVIDADES MANUALES", "Actividades Manuales",
                config.get("ACTIVIDADES MANUALES"), FieldExtractionConfig.ExtractionStrategy.HORIZONTAL));
        fields.add(new FieldExtractionConfig("EJERCICIOS O DEPORTES", "Ejercicios o Deportes",
                config.get("EJERCICIOS O DEPORTES"), FieldExtractionConfig.ExtractionStrategy.HORIZONTAL));
        return fields;
    }

    private List<FieldExtractionConfig> buildHabitosToxicosFields(Map<String, AreaConfig> config) {
        List<FieldExtractionConfig> fields = new ArrayList<>();
        fields.add(new FieldExtractionConfig("CONSUMIDOR DE ALCOHOL", "Consumidor de Alcohol",
                config.get("CONSUMIDOR DE ALCOHOL"), FieldExtractionConfig.ExtractionStrategy.HORIZONTAL));
        fields.add(new FieldExtractionConfig("FUMADOR ACTUAL", "Fumador Actual",
                config.get("FUMADOR ACTUAL"), FieldExtractionConfig.ExtractionStrategy.HORIZONTAL));
        return fields;
    }

    private List<FieldExtractionConfig> buildExamenFisicoFields(Map<String, AreaConfig> config) {
        List<FieldExtractionConfig> fields = new ArrayList<>();
        fields.add(new FieldExtractionConfig("Tension Arterial:", "Tensión Arterial",
                config.get("Tension Arterial:"), FieldExtractionConfig.ExtractionStrategy.HORIZONTAL));
        fields.add(new FieldExtractionConfig("Frecuencia Cardiaca:", "Frecuencia Cardíaca",
                config.get("Frecuencia Cardiaca:"), FieldExtractionConfig.ExtractionStrategy.HORIZONTAL));
        fields.add(new FieldExtractionConfig("Frecuencia Respiratoria:", "Frecuencia Respiratoria",
                config.get("Frecuencia Respiratoria:"), FieldExtractionConfig.ExtractionStrategy.HORIZONTAL));
        fields.add(new FieldExtractionConfig("Lateralidad Dominante:", "Lateralidad Dominante",
                config.get("Lateralidad Dominante:"), FieldExtractionConfig.ExtractionStrategy.HORIZONTAL));
        fields.add(new FieldExtractionConfig("Perimetro Abdominal:", "Perímetro Abdominal",
                config.get("Perimetro Abdominal:"), FieldExtractionConfig.ExtractionStrategy.EXACT_MATCH));
        fields.add(new FieldExtractionConfig("Peso:", "Peso",
                config.get("Peso:"), FieldExtractionConfig.ExtractionStrategy.HORIZONTAL));
        fields.add(new FieldExtractionConfig("Talla:", "Talla",
                config.get("Talla:"), FieldExtractionConfig.ExtractionStrategy.HORIZONTAL));
        fields.add(new FieldExtractionConfig("IMC:", "IMC",
                config.get("IMC:"), FieldExtractionConfig.ExtractionStrategy.HORIZONTAL));
        return fields;
    }

    private List<FieldExtractionConfig> buildConceptoOcupacionalFields(Map<String, AreaConfig> config) {
        List<FieldExtractionConfig> fields = new ArrayList<>();
        fields.add(new FieldExtractionConfig("Concepto Ocupacional", "Concepto Ocupacional",
                config.get("Concepto Ocupacional"), FieldExtractionConfig.ExtractionStrategy.VERTICAL));
        return fields;
    }

    private List<FieldExtractionConfig> buildCuvFields(Map<String, AreaConfig> config) {
        List<FieldExtractionConfig> fields = new ArrayList<>();
        fields.add(new FieldExtractionConfig("Diagnósticos", "Diagnósticos",
                config.get("Diagnósticos"), FieldExtractionConfig.ExtractionStrategy.VERTICAL));
        fields.add(new FieldExtractionConfig("Código", "Código de Examen",
                config.get("Código"), FieldExtractionConfig.ExtractionStrategy.VERTICAL));
        return fields;
    }

    private Map<String, AreaConfig> buildEvaluationConfig() {
        Map<String, AreaConfig> config = new HashMap<>();

        config.put("Fecha:", new AreaConfig(0, 100, 10));
        config.put("Tiempo Exposición", new AreaConfig(527, 10, 30)); // 1  -- metodo numero
        config.put("ACTIVIDADES MANUALES", new AreaConfig(240, 373, 14)); // 2
        config.put("EJERCICIOS O DEPORTES", new AreaConfig(240, 373, 14)); // 3
        config.put("CONSUMIDOR DE ALCOHOL", new AreaConfig(240, 373, 14)); // 4
        config.put("FUMADOR ACTUAL", new AreaConfig(240, 373, 14)); // 5
        config.put("Tension Arterial:", new AreaConfig(40, 80, 12)); // 6
        config.put("Frecuencia Cardiaca:", new AreaConfig(40, 80, 12)); // 7 -- metodo numero
        config.put("Frecuencia Respiratoria:", new AreaConfig(40, 80, 12)); // 8 -- metodo numero
        config.put("Lateralidad Dominante:", new AreaConfig(40, 120, 12)); // 9
        config.put("Perimetro Abdominal:", new AreaConfig(40, 80, 12)); // 10 -- metodo numero
        config.put("Peso:", new AreaConfig(10, 60, 12)); // 11 -- metodo numero
        config.put("Talla:", new AreaConfig(0, 60, 12)); // 12 -- metodo numero
        config.put("IMC:", new AreaConfig(40, 60, 12)); // 13
        config.put("Diagnósticos", new AreaConfig(-30, 144, 10)); // 14
        config.put("Concepto Ocupacional", new AreaConfig(0, 144, 12)); // 15
        config.put("Código", new AreaConfig(0, 20, 5)); // 16

        return config;
    }

    private static final Set<String> NUMBER_ONLY_FIELDS = Set.of(
            "Tiempo de Exposición",
            "Perímetro Abdominal",
            "Peso",
            "Frecuencia Respiratoria",
            "Frecuencia Cardíaca",
            "Talla"
    );

    private void applyNumberTransformations(Map<String, String> extracted) {
        for (String field : NUMBER_ONLY_FIELDS) {
            String val = extracted.get(field);
            if (val != null && !val.trim().isEmpty()) {
                String result = plainTextExtractor.extractOnlyNumbers(val);
                extracted.put(field, result != null ? result : "");
            }
        }
    }

    private static final Set<String> SUB_SECTION_TITLES = Set.of(
            "DATOS PERSONALES",
            "Motivo Consulta",
            "Elementos de Protección Personal (En la última labor o labor actual)",
            "Antecedentes de Exposición Laboral (El primero corresponde a la última labor o labor actual)",
            "ANTECEDENTES FAMILIARES",
            "ANTECEDENTES PERSONALES",
            "Antecedentes Personales por Sistemas",
            "Observaciones Adicionales a los Antecedente",
            "Revisión Por Sistemas",
            "ACTIVIDAD FÍSICA",
            "HÁBITOS TÓXICOS",
            "Examen Fisico",
            "Ampliación de los Hallazgos",
            "CONSULTAS REALIZADOS",
            "EXÁMENES REALIZADOS",
            "LABORATORIOS REALIZADOS",
            "Observaciones de los Exámenes Paraclínicos y/o Procedimientos Realizados en Ésta Atención",
            "Código",
            "Ánalisis y Plan de Tratamiento",
            "Concepto Ocupacional",
            "Requisito Salud"
    );
}