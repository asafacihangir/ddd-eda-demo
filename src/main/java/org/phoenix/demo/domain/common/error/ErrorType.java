package org.phoenix.demo.domain.common.error;


public enum ErrorType {
    CONFLICT("The data provided conflicts with existing data."),
    NOT_FOUND("The requested item could not be found."),
    BAD_REQUEST("Invalid request or parameters."),
    VALIDATION("Validation failed."),
    UNEXPECTED("Unexpected error happened.");

    private final String defaultMessage;

    ErrorType(String defaultMessage) {
        this.defaultMessage = defaultMessage;
    }

    public String defaultMessage() {
        return defaultMessage;
    }
}
