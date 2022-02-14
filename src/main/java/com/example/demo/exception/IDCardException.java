package com.example.demo.exception;

public class IDCardException extends RuntimeException {
    public IDCardException() { }

    public IDCardException(String message) {
        super(message);
    }

    public IDCardException(String message, Throwable cause) {
        super(message, cause);
    }
}