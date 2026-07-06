package com.example.PruebaPDFBox.business.LaboratoryBusiness;

import com.example.PruebaPDFBox.business.LaboratoryBusiness.LaboratoryImgBusiness.GlycemiaImgBusiness;
import com.example.PruebaPDFBox.business.LaboratoryBusiness.LaboratoryImgBusiness.HemogramImgBusiness;
import com.example.PruebaPDFBox.business.LaboratoryBusiness.LaboratoryImgBusiness.LipidProfileImgBusiness;
import com.example.PruebaPDFBox.model.Worker;
import com.example.PruebaPDFBox.record.OcrPageContext;
import com.example.PruebaPDFBox.record.SectionArea;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ExamImgBusiness {

    private final GlycemiaImgBusiness glycemiaImgBusiness;
    private final HemogramImgBusiness hemogramImgBusiness;
    private final LipidProfileImgBusiness lipidProfileImgBusiness;

    private static final java.util.Set<String> IMAGE_SECTION_KEYS = java.util.Set.of(
            "HEMOGRAM_IMAGE_LABEL",
            "GLYCEMYA_IMAGE_LABEL",
            "LIPID_PROFILE_IMAGE_LABEL"
    );

    public boolean processExam(
            PDDocument document,
            String imageSectionKey,
            SectionArea sectionArea,
            Worker worker,
            OcrPageContext context
    ) throws Exception {
    if (!IMAGE_SECTION_KEYS.contains(imageSectionKey)) {
            System.out.println("ImageSectionKey no reconocido: " + imageSectionKey);
            return false;
    }
    boolean processed = switch (imageSectionKey) {
            case "HEMOGRAM_IMAGE_LABEL" ->
                    hemogramImgBusiness.process(document, sectionArea, worker, sectionArea.examName(), context);
            case "GLYCEMYA_IMAGE_LABEL" ->
                    glycemiaImgBusiness.process(document, sectionArea, worker, sectionArea.examName(), context);
            case "LIPID_PROFILE_IMAGE_LABEL" ->
                    lipidProfileImgBusiness.process(document, sectionArea, worker, sectionArea.examName(), context);
            default -> {
            System.out.println("No hay business implementado para: " + imageSectionKey);
            yield false;
            }
    };
    System.out.println(processed
            ? "Examen de imagen procesado exitosamente: " + imageSectionKey
            : "Falló el procesamiento del examen: " + imageSectionKey);
    return processed;
    }
}