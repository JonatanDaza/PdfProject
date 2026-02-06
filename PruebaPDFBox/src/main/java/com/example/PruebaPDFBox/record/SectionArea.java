package com.example.PruebaPDFBox.record;

public record SectionArea(
        int startPage,
        float startY,
        int endPage,
        float endY
) {}