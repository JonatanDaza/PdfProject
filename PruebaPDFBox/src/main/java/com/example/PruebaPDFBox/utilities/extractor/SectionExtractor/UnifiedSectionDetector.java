package com.example.PruebaPDFBox.utilities.extractor.SectionExtractor;

import java.awt.image.BufferedImage;

import com.example.PruebaPDFBox.record.SectionArea;
import com.example.PruebaPDFBox.utilities.extractor.TextExtractor.ImageTextExtractor;

import lombok.RequiredArgsConstructor;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.stereotype.Component;

import java.util.*;
import java.text.Normalizer;

import com.example.PruebaPDFBox.record.OcrPageContext;

@Component
@RequiredArgsConstructor
public class UnifiedSectionDetector {

    public static final Map<String, Set<String>> ALL_SECTION_TITLES = buildSectionTitles();

    private final ImageTextExtractor imageTextExtractor;
    private final PdfImgSectionExtractor pdfImgSectionExtractor;

    private static final Map<String, Set<String>> SECTION_ANCHORS = Map.of(
            "OCCUPATIONAL_MEDICINE_LABEL", Set.of("MEDICO", "MÉDICO"),
            "AUDIOMETRY_LABEL",            Set.of("FONOAUDIOLOGO", "FONOAUDIÓLOGO"),
            "OPTOMETRY_LABEL",             Set.of("OPTOMETRA", "OPTOMETRISTA")
    );

    private static final Map<String, String> SECTION_CANONICAL_NAMES = Map.of(
            "OPTOMETRY_LABEL",            "OPTOMETRÍA",
            "OCCUPATIONAL_MEDICINE_LABEL","EVALUACIÓN MÉDICA OCUPACIONAL",
            "AUDIOMETRY_LABEL",           "AUDIOMETRÍA",
            "LABORATORY_LABEL_TEXT",      "LABORATORIO",
            "PSYCHOMOTOR_LABEL",          "PSICOMOTRICIDAD",
            "PSYCHOLOGY_LABEL",           "PSICOLOGÍA OCUPACIONAL",
            "SPIROMETRY_LABEL",           "ESPIROMETRÍA",
            "GLYCEMYA_IMAGE_LABEL",       "GLICEMIA",
            "HEMOGRAM_IMAGE_LABEL",       "HEMOGRAMA",
            "LIPID_PROFILE_IMAGE_LABEL",  "PERFIL LIPÍDICO"
    );

    private static Map<String, Set<String>> buildSectionTitles() {
        Map<String, Set<String>> titles = new LinkedHashMap<>();
        titles.put("OPTOMETRY_LABEL",            Set.of("EXAMEN DE OPTOMETRIA", "EXAMEN DE CONSULTA DE OPTOMETRIA", "EXAMEN DE VISIOMETRIA"));
        titles.put("OCCUPATIONAL_MEDICINE_LABEL", Set.of("EVALUACION MEDICA OCUPACIONAL", "EVALUACIÓN MEDICA OCUPACIONAL"));
        titles.put("AUDIOMETRY_LABEL",            Set.of("AUDIOMETRIA", "AUDIOMETRÍA"));
        titles.put("LABORATORY_LABEL_TEXT",       Set.of("INFORME DE RESULTADOS", "EXTENDIDO DE SANGRE PERIFERICA, ESTUDIO DE MORFOLOGIA"));
        titles.put("PSYCHOMOTOR_LABEL",           Set.of("RESUMEN DE RESULTADOS TEST PSICO-MOTRIZ"));
        titles.put("PSYCHOLOGY_LABEL",            Set.of("PSICOLOGIA OCUPACIONAL", "PSICOLOGÍA OCUPACIONAL"));
        titles.put("SPIROMETRY_LABEL",            Set.of("INFORME DE ESPIROMETRIA", "INFORME DE ESPIROMETRÍA"));
        titles.put("GLYCEMYA_IMAGE_LABEL",        Set.of("HEMATOLOGIA GLICEMIA ANALITO"));
        titles.put("HEMOGRAM_IMAGE_LABEL",        Set.of("CUADRO HEMATICO (HEMOGRAMA)"));
        titles.put("LIPID_PROFILE_IMAGE_LABEL",   Set.of("HEMATOLOGIA PERFIL LIPIDICO"));
        return Collections.unmodifiableMap(titles);
    }

    public Map<String, SectionArea> detectFromContext(OcrPageContext context, PDDocument document) {
        String[] pageTexts = context.pageTexts().toArray(new String[0]);
        String[] enriched = enrichWithEmbeddedImages(pageTexts, document);
        return detectSectionsFromTexts(enriched, context.totalPages());
    }

    public Map<String, SectionArea> detectFromContext(OcrPageContext context) {
        String[] pageTexts = context.pageTexts().toArray(new String[0]);
        return detectSectionsFromTexts(pageTexts, context.totalPages());
    }

