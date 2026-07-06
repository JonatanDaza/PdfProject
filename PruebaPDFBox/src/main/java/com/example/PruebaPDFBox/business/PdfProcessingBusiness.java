package com.example.PruebaPDFBox.business;

import java.awt.image.BufferedImage;
import java.util.*;

import com.example.PruebaPDFBox.business.LaboratoryBusiness.ExamImgBusiness;
import com.example.PruebaPDFBox.utilities.extractor.SectionExtractor.PdfImgSectionExtractor;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.example.PruebaPDFBox.business.ExamsTextBusiness.OptometryTextBusiness;
import com.example.PruebaPDFBox.business.ExamsTextBusiness.AudiometryTextBusiness;
import com.example.PruebaPDFBox.business.ExamsTextBusiness.OccupationalMedicineTextBusiness;
import com.example.PruebaPDFBox.business.ExamsTextBusiness.WorkerExamBusiness;
import com.example.PruebaPDFBox.model.Worker;
import com.example.PruebaPDFBox.record.OcrPageContext;
import com.example.PruebaPDFBox.record.SectionArea;
import com.example.PruebaPDFBox.service.ServiceEntity.WorkerService;
import com.example.PruebaPDFBox.utilities.extractor.SectionExtractor.PdfPageRenderer;
import com.example.PruebaPDFBox.utilities.extractor.SectionExtractor.UnifiedSectionDetector;
import com.example.PruebaPDFBox.utilities.extractor.TextExtractor.ImageTextExtractor;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PdfProcessingBusiness {

    private final UnifiedSectionDetector unifiedSectionDetector;
    private final PdfImgSectionExtractor pdfImgSectionExtractor;
    private final ExamImgBusiness examImgBusiness;
    private final WorkerExamBusiness workerExamBusiness;
    private final WorkerService workerService;
    private final OccupationalMedicineTextBusiness occupationalMedicineTextBusiness;
    private final AudiometryTextBusiness audiometryTextBusiness;
    private final OptometryTextBusiness optometryTextBusiness;
    private final PdfPageRenderer pdfPageRenderer;
    private final ImageTextExtractor imageTextExtractor;

    public Map<String, Object> processPdf(MultipartFile file) {
        try (PDDocument document = PDDocument.load(file.getInputStream())) {

            List<BufferedImage> pages = pdfPageRenderer.renderPages(document);
            List<String> pageTexts = pages.stream()
                    .map(imageTextExtractor::extractTextFromSingleImage)
                    .map(t -> t != null ? t : "")
                    .toList();
            OcrPageContext context = new OcrPageContext(pageTexts);

            Map<String, SectionArea> sections = unifiedSectionDetector.detectFromContext(context, document);
            System.out.println("Secciones detectadas: " + sections.keySet());

            if (sections.isEmpty()) {
                return Map.of(
                        "message", "No se detectaron secciones en el PDF",
                        "totalPages", document.getNumberOfPages()
                );
            }

            Worker worker = null;
            boolean workerPersisted = false;
            int examsProcessed = 0;

            for (Map.Entry<String, SectionArea> entry : sections.entrySet()) {
                String sectionKey   = entry.getKey();
                SectionArea sectionArea = entry.getValue();

                if (worker == null) {
                    try {
                        worker = workerExamBusiness.processWorker(context, sectionArea);
                        if (worker != null) {
                            worker = workerService.findOrCreate(worker);
                            workerPersisted = true;
                            System.out.println("Worker extraído desde: " + sectionArea.examName());
                        }
                    } catch (Exception e) {
                        System.err.println("Error extrayendo Worker: " + e.getMessage());
                    }
                }

                if (workerPersisted) {
                    boolean processed = false;

                    if (pdfImgSectionExtractor.hasValidImages(document, sectionArea)) {
                        processed = examImgBusiness.processExam(document, sectionKey, sectionArea, worker, context);
                    }

                    boolean textProcessed = switch (sectionKey) {
                        case "OCCUPATIONAL_MEDICINE_LABEL" ->
                                occupationalMedicineTextBusiness.process(context, sectionArea, worker, sectionArea.examName());
                        case "AUDIOMETRY_LABEL" ->
                                audiometryTextBusiness.process(context, sectionArea, worker, sectionArea.examName());
                        case "OPTOMETRY_LABEL" ->
                                optometryTextBusiness.process(context, sectionArea, worker, sectionArea.examName());
                        default -> false;
                    };

                    processed = processed || textProcessed;
                    if (processed) {
                        examsProcessed++;
                        System.out.println("Examen procesado: " + sectionArea.examName());
                    }
                }
            }

            if (worker == null) {
                throw new RuntimeException(
                        "No se pudo extraer datos del trabajador. Secciones encontradas: " + sections.keySet()
                );
            }

            return Map.of(
                    "message", "PDF procesado correctamente",
                    "examsProcessed", examsProcessed,
                    "totalPages", document.getNumberOfPages(),
                    "sectionsFound", sections.keySet()
            );

        } catch (Exception e) {
            throw new RuntimeException("Error procesando PDF: " + e.getMessage(), e);
        }
    }
}