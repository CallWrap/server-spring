package com.callwrap.faq.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "faq_entry_revisions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FaqEntryRevision {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "faq_entry_id", nullable = false)
    private UUID faqEntryId;

    @Column(nullable = false, columnDefinition = "text")
    private String question;

    @Column(nullable = false, columnDefinition = "text")
    private String answer;

    @Column(name = "revised_by_counselor_id", nullable = false)
    private UUID revisedByCounselorId;

    @Column(name = "revised_at", nullable = false)
    private LocalDateTime revisedAt;

    private FaqEntryRevision(
            UUID faqEntryId, String question, String answer, UUID revisedByCounselorId) {
        this.faqEntryId = faqEntryId;
        this.question = question;
        this.answer = answer;
        this.revisedByCounselorId = revisedByCounselorId;
        this.revisedAt = LocalDateTime.now();
    }

    public static FaqEntryRevision capture(FaqEntry entry, UUID counselorId) {
        return new FaqEntryRevision(
            entry.getId(), entry.getQuestion(), entry.getAnswer(), counselorId);
    }
}
