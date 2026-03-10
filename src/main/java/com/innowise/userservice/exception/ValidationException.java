package com.innowise.userservice.exception;

import java.io.Serial;

public class ValidationException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = -8768645971750474833L;

    public ValidationException() {
        super("Validation failed");
    }

    public ValidationException(String message) {
        super(message);
    }

    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
