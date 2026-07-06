// package com.example.PruebaPDFBox.utilities.extractor.TextExtractor;

// import com.example.PruebaPDFBox.record.AreaConfig;
// import com.example.PruebaPDFBox.record.FieldExtractionConfig;
// import com.example.PruebaPDFBox.record.SubSectionArea;
// import com.example.PruebaPDFBox.record.SectionArea;

// import org.apache.pdfbox.pdmodel.PDDocument;
// import org.apache.pdfbox.pdmodel.PDPage;
// import org.apache.pdfbox.text.PDFTextStripper;
// import org.apache.pdfbox.text.TextPosition;
// import org.springframework.stereotype.Component;

// import java.io.IOException;
// import java.util.HashMap;
// import java.util.List;
// import java.util.Map;
// import java.util.regex.Matcher;
// import java.util.regex.Pattern;

// @Component
// public class PlainTextExtractor {

//     // Clase interna para mantener el estado de cada campo
//     private static class FieldState {
//         final FieldExtractionConfig config;
//         boolean labelFound = false;
//         boolean valueCaptured = false;
//         float labelX = -1;
//         float labelY = -1;
//         float baseY = 0;
//         StringBuilder value = new StringBuilder();

//         FieldState(FieldExtractionConfig config) {
//             this.config = config;
//         }
//     }

//     public Map<String, String> extractMultipleValues(
//             PDDocument document,
//             List<FieldExtractionConfig> fields,
//             SectionArea section,
//             SubSectionArea subSection
//     ) throws IOException {

//         Map<String, String> results = new HashMap<>();
//         Map<String, FieldState> fieldStates = new HashMap<>();

//         // Inicializar estado para cada campo
//         for (FieldExtractionConfig field : fields) {
//             fieldStates.put(field.label(), new FieldState(field));
//             results.put(field.fieldName(), "");
//         }

//         PDFTextStripper stripper = new PDFTextStripper() {
//             int currentPage = -1;

//             @Override
//             protected void startPage(PDPage page) {
//                 currentPage++;
//             }

//             @Override
//             protected void writeString(String text, List<TextPosition> positions) {
//                 if (positions.isEmpty()) return;

//                 TextPosition first = positions.get(0);
//                 float x = first.getX();
//                 float y = first.getY();

//                 // Filtro por sección
//                 if (currentPage < section.startPage() || currentPage > section.endPage()) return;
//                 if (currentPage == section.startPage() && y < section.startY()) return;
//                 if (currentPage == section.endPage() && y > section.endY()) return;

//                 // Filtro por subsección
//                 if (subSection != null) {
//                     if (currentPage != subSection.page()) return;
//                     if (y < subSection.startY()) return;
//                     if (y > subSection.endY()) return;
//                     if (subSection.endY() != Float.MAX_VALUE && y > subSection.endY()) return;
//                 }

//                 // Procesar cada campo según su estrategia
//                 for (FieldState state : fieldStates.values()) {
//                     if (state.valueCaptured) continue;

//                     switch (state.config.strategy()) {
//                         case EXACT_MATCH:
//                             processExactMatch(text, x, y, state);
//                             break;
//                         case HORIZONTAL:
//                             processHorizontal(text, x, y, state);
//                             break;
//                         case VERTICAL:
//                             processVertical(text, x, y, state);
//                             break;
//                     }
//                 }
//             }

//             private void processExactMatch(String text, float x, float y, FieldState state) {
//                 String trimmed = text.trim();
//                 String label = state.config.label();

//                 if (!state.labelFound && trimmed.toUpperCase().contains(label.toUpperCase())) {
//                     state.labelFound = true;
//                     state.labelX = x;
//                     state.labelY = y;

//                     // Intentar capturar valor en la misma línea después del label
//                     int idx = trimmed.toUpperCase().indexOf(label.toUpperCase());
//                     String after = trimmed.substring(idx + label.length()).trim();
//                     if (!after.isEmpty()) {
//                         state.value.append(after);
//                         state.valueCaptured = true;
//                     }
//                 } else if (state.labelFound && !state.valueCaptured) {
//                     captureValue(text, x, y, state);
//                 }
//             }

