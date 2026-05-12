# Use Case: Coordinate Multiple Worker Instances via Lease Container

## Overview

**Use Case ID:** UC-104
**Use Case Name:** Coordinate Multiple Worker Instances via Lease Container
**Primary Actor:** Cosmos Change Feed Processor SDK (system actor)
**Secondary Actors:** Worker Instance A, Worker Instance B, `leases` container
**Goal:** Aynı `processorName` ile başlatılan birden fazla worker instance'ı arasında partition lease'lerini paylaştırıp her outbox event'in yalnızca bir instance tarafından dispatch edilmesini sağlamak.
**Status:** Draft

## Preconditions

- En az iki worker instance `app.role=worker` ile başlatıldı (UC-101).
- Her ikisi de aynı `processorName` ve aynı `leases` container'ını kullanıyor.
- `instanceName` (host adı) her instance için farklı.

## Main Success Scenario

1. Instance A başlar; `leases` container'da `processorName.` prefix'li lease dokümanlarını oluşturur veya devralır.
2. Instance B başlar; aynı prefix'li lease'leri arar.
3. Cosmos SDK partition'ları iki host arasında yeniden dengeler (rebalance).
4. Her partition için tek bir owner host belirlenir.
5. Yeni bir outbox dokümanı yazıldığında, ilgili partition'ın owner'ı bu item'ı handler'ına alır.
6. Sadece bir instance UC-102 ve UC-103 akışını çalıştırır.

## Alternative Flows

### A1: Instance Crash

**Trigger:** Owner instance ani şekilde durur (adım 5)
**Flow:**

1. Lease'in heartbeat süresi dolar.
2. Diğer instance lease'i devralır.
3. Devralan instance, son checkpoint'ten itibaren Change Feed'i okumaya devam eder.
4. Crash anında işlenmemiş item'lar yeni owner tarafından işlenir; `processed=true` olanlar UC-102 A3 ile atlanır.

### A2: Üçüncü Instance Eklendi

**Trigger:** Yeni bir worker `app.role=worker` ile başladı
**Flow:**

1. SDK rebalance tetikler.
2. Mevcut iki instance'tan partition'lar yeni instance'a da paylaştırılır.
3. Use case adım 5'ten devam.

### A3: Aynı Host Adı ile İki Instance

**Trigger:** `instanceName` aynı (kötü konfigürasyon)
**Flow:**

1. Lease container'da host kayıtları çakışır.
2. SDK undefined davranış sergileyebilir; lease ownership belirsizleşir.
3. Operasyon ekibi farklı `HOSTNAME` env var atayarak düzeltir.

## Postconditions

### Success Postconditions

- Her partition için tek owner instance var.
- Her outbox item yalnızca bir kez dispatch edilir (publish başarısı varsayımıyla).
- Lease container, host listesi ve checkpoint bilgisini barındırır.

### Failure Postconditions

- Yanlış konfigürasyon (aynı host adı) → ownership çakışması; manuel müdahale gerekir.

## Business Rules

### BR-111: Tek Logical Processor, Çoklu Host

Aynı `processorName` paylaşan tüm instance'lar tek bir mantıksal processor'ı oluşturur; iş bölüşümü partition seviyesindedir.

### BR-112: Lease Prefix İzolasyonu

Farklı `processorName` değerleri lease container'ı paylaşsa bile birbirinden izole çalışır.

### BR-113: Restart Sonrası Süreklilik

Restart edilen instance, lease checkpoint'inden itibaren devam eder; daha önce işlenmiş item'lar `processed=true` filtresiyle atlanır.