-- =====================================================================
-- WHODAS 2.0 - Notifiche push (notification service)
-- ---------------------------------------------------------------------
-- Tabella delle notifiche inviate dal backend al front-end.
--
-- La chiave primaria 'id' e' generata da una SEQUENCE esplicita
-- (notification_seq), come richiesto dalla specifica: "una chiave unica
-- legata a una sequenza".
--
-- Colonne della property JSON richiesta:
--   tipo_obj        -> tipo_obj       (es. "notifiche")
--   type_message    -> type_message   (es. "Warning", "Info", "Error")
--   oggetto         -> oggetto        (es. "Problema")
--   description     -> description    (es. "Prova Descrizione")
--   dataSpedizione  -> data_spedizione
--   dataLettura     -> data_lettura   (NULL finché non letta)
--   dataElimina     -> data_elimina   (NULL finché non eliminata logicamente)
--   stato           -> stato          ('SPEDITO','LETTO','MODIFICATO','CANCELLATO')
--
-- Colonne dell'operatore utente collegato:
--   user_id         -> user_id        (identificativo dell'operatore)
--   username        -> username       (username dell'operatore)
-- =====================================================================

CREATE SEQUENCE IF NOT EXISTS notification_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE TABLE IF NOT EXISTS notifications (
    id              BIGINT NOT NULL DEFAULT nextval('notification_seq') PRIMARY KEY,
    tipo_obj        VARCHAR(50)  NOT NULL DEFAULT 'notifiche',
    type_message    VARCHAR(50)  NOT NULL,
    oggetto         VARCHAR(255) NOT NULL,
    description     TEXT         NOT NULL,
    data_spedizione TIMESTAMP    NOT NULL,
    data_lettura    TIMESTAMP,
    data_elimina    TIMESTAMP,
    stato           VARCHAR(20)  NOT NULL DEFAULT 'SPEDITO',
    user_id         VARCHAR(255),
    username        VARCHAR(255),
    created_at      TIMESTAMP    NOT NULL,
    updated_at      TIMESTAMP
);

-- Vincolo di integrità: stato ammesso
ALTER TABLE notifications
    DROP CONSTRAINT IF EXISTS ck_notifications_stato;
ALTER TABLE notifications
    ADD CONSTRAINT ck_notifications_stato
    CHECK (stato IN ('SPEDITO', 'LETTO', 'MODIFICATO', 'CANCELLATO'));

-- Indici per query frequenti
CREATE INDEX IF NOT EXISTS idx_notifications_username ON notifications (username);
CREATE INDEX IF NOT EXISTS idx_notifications_stato ON notifications (stato);
CREATE INDEX IF NOT EXISTS idx_notifications_data_spedizione ON notifications (data_spedizione DESC);
CREATE INDEX IF NOT EXISTS idx_notifications_data_lettura ON notifications (data_lettura);
CREATE INDEX IF NOT EXISTS idx_notifications_data_elimina ON notifications (data_elimina);

-- Allinea la sequence al valore massimo esistente (idempotente)
SELECT setval('notification_seq',
              GREATEST(COALESCE((SELECT MAX(id) FROM notifications), 1), 1),
              true);
