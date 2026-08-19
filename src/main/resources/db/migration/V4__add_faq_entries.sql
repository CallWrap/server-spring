-- FAQ 자동생성 기능. needs_faq=true로 판정된 콜들을 소분류별로 모아 EXAONE으로
-- 대표 질문/답변을 합성한 결과를 저장한다. 검수(DRAFT → PUBLISHED) 전엔 상담사에게 노출 안 함.

ALTER TABLE analysis_results ADD COLUMN faq_processed_at TIMESTAMP;

-- FAQ 배치 후보 조회 시 "아직 안 쓰인 것"만 거르는 용도
CREATE INDEX idx_analysis_faq_unprocessed ON analysis_results (faq_processed_at)
    WHERE faq_processed_at IS NULL;

CREATE TABLE faq_entries (
    id                        UUID PRIMARY KEY,
    subcategory               VARCHAR(100) NOT NULL,
    question                  TEXT NOT NULL,
    answer                    TEXT NOT NULL,
    source_call_count         INT NOT NULL,
    sample_call_ids           JSONB,
    status                    VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    generated_at              TIMESTAMP NOT NULL,
    published_at              TIMESTAMP,
    reviewed_by_counselor_id  UUID REFERENCES counselors (id)
);

CREATE INDEX idx_faq_entries_status ON faq_entries (status);
CREATE INDEX idx_faq_entries_subcategory ON faq_entries (subcategory);
