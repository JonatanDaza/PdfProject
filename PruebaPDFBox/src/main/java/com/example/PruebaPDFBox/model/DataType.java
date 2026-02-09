package com.example.PruebaPDFBox.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Entity
@Table(name = "data_type")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DataType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @OneToMany(mappedBy = "dataType", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<DataValue> dataValues;
}

//    private String Year;
//    private String WorkAccident;
//    private String OccupationalDiseases;
//    private String ManualActivities;
//    private String PhysicalActivity;
//    private String SmokingHabits;
//    private String AlcoholHabits;
//    private String BloodPressure;
//    private String HeartRate;
//    private String RespiratoryRate;
//    private String DominantLaterality;
//    private String AbdominalPerimeter;
//    private String Weight;
//    private String Size;
//    private String IMC;
//    private String Diagnostics;
//    private String OccupationalConcept;
//    private String RightEar;
//    private String LeftEar;
//    private String ExamCode;
//
//    //Data from Laboratory
//    private String Hematocrit;
//    private String Hemoglobin;
//    private String WhiteBloodCells;
//    private String PlateletCount;
//    private String Glycemia;
//    private String CholesterolTotal;
//    private String CholesterolHDL;
//    private String CholesterolLDL;
//    private String Triglycerides;
//
//    //Data from Spirometry
//    private String Interpretation;
//
//    //Data saved as JSON strings
//    @Column(name = "health_requirements", columnDefinition = "TEXT")
//    @Convert(converter = JsonStringConverter.class)
//    private String HealthRequirements;
//
//    @Column(name = "observations", columnDefinition = "TEXT")
//    @Convert(converter = JsonStringConverter.class)
//    private String Observations;
//
//    @Column(name = "recommendations", columnDefinition = "TEXT")
//    @Convert(converter = JsonStringConverter.class)
//    private String Recommendations;
//
//    @Column(name ="recommendations_conduct", columnDefinition = "TEXT")
//    @Convert(converter = JsonStringConverter.class)
//    private String RecommendationsConduct;
//
//    @Column(name = "weighted_decibels", columnDefinition = "TEXT")
//    @Convert(converter = JsonStringConverter.class)
//    private String WeightedDecibels;
//}