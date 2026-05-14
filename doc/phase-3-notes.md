# Faz 3 — Service Bus Publisher (Kısa Özet)

## Hedef

Faz 2'deki worker artık log yerine Azure Service Bus topic'ine integration event yayımlayacak.

## Ana Parçalar

- **`ServiceBusIntegrationEventPublisher`** — Mevcut `LoggingIntegrationEventPublisher`'ı değiştirir; outbox'tan okunan event'i `order-events` topic'ine gönderir.
- **Idempotency** — Outbox dokümanına `status=PUBLISHED` + `publishedAt` alanları eklenir (sadece `processed` flag yetmez).
- **Demo consumer** — Aynı uygulama içinde `@ServiceBusListener` ile mesajı tüketip log'a basar (at-least-once doğrulaması için).
- **Audit hook** — `IntegrationAuditLogger` arayüzü hâlâ `Noop` (gerçek Cosmos audit Faz 4'te).

## Altyapı

- Service Bus namespace + `order-events` topic + `demo-consumer` subscription
- `spring-messaging-azure-servicebus` + `spring-cloud-azure-starter` bağımlılıkları

## Bitiş Kriteri

Order place → outbox → Service Bus → consumer log'u uçtan uca akar; outbox `PUBLISHED` işaretlenir; at-least-once teslimat gözlenir.

## Uygulama Notları (tamamlandı)

- **Starter seçimi:** `spring-messaging-azure-servicebus` (ServiceBusTemplate + `@ServiceBusListener` + `@EnableAzureMessaging`). JMS değil.
- **Header eşleme (.NET → Java):** `SUBJECT=eventType`, `MESSAGE_ID=outboxItemId`, `CORRELATION_ID=orderId`; `tenantId/orderId/outboxItemId/eventType` doğrudan Spring `Message` header'ı olarak set edilir — binder bilmediği header'ları otomatik olarak Service Bus `applicationProperties` map'ine taşır.
- **Idempotency patch:** `CosmosOutboxProcessedMarker.markPublished` tek `CosmosPatchOperations` çağrısında `/status="PUBLISHED"`, `/publishedAt`, `/processed=true` üçlüsünü atomik yazar. Change feed filtresi `processed==true` skip mantığı korunduğu için Faz 2 davranışı bozulmaz.
- **Fallback:** `app.servicebus.publisher.enabled=false` (veya unset) iken `LoggingIntegrationEventPublisher` aktif kalır (`matchIfMissing=true`). Service Bus etkinleştirildiğinde Logging publisher otomatik devre dışı.
- **Demo consumer:** `OrderEventsConsumer` `@ConditionalOnProperty(app.servicebus.consumer.enabled=true)` ile opt-in. Mesajı log'a yazar; opsiyonel `Consumer<Message<byte[]>>` observer bean'i ile integration test'lerde mesaj yakalama mümkün.

## Env

```
COSMOS_ENDPOINT=...
COSMOS_KEY=...
SERVICEBUS_CONNECTION_STRING=...
SERVICEBUS_PUBLISHER_ENABLED=true      # default true
SERVICEBUS_CONSUMER_ENABLED=true       # demo için
SERVICEBUS_TOPIC=order-events          # default
SERVICEBUS_SUBSCRIPTION=demo-consumer  # default
APP_ROLE=both
```

## Smoke

```
./mvnw spring-boot:run
curl -X POST http://localhost:8080/orders -H 'Content-Type: application/json' \
  -d '{"tenantId":"t1","customerId":"c1","items":[{"sku":"X","quantity":1,"unitAmount":10,"currency":"USD"}]}'
```

Beklenen log akışı: `OutboxChangeFeedProcessor` outbox'u yakalar → `ServiceBusIntegrationEventPublisher` `Published outbox ... to topic order-events` → `OrderEventsConsumer` `Consumed integration event tenantId=t1 ...`. Cosmos'ta outbox dokümanı `processed=true`, `status="PUBLISHED"`, `publishedAt=<UTC ISO-8601>`.