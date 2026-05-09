package org.phoenix.demo.domain.common.result;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;

class ResultTest {

    @Test
    void success_exposesValueAndIsSuccess() {
        Result<Integer, String> r = Result.success(42);
        assertTrue(r.isSuccess());
        assertFalse(r.isFailure());
        assertEquals(42, r.getValue());
        assertThrows(NoSuchElementException.class, r::getError);
    }

    @Test
    void failure_exposesErrorAndIsFailure() {
        Result<Integer, String> r = Result.failure("boom");
        assertTrue(r.isFailure());
        assertFalse(r.isSuccess());
        assertEquals("boom", r.getError());
        assertThrows(NoSuchElementException.class, r::getValue);
    }

    @Test
    void map_transformsSuccessOnly() {
        Result<Integer, String> ok = Result.success(2);
        Result<Integer, String> doubled = ok.map(i -> i * 2);
        assertEquals(4, doubled.getValue());

        Result<Integer, String> fail = Result.failure("nope");
        Result<Integer, String> stillFail = fail.map(i -> i * 2);
        assertEquals("nope", stillFail.getError());
    }

    @Test
    void flatMap_chainsAndShortCircuitsOnFailure() {
        Result<Integer, String> r = Result.<Integer, String>success(2)
                .flatMap((Integer i) -> Result.<Integer, String>success(i + 1))
                .flatMap((Integer i) -> Result.<Integer, String>failure("stop"))
                .flatMap((Integer i) -> Result.<Integer, String>success(i + 100));
        assertTrue(r.isFailure());
        assertEquals("stop", r.getError());
    }

    @Test
    void failure_rejectsNullError() {
        assertThrows(NullPointerException.class, () -> Result.failure(null));
    }
}
