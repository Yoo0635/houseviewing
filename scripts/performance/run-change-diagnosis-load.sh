#!/usr/bin/env sh
set -eu

RESULT_DIR="${RESULT_DIR:-scripts/performance/results}"
SUMMARY_PATH="${SUMMARY_PATH:-$RESULT_DIR/change-diagnosis-idempotency-summary.json}"
BASE_URL="${BASE_URL:-http://host.docker.internal:18080}"
STUB_URL="${STUB_URL:-http://host.docker.internal:18081}"
REQUESTS="${REQUESTS:-100}"

mkdir -p "$RESULT_DIR"

if command -v k6 >/dev/null 2>&1; then
  K6_SUMMARY_PATH="$SUMMARY_PATH" BASE_URL="$BASE_URL" STUB_URL="$STUB_URL" REQUESTS="$REQUESTS" \
    k6 run scripts/performance/change-diagnosis-idempotency.js
else
  docker run --rm \
    -v "$PWD:/work" \
    -w /work \
    -e K6_SUMMARY_PATH="$SUMMARY_PATH" \
    -e BASE_URL="$BASE_URL" \
    -e STUB_URL="$STUB_URL" \
    -e REQUESTS="$REQUESTS" \
    grafana/k6 run scripts/performance/change-diagnosis-idempotency.js
fi
