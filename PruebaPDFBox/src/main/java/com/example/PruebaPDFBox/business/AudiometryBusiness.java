package com.example.PruebaPDFBox.business;

import com.example.PruebaPDFBox.record.AreaConfig;
import com.example.PruebaPDFBox.record.FieldExtractionConfig;
import com.example.PruebaPDFBox.record.SectionArea;
import com.example.PruebaPDFBox.model.*;
import com.example.PruebaPDFBox.repository.ClinicalHistoryRepository;
import com.example.PruebaPDFBox.repository.ExamRepository;
import com.example.PruebaPDFBox.utilities.extractor.SectionTextExtractor;
import com.example.PruebaPDFBox.utilities.mapper.ObservationsMapper;
import com.example.PruebaPDFBox.utilities.mapper.RecommendationMapper;
import com.example.PruebaPDFBox.utilities.mapper.WeightedDecibelsMapper;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class AudiometryBusiness {

    private final SectionTextExtractor sectionTextExtractor;
    private final ExamRepository examRepository;
    private final ClinicalHistoryRepository clinicalHistoryRepository;
    private final WeightedDecibelsMapper weightedDecibelsMapper;
    private final ObservationsMapper observationsMapper;
    private final RecommendationMapper recommendationMapper;

    public AudiometryBusiness(
            SectionTextExtractor sectionTextExtractor,
            ExamRepository examRepository,
            ClinicalHistoryRepository clinicalHistoryRepository,
            WeightedDecibelsMapper weightedDecibelsMapper,
            ObservationsMapper observationsMapper,
            RecommendationMapper recommendationMapper) {
        this.sectionTextExtractor = sectionTextExtractor;
        this.examRepository = examRepository;
        this.clinicalHistoryRepository = clinicalHistoryRepository;
        this.weightedDecibelsMapper = weightedDecibelsMapper;
        this.observationsMapper = observationsMapper;
        this.recommendationMapper = recommendationMapper;
    }

    public boolean process(
            PDDocument document,
            SectionArea section,
            Worker worker,
            String examName
    ) throws Exception {

        Map<String, AreaConfig> config = buildAudiometryConfig();

        List<FieldExtractionConfig> fields = buildFieldConfigurations(config);
        Map<String, String> data = sectionTextExtractor.extractMultipleValues(document, fields, section);

        // Extraccion de fecha
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

        dataExam.setRightEar(data.get("oidoDerecho"));
        dataExam.setLeftEar(data.get("oidoIzquierdo"));
        dataExam.setDiagnostics(data.get("diagnosticos"));

        // Recorridos especiales
        String rawWeightedDecibels = sectionTextExtractor.extractTextBetweenLabels(document, "dB(A)", "SEVERIDAD", section);
        String weightedDecibelsJson = weightedDecibelsMapper.parseToJson(rawWeightedDecibels);
        dataExam.setWeightedDecibels(weightedDecibelsJson);

        String rawRecommendations = sectionTextExtractor.extractTextBetweenLabels(document, "OBSERVACION", "Observaciones", section);
        String recommendationsJson = recommendationMapper.parseToJson(rawRecommendations);
        dataExam.setRecommendations(recommendationsJson);

        String rawObservations = sectionTextExtractor.extractTextBetweenLabels(document, "Observaciones", "FONOAUDIÓLOGO", section);
        String observationsJson = observationsMapper.parseToJson(rawObservations);
        dataExam.setObservations(observationsJson);

        examRepository.save(exam);
        return true;
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
//        Map<String, AreaConfig> config = buildAudiometryConfig();
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
//        dataExam.setRightEar(sectionTextExtractor.extractValue(document, "Oído derecho:", section, config.get("Oído derecho:")));
//        dataExam.setLeftEar(sectionTextExtractor.extractValue(document, "Oído izquierdo:", section, config.get("Oído izquierdo:")));
//        dataExam.setDiagnostics(sectionTextExtractor.extractValueVertical(document, "Nombre del Diagnóstico", section, config.get("Nombre del Diagnóstico")));
//
//        // Extraer y convertir WeightedDecibels a JSON
//        String rawWeightedDecibels = sectionTextExtractor.extractTextBetweenLabels(document, "dB(A)", "SEVERIDAD", section);
//        String weightedDecibelsJson = weightedDecibelsMapper.parseToJson(rawWeightedDecibels);
//        dataExam.setWeightedDecibels(weightedDecibelsJson);
//
//        // Extraer y convertir Recommendations a JSON usando el mapper correcto
//        String rawRecommendations = sectionTextExtractor.extractTextBetweenLabels(document, "OBSERVACION", "Observaciones", section);
//        String recommendationsJson = recommendationMapper.parseToJson(rawRecommendations);
//        dataExam.setRecommendations(recommendationsJson);
//
//        // Extraer y convertir Observations a JSON usando el mapper correcto
//        String rawObservations = sectionTextExtractor.extractTextBetweenLabels(document, "Observaciones", "FONOAUDIÓLOGO" , section);
//        String observationsJson = observationsMapper.parseToJson(rawObservations);
//        dataExam.setObservations(observationsJson);
//
//        examRepository.save(exam);
//        return true;
//    }

    private Map<String, AreaConfig> buildAudiometryConfig() {

        Map<String, AreaConfig> config = new HashMap<>();

        config.put("Fecha:", new AreaConfig(0, 100, 12));

        config.put("Oído derecho:", new AreaConfig(0, 200, 21));
        config.put("Oído izquierdo:", new AreaConfig(0, 200, 21));
        config.put("Nombre del Diagnóstico", new AreaConfig(60, 130, 21));

        return config;
    }
}
