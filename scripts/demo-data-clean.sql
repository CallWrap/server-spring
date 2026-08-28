-- ─────────────────────────────────────────────────────────────
-- demo-data.sql이 넣은 것만 되돌린다. 시연 영상 찍기 전에 실행하면 화면이 빈 상태가 된다.
--
--   docker exec -i callwrap-spring-postgres psql -U callwrap -d callwrap < scripts/demo-data-clean.sql
--
-- 파이프라인으로 직접 접수한 실제 콜(source_request_id가 'DEMO-'로 시작하지 않는 것)과
-- admin/test 계정은 건드리지 않는다. DB를 통째로 날리고 싶으면 이 스크립트 대신
-- `docker compose down -v` 후 앱을 다시 띄우면 Flyway가 빈 스키마를 새로 만든다.
-- ─────────────────────────────────────────────────────────────

BEGIN;

-- FAQ는 질문 문구에 표식을 두지 않는다(시연 화면에 '[데모]' 같은 접두사가 보이면 안 된다).
-- 대신 Flyway V6이 심어둔 5건(id가 11111111-…로 고정)만 남기고 나머지를 지운다.
DELETE FROM faq_entry_revisions
 WHERE faq_entry_id IN (SELECT id FROM faq_entries WHERE id::text NOT LIKE '11111111-1111-4111-8111-%');
DELETE FROM faq_entries WHERE id::text NOT LIKE '11111111-1111-4111-8111-%';

-- transcripts / analysis_results / followup_notes는 calls에 ON DELETE CASCADE로 매달려 있다.
DELETE FROM calls WHERE source_request_id LIKE 'DEMO-%';

DELETE FROM complainants WHERE phone LIKE '010-77%';
DELETE FROM counselors   WHERE login_id LIKE 'demo-%';

COMMIT;

\echo ''
\echo '=== 남은 데이터 ==='
SELECT 'calls' AS t, COUNT(*) FROM calls
UNION ALL SELECT 'counselors', COUNT(*) FROM counselors
UNION ALL SELECT 'complainants', COUNT(*) FROM complainants
UNION ALL SELECT 'faq_entries', COUNT(*) FROM faq_entries;
