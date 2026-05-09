package org.phoenix.demo.ordermanagement.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class OrderStatusTest {

    @Test
    void codes_match_csharp_smartenum_values() {
        assertEquals(0, OrderStatus.PLACED.code());
        assertEquals(1, OrderStatus.SHIPPED.code());
        assertEquals(2, OrderStatus.CANCELLED.code());
    }

    @Test
    void valueOf_returns_constant_by_name() {
        assertEquals(OrderStatus.PLACED, OrderStatus.valueOf("PLACED"));
        assertEquals(OrderStatus.SHIPPED, OrderStatus.valueOf("SHIPPED"));
        assertEquals(OrderStatus.CANCELLED, OrderStatus.valueOf("CANCELLED"));
    }
}