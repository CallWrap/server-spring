package com.callwrap.review.controller;

import com.callwrap.counselor.domain.CounselorRole;
import com.callwrap.review.service.FollowupNoteService;
import com.callwrap.review.service.ReviewConfirmService;
import com.callwrap.review.service.ReviewCreateService;
import com.callwrap.review.service.ReviewQueryService;
import com.callwrap.review.service.ReviewStatsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ReviewControllerTest {

    @Mock
    private ReviewCreateService reviewCreateService;
    @Mock
    private ReviewQueryService reviewQueryService;
    @Mock
    private ReviewConfirmService reviewConfirmService;
    @Mock
    private ReviewStatsService reviewStatsService;
    @Mock
    private FollowupNoteService followupNoteService;
    @InjectMocks
    private ReviewController reviewController;

    @Test
    void 상담사는_전체_상담대장을_조회할_수_없다() {
        UUID principal = UUID.randomUUID();
        var authentication = new UsernamePasswordAuthenticationToken(
            principal, null, List.of(new SimpleGrantedAuthority(CounselorRole.COUNSELOR.name())));

        assertThatThrownBy(() -> reviewController.list(
            principal, authentication, null, null, null, true,
            "", "", null, "", 0, 20))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void 관리자의_전체_상담대장_요청은_전체조회_서비스로_전달된다() {
        UUID principal = UUID.randomUUID();
        UUID filterCounselorId = UUID.randomUUID();
        var authentication = new UsernamePasswordAuthenticationToken(
            principal, null, List.of(new SimpleGrantedAuthority(CounselorRole.ADMIN.name())));

        reviewController.list(
            principal, authentication, null, null, null, true,
            "악성콜", "사용법문의", filterCounselorId, "홍길동", 0, 50);

        verify(reviewQueryService).listAll(
            null, null, null, "악성콜", "사용법문의", filterCounselorId, "홍길동", 0, 50);
    }
}
