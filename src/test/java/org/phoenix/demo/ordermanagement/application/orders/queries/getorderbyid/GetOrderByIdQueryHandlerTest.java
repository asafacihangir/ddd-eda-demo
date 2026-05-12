package org.phoenix.demo.ordermanagement.application.orders.queries.getorderbyid;

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
import org.phoenix.demo.ordermanagement.application.orders.queries.OrderSummaryDto;
import org.phoenix.demo.ordermanagement.domain.Order;

class GetOrderByIdQueryHandlerTest {

    private static final String TENANT_ID = "tenant-1";

    @Test
    void handle_shouldReturnDto_whenOrderExists() {
        InMemoryOrderRepository repository = new InMemoryOrderRepository();
        Order order = newPlacedOrder();
        repository.add(order);
        GetOrderByIdQueryHandler handler = new GetOrderByIdQueryHandler(repository);

        Result<OrderSummaryDto, String> result =
            handler.handle(new GetOrderByIdQuery(TENANT_ID, order.getId().value().toString()));

        assertThat(result.isSuccess()).isTrue();
        OrderSummaryDto dto = result.getValue();
        assertThat(dto.tenantId()).isEqualTo(TENANT_ID);
        assertThat(dto.orderId()).isEqualTo("ORD-Q1");
        assertThat(dto.customerId()).isEqualTo("CUST-1");
        assertThat(dto.status()).isEqualTo("PLACED");
        assertThat(dto.currency()).isEqualTo("USD");
        assertThat(dto.total()).isEqualByComparingTo("100.00");
    }

    @Test
    void handle_shouldFail_whenIdIsNotUuid() {
        GetOrderByIdQueryHandler handler =
            new GetOrderByIdQueryHandler(new InMemoryOrderRepository());

        Result<OrderSummaryDto, String> result =
            handler.handle(new GetOrderByIdQuery(TENANT_ID, "not-a-uuid"));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).contains("UUID");
    }

    @Test
    void handle_shouldFail_whenOrderNotFound() {
        GetOrderByIdQueryHandler handler =
            new GetOrderByIdQueryHandler(new InMemoryOrderRepository());

        Result<OrderSummaryDto, String> result =
            handler.handle(new GetOrderByIdQuery(TENANT_ID, UUID.randomUUID().toString()));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).contains("not found");
    }

    private static Order newPlacedOrder() {
        Money zero = Money.create(new BigDecimal("0.00"), "USD").getValue();
        Money subtotal = Money.create(new BigDecimal("100.00"), "USD").getValue();
        MoneyBreakdown pricing = MoneyBreakdown.create(subtotal, zero, zero).getValue();
        return Order.placeNew(TENANT_ID, "ORD-Q1", "CUST-1", pricing).getValue();
    }

    private static class InMemoryOrderRepository implements OrderRepository {
        final Map<EntityId<Order>, Order> store = new HashMap<>();

        @Override public void add(Order order) { store.put(order.getId(), order); }
        @Override public Optional<Order> findById(EntityId<Order> id, String tenantId) {
            Order order = store.get(id);
            if (order == null || !order.getTenantId().equals(tenantId)) return Optional.empty();
            return Optional.of(order);
        }
        @Override public void update(Order order) { store.put(order.getId(), order); }
    }
}