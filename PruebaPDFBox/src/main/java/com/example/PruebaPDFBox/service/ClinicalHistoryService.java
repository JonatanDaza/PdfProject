package com.example.PruebaPDFBox.service;

import com.example.PruebaPDFBox.dto.ClinicalHistoryDto;
import com.example.PruebaPDFBox.dto.DataExamDto;
import com.example.PruebaPDFBox.model.ClinicalHistory;
import com.example.PruebaPDFBox.model.DataExam;
import com.example.PruebaPDFBox.repository.ClinicalHistoryRepository;
import com.example.PruebaPDFBox.repository.DataExamRepository;
import com.example.PruebaPDFBox.utilities.exception.CustomException;
import com.example.PruebaPDFBox.utilities.mapper.ClinicalHistoryMap;
import com.example.PruebaPDFBox.utilities.mapper.DataExamMap;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class ClinicalHistoryService {

    private final ClinicalHistoryRepository clinicalHistoryRepository;
    private final ClinicalHistoryMap clinicalHistoryMap;
    private final DataExamRepository dataExamRepository;

    public ClinicalHistoryService(ClinicalHistoryRepository clinicalHistoryRepository, DataExamRepository dataExamRepository) {
        this.clinicalHistoryRepository = clinicalHistoryRepository;
        this.clinicalHistoryMap = ClinicalHistoryMap.INSTANCE;
        this.dataExamRepository = dataExamRepository;
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

    public void update(Long id, DataExamDto dataExamDto) {
        try {
//            dataExamDto.setId(id);
            DataExam dataExam = dataExamRepository.findById(id)
                    .orElseThrow(() -> new CustomException("DataExam not found with id: " + id, HttpStatus.NOT_FOUND));
            DataExamMap.INSTANCE.updateDataExam(dataExamDto, dataExam);

            dataExamRepository.save(dataExam);
        } catch (DataAccessException e) {
            throw new CustomException("Error updating Clinical History: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception ex) {
            throw new CustomException("Unexpected error: " + ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
