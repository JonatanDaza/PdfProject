package com.example.PruebaPDFBox.business.ExamsImgBusiness;

import com.example.PruebaPDFBox.model.ClinicalHistory;
import com.example.PruebaPDFBox.model.Exam;
import com.example.PruebaPDFBox.model.Worker;
import com.example.PruebaPDFBox.record.SectionArea;
import com.example.PruebaPDFBox.service.ServiceEntity.ClinicalHistoryService;
import com.example.PruebaPDFBox.service.ServiceEntity.DataTypeService;
import com.example.PruebaPDFBox.service.ServiceEntity.DataValueService;
import com.example.PruebaPDFBox.service.ServiceEntity.ExamService;
import com.example.PruebaPDFBox.utilities.extractor.TextExtractor.ImageTextExtractor;
import com.example.PruebaPDFBox.utilities.extractor.TextExtractor.PlainTextExtractor;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;

@Component
@RequiredArgsConstructor
public class LaboratoryResultImgBusiness {

    private final ImageTextExtractor imageTextExtractor;
    private final PlainTextExtractor plainTextExtractor;
    private final ExamService examService;
    private final ClinicalHistoryService clinicalHistoryService;
    private final DataTypeService dataTypeService;
    private final DataValueService dataValueService;

    public boolean process(PDDocument document, SectionArea labSection, Worker worker) throws Exception {
        try {
            String examDate = plainTextExtractor.extractExamDate(document, labSection);

            if (examDate == null || examDate.isEmpty()) {
                System.err.println("No se encontró fecha de examen en DATOS PERSONALES");
                return false;
            }

            String examName = "RESULTADOS DE LABORATORIO";
            if (examService.examAlreadyExists(examDate, examName)) {
                System.out.println("Examen ya existe: " + examName + " - " + examDate);
                return false;
            }

            Map<String, String> values = buildDataValues(document, labSection);

            ClinicalHistory clinicalHistory = clinicalHistoryService.findOrCreate(worker, examDate);
            Exam exam = examService.save(examName, examDate, worker, clinicalHistory);
            dataValueService.saveAll(values, exam);

            return true;
        } catch (IOException e) {
            System.err.println("Error procesando resultados de laboratorio: " + e.getMessage());
            return false;
        }
    }


    private Map<String, String> buildDataValues(PDDocument document, SectionArea labSection) throws Exception {
        Map<String, String> values = new LinkedHashMap<>();

        List<Integer> targetPages = getPageRange(labSection);
        List<String> labels = getLaboratoryLabels();

        Map<String, String> extractedResults = imageTextExtractor.extractTextFromTable(
                document, targetPages, labels
        );

        // Mapear resultados extraídos a nombres estandarizados
        addValueIfPresent(values, "HEMATOCRITO", extractedResults.get("HEMATOCRITO"));
        addValueIfPresent(values, "HEMOGLOBINA", extractedResults.get("HEMOGLOBINA"));
        addValueIfPresent(values, "RTO.BLANCOS", extractedResults.get("RTO.BLANCOS"));
        addValueIfPresent(values, "RTO.PLAQUETAS", extractedResults.get("RTO.PLAQUETAS"));
        addValueIfPresent(values, "GLICEMIA", extractedResults.get("GLICEMIA"));
        addValueIfPresent(values, "COLESTEROL TOTAL", extractedResults.get("REFERENCIA COLESTEROL"));
        addValueIfPresent(values, "COLESTEROL HDL", extractedResults.get("TOTAL COLESTEROL"));
        addValueIfPresent(values, "COLESTEROL LDL", extractedResults.get("HDL COLESTEROL"));
        addValueIfPresent(values, "TRIGLICERIDOS", extractedResults.get("TRIGLICERIDOS"));

        return values;
    }

    private void addValueIfPresent(Map<String, String> map, String key, String value) {
        if (value != null && !value.trim().isEmpty()) {
            map.put(key, value);
        }
    }

    private List<Integer> getPageRange(SectionArea labSection) {
        List<Integer> pages = new ArrayList<>();
        for (int i = labSection.startPage(); i <= labSection.endPage(); i++) {
            pages.add(i);
        }
        return pages;
    }

    //Retorna lista de etiquetas a buscar
    //Ahora las etiquetas coinciden exactamente con las de la imagen
    private List<String> getLaboratoryLabels() {
        return Arrays.asList(
                "HEMATOCRITO",
                "HEMOGLOBINA",
                "RTO.BLANCOS",
                "RTO.PLAQUETAS",
                "GLICEMIA",
                "REFERENCIA COLESTEROL",
                "TOTAL COLESTEROL",
                "HDL COLESTEROL",
                "TRIGLICERIDOS"
        );
    }
}

