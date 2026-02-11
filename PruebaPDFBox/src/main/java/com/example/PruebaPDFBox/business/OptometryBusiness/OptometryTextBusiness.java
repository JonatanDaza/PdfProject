package com.example.PruebaPDFBox.business.OptometryBusiness;

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
import com.example.PruebaPDFBox.utilities.mapper.MapperJsons.RecommendationsConductMapper;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class OptometryTextBusiness {

    private final PlainTextExtractor plainTextExtractor;
    private final ExamRepository examRepository;
    private final ClinicalHistoryRepository clinicalHistoryRepository;
    private final RecommendationsConductMapper recommendationsConductMapper;
    private final DataTypeRepository dataTypeRepository;
    private final DataValueRepository dataValueRepository;

    public OptometryTextBusiness(
            PlainTextExtractor plainTextExtractor,
            ExamRepository examRepository,
            ClinicalHistoryRepository clinicalHistoryRepository, RecommendationsConductMapper recommendationsConductMapper, DataTypeRepository dataTypeRepository, DataValueRepository dataValueRepository)
    {
        this.plainTextExtractor = plainTextExtractor;
        this.examRepository = examRepository;
        this.clinicalHistoryRepository = clinicalHistoryRepository;
        this.recommendationsConductMapper = recommendationsConductMapper;
        this.dataTypeRepository = dataTypeRepository;
        this.dataValueRepository = dataValueRepository;
    }

    public boolean process(
            PDDocument document,
            SectionArea section,
            Worker worker,
            String examName
    ) throws Exception {

        Map<String, AreaConfig> config = buildOptometryConfig();

        List<FieldExtractionConfig> fields = buildFieldConfigurations(config);
        Map<String, String> data = plainTextExtractor.extractMultipleValues(document, fields, section);

        String examDate = plainTextExtractor.extractDate(data.get("fecha"));

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

        createDataValue("DIAGNÓSTICO",
                data.get("diagnostico"),
                exam);

        String rawRecommendationsConduct = plainTextExtractor.extractTextBetweenLabels(
                document, "CONDUCTA Y/O RECOMENDACIONES", "OTRAS OBSERVACIONES Y RECOMENDACIONES:", section
        );
        String recommendationsConductJson = recommendationsConductMapper.parseToJson(rawRecommendationsConduct);
        createDataValue("Conducta Y/O Recomendaciones", recommendationsConductJson, exam);

//        createDataValue("Código de Examen",
//                data.get("CÓDIGO"),
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
        fields.add(new FieldExtractionConfig("DIAGNÓSTICO", "diagnostico",
                config.get("DIAGNÓSTICO"), FieldExtractionConfig.ExtractionStrategy.EXACT_MATCH));
//        fields.add(new FieldExtractionConfig("CÓDIGO", "CÓDIGO",
//                config.get("CÓDIGO"), FieldExtractionConfig.ExtractionStrategy.VERTICAL));


        return fields;
    }

    private Map<String, AreaConfig> buildOptometryConfig() {
        Map<String, AreaConfig> config = new HashMap<>();

        config.put("Fecha:", new AreaConfig(0, 100, 12));
        config.put("DIAGNÓSTICO", new AreaConfig(0, 100, 21));
//        config.put("CÓDIGO", new AreaConfig(0, 60, 20));

        return config;
    }
}
