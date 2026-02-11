package com.example.PruebaPDFBox.business.LaboratoryBusiness;

import com.example.PruebaPDFBox.model.*;
import com.example.PruebaPDFBox.record.AreaConfig;
import com.example.PruebaPDFBox.record.FieldExtractionConfig;
import com.example.PruebaPDFBox.record.SectionArea;
import com.example.PruebaPDFBox.repository.ClinicalHistoryRepository;
import com.example.PruebaPDFBox.repository.DataTypeRepository;
import com.example.PruebaPDFBox.repository.DataValueRepository;
import com.example.PruebaPDFBox.repository.ExamRepository;
import com.example.PruebaPDFBox.utilities.extractor.TextExtractor.PlainTextExtractor;
import com.example.PruebaPDFBox.utilities.mapper.MapperJsons.MorphologyStudyMapper;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;

@Component
public class LaboratoryResultTextBusiness {

    private final PlainTextExtractor plainTextExtractor;
    private final ExamRepository examRepository;
    private final ClinicalHistoryRepository clinicalHistoryRepository;
    private final DataTypeRepository dataTypeRepository;
    private final DataValueRepository dataValueRepository;
    private final MorphologyStudyMapper morphologyStudyMapper;

    public LaboratoryResultTextBusiness(
            PlainTextExtractor plainTextExtractor,
            ExamRepository examRepository,
            ClinicalHistoryRepository clinicalHistoryRepository,
            DataTypeRepository dataTypeRepository,
            DataValueRepository dataValueRepository,
            MorphologyStudyMapper morphologyStudyMapper) {
        this.plainTextExtractor = plainTextExtractor;
        this.examRepository = examRepository;
        this.clinicalHistoryRepository = clinicalHistoryRepository;
        this.dataTypeRepository = dataTypeRepository;
        this.dataValueRepository = dataValueRepository;
        this.morphologyStudyMapper = morphologyStudyMapper;
    }

    public boolean process(PDDocument document, SectionArea section, String examName, Worker worker) throws IOException {


        Map<String, AreaConfig> config = buildLaboratoryConfig();
        List<FieldExtractionConfig> fields = buildFieldConfigurations(config);
        Map<String, String> data = plainTextExtractor.extractMultipleValues(document, fields, section);

        String examDate = plainTextExtractor.extractDate(data.get("fecha"));

        if (examDate == null || examDate.isEmpty()) {
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

        // Mapear los resultados extraídos a los campos de DataExam

        String rawMorphologyStudy = plainTextExtractor.extractTextBetweenLabels(
                document, "EXTENDIDO DE SANGRE PERIFERICA, ESTUDIO DE MORFOLOGIA", "TECNICA :", section
        );
        String MorphologyStudyJson = morphologyStudyMapper.parseToJson(rawMorphologyStudy);
        createDataValue("Estudio de Morfologia", MorphologyStudyJson, exam);

        createDataValue("ALT",
                plainTextExtractor.extractTextBetweenLabels(
                        document,
                        "ALANINO AMINOTRANSFERASAS (ALT,TGP)",
                        "mg/dL",
                        section),
                exam);

        createDataValue("AST",
                plainTextExtractor.extractTextBetweenLabels(
                        document,
                        "TRANSAMINASA GLUTAMICO OXALACETICA",
                        "U/L",
                        section),
                exam);

        createDataValue("HDL",
                plainTextExtractor.extractTextBetweenLabels(
                        document,
                        "COLESTEROL DE ALTA DENSIDAD [HDL]",
                        "mg/dL",
                        section),
                exam);

        createDataValue("COLESTEROL TOTAL",
                plainTextExtractor.extractTextBetweenLabels(
                        document,
                        "COLESTEROL TOTAL",
                        "mg/dL",
                        section),
                exam);

        createDataValue("CREATININA",
                plainTextExtractor.extractTextBetweenLabels(
                        document,
                        "CREATININA",
                        "mg/dL",
                        section),
                exam);

        createDataValue("GLUCOSA",
                plainTextExtractor.extractTextBetweenLabels(
                        document,
                        "GLUCOSA PRE",
                        "mg/dL",
                        section),
                exam);

        createDataValue("BUN",
                plainTextExtractor.extractTextBetweenLabels(
                        document,
                        "NITROGENO UREICO (BUN)",
                        "mg/dL",
                        section),
                exam);

        createDataValue("TRIGLICERIDOS",
                plainTextExtractor.extractTextBetweenLabels(
                        document,
                        "TRIGLICERIDOS",
                        "mg/dL",
                        section),
                exam);

        createDataValue("RTO.BLANCOS",
                plainTextExtractor.extractTextBetweenLabels(
                        document,
                        "RECUENTO DE BLANCOS",
                        "x10³/uL",
                        section),
                exam);

        createDataValue("HEMOGLOBINA",
                plainTextExtractor.extractTextBetweenLabels(
                        document,
                        "HEMOGLOBINA",
                        "g / dl",
                        section),
                exam);

        createDataValue("HEMATOCRITO",
                plainTextExtractor.extractTextBetweenLabels(
                        document,
                        "HEMATOCRITO",
                        "%",
                        section),
                exam);

        createDataValue("RTO.PLAQUETAS",
                plainTextExtractor.extractTextBetweenLabels(
                        document,
                        "RECUENTO DE PLAQUETAS",
                        "x10³/uL",
                        section),
                exam);

        createDataValue("RETICULOCITOS",
                plainTextExtractor.extractTextBetweenLabels(
                        document,
                        "RETICULOCITOS",
                        "%",
                        section),
                exam);

        createDataValue("ACIDO HIPURICO",
                plainTextExtractor.extractTextBetweenLabels(
                        document,
                        "ACIDO HIPÚRICO (TOLUENO)",
                        "g/g",
                        section),
                exam);

        createDataValue("ACIDO METILHIPURICO",
                plainTextExtractor.extractTextBetweenLabels(
                        document,
                        "ACIDO METILHIPÚRICO (XILENO)",
                        "g/g",
                        section),
                exam);

        createDataValue("FENOL",
                plainTextExtractor.extractTextBetweenLabels(
                        document,
                        "FENOL (BENCENO)",
                        "mg/g",
                        section),
                exam);

        createDataValue("O-CRESOL",
                plainTextExtractor.extractTextBetweenLabels(
                        document,
                        "O-CRESOL (TOLUENO)",
                        "mg/g",
                        section),
                exam);

        createDataValue("HDL",
                data.get("HDL"),
                exam);


        // Guardar en la base de datos
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

        fields.add(new FieldExtractionConfig("HDL", "HDL",
                config.get("HDL"), FieldExtractionConfig.ExtractionStrategy.HORIZONTAL));

        return fields;
    }

    private Map<String, AreaConfig> buildLaboratoryConfig() {
        Map<String, AreaConfig> config = new HashMap<>();

        config.put("HDL", new AreaConfig(40, 100, 12));

        return config;
    }
}
