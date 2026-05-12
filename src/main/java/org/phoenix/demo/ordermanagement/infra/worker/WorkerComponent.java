package org.phoenix.demo.ordermanagement.infra.worker;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Component
@ConditionalOnExpression("'${app.role:both}' == 'worker' or '${app.role:both}' == 'both'")
public @interface WorkerComponent {
}
