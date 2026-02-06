package com.example.PruebaPDFBox.utilities.config;

import net.sourceforge.tess4j.Tesseract;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

@Configuration
public class OCRConfig {

/// version 2
    @Bean
    @Scope("prototype")
    public Tesseract tesseract() {
        Tesseract t = new Tesseract();

        t.setDatapath("C:\\Users\\Zonamedica\\AppData\\Local\\Programs\\Tesseract-OCR\\tessdata");
        t.setLanguage("spa+eng");

        t.setPageSegMode(11);
        t.setOcrEngineMode(1);

        // Simplificar la whitelist
        t.setVariable("tessedit_char_whitelist",
                "0123456789.,*ABCDEFGHIJKLMNOPQRSTUVWXYZÁÉÍÓÚÜÑabcdefghijklmnopqrstuvwxyzáéíóúüñ /-:%()");

        return t;
    }

//// version 1
//    @Bean
//    @Scope("prototype")
//    public Tesseract tesseract() {
//        Tesseract t = new Tesseract();
//
//        t.setDatapath("C:\\Users\\Zonamedica\\AppData\\Local\\Programs\\Tesseract-OCR\\tessdata");
//        t.setLanguage("spa+eng");
//        t.setPageSegMode(6);
//        t.setOcrEngineMode(1);
//        t.setVariable("preserve_interword_spaces", "1");
//        t.setVariable("tessedit_char_whitelist",
//                "ABCDEFGHIJKLMNOPQRSTUVWXYZÁÉÍÓÚÜÑabcdefghijklmnopqrstuvwxyzáéíóúüñ" +
//                        "0123456789 !\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~×·*÷/%‰");
//        return t;
//    }
}

