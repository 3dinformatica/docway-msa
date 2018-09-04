# MSA

MSA (Mail Storage Agent) è il software che si occupa dell'archiviazione di messaggi di posta elettronica sul sistema 
documentale e di protocollo informatico DocWay. Gestisce sia caselle di posta elettronica standard che caselle PEC. 
Gestisce inoltre l'archiviazione di documenti di protocollo pervenuti tramite interoperabilità tra pubbliche 
amministrazioni. I protocolli supportati sono IMAP e POP.

## Dettagli

Le principali migliorie apportate dalla nuova versione MSA sono:

* pool di thread worker concorrenti per effettuare l’archiviazione in parallelo dei messaggi di posta elettronica 
di mailbox differenti;
* pannello WEB per il monitoraggio degli eventi e per il controllo dello stato di scaricamento sulla singola 
casella di posta: ultimo accesso, numero di messaggi letti, eventuali errori, elenco dei messaggi archiviati;
* possibilità di poter customizzare il comportamento dell’archiviatore a livello di singola casella di posta. 
Ad esempio poter personalizzare le regole per la  produzione del documento (metadati) a partire dalla mail, se 
cancellare il messaggio alla fine dell’elaborazione o se spostarlo su un diverso folder (solo per il protocollo IMAP).

## Prerequisiti

* Java 8

* MongoDB (vers. 3.6.3)

## Configurazione

Per poter installare l'MSA occorre configurare alcuni file:

1) ***application.properties***: descrive la porta da utilizzare per l'applicazione MSA e i parametri di connessione a
MongoDB per i vari audit per la registrazione dello stato dell'elaborazione delle caselle di posta.
2) ***it.highwaytech.broker.properties***: per i parametri di connessione al broker ExtraWay e le varie configurazioni
dell'audit applicativo.
3) ***it.tredi.msa.properties***: configurazioni generali delle modalità operative di esecuzione dell'agent (per maggiori
informazioni controllare i commenti presenti sul file).
4) ***log4j2.xml***: parametri di configurazione dei log applicativi.

## Configurazione servizi

### msa (LINUX)

- File di avvio di MSA su Linux.  Di seguito è riportata la sezione del file che deve **obbligatoriamente** essere 
configurata:

```
# N.B.: E' richiesto Java8 per l'esecuzione del processo
JAVA_COMMAND=/usr/bin/java

# Massima dimensione della memoria heap per il MSA
MAXHEAP=-Xmx128m

MSAPID=/tmp/msa.pid

NICE_LEVEL=10
```

### msa.bat (WINDOWS)

- File per un avvio da riga di comando in ambiente Windows. Di seguito è riportata la sezione del file che deve 
**obbligatoriamente** essere configurata:

```
rem la variabile JVM serve per impostare la java virtual machine che verra' utilizzata per avviare il servizio
rem per avviare da un persorso specifico si puo' settare la variabile come nell'esempio seguente
rem set "JVM=C:\JDKS\64bit\1.8.0_40\bin\java.exe"
rem NOTA BENE e' richiesta una versione di JVM non inferiore alla 1.8.0
set "JVM=java"

rem per settare le opzioni della JVM settare la variabile JVM_OPTS
set JVM_OPTS=-Xmx1024m -Xms1024m
```

### install-32.bat (WINDOWS)

- File per installare come servizio in ambiente Windows a 32 bit. Di seguito è riportata la sezione del file che deve 
**obbligatoriamente** essere configurata:

```
set JVM=auto
rem settata con auto ricava la jvm dal registro di windows
rem per impostarla a una jvm specifica occorre settarla al path della jvm.dll
rem set JVM="C:\JDKS\32bit\1.8.0_40\jre\bin\server\jvm.dll"

set xms=1024m
set xmx=1024m
```

### install-amd64.bat (WINDOWS)

- File per installare come servizio in ambiente Windows amd a 64 bit. Di seguito è riportata la sezione del file che deve 
**obbligatoriamente** essere configurata:

```
set JVM=auto
rem impostare a una jvm specifica occorre settarla al path della jvm.dll
rem set JVM="C:\JDKS\32bit\1.8.0_40\jre\bin\server\jvm.dll"

set xms=1024m
set xmx=1024m
```

### install-ia64.bat (WINDOWS)

- File per installare come servizio in ambiente Windows ia a 64 bit. Di seguito è riportata la sezione del file che deve 
**obbligatoriamente** essere configurata:

```
set JVM=auto
rem impostare a una jvm specifica occorre settarla al path della jvm.dll
rem set JVM="C:\JDKS\32bit\1.8.0_40\jre\bin\server\jvm.dll"

set xms=1024m
set xmx=1024m
```

### uninstall.bat (WINDOWS)

- File per rimuovere il servizio in ambiente Windows.


## Esecuzione

**Linux**

```
bin/msa {start|stop|status|restart|debug}
```

**Windows - Avvio Manuale**

Spostarsi da prompt nella directory bin e lanciare

```
msa.bat
```

**Windows - Installazione Servizio**

Spostarsi da prompt nella directory bin e lanciare a seconda del sistema

```
install_32.bat
```
oppure

```
install_amd64.bat
```
oppure

```
install_ia64.bat
```

**Windows - Disinstallazione Servizio**

Spostarsi da prompt nella directory bin e lanciare a seconda del sistema

```
uninstall.bat
```

## Sviluppi mancanti

Rispetto alla precedente release di MSA non stati portati i seguenti sviluppi:

* splitEmailByAttachments: suddivisione della mail in più documenti in base agli allegati contenuti (un allegato per ogni
documento). **N.B.** teoricamente questa funzione non dovrebbe essere necessaria su alcun cliente.

* rifiutoEmail: rifiuto (skip della conversione in documento) di un email in base alle estensioni degli
allegati contenuti (possibilità di definire un set di estensioni non supportate).

## BUILD
-compilare da Eclipse
-Posizionarsi nella home del progetto (Es: 'user/git/it.tredi.msa') ed eseguire il comando:
	mvn package
-Prelevare il file 'target/msa-<VERSION_NUMBER>.jar' e decomprimerlo
 
## START
-Posizionarsi nella cartella 'msa-<VERSION_NUMBER>' ed eseguire il comando
	java -Xmx1024m org.springframework.boot.loader.JarLauncher
	
	
