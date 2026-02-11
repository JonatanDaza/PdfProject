package com.example.PruebaPDFBox.utilities.extractor.SectionExtractor;

import com.example.PruebaPDFBox.record.SectionArea;
import com.example.PruebaPDFBox.utilities.extractor.Auxiliaries.PdfImgSectionExtractorAuxiliary;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.springframework.stereotype.Component;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

@Component
public class PdfImgSectionExtractor {

    private final PdfImgSectionExtractorAuxiliary auxiliar = new PdfImgSectionExtractorAuxiliary();

    public List<BufferedImage> extractImages(PDDocument document, SectionArea sectionArea) {
        List<BufferedImage> images = new ArrayList<>();

        for (int pageNum = sectionArea.startPage(); pageNum <= sectionArea.endPage(); pageNum++) {
            PDPage page = document.getPage(pageNum);
            List<BufferedImage> embeddedImages = auxiliar.extractEmbeddedImages(page, pageNum);

            if (!embeddedImages.isEmpty()) {
                images.addAll(embeddedImages);
            }
        }

        return images;
    }
}
