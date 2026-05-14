#!/usr/bin/env bash
set -euo pipefail

# Azure Service Bus setup for ddd-eda-demo
# Prereqs: az login

RG="${RG:-flame-demo-rg}"
LOCATION="${LOCATION:-westeurope}"
NS="${NS:-flame-demo-sb-$(date +%Y%m%d)}"
TOPIC="${TOPIC:-order-events}"
SUBSCRIPTION="${SUBSCRIPTION:-demo-consumer}"
SKU="${SKU:-Standard}"

echo "==> Resource group: $RG ($LOCATION)"
az group create -n "$RG" -l "$LOCATION" -o none

echo "==> Namespace: $NS ($SKU)"
az servicebus namespace create \
  -g "$RG" -n "$NS" --sku "$SKU" -l "$LOCATION" -o none

echo "==> Topic: $TOPIC"
az servicebus topic create \
  -g "$RG" --namespace-name "$NS" -n "$TOPIC" -o none

echo "==> Subscription: $SUBSCRIPTION"
az servicebus topic subscription create \
  -g "$RG" --namespace-name "$NS" \
  --topic-name "$TOPIC" -n "$SUBSCRIPTION" -o none

echo "==> Connection string:"
CONN=$(az servicebus namespace authorization-rule keys list \
  -g "$RG" --namespace-name "$NS" \
  -n RootManageSharedAccessKey \
  --query primaryConnectionString -o tsv)

cat <<EOF

============================================================
Service Bus ready. Add these to IntelliJ Run Config env vars:
============================================================

SERVICEBUS_ENABLED=true
SERVICEBUS_PUBLISHER_ENABLED=true
SERVICEBUS_CONSUMER_ENABLED=true
SERVICEBUS_TOPIC=$TOPIC
SERVICEBUS_SUBSCRIPTION=$SUBSCRIPTION
SERVICEBUS_CONNECTION_STRING=$CONN

============================================================
Cleanup when done:
  az servicebus namespace delete -g $RG -n $NS
============================================================
EOF