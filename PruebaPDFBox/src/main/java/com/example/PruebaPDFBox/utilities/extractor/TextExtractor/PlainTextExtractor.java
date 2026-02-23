package com.example.PruebaPDFBox.utilities.extractor.TextExtractor;

import com.example.PruebaPDFBox.record.AreaConfig;
import com.example.PruebaPDFBox.record.FieldExtractionConfig;
import com.example.PruebaPDFBox.record.SubSectionArea;
import com.example.PruebaPDFBox.record.SectionArea;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class PlainTextExtractor {

    // Clase interna para mantener el estado de cada campo
    private static class FieldState {
        final FieldExtractionConfig config;
        boolean labelFound = false;
        boolean valueCaptured = false;
        float labelX = -1;
        float labelY = -1;
        float baseY = 0;
        StringBuilder value = new StringBuilder();

        FieldState(FieldExtractionConfig config) {
            this.config = config;
        }
    }

    public Map<String, String> extractMultipleValues(
            PDDocument document,
            List<FieldExtractionConfig> fields,
            SectionArea section,
            SubSectionArea subSection
    ) throws IOException {

        Map<String, String> results = new HashMap<>();
        Map<String, FieldState> fieldStates = new HashMap<>();

        // Inicializar estado para cada campo
        for (FieldExtractionConfig field : fields) {
            fieldStates.put(field.label(), new FieldState(field));
            results.put(field.fieldName(), "");
        }

        PDFTextStripper stripper = new PDFTextStripper() {
            int currentPage = -1;

            @Override
            protected void startPage(PDPage page) {
                currentPage++;
            }

            @Override
            protected void writeString(String text, List<TextPosition> positions) {
                if (positions.isEmpty()) return;

                TextPosition first = positions.get(0);
                float x = first.getX();
                float y = first.getY();

                // Filtro por sección
                if (currentPage < section.startPage() || currentPage > section.endPage()) return;
                if (currentPage == section.startPage() && y < section.startY()) return;
                if (currentPage == section.endPage() && y > section.endY()) return;

                // Filtro por subsección
                if (subSection != null) {
                    if (currentPage != subSection.page()) return;
                    if (y < subSection.startY()) return;
                    if (y > subSection.endY()) return;
                    if (subSection.endY() != Float.MAX_VALUE && y > subSection.endY()) return;
                }

                // Procesar cada campo según su estrategia
                for (FieldState state : fieldStates.values()) {
                    if (state.valueCaptured) continue;

                    switch (state.config.strategy()) {
                        case EXACT_MATCH:
                            processExactMatch(text, x, y, state);
                            break;
                        case HORIZONTAL:
                            processHorizontal(text, x, y, state);
                            break;
                        case VERTICAL:
                            processVertical(text, x, y, state);
                            break;
                    }
                }
            }

            private void processExactMatch(String text, float x, float y, FieldState state) {
                String trimmed = text.trim();
                String label = state.config.label();

                if (!state.labelFound && trimmed.toUpperCase().contains(label.toUpperCase())) {
                    state.labelFound = true;
                    state.labelX = x;
                    state.labelY = y;

                    // Intentar capturar valor en la misma línea después del label
                    int idx = trimmed.toUpperCase().indexOf(label.toUpperCase());
                    String after = trimmed.substring(idx + label.length()).trim();
                    if (!after.isEmpty()) {
                        state.value.append(after);
                        state.valueCaptured = true;
                    }
                } else if (state.labelFound && !state.valueCaptured) {
                    captureValue(text, x, y, state);
                }
            }

            private void processHorizontal(String text, float x, float y, FieldState state) {
                if (!state.labelFound && text.toUpperCase().trim().contains(state.config.label().toUpperCase())) {
                    state.labelFound = true;
                    state.labelX = x;
                    state.labelY = y;
                } else if (state.labelFound && !state.valueCaptured) {
                    AreaConfig cfg = state.config.areaConfig();
                    float startX = state.labelX + cfg.offsetX();
                    float endX = startX + cfg.width();

                    if (y >= state.labelY - cfg.height()
                            && y <= state.labelY + cfg.height()
                            && x >= startX
                            && x <= endX) {
                        state.value.append(text.trim()).append(" ");
                        state.valueCaptured = true;
                    }
                }
            }

            private void processVertical(String text, float x, float y, FieldState state) {
                if (!state.labelFound && text.toUpperCase().trim().contains(state.config.label().toUpperCase())) {
                    state.labelFound = true;
                    state.labelX = x;
                    state.labelY = y;
                } else if (state.labelFound && !state.valueCaptured) {
                    AreaConfig cfg = state.config.areaConfig();

                    if (y > state.labelY
                            && y <= state.labelY + (cfg.height() * 4)
                            && Math.abs(x - state.labelX) <= cfg.width()) {
                        state.value.append(text.trim()).append(" ");
                    }
                }
            }

            private void captureValue(String text, float x, float y, FieldState state) {
                AreaConfig cfg = state.config.areaConfig();
                float startX = state.labelX + cfg.offsetX();
                float endX = startX + cfg.width();

                if (y >= state.labelY - cfg.height()
                        && y <= state.labelY + cfg.height()
                        && x >= startX
                        && x <= endX) {
                    state.value.append(text.trim());
                    state.valueCaptured = true;
                }
            }
        };

        stripper.setStartPage(section.startPage() + 1);
        stripper.setEndPage(section.endPage() + 1);

        stripper.setSortByPosition(true);
        stripper.getText(document);

        // Convertir resultados
        for (FieldState state : fieldStates.values()) {
            results.put(state.config.fieldName(), state.value.toString().trim());
        }

        return results;
    }

    // Extract only numbers
    public String extractOnlyNumbers(String value) {
        if (value == null || value.isEmpty()) return null;
        return value.replaceAll("[^0-9.,]", "").trim();
    }

    // Extract only the first number found
    public String extractFirstNumber(String value) {
        if (value == null || value.isEmpty()) return null;
        Matcher matcher = Pattern.compile("\\d+").matcher(value);
        return matcher.find() ? matcher.group() : null;
    }

    // Extract date in format dd/MM/yyyy
    public String extractDate(String value) {
        if (value == null || value.isEmpty()) return null;
        Matcher matcher = Pattern.compile("\\d{2}/\\d{2}/\\d{4}").matcher(value);
        if (matcher.find()) return matcher.group();
        return extractLongFormatDate(value);
    }

    private String extractLongFormatDate(String value) {
        if (value == null || value.isEmpty()) return null;

        Map<String, String> monthMap = getStringStringMap();
        Pattern pattern = Pattern.compile(
                "(?:\\w+,\\s*)?(\\d{1,2})\\s+de\\s+(\\w+)\\s+de\\s+(\\d{4})",
                Pattern.CASE_INSENSITIVE
        );

        Matcher matcher = pattern.matcher(value.toLowerCase());
        if (matcher.find()) {
            String day = matcher.group(1);
            String monthName = matcher.group(2);
            String year = matcher.group(3);

            String month = monthMap.get(monthName.toLowerCase());
            if (month != null) {
                if (day.length() == 1) day = "0" + day;
                return day + "/" + month + "/" + year;
            }
        }
        return null;
    }

    private static Map<String, String> getStringStringMap() {
        Map<String, String> monthMap = new HashMap<>();
        monthMap.put("enero", "01");
        monthMap.put("febrero", "02");
        monthMap.put("marzo", "03");
        monthMap.put("abril", "04");
        monthMap.put("mayo", "05");
        monthMap.put("junio", "06");
        monthMap.put("julio", "07");
        monthMap.put("agosto", "08");
        monthMap.put("septiembre", "09");
        monthMap.put("octubre", "10");
        monthMap.put("noviembre", "11");
        monthMap.put("diciembre", "12");
        return monthMap;
    }

    public String extractExamDate(PDDocument document, SectionArea section) {
        try {
            String fullText = extractFullTextFromSection(document, section);
            Pattern datePattern = Pattern.compile(
                    "(\\w+,\\s*)?(\\d{1,2})\\s+de\\s+(\\w+)\\s+de\\s+(\\d{4})\\s+\\d{1,2}:\\d{2}:\\d{2}\\s+[ap]\\.m\\.",
                    Pattern.CASE_INSENSITIVE
            );

            Matcher matcher = datePattern.matcher(fullText);
            if (matcher.find()) {
                String examDate = extractDate(matcher.group(0));
                if (examDate != null && !examDate.isEmpty()) return examDate;
            }
        } catch (Exception e) {
            // null
        }
        return null;
    }

    public String extractFullTextFromSection(PDDocument document, SectionArea section) throws IOException {
        PDFTextStripper stripper = new PDFTextStripper();

        if (section != null) {
            stripper.setStartPage(section.startPage() + 1);
            stripper.setEndPage(section.endPage() + 1);
        }

        stripper.setSortByPosition(true);
        return stripper.getText(document);
    }

    public String extractTextBetweenLabels(PDDocument document, String startLabel, String endLabel, SectionArea section) throws IOException {
        StringBuilder value = new StringBuilder();
        final boolean[] flags = {false, false}; // [startFound, endFound]
        final String upperStart = startLabel.toUpperCase();
        final String upperEnd = endLabel.toUpperCase();

        PDFTextStripper stripper = new PDFTextStripper() {
            int currentPage = -1;

            @Override
            protected void startPage(PDPage page) {
                currentPage++;
            }

            @Override
            protected void writeString(String text, List<TextPosition> positions) {
                if (flags[1] || positions.isEmpty()) return;

                float y = positions.get(0).getY();
                if (currentPage < section.startPage() || currentPage > section.endPage()) return;
                if (currentPage == section.startPage() && y < section.startY()) return;
                if (currentPage == section.endPage() && y > section.endY()) return;

                String upperText = text.toUpperCase().trim();

                if (!flags[0] && upperText.contains(upperStart)) {
                    flags[0] = true;
                    String after = text.substring(text.toUpperCase().indexOf(upperStart) + upperStart.length()).trim();
                    if (!after.isEmpty()) value.append(after).append(" ");
                } else if (flags[0] && upperText.contains(upperEnd)) {
                    flags[1] = true;
                    int idx = text.toUpperCase().indexOf(upperEnd);
                    if (idx > 0) {
                        String before = text.substring(0, idx).trim();
                        if (!before.isEmpty()) value.append(before).append(" ");
                    }
                } else if (flags[0]) {
                    value.append(text).append(" ");
                }
            }
        };

        stripper.setSortByPosition(true);
        stripper.getText(document);

        return value.toString().trim();
    }

    public Map<String, String> extractFromSubSection(
            PDDocument document,
            SectionArea section,
            Map<String, SubSectionArea> subSections,
            String subSectionName,
            List<FieldExtractionConfig> fields
    ) throws IOException {
        SubSectionArea area = subSections.get(subSectionName);

        return area != null
                ? extractMultipleValues(document, fields, section, area)
                : Collections.emptyMap();
    }
}


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
// import java.util.Collections;
// import java.util.HashMap;
// import java.util.List;
// import java.util.Map;
// import java.util.regex.Matcher;
// import java.util.regex.Pattern;

