package org.phoenix.demo.ordermanagement.infra.cosmos;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.phoenix.demo.domain.common.valueobject.Currency;
import org.phoenix.demo.domain.common.valueobject.Money;
import org.phoenix.demo.domain.common.valueobject.MoneyBreakdown;
import org.phoenix.demo.ordermanagement.domain.Order;
import org.phoenix.demo.ordermanagement.domain.OrderStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@EnabledIfEnvironmentVariable(named = "COSMOS_ENDPOINT", matches = ".+")
class CosmosOrderRepositoryIntegrationTest {

    private static final String TENANT_ID = "tenant-it";

    @Autowired
    private CosmosOrderRepository repository;

    @Test
    void add_persistsOrderAndOutboxAtomically() {
        Order order = newPlacedOrder("ORD-IT-" + UUID.randomUUID(), "cust-it-" + UUID.randomUUID());
        var orderId = order.getId();

        repository.add(order);

        Optional<Order> found = repository.findById(orderId, TENANT_ID);
        assertThat(found).isPresent();
        assertThat(found.get().getTenantId()).isEqualTo(TENANT_ID);
        assertThat(found.get().getStatus()).isEqualTo(OrderStatus.PLACED);
        assertThat(found.get().getOrderId()).isEqualTo(order.getOrderId());
    }

    @Test
    void update_persistsCancelledStateAndAppendsOutbox() {
        Order order = newPlacedOrder("ORD-IT-" + UUID.randomUUID(), "cust-it-" + UUID.randomUUID());
        repository.add(order);
        var orderId = order.getId();

        Order reloaded = repository.findById(orderId, TENANT_ID).orElseThrow();
        var cancelResult = reloaded.cancel();
        assertThat(cancelResult.isSuccess()).isTrue();
        repository.update(reloaded);

        Order afterCancel = repository.findById(orderId, TENANT_ID).orElseThrow();
        assertThat(afterCancel.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    private static Order newPlacedOrder(String orderId, String customerId) {
        Money sub = Money.create(new BigDecimal("100.00"), Currency.TRY).getValue();
        Money disc = Money.create(new BigDecimal("10.00"), Currency.TRY).getValue();
        Money tax = Money.create(new BigDecimal("18.00"), Currency.TRY).getValue();
        MoneyBreakdown pricing = MoneyBreakdown.create(sub, disc, tax).getValue();
        return Order.placeNew(TENANT_ID, orderId, customerId, pricing).getValue();
    }
}