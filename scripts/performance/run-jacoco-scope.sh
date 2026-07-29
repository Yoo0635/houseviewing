#!/usr/bin/env bash
set -euo pipefail

run_id="${RUN_ID:-$(date +%Y%m%d%H%M%S)}"
result_dir="${RESULT_DIR:-scripts/performance/results/${run_id}-coverage}"

mkdir -p "$result_dir"

(
  cd backend
  GRADLE_USER_HOME="${GRADLE_USER_HOME:-/tmp/gradle-home}" sh gradlew test jacocoTestReport
)

cp backend/build/reports/jacoco/test/jacocoTestReport.xml "$result_dir/jacocoTestReport.xml"
cp -R backend/build/reports/jacoco/test/html "$result_dir/html"
