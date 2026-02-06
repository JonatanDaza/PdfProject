package com.example.PruebaPDFBox.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Entity
@Table(name = "extract_pdf")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ExtractPdf {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Lob
    private String texto;

    private String CityAndFetch;
    private String TypeOfMedicalEvaluationOrProcedure;
    private String CompanyWhereYouWorkWillWorkOrWorked;
    private String EconomicActivity;
    private String CC;
    private String NameOfWorkerToApplicant;
    private String Gender;
    private String Age;
    private String MaritalStatus;
    private String EducationalLevel;
    private String EPS;
    private String AFP;
    private String ARl;
    private String BloodType;
    private String NoChildren;
    private String Income;
    private String WorkingHours;
    private String PositionArea;
    private String ProfessionOrPosition;
    private String FunctionsOfThePosition;

    @Lob
    @Column(length = 10000)
    private String imagePaths;

    @Transient
    private Integer totalImagenes;

    @Transient
    private List<String> imagenes;

}

