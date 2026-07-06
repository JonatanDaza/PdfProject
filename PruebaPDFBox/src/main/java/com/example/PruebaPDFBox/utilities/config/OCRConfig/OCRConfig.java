package com.example.PruebaPDFBox.utilities.config.OCRConfig;

import net.sourceforge.tess4j.Tesseract;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

@Configuration
public class OCRConfig {

    @Bean
    @Scope("prototype")
    public Tesseract tesseract() {
        Tesseract t = new Tesseract();

        t.setDatapath("C:\\Users\\Zonamedica\\AppData\\Local\\Programs\\Tesseract-OCR\\tessdata");
        t.setLanguage("spa+eng");
        t.setOcrEngineMode(1);
        t.setVariable("user_defined_dpi", "300");
        t.setPageSegMode(6);

        // SIN whitelist — comentada para diagnóstico.
        // t.setVariable("tessedit_char_whitelist", "...");

        t.setVariable("preserve_interword_spaces", "1");

        return t;
    }
}