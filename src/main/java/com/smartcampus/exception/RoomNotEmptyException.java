package com.smartcampus.exception;

public class RoomNotEmptyException extends ApiException {
    public RoomNotEmptyException(String message) {
        super("ROOM_NOT_EMPTY", message);
    }  
}