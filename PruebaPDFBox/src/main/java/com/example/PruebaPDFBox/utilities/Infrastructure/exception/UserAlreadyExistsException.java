package com.example.PruebaPDFBox.utilities.Infrastructure.exception;

public class UserAlreadyExistsException extends RuntimeException {
    public UserAlreadyExistsException(String message) {
        super(message);
    }
}
