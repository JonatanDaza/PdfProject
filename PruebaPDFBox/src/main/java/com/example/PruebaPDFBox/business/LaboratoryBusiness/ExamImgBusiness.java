package com.example.PruebaPDFBox.business.LaboratoryBusiness;

import com.example.PruebaPDFBox.business.LaboratoryBusiness.LaboratoryImgBusiness.GlycemiaImgBusiness;
import com.example.PruebaPDFBox.business.LaboratoryBusiness.LaboratoryImgBusiness.HemogramImgBusiness;
import com.example.PruebaPDFBox.business.LaboratoryBusiness.LaboratoryImgBusiness.LipidProfileImgBusiness;
import com.example.PruebaPDFBox.model.Worker;
import com.example.PruebaPDFBox.record.SectionArea;
import com.example.PruebaPDFBox.utilities.extractor.SectionExtractor.PdfOcrSectionDetector;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ExamImgBusiness {

    private final GlycemiaImgBusiness glycemiaImgBusiness;
    private final HemogramImgBusiness hemogramImgBusiness;
    private final LipidProfileImgBusiness lipidProfileImgBusiness;

    public boolean processExam(
            PDDocument document,
            String imageSectionKey,
            SectionArea sectionArea,
            Worker worker
    ) throws Exception {

        System.out.println("Procesando examen de imagen: " + imageSectionKey);
        System.out.println("   Páginas: " + (sectionArea.startPage() + 1) +
                " - " + (sectionArea.endPage() + 1));
        System.out.println("   Título encontrado: " + sectionArea.examName());


        boolean processed = false;

        if (PdfOcrSectionDetector.IMAGE_SECTION_TITLES.containsKey(imageSectionKey)) {

            switch (imageSectionKey) {
                case "HEMOGRAM_IMAGE_LABEL":
                    processed = hemogramImgBusiness.process(
                            document,
                            sectionArea,
                            worker,
                            sectionArea.examName()
                    );
                    break;

                case "GLYCEMYA_IMAGE_LABEL":
                    processed = glycemiaImgBusiness.process(
                            document,
                            sectionArea,
                            worker,
                            sectionArea.examName()
                    );
                    break;

                case "LIPID_PROFILE_IMAGE_LABEL":
                    processed = lipidProfileImgBusiness.process(
                            document,
                            sectionArea,
                            worker,
                            sectionArea.examName()
                    );
                    break;

                default:
                    System.out.println("No hay business implementado para: " + imageSectionKey);
                    return false;
            }
        } else {
            System.out.println("ImageSectionKey no reconocido: " + imageSectionKey);
            return false;
        }

        if (processed) {
            System.out.println("Examen de imagen procesado exitosamente: " + imageSectionKey);
        } else {
            System.out.println("Falló el procesamiento del examen: " + imageSectionKey);
        }

        return processed;
    }
}