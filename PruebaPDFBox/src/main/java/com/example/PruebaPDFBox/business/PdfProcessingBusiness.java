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

    private final UnifiedSectionDetector unifiedSectionDetector; // ← reemplaza los dos anteriores
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

            // ① Renderizar páginas → OCR → contexto inmutable
            List<BufferedImage> pages = pdfPageRenderer.renderPages(document);
            List<String> pageTexts = pages.stream()
                    .map(imageTextExtractor::extractTextFromSingleImage)
                    .map(t -> t != null ? t : "")
                    .toList();
            OcrPageContext context = new OcrPageContext(pageTexts);

            // ② Detectar secciones desde el contexto OCR
            Map<String, SectionArea> sections = unifiedSectionDetector.detectFromContext(context);
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

                // ③ Extraer worker usando OCR context (no PDDocument)
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

                    // Las imágenes embebidas aún usan PDDocument (datos binarios, no texto)
                    if (pdfImgSectionExtractor.hasValidImages(document, sectionArea)) {
                        processed = examImgBusiness.processExam(document, sectionKey, sectionArea, worker);
                    }

                    // Los exámenes de texto usan context OCR
                    boolean textProcessed = switch (sectionKey) {
                        case "OCCUPATIONAL_MEDICINE_LABEL" ->
                                occupationalMedicineTextBusiness.process(context, sectionArea, worker, sectionArea.examName());
                        case "AUDIOMETRY_LABEL" ->
                                audiometryTextBusiness.process(context, sectionArea, worker, sectionArea.examName());
                        case "OPTOMETRY_LABEL" ->
                                optometryTextBusiness.process(context, sectionArea, worker, sectionArea.examName());
                        // case "SPIROMETRY_LABEL" ->
                        //         throw new UnsupportedOperationException("SPIROMETRY_LABEL aún no implementado");
                        // case "GLYCEMIA_LABEL" ->
                        //         throw new UnsupportedOperationException("GLYCEMIA_LABEL aún no implementado");
                        // case "HEMOGRAM_LABEL" ->
                        //         throw new UnsupportedOperationException("HEMOGRAM_LABEL aún no implementado");
                        // case "LIPID_PROFILE_LABEL" ->
                        //         throw new UnsupportedOperationException("LIPID_PROFILE_LABEL aún no implementado");
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

// @Component
// @RequiredArgsConstructor
// public class PdfProcessingBusiness {
//     private final PdfSectionExtractor pdfSectionExtractor;
//     private final PdfOcrSectionDetector pdfOcrSectionDetector;
//     private final PdfImgSectionExtractor pdfImgSectionExtractor;
//     private final ExamImgBusiness examImgBusiness;
//     private final WorkerExamBusiness workerExamBusiness;
//     private final WorkerService workerService;
//     private final OccupationalMedicineTextBusiness occupationalMedicineTextBusiness;
//     private final AudiometryTextBusiness audiometryTextBusiness;
//     private final OptometryTextBusiness optometryTextBusiness;
//     private final SpirometryBusiness spirometryBusiness;
//     private final LaboratoryResultTextBusiness laboratoryResultTextBusiness;
//     public Map<String, Object> processPdf(MultipartFile file) {
//         try (PDDocument document = PDDocument.load(file.getInputStream())) {
//             // Mapper Sections of Plain Text
//             Map<String, SectionArea> sections = pdfSectionExtractor.extract(document);
//             System.out.println("Secciones por texto: " + sections.keySet());
//             // Mapper Sections of Images
//             Map<String, SectionArea> imageSections = pdfOcrSectionDetector.detectSectionsInImages(document);
//             if (!imageSections.isEmpty()) {
//                 System.out.println("Secciones detectadas en imágenes: " + imageSections.keySet());
//                 sections.putAll(imageSections); // Combinar ambos mapas
//             }
//             Worker worker = null;
//             boolean workerPersisted = false;
//             int examsProcessed = 0;
//             for (Map.Entry<String, SectionArea> entry : sections.entrySet()) {
//                 String sectionKey = entry.getKey();
//                 SectionArea sectionArea = entry.getValue();
//                 System.out.println("Procesando sección: " + sectionKey +
//                         " (páginas " + (sectionArea.startPage() + 1) +
//                         " - " + (sectionArea.endPage() + 1) + ")");
//                 // Intentar extraer Worker si aún no existe
//                 if (worker == null) {
//                     try {
//                         worker = workerExamBusiness.processWorker(document, sectionArea);
//                         if (worker != null) {
//                             System.out.println("Worker extraído desde: " + sectionArea.examName());
//                             worker = workerService.findOrCreate(worker);
//                             workerPersisted = true;
//                         }
//                     } catch (Exception e) {
//                         System.err.println("Error extrayendo Worker desde "
//                                 + sectionArea.examName() + ": " + e.getMessage());
//                     }
//                 }
//                 // Procesar exámenes SOLO si el Worker ya está listo
//                 if (workerPersisted) {
//                     boolean processed = false;
//                     if (pdfImgSectionExtractor.hasValidImages(document, sectionArea)) {
//                         System.out.println("Sección con imágenes: " + sectionKey);
//                         boolean imgProcessed = examImgBusiness.processExam(
//                                 document,
//                                 sectionKey,
//                                 sectionArea,
//                                 worker
//                         );
//                         processed = processed || imgProcessed;
//                     }
//                     boolean textProcessed = switch (sectionKey) {
//                         case "OCCUPATIONAL_MEDICINE_LABEL" ->
//                                 occupationalMedicineTextBusiness.process(
//                                         document, sectionArea, worker, sectionArea.examName());
//                         case "AUDIOMETRY_LABEL" ->
//                                 audiometryTextBusiness.process(
//                                         document, sectionArea, worker, sectionArea.examName());
//                         case "OPTOMETRY_LABEL" ->
//                                 optometryTextBusiness.process(
//                                         document, sectionArea, worker, sectionArea.examName());
// //
// //                        case "SPIROMETRY_LABEL" ->
// //                                spirometryBusiness.process(
// //                                        document, sectionArea, worker, sectionKey);
// //
// //                        case "LABORATORY_LABEL_TEXT" ->
// //                                laboratoryResultTextBusiness.process(
// //                                        document, sectionArea, worker);
// //
//                         default -> false;
//                     };
//                     processed = processed || textProcessed;
//                     if (processed) {
//                         examsProcessed++;
//                         System.out.println("Examen procesado: " + sectionArea.examName());
//                     } else {
//                         System.out.println("No se procesó: " + sectionArea.examName());
//                     }
//                 }
//             }
//             if (worker == null) {
//                 throw new RuntimeException("No se pudo extraer datos válidos del trabajador");
//             }
//             Map<String, Object> response = new HashMap<>();
//             response.put("message", "PDF procesado correctamente");
//             response.put("examsProcessed", examsProcessed);
//             response.put("totalPages", document.getNumberOfPages());
//             response.put("sectionsFound", sections.keySet());
//             return response;
//         } catch (Exception e) {
//             throw new RuntimeException("Error procesando PDF", e);
//         }
//     }
// }