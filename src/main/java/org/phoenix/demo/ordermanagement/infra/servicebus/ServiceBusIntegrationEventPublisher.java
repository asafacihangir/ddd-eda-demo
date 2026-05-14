package org.phoenix.demo.ordermanagement.infra.servicebus;

import com.azure.spring.cloud.service.servicebus.properties.ServiceBusEntityType;
import com.azure.spring.messaging.servicebus.core.ServiceBusTemplate;
import com.azure.spring.messaging.servicebus.support.ServiceBusMessageHeaders;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import org.phoenix.demo.ordermanagement.application.abstractions.IntegrationEventPublisher;
import org.phoenix.demo.ordermanagement.infra.worker.WorkerComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;

@WorkerComponent
@ConditionalOnProperty(name = "app.servicebus.publisher.enabled", havingValue = "true")
public class ServiceBusIntegrationEventPublisher implements IntegrationEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(ServiceBusIntegrationEventPublisher.class);

    private final ServiceBusTemplate template;
    private final ServiceBusPublisherOptions options;

    public ServiceBusIntegrationEventPublisher(ServiceBusTemplate template, ServiceBusPublisherOptions options) {
        this.template = template;
        this.options = options;
    }

    @PostConstruct
    void init() {
        template.setDefaultEntityType(ServiceBusEntityType.TOPIC);
    }

    @Override
    public void publishOutbox(String tenantId,
                              String outboxItemId,
                              String orderId,
                              String eventType,
                              String payloadJson) {
        byte[] body = (payloadJson == null || payloadJson.isEmpty() ? "{}" : payloadJson)
                .getBytes(StandardCharsets.UTF_8);

        Message<byte[]> message = MessageBuilder.withPayload(body)
                .setHeader(MessageHeaders.CONTENT_TYPE, "application/json")
                .setHeader(ServiceBusMessageHeaders.SUBJECT, eventType)
                .setHeader(ServiceBusMessageHeaders.MESSAGE_ID, outboxItemId)
                .setHeader(ServiceBusMessageHeaders.CORRELATION_ID, orderId)
                .setHeader("tenantId", tenantId)
                .setHeader("orderId", orderId)
                .setHeader("outboxItemId", outboxItemId)
                .setHeader("eventType", eventType)
                .build();

        template.sendAsync(options.getTopicName(), message).block();
        log.debug("Published outbox {} type {} tenant {} to topic {}",
                outboxItemId, eventType, tenantId, options.getTopicName());
    }
}