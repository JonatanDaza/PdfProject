package com.example.PruebaPDFBox.service;

import com.example.PruebaPDFBox.dto.ClinicalHistoryDto;
//import com.example.PruebaPDFBox.dto.DataExamDto;
import com.example.PruebaPDFBox.model.ClinicalHistory;
//import com.example.PruebaPDFBox.model.DataExam;
import com.example.PruebaPDFBox.repository.ClinicalHistoryRepository;
//import com.example.PruebaPDFBox.repository.DataExamRepository;
import com.example.PruebaPDFBox.utilities.Infrastructure.exception.CustomException;
import com.example.PruebaPDFBox.utilities.mapper.ClinicalHistoryMap;
//import com.example.PruebaPDFBox.utilities.mapper.DataExamMap;
import com.example.PruebaPDFBox.utilities.mapper.ExamMap;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class ClinicalHistoryService {

    private final ClinicalHistoryRepository clinicalHistoryRepository;
    private final ClinicalHistoryMap clinicalHistoryMap;

    public ClinicalHistoryService(ClinicalHistoryRepository clinicalHistoryRepository) {
        this.clinicalHistoryRepository = clinicalHistoryRepository;
        this.clinicalHistoryMap = ClinicalHistoryMap.INSTANCE;
    }

    public Page<ClinicalHistoryDto> getAllClinicalHistories(int page, int size) {
        try {
            PageRequest pageRequest = PageRequest.of(page, size);
            Page<ClinicalHistory> clinicalHistoryPage = clinicalHistoryRepository.findAllPaged(pageRequest);
            return clinicalHistoryMap.EntityToDTOs(clinicalHistoryPage);
        } catch (DataAccessException e) {
            throw new CustomException("Error retrieving Clinical Histories: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            throw new CustomException("Unexpected error: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public ClinicalHistoryDto findById(Long id) {
        try {
            ClinicalHistory clinicalHistory = clinicalHistoryRepository.findById(id)
                    .orElseThrow(() -> new CustomException("Clinical History not found with id: " + id, HttpStatus.NOT_FOUND));

            ClinicalHistoryDto dto = ClinicalHistoryMap.INSTANCE.EntityToDTO(clinicalHistory);
            dto.setExams(ExamMap.INSTANCE.EntityToDTOs(clinicalHistory.getExams()));

            return dto;
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            throw new CustomException("Error Getting Clinical History: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }
}
