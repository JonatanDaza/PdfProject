package com.example.PruebaPDFBox.record;

public record FieldExtractionConfig(
        String label,
        String fieldName,
        AreaConfig areaConfig,
        ExtractionStrategy strategy
) {
    public enum ExtractionStrategy {
        HORIZONTAL,      // extractValue
        VERTICAL,        // extractValueVertical
        EXACT_MATCH      // extractValueExact
    }
}