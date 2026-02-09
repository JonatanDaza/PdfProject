package com.example.PruebaPDFBox.dto;

import lombok.*;
import java.util.List;

@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ExamDto {
    private Long id;
    private String examName;
    private String examDate;
    private Long clinicalHistoryId;
    private List<DataValueDto> dataValues;
    private WorkerDto worker;
}

