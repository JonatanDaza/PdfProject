package com.example.PruebaPDFBox.business.AudiometryBusiness;

import com.example.PruebaPDFBox.model.ClinicalHistory;
import com.example.PruebaPDFBox.model.DataType;
import com.example.PruebaPDFBox.model.DataValue;
import com.example.PruebaPDFBox.model.Exam;
import com.example.PruebaPDFBox.model.Worker;
import com.example.PruebaPDFBox.record.AreaConfig;
import com.example.PruebaPDFBox.record.FieldExtractionConfig;
import com.example.PruebaPDFBox.record.SectionArea;
import com.example.PruebaPDFBox.repository.ClinicalHistoryRepository;
import com.example.PruebaPDFBox.repository.DataTypeRepository;
import com.example.PruebaPDFBox.repository.DataValueRepository;
import com.example.PruebaPDFBox.repository.ExamRepository;
import com.example.PruebaPDFBox.utilities.extractor.TextExtractor.PlainTextExtractor;
import com.example.PruebaPDFBox.utilities.mapper.MapperJsons.ObservationsMapper;
import com.example.PruebaPDFBox.utilities.mapper.MapperJsons.RecommendationMapper;
import com.example.PruebaPDFBox.utilities.mapper.MapperJsons.WeightedDecibelsMapper;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class AudiometryTextBusiness {

    private final PlainTextExtractor plainTextExtractor;
    private final ExamRepository examRepository;
    private final ClinicalHistoryRepository clinicalHistoryRepository;
    private final WeightedDecibelsMapper weightedDecibelsMapper;
    private final ObservationsMapper observationsMapper;
    private final RecommendationMapper recommendationMapper;
    private final DataTypeRepository dataTypeRepository;
    private final DataValueRepository dataValueRepository;

    public AudiometryTextBusiness(
            PlainTextExtractor plainTextExtractor,
            ExamRepository examRepository,
            ClinicalHistoryRepository clinicalHistoryRepository,
            WeightedDecibelsMapper weightedDecibelsMapper,
            ObservationsMapper observationsMapper,
            RecommendationMapper recommendationMapper,
            DataTypeRepository dataTypeRepository,
            DataValueRepository dataValueRepository) {
        this.plainTextExtractor = plainTextExtractor;
        this.examRepository = examRepository;
        this.clinicalHistoryRepository = clinicalHistoryRepository;
        this.weightedDecibelsMapper = weightedDecibelsMapper;
        this.observationsMapper = observationsMapper;
        this.recommendationMapper = recommendationMapper;
        this.dataTypeRepository = dataTypeRepository;
        this.dataValueRepository = dataValueRepository;
    }

    public boolean process(
            PDDocument document,
            SectionArea section,
            Worker worker,
            String examName
    ) throws Exception {

        Map<String, AreaConfig> config = buildAudiometryConfig();

        List<FieldExtractionConfig> fields = buildFieldConfigurations(config);
        Map<String, String> data = plainTextExtractor.extractMultipleValues(document, fields, section);

        // Extraccion de fecha
        String examDate = plainTextExtractor.extractDate(data.get("fecha"));

        if (examDate == null || examDate.trim().isEmpty()) {
            return false;
        }

        if (examRepository.findByExamDateAndExamName(examDate, examName).isPresent()) {
            return false;
        }

        ClinicalHistory hc = clinicalHistoryRepository.findByWorkerAndExamDate(worker, examDate)
                .orElseGet(() -> clinicalHistoryRepository.save(new ClinicalHistory()));


        Exam exam = new Exam();
        exam.setExamName(examName);
        exam.setExamDate(examDate);
        exam.setClinicalHistory(hc);
        exam.setWorker(worker);
        exam = examRepository.save(exam);

        createDataValue("Oído derecho",
                data.get("oidoDerecho"),
                exam);
        createDataValue("Oído izquierdo",data.get("oidoIzquierdo"),
                exam);
        createDataValue("Nombre del Diagnóstico",data.get("diagnosticos"),
                exam);

        // Recorridos especiales
        String rawWeightedDecibels = plainTextExtractor.extractTextBetweenLabels(
                document, "dB(A)", "SEVERIDAD", section
        );
        String weightedDecibelsJson = weightedDecibelsMapper.parseToJson(rawWeightedDecibels);
        createDataValue("Ponderado Decibelios", weightedDecibelsJson, exam);

        String rawRecommendations = plainTextExtractor.extractTextBetweenLabels(
                document, "OBSERVACION", "Observaciones", section
        );
        String recommendationsJson = recommendationMapper.parseToJson(rawRecommendations);
        createDataValue("Recomendaciones", recommendationsJson, exam);

        String rawObservations = plainTextExtractor.extractTextBetweenLabels(
                document, "Observaciones", "FONOAUDIÓLOGO", section
        );
        String observationsJson = observationsMapper.parseToJson(rawObservations);
        createDataValue("Observaciones", observationsJson, exam);

//        createDataValue("Código de Examen",
//                data.get("Código"),
//                exam);

        examRepository.save(exam);
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
        fields.add(new FieldExtractionConfig("Oído derecho:", "oidoDerecho",
                config.get("Oído derecho:"), FieldExtractionConfig.ExtractionStrategy.HORIZONTAL));
        fields.add(new FieldExtractionConfig("Oído izquierdo:", "oidoIzquierdo",
                config.get("Oído izquierdo:"), FieldExtractionConfig.ExtractionStrategy.HORIZONTAL));
        fields.add(new FieldExtractionConfig("Nombre del Diagnóstico", "diagnosticos",
                config.get("Nombre del Diagnóstico"), FieldExtractionConfig.ExtractionStrategy.VERTICAL));
//        fields.add(new FieldExtractionConfig("Código", "Código",
//                config.get("Código"), FieldExtractionConfig.ExtractionStrategy.VERTICAL));

        return fields;
    }

    private Map<String, AreaConfig> buildAudiometryConfig() {

        Map<String, AreaConfig> config = new HashMap<>();

        config.put("Fecha:", new AreaConfig(0, 100, 12));

        config.put("Oído derecho:", new AreaConfig(0, 200, 21));
        config.put("Oído izquierdo:", new AreaConfig(0, 200, 21));
        config.put("Nombre del Diagnóstico", new AreaConfig(60, 130, 21));
//        config.put("Código", new AreaConfig(0, 60, 20));

        return config;
    }
}
