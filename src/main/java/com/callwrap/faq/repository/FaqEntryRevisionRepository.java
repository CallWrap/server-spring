package com.callwrap.faq.repository;

import com.callwrap.faq.domain.FaqEntryRevision;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FaqEntryRevisionRepository extends JpaRepository<FaqEntryRevision, UUID> {

    List<FaqEntryRevision> findByFaqEntryIdOrderByRevisedAtDesc(UUID faqEntryId);

    boolean existsByFaqEntryId(UUID faqEntryId);
}
