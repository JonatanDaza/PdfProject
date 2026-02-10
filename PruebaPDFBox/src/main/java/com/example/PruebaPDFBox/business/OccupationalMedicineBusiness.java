package com.example.PruebaPDFBox.business;

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
import com.example.PruebaPDFBox.utilities.extractor.TextExtractor.SectionTextExtractor;
import com.example.PruebaPDFBox.utilities.mapper.MapperJsons.HealthRequirementMapper;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class OccupationalMedicineBusiness {

    private final SectionTextExtractor sectionTextExtractor;
    private final DataTypeRepository dataTypeRepository;
    private final DataValueRepository dataValueRepository;
    private final ClinicalHistoryRepository clinicalHistoryRepository;
    private final ExamRepository examRepository;
    private final HealthRequirementMapper healthRequirementMapper;

    public OccupationalMedicineBusiness(
            SectionTextExtractor sectionTextExtractor,
            DataTypeRepository dataTypeRepository,
            DataValueRepository dataValueRepository,
            ClinicalHistoryRepository clinicalHistoryRepository,
            ExamRepository examRepository,
            HealthRequirementMapper healthRequirementMapper
    ) {
        this.sectionTextExtractor = sectionTextExtractor;
        this.dataTypeRepository = dataTypeRepository;
        this.dataValueRepository = dataValueRepository;
        this.clinicalHistoryRepository = clinicalHistoryRepository;
        this.examRepository = examRepository;
        this.healthRequirementMapper = healthRequirementMapper;
    }

    public boolean process(
            PDDocument document,
            SectionArea section,
            Worker worker,
            String examName
    ) throws Exception {

        Map<String, AreaConfig> config = buildEvaluationConfig();
        List<FieldExtractionConfig> fields = buildFieldConfigurations(config);
        Map<String, String> data = sectionTextExtractor.extractMultipleValues(document, fields, section);

        String examDate = sectionTextExtractor.extractDate(data.get("fecha"));

        if (examDate == null || examDate.trim().isEmpty()) {
            return false;
        }

        if (examRepository.findByExamDateAndExamName(examDate, examName).isPresent()) {
            return false;
        }

        ClinicalHistory clinicalHistory = clinicalHistoryRepository
                .findByWorkerAndExamDate(worker, examDate)
                .orElseGet(() -> clinicalHistoryRepository.save(new ClinicalHistory()));

        Exam exam = new Exam();
        exam.setExamName(examName);
        exam.setExamDate(examDate);
        exam.setClinicalHistory(clinicalHistory);
        exam.setWorker(worker);
        exam = examRepository.save(exam);

        createDataValue("Tiempo de Exposición",
                sectionTextExtractor.extractOnlyNumbers(data.get("tiempoExposicion")),
                exam);

        createDataValue("Actividades Manuales",
                data.get("actividadesManuales"),
                exam);

        createDataValue("Ejercicios o Deportes",
                data.get("ejerciciosDeportes"),
                exam);

        createDataValue("Consumidor de Alcohol",
                data.get("consumidorAlcohol"),
                exam);

        createDataValue("Fumador Actual",
                data.get("fumadorActual"),
                exam);

        createDataValue("Tensión Arterial",
                data.get("tensionArterial"),
                exam);

        createDataValue("Frecuencia Cardíaca",
                sectionTextExtractor.extractOnlyNumbers(data.get("frecuenciaCardiaca")),
                exam);

        createDataValue("Frecuencia Respiratoria",
                sectionTextExtractor.extractOnlyNumbers(data.get("frecuenciaRespiratoria")),
                exam);

        createDataValue("Lateralidad Dominante",
                data.get("lateralidadDominante"),
                exam);

        createDataValue("Perímetro Abdominal",
                sectionTextExtractor.extractOnlyNumbers(data.get("perimetroAbdominal")),
                exam);

        createDataValue("Peso",
                sectionTextExtractor.extractOnlyNumbers(data.get("peso")),
                exam);

        createDataValue("Talla",
                sectionTextExtractor.extractOnlyNumbers(data.get("talla")),
                exam);

        createDataValue("IMC",
                data.get("imc"),
                exam);

        createDataValue("Diagnósticos",
                data.get("diagnosticos"),
                exam);

        createDataValue("Concepto Ocupacional",
                data.get("conceptoOcupacional"),
                exam);

        createDataValue("Código de Examen",
                data.get("codigo"),
                exam);

        String rawHealthRequirements = sectionTextExtractor.extractTextBetweenLabels(
                document, "Cumple", "Restricciones Laborales", section
        );
        String healthRequirementsJson = healthRequirementMapper.parseToJson(rawHealthRequirements);
        createDataValue("Requisitos de Salud", healthRequirementsJson, exam);

        return true;
    }

    private void createDataValue(String typeName, String value, Exam exam) {
        if (value == null || value.trim().isEmpty()) {
            return;
        }

        DataType dataType = dataTypeRepository.findByName(typeName)
                .orElseGet(() -> {
                    DataType newType = new DataType();
                    newType.setName(typeName);
                    return dataTypeRepository.save(newType);
                });

        DataValue dataValue = new DataValue();
        dataValue.setValue(value);
        dataValue.setDataType(dataType);
        dataValue.setExam(exam);

        dataValueRepository.save(dataValue);
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
