# Architecture

```mermaid
flowchart LR
  Client([HTTP Client]):::ext

  subgraph API["API Layer (api/)"]
    OC[OrderController]
    AEH[ApiExceptionHandler]
  end

  subgraph APP["Application Layer (application/)"]
    direction TB
    DISP[RequestDispatcher<br/>CQRS + Validation Pipeline]
    CMD[Command Handlers<br/>CreateOrder, CancelOrder]
    QRY[Query Handlers<br/>GetOrderById]
    MAP[OrderIntegrationEventMapper<br/>OutboxMapper]
    PORTS[/Ports / Abstractions<br/>OrderRepository<br/>IntegrationEventPublisher<br/>OutboxProcessedMarker/]
  end

  subgraph DOM["Domain Layer (domain/)"]
    AGG[Order Aggregate]
    EVT[Domain Events<br/>OrderPlaced/Cancelled/Shipped]
  end

  subgraph INFRA["Infrastructure Layer (infra/)"]
    direction TB
    subgraph COSMOS["cosmos/"]
      REPO[CosmosOrderRepository<br/>SpringDataOrderRepository]
      DOCS[OrderCosmosDocument<br/>OutboxCosmosDocument]
      SER[JacksonOutboxPayloadSerializer]
    end
    subgraph CF["changefeed/"]
      CFP[OutboxChangeFeedProcessor]
      MARK[CosmosOutboxProcessedMarker]
      LOG[LoggingIntegrationEventPublisher]
    end
    subgraph SB["servicebus/"]
      PUB[ServiceBusIntegrationEventPublisher]
      CONS[OrderEventsConsumer]
    end
    WRK[WorkerComponent<br/>role: api / worker / both]
  end

  subgraph AZ["Azure"]
    CDB[(Cosmos DB<br/>orders + leases)]
    SBUS{{Service Bus Topic<br/>order-events}}
  end

  Client -->|REST| OC
  OC --> DISP
  DISP --> CMD
  DISP --> QRY
  CMD --> AGG
  AGG --> EVT
  CMD --> PORTS
  QRY --> PORTS
  PORTS -.implements.-> REPO
  REPO --> DOCS
  REPO -->|atomic write:<br/>order + outbox| CDB

  CDB -.change feed.-> CFP
  CFP --> SER
  CFP --> PUB
  CFP --> MARK
  MARK -->|mark dispatched| CDB
  PUB -->|publish event| SBUS
  LOG -.dev fallback.-> CFP

  SBUS -->|subscribe| CONS
  WRK -.activates.-> CFP
  WRK -.activates.-> CONS

  classDef ext fill:#eee,stroke:#666
  classDef azure fill:#cfe2ff,stroke:#0d6efd
  class CDB,SBUS azure
```

## Flow

- **Write path:** `OrderController` → CQRS dispatcher → command handler → `Order` aggregate → `CosmosOrderRepository` writes the order document and outbox record to Cosmos DB in a single transactional batch.
- **Outbox dispatch:** `OutboxChangeFeedProcessor` reads the Cosmos Change Feed (coordinated via the `leases` container) → `IntegrationEventPublisher` publishes to the Service Bus topic `order-events` → `CosmosOutboxProcessedMarker` marks the outbox record as dispatched.
- **Consume:** `OrderEventsConsumer` subscribes to the Service Bus topic.
- **Roles:** `WorkerComponent` activates the change feed processor and/or consumer based on the `APP_ROLE` profile (`api` / `worker` / `both`).