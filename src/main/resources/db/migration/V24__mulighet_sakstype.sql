ALTER TABLE klage.registrering_mulighet
    ALTER COLUMN fagsak_id DROP NOT NULL;

ALTER TABLE klage.registrering_mulighet
    ADD COLUMN IF NOT EXISTS sakstype_id TEXT NOT NULL DEFAULT '1';

ALTER TABLE klage.registrering_mulighet
    ADD COLUMN IF NOT EXISTS arkivsaksnummer TEXT;
