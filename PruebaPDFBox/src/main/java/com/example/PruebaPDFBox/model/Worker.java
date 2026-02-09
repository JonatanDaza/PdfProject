package com.example.PruebaPDFBox.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "worker")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Worker {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cc", nullable = false, unique = true)
    private String CC;
    private String name;
    private String Gender;
    private String Age;
    private String MaritalStatus;
    private String EducationalLevel;

    @OneToMany(mappedBy = "worker")
    @JsonBackReference
    private List<Exam> exams = new ArrayList<>();
}