INSERT INTO counselors (id, login_id, password_hash, name)
VALUES (gen_random_uuid(), 'test', '$2b$10$GaT.fo9ZGmJ63hyBXUS04uLIvCFdfTazZU/318kCnmWU0WZ02Qi7i', N'테스트 상담사')
ON CONFLICT (login_id) DO UPDATE SET password_hash = EXCLUDED.password_hash;