// @Component
// public class PlainTextExtractor {
    
//     private static final boolean DEBUG = true; // Mantener en true para ver las coordenadas
    
//     public Map<String, String> extractMultipleValues(
//         PDDocument document,
//         List<FieldExtractionConfig> fields,
//         SectionArea section,
//         SubSectionArea subSection
//     ) throws IOException {

//         if (DEBUG) {
//             System.out.println("\n=== INICIANDO EXTRACCIÓN ===");
//             System.out.println("Sección: páginas " + section.startPage() + "-" + section.endPage() + 
//                              ", Y: " + section.startY() + "-" + section.endY());
//             if (subSection != null) {
//                 System.out.println("Subsección: página " + subSection.page() + 
//                                  ", Y: " + subSection.startY() + "-" + subSection.endY());
//             }
//             System.out.println("Campos a extraer: " + fields.size());
//             for (FieldExtractionConfig field : fields) {
//                 System.out.println("  - " + field.fieldName() + " (label: '" + field.label() + 
//                                  "', estrategia: " + field.strategy() + 
//                                  ", offsetX ACTUAL: " + field.areaConfig().offsetX() + 
//                                  ", width ACTUAL: " + field.areaConfig().width() + 
//                                  ", height ACTUAL: " + field.areaConfig().height() + ")");
//             }
//             System.out.println("\n=== BUSCANDO LABELS Y VALORES ===\n");
//         }

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
//                 if (DEBUG) {
//                     System.out.println("\n--- Página " + (currentPage + 1) + " ---");
//                 }
//             }

