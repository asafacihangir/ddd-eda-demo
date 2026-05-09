package org.phoenix.demo.domain.common.valueobject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.phoenix.demo.domain.common.error.DomainError;
import org.phoenix.demo.domain.common.error.ErrorType;
import org.phoenix.demo.domain.common.result.Result;

class CurrencyTest {

    @Test
    void create_acceptsThreeLetterUppercase() {
        Result<Currency, DomainError> r = Currency.create("USD");
        assertTrue(r.isSuccess());
        assertEquals("USD", r.getValue().code());
    }

    @Test
    void create_normalizesLowercase() {
        Result<Currency, DomainError> r = Currency.create("usd");
        assertTrue(r.isSuccess());
        assertEquals("USD", r.getValue().code());
    }

    @Test
    void create_trimsWhitespace() {
        Result<Currency, DomainError> r = Currency.create("  eur  ");
        assertTrue(r.isSuccess());
        assertEquals("EUR", r.getValue().code());
    }

    @Test
    void create_rejectsTooShort() {
        Result<Currency, DomainError> r = Currency.create("US");
        assertTrue(r.isFailure());
        assertEquals(ErrorType.BAD_REQUEST, r.getError().type());
    }

    @Test
    void create_rejectsBlank() {
        assertTrue(Currency.create("").isFailure());
        assertTrue(Currency.create("   ").isFailure());
    }

    @Test
    void create_rejectsNull() {
        assertTrue(Currency.create(null).isFailure());
    }

    @Test
    void create_rejectsDigits() {
        assertTrue(Currency.create("US1").isFailure());
    }

    @Test
    void presets_haveExpectedCodes() {
        assertEquals("USD", Currency.USD.code());
        assertEquals("EUR", Currency.EUR.code());
        assertEquals("GBP", Currency.GBP.code());
    }
}
