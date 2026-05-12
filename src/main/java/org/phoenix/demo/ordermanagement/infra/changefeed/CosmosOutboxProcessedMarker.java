package org.phoenix.demo.ordermanagement.infra.changefeed;

import com.azure.cosmos.CosmosAsyncClient;
import com.azure.cosmos.CosmosAsyncContainer;
import com.azure.cosmos.models.CosmosPatchOperations;
import com.azure.cosmos.models.PartitionKey;
import org.phoenix.demo.ordermanagement.application.abstractions.OutboxProcessedMarker;
import org.phoenix.demo.ordermanagement.infra.cosmos.CosmosProperties;
import org.phoenix.demo.ordermanagement.infra.cosmos.OutboxCosmosDocument;
import org.phoenix.demo.ordermanagement.infra.worker.WorkerComponent;
import org.springframework.beans.factory.annotation.Qualifier;

@WorkerComponent
public class CosmosOutboxProcessedMarker implements OutboxProcessedMarker {

    private final CosmosAsyncContainer ordersContainer;

    public CosmosOutboxProcessedMarker(
            @Qualifier("orderManagementCosmosAsyncClient") CosmosAsyncClient client,
            @Qualifier("orderManagementCosmosProperties") CosmosProperties properties) {
        this.ordersContainer = client
                .getDatabase(properties.getDatabase())
                .getContainer(properties.getOrdersContainer());
    }

    @Override
    public void markProcessed(String tenantId, String outboxItemId) {
        CosmosPatchOperations ops = CosmosPatchOperations.create().set("/processed", true);
        ordersContainer.patchItem(outboxItemId, new PartitionKey(tenantId), ops, OutboxCosmosDocument.class).block();
    }
}
