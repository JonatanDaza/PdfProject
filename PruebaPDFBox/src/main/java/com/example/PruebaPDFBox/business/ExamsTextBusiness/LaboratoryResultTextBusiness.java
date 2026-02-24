package com.example.PruebaPDFBox.business.ExamsTextBusiness;

import com.example.PruebaPDFBox.model.*;
import com.example.PruebaPDFBox.record.AreaConfig;
import com.example.PruebaPDFBox.record.FieldExtractionConfig;
import com.example.PruebaPDFBox.record.SectionArea;
import com.example.PruebaPDFBox.record.SubSectionArea;
import com.example.PruebaPDFBox.service.ServiceEntity.ClinicalHistoryService;
import com.example.PruebaPDFBox.service.ServiceEntity.DataValueService;
import com.example.PruebaPDFBox.service.ServiceEntity.ExamService;
import com.example.PruebaPDFBox.utilities.extractor.SectionExtractor.PdfSubSectionExtractor;
import com.example.PruebaPDFBox.utilities.extractor.TextExtractor.PlainTextExtractor;
import com.example.PruebaPDFBox.utilities.mapper.MapperJsons.MorphologyStudyMapper;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;

@Component
@RequiredArgsConstructor
public class LaboratoryResultTextBusiness {

    private final PlainTextExtractor plainTextExtractor;
    private final ExamService examService;
    private final ClinicalHistoryService clinicalHistoryService;
    private final DataValueService dataValueService;
    private final PdfSubSectionExtractor pdfSubSectionExtractor;

    public boolean process(PDDocument document, SectionArea section, Worker worker) throws Exception {
        try {
            // Extraer subsecciones primero
            Map<String, SubSectionArea> subSections = pdfSubSectionExtractor.extract(
                    document, section, SUB_SECTION_TITLES
            );

            // Extraer fecha desde DATOS PERSONALES
            SubSectionArea datosPersonalesArea = subSections.get("DATOS PERSONALES");
            String examDate = null;

            if (datosPersonalesArea != null) {
                Map<String, AreaConfig> config = buildLaboratoryConfig();
                List<FieldExtractionConfig> dateFields = new ArrayList<>();
                dateFields.add(new FieldExtractionConfig(
                        "Fecha:", "Fecha:",
                        config.get("Fecha:"),
                        FieldExtractionConfig.ExtractionStrategy.EXACT_MATCH
                ));

                Map<String, String> dateData = plainTextExtractor.extractMultipleValues(
                        document, dateFields, section, datosPersonalesArea
                );

                String rawDate = dateData.get("Fecha:");
                examDate = plainTextExtractor.extractDate(rawDate);
            }

            if (examDate == null || examDate.isEmpty()) {
                System.err.println("No se encontró fecha de examen en DATOS PERSONALES");
                return false;
            }

            String examName = "AUDIOMETRÍA";
            if (examService.examAlreadyExists(examDate, examName)) {
                System.out.println("Examen ya existe: " + examName + " - " + examDate);
                return false;
            }

            Map<String, String> values = buildDataValues(document, section, subSections);

            ClinicalHistory clinicalHistory = clinicalHistoryService.findOrCreate(worker, examDate);
            Exam exam = examService.save(examName, examDate, worker, clinicalHistory);
            dataValueService.saveAll(values, exam);

            return true;
        } catch (IOException e) {
            System.err.println("Error procesando medicina ocupacional: " + e.getMessage());
            return false;
        }
    }


    private Map<String, String> buildDataValues(
            PDDocument document,
            SectionArea section,
            Map<String, SubSectionArea> subSections
    ) throws IOException {

        Map<String, String> values = new LinkedHashMap<>();
        Map<String, AreaConfig> config = buildLaboratoryConfig();

        return values;
    }

