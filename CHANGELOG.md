# Change Log

## [1.0.0] - 2017-05-13
-Riscrittura MSA

## [1.0.1] - 2017-05-25 
-Aggiunti in it.tredi.msa.properties parametri per impostare timeout per la connessione al server smtp  per DocWay4
	#timeout per socket e connection per l'invio di messaggi email - smtp server 
	docway4mailboxmanager.mail-sender.socket-timeout=60000
	docway4mailboxmanager.mail-sender.connection-timeout=10000
-In DocwayMailboxManager realizzato codice per eseguire n-tentativi in fase di connessione al server smtp

## [1.0.2] - 2017-06-27
-Modificato codice in maniera da non inviare messaggio di errore via email del tipo interruped exception su MongoDb in stop del servizio
-Corretto problema di estrazione del mittente da ACL nel caso di PEC (l'indirizzo del mittente per la ricerca in ACL ora viene estratto da daticert.xml)

 
 