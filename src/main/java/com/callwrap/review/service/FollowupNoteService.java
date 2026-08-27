package com.callwrap.review.service;

import com.callwrap.counselor.repository.CounselorRepository;
import com.callwrap.global.exception.CallWrapException;
import com.callwrap.global.exception.ErrorCode;
import com.callwrap.review.domain.FollowupNote;
import com.callwrap.review.dto.ReviewDetailResponse.FollowupNoteView;
import com.callwrap.review.repository.CallRepository;
import com.callwrap.review.repository.FollowupNoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** 후속 처리 이력 작성(백엔드_요청사항_상담사화면.md 4번). 작성만 하고 쌓아두는 로그라 수정/삭제는 없다. */
@Service
@RequiredArgsConstructor
public class FollowupNoteService {

    private final FollowupNoteRepository followupNoteRepository;
    private final CallRepository callRepository;
    private final CounselorRepository counselorRepository;

    @Transactional
    public FollowupNoteView create(UUID callId, UUID counselorId, String content) {
        if (!callRepository.existsById(callId)) {
            throw new CallWrapException(ErrorCode.REVIEW_NOT_FOUND);
        }

        FollowupNote note = followupNoteRepository.save(
            FollowupNote.builder().callId(callId).counselorId(counselorId).content(content).build());

        // 작성자 이름을 응답에 바로 실어줘서 프론트가 상담사를 따로 조회하지 않아도 되게 한다.
        String counselorName = counselorRepository.findById(counselorId)
            .map(com.callwrap.counselor.domain.Counselor::getName)
            .orElse(null);

        return new FollowupNoteView(note.getId(), note.getContent(), counselorName, note.getCreatedAt());
    }
}
