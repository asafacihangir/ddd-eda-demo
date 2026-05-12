package org.phoenix.demo.ordermanagement.application.orders.commands.createorder;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.phoenix.demo.domain.common.EntityId;
import org.phoenix.demo.domain.common.result.Result;
import org.phoenix.demo.ordermanagement.application.abstractions.repositories.OrderRepository;
import org.phoenix.demo.ordermanagement.domain.Order;

class CreateOrderCommandHandlerTest {

    @Test
    void handle_shouldPersistOrderAndReturnId_whenCommandIsValid() {
        InMemoryOrderRepository repository = new InMemoryOrderRepository();
        CreateOrderCommandHandler handler = new CreateOrderCommandHandler(repository);

        CreateOrderCommand command = new CreateOrderCommand(
            "tenant-1",
            "ORD-001",
            "CUST-42",
            new BigDecimal("100.00"),
            new BigDecimal("0.00"),
            new BigDecimal("18.00"),
            "USD"
        );

        Result<String, String> result = handler.handle(command);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue()).isNotBlank();
        assertThat(repository.added).hasSize(1);
        assertThat(repository.added.get(0).getOrderId()).isEqualTo("ORD-001");
        assertThat(repository.added.get(0).getTenantId()).isEqualTo("tenant-1");
    }

    @Test
    void handle_shouldReturnFailure_whenCurrencyCodeIsInvalid() {
        InMemoryOrderRepository repository = new InMemoryOrderRepository();
        CreateOrderCommandHandler handler = new CreateOrderCommandHandler(repository);

        CreateOrderCommand command = new CreateOrderCommand(
            "tenant-1",
            "ORD-002",
            "CUST-42",
            new BigDecimal("100.00"),
            new BigDecimal("0.00"),
            new BigDecimal("18.00"),
            "INVALID"
        );

        Result<String, String> result = handler.handle(command);

        assertThat(result.isFailure()).isTrue();
        assertThat(repository.added).isEmpty();
    }

    private static class InMemoryOrderRepository implements OrderRepository {
        final List<Order> added = new ArrayList<>();

        @Override
        public void add(Order order) {
            added.add(order);
        }

        @Override
        public Optional<Order> findById(EntityId<Order> id, String tenantId) {
            return added.stream()
                .filter(o -> o.getId().equals(id) && o.getTenantId().equals(tenantId))
                .findFirst();
        }

        @Override
        public void update(Order order) {
            // no-op for tests
        }
    }
}