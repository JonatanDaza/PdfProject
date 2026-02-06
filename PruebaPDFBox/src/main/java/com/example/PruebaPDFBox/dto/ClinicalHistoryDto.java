package com.example.PruebaPDFBox.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ClinicalHistoryDto {
    private Long id;
    private List<ExamDto> exams;
}
