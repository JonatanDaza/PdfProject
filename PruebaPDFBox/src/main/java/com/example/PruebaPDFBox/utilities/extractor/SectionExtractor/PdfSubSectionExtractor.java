// package com.example.PruebaPDFBox.utilities.extractor.SectionExtractor;

// import java.io.IOException;
// import java.util.Comparator;
// import java.util.LinkedHashMap;
// import java.util.List;
// import java.util.Map;
// import java.util.Set;

// import lombok.RequiredArgsConstructor;
// import org.apache.pdfbox.pdmodel.PDDocument;
// import org.springframework.stereotype.Component;

// import com.example.PruebaPDFBox.record.PdfTextBlock;
// import com.example.PruebaPDFBox.record.SectionArea;
// import com.example.PruebaPDFBox.record.SubSectionArea;
// import com.example.PruebaPDFBox.utilities.extractor.Auxiliaries.PdfSectionExtractorAuxiliary;

// @Component
// @RequiredArgsConstructor
// public class PdfSubSectionExtractor {

//     private final PdfSectionExtractorAuxiliary pdfSectionExtractorAuxiliary;

//     public Map<String, SubSectionArea> extract(
//             PDDocument document,
//             SectionArea section,
//             Set<String> subTitles
//     ) throws IOException {

//         // Normalizar los subtítulos a mayúsculas para comparación
//         Set<String> upperSubTitles = new java.util.HashSet<>();
//         for (String title : subTitles) {
//             upperSubTitles.add(title.toUpperCase().trim());
//         }

//         List<PdfTextBlock> blocks = pdfSectionExtractorAuxiliary.extractTextBlocks(document);

//         List<PdfTextBlock> filtered = blocks.stream()
//                 .filter(b ->
//                         b.page() >= section.startPage()
//                                 && b.page() <= section.endPage()
//                                 && upperSubTitles.contains(b.text().toUpperCase().trim())
//                 )
//                 .sorted(Comparator
//                                 .comparingInt(PdfTextBlock::page)
//                                 .thenComparing(PdfTextBlock::y)//.reversed() // SIN .reversed() — orden ascendente (de arriba a abajo)
// //                .sorted(
// //                        Comparator
// //                                .comparingInt(PdfTextBlock::page)
// //                                .thenComparing(Comparator.comparing(PdfTextBlock::y).reversed())
//                 )
//                 .toList();

//         Map<String, SubSectionArea> result = new LinkedHashMap<>();

//         for (int i = 0; i < filtered.size(); i++) {

//             PdfTextBlock current = filtered.get(i);
//             PdfTextBlock next = (i + 1 < filtered.size()) ? filtered.get(i + 1) : null;

//             // endY es la Y del siguiente subtítulo, o el final de la sección

//             float endY;
//             int endPage;

//             if (next != null && next.page() == current.page()) {
//                 // Mismo página: endY es el Y del siguiente subtítulo
//                 endY = next.y();
//                 endPage = current.page();
//             } else {
//                 // Diferente página o no hay siguiente: fin de la sección o página completa
//                 endY = section.endY(); // o Float.MAX_VALUE si no aplica
//                 endPage = current.page();
//             }

//             result.put(
//                     current.text().toUpperCase().trim(),
//                     new SubSectionArea(
//                             current.page(),
//                             current.y(),   // startY = donde empieza el subtítulo
//                             endY           // endY = donde empieza el siguiente (o fin de sección)
//                     )
//             );
//         }

//         return result;
//     }
// }