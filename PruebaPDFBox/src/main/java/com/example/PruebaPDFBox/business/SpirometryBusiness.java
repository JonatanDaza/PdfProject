package com.example.PruebaPDFBox.business;

import com.example.PruebaPDFBox.model.*;
import com.example.PruebaPDFBox.record.AreaConfig;
import com.example.PruebaPDFBox.record.FieldExtractionConfig;
import com.example.PruebaPDFBox.record.SectionArea;
import com.example.PruebaPDFBox.repository.ClinicalHistoryRepository;
import com.example.PruebaPDFBox.repository.DataTypeRepository;
import com.example.PruebaPDFBox.repository.DataValueRepository;
import com.example.PruebaPDFBox.repository.ExamRepository;
import com.example.PruebaPDFBox.utilities.extractor.TextExtractor.PlainTextExtractor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class SpirometryBusiness {

    private final PlainTextExtractor plainTextExtractor;
    private final ExamRepository examRepository;
    private final ClinicalHistoryRepository clinicalHistoryRepository;
    private final DataTypeRepository dataTypeRepository;
    private final DataValueRepository dataValueRepository;

    public SpirometryBusiness(
            PlainTextExtractor plainTextExtractor,
            ExamRepository examRepository,
            ClinicalHistoryRepository clinicalHistoryRepository,
            DataTypeRepository dataTypeRepository, DataValueRepository dataValueRepository) {
        this.plainTextExtractor = plainTextExtractor;
        this.examRepository = examRepository;
        this.clinicalHistoryRepository = clinicalHistoryRepository;
        this.dataTypeRepository = dataTypeRepository;
        this.dataValueRepository = dataValueRepository;
    }

    public boolean process(
            PDDocument document,
            SectionArea section,
            Worker worker,
            String examName
    ) throws Exception {

        Map<String, AreaConfig> config = buildSpirometryConfig();

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

        createDataValue("Interpretación",
                data.get("Interpretación"),
                exam);

        examRepository.save(exam);
        return true;
    }

    private List<FieldExtractionConfig> buildFieldConfigurations(Map<String, AreaConfig> config) {
        List<FieldExtractionConfig> fields = new ArrayList<>();

        fields.add(new FieldExtractionConfig("Fecha:", "fecha",
                config.get("Fecha:"), FieldExtractionConfig.ExtractionStrategy.HORIZONTAL));


        fields.add(new FieldExtractionConfig("Interpretación", "Interpretacion",
                config.get("Interpretación"), FieldExtractionConfig.ExtractionStrategy.VERTICAL));

        return fields;
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

    private Map<String, AreaConfig> buildSpirometryConfig() {
        Map<String, AreaConfig> config = new HashMap<>();

        config.put("Fecha:", new AreaConfig(0, 100, 10));

        config.put("Interpretación", new AreaConfig(0, 50, 50));

        return config;
    }
}
