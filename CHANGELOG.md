# Change Log

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
 