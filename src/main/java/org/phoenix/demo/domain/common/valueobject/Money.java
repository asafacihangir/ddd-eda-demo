package org.phoenix.demo.domain.common.valueobject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

import org.phoenix.demo.domain.common.error.DomainError;
import org.phoenix.demo.domain.common.result.Result;


public record Money(BigDecimal amount, Currency currency) implements Comparable<Money> {

    private static final int DEFAULT_SCALE = 2;
    private static final RoundingMode DEFAULT_ROUNDING = RoundingMode.HALF_EVEN;

    public Money {
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(currency, "currency");
    }

    public static Result<Money, DomainError> create(BigDecimal amount, String currencyCode) {
        Result<BigDecimal, DomainError> validatedAmount = validateAmount(amount);
        if (validatedAmount.isFailure()) {
            return Result.failure(validatedAmount.getError());
        }
        return Currency.create(currencyCode).map(c -> new Money(validatedAmount.getValue(), c));
    }

    public static Result<Money, DomainError> create(BigDecimal amount, Currency currency) {
        if (currency == null) {
            return Result.failure(DomainError.badRequest("Currency must be non-null."));
        }
        return validateAmount(amount).map(validAmount -> new Money(validAmount, currency));
    }


    public Result<Money, DomainError> add(Money other) {
        Result<Money, DomainError> guard = requireSameCurrency(other);
        if (guard.isFailure()) {
            return guard;
        }
        return Result.success(new Money(amount.add(other.amount), currency));
    }

    public Result<Money, DomainError> subtract(Money other) {
        Result<Money, DomainError> guard = requireSameCurrency(other);
        if (guard.isFailure()) {
            return guard;
        }
        BigDecimal result = amount.subtract(other.amount);
        if (result.signum() < 0) {
            return Result.failure(DomainError.badRequest("Money result cannot be negative."));
        }
        return Result.success(new Money(result, currency));
    }

    public Result<Money, DomainError> multiply(BigDecimal factor) {
        if (factor == null || factor.signum() < 0) {
            return Result.failure(DomainError.badRequest("Factor must be greater than or equal to 0."));
        }
        BigDecimal scaled = amount.multiply(factor).setScale(DEFAULT_SCALE, DEFAULT_ROUNDING);
        return Result.success(new Money(scaled, currency));
    }


    @Override
    public int compareTo(Money other) {
        Objects.requireNonNull(other, "other");
        if (!currency.equals(other.currency)) {
            throw new IllegalArgumentException(
                    "Cannot compare Money with different currencies: "
                            + currency.code() + " vs " + other.currency.code());
        }
        return amount.compareTo(other.amount);
    }

    @Override
    public String toString() {
        return amount.toPlainString() + " " + currency.code();
    }

    private static Result<BigDecimal, DomainError> validateAmount(BigDecimal amount) {
        if (amount == null || amount.signum() < 0) {
            return Result.failure(DomainError.badRequest("Amount must be greater than or equal to 0."));
        }
        return Result.success(amount);
    }

    private Result<Money, DomainError> requireSameCurrency(Money other) {
        if (other == null) {
            return Result.failure(DomainError.badRequest("Other money must be non-null."));
        }
        if (!currency.equals(other.currency)) {
            return Result.failure(DomainError.badRequest(
                    "Currency mismatch: " + currency.code() + " vs " + other.currency.code() + "."));
        }
        return Result.success(this);
    }
}