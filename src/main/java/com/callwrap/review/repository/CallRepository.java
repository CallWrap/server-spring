package com.callwrap.review.repository;

import com.callwrap.review.domain.Call;
import com.callwrap.review.domain.CallStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface CallRepository extends JpaRepository<Call, UUID> {

    Optional<Call> findBySourceRequestId(String sourceRequestId);

    boolean existsBySourceRequestId(String sourceRequestId);

    // status 지정 시 idx_calls_status를 그대로 태운다. status가 null이면 전체 목록(정렬만)
    @Query("SELECT c FROM Call c WHERE (:status IS NULL OR c.status = :status) ORDER BY c.updatedAt DESC")
    Page<Call> searchReviews(@Param("status") CallStatus status, Pageable pageable);
}
