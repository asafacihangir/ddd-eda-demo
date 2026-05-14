package org.phoenix.demo.ordermanagement.infra.servicebus;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.BiConsumer;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
        "app.role=worker",
        "app.servicebus.publisher.enabled=true",
        "app.servicebus.consumer.enabled=true"
})
@EnabledIfEnvironmentVariable(named = "SERVICEBUS_CONNECTION_STRING", matches = ".+")
class ServiceBusIntegrationEventPublisherIntegrationTest {

    private static final ConcurrentLinkedQueue<String> RECEIVED_OUTBOX_IDS = new ConcurrentLinkedQueue<>();

    @Autowired
    private ServiceBusIntegrationEventPublisher publisher;

    @Test
    void publish_messageIsConsumedBySubscriber() {
        String outboxId = "it-" + UUID.randomUUID();
        String orderId = "ORD-" + UUID.randomUUID();

        publisher.publishOutbox(
                "tenant-it",
                outboxId,
                orderId,
                "OrderPlacedIntegrationEvent",
                "{\"orderId\":\"" + orderId + "\"}");

        Awaitility.await()
                .atMost(Duration.ofSeconds(30))
                .pollInterval(Duration.ofSeconds(1))
                .untilAsserted(() -> assertThat(RECEIVED_OUTBOX_IDS).contains(outboxId));
    }

    @TestConfiguration
    static class CaptureConfig {

        @Bean
        BiConsumer<String, byte[]> capturingObserver() {
            return (outboxItemId, payload) -> {
                if (outboxItemId != null) {
                    RECEIVED_OUTBOX_IDS.add(outboxItemId);
                }
            };
        }
    }
}
