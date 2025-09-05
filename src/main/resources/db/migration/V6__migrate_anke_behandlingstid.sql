UPDATE klage.registrering
SET behandlingstid_unit_type_id = '1', behandlingstid_units = 0
WHERE type_id = '2' AND finished IS null;

