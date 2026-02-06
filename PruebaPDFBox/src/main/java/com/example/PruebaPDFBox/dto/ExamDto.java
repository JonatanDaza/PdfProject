package com.example.PruebaPDFBox.dto;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ExamDto {
    private Long id;
    private String examName;
    private String examDate;
    private Long clinicalHistoryId;
    private DataExamDto dataExam;
    private WorkerDto worker;
}
