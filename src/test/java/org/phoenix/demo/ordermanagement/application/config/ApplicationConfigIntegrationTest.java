package org.phoenix.demo.ordermanagement.application.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.phoenix.demo.domain.common.EntityId;
import org.phoenix.demo.domain.common.result.Result;
import org.phoenix.demo.ordermanagement.application.abstractions.repositories.OrderRepository;
import org.phoenix.demo.ordermanagement.application.orders.commands.createorder.CreateOrderCommand;
import org.phoenix.demo.ordermanagement.domain.Order;
import org.phoenix.demo.ordermanagement.infra.cqrs.CqrsConfiguration;
import org.phoenix.demo.shared.cqrs.ApplicationValidationException;
import org.phoenix.demo.shared.cqrs.RequestDispatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig
@Import({ApplicationConfig.class, CqrsConfiguration.class})
class ApplicationConfigIntegrationTest {

    @Autowired RequestDispatcher dispatcher;
    @Autowired RecordingOrderRepository repository;

    @Test
    void dispatch_shouldRunHandler_whenCommandIsValid() {
        CreateOrderCommand command = new CreateOrderCommand(
            "ORD-100", "CUST-1",
            new BigDecimal("50.00"), new BigDecimal("0.00"), new BigDecimal("9.00"),
            "USD"
        );

        Result<String, String> result = dispatcher.dispatch(command);

        assertThat(result.isSuccess()).isTrue();
        assertThat(repository.added).hasSize(1);
    }

    @Test
    void dispatch_shouldRejectViaValidationBehavior_beforeHandler() {
        CreateOrderCommand invalid = new CreateOrderCommand(
            "", "CUST-1",
            new BigDecimal("50.00"), new BigDecimal("0.00"), new BigDecimal("9.00"),
            "USD"
        );

        assertThatThrownBy(() -> dispatcher.dispatch(invalid))
            .isInstanceOf(ApplicationValidationException.class);

        assertThat(repository.added)
            .as("handler must not be reached when validation fails")
            .isEmpty();
    }

    @Configuration
    static class TestConfig {

        @Bean
        Validator validator() {
            return Validation.buildDefaultValidatorFactory().getValidator();
        }

        @Bean
        RecordingOrderRepository orderRepository() {
            return new RecordingOrderRepository();
        }
    }

    static class RecordingOrderRepository implements OrderRepository {
        final List<Order> added = new ArrayList<>();

        @Override public void add(Order order) { added.add(order); }
        @Override public Optional<Order> findById(EntityId<Order> id) { return Optional.empty(); }
        @Override public void update(Order order) { }
    }
}