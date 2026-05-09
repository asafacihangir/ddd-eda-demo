package org.phoenix.demo.shared.validation;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.List;
import java.util.Set;
import org.phoenix.demo.shared.cqrs.ApplicationValidationException;
import org.phoenix.demo.shared.cqrs.Next;
import org.phoenix.demo.shared.cqrs.PipelineBehavior;
import org.phoenix.demo.shared.cqrs.Request;

public class ValidationPipelineBehavior<REQ extends Request<R>, R>
        implements PipelineBehavior<REQ, R> {

    private final Validator validator;

    public ValidationPipelineBehavior(Validator validator) {
        this.validator = validator;
    }

    @Override
    public R handle(REQ request, Next<R> next) {
        Set<ConstraintViolation<REQ>> violations = validator.validate(request);

        if (!violations.isEmpty()) {
            List<String> errors = violations.stream()
                .map(v -> v.getPropertyPath() + " " + v.getMessage())
                .sorted()
                .toList();
            throw new ApplicationValidationException(errors);
        }

        return next.proceed();
    }
}