-- 로그인 권한 분리(상담사/중간관리자). 기존 계정은 전부 COUNSELOR로 남는다 — 관리자 계정은
-- 회원가입 API가 없어서(POC 규모) 이 마이그레이션 이후 직접 DB에 role='ADMIN'으로 시딩한다.
ALTER TABLE counselors
    ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'COUNSELOR';

ALTER TABLE counselors
    ADD CONSTRAINT chk_counselors_role CHECK (role IN ('COUNSELOR', 'ADMIN'));
