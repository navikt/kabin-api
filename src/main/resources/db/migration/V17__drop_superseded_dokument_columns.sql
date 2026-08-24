-- `confirmed` was superseded by `status` in V14, and `is_hoveddokument` by
-- `registrering.hoveddokument_id` in V16. Both were kept through one release so that pods running the
-- previous version kept working during the rolling deploy. That release is out, so drop them.
ALTER TABLE klage.registrering_dokument
    DROP COLUMN confirmed;

ALTER TABLE klage.registrering_dokument
    DROP COLUMN is_hoveddokument;