//             @Override
//             protected void writeString(String text, List<TextPosition> positions) {
//                 if (positions.isEmpty()) return;

//                 TextPosition first = positions.get(0);
//                 float x = first.getX();
//                 float y = first.getY();
//                 float width = first.getWidth();
//                 float height = first.getHeight();
//                 String trimmedText = text.trim();

//                 if (DEBUG && !trimmedText.isEmpty()) {
//                     System.out.println("Texto: '" + trimmedText + "' | X: " + x + " | Y: " + y + 
//                                      " | Width: " + width + " | Height: " + height);
//                 }

//                 // FILTRO POR SUBSECCIÓN
//                 if (subSection != null) {
//                     if (currentPage < subSection.page()) return;
//                     if (currentPage > subSection.page()) return;
//                     if (y > subSection.startY()) return;
//                     if (y < subSection.endY()) return;
                    
//                     if (DEBUG && !trimmedText.isEmpty()) {
//                         System.out.println("  ✓ DENTRO de subsección");
//                     }
//                 }

//                 // Procesar cada campo según su estrategia
//                 for (FieldState state : fieldStates.values()) {
//                     if (state.valueCaptured) continue;

//                     switch (state.config.strategy()) {
//                         case EXACT_MATCH:
//                             processExactMatch(text, x, y, width, height, state);
//                             break;
//                         case HORIZONTAL:
//                             processHorizontal(text, x, y, width, height, state);
//                             break;
//                         case VERTICAL:
//                             processVertical(text, x, y, width, height, state);
//                             break;
//                     }
//                 }
//             }

