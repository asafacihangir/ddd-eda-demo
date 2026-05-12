package org.phoenix.demo.ordermanagement.application.orders.commands.createorder;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import org.phoenix.demo.domain.common.result.Result;
import org.phoenix.demo.shared.cqrs.Command;

public record CreateOrderCommand(
        @NotBlank String tenantId,
        @NotBlank String orderId,
        @NotBlank String customerId,
        @NotNull @Positive BigDecimal subtotal,
        @NotNull BigDecimal discount,
        @NotNull BigDecimal tax,
        @NotBlank String currencyCode
) implements Command<Result<String, String>> {
}