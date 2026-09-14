#!/usr/bin/env sh
set -eu

LABEL="${LABEL:?LABEL is required}"
EXPECTED_ITEMS="${EXPECTED_ITEMS:?EXPECTED_ITEMS is required}"
BASE_URL="${BASE_URL:-http://127.0.0.1:18080}"
RESULT_DIR="${RESULT_DIR:-scripts/performance/results}"
HISTORY_COUNT="${HISTORY_COUNT:-1000}"
VUS="${VUS:-10}"
ITERATIONS="${ITERATIONS:-100}"

test "$HISTORY_COUNT" -eq 1000
test "$VUS" -eq 10
test "$ITERATIONS" -eq 100
test "$EXPECTED_ITEMS" -eq 1000 || test "$EXPECTED_ITEMS" -eq 10

mkdir -p "$RESULT_DIR"

ACCESS_TOKEN="$(curl -fsS "$BASE_URL/auth/login" \
  -H 'Content-Type: application/json' \
  -H 'X-Device-Id: analysis-history-performance' \
  -d '{"loginId":"analysis-perf","password":"performance-password"}' \
  | python3 -c 'import json,sys; print(json.load(sys.stdin)["accessToken"])')"

i=0
while [ "$i" -lt 10 ]; do
  curl -fsS -o /dev/null "$BASE_URL/analyses?offset=0" \
    -H "Authorization: Bearer $ACCESS_TOKEN"
  i=$((i + 1))
done

BASE_URL="$BASE_URL" \
ACCESS_TOKEN="$ACCESS_TOKEN" \
EXPECTED_ITEMS="$EXPECTED_ITEMS" \
K6_SUMMARY_PATH="$RESULT_DIR/${LABEL}.json" \
  k6 run --quiet scripts/performance/analysis-history-offset.js
