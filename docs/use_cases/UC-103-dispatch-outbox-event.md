# Use Case: Dispatch Outbox Event

## Overview

**Use Case ID:** UC-103
**Use Case Name:** Dispatch Outbox Event (publish + mark processed)
**Primary Actor:** Outbox Change Feed Processor (system actor)
**Secondary Actors:** Integration Audit Logger, Integration Event Publisher (LoggingIntegrationEventPublisher), Outbox Processed Marker (CosmosOutboxProcessedMarker), Cosmos DB `orders` container
**Goal:** Geçerli bir outbox item'ını entegrasyon kanalına (Faz 2'de log) publish edip Cosmos'taki outbox dokümanını `processed=true` olarak işaretlemek.
**Status:** Draft

## Preconditions

- Item, UC-102'deki tüm filtrelerden geçti (entity type = `outbox`, valid, `processed=false`).
- `IntegrationEventPublisher`, `OutboxProcessedMarker`, `IntegrationAuditLogger` bean'leri context'te mevcut.

## Main Success Scenario

1. Processor `dispatch(item)` metodunu çağırır.
2. `IntegrationAuditLogger.logPublish(tenantId, orderId, outboxItemId, eventType, payloadJson)` çalışır (Faz 2'de no-op).
3. `IntegrationEventPublisher.publishOutbox(tenantId, outboxItemId, orderId, eventType, payloadJson)` çağrılır.
4. `LoggingIntegrationEventPublisher`, SLF4J INFO log yayımlar: `tenantId`, `orderId`, `outboxItemId`, `eventType`, payload length.
5. `OutboxProcessedMarker.markProcessed(tenantId, outboxItemId)` çağrılır.
6. `CosmosOutboxProcessedMarker`, Cosmos Async SDK üzerinden `patchItem` ile `/processed` alanını `true` yapar (partition key: `tenantId`).
7. Patch işlemi `block()` ile tamamlanır; dispatch akışı sonlanır.

## Alternative Flows

### A1: Service Bus Publisher Mevcut

**Trigger:** Faz 3'te `IntegrationEventPublisher` bean'i Service Bus impl. olarak override edildi (adım 3 öncesi)
**Flow:**

1. `@ConditionalOnMissingBean` nedeniyle `LoggingIntegrationEventPublisher` devre dışı.
2. Publish gerçek Service Bus topic'e gönderilir.
3. Use case adım 5'ten devam.

### A2: Patch Sırasında 404 (Item Bulunamadı)

**Trigger:** Outbox dokümanı silindi veya farklı partition key (adım 6)
**Flow:**

1. Cosmos SDK `CosmosException(404)` fırlatır.
2. Exception batch processor'a yükselir (UC-102 A4 kapsamına girer).
3. Batch processor exception'ı log'lar ve bir sonraki item ile devam eder.

### A3: Patch Sırasında Pre-condition Failure / 412

**Trigger:** Concurrent başka bir worker zaten `processed=true` yaptı (adım 6)
**Flow:**

1. SDK exception fırlatır.
2. Lease coordination tek instance dispatch'i garantilese de exception loglanır.
3. Use case sonlanır; item zaten dispatch edilmiş kabul edilir.

### A4: Publisher Exception Fırlatır

**Trigger:** `publishOutbox` exception (adım 3)
**Flow:**

1. Exception batch processor'a yükselir.
2. `markProcessed` çağrılmaz; doküman `processed=false` kalır.
3. Bir sonraki restart'ta Change Feed başlangıçtan/checkpoint'ten item'ı tekrar getirir ve dispatch yeniden denenir.

## Postconditions

### Success Postconditions

- Entegrasyon event'i log'a (veya Service Bus'a) yayımlandı.
- Cosmos `orders` container'da outbox dokümanı `processed=true` durumunda.
- Audit log kaydı oluştu (Faz 2 no-op, Faz 4 Cosmos versiyonu).

### Failure Postconditions

- `publishOutbox` başarısızsa `processed` alanı değişmez; at-least-once delivery korunur.
- Aynı event birden fazla kez yayımlanmış olabilir; tüketici idempotency kuralları (event ID = `outboxItemId`) ile başa çıkar.

## Business Rules

### BR-107: At-Least-Once Delivery

Publish başarılı olmadan `processed=true` yazılmaz. Publish başarılı olup mark adımı patlarsa, restart sonrası event tekrar yayımlanabilir; tüketici idempotency tarafında çözer.

### BR-108: Event ID Olarak Outbox Item ID

`outboxItemId` (Cosmos doc id) idempotency anahtarıdır; publisher ve tüketici bu değeri kullanmalıdır.

### BR-109: Partition Key = tenantId

Outbox dokümanları `tenantId` partition key'i ile yazıldığı için patch ve okuma işlemlerinde aynı partition key kullanılır.

### BR-110: Audit Log Önce, Publish Sonra

Audit log publish'ten önce yazılır; publish başarısız olsa bile audit izi kalır.