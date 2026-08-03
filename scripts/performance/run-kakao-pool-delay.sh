#!/usr/bin/env sh
set -eu

RESULT_DIR="${RESULT_DIR:-scripts/performance/results}"
SUMMARY_PATH="${SUMMARY_PATH:-$RESULT_DIR/kakao-pool-delay-summary.json}"
BASE_URL="${BASE_URL:-http://host.docker.internal:18080}"
REQUESTS="${REQUESTS:-100}"
MONITOR_DURATION="${MONITOR_DURATION:-20s}"

mkdir -p "$RESULT_DIR"

if command -v k6 >/dev/null 2>&1; then
  K6_SUMMARY_PATH="$SUMMARY_PATH" BASE_URL="$BASE_URL" REQUESTS="$REQUESTS" MONITOR_DURATION="$MONITOR_DURATION" \
    k6 run scripts/performance/kakao-pool-delay.js
else
  docker run --rm \
    -v "$PWD:/work" \
    -w /work \
    -e K6_SUMMARY_PATH="$SUMMARY_PATH" \
    -e BASE_URL="$BASE_URL" \
    -e REQUESTS="$REQUESTS" \
    -e MONITOR_DURATION="$MONITOR_DURATION" \
    grafana/k6 run scripts/performance/kakao-pool-delay.js
fi