//             private void processExactMatch(String text, float x, float y, float width, float height, FieldState state) {
//                 String trimmedText = text.trim();
                
//                 if (!state.labelFound && trimmedText.equalsIgnoreCase(state.config.label().trim())) {
//                     state.labelFound = true;
//                     state.labelX = x;
//                     state.labelY = y;
                    
//                     if (DEBUG) {
//                         System.out.println("  🔍 [EXACT_MATCH] LABEL encontrado: '" + state.config.label() + 
//                                          "' en X: " + x + ", Y: " + y);
//                     }
//                     return;
//                 }

//                 if (state.labelFound && !state.valueCaptured && !trimmedText.isEmpty()) {
//                     // CALCULAR coordenadas relativas al label
//                     float offsetX = x - state.labelX;
//                     float offsetY = y - state.labelY;
                    
//                     if (DEBUG) {
//                         System.out.println("  📍 [EXACT_MATCH] POSIBLE VALOR: '" + trimmedText + "'");
//                         System.out.println("     Coordenadas del label: X=" + state.labelX + ", Y=" + state.labelY);
//                         System.out.println("     Coordenadas del valor: X=" + x + ", Y=" + y);
//                         System.out.println("     → offsetX = " + offsetX + " (X valor - X label)");
//                         System.out.println("     → offsetY = " + offsetY + " (Y valor - Y label)");
//                         System.out.println("     → width sugerido = " + width + " (ancho del texto)");
//                         System.out.println("     → height sugerido = " + height + " (alto del texto)");
//                         System.out.println("     ⚠️  Para configurar: new AreaConfig(" + offsetX + ", " + 
//                                          width + ", " + height + ")");
//                     }
                    
//                     captureValue(text, x, y, width, height, state);
//                 }
//             }

//             private void processHorizontal(String text, float x, float y, float width, float height, FieldState state) {
//                 String upperText = text.toUpperCase();
//                 String upperLabel = state.config.label().toUpperCase();
//                 String trimmedText = text.trim();
                
//                 if (!state.labelFound && upperText.contains(upperLabel)) {
//                     state.labelFound = true;
//                     state.labelX = x;
//                     state.labelY = y;
                    
//                     if (DEBUG) {
//                         System.out.println("  🔍 [HORIZONTAL] LABEL encontrado: '" + state.config.label() + 
//                                          "' en X: " + x + ", Y: " + y);
//                     }
//                     return;
//                 }

//                 if (state.labelFound && !state.valueCaptured && !trimmedText.isEmpty()) {
//                     // CALCULAR coordenadas relativas al label
//                     float offsetX = x - state.labelX;
//                     float offsetY = y - state.labelY;
                    
//                     if (DEBUG) {
//                         System.out.println("  📍 [HORIZONTAL] POSIBLE VALOR: '" + trimmedText + "'");
//                         System.out.println("     Coordenadas del label: X=" + state.labelX + ", Y=" + state.labelY);
//                         System.out.println("     Coordenadas del valor: X=" + x + ", Y=" + y);
//                         System.out.println("     → offsetX = " + offsetX + " (X valor - X label)");
//                         System.out.println("     → offsetY = " + offsetY + " (Y valor - Y label)");
//                         System.out.println("     → width sugerido = " + width + " (ancho del texto)");
//                         System.out.println("     → height sugerido = " + height + " (alto del texto)");
//                         System.out.println("     ⚠️  Para configurar: new AreaConfig(" + offsetX + ", " + 
//                                          width + ", " + height + ")");
//                     }
                    
