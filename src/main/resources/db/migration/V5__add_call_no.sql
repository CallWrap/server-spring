-- 대기열에서 상담사가 콜을 부를 "사람이 읽는 번호". UUID(36자)는 화면 식별자로 못 쓴다.
-- source_request_id(사보원 요청번호)는 형식이 아직 정해지지 않아 표시용으로 쓸 수 없어서,
-- 접수 순서를 그대로 따르는 자체 시퀀스를 둔다.

CREATE SEQUENCE calls_call_no_seq;

ALTER TABLE calls ADD COLUMN call_no INT;

-- 기존 행은 접수 순서대로 채운다
WITH ordered AS (
    SELECT id, row_number() OVER (ORDER BY created_at, id) AS n FROM calls
)
UPDATE calls c SET call_no = o.n FROM ordered o WHERE c.id = o.id;

-- 다음 번호가 기존 최대값 뒤에서 이어지게 한다 (행이 없으면 1부터)
SELECT setval(
    'calls_call_no_seq',
    GREATEST((SELECT COALESCE(max(call_no), 0) FROM calls), 1),
    (SELECT COALESCE(max(call_no), 0) FROM calls) > 0
);

ALTER TABLE calls ALTER COLUMN call_no SET DEFAULT nextval('calls_call_no_seq');
ALTER TABLE calls ALTER COLUMN call_no SET NOT NULL;
ALTER SEQUENCE calls_call_no_seq OWNED BY calls.call_no;

CREATE UNIQUE INDEX uq_calls_call_no ON calls (call_no);
