package com.example.PruebaPDFBox.service.ServiceEntity;

import com.example.PruebaPDFBox.dto.ClinicalHistoryDto;
import com.example.PruebaPDFBox.repository.ClinicalHistoryRepository;
import com.example.PruebaPDFBox.utilities.mapper.ClinicalHistoryMap;
import com.example.PruebaPDFBox.utilities.mapper.MapperJsons.EntityJsonMapperFacade;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class ClinicalHistoryService {

    private final ClinicalHistoryRepository clinicalHistoryRepository;
    private final ClinicalHistoryMap clinicalHistoryMap;
    private final EntityJsonMapperFacade entityJsonMapperFacade;

    public ClinicalHistoryService(ClinicalHistoryRepository clinicalHistoryRepository, EntityJsonMapperFacade entityJsonMapperFacade) {
        this.clinicalHistoryRepository = clinicalHistoryRepository;
        this.clinicalHistoryMap = ClinicalHistoryMap.INSTANCE;
        this.entityJsonMapperFacade = entityJsonMapperFacade;
    }

    public Page<ClinicalHistoryDto> getAllClinicalHistories(int page, int size) {

        Pageable pageable = PageRequest.of(page, size);

        Page<Long> idPage = clinicalHistoryRepository.findPagedIds(pageable);
        if (idPage.isEmpty()) {
            return Page.empty(pageable);
        }

        Page<ClinicalHistoryDto> result =
                clinicalHistoryMap.EntityToDTOs(
                    new PageImpl<>(clinicalHistoryRepository.findByIdsWithDetails(idPage.getContent()),
                            pageable,
                            idPage.getTotalElements()
                    )
        );
        entityJsonMapperFacade.hydrateJsonValues(result.getContent());

        return result;
    }
}
