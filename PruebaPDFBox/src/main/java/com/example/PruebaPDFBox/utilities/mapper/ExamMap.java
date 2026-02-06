package com.example.PruebaPDFBox.utilities.mapper;

import com.example.PruebaPDFBox.dto.ExamDto;
import com.example.PruebaPDFBox.model.Exam;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.factory.Mappers;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.util.List;

@Mapper(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE, uses = {WorkerMap.class, DataExamMap.class})
public interface ExamMap {
    ExamMap INSTANCE = Mappers.getMapper(ExamMap.class);

    @Mapping(target = "clinicalHistoryId", source = "clinicalHistory.id")
    ExamDto EntityToDTO(Exam exam);

    @Mapping(target = "clinicalHistory", ignore = true)
    Exam DTOToEntity(ExamDto exam);

    List<ExamDto> EntityToDTOs(List<Exam> exams);

    @Mapping(target = "clinicalHistory", ignore = true)
    void updateExam(ExamDto examDto, @MappingTarget Exam exam);

    default Page<ExamDto> EntityToDTOs(Page<Exam> exams) {
        List<ExamDto> dtos = EntityToDTOs(exams.getContent());
        return new PageImpl<>(dtos, exams.getPageable(), exams.getTotalElements());
    }
}
