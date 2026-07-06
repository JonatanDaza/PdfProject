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
            Tesseract tesseract = getTesseract();
            StringBuilder fullText = new StringBuilder();

            // Título — octavo superior con PSM 11
            tesseract.setPageSegMode(11);
            String titleText = tesseract.doOCR(image,
                    new java.awt.Rectangle(0, 0, image.getWidth(), image.getHeight() / 8));
            if (titleText != null && !titleText.isBlank()) {
                fullText.append(titleText.trim()).append("\n");
            }

            // Cuerpo en franjas con PSM 4
            List<BufferedImage> strips = auxiliar.splitIntoStrips(image, 5, 30);
            for (BufferedImage strip : strips) {
                tesseract.setPageSegMode(4);
                String stripText = tesseract.doOCR(strip);
                if (stripText != null && !stripText.isBlank()) {
                    fullText.append(stripText.trim()).append("\n");
                }
            }

            return cleanOcrNoise(fullText.toString());

        } catch (Exception e) {
            System.err.println("Error en OCR: " + e.getMessage());
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
                if (image == null) continue;
                Map<String, String> data = tryExtractWithMultiplePSM(image, labels);
                extractedData.putAll(data);
            }
        }

        return extractedData;
    }

    private Map<String, String> tryExtractWithMultiplePSM(BufferedImage image, List<String> labels) {
        Tesseract tesseract = getTesseract();
        tesseract.setPageSegMode(6);
        Map<String, String> data = extractUsingTextAnalysis(image, labels, tesseract);
        return data.isEmpty() ? new HashMap<>() : data;
    }

    private Map<String, String> extractUsingTextAnalysis(
            BufferedImage image,
            List<String> labels,
            Tesseract tesseract
    ) {
        try {
            String text = tesseract.doOCR(image);
            String normalized = text.replace("\r", "")
                    .replaceAll("\\n{2,}", "\n")
                    .trim();

            List<String> lines = Arrays.stream(normalized.split("\n"))
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .toList();

            auxiliar.logExtractedText(String.join("\n", lines));

            return auxiliar.extractDataFromText(labels, lines.toArray(new String[0]));
        } catch (Exception e) {
            throw new CustomException("Error al analizar el texto: " + e.getMessage(),
                    HttpStatus.BAD_REQUEST);
        }
    }

    private String cleanOcrNoise(String text) {
        return text
                .replaceAll("(?m)^\\s*[oO0]\\s*$\\n?", "")
                .replaceAll("[ \\t]{2,}", " ")
                .replaceAll("\\r\\n", "\n")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }
}