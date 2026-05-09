# Notlar

---

### S: `OrderCancelledEvent` sınıfındaki neden `final` anahtar kelimesi kullanılmıştır?

**C:** `final class` + `final fields` kombinasyonu, domain event'in immutable, type-safe ve güvenli bir value object olmasını sağlar — DDD'nin temel prensiplerinden biri.

---

### S: `Order` sınıfı incele, sonrasında Aggregate Root konusunu anlat?

**C:**

#### DDD Tanımı

- **Aggregate** = Birlikte tutarlı kalması gereken nesneler kümesi.
- **Aggregate Root** = Bu kümenin tek giriş kapısı (entry point) olan ana entity.

> **Kural:** Dışarıdan yapılan tüm değişiklikler Aggregate Root üzerinden geçer. Bu sayede invariant'lar (iş kuralları) korunur.

---

#### Kısaca

> Aggregate Root, domain'inizin patronudur. İçerideki çalışanlarla (entity ve value object'lerle) doğrudan konuşamazsınız — önce patrondan randevu alırsınız. Patron, kuralların çiğnenmemesini, herkesin uyumlu çalışmasını ve dışarıya tutarlı bir mesaj gitmesini garanti eder.

`Order` sınıfı bu rolü çok temiz bir şekilde oynuyor: immutable kimlik + kontrollü davranış + olay yayma + invariant koruma. Klasik bir DDD aggregate root örneği. 👌

---

### S: `application/abstractions/cqrs` paketini incele ve açıkla?

**C:**

Bu paket, uygulamanın kendi mini **CQRS + Mediator altyapısını** tanımlar. .NET dünyasındaki **MediatR** kütüphanesinin Java karşılığı olarak düşünülebilir.

#### CQRS Nedir?

> **CQRS = Command Query Responsibility Segregation**
>
> Veriyi **değiştiren** işlemler (Command) ile veriyi **okuyan** işlemler (Query) ayrı tutulmalıdır.

- **Command** → state değiştirir (örn: `PlaceOrderCommand`)
- **Query** → state okur, değiştirmez (örn: `GetOrderByIdQuery`)

#### Pakette Ne Var?

```
Request<R>           ← her istek (giriş kapısı)
  ├── Command<R>     ← yazma işlemleri
  └── Query<R>       ← okuma işlemleri

RequestHandler<REQ, R>          ← bir isteği işleyen
  ├── CommandHandler<C, R>
  └── QueryHandler<Q, R>

PipelineBehavior<REQ, R>        ← ara katman (logging, validation, vb.)
Next<R>                         ← bir sonraki adımı çağıran fonksiyon
RequestDispatcher               ← isteği doğru handler'a yönlendiren mediator
```

#### Her Sınıfın Rolü

| Tip | Rol | Analoji |
|-----|-----|---------|
| `Request<R>` | Tüm isteklerin temel marker interface'i. `R` = dönüş tipi. | Posta zarfı |
| `Command<R>` | Yazma niyetini ifade eder. | "Şu siparişi iptal et" mektubu |
| `Query<R>` | Okuma niyetini ifade eder. | "Şu siparişin durumu ne?" mektubu |
| `RequestHandler<REQ, R>` | Bir isteği gerçekten işleyen kod. | Mektubu okuyan ve işleyen kişi |
| `CommandHandler<C, R>` | Sadece Command tipindeki istekleri işler. | Yazma işlemi uzmanı |
| `QueryHandler<Q, R>` | Sadece Query tipindeki istekleri işler. | Okuma işlemi uzmanı |
| `RequestDispatcher` | İsteği uygun handler'a yönlendiren mediator. | Posta dağıtıcı / merkez santrali |
| `PipelineBehavior<REQ, R>` | Handler çağrılmadan önce/sonra araya giren ara katman. | Güvenlik / logging görevlisi |
| `Next<R>` | Pipeline'da "bir sonraki adıma geç" fonksiyonu. | "Mektubu sonraki memura ver" |

#### Çalışma Akışı

```
Controller
    │
    │  dispatcher.dispatch(new CancelOrderCommand("123"))
    ▼
RequestDispatcher  ──►  PipelineBehavior #1 (Validation)
                              │
                              │ next.proceed()
                              ▼
                       PipelineBehavior #2 (Logging)
                              │
                              │ next.proceed()
                              ▼
                       PipelineBehavior #3 (Transaction)
                              │
                              │ next.proceed()
                              ▼
                       CommandHandler.handle(cmd)
                              │
                              ▼
                          Domain (Order)
```

> Her `PipelineBehavior`, isteği alır → kendi işini yapar (örn: validate) → `next.proceed()` ile bir sonrakine devreder. **Russian doll** (iç içe matruşka) mantığı.

#### Generic Tipler Type-Safety Sağlar

```java
RequestHandler<REQ extends Request<R>, R>
```