//                     captureValue(text, x, y, width, height, state);
//                 }
//             }

//             private void processVertical(String text, float x, float y, float width, float height, FieldState state) {
//                 if (text == null || text.isBlank()) return;
//                 String trimmedText = text.trim();

//                 if (!state.labelFound && text.toUpperCase().contains(state.config.label().toUpperCase())) {
//                     state.labelFound = true;
//                     state.baseY = y;
//                     state.labelX = x; // Guardamos también X para referencia
                    
//                     if (DEBUG) {
//                         System.out.println("  🔍 [VERTICAL] LABEL encontrado: '" + state.config.label() + 
//                                          "' en X: " + x + ", Y: " + y + " (baseY: " + y + ")");
//                     }
//                     return;
//                 }

//                 if (!state.labelFound) return;

//                 AreaConfig cfg = state.config.areaConfig();
//                 float maxY = state.baseY + cfg.height();

//                 if (y > maxY) {
//                     if (DEBUG && state.value.length() > 0) {
//                         System.out.println("  ⏹️ [VERTICAL] Finalizando captura para '" + state.config.fieldName() + 
//                                          "' - valor acumulado: '" + state.value.toString().trim() + "'");
//                     }
//                     state.valueCaptured = true;
//                     return;
//                 }

//                 boolean insideColumn = x >= cfg.offsetX() && x <= cfg.offsetX() + cfg.width();
//                 boolean belowLabel = y > state.baseY && y <= maxY;

//                 if (DEBUG && !trimmedText.isEmpty()) {
//                     float offsetX = x - state.labelX;
//                     float offsetY = y - state.baseY;
                    
//                     System.out.println("  📍 [VERTICAL] TEXTO ENCONTRADO: '" + trimmedText + "'");
//                     System.out.println("     Coordenadas del label: X=" + state.labelX + ", Y(base)=" + state.baseY);
//                     System.out.println("     Coordenadas del texto: X=" + x + ", Y=" + y);
//                     System.out.println("     → offsetX = " + offsetX + " (X texto - X label)");
//                     System.out.println("     → offsetY = " + offsetY + " (Y texto - Y label base)");
//                     System.out.println("     → width texto = " + width);
//                     System.out.println("     → height texto = " + height);
//                     System.out.println("     → insideColumn: " + insideColumn + " (X entre " + cfg.offsetX() + 
//                                      " y " + (cfg.offsetX() + cfg.width()) + ")");
//                     System.out.println("     → belowLabel: " + belowLabel + " (Y entre " + state.baseY + 
//                                      " y " + maxY + ")");
                    
//                     if (insideColumn && belowLabel) {
//                         System.out.println("     ⚠️  Para configurar VERTICAL: offsetX=" + offsetX + 
//                                          ", width=" + width + ", height=" + (y - state.baseY));
//                     }
//                 }

//                 if (insideColumn && belowLabel) {
//                     state.value.append(text).append(" ");
//                 }
//             }

//             private void captureValue(String text, float x, float y, float width, float height, FieldState state) {
//                 if (state.valueCaptured) return;
                
//                 AreaConfig cfg = state.config.areaConfig();
//                 float startX = state.labelX + cfg.offsetX();
//                 float endX = startX + cfg.width();

//                 boolean horizontalMatch =
//                         y >= state.labelY - cfg.height()
//                                 && y <= state.labelY + cfg.height()
//                                 && x >= startX
//                                 && x <= endX;

//                 boolean verticalMatch =
//                         y > state.labelY
//                                 && y <= state.labelY + (cfg.height() * 4)
//                                 && Math.abs(x - state.labelX) <= cfg.width();

//                 if (DEBUG && !text.trim().isEmpty()) {
//                     System.out.println("    Verificando si el valor está dentro del área configurada:");
//                     System.out.println("      Label: (" + state.labelX + ", " + state.labelY + ")");
//                     System.out.println("      Valor: (" + x + ", " + y + ")");
//                     System.out.println("      Área configurada:");
//                     System.out.println("        offsetX: " + cfg.offsetX() + " → X: " + startX + " a " + endX);
//                     System.out.println("        height: " + cfg.height() + " → Y: " + 
//                                      (state.labelY - cfg.height()) + " a " + (state.labelY + cfg.height()));
//                     System.out.println("      offsetX calculado real: " + (x - state.labelX));
//                     System.out.println("      width real del texto: " + width);
//                     System.out.println("      height real del texto: " + height);
                    
