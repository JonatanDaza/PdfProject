package com.example.PruebaPDFBox.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DataExamDto {
    private Long id;
    private String Year;
    private String WorkAccident;
    private String OccupationalDiseases;
    private String ManualActivities;
    private String PhysicalActivity;
    private String SmokingHabits;
    private String AlcoholHabits;
    private String BloodPressure;
    private String HeartRate;
    private String RespiratoryRate;
    private String DominantLaterality;
    private String AbdominalPerimeter;
    private String Weight;
    private String Size;
    private String IMC;
    private String Diagnostics;
    private String OccupationalConcept;
    private String RightEar;
    private String LeftEar;
    private String ExamCode;

    // Campos de Laboratorio
    private String Hematocrit;
    private String Hemoglobin;
    private String WhiteBloodCells;
    private String PlateletCount;
    private String Glycemia;
    private String CholesterolTotal;
    private String CholesterolHDL;
    private String CholesterolLDL;
    private String Triglycerides;

    private String Interpretation;

    private List<HealthRequirementDto> HealthRequirements;
    private List<ObservationsDto> Observations;
    private List<RecommendationDto> Recommendations;
    private List<RecommendationsConductDto> RecommendationsConduct;
    private List<WeightedDecibelsDto> WeightedDecibels;
}
