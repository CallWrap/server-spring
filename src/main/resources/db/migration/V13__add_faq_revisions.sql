ALTER TABLE faq_entries
    ADD COLUMN merged_into_id UUID REFERENCES faq_entries (id),
    ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT now();

UPDATE faq_entries
SET updated_at = COALESCE(published_at, generated_at);

CREATE TABLE faq_entry_revisions (
    id                         UUID PRIMARY KEY,
    faq_entry_id               UUID NOT NULL REFERENCES faq_entries (id) ON DELETE CASCADE,
    question                   TEXT NOT NULL,
    answer                     TEXT NOT NULL,
    revised_by_counselor_id    UUID NOT NULL REFERENCES counselors (id),
    revised_at                 TIMESTAMP NOT NULL
);

CREATE INDEX idx_faq_revisions_entry_time
    ON faq_entry_revisions (faq_entry_id, revised_at DESC);
