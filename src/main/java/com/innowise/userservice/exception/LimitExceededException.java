package com.innowise.userservice.exception;

import java.io.Serial;

public class LimitExceededException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 7523876459512577554L;

    public LimitExceededException() {
        super("Limit exceeded");
    }

    public LimitExceededException(String message) {
        super(message);
    }

    public LimitExceededException(String message, Throwable cause) {
        super(message, cause);
    }
}
