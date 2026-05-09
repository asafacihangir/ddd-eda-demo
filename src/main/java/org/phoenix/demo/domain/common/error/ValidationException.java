package org.phoenix.demo.domain.common.error;

import java.util.List;
import java.util.Objects;


public final class ValidationException extends RuntimeException {

    private final List<String> errors;

    public ValidationException(List<String> errors) {
        super("Validation failed.");
        this.errors = List.copyOf(Objects.requireNonNull(errors, "errors"));
    }

    public List<String> getErrors() {
        return errors;
    }
}
