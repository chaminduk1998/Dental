package com.dental.dao;

/** Unchecked wrapper so the service tier never has to handle raw {@code SQLException}. */
public class DataAccessException extends RuntimeException {

    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
