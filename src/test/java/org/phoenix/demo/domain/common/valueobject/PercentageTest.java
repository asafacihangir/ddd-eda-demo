package org.phoenix.demo.domain.common.valueobject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.phoenix.demo.domain.common.error.DomainError;
import org.phoenix.demo.domain.common.result.Result;

class PercentageTest {

    @Test
    void create_acceptsZero() {
        assertTrue(Percentage.create(BigDecimal.ZERO).isSuccess());
    }

    @Test
    void create_acceptsOneHundred() {
        assertTrue(Percentage.create(new BigDecimal("100")).isSuccess());
    }

    @Test
    void create_rejectsNegative() {
        Result<Percentage, DomainError> r = Percentage.create(new BigDecimal("-0.01"));
        assertTrue(r.isFailure());
    }

    @Test
    void create_rejectsAboveOneHundred() {
        assertTrue(Percentage.create(new BigDecimal("100.01")).isFailure());
    }

    @Test
    void toFactor_dividesByOneHundred() {
        Percentage p = Percentage.create(new BigDecimal("25")).getValue();
        assertEquals(0, new BigDecimal("0.25").compareTo(p.toFactor()));
    }
}
