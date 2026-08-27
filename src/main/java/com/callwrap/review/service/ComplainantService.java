package com.callwrap.review.service;

import com.callwrap.review.domain.Complainant;
import com.callwrap.review.repository.ComplainantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.UUID;

/**
 * 콜 접수 시 넘어온 민원인 정보(이름/전화/소속, 전부 nullable)로 complainants 행을 찾거나 만든다
 * (백엔드_요청사항_상담사화면.md 1번, A안).
 */
@Service
@RequiredArgsConstructor
public class ComplainantService {

    private final ComplainantRepository complainantRepository;

    @Transactional
    public UUID resolve(String name, String phone, String org) {
        String normalizedName = blankToNull(name);
        String normalizedPhone = blankToNull(phone);
        String normalizedOrg = blankToNull(org);

        if (normalizedName == null && normalizedPhone == null && normalizedOrg == null) {
            return null;
        }

        if (normalizedPhone != null) {
            // 전화번호가 키다 — 같은 번호면 같은 사람으로 합쳐서 이력(2번)이 이어지게 한다.
            complainantRepository.upsertByPhone(UUID.randomUUID(), normalizedName, normalizedPhone, normalizedOrg);
            return complainantRepository.findByPhone(normalizedPhone)
                .map(Complainant::getId)
                .orElseThrow(() -> new IllegalStateException("upsert 직후 조회 실패: phone=" + normalizedPhone));
        }

        // 전화번호가 없으면 동일인 판별 키가 없다 — 이 콜만의 단발성 레코드로 남긴다(이력 집계에는 안 묶임).
        Complainant created = complainantRepository.save(
            Complainant.builder().name(normalizedName).org(normalizedOrg).build());
        return created.getId();
    }

    private String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
