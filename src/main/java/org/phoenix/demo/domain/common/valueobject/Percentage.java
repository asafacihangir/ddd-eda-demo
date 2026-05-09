package org.phoenix.demo.domain.common.valueobject;

import org.phoenix.demo.domain.common.error.DomainError;
import org.phoenix.demo.domain.common.result.Result;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Objects;


public record Percentage(BigDecimal value) {

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    public Percentage {
        Objects.requireNonNull(value, "value");
        if (value.compareTo(ZERO) < 0 || value.compareTo(ONE_HUNDRED) > 0) {
            throw new IllegalArgumentException("Percentage must be between 0 and 100.");
        }
    }

    public static Result<Percentage, DomainError> create(BigDecimal value) {
        if (value == null) {
            return Result.failure(DomainError.badRequest("Percentage value is required."));
        }
        try {
            return Result.success(new Percentage(value));
        } catch (IllegalArgumentException ex) {
            return Result.failure(DomainError.badRequest(ex.getMessage()));
        }
    }

    public static Result<Percentage, DomainError> of(int value) {
        return create(BigDecimal.valueOf(value));
    }

    public BigDecimal toFactor() {
        return toFactor(MathContext.DECIMAL64);
    }

    public BigDecimal toFactor(MathContext mathContext) {
        Objects.requireNonNull(mathContext, "mathContext");
        return value.divide(ONE_HUNDRED, mathContext);
    }
}