//                     if (horizontalMatch || verticalMatch) {
//                         System.out.println("    ✅ DENTRO del área configurada");
//                     } else {
//                         System.out.println("    ❌ FUERA del área configurada");
//                     }
//                 }

//                 if (horizontalMatch || verticalMatch) {
//                     state.value.append(text).append(" ");
//                     if (DEBUG) {
//                         System.out.println("      ✓ VALOR CAPTURADO - Acumulado: '" + state.value.toString().trim() + "'");
//                     }
                    
//                     // Para EXACT_MATCH y HORIZONTAL, capturamos solo una vez
//                     if (state.config.strategy() != FieldExtractionConfig.ExtractionStrategy.VERTICAL) {
//                         state.valueCaptured = true;
//                     }
//                 }
//             }
//         };

//         stripper.setStartPage(section.startPage());
//         stripper.setEndPage(section.endPage());

//         stripper.setSortByPosition(true);
//         stripper.getText(document);

//         // Convertir resultados
//         for (FieldState state : fieldStates.values()) {
//             String extractedValue = state.value.toString().trim();
//             results.put(state.config.fieldName(), extractedValue);
            
//             if (DEBUG && !extractedValue.isEmpty()) {
//                 System.out.println("\n✅ RESULTADO FINAL - " + state.config.fieldName() + 
//                                  ": '" + extractedValue + "'");
//                 System.out.println("   Coordenadas finales del label: X=" + state.labelX + ", Y=" + state.labelY);
//             } else if (DEBUG && state.labelFound && extractedValue.isEmpty()) {
//                 System.out.println("\n⚠️  NO SE CAPTURÓ VALOR para " + state.config.fieldName() + 
//                                  " (label encontrado en X:" + state.labelX + ", Y:" + state.labelY + ")");
//             }
//         }

//         if (DEBUG) {
//             System.out.println("\n=== EXTRACCIÓN COMPLETADA ===");
//         }

//         return results;
//     }

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

//     //Resto de métodos igual...
//     public String extractOnlyNumbers(String value) {
//         if (value == null || value.isEmpty()) {
//             return null;
//         }
//         return value.replaceAll("[^0-9.,]", "").trim();
//     }

//     public String extractFirstNumber(String value) {
//         if (value == null || value.isEmpty()) {
//             return null;
//         }
//         Matcher matcher = Pattern.compile("\\d+").matcher(value);
//         if (matcher.find()) {
//             return matcher.group();
//         }
//         return null;
//     }

//     public String extractDate(String value) {
//         if (value == null || value.isEmpty()) {
//             return null;
//         }

//         Matcher matcher = Pattern.compile("\\d{2}/\\d{2}/\\d{4}").matcher(value);
//         if (matcher.find()) {
//             return matcher.group();
//         }

//         return extractLongFormatDate(value);
//     }

//     private String extractLongFormatDate(String value) {
//         if (value == null || value.isEmpty()) {
//             return null;
//         }

//         Map<String, String> monthMap = getStringStringMap();
//         Pattern pattern = Pattern.compile(
//             "(?:\\w+,\\s*)?(\\d{1,2})\\s+de\\s+(\\w+)\\s+de\\s+(\\d{4})",
//             Pattern.CASE_INSENSITIVE
//         );

//         Matcher matcher = pattern.matcher(value.toLowerCase());
//         if (matcher.find()) {
//             String day = matcher.group(1);
//             String monthName = matcher.group(2);
//             String year = matcher.group(3);

//             String month = monthMap.get(monthName.toLowerCase());
//             if (month != null) {
//                 if (day.length() == 1) {
//                     day = "0" + day;
//                 }
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
//                 "(\\w+,\\s*)?(\\d{1,2})\\s+de\\s+(\\w+)\\s+de\\s+(\\d{4})\\s+\\d{1,2}:\\d{2}:\\d{2}\\s+[ap]\\.m\\.",
//                 Pattern.CASE_INSENSITIVE
//             );

