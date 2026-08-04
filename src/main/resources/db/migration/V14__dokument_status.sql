ALTER TABLE klage.registrering_dokument
    ADD COLUMN status TEXT NOT NULL DEFAULT 'UPLOADING';

UPDATE klage.registrering_dokument
SET status = 'DONE'
WHERE confirmed = TRUE;

ALTER TABLE klage.registrering_dokument
    ALTER COLUMN status DROP DEFAULT;

-- The generation of the blob that was virus scanned, so a resumed conversion converts exactly the
-- bytes we scanned.
ALTER TABLE klage.registrering_dokument
    ADD COLUMN scanned_generation BIGINT;

-- `confirmed` is superseded by `status` and no longer mapped, but is kept (with a default, so inserts
-- from the new version work) until the next release, so that pods running the previous version keep
-- working through the rolling deploy. Drop it in a later migration.
ALTER TABLE klage.registrering_dokument
    ALTER COLUMN confirmed SET DEFAULT FALSE;
