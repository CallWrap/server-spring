package com.callwrap.review.repository;

import com.callwrap.review.domain.Call;
import com.callwrap.review.domain.CallStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CallRepository extends JpaRepository<Call, UUID> {

    Optional<Call> findBySourceRequestId(String sourceRequestId);

    boolean existsBySourceRequestId(String sourceRequestId);

    // status 지정 시 idx_calls_status를 그대로 태운다. status가 null이면 전체 목록(정렬만)
    @Query("SELECT c FROM Call c WHERE (:status IS NULL OR c.status = :status) ORDER BY c.updatedAt DESC")
    Page<Call> searchReviews(@Param("status") CallStatus status, Pageable pageable);

    /**
     * 관리자 대시보드 개요용. 예전엔 목록 API로 최근 N건을 받아와 프론트에서 상태별로 세었는데,
     * 그러면 (1) size 제한 밖의 콜은 통계에서 아예 빠지고 (2) 세는 것뿐인데 JSONB 전체를 다 실어보낸다.
     * idx_calls_status가 이미 있어서 GROUP BY도 그 인덱스를 탄다.
     */
    @Query("SELECT c.status AS status, COUNT(c) AS cnt FROM Call c GROUP BY c.status")
    List<StatusCountProjection> countByStatus();

    interface StatusCountProjection {
        CallStatus getStatus();
        Long getCnt();
    }
}
