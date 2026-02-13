package com.example.PruebaPDFBox.business.ExamsTextBusiness;

import com.example.PruebaPDFBox.model.Worker;
import com.example.PruebaPDFBox.model.Exam;
import com.example.PruebaPDFBox.model.DataType;
import com.example.PruebaPDFBox.model.DataValue;
import com.example.PruebaPDFBox.model.ClinicalHistory;
import com.example.PruebaPDFBox.record.AreaConfig;
import com.example.PruebaPDFBox.record.FieldExtractionConfig;
import com.example.PruebaPDFBox.record.SectionArea;
import com.example.PruebaPDFBox.repository.DataTypeRepository;
import com.example.PruebaPDFBox.repository.DataValueRepository;
import com.example.PruebaPDFBox.repository.ClinicalHistoryRepository;
import com.example.PruebaPDFBox.repository.ExamRepository;
import com.example.PruebaPDFBox.service.OccupationalMedicineTextService;
import com.example.PruebaPDFBox.utilities.extractor.TextExtractor.PlainTextExtractor;
import com.example.PruebaPDFBox.utilities.mapper.MapperJsons.HealthRequirementMapper;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@RequiredArgsConstructor
public class OccupationalMedicineTextBusiness {

    private final PlainTextExtractor plainTextExtractor;
    private final HealthRequirementMapper healthRequirementMapper;
    private final OccupationalMedicineTextService occupationalMedicineTextService;

    public boolean process(
            PDDocument document,
            SectionArea section,
            Worker worker,
            String examName
    ) throws Exception {

        // 1. Extraer texto del PDF
        Map<String, AreaConfig> config = buildEvaluationConfig();
        List<FieldExtractionConfig> fields = buildFieldConfigurations(config);
        Map<String, String> data = plainTextExtractor.extractMultipleValues(document, fields, section);

        // 2. Validar fecha — lógica de negocio
        String examDate = plainTextExtractor.extractDate(data.get("fecha"));
        if (examDate == null || examDate.trim().isEmpty()) {
            return false;
        }

        // 3. Validar duplicado — delega al service
        if (occupationalMedicineTextService.examAlreadyExists(examDate, examName)) {
            return false;
        }

        // 4. Persistir examen — delega al service
        Exam exam = occupationalMedicineTextService.saveExam(examName, examDate, worker);

        // 5. Preparar y persistir DataValues — business transforma, service persiste
        Map<String, String> dataValues = buildDataValues(data, document, section);
        occupationalMedicineTextService.saveAllDataValues(dataValues, exam);

        return true;
    }

    private Map<String, String> buildDataValues(
            Map<String, String> data,
            PDDocument document,
            SectionArea section
    ) throws Exception {

        Map<String, String> values = new LinkedHashMap<>();

        values.put("Tiempo de Exposición",
                plainTextExtractor.extractOnlyNumbers(
                data.get("tiempoExposicion"))
        );

        values.put("Actividades Manuales",
                data.get("actividadesManuales")
        );

        values.put("Ejercicios o Deportes",
                data.get("ejerciciosDeportes")
        );

        values.put("Consumidor de Alcohol",
                data.get("consumidorAlcohol")
        );

        values.put("Fumador Actual",
                data.get("fumadorActual")
        );

        values.put("Tensión Arterial",
                data.get("tensionArterial")
        );

        values.put("Frecuencia Cardíaca",
                plainTextExtractor.extractOnlyNumbers(
                data.get("frecuenciaCardiaca"))
        );

        values.put("Frecuencia Respiratoria",
                plainTextExtractor.extractOnlyNumbers(
                data.get("frecuenciaRespiratoria"))
        );

        values.put("Lateralidad Dominante",
                data.get("lateralidadDominante")
        );

        values.put("Perímetro Abdominal",
                plainTextExtractor.extractOnlyNumbers(
                data.get("perimetroAbdominal"))
        );

        values.put("Peso",
                plainTextExtractor.extractOnlyNumbers(
                data.get("peso"))
        );

        values.put("Talla",
                plainTextExtractor.extractOnlyNumbers(
                data.get("talla"))
        );

        values.put("IMC",
                data.get("imc")
        );

        values.put("Diagnósticos",
                data.get("diagnosticos")
        );

        values.put("Concepto Ocupacional",
                data.get("conceptoOcupacional")
        );

        values.put("Código de Examen",
                data.get("codigo")
        );

        // Procesamiento especial — lógica de negocio, se queda en Business
        String rawHealthRequirements = plainTextExtractor.extractTextBetweenLabels(
                document, "Cumple", "Restricciones Laborales", section
        );
        values.put("Requisitos de Salud", healthRequirementMapper.parseToJson(rawHealthRequirements));

        return values;
    }