- `CancelOrderCommand implements Command<Result<Void, DomainError>>`
- `CancelOrderHandler implements CommandHandler<CancelOrderCommand, Result<Void, DomainError>>`

Compiler, yanlış handler'ı yanlış command'e bağlamanıza izin vermez. 🛡️

#### Pratik Örnek

```java
// 1. Command tanımla
public record CancelOrderCommand(String orderId)
    implements Command<Result<Void, DomainError>> {}

// 2. Handler yaz
public class CancelOrderHandler
    implements CommandHandler<CancelOrderCommand, Result<Void, DomainError>> {

    public Result<Void, DomainError> handle(CancelOrderCommand cmd) {
        // domain işlemi...
    }
}

// 3. Çağır
dispatcher.dispatch(new CancelOrderCommand("ORD-123"));
```

#### Bu Tasarımın Faydaları

| Fayda | Nasıl? |
|-------|--------|
| **Decoupling** | Controller, hangi handler'ın çalıştığını bilmez — sadece dispatcher'a iletir |
| **Cross-cutting concerns** | Logging, validation, transaction, retry → `PipelineBehavior` ile tek yerden |
| **Test edilebilirlik** | Her handler tek bir iş yapar, kolay mock'lanır |
| **CQRS netliği** | Yazma vs okuma kod yolu net olarak ayrılır |
| **Type safety** | Generic'ler sayesinde derleme zamanında yanlış eşleşme tespit edilir |

#### Kısaca

> Bu paket, `Controller → Use Case → Domain` zincirine **disiplinli bir omurga** kurar. Her istek bir **zarf (Request)**, her zarf doğru **uzmana (Handler)** gider, yolda da **denetçiler (PipelineBehavior)** araya girer. Tüm bunları **postacı (RequestDispatcher)** koordine eder.

Sade, type-safe ve genişletilebilir bir mini-MediatR. 👌

---

### S: `application/behaviours`, `application/config`, `application/dispatcher` paketlerini incele ve açıkla?

**C:**

Bir önceki sorudaki `cqrs` paketi sadece **interface'leri** tanımlıyordu (sözleşmeler). Bu üç paket ise o sözleşmelerin **gerçek hayattaki uygulamasını** içerir — yani CQRS altyapısının çalışan motoru.

#### Genel Bakış

```
abstractions/cqrs/   →  Sözleşmeler (interface'ler)         "NE olacak?"
   │
   ├── behaviours/   →  Pipeline davranışları                "Ara katmanlar"
   ├── dispatcher/   →  Mediator implementasyonu             "Postacı"
   └── config/       →  Spring tutkalı (DI, Bean tanımları)  "Birleştirici"
```

---

#### 1. `behaviours/ValidationPipelineBehavior`

**Görev:** Her command/query, handler'a ulaşmadan önce **otomatik olarak doğrulanır** (Bean Validation, `jakarta.validation`).

**Çalışma Mantığı:**

```java
public R handle(REQ request, Next<R> next) {
    var violations = validator.validate(request);   // 1. Doğrula

    if (!violations.isEmpty()) {
        throw new ApplicationValidationException(errors);  // 2. Hatalıysa fırlat
    }

    return next.proceed();   // 3. Geçerliyse zincirde devam et
}
```

**Pratik Anlamı:**

```java
public record CreateOrderCommand(
    @NotBlank String customerId,
    @Positive BigDecimal amount
) implements Command<...> {}
```

> Handler'a `customerId` boş gelmez. Çünkü zincirin başında bu davranış yakalar. **Validation kodu her handler'a tekrar tekrar yazılmaz.** Bu, **cross-cutting concern** çözümünün klasik örneğidir.

**Analoji:** Mektubu uzmana götürmeden önce **giriş kontrolünden** geçirmek. Eksik adres varsa içeri sokmaz.

---

#### 2. `dispatcher/SpringRequestDispatcher`

Bu paketin **kalbi**. Mini-MediatR'ın gerçek implementasyonu.

##### A) Handler Registry Kurmak (Constructor)

```java
this.handlersByRequestType = indexHandlersByRequestType(handlers);
```

Spring tüm `RequestHandler` bean'lerini topluyor, dispatcher bunları bir `Map<RequestType, Handler>` olarak indeksliyor:

```
CreateOrderCommand   →  CreateOrderCommandHandler
CancelOrderCommand   →  CancelOrderCommandHandler
GetOrderByIdQuery    →  GetOrderByIdQueryHandler
```

`GenericTypeResolver` ile her handler'ın hangi request tipini işlediği **runtime'da generic'ten okunuyor** (Spring magic). 🔍

> **Çakışma kontrolü:** Aynı request tipi için iki handler varsa `IllegalStateException` fırlatır. Süper güvenli.

##### B) Dispatch Etmek

