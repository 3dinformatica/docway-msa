# Change Log


## [3.0.5] - 2019-02-25

### Added
- Prima implementazione del writer dell'audit su LOG anziché su MongoDB (__da verificare__)
- Possibilità di istanziare il MailReader specificando le property mail.mime.address.strict (default = true) per evitare la validazione dell'header di javamail
- Migliorata la gestione del recupero dei nomi di file allegati ai messaggi (gestione di eventuali problemi di encoding nel nome dei file)

### Changed
- Aggiornata la dipendenza del broker per supporto a numero di documenti superiore a 16 milioni.

### Fixed
- Corretta la scrittura di log di errore in caso di mancato parsing di un messaggio email (registrazione sul log dell'applicativo dell'errore riscontrato in fase di parsing)
- Registrazione su Audit di messaggi sui quali è fallito il parsing iniziale (fino alla versione precedente venivano ignorati a livello di audit)


## [3.0.4] - 2019-01-28

### Added
- Aggiunto store type SAVE\_ORPHAN\_PEC\_RECEIPT\_AS\_VARIE per il salvataggio di ricevute PEC orfane come documenti non protocollati (richiede la property '_docway4mailboxmanager.pec.orphan-receipts-as-varie_')

### Changed
- Modificata la logica dei contatori su messaggi elaborati, skippati e in errore processati su una specifica mailbox

### Fixed
- In generazione documenti da fatturePA, compilazione dei campi del rif. esterno con i dati estratti dalla fattura stessa (e non recuperati da ACL) (Task #16546)
- Corretto indice del messaggio in elaborazione sul log di MSA
- Corretta la registrazione dell'indirizzo mail del mittente su audit di MSA
- Corretto i parametri di configurazione per il protocollo SMTP-TLS (nuova versione mail.jar)


## [3.0.3] - 2019-01-07

### Changed
- Aumentato il livello di log a DEBUG

### Fixed
- Corretto errore in fase di elaborazione di un messaggio: Part NULL (Task #16775).
- Corretto query su ricerca documento per aggancio notifiche. Ci sono casi nei quali vengono fatte ricerche su nrecord anziche' num\_prot (Task #16797).
- Corretto controllo su porta socket occupata. Su versione precedente possibilità di avviare istanze multiple di MSA.
- Aggiunta del riferimento al thread corrente allo username dell'utente per xw per eliminazione di eventuali problemi di "Protezione file non riuscita" dovuti a gestione multithread.
- Riconoscimento di file danneggiati all'interno della mail: Viene forzato il caricamento dell'EML come allegato al documento e viene aggiunta al campo note l'indicazione dei file danneggiati rilevati (Task #16824)
- Corretto bug in individuazione di files definiti su segnatura.xml (validazione della segnatura)
- Corretti xpath di estrazione Identificatore da notifiche relative ad interoperabilità


## [3.0.2] - 2018-12-07 

### Fixed
- Aggiornata la versione della libreria mail


## [3.0.1] - 2018-10-17 

### Added
- Aggiunto store type UPDATE\_NEW\_RECIPIENT\_INTEROP\_PA per gestire le seguenti casistiche:
    - messaggio di interoperabilità inviato più caselle di posta gestite dall'archiviatore
    - differenti messaggi contenenti la medesima Segnatura.xml inviate a più caselle di posta gestite dall'archiviatore

### Fixed
- Corretto comportamento dello store type UPDATE\_NEW\_RECIPIENT: gli RPA delle caselle successive (alla prima) vengono riportati come CC con diritto di intervento


## [3.0.0] - 2018-09-06 

### Added
- Aggiunta la possibilità di disabilitare l'invio delle mail di notifica in caso di errori in fase di scaricamento delle mail o di connessione
alle caselle di posta
- Definizione degli script di avvio (ed installazione come servizio) del processo di MSA su ambienti Linux e Windows
- Pacchettizzazione di MSA per installazione su ambienti Linux e Windows

### Changed
- Allineata la gestione delle FatturePA agli ultimi interventi svolti sulla libreria inclusa nella vecchia release di MSA

### Fixed
- La configurazione della protocollazione delle fatturePA deve essere recuperata dalla configurazione della casella di posta su eXtraWay e non dal file di
properties di MSA


## [1.0.2] - 2018-06-27

### Changed
- Modificato codice in maniera da non inviare messaggio di errore via email del tipo interruped exception su MongoDb in stop del servizio

### Fixed
- Corretto problema di estrazione del mittente da ACL nel caso di PEC (l'indirizzo del mittente per la ricerca in ACL ora viene estratto da daticert.xml)


## [1.0.1] - 2018-05-25 

### Added
- Aggiunti in it.tredi.msa.properties parametri per impostare timeout per la connessione al server smtp  per DocWay4
    - #timeout per socket e connection per l'invio di messaggi email - smtp server 
    - docway4mailboxmanager.mail-sender.socket-timeout=60000
    - docway4mailboxmanager.mail-sender.connection-timeout=10000
- In DocwayMailboxManager realizzato codice per eseguire n-tentativi in fase di connessione al server smtp


## [1.0.0] - 2018-05-13

### Added
- Riscrittura MSA su framework Spring con gestione scaricamento email multithread
 