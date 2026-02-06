package com.example.PruebaPDFBox.business;

import com.example.PruebaPDFBox.record.AreaConfig;
import com.example.PruebaPDFBox.record.FieldExtractionConfig;
import com.example.PruebaPDFBox.record.SectionArea;
import com.example.PruebaPDFBox.model.*;
import com.example.PruebaPDFBox.repository.ClinicalHistoryRepository;
import com.example.PruebaPDFBox.repository.ExamRepository;
import com.example.PruebaPDFBox.utilities.extractor.SectionTextExtractor;
import com.example.PruebaPDFBox.utilities.mapper.RecommendationsConductMapper;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class OptometryBusiness {

    private final SectionTextExtractor sectionTextExtractor;
    private final ExamRepository examRepository;
    private final ClinicalHistoryRepository clinicalHistoryRepository;
    private final RecommendationsConductMapper recommendationsConductMapper;

    public OptometryBusiness(
            SectionTextExtractor sectionTextExtractor,
            ExamRepository examRepository,
            ClinicalHistoryRepository clinicalHistoryRepository, RecommendationsConductMapper recommendationsConductMapper)
    {
        this.sectionTextExtractor = sectionTextExtractor;
        this.examRepository = examRepository;
        this.clinicalHistoryRepository = clinicalHistoryRepository;
        this.recommendationsConductMapper = recommendationsConductMapper;
    }

    public boolean process(
            PDDocument document,
            SectionArea section,
            Worker worker,
            String examName
    ) throws Exception {

        Map<String, AreaConfig> config = buildOptometryConfig();

        List<FieldExtractionConfig> fields = buildFieldConfigurations(config);
        Map<String, String> data = sectionTextExtractor.extractMultipleValues(document, fields, section);

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

        dataExam.setDiagnostics(data.get("diagnostico"));

        String rawRecommendationsConduct = sectionTextExtractor.extractTextBetweenLabels(
                document,"CONDUCTA Y/O RECOMENDACIONES", "OTRAS OBSERVACIONES Y RECOMENDACIONES:", section);
        String recommendationsConductJson = recommendationsConductMapper.parseToJson(rawRecommendationsConduct);
        dataExam.setRecommendationsConduct(recommendationsConductJson);

        examRepository.save(exam);
        return true;
    }

    private List<FieldExtractionConfig> buildFieldConfigurations(Map<String, AreaConfig> config) {
        List<FieldExtractionConfig> fields = new ArrayList<>();

        fields.add(new FieldExtractionConfig("Fecha:", "fecha",
                config.get("Fecha:"), FieldExtractionConfig.ExtractionStrategy.HORIZONTAL));
        fields.add(new FieldExtractionConfig("DIAGNÓSTICO", "diagnostico",
                config.get("DIAGNÓSTICO"), FieldExtractionConfig.ExtractionStrategy.EXACT_MATCH));

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
//        Map<String, AreaConfig> config = buildOptometryConfig();
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
//        exam.setExamName(examName);
//        exam.setExamDate(examDate);
//
//        exam.setClinicalHistory(hc);
//        exam.setWorker(worker);
//        exam.setDataExam(dataExam);
//
//        dataExam.setDiagnostics(sectionTextExtractor.extractValueExact(document, "DIAGNÓSTICO", section, config.get("DIAGNÓSTICO")));
//
//        String rawRecommendationsConduct = sectionTextExtractor.extractTextBetweenLabels(document,"CONDUCTA Y/O RECOMENDACIONES", "OTRAS OBSERVACIONES Y RECOMENDACIONES:", section);
//        String recommendationsConductJson = recommendationsConductMapper.parseToJson(rawRecommendationsConduct);
//        dataExam.setRecommendationsConduct(recommendationsConductJson);
//
//        examRepository.save(exam);
//        return true;
//    }
//

    private Map<String, AreaConfig> buildOptometryConfig() {
        Map<String, AreaConfig> config = new HashMap<>();

        config.put("Fecha:", new AreaConfig(0, 100, 12));
        config.put("DIAGNÓSTICO", new AreaConfig(0, 100, 21));

        return config;
    }
}
