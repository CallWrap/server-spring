package com.callwrap.counselor.service;

import com.callwrap.counselor.domain.Counselor;
import com.callwrap.counselor.dto.CounselorMeResponse;
import com.callwrap.counselor.repository.CounselorRepository;
import com.callwrap.global.exception.CallWrapException;
import com.callwrap.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CounselorQueryService {

    private final CounselorRepository counselorRepository;

    @Transactional(readOnly = true)
    public CounselorMeResponse getMe(UUID counselorId) {
        Counselor counselor = counselorRepository.findById(counselorId)
                .orElseThrow(() -> new CallWrapException(ErrorCode.INVALID_TOKEN));
        return new CounselorMeResponse(counselor.getId(), counselor.getName(), counselor.getLoginId());
    }
}
