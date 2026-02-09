package com.example.PruebaPDFBox.utilities.mapper;

import com.example.PruebaPDFBox.dto.ClinicalHistoryDto;
import com.example.PruebaPDFBox.model.ClinicalHistory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.factory.Mappers;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.util.List;

@Mapper(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE, uses = ExamMap.class)
public interface ClinicalHistoryMap {
    ClinicalHistoryMap INSTANCE = Mappers.getMapper(ClinicalHistoryMap.class);

    ClinicalHistoryDto EntityToDTO(ClinicalHistory clinicalHistory);

    ClinicalHistory DTOToEntity(ClinicalHistoryDto clinicalHistory);

    List<ClinicalHistoryDto> EntityToDTOs(List<ClinicalHistory> clinicalHistories);

    void updateClinicalHistory(ClinicalHistoryDto clinicalHistoryDto, @MappingTarget ClinicalHistory clinicalHistory);

    default Page<ClinicalHistoryDto> EntityToDTOs(Page<ClinicalHistory> clinicalHistories) {
        List<ClinicalHistoryDto> dtos = EntityToDTOs(clinicalHistories.getContent());
        return new PageImpl<>(dtos, clinicalHistories.getPageable(), clinicalHistories.getTotalElements());
    }
}
