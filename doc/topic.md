# Proje Konuları — Soru & Cevap

Bu proje DDD (Domain-Driven Design) + EDA (Event-Driven Architecture) deseniyle Azure Cosmos DB üzerinde inşa edilen bir Order Management örneği. Aşağıdaki sorular projedeki temel mimari kararları açıklar.

---

## Temel Mimari

### DDD nedir ve bu projede nasıl uygulanıyor?

**Domain-Driven Design**, iş kurallarını teknik detaylardan ayırarak modelleyen bir yaklaşımdır. Bu projede:

- `domain/` paketi saf iş kurallarını içerir (Order aggregate, value objects, domain events).
- `application/` katmanı use-case orkestrasyonu yapar (command/query handler'lar).
- `infra/` katmanı teknik detayları gizler (Cosmos, JSON serializer, repository implementasyonu).

Domain katmanı hiçbir framework veya altyapı bağımlılığı taşımaz — yalnız Java.

### EDA (Event-Driven Architecture) ne demek?

Servisler arası iletişim **olaylar (events)** üzerinden olur. Doğrudan API çağırmak yerine, bir servis "OrderPlaced" gibi bir olay yayınlar; ilgilenen servisler dinler. Bu projede outbox + Change Feed Processor mekanizması bu olay yayılımını sağlar.

### CQRS nedir ve neden ayrı paketlerde?

**Command Query Responsibility Segregation**: yazma (command) ve okuma (query) işlemleri ayrı modellerle yapılır. Bu projede `application/orders/commands/` ve `application/orders/queries/` ayrımı bunu yansıtır. Faz 4'te okuma modeli için ayrı bir projection container eklenecek.

---

## Outbox Pattern

### Outbox pattern nedir?

Aggregate (Order) ve onun ürettiği integration event'ler **aynı transaction içinde** veritabanına yazılır. Olaylar geçici bir "outbox" tablosuna/dokümanına eklenir. Sonradan ayrı bir worker bu outbox kayıtlarını okuyup mesaj broker'a (Service Bus, Kafka vb.) iletir.

### Neden outbox'a ihtiyacımız var? Doğrudan publish edemez miyiz?

Hayır, çünkü iki ayrı sistem (DB + broker) arasında **distributed transaction yok**. Senaryo:
1. Order DB'ye yazılır → başarılı
2. Event broker'a publish edilmeye çalışılır → ağ hatası, kayıp
3. Sonuç: Order var ama olay yayılmadı → diğer servisler bilgisiz

Outbox bu sorunu çözer: önce **tek bir transaction**'da hem aggregate hem event yazılır, sonra worker güvenle yayımlar.

### Neden outbox ayrı bir container/tablo değil de aynı `orders` container'ında saklanıyor?

Cosmos DB'de **transactional batch** sadece **tek bir partition key** içinde çalışır. Order ve outbox dokümanlarını aynı container'da, aynı `tenantId` partition key'i altında tutarak atomik yazma garantisi alıyoruz. Ayrı container kullanırsak iki yazma işlemi arasında hata olursa tutarsızlık doğar.

Dokümanları ayırt etmek için `_type` alanı kullanılır: `"order"` veya `"outbox"`.

### Neden `_type` alanı? `entityType` daha okunaklı olmaz mıydı?

Doğru, plan ve .NET referansı `entityType` öneriyordu. Phase 1 implementasyonu `_type` kullanıyor; bu küçük bir tutarsızlık ama veri migration'ı gerektireceği için Phase 2'de değiştirilmedi.

---

## Cosmos Change Feed Processor

### Cosmos Change Feed nedir?

Cosmos DB'nin bir container'a yapılan **her değişikliği (insert + update)** sıralı şekilde dinlemenizi sağlayan built-in mekanizmasıdır. Kafka topic'ine benzer ama veritabanının kendisi tarafından sağlanır — ekstra altyapı yok.

### Change Feed Processor ne yapar?

SDK tarafından sağlanan, change feed'i **otomatik checkpoint** ve **partition paylaşımı** ile tüketen yüksek seviyeli API. Şu sorumlulukları üstlenir:

- Nereye kadar okudum? → `leases` container'ında kayıt.
- Birden fazla worker varsa partition'ları kim dinleyecek? → lease coordination.
- Yeni doküman geldiğinde callback'i tetikler.

### `leases` container'ı ne işe yarar?

Change Feed Processor'un **state'ini** tutar. Hangi partition'ı kimin işlediğini, en son hangi continuation token'a kadar okuduğunu burada saklar. Restart sonrası processor buradan kaldığı yerden devam eder.

### Neden `processed=true` patch'i atıyoruz? At-least-once değil mi?

Evet, change feed at-least-once garantisi sunar: lease commit'i başarısız olursa aynı doküman tekrar okunabilir. **Idempotency** için her outbox kaydını işlediğimizde `processed=true` ile işaretleriz; bir sonraki okumada `processed=true` olanlar skip edilir. Böylece aynı event iki kez publish edilmez.

### Aynı outbox iki kere işlenebilir mi?

İki worker arasında: Hayır, lease coordination tek bir worker'ın belirli partition'a sahip olmasını garanti eder.

Aynı worker'da: Teorik olarak evet (lease commit edilmeden önce dispatch tamamlanmış olabilir). Bu yüzden `processed=true` patch kontrolü ve consumer tarafında idempotent handler önemlidir.

---

## Worker Gating ve Deployment Modları

### `app.role` ne işe yarar?

Tek bir JAR'ı üç farklı şekilde çalıştırma imkanı verir:

- `app.role=api` → sadece HTTP controller'lar, processor yok
- `app.role=worker` → sadece change feed processor, HTTP yok
- `app.role=both` (default) → ikisi birden, geliştirme için pratik

Production'da API ve worker'ı **ayrı pod'lara** ayırırsanız her birinin yükü bağımsız ölçeklenebilir.

### `@WorkerComponent` neden meta-anotasyon olarak yazıldı?

Sıradan `@Component` + `@ConditionalOnExpression` her worker sınıfında tekrarlanır. Meta-anotasyon bunu tek satıra indirir:

```java
@WorkerComponent
public class OutboxChangeFeedProcessor { ... }
```

Bu, `@Component @ConditionalOnExpression("'${app.role:both}' == 'worker' or '${app.role:both}' == 'both'")` ile eşdeğer.

### `worker` profile neden `web-application-type: none` set ediyor?

`spring-boot-starter-webmvc` sınıf yolunda olduğu için Spring varsayılan olarak HTTP server (port 8080) açar. Worker-only modda HTTP'ye ihtiyacımız yok — boşuna port tutmak, log kirletmek ve health check ile karışıklık yaratmak istemiyoruz. `application.yml`'deki profile-aktivasyonu bunu çözer.

### Aynı makinede iki worker instance çalıştırmak ne fayda eder?

**Lease coordination testi.** Cosmos partition'larını paylaşırlar; tek bir outbox dokümanı yalnız bir instance tarafından dispatch edilir. Production'da paralel iş yükünü ölçeklendirmek için kullanılır.

---

## SmartLifecycle vs PostConstruct

### Neden `@PostConstruct` ile başlatıyoruz, `SmartLifecycle` ile değil?

İlk implementasyon `SmartLifecycle` ile yapıldı (.NET'teki `IHostedService` benzetimi). Refactor sonrası `@PostConstruct` ile değiştirildi çünkü:

- 4 metod (start/stop/isRunning/getPhase) yerine 2 metod yeter
- Spring lifecycle phase'lerine bizim ihtiyacımız yok
- Daha az kod, daha az indirection

**Ne zaman SmartLifecycle gerekir?** Birden fazla bean'in başlatma sırası önemliyse, `getPhase()` ile sıralanır. Bizim tek processor'umuz var, bu yüzden basit `@PostConstruct` yeterli.

---

## Test Stratejisi

### Unit test ve integration test'i nasıl ayırıyoruz?

- **Unit test**: Saf domain ve application logic. Spring context yok, JUnit + AssertJ + Mockito. Hızlı (~ms).
- **Integration test**: Gerçek Cosmos DB. `@SpringBootTest` + `@EnabledIfEnvironmentVariable(named = "COSMOS_ENDPOINT", ...)`. Yavaş ama gerçek davranışı doğrular.

### Neden Testcontainers veya emülatör kullanmıyoruz?

Cosmos DB emülatörü Linux/macOS Apple Silicon üzerinde sorunlu. Gerçek Azure hesabı ile test etmek hem CI'da hem lokal'de daha tutarlı; ücret minimal (RU/s düşük tutuluyor).

### `@EnabledIfEnvironmentVariable` ne sağlıyor?

`COSMOS_ENDPOINT` env var'ı yoksa integration test'leri **JUnit-level'da** atlanır. Spring context bile yüklenmez, hata almaz, hızlı geçer.

---

## Multi-Tenancy

### `tenantId` neden her dokümanda?

Cosmos'ta partition key'dir. Aynı container'da farklı tenant'ların verilerini izole tutar:

- Sorgu performansı: tek partition'da kalır
- Throughput dağılımı: tenant başına RU
- Yetkilendirme: query'lerde her zaman tenant filter

### Cross-tenant query yapılabilir mi?

Teknik olarak evet (cross-partition query) ama maliyet ve performans cezası var. Mimari kararı: **asla** cross-tenant operasyon yapmıyoruz; her use-case bir tenant context'inde çalışır.

---

## Faz Yol Haritası

### Faz 1'de ne yapıldı?

Order aggregate, domain events, Cosmos repository, transactional batch ile outbox yazma. API HTTP endpoint'leri (`POST /orders`, `POST /orders/{id}/cancel`).

### Faz 2 (mevcut) neyi tamamladı?

Change Feed Processor: outbox dokümanlarını okuyup log'a "publish" eder ve `processed=true` patch'ler. Worker gating altyapısı (`@WorkerComponent`, `app.role`). Service Bus yok.

### Faz 3'te ne gelecek?

Service Bus entegrasyonu. `LoggingIntegrationEventPublisher` yerine `ServiceBusIntegrationEventPublisher` devreye girer. Audit logger da Cosmos versiyonuna yükseltilir (Faz 4 ile birlikte).

### Faz 4'te ne gelecek?

CQRS okuma modeli: `OrderSummaryProjector` Change Feed'den order dokümanlarını okur, ayrı bir `order-summary` container'ına denormalize eder. Bu noktada Outbox processor + Summary projector arasında ortak abstraction (eğer gerçek tekrar görürsek) düşünülebilir.

---

## Tasarım Felsefesi

### Neden bu kadar çok plan dokümanı (.planning/)?

Multi-agent / multi-session geliştirme akışı için. Her faz öncesi karar gerekçeleri, dosya değişiklikleri, verification adımları yazılıyor; sonraki conversation bu bağlamı yeniden inşa etmek zorunda kalmıyor.

### Neden bazı yerlerde "premature abstraction" değil "üç tekrar görene kadar bekle" kuralı uygulandı?

YAGNI (You Aren't Gonna Need It). İlk implementasyonda `ChangeFeedItemHandlerSpec`/`BatchProcessor` abstraction'ları eklenmişti (.NET birebir port). Refactor'da silindi çünkü tek consumer var. Faz 4'te ikinci consumer (`OrderSummaryProjector`) eklendiğinde, **gerçek tekrarı görerek** abstract edilebilir — varsayımsal değil somut gereksinim üzerine.

### Neden Spring Cloud Azure starter kullanmıyoruz, Cosmos SDK'yı doğrudan kullanıyoruz?

`azure-spring-data-cosmos` Spring Data repository'leri sunar ama transactional batch ve change feed processor için **alt seviye `CosmosAsyncClient`** gerekir. Karma kullanım: basit CRUD için Spring Data, kompleks operasyonlar için SDK doğrudan.
