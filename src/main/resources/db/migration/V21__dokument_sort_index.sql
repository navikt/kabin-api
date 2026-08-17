-- The order the uploaded documents end up in on the journalpost, sent on to kabal-api and finally to
-- dokarkiv as `rekkefoelge`. The document with the lowest number is the hoveddokument. This replaces
-- `registrering.hoveddokument_id`.
--
-- The numbers are spread out with a large gap between them, so that moving a document only changes the
-- number of that one document: the client picks a number between its new neighbours. The range is the
-- safe integer range of a JavaScript number, and the gap is 1/10000 of it.
ALTER TABLE klage.registrering_dokument
    ADD COLUMN sort_index DOUBLE PRECISION NOT NULL DEFAULT 0;

UPDATE klage.registrering_dokument d
SET sort_index = numbered.new_sort_index
FROM (SELECT rd.id,
             (ROW_NUMBER() OVER (
                 PARTITION BY rd.registrering_id
                 -- The document that was hoveddokument keeps its place at the front.
                 ORDER BY (r.hoveddokument_id IS NOT DISTINCT FROM rd.id) DESC, rd.created
                 ) - 1) * (9007199254740991::double precision / 10000) AS new_sort_index
      FROM klage.registrering_dokument rd
               JOIN klage.registrering r ON r.id = rd.registrering_id) numbered
WHERE d.id = numbered.id;

-- `hoveddokument_id` is superseded by the numbering above and no longer mapped, but is kept (it is
-- already nullable, so inserts from the new version work) until the next release, so that pods running
-- the previous version keep working through the rolling deploy. Drop it in a later migration.
