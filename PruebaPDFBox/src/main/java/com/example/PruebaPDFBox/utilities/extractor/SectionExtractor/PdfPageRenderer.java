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

    private static final float DPI = 300f; // Balance calidad/rendimiento para OCR

    /**
     * Renderiza todas las páginas del PDF como imágenes en memoria.
     * Las imágenes NO se guardan en disco.
     *
     * @return Lista de imágenes indexadas por número de página (0-based)
     */
    public List<BufferedImage> renderPages(PDDocument document) throws IOException {
        PDFRenderer renderer = new PDFRenderer(document);
        List<BufferedImage> pages = new ArrayList<>();

        for (int i = 0; i < document.getNumberOfPages(); i++) {
            BufferedImage image = renderer.renderImageWithDPI(i, DPI, ImageType.GRAY);
            pages.add(image);
        }

        return pages;
    }

    /**
     * Renderiza un rango específico de páginas (útil si el PDF es muy grande).
     */
    public List<BufferedImage> renderPages(PDDocument document, int startPage, int endPage) throws IOException {
        PDFRenderer renderer = new PDFRenderer(document);
        List<BufferedImage> pages = new ArrayList<>();

        int end = Math.min(endPage, document.getNumberOfPages() - 1);
        for (int i = startPage; i <= end; i++) {
            pages.add(renderer.renderImageWithDPI(i, DPI, ImageType.GRAY));
        }

        return pages;
    }
}