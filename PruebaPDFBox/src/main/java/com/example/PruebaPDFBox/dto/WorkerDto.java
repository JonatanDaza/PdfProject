package com.example.PruebaPDFBox.dto;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class WorkerDto {
    private Long id;
    private String CC;
    private String Gender;
    private String Age;
    private String MaritalStatus;
    private String EducationalLevel;
}
