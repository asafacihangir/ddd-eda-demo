# Application Layer — C# → Java Port Plan

**Hedef:** `sample-project/.../OrderManagement.Application` (C#) katmanını Spring Boot tabanlı Java projesine port etmek.
**Bağımlılık:** Domain ✅ ve Common ✅ paketleri tamamlanmış olmalıdır.
**Kapsam dışı:** Persistence (JPA/Cosmos), HTTP API, Worker — bunlar Infrastructure/API/Worker katmanlarına aittir.

---

## 1. Use Case Kataloğu

Aşağıdaki use case'ler, sample-project'in `Application` klasöründeki dosya yapısı bire bir taranarak çıkarılmıştır.

### UC-APP-01 — CQRS Soyutlamalarının Tanımlanması

| Alan | Değer |
|---|---|
| **Aktör** | Geliştirici (port'u yapan) |
| **Ön Koşul** | Domain.Common içinde `Result<T>` ve `Error` tipleri mevcut |
| **Tetikleyici** | Application katmanı için temel komut/sorgu sözleşmeleri gerekiyor |
| **Ana Akış** | 1. `Command<TResult>` marker interface tanımla. 2. `CommandHandler<C, R>` interface tanımla (`Result<R> handle(C command)`). 3. `Query<TResult>` marker interface tanımla. 4. `QueryHandler<Q, R>` interface tanımla. |
| **Sonuç** | Tüm use-case handler'ları aynı sözleşmeye uyar |
| **Kabul Kriteri** | Interface'ler **sadece** `domain.common` ve JDK tiplerine bağımlıdır; framework yok |
| **Kaynak Dosyalar** | `Abstractions/CQRS/ICommand.cs`, `ICommandHandler.cs`, `IQuery.cs`, `IQueryHandler.cs` |

### UC-APP-02 — Repository ve Persistence Port'ları

| Alan | Değer |
|---|---|
| **Aktör** | Application use-case'leri |
| **Ön Koşul** | Domain `Order` aggregate hazır |
| **Tetikleyici** | Use-case'ler aggregate kalıcılığına ihtiyaç duyar |
| **Ana Akış** | 1. `OrderRepository` interface (getById, add, update). 2. `OrderPersistenceState` (aggregate + outbox snapshot DTO). 3. `OutboxProcessedMarker` interface. |
| **Sonuç** | Application, persistence ayrıntılarını bilmeden aggregate'i kalıcılaştırabilir |
| **Kabul Kriteri** | Hiçbir JPA/Spring Data anotasyonu yok |
| **Kaynak Dosyalar** | `Abstractions/Repositories/IOrderRepository.cs`, `Abstractions/OrderPersistenceState.cs`, `Abstractions/IOutboxProcessedMarker.cs` |

### UC-APP-03 — Integration Event Yayını için Port'lar

| Alan | Değer |
|---|---|
| **Aktör** | Outbox işleyici |
| **Ön Koşul** | Outbox kaydı modeli hazır |
| **Tetikleyici** | Domain event'lerin dış sistemlere yayılması gerekir |
| **Ana Akış** | 1. `IntegrationEventPublisher` interface. 2. `IntegrationAuditLogger` interface. |
| **Sonuç** | Application, mesaj broker türünü bilmez |
| **Kabul Kriteri** | Implementasyon Infrastructure katmanında yapılacak |
| **Kaynak Dosyalar** | `Abstractions/IIntegrationEventPublisher.cs`, `Abstractions/IIntegrationAuditLogger.cs` |

### UC-APP-04 — Sipariş Oluşturma (CreateOrder)

| Alan | Değer |
|---|---|
| **Aktör** | Müşteri (API üzerinden) |
| **Ön Koşul** | Geçerli müşteri kimliği ve sipariş kalemleri |
| **Tetikleyici** | API bir `CreateOrderCommand` gönderir |
| **Ana Akış** | 1. Komut doğrulama (jakarta.validation). 2. `Order.create(...)` factory ile aggregate oluştur. 3. Repository'e ekle. 4. `Result<OrderId>` döndür. |
| **Alternatif Akış** | Doğrulama hatası → `Result.failure(validationError)` |
| **Sonuç** | Yeni sipariş kalıcılaştırıldı, `OrderCreated` domain event üretildi |
| **Kabul Kriteri** | In-memory repository ile birim test geçer |
| **Kaynak Dosyalar** | `Orders/Commands/CreateOrder/*` |

### UC-APP-05 — Sipariş İptali (CancelOrder)

| Alan | Değer |
|---|---|
| **Aktör** | Müşteri / operasyon |
| **Ön Koşul** | Sipariş mevcut ve iptal edilebilir durumda |
| **Tetikleyici** | `CancelOrderCommand(orderId, reason)` |
| **Ana Akış** | 1. Aggregate'i yükle. 2. `order.cancel(reason)` çağır. 3. Repository'i güncelle. |
| **Alternatif Akış** | Sipariş bulunamadı → `OrderErrors.NotFound`. İptal edilemez durum → domain hatası. |
| **Sonuç** | `OrderCancelled` domain event üretildi |
| **Kabul Kriteri** | Domain kuralları handler'da değil aggregate'te enforce edilir |
| **Kaynak Dosyalar** | `Orders/Commands/CancelOrder/*` |

### UC-APP-06 — Sipariş Sorgulama (GetOrderById / OrderSummary)

| Alan | Değer |
|---|---|
| **Aktör** | API |
| **Tetikleyici** | `GetOrderByIdQuery(orderId)` |
| **Ana Akış** | 1. Query handler repository'den okur. 2. `OrderSummaryDto`'ya map eder. 3. `Result<OrderSummaryDto>` döner. |
| **Sonuç** | DTO döndürülür; aggregate dışarı sızmaz |
| **Kaynak Dosyalar** | `Orders/Queries/OrderSummaryDto.cs`, ilgili query + handler |

### UC-APP-07 — Validation Pipeline Behavior

| Alan | Değer |
|---|---|
| **Aktör** | CommandBus |
| **Tetikleyici** | Herhangi bir komut işlenmeden önce |
| **Ana Akış** | 1. `PipelineBehavior<C, R>` chain'i kur. 2. `ValidationPipelineBehavior` jakarta `Validator` ile komutu doğrula. 3. Hata varsa handler'ı çağırma; `Result.failure(...)` döndür. |
| **Sonuç** | Handler'lar yalnızca geçerli komut alır |
| **Kaynak Dosya** | `Behaviours/ValidationPipelineBehaviour.cs` |

### UC-APP-08 — Outbox Kayıt Modeli

| Alan | Değer |
|---|---|
| **Aktör** | Repository implementasyonu (Infrastructure) |
| **Tetikleyici** | Aggregate persist edildiğinde domain event'leri outbox'a yazılmalı |
| **Ana Akış** | 1. `OutboxRecord` (id, type, payload, occurredAt, processedAt). 2. `OutboxMapperExtension`: domain event → `OutboxRecord`. |
| **Sonuç** | Outbox modeli Application'da, tablo Infrastructure'da |
| **Kaynak Dosyalar** | `Outbox/OutboxRecord.cs`, `Outbox/OutboxMapperExtension.cs` |

### UC-APP-09 — Integration Event'ler ve Mapping

| Alan | Değer |
|---|---|
| **Aktör** | Outbox publisher |
| **Tetikleyici** | Bir outbox kaydı işlenmeye hazır |
| **Ana Akış** | 1. `OrderIntegrationEvents` (OrderCreatedV1, OrderCancelledV1) record'ları. 2. `OrderIntegrationEventMapper`: domain event → integration event. |
| **Sonuç** | Dış kontrat (integration event) iç kontrattan (domain event) ayrıştı |
| **Kaynak Dosyalar** | `Integration/OrderIntegrationEvents.cs`, `Integration/OrderIntegrationEventMapper.cs` |

### UC-APP-10 — Spring Configuration (DI)

| Alan | Değer |
|---|---|
| **Aktör** | Spring Boot |
| **Tetikleyici** | Uygulama başlatma |
| **Ana Akış** | 1. `@Configuration ApplicationConfig`. 2. Handler'ları bean olarak topla. 3. Pipeline behavior chain'ini bean olarak yayınla. |
| **Sonuç** | Application katmanı tek import ile devreye girer |
| **Kaynak Dosya** | `DependencyInjection/ServiceCollectionExtensions.cs` |

---

## 2. Task Listesi (Yürütme Sırası)

> Her task'tan sonra `./mvnw compile` yeşil kalmalı. Her use-case için en az bir birim test yazılmalı.

### Faz 1 — Soyutlamalar (handler'lar yazılamadan önce zorunlu)
- [ ] **T1.1** Paket iskeleti: `org.phoenix.demo.ordermanagement.application.{abstractions,behaviours,orders,outbox,integration,config}`
- [ ] **T1.2** UC-APP-01: CQRS interface'leri (`Command`, `CommandHandler`, `Query`, `QueryHandler`)
- [ ] **T1.3** UC-APP-02: `OrderRepository`, `OrderPersistenceState`, `OutboxProcessedMarker`
- [ ] **T1.4** UC-APP-03: `IntegrationEventPublisher`, `IntegrationAuditLogger`
- [ ] **T1.5** Smoke testi: interface'ler derleniyor, sadece domain+common bağımlılığı var (ArchUnit veya manuel grep)

### Faz 2 — İlk uçtan uca komut (template oluşturma)
- [ ] **T2.1** UC-APP-04: `CreateOrderCommand` (record + jakarta.validation anotasyonları)
- [ ] **T2.2** UC-APP-04: `CreateOrderCommandHandler` — Domain `Order.create()` çağrısı + repository
- [ ] **T2.3** Birim test: in-memory `OrderRepository` ile happy-path + validation hata path'i

### Faz 3 — Pipeline behavior'lar
- [ ] **T3.1** UC-APP-07: `PipelineBehavior<C,R>` interface + `CommandBus` (zincir kur)
- [ ] **T3.2** UC-APP-07: `ValidationPipelineBehavior` (jakarta Validator)
- [ ] **T3.3** Test: geçersiz komut → handler çağrılmaz, `Result.failure` döner

### Faz 4 — Kalan komutlar ve sorgular
- [ ] **T4.1** UC-APP-05: `CancelOrderCommand` + handler + test
- [ ] **T4.2** UC-APP-06: `GetOrderByIdQuery` + handler + `OrderSummaryDto` + test
- [ ] **T4.3** Sample-project'teki diğer komut/sorgu klasörleri taranıp listelenir, eksiksiz portlanır

### Faz 5 — Outbox & Integration
- [ ] **T5.1** UC-APP-08: `OutboxRecord` record + `OutboxMapper`
- [ ] **T5.2** UC-APP-09: `OrderIntegrationEvents` record'ları
- [ ] **T5.3** UC-APP-09: `OrderIntegrationEventMapper` (domain → integration) + test

### Faz 6 — DI ve sızıntı kontrolü
- [ ] **T6.1** UC-APP-10: `ApplicationConfig @Configuration` — handler ve behavior bean'leri
- [ ] **T6.2** ArchUnit kuralı: `application` paketinden `infrastructure`/`api` paketlerine bağımlılık YASAK
- [ ] **T6.3** ArchUnit kuralı: `application` içinde `jakarta.persistence.*` ve `org.springframework.data.*` import'u YASAK
- [ ] **T6.4** Tüm test suite yeşil; `./mvnw verify` başarılı

---

## 3. Disiplin Kuralları

1. **Bağımlılık yönü:** Application → Domain + Common. Asla tersi.
2. **Framework dozu:** Sadece `@Service`, `@Component`, `@Configuration`, `jakarta.validation.*` kabul. JPA/Web/Data **yasak**.
3. **Result pattern:** Handler'lar exception fırlatmaz; `Result<T>` döner (Common'da port edildiği varsayılır).
4. **Test edilebilirlik:** Her handler in-memory repository ile test edilebilmeli (Spring context'i yüklenmeden).
5. **Commit disiplini:** Her task ayrı commit. Mesaj formatı: `feat(application): UC-APP-04 CreateOrder command + handler`.

---

## 4. Sıradaki Adım

`T1.1` ile başla: paket iskeletini oluştur, sonra `T1.2`'de CQRS interface'lerini sample-project'in `ICommand.cs` / `ICommandHandler.cs` içeriğini bire bir okuyarak Java'ya çevir.