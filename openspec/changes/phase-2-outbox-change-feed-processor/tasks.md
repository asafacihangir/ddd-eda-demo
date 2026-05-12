## 1. Worker gating altyapısı

- [ ] 1.1 `infra/worker/WorkerComponent.java` meta-anotasyonunu oluştur (`@Component` + `@ConditionalOnExpression("'${app.role:both}' == 'worker' or '${app.role:both}' == 'both'")`, `@Target(TYPE)`, `@Retention(RUNTIME)`)
- [ ] 1.2 Phase 1'deki `OrderController` (veya REST katmanı) için mevcut `app.role` gating'inin (`api` / `both`) yerinde olduğunu doğrula; eksikse ayrı bir issue olarak not düş (Phase 2 scope dışı modifikasyon yapma)

## 2. Application abstractions

- [ ] 2.1 `application/abstractions/ChangeFeedItem.java` record'unu oluştur (alanlar: `id`, `tenantId`, `entityType`, `orderId`, `eventType`, `payloadJson`, `processed`, `metadata: Map<String,String>`, `createdAtUtc`)
- [ ] 2.2 Mevcut `IntegrationEventPublisher`, `OutboxProcessedMarker`, `IntegrationAuditLogger` imzalarının dokunulmadığını doğrula

## 3. Change feed application logic

- [ ] 3.1 `application/changefeed/ChangeFeedItemHandlerSpec.java` record'unu oluştur (`entityType`, `isValidItem`, `invalidItemLogMessage`, `handleItem`, `shouldSkip`)
- [ ] 3.2 `application/changefeed/ChangeFeedItemBatchProcessor.java` sınıfını oluştur; `process(List<ChangeFeedItem>, ChangeFeedItemHandlerSpec)` metodu: entity type filter → isValidItem (warn+skip) → shouldSkip → handleItem; tek item exception → log + devam
- [ ] 3.3 SLF4J logger'ı ekle ve WARN/ERROR mesajları için `invalidItemLogMessage` kullan

## 4. Cosmos infra — options ve konfigürasyon

- [ ] 4.1 `infra/changefeed/OutboxChangeFeedOptions.java` oluştur (`@ConfigurationProperties("app.outbox.changefeed")`, alanlar: `processorName=outbox-dispatcher`, `instanceName=${HOSTNAME:local-instance}`, `feedPollDelay=PT1S`, `maxItemsPerBatch=100`)
- [ ] 4.2 `OrderManagementCosmosConfiguration` üzerine `@EnableConfigurationProperties(OutboxChangeFeedOptions.class)` ekle (veya mevcut `@ConfigurationPropertiesScan` paketini doğrula)
- [ ] 4.3 `application.yml` içine `app.outbox.changefeed` bloğunu ekle (processor-name, instance-name, feed-poll-delay, max-items-per-batch)
- [ ] 4.4 Phase 1 `ensureLeasesContainer` partition key path'inin Change Feed Processor SDK gereksinimi (`/id`) ile uyumlu olduğunu doğrula; uyumsuzsa not düş

## 5. Cosmos infra — Change Feed Processor

- [ ] 5.1 `infra/changefeed/OutboxChangeFeedProcessor.java` sınıfını oluştur (`@WorkerComponent implements SmartLifecycle`)
- [ ] 5.2 Constructor injection: `CosmosAsyncClient`, `CosmosProperties`, `OutboxChangeFeedOptions`, `ChangeFeedItemBatchProcessor`, `IntegrationEventPublisher`, `OutboxProcessedMarker`, `IntegrationAuditLogger`, `ObjectMapper`
- [ ] 5.3 `start()`: feed + lease container resolve, `ChangeFeedProcessorBuilder` ile builder kur (`hostName`, `feedContainer`, `leaseContainer`, `options.setLeasePrefix(processorName+".")`, `setFeedPollDelay`, `setMaxItemCount`), `handleChanges(this::handleBatch)`, `buildChangeFeedProcessor().start().block()`
- [ ] 5.4 `stop()`: `processor.stop().block()`; `isRunning()` ve `getPhase()` implement et
- [ ] 5.5 `handleBatch(List<JsonNode>)`: Jackson ile `ChangeFeedItem`'a map (`_type` → `entityType` alias), spec inşa et (`entityType="outbox"`, `isValidItem`, `shouldSkip = processed`, `handleItem = this::dispatch`), `batchProcessor.process(...)` çağır
- [ ] 5.6 `dispatch(ChangeFeedItem)`: sırayla `auditLogger.logPublish(...)` → `publisher.publishOutbox(...)` → `processedMarker.markProcessed(...)`

