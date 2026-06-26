package com.example.PruebaPDFBox.utilities.extractor.SectionExtractor;

import com.example.PruebaPDFBox.record.SectionArea;
import com.example.PruebaPDFBox.utilities.extractor.TextExtractor.ImageTextExtractor;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.stereotype.Component;

import java.awt.image.BufferedImage;
import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class PdfOcrSectionDetector {

    private final PdfImgSectionExtractor pdfImgSectionExtractor;
    private final ImageTextExtractor imageTextExtractor;

    public static final Map<String, Set<String>> IMAGE_SECTION_TITLES = Map.of(
            "GLYCEMYA_IMAGE_LABEL", new HashSet<>(List.of(
                    "HEMATOLOGIA GLICEMIA ANALITO "
            ))
            ,
            "HEMOGRAM_IMAGE_LABEL", new HashSet<>(List.of(
                    "CUADRO HEMATICO (HEMOGRAM A)",
                    "CUADRO HEMATICO (HEMOGRAMA)"
            ))
            ,
            "LIPID_PROFILE_IMAGE_LABEL", new HashSet<>(List.of(
                    "HEMATOLOGIA PERFIL LIPIDICO"
            ))
    );

    public static final Set<String> ALL_IMAGE_TITLES = IMAGE_SECTION_TITLES.values()
            .stream()
            .flatMap(Set::stream)
            .map(String::toUpperCase)
            .collect(Collectors.toSet());

    //Busca secciones en imágenes y las retorna como un Map<String, SectionArea>
    public Map<String, SectionArea> detectSectionsInImages(PDDocument document) {
        Map<String, SectionArea> imageSections = new HashMap<>();

        // CORRECCIÓN: Usar IMAGE_SECTION_TITLES.entrySet()
        for (Map.Entry<String, Set<String>> entry : IMAGE_SECTION_TITLES.entrySet()) {
            String sectionKey = entry.getKey();  // "LABORATORY_IMAGE_LABEL"
            Set<String> possibleTitles = entry.getValue();

            SectionArea foundSection = findSectionByImageText(document, possibleTitles);

            if (foundSection != null) {
                imageSections.put(sectionKey, foundSection);
                System.out.println("Sección detectada en imagen: " + sectionKey +
                        " en página " + (foundSection.startPage() + 1) +
                        " (título: '" + foundSection.examName() + "')");
            }
        }

        return imageSections;
    }

    private SectionArea findSectionByImageText(PDDocument document, Set<String> possibleTitles) {
        System.out.println("Buscando títulos: " + possibleTitles);

        for (int pageNum = 0; pageNum < document.getNumberOfPages(); pageNum++) {

            List<BufferedImage> images = pdfImgSectionExtractor.extractImages(
                    document,
                    new SectionArea(pageNum, 0, pageNum, Float.MAX_VALUE, "")
            );

            System.out.println("   Página " + (pageNum + 1) + ": " + images.size() + " imágenes");

            for (BufferedImage image : images) {
                String ocrText = imageTextExtractor.extractTextFromSingleImage(image);

                if (!ocrText.isEmpty()) {
                    String preview = ocrText.length() > 90 ? ocrText.substring(0, 90) + "..." : ocrText;
                    System.out.println("   OCR en página " + (pageNum + 1) + ": " + preview);

                    // Buscar coincidencia exacta (sin normalizar)
                    for (String title : possibleTitles) {
                        if (ocrText.contains(title)) {
                            System.out.println("   ✅ ENCONTRADO: '" + title + "'");

                            // Encontrar la página final buscando el siguiente título
                            int endPage = findNextSectionPage(document, pageNum);

                            return new SectionArea(
                                    pageNum,
                                    0,
                                    endPage,
                                    Float.MAX_VALUE,
                                    title
                            );
                        }
                    }
                }
            }
        }
        return null;
    }

    private int findNextSectionPage(PDDocument document, int currentPage) {
        int totalPages = document.getNumberOfPages();

        // Buscar desde la página siguiente hasta el final
        for (int page = currentPage + 1; page < totalPages; page++) {

            List<BufferedImage> images = pdfImgSectionExtractor.extractImages(
                    document,
                    new SectionArea(page, 0, page, Float.MAX_VALUE, "")
            );

            // Revisar cada imagen en la página
            for (BufferedImage image : images) {
                String ocrText = imageTextExtractor.extractTextFromSingleImage(image);

                if (!ocrText.isEmpty()) {
                    // Verificar si esta imagen contiene algún título de sección conocido
                    for (Set<String> titles : PdfOcrSectionDetector.IMAGE_SECTION_TITLES.values()) {
                        for (String title : titles) {
                            if (ocrText.contains(title)) {
                                System.out.println("   📌 Siguiente sección encontrada en página " +
                                        (page + 1) + ": '" + title + "'");
                                // La sección actual termina en la página anterior
                                return page - 1;
                            }
                        }
                    }
                }
            }
        }

        // Si no hay más secciones, termina en la última página
        System.out.println("   📌 No hay más secciones, termina en última página");
        return totalPages - 1;
    }
//
//    public static String normalize(String text) {
//        if (text == null) return "";
//        return text.toUpperCase().trim();
//    }
//
//    public static String findSectionKeyByTitle(String title) {
//        if (title == null) return null;
//
//        String normalizedTitle = normalize(title);
//
//        return IMAGE_SECTION_TITLES.entrySet().stream()
//                .filter(entry -> entry.getValue().stream()
//                        .anyMatch(t -> normalizedTitle.contains(normalize(t)) ||
//                                normalize(t).contains(normalizedTitle)))
//                .map(Map.Entry::getKey)
//                .findFirst()
//                .orElse(null);
//    }
}
