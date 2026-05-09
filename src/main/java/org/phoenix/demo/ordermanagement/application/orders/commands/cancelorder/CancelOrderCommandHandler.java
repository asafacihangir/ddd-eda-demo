package org.phoenix.demo.ordermanagement.application.orders.commands.cancelorder;

import java.util.Optional;
import java.util.UUID;
import org.phoenix.demo.domain.common.EntityId;
import org.phoenix.demo.domain.common.error.DomainError;
import org.phoenix.demo.domain.common.result.Result;
import org.phoenix.demo.ordermanagement.application.abstractions.cqrs.CommandHandler;
import org.phoenix.demo.ordermanagement.application.abstractions.repositories.OrderRepository;
import org.phoenix.demo.ordermanagement.domain.Order;

public class CancelOrderCommandHandler
        implements CommandHandler<CancelOrderCommand, Result<Void, String>> {

    private final OrderRepository orderRepository;

    public CancelOrderCommandHandler(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    public Result<Void, String> handle(CancelOrderCommand command) {
        EntityId<Order> id;
        try {
            id = EntityId.of(UUID.fromString(command.orderId()));
        } catch (IllegalArgumentException ex) {
            return Result.failure("orderId is not a valid UUID");
        }

        Optional<Order> found = orderRepository.findById(id);
        if (found.isEmpty()) {
            return Result.failure("Order not found: " + command.orderId());
        }

        Order order = found.get();
        Result<Void, DomainError> cancelResult = order.cancel();
        if (cancelResult.isFailure()) {
            return Result.failure(cancelResult.getError().message());
        }

        orderRepository.update(order);
        return Result.success(null);
    }
}