    private static final Set<String> SUB_SECTION_TITLES = Set.of();

//
//    public boolean process(PDDocument document, SectionArea section, String examName, Worker worker) throws IOException {
//
//
//        Map<String, AreaConfig> config = buildLaboratoryConfig();
//        List<FieldExtractionConfig> fields = buildFieldConfigurations(config);
//        Map<String, String> data = plainTextExtractor.extractMultipleValues(document, fields, section);
//
//        String examDate = plainTextExtractor.extractDate(data.get("fecha"));
//
//        if (examDate == null || examDate.isEmpty()) {
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
//        Exam exam = new Exam();
//        exam.setExamName(examName);
//        exam.setExamDate(examDate);
//        exam.setClinicalHistory(hc);
//        exam.setWorker(worker);
//        exam = examRepository.save(exam);
//
//        // Mapear los resultados extraídos a los campos de DataExam
//
//        String rawMorphologyStudy = plainTextExtractor.extractTextBetweenLabels(
//                document, "EXTENDIDO DE SANGRE PERIFERICA, ESTUDIO DE MORFOLOGIA", "TECNICA :", section
//        );
//        String MorphologyStudyJson = morphologyStudyMapper.parseToJson(rawMorphologyStudy);
//        createDataValue("Estudio de Morfologia", MorphologyStudyJson, exam);
//
//        createDataValue("ALT",
//                plainTextExtractor.extractTextBetweenLabels(
//                        document,
//                        "ALANINO AMINOTRANSFERASAS (ALT,TGP)",
//                        "mg/dL",
//                        section),
//                exam);
//
//        createDataValue("AST",
//                plainTextExtractor.extractTextBetweenLabels(
//                        document,
//                        "TRANSAMINASA GLUTAMICO OXALACETICA",
//                        "U/L",
//                        section),
//                exam);
//
//        createDataValue("HDL",
//                plainTextExtractor.extractTextBetweenLabels(
//                        document,
//                        "COLESTEROL DE ALTA DENSIDAD [HDL]",
//                        "mg/dL",
//                        section),
//                exam);
//
//        createDataValue("COLESTEROL TOTAL",
//                plainTextExtractor.extractTextBetweenLabels(
//                        document,
//                        "COLESTEROL TOTAL",
//                        "mg/dL",
//                        section),
//                exam);
//
//        createDataValue("CREATININA",
//                plainTextExtractor.extractTextBetweenLabels(
//                        document,
//                        "CREATININA",
//                        "mg/dL",
//                        section),
//                exam);
//
//        createDataValue("GLUCOSA",
//                plainTextExtractor.extractTextBetweenLabels(
//                        document,
//                        "GLUCOSA PRE",
//                        "mg/dL",
//                        section),
//                exam);
//
//        createDataValue("BUN",
//                plainTextExtractor.extractTextBetweenLabels(
//                        document,
//                        "NITROGENO UREICO (BUN)",
//                        "mg/dL",
//                        section),
//                exam);
//
//        createDataValue("TRIGLICERIDOS",
//                plainTextExtractor.extractTextBetweenLabels(
//                        document,
//                        "TRIGLICERIDOS",
//                        "mg/dL",
//                        section),
//                exam);
//
//        createDataValue("RTO.BLANCOS",
//                plainTextExtractor.extractTextBetweenLabels(
//                        document,
//                        "RECUENTO DE BLANCOS",
//                        "x10³/uL",
//                        section),
//                exam);
//
//        createDataValue("HEMOGLOBINA",
//                plainTextExtractor.extractTextBetweenLabels(
//                        document,
//                        "HEMOGLOBINA",
//                        "g / dl",
//                        section),
//                exam);
//
//        createDataValue("HEMATOCRITO",
//                plainTextExtractor.extractTextBetweenLabels(
//                        document,
//                        "HEMATOCRITO",
//                        "%",
//                        section),
//                exam);
//
//        createDataValue("RTO.PLAQUETAS",
//                plainTextExtractor.extractTextBetweenLabels(
//                        document,
//                        "RECUENTO DE PLAQUETAS",
//                        "x10³/uL",
//                        section),
//                exam);
//
//        createDataValue("RETICULOCITOS",
//                plainTextExtractor.extractTextBetweenLabels(
//                        document,
//                        "RETICULOCITOS",
//                        "%",
//                        section),
//                exam);
//
//        createDataValue("ACIDO HIPURICO",
//                plainTextExtractor.extractTextBetweenLabels(
//                        document,
//                        "ACIDO HIPÚRICO (TOLUENO)",
//                        "g/g",
//                        section),
//                exam);
//
//        createDataValue("ACIDO METILHIPURICO",
//                plainTextExtractor.extractTextBetweenLabels(
//                        document,
//                        "ACIDO METILHIPÚRICO (XILENO)",
//                        "g/g",
//                        section),
//                exam);
//
//        createDataValue("FENOL",
//                plainTextExtractor.extractTextBetweenLabels(
//                        document,
//                        "FENOL (BENCENO)",
//                        "mg/g",
//                        section),
//                exam);
//
//        createDataValue("O-CRESOL",
//                plainTextExtractor.extractTextBetweenLabels(
//                        document,
//                        "O-CRESOL (TOLUENO)",
//                        "mg/g",
//                        section),
//                exam);
//
//        createDataValue("HDL",
//                data.get("HDL"),
//                exam);
//
//
//        // Guardar en la base de datos
//        examRepository.save(exam);
//
//        return true;
//    }
//
//    private void createDataValue(String typeName, String value, Exam exam) {
//        if (value == null || value.trim().isEmpty()) {
//            return;
//        }
//
//        DataType dataType = dataTypeRepository.findByName(typeName)
//                .orElseGet(() -> {
//                    DataType newType = new DataType();
//                    newType.setName(typeName);
//                    return dataTypeRepository.save(newType);
//                });
//
//        DataValue dataValue = new DataValue();
//        dataValue.setValue(value);
//        dataValue.setDataType(dataType);
//        dataValue.setExam(exam);
//
//        dataValueRepository.save(dataValue);
//    }
//
//    private List<FieldExtractionConfig> buildFieldConfigurations(Map<String, AreaConfig> config) {
//        List<FieldExtractionConfig> fields = new ArrayList<>();
//
//        fields.add(new FieldExtractionConfig("HDL", "HDL",
//                config.get("HDL"), FieldExtractionConfig.ExtractionStrategy.HORIZONTAL));
//
//        return fields;
//    }

    private Map<String, AreaConfig> buildLaboratoryConfig() {
        Map<String, AreaConfig> config = new HashMap<>();

        config.put("HDL", new AreaConfig(40, 100, 12));

        return config;
    }
}
