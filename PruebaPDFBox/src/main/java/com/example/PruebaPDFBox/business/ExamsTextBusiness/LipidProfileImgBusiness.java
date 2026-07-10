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
public class LipidProfileImgBusiness {

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
                System.err.println("No se encontró fecha en LipidProfileImgBusiness");
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
            System.err.println("Error procesando Perfil Lipídico: " + e.getMessage());
            return false;
        }
    }

    private Map<String, String> buildDataValues(OcrPageContext context, SectionArea section) {

        Map<String, String> values = new LinkedHashMap<>();

        String text = context.textForSection(section.startPage(), section.endPage());

        String flat = text.replaceAll("\\r?\\n", " ").replaceAll("\\s+", " ");

        System.out.println("=== PERFIL LIPÍDICO - flat ===\n" + flat);
        addIfPresent(values,
                "COLESTEROL TOTAL",
                ocrFieldExtractor.extractLabValue(text, "COLESTEROL TOTAL"));

        if (!values.containsKey("COLESTEROL TOTAL")) {
            addIfPresent(values, "COLESTEROL TOTAL",
                    ocrFieldExtractor.extractLabValue(text, "TOTAL"));
        }
        
        addIfPresent(values,
            "COLESTEROL HDL",
            ocrFieldExtractor.extractLabValue(text, "HDL"));

        if (!values.containsKey("COLESTEROL HDL")) {
            addIfPresent(values, "COLESTEROL HDL",
                    ocrFieldExtractor.extractLabValue(text, "HDL"));
        }

        addIfPresent(values,
                "COLESTEROL LDL",
                ocrFieldExtractor.extractLabValue(text, "LDL"));

        addIfPresent(values, "COLESTEROL LDL",    extractLdl(flat));

        if (!values.containsKey("COLESTEROL LDL")) {
            addIfPresent(values, "COLESTEROL LDL",
                    ocrFieldExtractor.extractLabValue(text, "LDL"));
        }

        addIfPresent(values,
                "TRIGLICERIDOS",
                ocrFieldExtractor.extractLabValue(text, "TRIGLICERIDOS"));


        System.out.println("=== PERFIL LIPÍDICO - valores ===");
        values.forEach((k, v) -> System.out.println("  [" + k + "] -> '" + v + "'"));
        return values;
    }

    private String extractLdl(String flat) {
        System.out.println("=== LDL - buscando en flat ===");

        // Patrón 1: "COLES 138* mg/dl 50-130 LDL"
        // "COLES " con espacio evita matchear "COLESTEROL"
        Matcher m1 = Pattern.compile(
                "(?i)\\bCOLES\\s+(\\d+[.,]?\\d*)\\s*\\*?\\s*mg"
        ).matcher(flat);
        if (m1.find()) {
            System.out.println("  LDL patrón 1 (COLES+espacio): '" + m1.group(1) + "'");
            return m1.group(1).replace(',', '.');
        }

        // Patrón 2: "TEROL COLES 138" — OCR parte "COLESTEROL" en "COLES TEROL"
        Matcher m2 = Pattern.compile(
                "(?i)TEROL\\s+COLES\\s+(\\d+[.,]?\\d*)"
        ).matcher(flat);
        if (m2.find()) {
            System.out.println("  LDL patrón 2 (TEROL COLES): '" + m2.group(1) + "'");
            return m2.group(1).replace(',', '.');
        }

        // Patrón 3: número entre "50-130" (rango de referencia del LDL) precedido de valor
        Matcher m3 = Pattern.compile(
                "(\\d+[.,]?\\d*)\\s*\\*?\\s*mg/dl\\s+50-130"
        ).matcher(flat);
        if (m3.find()) {
            System.out.println("  LDL patrón 3 (rango 50-130): '" + m3.group(1) + "'");
            return m3.group(1).replace(',', '.');
        }

        String fallback = ocrFieldExtractor.extractLabValue(flat, "LDL");
        System.out.println("  LDL fallback: '" + fallback + "'");
        return fallback;
    }
    
    private void addIfPresent(Map<String, String> map, String key, String value) {
        if (value != null && !value.isBlank()) map.put(key, value);
    }
}