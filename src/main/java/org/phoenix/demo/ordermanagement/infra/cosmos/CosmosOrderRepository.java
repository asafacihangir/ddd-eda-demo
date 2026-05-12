package org.phoenix.demo.ordermanagement.infra.cosmos;

import com.azure.cosmos.CosmosAsyncClient;
import com.azure.cosmos.CosmosAsyncContainer;
import com.azure.cosmos.models.CosmosBatch;
import com.azure.cosmos.models.CosmosBatchResponse;
import com.azure.cosmos.models.PartitionKey;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.phoenix.demo.domain.common.EntityId;
import org.phoenix.demo.domain.common.event.DomainEvent;
import org.phoenix.demo.ordermanagement.application.abstractions.repositories.OrderRepository;
import org.phoenix.demo.ordermanagement.application.outbox.OutboxMapper;
import org.phoenix.demo.ordermanagement.application.outbox.OutboxRecord;
import org.phoenix.demo.ordermanagement.domain.Order;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

@Repository
public class CosmosOrderRepository implements OrderRepository {

    private final CosmosAsyncContainer container;
    private final SpringDataOrderRepository springDataRepository;
    private final OrderDocumentMapper documentMapper;
    private final OutboxMapper outboxMapper;

    public CosmosOrderRepository(
            CosmosAsyncClient cosmosAsyncClient,
            @Qualifier("orderManagementCosmosProperties") CosmosProperties properties,
            SpringDataOrderRepository springDataRepository,
            OrderDocumentMapper documentMapper,
            OutboxMapper outboxMapper) {
        this.container = cosmosAsyncClient
                .getDatabase(properties.getDatabase())
                .getContainer(properties.getOrdersContainer());
        this.springDataRepository = springDataRepository;
        this.documentMapper = documentMapper;
        this.outboxMapper = outboxMapper;
    }

    @Override
    public void add(Order order) {
        OrderCosmosDocument orderDoc = documentMapper.toDocument(order);
        List<OutboxCosmosDocument> outboxDocs = buildOutboxDocs(order, orderDoc.getCustomerId());

        CosmosBatch batch = CosmosBatch.createCosmosBatch(new PartitionKey(orderDoc.getTenantId()));
        batch.createItemOperation(orderDoc);
        outboxDocs.forEach(batch::createItemOperation);

        executeBatch(batch);
    }

    @Override
    public Optional<Order> findById(EntityId<Order> id, String tenantId) {
        return springDataRepository.findOrderByIdAndTenantId(id.value().toString(), tenantId)
                .stream()
                .findFirst()
                .map(documentMapper::toAggregate);
    }

    @Override
    public void update(Order order) {
        OrderCosmosDocument orderDoc = documentMapper.toDocument(order);
        List<OutboxCosmosDocument> outboxDocs = buildOutboxDocs(order, orderDoc.getCustomerId());

        CosmosBatch batch = CosmosBatch.createCosmosBatch(new PartitionKey(orderDoc.getTenantId()));
        batch.replaceItemOperation(orderDoc.getId(), orderDoc);
        outboxDocs.forEach(batch::createItemOperation);

        executeBatch(batch);
    }

    private List<OutboxCosmosDocument> buildOutboxDocs(Order order, String customerId) {
        List<DomainEvent> events = order.popDomainEvents();
        List<OutboxRecord> records = outboxMapper.toOutboxRecords(events, order.getTenantId(), order.getOrderId());
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        return records.stream()
                .map(r -> documentMapper.toOutboxDocument(r, customerId, now))
                .toList();
    }

    private void executeBatch(CosmosBatch batch) {
        CosmosBatchResponse response = container.executeCosmosBatch(batch).block();
        if (response == null || !response.isSuccessStatusCode()) {
            int statusCode = response == null ? -1 : response.getStatusCode();
            String errorMessage = response == null ? "no response" : response.getErrorMessage();
            throw new IllegalStateException(
                    "Cosmos batch failed: status=" + statusCode + ", message=" + errorMessage);
        }
    }
}