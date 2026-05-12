# Use Case: Process Change Feed Batch

## Overview

**Use Case ID:** UC-102
**Use Case Name:** Process Change Feed Batch
**Primary Actor:** Outbox Change Feed Processor (system actor)
**Secondary Actors:** ChangeFeedItemBatchProcessor, Integration Event Publisher, Outbox Processed Marker, Integration Audit Logger
**Goal:** Cosmos Change Feed'den gelen bir doküman batch'ini filtreleyip yalnızca geçerli outbox event'lerini sıralı şekilde dispatch etmek.
**Status:** Draft

## Preconditions

- Outbox Change Feed Processor çalışır durumda (UC-101 başarıyla tamamlanmış).
- Cosmos Change Feed, `orders` container'ında bir veya daha fazla yeni/güncellenmiş doküman üretmiş.

## Main Success Scenario

1. Cosmos SDK, processor'ın handler'ını batch (`List<JsonNode>`) ile çağırır.
2. Processor, her JSON node'unu `ChangeFeedItem` DTO'suna map'ler (`_type` → `entityType`, `id`, `tenantId`, `orderId`, `eventType`, `payloadJson`, `processed`, `metadata`, `createdAtUtc`).
3. Processor, handler spec'i inşa eder: entity type = `"outbox"`, `isValidItem`, `shouldSkip = processed==true`, `handleItem = dispatch`.
4. `ChangeFeedItemBatchProcessor.process(items, spec)` çağrılır.
5. Batch processor her item için sırayla:
   - a. `entityType` filtresini uygular; eşleşmiyorsa atlar.
   - b. `isValidItem` true ise devam eder.
   - c. `shouldSkip` false ise devam eder.
   - d. `handleItem.accept(item)` çağrılır (dispatch akışı UC-103).
6. Tüm geçerli item'lar işlendiğinde handler döner; Cosmos SDK checkpoint kaydeder.

## Alternative Flows

### A1: Entity Type Outbox Değil

**Trigger:** `item.entityType != "outbox"` (örn. `order`) (adım 5a)
**Flow:**

1. Item atlanır; `handleItem` çağrılmaz.
2. Bir sonraki item ile devam edilir.

### A2: Geçersiz Item

**Trigger:** `tenantId == null` veya `id` boş (adım 5b)
**Flow:**

1. Batch processor WARN seviyesinde log yayımlar (`invalidItemLogMessage`).
2. Item atlanır; bir sonraki item ile devam.

### A3: Daha Önce İşlenmiş Item

**Trigger:** `item.processed == true` (adım 5c)
**Flow:**

1. Item atlanır (idempotency); `handleItem` çağrılmaz.
2. Bir sonraki item ile devam.

### A4: Dispatch Sırasında Exception

**Trigger:** `handleItem.accept(item)` exception fırlatır (adım 5d)
**Flow:**

1. Batch processor exception'ı yakalar ve error log'lar.
2. Exception Cosmos SDK'ya throw edilmez (checkpoint güncellenir).
3. At-least-once semantiği için item bir sonraki Change Feed turunda tekrar gelmez; ancak `processed=true` patch atılmadığı için bir sonraki restart'ta tekrar dispatch denenir.
4. Batch'in kalan item'ları işlenmeye devam eder.

## Postconditions

### Success Postconditions

- Batch'teki tüm geçerli outbox item'ları için dispatch akışı çalıştırıldı.
- Cosmos SDK checkpoint, batch'in continuation token'ını ilerletti.

### Failure Postconditions

- Tek item'da hata batch'in geri kalanını durdurmaz.
- Dispatch edilemeyen item'lar `processed=false` kalır; restart sonrası yeniden işlenmeye çalışılır.

## Business Rules

### BR-104: Sadece Outbox Tipini Dispatch Et

`order` tipindeki dokümanlar bu pipeline'da dispatch edilmez; processor sadece `_type == "outbox"` olanlarla ilgilenir.

### BR-105: Idempotent Skip

`processed=true` olan item asla yeniden dispatch edilmez.

### BR-106: Item İzolasyonu

Bir item'daki hata batch'in diğer item'larını etkilemez.