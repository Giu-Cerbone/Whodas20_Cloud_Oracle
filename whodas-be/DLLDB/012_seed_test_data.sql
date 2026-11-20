-- =====================================================================
-- WHODAS 2.0 - DATI DI TEST / SEED
-- ---------------------------------------------------------------------
-- Genera un insieme ampio e coerente di dati per test end-to-end del
-- front-end e delle API: dashboard, lista pazienti, form paziente,
-- questionario, certificati, calcolo punteggi, filtri e aggregazioni.
--
-- VOLUME GENERATO (circa 58.000 record)
--   questionnaire_items          36   item WHODAS 2.0 (6+5+4+5+8+8 = 36)
--   patients                    ~600  codice fiscale CALCOLATO (check digit CIN)
--   assessments                ~1800  3 per paziente (2 COMPLETED + 1 DRAFT)
--   item_responses            ~58000  36 per valutazione completata, 0-36 per bozza
--   certificates                ~780  stati ISSUED / EXPIRED / REVOKED
--   certificate_domain_scores   ~4700  6 domini per certificato
--
-- COPERTURA FUNZIONALE
--   - tutti e 5 i livelli di disabilita' (NONE / MILD / MODERATE / SEVERE / EXTREME)
--   - valutazione iniziale + follow-up con miglioramento sullo stesso paziente
--   - bozze vuote, parziali e complete ma non sottomesse
--   - pazienti senza condizione di salute e pazienti con gender = 'Altro'
--   - certificati emessi, scaduti e revocati
--
-- COERENZA CON IL BACKEND (whodas-assessment-service)
--   - total_score          = somma grezza complessiva (0..144)
--   - overall_simple_score = overall_complex_score = total_score
--   - percent_disability   = raw / (n * 4) * 100
--   I punteggi NON sono scritti a mano: vengono ricalcolati dal database
--   (sezione 7) con la stessa formula di AssessmentService#submitAndScore,
--   quindi non possono divergere dalle risposte effettivamente salvate.
--   - item_responses.domain e' NOT NULL (vedi 008_align_schema.sql) ed e'
--     valorizzato dal catalogo: senza di esso il raggruppamento per dominio
--     del servizio falliva.
--
-- Il file e' IDEMPOTENTE e TRASPORTABILE: ripulisce i dati che aveva
-- generato in precedenza prima di reinserirli, quindi puo' essere
-- eseguito piu' volte e su qualunque database con lo stesso schema.
--
-- I codici fiscali sono calcolati con l'algoritmo CIN dell'Agenzia delle
-- Entrate (carattere di controllo corretto) ma NON corrispondono a
-- persone reali.
-- =====================================================================

BEGIN;

-- Seme fisso: il dataset e' riproducibile a parita' di versione di Postgres.
SELECT setseed(0.424242);

-- ---------------------------------------------------------------------
-- 0. PULIZIA DEI DATI PRECEDENTI
--    TRUNCATE ... RESTART IDENTITY azzera anche le sequence, cosi' gli id
--    ripartono da 1 a ogni esecuzione e restano leggibili.
--    Nessuna delle tabelle e' referenziata da chiavi esterne: i vincoli
--    fra servizi diversi sono garantiti dal codice applicativo.
-- ---------------------------------------------------------------------
TRUNCATE TABLE certificate_domain_scores, certificates, item_responses,
               assessments, patients, questionnaire_items
    RESTART IDENTITY CASCADE;

-- ---------------------------------------------------------------------
-- 1. TABELLE DI RIFERIMENTO (temporanee, usate solo dal generatore)
-- ---------------------------------------------------------------------

-- 1a. Comuni italiani con il rispettivo codice catastale di 4 caratteri
--     (belforio), che occupa le posizioni 12-15 del codice fiscale.
--     La tabella non ha ON COMMIT DROP perche' la verifica 10h la
--     interroga dopo il COMMIT per controllare la validita' del comune.
CREATE TEMP TABLE _comuni (
    codice    varchar(6) PRIMARY KEY,
    nome      varchar(60),
    provincia varchar(2),
    regione   varchar(60)
);

INSERT INTO _comuni (codice, nome, provincia, regione) VALUES
('H501','Roma',              'RM','Lazio'),                  ('F205','Milano',            'MI','Lombardia'),
('F839','Napoli',            'NA','Campania'),               ('L219','Torino',            'TO','Piemonte'),
('G273','Palermo',           'PA','Sicilia'),                ('A944','Bologna',           'BO','Emilia-Romagna'),
('A662','Bari',              'BA','Puglia'),                 ('D612','Firenze',           'FI','Toscana'),
('D969','Genova',            'GE','Liguria'),                ('C351','Catania',           'CT','Sicilia'),
('L736','Venezia',           'VE','Veneto'),                 ('L781','Verona',            'VR','Veneto'),
('F158','Messina',           'ME','Sicilia'),                ('G224','Padova',            'PD','Veneto'),
('L424','Trieste',           'TS','Friuli-Venezia Giulia'),  ('L049','Taranto',           'TA','Puglia'),
('B157','Brescia',           'BS','Lombardia'),              ('G337','Parma',             'PR','Emilia-Romagna'),
('F257','Modena',            'MO','Emilia-Romagna'),         ('H224','Reggio Calabria',   'RC','Calabria'),
('G478','Perugia',           'PG','Umbria'),                 ('H199','Ravenna',           'RA','Emilia-Romagna'),
('E625','Livorno',           'LI','Toscana'),                ('B354','Cagliari',          'CA','Sardegna'),
('D643','Foggia',            'FG','Puglia'),                 ('H294','Rimini',            'RN','Emilia-Romagna'),
('H703','Salerno',           'SA','Campania'),               ('D548','Ferrara',           'FE','Emilia-Romagna'),
('I452','Sassari',           'SS','Sardegna'),               ('E472','Latina',            'LT','Lazio'),
('F704','Monza',             'MB','Lombardia'),              ('I754','Siracusa',          'SR','Sicilia'),
('G482','Pescara',           'PE','Abruzzo'),                ('A794','Bergamo',           'BG','Lombardia'),
('D704','Forli',             'FC','Emilia-Romagna'),         ('L378','Trento',            'TN','Trentino-Alto Adige'),
('L840','Vicenza',           'VI','Veneto'),                 ('L117','Terni',             'TR','Umbria'),
('A952','Bolzano',           'BZ','Trentino-Alto Adige'),    ('F952','Novara',            'NO','Piemonte'),
('G535','Piacenza',          'PC','Emilia-Romagna'),         ('A271','Ancona',            'AN','Marche'),
('C573','Cesena',            'FC','Emilia-Romagna'),         ('E506','Lecce',             'LE','Puglia'),
('A669','Barletta',          'BT','Puglia'),                 ('A182','Alessandria',       'AL','Piemonte'),
('E463','La Spezia',         'SP','Liguria'),                ('G702','Pisa',              'PI','Toscana'),
('H223','Caserta',           'CE','Campania'),               ('G626','Castelfranco Emilia','BO','Emilia-Romagna');

