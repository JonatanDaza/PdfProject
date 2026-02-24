package com.example.PruebaPDFBox.business;

// import com.example.PruebaPDFBox.business.ExamsTextBusiness.AudiometryTextBusiness;
// import com.example.PruebaPDFBox.business.ExamsImgBusiness.LaboratoryResultImgBusiness;
// import com.example.PruebaPDFBox.business.ExamsTextBusiness.LaboratoryResultTextBusiness;
// import com.example.PruebaPDFBox.business.ExamsTextBusiness.OptometryTextBusiness;
// import com.example.PruebaPDFBox.business.ExamsTextBusiness.SpirometryBusiness;
import com.example.PruebaPDFBox.business.ExamsTextBusiness.OccupationalMedicineTextBusiness;
import com.example.PruebaPDFBox.business.ExamsTextBusiness.WorkerExamBusiness;
import com.example.PruebaPDFBox.model.Worker;
import com.example.PruebaPDFBox.record.SectionArea;
import com.example.PruebaPDFBox.repository.WorkerRepository;
import com.example.PruebaPDFBox.service.ServiceEntity.WorkerService;
import com.example.PruebaPDFBox.utilities.extractor.SectionExtractor.PdfSectionExtractor;

import lombok.RequiredArgsConstructor;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@Component
@RequiredArgsConstructor
public class PdfProcessingBusiness {

    private final PdfSectionExtractor pdfSectionExtractor;
    private final WorkerRepository workerRepository;
    private final WorkerExamBusiness workerExamBusiness;
    private final OccupationalMedicineTextBusiness occupationalMedicineTextBusiness;
    private final WorkerService workerService;
    // private final AudiometryTextBusiness audiometryTextBusiness;
    // private final OptometryTextBusiness optometryTextBusiness;
    // private final LaboratoryResultImgBusiness laboratoryResultImgBusiness;
    // private final SpirometryBusiness spirometryBusiness;
    // private final LaboratoryResultTextBusiness laboratoryResultTextBusiness;

    public Map<String, Object> processPdf(MultipartFile file) {

        try (PDDocument document = PDDocument.load(file.getInputStream())) {

            Map<String, SectionArea> sections = pdfSectionExtractor.extract(document);

            int examsProcessed = 0;

            for (Map.Entry<String, SectionArea> entry : sections.entrySet()) {

                String sectionKey = entry.getKey();
                SectionArea sectionArea = entry.getValue();

                Worker worker = workerExamBusiness.processWorker(document, sectionArea);

                if (worker == null) {
                    throw new RuntimeException("No se pudo extraer datos válidos del trabajador");
                }

                worker = workerService.findOrCreate(worker);

                if ("OCCUPATIONAL_MEDICINE_LABEL".equals(sectionKey)) {
                    if (occupationalMedicineTextBusiness.process(document, sectionArea, worker)) {
                        examsProcessed++;
                    }
                }

                // if ("AUDIOMETRY_LABEL".equals(sectionKey)) {
                //     if (audiometryTextBusiness.process(document, sectionArea, worker, sectionKey)) {
                //         examsProcessed++;
                //     }
                // }

                // if ("OPTOMETRY_LABEL".equals(sectionKey)) {
                //     if (optometryTextBusiness.process(document, sectionArea, worker, sectionKey)) {
                //         examsProcessed++;
                //     }
                // }
                
//                if ("SPIROMETRY_LABEL".equals(entry.getKey())) {
//                    if (spirometryBusiness.process(document, entry.getValue(), worker, entry.getKey())) {
//                        examsProcessed++;
//                    }
//                }

                // if ("LABORATORY_LABEL".equals(sectionKey)) {
                //     if (laboratoryResultImgBusiness.process(document, sectionArea, sectionKey, worker)) {
                //         examsProcessed++;
                //     }
                // }

                // if ("LABORATORY_LABEL_TEXT".equals(sectionKey)) {
                //     if (laboratoryResultTextBusiness.process(document, sectionArea, sectionKey, worker)) {
                //         examsProcessed++;
                //     }
                // }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("message", "PDF procesado correctamente");
            response.put("examsProcessed", examsProcessed);
            response.put("totalPages", document.getNumberOfPages());

            return response;

        } catch (Exception e) {
            throw new RuntimeException("Error procesando PDF", e);
        }
    }
}