ALTER TABLE klage.registrering
    ADD COLUMN additional_kabal_mulighet_id UUID;

ALTER TABLE klage.registrering
    RENAME COLUMN oppgave_id TO gosys_oppgave_id;

