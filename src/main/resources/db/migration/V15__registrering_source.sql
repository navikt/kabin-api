-- Whether the registrering is based on an existing journalpost or on document(s) uploaded in Kabin.
ALTER TABLE klage.registrering
    ADD COLUMN source TEXT NOT NULL DEFAULT 'JOURNALPOST';

UPDATE klage.registrering r
SET source = 'UPLOADED_DOCUMENTS'
WHERE EXISTS (SELECT 1
              FROM klage.registrering_dokument d
              WHERE d.registrering_id = r.id);
