package com.example.PruebaPDFBox.utilities.extractor.SectionExtractor;

import com.example.PruebaPDFBox.record.OcrPageContext;
import com.example.PruebaPDFBox.record.SectionArea;
import com.example.PruebaPDFBox.utilities.extractor.TextExtractor.OcrFieldExtractor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@RequiredArgsConstructor
public class OcrSubSectionExtractor {

    private final OcrFieldExtractor ocrFieldExtractor;

    /**
     * Devuelve un mapa de subTítulo → texto de esa subsección.
     * Reemplaza PdfSubSectionExtractor; trabaja sobre texto OCR en vez de coordenadas.
     *
     * @param context    contexto OCR con texto por página
     * @param section    área de la sección principal (páginas start→end)
     * @param subTitles  títulos de subsecciones a detectar
     */
    public Map<String, String> extract(
            OcrPageContext context,
            SectionArea section,
            Set<String> subTitles
    ) {
        // Texto completo de la sección principal
        String sectionText = context.textForSection(section.startPage(), section.endPage());

        // Ordenar los títulos por posición de aparición en el texto
        List<String> orderedTitles = findOrderedTitles(sectionText, subTitles);

        Map<String, String> result = new LinkedHashMap<>();

        for (int i = 0; i < orderedTitles.size(); i++) {
            String current = orderedTitles.get(i);

            // Los títulos siguientes son el límite de esta subsección
            Set<String> nextTitles = new HashSet<>(orderedTitles.subList(
                    i + 1, orderedTitles.size()
            ));

            String subText = ocrFieldExtractor.extractSubSection(sectionText, current, nextTitles);
            result.put(current.toUpperCase().trim(), subText);
        }

        return result;
    }

    private List<String> findOrderedTitles(String text, Set<String> subTitles) {
        String upper = text.toUpperCase();
        List<Map.Entry<Integer, String>> found = new ArrayList<>();

        for (String title : subTitles) {
            int idx = upper.indexOf(title.toUpperCase().trim());
            if (idx != -1) {
                found.add(Map.entry(idx, title));
            }
        }

        found.sort(Map.Entry.comparingByKey());
        return found.stream().map(Map.Entry::getValue).toList();
    }
}