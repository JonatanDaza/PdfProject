package com.example.PruebaPDFBox.record;

public record PdfTextBlock(
        String text,
        int page,
        float x,
        float y
) {}