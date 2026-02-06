package com.example.PruebaPDFBox.service;

import com.example.PruebaPDFBox.business.PdfProcessingBusiness;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class PdfProcessingService {

    private final PdfProcessingBusiness pdfProcessingBusiness;

    public PdfProcessingService(PdfProcessingBusiness pdfProcessingBusiness) {
        this.pdfProcessingBusiness = pdfProcessingBusiness;
    }

    public Object process(MultipartFile file) {
        return pdfProcessingBusiness.processPdf(file);
    }

}