package com.example.PruebaPDFBox.business.ExamsTextBusiness;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.example.PruebaPDFBox.model.ClinicalHistory;
import com.example.PruebaPDFBox.model.Exam;
import com.example.PruebaPDFBox.model.Worker;
import com.example.PruebaPDFBox.record.OcrPageContext;
import com.example.PruebaPDFBox.record.SectionArea;
import com.example.PruebaPDFBox.service.ServiceEntity.ClinicalHistoryService;
import com.example.PruebaPDFBox.service.ServiceEntity.DataValueService;
import com.example.PruebaPDFBox.service.ServiceEntity.ExamService;
import com.example.PruebaPDFBox.utilities.extractor.TextExtractor.OcrFieldExtractor;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class GlycemiaImgBusiness {

    private final OcrFieldExtractor ocrFieldExtractor;
    private final ExamService examService;
    private final ClinicalHistoryService clinicalHistoryService;
    private final DataValueService dataValueService;

    public boolean process(
        OcrPageContext context,
            SectionArea section,
            Worker worker,
            String examName
    ) throws Exception {
        try {
            String sectionText = context.textForSection(section.startPage(), section.endPage());
            String rawDate = ocrFieldExtractor.extractAfterLabel(sectionText, "Fecha y Hora:");
            String examDate = ocrFieldExtractor.extractDate(rawDate);

            // Fallback: buscar cualquier fecha en el texto
            if (examDate == null || examDate.isBlank()) {
                Matcher m = Pattern.compile("\\d{2}/\\d{2}/\\d{4}").matcher(sectionText);
                if (m.find()) examDate = m.group();
            }

            if (examDate == null || examDate.isBlank()) {
                System.err.println("No se encontró fecha en GlycemiaImgBusiness");
                return false;
            }

            if (examService.examAlreadyExists(examDate, examName)) {
                System.out.println("Examen ya existe: " + examName + " - " + examDate);
                return false;
            }

            Map<String, String> values = buildDataValues(context, section);

            ClinicalHistory clinicalHistory = clinicalHistoryService.findOrCreate(worker, examDate);
            Exam exam = examService.save(examName, examDate, worker, clinicalHistory);
            dataValueService.saveAll(values, exam);

            return true;
        } catch (Exception e) {
            System.err.println("Error procesando Glicemia: " + e.getMessage());
            return false;
        }
    }

    private Map<String, String> buildDataValues(OcrPageContext context, SectionArea section) {

        Map<String, String> values = new LinkedHashMap<>();

        String text = context.textForSection(section.startPage(), section.endPage());

        System.out.println("=== TEXTO OCR GLICEMIA ===");
        System.out.println(text);

        String glicemia = ocrFieldExtractor.extractLabValue(text, "GLICEMIA");
        
        System.out.println("=== GLICEMIA EXTRAÍDA ===");
        System.out.println(glicemia);
        
        addIfPresent(values, "GLICEMIA", glicemia);

        return values;
    }

    private void addIfPresent(Map<String, String> map, String key, String value) {
        if (value != null && !value.isBlank()) map.put(key, value);
    }
}