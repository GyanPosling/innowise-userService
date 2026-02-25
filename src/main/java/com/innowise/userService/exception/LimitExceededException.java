package com.innowise.userservice.exception;


public class LimitExceededException extends RuntimeException {
    public LimitExceededException(String message) {
        super(message);
    }
}