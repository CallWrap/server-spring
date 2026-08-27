package com.callwrap.review.repository;

import com.callwrap.review.domain.FollowupNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface FollowupNoteRepository extends JpaRepository<FollowupNote, UUID> {

    /**
     * 상세 화면의 후속 처리 패널용 — 작성자 이름까지 한 번에 조인해서 준다.
     * 프론트가 상담사를 따로 조회하지 않아도 되게(백엔드_요청사항_상담사화면.md 4번).
     */
    @Query("SELECT n.id AS id, n.content AS content, co.name AS counselorName, n.createdAt AS createdAt "
         + "FROM FollowupNote n JOIN Counselor co ON co.id = n.counselorId "
         + "WHERE n.callId = :callId ORDER BY n.createdAt DESC")
    List<FollowupNoteProjection> findViewsByCallId(@Param("callId") UUID callId);

    interface FollowupNoteProjection {
        UUID getId();
        String getContent();
        String getCounselorName();
        LocalDateTime getCreatedAt();
    }
}
