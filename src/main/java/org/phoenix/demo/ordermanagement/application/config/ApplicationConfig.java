package org.phoenix.demo.ordermanagement.application.config;

import jakarta.validation.Validator;
import java.util.List;
import org.phoenix.demo.ordermanagement.application.abstractions.cqrs.PipelineBehavior;
import org.phoenix.demo.ordermanagement.application.abstractions.cqrs.RequestDispatcher;
import org.phoenix.demo.ordermanagement.application.abstractions.cqrs.RequestHandler;
import org.phoenix.demo.ordermanagement.application.abstractions.repositories.OrderRepository;
import org.phoenix.demo.ordermanagement.application.behaviours.ValidationPipelineBehavior;
import org.phoenix.demo.ordermanagement.application.dispatcher.SpringRequestDispatcher;
import org.phoenix.demo.ordermanagement.application.orders.commands.cancelorder.CancelOrderCommandHandler;
import org.phoenix.demo.ordermanagement.application.orders.commands.createorder.CreateOrderCommandHandler;
import org.phoenix.demo.ordermanagement.application.orders.queries.getorderbyid.GetOrderByIdQueryHandler;
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

    @Bean
    public PipelineBehavior<?, ?> validationPipelineBehavior(Validator validator) {
        return new ValidationPipelineBehavior<>(validator);
    }

    @Bean
    public RequestDispatcher requestDispatcher(List<RequestHandler<?, ?>> handlers,
                                               List<PipelineBehavior<?, ?>> behaviors) {
        return new SpringRequestDispatcher(handlers, behaviors);
    }
}