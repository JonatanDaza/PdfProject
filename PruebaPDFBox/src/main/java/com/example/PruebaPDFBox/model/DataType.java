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