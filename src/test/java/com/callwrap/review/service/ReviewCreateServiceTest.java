package com.callwrap.review.service;

import com.callwrap.global.exception.CallWrapException;
import com.callwrap.global.exception.ErrorCode;
import com.callwrap.pipeline.service.JobDispatchService;
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
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewCreateServiceTest {

    @Mock
    private CallRepository callRepository;

    @Mock
    private JobDispatchService jobDispatchService;

    @Test
    void 중복된_sourceRequestId면_Job을_발행하지_않고_예외를_던진다() {
        var request = new ReviewCreateRequest(
                "SRC-DUP", UUID.randomUUID().toString(), "https://audio/1.wav", LocalDateTime.now());
        when(callRepository.existsBySourceRequestId("SRC-DUP")).thenReturn(true);

        var service = new ReviewCreateService(callRepository, jobDispatchService);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(CallWrapException.class)
                .extracting(e -> ((CallWrapException) e).getErrorCode())
                .isEqualTo(ErrorCode.DUPLICATE_SOURCE_REQUEST);

        verifyNoInteractions(jobDispatchService);
    }

    @Test
    void 정상_요청이면_콜을_저장하고_Job을_발행한다() {
        UUID counselorId = UUID.randomUUID();
        var request = new ReviewCreateRequest(
                "SRC-NEW", counselorId.toString(), "https://audio/2.wav", LocalDateTime.now());
        when(callRepository.existsBySourceRequestId("SRC-NEW")).thenReturn(false);
        when(callRepository.save(any(Call.class))).thenAnswer(invocation -> {
            Call saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", UUID.randomUUID());
            return saved;
        });

        var service = new ReviewCreateService(callRepository, jobDispatchService);
        ReviewCreateResponse response = service.create(request);

        assertThat(response.reviewId()).isNotNull();

        ArgumentCaptor<Call> callCaptor = ArgumentCaptor.forClass(Call.class);
        var inOrder = org.mockito.Mockito.inOrder(callRepository, jobDispatchService);
        inOrder.verify(callRepository).save(any(Call.class));
        inOrder.verify(jobDispatchService).dispatch(callCaptor.capture(), any(Map.class));
        assertThat(callCaptor.getValue().getId()).isEqualTo(response.reviewId());
    }
}