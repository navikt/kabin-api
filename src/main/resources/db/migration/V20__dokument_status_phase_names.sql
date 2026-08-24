UPDATE klage.registrering_dokument
SET status = 'UPLOADING_DONE'
WHERE status = 'UPLOADED';
