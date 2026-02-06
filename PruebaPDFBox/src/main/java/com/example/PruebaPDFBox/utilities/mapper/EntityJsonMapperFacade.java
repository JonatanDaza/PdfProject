package com.example.PruebaPDFBox.utilities.mapper;

import com.example.PruebaPDFBox.dto.*;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class EntityJsonMapperFacade {

    private final WeightedDecibelsMapper weightedDecibelsMapper;
    private final ObservationsMapper observationsMapper;
    private final RecommendationMapper recommendationMapper;
    private final RecommendationsConductMapper recommendationsConductMapper;
    private final HealthRequirementMapper healthRequirementMapper;

    public EntityJsonMapperFacade(
            WeightedDecibelsMapper weightedDecibelsMapper,
            ObservationsMapper observationsMapper,
            RecommendationMapper recommendationMapper,
            RecommendationsConductMapper recommendationsConductMapper,
            HealthRequirementMapper healthRequirementMapper) {
        this.weightedDecibelsMapper = weightedDecibelsMapper;
        this.observationsMapper = observationsMapper;
        this.recommendationMapper = recommendationMapper;
        this.recommendationsConductMapper = recommendationsConductMapper;
        this.healthRequirementMapper = healthRequirementMapper;
    }

    // WeightedDecibels
    public String parseWeightedDecibelsToJson(String rawText) {
        return weightedDecibelsMapper.parseToJson(rawText);
    }

    public List<WeightedDecibelsDto> weightedDecibelsFromJson(String json) {
        return weightedDecibelsMapper.fromJson(json);
    }

    public String weightedDecibelsToJson(List<WeightedDecibelsDto> dtos) {
        return weightedDecibelsMapper.toJson(dtos);
    }

    // Observations
    public String parseObservationsToJson(String rawText) {
        return observationsMapper.parseToJson(rawText);
    }

    public List<ObservationsDto> observationsFromJson(String json) {
        return observationsMapper.fromJson(json);
    }

    public String observationsToJson(List<ObservationsDto> dtos) {
        return observationsMapper.toJson(dtos);
    }

    // Recommendations
    public String parseRecommendationsToJson(String rawText) {
        return recommendationMapper.parseToJson(rawText);
    }

    public List<RecommendationDto> recommendationsFromJson(String json) {
        return recommendationMapper.fromJson(json);
    }

    public String recommendationsToJson(List<RecommendationDto> dtos) {
        return recommendationMapper.toJson(dtos);
    }

    // RecommendationsConduct
    public String parseRecommendationsConductToJson(String rawText) {
        return recommendationsConductMapper.parseToJson(rawText);
    }

    public List<RecommendationsConductDto> recommendationsConductFromJson(String json) {
        return recommendationsConductMapper.fromJson(json);
    }

    public String recommendationsConductToJson(List<RecommendationsConductDto> dtos) {
        return recommendationsConductMapper.toJson(dtos);
    }

    // HealthRequirements
    public String parseHealthRequirementsToJson(String rawText) {
        return healthRequirementMapper.parseToJson(rawText);
    }

    public List<HealthRequirementDto> healthRequirementsFromJson(String json) {
        return healthRequirementMapper.fromJson(json);
    }

    public String healthRequirementsToJson(List<HealthRequirementDto> dtos) {
        return healthRequirementMapper.toJson(dtos);
    }
}
