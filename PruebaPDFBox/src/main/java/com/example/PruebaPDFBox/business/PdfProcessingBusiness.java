package com.example.PruebaPDFBox.business;

import com.example.PruebaPDFBox.business.AudiometryBusiness.AudiometryTextBusiness;
import com.example.PruebaPDFBox.business.LaboratoryBusiness.LaboratoryResultImgBusiness;
import com.example.PruebaPDFBox.business.LaboratoryBusiness.LaboratoryResultTextBusiness;
import com.example.PruebaPDFBox.business.OccupationalMedicineBusiness.OccupationalMedicineTextBusiness;
import com.example.PruebaPDFBox.business.OptometryBusiness.OptometryTextBusiness;
import com.example.PruebaPDFBox.model.Worker;
import com.example.PruebaPDFBox.record.AreaConfig;
import com.example.PruebaPDFBox.record.FieldExtractionConfig;
import com.example.PruebaPDFBox.record.SectionArea;
import com.example.PruebaPDFBox.repository.WorkerRepository;
import com.example.PruebaPDFBox.utilities.extractor.SectionExtractor.PdfSectionExtractor;
import com.example.PruebaPDFBox.utilities.extractor.TextExtractor.SectionTextExtractor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import java.util.*;

@Component
public class PdfProcessingBusiness {

    private final PdfSectionExtractor pdfSectionExtractor;
    private final OccupationalMedicineTextBusiness occupationalMedicineTextBusiness;
    private final WorkerRepository workerRepository;
    private final SectionTextExtractor sectionTextExtractor;
    private final AudiometryTextBusiness audiometryTextBusiness;
    private final OptometryTextBusiness optometryTextBusiness;
    private final LaboratoryResultImgBusiness laboratoryResultImgBusiness;
    private final SpirometryBusiness spirometryBusiness;
    private final LaboratoryResultTextBusiness laboratoryResultTextBusiness;

    public PdfProcessingBusiness(PdfSectionExtractor pdfSectionExtractor, OccupationalMedicineTextBusiness occupationalMedicineTextBusiness, WorkerRepository workerRepository, SectionTextExtractor sectionTextExtractor, AudiometryTextBusiness audiometryTextBusiness, OptometryTextBusiness optometryTextBusiness, LaboratoryResultImgBusiness laboratoryResultImgBusiness, SpirometryBusiness spirometryBusiness, LaboratoryResultTextBusiness laboratoryResultTextBusiness) {
        this.pdfSectionExtractor = pdfSectionExtractor;
        this.occupationalMedicineTextBusiness = occupationalMedicineTextBusiness;
        this.workerRepository = workerRepository;
        this.sectionTextExtractor = sectionTextExtractor;
        this.audiometryTextBusiness = audiometryTextBusiness;
        this.optometryTextBusiness = optometryTextBusiness;
        this.laboratoryResultImgBusiness = laboratoryResultImgBusiness;
        this.spirometryBusiness = spirometryBusiness;
        this.laboratoryResultTextBusiness = laboratoryResultTextBusiness;
    }

