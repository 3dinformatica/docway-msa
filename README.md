# MSA

## [1.0.0] - 2017-05-13 
Prima emissione

## [1.0.1] - 2017-05-25 
-Aggiunti in it.tredi.msa.properties parametri per impostare timeout per la connessione al server smtp  per DocWay4
	#timeout per socket e connection per l'invio di messaggi email - smtp server 
	docway4mailboxmanager.mail-sender.socket-timeout=60000
	docway4mailboxmanager.mail-sender.connection-timeout=10000
-In DocwayMailboxManager realizzato codice per eseguire n-tentativi in fase di connessione al server smtp

 
