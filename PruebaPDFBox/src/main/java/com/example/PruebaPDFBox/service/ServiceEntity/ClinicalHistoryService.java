package com.example.PruebaPDFBox.service.ServiceEntity;

import com.example.PruebaPDFBox.dto.ClinicalHistoryDto;
import com.example.PruebaPDFBox.dto.ExamDto;
import com.example.PruebaPDFBox.model.ClinicalHistory;
import com.example.PruebaPDFBox.model.Worker;
import com.example.PruebaPDFBox.repository.ClinicalHistoryRepository;
import com.example.PruebaPDFBox.utilities.mapper.ClinicalHistoryMap;
import com.example.PruebaPDFBox.utilities.mapper.MapperJsons.EntityJsonMapperFacade;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClinicalHistoryService {

    private final ClinicalHistoryRepository clinicalHistoryRepository;
    private final EntityJsonMapperFacade entityJsonMapperFacade;
    private final ExamService examService;

    @Transactional
    public Page<ClinicalHistoryDto> getAllClinicalHistories(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Long> idPage = clinicalHistoryRepository.findPagedIds(pageable);
        if (idPage.isEmpty()) {
            return Page.empty(pageable);
        }
        Page<ClinicalHistoryDto> result = ClinicalHistoryMap.INSTANCE.EntityToDTOs(
                    new PageImpl<>(clinicalHistoryRepository.findByIdsWithDetails(idPage.getContent()),
                            pageable,
                            idPage.getTotalElements()
                    )
        );
        entityJsonMapperFacade.hydrateJsonValues(result.getContent());

        return result;
    }

    @Transactional
    public ClinicalHistory findOrCreate(Worker worker, String examDate) {
        return clinicalHistoryRepository
                .findByWorkerAndExamDate(worker, examDate)
                .orElseGet(() -> clinicalHistoryRepository.save(new ClinicalHistory()));
    }

    @Transactional
    public ClinicalHistory save(ClinicalHistory clinicalHistory) {
        return clinicalHistoryRepository.save(clinicalHistory);
    }

    public List<ExamDto> getExamsByClinicalHistoryId(Long clinicalHistoryId) {
        return examService.getExamsByClinicalHistoryId(clinicalHistoryId);
    }
}
