package org.phoenix.demo.ordermanagement.infra.servicebus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.azure.spring.messaging.servicebus.core.ServiceBusTemplate;
import com.azure.spring.messaging.servicebus.support.ServiceBusMessageHeaders;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import reactor.core.publisher.Mono;

class ServiceBusIntegrationEventPublisherTest {

    private ServiceBusTemplate template;
    private ServiceBusPublisherOptions options;
    private ServiceBusIntegrationEventPublisher publisher;

    @BeforeEach
    void setUp() {
        template = mock(ServiceBusTemplate.class);
        options = new ServiceBusPublisherOptions();
        options.setTopicName("order-events");
        options.setEnabled(true);
        publisher = new ServiceBusIntegrationEventPublisher(template, options);
        publisher.init();

        when(template.sendAsync(eq("order-events"), org.mockito.ArgumentMatchers.<Message<?>>any()))
                .thenReturn(Mono.empty());
    }

    @Test
    void publishOutbox_setsHeadersAndApplicationProperties() {
        publisher.publishOutbox(
                "tenant-1",
                "outbox-1",
                "ORD-1",
                "OrderPlacedIntegrationEvent",
                "{\"orderId\":\"ORD-1\"}");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Message<byte[]>> captor = ArgumentCaptor.forClass(Message.class);
        verify(template).sendAsync(eq("order-events"), captor.capture());

        Message<byte[]> sent = captor.getValue();
        MessageHeaders headers = sent.getHeaders();

        assertThat(headers.get(MessageHeaders.CONTENT_TYPE)).isEqualTo("application/json");
        assertThat(headers.get(ServiceBusMessageHeaders.SUBJECT)).isEqualTo("OrderPlacedIntegrationEvent");
        assertThat(headers.get(ServiceBusMessageHeaders.MESSAGE_ID)).isEqualTo("outbox-1");
        assertThat(headers.get(ServiceBusMessageHeaders.CORRELATION_ID)).isEqualTo("ORD-1");
        assertThat(headers.get("tenantId")).isEqualTo("tenant-1");
        assertThat(headers.get("orderId")).isEqualTo("ORD-1");
        assertThat(headers.get("outboxItemId")).isEqualTo("outbox-1");
        assertThat(headers.get("eventType")).isEqualTo("OrderPlacedIntegrationEvent");

        assertThat(new String(sent.getPayload(), StandardCharsets.UTF_8))
                .isEqualTo("{\"orderId\":\"ORD-1\"}");
    }

    @Test
    void publishOutbox_emptyPayload_defaultsToEmptyJsonObject() {
        publisher.publishOutbox("t", "o", "ORD", "Evt", "");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Message<byte[]>> captor = ArgumentCaptor.forClass(Message.class);
        verify(template).sendAsync(eq("order-events"), captor.capture());

        assertThat(new String(captor.getValue().getPayload(), StandardCharsets.UTF_8))
                .isEqualTo("{}");
    }
}
