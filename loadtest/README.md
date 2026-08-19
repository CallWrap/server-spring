# 부하 테스트 (Locust / k6)

Spring REST API를 통해 실제 파이프라인(Spring → RabbitMQ → FastAPI → RabbitMQ → Spring)에 부하를 준다.
방법론과 관찰 지점 상세 설계는 `CallWrap_부하테스트_설계.md`(프로젝트 문서) 참고.

## 준비
1. `docker compose up -d`로 로컬 전체 스택 기동
2. `pip install -r loadtest/requirements.txt`
3. 샘플 오디오 사전 업로드 (모든 가상 상담사가 같은 오디오로 콜 접수):
   `python -m loadtest.seed_audio ./sample.wav`
   출력된 `LOADTEST_SAMPLE_AUDIO_URI=...` 값을 환경변수로 export
4. `LOADTEST_LOGIN_ID`, `LOADTEST_PASSWORD`, `LOADTEST_COUNSELOR_ID` 환경변수 설정

## 실행 — Locust (실제 상담사 행동 재현)
`locust -f loadtest/locustfile.py --host http://localhost:8080`
→ http://localhost:8089 접속 → 가상 사용자 수/초당 증가량 입력 → Start

## 실행 — k6 (순수 처리량 한계선 측정)
`k6 run --summary-export=summary.json loadtest/k6/breakpoint_create_review.js`

## 관찰 포인트
- Locust 대시보드 / k6 콘솔 요약: 응답시간(p50/p95/p99), 처리량(RPS), 실패율
- Grafana: `callwrap_queue_depth{queue="callwrap.jobs"}` / `{queue="callwrap.spring.result-queue"}` — 부하가 늘 때 어느 큐부터 쌓이는지
- Grafana: `callwrap_job_processing_seconds` — Job 발행부터 결과 수신까지 E2E 지연 (p50/p95/p99 자동 발행)
- RabbitMQ Management UI(`localhost:15672`): 큐별 Ready/Unacked 메시지 수
- `docker stats`: 어떤 컨테이너(Spring/FastAPI worker/RabbitMQ/PostgreSQL)가 먼저 포화되는지
