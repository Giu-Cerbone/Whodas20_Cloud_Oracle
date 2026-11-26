-- =====================================================================
-- WHODAS 2.0 - Collegamento pazienti a utenza registrata (uid)
-- ---------------------------------------------------------------------
-- Aggiunge alla tabella patients il riferimento all'utenza registrata
-- tramite ldap_uid (UID dell'utenza, NON lo username in chiaro).
-- Il gateway inoltra gia' l'header X-Username (valorizzato dallo uid
-- dell'utenza autenticata su Keycloak/LDAP), cosi' il patient-service
-- filtra i pazienti per registered_user_uid senza chiamate al user-service.
-- E' idempotente: puo' essere rieseguito su database gia' esistenti.
-- =====================================================================

-- 1. Colonna di riferimento (aggiunta solo se mancante)
ALTER TABLE patients
    ADD COLUMN IF NOT EXISTS registered_user_uid VARCHAR(100);

-- 2. Chiave esterna verso registered_users(ldap_uid) (UID dell'utenza)
ALTER TABLE patients
    DROP CONSTRAINT IF EXISTS fk_patients_registered_user;
ALTER TABLE patients
    ADD CONSTRAINT fk_patients_registered_user
    FOREIGN KEY (registered_user_uid) REFERENCES registered_users(ldap_uid);

-- 3. Indice per le query per utenza
CREATE INDEX IF NOT EXISTS idx_patients_registered_user_uid
    ON patients (registered_user_uid);

-- 4. Garantisce l'esistenza delle sole utenze coerenti con LDAP
--    (whodas_user, gcerbone) più l'eccezione richiesta (testuser).
--    Valori fittizi per i campi NOT NULL di registered_users.
INSERT INTO registered_users (
    username, password_hash, first_name, last_name, fiscal_code,
    address, postal_code, comune, city, phone, email, status,
    ldap_uid, keycloak_user_id, registered_at, modified_at
) VALUES
    ('testuser', 'x', 'Test', 'User', 'TESTUSER00A01H5X',
     NULL, NULL, NULL, NULL, NULL, 'testuser@example.com', 'ATTIVO',
     'testuser', NULL, now(), NULL),
    ('whodas_user', 'x', 'Whodas', 'User', 'WHODASUS00A01H5Y',
     NULL, NULL, NULL, NULL, NULL, 'whodas_user@example.com', 'ATTIVO',
     'whodas_user', NULL, now(), NULL),
    ('gcerbone', 'x', 'Giuseppe', 'Cerbone', '12345678901245FF',
     NULL, NULL, NULL, NULL, NULL, 'giuseppe.cerbone@gmail.com', 'ATTIVO',
     'gcerbone', NULL, now(), NULL)
ON CONFLICT (ldap_uid) DO NOTHING;

-- 5. Backfill dei pazienti gia' esistenti: una parte a testuser, il resto a whodas_user.
--    Scelta: i primi 200 pazienti (per id) vanno a testuser, tutti gli altri a whodas_user.
--    gcerbone resta senza pazienti assegnati (utenza allineata a LDAP, disponibile per uso futuro).
WITH ordered AS (
    SELECT id, row_number() OVER (ORDER BY id) AS rn
    FROM patients
    WHERE registered_user_uid IS NULL
)
UPDATE patients p
SET registered_user_uid = CASE
    WHEN o.rn <= 200 THEN 'testuser'
    ELSE 'whodas_user'
END
FROM ordered o
WHERE p.id = o.id;

-- Verifica del backfill
SELECT registered_user_uid, COUNT(*) AS pazienti
FROM patients
GROUP BY registered_user_uid
ORDER BY registered_user_uid;