    private List<FieldExtractionConfig> buildFieldConfigurations(Map<String, AreaConfig> config) {
        List<FieldExtractionConfig> fields = new ArrayList<>();

        fields.add(new FieldExtractionConfig("Fecha:", "fecha",
                config.get("Fecha:"), FieldExtractionConfig.ExtractionStrategy.HORIZONTAL));
        fields.add(new FieldExtractionConfig("EJERCICIOS O DEPORTES", "ejerciciosDeportes",
                config.get("EJERCICIOS O DEPORTES"), FieldExtractionConfig.ExtractionStrategy.HORIZONTAL));
        fields.add(new FieldExtractionConfig("CONSUMIDOR DE ALCOHOL", "consumidorAlcohol",
                config.get("CONSUMIDOR DE ALCOHOL"), FieldExtractionConfig.ExtractionStrategy.HORIZONTAL));
        fields.add(new FieldExtractionConfig("FUMADOR ACTUAL", "fumadorActual",
                config.get("FUMADOR ACTUAL"), FieldExtractionConfig.ExtractionStrategy.HORIZONTAL));
        fields.add(new FieldExtractionConfig("Tension Arterial:", "tensionArterial",
                config.get("Tension Arterial:"), FieldExtractionConfig.ExtractionStrategy.HORIZONTAL));
        fields.add(new FieldExtractionConfig("Frecuencia Cardiaca:", "frecuenciaCardiaca",
                config.get("Frecuencia Cardiaca:"), FieldExtractionConfig.ExtractionStrategy.HORIZONTAL));
        fields.add(new FieldExtractionConfig("Frecuencia Respiratoria:", "frecuenciaRespiratoria",
                config.get("Frecuencia Respiratoria:"), FieldExtractionConfig.ExtractionStrategy.HORIZONTAL));
        fields.add(new FieldExtractionConfig("Lateralidad Dominante:", "lateralidadDominante",
                config.get("Lateralidad Dominante:"), FieldExtractionConfig.ExtractionStrategy.HORIZONTAL));
        fields.add(new FieldExtractionConfig("Perimetro Abdominal:", "perimetroAbdominal",
                config.get("Perimetro Abdominal:"), FieldExtractionConfig.ExtractionStrategy.HORIZONTAL));
        fields.add(new FieldExtractionConfig("Peso:", "peso",
                config.get("Peso:"), FieldExtractionConfig.ExtractionStrategy.HORIZONTAL));
        fields.add(new FieldExtractionConfig("Talla:", "talla",
                config.get("Talla:"), FieldExtractionConfig.ExtractionStrategy.HORIZONTAL));
        fields.add(new FieldExtractionConfig("IMC:", "imc",
                config.get("IMC:"), FieldExtractionConfig.ExtractionStrategy.HORIZONTAL));
        fields.add(new FieldExtractionConfig("Tiempo Exposición", "tiempoExposicion",
                config.get("Tiempo Exposición"), FieldExtractionConfig.ExtractionStrategy.VERTICAL));
        fields.add(new FieldExtractionConfig("ACTIVIDADES MANUALES", "actividadesManuales",
                config.get("ACTIVIDADES MANUALES"), FieldExtractionConfig.ExtractionStrategy.VERTICAL));
        fields.add(new FieldExtractionConfig("Diagnósticos", "diagnosticos",
                config.get("Diagnósticos"), FieldExtractionConfig.ExtractionStrategy.VERTICAL));
        fields.add(new FieldExtractionConfig("Concepto Ocupacional", "conceptoOcupacional",
                config.get("Concepto Ocupacional"), FieldExtractionConfig.ExtractionStrategy.VERTICAL));
        fields.add(new FieldExtractionConfig("Código", "codigo",
                config.get("Código"), FieldExtractionConfig.ExtractionStrategy.VERTICAL));

        return fields;
    }

    private Map<String, AreaConfig> buildEvaluationConfig() {
        Map<String, AreaConfig> config = new HashMap<>();

        config.put("Fecha:", new AreaConfig(0, 100, 10));
        config.put("Tiempo Exposición", new AreaConfig(527, 10, 30));
        config.put("ACTIVIDADES MANUALES", new AreaConfig(240, 241, 14));
        config.put("EJERCICIOS O DEPORTES", new AreaConfig(240, 373, 14));
        config.put("CONSUMIDOR DE ALCOHOL", new AreaConfig(240, 373, 14));
        config.put("FUMADOR ACTUAL", new AreaConfig(240, 373, 14));
        config.put("Tension Arterial:", new AreaConfig(40, 80, 12));
        config.put("Frecuencia Cardiaca:", new AreaConfig(40, 80, 12));
        config.put("Frecuencia Respiratoria:", new AreaConfig(40, 80, 12));
        config.put("Lateralidad Dominante:", new AreaConfig(40, 120, 12));
        config.put("Perimetro Abdominal:", new AreaConfig(40, 80, 12));
        config.put("Peso:", new AreaConfig(10, 60, 12));
        config.put("Talla:", new AreaConfig(0, 60, 12));
        config.put("IMC:", new AreaConfig(40, 60, 12));
        config.put("Diagnósticos", new AreaConfig(78, 20, 20));
        config.put("Concepto Ocupacional", new AreaConfig(0, 194, 20));
        config.put("Código", new AreaConfig(0, 60, 20));

        return config;
    }
}
