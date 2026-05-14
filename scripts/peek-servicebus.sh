#!/usr/bin/env bash
set -euo pipefail

# Peek message counts on the topic subscription.
RG="${RG:-flame-demo-rg}"
NS="${NS:?set NS=<namespace-name>}"
TOPIC="${TOPIC:-order-events}"
SUBSCRIPTION="${SUBSCRIPTION:-demo-consumer}"

echo "==> Topic count details:"
az servicebus topic show \
  -g "$RG" --namespace-name "$NS" -n "$TOPIC" \
  --query countDetails -o jsonc

echo
echo "==> Subscription count details:"
az servicebus topic subscription show \
  -g "$RG" --namespace-name "$NS" \
  --topic-name "$TOPIC" -n "$SUBSCRIPTION" \
  --query "{messageCount:messageCount, countDetails:countDetails}" -o jsonc