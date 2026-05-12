package org.phoenix.demo.ordermanagement.application.orders.commands.cancelorder;

import jakarta.validation.constraints.NotBlank;
import org.phoenix.demo.domain.common.result.Result;
import org.phoenix.demo.shared.cqrs.Command;

public record CancelOrderCommand(
        @NotBlank String tenantId,
        @NotBlank String orderId
) implements Command<Result<Void, String>> {
}