-- 1b. Cognomi italiani.
CREATE TEMP TABLE _cognomi (nome varchar(40)) ON COMMIT DROP;
INSERT INTO _cognomi (nome) VALUES
('Rossi'),('Bianchi'),('Esposito'),('Verdi'),('Greco'),('Russo'),('Ferrari'),('Romano'),
('Colombo'),('Ricci'),('Marino'),('Bruno'),('Gallo'),('Conti'),('Mancini'),('Costa'),('Giordano'),
('Rizzo'),('Lombardi'),('Moretti'),('Barbieri'),('Fontana'),('Santoro'),('Mariani'),('Rinaldi'),
('Caruso'),('Ferrara'),('Galli'),('Martini'),('Leone'),('Longo'),('Gentile'),('Martinelli'),('Vitale'),
('Lombardo'),('Serra'),('Coppola'),('De Santis'),('Damato'),('Sala'),('Pellegrini'),('Monti'),
('Cattaneo'),('Valentini'),('Tortora'),('Sanna'),('Fadda'),('Farina'),('Testa'),('D Amico'),
('Palumbo'),('Sorrentino'),('Bellini'),('Montanari'),('Cavallo'),('Amato'),('Silvestri'),('Di Stefano'),
('Fiore'),('Ruggeri'),('Sartori'),('Basile'),('Guercio'),('Pagano'),('Carbone'),('Mancuso'),
('Orlando'),('Parisi'),('Villa'),('Giuliano'),('Milo'),('Battaglia'),('Cosenza'),('Donato'),
('Palmieri'),('Rinaldi'),('Sartori'),('Venturi'),('Zanetti'),('Orlando'),('Piras'),('Sala');

-- 1c. Nomi di battesimo con il rispettivo sesso anagrafico.
CREATE TEMP TABLE _nomi (nome varchar(40), sesso char(1)) ON COMMIT DROP;
INSERT INTO _nomi (nome, sesso) VALUES
('Mario','M'),('Giulia','F'),('Luca','M'),('Anna','F'),('Paolo','M'),('Maria','F'),
('Francesco','M'),('Chiara','F'),('Alessandro','M'),('Francesca','F'),('Matteo','M'),('Sara','F'),
('Andrea','M'),('Martina','F'),('Davide','M'),('Elena','F'),('Simone','M'),('Valentina','F'),
('Stefano','M'),('Beatrice','F'),('Gabriele','M'),('Alice','F'),('Federico','M'),('Anna Maria','F'),
('Riccardo','M'),('Lucia','F'),('Tommaso','M'),('Greta','F'),('Emanuele','M'),('Irene','F'),
('Gianluca','M'),('Noemi','F'),('Massimo','M'),('Silvia','F'),('Roberto','M'),('Camilla','F'),
('Michele','M'),('Aurora','F'),('Antonio','M'),('Ginevra','F'),('Giuseppe','M'),('Ludovica','F'),
('Nicola','M'),('Emma','F'),('Pietro','M'),('Sofia','F'),('Vincenzo','M'),('Caterina','F'),
('Salvatore','M'),('Giorgia','F'),('Marco','M'),('Nicole','F'),('Lorenzo','M'),('Marta','F'),
('Alessio','M'),('Sara','F'),('Cristian','M'),('Elisa','F'),('Fabio','M'),('Anna','F'),
('Diego','M'),('Carlotta','F'),('Enrico','M'),('Bianca','F'),('Cesare','M'),('Costanza','F'),
('Ernesto','M'),('Daniela','F'),('Corrado','M'),('Federica','F'),('Omar','M'),('Rita','F'),
('Pierluigi','M'),('Manuela','F'),('Quirino','M'),('Nadia','F'),('Raffaele','M'),('Cinzia','F'),
('Ugo','M'),('Paola','F'),('Vito','M'),('Barbara','F'),('Edoardo','M'),('Alessandra','F'),
('Flavio','M'),('Rosalia','F'),('Ivano','M'),('Grazia','F'),('Alex','M'),('Noa','F');

-- 1d. Condizioni di salute per il campo health_condition.
CREATE TEMP TABLE _condizioni (testo varchar(200)) ON COMMIT DROP;
INSERT INTO _condizioni (testo) VALUES
('Spondilosi lombare e lombalgia cronica. Trattamento fisioterapico in corso.'),
('Artrite reumatoide in remissione. Controllo annuale.'),
('Sindrome di Down con ritardo dello sviluppo. Follow-up neuropsichiatrico.'),
('Sclerosi multipla in remissione parziale. Ausilio per la deambulazione.'),
('Post-operatorio di ricostruzione del legno dopo infortunio sul lavoro.'),
('Disturbi dell''umore in terapia farmacologica.'),
('Ipertensione arteriosa e diabete di tipo 2. Nessuna limitazione funzionale.'),
('Coxartrosi bilaterale in attesa di protesi.'),
('Esito di ictus cerebrale con emiparesi destra.'),
('Broncopneumopatia cronica ostruttiva. Riabilitazione respiratoria.'),
('Gonalgia bilaterale. Nessuna limitazione nelle attivita'' quotidiane.'),
('Miopatia congenita. Forza muscolare ridotta agli arti inferiori.'),
('Morbo di Parkinson in stadio iniziale (Hoehn e Yahr 2).'),
('Cecita'' congenita. Uso di screen reader e Braille per la scrittura.'),
('Sordita'' neurosensoriale bilaterale. Uso della lip reading.'),
('Trauma cranico. Deficit di memoria e di concentrazione.'),
('Tiroidectomia totale. Ipotiroidismo in terapia sostitutiva.'),
('Nessuna patologia dichiarata.'),
('Alcolismo cronico in remissione. Supporto psicosociale.'),
('Lombalgia meccanica. Frequenti episodi acuti.');

