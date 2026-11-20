#!/usr/bin/env bash
# Triggers CONTROLLED failures so you can show them in the logs and on the dashboard.
# Usage: bash scripts/trigger-failure.sh [error|slow]     (default: error)
#   error -> 20 calls to /api/debug/error  => HTTP 500, ERROR log with stack trace,
#                                             techmart_errors_total{type="unexpected"} goes up
#   slow  -> 10 calls to /api/debug/slow   => high latency on the latency panels
BASE="${BASE_URL:-http://localhost:8080}"
MODE="${1:-error}"
case "$MODE" in
  error)
    for i in $(seq 1 20); do
      curl -s -o /dev/null -w "call $i -> HTTP %{http_code}\n" "$BASE/api/debug/error"
      sleep 0.5
    done
    echo "Now check:  grep unexpected_exception backend/logs/techmart.log | tail -3"
    ;;
  slow)
    for i in $(seq 1 10); do
      curl -s -o /dev/null -w "call $i -> HTTP %{http_code} in %{time_total}s\n" "$BASE/api/debug/slow?ms=1500"
    done
    ;;
  *) echo "Usage: $0 [error|slow]"; exit 1 ;;
esac
