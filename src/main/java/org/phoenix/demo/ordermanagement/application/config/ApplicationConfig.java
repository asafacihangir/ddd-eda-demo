package org.phoenix.demo.ordermanagement.application.config;

import org.phoenix.demo.ordermanagement.application.abstractions.repositories.OrderRepository;
import org.phoenix.demo.ordermanagement.application.orders.commands.cancelorder.CancelOrderCommandHandler;
import org.phoenix.demo.ordermanagement.application.orders.commands.createorder.CreateOrderCommandHandler;
import org.phoenix.demo.ordermanagement.application.orders.queries.getorderbyid.GetOrderByIdQueryHandler;
import org.phoenix.demo.shared.cqrs.RequestHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationConfig {

    @Bean
    public RequestHandler<?, ?> createOrderCommandHandler(OrderRepository orderRepository) {
        return new CreateOrderCommandHandler(orderRepository);
    }

    @Bean
    public RequestHandler<?, ?> cancelOrderCommandHandler(OrderRepository orderRepository) {
        return new CancelOrderCommandHandler(orderRepository);
    }

    @Bean
    public RequestHandler<?, ?> getOrderByIdQueryHandler(OrderRepository orderRepository) {
        return new GetOrderByIdQueryHandler(orderRepository);
    }
}