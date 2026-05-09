package org.phoenix.demo.ordermanagement.infra.cqrs;

import jakarta.validation.Validator;
import java.util.List;
import org.phoenix.demo.shared.cqrs.PipelineBehavior;
import org.phoenix.demo.shared.cqrs.RequestDispatcher;
import org.phoenix.demo.shared.cqrs.RequestHandler;
import org.phoenix.demo.shared.validation.ValidationPipelineBehavior;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CqrsConfiguration {

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