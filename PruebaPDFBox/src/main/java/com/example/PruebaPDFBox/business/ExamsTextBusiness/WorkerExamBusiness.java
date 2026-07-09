package com.example.PruebaPDFBox.business.ExamsTextBusiness;

import com.example.PruebaPDFBox.model.Worker;
import com.example.PruebaPDFBox.record.OcrPageContext;
import com.example.PruebaPDFBox.record.SectionArea;
import com.example.PruebaPDFBox.service.ServiceEntity.WorkerService;
import com.example.PruebaPDFBox.utilities.extractor.SectionExtractor.OcrSubSectionExtractor;
import com.example.PruebaPDFBox.utilities.extractor.TextExtractor.OcrFieldExtractor;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class WorkerExamBusiness {

    private final OcrFieldExtractor ocrFieldExtractor;
    private final OcrSubSectionExtractor ocrSubSectionExtractor;
    private final WorkerService workerService;

    public static final Set<String> WORKER_SUB_SECTION_TITLES = Set.of("DATOS PERSONALES");

    public Worker processWorker(OcrPageContext context, SectionArea section) {
        Map<String, String> subSections = ocrSubSectionExtractor.extract(
                context, section, WORKER_SUB_SECTION_TITLES);

        String text = subSections.get("DATOS PERSONALES");
        if (text == null || text.isBlank()) {
            System.out.println("⚠️ DATOS PERSONALES no encontrado, usando texto completo como fallback");
            text = context.textForSection(section.startPage(), section.endPage());
        }

        if (text == null || text.isBlank()) {
            System.err.println("❌ No hay texto disponible para extraer worker");
            return null;
        }

        System.out.println("=== WORKER - texto de extracción ===\n" + text);

        String cc       = extractCC(text);
        String genero   = extractGenero(text);
        String edad     = extractEdad(text);
        String nombre   = extractNombre(text);
        String estadoCivil    = extractEstadoCivil(text);
        String nivelEducativo = extractNivelEducativo(text);

        System.out.println("\n=== WORKER - Datos extraídos ===");
        System.out.println("  CC:               '" + cc + "' " + (cc.isBlank() ? "❌" : "✅"));
        System.out.println("  Género:           '" + genero + "' " + (genero.isBlank() ? "⚠️" : "✅"));
        System.out.println("  Edad:             '" + edad + "' " + (edad.isBlank() ? "⚠️" : "✅"));
        System.out.println("  Nombre:           '" + nombre + "'");
        System.out.println("  Estado Civil:     '" + estadoCivil + "'");
        System.out.println("  Nivel Educativo:  '" + nivelEducativo + "'");

        if (cc.isBlank()) {
            System.err.println("❌ CC obligatorio no encontrado");
            return null;
        }

        Map<String, String> extractedData = new HashMap<>();
        extractedData.put("CC:",                                    cc);
        extractedData.put("Genero:",                                truncate(genero, 30));
        extractedData.put("Edad:",                                  truncate(edad, 10));
        extractedData.put("Estado Civil:",                          estadoCivil);
        extractedData.put("Nivel Educativo:",                       nivelEducativo);
        extractedData.put("Nombre del Trabajador a Aspirante:",     truncate(nombre, 100));

        return workerService.buildFromExtractedData(extractedData, cc);
    }

    // ── Extractores privados ────────────────────────────────────────────────

    private String extractCC(String text) {
        Pattern p = Pattern.compile(
                "(?i)\\b(?:C\\.?C\\.?|C\\.?E\\.?|ce|cc)\\s*[:\\-]?\\s*(\\d{6,15})");
        Matcher m = p.matcher(text);
        String best = "";
        int nameIdx = text.toUpperCase().lastIndexOf("NOMBRE");

        while (m.find()) {
            String found = m.group(1);
            if (nameIdx != -1 && m.start() >= nameIdx && best.isBlank()) {
                best = found;
            } else if (best.isBlank()) {
                best = found;
            }
        }

        if (best.isBlank()) {
            String raw = ocrFieldExtractor.extractAfterLabel(text, "CC:");
            best = ocrFieldExtractor.extractFirstNumber(raw);
        }
        return best;
    }

    private String extractGenero(String text) {
        String genero = ocrFieldExtractor.extractAfterLabel(text, "Genero:");
        if (genero.isBlank()) genero = ocrFieldExtractor.extractAfterLabel(text, "Género:");
        if (genero.isBlank()) genero = ocrFieldExtractor.extractAfterLabel(text, "Sexo:");
        if (genero.isBlank()) {
            Matcher m = Pattern.compile("(?i)\\b(MASCULINO|FEMENINO|HOMBRE|MUJER)\\b").matcher(text);
            if (m.find()) genero = m.group(1);
        }
        if (!genero.isBlank()) {
            genero = genero.split("\\r?\\n")[0].trim().split("\\s+")[0].trim();
        }
        return genero;
    }

    private String extractEdad(String text) {
        String raw = ocrFieldExtractor.extractAfterLabel(text, "Edad:");
        String edad = ocrFieldExtractor.extractFirstNumber(raw);
        if (edad.isBlank()) {
            Matcher m = Pattern.compile("(?i)(\\d{1,3})\\s*(?:AÑOS|ANOS)").matcher(text);
            if (m.find()) edad = m.group(1);
        }
        return edad;
    }

    private String extractNombre(String text) {
        String nombre = ocrFieldExtractor.extractAfterLabel(text, "Nombre del Trabajador a Aspirante:");
        if (nombre.isBlank()) nombre = ocrFieldExtractor.extractAfterLabel(text, "Nombres y Apellidos:");
        if (nombre.isBlank()) nombre = ocrFieldExtractor.extractAfterLabel(text, "Nombre:");

        if (nombre.isBlank()) {
            Matcher m = Pattern.compile(
                    "(?i)\\bNombre(?: del Trabajador a Aspirante)?\\s*[:\\-]?\\s*(.+?)(?=\\s+(?:Genero|G[eé]nero|Sexo|Edad|CC|CE|Fecha|Empresa|Municipio|Cargo)\\b|$)",
                    Pattern.DOTALL).matcher(text);
            if (m.find()) nombre = m.group(1).trim();
        }

        if (!nombre.isBlank()) {
            nombre = nombre
                    .replaceAll("(?i)^del\\s+trabajador\\s+a\\s+aspirante\\s*[:\\-]?\\s*", "")
                    // Corta en cualquier campo conocido que pueda aparecer inline
                    .replaceAll("(?i)\\s+(?:C[oó]digo|Genero|G[eé]nero|Sexo|Edad|CC|CE|Fecha|Empresa|Municipio|Cargo)\\b.*$(?s)", "")
                    .replaceAll("\\r?\\n", " ")
                    .replaceAll("\\s+", " ")
                    .trim();
            if (nombre.length() > 100) nombre = nombre.substring(0, 100).trim();
        }
        return nombre;
    }

    private String extractEstadoCivil(String text) {
        Pattern p = Pattern.compile(
                "Estado\\s+Civil:\\s*(.*?)(?=\\s+Nivel\\s+Educativo:|\\s+E\\.P\\.S:|$)",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher m = p.matcher(text);
        if (m.find()) {
            return truncate(m.group(1).replaceAll("\\s+", " ").trim(), 80);
        }
        return "";
    }

    private String extractNivelEducativo(String text) {
        Pattern p = Pattern.compile(
                "Nivel\\s+Educativo:\\s*(.*?)(?=\\s+E\\.P\\.S:|\\s+A\\.F\\.P:|\\s+Tipo\\s+de\\s+Sangre:|$)",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher m = p.matcher(text);
        if (m.find()) {
            return truncate(m.group(1).replaceAll("\\s+", " ").trim(), 100);
        }
        return "";
    }

    private String truncate(String value, int maxLength) {
        if (value == null) return "";
        if (value.length() > maxLength) {
            System.err.println("⚠️ Truncado a " + maxLength + " chars");
            return value.substring(0, maxLength).trim();
        }
        return value;
    }
}