    public Map<String, Object> processPdf(MultipartFile file) {

        try (PDDocument document = PDDocument.load(file.getInputStream())) {

            Map<String, SectionArea> sections = pdfSectionExtractor.extract(document);

            Worker worker = processWorker(document, sections);
            if (worker == null) {
                throw new RuntimeException("No se pudo extraer datos válidos del trabajador");
            }

            Worker finalWorker = worker;
            worker = workerRepository.findByCC(worker.getCC()).orElseGet(() -> workerRepository.save(finalWorker));

            int examsProcessed = 0;

            for (Map.Entry<String, SectionArea> entry : sections.entrySet()) {
                if ("OCCUPATIONAL_MEDICINE_LABEL".equals(entry.getKey())) {
                    if (occupationalMedicineTextBusiness.process(document, entry.getValue(), worker, entry.getKey())) {
                        examsProcessed++;
                    }
                }

                if ("AUDIOMETRY_LABEL".equals(entry.getKey())) {
                    if (audiometryTextBusiness.process(document, entry.getValue(), worker, entry.getKey())) {
                        examsProcessed++;
                    }
                }

                if ("OPTOMETRY_LABEL".equals(entry.getKey())) {
                    if (optometryTextBusiness.process(document, entry.getValue(), worker, entry.getKey())) {
                        examsProcessed++;
                    }
                }

//                if ("SPIROMETRY_LABEL".equals(entry.getKey())) {
//                    if (spirometryBusiness.process(document, entry.getValue(), worker, entry.getKey())) {
//                        examsProcessed++;
//                    }
//                }

                if ("LABORATORY_LABEL".equals(entry.getKey())) {
                    if (laboratoryResultImgBusiness.process(document, entry.getValue(), entry.getKey(), worker)) {
                        examsProcessed++;
                    }
                }

                if ("LABORATORY_LABEL_TEXT".equals(entry.getKey())) {
                    if (laboratoryResultTextBusiness.process(document, entry.getValue(), entry.getKey(), worker)) {
                        examsProcessed++;
                    }
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("message", "PDF procesado correctamente");
            response.put("examsProcessed", examsProcessed);
            response.put("totalPages", document.getNumberOfPages());

            return response;

        } catch (Exception e) {
            throw new RuntimeException("Error procesando PDF", e);
        }
    }

    private Worker processWorker(PDDocument document, Map<String, SectionArea> sections) {
        Map<String, AreaConfig> config = buildDataConfig();
        Map<String, String> extractedData = new HashMap<>();

        for (Map.Entry<String, SectionArea> entry : sections.entrySet()) {
            SectionArea section = entry.getValue();
            String sectionName = entry.getKey();

            try {
                List<FieldExtractionConfig> fields = buildWorkerFieldConfigurations(config);

                Map<String, String> sectionData = sectionTextExtractor.extractMultipleValues(document, fields, section);

                for (Map.Entry<String, String> dataEntry : sectionData.entrySet()) {
                    String key = dataEntry.getKey();
                    String value = dataEntry.getValue();

                    if (value != null && !value.trim().isEmpty()) {
                        if (!extractedData.containsKey(key) || extractedData.get(key).trim().isEmpty()) {
                            extractedData.put(key, value);
                        }
                    }
                }

            } catch (Exception e) {
                throw new RuntimeException("Error procesando sección " + sectionName + ": " + e.getMessage());
            }
        }

        String cc = extractedData.get("CC:");
        if (cc == null || cc.trim().isEmpty()) {
            System.err.println("No se encontró CC válido");
            return null;
        }

        Worker worker = new Worker();
        worker.setCC(cc);
        worker.setGender(extractedData.get("Genero:"));
        worker.setAge(sectionTextExtractor.extractFirstNumber(extractedData.get("Edad:")));
        worker.setMaritalStatus(extractedData.get("Estado Civil:"));
        worker.setEducationalLevel(extractedData.get("Nivel Educativo:"));

//        worker.setName(extractedData.get("Nombre:"));

        return worker;
    }

    private List<FieldExtractionConfig> buildWorkerFieldConfigurations(Map<String, AreaConfig> config) {
        List<FieldExtractionConfig> fields = new ArrayList<>();

        fields.add(new FieldExtractionConfig("CC:", "CC:", config.get("CC:"), FieldExtractionConfig.ExtractionStrategy.EXACT_MATCH));
        fields.add(new FieldExtractionConfig("Genero:", "Genero:", config.get("Genero:"), FieldExtractionConfig.ExtractionStrategy.EXACT_MATCH));
        fields.add(new FieldExtractionConfig("Edad:", "Edad:", config.get("Edad:"), FieldExtractionConfig.ExtractionStrategy.EXACT_MATCH));
        fields.add(new FieldExtractionConfig("Estado Civil:", "Estado Civil:", config.get("Estado Civil:"), FieldExtractionConfig.ExtractionStrategy.EXACT_MATCH));
        fields.add(new FieldExtractionConfig("Nivel Educativo:", "Nivel Educativo:", config.get("Nivel Educativo:"), FieldExtractionConfig.ExtractionStrategy.EXACT_MATCH));

//        fields.add(new FieldExtractionConfig("Nombre:", "Nombre:", config.get("Nombre:"), FieldExtractionConfig.ExtractionStrategy.EXACT_MATCH));

        return fields;
    }

    private Map<String, AreaConfig> buildDataConfig() {
        Map<String, AreaConfig> config = new HashMap<>();

        config.put("CC:", new AreaConfig(0, 172, 12));
        config.put("Genero:", new AreaConfig(0, 192, 12));
        config.put("Edad:", new AreaConfig(0, 192, 12));
        config.put("Estado Civil:", new AreaConfig(40, 80, 12));
        config.put("Nivel Educativo:", new AreaConfig(40, 120, 12));

//        config.put("Nombre:", new AreaConfig(0, 160, 12));

        return config;
    }
}
