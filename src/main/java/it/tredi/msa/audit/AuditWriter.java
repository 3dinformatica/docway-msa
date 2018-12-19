package it.tredi.msa.audit;

import it.tredi.msa.configuration.MailboxConfiguration;
import it.tredi.msa.entity.AuditMailboxRun;
import it.tredi.msa.mailboxmanager.ParsedMessage;

/**
 * Registrazione dell'audit relativo allo scaricamento delle email dalle caselle di posta configurate (stato di scaricamento, registrazione di errori, 
 * log completo dei messaggi processati)
 */
public abstract class AuditWriter {
	
	/**
	 * Identifica se l'audit di MSA risulta configurato come FULL (registrazione di tutte le mail processate indipendentemente dall'esito) o 
	 * BASE (registrazione nell'audit dei soli errori riscontrati)
	 */
	private boolean full;
	
	/**
	 * Scrittura sull'audit di MSA di un messaggio email processato correttamente
	 * @param mailboxConfiguration Configurazione della casella di posta
	 * @param parsedMessage Messaggio email parsato
	 * @throws Exception
	 */
	public abstract void writeSuccessAuditMessage(MailboxConfiguration mailboxConfiguration, ParsedMessage parsedMessage) throws Exception;
	
	/**
	 * Scrittura sull'audit di MSA di un messaggio email sul quale sono stati riscontrati errori
	 * @param mailboxConfiguration Configurazione della casella di posta
	 * @param parsedMessage Messaggio email parsato
	 * @param e Eccezione riscontrata in fase di elaborazione della mail
	 * @throws Exception
	 */
	public abstract void writeErrorAuditMessage(MailboxConfiguration mailboxConfiguration, ParsedMessage parsedMessage, Exception e) throws Exception;
	
	/**
	 * Ritorna true se il messaggio email parsato risulta fra quelli andati in errore (messaggio precedentemente registrato sull'audit
	 * di MSA perche' riscontrato errore)
	 * @param mailboxConfiguration Configurazione della casella di posta
	 * @param parsedMessage Messaggio email da verificare
	 * @return true se il messaggio risulta fra quelli andati in errore in precedenza, false altrimenti
	 * @throws Exception
	 */
	public abstract boolean isErrorMessageFoundInAudit(MailboxConfiguration mailboxConfiguration, ParsedMessage parsedMessage) throws Exception;
	
	/**
	 * Registrazione dello stato di avanzamento su una casella di posta
	 * @param auditMailboxRun Dettaglio di avanzamento su una casella di posta elettronica
	 * @throws Exception
	 */
	public abstract AuditMailboxRun writeAuditMailboxRun(AuditMailboxRun auditMailboxRun) throws Exception;

	/**
	 * Ritorna true se MSA è configurato per registrare in audit tutti i messaggi processati, false se devono essere mantenuti
	 * le sole email sulle quali è stato riscontrato errore
	 * @return
	 */
	public boolean isFull() {
		return full;
	}

	/**
	 * Setta la modalità di registrazione dell'audit (true = FULL, false = BASE)
	 * @param full
	 */
	public void setFull(String full) {
		this.setFull(Boolean.parseBoolean(full));
	}	
	
	/**
	 * Setta la modalità di registrazione dell'audit (true = FULL, false = BASE)
	 * @param full
	 */
	public void setFull(boolean full) {
		this.full = full;
	}
	
}
