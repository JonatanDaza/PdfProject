package com.example.PruebaPDFBox.business;

import com.example.PruebaPDFBox.record.AreaConfig;
import com.example.PruebaPDFBox.record.FieldExtractionConfig;
import com.example.PruebaPDFBox.record.SectionArea;
import com.example.PruebaPDFBox.model.*;
import com.example.PruebaPDFBox.repository.ClinicalHistoryRepository;
import com.example.PruebaPDFBox.repository.ExamRepository;
import com.example.PruebaPDFBox.utilities.extractor.SectionTextExtractor;
import com.example.PruebaPDFBox.utilities.mapper.HealthRequirementMapper;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class OccupationalMedicineBusiness {

    private final SectionTextExtractor sectionTextExtractor;
    private final ExamRepository examRepository;
    private final ClinicalHistoryRepository clinicalHistoryRepository;
    private final HealthRequirementMapper healthRequirementMapper;

    public OccupationalMedicineBusiness(
            SectionTextExtractor sectionTextExtractor,
            ExamRepository examRepository,
            ClinicalHistoryRepository clinicalHistoryRepository,
            HealthRequirementMapper healthRequirementMapper
    ) {
        this.sectionTextExtractor = sectionTextExtractor;
        this.examRepository = examRepository;
        this.clinicalHistoryRepository = clinicalHistoryRepository;
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

        // Extraer fecha
        String examDate = sectionTextExtractor.extractDate(data.get("fecha"));

        if (examDate == null || examDate.trim().isEmpty()) {
            return false;
        }

        if (examRepository.findByExamDateAndExamName(examDate, examName).isPresent()) {
            return false;
        }

        ClinicalHistory hc = clinicalHistoryRepository.findByWorkerAndExamDate(worker, examDate)
                .orElseGet(() -> clinicalHistoryRepository.save(new ClinicalHistory()));

        DataExam dataExam = new DataExam();
        Exam exam = new Exam();

        exam.setExamName(examName);
        exam.setExamDate(examDate);
        exam.setClinicalHistory(hc);
        exam.setWorker(worker);
        exam.setDataExam(dataExam);

        // Mapear valores extraídos
        dataExam.setYear(sectionTextExtractor.extractOnlyNumbers(data.get("tiempoExposicion")));
        dataExam.setManualActivities(data.get("actividadesManuales"));
        dataExam.setPhysicalActivity(data.get("ejerciciosDeportes"));
        dataExam.setAlcoholHabits(data.get("consumidorAlcohol"));
        dataExam.setSmokingHabits(data.get("fumadorActual"));

        dataExam.setBloodPressure(data.get("tensionArterial"));
        dataExam.setHeartRate(sectionTextExtractor.extractOnlyNumbers(data.get("frecuenciaCardiaca")));
        dataExam.setRespiratoryRate(sectionTextExtractor.extractOnlyNumbers(data.get("frecuenciaRespiratoria")));
        dataExam.setDominantLaterality(data.get("lateralidadDominante"));
        dataExam.setAbdominalPerimeter(sectionTextExtractor.extractOnlyNumbers(data.get("perimetroAbdominal")));

        dataExam.setWeight(sectionTextExtractor.extractOnlyNumbers(data.get("peso")));
        dataExam.setSize(sectionTextExtractor.extractOnlyNumbers(data.get("talla")));
        dataExam.setIMC(data.get("imc"));

        dataExam.setDiagnostics(data.get("diagnosticos"));
        dataExam.setOccupationalConcept(data.get("conceptoOcupacional"));
        dataExam.setExamCode(data.get("codigo"));

        // Este requiere recorrido separado por su naturaleza especial
        String rawHealthRequirements = sectionTextExtractor.extractTextBetweenLabels(
                document, "Cumple", "Restricciones Laborales", section
        );
        String healthRequirementsJson = healthRequirementMapper.parseToJson(rawHealthRequirements);
        dataExam.setHealthRequirements(healthRequirementsJson);

        examRepository.save(exam);
        return true;
    }

    private List<FieldExtractionConfig> buildFieldConfigurations(Map<String, AreaConfig> config) {
        List<FieldExtractionConfig> fields = new ArrayList<>();

        // HORIZONTAL
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

        // VERTICAL
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

//
//    public boolean process(
//            PDDocument document,
//            SectionArea section,
//            Worker worker,
//            String examName
//    ) throws Exception {
//
//        Map<String, AreaConfig> config = buildEvaluationConfig();
//
//        String rawDate = sectionTextExtractor.extractValue(document, "Fecha:", section, config.get("Fecha:"));
//        String examDate = sectionTextExtractor.extractDate(rawDate);
//
//        if (examDate == null || examDate.trim().isEmpty()) {
//            return false;
//        }
//
//        if (examRepository.findByExamDateAndExamName(examDate, examName).isPresent()) {
//            return false;
//        }
//
//        ClinicalHistory hc = clinicalHistoryRepository.findByWorkerAndExamDate(worker, examDate)
//                .orElseGet(() -> clinicalHistoryRepository.save(new ClinicalHistory()));
//
//        // Entidades del examen
//        DataExam dataExam = new DataExam();
//        Exam exam = new Exam();
//
//        //Asignar nombre del examen
//        exam.setExamName(examName);
//        exam.setExamDate(examDate);
//
//        exam.setClinicalHistory(hc);
//        exam.setWorker(worker);
//        exam.setDataExam(dataExam);
//
//        dataExam.setYear(sectionTextExtractor.extractOnlyNumbers(sectionTextExtractor.extractValueVertical(document, "Tiempo Exposición", section, config.get("Tiempo Exposición"))));
//        dataExam.setManualActivities(sectionTextExtractor.extractValueVertical(document, "ACTIVIDADES MANUALES", section, config.get("ACTIVIDADES MANUALES")));
//        dataExam.setPhysicalActivity(sectionTextExtractor.extractValue(document, "EJERCICIOS O DEPORTES", section, config.get("EJERCICIOS O DEPORTES")));
//        dataExam.setAlcoholHabits(sectionTextExtractor.extractValue(document, "CONSUMIDOR DE ALCOHOL", section, config.get("CONSUMIDOR DE ALCOHOL")));
//        dataExam.setSmokingHabits(sectionTextExtractor.extractValue(document, "FUMADOR ACTUAL", section, config.get("FUMADOR ACTUAL")));
//
//        dataExam.setBloodPressure(sectionTextExtractor.extractValue(document, "Tension Arterial:", section, config.get("Tension Arterial:")));
//        dataExam.setHeartRate(sectionTextExtractor.extractOnlyNumbers(sectionTextExtractor.extractValue(document, "Frecuencia Cardiaca:", section, config.get("Frecuencia Cardiaca:"))));
//        dataExam.setRespiratoryRate(sectionTextExtractor.extractOnlyNumbers(sectionTextExtractor.extractValue(document, "Frecuencia Respiratoria:", section, config.get("Frecuencia Respiratoria:"))));
//        dataExam.setDominantLaterality(sectionTextExtractor.extractValue(document, "Lateralidad Dominante:", section, config.get("Lateralidad Dominante:")));
//        dataExam.setAbdominalPerimeter(sectionTextExtractor.extractOnlyNumbers(sectionTextExtractor.extractValue(document, "Perimetro Abdominal:", section, config.get("Perimetro Abdominal:"))));
//
//        dataExam.setWeight(sectionTextExtractor.extractOnlyNumbers(sectionTextExtractor.extractValue(document, "Peso:", section, config.get("Peso:"))));
//        dataExam.setSize(sectionTextExtractor.extractOnlyNumbers(sectionTextExtractor.extractValue(document, "Talla:", section, config.get("Talla:"))));
//        dataExam.setIMC(sectionTextExtractor.extractValue(document, "IMC:", section, config.get("IMC:")));
//
//        dataExam.setDiagnostics(sectionTextExtractor.extractValueVertical(document, "Diagnósticos", section, config.get("Diagnósticos")));
//        dataExam.setOccupationalConcept(sectionTextExtractor.extractValueVertical(document, "Concepto Ocupacional", section, config.get("Concepto Ocupacional")));
//
//        dataExam.setExamCode(sectionTextExtractor.extractValueVertical(document, "Código", section, config.get("Código")));
//
//        // Extraer texto de requisitos de salud y convertir a JSON
//        String rawHealthRequirements = sectionTextExtractor.extractTextBetweenLabels(document, "Cumple", "Restricciones Laborales", section);
//        String healthRequirementsJson = healthRequirementMapper.parseToJson(rawHealthRequirements);
//        dataExam.setHealthRequirements(healthRequirementsJson);
//
//        examRepository.save(exam);
//        return true;
//    }

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

        config.put("Código",new AreaConfig(0, 60, 20));

        return config;
    }
}
