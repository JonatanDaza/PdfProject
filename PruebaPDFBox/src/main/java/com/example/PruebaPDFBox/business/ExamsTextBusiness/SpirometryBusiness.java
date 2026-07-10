package com.example.PruebaPDFBox.business.ExamsTextBusiness;

import java.util.*;

import org.springframework.stereotype.Component;

import com.example.PruebaPDFBox.model.*;
import com.example.PruebaPDFBox.record.OcrPageContext;
import com.example.PruebaPDFBox.record.SectionArea;
import com.example.PruebaPDFBox.service.ServiceEntity.ClinicalHistoryService;
import com.example.PruebaPDFBox.service.ServiceEntity.DataValueService;
import com.example.PruebaPDFBox.service.ServiceEntity.ExamService;
import com.example.PruebaPDFBox.utilities.extractor.TextExtractor.OcrFieldExtractor;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SpirometryBusiness {

    private final OcrFieldExtractor ocrFieldExtractor;
    private final ExamService examService;
    private final ClinicalHistoryService clinicalHistoryService;
    private final DataValueService dataValueService;

    public boolean process(OcrPageContext context, SectionArea section, Worker worker, String examName) {
        try {
                // Extraer subsecciones primero
                String sectionText = context.textForSection(section.startPage(), section.endPage());

                System.out.println("=== ESPIROMETRÍA - TEXTO OCR ===");
                System.out.println(sectionText);

                String rawDate = ocrFieldExtractor.extractAfterLabel(sectionText, "Fecha:");

                if (rawDate.isBlank()) {
                    rawDate = ocrFieldExtractor.extractAfterLabel(sectionText, "Fecha y Hora:");
                }

                String examDate = ocrFieldExtractor.extractDate(rawDate);

                if (examDate.isBlank()) {
                    examDate = ocrFieldExtractor.extractDate(sectionText);
                }

                if (examDate.isBlank()) {
                    System.err.println("No se encontró fecha");
                    return false;
                }

            if (examDate == null || examDate.isEmpty()) {
                System.err.println("No se encontró fecha de examen en DATOS PERSONALES");
                return false;
            }

            if (examService.examAlreadyExists(examDate, examName)) {
                System.out.println("Examen ya existe: " + examName + " - " + examDate);
                return false;
            }

            System.out.println("rawDate: " + rawDate);
            System.out.println("examDate: " + examDate);
            
            Map<String, String> values = buildDataValues(context, section);

            System.out.println("=== ESPIROMETRÍA - VALORES ===");
            values.forEach((k, v) ->
                System.out.println("  [" + k + "] -> '" + v + "'")
            );

            ClinicalHistory clinicalHistory = clinicalHistoryService.findOrCreate(worker, examDate);
            Exam exam = examService.save(examName, examDate, worker, clinicalHistory);
            dataValueService.saveAll(values, exam);

            return true;
        } catch (Exception e) {
            System.err.println("Error procesando medicina ocupacional: " + e.getMessage());
            return false;
        }
    }

    private Map<String, String> buildDataValues(
            OcrPageContext context,
            SectionArea section
    ) {

        Map<String, String> values = new LinkedHashMap<>();

        String text = context.textForSection(section.startPage(), section.endPage());

        String interpretacion = ocrFieldExtractor.extractAfterLabel(text, "Interpretación:");

        if (interpretacion.isBlank()) {
            interpretacion = ocrFieldExtractor.extractAfterLabel(text, "Interpretacion:");
        }

        values.put("Interpretación", interpretacion);

        return values;
    }
}
