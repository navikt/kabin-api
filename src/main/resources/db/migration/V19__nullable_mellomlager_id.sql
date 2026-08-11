-- UNSUPPORTED_TYPE documents are created without a file in the file store, so mellomlager_id
-- is not available for them.
ALTER TABLE klage.registrering_dokument
    ALTER COLUMN mellomlager_id DROP NOT NULL;
