package com.example.PruebaPDFBox.business.LaboratoryBusiness;

import com.example.PruebaPDFBox.model.*;
import com.example.PruebaPDFBox.record.AreaConfig;
import com.example.PruebaPDFBox.record.FieldExtractionConfig;
import com.example.PruebaPDFBox.record.SectionArea;
import com.example.PruebaPDFBox.repository.ClinicalHistoryRepository;
import com.example.PruebaPDFBox.repository.DataTypeRepository;
import com.example.PruebaPDFBox.repository.DataValueRepository;
import com.example.PruebaPDFBox.repository.ExamRepository;
import com.example.PruebaPDFBox.utilities.extractor.TextExtractor.SectionTextExtractor;
import com.example.PruebaPDFBox.utilities.mapper.MapperJsons.MorphologyStudyMapper;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;

@Component
public class LaboratoryResultTextBusiness {

    private final SectionTextExtractor sectionTextExtractor;
    private final ExamRepository examRepository;
    private final ClinicalHistoryRepository clinicalHistoryRepository;
    private final DataTypeRepository dataTypeRepository;
    private final DataValueRepository dataValueRepository;
    private final MorphologyStudyMapper morphologyStudyMapper;

    public LaboratoryResultTextBusiness(
            SectionTextExtractor sectionTextExtractor,
            ExamRepository examRepository,
            ClinicalHistoryRepository clinicalHistoryRepository,
            DataTypeRepository dataTypeRepository,
            DataValueRepository dataValueRepository,
            MorphologyStudyMapper morphologyStudyMapper) {
        this.sectionTextExtractor = sectionTextExtractor;
        this.examRepository = examRepository;
        this.clinicalHistoryRepository = clinicalHistoryRepository;
        this.dataTypeRepository = dataTypeRepository;
        this.dataValueRepository = dataValueRepository;
        this.morphologyStudyMapper = morphologyStudyMapper;
    }

    public boolean process(PDDocument document, SectionArea section, String examName, Worker worker) throws IOException {


        Map<String, AreaConfig> config = buildLaboratoryConfig();
        List<FieldExtractionConfig> fields = buildFieldConfigurations(config);
        Map<String, String> data = sectionTextExtractor.extractMultipleValues(document, fields, section);

        String examDate = sectionTextExtractor.extractDate(data.get("fecha"));

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

        String rawMorphologyStudy = sectionTextExtractor.extractTextBetweenLabels(
                document, "EXTENDIDO DE SANGRE PERIFERICA, ESTUDIO DE MORFOLOGIA", "TECNICA :", section
        );
        String MorphologyStudyJson = morphologyStudyMapper.parseToJson(rawMorphologyStudy);
        createDataValue("Estudio de Morfologia", MorphologyStudyJson, exam);

        createDataValue("ALT",
                sectionTextExtractor.extractTextBetweenLabels(
                        document,
                        "ALANINO AMINOTRANSFERASAS (ALT,TGP)",
                        "mg/dL",
                        section),
                exam);

        createDataValue("AST",
                sectionTextExtractor.extractTextBetweenLabels(
                        document,
                        "TRANSAMINASA GLUTAMICO OXALACETICA",
                        "U/L",
                        section),
                exam);

        createDataValue("HDL",
                sectionTextExtractor.extractTextBetweenLabels(
                        document,
                        "COLESTEROL DE ALTA DENSIDAD [HDL]",
                        "mg/dL",
                        section),
                exam);

        createDataValue("COLESTEROL TOTAL",
                sectionTextExtractor.extractTextBetweenLabels(
                        document,
                        "COLESTEROL TOTAL",
                        "mg/dL",
                        section),
                exam);

        createDataValue("CREATININA",
                sectionTextExtractor.extractTextBetweenLabels(
                        document,
                        "CREATININA",
                        "mg/dL",
                        section),
                exam);

        createDataValue("GLUCOSA",
                sectionTextExtractor.extractTextBetweenLabels(
                        document,
                        "GLUCOSA PRE",
                        "mg/dL",
                        section),
                exam);

        createDataValue("BUN",
                sectionTextExtractor.extractTextBetweenLabels(
                        document,
                        "NITROGENO UREICO (BUN)",
                        "mg/dL",
                        section),
                exam);

        createDataValue("TRIGLICERIDOS",
                sectionTextExtractor.extractTextBetweenLabels(
                        document,
                        "TRIGLICERIDOS",
                        "mg/dL",
                        section),
                exam);

        createDataValue("RTO.BLANCOS",
                sectionTextExtractor.extractTextBetweenLabels(
                        document,
                        "RECUENTO DE BLANCOS",
                        "x10³/uL",
                        section),
                exam);

        createDataValue("HEMOGLOBINA",
                sectionTextExtractor.extractTextBetweenLabels(
                        document,
                        "HEMOGLOBINA",
                        "g / dl",
                        section),
                exam);

        createDataValue("HEMATOCRITO",
                sectionTextExtractor.extractTextBetweenLabels(
                        document,
                        "HEMATOCRITO",
                        "%",
                        section),
                exam);

        createDataValue("RTO.PLAQUETAS",
                sectionTextExtractor.extractTextBetweenLabels(
                        document,
                        "RECUENTO DE PLAQUETAS",
                        "x10³/uL",
                        section),
                exam);

        createDataValue("RETICULOCITOS",
                sectionTextExtractor.extractTextBetweenLabels(
                        document,
                        "RETICULOCITOS",
                        "%",
                        section),
                exam);

        createDataValue("ACIDO HIPURICO",
                sectionTextExtractor.extractTextBetweenLabels(
                        document,
                        "ACIDO HIPÚRICO (TOLUENO)",
                        "g/g",
                        section),
                exam);

        createDataValue("ACIDO METILHIPURICO",
                sectionTextExtractor.extractTextBetweenLabels(
                        document,
                        "ACIDO METILHIPÚRICO (XILENO)",
                        "g/g",
                        section),
                exam);

        createDataValue("FENOL",
                sectionTextExtractor.extractTextBetweenLabels(
                        document,
                        "FENOL (BENCENO)",
                        "mg/g",
                        section),
                exam);

        createDataValue("O-CRESOL",
                sectionTextExtractor.extractTextBetweenLabels(
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
