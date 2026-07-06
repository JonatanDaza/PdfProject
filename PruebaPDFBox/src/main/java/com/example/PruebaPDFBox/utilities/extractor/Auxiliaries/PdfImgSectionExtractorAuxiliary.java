package com.example.PruebaPDFBox.utilities.extractor.Auxiliaries;

import com.example.PruebaPDFBox.utilities.Infrastructure.exception.CustomException;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.http.HttpStatus;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.*;
import java.util.List;

public class PdfImgSectionExtractorAuxiliary {

    public List<BufferedImage> extractEmbeddedImages(PDPage page, int pageNum) {
        try {
            if (page.getResources() == null) return Collections.emptyList();
            return extractFromResources(page.getResources());
        } catch (Exception e) {
            throw new CustomException(
                    "Error al extraer imágenes incrustadas de la página " + pageNum,
                    HttpStatus.BAD_REQUEST
            );
        }
    }

    private List<BufferedImage> extractFromResources(PDResources resources) throws IOException {
        List<BufferedImage> images = new ArrayList<>();
        for (COSName name : resources.getXObjectNames()) {
            images.addAll(extractFromXObject(resources.getXObject(name)));
        }
        return images;
    }

    private List<BufferedImage> extractFromXObject(PDXObject xObject) throws IOException {
        if (xObject instanceof PDImageXObject img) {
            BufferedImage buffered = img.getImage();
            if (buffered == null) return Collections.emptyList();
            return List.of(enhanceImageForOCR(buffered));
        }
        if (xObject instanceof PDFormXObject form) {
            if (form.getResources() == null) return Collections.emptyList();
            return extractFromResources(form.getResources());
        }
        return Collections.emptyList();
    }

    private BufferedImage enhanceImageForOCR(BufferedImage original) {
        int w = original.getWidth(), h = original.getHeight();
        BufferedImage grayscale = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g = grayscale.createGraphics();
        g.drawImage(original, 0, 0, null);
        g.dispose();

        BufferedImage enhanced = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int gray = (grayscale.getRGB(x, y) >> 16) & 0xff;
                gray = Math.max(0, Math.min(255, (int) ((gray - 128) * 1.2 + 128)));
                int rgb = (gray << 16) | (gray << 8) | gray;
                enhanced.setRGB(x, y, rgb);
            }
        }
        return enhanced;
    }
}