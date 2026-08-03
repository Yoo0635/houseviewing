import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Gauge } from 'k6/metrics';

const baseUrl = __ENV.BASE_URL || 'http://host.docker.internal:18080';
const requests = Number(__ENV.REQUESTS || 100);
const monitorDuration = __ENV.MONITOR_DURATION || '20s';

export const options = {
  scenarios: {
    register_houses: {
      executor: 'per-vu-iterations',
      vus: requests,
      iterations: 1,
      maxDuration: monitorDuration,
      exec: 'registerHouse',
    },
    monitor_hikari: {
      executor: 'constant-arrival-rate',
      rate: 5,
      timeUnit: '1s',
      duration: monitorDuration,
      preAllocatedVUs: 2,
      exec: 'monitorHikari',
    },
  },
};

const houseCreated = new Counter('house_created');
const houseFailed = new Counter('house_failed');
const hikariPending = new Gauge('hikari_pending_connections');

export function setup() {
  const suffix = `${Date.now()}-${Math.floor(Math.random() * 100000)}`;
  const loginId = `kakao-pool-${suffix}`;
  const password = 'password123';
  const registerRes = http.post(`${baseUrl}/users/register`, JSON.stringify({
    name: 'kakao-pool',
    email: `${loginId}@example.com`,
    loginId,
    password,
  }), {
    headers: { 'Content-Type': 'application/json' },
  });
  check(registerRes, { 'user registered': (res) => res.status === 201 });

  const loginRes = http.post(`${baseUrl}/auth/login`, JSON.stringify({
    loginId,
    password,
  }), {
    headers: {
      'Content-Type': 'application/json',
      'X-Device-Id': `k6-${suffix}`,
    },
  });
  check(loginRes, { 'user logged in': (res) => res.status === 200 });
  return { accessToken: loginRes.json('accessToken') };
}

export function registerHouse(data) {
  const id = `${__VU}-${Date.now()}`;
  const res = http.post(`${baseUrl}/houses/register`, JSON.stringify({
    nickname: `pool-${id}`,
    originAddress: `서울특별시 강남구 테헤란로 427 ${id}`,
  }), {
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${data.accessToken}`,
    },
    timeout: '10s',
  });
  if (res.status === 201) {
    houseCreated.add(1);
  } else {
    houseFailed.add(1);
  }
  check(res, { 'house register completed': (r) => r.status === 201 });
}

export function monitorHikari(data) {
  const res = http.get(`${baseUrl}/actuator/metrics/hikaricp.connections.pending`, {
    headers: { Authorization: `Bearer ${data.accessToken}` },
    timeout: '2s',
  });
  if (res.status === 200) {
    hikariPending.add(Number(res.json('measurements.0.value') || 0));
  }
  sleep(0.1);
}

export function handleSummary(summary) {
  const output = __ENV.K6_SUMMARY_PATH || 'scripts/performance/results/kakao-pool-delay-summary.json';
  return {
    [output]: JSON.stringify(summary, null, 2),
    stdout: textSummary(summary),
  };
}

function textSummary(summary) {
  const pending = summary.metrics.hikari_pending_connections;
  const created = summary.metrics.house_created;
  const failed = summary.metrics.house_failed;
  return JSON.stringify({
    house_created: created && created.values ? created.values.count : 0,
    house_failed: failed && failed.values ? failed.values.count : 0,
    hikari_pending_max: pending && pending.values ? pending.values.max : null,
  }, null, 2);
}
