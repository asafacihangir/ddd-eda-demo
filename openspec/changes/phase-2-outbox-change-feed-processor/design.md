## Context

Phase 1, `Order` aggregate ile outbox kayıtlarını aynı Cosmos `orders` container'ına transactional batch ile yazıyor. `OutboxCosmosDocument` JSON şeması: `id`, `tenantId` (partition key), `_type="outbox"`, `orderId`, `eventType`, `payloadJson`, `processed: boolean`, `createdAtUtc`. `leases` container Phase 1 startup'ında `OrderManagementCosmosConfiguration.ensureLeasesContainer` ile oluşturuluyor. Application abstractions tanımlı fakat implementasyon yok; outbox kayıtları okunmuyor.

Bu fazda hedef, Cosmos Change Feed Processor (Java SDK v4) ile `orders` container'ını dinleyen, `_type=outbox` dokümanlarını yayınlayan ve `processed=true` patch'leyen bir worker eklemek. Referans uygulama .NET `IHostedService` ve `ChangeFeedProcessor` deseni; Java tarafında muadiller `SmartLifecycle` + `ChangeFeedProcessorBuilder`.

Tek bir Spring Boot JAR üç süreç modunda (`APP_ROLE=api|worker|both`) koşacak. Container/Kubernetes deployment'ında API ve worker süreçleri ayrı pod'lar olacak; tek instance horizontal-scale durumunda lease coordination üzerinden iş paylaşımı.

## Goals / Non-Goals

**Goals:**
- `orders` container'ındaki outbox dokümanlarını at-least-once + idempotent (processed flag) semantikle yayınlamak.
- Worker bean'lerini `app.role` ile gate'lemek; `api` modunda processor instance edilmemeli.
- Application logic'i (filter/skip/handle) Cosmos SDK bağımlılığı olmadan test edilebilir tutmak.
- Restart sonrası lease checkpoint'inden devam; aynı doc tekrar yayınlanmamalı (`processed=true` skip).
- İki worker instance ile lease coordination otomatik.

**Non-Goals:**
- Service Bus / Event Grid publishing (Phase 3).
- Cosmos tabanlı audit log persist (Phase 4).
- `_type` JSON alanının `entityType` olarak yeniden adlandırılması (geriye dönük migration gerektirir).
- Dead-letter queue, retry policy, poison message handling (henüz gerek yok; at-least-once yeterli).
- Outbox cleanup / TTL.

## Decisions

### Worker gating: özel `@WorkerComponent` meta-anotasyon
Spring profile (`@Profile`) yerine `@ConditionalOnExpression`'lı meta-anotasyon tercih edildi:
- Tek satırlık deklarasyon (`@WorkerComponent`), her bean üzerinde `app.role` expression tekrarlanmaz.
- API + worker `both` modunu doğal destekler.
- Alternatif (`@Profile("worker | both")`): Spring profile sayısını şişirir ve `spring.profiles.active` ile çakışır.

### Lifecycle: `SmartLifecycle`
`@PostConstruct` yerine `SmartLifecycle.start()` tercih edildi:
- Cosmos SDK'nın `processor.start()` çağrısı reactor `Mono` döner ve uzun süreli; `@PostConstruct` içinde block etmek context startup'ı yavaşlatır.
- `SmartLifecycle` Spring application context lifecycle event'lerine bağlı; graceful shutdown'da `stop()` deterministik çalışır.
- Alternatif (`ApplicationListener<ContextRefreshedEvent>`): `stop()` simetrisi yok, manuel `DisposableBean` gerekir.

### Saf application logic: `ChangeFeedItem` + `ChangeFeedItemBatchProcessor`
Cosmos SDK `JsonNode` listesi infra'da `ChangeFeedItem` record'ına map'lenir; batch processor sadece bu DTO'yu görür:
- Unit testler Cosmos SDK olmadan koşar.
- Aynı pipeline ileride farklı entity type'larla (örn. Phase 4 audit log) yeniden kullanılabilir.
- .NET tarafındaki `ChangeFeedItemHandlerSpec` record'unun bire bir karşılığı; kavramsal paralellik.

### Hata izolasyonu: tek item exception → log + devam
Batch processor bir item'ın exception'ı için throw etmez, log + sonraki item ile devam eder:
- Change Feed Processor exception'da tüm batch'i replay eder; idempotency `processed=true` patch ile zaten sağlanıyor.
- Bir corrupt doc tüm pipeline'ı durdurmamalı.
- Trade-off: kalıcı corrupt doc sonsuz retry'a girer (Phase 5'te poison handling).

