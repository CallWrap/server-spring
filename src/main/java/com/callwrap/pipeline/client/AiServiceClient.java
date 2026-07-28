package com.callwrap.pipeline.client;

import com.callwrap.global.common.Judgment;
import com.callwrap.global.exception.AiServiceException;
import com.callwrap.pipeline.client.dto.CategoryClassificationResponse;
import com.callwrap.pipeline.client.dto.DiarizeResponse;
import com.callwrap.pipeline.client.dto.FollowupResult;
import com.callwrap.pipeline.client.dto.QualityGateResponse;
import com.callwrap.pipeline.client.dto.SttSegment;
import com.callwrap.pipeline.client.dto.TranscribeResponse;
import com.callwrap.transcript.domain.Utterance;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.Map;

/*
 * FastAPI(AI 모델 호출 레이어)와의 HTTP 통신만 책임진다
 * 오케스트레이션(호출 순서·재시도 판단)은 PipelineOrchestrationService가, 판단 결과 저장은 각 도메인 리포지토리가 맡도록 설계했다
 * WebClient를 .block()으로 사용하도록,, — 앱 전체를 리액티브로 바꾸지 않고, 외부 HTTP 호출 지점에서만 WebClient를 쓰는 절충안
 * (Spring MVC + WebClient 패턴 사용)
 */
@Component
@RequiredArgsConstructor
public class AiServiceClient {

    private final WebClient aiServiceWebClient;

    public TranscribeResponse transcribe(String audioFileUrl, String reviewId) {
        return post("/stt", Map.of("audioFileUrl", audioFileUrl, "reviewId", reviewId), TranscribeResponse.class);
    }

    public DiarizeResponse diarize(String audioFileUrl, List<SttSegment> segments) {
        return post("/diarize", Map.of("audioFileUrl", audioFileUrl, "segments", segments), DiarizeResponse.class);
    }

    public QualityGateResponse runQualityGate(List<Utterance> utterances) {
        return post("/quality-gate", Map.of("utterances", utterances), QualityGateResponse.class);
    }

    public Judgment<Object> classifyType(List<Utterance> utterances) {
        return post("/classify/type", Map.of("utterances", utterances), new ParameterizedTypeReference<>() {
        });
    }

    public CategoryClassificationResponse classifyCategory(List<Utterance> utterances) {
        return post("/classify/category", Map.of("utterances", utterances), CategoryClassificationResponse.class);
    }

    public Judgment<Object> classifyGrade(List<Utterance> utterances) {
        return post("/classify/grade", Map.of("utterances", utterances), new ParameterizedTypeReference<>() {
        });
    }

    public Judgment<Object> summarize(List<Utterance> utterances) {
        return post("/summarize", Map.of("utterances", utterances), new ParameterizedTypeReference<>() {
        });
    }

    public FollowupResult judgeFollowup(List<Utterance> utterances, Map<String, Object> classification) {
        return post("/followup", Map.of("utterances", utterances, "classification", classification),
                FollowupResult.class);
    }

    private <T> T post(String uri, Object body, Class<T> responseType) {
        try {
            return aiServiceWebClient.post().uri(uri).bodyValue(body)
                    .retrieve().bodyToMono(responseType).block();
        } catch (WebClientResponseException e) {
            throw new AiServiceException(uri + " 호출 실패: " + e.getStatusCode(), e);
        }
    }

    private <T> T post(String uri, Object body, ParameterizedTypeReference<T> responseType) {
        try {
            return aiServiceWebClient.post().uri(uri).bodyValue(body)
                    .retrieve().bodyToMono(responseType).block();
        } catch (WebClientResponseException e) {
            throw new AiServiceException(uri + " 호출 실패: " + e.getStatusCode(), e);
        }
    }
}
