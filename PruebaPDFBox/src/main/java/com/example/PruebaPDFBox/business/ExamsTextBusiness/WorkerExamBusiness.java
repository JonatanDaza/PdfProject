package com.example.PruebaPDFBox.business.ExamsTextBusiness;

import com.example.PruebaPDFBox.model.Worker;
import com.example.PruebaPDFBox.record.OcrPageContext;
import com.example.PruebaPDFBox.record.SectionArea;
import com.example.PruebaPDFBox.service.ServiceEntity.WorkerService;
import com.example.PruebaPDFBox.utilities.extractor.SectionExtractor.OcrSubSectionExtractor;
import com.example.PruebaPDFBox.utilities.extractor.TextExtractor.OcrFieldExtractor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class WorkerExamBusiness {

    private final OcrFieldExtractor ocrFieldExtractor;
    private final OcrSubSectionExtractor ocrSubSectionExtractor;
    private final WorkerService workerService;

    public static final Set<String> WORKER_SUB_SECTION_TITLES = Set.of("DATOS PERSONALES");

    public Worker processWorker(OcrPageContext context, SectionArea section) {

        Map<String, String> subSections = ocrSubSectionExtractor.extract(
                context, section, WORKER_SUB_SECTION_TITLES
        );

        System.out.println("\n=== WORKER - SubSecciones encontradas ===");
        subSections.forEach((k, v) -> System.out.println(
                "  [" + k + "] -> " + (v != null && !v.isBlank() ? "✅" : "❌ VACÍO")
        ));

        String datosPersonalesText = subSections.get("DATOS PERSONALES");
        if (datosPersonalesText == null || datosPersonalesText.isBlank()) {
            System.err.println("❌ No se encontró subsección DATOS PERSONALES");
            return null;
        }

        System.out.println("=== DATOS PERSONALES texto ===\n" + datosPersonalesText);

        // Extracción de campos por regex sobre el texto de la subsección
        String rawCC    = ocrFieldExtractor.extractAfterLabel(datosPersonalesText, "CC:");
        String cc       = ocrFieldExtractor.extractFirstNumber(rawCC);

        String rawGenero = ocrFieldExtractor.extractAfterLabel(datosPersonalesText, "Genero:");
        if (rawGenero.isBlank()) rawGenero = ocrFieldExtractor.extractAfterLabel(datosPersonalesText, "Género:");
        if (rawGenero.isBlank()) rawGenero = ocrFieldExtractor.extractAfterLabel(datosPersonalesText, "Sexo:");

        String rawAge   = ocrFieldExtractor.extractAfterLabel(datosPersonalesText, "Edad:");
        String age      = ocrFieldExtractor.extractFirstNumber(rawAge);

        String estadoCivil    = ocrFieldExtractor.extractAfterLabel(datosPersonalesText, "Estado Civil:");
        String nivelEducativo = ocrFieldExtractor.extractAfterLabel(datosPersonalesText, "Nivel Educativo:");

        String nombreTrabajador = ocrFieldExtractor.extractAfterLabel(datosPersonalesText, "Nombre del Trabajador a Aspirante:");

        System.out.println("\n=== WORKER - Datos extraídos ===");
        System.out.println("  CC:              '" + cc + "' " + (cc.isBlank() ? "❌" : "✅"));
        System.out.println("  Género:          '" + rawGenero + "' " + (rawGenero.isBlank() ? "❌" : "✅"));
        System.out.println("  Edad:            '" + age + "' " + (age.isBlank() ? "❌" : "✅"));
        System.out.println("  Estado Civil:    '" + estadoCivil + "'");
        System.out.println("  Nivel Educativo: '" + nivelEducativo + "'");

        if (cc.isBlank()) {
            System.err.println("❌ CC obligatorio no encontrado");
            return null;
        }
        if (rawGenero.isBlank()) {
            System.err.println("❌ Género obligatorio no encontrado");
            return null;
        }
        if (age.isBlank()) {
            System.err.println("❌ Edad obligatoria no encontrada");
            return null;
        }

        Map<String, String> extractedData = Map.of(
                "CC:",              cc,
                "Genero:",          truncate(rawGenero, 30),
                "Edad:",            truncate(age, 10),
                "Estado Civil:",    truncate(estadoCivil, 30),
                "Nivel Educativo:", truncate(nivelEducativo, 60),
                "Nombre del Trabajador a Aspirante:", truncate(nombreTrabajador, 100)
        );

        return workerService.buildFromExtractedData(extractedData, cc);
    }

    private String truncate(String value, int maxLength) {
        if (value == null) return "";
        if (value.length() > maxLength) {
            System.err.println("⚠️  Truncado a " + maxLength + " chars: '" + value.substring(0, 40) + "...'");
            return value.substring(0, maxLength).trim();
        }
        return value;
    }
}