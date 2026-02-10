package com.example.PruebaPDFBox.utilities.mapper.MapperJsons;

import com.example.PruebaPDFBox.dto.ClinicalHistoryDto;
import com.example.PruebaPDFBox.dto.DTOJson.*;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class EntityJsonMapperFacade {

    private final WeightedDecibelsMapper weightedDecibelsMapper;
    private final ObservationsMapper observationsMapper;
    private final RecommendationMapper recommendationMapper;
    private final RecommendationsConductMapper recommendationsConductMapper;
    private final HealthRequirementMapper healthRequirementMapper;
    private final MorphologyStudyMapper morphologyStudyMapper;

    public EntityJsonMapperFacade(
            WeightedDecibelsMapper weightedDecibelsMapper,
            ObservationsMapper observationsMapper,
            RecommendationMapper recommendationMapper,
            RecommendationsConductMapper recommendationsConductMapper,
            HealthRequirementMapper healthRequirementMapper, MorphologyStudyMapper morphologyStudyMapper) {
        this.weightedDecibelsMapper = weightedDecibelsMapper;
        this.observationsMapper = observationsMapper;
        this.recommendationMapper = recommendationMapper;
        this.recommendationsConductMapper = recommendationsConductMapper;
        this.healthRequirementMapper = healthRequirementMapper;
        this.morphologyStudyMapper = morphologyStudyMapper;
    }

    public Object parseDataValueJson(String json, String dataTypeName) {
        if (json == null || json.isEmpty()) return json;

        return switch (dataTypeName) {
            case "Ponderado Decibelios" ->
                    weightedDecibelsFromJson(json);
            case "Observaciones" ->
                    observationsFromJson(json);
            case "Recomendaciones" ->
                    recommendationsFromJson(json);
            case "Recomendaciones de Conducta" ->
                    recommendationsConductFromJson(json);
            case "Requisitos de Salud" ->
                    healthRequirementsFromJson(json);
            case "Estudio de Morfologia" ->
                    morphologyStudyFromJson(json);
            default -> json; // si no se mapea, dejamos el string
        };
    }

    public void hydrateJsonValues(List<ClinicalHistoryDto> histories) {
        histories.forEach(history ->
                history.getExams().forEach(exam ->
                        exam.getDataValues().forEach(dataValue -> {
                            String json = (String) dataValue.getValue(); // raw del DB
                            Object parsed = parseDataValueJson(json, dataValue.getDataType().getName());
                            dataValue.setValue(parsed);
                        })
                )
        );
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
    // MorphologyStudy

    public String parseMorphologyStudyToJson(String rawText) {
        return morphologyStudyMapper.parseToJson(rawText);
    }

    public List<MorphologyStudyDto> morphologyStudyFromJson(String json) {
        return morphologyStudyMapper.fromJson(json);
    }

    public String morphologyStudyToJson(List<MorphologyStudyDto> dtos) {
        return morphologyStudyMapper.toJson(dtos);
    }

}
