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
    mulighet_id                          UUID,
    mottatt_vedtaksinstans               DATE,
    mottatt_klageinstans                 DATE,
    behandlingstid_units                 INT              NOT NULL,
    behandlingstid_unit_type_id          TEXT             NOT NULL,
    hjemmel_id_list                      TEXT,
    ytelse_id                            TEXT,
    saksbehandler_ident                  TEXT,
    oppgave_id                           BIGINT,
    send_svarbrev                        BOOLEAN,
    override_svarbrev_custom_text        BOOLEAN          NOT NULL DEFAULT FALSE,
    svarbrev_title                       TEXT             NOT NULL,
    svarbrev_custom_text                 TEXT,
    override_svarbrev_behandlingstid     BOOLEAN          NOT NULL DEFAULT FALSE,
    svarbrev_behandlingstid_units        INT,
    svarbrev_behandlingstid_unit_type_id TEXT,
    svarbrev_fullmektig_fritekst         TEXT,
    created                              TIMESTAMP        NOT NULL,
    modified                             TIMESTAMP        NOT NULL,
    created_by                           TEXT             NOT NULL,
    finished                             TIMESTAMP,
    behandling_id                        UUID,
    will_create_new_journalpost          BOOLEAN          NOT NULL DEFAULT FALSE,
    muligheter_fetched                   TIMESTAMP
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
    poststed        TEXT,
    landkode        TEXT,
    registrering_id UUID REFERENCES klage.registrering (id) ON DELETE CASCADE
);

CREATE TABLE klage.registrering_mulighet
(
    id                            UUID PRIMARY KEY NOT NULL,
    registrering_id               UUID REFERENCES klage.registrering (id) ON DELETE CASCADE,
    current_fagsystem_id          TEXT             NOT NULL,
    original_fagsystem_id         TEXT             NOT NULL,
    current_fagystem_technical_id TEXT             NOT NULL,
    tema_id                       TEXT             NOT NULL,
    vedtak_date                   DATE,
    fagsak_id                     TEXT             NOT NULL,
    ytelse_id                     TEXT,
    hjemmel_id_list               TEXT,
    klager_type                   TEXT,
    klager_value                  TEXT,
    klager_name                   TEXT,
    fullmektig_type               TEXT,
    fullmektig_value              TEXT,
    fullmektig_name               TEXT,
    previous_saksbehandler_ident  TEXT,
    previous_saksbehandler_name   TEXT,
    type_id                       TEXT             NOT NULL,
    klage_behandlende_enhet       TEXT,
    saken_gjelder_type            TEXT             NOT NULL,
    saken_gjelder_value           TEXT             NOT NULL,
    saken_gjelder_name            TEXT,

    saken_gjelder_available       BOOLEAN,
    saken_gjelder_language        TEXT,
    saken_gjelder_utsendingskanal TEXT,
    saken_gjelder_adresselinje1   TEXT,
    saken_gjelder_adresselinje2   TEXT,
    saken_gjelder_adresselinje3   TEXT,
    saken_gjelder_postnummer      TEXT,
    saken_gjelder_poststed        TEXT,
    saken_gjelder_landkode        TEXT,

    klager_available              BOOLEAN,
    klager_language               TEXT,
    klager_utsendingskanal        TEXT,
    klager_adresselinje1          TEXT,
    klager_adresselinje2          TEXT,
    klager_adresselinje3          TEXT,
    klager_postnummer             TEXT,
    klager_poststed               TEXT,
    klager_landkode               TEXT,

    fullmektig_available          BOOLEAN,
    fullmektig_language           TEXT,
    fullmektig_utsendingskanal    TEXT,
    fullmektig_adresselinje1      TEXT,
    fullmektig_adresselinje2      TEXT,
    fullmektig_adresselinje3      TEXT,
    fullmektig_postnummer         TEXT,
    fullmektig_poststed           TEXT,
    fullmektig_landkode           TEXT,
    created                       TIMESTAMP        NOT NULL
);

CREATE TABLE klage.mulighet_existing_ankebehandling
(
    id                       UUID PRIMARY KEY NOT NULL,
    registrering_mulighet_id UUID REFERENCES klage.registrering_mulighet (id) ON DELETE CASCADE,
    ankebehandling_id        UUID             NOT NULL,
    created                  TIMESTAMP        NOT NULL,
    completed                TIMESTAMP
);

CREATE TABLE klage.mulighet_saken_gjelder_part_status
(
    registrering_mulighet_id UUID REFERENCES klage.registrering_mulighet (id) ON DELETE CASCADE,
    status                   TEXT,
    date                     DATE,
    created                  TIMESTAMP,
    PRIMARY KEY (registrering_mulighet_id, status, date)
);

CREATE TABLE klage.mulighet_klager_part_status
(
    registrering_mulighet_id UUID REFERENCES klage.registrering_mulighet (id) ON DELETE CASCADE,
    status                   TEXT,
    date                     DATE,
    created                  TIMESTAMP,
    PRIMARY KEY (registrering_mulighet_id, status, date)
);

CREATE TABLE klage.mulighet_fullmektig_part_status
(
    registrering_mulighet_id UUID REFERENCES klage.registrering_mulighet (id) ON DELETE CASCADE,
    status                   TEXT,
    date                     DATE,
    created                  TIMESTAMP,
    PRIMARY KEY (registrering_mulighet_id, status, date)
);

CREATE INDEX registrering_svarbrev_receiver_idx ON klage.svarbrev_receiver (registrering_id);

CREATE INDEX registrering_mulighet_id_idx ON klage.registrering_mulighet (registrering_id);

CREATE INDEX mulighet_saken_gjelder_part_status_idx ON klage.mulighet_saken_gjelder_part_status (registrering_mulighet_id);

CREATE INDEX mulighet_klager_part_status_idx ON klage.mulighet_klager_part_status (registrering_mulighet_id);

CREATE INDEX mulighet_fullmektig_part_status_idx ON klage.mulighet_fullmektig_part_status (registrering_mulighet_id);

CREATE INDEX mulighet_existing_ankebehandling_idx ON klage.mulighet_existing_ankebehandling (registrering_mulighet_id);
