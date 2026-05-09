package org.phoenix.demo.ordermanagement.application.behaviours;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.phoenix.demo.ordermanagement.application.abstractions.ApplicationValidationException;
import org.phoenix.demo.ordermanagement.application.abstractions.cqrs.Command;
import org.phoenix.demo.ordermanagement.application.abstractions.cqrs.Next;

class ValidationPipelineBehaviorTest {

    private final Validator validator =
        Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void handle_shouldProceed_whenRequestIsValid() {
        ValidationPipelineBehavior<SampleCommand, String> behavior =
            new ValidationPipelineBehavior<>(validator);
        Next<String> next = () -> "OK";

        String result = behavior.handle(new SampleCommand("hello"), next);

        assertThat(result).isEqualTo("OK");
    }

    @Test
    void handle_shouldThrow_whenRequestIsInvalid() {
        ValidationPipelineBehavior<SampleCommand, String> behavior =
            new ValidationPipelineBehavior<>(validator);
        boolean[] nextCalled = {false};
        Next<String> next = () -> {
            nextCalled[0] = true;
            return "OK";
        };

        assertThatThrownBy(() -> behavior.handle(new SampleCommand(""), next))
            .isInstanceOf(ApplicationValidationException.class)
            .hasMessageContaining("name");

        assertThat(nextCalled[0])
            .as("next should not be invoked when validation fails")
            .isFalse();
    }

    private record SampleCommand(@NotBlank String name) implements Command<String> { }
}