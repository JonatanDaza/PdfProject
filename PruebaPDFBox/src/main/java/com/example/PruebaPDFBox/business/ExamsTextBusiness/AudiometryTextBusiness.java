package com.example.PruebaPDFBox.business.ExamsTextBusiness;

import com.example.PruebaPDFBox.model.ClinicalHistory;
import com.example.PruebaPDFBox.model.DataType;
import com.example.PruebaPDFBox.model.DataValue;
import com.example.PruebaPDFBox.model.Exam;
import com.example.PruebaPDFBox.model.Worker;
import com.example.PruebaPDFBox.record.AreaConfig;
import com.example.PruebaPDFBox.record.FieldExtractionConfig;
import com.example.PruebaPDFBox.record.SectionArea;
import com.example.PruebaPDFBox.record.SubSectionArea;
import com.example.PruebaPDFBox.repository.ClinicalHistoryRepository;
import com.example.PruebaPDFBox.repository.DataTypeRepository;
import com.example.PruebaPDFBox.repository.DataValueRepository;
import com.example.PruebaPDFBox.repository.ExamRepository;
import com.example.PruebaPDFBox.service.ServiceEntity.ClinicalHistoryService;
import com.example.PruebaPDFBox.service.ServiceEntity.DataValueService;
import com.example.PruebaPDFBox.service.ServiceEntity.ExamService;
import com.example.PruebaPDFBox.utilities.extractor.SectionExtractor.PdfSubSectionExtractor;
import com.example.PruebaPDFBox.utilities.extractor.TextExtractor.PlainTextExtractor;
import com.example.PruebaPDFBox.utilities.mapper.MapperJsons.ObservationsMapper;
import com.example.PruebaPDFBox.utilities.mapper.MapperJsons.RecommendationMapper;
import com.example.PruebaPDFBox.utilities.mapper.MapperJsons.WeightedDecibelsMapper;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;

@Component
@RequiredArgsConstructor
public class AudiometryTextBusiness {

    private final PlainTextExtractor plainTextExtractor;
    private final WeightedDecibelsMapper weightedDecibelsMapper;
    private final ObservationsMapper observationsMapper;
    private final RecommendationMapper recommendationMapper;
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
                Map<String, AreaConfig> config = buildAudiometryConfig();
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
        Map<String, AreaConfig> config = buildAudiometryConfig();

        return values;
    }

    private static final Set<String> SUB_SECTION_TITLES = Set.of();

    private Map<String, AreaConfig> buildAudiometryConfig() {

        Map<String, AreaConfig> config = new HashMap<>();

        config.put("Fecha:", new AreaConfig(0, 100, 12));

        config.put("Oído derecho:", new AreaConfig(0, 200, 21));
        config.put("Oído izquierdo:", new AreaConfig(0, 200, 21));
        config.put("Nombre del Diagnóstico", new AreaConfig(60, 130, 21));
//        config.put("Código", new AreaConfig(0, 60, 20));

        return config;
    }



//        Exam exam = new Exam();
//        exam.setExamName(examName);
//        exam.setExamDate(examDate);
//        exam.setClinicalHistory(hc);
//        exam.setWorker(worker);
//        exam = examRepository.save(exam);
//
//        createDataValue("Oído derecho",
//                data.get("oidoDerecho"),
//                exam);
//        createDataValue("Oído izquierdo",data.get("oidoIzquierdo"),
//                exam);
//        createDataValue("Nombre del Diagnóstico",data.get("diagnosticos"),
//                exam);
//
//        // Recorridos especiales
//        String rawWeightedDecibels = plainTextExtractor.extractTextBetweenLabels(
//                document, "dB(A)", "SEVERIDAD", section
//        );
//        String weightedDecibelsJson = weightedDecibelsMapper.parseToJson(rawWeightedDecibels);
//        createDataValue("Ponderado Decibelios", weightedDecibelsJson, exam);
//
//        String rawRecommendations = plainTextExtractor.extractTextBetweenLabels(
//                document, "OBSERVACION", "Observaciones", section
//        );
//        String recommendationsJson = recommendationMapper.parseToJson(rawRecommendations);
//        createDataValue("Recomendaciones", recommendationsJson, exam);
//
//        String rawObservations = plainTextExtractor.extractTextBetweenLabels(
//                document, "Observaciones", "FONOAUDIÓLOGO", section
//        );
//        String observationsJson = observationsMapper.parseToJson(rawObservations);
//        createDataValue("Observaciones", observationsJson, exam);
//
////        createDataValue("Código de Examen",
////                data.get("Código"),
////                exam);
//
//        examRepository.save(exam);
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
//        fields.add(new FieldExtractionConfig("Fecha:", "fecha",
//                config.get("Fecha:"), FieldExtractionConfig.ExtractionStrategy.HORIZONTAL));
//        fields.add(new FieldExtractionConfig("Oído derecho:", "oidoDerecho",
//                config.get("Oído derecho:"), FieldExtractionConfig.ExtractionStrategy.HORIZONTAL));
//        fields.add(new FieldExtractionConfig("Oído izquierdo:", "oidoIzquierdo",
//                config.get("Oído izquierdo:"), FieldExtractionConfig.ExtractionStrategy.HORIZONTAL));
//        fields.add(new FieldExtractionConfig("Nombre del Diagnóstico", "diagnosticos",
//                config.get("Nombre del Diagnóstico"), FieldExtractionConfig.ExtractionStrategy.VERTICAL));
////        fields.add(new FieldExtractionConfig("Código", "Código",
////                config.get("Código"), FieldExtractionConfig.ExtractionStrategy.VERTICAL));
//
//        return fields;
//    }
//
}
