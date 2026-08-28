package com.callwrap.counselor.domain;

import com.callwrap.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "counselors")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Counselor extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "login_id", nullable = false, unique = true, length = 100)
    private String loginId;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 100)
    private String team;

    // DB 컬럼은 V7 마이그레이션에서 기존 행에 DEFAULT 'COUNSELOR'로 채워졌다 — 기존 계정은 전부 상담사로 남는다.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CounselorRole role;

    @Builder
    private Counselor(String loginId, String passwordHash, String name, String team, CounselorRole role) {
        this.loginId = loginId;
        this.passwordHash = passwordHash;
        this.name = name;
        this.team = team;
        this.role = role;
    }
}
