package com.smartcampus.exception;

public class BadRequestException extends ApiException {
    public BadRequestException(String message) {
        super("BAD_REQUEST", message);
    }
}