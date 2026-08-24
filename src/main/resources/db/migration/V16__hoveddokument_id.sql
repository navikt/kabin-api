-- The hoveddokument is now pointed to from the registrering instead of being flagged on each
-- document, so several documents can never be hoveddokument at the same time.
ALTER TABLE klage.registrering
    ADD COLUMN hoveddokument_id UUID;

UPDATE klage.registrering r
SET hoveddokument_id = (SELECT d.id
                        FROM klage.registrering_dokument d
                        WHERE d.registrering_id = r.id
                          AND d.is_hoveddokument = TRUE
                        ORDER BY d.created
                        LIMIT 1);

-- `is_hoveddokument` is superseded by `registrering.hoveddokument_id` and no longer mapped, but is
-- kept (with a default, so inserts from the new version work) until the next release, so that pods
-- running the previous version keep working through the rolling deploy. Drop it in a later migration.
ALTER TABLE klage.registrering_dokument
    ALTER COLUMN is_hoveddokument SET DEFAULT FALSE;
