package com.example.PruebaPDFBox.utilities.extractor;

import com.example.PruebaPDFBox.record.PdfTextBlock;
import com.example.PruebaPDFBox.record.SectionArea;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;

@Component
public class PdfSectionExtractor {

    private static final Map<String, Set<String>> SECTION_TITLES = Map.of(
            "OPTOMETRY_LABEL", Set.of("EXAMEN DE OPTOMETRIA", "EXAMEN DE CONSULTA DE OPTOMETRIA"),
            "OCCUPATIONAL_MEDICINE_LABEL", Set.of("EVALUACIÓN MEDICA OCUPACIONAL"),
            "AUDIOMETRY_LABEL", Set.of("AUDIOMETRÍA"),
            "SPIROMETRY_LABEL", Set.of("ESPIROMETRÍA"),
            "LABORATORY_LABEL", Set.of("RESULTADOS DEL LABORATORIO", "Laboratorio", "Laboratorio N°: 69.011", "Entidad:"),
            "PSYCHOMOTOR_LABEL", Set.of("Resumen de Resultados Test PSICO-MOTRÍZ"),
            "PSYCHOLOGY_LABEL", Set.of("PSICOLOGÍA OCUPACIONAL")
    );

    private static final Set<String> ALL_TITLES = SECTION_TITLES.values()
            .stream()
            .flatMap(Set::stream)
            .map(String::toUpperCase)
            .collect(java.util.stream.Collectors.toSet());

    public Map<String, SectionArea> extract(PDDocument document) throws IOException {

        List<PdfTextBlock> blocks = extractTextBlocks(document);

        List<PdfTextBlock> titles = blocks.stream()
                .filter(b -> ALL_TITLES.contains(b.text().toUpperCase().trim()))
                .sorted(Comparator
                        .comparingInt(PdfTextBlock::page)
                        .thenComparing(PdfTextBlock::y).reversed()
                )
                .toList();

        Map<String, SectionArea> sections = new LinkedHashMap<>();

        for (int i = 0; i < titles.size(); i++) {
            PdfTextBlock current = titles.get(i);
            PdfTextBlock next = (i + 1 < titles.size()) ? titles.get(i + 1) : null;

            String normalizedTitle = normalize(current.text());
            String sectionKey = SECTION_TITLES.entrySet().stream()
                    .filter(e -> e.getValue().stream()
                            .anyMatch(v -> v.equalsIgnoreCase(current.text())))
                    .map(Map.Entry::getKey)
                    .findFirst()
                    .orElse(normalizedTitle);

            int endPage;
            float endY;

            if (next != null && next.page() == current.page()) {
                endPage = current.page();
                endY = next.y();
            } else if (next != null && next.page() > current.page()) {
                endPage = next.page();
                endY = Float.MAX_VALUE;
            } else {
                endPage = document.getNumberOfPages() - 1;
                endY = Float.MAX_VALUE;
            }

            SectionArea area = new SectionArea(current.page(), current.y(), endPage, endY);
            sections.put(sectionKey, area);
        }
        return sections;
    }


    //Extract all text blocks from the PDF document with their positions
    private List<PdfTextBlock> extractTextBlocks(PDDocument document) throws IOException {

        List<PdfTextBlock> blocks = new ArrayList<>();

        PDFTextStripper stripper = new PDFTextStripper() {
            int page = 0;

            @Override
            protected void startPage(PDPage page) {
                this.page++;
            }

            @Override
            protected void writeString(String text, List<TextPosition> positions) {
                if (positions.isEmpty()) return;

                TextPosition pos = positions.get(0);
                blocks.add(new PdfTextBlock(
                        text.trim(),
                        page - 1,
                        pos.getX(),
                        pos.getY()
                ));
            }
        };

        stripper.setSortByPosition(true);
        stripper.getText(document);

        return blocks;
    }

    private String normalize(String text) {
        return text.toUpperCase().trim();
    }
}