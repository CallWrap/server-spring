package com.callwrap.counselor.config;

import com.callwrap.counselor.domain.Counselor;
import com.callwrap.counselor.domain.CounselorRole;
import com.callwrap.counselor.repository.CounselorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/*
 * 로컬/데모용 계정 자동 시딩. 회원가입 API가 없는 POC 규모라 여태 admin 계정을
 * "docker exec + psql로 직접 INSERT"로만 만들 수 있었는데, Docker Desktop이 불안정할 때마다
 * 막혀버려서 앱 기동 시 자동으로 만들도록 옮겼다. existsByLoginId로 이미 있는지 먼저 확인하므로
 * 몇 번을 재기동해도, DB 볼륨이 초기화돼도 안전(idempotent)하게 다시 채워진다.
 *
 * ⚠️ admin/admin, test/test는 데모용 하드코딩 값이다 — 실서비스로 넘어가면
 * 반드시 회원가입/초대 플로우로 교체하고 이 클래스는 지울 것.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DevAccountSeeder implements ApplicationRunner {

    private final CounselorRepository counselorRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        seed("admin", "admin", "관리자", CounselorRole.ADMIN);
        seed("test", "test", "테스트 상담사", CounselorRole.COUNSELOR);
    }

    private void seed(String loginId, String rawPassword, String name, CounselorRole role) {
        if (counselorRepository.existsByLoginId(loginId)) {
            return;
        }
        Counselor counselor = Counselor.builder()
                .loginId(loginId)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .name(name)
                .role(role)
                .build();
        counselorRepository.save(counselor);
        log.info("[seed] 계정 자동 생성: {} ({})", loginId, role);
    }
}
