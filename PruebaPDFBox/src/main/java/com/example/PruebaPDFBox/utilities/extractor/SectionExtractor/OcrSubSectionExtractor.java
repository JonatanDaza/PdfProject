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

    public Map<String, String> extract(
            OcrPageContext context,
            SectionArea section,
            Set<String> subTitles
    ) {
        String sectionText = context.textForSection(section.startPage(), section.endPage());
        List<String> orderedTitles = findOrderedTitles(sectionText, subTitles);

        Map<String, String> result = new LinkedHashMap<>();
        for (int i = 0; i < orderedTitles.size(); i++) {
            String current = orderedTitles.get(i);
            Set<String> nextTitles = new HashSet<>(orderedTitles.subList(i + 1, orderedTitles.size()));
            String subText = ocrFieldExtractor.extractSubSection(sectionText, current, nextTitles);
            result.put(current.toUpperCase().trim(), subText);
        }

        return result;
    }

    private List<String> findOrderedTitles(String text, Set<String> subTitles) {
        String upperNormalized = normalize(text.toUpperCase());
        List<Map.Entry<Integer, String>> found = new ArrayList<>();

        for (String title : subTitles) {
            String titleNormalized = normalize(title.toUpperCase().trim());
            int idx = upperNormalized.indexOf(titleNormalized);
            if (idx != -1) found.add(Map.entry(idx, title));
        }

        found.sort(Map.Entry.comparingByKey());
        return found.stream().map(Map.Entry::getValue).toList();
    }

    private String normalize(String text) {
        if (text == null) return "";
        return text
                .replace("Á","A").replace("É","E").replace("Í","I")
                .replace("Ó","O").replace("Ú","U").replace("Ü","U")
                .replace("Ñ","N")
                .replaceAll("\\s+", " ")
                .trim();
    }
}