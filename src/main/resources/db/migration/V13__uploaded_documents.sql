ALTER TABLE klage.registrering
    ADD COLUMN inngaaende_kanal TEXT;

CREATE TABLE klage.registrering_dokument
(
    id               UUID PRIMARY KEY NOT NULL,
    registrering_id  UUID REFERENCES klage.registrering (id) ON DELETE CASCADE,
    mellomlager_id   TEXT             NOT NULL,
    name             TEXT             NOT NULL,
    size             BIGINT           NOT NULL,
    is_hoveddokument BOOLEAN          NOT NULL,
    confirmed        BOOLEAN          NOT NULL,
    created          TIMESTAMP        NOT NULL
);