```java
public <R, REQ extends Request<R>> R dispatch(REQ request) {
    RequestHandler handler = handlersByRequestType.get(request.getClass());

    Next<R> chain = () -> handler.handle(request);   // 1. Zincirin sonu

    for (int i = behaviors.size() - 1; i >= 0; i--) {
        PipelineBehavior behavior = behaviors.get(i);
        Next<R> currentChain = chain;
        chain = () -> behavior.handle(request, currentChain);  // 2. Sarmala
    }

    return chain.proceed();   // 3. Çalıştır
}
```

##### Russian Doll Yapısı

Behavior'lar **tersten** sarmalanır. Sonuçta oluşan zincir:

```
proceed()
  └─► Behavior #1 (Validation)
        └─► Behavior #2 (Logging)
              └─► Behavior #3 (Transaction)
                    └─► Handler.handle(request)  ◄── gerçek iş
```

Her halka, bir sonrakini `next.proceed()` ile çağırır. Klasik **Chain of Responsibility** pattern'i.

**Analoji:** Postanedeki **dağıtım merkezi**:
1. Mektubu alır
2. Hangi uzmana gideceğini adres defterinden (Map) bulur
3. Önce güvenlik kontrolünden, sonra loglama görevlisinden, sonra muhasebeden geçirir
4. En sonunda doğru uzmana ulaştırır

---

#### 3. `config/ApplicationConfig`

Spring'in **tutkalı**. Tüm parçaları DI container'a tanıtır.

| Bean | Görev |
|------|-------|
| `createOrderCommandHandler` | Yeni sipariş oluşturma handler'ı |
| `cancelOrderCommandHandler` | Sipariş iptal handler'ı |
| `getOrderByIdQueryHandler` | Sipariş sorgulama handler'ı |
| `validationPipelineBehavior` | Validation ara katmanı |
| `requestDispatcher` | **Hepsini bir araya getiren mediator** |

**Önemli Detay:**

```java
@Bean
public RequestDispatcher requestDispatcher(
    List<RequestHandler<?, ?>> handlers,        // ← Spring tüm handler'ları toplayıp verir
    List<PipelineBehavior<?, ?>> behaviors      // ← Tüm behavior'ları toplayıp verir
) {
    return new SpringRequestDispatcher(handlers, behaviors);
}
```

> Spring, parametrede `List<X>` görünce **tüm X tipli bean'leri otomatik enjekte eder**. Yeni bir handler eklendiğinde dispatcher'ı **dokunmaya gerek yok** — Spring keşfeder.

**Niye `@Configuration` ile Manuel? `@Component` Değil?**

Handler'lar manuel `@Bean` olarak tanımlanmış — `@Service`/`@Component` yerine. Sebep:

- Application katmanı **framework'e bağımsız** kalsın
- Handler sınıflarında Spring annotation'ı yok → **saf POJO**
- Spring bağımlılığı sadece config katmanında merkezileşmiş

> Bu, **Hexagonal/Clean Architecture** prensibine uygun bir tercih. 👏

---

#### Üç Paketin Birlikte Çalışması (End-to-End Akış)

```
1. Controller:
   dispatcher.dispatch(new CreateOrderCommand("C1", 100.0));

2. SpringRequestDispatcher:
   ├─ Map'ten CreateOrderCommandHandler'ı bul
   └─ Behavior zincirini sarmala

3. ValidationPipelineBehavior:
   ├─ @NotBlank, @Positive vs. doğrula
   ├─ Hatalıysa → ApplicationValidationException
   └─ Geçerliyse → next.proceed()

4. CreateOrderCommandHandler:
   ├─ Order.placeNew(...) çağır
   ├─ orderRepository.save(...)
   └─ Result<Order, DomainError> döndür

5. Sonuç Controller'a geri akar.
```

---

#### Tasarımın Kazandırdıkları

| Özellik | Açıklama |
|---------|----------|
| **Decoupling** | Controller, hangi handler'ın çalıştığını bilmiyor |
| **Otomatik registry** | `GenericTypeResolver` ile handler-request eşleşmesi otomatik |
| **Genişletilebilirlik** | Yeni handler/behavior ekle → config'e satır ekle, gerisi otomatik |
| **Cross-cutting concerns** | Validation tek noktada — ileride logging, transaction, retry kolayca eklenir |
| **Framework izolasyonu** | Application katmanı saf Java; Spring sadece config'de |
| **Type safety** | Generic'lerle compile-time doğrulama, dispatch'te runtime kontrol |
| **Çakışma koruması** | Aynı request için iki handler varsa açılışta hata |

#### Kısaca

> Bu üç paket birlikte, `cqrs/` altındaki interface'lerin **canlı çalışan motorunu** kurar.
>
> - **`behaviours/`** = Validation gibi cross-cutting kuralları yazdığımız yer
> - **`dispatcher/`** = Request'i doğru handler'a yönlendiren ve pipeline'ı zincirleyen motor
> - **`config/`** = Spring DI ile tüm parçaları birleştiren tutkal

Saf Java handler + Spring tutkalı + chain-of-responsibility pipeline = **temiz, esnek, test edilebilir bir mini-MediatR**. 🎯