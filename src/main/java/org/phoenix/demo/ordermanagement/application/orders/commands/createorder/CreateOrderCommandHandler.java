package org.phoenix.demo.ordermanagement.application.orders.commands.createorder;

import org.phoenix.demo.domain.common.error.DomainError;
import org.phoenix.demo.domain.common.result.Result;
import org.phoenix.demo.domain.common.valueobject.Money;
import org.phoenix.demo.domain.common.valueobject.MoneyBreakdown;
import org.phoenix.demo.shared.cqrs.CommandHandler;
import org.phoenix.demo.ordermanagement.application.abstractions.repositories.OrderRepository;
import org.phoenix.demo.ordermanagement.domain.Order;

public class CreateOrderCommandHandler
        implements CommandHandler<CreateOrderCommand, Result<String, String>> {

    private final OrderRepository orderRepository;

    public CreateOrderCommandHandler(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    public Result<String, String> handle(CreateOrderCommand command) {
        Result<Money, DomainError> subtotal =
            Money.create(command.subtotal(), command.currencyCode());
        if (subtotal.isFailure()) {
            return Result.failure(subtotal.getError().message());
        }

        Result<Money, DomainError> discount =
            Money.create(command.discount(), command.currencyCode());
        if (discount.isFailure()) {
            return Result.failure(discount.getError().message());
        }

        Result<Money, DomainError> tax =
            Money.create(command.tax(), command.currencyCode());
        if (tax.isFailure()) {
            return Result.failure(tax.getError().message());
        }

        Result<MoneyBreakdown, DomainError> pricing =
            MoneyBreakdown.create(subtotal.getValue(), discount.getValue(), tax.getValue());
        if (pricing.isFailure()) {
            return Result.failure(pricing.getError().message());
        }

        Result<Order, DomainError> order =
            Order.placeNew(command.tenantId(), command.orderId(), command.customerId(), pricing.getValue());
        if (order.isFailure()) {
            return Result.failure(order.getError().message());
        }

        orderRepository.add(order.getValue());
        return Result.success(order.getValue().getId().value().toString());
    }
}