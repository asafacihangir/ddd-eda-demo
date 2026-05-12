package org.phoenix.demo.ordermanagement.infra.changefeed;

import com.azure.cosmos.ChangeFeedProcessor;
import com.azure.cosmos.ChangeFeedProcessorBuilder;
import com.azure.cosmos.CosmosAsyncClient;
import com.azure.cosmos.models.ChangeFeedProcessorOptions;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.phoenix.demo.ordermanagement.application.abstractions.IntegrationAuditLogger;
import org.phoenix.demo.ordermanagement.application.abstractions.IntegrationEventPublisher;
import org.phoenix.demo.ordermanagement.application.abstractions.OutboxProcessedMarker;
import org.phoenix.demo.ordermanagement.infra.cosmos.CosmosProperties;
import org.phoenix.demo.ordermanagement.infra.worker.WorkerComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.List;

@WorkerComponent
public class OutboxChangeFeedProcessor {

    private static final Logger log = LoggerFactory.getLogger(OutboxChangeFeedProcessor.class);

    private final CosmosAsyncClient client;
    private final CosmosProperties cosmos;
    private final OutboxChangeFeedOptions options;
    private final IntegrationEventPublisher publisher;
    private final OutboxProcessedMarker marker;
    private final IntegrationAuditLogger audit;

    private volatile ChangeFeedProcessor processor;

    public OutboxChangeFeedProcessor(
            @Qualifier("orderManagementCosmosAsyncClient") CosmosAsyncClient client,
            @Qualifier("orderManagementCosmosProperties") CosmosProperties cosmos,
            OutboxChangeFeedOptions options,
            IntegrationEventPublisher publisher,
            OutboxProcessedMarker marker,
            IntegrationAuditLogger audit) {
        this.client = client;
        this.cosmos = cosmos;
        this.options = options;
        this.publisher = publisher;
        this.marker = marker;
        this.audit = audit;
    }

    @PostConstruct
    public void start() {
        processor = new ChangeFeedProcessorBuilder()
                .hostName(options.getInstanceName())
                .feedContainer(client.getDatabase(cosmos.getDatabase()).getContainer(cosmos.getOrdersContainer()))
                .leaseContainer(client.getDatabase(cosmos.getDatabase()).getContainer(cosmos.getLeasesContainer()))
                .options(new ChangeFeedProcessorOptions()
                        .setLeasePrefix(options.getProcessorName() + ".")
                        .setFeedPollDelay(options.getFeedPollDelay())
                        .setMaxItemCount(options.getMaxItemsPerBatch()))
                .handleChanges(this::handleBatch)
                .buildChangeFeedProcessor();
        log.info("Starting OutboxChangeFeedProcessor processor={} instance={} db={} feed={} lease={}",
                options.getProcessorName(), options.getInstanceName(),
                cosmos.getDatabase(), cosmos.getOrdersContainer(), cosmos.getLeasesContainer());
        processor.start().block();
    }

    @PreDestroy
    public void stop() {
        if (processor != null) {
            try {
                processor.stop().block();
            } catch (RuntimeException ex) {
                log.warn("Error stopping ChangeFeedProcessor", ex);
            }
        }
    }

    private void handleBatch(List<JsonNode> docs) {
        for (JsonNode d : docs) {
            if (!"outbox".equals(text(d, "_type"))) continue;
            if (d.path("processed").asBoolean(false)) continue;

            String tenantId = text(d, "tenantId");
            String id = text(d, "id");
            if (tenantId == null || id == null || id.isBlank()) {
                log.warn("Skipping invalid outbox change-feed item id={} tenantId={}", id, tenantId);
                continue;
            }

            String orderId = text(d, "orderId");
            String eventType = text(d, "eventType");
            String payloadJson = text(d, "payloadJson");
            try {
                audit.logPublish(tenantId, orderId, id, eventType, payloadJson);
                publisher.publishOutbox(tenantId, id, orderId, eventType, payloadJson);
                marker.markProcessed(tenantId, id);
            } catch (RuntimeException ex) {
                log.error("Dispatch failed id={} tenantId={}; continuing", id, tenantId, ex);
            }
        }
    }

    private static String text(JsonNode node, String field) {
        return node.hasNonNull(field) ? node.get(field).asText() : null;
    }
}
