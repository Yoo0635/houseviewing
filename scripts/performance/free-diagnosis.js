import http from 'k6/http';
import { check, sleep } from 'k6';
import exec from 'k6/execution';
import { Counter, Gauge, Trend } from 'k6/metrics';

const baseUrl = __ENV.BASE_URL || 'http://localhost:18080';
const stubUrl = __ENV.STUB_URL || 'http://localhost:18081';
const requests = Number(__ENV.REQUESTS || 50);
const mode = __ENV.MODE || 'different';
const oldApi = __ENV.OLD_API === 'true';

export const options = {
  scenarios: mode === 'connection' ? {
    diagnose: { executor: 'per-vu-iterations', vus: requests, iterations: 1, maxDuration: '60s', exec: 'diagnoseConnection' },
    monitor: { executor: 'constant-arrival-rate', rate: 10, timeUnit: '1s', duration: '12s', preAllocatedVUs: 2, exec: 'monitorPool' },
  } : {
    diagnose: { executor: 'per-vu-iterations', vus: requests, iterations: 1, maxDuration: '60s', exec: 'diagnoseConcurrent' },
  },
};

const completed = new Counter('diagnosis_completed');
const processing = new Counter('diagnosis_processing');
const conflict = new Counter('diagnosis_conflict');
const failed = new Counter('diagnosis_failed');
const active = new Trend('hikari_active');
const pending = new Trend('hikari_pending');
const usageAvgMs = new Gauge('hikari_usage_avg_ms');
const usageMaxMs = new Gauge('hikari_usage_max_ms');

export function setup() {
  http.post(`${stubUrl}/reset`);
  const users = [];
  const count = mode === 'connection' ? requests : 1;
  for (let i = 0; i < count; i += 1) users.push(createUser(i));
  const requestId = crypto.randomUUID();
  if (mode === 'completed') {
    const first = diagnose(users[0], requestId);
    check(first, { 'initial diagnosis completed': (r) => r.status === 201 });
  }
  return { users, requestId, usage: usage(users[0]) };
}

export function diagnoseConcurrent(data) {
  const requestId = mode === 'different' ? crypto.randomUUID() : data.requestId;
  record(diagnose(data.users[0], requestId));
}

export function diagnoseConnection(data) {
  record(diagnose(data.users[exec.scenario.iterationInTest], crypto.randomUUID()));
}

export function monitorPool(data) {
  sampleMetric('active', active, data.users[0]);
  sampleMetric('pending', pending, data.users[0]);
  sleep(0.05);
}

export function teardown(data) {
  const after = usage(data.users[0]);
  const count = after.count - data.usage.count;
  usageAvgMs.add(count > 0 ? ((after.total - data.usage.total) / count) * 1000 : 0);
  usageMaxMs.add(after.max * 1000);
}

function createUser(i) {
  const suffix = `${Date.now()}-${i}-${Math.floor(Math.random() * 1000000)}`;
  const loginId = `free-k6-${suffix}`;
  const password = 'password123';
  const register = http.post(`${baseUrl}/users/register`, JSON.stringify({
    name: 'free-k6', email: `${loginId}@example.com`, loginId, password,
  }), { headers: { 'Content-Type': 'application/json' } });
  check(register, { 'user registered': (r) => r.status === 201 });
  const login = http.post(`${baseUrl}/auth/login`, JSON.stringify({ loginId, password }), {
    headers: { 'Content-Type': 'application/json', 'X-Device-Id': `k6-${suffix}` },
  });
  check(login, { 'user logged in': (r) => r.status === 200 });
  return login.json('accessToken');
}

function diagnose(token, requestId) {
  return http.post(`${baseUrl}/analysis/pre-contract-diganoses`, {
    file: http.file('registry snapshot', 'registry.txt', 'text/plain'),
    data: http.file(JSON.stringify({ nickname: '동시성 검증', address: '서울특별시 강남구 테헤란로 427' }), 'data.json', 'application/json'),
  }, {
    headers: { Authorization: `Bearer ${token}`, 'Idempotency-Key': requestId },
    timeout: '45s',
  });
}

function record(res) {
  if (res.status === 409) return conflict.add(1);
  if (res.status !== 201) return failed.add(1);
  if (!oldApi && res.json('status') === 'PROCESSING') return processing.add(1);
  completed.add(1);
}

function sampleMetric(name, gauge, token) {
  const res = http.get(`${baseUrl}/actuator/metrics/hikaricp.connections.${name}`, {
    headers: { Authorization: `Bearer ${token}` }, timeout: '2s',
  });
  if (res.status === 200) gauge.add(Number(res.json('measurements.0.value') || 0));
}

function usage(token) {
  const res = http.get(`${baseUrl}/actuator/metrics/hikaricp.connections.usage`, {
    headers: { Authorization: `Bearer ${token}` }, timeout: '5s',
  });
  const measurements = res.status === 200 ? res.json('measurements') : [];
  const number = (name) => Number(measurements.find((item) => item.statistic === name)?.value || 0);
  return { count: number('COUNT'), total: number('TOTAL_TIME'), max: number('MAX') };
}

export function handleSummary(summary) {
  const output = __ENV.K6_SUMMARY_PATH || `scripts/performance/results/free-${mode}.json`;
  return { [output]: JSON.stringify(summary, null, 2), stdout: JSON.stringify({
    completed: count(summary, 'diagnosis_completed'), processing: count(summary, 'diagnosis_processing'),
    conflict: count(summary, 'diagnosis_conflict'), failed: count(summary, 'diagnosis_failed'),
    hikari_active_avg: value(summary, 'hikari_active', 'avg'), hikari_active_p95: value(summary, 'hikari_active', 'p(95)'),
    hikari_active_max: value(summary, 'hikari_active', 'max'), hikari_pending_avg: value(summary, 'hikari_pending', 'avg'),
    hikari_pending_p95: value(summary, 'hikari_pending', 'p(95)'), hikari_pending_max: value(summary, 'hikari_pending', 'max'),
    hikari_usage_avg_ms: value(summary, 'hikari_usage_avg_ms', 'value'),
    hikari_usage_max_ms: value(summary, 'hikari_usage_max_ms', 'value'),
  }, null, 2) };
}

function count(summary, name) { return summary.metrics[name]?.values?.count || 0; }
function value(summary, name, field) { return summary.metrics[name]?.values?.[field] ?? null; }
