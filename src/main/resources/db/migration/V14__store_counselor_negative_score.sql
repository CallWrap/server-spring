ALTER TABLE calls
    ADD COLUMN counselor_negative_score DOUBLE PRECISION NOT NULL DEFAULT 0,
    ADD CONSTRAINT chk_calls_counselor_negative_score
        CHECK (counselor_negative_score BETWEEN 0 AND 1);

-- V11 이후 쌓인 발화별 감정 결과도 동일한 규칙으로 콜 단위 값으로 채운다.
UPDATE calls c
SET counselor_negative_score = calculated.negative_score
FROM (
    SELECT ar.call_id,
           COALESCE(AVG(-(item ->> 'score')::double precision), 0) AS negative_score
    FROM analysis_results ar
    LEFT JOIN LATERAL JSONB_ARRAY_ELEMENTS(
        COALESCE(ar.utterance_sentiments, '[]'::jsonb)
    ) item ON item ->> 'speaker' = '상담사'
          AND (item ->> 'score')::double precision < 0
    GROUP BY ar.call_id
) calculated
WHERE c.id = calculated.call_id;
