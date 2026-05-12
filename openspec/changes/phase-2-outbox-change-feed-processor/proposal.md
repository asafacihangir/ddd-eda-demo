## Why

Phase 1'de `Order` aggregate ve outbox event'leri aynı Cosmos `orders` container'ına transactional batch ile yazılıyor; ancak bu outbox kayıtlarını okuyup yayınlayan bir bileşen yok — event'ler şu an Cosmos'ta sıkışmış durumda. Phase 2, Cosmos Change Feed Processor tabanlı bir worker ile outbox kayıtlarını sürekli okuyup (şimdilik log'a) yayınlayarak ve `processed=true` olarak işaretleyerek event-driven pipeline'ı uçtan uca çalışır hale getirir; Service Bus entegrasyonu Phase 3'e ertelenir.

## What Changes

- `app.role` profil değeri ile API/worker süreçlerini ayırmak için yeni `@WorkerComponent` meta-anotasyon (`worker` veya `both` rollerinde aktif).
- Cosmos Change Feed Processor tabanlı `OutboxChangeFeedProcessor` (SmartLifecycle), `orders` container'ını dinler ve `_type=outbox` dokümanları için dispatch pipeline'ı çalıştırır.
- Application katmanında saf logic olarak `ChangeFeedItem` DTO, `ChangeFeedItemHandlerSpec` ve `ChangeFeedItemBatchProcessor` (filter/skip/handle akışı, hata izolasyonu).
- `IntegrationEventPublisher` için `LoggingIntegrationEventPublisher` (default, `@ConditionalOnMissingBean`), `OutboxProcessedMarker` için Cosmos patch tabanlı `CosmosOutboxProcessedMarker`, `IntegrationAuditLogger` için `NoopIntegrationAuditLogger` implementasyonları.
- `app.outbox.changefeed.*` konfigürasyon bloğu ve `OutboxChangeFeedOptions` (`@ConfigurationProperties`); `OrderManagementCosmosConfiguration`'a property binding'in eklenmesi.
- Unit test (`ChangeFeedItemBatchProcessorTest`) ve gerçek Cosmos hesabına bağlanan integration test (`OutboxChangeFeedProcessorIntegrationTest`).

## Capabilities

### New Capabilities
- `outbox-change-feed-dispatcher`: Cosmos `orders` container'ındaki outbox dokümanlarını Change Feed üzerinden tüketip Integration Event Publisher'a yönlendiren, idempotent (`processed=true`) ve `app.role` ile süreç bazında gate'lenen worker yetkinliği.

### Modified Capabilities
<!-- Henüz yayınlanmış spec yok; mevcut capability deltası değil yeni capability ekleniyor. -->

## Impact

- **Yeni paketler:** `infra.worker`, `application.changefeed`, `infra.changefeed`.
- **Application abstractions:** `ChangeFeedItem` DTO eklenir (`IntegrationEventPublisher`, `OutboxProcessedMarker`, `IntegrationAuditLogger` imzaları değişmez).
- **Cosmos:** `orders` container Change Feed olarak okunur; `leases` container (Phase 1'de oluşturulmuş) lease store olarak kullanılır; outbox dokümanları üzerinde `/processed` JSON patch operasyonu.
- **Runtime topology:** Aynı JAR'dan `APP_ROLE=api|worker|both` ile üç farklı süreç modu; `api` modunda processor bean'leri yüklenmez.
- **Konfigürasyon:** `application.yml` içinde `app.outbox.changefeed` bloğu (processor-name, instance-name, feed-poll-delay, max-items-per-batch).
- **Bağımlılıklar:** Phase 1'de eklenmiş Cosmos SDK; ek bağımlılık yok.
- **Test:** Integration test, `COSMOS_ENDPOINT` / `COSMOS_KEY` env var'larını gerektirir (Phase 1 pattern'i).
- **Out of scope:** Service Bus publisher (Phase 3), Cosmos audit log impl. (Phase 4), `_type` → `entityType` JSON alanı yeniden adlandırma (geriye dönük migration gerektirir).