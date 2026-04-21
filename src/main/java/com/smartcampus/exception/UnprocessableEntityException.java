package com.smartcampus.exception;

public class UnprocessableEntityException extends ApiException {
    public UnprocessableEntityException(String message) {
        super("UNPROCESSABLE_ENTITY", message);
    }
}

