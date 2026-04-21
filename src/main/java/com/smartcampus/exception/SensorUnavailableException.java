package com.smartcampus.exception;
/**
 * Exception thrown when a sensor is unavailable.
 */
public class SensorUnavailableException extends ApiException {

    public SensorUnavailableException(String message) {
        super("FORBIDDEN", message);
    }
}