## 6. Cosmos infra — default impl'ler

- [ ] 6.1 `infra/changefeed/LoggingIntegrationEventPublisher.java` oluştur (`@WorkerComponent`, `@ConditionalOnMissingBean(IntegrationEventPublisher.class)`, INFO log: tenantId, orderId, outboxItemId, eventType, payload length)
- [ ] 6.2 `infra/changefeed/CosmosOutboxProcessedMarker.java` oluştur (`@WorkerComponent`, Cosmos async patch `CosmosPatchOperations.create().set("/processed", true)`, partition key = tenantId)
- [ ] 6.3 `infra/changefeed/NoopIntegrationAuditLogger.java` oluştur (`@WorkerComponent`, `@ConditionalOnMissingBean(IntegrationAuditLogger.class)`, boş gövde)

## 7. Unit testler

- [ ] 7.1 `test/.../application/changefeed/ChangeFeedItemBatchProcessorTest.java` oluştur
- [ ] 7.2 Senaryo: entity type mismatch → handleItem çağrılmaz
- [ ] 7.3 Senaryo: processed=true → skip
- [ ] 7.4 Senaryo: isValidItem false → warn log + skip (caplog/log assertion)
- [ ] 7.5 Senaryo: handleItem exception fırlatır → sonraki item işlenir
- [ ] 7.6 Senaryo: happy path → handleItem çağrılır

## 8. Integration test

- [ ] 8.1 `test/.../infra/changefeed/OutboxChangeFeedProcessorIntegrationTest.java` oluştur (`@SpringBootTest`, `properties = {"app.role=worker"}`)
- [ ] 8.2 `@MockBean IntegrationEventPublisher` ile dispatch verification
- [ ] 8.3 Test setup: `orders` container'a manuel `_type=outbox, processed=false` dokümanı yaz
- [ ] 8.4 Test assertion: publisher mock 5sn içinde `publishOutbox(...)` argümanlarıyla çağrıldı
- [ ] 8.5 Test assertion: Cosmos'tan doc'u tekrar oku, `processed=true` olduğunu doğrula
- [ ] 8.6 Test cleanup: oluşturulan outbox dokümanını ve lease state'ini sil (veya unique processor-name kullan)

## 9. End-to-end verification

- [ ] 9.1 `source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./mvnw clean verify` yeşil
- [ ] 9.2 Smoke `APP_ROLE=both`: POST /orders → 1-2sn içinde log'da `LoggingIntegrationEventPublisher` çıktısı (tenantId, orderId, outboxItemId, eventType=`OrderPlacedEvent`)
- [ ] 9.3 Smoke `APP_ROLE=both`: Cosmos data explorer'da outbox dokümanı `processed=true`
- [ ] 9.4 Smoke `APP_ROLE=both`: uygulamayı restart et, aynı doküman tekrar publish edilmiyor (`processed=true` skip)
- [ ] 9.5 Smoke `APP_ROLE=api`: `OutboxChangeFeedProcessor.start()` log'u yok, REST endpoint cevap veriyor
- [ ] 9.6 Smoke `APP_ROLE=worker`: processor başlıyor, REST controller'lar inactive
- [ ] 9.7 Lease coordination: iki `APP_ROLE=worker` instance ile tek order için tek instance'ta dispatch log'u