-- 1e. Tabelle del calcolo del carattere di controllo del codice fiscale
--     (algoritmo CIN: valori dispari in posizione dispari, pari in posizione pari).
--     Non hanno ON COMMIT DROP perche' la verifica 10h, che esegue un
--     controllo indipendente del CIN, gira dopo il COMMIT. Vivono solo per
--     la sessione e spariscono alla chiusura della connessione.
CREATE TEMP TABLE _cf_dispari (c char(1) PRIMARY KEY, v int);
INSERT INTO _cf_dispari (c, v) VALUES
('0',1),('1',0),('2',5),('3',7),('4',9),('5',13),('6',15),('7',17),('8',19),('9',21),
('A',1),('B',0),('C',5),('D',7),('E',9),('F',13),('G',15),('H',17),('I',19),('J',21),
('K',2),('L',4),('M',6),('N',8),('O',10),('P',12),('Q',14),('R',16),('S',18),('T',20),
('U',3),('V',5),('W',7),('X',9),('Y',11),('Z',13);

CREATE TEMP TABLE _cf_pari (c char(1) PRIMARY KEY, v int);
INSERT INTO _cf_pari (c, v) VALUES
('0',0),('1',1),('2',2),('3',3),('4',4),('5',5),('6',6),('7',7),('8',8),('9',9),
('A',0),('B',1),('C',2),('D',3),('E',4),('F',5),('G',6),('H',7),('I',8),('J',9),
('K',0),('L',1),('M',2),('N',3),('O',4),('P',5),('Q',6),('R',7),('S',8),('T',9),
('U',10),('V',11),('W',12),('X',13),('Y',14),('Z',15);

-- 1f. Lettera del mese nel codice fiscale (gennaio = A, febbraio = B, ...).
CREATE TEMP TABLE _mesi (mese int PRIMARY KEY, lettera char(1)) ON COMMIT DROP;
INSERT INTO _mesi (mese, lettera) VALUES
(1,'A'),(2,'B'),(3,'C'),(4,'D'),(5,'E'),(6,'H'),
(7,'L'),(8,'M'),(9,'P'),(10,'R'),(11,'T'),(12,'D');

