package com.example.PruebaPDFBox.controller;

import com.example.PruebaPDFBox.dto.ClinicalHistoryDto;
import com.example.PruebaPDFBox.dto.DataExamDto;
import com.example.PruebaPDFBox.record.PaginationRequest;
import com.example.PruebaPDFBox.service.ClinicalHistoryService;
import com.example.PruebaPDFBox.service.PdfProcessingService;
import com.example.PruebaPDFBox.utilities.exception.CustomException;
import com.example.PruebaPDFBox.utilities.http.ResponseHttpApi;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

@RestController
@RequestMapping("/pdf")
public class PdfProcessingController {

    private static final String PDF_FOLDER = "pdfs";

    private final PdfProcessingService service;
    private final ClinicalHistoryService clinicalHistoryService;

    public PdfProcessingController(PdfProcessingService service, ClinicalHistoryService clinicalHistoryService) {
        this.service = service;
        this.clinicalHistoryService = clinicalHistoryService;
    }

    @PostMapping(value = "/extract",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> extract(@RequestParam(value = "file", required = false) MultipartFile file) {
        if (file == null) {
            return ResponseEntity.badRequest().body("No se recibió el archivo 'file'");
        }
        return ResponseEntity.ok(service.process(file));
    }

    @PostMapping(value = "/HC")
    public Map<String, Object> AllClinicalHistories(@RequestBody PaginationRequest request) {
        try {
            Page<ClinicalHistoryDto> clinicalHistoryDtoPage = clinicalHistoryService.getAllClinicalHistories(
                    request.getPage(),
                    request.getSize()
            );
            return ResponseHttpApi.responseHttpFindAll(
                    clinicalHistoryDtoPage.getContent(),
                    ResponseHttpApi.CODE_OK,
                    "Query ok",
                    clinicalHistoryDtoPage.getTotalPages(),
                    request.getPage(),
                    (int) clinicalHistoryDtoPage.getTotalElements()
            );
        } catch (Exception e) {
            throw new RuntimeException("Error retrieving Clinical Histories: " + e.getMessage(), e);
        }
    }

    @PutMapping(value = "/HC/update/{id}")
    public Map<String, Object> update(@PathVariable Long id, @RequestBody DataExamDto dataExamDto) {
        try {
            clinicalHistoryService.update(id, dataExamDto);
            return ResponseHttpApi.responseHttpAction(id,
                    ResponseHttpApi.CODE_OK, "Ok"
            );
        } catch (CustomException e) {
            return ResponseHttpApi.responseHttpAction(null,ResponseHttpApi.CODE_BAD, e.getMessage());
        } catch (Exception e) {
            return ResponseHttpApi.responseHttpError(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/upload")
    public ResponseEntity<String> uploadPdf(@RequestParam("file") MultipartFile file) {
        try {
            Path pdfPath = Paths.get(PDF_FOLDER);

            String fileName = file.getOriginalFilename();
            assert fileName != null;
            Path destinationPath = pdfPath.resolve(fileName);
            Files.copy(file.getInputStream(), destinationPath,
                    REPLACE_EXISTING);

            return ResponseEntity.ok("Archivo subido exitosamente en: " + destinationPath);
        } catch (Exception e) {
            throw new RuntimeException("Error al guardar el archivo: " + e.getMessage());
        }
    }
}