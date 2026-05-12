package org.phoenix.demo.ordermanagement.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public record PlaceOrderRequest(
        @NotBlank String tenantId,
        @NotBlank String orderId,
        @NotBlank String customerId,
        @NotNull @PositiveOrZero BigDecimal subtotalAmount,
        @NotNull @PositiveOrZero BigDecimal discountAmount,
        @NotNull @PositiveOrZero BigDecimal taxAmount,
        @NotBlank String currencyCode
) {
}