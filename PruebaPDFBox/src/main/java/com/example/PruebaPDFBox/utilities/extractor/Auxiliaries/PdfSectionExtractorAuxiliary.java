// package com.example.PruebaPDFBox.utilities.extractor.Auxiliaries;

// import com.example.PruebaPDFBox.record.PdfTextBlock;
// import org.apache.pdfbox.pdmodel.PDDocument;
// import org.apache.pdfbox.pdmodel.PDPage;
// import org.apache.pdfbox.text.PDFTextStripper;
// import org.apache.pdfbox.text.TextPosition;
// import org.springframework.stereotype.Component;

// import java.io.IOException;
// import java.util.*;
// import java.util.stream.Collectors;

// @Component
// public class PdfSectionExtractorAuxiliary {

//     public static final Map<String, Set<String>> SECTION_TITLES = Map.of(
//             "OPTOMETRY_LABEL", Set.of("EXAMEN DE OPTOMETRIA", "EXAMEN DE CONSULTA DE OPTOMETRIA", "EXAMEN DE VISIOMETRIA"),
//             "OCCUPATIONAL_MEDICINE_LABEL", Set.of("EVALUACIÓN MEDICA OCUPACIONAL"),
//             "AUDIOMETRY_LABEL", Set.of("AUDIOMETRÍA"),
//             "LABORATORY_LABEL_TEXT", Set.of("INFORME DE RESULTADOS", "EXTENDIDO DE SANGRE PERIFERICA, ESTUDIO DE MORFOLOGIA"),
//             "PSYCHOMOTOR_LABEL", Set.of("Resumen de Resultados Test PSICO-MOTRÍZ"),
//             "PSYCHOLOGY_LABEL", Set.of("PSICOLOGÍA OCUPACIONAL"),
//             "SPIROMETRY_LABEL", Set.of("INFORME DE ESPIROMETRÍA")
//     );

//     public static final Set<String> ALL_TITLES = SECTION_TITLES.values()
//             .stream()
//             .flatMap(Set::stream)
//             .map(String::toUpperCase)
//             .collect(Collectors.toSet());

//     //Extract all text blocks from the PDF document with their positions
//     public List<PdfTextBlock> extractTextBlocks(PDDocument document) throws IOException {

//         List<PdfTextBlock> blocks = new ArrayList<>();

//         PDFTextStripper stripper = new PDFTextStripper() {
//             int page = 0;

//             @Override
//             protected void startPage(PDPage page) {
//                 this.page++;
//             }

//             @Override
//             protected void writeString(String text, List<TextPosition> positions) {
//                 if (positions.isEmpty()) return;

//                 TextPosition pos = positions.get(0);
//                 blocks.add(new PdfTextBlock(
//                         text.trim(),
//                         page - 1,
//                         pos.getX(),
//                         pos.getY()
//                 ));
//             }
//         };

//         stripper.setSortByPosition(true);
//         stripper.getText(document);

//         return blocks;
//     }

//     public static String normalize(String text) {
//         return text.toUpperCase().trim();
//     }
// }