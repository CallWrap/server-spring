# spring/

한국사회보장정보원 산학협력 POC "CallWrap"의 데이터/오케스트레이션 서비스. 파이프라인의 ①②③④⑥
(전체 조율) + 인증을 담당한다.

**담당:** 김규리

상담 검토 생성·조회·확정, 상태 관리, RabbitMQ를 통한 FastAPI(AI 모델 호출 레이어) 오케스트레이션,
인증을 담당한다. AI 모델 호출 자체(STT/화자분리/분류/요약)는 이 서비스가 하지 않는다 —
전부 `api/`(FastAPI)에 위임한다. `api/`는 이 전환에 맞춰 Celery/Redis를 걷어내고 순수
AI 모델 호출 계층으로 슬림화될 예정 (담당: 이정민).

## 원칙

- **도메인 분리**: `counselor`(인증) / `review`(콜·검토) / `transcript`(전사) / `qualitycheck`(품질게이트
  결과) / `analysis`(AI 분석 결과)를 ERD 테이블과 1:1로 대응하는 패키지로 나눴다. 여러 도메인을
  가로지르는 오케스트레이션(파이프라인 진행)만 `pipeline` 패키지에 별도로 둔다.
- **단일 책임**: `review` 도메인 안에서도 생성(`ReviewCreateService`)·조회(`ReviewQueryService`)·
  확정(`ReviewConfirmService`)을 별도 클래스로 쪼갰다. 하나의 "CallService"로 뭉치지 않는다.
- **클린코드**: 컨트롤러는 검증·위임만 한다. 엔티티는 불변식(예: `AnalysisResult.confirm()`으로만
  확정 정보를 채움)을 스스로 지킨다. `PipelineOrchestrationService`는 "언제 무엇을 부를지"만 알고,
  실제 HTTP 호출은 `AiServiceClient`, 저장은 각 리포지토리로 위임한다.
- **대용량 데이터 처리 효율**:
  - JPA `open-in-view: false`, batch insert/update 설정(`hibernate.jdbc.batch_size` 등).
  - `calls.status`, `analysis_results`의 `consultation_type`/`grade` JSONB 값에 표현식 인덱스를
    걸어(`V1__init_schema.sql`) 대시보드 집계·검토 대기열 조회가 풀스캔을 타지 않게 했다.
  - `Call.counselorId`는 연관관계(`@ManyToOne`) 대신 원시 UUID로 들고 있다 — 목록/상세 조회처럼
    가장 빈번한 경로에서 불필요한 조인·N+1 위험을 원천 차단하고, 상담사 정보가 실제로 필요할 때만
    명시적 쿼리로 가져온다.
  - 목록 조회(`ReviewQueryService.list`)는 전사·분석 JSONB 전체를 절대 가져오지 않는다 — 상태값
    등 요약 정보만 담은 `ReviewSummaryResponse`로 페이지네이션한다.
  - 조회 서비스는 전부 `@Transactional(readOnly = true)`로 불필요한 dirty-checking 비용을 없앤다.

## 패키지 구조

```
com.callwrap
├── global          # 설정, 보안(JWT), 예외 처리, 공통 타입(BaseTimeEntity, Judgment<T>)
├── counselor       # 상담사 도메인 — 인증/내 정보
├── review          # 콜(calls) 도메인 — 검토 생성/조회/확정
├── transcript      # 전사 결과 도메인
├── qualitycheck    # 품질 게이트 결과 도메인
├── analysis        # AI 분석 결과 도메인
└── pipeline        # RabbitMQ 오케스트레이션 + FastAPI 호출 클라이언트 (여러 도메인을 조율하는 애플리케이션 계층)
```

## 기존 구현과의 관계

체크①②③④와 `orchestrator.py`의 액션 종합 로직은 FastAPI의 `POST /quality-gate` 안으로 그대로
이식되는 것을 전제로 한다. 이 프로젝트(Spring)는 그 결과(`action: pass/retry/human_review`)를
받아서 다음 단계로 넘길지, 재처리할지, 사람에게 넘길지만 판단한다 — 판정 로직 자체를 다시 짜지 않는다.

## 로컬 실행

```bash
docker compose up -d          # Postgres 16 + RabbitMQ
./gradlew bootRun --args='--spring.profiles.active=local'
```

필요 환경변수(운영 전제, `.env` 또는 배포 환경에서 주입):

| 변수 | 설명 |
|---|---|
| `JWT_SECRET` | JWT 서명 키 (32바이트 이상 랜덤 문자열) |
| `AI_SERVICE_BASE_URL` | FastAPI 서비스 주소 (기본 `http://localhost:8000`) |

## 미해결 / TODO

- `needs_faq` 판단을 어느 FastAPI 엔드포인트가 맡을지 미정 — 정해지면 `PipelineOrchestrationService`에
  연결.
- 로그아웃 시 refreshToken 서버측 폐기(Redis 블랙리스트)는 아직 없음 — 지금은 쿠키 무효화만.
- `ReviewCreateRequest.callStartedAt`을 저장할 컬럼이 아직 없음 — 필요해지면 `calls`에 마이그레이션
  추가.
- 사보원 -> Spring 검토 생성 엔드포인트의 시스템 간 인증(API Key)은 아직 미구현 (`SecurityConfig`에서
  전체 인증 요구 중).
