ALTER TABLE analysis_results
    ADD COLUMN utterance_sentiments JSONB NOT NULL DEFAULT '[]'::jsonb;
