package com.example.PruebaPDFBox.utilities.Infrastructure.exception;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(String message) {
        super(message);
    }
}