-- 1g. Voci per indirizzi e note, accorpate in array per una scelta casuale
--     coerente (stesso indice per piu' campi correlati).
CREATE TEMP TABLE _tasti AS
SELECT (SELECT array_agg(nome)  FROM _cognomi)   AS cognomi,
       (SELECT array_agg(nome || '|' || sesso) FROM _nomi) AS nomi,
       (SELECT array_agg(codice) FROM _comuni)   AS codici,
       (SELECT array_agg(nome)   FROM _comuni)   AS comuni,
       (SELECT array_agg(regione) FROM _comuni)  AS regioni,
       (SELECT array_agg(testo)  FROM _condizioni) AS condizioni,
       ARRAY['Via Roma','Via Garibaldi','Via Mazzini','Corso Italia','Via Dante',
              'Viale Europa','Via G. Verdi','Piazza del Popolo','Via Napoleone',
              'Via A. Manzoni'] AS strade;

-- ---------------------------------------------------------------------
-- 2. QUESTIONNAIRE ITEMS - WHODAS 2.0, versione a 36 item
--    I codici dominio coincidono con it.whodas.common.domain.DomainType.
--    6 + 5 + 4 + 5 + 8 + 8 = 36
-- ---------------------------------------------------------------------
INSERT INTO questionnaire_items
    (item_code, domain, domain_label, domain_label_it, question_text, question_text_it,
     icf_code, item_group, order_num, response_type)
VALUES
-- COGNITION / Comprendere e comunicare (6)
('D1S1',  'COGNITION', 'Understanding and communicating', 'Comprendere e comunicare',
 'Understanding and communicating', 'Capire e comunicare', 'b1-b2', 'SECTION_1', 1, 'LIKERT_5'),
('D1S2',  'COGNITION', 'Understanding and communicating', 'Comprendere e comunicare',
 'Concentrating', 'Concentrare la propria attenzione per almeno 10 minuti', 'b1-b2', 'SECTION_1', 2, 'LIKERT_5'),
('D1S3',  'COGNITION', 'Understanding and communicating', 'Comprendere e comunicare',
 'Solving problems of daily life', 'Risolvere i problemi della vita quotidiana', 'b1-b2', 'SECTION_1', 3, 'LIKERT_5'),
('D1S4',  'COGNITION', 'Understanding and communicating', 'Comprendere e comunicare',
 'Using a computer or cell phone', 'Usare il computer o il telefono cellulare', 'b1-b2', 'SECTION_1', 4, 'LIKERT_5'),
('D1S5',  'COGNITION', 'Understanding and communicating', 'Comprendere e comunicare',
 'Focusing on own appearance', 'Prendersi cura del proprio aspetto fisico', 'b1-b2', 'SECTION_1', 5, 'LIKERT_5'),
('D1S6',  'COGNITION', 'Understanding and communicating', 'Comprendere e comunicare',
 'Solving problems in communication with others', 'Risolvere i problemi di comunicazione con altre persone', 'b1-b2', 'SECTION_1', 6, 'LIKERT_5'),

-- MOBILITY / Mobilita' (5)
('D1S7',  'MOBILITY', 'Moving and getting around', 'Mobilita''',
 'Standing up from a chair and standing', 'Alzarsi in piedi e stare in piedi', 'd4', 'SECTION_2', 7, 'LIKERT_5'),
('D1S8',  'MOBILITY', 'Moving and getting around', 'Mobilita''',
 'Walking', 'Camminare', 'd4', 'SECTION_2', 8, 'LIKERT_5'),
('D1S9',  'MOBILITY', 'Moving and getting around', 'Mobilita''',
 'Climbing stairs', 'Salire le scale', 'd4', 'SECTION_2', 9, 'LIKERT_5'),
('D1S10', 'MOBILITY', 'Moving and getting around', 'Mobilita''',
 'Moving around within the home', 'Spostarsi all''interno della propria casa', 'd4', 'SECTION_2', 10, 'LIKERT_5'),
('D1S11', 'MOBILITY', 'Moving and getting around', 'Mobilita''',
 'Moving around outside the home, walking', 'Spostarsi all''esterno, camminare', 'd4', 'SECTION_2', 11, 'LIKERT_5'),

-- SELF_CARE / Cura di se' (4)
('D1S12', 'SELF_CARE', 'Self-care', 'Cura di se''',
 'Washing oneself', 'Lavarsi', 'd5', 'SECTION_3', 12, 'LIKERT_5'),
('D1S13', 'SELF_CARE', 'Self-care', 'Cura di se''',
 'Dressing', 'Vestirsi', 'd5', 'SECTION_3', 13, 'LIKERT_5'),
('D1S14', 'SELF_CARE', 'Self-care', 'Cura di se''',
 'Eating', 'Mangiare', 'd5', 'SECTION_3', 14, 'LIKERT_5'),
('D1S15', 'SELF_CARE', 'Self-care', 'Cura di se''',
 'Staying alone', 'Stare da solo/a', 'd5', 'SECTION_3', 15, 'LIKERT_5'),

-- GETTING_ALONG / Relazionarsi con le persone (5)
('D1S16', 'GETTING_ALONG', 'Getting along with people', 'Relazionarsi con le persone',
 'Engaging in work or other activities with others', 'Svolgere compiti o attivita'' insieme ad altre persone', 'd4-d5', 'SECTION_4', 16, 'LIKERT_5'),
('D1S17', 'GETTING_ALONG', 'Getting along with people', 'Relazionarsi con le persone',
 'Taking part in social activities', 'Prendere parte ad attivita'' sociali', 'd4-d5', 'SECTION_4', 17, 'LIKERT_5'),
('D1S18', 'GETTING_ALONG', 'Getting along with people', 'Relazionarsi con le persone',
 'Being part of a family', 'Far parte di una famiglia', 'd4-d5', 'SECTION_4', 18, 'LIKERT_5'),
('D1S19', 'GETTING_ALONG', 'Getting along with people', 'Relazionarsi con le persone',
 'Making and maintaining friends', 'Stabilire e mantenere amicizie', 'd4-d5', 'SECTION_4', 19, 'LIKERT_5'),
('D1S20', 'GETTING_ALONG', 'Getting along with people', 'Relazionarsi con le persone',
 'Being influenced by other people', 'Farsi influenzare da altre persone', 'd4-d5', 'SECTION_4', 20, 'LIKERT_5'),

-- LIFE_ACTIVITIES / Attivita' della vita quotidiana (8)
('D1S21', 'LIFE_ACTIVITIES', 'Life activities', 'Attivita'' della vita quotidiana',
 'Doing household tasks', 'Svolgere le faccende domestiche', 'd6', 'SECTION_5', 21, 'LIKERT_5'),
('D1S22', 'LIFE_ACTIVITIES', 'Life activities', 'Attivita'' della vita quotidiana',
 'Shopping', 'Fare acquisti', 'd6', 'SECTION_5', 22, 'LIKERT_5'),
('D1S23', 'LIFE_ACTIVITIES', 'Life activities', 'Attivita'' della vita quotidiana',
 'Cooking', 'Preparare i pasti', 'd6', 'SECTION_5', 23, 'LIKERT_5'),
('D1S24', 'LIFE_ACTIVITIES', 'Life activities', 'Attivita'' della vita quotidiana',
 'Doing housework', 'Fare i lavori di casa', 'd6', 'SECTION_5', 24, 'LIKERT_5'),
('D1S25', 'LIFE_ACTIVITIES', 'Life activities', 'Attivita'' della vita quotidiana',
 'Running errands', 'Fare commissioni', 'd6', 'SECTION_5', 25, 'LIKERT_5'),
('D1S26', 'LIFE_ACTIVITIES', 'Life activities', 'Attivita'' della vita quotidiana',
 'Finding a job', 'Farsi aiutare a trovare lavoro', 'd6', 'SECTION_5', 26, 'LIKERT_5'),
('D1S27', 'LIFE_ACTIVITIES', 'Life activities', 'Attivita'' della vita quotidiana',
 'Engaging in work', 'Svolgere attivita'' di lavoro', 'd6', 'SECTION_5', 27, 'LIKERT_5'),
('D1S28', 'LIFE_ACTIVITIES', 'Life activities', 'Attivita'' della vita quotidiana',
 'Taking part in leisure or free time activities', 'Prendere parte ad attivita'' di svago e tempo libero', 'd6', 'SECTION_5', 28, 'LIKERT_5'),

-- PARTICIPATION / Partecipazione alla vita sociale (8)
('D1S29', 'PARTICIPATION', 'Participation', 'Partecipazione alla vita sociale',
 'Joining in community activities', 'Prendere parte alle attivita'' della comunita''', 'd5-d6', 'SECTION_6', 29, 'LIKERT_5'),
('D1S30', 'PARTICIPATION', 'Participation', 'Partecipazione alla vita sociale',
 'Getting news and information using social media', 'Informarsi e utilizzare i social media', 'd5-d6', 'SECTION_6', 30, 'LIKERT_5'),
('D1S31', 'PARTICIPATION', 'Participation', 'Partecipazione alla vita sociale',
 'Being a member of an association or club', 'Essere membro di un''associazione o di un club', 'd5-d6', 'SECTION_6', 31, 'LIKERT_5'),
('D1S32', 'PARTICIPATION', 'Participation', 'Partecipazione alla vita sociale',
 'Taking part in cultural or sporting activities', 'Prendere parte ad attivita'' culturali e sportive', 'd5-d6', 'SECTION_6', 32, 'LIKERT_5'),
('D1S33', 'PARTICIPATION', 'Participation', 'Partecipazione alla vita sociale',
 'Maintaining friendships', 'Conservare le amicizie', 'd5-d6', 'SECTION_6', 33, 'LIKERT_5'),
('D1S34', 'PARTICIPATION', 'Participation', 'Partecipazione alla vita sociale',
 'Getting help from others', 'Farsi aiutare dagli altri', 'd5-d6', 'SECTION_6', 34, 'LIKERT_5'),
('D1S35', 'PARTICIPATION', 'Participation', 'Partecipazione alla vita sociale',
 'Contributing to community activities', 'Contribuire alle attivita'' della comunita''', 'd5-d6', 'SECTION_6', 35, 'LIKERT_5'),
('D1S36', 'PARTICIPATION', 'Participation', 'Partecipazione alla vita sociale',
 'Enjoying life', 'Godersi appieno la vita', 'd5-d6', 'SECTION_6', 36, 'LIKERT_5');

-- ---------------------------------------------------------------------
-- 3. PAZIENTI (~600) con codice fiscale strutturalmente valido
--
--    Il codice fiscale e' composto secondo l'algoritmo dell'Agenzia delle
--    Entrate:
    -- 3 consonanti del cognome + 3 consonanti del nome + ultime 2 cifre
    -- dell'anno + lettera del mese + giorno (+40 per le donne)
    -- + codice del comune (4) + carattere di controllo CIN.
--    Le consonanti sono: 1a e 4a se il nome ha almeno 4 consonanti,
--    altrimenti le prime 3 completate con X.
-- ---------------------------------------------------------------------
CREATE TEMP TABLE _pazienti (
    id            serial,
    cognome       varchar(40),
    nome          varchar(40),
    sesso         char(1),
    birth_date    date,
    birth_place   varchar(60),
    codice_comune varchar(6),
    regione       varchar(60),
    difficolta    double precision,
    cons_cognome  varchar(3),
    cons_nome     varchar(3)
) ON COMMIT DROP;

-- Il LATERAL e' correlato a g(i) perche' random() venga valutato una volta
-- per riga: con una sottoquery non correlata Postgres la calcolerebbe una
-- volta sola e tutti i pazienti risulterebbero identici.
INSERT INTO _pazienti (cognome, nome, sesso, birth_date, birth_place,
                       codice_comune, regione, difficolta, cons_cognome, cons_nome)
SELECT s.cognome, n.nome, n.sesso, s.birth_date, s.comune_nome,
       s.comune_codice, s.regione, s.difficolta,
       codice.cons_cognome, codice.cons_nome
FROM generate_series(1, 600) AS g(i)
CROSS JOIN _tasti t
CROSS JOIN LATERAL (
    SELECT (random() * array_length(t.cognomi, 1))::int + g.i * 0 AS i_cog,
           (random() * array_length(t.nomi,    1))::int + g.i * 0 AS i_nom,
           (random() * array_length(t.comuni,  1))::int + g.i * 0 AS i_com,
           DATE '1940-01-01' + ((random() * 23000)::int)         AS birth_date,
           random()                                              AS r_diff
) rnd
CROSS JOIN LATERAL (
    SELECT (t.cognomi)[1 + (rnd.i_cog % array_length(t.cognomi, 1))]  AS cognome,
           (t.nomi)[1    + (rnd.i_nom % array_length(t.nomi,    1))]  AS nominativo,
           (t.comuni)[1  + (rnd.i_com % array_length(t.comuni,  1))]  AS comune_nome,
           (t.codici)[1  + (rnd.i_com % array_length(t.codici,  1))]  AS comune_codice,
           (t.regioni)[1 + (rnd.i_com % array_length(t.regioni, 1))]  AS regione,
           rnd.birth_date                                             AS birth_date,
           -- Quattro fasce di difficolta', una per ciascun livello di
           -- disabilita' WHODAS, cosi' tutte le interpretazioni sono
           -- rappresentate nel dataset:
           --   [0.00-0.05] nessuna   [0.06-0.20] lieve
           --   [0.21-0.45] moderata  [0.50-0.80] grave
           CASE WHEN rnd.r_diff < 0.12 THEN 0.00 + rnd.r_diff * 0.42
                WHEN rnd.r_diff < 0.45 THEN 0.06 + (rnd.r_diff - 0.12) * 0.42
                WHEN rnd.r_diff < 0.75 THEN 0.21 + (rnd.r_diff - 0.45) * 0.80
                ELSE                     0.50 + (rnd.r_diff - 0.75) * 1.20
           END                                                        AS difficolta
) s
CROSS JOIN LATERAL (
    SELECT split_part(s.nominativo, '|', 1) AS nome,
           split_part(s.nominativo, '|', 2) AS sesso
) n
-- Le consonanti del codice fiscale si ricavano togliendo vocali e spazi
-- (i cognomi composti come "Di Stefano" contengono uno spazio), poi
-- prendendo la 1a e la 4a se ci sono almeno 4 consonanti, altrimenti
-- le prime 3 completate con X.
CROSS JOIN LATERAL (
    SELECT regexp_replace(regexp_replace(upper(s.cognome), '[^A-Z]', '', 'g'),
                          '[AEIOU]', '', 'g') AS cg,
           regexp_replace(regexp_replace(upper(split_part(s.nominativo, '|', 1)),
                                         '[^A-Z]', '', 'g'),
                          '[AEIOU]', '', 'g') AS nm
) cons
CROSS JOIN LATERAL (
    SELECT CASE WHEN length(cons.cg) >= 4
                THEN substr(cons.cg, 1, 1) || substr(cons.cg, 4, 1) || 'X'
                ELSE rpad(substr(cons.cg, 1, 3), 3, 'X') END AS cons_cognome,
           CASE WHEN length(cons.nm) >= 4
                THEN substr(cons.nm, 1, 1) || substr(cons.nm, 4, 1) || 'X'
                ELSE rpad(substr(cons.nm, 1, 3), 3, 'X') END AS cons_nome
) codice;

-- Calcolo del carattere di controllo e assemblaggio del codice a 16 caratteri.
CREATE TEMP TABLE _cf AS
WITH base AS (
    SELECT p.id,
           p.cons_cognome
        || p.cons_nome
        || lpad((extract(year FROM p.birth_date)::int % 100)::text, 2, '0')
        || m.lettera
        || lpad((extract(day FROM p.birth_date)::int
                 + CASE WHEN p.sesso = 'F' THEN 40 ELSE 0 END)::text, 2, '0')
        || p.codice_comune AS cf15
    FROM _pazienti p
    JOIN _mesi m ON m.mese = extract(month FROM p.birth_date)::int
),
somma AS (
    SELECT b.id,
           b.cf15,
           SUM(CASE WHEN g.pos % 2 = 1 THEN d.v ELSE p.v END) AS totale
    FROM base b
    CROSS JOIN LATERAL generate_series(1, 15) AS g(pos)
    JOIN _cf_dispari d ON d.c = substr(b.cf15, g.pos, 1)
    JOIN _cf_pari    p ON p.c = substr(b.cf15, g.pos, 1)
    GROUP BY b.id, b.cf15
)
SELECT id,
       cf15 || chr((65 + (totale % 26))::int) AS fiscal_code
FROM somma;

INSERT INTO patients
    (first_name, last_name, fiscal_code, birth_date, birth_place, gender,
     address, phone, email, health_condition, notes)
-- Si mantiene la grafia del nome e del cognome, soltanto inizializzando
-- la prima lettera: i cognomi composti ("Di Stefano") perderebbero
-- altrimenti le maiuscole interne.
SELECT upper(left(s.nome, 1))    || substr(s.nome, 2),
       upper(left(s.cognome, 1)) || substr(s.cognome, 2),
       cf.fiscal_code,
       s.birth_date,
       s.birth_place,
       s.sesso,
       (t.strade)[1 + (s.id % array_length(t.strade, 1))]
         || ' ' || (1 + (s.id * 7 % 180)) || ', '
         || (10000 + (s.id * 13 % 89999)) || ' ' || s.birth_place,
       '3' || lpad(((s.id * 3) % 100000000)::text, 8, '0') || (s.id % 10),
       lower(regexp_replace(s.nome || '.' || s.cognome, '[^a-zA-Z]', '', 'g'))
         || s.id || '@example.com',
       -- 1 paziente su 12 senza condizione di salute dichiarata
       CASE WHEN s.id % 12 = 0 THEN NULL ELSE (t.condizioni)[1 + (s.id % array_length(t.condizioni, 1))] END,
       'Paziente generato per test funzionale WHODAS 2.0.'
FROM _pazienti s
JOIN _cf    cf ON cf.id = s.id
CROSS JOIN _tasti t
ON CONFLICT (fiscal_code) DO NOTHING;

-- Pazienti con identita' di genere non convenzionale: il sesso anagrafico
-- del codice fiscale resta invariato, la terza opzione della select del
-- front-end viene pero' esercitata.
UPDATE patients
SET gender = 'Altro'
WHERE id % 97 = 0
  AND gender <> 'Altro';

-- ---------------------------------------------------------------------
-- 4. ASSESSMENTS (~1800) - 3 valutazioni per paziente:
--      1) iniziale   COMPLETED alla difficolta' di partenza
--      2) follow-up  COMPLETED con difficolta' ridotta del 25%
--      3) bozza      DRAFT, parziale / vuota / completa ma non sottomessa
-- ---------------------------------------------------------------------
CREATE TEMP TABLE _valutazioni (
    id          bigserial,
    patient_id  bigint,
    seq         int,
    status      varchar(20),
    difficolta  double precision,
    mode        varchar(40),
    partiali    int
) ON COMMIT DROP;

