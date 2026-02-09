package com.example.PruebaPDFBox.business;

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
import com.example.PruebaPDFBox.utilities.extractor.ImageTextExtractor;
import com.example.PruebaPDFBox.utilities.extractor.SectionTextExtractor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;

@Component
public class LaboratoryResultBusiness {

    private final ImageTextExtractor imageTextExtractor;
    private final SectionTextExtractor sectionTextExtractor;
    private final ExamRepository examRepository;
    private final ClinicalHistoryRepository clinicalHistoryRepository;
    private final DataTypeRepository dataTypeRepository;
    private final DataValueRepository dataValueRepository;

    public LaboratoryResultBusiness(
            ImageTextExtractor imageTextExtractor,
            SectionTextExtractor sectionTextExtractor,
            ExamRepository examRepository,
            ClinicalHistoryRepository clinicalHistoryRepository, DataTypeRepository dataTypeRepository, DataValueRepository dataValueRepository) {
        this.imageTextExtractor = imageTextExtractor;
        this.sectionTextExtractor = sectionTextExtractor;
        this.examRepository = examRepository;
        this.clinicalHistoryRepository = clinicalHistoryRepository;
        this.dataTypeRepository = dataTypeRepository;
        this.dataValueRepository = dataValueRepository;
    }

    public boolean process(PDDocument document, SectionArea labSection, String examName, Worker worker) throws IOException {

        String examDate = sectionTextExtractor.extractExamDate(document, labSection);

        if (examDate == null || examDate.isEmpty()) {
            return false;
        }

        if (examRepository.findByExamDateAndExamName(examDate, examName).isPresent()) {
            return false;
        }

        ClinicalHistory hc = clinicalHistoryRepository.findByWorkerAndExamDate(worker, examDate)
                .orElseGet(() -> clinicalHistoryRepository.save(new ClinicalHistory()));

        List<Integer> targetPages = getPageRange(labSection);
        Map<String, String> results;

        // Validar si hay imagen válida para extraer desde tabla
        if (imageTextExtractor.hasValidTableImage(document, targetPages)) {
            // Extraer desde imagen de tabla
            List<String> labels = getLaboratoryLabels();
            results = imageTextExtractor.extractTextFromTable(document, targetPages, labels);
        } else {
            // Extraer desde texto directo
            Map<String, AreaConfig> config = buildLaboratoryConfig();
            List<FieldExtractionConfig> fields = buildFieldConfigurations(config);
            results = sectionTextExtractor.extractMultipleValues(document, fields, labSection);
        }

        Exam exam = new Exam();
        exam.setExamName(examName);
        exam.setExamDate(examDate);
        exam.setClinicalHistory(hc);
        exam.setWorker(worker);
        exam = examRepository.save(exam);

        // Mapear los resultados extraídos a los campos de DataExam
        createDataValue("HEMATOCRITO",
                results.get("HEMATOCRITO"),
                exam);

        createDataValue("HEMOGLOBINA",
                results.get("HEMOGLOBINA"),
                exam
        );

        createDataValue("RTO.BLANCOS",
                results.get("RTO.BLANCOS"),
                exam
        );

        createDataValue("RTO.PLAQUETAS",
                results.get("RTO.PLAQUETAS"),
                exam
        );

        createDataValue("GLICEMIA",
                results.get("GLICEMIA"),
                exam
        );

        createDataValue("COLESTEROL TOTAL",
                results.get("COLESTEROL TOTAL"),
                exam
        );

        createDataValue("COLESTEROL HDL",
                results.get("COLESTEROL HDL"),
                exam
        );

        createDataValue("COLESTEROL LDL",
                results.get("COLESTEROL LDL"),
                exam
        );

        createDataValue("TRIGLICERIDOS",
                results.get("TRIGLICERIDOS"),
                exam
        );


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

    private List<Integer> getPageRange(SectionArea section) {
        List<Integer> pages = new ArrayList<>();
        for (int i = section.startPage(); i <= section.endPage(); i++) {
            pages.add(i);
        }
        return pages;
    }

    //Retorna lista de etiquetas a buscar
    //Ahora las etiquetas coinciden exactamente con las de la imagen
    private List<String> getLaboratoryLabels() {
        return Arrays.asList(
                // Cuadro Hemático
                "HEMATOCRITO",
                "HEMOGLOBINA",
                "RTO.BLANCOS",
                "RTO.PLAQUETAS",

                // Química Sanguínea
                "GLICEMIA",
                "COLESTEROL TOTAL",
                "COLESTEROL HDL",
                "COLESTEROL LDL",
                "TRIGLICERIDOS"
        );
    }

    private List<FieldExtractionConfig> buildFieldConfigurations(Map<String, AreaConfig> config) {
        List<FieldExtractionConfig> fields = new ArrayList<>();

        fields.add(new FieldExtractionConfig("HEMATOCRITO", "HEMATOCRITO",
                config.get("HEMATOCRITO"), FieldExtractionConfig.ExtractionStrategy.HORIZONTAL));
        fields.add(new FieldExtractionConfig("HEMOGLOBINA", "HEMOGLOBINA",
                config.get("HEMOGLOBINA"), FieldExtractionConfig.ExtractionStrategy.HORIZONTAL));
        fields.add(new FieldExtractionConfig("RTO.BLANCOS", "RTO.BLANCOS",
                config.get("RTO.BLANCOS"), FieldExtractionConfig.ExtractionStrategy.HORIZONTAL));
        fields.add(new FieldExtractionConfig("RTO.PLAQUETAS", "RTO.PLAQUETAS",
                config.get("RTO.PLAQUETAS"), FieldExtractionConfig.ExtractionStrategy.HORIZONTAL));
        fields.add(new FieldExtractionConfig("GLICEMIA", "GLICEMIA",
                config.get("GLICEMIA"), FieldExtractionConfig.ExtractionStrategy.HORIZONTAL));
        fields.add(new FieldExtractionConfig("COLESTEROL TOTAL", "COLESTEROL TOTAL",
                config.get("COLESTEROL TOTAL"), FieldExtractionConfig.ExtractionStrategy.HORIZONTAL));
        fields.add(new FieldExtractionConfig("COLESTEROL HDL", "COLESTEROL HDL",
                config.get("COLESTEROL HDL"), FieldExtractionConfig.ExtractionStrategy.HORIZONTAL));
        fields.add(new FieldExtractionConfig("COLESTEROL LDL", "COLESTEROL LDL",
                config.get("COLESTEROL LDL"), FieldExtractionConfig.ExtractionStrategy.HORIZONTAL));
        fields.add(new FieldExtractionConfig("TRIGLICERIDOS", "TRIGLICERIDOS",
                config.get("TRIGLICERIDOS"), FieldExtractionConfig.ExtractionStrategy.HORIZONTAL));

        return fields;
    }

    private Map<String, AreaConfig> buildLaboratoryConfig() {
        Map<String, AreaConfig> config = new HashMap<>();

        config.put("HEMATOCRITO", new AreaConfig(40, 80, 12));
        config.put("HEMOGLOBINA", new AreaConfig(40, 80, 12));
        config.put("RTO.BLANCOS", new AreaConfig(40, 80, 12));
        config.put("RTO.PLAQUETAS", new AreaConfig(40, 80, 12));
        config.put("GLICEMIA", new AreaConfig(40, 80, 12));
        config.put("COLESTEROL TOTAL", new AreaConfig(40, 100, 12));
        config.put("COLESTEROL HDL", new AreaConfig(40, 100, 12));
        config.put("COLESTEROL LDL", new AreaConfig(40, 100, 12));
        config.put("TRIGLICERIDOS", new AreaConfig(40, 80, 12));

        return config;
    }
}
