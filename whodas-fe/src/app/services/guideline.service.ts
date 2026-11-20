import { Injectable } from '@angular/core';

export interface GuidelineSection {
  id: string;
  title: string;
  icon: string;
  content: string;
  keywords: string[];
}

@Injectable({ providedIn: 'root' })
export class GuidelineService {
  getAllSections(): GuidelineSection[] {
    return [
      {
        id: 'introduzione',
        title: 'Introduzione',
        icon: 'fas fa-info-circle',
        keywords: ['introduzione', 'panoramica', 'sistema', 'whodas'],
        content: `
          <h2>Benvenuto in WHODAS 2.0 Platform</h2>
          <p>WHODAS 2.0 Platform è il sistema web ufficiale per la somministrazione, gestione e certificazione del questionario <strong>WHO Disability Assessment Schedule 2.0 (WHODAS 2.0)</strong>, lo strumento standardizzato dell'Organizzazione Mondiale della Sanità per la valutazione della disabilità e del funzionamento.</p>
          
          <h3>Obiettivi della Piattaforma</h3>
          <ul>
            <li><strong>Somministrazione guidata</strong> del questionario a 36 item in tre modalita:
               <ul>
                 <li><strong>Autosomministrato</strong>: il paziente compila il questionario da solo. Note: non e obbligatorio che sia presente il medico curante.</li>
                 <li><strong>Somministrato da un intervistatore</strong>: un professionista (medico, psicologo, assistente sociale o altro operatore formato) legge le domande e registra le risposte del paziente. Note: le risposte devono comunque provenire dal paziente, eventualmente con l'aiuto di un operatore che spiega le domande.</li>
                 <li><strong>Proxy</strong>: un familiare, caregiver o altra persona che assiste il paziente risponde al posto suo quando questo non e in grado di farlo adeguatamente. Note: se il paziente ha difficolta cognitive, comunicative o di altro tipo, puo essere utilizzata la compilazione tramite proxy (familiare/caregiver).</li>
               </ul>
             </li>
            <li><strong>Calcolo automatico</strong> dei punteggi per i 6 domini funzionali (Cognizione, Mobilità, Cura di sé, Relazioni, Attività della vita, Partecipazione)</li>
            <li><strong>Interpretazione standardizzata</strong>: NONE, MILD, MODERATE, SEVERE, EXTREME</li>
            <li><strong>Generazione certificati PDF</strong> ufficiali con dati anagrafici, punteggi e firma digitale</li>
            <li><strong>Gestione completa pazienti</strong>: anagrafica, storico valutazioni, documenti</li>
            <li><strong>Single Sign-On aziendale</strong> via Keycloak con federazione LDAP</li>
            <li><strong>Notifiche real-time</strong> per eventi di sistema e scadenze</li>
            <li><strong>Import/Export documenti</strong> con OCR per digitalizzazione cartaceo</li>
          </ul>
          
          <h3>Accesso al Sistema</h3>
          <p>L'accesso avviene tramite credenziali aziendali (username/password) gestite da Keycloak con federazione LDAP. Al primo accesso, se non si possiedono credenziali, è possibile richiedere la registrazione tramite il pulsante <strong>"Nuova registrazione"</strong> nella pagina di login.</p>
          
          <div class="alert alert-info">
            <strong>Nota:</strong> La piattaforma è ottimizzata per browser moderni (Chrome, Firefox, Edge, Safari). JavaScript deve essere abilitato.
          </div>
        `
      },
      {
        id: 'login',
        title: 'Accesso (Login)',
        icon: 'fas fa-sign-in-alt',
        keywords: ['login', 'accesso', 'autenticazione', 'credenziali', 'password'],
        content: `
          <h2>Accesso al Sistema</h2>
          <p>La pagina di login è il punto di ingresso principale. Presenta un form con due campi:</p>
          
          <h3>Campi Richiesti</h3>
          <ul>
            <li><strong>Username</strong>: il tuo identificativo aziendale (es. mario.rossi)</li>
            <li><strong>Password</strong>: la tua password aziendale</li>
          </ul>
          
          <h3>Funzionalità Aggiuntive</h3>
          <ul>
            <li><strong>Password dimenticata?</strong>: link per avviare la procedura di reset password via email</li>
            <li><strong>Nuova registrazione</strong>: pulsante per richiedere un nuovo account (vedi sezione <a href="#" onclick="return false;" data-section="registrazione">Registrazione</a>)</li>
            <li><strong>Messaggio di successo</strong>: dopo la registrazione, appare un banner verde confermante l'avvenuta registrazione</li>
          </ul>
          
          <h3>Errori Comuni</h3>
          <table>
            <thead><tr><th>Errore</th><th>Causa</th><th>Soluzione</th></tr></thead>
            <tbody>
              <tr><td>Credenziali non valide</td><td>Username o password errati</td><td>Verificare maiuscole/minuscole, riprovare</td></tr>
              <tr><td>Account bloccato</td><td>Troppi tentativi falliti</td><td>Attendere 15 min o contattare admin</td></tr>
              <tr><td>Sessione scaduta</td><td>Token JWT scaduto (1 ora)</td><td>Rifare login</td></tr>
            </tbody>
          </table>
          
          <div class="alert alert-warning">
            <strong>Sicurezza:</strong> Non condividere mai le tue credenziali. La sessione scade automaticamente dopo 1 ora di inattività.
          </div>
        `
      },
      {
        id: 'registrazione',
        title: 'Registrazione Nuovo Utente',
        icon: 'fas fa-user-plus',
        keywords: ['registrazione', 'nuovo utente', 'iscrizione', 'account', 'ldap', 'keycloak'],
        content: `
          <h2>Registrazione Self-Service</h2>
          <p>Gli utenti senza credenziali possono richiedere un account tramite il pulsante <strong>"Nuova registrazione"</strong> nella pagina di login. Il processo è completamente automatizzato e richiede l'inserimento di 12 campi.</p>
          
          <h3>Campi del Form di Registrazione</h3>
          <table>
            <thead><tr><th>Campo</th><th>Tipo</th><th>Obbligatorio</th><th>Note</th></tr></thead>
            <tbody>
              <tr><td>Username</td><td>Testo</td><td>Sì</td><td>Identificativo univoco per login</td></tr>
              <tr><td>Password</td><td>Password</td><td>Sì</td><td>Minimo 8 caratteri, conferma richiesta</td></tr>
              <tr><td>Nome</td><td>Testo</td><td>Sì</td><td>Nome di battesimo</td></tr>
              <tr><td>Cognome</td><td>Testo</td><td>Sì</td><td>Cognome</td></tr>
              <tr><td>Data di nascita</td><td>Data</td><td>Sì</td><td>Formato GG/MM/AAAA</td></tr>
              <tr><td>Codice Fiscale</td><td>Testo</td><td>Sì</td><td>16 caratteri, validazione italiana, <strong>univoco</strong></td></tr>
              <tr><td>Indirizzo</td><td>Testo</td><td>Sì</td><td>Via, numero civico</td></tr>
              <tr><td>CAP</td><td>Testo</td><td>Sì</td><td>5 cifre</td></tr>
              <tr><td>Comune</td><td>Testo</td><td>Sì</td><td>Comune di residenza</td></tr>
              <tr><td>Città/Provincia</td><td>Testo</td><td>Sì</td><td>Città o provincia</td></tr>
              <tr><td>Telefono</td><td>Tel</td><td>Sì</td><td>Numero di contatto</td></tr>
              <tr><td>Email</td><td>Email</td><td>Sì</td><td>Per notifiche e reset password, <strong>univoca</strong></td></tr>
            </tbody>
          </table>
          
          <h3>Validazione in Tempo Reale</h3>
          <p>Quando il cursore esce dai campi <strong>Codice Fiscale</strong> ed <strong>Email</strong> (evento <em>onBlur</em>), il sistema verifica automaticamente l'unicità:</p>
          <ul>
            <li>Se <strong>già esistente</strong>: appare un modal di avviso giallo</li>
            <li>Se <strong>disponibile</strong>: nessun messaggio, procedere normalmente</li>
          </ul>
          
          <h3>Processo Backend (Automatico)</h3>
          <ol>
            <li>Validazione dati e unicità (CF, Email, Username)</li>
            <li>Creazione utente in <strong>OpenLDAP</strong> sotto <code>ou=users,dc=whodascompany,dc=local</code></li>
            <li>Sincronizzazione <strong>Keycloak</strong> (full sync provider LDAP)</li>
            <li>Assegnazione ruolo <code>user</code> in Keycloak</li>
            <li>Salvataggio audit in tabella <code>registered_users</code> (status ATTIVO)</li>
            <li>Rollback automatico se Keycloak sync fallisce (cancellazione utente LDAP)</li>
          </ol>
          
          <h3>Dopo la Registrazione</h3>
          <ul>
            <li>Login automatico e redirect alla <strong>Dashboard</strong></li>
            <li>Email di conferma inviata all'indirizzo registrato</li>
            <li>Account immediatamente utilizzabile</li>
          </ul>
          
          <div class="alert alert-success">
            <strong>Completato!</strong> L'utente è provvisto in LDAP, Keycloak e DB in un'unica transazione atomica.
          </div>
        `
      },
      {
        id: 'dashboard',
        title: 'Dashboard',
        icon: 'fas fa-tachometer-alt',
        keywords: ['dashboard', 'home', 'panoramica', 'statistiche'],
        content: `
          <h2>Dashboard Principale</h2>
          <p>Dopo il login, l'utente atterra sulla Dashboard, che fornisce una panoramica sintetica dello stato del sistema e accessi rapidi alle funzioni principali.</p>
          
          <h3>Widget Disponibili</h3>
          <ul>
            <li><strong>Statistiche Pazienti</strong>: totale, nuovi questo mese, in carico</li>
            <li><strong>Assessment Recenti</strong>: ultimi 5 assessment creati/completati</li>
            <li><strong>Certificati in Scadenza</strong>: alert per certificati prossimi alla scadenza</li>
            <li><strong>Notifiche Non Lette</strong>: badge con conteggio (vedi sezione <a href="#" onclick="return false;" data-section="notifiche">Notifiche</a>)</li>
            <li><strong>Azioni Rapide</strong>: bottoni per "Nuovo Paziente", "Nuovo Assessment", "Upload Documento"</li>
          </ul>
          
          <h3>Navigazione</h3>
          <p>Dal menu laterale o dalla navbar superiore è possibile accedere a tutte le sezioni. La Dashboard è il punto di ritorno costante (logo WHODAS 2.0 in alto a sinistra).</p>
        `
      },
      {
        id: 'pazienti',
        title: 'Gestione Pazienti',
        icon: 'fas fa-users',
        keywords: ['pazienti', 'anagrafica', 'codice fiscale', 'ricerca', 'crud'],
        content: `
          <h2>Gestione Pazienti</h2>
          <p>Sezione dedicata all'anagrafica dei pazienti e alla gestione dello storico clinico.</p>
          
          <h3>Elenco Pazienti (<code>/patients</code>)</h3>
          <ul>
            <li>Tabella paginata, ordinabile per colonna</li>
            <li>Ricerca full-text per: Cognome, Codice Fiscale, Nome</li>
            <li>Azioni per riga: <strong>Visualizza</strong>, <strong>Modifica</strong>, <strong>Nuovo Assessment</strong>, <strong>Documenti</strong>, <strong>Certificati</strong></li>
            <li>Badge stato: <span class="badge bg-success">Attivo</span> / <span class="badge bg-secondary">Archiviato</span></li>
          </ul>
          
          <h3>Nuovo Paziente (<code>/patients/new</code>)</h3>
          <table>
            <thead><tr><th>Campo</th><th>Obbligatorio</th><th>Validazione</th></tr></thead>
            <tbody>
              <tr><td>Nome</td><td>Sì</td><td>Solo lettere, max 100 char</td></tr>
              <tr><td>Cognome</td><td>Sì</td><td>Solo lettere, max 100 char</td></tr>
              <tr><td>Codice Fiscale</td><td>Sì</td><td>16 char, algoritmo CIN italiano, <strong>univoco</strong></td></tr>
              <tr><td>Data Nascita</td><td>Sì</td><td>Data picker, non futura</td></tr>
              <tr><td>Luogo Nascita</td><td>No</td><td>Testo libero</td></tr>
              <tr><td>Sesso</td><td>Sì</td><td>M/F</td></tr>
              <tr><td>Indirizzo</td><td>No</td><td>Testo libero</td></tr>
              <tr><td>Telefono</td><td>No</td><td>Formato italiano</td></tr>
              <tr><td>Email</td><td>No</td><td>Formato email valido</td></tr>
              <tr><td>Condizione Sanitaria</td><td>No</td><td>Testo libero (note cliniche)</td></tr>
              <tr><td>Note</td><td>No</td><td>Testo libero</td></tr>
            </tbody>
          </table>
          
          <h3>Modifica Paziente (<code>/patients/edit/:id</code>)</h3>
          <p>Stesso form del nuovo paziente, pre-compilato. Il Codice Fiscale non è modificabile se esistono assessment collegati.</p>
          
          <div class="alert alert-info">
            <strong>Integrità:</strong> Il sistema impedisce la cancellazione di pazienti con assessment, certificati o documenti associati.
          </div>
        `
      },
      {
        id: 'questionario',
        title: 'Questionario e Assessment',
        icon: 'fas fa-clipboard-list',
        keywords: ['questionario', 'assessment', 'whodas', '36 item', 'domini', 'punteggio', 'compilazione'],
        content: `
          <h2>Questionario WHODAS 2.0 e Assessment</h2>
          <p>Cuore della piattaforma: somministrazione del questionario standardizzato a 36 item suddivisi in 6 domini funzionali.</p>
          
          <h3>Selezione Paziente e Tipo Compilazione (<code>/questionnaire</code>)</h3>
          <ul>
            <li>Ricerca paziente per CF o cognome</li>
            <li>Selezione <strong>Tipo Compilazione</strong>:
              <ul>
                <li><strong>Autosomministrato</strong>: il paziente compila autonomamente</li>
                <li><strong>Somministrato da intervistatore</strong>: operatore guida la compilazione</li>
                <li><strong>Proxy</strong>: familiare/caregiver compila per il paziente</li>
              </ul>
            </li>
            <li>Selezione lingua (italiano default)</li>
          </ul>
          
          <h3>Runner Questionario (<code>/assessment/run/:patientId</code>)</h3>
          <ul>
            <li>Navigazione step-by-step (1 item per schermata)</li>
            <li>Barra progresso visiva (1-36)</li>
            <li>Scala Likert 0-4 per ogni item:
              <ul>
                <li>0 = Nessuna difficoltà</li>
                <li>1 = Leggera</li>
                <li>2 = Moderata</li>
                <li>3 = Grave</li>
                <li>4 = Estrema / Impossibile</li>
              </ul>
            </li>
            <li>Campo "Giorni" opzionale per frequenza</li>
            <li>Note libere per item</li>
            <li>Salvataggio automatico bozza (status DRAFT)</li>
            <li>Navigazione avanti/indietro, salto a item specifico</li>
          </ul>
          
          <h3>6 Domini WHODAS 2.0</h3>
          <table>
            <thead><tr><th>Dominio</th><th>Item</th><th>Descrizione</th></tr></thead>
            <tbody>
              <tr><td>Cognizione (D1)</td><td>6</td><td>Concentrazione, memoria, risoluzione problemi, apprendimento</td></tr>
              <tr><td>Mobilità (D2)</td><td>5</td><td>Spostarsi, stare in piedi, camminare, salire scale</td></tr>
              <tr><td>Cura di sé (D3)</td><td>4</td><td>Lavarsi, vestirsi, mangiare, igiene personale</td></tr>
              <tr><td>Relazioni (D4)</td><td>5</td><td>Interazioni sociali, relazioni familiari/amichevoli</td></tr>
              <tr><td>Attività vita (D5)</td><td>8</td><td>Lavoro, studio, faccende domestiche, ruolo sociale</td></tr>
              <tr><td>Partecipazione (D6)</td><td>8</td><td>Partecipazione comunitaria, barriere ambientali, discriminazione</td></tr>
            </tbody>
          </table>
          
          <h3>Calcolo Punteggi e Interpretazione</h3>
          <ul>
            <li>Punteggio grezzo per dominio: somma item (0-4) → scala 0-100</li>
            <li>Punteggio globale: media pesata 6 domini</li>
            <li>Interpretazione automatica:
              <ul>
                <li>0-4% = <span class="badge bg-success">NONE</span> (nessuna disabilità)</li>
                <li>5-24% = <span class="badge bg-info">MILD</span> (lieve)</li>
                <li>25-49% = <span class="badge bg-warning">MODERATE</span> (moderata)</li>
                <li>50-74% = <span class="badge bg-orange">SEVERE</span> (grave)</li>
                <li>75-100% = <span class="badge bg-danger">EXTREME</span> (estrema)</li>
              </ul>
            </li>
          </ul>
          
          <h3>Stati Assessment</h3>
          <ul>
            <li><span class="badge bg-secondary">DRAFT</span>: in compilazione, modificabile</li>
            <li><span class="badge bg-success">COMPLETED</span>: finalizzato, genera certificato</li>
          </ul>
          
          <div class="alert alert-success">
            <strong>Completamento:</strong> Al termine, l'assessment passa a COMPLETED e attiva la generazione automatica del certificato PDF.
          </div>
        `
      },
      {
        id: 'documenti',
        title: 'Gestione Documenti',
        icon: 'fas fa-file-lines',
        keywords: ['documenti', 'upload', 'download', 'ocr', 'import', 'export', 'storico'],
        content: `
          <h2>Gestione Documenti</h2>
          <p>Sistema completo per import, export, OCR e archiviazione documenti clinici e amministrativi.</p>
          
          <h3>Upload Documenti (<code>/documents</code>)</h3>
          <ul>
            <li>Drag & drop o click per selezione file</li>
            <li>Formati supportati: <strong>PDF, DOC/DOCX, XLS/XLSX, JPG, PNG, TIFF</strong></li>
            <li>Dimensione max: 50 MB per file</li>
            <li>Associazione obbligatoria a Paziente e (opzionale) Assessment</li>
            <li>Metadati automatici: checksum SHA-256, data upload, utente</li>
          </ul>
          
          <h3>OCR e Estrazione Dati</h3>
          <ul>
            <li>Motore: <strong>Tesseract (Tess4J)</strong> per immagini e PDF scannerizzati</li>
            <li>Estrazione automatica: dati anagrafici, codice fiscale, date, punteggi WHODAS</li>
            <li>Campi estratti con <strong>confidence score</strong> (0-100%)</li>
            <li>Revisione manuale prima della conferma</li>
            <li>Creazione automatica paziente/assessment se dati completi</li>
          </ul>
          
          <h3>Download Documenti (<code>/documents/download</code>)</h3>
          <ul>
            <li>Filtro per paziente, tipo, data, stato</li>
            <li>Download singolo o multiplo (ZIP)</li>
            <li>Log download per audit trail</li>
          </ul>
          
          <h3>Storico Import/Export (<code>/documents/history</code>)</h3>
          <ul>
            <li>Tabella completa con: filename, paziente, assessment, metodo estrazione, stato, data</li>
            <li>Stati: <span class="badge bg-primary">PENDING</span> <span class="badge bg-success">COMPLETED</span> <span class="badge bg-danger">FAILED</span> <span class="badge bg-warning">REVIEW</span></li>
            <li>Dettaglio: testo estratto, campi riconosciuti, item WHODAS identificati</li>
            <li>Rielaborazione OCR per documenti falliti</li>
          </ul>
          
          <div class="alert alert-info">
            <strong>Privacy:</strong> Tutti i documenti sono memorizzati criptati (BYTEA PostgreSQL) e accessibili solo utenti autorizzati.
          </div>
        `
      },
      {
        id: 'certificati',
        title: 'Certificati',
        icon: 'fas fa-certificate',
        keywords: ['certificati', 'pdf', 'generazione', 'firma', 'stampa', 'validità'],
        content: `
          <h2>Gestione Certificati</h2>
          <p>Generazione, visualizzazione e gestione dei certificati WHODAS 2.0 ufficiali in formato PDF.</p>
          
          <h3>Generazione Automatica</h3>
          <ul>
            <li>Attivata al completamento assessment (status COMPLETED)</li>
            <li>Motore: <strong>iText 8</strong> con template AcroForm</li>
            <li>Numerazione progressiva univoca per anno</li>
            <li>Validità standard: 12 mesi dalla data emissione</li>
          </ul>
          
          <h3>Contenuto Certificato</h3>
          <ul>
            <li>Intestazione ufficiale WHODAS 2.0</li>
            <li>Dati anagrafici paziente (nome, cognome, CF, data nascita)</li>
            <li>Tipo compilazione, lingua, data assessment, intervistatore (se applicabile)</li>
            <li>Punteggi per i 6 domini (0-100) + punteggio globale</li>
            <li>Interpretazione: NONE / MILD / MODERATE / SEVERE / EXTREME</li>
            <li>Ente certificatore, medico firmatario, timbro digitale</li>
            <li>QR code per verifica autenticità (opzionale)</li>
          </ul>
          
          <h3>Gestione (<code>/certificates</code>)</h3>
          <ul>
            <li>Elenco certificati con filtri: paziente, stato, data emissione, scadenza</li>
            <li>Stati: <span class="badge bg-success">VALIDO</span> <span class="badge bg-warning">IN SCADENZA</span> (<30gg) <span class="badge bg-danger">SCADUTO</span> <span class="badge bg-secondary">REVOCATO</span></li>
            <li>Azioni: <strong>Visualizza PDF</strong>, <strong>Download</strong>, <strong>Stampa</strong>, <strong>Revoke</strong> (solo admin)</li>
            <li>Rigenerazione certificato se dati assessment modificati</li>
          </ul>
          
          <h3>Notifiche Automatiche</h3>
          <ul>
            <li>Email al paziente/medico all'emissione</li>
            <li>Alert 30/15/7 giorni prima scadenza</li>
            <li>Notifica in-app (badge navbar) per certificati in scadenza</li>
          </ul>
          
          <div class="alert alert-success">
            <strong>Standard:</strong> I certificati sono conformi alle specifiche WHO per WHODAS 2.0 e legalmente validi per scopi amministrativi/sanitari.
          </div>
        `
      },
      {
        id: 'notifiche',
        title: 'Notifiche',
        icon: 'fas fa-bell',
        keywords: ['notifiche', 'campanella', 'badge', 'sse', 'real-time', 'email', 'push'],
        content: `
          <h2>Sistema Notifiche</h2>
          <p>Notifiche real-time via Server-Sent Events (SSE) e email per eventi di sistema, scadenze e azioni utente.</p>
          
          <h3>Navbar - Campanella Notifiche</h3>
          <ul>
            <li>Icona <i class="fas fa-bell"></i> in alto a destra (sempre visibile se autenticato)</li>
            <li>Badge rosso con <strong>conteggio non lette</strong> (aggiornato real-time)</li>
            <li>Click → Dropdown panel con ultime 5 notifiche non lette</li>
            <li>Azioni nel dropdown:
              <ul>
                <li>Marcare singola come letta</li>
                <li>Eliminare singola</li>
                <li><strong>Marcare tutte come lette</strong></li>
                <li>Link <strong>"Gestisci"</strong> → pagina completa <code>/notifications</code></li>
              </ul>
            </li>
          </ul>
          
          <h3>Pagina Notifiche Completa (<code>/notifications</code>)</h3>
          <ul>
            <li>Tabella paginata con filtri: tipo, stato, data range</li>
            <li>Stati: <span class="badge bg-primary">SPEDITO</span> <span class="badge bg-success">LETTO</span> <span class="badge bg-warning">MODIFICATO</span> <span class="badge bg-danger">CANCELLATO</span></li>
            <li>Azioni bulk: marca tutte lette, elimina selezionate</li>
            <li>Dettaglio click: modal con contenuto completo</li>
          </ul>
          
          <h3>Tipi di Notifica</h3>
          <table>
            <thead><tr><th>Tipo</th><th>Trigger</th><th>Canale</th></tr></thead>
            <tbody>
              <tr><td>Assessment completato</td><td>Assessment → COMPLETED</td><td>In-app + Email</td></tr>
              <tr><td>Certificato generato</td><td>Certificato creato</td><td>In-app + Email</td></tr>
              <tr><td>Certificato in scadenza</td><td>30/15/7 giorni prima</td><td>In-app + Email</td></tr>
              <tr><td>Nuovo paziente assegnato</td><td>Admin assegna paziente</td><td>In-app</td></tr>
              <tr><td>Documento elaborato</td><td>OCR completato</td><td>In-app</td></tr>
              <tr><td>Sistema</td><td>Manutenzione, aggiornamenti</td><td>In-app + Email</td></tr>
            </tbody>
          </table>
          
          <h3>Tecnologia SSE</h3>
          <ul>
            <li>Connessione persistente HTTP/1.1 (endpoint <code>/api/v1/notifications/stream</code>)</li>
            <li>Auto-riconnesione su disconnessione</li>
            <li>Chiusura automatica su logout o navigazione pagina login</li>
            <li>Header Authorization: Basic base64(username:token) per autenticazione</li>
          </ul>
        `
      },
      {
        id: 'utenti',
        title: 'Gestione Utenti (Admin)',
        icon: 'fas fa-users-cog',
        keywords: ['utenti', 'admin', 'gestione', 'ruoli', 'abilitazione', 'disabilitazione'],
        content: `
          <h2>Gestione Utenti (Solo Amministratori)</h2>
          <p>Sezione riservata agli amministratori per la gestione degli account utente del sistema.</p>
          
          <h3>Funzionalità</h3>
          <ul>
            <li>Elenco utenti con paginazione e ricerca (username, email, nome, cognome)</li>
            <li>Stato account: <span class="badge bg-success">ATTIVO</span> <span class="badge bg-warning">SCADUTO</span> <span class="badge bg-danger">CANCELLATO</span></li>
            <li>Sorgente: <span class="badge bg-info">LDAP</span> (federati) / <span class="badge bg-primary">LOCAL</span> (registrati self-service)</li>
            <li>Azioni: <strong>Visualizza dettagli</strong>, <strong>Disabilita/Abilita</strong>, <strong>Reset Password</strong>, <strong>Elimina (soft)</strong></li>
            <li>Visualizzazione ruoli Keycloak assegnati</li>
            <li>Link diretto a Keycloak Admin Console per gestione avanzata</li>
          </ul>
          
          <h3>Reset Password Admin</h3>
          <ul>
            <li>Genera token temporaneo (validità 24h)</li>
            <li>Invia email all'utente con link <code>/reset-password?token=...</code></li>
            <li>Utente imposta nuova password (policy: min 8 char, maiuscola, numero, speciale)</li>
          </ul>
          
          <div class="alert alert-warning">
            <strong>Attenzione:</strong> La disabilitazione utente in Keycloak revoca immediatamente tutti i token attivi. L'utente viene scollegato alla prossima richiesta.
          </div>
        `
      },
      {
        id: 'password',
        title: 'Recupero e Reset Password',
        icon: 'fas fa-key',
        keywords: ['password', 'recupero', 'reset', 'dimenticata', 'token', 'email'],
        content: `
          <h2>Recupero e Reset Password</h2>
          <p>Procedura self-service per utenti che hanno dimenticato la password.</p>
          
          <h3>Flusso "Password Dimenticata"</h3>
          <ol>
            <li>Pagina login → click <strong>"Password dimenticata?"</strong></li>
            <li>Inserimento <strong>username</strong> o <strong>email</strong> registrata</li>
            <li>Sistema verifica esistenza e invia email con link univoco</li>
            <li>Link format: <code>/reset-password?token=XYZ123</code> (validità 24 ore)</li>
            <li>Pagina reset: inserimento <strong>nuova password</strong> (2x conferma)</li>
            <li>Validazione policy: min 8 char, 1 maiuscola, 1 numero, 1 speciale</li>
            <li>Conferma → login automatico con nuove credenziali</li>
          </ol>
          
          <h3>Token Reset Password</h3>
          <ul>
            <li>Memorizzato in tabella <code>password_reset_tokens</code> (hash, non plaintext)</li>
            <li>Single-use: invalidato al primo utilizzo</li>
            <li>Scadenza automatica: 24 ore dalla generazione</li>
            <li>Rate limiting: max 3 richieste/ora per email</li>
          </ul>
          
          <h3>Cambio Password da Loggato</h3>
          <p>Non ancora implementato in UI. Attualmente solo tramite admin o flusso "dimenticata". Previsto in roadmap v1.1.</p>
        `
      },
      {
        id: 'sicurezza',
        title: 'Sicurezza e Privacy',
        icon: 'fas fa-shield-alt',
        keywords: ['sicurezza', 'privacy', 'gdpr', 'crittografia', 'token', 'sessione', 'logout'],
        content: `
          <h2>Sicurezza e Privacy</h2>
          
          <h3>Autenticazione</h3>
          <ul>
            <li><strong>Keycloak</strong> Identity Provider (OIDC/SAML 2.0)</li>
            <li>Federazione <strong>LDAP</strong> per utenti aziendali</li>
            <li>Token <strong>JWT RS256</strong> (access: 1h, refresh: 24h)</li>
            <li>Introspection token ad ogni richiesta via API Gateway</li>
            <li>Header auth: <code>Authorization: Basic base64(username:access_token)</code></li>
          </ul>
          
          <h3>Autorizzazione</h3>
          <ul>
            <li>Ruolo base: <code>user</code> (tutti gli utenti)</li>
            <li>Ruoli admin gestiti in Keycloak (realm roles)</li>
            <li>Frontend: route guard per autenticazione</li>
            <li>Backend: validazione token + ruoli (da implementare fully)</li>
          </ul>
          
          <h3>Protezione Dati</h3>
          <ul>
            <li>HTTPS obbligatorio in produzione (Let's Encrypt + nginx reverse proxy)</li>
            <li>Database: password hashate (BCrypt), token hashati</li>
            <li>Documenti: memorizzati come BYTEA criptati in PostgreSQL</li>
            <li>Comunicazione inter-servizi: rete Docker isolata</li>
            <li>Secrets: variabili d'ambiente (file <code>.env</code> non in repo)</li>
          </ul>
          
          <h3>GDPR Compliance</h3>
          <ul>
            <li>Diritto accesso: export dati utente (JSON/PDF)</li>
            <li>Diritto rettifica: modifica profilo utente</li>
            <li>Diritto cancellazione: soft delete (status CANCELLATO), anonimizzazione</li>
            <li>Diritto portabilità: export assessment/certificati</li>
            <li>Data retention: configurabile per tipo dato (default 10 anni clinici)</li>
            <li>Log accessi: audit trail su operazioni sensibili</li>
          </ul>
          
          <h3>Best Practices Utente</h3>
          <ul>
            <li>Usa password uniche e complesse</li>
            <li>Non condividere credenziali</li>
            <li>Logout esplicito su postazioni condivise</li>
            <li>Segnala accessi sospetti all'admin</li>
            <li>Aggiorna email/telefono per notifiche</li>
          </ul>
        `
      },
      {
        id: 'risoluzione-problemi',
        title: 'Risoluzione Problemi Comuni',
        icon: 'fas fa-tools',
        keywords: ['problemi', 'errori', 'troubleshooting', 'faq', 'assistenza', 'supporto'],
        content: `
          <h2>Risoluzione Problemi Comuni (FAQ)</h2>
          
          <h3>Accesso e Login</h3>
          <table>
            <thead><tr><th>Problema</th><th>Soluzione</th></tr></thead>
            <tbody>
              <tr><td>"Credenziali non valide" ma sono corrette</td><td>Verifica Caps Lock, prova "Password dimenticata", contatta admin se account bloccato</td></tr>
              <tr><td>Redirect continuo a /login</td><td>Cookie/cache browser: pulisci cache, prova incognito, verifica orario sistema</td></tr>
              <tr><td>Token scaduto durante lavoro</td><td>Normale dopo 1h inattività: rifai login, i dati bozza sono salvati (DRAFT)</td></tr>
            </tbody>
          </table>
          
          <h3>Questionario e Assessment</h3>
          <table>
            <thead><tr><th>Problema</th><th>Soluzione</th></tr></thead>
            <tbody>
              <tr><td>Item non salvati</td><td>Verifica connessione, il salvataggio è automatico ogni cambio item; ricarica pagina</td></tr>
              <tr><td>Punteggio non calcolato</td><td>Completa tutti 36 item obbligatori; controlla campi "Giorni" se richiesti</td></tr>
              <tr><td>Assessment bloccato in DRAFT</td><td>Verifica item mancanti (barra progresso), click "Completa" in fondo</td></tr>
            </tbody>
          </table>
          
          <h3>Documenti e OCR</h3>
          <table>
            <thead><tr><th>Problema</th><th>Soluzione</th></tr></thead>
            <tbody>
              <tr><td>OCR non estrae testo</td><td>Verifica qualità scansione (min 300 DPI), formato supportato, riprova con PDF nativo</td></tr>
              <tr><td>Upload fallito</td><td>File > 50MB? Comprimi. Formato non supportato? Converti in PDF.</td></tr>
            </tbody>
          </table>
          
          <h3>Certificati</h3>
          <table>
            <thead><tr><th>Problema</th><th>Soluzione</th></tr></thead>
            <tbody>
              <tr><td>Certificato non generato</td><td>Assessment deve essere COMPLETED (non DRAFT). Controlla stato.</td></tr>
              <tr><td>Dati errati su certificato</td><td>Modifica assessment → ricompleta → rigenera certificato</td></tr>
            </tbody>
          </table>
          
          <h3>Notifiche</h3>
          <table>
            <thead><tr><th>Problema</th><th>Soluzione</th></tr></thead>
            <tbody>
              <tr><td>Badge non aggiornato</td><td>Riclicca campanella, o ricarica pagina (SSE si riconnette)</td></tr>
              <tr><td>Email non ricevute</td><td>Controlla spam, verifica email profilo, contatta admin per configurazione SMTP</td></tr>
            </tbody>
          </table>
          
          <h3>Contatto Supporto</h3>
          <ul>
            <li>Email: <code>support@whodas.local</code></li>
            <li>Includi: username, browser, ora, screenshot errore, passi per riprodurre</li>
            <li>Log applicazione: <code>/logs</code> (solo admin)</li>
          </ul>
        `
      },
      {
        id: 'scorciatoie',
        title: 'Scorciatoie da Tastiera',
        icon: 'fas fa-keyboard',
        keywords: ['scorciatoie', 'tastiera', 'shortcut', 'hotkey', 'accessibilità'],
        content: `
          <h2>Scorciatoie da Tastiera</h2>
          <p>La piattaforma supporta navigazione da tastiera per accessibilità ed efficienza.</p>
          
          <h3>Globali</h3>
          <table>
            <thead><tr><th>Combinazione</th><th>Azione</th></tr></thead>
            <tbody>
              <tr><td><kbd>Alt</kbd> + <kbd>H</kbd></td><td>Home / Dashboard</td></tr>
              <tr><td><kbd>Alt</kbd> + <kbd>P</kbd></td><td>Pazienti</td></tr>
              <tr><td><kbd>Alt</kbd> + <kbd>Q</kbd></td><td>Questionario</td></tr>
              <tr><td><kbd>Alt</kbd> + <kbd>D</kbd></td><td>Documenti</td></tr>
              <tr><td><kbd>Alt</kbd> + <kbd>C</kbd></td><td>Certificati</td></tr>
              <tr><td><kbd>Alt</kbd> + <kbd>N</kbd></td><td>Notifiche (focus campanella)</td></tr>
              <tr><td><kbd>Alt</kbd> + <kbd>U</kbd></td><td>Utenti (se admin)</td></tr>
              <tr><td><kbd>Alt</kbd> + <kbd>L</kbd></td><td>Logout</td></tr>
              <tr><td><kbd>Alt</kbd> + <kbd>M</kbd></td><td>Apri Manuale (questo modal)</td></tr>
              <tr><td><kbd>Esc</kbd></td><td>Chiudi modal/dropdown aperti</td></tr>
            </tbody>
          </table>
          
          <h3>Questionario Runner</h3>
          <table>
            <thead><tr><th>Combinazione</th><th>Azione</th></tr></thead>
            <tbody>
              <tr><td><kbd>→</kbd> / <kbd>Enter</kbd></td><td>Item successivo</td></tr>
              <tr><td><kbd>←</kbd></td><td>Item precedente</td></tr>
              <tr><td><kbd>0-4</kbd></td><td>Seleziona punteggio Likert</td></tr>
              <tr><td><kbd>Alt</kbd> + <kbd>S</kbd></td><td>Salva bozza</td></tr>
              <tr><td><kbd>Alt</kbd> + <kbd>F</kbd></td><td>Finalizza assessment</td></tr>
              <tr><td><kbd>G</kbd></td><td>Focus campo "Giorni"</td></tr>
              <tr><td><kbd>N</kbd></td><td>Focus campo "Note"</td></tr>
            </tbody>
          </table>
          
          <h3>Form Generici</h3>
          <table>
            <thead><tr><th>Combinazione</th><th>Azione</th></tr></thead>
            <tbody>
              <tr><td><kbd>Tab</kbd></td><td>Campo successivo</td></tr>
              <tr><td><kbd>Shift</kbd> + <kbd>Tab</kbd></td><td>Campo precedente</td></tr>
              <tr><td><kbd>Enter</kbd></td><td>Submit form (se focus su button)</td></tr>
              <tr><td><kbd>Esc</kbd></td><td>Annulla/Chiudi</td></tr>
            </tbody>
          </table>
          
          <div class="alert alert-info">
            <strong>Accessibilità:</strong> Tutti i componenti supportano screen reader (ARIA labels), focus visible, contrasto WCAG AA.
          </div>
        `
      }
    ];
  }

  getSectionById(id: string): GuidelineSection | undefined {
    return this.getAllSections().find(s => s.id === id);
  }
}