INSERT INTO _valutazioni (patient_id, seq, status, difficolta, mode, partiali)
SELECT p.id,
       v.seq,
       v.status,
       g.difficolta * v.fattore,
       v.mode,
       CASE v.seq
           WHEN 3 THEN
               CASE WHEN random() < 0.35 THEN 0
                    WHEN random() < 0.70 THEN 8 + (random() * 13)::int
                    ELSE 36
               END
           ELSE 0
       END
-- La difficolta' individuale sta in _pazienti, ma l'id del paziente e' quello
-- assegnato dalla sequence di patients: il collegamento passa dal codice
-- fiscale, che e' la chiave naturale condivisa.
FROM patients p
JOIN _cf     cf ON cf.fiscal_code = p.fiscal_code
JOIN _pazienti g ON g.id = cf.id
CROSS JOIN (VALUES
    (1, 'COMPLETED', 1.00, 'SELF_ADMINISTERED'),
    (2, 'COMPLETED', 0.75, 'SELF_ADMINISTERED'),
    (3, 'DRAFT',      0.90, 'INTERVIEWER')
) AS v(seq, status, fattore, mode);

INSERT INTO assessments
    (patient_id, version, status, mode, language, interviewer, notes, created_at, updated_at, compile_type_id)
    SELECT p.patient_id,
           '36-ITEM',
           p.status,
           p.mode,
           'it-IT',
           'Dr. ' || (t.cognomi)[1 + (p.id % array_length(t.cognomi, 1))],
           CASE p.seq
               WHEN 1 THEN 'Valutazione iniziale.'
               WHEN 2 THEN 'Follow-up: rivalutazione periodica.'
               ELSE        'Bozza in compilazione.'
           END,
           TIMESTAMP '2025-01-06 08:00' + ((p.patient_id * 137) % 560) * INTERVAL '1 day'
                                + ((p.patient_id * 29) % 480) * INTERVAL '1 minute',
           TIMESTAMP '2025-01-06 08:00' + ((p.patient_id * 137) % 560) * INTERVAL '1 day'
                                + ((p.patient_id * 29) % 480) * INTERVAL '1 minute',
           -- Mappa la modalità di compilazione al tipo corrispondente:
           --   SELF_ADMINISTERED -> 1 (Autosomministrato)
           --   INTERVIEWER        -> 2 (Somministrato da un intervistatore)
           CASE p.mode
               WHEN 'SELF_ADMINISTERED' THEN 1
               WHEN 'INTERVIEWER'        THEN 2
               ELSE 1
           END
    FROM _valutazioni p
    CROSS JOIN _tasti t;

