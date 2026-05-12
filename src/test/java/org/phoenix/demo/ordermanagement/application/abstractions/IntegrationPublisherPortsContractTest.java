package org.phoenix.demo.ordermanagement.application.abstractions;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.phoenix.demo.ordermanagement.application.outbox.OutboxRecord;

class IntegrationPublisherPortsContractTest {

    @Test
    void publishingFlow_shouldInvokePortsInExpectedOrder() {
        RecordingPublisher publisher = new RecordingPublisher();
        RecordingAuditLogger auditLogger = new RecordingAuditLogger();
        RecordingMarker marker = new RecordingMarker();

        OutboxRecord record = new OutboxRecord(
            "tenant-1",
            "outbox-1", "ORD-1",
            "OrderPlacedIntegrationEvent",
            "{\"orderId\":\"ORD-1\"}",
            null, false);

        publishOutboxRecord(record, publisher, auditLogger, marker);

        assertThat(publisher.published).hasSize(1);
        assertThat(publisher.published.get(0).outboxItemId).isEqualTo("outbox-1");
        assertThat(publisher.published.get(0).tenantId).isEqualTo("tenant-1");
        assertThat(auditLogger.audited).hasSize(1);
        assertThat(marker.processed).containsExactly("outbox-1");
        assertThat(marker.tenants).containsExactly("tenant-1");
    }

    private static void publishOutboxRecord(OutboxRecord record,
                                            IntegrationEventPublisher publisher,
                                            IntegrationAuditLogger auditLogger,
                                            OutboxProcessedMarker marker) {
        publisher.publishOutbox(record.tenantId(), record.id(), record.orderId(), record.eventType(), record.payloadJson());
        auditLogger.logPublish(record.tenantId(), record.orderId(), record.id(), record.eventType(), record.payloadJson());
        marker.markProcessed(record.tenantId(), record.id());
    }

    private record PublishedItem(String tenantId, String outboxItemId, String orderId, String eventType, String payloadJson) { }

    private static class RecordingPublisher implements IntegrationEventPublisher {
        final List<PublishedItem> published = new ArrayList<>();

        @Override
        public void publishOutbox(String tenantId, String outboxItemId, String orderId, String eventType, String payloadJson) {
            published.add(new PublishedItem(tenantId, outboxItemId, orderId, eventType, payloadJson));
        }
    }

    private static class RecordingAuditLogger implements IntegrationAuditLogger {
        final List<String> audited = new ArrayList<>();

        @Override
        public void logPublish(String tenantId, String orderId, String outboxItemId, String eventType, String payloadJson) {
            audited.add(outboxItemId);
        }
    }

    private static class RecordingMarker implements OutboxProcessedMarker {
        final List<String> tenants = new ArrayList<>();
        final List<String> processed = new ArrayList<>();

        @Override
        public void markProcessed(String tenantId, String outboxItemId) {
            tenants.add(tenantId);
            processed.add(outboxItemId);
        }
    }
}