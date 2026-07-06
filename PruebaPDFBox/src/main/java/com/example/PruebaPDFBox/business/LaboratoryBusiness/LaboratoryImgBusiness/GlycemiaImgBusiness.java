package com.example.PruebaPDFBox.business.LaboratoryBusiness.LaboratoryImgBusiness;

import java.io.IOException;
import java.util.*;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.stereotype.Component;

import com.example.PruebaPDFBox.model.ClinicalHistory;
import com.example.PruebaPDFBox.model.Exam;
import com.example.PruebaPDFBox.model.Worker;
import com.example.PruebaPDFBox.record.OcrPageContext;
import com.example.PruebaPDFBox.record.SectionArea;
import com.example.PruebaPDFBox.service.ServiceEntity.ClinicalHistoryService;
import com.example.PruebaPDFBox.service.ServiceEntity.DataValueService;
import com.example.PruebaPDFBox.service.ServiceEntity.ExamService;
import com.example.PruebaPDFBox.utilities.extractor.TextExtractor.ImageTextExtractor;
import com.example.PruebaPDFBox.utilities.extractor.TextExtractor.OcrFieldExtractor;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class GlycemiaImgBusiness {

    private final ImageTextExtractor imageTextExtractor;
    private final OcrFieldExtractor ocrFieldExtractor;
    private final ExamService examService;
    private final ClinicalHistoryService clinicalHistoryService;
    private final DataValueService dataValueService;

    public boolean process(
            PDDocument document,
            SectionArea section,
            Worker worker,
            String examName,
            OcrPageContext context  // ← recibe el contexto OCR
    ) throws Exception {
        try {
            // Fecha desde el texto OCR de la sección
            String sectionText = context.textForSection(section.startPage(), section.endPage());
            String rawDate = ocrFieldExtractor.extractAfterLabel(sectionText, "Fecha:");
            String examDate = ocrFieldExtractor.extractDate(rawDate);

            if (examDate == null || examDate.isBlank()) {
                System.err.println("No se encontró fecha en GlycemiaImgBusiness");
                return false;
            }

            if (examService.examAlreadyExists(examDate, examName)) {
                System.out.println("Examen ya existe: " + examName + " - " + examDate);
                return false;
            }

            Map<String, String> values = buildDataValues(document, section);

            ClinicalHistory clinicalHistory = clinicalHistoryService.findOrCreate(worker, examDate);
            Exam exam = examService.save(examName, examDate, worker, clinicalHistory);
            dataValueService.saveAll(values, exam);

            return true;
        } catch (IOException e) {
            System.err.println("Error procesando Glicemia: " + e.getMessage());
            return false;
        }
    }

    private Map<String, String> buildDataValues(PDDocument document, SectionArea section) throws Exception {
        Map<String, String> values = new LinkedHashMap<>();
        List<Integer> pages = getPageRange(section);

        Map<String, String> extracted = imageTextExtractor.extractTextFromTable(
                document, pages, List.of("GLICEMIA")
        );

        addIfPresent(values, "GLICEMIA", extracted.get("GLICEMIA"));
        return values;
    }

    private List<Integer> getPageRange(SectionArea section) {
        List<Integer> pages = new ArrayList<>();
        for (int i = section.startPage(); i <= section.endPage(); i++) pages.add(i);
        return pages;
    }

    private void addIfPresent(Map<String, String> map, String key, String value) {
        if (value != null && !value.isBlank()) map.put(key, value);
    }
}