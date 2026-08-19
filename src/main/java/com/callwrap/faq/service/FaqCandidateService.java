package com.callwrap.faq.service;

import com.callwrap.analysis.repository.AnalysisResultRepository;
import com.callwrap.faq.dto.FaqCandidateResponse;
import com.callwrap.faq.dto.FaqCandidatesResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FaqCandidateService {

    private final AnalysisResultRepository analysisResultRepository;

    @Transactional(readOnly = true)
    public FaqCandidatesResponse getCandidates() {
        var candidates = analysisResultRepository.findFaqCandidates().stream()
            .map(p -> new FaqCandidateResponse(p.getCallId(), p.getSubcategory(), p.getInquiry(), p.getResolution()))
            .toList();
        return new FaqCandidatesResponse(candidates);
    }
}
