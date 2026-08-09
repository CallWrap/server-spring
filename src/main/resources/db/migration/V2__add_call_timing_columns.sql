-- 사보원/중앙시스템이 알려주는 통화 시작시각·통화시간을 실제로 저장한다.
ALTER TABLE calls
    ADD COLUMN call_started_at TIMESTAMP,
    ADD COLUMN call_duration_sec INT;
