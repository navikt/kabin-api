CREATE TABLE klage.mulighet_existing_behandling
(
    id                       UUID PRIMARY KEY NOT NULL,
    type_id                  TEXT             NOT NULL,
    registrering_mulighet_id UUID REFERENCES klage.registrering_mulighet (id) ON DELETE CASCADE,
    behandling_id            UUID             NOT NULL,
    created                  TIMESTAMP        NOT NULL,
    completed                TIMESTAMP
);

CREATE INDEX mulighet_existing_behandling_idx ON klage.mulighet_existing_behandling (registrering_mulighet_id);

INSERT INTO klage.mulighet_existing_behandling(id, type_id, registrering_mulighet_id, behandling_id, created, completed)
SELECT gen_random_uuid(),
       '2',
       am.registrering_mulighet_id,
       am.ankebehandling_id,
       am.created,
       am.completed
FROM klage.mulighet_existing_ankebehandling am;

