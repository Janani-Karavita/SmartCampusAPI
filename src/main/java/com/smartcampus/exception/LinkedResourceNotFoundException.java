package com.smartcampus.exception;

public class LinkedResourceNotFoundException extends ApiException {
    public LinkedResourceNotFoundException(String message) {
        super("LINKED_RESOURCE_NOT_FOUND", message);
    } 
}
