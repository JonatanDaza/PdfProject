// package com.example.PruebaPDFBox.utilities.extractor.SectionExtractor;

// import com.example.PruebaPDFBox.record.PdfTextBlock;
// import com.example.PruebaPDFBox.record.SectionArea;
// import com.example.PruebaPDFBox.utilities.extractor.Auxiliaries.PdfSectionExtractorAuxiliary;
// import org.apache.pdfbox.pdmodel.PDDocument;
// import org.springframework.stereotype.Component;

// import java.io.IOException;
// import java.util.*;

// @Component
// public class PdfSectionExtractor {

//     private final PdfSectionExtractorAuxiliary pdfSectionExtractorAuxiliary;

//     public PdfSectionExtractor(PdfSectionExtractorAuxiliary pdfSectionExtractorAuxiliary) {
//         this.pdfSectionExtractorAuxiliary = pdfSectionExtractorAuxiliary;
//     }

//     public Map<String, SectionArea> extract(PDDocument document) throws IOException {

//         List<PdfTextBlock> blocks = pdfSectionExtractorAuxiliary.extractTextBlocks(document);

//         List<PdfTextBlock> titles = blocks.stream()
//                 .filter(b -> PdfSectionExtractorAuxiliary.ALL_TITLES.contains(b.text().toUpperCase().trim()))
// //                .sorted(Comparator
// //                        .comparingInt(PdfTextBlock::page)
// //                        .thenComparing(PdfTextBlock::y).reversed()//.reversed() // SIN .reversed() — orden ascendente (de arriba a abajo)
//                 .sorted(
//                         Comparator
//                                 .comparingInt(PdfTextBlock::page)
//                                 .thenComparing(Comparator.comparing(PdfTextBlock::y).reversed())
//                 )
//                 .toList();

//         Map<String, SectionArea> sections = new LinkedHashMap<>();

//         for (int i = 0; i < titles.size(); i++) {
//             PdfTextBlock current = titles.get(i);
//             PdfTextBlock next = (i + 1 < titles.size()) ? titles.get(i + 1) : null;

//             String normalizedTitle = PdfSectionExtractorAuxiliary.normalize(current.text());
//             String sectionKey = PdfSectionExtractorAuxiliary.SECTION_TITLES.entrySet().stream()
//                     .filter(e -> e.getValue().stream()
//                             .anyMatch(v -> v.equalsIgnoreCase(current.text())))
//                     .map(Map.Entry::getKey)
//                     .findFirst()
//                     .orElse(normalizedTitle);

//             int endPage;
//             float endY;

//             if (next != null && next.page() == current.page()) {
//                 endPage = current.page();
//                 endY = next.y();
//             } else if (next != null && next.page() > current.page()) {
//                 endPage = next.page();
//                 endY = Float.MAX_VALUE;
//             } else {
//                 endPage = document.getNumberOfPages() - 1;
//                 endY = Float.MAX_VALUE;
//             }

//             SectionArea area = new SectionArea(current.page(), current.y(), endPage, endY, current.text().trim());
//             sections.put(sectionKey, area);
//         }
//         return sections;
//     }
// }