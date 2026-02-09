package com.example.PruebaPDFBox.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DataValueDto {

    private Long id;
    private String value;
    private DataTypeDto dataType;
    private ExamDto exam;
}