-- ---------------------------------------------------------------------
-- 5. ITEM RESPONSES
--    Il punteggio di ogni item dipende dalla difficolta' del paziente piu'
--    un rumore casuale, cosi' la distribuzione risultante e' realistica e
--    non uniforme. Scala WHODAS 2.0: da 0 (nessuna difficolta') a 4.
-- ---------------------------------------------------------------------

-- 5a. Valutazioni COMPLETE: 36 risposte ciascuna.
--     Il rumore e' proporzionale alla difficolta': con poco rumore i
--     pazienti quasi sani restano effettivamente sotto il 5% (livello NONE),
--     con rumore ampio i casi gravi saturano verso il 4.
INSERT INTO item_responses (assessment_id, item_code, domain, score, days, note, created_at)
SELECT a.id,
       q.item_code,
       q.domain,
       LEAST(4, GREATEST(0, ROUND(4 * LEAST(1, GREATEST(0,
           v.difficolta + (random() - 0.5) * (0.15 + 0.60 * v.difficolta)
       )))::int)) AS score,
       CASE WHEN q.order_num = 4  THEN 7
            WHEN q.order_num IN (8, 9) THEN 30
            ELSE 7 + (random() * 24)::int
       END AS days,
       CASE WHEN random() < 0.08
            THEN (ARRAY['Difficolta'' segnalata in sede di somministrazione',
                        'Paziente riferisce affaticamento',
                        'Richiede supervisione',
                        'Usa ausilio',
                        'Difficolta'' non osservata in sede di test'])[1 + (random() * 5)::int]
            ELSE NULL
       END AS note,
       a.created_at + INTERVAL '10 minutes'
FROM _valutazioni v
JOIN assessments a ON a.id = v.id
CROSS JOIN questionnaire_items q
WHERE v.status = 'COMPLETED'
  AND NOT EXISTS (
      SELECT 1 FROM item_responses r
      WHERE r.assessment_id = a.id AND r.item_code = q.item_code
  );

-- 5b. Bozze: risposte solo per i primi N item, con qualche item lasciato
--     senza punteggio (non risposto).
INSERT INTO item_responses (assessment_id, item_code, domain, score, days, note, created_at)
SELECT a.id,
       q.item_code,
       q.domain,
       CASE WHEN random() < 0.10 THEN NULL
            ELSE LEAST(4, GREATEST(0, ROUND(4 * LEAST(1, GREATEST(0,
                v.difficolta + (random() - 0.5) * (0.15 + 0.60 * v.difficolta)
            )))::int))
       END,
       CASE WHEN random() < 0.15 THEN NULL ELSE 7 + (random() * 24)::int END,
       NULL,
       a.created_at + INTERVAL '5 minutes'
FROM _valutazioni v
JOIN assessments a ON a.id = v.id
CROSS JOIN questionnaire_items q
WHERE v.status = 'DRAFT'
  AND q.order_num <= v.partiali
  AND NOT EXISTS (
      SELECT 1 FROM item_responses r
      WHERE r.assessment_id = a.id AND r.item_code = q.item_code
  );

-- ---------------------------------------------------------------------
-- 6. CALCOLO DEI PUNTEGGI (solo valutazioni COMPLETE)
--    Replica di AssessmentService#submitAndScore.
-- ---------------------------------------------------------------------
UPDATE assessments a
SET total_score                = t.raw,
    overall_percent_disability = t.raw / (t.n * 4.0) * 100,
    overall_simple_score       = t.raw,
    overall_complex_score      = t.raw,
    overall_average_score      = t.raw / t.n,
    total_items                = t.n,
    answered_items             = t.answered,
    missing_items              = t.n - t.answered,
    interpretation             = CASE
                                    WHEN t.raw / (t.n * 4.0) * 100 <  5 THEN 'NONE'
                                    WHEN t.raw / (t.n * 4.0) * 100 < 25 THEN 'MILD'
                                    WHEN t.raw / (t.n * 4.0) * 100 < 50 THEN 'MODERATE'
                                    WHEN t.raw / (t.n * 4.0) * 100 < 75 THEN 'SEVERE'
                                    ELSE 'EXTREME'
                                END,
    completed_at               = a.created_at + INTERVAL '30 minutes',
    scored_at                  = a.created_at + INTERVAL '30 minutes',
    updated_at                 = a.created_at + INTERVAL '30 minutes'
FROM (
    SELECT assessment_id,
           COUNT(*)                AS n,
           COUNT(score)            AS answered,
           COALESCE(SUM(score), 0) AS raw
    FROM item_responses
    GROUP BY assessment_id
) t
WHERE a.id = t.assessment_id
  AND a.status = 'COMPLETED';

-- ---------------------------------------------------------------------
-- 7. CERTIFICATES (~780)
--    Emessi su una parte delle valutazioni completate, con i tre stati
--    previsti dal dominio: ISSUED, EXPIRED, REVOKED.
--    Il testo del certificato e' coerente con la regione di nascita del
--    paziente, che e' anche la regione del comitato che lo emette.
-- ---------------------------------------------------------------------
CREATE TEMP TABLE _da_certificare AS
SELECT a.id       AS assessment_id,
       a.patient_id,
       a.overall_percent_disability,
       a.overall_simple_score,
       p.first_name || ' ' || p.last_name AS patient_name,
       co.regione,
       a.created_at + INTERVAL '2 hours'   AS issue_date
FROM assessments a
JOIN patients p ON p.id = a.patient_id::bigint
     CROSS JOIN LATERAL (
         SELECT regione FROM _comuni WHERE nome = p.birth_place LIMIT 1
     ) co
WHERE a.status = 'COMPLETED'
  AND random() < 0.65
  AND co.regione IS NOT NULL;

INSERT INTO certificates
    (certificate_number, assessment_id, patient_id, patient_name, issue_date, valid_until,
     status, overall_percent_disability, overall_complex_score,
     certifying_body, doctor_name, doctor_license, notes, created_at, updated_at)
SELECT 'WHODAS-' || extract(year FROM d.issue_date)::int::text
       || '-' || lpad((row_number() OVER (ORDER BY d.assessment_id))::text, 5, '0'),
       d.assessment_id,
       d.patient_id,
       d.patient_name,
       d.issue_date,
       d.issue_date + INTERVAL '1 year',
       CASE WHEN random() < 0.06 THEN 'REVOKED'
            WHEN d.issue_date < (now() - INTERVAL '300 days') THEN 'EXPIRED'
            ELSE 'ISSUED'
       END,
       d.overall_percent_disability,
       d.overall_simple_score,
       'Comitato di Valutazione Medico Legale - Regione ' || d.regione,
       'Dr. ' || (t.cognomi)[1 + (d.assessment_id % array_length(t.cognomi, 1))] || ' '
              || (t.cognomi)[1 + ((d.assessment_id * 7) % array_length(t.cognomi, 1))],
       'OMCEI-' || lpad((10000 + (random() * 89999))::int::text, 5, '0'),
       NULL,
       d.issue_date,
       d.issue_date
FROM _da_certificare d
CROSS JOIN _tasti t;

-- ---------------------------------------------------------------------
-- 8. CERTIFICATE DOMAIN SCORES - derivati dalle risposte della valutazione
--    collegata, con le etichette prese da questionnaire_items.
-- ---------------------------------------------------------------------
INSERT INTO certificate_domain_scores
    (certificate_id, domain, domain_label, domain_label_it, item_count,
     raw_score, average_score, simple_score, complex_score, percent_disability)
SELECT c.id, r.domain, q.domain_label, q.domain_label_it,
       COUNT(*), SUM(r.score), SUM(r.score)::double precision / COUNT(*),
       SUM(r.score), SUM(r.score),
       SUM(r.score)::double precision / (COUNT(*) * 4) * 100
FROM certificates c
JOIN item_responses     r ON r.assessment_id = c.assessment_id
JOIN questionnaire_items q ON q.item_code     = r.item_code
WHERE r.score IS NOT NULL
GROUP BY c.id, r.domain, q.domain_label, q.domain_label_it;

-- ---------------------------------------------------------------------
-- 9. RIALLINEAMENTO DELLE SEQUENCE IDENTITY
-- ---------------------------------------------------------------------
SELECT setval(pg_get_serial_sequence('patients', 'id'),
              GREATEST(COALESCE((SELECT MAX(id) FROM patients), 1), 1));
SELECT setval(pg_get_serial_sequence('questionnaire_items', 'id'),
              GREATEST(COALESCE((SELECT MAX(id) FROM questionnaire_items), 1), 1));
SELECT setval(pg_get_serial_sequence('assessments', 'id'),
              GREATEST(COALESCE((SELECT MAX(id) FROM assessments), 1), 1));
SELECT setval(pg_get_serial_sequence('item_responses', 'id'),
              GREATEST(COALESCE((SELECT MAX(id) FROM item_responses), 1), 1));
SELECT setval(pg_get_serial_sequence('certificates', 'id'),
              GREATEST(COALESCE((SELECT MAX(id) FROM certificates), 1), 1));
SELECT setval(pg_get_serial_sequence('certificate_domain_scores', 'id'),
              GREATEST(COALESCE((SELECT MAX(id) FROM certificate_domain_scores), 1), 1));

COMMIT;

-- =====================================================================
-- 10. VERIFICA
-- =====================================================================

-- 10a. Conteggio record per tabella
SELECT 'patients'                 AS tabella, COUNT(*) AS record FROM patients
UNION ALL SELECT 'questionnaire_items',       COUNT(*) FROM questionnaire_items
UNION ALL SELECT 'assessments',               COUNT(*) FROM assessments
UNION ALL SELECT 'item_responses',            COUNT(*) FROM item_responses
UNION ALL SELECT 'certificates',              COUNT(*) FROM certificates
UNION ALL SELECT 'certificate_domain_scores', COUNT(*) FROM certificate_domain_scores
ORDER BY 1;

-- 10b. I 36 item devono coprire i 6 domini con i conteggi WHODAS 2.0
SELECT domain, COUNT(*) AS item, MIN(order_num) AS dal, MAX(order_num) AS al
FROM questionnaire_items
GROUP BY domain
ORDER BY MIN(order_num);

-- 10c. I 5 livelli di disabilita' devono essere tutti presenti
SELECT interpretation, COUNT(*) AS valutazioni,
       ROUND(AVG(overall_percent_disability)::numeric, 2) AS media_pct
FROM assessments
WHERE status = 'COMPLETED'
GROUP BY interpretation
ORDER BY 2 DESC;

-- 10d. Coerenza punteggi: valori salvati vs ricalcolati dalle risposte
SELECT COUNT(*) AS valutazioni_incoerenti FROM (
    SELECT a.id
    FROM assessments a
    JOIN item_responses r ON r.assessment_id = a.id
    WHERE a.status = 'COMPLETED'
    GROUP BY a.id, a.total_score, a.overall_simple_score, a.overall_percent_disability,
             a.total_items, a.answered_items, a.missing_items
    HAVING ABS(a.overall_percent_disability
               - SUM(r.score)::double precision / (COUNT(*) * 4) * 100) > 0.0001
        OR a.overall_simple_score <> SUM(r.score)
        OR a.total_score <> SUM(r.score)
        OR a.total_items <> COUNT(*)
        OR a.missing_items <> COUNT(*) - COUNT(r.score)
) incoerenti;

-- 10e. Integrita' referenziale e validita' dei valori
SELECT
    (SELECT COUNT(*) FROM item_responses r
       LEFT JOIN questionnaire_items q ON q.item_code = r.item_code
      WHERE q.item_code IS NULL)                                     AS item_code_invalido,
    (SELECT COUNT(*) FROM item_responses WHERE domain IS NULL)        AS dominio_mancante,
    (SELECT COUNT(*) FROM item_responses WHERE score NOT BETWEEN 0 AND 4) AS score_out_of_range,
    (SELECT COUNT(*) FROM patients
      WHERE fiscal_code IS NULL OR length(fiscal_code) <> 16)         AS cf_non_valido,
    (SELECT COUNT(*) FROM assessments a
      WHERE a.patient_id !~ '^[0-9]+$'
         OR NOT EXISTS (SELECT 1 FROM patients p WHERE p.id::text = a.patient_id))
                                                                    AS paziente_inesistente,
    (SELECT COUNT(*) FROM certificates c
      WHERE NOT EXISTS (SELECT 1 FROM assessments a WHERE a.id = c.assessment_id))
                                                                    AS assessment_inesistente,
    (SELECT COUNT(*) FROM certificate_domain_scores
      WHERE NOT EXISTS (SELECT 1 FROM certificates c WHERE c.id = certificate_id))
                                                                    AS certificato_inesistente;

-- 10f. Stati dei certificati
SELECT status, COUNT(*) AS certificati FROM certificates GROUP BY status ORDER BY 2 DESC;

-- 10g. Valutazioni per stato
SELECT status, COUNT(*) AS valutazioni FROM assessments GROUP BY status ORDER BY 2 DESC;

-- 10h. Controllo indipendente del carattere di controllo del codice fiscale.
--      Struttura del codice a 16 caratteri:
--        1-3 cognome   4-6 nome   7-8 anno   9 mese
--        10-11 giorno  12-15 comune   16 carattere di controllo CIN
--
--      Il CIN viene ricalcolato con un meccanismo diverso rispetto alla
--      generazione (regexp_split_to_table invece di generate_series+substr)
--      perche' il confronto sia davvero indipendente.
--      Il giorno e' confrontato con birth_date e non con il campo gender:
--      il sesso anagrafico codificato nel CF e' quello alla nascita e non
--      coincide per forza con il genere registrato (valore 'Altro').
WITH controllo AS (
    SELECT f.id, f.fiscal_code, f.birth_date,
           chr((65 + ((
               SELECT COALESCE(SUM(
                   CASE WHEN t.ord % 2 = 1 THEN d.v ELSE p.v END), 0)
               FROM regexp_split_to_table(f.fiscal_code, '')
                    WITH ORDINALITY AS t(ch, ord)
               JOIN _cf_dispari d ON d.c = t.ch
               JOIN _cf_pari    p ON p.c = t.ch
               WHERE t.ord <= 15
           ) % 26))::int) AS check_calcolato
    FROM patients f
)
SELECT COUNT(*) FILTER (WHERE substr(fiscal_code, 16, 1) <> check_calcolato)  AS check_errato,
       COUNT(*) FILTER (WHERE length(fiscal_code) <> 16)                      AS lunghezza_errata,
       COUNT(*) FILTER (WHERE substr(fiscal_code, 7, 2) <> substr(extract(year FROM birth_date)::text, 3, 2))
                                                                         AS anno_invalido,
       COUNT(*) FILTER (WHERE substr(fiscal_code, 9, 1)
                            NOT IN ('A','B','C','D','E','H','L','M','P','R','T')) AS mese_invalido,
       COUNT(*) FILTER (WHERE substr(fiscal_code, 10, 2)::int % 40
                            <> extract(day FROM birth_date))                AS giorno_invalido,
       COUNT(*) FILTER (WHERE NOT EXISTS (SELECT 1 FROM _comuni c
                                           WHERE substr(fiscal_code, 12, 4) = c.codice))
                                                                         AS comune_invalido
FROM controllo;

-- 10i. Campione di pazienti con codice fiscale
SELECT id, first_name, last_name, fiscal_code, birth_date, birth_place, gender
FROM patients
ORDER BY id
LIMIT 10;
