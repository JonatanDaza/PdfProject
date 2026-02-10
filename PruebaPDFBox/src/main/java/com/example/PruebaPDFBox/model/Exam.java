package com.example.PruebaPDFBox.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Set;

@Entity
@Table(name = "exam")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Exam {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "exam_name", nullable = false)
    private String examName;

    @Column(name = "exam_date", nullable = false)
    private String examDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clinical_history_id", nullable = false)
    @JsonBackReference
    private ClinicalHistory clinicalHistory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "worker_id", nullable = false)
    @JsonBackReference
    private Worker worker;

    @OneToMany(mappedBy = "exam", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private Set<DataValue> dataValues;
}