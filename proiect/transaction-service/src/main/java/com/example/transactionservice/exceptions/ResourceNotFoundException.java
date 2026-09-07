package com.example.transactionservice.exceptions;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException() {
        super("Resursa nu a fost gasita");
    }

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
