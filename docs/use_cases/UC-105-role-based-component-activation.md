# Use Case: Role-Based Component Activation

## Overview

**Use Case ID:** UC-105
**Use Case Name:** Role-Based Component Activation (`app.role` gating)
**Primary Actor:** Operations Engineer (deployment time) / Spring Boot Application Container (runtime)
**Goal:** Aynı uygulama artifact'ı `app.role` değerine göre yalnızca API, yalnızca Worker veya her ikisini birden çalıştırabilsin.
**Status:** Draft

## Preconditions

- Uygulama paketlendi; `app.role` property'si konfigüre edilebilir.
- `@WorkerComponent` meta-anotasyonu (Phase 2) ve API controller'larındaki `app.role ∈ {api, both}` gating (Phase 1) mevcut.

## Main Success Scenario (app.role=both)

1. Operations Engineer `APP_ROLE=both ./mvnw spring-boot:run` ile uygulamayı başlatır.
2. Spring property resolver `app.role=both` değerini okur.
3. API controller'ları (`@ConditionalOnExpression`) yüklenir; HTTP 8080 dinlenir.
4. `@WorkerComponent` ile işaretli tüm bean'ler (`OutboxChangeFeedProcessor`, `LoggingIntegrationEventPublisher`, `CosmosOutboxProcessedMarker`, `NoopIntegrationAuditLogger`) yüklenir.
5. UC-101 akışı çalışır; processor başlar.
6. Aynı process içinde hem API hem worker aktif.

## Alternative Flows

### A1: API-only Mode

**Trigger:** `APP_ROLE=api`
**Flow:**

1. API controller'lar yüklenir.
2. `@WorkerComponent` bean'leri yüklenmez (condition expression false).
3. `OutboxChangeFeedProcessor` instantiate edilmez; UC-101 hiç tetiklenmez.
4. Smoke test: log'da `Started OutboxChangeFeedProcessor` görünmemeli.
5. HTTP endpoint'leri normal çalışır.

### A2: Worker-only Mode

**Trigger:** `APP_ROLE=worker`
**Flow:**

1. API controller'ları yüklenmez (`app.role ∈ {api, both}` koşulu false).
2. `@WorkerComponent` bean'leri yüklenir.
3. UC-101 çalışır; processor başlar.
4. HTTP endpoint'ler çağrılamaz (controller yok veya 404).

### A3: Geçersiz Rol Değeri

**Trigger:** `APP_ROLE=foobar`
**Flow:**

1. Hem API hem worker condition expression'ları false döner.
2. Uygulama başlar ancak işlevsel bean yoktur; sadece sağlık endpoint'i (varsa) cevap verir.
3. Operations Engineer log üzerinden farkeder ve konfigürasyonu düzeltir.

## Postconditions

### Success Postconditions

- Doğru bean seti aktive edildi; istenmeyen bileşenler hiç oluşturulmadı.
- Operasyon ekibi tek artifact ile dağıtım yapabiliyor.

### Failure Postconditions

- Geçersiz rolde uygulama "boş" çalışır; iş yapılmaz.

## Business Rules

### BR-114: Default Rol

`app.role` set edilmemişse default `both` davranışı uygulanır (geliştirici deneyimi için).

### BR-115: Anotasyon Tabanlı Gating

Worker bileşenleri her seferinde aynı `@ConditionalOnExpression` ifadesini yazmak yerine `@WorkerComponent` meta-anotasyonunu kullanır.

### BR-116: Tek Artifact, Çoklu Rol

API ve Worker ayrı deploy artifact'larına bölünmez; aynı JAR farklı `app.role` değerleriyle iki kez deploy edilebilir.