    private String[] enrichWithEmbeddedImages(String[] pageTexts, PDDocument document) {
        String[] enriched = Arrays.copyOf(pageTexts, pageTexts.length);
        for (int i = 0; i < document.getNumberOfPages(); i++) {
            List<BufferedImage> images = pdfImgSectionExtractor.extractImages(
                    document, new SectionArea(i, 0, i, Float.MAX_VALUE, ""));
            for (BufferedImage img : images) {
                String imgText = imageTextExtractor.extractTextFromSingleImage(img);
                if (imgText != null && !imgText.isBlank()) {
                    enriched[i] = enriched[i] + "\n" + imgText.toUpperCase();
                }
            }
        }
        return enriched;
    }

    private Map<String, SectionArea> detectSectionsFromTexts(String[] pageTexts, int totalPages) {
        Map<String, SectionArea> sections = new LinkedHashMap<>();

        int currentPage = 0;
        while (currentPage < pageTexts.length) {
            boolean matched = false;
            for (Map.Entry<String, Set<String>> entry : ALL_SECTION_TITLES.entrySet()) {
                SectionArea area = findSection(entry.getKey(), entry.getValue(), pageTexts, totalPages, currentPage);
                if (area != null && !sections.containsKey(entry.getKey())) {
                    sections.put(entry.getKey(), area);
                    System.out.println("Sección detectada: " + entry.getKey() +
                            " (páginas " + (area.startPage() + 1) + "-" + (area.endPage() + 1) +
                            ", título: '" + area.examName() + "')");
                    currentPage = area.endPage() + 1;
                    matched = true;
                    break;
                }
            }
            if (!matched) currentPage++;
        }

        return sections;
    }

    private SectionArea findSection(String sectionKey, Set<String> candidates, String[] pageTexts, int totalPages, int startPage) {
        for (int page = startPage; page < pageTexts.length; page++) {
            String normalizedPage = normalize(pageTexts[page]);
            for (String candidate : candidates) {
                String normalizedCandidate = normalize(candidate);
                if (normalizedPage.contains(normalizedCandidate) || fuzzyMatch(normalizedPage, normalizedCandidate)) {
                    int end = findSectionEnd(sectionKey, pageTexts, totalPages, page);
                    System.out.println("  ✅ '" + candidate + "' en página " + (page + 1));
                    return new SectionArea(page, 0, end, Float.MAX_VALUE, SECTION_CANONICAL_NAMES.getOrDefault(sectionKey, candidate));
                }
            }
        }
        return null;
    }

    private int findSectionEnd(String sectionKey, String[] pageTexts, int totalPages, int currentPage) {
        Set<String> anchors = SECTION_ANCHORS.getOrDefault(sectionKey, Collections.emptySet());
        if (!anchors.isEmpty()) {
            for (int page = currentPage + 1; page < pageTexts.length; page++) {
                String norm = normalize(pageTexts[page]);
                if (anchors.stream().anyMatch(a -> norm.contains(normalize(a)))) {
                    return page;
                }
            }
            return totalPages - 1;
        }

        Set<String> allNormalized = ALL_SECTION_TITLES.values().stream()
                .flatMap(Set::stream)
                .map(this::normalize)
                .collect(java.util.stream.Collectors.toSet());

        for (int page = currentPage + 1; page < pageTexts.length; page++) {
            String norm = normalize(pageTexts[page]);
            if (allNormalized.stream().anyMatch(norm::contains)) return page - 1;
        }
        return totalPages - 1;
    }

    private String normalize(String text) {
        if (text == null) return "";
        return Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toUpperCase()
                .replaceAll("[^A-Z0-9\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private boolean fuzzyMatch(String text, String pattern) {
        if (text == null || pattern == null || pattern.isBlank()) return false;
        if (text.contains(pattern)) return true;
        int maxDistance = Math.max(1, pattern.length() / 4);
        int windowSize = pattern.length();
        for (int i = 0; i + windowSize <= text.length(); i++) {
            if (levenshteinDistance(text.substring(i, i + windowSize), pattern) <= maxDistance) return true;
        }
        return false;
    }

    private int levenshteinDistance(String a, String b) {
        int n = a.length(), m = b.length();
        if (n == 0) return m;
        if (m == 0) return n;
        int[] prev = new int[m + 1], curr = new int[m + 1];
        for (int j = 0; j <= m; j++) prev[j] = j;
        for (int i = 1; i <= n; i++) {
            curr[0] = i;
            for (int j = 1; j <= m; j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                curr[j] = Math.min(Math.min(curr[j-1] + 1, prev[j] + 1), prev[j-1] + cost);
            }
            System.arraycopy(curr, 0, prev, 0, m + 1);
        }
        return prev[m];
    }
}