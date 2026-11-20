#!/usr/bin/env bash
# Generates realistic traffic against the backend so the Grafana dashboard has data.
# Usage: bash scripts/generate-traffic.sh [rounds]     (default 20 rounds, ~1 s each)
# Optional: BASE_URL=http://localhost:8080 bash scripts/generate-traffic.sh 40
BASE="${BASE_URL:-http://localhost:8080}"
ROUNDS="${1:-20}"
EMAIL="load$(date +%s)@example.com"
PASSWORD="password123"
JSON="Content-Type: application/json"

echo "Registering $EMAIL ..."
TOKEN=$(curl -s -X POST "$BASE/api/auth/register" -H "$JSON" \
  -d "{\"name\":\"Load Tester\",\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\"}" \
  | grep -o '"token":"[^"]*"' | cut -d'"' -f4)
if [ -z "$TOKEN" ]; then echo "Could not register - is the backend running on $BASE ?"; exit 1; fi
AUTH="Authorization: Bearer $TOKEN"

for i in $(seq 1 "$ROUNDS"); do
  PID=$(( (i % 14) + 1 ))
  # browsing (public endpoints)
  curl -s -o /dev/null "$BASE/api/products"
  curl -s -o /dev/null "$BASE/api/products?search=laptop&sort=price_asc"
  curl -s -o /dev/null "$BASE/api/products/$PID"
  curl -s -o /dev/null "$BASE/api/categories"
  # cart: add -> change quantity -> view -> remove
  ITEM=$(curl -s -X POST "$BASE/api/cart/items" -H "$AUTH" -H "$JSON" \
    -d "{\"productId\":$PID,\"quantity\":1}" | grep -o '"id":[0-9]*' | head -1 | cut -d: -f2)
  if [ -n "$ITEM" ]; then
    curl -s -o /dev/null -X PUT "$BASE/api/cart/items/$ITEM" -H "$AUTH" -H "$JSON" -d '{"quantity":2}'
    curl -s -o /dev/null "$BASE/api/cart" -H "$AUTH"
    curl -s -o /dev/null -X DELETE "$BASE/api/cart/items/$ITEM" -H "$AUTH"
  fi
  # a few normal "client mistakes" (4xx) now and then
  if [ $((i % 4)) -eq 0 ]; then
    curl -s -o /dev/null "$BASE/api/products/9999"                       # 404
    curl -s -o /dev/null "$BASE/api/cart"                                 # 401 (no token)
    curl -s -o /dev/null -X POST "$BASE/api/auth/login" -H "$JSON" \
      -d '{"email":"nobody@example.com","password":"wrong-password"}'     # 401 failed login
  fi
  echo "round $i/$ROUNDS done"
  sleep 1
done
echo "Finished. Look at the Grafana dashboard (http://localhost:3000)."
