package org.phoenix.demo.ordermanagement.infra.changefeed;

import com.azure.cosmos.CosmosAsyncClient;
import com.azure.cosmos.CosmosAsyncContainer;
import com.azure.cosmos.models.CosmosItemRequestOptions;
import com.azure.cosmos.models.PartitionKey;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.mockito.ArgumentCaptor;
import org.phoenix.demo.ordermanagement.application.abstractions.IntegrationEventPublisher;
import org.phoenix.demo.ordermanagement.infra.cosmos.CosmosProperties;
import org.phoenix.demo.ordermanagement.infra.cosmos.OutboxCosmosDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@SpringBootTest
@TestPropertySource(properties = {"app.role=worker"})
@EnabledIfEnvironmentVariable(named = "COSMOS_ENDPOINT", matches = ".+")
class OutboxChangeFeedProcessorIntegrationTest {

    private static final String TENANT_ID = "tenant-cf-it";

    @MockitoBean
    private IntegrationEventPublisher publisher;

    @Autowired
    @Qualifier("orderManagementCosmosAsyncClient")
    private CosmosAsyncClient client;

    @Autowired
    @Qualifier("orderManagementCosmosProperties")
    private CosmosProperties properties;

    @Test
    void changefeed_dispatches_outbox_item_and_marks_processed() {
        CosmosAsyncContainer orders = client
                .getDatabase(properties.getDatabase())
                .getContainer(properties.getOrdersContainer());

        String outboxId = "outbox-cf-" + UUID.randomUUID();
        OutboxCosmosDocument doc = new OutboxCosmosDocument();
        doc.setId(outboxId);
        doc.setTenantId(TENANT_ID);
        doc.setOrderId("order-cf-" + UUID.randomUUID());
        doc.setCustomerId("cust-cf");
        doc.setEventType("OrderPlacedEvent");
        doc.setPayloadJson("{\"orderId\":\"x\"}");
        doc.setMetadata(new HashMap<>());
        doc.setProcessed(false);
        doc.setCreatedAtUtc(OffsetDateTime.now());

        orders.createItem(doc, new PartitionKey(TENANT_ID), new CosmosItemRequestOptions()).block();

        Awaitility.await()
                .atMost(Duration.ofSeconds(30))
                .pollInterval(Duration.ofSeconds(1))
                .untilAsserted(() -> verify(publisher, atLeastOnce())
                        .publishOutbox(any(), any(), any(), any(), any()));

        ArgumentCaptor<String> outboxIdCaptor = ArgumentCaptor.forClass(String.class);
        verify(publisher, atLeastOnce()).publishOutbox(
                any(), outboxIdCaptor.capture(), any(), any(), any());
        assertThat(outboxIdCaptor.getAllValues()).contains(outboxId);

        Awaitility.await()
                .atMost(Duration.ofSeconds(15))
                .pollInterval(Duration.ofSeconds(1))
                .untilAsserted(() -> {
                    OutboxCosmosDocument reloaded = orders
                            .readItem(outboxId, new PartitionKey(TENANT_ID), OutboxCosmosDocument.class)
                            .block()
                            .getItem();
                    assertThat(reloaded.isProcessed()).isTrue();
                    assertThat(reloaded.getStatus()).isEqualTo("PUBLISHED");
                    assertThat(reloaded.getPublishedAt()).isNotNull();
                });
    }
}