//package com.example.PruebaPDFBox.business.ExamsImgBusiness;
//
//import com.example.PruebaPDFBox.model.ClinicalHistory;
//import com.example.PruebaPDFBox.model.DataType;
//import com.example.PruebaPDFBox.model.DataValue;
//import com.example.PruebaPDFBox.model.Exam;
//import com.example.PruebaPDFBox.model.Worker;
//import com.example.PruebaPDFBox.record.SectionArea;
//import com.example.PruebaPDFBox.repository.ClinicalHistoryRepository;
//import com.example.PruebaPDFBox.repository.DataTypeRepository;
//import com.example.PruebaPDFBox.repository.DataValueRepository;
//import com.example.PruebaPDFBox.repository.ExamRepository;
//import com.example.PruebaPDFBox.utilities.extractor.TextExtractor.ImageTextExtractor;
//import com.example.PruebaPDFBox.utilities.extractor.TextExtractor.PlainTextExtractor;
//import org.apache.pdfbox.pdmodel.PDDocument;
//import org.springframework.stereotype.Component;
//
//import java.io.IOException;
//import java.util.*;
//
//@Component
//public class LaboratoryResultImgBusiness {
//
//    private final ImageTextExtractor imageTextExtractor;
//    private final PlainTextExtractor plainTextExtractor;
//    private final ExamRepository examRepository;
//    private final ClinicalHistoryRepository clinicalHistoryRepository;
//    private final DataTypeRepository dataTypeRepository;
//    private final DataValueRepository dataValueRepository;
//
//    public LaboratoryResultImgBusiness(
//            ImageTextExtractor imageTextExtractor,
//            PlainTextExtractor plainTextExtractor,
//            ExamRepository examRepository,
//            ClinicalHistoryRepository clinicalHistoryRepository, DataTypeRepository dataTypeRepository, DataValueRepository dataValueRepository) {
//        this.imageTextExtractor = imageTextExtractor;
//        this.plainTextExtractor = plainTextExtractor;
//        this.examRepository = examRepository;
//        this.clinicalHistoryRepository = clinicalHistoryRepository;
//        this.dataTypeRepository = dataTypeRepository;
//        this.dataValueRepository = dataValueRepository;
//    }
//
//    public boolean process(PDDocument document, SectionArea labSection, String examName, Worker worker) throws IOException {
//
//        String examDate = plainTextExtractor.extractExamDate(document, labSection);
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
//        List<Integer> targetPages = getPageRange(labSection);
//        Map<String, String> results;
//
//        List<String> labels = getLaboratoryLabels();
//        results = imageTextExtractor.extractTextFromTable(document, targetPages, labels);
//
//        Exam exam = new Exam();
//        exam.setExamName(examName);
//        exam.setExamDate(examDate);
//        exam.setClinicalHistory(hc);
//        exam.setWorker(worker);
//        exam = examRepository.save(exam);
//
//        // Mapear los resultados extraídos a los campos de DataExam
//        createDataValue("HEMATOCRITO",
//                results.get("HEMATOCRITO"),
//                exam);
//
//        createDataValue("HEMOGLOBINA",
//                results.get("HEMOGLOBINA"),
//                exam
//        );
//
//        createDataValue("RTO.BLANCOS",
//                results.get("RTO.BLANCOS"),
//                exam
//        );
//
//        createDataValue("RTO.PLAQUETAS",
//                results.get("RTO.PLAQUETAS"),
//                exam
//        );
//
//        createDataValue("GLICEMIA",
//                results.get("GLICEMIA"),
//                exam
//        );
//
//        createDataValue("COLESTEROL TOTAL",
//                results.get("REFERENCIA COLESTEROL"),
//                exam
//        );
//
//        createDataValue("COLESTEROL HDL",
//                results.get("TOTAL COLESTEROL"),
//                exam
//        );
//
//        createDataValue("COLESTEROL LDL",
//                results.get("HDL COLESTEROL"),
//                exam
//        );
//
//        createDataValue("TRIGLICERIDOS",
//                results.get("TRIGLICERIDOS"),
//                exam
//        );
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
//    private List<Integer> getPageRange(SectionArea section) {
//        List<Integer> pages = new ArrayList<>();
//        for (int i = section.startPage(); i <= section.endPage(); i++) {
//            pages.add(i);
//        }
//        return pages;
//    }
//
//    //Retorna lista de etiquetas a buscar
//    //Ahora las etiquetas coinciden exactamente con las de la imagen
//    private List<String> getLaboratoryLabels() {
//        return Arrays.asList(
//                // Cuadro Hemático
//                "HEMATOCRITO",
//                "HEMOGLOBINA",
//                "RTO.BLANCOS",
//                "RTO.PLAQUETAS",
//
//                // Química Sanguínea
//                "GLICEMIA",
//                "REFERENCIA COLESTEROL",
//                "TOTAL COLESTEROL",
//                "HDL COLESTEROL",
//                "TRIGLICERIDOS"
//        );
//    }
//}
