package org.phoenix.demo.domain.common.valueobject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.phoenix.demo.domain.common.error.DomainError;
import org.phoenix.demo.domain.common.error.ErrorType;
import org.phoenix.demo.domain.common.result.Result;

class MoneyTest {

    private static Money ten(Currency c) {
        return Money.create(new BigDecimal("10"), c).getValue();
    }

    @Test
    void create_rejectsNegativeAmount() {
        Result<Money, DomainError> r = Money.create(new BigDecimal("-1"), Currency.USD);
        assertTrue(r.isFailure());
        assertEquals(ErrorType.BAD_REQUEST, r.getError().type());
    }

    @Test
    void create_acceptsZero() {
        Result<Money, DomainError> r = Money.create(BigDecimal.ZERO, Currency.USD);
        assertTrue(r.isSuccess());
    }

    @Test
    void create_withCurrencyCode_propagatesCurrencyError() {
        Result<Money, DomainError> r = Money.create(BigDecimal.ONE, "X");
        assertTrue(r.isFailure());
    }

    @Test
    void add_succeedsForSameCurrency() {
        Money a = ten(Currency.USD);
        Money b = ten(Currency.USD);
        Result<Money, DomainError> sum = a.add(b);
        assertTrue(sum.isSuccess());
        assertEquals(0, new BigDecimal("20").compareTo(sum.getValue().amount()));
    }

    @Test
    void add_failsOnCurrencyMismatch() {
        assertTrue(ten(Currency.USD).add(ten(Currency.EUR)).isFailure());
    }

    @Test
    void subtract_failsWhenResultNegative() {
        Money small = Money.create(new BigDecimal("3"), Currency.USD).getValue();
        Money big = Money.create(new BigDecimal("5"), Currency.USD).getValue();
        assertTrue(small.subtract(big).isFailure());
    }

    @Test
    void subtract_failsOnCurrencyMismatch() {
        assertTrue(ten(Currency.USD).subtract(ten(Currency.EUR)).isFailure());
    }

    @Test
    void multiply_rejectsNegativeFactor() {
        assertTrue(ten(Currency.USD).multiply(new BigDecimal("-1")).isFailure());
    }

    @Test
    void multiply_returnsScaledMoney() {
        Money result = ten(Currency.USD).multiply(new BigDecimal("3")).getValue();
        assertEquals(0, new BigDecimal("30").compareTo(result.amount()));
    }
}
