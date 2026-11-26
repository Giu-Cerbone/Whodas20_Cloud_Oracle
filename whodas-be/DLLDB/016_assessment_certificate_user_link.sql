-- =====================================================================
-- WHODAS 2.0 - Collegamento valutazioni e certificati all'utenza (uid)
-- ---------------------------------------------------------------------
-- Aggiunge a assessments e certificates il riferimento all'utenza
-- registrata tramite ldap_uid (UID dell'utenza, NON lo username).
-- Il legame si eredita dal paziente di riferimento:
--   assessments.patient_id  -> patients.id  -> patients.registered_user_uid
--   certificates.patient_id -> patients.id  -> patients.registered_user_uid
-- E' idempotente: puo' essere rieseguito su database gia' esistenti.
-- =====================================================================

-- 1. Colonna su assessments (aggiunta solo se mancante)
ALTER TABLE assessments
    ADD COLUMN IF NOT EXISTS registered_user_uid VARCHAR(100);

-- 2. Colonna su certificates (aggiunta solo se mancante)
ALTER TABLE certificates
    ADD COLUMN IF NOT EXISTS registered_user_uid VARCHAR(100);

-- 3. Chiavi esterne verso registered_users(ldap_uid)
ALTER TABLE assessments
    DROP CONSTRAINT IF EXISTS fk_assessments_registered_user;
ALTER TABLE assessments
    ADD CONSTRAINT fk_assessments_registered_user
    FOREIGN KEY (registered_user_uid) REFERENCES registered_users(ldap_uid);

ALTER TABLE certificates
    DROP CONSTRAINT IF EXISTS fk_certificates_registered_user;
ALTER TABLE certificates
    ADD CONSTRAINT fk_certificates_registered_user
    FOREIGN KEY (registered_user_uid) REFERENCES registered_users(ldap_uid);

-- 4. Indici per le query per utenza
CREATE INDEX IF NOT EXISTS idx_assessments_registered_user_uid
    ON assessments (registered_user_uid);
CREATE INDEX IF NOT EXISTS idx_certificates_registered_user_uid
    ON certificates (registered_user_uid);

-- 5. Backfill: eredita l'uid dal paziente di riferimento.
--    Il patient_id di assessments/certificates e' memorizzato come stringa
--    dell'id numerico del paziente (vedi seed 012_seed_test_data.sql).
WITH patient_uid AS (
    SELECT id::text AS patient_id_text, registered_user_uid
    FROM patients
    WHERE registered_user_uid IS NOT NULL
)
UPDATE assessments a
SET registered_user_uid = pu.registered_user_uid
FROM patient_uid pu
WHERE a.registered_user_uid IS NULL
  AND a.patient_id = pu.patient_id_text;

WITH patient_uid AS (
    SELECT id::text AS patient_id_text, registered_user_uid
    FROM patients
    WHERE registered_user_uid IS NOT NULL
)
UPDATE certificates c
SET registered_user_uid = pu.registered_user_uid
FROM patient_uid pu
WHERE c.registered_user_uid IS NULL
  AND c.patient_id = pu.patient_id_text;

-- Verifica del backfill
(SELECT 'assessments' AS tabella, registered_user_uid, COUNT(*) AS record
FROM assessments
GROUP BY registered_user_uid
ORDER BY registered_user_uid)
UNION ALL
(SELECT 'certificates' AS tabella, registered_user_uid, COUNT(*) AS record
FROM certificates
GROUP BY registered_user_uid
ORDER BY registered_user_uid);
