package com.callwrap.review.service;

import com.callwrap.global.exception.CallWrapException;
import com.callwrap.global.exception.ErrorCode;
import com.callwrap.pipeline.messaging.VoiceProcessingProducer;
import com.callwrap.review.domain.Call;
import com.callwrap.review.dto.ReviewCreateRequest;
import com.callwrap.review.dto.ReviewCreateResponse;
import com.callwrap.review.repository.CallRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * DB/Spring 컨텍스트 없이 서비스 로직만 검증한다 — 도메인 서비스를 작게 쪼개서
 * Mockito만으로 충분히 테스트 가능함
 */
@ExtendWith(MockitoExtension.class)
class ReviewCreateServiceTest {

    @Mock
    private CallRepository callRepository;

    @Mock
    private VoiceProcessingProducer voiceProcessingProducer;

    @Test
    void 중복된_sourceRequestId면_큐에_발행하지_않고_예외를_던진다() {
        var request = new ReviewCreateRequest(
                "SRC-DUP", UUID.randomUUID().toString(), "https://audio/1.wav", LocalDateTime.now());
        when(callRepository.existsBySourceRequestId("SRC-DUP")).thenReturn(true);

        var service = new ReviewCreateService(callRepository, voiceProcessingProducer);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(CallWrapException.class)
                .extracting(e -> ((CallWrapException) e).getErrorCode())
                .isEqualTo(ErrorCode.DUPLICATE_SOURCE_REQUEST);

        verifyNoInteractions(voiceProcessingProducer);
    }

    @Test
    void 정상_요청이면_콜을_저장하고_큐에_발행한다() {
        UUID counselorId = UUID.randomUUID();
        var request = new ReviewCreateRequest(
                "SRC-NEW", counselorId.toString(), "https://audio/2.wav", LocalDateTime.now());
        when(callRepository.existsBySourceRequestId("SRC-NEW")).thenReturn(false);
        // 실제 Hibernate 없이도 UUID 채번을 흉내내 저장 후 상태를 재현한다.
        when(callRepository.save(any(Call.class))).thenAnswer(invocation -> {
            Call saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", UUID.randomUUID());
            return saved;
        });

        var service = new ReviewCreateService(callRepository, voiceProcessingProducer);
        ReviewCreateResponse response = service.create(request);

        assertThat(response.status()).isEqualTo("UPLOADED");
        assertThat(response.reviewId()).isNotNull();

        ArgumentCaptor<UUID> callIdCaptor = ArgumentCaptor.forClass(UUID.class);
        var inOrder = org.mockito.Mockito.inOrder(callRepository, voiceProcessingProducer);
        inOrder.verify(callRepository).save(any(Call.class));
        inOrder.verify(voiceProcessingProducer).publish(callIdCaptor.capture());
        assertThat(callIdCaptor.getValue()).isEqualTo(response.reviewId());
    }
}