### Idempotency: `processed=true` JSON patch
Tüm doc replace yerine `CosmosPatchOperations.set("/processed", true)`:
- RU maliyeti düşük, concurrent update riski yok.
- Restart sonrası change feed checkpoint'i lease'te tutulduğu için aynı doc tekrar gelirse `shouldSkip = processed` ile skip.

### Default impl'ler `@ConditionalOnMissingBean`
`LoggingIntegrationEventPublisher` ve `NoopIntegrationAuditLogger` `@ConditionalOnMissingBean` ile işaretli; Phase 3'te Service Bus impl, Phase 4'te Cosmos audit logger eklenince otomatik devralacak. `CosmosOutboxProcessedMarker` için tek impl yeterli, `@ConditionalOnMissingBean` gereksiz.

### Konfigürasyon: `OutboxChangeFeedOptions` ayrı, Cosmos isimleri `CosmosProperties`'te
Database/container adları zaten Phase 1 `CosmosProperties`'te (`database`, `ordersContainer`, `leasesContainer`). Yeni options sınıfında sadece processor-spesifik alanlar:
- `processorName` (lease prefix), `instanceName` (host name), `feedPollDelay`, `maxItemsPerBatch`.
- Duplicate config yok; lease container ismi Cosmos config'ten okunur.

## Risks / Trade-offs

- **Risk**: Change Feed Processor SDK `block()` ile başlatılıyor; startup blocking olabilir.
  → Mitigation: `SmartLifecycle.getPhase()` ile geç başlat; HTTP listener (API + both modu) zaten kendi phase'inde, processor sonradan başlar.
- **Risk**: At-least-once → aynı event birden fazla yayınlanabilir (örn. dispatch sonrası, `markProcessed` öncesi crash).
  → Mitigation: Phase 3'te Service Bus consumer'ları idempotent design edilecek (outboxItemId message-id olarak); Phase 2'de log impl. zararsız.
- **Risk**: `LoggingIntegrationEventPublisher` ve `CosmosOutboxProcessedMarker` integration test'te aynı anda çalışınca log spam.
  → Mitigation: Test'te `@MockBean IntegrationEventPublisher` ile override; default impl by-pass edilir.
- **Risk**: Lease container Phase 1'de oluşturuldu ama partition key path'i Phase 2 ile uyumsuz olabilir.
  → Mitigation: Phase 1 implementasyonu Cosmos SDK önerisini (`/id`) kullanmış; doğrulanacak ve gerekirse `ensureLeasesContainer` patch'lenecek (tasks'te kontrol step'i).
- **Trade-off**: `_type` JSON alanı `entityType` olarak adlandırılmıyor; phases.md ile metin uyumsuzluğu kalıyor.
  → Belgeleme: `ChangeFeedItem.entityType` Java alanı ile `_type` JSON alanı arasında Jackson alias mapping; kodda yorum satırıyla işaret.
- **Trade-off**: Worker süreçleri için ayrı port/HTTP disable opsiyonu yok; `app.role=worker` modunda Tomcat hâlâ ayağa kalkar (sadece controller'lar gate'li).
  → Phase 2'de kabul edilebilir; gerekirse `spring.main.web-application-type=none` profile-conditional sonraki fazda.

## Migration Plan

Phase 2 net-new kod; existing davranış değişmiyor.

1. Mevcut Phase 1 deploy'unda `APP_ROLE` env yok → default `both`, processor başlar.
2. Production'a deploy edilirken `APP_ROLE=api` (web tier) ve `APP_ROLE=worker` (background tier) olarak ayrılır.
3. Rollback: previous JAR'a dön; Cosmos şema değişikliği yok (yeni alan eklenmiyor), data uyumlu.
4. Smoke test sırası: `both` → API+worker tek pod doğrula → ayrı pod'lara böl.

## Open Questions

- `ensureLeasesContainer` partition key path'i nedir? (Phase 1 kodu kontrol edilmeli; SDK önerisi `/id`.) Eğer farklıysa Change Feed Processor builder hata verir; bu durum tasks'te erken doğrulanmalı.
- `instanceName` çakışmasında SDK davranışı? İki worker aynı `HOSTNAME` ile gelirse lease conflict — production'da Kubernetes pod adı unique olur; local'de tek instance varsayımı.