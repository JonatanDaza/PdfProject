package com.example.PruebaPDFBox.utilities.extractor.TextExtractor;

import com.example.PruebaPDFBox.record.SectionArea;
import com.example.PruebaPDFBox.utilities.Infrastructure.exception.CustomException;
import com.example.PruebaPDFBox.utilities.extractor.Auxiliaries.ImageTextExtractorAuxiliary;
import com.example.PruebaPDFBox.utilities.extractor.SectionExtractor.PdfImgSectionExtractor;
import net.sourceforge.tess4j.Tesseract;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.awt.image.BufferedImage;
import java.util.*;

@Component
public class ImageTextExtractor {

    private static final int MIN_IMAGE_WIDTH = 1500;
    public static final int MIN_VALID_IMAGE_WIDTH = 700;
    public static final int MIN_VALID_IMAGE_HEIGHT = 300;

    private static final int[] PSM_MODES = {6};

    private final ThreadLocal<Tesseract> tesseractThreadLocal;
    private final PdfImgSectionExtractor pdfImgSectionExtractor;
    private final ImageTextExtractorAuxiliary auxiliar;

    public ImageTextExtractor(
            ObjectProvider<Tesseract> tesseractProvider,
            PdfImgSectionExtractor pdfImgSectionExtractor
    ) {
        this.tesseractThreadLocal = ThreadLocal.withInitial(tesseractProvider::getObject);
        this.pdfImgSectionExtractor = pdfImgSectionExtractor;
        this.auxiliar = new ImageTextExtractorAuxiliary();
    }

    private Tesseract getTesseract() {
        return tesseractThreadLocal.get();
    }

    public String extractTextFromSingleImage(BufferedImage image) {
        try {
            if (!auxiliar.isValidTableImage(image)) {
                return "";
            }

            BufferedImage optimizedImage = auxiliar.optimizeImageForOCR(image, MIN_IMAGE_WIDTH);
            Tesseract tesseract = getTesseract();
            auxiliar.configureTesseract(tesseract);

            tesseract.setPageSegMode(6);
            String text = tesseract.doOCR(optimizedImage);

            // Preserva saltos de línea — solo normaliza múltiples líneas vacías
            return text.replaceAll("\\r\\n", "\n")
                    .replaceAll("\\n{3,}", "\n\n")
                    .trim();

//            if (!auxiliar.isValidTableImage(image)) {
//                return "";
//            }
//
//            BufferedImage optimizedImage = auxiliar.optimizeImageForOCR(image, MIN_IMAGE_WIDTH);
//            Tesseract tesseract = getTesseract();
//            auxiliar.configureTesseract(tesseract);
//
//            // Probar con diferentes PSM modes para mejor detección de títulos
//            int[] titlePsmModes = {6, 3, 1}; // PSM 6: bloque uniforme, 3: línea, 1: texto automático
//
//            for (int psm : titlePsmModes) {
//                tesseract.setPageSegMode(psm);
//                String text = tesseract.doOCR(optimizedImage);
//
//                if (!auxiliar.isBlank(text)) {
//                    // Limpiar el texto para mejor coincidencia
//                    text = text.replaceAll("\\r?\\n+", " ").trim();
//                    return text;
//                }
//            }
//
//            return "";
        } catch (Exception e) {
            System.err.println("Error en OCR simple: " + e.getMessage());
            return "";
        }
    }

    public Map<String, String> extractTextFromTable(
            PDDocument document,
            List<Integer> targetPages,
            List<String> labels
    ) {
        Map<String, String> extractedData = new HashMap<>();

        for (int pageNum : targetPages) {
            if (!auxiliar.isValidPage(document, pageNum)) continue;

            List<BufferedImage> images = pdfImgSectionExtractor.extractImages(
                    document, new SectionArea(pageNum, 0, pageNum, Float.MAX_VALUE, "")
            );

            for (BufferedImage image : images) {
                if (!auxiliar.isValidTableImage(image)) continue;

                BufferedImage optimizedImage = auxiliar.optimizeImageForOCR(image, MIN_IMAGE_WIDTH);
                Map<String, String> data = tryExtractWithMultiplePSM(optimizedImage, labels);
                extractedData.putAll(data);
            }
        }

        return extractedData;
    }

    private Map<String, String> tryExtractWithMultiplePSM(
            BufferedImage image,
            List<String> labels
    ) {
        Tesseract tesseract = getTesseract();
        auxiliar.configureTesseract(tesseract);

        for (int psm : PSM_MODES) {
            tesseract.setPageSegMode(psm);
            auxiliar.logAttempt(psm);

            Map<String, String> data = extractUsingTextAnalysis(image, labels, tesseract);

            if (!data.isEmpty()) {
                auxiliar.logSuccess(psm, data.size());
                return data;
            }
        }

        return new HashMap<>();
    }

    private Map<String, String> extractUsingTextAnalysis(
            BufferedImage image,
            List<String> labels,
            Tesseract tesseract
    ) {
        try {
            String text = tesseract.doOCR(image);

            String singleLineText = text.replaceAll("\\r?\\n+", " ").replaceAll("\\s+", " ").trim();

            if (auxiliar.isBlank(singleLineText)) return new HashMap<>();

            auxiliar.logExtractedText(singleLineText);
            return auxiliar.extractDataFromText(labels, new String[]{singleLineText});

        } catch (Exception e) {
            throw new CustomException("Error al analizar el texto: " + e.getMessage(),
                    HttpStatus.BAD_REQUEST);
        }
    }
}