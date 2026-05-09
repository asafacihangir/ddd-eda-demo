package org.phoenix.demo.domain.common.valueobject;

import org.phoenix.demo.domain.common.error.DomainError;
import org.phoenix.demo.domain.common.result.Result;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;


public record Currency(String code) {

    private static final Pattern CODE_PATTERN = Pattern.compile("^[A-Z]{3}$");

    public Currency {
        Objects.requireNonNull(code, "code");
        code = code.trim().toUpperCase(Locale.ROOT);
        if (!CODE_PATTERN.matcher(code).matches()) {
            throw new IllegalArgumentException("Currency must be a 3-letter ISO code (e.g., USD, EUR).");
        }
    }

    public static final Currency USD = new Currency("USD");
    public static final Currency EUR = new Currency("EUR");
    public static final Currency GBP = new Currency("GBP");
    public static final Currency TRY = new Currency("TRY");

    public static Result<Currency, DomainError> create(String code) {
        if (code == null || code.isBlank()) {
            return Result.failure(DomainError.badRequest("Currency is required."));
        }
        try {
            return Result.success(new Currency(code));
        } catch (IllegalArgumentException ex) {
            return Result.failure(DomainError.badRequest(ex.getMessage()));
        }
    }

    @Override
    public String toString() {
        return code;
    }
}