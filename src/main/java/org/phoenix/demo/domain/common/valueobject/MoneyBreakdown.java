package org.phoenix.demo.domain.common.valueobject;

import java.math.BigDecimal;
import java.util.Objects;

import org.phoenix.demo.domain.common.error.DomainError;
import org.phoenix.demo.domain.common.result.Result;


public record MoneyBreakdown(Money subtotal, Money discount, Money tax, Money total) {

    public MoneyBreakdown {
        Objects.requireNonNull(subtotal, "subtotal");
        Objects.requireNonNull(discount, "discount");
        Objects.requireNonNull(tax, "tax");
        Objects.requireNonNull(total, "total");
    }

    public static Result<MoneyBreakdown, DomainError> create(Money subtotal, Money discount, Money tax) {
        Objects.requireNonNull(subtotal, "subtotal");
        Objects.requireNonNull(discount, "discount");
        Objects.requireNonNull(tax, "tax");

        if (discount.amount().compareTo(subtotal.amount()) > 0) {
            return Result.failure(DomainError.badRequest("Discount cannot be greater than subtotal."));
        }

        return subtotal.subtract(discount)
                .flatMap(afterDiscount -> afterDiscount.add(tax))
                .map(total -> new MoneyBreakdown(subtotal, discount, tax, total));
    }

    public static Result<MoneyBreakdown, DomainError> fromTotal(Money total) {
        Objects.requireNonNull(total, "total");
        return Money.create(BigDecimal.ZERO, total.currency())
                .map(zero -> new MoneyBreakdown(total, zero, zero, total));
    }
}