package com.example.PruebaPDFBox.business;

import com.example.PruebaPDFBox.model.ClinicalHistory;
import com.example.PruebaPDFBox.model.DataExam;
import com.example.PruebaPDFBox.model.Exam;
import com.example.PruebaPDFBox.model.Worker;
import com.example.PruebaPDFBox.record.SectionArea;
import com.example.PruebaPDFBox.repository.ClinicalHistoryRepository;
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

    public LaboratoryResultBusiness(
            ImageTextExtractor imageTextExtractor,
            SectionTextExtractor sectionTextExtractor,
            ExamRepository examRepository,
            ClinicalHistoryRepository clinicalHistoryRepository) {
        this.imageTextExtractor = imageTextExtractor;
        this.sectionTextExtractor = sectionTextExtractor;
        this.examRepository = examRepository;
        this.clinicalHistoryRepository = clinicalHistoryRepository;
    }

    public boolean process(PDDocument document, SectionArea labSection, String examName, Worker worker) throws IOException {

        // Extraer la fecha del examen con múltiples estrategias
        String examDate = sectionTextExtractor.extractExamDate(document, labSection);

        // Validar que se haya extraído una fecha válida
        if (examDate == null || examDate.isEmpty()) {
            return false;
        }

        // Verificar si el examen ya existe
        if (examRepository.findByExamDateAndExamName(examDate, examName).isPresent()) {
            return false;
        }

        // Obtener o crear historia clínica
        ClinicalHistory hc = clinicalHistoryRepository.findByWorkerAndExamDate(worker, examDate)
                .orElseGet(() -> clinicalHistoryRepository.save(new ClinicalHistory()));

        // Extraer datos de la tabla de laboratorio
        List<Integer> targetPages = getPageRange(labSection);
        List<String> labels = getLaboratoryLabels();

        Map<String, String> results = imageTextExtractor.extractTextFromTable(
                document,
                targetPages,
                labels
        );

        // Crear entidades
        DataExam dataExam = new DataExam();
        Exam exam = new Exam();

        // Asignar información del examen
        exam.setExamName(examName);
        exam.setExamDate(examDate);
        exam.setClinicalHistory(hc);
        exam.setWorker(worker);
        exam.setDataExam(dataExam);

        // Mapear los resultados extraídos a los campos de DataExam
        dataExam.setHematocrit(results.get("HEMATOCRITO"));
        dataExam.setHemoglobin(results.get("HEMOGLOBINA"));
        dataExam.setWhiteBloodCells(results.get("RTO.BLANCOS"));
        dataExam.setPlateletCount(results.get("RTO.PLAQUETAS"));
        dataExam.setGlycemia(results.get("GLICEMIA"));
        dataExam.setCholesterolTotal(results.get("COLESTEROL TOTAL"));
        dataExam.setCholesterolHDL(results.get("COLESTEROL HDL"));
        dataExam.setCholesterolLDL(results.get("COLESTEROL LDL"));
        dataExam.setTriglycerides(results.get("TRIGLICERIDOS"));

        // Guardar en la base de datos
        examRepository.save(exam);

        return true;
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
}
