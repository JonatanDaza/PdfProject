package com.example.PruebaPDFBox.business.LaboratoryBusiness.LaboratoryImgBusiness;

import java.io.IOException;
import java.util.*;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.stereotype.Component;

import com.example.PruebaPDFBox.model.ClinicalHistory;
import com.example.PruebaPDFBox.model.Exam;
import com.example.PruebaPDFBox.model.Worker;
import com.example.PruebaPDFBox.record.SectionArea;
import com.example.PruebaPDFBox.service.ServiceEntity.ClinicalHistoryService;
import com.example.PruebaPDFBox.service.ServiceEntity.DataValueService;
import com.example.PruebaPDFBox.service.ServiceEntity.ExamService;
import com.example.PruebaPDFBox.utilities.extractor.TextExtractor.ImageTextExtractor;
import com.example.PruebaPDFBox.utilities.extractor.TextExtractor.PlainTextExtractor;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class GlycemiaImgBusiness {

    private final ImageTextExtractor imageTextExtractor;
    private final PlainTextExtractor plainTextExtractor;
    private final ExamService examService;
    private final ClinicalHistoryService clinicalHistoryService;
    private final DataValueService dataValueService;

    public boolean process(PDDocument document, SectionArea glycemiaSection, Worker worker, String examName) throws Exception {
        try {
            String examDate = plainTextExtractor.extractExamDate(document, glycemiaSection);

            if (examDate == null || examDate.isEmpty()) {
                System.err.println("No se encontró fecha de examen en DATOS PERSONALES");
                return false;
            }

            if (examService.examAlreadyExists(examDate, examName)) {
                System.out.println("Examen ya existe: " + examName + " - " + examDate);
                return false;
            }

            Map<String, String> values = buildDataValues(document, glycemiaSection);

            ClinicalHistory clinicalHistory = clinicalHistoryService.findOrCreate(worker, examDate);
            Exam exam = examService.save(examName, examDate, worker, clinicalHistory);
            dataValueService.saveAll(values, exam);

            return true;
        } catch (IOException e) {
            System.err.println("Error procesando resultados de Glicemia: " + e.getMessage());
            return false;
        }
    }


    private Map<String, String> buildDataValues(PDDocument document, SectionArea glycemiaSection) throws Exception {
        Map<String, String> values = new LinkedHashMap<>();

        List<Integer> targetPages = getPageRange(glycemiaSection);
        List<String> labels = getLaboratoryLabels();

        Map<String, String> extractedResults = imageTextExtractor.extractTextFromTable(
                document, targetPages, labels
        );

        // Mapear resultados extraídos a nombres estandarizados
        addValueIfPresent(values, "GLICEMIA", extractedResults.get("GLICEMIA"));

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
        return List.of(
                //HEMATOLOGIA GLICEMIA
                "GLICEMIA"
        );
    }
}
