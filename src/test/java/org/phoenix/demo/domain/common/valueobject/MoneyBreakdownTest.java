package org.phoenix.demo.domain.common.valueobject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.phoenix.demo.domain.common.error.DomainError;
import org.phoenix.demo.domain.common.result.Result;

class MoneyBreakdownTest {

    private static Money usd(String amount) {
        return Money.create(new BigDecimal(amount), Currency.USD).getValue();
    }

    @Test
    void create_computesTotalAsSubtotalMinusDiscountPlusTax() {
        Result<MoneyBreakdown, DomainError> r =
                MoneyBreakdown.create(usd("100"), usd("10"), usd("5"));
        assertTrue(r.isSuccess());
        assertEquals(0, new BigDecimal("95").compareTo(r.getValue().total().amount()));
    }

    @Test
    void create_failsOnDiscountGreaterThanSubtotal() {
        Result<MoneyBreakdown, DomainError> r =
                MoneyBreakdown.create(usd("10"), usd("20"), usd("0"));
        assertTrue(r.isFailure());
    }

    @Test
    void create_failsOnCurrencyMismatch() {
        Money eur = Money.create(BigDecimal.ONE, Currency.EUR).getValue();
        Result<MoneyBreakdown, DomainError> r =
                MoneyBreakdown.create(usd("100"), eur, usd("0"));
        assertTrue(r.isFailure());
    }

    @Test
    void fromTotal_setsZeroDiscountAndTax() {
        Result<MoneyBreakdown, DomainError> r = MoneyBreakdown.fromTotal(usd("50"));
        assertTrue(r.isSuccess());
        MoneyBreakdown b = r.getValue();
        assertEquals(0, BigDecimal.ZERO.compareTo(b.discount().amount()));
        assertEquals(0, BigDecimal.ZERO.compareTo(b.tax().amount()));
        assertEquals(0, new BigDecimal("50").compareTo(b.total().amount()));
        assertEquals(0, new BigDecimal("50").compareTo(b.subtotal().amount()));
    }
}
