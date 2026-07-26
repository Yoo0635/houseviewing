#!/usr/bin/env bash

set -Eeuo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DEPLOY_DIR="${1:-$(cd "${SCRIPT_DIR}/.." && pwd)}"
COMPOSE_FILE="${DEPLOY_DIR}/docker-compose.prod.yml"
ENV_FILE="${DEPLOY_DIR}/.env"
HEALTH_RETRIES="${HEALTH_RETRIES:-36}"
HEALTH_INTERVAL_SECONDS="${HEALTH_INTERVAL_SECONDS:-5}"

log() {
  printf '[deploy] %s\n' "$*"
}

fail() {
  log "$1"
  if [[ -f "${COMPOSE_FILE}" && -f "${ENV_FILE}" ]]; then
    docker compose --env-file "${ENV_FILE}" -f "${COMPOSE_FILE}" ps || true
    docker compose --env-file "${ENV_FILE}" -f "${COMPOSE_FILE}" logs --tail=100 || true
  fi
  exit 1
}

for command_name in docker curl; do
  command -v "${command_name}" >/dev/null 2>&1 \
    || fail "required command is missing: ${command_name}"
done

docker compose version >/dev/null 2>&1 \
  || fail "Docker Compose v2 is required"

[[ -d "${DEPLOY_DIR}" ]] || fail "deployment directory does not exist: ${DEPLOY_DIR}"
[[ -f "${COMPOSE_FILE}" ]] || fail "Compose file does not exist: ${COMPOSE_FILE}"
[[ -f "${ENV_FILE}" ]] || fail "environment file does not exist: ${ENV_FILE}"

cd "${DEPLOY_DIR}"

COMPOSE_ENV="$(
  docker compose \
    --env-file "${ENV_FILE}" \
    -f "${COMPOSE_FILE}" \
    config --environment
)"

required_variables=(
  GHCR_NAMESPACE
  GHCR_REPOSITORY
  IMAGE_TAG
  MYSQL_ROOT_PASSWORD
  MYSQL_USER
  MYSQL_PASSWORD
  KAKAO_REST_API_KEY
  AWS_ACCESS_KEY
  AWS_SECRET_KEY
  JWT_SECRET
  API_URL
  SECRET_KEY
  RTMS_SERVICE_KEY
)

for variable_name in "${required_variables[@]}"; do
  variable_value="$(
    awk -F= -v key="${variable_name}" '
      $1 == key {
        print substr($0, length(key) + 2)
        found = 1
        exit
      }
      END {
        if (!found) {
          exit 1
        }
      }
    ' <<<"${COMPOSE_ENV}"
  )" || fail "required variable is missing from ${ENV_FILE}: ${variable_name}"

  [[ -n "${variable_value}" ]] \
    || fail "required variable is empty in ${ENV_FILE}: ${variable_name}"
done

compose() {
  docker compose --env-file "${ENV_FILE}" -f "${COMPOSE_FILE}" "$@"
}

wait_for_backend() {
  curl --fail --silent --show-error \
    --max-time 5 \
    http://127.0.0.1:8080/actuator/health \
    >/dev/null
}

wait_for_python() {
  compose exec -T python-server python -c \
    "import urllib.request; urllib.request.urlopen('http://127.0.0.1:8000/health', timeout=3)" \
    >/dev/null
}

log "validating Docker Compose configuration"
compose config --quiet

log "pulling immutable application images"
compose pull

log "starting production services"
compose up -d --remove-orphans

for ((attempt = 1; attempt <= HEALTH_RETRIES; attempt++)); do
  if wait_for_backend && wait_for_python; then
    log "deployment is healthy"
    compose ps
    exit 0
  fi

  log "waiting for health checks (${attempt}/${HEALTH_RETRIES})"
  sleep "${HEALTH_INTERVAL_SECONDS}"
done

fail "deployment did not become healthy within the configured timeout"
