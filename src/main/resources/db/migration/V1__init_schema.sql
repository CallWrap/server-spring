-- CallWrap 초기 스키마
-- 콜(calls)을 중심으로 transcripts/quality_check_results/analysis_results가 매달리는 구조.
-- 설계 원칙: UUID PK, 자식 테이블 ON DELETE CASCADE, 자주 바뀌는 값은 JSONB, 자주 거르는 값은 컬럼+인덱스.

CREATE TABLE counselors (
    id            UUID PRIMARY KEY,
    login_id      VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    name          VARCHAR(100) NOT NULL,
    created_at    TIMESTAMP NOT NULL DEFAULT now(),
    updated_at    TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uq_counselors_login_id UNIQUE (login_id)
);

CREATE TABLE calls (
    id                 UUID PRIMARY KEY,
    counselor_id       UUID NOT NULL REFERENCES counselors (id),
    audio_path         VARCHAR(500),
    status             VARCHAR(30) NOT NULL,
    retry_count        INT NOT NULL DEFAULT 0,
    source_request_id  VARCHAR(200) NOT NULL,
    created_at         TIMESTAMP NOT NULL DEFAULT now(),
    updated_at         TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uq_calls_source_request_id UNIQUE (source_request_id)
);

-- 검토 대기열("NEEDS_HUMAN_REVIEW인 콜") 조회, 목록 필터링에 사용
CREATE INDEX idx_calls_status ON calls (status);
CREATE INDEX idx_calls_counselor_id ON calls (counselor_id);

CREATE TABLE transcripts (
    id              UUID PRIMARY KEY,
    call_id         UUID NOT NULL REFERENCES calls (id) ON DELETE CASCADE,
    utterances      JSONB,
    retry_iteration INT NOT NULL DEFAULT 0,
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);

-- call_id에 UNIQUE를 걸지 않는다: 재시도마다 새 전사를 쌓아 이력을 보존한다 (quality_check_results와 동일 원칙).
CREATE INDEX idx_transcripts_call_id ON transcripts (call_id, retry_iteration);

CREATE TABLE quality_check_results (
    id                UUID PRIMARY KEY,
    call_id           UUID NOT NULL REFERENCES calls (id) ON DELETE CASCADE,
    check_name        VARCHAR(100),
    passed            BOOLEAN,
    score             DOUBLE PRECISION,
    threshold         DOUBLE PRECISION,
    higher_is_better  BOOLEAN,
    action            VARCHAR(30),
    reason            TEXT,
    details           JSONB,
    checked_at        TIMESTAMP,
    retry_iteration   INT NOT NULL DEFAULT 0
);

-- 특정 콜의 특정 재시도 회차 결과 조회용
CREATE INDEX idx_check_call ON quality_check_results (call_id, retry_iteration);

CREATE TABLE analysis_results (
    id                          UUID PRIMARY KEY,
    call_id                     UUID NOT NULL REFERENCES calls (id) ON DELETE CASCADE,
    confirmed_by_counselor_id   UUID REFERENCES counselors (id),
    summary                     JSONB,
    consultation_type           JSONB,
    consultation_category       JSONB,
    consultation_subcategory    JSONB,
    grade                       JSONB,
    needs_followup              JSONB,
    needs_faq                   JSONB,
    failures                    JSONB,
    confirmed_at                TIMESTAMP,
    edited_fields               JSONB,
    created_at                  TIMESTAMP NOT NULL DEFAULT now(),
    -- 콜당 1건 원칙을 DB가 강제한다.
    CONSTRAINT uq_analysis_results_call_id UNIQUE (call_id)
);

-- 대시보드가 등급·유형으로 집계(악성콜 카운트, 유형 분포)할 때 사용
CREATE INDEX idx_analysis_type ON analysis_results ((consultation_type ->> 'value'));
CREATE INDEX idx_analysis_grade ON analysis_results ((grade ->> 'value'));
CREATE INDEX idx_analysis_confirmed_by ON analysis_results (confirmed_by_counselor_id);
