ALTER TABLE klage.registrering_dokument
    ADD COLUMN content_type TEXT NOT NULL DEFAULT 'application/pdf';
