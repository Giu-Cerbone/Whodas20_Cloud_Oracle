-- =====================================================================
-- WHODAS 2.0 - Allineamento dello schema
-- ---------------------------------------------------------------------
-- Gli script 001-006 usano CREATE TABLE IF NOT EXISTS, quindi su un
-- database gia' esistente le modifiche non vengono applicate.
-- Questo file riporta lo schema allo stato previsto dal codice.
-- E' idempotente: puo' essere rieseguito.
-- =====================================================================

-- 1. assessments.mode era VARCHAR(20), troppo stretto per i valori
--    previsti dal dominio (es. 'INTERVIEWER_ADMINISTERED', 23 caratteri).
ALTER TABLE assessments
    ALTER COLUMN mode TYPE VARCHAR(40);

-- 2. item_responses.domain deve essere valorizzato: il calcolo del
--    punteggio raggruppa le risposte per dominio. Con NULL il
--    raggruppamento falliva e /submit rispondeva 500.
--    Prima di rendere la colonna NOT NULL i dati esistenti vengono
--    riempiti dal catalogo, cosi' lo script non fallisce.
UPDATE item_responses r
SET domain = q.domain
FROM questionnaire_items q
WHERE q.item_code = r.item_code
  AND (r.domain IS NULL OR btrim(r.domain) = '');

DELETE FROM item_responses r
WHERE r.domain IS NULL
  AND NOT EXISTS (SELECT 1 FROM questionnaire_items q WHERE q.item_code = r.item_code);

ALTER TABLE item_responses
    ALTER COLUMN domain SET NOT NULL;

-- 3. Scala WHODAS 2.0: da 0 (nessuna difficolta') a 4 (impossibile).
--    Un valore fuori range produceva percentuali > 100 senza errore.
ALTER TABLE item_responses
    DROP CONSTRAINT IF EXISTS chk_item_responses_score_range;
ALTER TABLE item_responses
    ADD CONSTRAINT chk_item_responses_score_range
    CHECK (score IS NULL OR score BETWEEN 0 AND 4);

-- 4. Interpretazione e' un enum chiuso definito in
--    AssessmentService#interpretDisability.
ALTER TABLE assessments
    DROP CONSTRAINT IF EXISTS chk_assessments_interpretation;
ALTER TABLE assessments
    ADD CONSTRAINT chk_assessments_interpretation
    CHECK (interpretation IS NULL
           OR interpretation IN ('NONE', 'MILD', 'MODERATE', 'SEVERE', 'EXTREME'));

-- 5. Coerenza obbligatoria fra i punteggi complessivi e le risposte.
ALTER TABLE assessments
    DROP CONSTRAINT IF EXISTS chk_assessments_scores;
ALTER TABLE assessments
    ADD CONSTRAINT chk_assessments_scores
    CHECK ((total_score IS NULL OR overall_simple_score IS NULL
            OR ABS(total_score - overall_simple_score) < 0.0001)
           AND (total_items IS NULL OR answered_items IS NULL
                OR (answered_items <= total_items AND missing_items = total_items - answered_items)));

-- 6. patient_id e' VARCHAR perché i microservizi non hanno una FK condivisa,
--    ma non può essere vuoto.
ALTER TABLE patients
    DROP CONSTRAINT IF EXISTS chk_patients_fiscal_code;
ALTER TABLE patients
    ADD CONSTRAINT chk_patients_fiscal_code
    CHECK (fiscal_code IS NULL OR btrim(fiscal_code) <> '');

-- 7. Collegamento paziente -> utenza registrata (UID dell'utenza, non username).
--    Aggiunto in modo idempotente per database gia' esistenti.
ALTER TABLE patients
    ADD COLUMN IF NOT EXISTS registered_user_uid VARCHAR(100);
ALTER TABLE patients
    DROP CONSTRAINT IF EXISTS fk_patients_registered_user;
ALTER TABLE patients
    ADD CONSTRAINT fk_patients_registered_user
    FOREIGN KEY (registered_user_uid) REFERENCES registered_users(ldap_uid);
CREATE INDEX IF NOT EXISTS idx_patients_registered_user_uid
    ON patients (registered_user_uid);

-- 8. Collegamento valutazioni e certificati all'utenza registrata (UID dell'utenza).
ALTER TABLE assessments
    ADD COLUMN IF NOT EXISTS registered_user_uid VARCHAR(100);
ALTER TABLE assessments
    DROP CONSTRAINT IF EXISTS fk_assessments_registered_user;
ALTER TABLE assessments
    ADD CONSTRAINT fk_assessments_registered_user
    FOREIGN KEY (registered_user_uid) REFERENCES registered_users(ldap_uid);
CREATE INDEX IF NOT EXISTS idx_assessments_registered_user_uid
    ON assessments (registered_user_uid);

ALTER TABLE certificates
    ADD COLUMN IF NOT EXISTS registered_user_uid VARCHAR(100);
ALTER TABLE certificates
    DROP CONSTRAINT IF EXISTS fk_certificates_registered_user;
ALTER TABLE certificates
    ADD CONSTRAINT fk_certificates_registered_user
    FOREIGN KEY (registered_user_uid) REFERENCES registered_users(ldap_uid);
CREATE INDEX IF NOT EXISTS idx_certificates_registered_user_uid
    ON certificates (registered_user_uid);
