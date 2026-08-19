import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend } from 'k6/metrics';

const BASE_URL = __ENV.LOADTEST_BASE_URL || 'http://localhost:8080';
const LOGIN_ID = __ENV.LOADTEST_LOGIN_ID || 'tester';
const PASSWORD = __ENV.LOADTEST_PASSWORD || 'test1234!';
const COUNSELOR_ID = __ENV.LOADTEST_COUNSELOR_ID;
const AUDIO_URI = __ENV.LOADTEST_SAMPLE_AUDIO_URI;

const createReviewDuration = new Trend('create_review_duration');

export const options = {
  scenarios: {
    breakpoint: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '30s', target: 1 },   // ① baseline
        { duration: '2m', target: 50 },   // ② ramp-up
        { duration: '3m', target: 50 },   // ③ sustained
        { duration: '1m', target: 150 },  // ④ spike
        { duration: '2m', target: 0 },    // cooldown
      ],
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<1000'],
  },
};

export function setup() {
  const res = http.post(`${BASE_URL}/api/auth/login`, JSON.stringify({
    loginId: LOGIN_ID,
    password: PASSWORD,
  }), { headers: { 'Content-Type': 'application/json' } });

  check(res, { '로그인 성공': (r) => r.status === 200 });
  return { token: res.json('accessToken') };
}

export default function (data) {
  const headers = {
    'Content-Type': 'application/json',
    Authorization: `Bearer ${data.token}`,
  };

  const payload = JSON.stringify({
    sourceRequestId: `k6-${__VU}-${__ITER}-${Date.now()}`,
    counselorId: COUNSELOR_ID,
    audioFileUrl: AUDIO_URI,
  });

  const res = http.post(`${BASE_URL}/api/reviews`, payload, {
    headers,
    tags: { name: 'create_review' },
  });
  createReviewDuration.add(res.timings.duration);
  check(res, { '접수 성공(200)': (r) => r.status === 200 });

  sleep(1);
}
