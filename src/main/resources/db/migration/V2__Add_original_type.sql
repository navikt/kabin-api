ALTER TABLE klage.registrering_mulighet
    ADD COLUMN original_type_id TEXT;

UPDATE klage.registrering_mulighet
SET original_type_id = type_id;

ALTER TABLE klage.registrering_mulighet
    ALTER COLUMN original_type_id SET NOT NULL;