//             private void processHorizontal(String text, float x, float y, FieldState state) {
//                 if (!state.labelFound && text.toUpperCase().trim().contains(state.config.label().toUpperCase())) {
//                     state.labelFound = true;
//                     state.labelX = x;
//                     state.labelY = y;
//                 } else if (state.labelFound && !state.valueCaptured) {
//                     AreaConfig cfg = state.config.areaConfig();
//                     float startX = state.labelX + cfg.offsetX();
//                     float endX = startX + cfg.width();

//                     if (y >= state.labelY - cfg.height()
//                             && y <= state.labelY + cfg.height()
//                             && x >= startX
//                             && x <= endX) {
//                         state.value.append(text.trim()).append(" ");
//                         state.valueCaptured = true;
//                     }
//                 }
//             }

//             private void processVertical(String text, float x, float y, FieldState state) {
//                 if (!state.labelFound && text.toUpperCase().trim().contains(state.config.label().toUpperCase())) {
//                     state.labelFound = true;
//                     state.labelX = x;
//                     state.labelY = y;
//                 } else if (state.labelFound && !state.valueCaptured) {
//                     AreaConfig cfg = state.config.areaConfig();

//                     if (y > state.labelY
//                             && y <= state.labelY + (cfg.height() * 4)
//                             && Math.abs(x - state.labelX) <= cfg.width()) {
//                         state.value.append(text.trim()).append(" ");
//                     }
//                 }
//             }

//             private void captureValue(String text, float x, float y, FieldState state) {
//                 AreaConfig cfg = state.config.areaConfig();
//                 float startX = state.labelX + cfg.offsetX();
//                 float endX = startX + cfg.width();

//                 if (y >= state.labelY - cfg.height()
//                         && y <= state.labelY + cfg.height()
//                         && x >= startX
//                         && x <= endX) {
//                     state.value.append(text.trim());
//                     state.valueCaptured = true;
//                 }
//             }
//         };

//         stripper.setStartPage(section.startPage() + 1);
//         stripper.setEndPage(section.endPage() + 1);

//         stripper.setSortByPosition(true);
//         stripper.getText(document);

//         // Convertir resultados
//         for (FieldState state : fieldStates.values()) {
//             results.put(state.config.fieldName(), state.value.toString().trim());
//         }

//         return results;
//     }

//     // Extract only numbers
//     public String extractOnlyNumbers(String value) {
//         if (value == null || value.isEmpty()) return null;
//         return value.replaceAll("[^0-9.,]", "").trim();
//     }

//     // Extract only the first number found
//     public String extractFirstNumber(String value) {
//         if (value == null || value.isEmpty()) return null;
//         Matcher matcher = Pattern.compile("\\d+").matcher(value);
//         return matcher.find() ? matcher.group() : null;
//     }

//     // Extract date in format dd/MM/yyyy
//     public String extractDate(String value) {
//         if (value == null || value.isEmpty()) return null;
//         Matcher matcher = Pattern.compile("\\d{2}/\\d{2}/\\d{4}").matcher(value);
//         if (matcher.find()) return matcher.group();
//         return extractLongFormatDate(value);
//     }

//     private String extractLongFormatDate(String value) {
//         if (value == null || value.isEmpty()) return null;

//         Map<String, String> monthMap = getStringStringMap();
//         Pattern pattern = Pattern.compile(
//                 "(?:\\w+,\\s*)?(\\d{1,2})\\s+de\\s+(\\w+)\\s+de\\s+(\\d{4})",
//                 Pattern.CASE_INSENSITIVE
//         );

//         Matcher matcher = pattern.matcher(value.toLowerCase());
//         if (matcher.find()) {
//             String day = matcher.group(1);
//             String monthName = matcher.group(2);
//             String year = matcher.group(3);

