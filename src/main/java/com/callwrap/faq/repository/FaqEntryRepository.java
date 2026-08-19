package com.callwrap.faq.repository;

import com.callwrap.faq.domain.FaqEntry;
import com.callwrap.faq.domain.FaqStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FaqEntryRepository extends JpaRepository<FaqEntry, UUID> {

    List<FaqEntry> findByStatusOrderByGeneratedAtDesc(FaqStatus status);
}
