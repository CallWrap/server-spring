-- 후속 처리 이력(백엔드_요청사항_상담사화면.md 4번). 작성만 하고 쌓아두는 로그라 수정/삭제는 없다.
CREATE TABLE followup_notes (
    id            UUID PRIMARY KEY,
    call_id       UUID NOT NULL REFERENCES calls (id) ON DELETE CASCADE,
    counselor_id  UUID NOT NULL REFERENCES counselors (id),
    content       TEXT NOT NULL,
    created_at    TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_followup_notes_call_id ON followup_notes (call_id, created_at DESC);
