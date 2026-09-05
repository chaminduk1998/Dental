package com.dental.util;

/** Thrown by the business layer when user input fails validation (HTTP 400). */
public class ValidationException extends RuntimeException {

    public ValidationException(String message) {
        super(message);
    }
}
