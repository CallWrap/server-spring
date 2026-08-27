-- 민원인 정보(백엔드_요청사항_상담사화면.md 1번, A안). 별도 테이블로 분리해 상담 이력 집계(2번)까지
-- 같이 푼다 — 같은 사람이 여러 번 전화하므로 전화번호로 이력을 잇는다.
CREATE TABLE complainants (
    id           UUID PRIMARY KEY,
    name         VARCHAR(100),
    phone        VARCHAR(20),
    org          VARCHAR(200),
    created_at   TIMESTAMP NOT NULL DEFAULT now(),
    updated_at   TIMESTAMP NOT NULL DEFAULT now()
);
-- phone이 NULL인 행은 여러 개 있어도 된다(PostgreSQL UNIQUE 인덱스는 NULL끼리 안 겹친다고 본다) —
-- 전화번호 없이 들어온 민원인 정보를 위한 것.
CREATE UNIQUE INDEX uq_complainants_phone ON complainants (phone);

ALTER TABLE calls ADD COLUMN complainant_id UUID REFERENCES complainants (id);
CREATE INDEX idx_calls_complainant_id ON calls (complainant_id);
