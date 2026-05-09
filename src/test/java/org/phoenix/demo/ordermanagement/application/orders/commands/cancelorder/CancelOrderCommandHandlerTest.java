package org.phoenix.demo.ordermanagement.application.orders.commands.cancelorder;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.phoenix.demo.domain.common.EntityId;
import org.phoenix.demo.domain.common.result.Result;
import org.phoenix.demo.domain.common.valueobject.Money;
import org.phoenix.demo.domain.common.valueobject.MoneyBreakdown;
import org.phoenix.demo.ordermanagement.application.abstractions.repositories.OrderRepository;
import org.phoenix.demo.ordermanagement.domain.Order;
import org.phoenix.demo.ordermanagement.domain.OrderStatus;

class CancelOrderCommandHandlerTest {

    @Test
    void handle_shouldCancelOrder_whenOrderExistsAndIsPlaced() {
        InMemoryOrderRepository repository = new InMemoryOrderRepository();
        Order order = newPlacedOrder();
        repository.add(order);
        CancelOrderCommandHandler handler = new CancelOrderCommandHandler(repository);

        Result<Void, String> result = handler.handle(
            new CancelOrderCommand(order.getId().value().toString()));

        assertThat(result.isSuccess()).isTrue();
        assertThat(repository.updated.get(order.getId()))
            .as("update should have been called")
            .isNotNull();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void handle_shouldFail_whenOrderIdIsNotUuid() {
        InMemoryOrderRepository repository = new InMemoryOrderRepository();
        CancelOrderCommandHandler handler = new CancelOrderCommandHandler(repository);

        Result<Void, String> result = handler.handle(new CancelOrderCommand("not-a-uuid"));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).contains("UUID");
    }

    @Test
    void handle_shouldFail_whenOrderNotFound() {
        InMemoryOrderRepository repository = new InMemoryOrderRepository();
        CancelOrderCommandHandler handler = new CancelOrderCommandHandler(repository);

        Result<Void, String> result = handler.handle(
            new CancelOrderCommand(UUID.randomUUID().toString()));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).contains("not found");
    }

    private static Order newPlacedOrder() {
        Money zero = Money.create(new BigDecimal("0.00"), "USD").getValue();
        Money subtotal = Money.create(new BigDecimal("100.00"), "USD").getValue();
        MoneyBreakdown pricing = MoneyBreakdown.create(subtotal, zero, zero).getValue();
        return Order.placeNew("ORD-CXL", "CUST-1", pricing).getValue();
    }

    private static class InMemoryOrderRepository implements OrderRepository {
        final Map<EntityId<Order>, Order> store = new HashMap<>();
        final Map<EntityId<Order>, Order> updated = new HashMap<>();

        @Override public void add(Order order) { store.put(order.getId(), order); }
        @Override public Optional<Order> findById(EntityId<Order> id) {
            return Optional.ofNullable(store.get(id));
        }
        @Override public void update(Order order) { updated.put(order.getId(), order); }
    }
}