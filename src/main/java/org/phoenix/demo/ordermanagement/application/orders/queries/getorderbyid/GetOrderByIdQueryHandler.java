package org.phoenix.demo.ordermanagement.application.orders.queries.getorderbyid;

import java.util.Optional;
import java.util.UUID;
import org.phoenix.demo.domain.common.EntityId;
import org.phoenix.demo.domain.common.result.Result;
import org.phoenix.demo.shared.cqrs.QueryHandler;
import org.phoenix.demo.ordermanagement.application.abstractions.repositories.OrderRepository;
import org.phoenix.demo.ordermanagement.application.orders.queries.OrderSummaryDto;
import org.phoenix.demo.ordermanagement.domain.Order;

public class GetOrderByIdQueryHandler
        implements QueryHandler<GetOrderByIdQuery, Result<OrderSummaryDto, String>> {

    private final OrderRepository orderRepository;

    public GetOrderByIdQueryHandler(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    public Result<OrderSummaryDto, String> handle(GetOrderByIdQuery query) {
        EntityId<Order> id;
        try {
            id = EntityId.of(UUID.fromString(query.id()));
        } catch (IllegalArgumentException ex) {
            return Result.failure("id is not a valid UUID");
        }

        Optional<Order> found = orderRepository.findById(id);
        if (found.isEmpty()) {
            return Result.failure("Order not found: " + query.id());
        }

        return Result.success(toDto(found.get()));
    }

    private static OrderSummaryDto toDto(Order order) {
        return new OrderSummaryDto(
            order.getId().value().toString(),
            order.getOrderId(),
            order.getCustomerId(),
            order.getStatus().name(),
            order.getPricing().total().amount(),
            order.getPricing().total().currency().code(),
            order.getCreatedAtUtc(),
            order.getLastModifiedAtUtc()
        );
    }
}