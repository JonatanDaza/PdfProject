package com.example.PruebaPDFBox.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "clinical_history")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ClinicalHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToMany(
            mappedBy = "clinicalHistory",
            cascade = CascadeType.MERGE
    )
    private List<Exam> exams;
}