//             String month = monthMap.get(monthName.toLowerCase());
//             if (month != null) {
//                 if (day.length() == 1) day = "0" + day;
//                 return day + "/" + month + "/" + year;
//             }
//         }
//         return null;
//     }

//     private static Map<String, String> getStringStringMap() {
//         Map<String, String> monthMap = new HashMap<>();
//         monthMap.put("enero", "01");
//         monthMap.put("febrero", "02");
//         monthMap.put("marzo", "03");
//         monthMap.put("abril", "04");
//         monthMap.put("mayo", "05");
//         monthMap.put("junio", "06");
//         monthMap.put("julio", "07");
//         monthMap.put("agosto", "08");
//         monthMap.put("septiembre", "09");
//         monthMap.put("octubre", "10");
//         monthMap.put("noviembre", "11");
//         monthMap.put("diciembre", "12");
//         return monthMap;
//     }

//     public String extractExamDate(PDDocument document, SectionArea section) {
//         try {
//             String fullText = extractFullTextFromSection(document, section);
//             Pattern datePattern = Pattern.compile(
//                     "(\\w+,\\s*)?(\\d{1,2})\\s+de\\s+(\\w+)\\s+de\\s+(\\d{4})\\s+\\d{1,2}:\\d{2}:\\d{2}\\s+[ap]\\.m\\.",
//                     Pattern.CASE_INSENSITIVE
//             );

//             Matcher matcher = datePattern.matcher(fullText);
//             if (matcher.find()) {
//                 String examDate = extractDate(matcher.group(0));
//                 if (examDate != null && !examDate.isEmpty()) return examDate;
//             }
//         } catch (Exception e) {
//             // null
//         }
//         return null;
//     }

//     public String extractFullTextFromSection(PDDocument document, SectionArea section) throws IOException {
//         PDFTextStripper stripper = new PDFTextStripper();

//         if (section != null) {
//             stripper.setStartPage(section.startPage() + 1);
//             stripper.setEndPage(section.endPage() + 1);
//         }

//         stripper.setSortByPosition(true);
//         return stripper.getText(document);
//     }

//     public String extractTextBetweenLabels(PDDocument document, String startLabel, String endLabel, SectionArea section) throws IOException {
//         StringBuilder value = new StringBuilder();
//         final boolean[] flags = {false, false}; // [startFound, endFound]
//         final String upperStart = startLabel.toUpperCase();
//         final String upperEnd = endLabel.toUpperCase();

//         PDFTextStripper stripper = new PDFTextStripper() {
//             int currentPage = -1;

//             @Override
//             protected void startPage(PDPage page) {
//                 currentPage++;
//             }

//             @Override
//             protected void writeString(String text, List<TextPosition> positions) {
//                 if (flags[1] || positions.isEmpty()) return;

//                 float y = positions.get(0).getY();
//                 if (currentPage < section.startPage() || currentPage > section.endPage()) return;
//                 if (currentPage == section.startPage() && y < section.startY()) return;
//                 if (currentPage == section.endPage() && y > section.endY()) return;

//                 String upperText = text.toUpperCase().trim();

//                 if (!flags[0] && upperText.contains(upperStart)) {
//                     flags[0] = true;
//                     String after = text.substring(text.toUpperCase().indexOf(upperStart) + upperStart.length()).trim();
//                     if (!after.isEmpty()) value.append(after).append(" ");
//                 } else if (flags[0] && upperText.contains(upperEnd)) {
//                     flags[1] = true;
//                     int idx = text.toUpperCase().indexOf(upperEnd);
//                     if (idx > 0) {
//                         String before = text.substring(0, idx).trim();
//                         if (!before.isEmpty()) value.append(before).append(" ");
//                     }
//                 } else if (flags[0]) {
//                     value.append(text).append(" ");
//                 }
//             }
//         };

//         stripper.setSortByPosition(true);
//         stripper.getText(document);

//         return value.toString().trim();
//     }
// }