package com.example.PruebaPDFBox.utilities.extractor;

import com.example.PruebaPDFBox.record.SectionArea;
import com.example.PruebaPDFBox.utilities.exception.CustomException;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class SectionImgExtractor {

/// version 2 funcional
    private static final int MIN_EMBEDDED_WIDTH = 700;
    private static final int MIN_EMBEDDED_HEIGHT = 300;

    public List<BufferedImage> extractImages(PDDocument document, SectionArea sectionArea) {
        List<BufferedImage> images = new ArrayList<>();

        for (int pageNum = sectionArea.startPage(); pageNum <= sectionArea.endPage(); pageNum++) {
            PDPage page = document.getPage(pageNum);
            List<BufferedImage> embeddedImages = extractEmbeddedImages(page, pageNum);

            if (!embeddedImages.isEmpty()) {
                images.addAll(embeddedImages);
            }
        }

        return images;
    }

    private List<BufferedImage> extractEmbeddedImages(PDPage page, int pageNum) {
        List<BufferedImage> images = new ArrayList<>();

        try {
            if (page.getResources() == null) return images;

            for (COSName name : page.getResources().getXObjectNames()) {
                PDXObject xObject = page.getResources().getXObject(name);

                if (xObject instanceof PDImageXObject) {
                    PDImageXObject image = (PDImageXObject) xObject;
                    BufferedImage bufferedImage = image.getImage();

                    if (isValidImageSize(bufferedImage)) {
                        // Aplicar preprocesamiento adicional
                        BufferedImage processedImage = enhanceImageForOCR(bufferedImage);
                        System.out.println("Imagen válida encontrada: " +
                                processedImage.getWidth() + "x" + processedImage.getHeight());
                        images.add(processedImage);
                    } else {
                        System.out.println("Imagen descartada por tamaño: " +
                                bufferedImage.getWidth() + "x" + bufferedImage.getHeight());
                    }

                } else if (xObject instanceof PDFormXObject) {
                    PDFormXObject form = (PDFormXObject) xObject;
                    if (form.getResources() != null) {
                        for (COSName formName : form.getResources().getXObjectNames()) {
                            PDXObject formXObject = form.getResources().getXObject(formName);
                            if (formXObject instanceof PDImageXObject) {
                                BufferedImage bufferedImage = ((PDImageXObject) formXObject).getImage();

                                if (isValidImageSize(bufferedImage)) {
                                    BufferedImage processedImage = enhanceImageForOCR(bufferedImage);
                                    images.add(processedImage);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new CustomException(
                    "Error al extraer imágenes incrustadas de la página " + pageNum,
                    HttpStatus.BAD_REQUEST
            );
        }

        return images;
    }

    /**
     * Mejora la imagen para OCR mediante binarización adaptativa y eliminación de ruido
     */
    private BufferedImage enhanceImageForOCR(BufferedImage original) {
        int width = original.getWidth();
        int height = original.getHeight();

        // 1. Convertir a escala de grises
        BufferedImage grayscale = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g = grayscale.createGraphics();
        g.drawImage(original, 0, 0, null);
        g.dispose();

        // 2. Aumentar contraste
        BufferedImage enhanced = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = grayscale.getRGB(x, y);
                int gray = (rgb >> 16) & 0xff;

                // Aumentar contraste
                gray = (int) ((gray - 128) * 1.5 + 128);
                gray = Math.max(0, Math.min(255, gray));

                int newRgb = (gray << 16) | (gray << 8) | gray;
                enhanced.setRGB(x, y, newRgb);
            }
        }

        return enhanced;
    }

    private boolean isValidImageSize(BufferedImage image) {
        return image != null &&
                image.getWidth() >= MIN_EMBEDDED_WIDTH &&
                image.getHeight() >= MIN_EMBEDDED_HEIGHT;
    }

    private BufferedImage renderPageAsImage(PDDocument document, int pageNum) {
        try {
            PDFRenderer renderer = new PDFRenderer(document);
            float dpi = 300f;
            BufferedImage image = renderer.renderImageWithDPI(pageNum, dpi);

            System.out.println("Página " + pageNum + " renderizada: " +
                    image.getWidth() + "x" + image.getHeight());

            return image;

        } catch (IOException e) {
            throw new CustomException(
                    "Error al renderizar página " + pageNum + " como imagen",
                    HttpStatus.BAD_REQUEST
            );
        }
    }
}

//// version 1
//    public List<BufferedImage> extractImages(PDDocument document, SectionArea sectionArea) {
//        List<BufferedImage> images = new ArrayList<>();
//
//        // Extraer imágenes de las páginas de la sección
//        for (int pageNum = sectionArea.startPage(); pageNum <= sectionArea.endPage(); pageNum++) {
//            PDPage page = document.getPage(pageNum);
//
//            for (COSName name : page.getResources().getXObjectNames()) {
//                try {
//                    PDXObject xObject = page.getResources().getXObject(name);
//                    if (xObject instanceof PDImageXObject image) {
//                        images.add(image.getImage());
//                    }
//                } catch (Exception e) {
//                    throw new CustomException("Error al extraer imágenes de la página " + pageNum, HttpStatus.BAD_REQUEST);
//                }
//            }
//        }
//
//        return images;
//    }
//}
