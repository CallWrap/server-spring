package com.callwrap.counselor.repository;

import com.callwrap.counselor.domain.Counselor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CounselorRepository extends JpaRepository<Counselor, UUID> {

    Optional<Counselor> findByLoginId(String loginId);

    boolean existsByLoginId(String loginId);
}
