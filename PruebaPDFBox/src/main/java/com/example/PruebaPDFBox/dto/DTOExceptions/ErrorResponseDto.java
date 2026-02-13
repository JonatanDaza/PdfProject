package com.example.PruebaPDFBox.dto.DTOExceptions;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponseDto {
    private int status;
    private String message;
    private LocalDateTime timestamp;
    private String path;
}