package com.callwrap.review.domain;

import com.callwrap.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/*
 * 민원인(백엔드_요청사항_상담사화면.md 1번, A안). calls와 분리해두는 이유는 상담 이력 집계(2번)
 * 때문이다 — 같은 사람이 여러 번 전화하므로 phone으로 같은 사람을 찾아 이력을 잇는다.
 * name/org는 콜마다 다시 넘어올 수 있어(오탈자, 소속 변경 등) upsert 시 값이 있을 때만 덮어쓴다
 * (ComplainantRepository.upsertByPhone 참고). phone이 아예 없는 콜은 이력을 이을 키가 없어
 * 단발성 레코드로 남는다 — resolve 쪽에서 처리한다.
 */
@Entity
@Table(name = "complainants")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Complainant extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(length = 100)
    private String name;

    @Column(length = 20)
    private String phone;

    @Column(length = 200)
    private String org;

    @Builder
    private Complainant(String name, String phone, String org) {
        this.name = name;
        this.phone = phone;
        this.org = org;
    }
}
