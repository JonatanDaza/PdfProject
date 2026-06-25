package com.example.PruebaPDFBox.utilities.extractor.SectionExtractor;

import com.example.PruebaPDFBox.record.SectionArea;
import com.example.PruebaPDFBox.utilities.extractor.TextExtractor.ImageTextExtractor;

import lombok.RequiredArgsConstructor;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.stereotype.Component;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.*;

import com.example.PruebaPDFBox.record.OcrPageContext;

@Component
@RequiredArgsConstructor
public class UnifiedSectionDetector {

    private final PdfPageRenderer pdfPageRenderer;
    private final ImageTextExtractor imageTextExtractor;

    public static final Map<String, Set<String>> ALL_SECTION_TITLES = buildSectionTitles();

    private static Map<String, Set<String>> buildSectionTitles() {
        Map<String, Set<String>> titles = new LinkedHashMap<>();
        titles.put("OPTOMETRY_LABEL",            Set.of("EXAMEN DE OPTOMETRIA", "EXAMEN DE CONSULTA DE OPTOMETRIA", "EXAMEN DE VISIOMETRIA"));
        titles.put("OCCUPATIONAL_MEDICINE_LABEL", Set.of("EVALUACIÓN MEDICA OCUPACIONAL"));
        titles.put("AUDIOMETRY_LABEL",            Set.of("AUDIOMETRÍA"));
        titles.put("LABORATORY_LABEL_TEXT",       Set.of("INFORME DE RESULTADOS", "EXTENDIDO DE SANGRE PERIFERICA, ESTUDIO DE MORFOLOGIA"));
        titles.put("PSYCHOMOTOR_LABEL",           Set.of("Resumen de Resultados Test PSICO-MOTRÍZ"));
        titles.put("PSYCHOLOGY_LABEL",            Set.of("PSICOLOGÍA OCUPACIONAL"));
        titles.put("SPIROMETRY_LABEL",            Set.of("INFORME DE ESPIROMETRÍA"));
        titles.put("GLYCEMYA_IMAGE_LABEL",        Set.of("HEMATOLOGIA GLICEMIA ANALITO"));
        titles.put("HEMOGRAM_IMAGE_LABEL",        Set.of("CUADRO HEMATICO (HEMOGRAMA)"));
        titles.put("LIPID_PROFILE_IMAGE_LABEL",   Set.of("HEMATOLOGIA PERFIL LIPIDICO"));
        return Collections.unmodifiableMap(titles);
    }

    /** Entry point cuando se tiene el PDDocument (renderiza + OCR internamente) */
    public Map<String, SectionArea> detect(PDDocument document) throws IOException {
        List<BufferedImage> pages = pdfPageRenderer.renderPages(document);
        String[] pageTexts = extractAllPageTexts(pages);          // BufferedImage → OCR
        return detectSectionsFromTexts(pageTexts, document.getNumberOfPages());
    }

    /** Entry point cuando el OCR ya fue hecho fuera (OcrPageContext ya tiene los textos) */
    public Map<String, SectionArea> detectFromContext(OcrPageContext context) {
        String[] pageTexts = context.pageTexts().toArray(new String[0]);  // String[] directo
        return detectSectionsFromTexts(pageTexts, context.totalPages());
    }

    // ── Método privado unificado ────────────────────────────────────────────

    private Map<String, SectionArea> detectSectionsFromTexts(String[] pageTexts, int totalPages) {
        Map<String, SectionArea> sections = new LinkedHashMap<>();

        for (Map.Entry<String, Set<String>> entry : ALL_SECTION_TITLES.entrySet()) {
            String sectionKey    = entry.getKey();
            Set<String> candidates = entry.getValue();

            SectionArea area = findSection(candidates, pageTexts, totalPages);
            if (area != null) {
                sections.put(sectionKey, area);
                System.out.println("Sección detectada: " + sectionKey +
                        " (páginas " + (area.startPage() + 1) + "-" + (area.endPage() + 1) +
                        ", título: '" + area.examName() + "')");
            }
        }

        return sections;
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private String[] extractAllPageTexts(List<BufferedImage> pages) {
        String[] texts = new String[pages.size()];
        for (int i = 0; i < pages.size(); i++) {
            String text = imageTextExtractor.extractTextFromSingleImage(pages.get(i));
            texts[i] = text != null ? text.toUpperCase() : "";

            System.out.println("\n========== OCR COMPLETO PÁGINA " + (i + 1) + " ==========");
            System.out.println(texts[i]);
            System.out.println("========== FIN PÁGINA " + (i + 1) + " ==========\n");
        }
        return texts;
    }

    private SectionArea findSection(Set<String> candidates, String[] pageTexts, int totalPages) {
        for (int page = 0; page < pageTexts.length; page++) {
            for (String candidate : candidates) {
                String pageText = pageTexts[page];
                String candidateUpper = candidate.toUpperCase();

                // 1. Búsqueda exacta
                if (pageText.contains(candidateUpper)) {
                    return buildSection(page, candidate, pageTexts, totalPages);
                }

                // 2. Búsqueda normalizada (sin tildes)
                if (normalize(pageText).contains(normalize(candidateUpper))) {
                    System.out.println("  ✅ (normalizado) '" + candidate + "' en página " + (page + 1));
                    return buildSection(page, candidate, pageTexts, totalPages);
                }

                // 3. Búsqueda por todas las palabras significativas (≥4 chars)
                String[] words = candidateUpper.split("\\s+");
                long significantWords = Arrays.stream(words)
                        .filter(w -> w.length() >= 4)
                        .count();
                long matchedWords = Arrays.stream(words)
                        .filter(w -> w.length() >= 4 && pageText.contains(w))
                        .count();

                if (significantWords > 0 && matchedWords == significantWords) {
                    System.out.println("  ✅ (por palabras) '" + candidate + "' en página " + (page + 1));
                    return buildSection(page, candidate, pageTexts, totalPages);
                }
            }
        }
        return null;
    }

    private SectionArea buildSection(int page, String candidate, String[] pageTexts, int totalPages) {
        int endPage = findNextSectionPage(page, pageTexts, totalPages);
        System.out.println("  ✅ '" + candidate + "' encontrado en página " + (page + 1));
        return new SectionArea(page, 0, endPage, Float.MAX_VALUE, candidate);
    }

    private String normalize(String text) {
        if (text == null) return "";
        return text
            .replace("Á","A").replace("É","E").replace("Í","I")
            .replace("Ó","O").replace("Ú","U").replace("Ü","U")
            .replace("Ñ","N")
            .replaceAll("[^A-Z0-9\\s]", " ")
            .replaceAll("\\s+", " ")
            .trim();
    }

    private int findNextSectionPage(int currentPage, String[] pageTexts, int totalPages) {
        Set<String> allTitlesUpper = new HashSet<>();
        for (Set<String> titles : ALL_SECTION_TITLES.values()) {
            for (String t : titles) {
                allTitlesUpper.add(t.toUpperCase());
            }
        }

        for (int page = currentPage + 1; page < pageTexts.length; page++) {
            for (String title : allTitlesUpper) {
                if (pageTexts[page].contains(title)) {
                    return page - 1;
                }
            }
        }

        return totalPages - 1;
    }
}