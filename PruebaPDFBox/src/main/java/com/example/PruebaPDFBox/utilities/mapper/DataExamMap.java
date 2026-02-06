package com.example.PruebaPDFBox.utilities.mapper;

import com.example.PruebaPDFBox.dto.DataExamDto;
import com.example.PruebaPDFBox.dto.HealthRequirementDto;
import com.example.PruebaPDFBox.dto.ObservationsDto;
import com.example.PruebaPDFBox.dto.RecommendationDto;
import com.example.PruebaPDFBox.model.DataExam;
import com.example.PruebaPDFBox.dto.RecommendationsConductDto;
import com.example.PruebaPDFBox.dto.WeightedDecibelsDto;
import com.example.PruebaPDFBox.utilities.config.JsonMapperConfig;
import com.fasterxml.jackson.core.type.TypeReference;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.util.List;

@Mapper(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface DataExamMap {
    DataExamMap INSTANCE = Mappers.getMapper(DataExamMap.class);

    @Mapping(source = "healthRequirements", target = "healthRequirements", qualifiedByName = "jsonToHealthRequirementDtos")
    @Mapping(source = "observations", target = "observations", qualifiedByName = "jsonToObservationsDtos")
    @Mapping(source = "recommendations", target = "recommendations", qualifiedByName = "jsonToRecommendationDtos")
    @Mapping(source = "recommendationsConduct", target = "recommendationsConduct", qualifiedByName = "jsonToRecommendationsConductDtos")
    @Mapping(source = "weightedDecibels", target = "weightedDecibels", qualifiedByName = "jsonToWeightedDecibelsDtos")
    DataExamDto EntityToDTO(DataExam dataExam);

    @Mapping(source = "healthRequirements", target = "healthRequirements", qualifiedByName = "healthRequirementDtosToJson")
    @Mapping(source = "observations", target = "observations", qualifiedByName = "observationsDtosToJson")
    @Mapping(source = "recommendations", target = "recommendations", qualifiedByName = "recommendationDtosToJson")
    @Mapping(source = "recommendationsConduct", target = "recommendationsConduct", qualifiedByName = "recommendationsConductDtosToJson")
    @Mapping(source = "weightedDecibels", target = "weightedDecibels", qualifiedByName = "weightedDecibelsDtosToJson")
    DataExam DTOToEntity(DataExamDto dataExam);

    List<DataExamDto> EntityToDTOs(List<DataExam> dataExams);

    @Mapping(source = "healthRequirements", target = "healthRequirements", qualifiedByName = "healthRequirementDtosToJson")
    @Mapping(source = "observations", target = "observations", qualifiedByName = "observationsDtosToJson")
    @Mapping(source = "recommendations", target = "recommendations", qualifiedByName = "recommendationDtosToJson")
    @Mapping(source = "recommendationsConduct", target = "recommendationsConduct", qualifiedByName = "recommendationsConductDtosToJson")
    @Mapping(source = "weightedDecibels", target = "weightedDecibels", qualifiedByName = "weightedDecibelsDtosToJson")
    void updateDataExam(DataExamDto dataExamDto, @MappingTarget DataExam dataExam);


    // ==================== HealthRequirements ====================

    @Named("healthRequirementDtosToJson")
    default String healthRequirementDtosToJson(List<HealthRequirementDto> dtos) {
        return JsonMapperConfig.objectToJson(dtos);
    }

    @Named("jsonToHealthRequirementDtos")
    default List<HealthRequirementDto> jsonToHealthRequirementDtos(String json) {
        return JsonMapperConfig.jsonToList(json, new TypeReference<List<HealthRequirementDto>>() {});
    }

    // ==================== Observations ====================

    @Named("observationsDtosToJson")
    default String observationsDtosToJson(List<ObservationsDto> dtos) {
        return JsonMapperConfig.objectToJson(dtos);
    }

    @Named("jsonToObservationsDtos")
    default List<ObservationsDto> jsonToObservationsDtos(String json) {
        return JsonMapperConfig.jsonToList(json, new TypeReference<List<ObservationsDto>>() {});
    }

    // ==================== Recommendations ====================

    @Named("recommendationDtosToJson")
    default String recommendationDtosToJson(List<RecommendationDto> dtos) {
        return JsonMapperConfig.objectToJson(dtos);
    }

    @Named("jsonToRecommendationDtos")
    default List<RecommendationDto> jsonToRecommendationDtos(String json) {
        return JsonMapperConfig.jsonToList(json, new TypeReference<List<RecommendationDto>>() {});
    }

    // ==================== RecommendationsConduct ====================

    @Named("recommendationsConductDtosToJson")
    default String recommendationsConductDtosToJson(List<RecommendationsConductDto> dtos) {
        return JsonMapperConfig.objectToJson(dtos);
    }

    @Named("jsonToRecommendationsConductDtos")
    default List<RecommendationsConductDto> jsonToRecommendationsConductDtos(String json) {
        return JsonMapperConfig.jsonToList(json, new TypeReference<List<RecommendationsConductDto>>() {});
    }

    // ==================== WeightedDecibels ====================

    @Named("weightedDecibelsDtosToJson")
    default String weightedDecibelsDtosToJson(List<WeightedDecibelsDto> dtos) {
        return JsonMapperConfig.objectToJson(dtos);
    }

    @Named("jsonToWeightedDecibelsDtos")
    default List<WeightedDecibelsDto> jsonToWeightedDecibelsDtos(String json) {
        return JsonMapperConfig.jsonToList(json, new TypeReference<List<WeightedDecibelsDto>>() {});
    }

    // ==================== Page Conversion ====================

    default Page<DataExamDto> EntityToDTOs(Page<DataExam> dataExams) {
        List<DataExamDto> dtos = EntityToDTOs(dataExams.getContent());
        return new PageImpl<>(dtos, dataExams.getPageable(), dataExams.getTotalElements());
    }
}
