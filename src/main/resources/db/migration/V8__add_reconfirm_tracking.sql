-- 재확정 허용(백엔드_요청사항_상담사화면.md 3번, A안). 이미 확정된 콜을 다시 확정할 수 있게
-- 됨에 따라, confirmed_at/confirmed_by_counselor_id는 "가장 최근" 값으로 덮어써진다 —
-- 최소한 "몇 번 확정됐는지"는 남기기 위해 카운터를 둔다.
ALTER TABLE analysis_results
    ADD COLUMN reconfirm_count INT NOT NULL DEFAULT 0;

-- 기존에 이미 확정된 행(confirmed_at이 있는 행)은 1로 채워 "최초 확정 1회"와 맞춘다.
UPDATE analysis_results SET reconfirm_count = 1 WHERE confirmed_at IS NOT NULL;