//             Matcher matcher = datePattern.matcher(fullText);
//             if (matcher.find()) {
//                 String examDate = extractDate(matcher.group(0));
//                 if (examDate != null && !examDate.isEmpty()) {
//                     return examDate;
//                 }
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
//         if (DEBUG) {
//             System.out.println("\n=== EXTRACT TEXT BETWEEN LABELS ===");
//             System.out.println("startLabel: '" + startLabel + "', endLabel: '" + endLabel + "'");
//         }
        
//         StringBuilder value = new StringBuilder();
//         final boolean[] flags = {false, false};
//         final String upperStart = startLabel.toUpperCase();
//         final String upperEnd = endLabel.toUpperCase();

//         PDFTextStripper stripper = new PDFTextStripper() {
//             int currentPage = -1;

//             @Override
//             protected void startPage(PDPage page) {
//                 currentPage++;
//                 if (DEBUG) {
//                     System.out.println("\n--- Página " + (currentPage + 1) + " ---");
//                 }
//             }

//             @Override
//             protected void writeString(String text, List<TextPosition> positions) {
//                 if (flags[1] || positions.isEmpty()) return;

//                 float y = positions.get(0).getY();
//                 if (currentPage < section.startPage() || currentPage > section.endPage()) return;
//                 if (currentPage == section.startPage() && y < section.startY()) return;
//                 if (currentPage == section.endPage() && y > section.endY()) return;

//                 String trimmedText = text.trim();
//                 String upperText = text.toUpperCase().trim();

//                 if (DEBUG && !trimmedText.isEmpty()) {
//                     System.out.println("Texto: '" + trimmedText + "' | Y: " + y);
//                 }

//                 if (!flags[0] && upperText.contains(upperStart)) {
//                     flags[0] = true;
//                     if (DEBUG) {
//                         System.out.println("  🔍 Start label encontrado en Y: " + y);
//                     }
//                     appendAfter(text, upperStart);
//                 } else if (flags[0] && upperText.contains(upperEnd)) {
//                     flags[1] = true;
//                     if (DEBUG) {
//                         System.out.println("  ⏹️ End label encontrado en Y: " + y);
//                     }
//                     appendBefore(text, upperEnd);
//                 } else if (flags[0]) {
//                     if (DEBUG && !trimmedText.isEmpty()) {
//                         System.out.println("  📥 Capturando: '" + trimmedText + "'");
//                     }
//                     value.append(text).append(" ");
//                 }
//             }

//             private void appendAfter(String text, String label) {
//                 String after = text.substring(text.toUpperCase().indexOf(label) + label.length()).trim();
//                 if (!after.isEmpty()) {
//                     if (DEBUG) {
//                         System.out.println("  📥 Capturando después de label: '" + after + "'");
//                     }
//                     value.append(after).append(" ");
//                 }
//             }

//             private void appendBefore(String text, String label) {
//                 int idx = text.toUpperCase().indexOf(label);
//                 if (idx > 0) {
//                     String before = text.substring(0, idx).trim();
//                     if (!before.isEmpty()) {
//                         if (DEBUG) {
//                             System.out.println("  📥 Capturando antes de end label: '" + before + "'");
//                         }
//                         value.append(before).append(" ");
//                     }
//                 }
//             }
//         };

//         stripper.setSortByPosition(true);
//         stripper.getText(document);
        
//         String result = value.toString().trim();
//         if (DEBUG) {
//             System.out.println("\n✅ RESULTADO FINAL: '" + result + "'");
//             System.out.println("=== FIN EXTRACT TEXT BETWEEN LABELS ===\n");
//         }
        
//         return result;
//     }

//     public Map<String, String> extractFromSubSection(
//         PDDocument document,
//         SectionArea section,
//         Map<String, SubSectionArea> subSections,
//         String subSectionName,
//         List<FieldExtractionConfig> fields
//     ) throws IOException {
//         SubSectionArea area = subSections.get(subSectionName);
        
//         if (DEBUG) {
//             System.out.println("\n=== EXTRACT FROM SUBSECTION: " + subSectionName + " ===");
//             if (area != null) {
//                 System.out.println("Área encontrada: página " + area.page() + 
//                                  ", Y: " + area.startY() + "-" + area.endY());
//             } else {
//                 System.out.println("⚠️  No se encontró la subsección: " + subSectionName);
//             }
//         }
        
//         return area != null
//                 ? extractMultipleValues(
//                         document,
//                         fields,
//                         section,
//                         area
//                     )
//                 : Collections.emptyMap();
//     }

// }