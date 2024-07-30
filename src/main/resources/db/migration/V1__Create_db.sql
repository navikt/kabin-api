DO
$$
    BEGIN
        IF EXISTS
                (SELECT 1 FROM pg_roles WHERE rolname = 'cloudsqliamuser')
        THEN
            GRANT USAGE ON SCHEMA public TO cloudsqliamuser;
            GRANT USAGE ON SCHEMA klage TO cloudsqliamuser;
            GRANT SELECT ON ALL TABLES IN SCHEMA public TO cloudsqliamuser;
            GRANT SELECT ON ALL TABLES IN SCHEMA klage TO cloudsqliamuser;
            ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT ON TABLES TO cloudsqliamuser;
            ALTER DEFAULT PRIVILEGES IN SCHEMA klage GRANT SELECT ON TABLES TO cloudsqliamuser;
        END IF;
    END
$$;

CREATE TABLE klage.registrering
(
    id                                   UUID PRIMARY KEY NOT NULL,
    saken_gjelder_type                   TEXT,
    saken_gjelder_value                  TEXT,
    klager_type                          TEXT,
    klager_value                         TEXT,
    fullmektig_type                      TEXT,
    fullmektig_value                     TEXT,
    avsender_type                        TEXT,
    avsender_value                       TEXT,
    journalpost_id                       TEXT,
    type_id                              TEXT,
    mulighet_id                          TEXT,
    mulighet_fagsystem_id                TEXT,
    mottatt_vedtaksinstans               DATE,
    mottatt_klageinstans                 DATE,
    behandlingstid_units                 INT,
    behandlingstid_unit_type_id          TEXT,
    hjemmel_id_list                      TEXT,
    ytelse_id                            TEXT,
    saksbehandler_ident                  TEXT,
    oppgave_id                           TEXT,
    send_svarbrev                        BOOLEAN,
    override_svarbrev_custom_text        BOOLEAN,
    svarbrev_title                       TEXT,
    svarbrev_custom_text                 TEXT,
    override_svarbrev_behandlingstid     BOOLEAN,
    svarbrev_behandlingstid_units        INT,
    svarbrev_behandlingstid_unit_type_id TEXT,
    svarbrev_fullmektig_fritekst         TEXT,
    created                              TIMESTAMP        NOT NULL,
    modified                             TIMESTAMP        NOT NULL,
    created_by                           TEXT             NOT NULL,
    finished                             TIMESTAMP
);

CREATE TABLE klage.svarbrev_receiver
(
    id              UUID PRIMARY KEY NOT NULL,
    part_type       TEXT,
    part_value      TEXT,
    handling        TEXT,
    adresselinje1   TEXT,
    adresselinje2   TEXT,
    adresselinje3   TEXT,
    postnummer      TEXT,
    landkode        TEXT,
    registrering_id UUID REFERENCES klage.registrering (id) ON DELETE CASCADE
);

CREATE INDEX registrering_svarbrev_receiver_idx ON klage.svarbrev_receiver (registrering_id);
