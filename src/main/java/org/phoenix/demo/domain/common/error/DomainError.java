package org.phoenix.demo.domain.common.error;

import java.util.List;
import java.util.Objects;


public record DomainError(ErrorType type, String message, List<String> errors) {

    public DomainError {
        Objects.requireNonNull(type, "type");
        message = Objects.requireNonNullElse(message, type.defaultMessage());
        errors = errors == null ? List.of() : List.copyOf(errors);
    }

    public static DomainError conflict(String message) {
        return new DomainError(ErrorType.CONFLICT, message, List.of());
    }

    public static DomainError notFound(String message) {
        return new DomainError(ErrorType.NOT_FOUND, message, List.of());
    }

    public static DomainError badRequest(String message) {
        return new DomainError(ErrorType.BAD_REQUEST, message, List.of());
    }

    public static DomainError validation(String message, List<String> errors) {
        return new DomainError(ErrorType.VALIDATION, message, errors);
    }

    public static DomainError unexpected(String message) {
        return new DomainError(ErrorType.UNEXPECTED, message, List.of());
    }
}
