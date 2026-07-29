#!/usr/bin/env bash
set -euo pipefail

base_url="${BASE_URL:-http://localhost:8080}"
run_id="${RUN_ID:-$(date +%Y%m%d%H%M%S)}"
result_dir="${RESULT_DIR:-scripts/performance/results/${run_id}-signup}"

mkdir -p "$result_dir"

BASE_URL="$base_url" \
RUN_ID="$run_id" \
VUS="${VUS:-100}" \
ITERATIONS="${ITERATIONS:-100}" \
RESULT_PATH="$result_dir/k6-summary.json" \
k6 run scripts/performance/signup-concurrency.js

curl -s "$base_url/actuator/metrics/hikaricp.connections.max" > "$result_dir/hikari-max.json"
curl -s "$base_url/actuator/metrics/hikaricp.connections.pending" > "$result_dir/hikari-pending.json"
