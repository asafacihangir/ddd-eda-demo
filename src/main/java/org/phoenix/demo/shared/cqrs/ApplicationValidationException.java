package org.phoenix.demo.shared.cqrs;

import java.util.List;

public class ApplicationValidationException extends RuntimeException {

    private final List<String> errors;

    public ApplicationValidationException(List<String> errors) {
        super("Request validation failed: " + String.join("; ", errors));
        this.errors = List.copyOf(errors);
    }

    public List<String> getErrors() {
        return errors;
    }
}