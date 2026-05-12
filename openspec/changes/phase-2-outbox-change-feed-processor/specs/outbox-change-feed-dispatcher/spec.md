## ADDED Requirements

### Requirement: Worker süreç gating
Sistem, worker bean'lerini yalnızca `app.role` konfigürasyon değeri `worker` veya `both` olduğunda yüklemeli; `api` modunda Cosmos Change Feed Processor instance edilmemelidir.

#### Scenario: API modunda processor başlamaz
- **WHEN** uygulama `APP_ROLE=api` ile başlatılır
- **THEN** `OutboxChangeFeedProcessor` bean'i Spring context'te bulunmaz ve `SmartLifecycle.start()` çağrısı gerçekleşmez

#### Scenario: Worker modunda processor başlar
- **WHEN** uygulama `APP_ROLE=worker` ile başlatılır
- **THEN** `OutboxChangeFeedProcessor` bean'i instance edilir ve context refresh sonrası `start()` çağrısı yapılır

#### Scenario: Both modunda hem API hem worker bean'leri yüklenir
- **WHEN** uygulama `APP_ROLE=both` ile başlatılır
- **THEN** REST controller'lar ve `OutboxChangeFeedProcessor` aynı süreçte aktif olur

### Requirement: Change Feed üzerinden outbox dispatch
Sistem, `orders` Cosmos container'ındaki değişiklikleri Change Feed Processor ile sürekli okumalı; her batch içindeki `_type=outbox` dokümanlarını `IntegrationEventPublisher` üzerinden yayınlamalıdır.

#### Scenario: Yeni outbox kaydı yayınlanır
- **WHEN** `orders` container'ına `_type=outbox`, `processed=false` bir doküman yazılır
- **THEN** Change Feed Processor 5 saniye içinde dokümanı okur ve `IntegrationEventPublisher.publishOutbox(tenantId, outboxItemId, orderId, eventType, payloadJson)` çağrısı yapılır

#### Scenario: Outbox dışı doküman atlanır
- **WHEN** Change Feed batch'i içinde `_type=order` bir doküman bulunur
- **THEN** `IntegrationEventPublisher` çağrılmaz ve doküman için `markProcessed` patch'i uygulanmaz

#### Scenario: Geçersiz outbox dokümanı işlenmez
- **WHEN** Change Feed item'ı `_type=outbox` fakat `tenantId` null veya `id` boş ise
- **THEN** WARN seviyesinde log yazılır ve `IntegrationEventPublisher` çağrılmaz

### Requirement: İdempotent işleme
Sistem, başarıyla yayınlanan her outbox dokümanını Cosmos JSON patch ile `processed=true` olarak işaretlemeli; restart veya feed replay durumunda aynı doküman tekrar yayınlanmamalıdır.

#### Scenario: Yayın sonrası processed=true patch
- **WHEN** `IntegrationEventPublisher.publishOutbox(...)` exception fırlatmadan tamamlanır
- **THEN** `OutboxProcessedMarker.markProcessed(tenantId, outboxItemId)` çağrılır ve Cosmos dokümanının `/processed` alanı `true` olur

#### Scenario: processed=true kaydı skip edilir
- **WHEN** Change Feed batch'inde `_type=outbox`, `processed=true` bir doküman gelir
- **THEN** `IntegrationEventPublisher` çağrılmaz ve dispatch pipeline'ı bu item için atlanır

#### Scenario: Restart sonrası tekrar yayın olmaz
- **WHEN** worker daha önce yayınladığı bir outbox dokümanını lease checkpoint'inden okurken yeniden başlatılır
- **THEN** lease checkpoint'i sayesinde aynı item tekrar batch'e gelmez veya gelse bile `processed=true` skip kuralı devreye girer; tek bir publish log entry'si gözlemlenir

### Requirement: Hata izolasyonu ve at-least-once semantiği
Sistem, batch içinde bir item'ın işlenmesi sırasında exception fırlatılırsa kalan item'ları işlemeye devam etmeli; başarısız item için Change Feed Processor mekanizması batch'i replay edebilmelidir.

#### Scenario: Batch içinde tekil hata diğer item'ları durdurmaz
- **WHEN** batch'teki bir item'ın `handleItem` çağrısı RuntimeException fırlatır
- **THEN** ERROR log yazılır, aynı batch'teki sonraki item'lar işlenmeye devam eder

#### Scenario: Calışan publisher hatası replay ile düzeltilir
- **WHEN** transient publisher hatası nedeniyle bir item'ın `markProcessed` çağrısı tamamlanmadan worker durur
- **THEN** Change Feed Processor lease checkpoint'i bu batch'i tekrar dağıttığında, item'ın `processed=false` olması beklenir ve yeniden yayınlanır (at-least-once)

### Requirement: Lease coordination ve horizontal scale
Sistem, aynı `processor-name` ile birden fazla worker instance çalıştığında lease container üzerinden Change Feed partition'larını instance'lar arasında otomatik paylaştırmalıdır.

#### Scenario: İki instance ile tek doküman tek instance'ta işlenir
- **WHEN** iki worker süreci aynı `processor-name` ve farklı `instance-name` ile başlatılır ve `orders` container'a tek outbox kaydı yazılır
- **THEN** publish log entry'si yalnızca bir instance'ta görülür

#### Scenario: Bir instance düştüğünde diğeri devralır
- **WHEN** iki instance'tan biri shutdown olur
- **THEN** kalan instance, düşen instance'a ait lease'leri kısa süre içinde devralır ve dispatch işlemine kesintisiz devam eder

### Requirement: Konfigüre edilebilir Change Feed davranışı
Sistem, processor adı, instance adı, feed poll gecikmesi ve maksimum batch boyutu için `app.outbox.changefeed.*` altında konfigürasyon sağlamalıdır.

#### Scenario: Default değerlerle çalışır
- **WHEN** `application.yml` içinde `app.outbox.changefeed` bloğu eksiktir
- **THEN** processor `processor-name=outbox-dispatcher`, `feed-poll-delay=1s`, `max-items-per-batch=100` default değerleri ile başlar

#### Scenario: Override edilen değerler uygulanır
- **WHEN** `app.outbox.changefeed.feed-poll-delay=5s` ve `max-items-per-batch=10` set edilir
- **THEN** Change Feed Processor builder bu değerlerle yapılandırılır

### Requirement: Default integration publisher ve audit logger override edilebilir
Sistem, `IntegrationEventPublisher` ve `IntegrationAuditLogger` için default implementasyon sağlamalı; başka bir bean tanımlanırsa default geri çekilmelidir.

#### Scenario: Default logging publisher
- **WHEN** uygulama context'inde `IntegrationEventPublisher` bean'i başka bir tanım ile yer almıyorsa
- **THEN** `LoggingIntegrationEventPublisher` aktif olur ve INFO seviyesinde tenantId, orderId, outboxItemId, eventType, payload length log'lar

#### Scenario: Custom publisher default'u override eder
- **WHEN** Spring context'e başka bir `IntegrationEventPublisher` impl. bean'i eklenir
- **THEN** `LoggingIntegrationEventPublisher` `@ConditionalOnMissingBean` nedeniyle yüklenmez ve sadece custom impl çalışır