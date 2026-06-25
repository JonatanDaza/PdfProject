package com.example.PruebaPDFBox.record;

import java.util.List;

public record OcrPageContext(List<String> pageTexts ) {
    
    
    public String textForSection(int startPage, int endPage) {
        StringBuilder sb = new StringBuilder();
        for (int i = startPage; i <= Math.min(endPage, pageTexts.size() - 1); i++) {
            sb.append(pageTexts.get(i)).append("\n");
        }
        return sb.toString();
    }

    public int totalPages() {
        return pageTexts.size();
    }
}