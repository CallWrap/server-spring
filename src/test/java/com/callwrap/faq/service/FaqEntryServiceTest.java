package com.callwrap.faq.service;

import com.callwrap.analysis.repository.AnalysisResultRepository;
import com.callwrap.faq.domain.FaqEntry;
import com.callwrap.faq.domain.FaqEntryRevision;
import com.callwrap.faq.domain.FaqStatus;
import com.callwrap.faq.dto.FaqEntryUpdateRequest;
import com.callwrap.faq.repository.FaqEntryRepository;
import com.callwrap.faq.repository.FaqEntryRevisionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FaqEntryServiceTest {

    @Mock
    private FaqEntryRepository faqEntryRepository;
    @Mock
    private AnalysisResultRepository analysisResultRepository;
    @Mock
    private FaqEntryRevisionRepository faqEntryRevisionRepository;

    private FaqEntryService faqEntryService;

    @BeforeEach
    void setUp() {
        faqEntryService = new FaqEntryService(
            faqEntryRepository, analysisResultRepository, faqEntryRevisionRepository);
    }

    @Test
    void 유사FAQ는_같은_소분류에서_유사도_높은순으로_반환한다() {
        FaqEntry draft = entry("로그인", "비밀번호를 잊었을 때 어떻게 재설정하나요?", "새 초안");
        FaqEntry close = entry("로그인", "비밀번호를 잊었을 때 재설정 방법은 무엇인가요?", "기존 답변");
        FaqEntry far = entry("로그인", "아이디를 새로 만들 수 있나요?", "다른 답변");
        when(faqEntryRepository.findById(draft.getId())).thenReturn(Optional.of(draft));
        when(faqEntryRepository.findBySubcategoryAndStatusIn(
            "로그인", List.of(FaqStatus.DRAFT, FaqStatus.PUBLISHED)))
            .thenReturn(List.of(far, draft, close));

        var result = faqEntryService.similar(draft.getId());

        assertThat(result).extracting("id").containsExactly(close.getId(), far.getId());
        assertThat(result.get(0).similarity()).isGreaterThan(result.get(1).similarity());
    }

    @Test
    void FAQ수정은_기존_질문답변을_개정이력으로_남긴다() {
        UUID counselorId = UUID.randomUUID();
        FaqEntry entry = entry("로그인", "기존 질문", "기존 답변");
        when(faqEntryRepository.findById(entry.getId())).thenReturn(Optional.of(entry));

        faqEntryService.update(
            entry.getId(), counselorId, new FaqEntryUpdateRequest("수정 질문", "수정 답변"));

        ArgumentCaptor<FaqEntryRevision> revision = ArgumentCaptor.forClass(FaqEntryRevision.class);
        verify(faqEntryRevisionRepository).save(revision.capture());
        assertThat(revision.getValue().getQuestion()).isEqualTo("기존 질문");
        assertThat(revision.getValue().getAnswer()).isEqualTo("기존 답변");
        assertThat(revision.getValue().getRevisedByCounselorId()).isEqualTo(counselorId);
        assertThat(entry.getQuestion()).isEqualTo("수정 질문");
    }

    @Test
    void 초안을_기존FAQ에_병합하면_기존값을_보관하고_초안을_MERGED로_종료한다() {
        UUID counselorId = UUID.randomUUID();
        FaqEntry draft = entry("로그인", "새 질문", "새 답변");
        FaqEntry target = entry("로그인", "기존 질문", "기존 답변");
        target.publish(counselorId);
        when(faqEntryRepository.findById(draft.getId())).thenReturn(Optional.of(draft));
        when(faqEntryRepository.findById(target.getId())).thenReturn(Optional.of(target));

        var result = faqEntryService.mergeInto(draft.getId(), target.getId(), counselorId);

        ArgumentCaptor<FaqEntryRevision> revision = ArgumentCaptor.forClass(FaqEntryRevision.class);
        verify(faqEntryRevisionRepository).save(revision.capture());
        assertThat(revision.getValue().getQuestion()).isEqualTo("기존 질문");
        assertThat(target.getQuestion()).isEqualTo("새 질문");
        assertThat(target.getAnswer()).isEqualTo("새 답변");
        assertThat(draft.getStatus()).isEqualTo(FaqStatus.MERGED);
        assertThat(draft.getMergedIntoId()).isEqualTo(target.getId());
        assertThat(result.id()).isEqualTo(target.getId());
    }

    private FaqEntry entry(String subcategory, String question, String answer) {
        FaqEntry entry = FaqEntry.draft(
            subcategory, question, answer, 1, List.of(UUID.randomUUID()));
        ReflectionTestUtils.setField(entry, "id", UUID.randomUUID());
        return entry;
    }
}
