package com.example.PruebaPDFBox.utilities.Infrastructure.exception;

import com.example.PruebaPDFBox.utilities.Infrastructure.http.ResponseHttpApi;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleHttpMessageNotReadableException() {

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ResponseHttpApi.responseHttpError(
                        "El cuerpo de la petición es requerido o tiene formato inválido. ",
                        HttpStatus.BAD_REQUEST
                ));
    }

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<Map<String, Object>> handleCustomException(CustomException ex) {
        return ResponseEntity
                .status(ex.getStatus())
                .body(ResponseHttpApi.responseHttpError(ex.getMessage(), ex.getStatus()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(
            MethodArgumentNotValidException ex) {

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        Map<String, Object> response = ResponseHttpApi.responseHttpError(
                "Error de validación en los campos enviados",
                HttpStatus.BAD_REQUEST
        );
        response.put("errors", errors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ResponseHttpApi.responseHttpError(
                        "Error interno del servidor: " + ex.getMessage(),
                        HttpStatus.INTERNAL_SERVER_ERROR
                ));
    }
}