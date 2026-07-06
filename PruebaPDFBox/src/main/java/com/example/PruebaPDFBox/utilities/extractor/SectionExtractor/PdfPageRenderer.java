package com.example.PruebaPDFBox.utilities.extractor.SectionExtractor;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Component;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class PdfPageRenderer {

    private static final float DPI = 300f;

    public List<BufferedImage> renderPages(PDDocument document) throws IOException {
        PDFRenderer renderer = new PDFRenderer(document);
        List<BufferedImage> pages = new ArrayList<>();

        int total = document.getNumberOfPages();
        for (int i = 0; i < total; i++) {
            try {
                pages.add(renderer.renderImageWithDPI(i, DPI, ImageType.RGB));
            } catch (Exception e) {
                System.err.println("Error rendering page " + (i + 1) + ": " + e.getMessage());
                // Add a tiny placeholder image so page indexes remain aligned with the document
                pages.add(new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB));
            }
        }

        return pages;
    }
}