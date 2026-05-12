# Use Case: Start Outbox Change Feed Processor

## Overview

**Use Case ID:** UC-101
**Use Case Name:** Start Outbox Change Feed Processor
**Primary Actor:** Spring Boot Application Container (system actor)
**Secondary Actors:** Cosmos DB `orders` container, Cosmos DB `leases` container
**Goal:** Worker rolünde başlatılan uygulama, Cosmos Change Feed Processor'ı ayağa kaldırıp `orders` container'ındaki değişiklikleri dinlemeye başlasın.
**Status:** Draft

## Preconditions

- Cosmos DB hesabı erişilebilir; `COSMOS_ENDPOINT` ve `COSMOS_KEY` env var'ları tanımlı.
- `orders` ve `leases` container'ları mevcut (Phase 1'de `ensureLeasesContainer` ile garanti edildi).
- `app.role` konfigürasyonu `worker` veya `both` değerine sahip.
- `app.outbox.changefeed` konfigürasyon bloğu yüklendi (processor name, instance name, poll delay, max items).

## Main Success Scenario

1. Spring container, `@WorkerComponent` ile işaretli `OutboxChangeFeedProcessor` bean'ini oluşturur.
2. Spring lifecycle yöneticisi `SmartLifecycle.start()` metodunu çağırır.
3. Processor, `CosmosAsyncClient` üzerinden `orders` (feed) ve `leases` (lease) container handle'larını alır.
4. Processor, `ChangeFeedProcessorBuilder` ile builder'ı yapılandırır: host adı `instanceName`, lease prefix `processorName + "."`, feed poll delay ve max item count uygulanır.
5. Processor, `handleChanges` callback'ini kendi batch handler metoduna bağlar.
6. Processor build edilir ve `start().block()` ile asenkron başlatma tamamlanır.
7. `isRunning()` `true` döner; sistem başlatma log'u yayımlar.

## Alternative Flows

### A1: Uygulama API Rolünde Başlatıldı

**Trigger:** `app.role=api` (adım 1 öncesi)
**Flow:**

1. `@WorkerComponent` meta-anotasyonu `@ConditionalOnExpression` koşulunu sağlamaz.
2. `OutboxChangeFeedProcessor` bean'i hiç oluşturulmaz.
3. Lifecycle çağrısı yapılmaz; use case sonlanır.

### A2: Cosmos Erişim Hatası

**Trigger:** Cosmos endpoint/key geçersiz veya ağ erişimi yok (adım 3)
**Flow:**

1. SDK exception fırlatır.
2. Processor hata log'lar, `start()` bloklanır ve uygulama başlatması başarısız olur.
3. Use case sonlanır; postcondition: processor başlatılamadı.

### A3: İkinci Worker Instance Aynı Anda Başladı

**Trigger:** Aynı `processorName` ile başka bir host (adım 6)
**Flow:**

1. Cosmos lease container, partition lease'lerini iki instance arasında paylaştırır.
2. Her iki instance da çalışır durumda olur; sadece kendi lease'ine düşen partition'ları işler.
3. Use case adım 7 ile tamamlanır (her iki tarafta).

## Postconditions

### Success Postconditions

- Change Feed Processor çalışır durumda; lease container'da host kaydı görünür.
- Sonraki yeni dokümanlar (özellikle outbox) batch handler'a iletilir.
- `SmartLifecycle.isRunning()` `true` döner.

### Failure Postconditions

- Processor başlatılamadı; uygulama context'i başlatma sırasında hata log'lar.
- Lease container'da host kaydı oluşmaz.

## Business Rules

### BR-101: Worker Gating

Processor yalnızca `app.role ∈ {worker, both}` rollerinde aktiftir. `api` rolünde processor başlatılmaz.

### BR-102: Lease Prefix Zorunluluğu

Aynı Cosmos hesabı üzerinde birden fazla logical processor çalışacaksa, lease prefix processor adına göre ayrılır.

### BR-103: Tek Komutla Çalıştırma

`app.role=both` modunda HTTP API ve Worker tek bir process içinde birlikte çalışır.

---

# Use Case: Stop Outbox Change Feed Processor

## Overview

**Use Case ID:** UC-101.1
**Use Case Name:** Stop Outbox Change Feed Processor (graceful shutdown)
**Primary Actor:** Spring Boot Application Container
**Goal:** Uygulama kapanırken processor temiz şekilde durdurulup lease'ler bırakılsın.
**Status:** Draft

## Preconditions

- Processor çalışır durumda (`isRunning() == true`).

## Main Success Scenario

1. Spring container `SmartLifecycle.stop()` çağrısı yapar.
2. Processor `processor.stop().block()` ile SDK'ya durdurma sinyali gönderir.
3. Aktif batch işlenmesi tamamlanır veya iptal edilir.
4. Lease container'daki host kaydı serbest bırakılır.
5. `isRunning()` `false` döner.

## Alternative Flows

### A1: Durdurma Sırasında Aktif Batch Var

**Trigger:** Shutdown sinyali geldiğinde handler içinde işlem akıyor (adım 2)
**Flow:**

1. Mevcut item için `handleItem` tamamlanmaya çalışılır.
2. Item'a karşılık `processed=true` patch'i atılamamış olabilir.
3. Bir sonraki başlatmada Change Feed aynı item'ı tekrar getirir; idempotency kuralı (`processed=true skip`) korur.

## Postconditions

### Success Postconditions

- Processor kapalı; lease serbest.
- Diğer worker instance'lar partition'ları devralabilir.

### Failure Postconditions

- Lease kilidi zaman aşımı ile düşene kadar başka instance partition'ı devralamaz.