package it.tredi.msa.mailboxmanager.docway;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.Message.RecipientType;
import javax.mail.Part;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import it.tredi.mail.MailClientHelper;
import it.tredi.mail.MailSender;
import it.tredi.mail.entity.MailAttach;
import it.tredi.msa.Utils;
import it.tredi.msa.configuration.docway.DocwayMailboxConfiguration;
import it.tredi.msa.mailboxmanager.ContentProvider;
import it.tredi.msa.mailboxmanager.MailboxManager;
import it.tredi.msa.mailboxmanager.MessageContentProvider;
import it.tredi.msa.mailboxmanager.ParsedMessage;
import it.tredi.msa.mailboxmanager.PartContentProvider;
import it.tredi.msa.mailboxmanager.StringContentProvider;
import it.tredi.msa.mailboxmanager.docway.exception.MultipleFoldersException;
import it.tredi.msa.mailboxmanager.docway.fatturapa.FatturaPAItem;
import it.tredi.msa.mailboxmanager.docway.fatturapa.FatturaPAUtils;
import it.tredi.msa.mailboxmanager.docway.fatturapa.conf.OggettoDocumentoBuilder;
import it.tredi.msa.mailboxmanager.docway.fatturapa.conf.OggettoParseMode;

/**
 * Estensione della gestione delle mailbox (lettura, elaborazione messaggi, ecc.) per finalita' di gestione documentale
 */
public abstract class DocwayMailboxManager extends MailboxManager {
	
	protected Date currentDate;
	protected MailSender mailSender;
	protected boolean ignoreMessage;
	
	protected static final String TESTO_EMAIL_FILENAME = "testo email.txt";
	protected static final String TESTO_HTML_EMAIL_FILENAME = "testo email.html";
	protected static final String MESSAGGIO_ORIGINALE_EMAIL_FILENAME = "MessaggioOriginale.eml";
	protected static final String DEFAULT_ALLEGATO = "0 - nessun allegato";
	
	private final static String FILE_NOT_FOUND_IN_SEGNATURA = "Non è stato possibile individuare tra gli allegati della mail il file referenziato nel messaggio di interoperabilità PA: %s\n";
	private final static String DAMAGED_FILE_IN_SEGNATURA = "Non è stato possibile scaricare uno degli allegati della mail referenziato nel messaggio di interoperabilità PA (possibile file corrotto): %s\n";
	private final static String SEGNATURA_MESSAGE_AS_BOZZA = "Il documento è stato creato come bozza a causa di errori in fase di importazione del messaggio di interoperabilità Segnatura.xml.\n";
	private final static String SEGNATURA_PARSE_ERROR = "Si sono verificati degli errori in fase di importazione del messaggio di interoperabilità Segnatura.xml.\n";
	private final static String SEGNATURA_NULL_EMPTY_FIELD = "Valore campo '%s' nullo o vuoto.\n";
	private final static String SEGNATURA_FIELD_FORMAT_ERROR = "Valore campo '%s' in formato scorretto: %s\n";
	
	private final static String ORPHAN_RECEIPTS_MESSAGE = "Ricevuta Orfana: Non è stato possibile individuare il documento di origine al quale la ricevuta fa riferimento.";
	
	private final static int MAILSENDER_CONNECTION_ATTEMPTS = 3;
	
	private static final Logger logger = LogManager.getLogger(DocwayMailboxManager.class.getName());
	
	public enum StoreType {
	    SAVE_NEW_DOCUMENT,
	    UPDATE_PARTIAL_DOCUMENT,
	    UPDATE_NEW_RECIPIENT,
	    SKIP_DOCUMENT,
	    SAVE_ORPHAN_PEC_RECEIPT_AS_VARIE,
	    ATTACH_INTEROP_PA_PEC_RECEIPT,
	    ATTACH_INTEROP_PA_NOTIFICATION,
	    SAVE_NEW_DOCUMENT_INTEROP_PA,
	    UPDATE_PARTIAL_DOCUMENT_INTEROP_PA,
	    SAVE_NEW_DOCUMENT_FATTURA_PA,
	    UPDATE_PARTIAL_DOCUMENT_FATTURA_PA,
	    ATTACH_FATTURA_PA_NOTIFICATION,
	    ATTACH_FATTURA_PA_PEC_RECEIPT,
	    IGNORE_MESSAGE,
	    UPDATE_NEW_RECIPIENT_INTEROP_PA
	}
	
	protected abstract Object saveNewDocument(DocwayDocument doc, ParsedMessage parsedMessage) throws Exception;
	protected abstract Object updatePartialDocument(DocwayDocument doc) throws Exception;
	protected abstract Object updateDocumentWithRecipient(DocwayDocument doc) throws Exception;
	protected abstract RifEsterno createRifEsterno(String name, String address) throws Exception;
	
	/**
	 * Definizione dei riferimenti interni (assegnatari) del documento in base al messaggio parsato e i parametri di configurazione
	 * letti dalla casella di posta
	 * @param parsedMessage Messaggio parsato (in fase di elaborazione)
	 * @return
	 * @throws Exception
	 */
	protected abstract List<RifInterno> createRifInterni(ParsedMessage parsedMessage) throws Exception;
	
	protected abstract void sendNotificationEmails(DocwayDocument doc, Object saveDocRetObj);
	
	/**
	 * Dato il messaggio email parsato, viene identificato il tipo di registrazione da compiere (creazione di un nuovo documento, aggancio
	 * di una notifica a doc esistente, ecc.)
	 * @param parsedMessage Messaggio email parsato
	 * @return Tipologia di registrazione da applicare al messaggio parsato (nuovo doc, update di doc esistente, ecc.)
	 * @throws Exception
	 */
	protected abstract StoreType decodeStoreType(ParsedMessage parsedMessage) throws Exception;
	
	protected abstract void attachInteropPAPecReceiptToDocument(ParsedMessage parsedMessage) throws Exception;
	protected abstract void attachInteropPANotificationToDocument(ParsedMessage parsedMessage) throws Exception;
	protected abstract String buildNewNumprotStringForSavingDocument() throws Exception;
	protected abstract String buildNewNumrepStringForSavingDocument(String repertorioCod) throws Exception;
	protected abstract RifEsterno createMittenteFatturaPA(ParsedMessage parsedMessage) throws Exception;
	protected abstract void attachFatturaPANotificationToDocument(ParsedMessage parsedMessage) throws Exception;
	protected abstract void attachFatturaPAPecReceiptToDocument(ParsedMessage parsedMessage) throws Exception;
	
	@Override
	public void customInit() {
		DocwayMailboxConfiguration conf = (DocwayMailboxConfiguration)getConfiguration();
		if (conf.getSmtpHost() != null && !conf.getSmtpHost().isEmpty()) {
			mailSender = MailClientHelper.createMailSender(conf.getSmtpHost(), conf.getSmtpPort(), conf.getSmtpUser(), conf.getSmtpPassword(), conf.getSmtpProtocol());
			mailSender.setSocketTimeout(conf.getSmtpSocketTimeout());
			mailSender.setConnectionTimeout(conf.getSmtpConnectionTimeout());
		}
	}
	
	@Override
    public void openSession() throws Exception {
		DocwayMailboxConfiguration conf = (DocwayMailboxConfiguration)getConfiguration();
		super.openSession();
		if (mailSender != null) {

			//n-tentivi di connessione al server smtp
        	for (int attemptIndex = 1; attemptIndex <= MAILSENDER_CONNECTION_ATTEMPTS; attemptIndex++) {
            	try {
            		mailSender.connect();
            		break;
            	}
            	catch (Exception e) {
            		logger.warn("[" + conf.getAddress() + "] connection failed: (" + attemptIndex + "/" +MAILSENDER_CONNECTION_ATTEMPTS + ") attempt. Trying again (1) sec.");
            		if (attemptIndex == MAILSENDER_CONNECTION_ATTEMPTS)
            			throw e;
            		Thread.sleep(1000); //1 sec delay
            	}    		
        	}			
			
		}
    }
	
	@Override
    public void closeSession() {
    	super.closeSession();
		try {
			if (mailSender != null)
				mailSender.disconnect();
		}
		catch (Exception e) {
			logger.warn("[" + getConfiguration().getAddress() + "] failed to close mailSender session", e);
		}		
	}	
	
	@Override
    public ParsedMessage parseMessage(Message message) throws Exception {
		DocwayMailboxConfiguration conf = (DocwayMailboxConfiguration) super.getConfiguration();
    	return new DocwayParsedMessage(message, conf.isCasellaImport());
    }
	
	/**
	 * Ritorna true se il messaggio di posta elettronica corrisponde ad un messaggio inoltrato alla mailbox corrente, false
	 * se la mailbox corrente risulta come mittente e/o destinatario del messaggio
	 * @return
	 */
	public boolean isForwardedToTagsMailbox(ParsedMessage parsedMessage) {
		boolean forwardedToTags = false;
		if (parsedMessage != null) {
			DocwayMailboxConfiguration conf = (DocwayMailboxConfiguration) super.getConfiguration();
			if (conf.isArchiviazioneByTags() // la casella corrente corrisponde ad una mailbox di archiviazione tramite tags
					&& parsedMessage.isReplyOrForward() // il messaggio corrisponde ad un inoltro o rispondi
					&& parsedMessage.containsTags() // l'oggetto del messaggio contiene almeno 1 tag
					&& this.equalsToAddress(parsedMessage, conf.getAddress()) // l'unico destinatario del messaggio corrisponde alla casella di archiviazione
				) {
				
				forwardedToTags = true;
			}
		}
		return forwardedToTags;
	}
	
	private boolean equalsToAddress(ParsedMessage parsedMessage, String expected) {
		InternetAddress[] addresses = parsedMessage.getToAddresses();
		return (expected != null && !expected.isEmpty() && addresses != null 
				&& addresses.length == 1 
				&& addresses[0] != null
				&& addresses[0].getAddress() != null 
				&& addresses[0].getAddress().equalsIgnoreCase(expected));
	}
	
	@Override
    public void storeMessage(ParsedMessage parsedMessage) throws Exception {
		DocwayMailboxConfiguration conf = (DocwayMailboxConfiguration)getConfiguration();
		if (logger.isInfoEnabled())
    		logger.info("[" + conf.getAddress() + "] storing message [" + parsedMessage.getMessageId() + "]");
		
		this.currentDate = new Date();
		this.ignoreMessage = false;
		
		StoreType storeType = decodeStoreType(parsedMessage);
		if (logger.isInfoEnabled())
			logger.info("[" + conf.getAddress() + "] message [" + parsedMessage.getMessageId() + "] store type [" + storeType + "]");
		
		if (storeType == StoreType.SAVE_NEW_DOCUMENT || storeType == StoreType.SAVE_ORPHAN_PEC_RECEIPT_AS_VARIE || storeType == StoreType.UPDATE_PARTIAL_DOCUMENT || storeType == StoreType.UPDATE_NEW_RECIPIENT) { //save new document or update existing one
			//build new Docway document
			DocTipoEnum forcedType = null;
			if (storeType == StoreType.SAVE_ORPHAN_PEC_RECEIPT_AS_VARIE) {
				forcedType = DocTipoEnum.VARIE; // viene forzata la creazione di un documento non protocollato (generico)
				parsedMessage.addRelevantMessage(ORPHAN_RECEIPTS_MESSAGE); // aggiunta al documento dell'annotazione relativa alla ricevuta orfana
			}
			DocwayDocument doc = createDocwayDocumentByMessage(parsedMessage, forcedType);
			
			//save new document
			Object retObj = null;
			if (storeType == StoreType.SAVE_NEW_DOCUMENT || storeType == StoreType.SAVE_ORPHAN_PEC_RECEIPT_AS_VARIE) //1. doc not found by messageId -> save new document
				retObj = saveNewDocument(doc, parsedMessage);
			else if (storeType == StoreType.UPDATE_PARTIAL_DOCUMENT) //2. doc found by messageId flagged as partial (attachments upload not completed) -> update document adding missing attachments
				retObj = updatePartialDocument(doc);			
			else if (storeType == StoreType.UPDATE_NEW_RECIPIENT) //3. doc found with different recipient email (same email sent to different mailboxes) -> update document adding new CCs
				retObj = updateDocumentWithRecipient(doc);			
			
			//notify emails
			if (conf.isNotificationEnabled() && (conf.isNotifyRPA() || conf.isNotifyCC())) { //if notification is activated
				if (logger.isInfoEnabled())
					logger.info("[" + conf.getAddress() + "] sending notification emails [" + parsedMessage.getMessageId() + "]");
				sendNotificationEmails(doc, retObj);
			}							
		}
		else if (storeType == StoreType.SAVE_NEW_DOCUMENT_INTEROP_PA || storeType == StoreType.UPDATE_PARTIAL_DOCUMENT_INTEROP_PA || storeType == StoreType.UPDATE_NEW_RECIPIENT_INTEROP_PA) { //save new interopPA document (Segnatura.xml) or update existing one
			//build new Docway document
			DocwayDocument doc = createDocwayDocumentByInteropPAMessage(parsedMessage);
			
			//save new document
			Object retObj = null;
			if (storeType == StoreType.SAVE_NEW_DOCUMENT_INTEROP_PA) //1. doc not found by messageId -> save new document
				retObj = saveNewDocument(doc, parsedMessage);
			else if (storeType == StoreType.UPDATE_PARTIAL_DOCUMENT_INTEROP_PA) //2. doc found by messageId flagged as partial (attachments upload not completed) -> update document adding missing attachments
				retObj = updatePartialDocument(doc);
			else if (storeType == StoreType.UPDATE_NEW_RECIPIENT_INTEROP_PA) //3. doc found with different recipient email (same email sent to different mailboxes) -> update document adding new CCs
				retObj = updateDocumentWithRecipient(doc);	
			
			//notify emails
			if (conf.isNotificationEnabled() && (conf.isNotifyRPA() || conf.isNotifyCC())) { //if notification is activated
				if (logger.isInfoEnabled())
					logger.info("[" + conf.getAddress() + "] sending notification emails [" + parsedMessage.getMessageId() + "]");
				sendNotificationEmails(doc, retObj);
			}
		}		
		else if (storeType == StoreType.ATTACH_INTEROP_PA_PEC_RECEIPT) { //PEC receipt for interopPA/fatturaPA message/notification
			attachInteropPAPecReceiptToDocument(parsedMessage);
		}
		else if (storeType == StoreType.ATTACH_INTEROP_PA_NOTIFICATION) { //interopPA notification (Aggiornamento.xml, Eccezione.xml, Annullamento.xml, Conferma.xml)
			attachInteropPANotificationToDocument(parsedMessage);
		}
		else if (storeType == StoreType.SAVE_NEW_DOCUMENT_FATTURA_PA || storeType == StoreType.UPDATE_PARTIAL_DOCUMENT_FATTURA_PA) { //save new fatturaPA document or update existing one
			//build new Docway document
			DocwayDocument doc = createDocwayDocumentByFatturaPAMessage(parsedMessage);
			
			//save new document
			Object retObj = null;
			if (storeType == StoreType.SAVE_NEW_DOCUMENT_FATTURA_PA) //1. doc not found by messageId -> save new document
				retObj = saveNewDocument(doc, parsedMessage);
			else if (storeType == StoreType.UPDATE_PARTIAL_DOCUMENT_FATTURA_PA) //2. doc found by messageId flagged as partial (attachments upload not completed) -> update document adding missing attachments
				retObj = updatePartialDocument(doc);

			//notify emails
			if (conf.isNotificationEnabled() && (conf.isNotifyRPA() || conf.isNotifyCC())) { //if notification is activated
				if (logger.isInfoEnabled())
					logger.info("[" + conf.getAddress() + "] sending notification emails [" + parsedMessage.getMessageId() + "]");
				sendNotificationEmails(doc, retObj);
			}
		}		
		else if (storeType == StoreType.ATTACH_FATTURA_PA_NOTIFICATION) { //fatturaPA notification (Scarto, Mancata consegna, Esito committente, Scarto esito committente, Decorrenza dei termini)
			attachFatturaPANotificationToDocument(parsedMessage);
		}		
		else if (storeType == StoreType.ATTACH_FATTURA_PA_PEC_RECEIPT) { //PEC receipt for fatturaPA message/notification
			attachFatturaPAPecReceiptToDocument(parsedMessage);
		}		
		else if (storeType == StoreType.SKIP_DOCUMENT) //4. there's nothing to do (maybe previous message deletion/move failed)
			;
		else if (storeType == StoreType.IGNORE_MESSAGE) //ignore message without moving/deleting it
			ignoreMessage = true;
		else
			throw new Exception("Unsupported store type: " + storeType);
	}
	
	/**
	 * Dato il messaggio parsato letto dalla casella di posta viene istanziato l'oggetto documento in base alle
	 * specifiche della configurazione della casella stessa
	 * @param parsedMessage Messaggio di posta da convertire in documento
	 * @return Documento da registrare su DocWay
	 * @throws Exception
	 */
	private DocwayDocument createDocwayDocumentByMessage(ParsedMessage  parsedMessage) throws Exception {
		return createDocwayDocumentByMessage(parsedMessage, DocTipoEnum.NOONE);
	}
	
	/**
	 * Dato il messaggio parsato letto dalla casella di posta viene istanziato l'oggetto documento in base alle
	 * specifiche della configurazione della casella stessa
	 * @param parsedMessage Messaggio di posta da convertire in documento
	 * @param docAsVarie true se occorre forzare il salvataggio del documento come "non protocollato", false altrimenti (generazione del documento in base alla configurazione prevista)
	 * @return Documento da registrare su DocWay
	 * @throws Exception
	 */
	private DocwayDocument createDocwayDocumentByMessage(ParsedMessage  parsedMessage, DocTipoEnum forcedType) throws Exception {
		DocwayMailboxConfiguration conf = (DocwayMailboxConfiguration)getConfiguration();
		DocwayDocument doc = new DocwayDocument();
		
		// mbernardini 01/07/2019 : archiviazione tramite TAGS
		// In caso di archiviazione tramite TAGS attiva potrebbe essere necessario applicare variazioni alla costruzione 
		// del documento in base a quanto previsto dal document model
		FascicoloReference fascicolo = null;
		
		if (conf.isArchiviazioneByTags() && parsedMessage.containsTags()) {
			fascicolo = this.findCodFascicoloByTags(conf.getCodAmmAoo(), parsedMessage.getSubjectTags());
			
			if (fascicolo != null && fascicolo.getCodFascicolo() != null && !fascicolo.getCodFascicolo().isEmpty()) {
				if (logger.isDebugEnabled())
					logger.debug("[" + conf.getAddress() + "] fascicolazione tramite TAGS. fascicolo = " + fascicolo.getCodFascicolo());
				
				// identificazione del flusso di documento
				forcedType = getFlussoDocByRecipients(parsedMessage);
				if (logger.isDebugEnabled())
					logger.debug("[" + conf.getAddress() + "] fascicolazione tramite TAGS. tipo doc calcolato = " + forcedType);
			}
		}
		
		if (forcedType == null)
			forcedType = DocTipoEnum.NOONE;
		
		if (logger.isDebugEnabled())
			logger.debug("[" + conf.getAddress() + "] creazione del documento da messaggio parsato. messageId = " 
						+ parsedMessage.getMessageId() 
						+ ((forcedType != DocTipoEnum.NOONE) ? ", FORZATO IL SALVATAGGIO COME DOCUMENTO '" + forcedType.getText() + "'" : ""));
		
		//tipo doc
		if (forcedType.isValidValue())
			doc.setTipo(forcedType.getText());
		else
			doc.setTipo(conf.getTipoDoc());
		
		//cod_amm_aoo
		doc.setCodAmmAoo(conf.getCodAmmAoo());
		
		//anno
		doc.setAnno(conf.isCurrentYear()? (new SimpleDateFormat("yyyy")).format(currentDate) : "");
		
		if (forcedType != DocTipoEnum.VARIE) {
			//bozza
			doc.setBozza(conf.isBozza());
			
			//num_prot
			doc.setNumProt((conf.isNumProt()) ? doc.getAnno() + "-" + doc.getCodAmmAoo() + "-." : "");
			
			//annullato
			doc.setAnnullato(false);
			
			//repertorio
			if (forcedType.getText().equals("") || forcedType.getText().equals(conf.getTipoDoc())) { 
				// il repertorio deve essere gestito solo se non e' stato sovrascritto il tipo di documento (forcedType nullo) o se il tipo forzato
				// corrisponde a quello previsto dalla configurazione (document model)
				doc.setRepertorio(conf.getRepertorio());
				doc.setRepertorioCod(conf.getRepertorioCod());
			}
		}
		
		//data prot
		doc.setDataProt(conf.isCurrentDate()? (new SimpleDateFormat("yyyyMMdd")).format(currentDate) : "");
		
		//messageId
		doc.setMessageId(parsedMessage.getMessageId());
		
		//recipientEmail
		doc.setRecipientEmail(conf.getEmail());
		
		//sent date
		doc.setSentDate(parsedMessage.getSentDate());
		
		//autore
		if (doc.getTipo().equalsIgnoreCase(DocwayMailboxConfiguration.DOC_TIPO_VARIE))
			doc.setAutore((parsedMessage.getFromPersonal() == null || parsedMessage.getFromPersonal().isEmpty())? parsedMessage.getFromAddress() : parsedMessage.getFromPersonal());

		//oggetto
		doc.setOggetto(parsedMessage.getSubject());
		
		//tipologia
		doc.setTipologia(conf.getTipologia());
		
		//mezzo trasmissione
		doc.setMezzoTrasmissione(conf.getMezzoTrasmissione());
		
		//rif esterni
		if (doc.getTipo().equalsIgnoreCase(DocwayMailboxConfiguration.DOC_TIPO_ARRIVO)) {
			String address = parsedMessage.isPecMessage()? parsedMessage.getMittenteAddressFromDatiCertPec() : parsedMessage.getFromAddress();
			doc.addRifEsterno(createRifEsterno((parsedMessage.getFromPersonal() == null || parsedMessage.getFromPersonal().isEmpty())? address : parsedMessage.getFromPersonal(), address));
		}
		else if (doc.getTipo().equalsIgnoreCase(DocwayMailboxConfiguration.DOC_TIPO_PARTENZA)) {
			Address[] recipients = parsedMessage.getMessage().getRecipients(RecipientType.TO);
			for (Address recipient:recipients) {
				String personal = ((InternetAddress)recipient).getPersonal();
				String address = ((InternetAddress)recipient).getAddress();
				doc.addRifEsterno(createRifEsterno((personal==null || personal.isEmpty())? address : personal, address));
			}
			// TODO gestione dei CC
		}
		
		//voce di indice
		doc.setVoceIndice(conf.getVoceIndice());
		
		//classif
		doc.setClassif(conf.getClassif());
		doc.setClassifCod(conf.getClassifCod());
		
		//note
		if (conf.isNoteAutomatiche()) {
			doc.setNote(parsedMessage.getTextPartsWithHeaders());
		}
		
		//rif interni
		List<RifInterno> rifInterni = createRifInterni(parsedMessage);
		rifInterni = this.mergeRifInterniFascicolo(rifInterni, fascicolo); // eventuale archiviazione in fascicolo tramite TAGS estratti dall'oggetto del messaggio
		for (RifInterno rifInterno:rifInterni)
			doc.addRifInterno(rifInterno);
		
		//storia creazione
		StoriaItem creazione = new StoriaItem("creazione");
		creazione.setOper(conf.getOper());
		creazione.setUffOper(conf.getUffOper());
		creazione.setData(currentDate);
		creazione.setOra(currentDate);
		doc.addStoriaItem(creazione);
		
		//aggiunta in storia delle operazioni relative ai rif interni
		for (RifInterno rifInterno:rifInterni) {
			StoriaItem storiaItem = StoriaItem.createFromRifInterno(rifInterno);
			storiaItem.setOperatore(conf.getOperatore());
			storiaItem.setData(currentDate);
			storiaItem.setOra(currentDate);
			doc.addStoriaItem(storiaItem);
		}
		
		//files + immagini + allegato
		if (logger.isDebugEnabled())
			logger.debug("[" + conf.getAddress() + "] gestione files e immagini...");
		createDocwayFiles(parsedMessage, doc);
		
		//parsedMessage.relevantMessages -> postit
		for (String relevantMessage:parsedMessage.getRelevantMssages()) {
			Postit postit = new Postit();
			postit.setText(relevantMessage);
			postit.setOperatore(conf.getOperatore());
			postit.setData(currentDate);
			postit.setOra(currentDate);
			doc.addPostit(postit);
		}
		
		return doc;
	}
	
	/**
	 * Unione dei rif interni previsti dalla configurazione della mailbox (document model) con quelli ereditati dall'eventuale fascicolo
	 * sul quale il documento dovra' essere caricato
	 * @param rifInterni Elenco di rif interni previsti da configurazione della mailbox
	 * @param fascicolo Informazioni sul fascicolo all'interno del quale caricare il documento (contiene i rif interni definiti sul fascicolo)
	 * @return
	 */
	private List<RifInterno> mergeRifInterniFascicolo(List<RifInterno> rifInterni, FascicoloReference fascicolo) {
		if (fascicolo != null && fascicolo.getCodFascicolo() != null && !fascicolo.getCodFascicolo().isEmpty()) {
			if (rifInterni == null)
				rifInterni = new ArrayList<>();
			
			// TODO non e' detto che il caricamento dei rif del fascicolo debba essere fatto, dovrebbe dipendere da una property di docway
			List<String> codL = new ArrayList<>();
			List<RifInterno> merged = new ArrayList<>();
			for(int i=0; i<fascicolo.getRifs().size(); i++) {
				RifInterno rif = fascicolo.getRifs().get(i);
				rif.setCodFasc(fascicolo.getCodFascicolo());
				merged.add(rif);
				codL.add(rif.getCodPersona());
			}
			for(int i=0; i<rifInterni.size(); i++) {
				RifInterno rif = rifInterni.get(i);
				if (rif.getDiritto().equalsIgnoreCase("RPA")) {
					rif.setDiritto("CC"); // l'RPA corrispondera' a quello del fasicoclo
					rif.setIntervento(true);
				}
				if (!codL.contains(rif.getCodPersona())) { // rif interno non ereditato dal fascicolo
					merged.add(rif);
					codL.add(rif.getCodPersona());
				}
			}
			rifInterni = merged;
		}
		return rifInterni;
	}
	
	/**
	 * Cerca di desumere il tipo di documento in base all'analisi del mittente e destinatari del messaggio
	 * @param message
	 * @throws Exception in caso di altra eccezione
	 * @return
	 */
	protected DocTipoEnum getFlussoDocByRecipients(ParsedMessage message) throws Exception {
		DocTipoEnum flusso = null;
		
		boolean mittenteInterno = this.isMittenteInterno(message);
		boolean destinatariEsterni = this.containsDestinatariEsterni(message);
		if (!mittenteInterno) {
			// mittente del messaggio ESTERNO -> doc. in arrivo
			flusso = DocTipoEnum.ARRIVO;
		}
		else {
			// mittente del messaggio INTERNO
			
			if (isForwardedToTagsMailbox(message)) {
				// messaggio inoltrato alla casella di gestione dei TAGS -> doc. varie
				flusso = DocTipoEnum.VARIE;
			}
			else {
				if (destinatariEsterni) {
					// almeno un rif. esterno presente fra i destinatari -> doc. in partenza
					flusso = DocTipoEnum.PARTENZA;
				}
				else {
					// individuati solo rif. interni -> doc. interno
				
					flusso = DocTipoEnum.VARIE;
					// TODO andrebbe gestito come caso di doc interno:
					// Questo implica il recupero dei rif. interni dai destinatari del documento e una logica completamente diversa della costruzione del doc XML
					//flusso = DocTipoEnum.INTERNO;
				}
			}
		}
		return flusso;
	}
	
	/**
	 * Ricerca di un fascicolo in base ai TAGS indicati nell'oggetto del messaggio. Deve essere recuperato uno ed un solo
	 * fascicolo in base ai TAGS presenti sul messagggio.
	 * @param codammaoo cod_amm_aoo in base al quale filtrare i fascicoli
	 * @param tags Elenco di TAGS in base ai quali ricercare il fascicolo
	 * @return Fascicolo trovato in base ai TAGS presenti nell'oggetto del messaggio
	 * @throws MultipleFoldersException in caso di piu' fascicoli trovati in base ai TAGS specificati
	 * @throws Exception in caso di altra eccezione
	 */
	protected abstract FascicoloReference findCodFascicoloByTags(String codammaoo, List<String> tags) throws MultipleFoldersException, Exception;
	
	/**
	 * Verifica se il mittente del messaggio corrisponde ad un utente interno o ad uno esterno. Ritorna true se l'utente che risulta essere
	 * il mittente del messaggio fa parte dell'anagrafica interna (persona/struttura interna), false altrimenti.
	 * @param message
	 * @return
	 * @throws Exception
	 */
	protected abstract boolean isMittenteInterno(ParsedMessage message) throws Exception;
	
	/**
	 * Verifica se il messaggio corrente contiene degli indirizzi email che possono essere associati a destinatari esterni (persone/strutture esterne)
	 * @param message
	 * @return
	 * @throws Exception
	 */
	protected abstract boolean containsDestinatariEsterni(ParsedMessage message) throws Exception;
	
	/**
	 * Verifica se il messaggio corrente contiene degli indirizzi email che possono essere associati a destinatari interni 
	 * all'azienda (persone/strutture interne)
	 * @param message
	 * @return
	 * @throws Exception
	 */
	protected abstract boolean containsDestinatariInterni(ParsedMessage message) throws Exception;
	
	
	private void createDocwayFiles(ParsedMessage parsedMessage, DocwayDocument doc) throws Exception {
		//email body html/text attachment
		// mbernardini 12/06/2019 : ripristinato il salvataggio come allegato del contenuto estratto come HTML e testo semplice
		_attachMessageContent(doc, parsedMessage.getHtmlParts(), TESTO_HTML_EMAIL_FILENAME);
		_attachMessageContent(doc, parsedMessage.getTextPartsWithHeaders(), TESTO_EMAIL_FILENAME);
		
		// mbernardini 17/12/2018 : gestione di messaggi email contenenti allegati danneggiati
		boolean damagedFound = false;
		List<String> damagedFiles = new ArrayList<>();
		
		//email attachments (files + immagini)
		List<MailAttach> attachments = parsedMessage.getAttachments();
		for (MailAttach attachment:attachments) {
			DocwayFile file = createDocwayFile();
			
			if (logger.isDebugEnabled())
				logger.debug("Read content from attachment " + attachment.getFileName() + "...");
			
			// Se non si riesce a caricare il contenuto del file diamo per scontato che si tratti di un file danneggiato
			boolean fileLoaded = false;
			try {
				file.setContentByProvider(new PartContentProvider(attachment.getPart()));
				fileLoaded = true;
			}
			catch(Exception e) {
				logger.warn("[" + attachment.getFileName() + "] Unable to read file content, damaged file... " + e.getMessage(), e);
				logger.info("Mark file " + attachment.getFileName() + " as DAMAGED file!");
				damagedFound = true;
				damagedFiles.add(attachment.getFileName());
			}
			if (fileLoaded) {
				file.setName(attachment.getFileName());
				if (isImage(file.getName())) //immagine
						doc.addImmagine(file);
				else //file
					doc.addFile(file);
			}
			
			//allegato
			doc.addAllegato(attachment.getFileName());
		}
		
		//allegato - default
		if (doc.getAllegato().isEmpty())
			doc.addAllegato(DEFAULT_ALLEGATO);
		
		// In caso di file danneggiati viene forzata l'aggiunta al documento dell'EML del messaggio email
		//EML
		if (((DocwayMailboxConfiguration)getConfiguration()).isStoreEml() || damagedFound) {
			DocwayFile file = createDocwayFile();
			file.setContentByProvider(new MessageContentProvider(parsedMessage.getMessage(), true));
			file.setName(MESSAGGIO_ORIGINALE_EMAIL_FILENAME);
			doc.addFile(file);			
		}
		
		// mbernardini 17/12/2018 : indicazione di eventuali file danneggiati all'interno delle note del documento
		if (damagedFound && !damagedFiles.isEmpty()) {
			// aggiunta di un'annotazione specifica sul documento XML
			parsedMessage.addRelevantMessage("Rilevati possibili file danneggiati allegati alla mail: " + String.join(", ", damagedFiles));
			//String note = doc.getNote();
			//if (note == null)
			//	note = "";
			//note = "Rilevati possibili file danneggiati allegati alla mail: " + String.join(", ", damagedFiles) + "\n-----\n" + note;
			//doc.setNote(note);
		}
	}
	
	/**
	 * Aggiunta del testo estratto dal messaggio (in formato HTML o testo semplice) al documento
	 * @param doc
	 * @param content
	 * @param attachName
	 * @throws Exception 
	 */
	private void _attachMessageContent(DocwayDocument doc, String content, String attachName) throws Exception {
		if (doc != null && content != null && !content.isEmpty()) {
			DocwayFile file = createDocwayFile();
			file.setName(attachName);
			file.setContentByProvider(new StringContentProvider(content.trim()));
			doc.addFile(file);
		}
	}
	
	protected boolean isImage(String fileName) {
		return fileName.toLowerCase().endsWith(".jpg")
				|| fileName.toLowerCase().endsWith(".jpeg")
				|| fileName.toLowerCase().endsWith(".tif")
				|| fileName.toLowerCase().endsWith(".tiff")
				|| fileName.toLowerCase().endsWith(".bmp")
				|| fileName.toLowerCase().endsWith(".png");
	}	

	private DocwayFile createDocwayFile() {
		DocwayMailboxConfiguration conf = (DocwayMailboxConfiguration)getConfiguration();
		DocwayFile file = new DocwayFile();
		file.setOperatore(conf.getOperatore());
		file.setCodOperatore("");
		file.setData(currentDate);
		file.setOra(currentDate);
		return file;
	}	
	
	private DocwayDocument createDocwayDocumentByInteropPAMessage(ParsedMessage  parsedMessage) throws Exception {
		DocwayMailboxConfiguration conf = (DocwayMailboxConfiguration)getConfiguration();
		if (logger.isDebugEnabled())
			logger.debug("[" + conf.getAddress() + "] creazione del documento da messaggio Interoperabilita'. messageId = " + parsedMessage.getMessageId());
		
		DocwayParsedMessage dcwParsedMessage = (DocwayParsedMessage)parsedMessage;
		Document segnaturaDocument = dcwParsedMessage.getSegnaturaInteropPADocument();

		String motivazioneNotificaEccezione = "";
	
/*		
		//validazione DTD
		String dtdSchemaUrl = "segnatura.dtd";
	    DocumentType docType = segnaturaDocument.getDocType();
	    if (docType != null)
	        docType.setSystemID(dtdSchemaUrl);
	    else
	        segnaturaDocument.setDocType(new DefaultDocumentType(segnaturaDocument.getRootElement().getName(), dtdSchemaUrl));
	    try {
		    SAXReader reader = new SAXReader(true);
		    reader.read(new StringReader(segnaturaDocument.asXML()));
	    }
	    catch (Exception e) {
	    	motivazioneNotificaEccezione += "Errore di validazione rispetto a segnatura.dtd: " + e;
	    }
*/	    

		//check preliminari
		
		//oggetto
		String oggetto = "[N/D]";
		try {
			oggetto = segnaturaDocument.selectSingleNode("/Segnatura/Intestazione/Oggetto").getText();	
		}
		catch (Exception e) {
			motivazioneNotificaEccezione += String.format(SEGNATURA_NULL_EMPTY_FIELD, "Oggetto");
		}
		
		//email certificata
		String emailCertificata = "";
		try {
			emailCertificata = segnaturaDocument.selectSingleNode("/Segnatura/Intestazione/Origine/IndirizzoTelematico").getText();
		}
		catch (Exception e) {
			motivazioneNotificaEccezione += String.format(SEGNATURA_NULL_EMPTY_FIELD, "IndirizzoTelematico");
			emailCertificata = dcwParsedMessage.getMittenteAddressFromDatiCertPec();
		}
		
		//CodiceAmministrazione
		// mbernardini 14/05/2019 : Reso piu' flessibile il formato del codice amministrazione in intestazione delle segnatura da normativa del 2013.
		// Il codice e’ dato dal valore assegnato automaticamente all’Amministrazione dall’IPA in fase di accreditamento
		String codiceAmministrazione = "[N/D]";
		Element nodeCodAmministrazione = (Element) segnaturaDocument.selectSingleNode("/Segnatura/Intestazione/Identificatore/CodiceAmministrazione");
		if (nodeCodAmministrazione != null && nodeCodAmministrazione.getTextTrim() != null && !nodeCodAmministrazione.getTextTrim().isEmpty())
			codiceAmministrazione = nodeCodAmministrazione.getTextTrim();
		else
			motivazioneNotificaEccezione += String.format(SEGNATURA_NULL_EMPTY_FIELD, "CodiceAmministrazione");
			
		//CodiceAOO
		String codiceAOO = "[N/D]";
		try {
			codiceAOO = segnaturaDocument.selectSingleNode("/Segnatura/Intestazione/Identificatore/CodiceAOO").getText();
			
			// mbernardini 14/05/2019 : Corretto il formato del codice AOO per compatibilita' con normativa
			//Pattern pattern = Pattern.compile("([A-Z]|[a-z]|[0-9]|-){1,16}");
			Pattern pattern = Pattern.compile("([A-Z]|[a-z]|[0-9]|-|_|\\.){1,16}");
			Matcher matcher = pattern.matcher(codiceAOO);
			if (!matcher.matches())
				motivazioneNotificaEccezione += "Valore campo 'CodiceAOO' in formato scorretto: " + codiceAOO + ".\n";

		}
		catch (Exception e) {
			motivazioneNotificaEccezione += String.format(SEGNATURA_NULL_EMPTY_FIELD, "CodiceAOO");
		}

		//NumeroRegistrazione
		String nProt = "[N/D]";
		try {
			nProt = segnaturaDocument.selectSingleNode("/Segnatura/Intestazione/Identificatore/NumeroRegistrazione").getText();
			
			Pattern pattern = Pattern.compile("[0-9]{7}");
			Matcher matcher = pattern.matcher(nProt);
			if (!matcher.matches())
				motivazioneNotificaEccezione += "Valore campo 'NumeroRegistrazione' in formato scorretto: " + nProt + ".\n";
			
		}
		catch (Exception e) {
			motivazioneNotificaEccezione += String.format(SEGNATURA_NULL_EMPTY_FIELD, "NumeroRegistrazione");
		}				

		//DataRegistrazione
		String dataProt = "9999-12-31";
		Date dataProtD = new SimpleDateFormat("yyyy-MM-dd").parse(dataProt);
		try {
			dataProt = segnaturaDocument.selectSingleNode("/Segnatura/Intestazione/Identificatore/DataRegistrazione").getText();
	        dataProtD = new SimpleDateFormat("yyyy-MM-dd").parse(dataProt);
		}
		catch (Exception e) {
			motivazioneNotificaEccezione += String.format(SEGNATURA_FIELD_FORMAT_ERROR, "DataRegistrazione", dataProt);
		}

		//create DocwayDocument
		DocwayDocument doc = new DocwayDocument();
		
		//tipo doc
		doc.setTipo(DocwayMailboxConfiguration.DOC_TIPO_ARRIVO);
		
		//cod_amm_aoo
		doc.setCodAmmAoo(conf.getCodAmmAoo());
		
		//data prot
		doc.setDataProt((new SimpleDateFormat("yyyyMMdd")).format(currentDate));
		
		//messageId
		doc.setMessageId(parsedMessage.getMessageId());
		
		//recipientEmail
		doc.setRecipientEmail(conf.getEmail());
		
		//sent date
		doc.setSentDate(parsedMessage.getSentDate());
		
		//annullato
		doc.setAnnullato(false);
		
		//oggetto
		doc.setOggetto(oggetto);
		
		//tipologia
		doc.setTipologia(conf.getTipologiaSegnatura());
		
		//mezzo trasmissione
		doc.setMezzoTrasmissione(conf.getMezzoTrasmissioneSegnatura());
		
		//rif esterno
		RifEsterno rifEsterno = new RifEsterno();
		doc.addRifEsterno(rifEsterno);
        rifEsterno.setCodiceAmministrazione(codiceAmministrazione);
        rifEsterno.setCodiceAOO(codiceAOO);
        rifEsterno.setDataProt(new SimpleDateFormat("yyyyMMdd").format(dataProtD));
        rifEsterno.setnProt(dataProt.substring(0, 4) + "-" + codiceAmministrazione + codiceAOO + "-" + nProt);
		
        //rif esterno: denominazione mittente
        String denominazione = "";
        String path = "/Segnatura/Intestazione/Origine/Mittente/Amministrazione/Denominazione";
		for (int depth=0; depth<=5; depth++) {
			String value = "";
			if (segnaturaDocument.selectSingleNode(path) != null)
				value = segnaturaDocument.selectSingleNode(path).getText();
			if (value.isEmpty())
				break;
			denominazione += " - " + value;
			path += "/UnitaOrganizzativa/Denominazione";
		}
		if (!denominazione.isEmpty())
			denominazione = denominazione.substring(3);
		else
			denominazione = "[N/D]";
        rifEsterno.setNome(denominazione);
        
        //rif esterno: indirizzo, tel, fax, email, email_certificata
        String indirizzo = "";
        Attribute att = (Attribute)segnaturaDocument.selectSingleNode("/Segnatura/Intestazione/Origine/Mittente/Amministrazione/UnitaOrganizzativa/IndirizzoPostale/Toponimo/@dug");
        if (att != null && !att.getText().isEmpty())
        	indirizzo += att.getText();
        @SuppressWarnings("unchecked")
		List<Element> els = segnaturaDocument.selectNodes("/Segnatura/Intestazione/Origine/Mittente/Amministrazione/UnitaOrganizzativa/IndirizzoPostale/*");
        for (Element el:els) {
        	if (!el.getText().isEmpty()) {
        		if (!indirizzo.isEmpty())
        			indirizzo += " - ";
        		indirizzo += el.getText();
        	}
        }
        if (!indirizzo.trim().isEmpty())
        	rifEsterno.setIndirizzo(indirizzo.trim());
        rifEsterno.setEmailCertificata(emailCertificata);
        Element el = (Element)segnaturaDocument.selectSingleNode("/Segnatura/Intestazione/Origine/Mittente//IndirizzoTelematico[text()!='']");
        if (el != null)
        	rifEsterno.setEmail(el.getText());
        el = (Element)segnaturaDocument.selectSingleNode("/Segnatura/Intestazione/Origine/Mittente//Telefono[text()!='']");
        if (el != null)
        	rifEsterno.setTel(el.getText());        
        el = (Element)segnaturaDocument.selectSingleNode("/Segnatura/Intestazione/Origine/Mittente//Fax[text()!='']");
        if (el != null)
        	rifEsterno.setFax(el.getText());

        //rif esterno: referente
        String referente = "";
        el = (Element)segnaturaDocument.selectSingleNode("/Segnatura/Intestazione/Origine/Mittente/Amministrazione//Persona/Titolo[text()!='']");
        if (el != null)
        	referente += " " + el.getText();
        el = (Element)segnaturaDocument.selectSingleNode("/Segnatura/Intestazione/Origine/Mittente/Amministrazione//Persona/Cognome[text()!='']");
        if (el != null)
        	referente += " " + el.getText();
        el = (Element)segnaturaDocument.selectSingleNode("/Segnatura/Intestazione/Origine/Mittente/Amministrazione//Persona/Nome[text()!='']");
        if (el != null)
        	referente += " " + el.getText();
        if (!referente.trim().isEmpty())
        	rifEsterno.setReferenteNominativo(referente.trim());

        /*
		//classif
        String classif = "";
        el = (Element)segnaturaDocument.selectSingleNode("/Segnatura/Intestazione/Classifica/Denominazione");
        if (el != null && !el.getText().isEmpty()) {
        	classif = el.getText();
        	doc.setClassif(classif);
        	if (classif.indexOf(" ") > 0)
        		doc.setClassifCod(classif.substring(0, classif.indexOf(" ")));
        }
        */
        
        // mbernardini 01/07/2019 : archiviazione tramite TAGS
		// In caso di archiviazione tramite TAGS attiva potrebbe essere necessario applicare variazioni alla costruzione 
		// del documento in base a quanto previsto dal document model
		FascicoloReference fascicolo = null;
		if (conf.isArchiviazioneByTags() && parsedMessage.containsTags()) {
			fascicolo = this.findCodFascicoloByTags(conf.getCodAmmAoo(), parsedMessage.getSubjectTags());
			
			if (fascicolo != null && fascicolo.getCodFascicolo() != null && !fascicolo.getCodFascicolo().isEmpty()) {
				if (logger.isDebugEnabled())
					logger.debug("[" + conf.getAddress() + "] fascicolazione tramite TAGS. fascicolo = " + fascicolo.getCodFascicolo());
			}
			
			// TODO In caso di email PEC di interoperabilita' non e' necessario valutare il flusso del documento perche' dovrebbe sempre trattarsi di un documento in arrivo
		}
		
		//rif interni
		List<RifInterno> rifInterni = createRifInterni(parsedMessage);
		rifInterni = this.mergeRifInterniFascicolo(rifInterni, fascicolo); // eventuale archiviazione in fascicolo tramite TAGS estratti dall'oggetto del messaggio
		for (RifInterno rifInterno:rifInterni)
			doc.addRifInterno(rifInterno);
		
		//storia creazione
		StoriaItem creazione = new StoriaItem("creazione");
		creazione.setOper(conf.getOper());
		creazione.setUffOper(conf.getUffOper());
		creazione.setData(currentDate);
		creazione.setOra(currentDate);
		doc.addStoriaItem(creazione);
		
		//aggiunta in storia delle operazioni relative ai rif interni
		for (RifInterno rifInterno:rifInterni) {
			StoriaItem storiaItem = StoriaItem.createFromRifInterno(rifInterno);
			storiaItem.setOperatore(conf.getOperatore());
			storiaItem.setData(currentDate);
			storiaItem.setOra(currentDate);
			doc.addStoriaItem(storiaItem);
		}
		
		// mbernardini 19/12/2018 : spostato sopra il set della bozza perche' da questo metodo derivano eventuali errori sulla segnatura
		//files + immagini + allegato
		motivazioneNotificaEccezione += createDocwayFilesForInteropPAMessage(segnaturaDocument, parsedMessage, doc);		
		
		//bozza
		doc.setBozza(!conf.isProtocollaSegnatura() || !motivazioneNotificaEccezione.isEmpty());
		
		//anno
		doc.setAnno(!doc.isBozza()? (new SimpleDateFormat("yyyy")).format(currentDate) : "");
		
		//num_prot
		doc.setNumProt(!doc.isBozza()? buildNewNumprotStringForSavingDocument() : "");

		//motivazione -> relevant message
		if (!motivazioneNotificaEccezione.isEmpty()) {
			if (doc.isBozza())
				motivazioneNotificaEccezione = SEGNATURA_MESSAGE_AS_BOZZA + motivazioneNotificaEccezione;
			else
				motivazioneNotificaEccezione = SEGNATURA_PARSE_ERROR + motivazioneNotificaEccezione;

			dcwParsedMessage.addRelevantMessage(motivazioneNotificaEccezione);
			dcwParsedMessage.setMotivazioneNotificaEccezioneToSend(motivazioneNotificaEccezione);
		}
		
		//parsedMessage.relevantMessages -> postit
		for (String relevantMessage:parsedMessage.getRelevantMssages()) {
			Postit postit = new Postit();
			postit.setText(relevantMessage);
			postit.setOperatore(conf.getOperatore());
			postit.setData(currentDate);
			postit.setOra(currentDate);
			doc.addPostit(postit);
		}		
		
		return doc;
	}	

	/**
	 * Creazione di un documento ricevuto tramite interoperabilità (parsing del file segnatura.xml)
	 * @param segnaturaDocument
	 * @param parsedMessage
	 * @param doc
	 * @return
	 * @throws Exception
	 */
	private String createDocwayFilesForInteropPAMessage(Document segnaturaDocument, ParsedMessage parsedMessage, DocwayDocument doc) throws Exception {
		String motivazioneNotificaEccezione = "";
		
		//get rif esterno
		RifEsterno rifEsterno = doc.getRifEsterni().get(0);
		
		//Segnatura.xml
		Part attachment = parsedMessage.getFirstAttachmentByName("Segnatura.xml");
		InteroperabilitaItem interopItem = new InteroperabilitaItem();
		interopItem.setName("Segnatura.xml");
		interopItem.setData(currentDate);
		interopItem.setOra(currentDate);
		interopItem.setInfo("Ricezione Telematica (Segnatura.xml)");
		interopItem.setMessageId(parsedMessage.getMessageId());
		interopItem.setContentProvider(new PartContentProvider(attachment));
		rifEsterno.addInteroperabilitaItem(interopItem);		
		
		//doc principale
		motivazioneNotificaEccezione += addFileFromSegnatura((Element)segnaturaDocument.selectSingleNode("/Segnatura/Descrizione/Documento"), doc, parsedMessage, false);
		
		//allegati
		@SuppressWarnings("unchecked")
		List<Element> documentoElL = segnaturaDocument.selectNodes("/Segnatura/Descrizione/Allegati/Documento");
		if (documentoElL != null) {
			for (Element documentoEl:documentoElL)
				motivazioneNotificaEccezione += addFileFromSegnatura(documentoEl, doc, parsedMessage, true);
		}

		//EML
		interopItem = new InteroperabilitaItem();
		interopItem.setName("Ricezione telematica.eml");
		interopItem.setData(currentDate);
		interopItem.setOra(currentDate);
		interopItem.setInfo("Ricezione Telematica");
		interopItem.setMessageId(parsedMessage.getMessageId());
		interopItem.setContentProvider(new MessageContentProvider(parsedMessage.getMessage(), false));
		rifEsterno.addInteroperabilitaItem(interopItem);
		
		//allegato - default
		if (doc.getAllegato().isEmpty())
			doc.addAllegato(DEFAULT_ALLEGATO);
		
		return motivazioneNotificaEccezione;
	}	
	
	/**
	 * Caricamento sul documento di un file specificato dalla segnatura.xml
	 * @param documentoEl
	 * @param doc
	 * @param parsedMessage
	 * @param addAllegato
	 * @return
	 * @throws Exception
	 */
	private String addFileFromSegnatura(Element documentoEl, DocwayDocument doc, ParsedMessage parsedMessage, boolean addAllegato) throws Exception {
		if (documentoEl != null && !documentoEl.attributeValue("nome", "").isEmpty()) {
			Part attachment = parsedMessage.getFirstAttachmentByName(documentoEl.attributeValue("nome"));
			if (attachment != null) {
				DocwayFile file = createDocwayFile();
				
				// mbernardini 29/03/2019 : gestione di messaggi di interoperabilita' contenenti allegati danneggiati
				boolean fileLoaded = false;
				try {
					file.setContentByProvider(new PartContentProvider(attachment));
					fileLoaded = true;
				}
				catch(Exception e) {
					logger.warn("[" + attachment.getFileName() + "] Unable to read file content, damaged file... " + e.getMessage(), e);
				}
				
				if (fileLoaded) {
					file.setName(attachment.getFileName());
					if (isImage(file.getName())) //immagine
							doc.addImmagine(file);
					else //file
						doc.addFile(file);
					
					if (addAllegato)
						doc.addAllegato(file.getName());	
					return "";
				}
				else {
					// Possibile file corrotto, invio della notifica di eccezione
					return String.format(DAMAGED_FILE_IN_SEGNATURA, documentoEl.attributeValue("nome"));					
				}
			}
			else if (documentoEl.attributeValue("TipoRiferimento", "MIME").equals("MIME"))
				return String.format(FILE_NOT_FOUND_IN_SEGNATURA, documentoEl.attributeValue("nome"));
		}	
		return "";
	}
	
	protected void sendConfermaRicezioneInteropPAMessage(ParsedMessage parsedMessage, DocwayDocument doc, String codRegistro, String numProt, String dataProt, String subject) throws Exception {
		DocwayMailboxConfiguration conf = (DocwayMailboxConfiguration)getConfiguration();
		
		if (logger.isDebugEnabled())
			logger.debug("[" + conf.getAddress() + "] invio della conferma di ricezione per il messaggio di Interoperabilita'. messageId = " + parsedMessage.getMessageId());
		
		DocwayParsedMessage dcwParsedMessage = (DocwayParsedMessage)parsedMessage;
		Document segnaturaDocument = dcwParsedMessage.getSegnaturaInteropPADocument();
		
		Element confermaRicezioneEl = DocumentHelper.createElement("ConfermaRicezione");
		Document confermaRicezioneDoc = DocumentHelper.createDocument(confermaRicezioneEl);		
		
		//ConfermaRicezione/Identificatore
		confermaRicezioneEl.add(createIdentificatoreEl(conf.getCodAmmInteropPA(), conf.getCodAooInteropPA(), codRegistro, numProt, dataProt));
	
		//ConfermaRicezione/MessaggioRicevuto/Identificatore
		Element messaggioRicevutoEl = DocumentHelper.createElement("MessaggioRicevuto");
		confermaRicezioneEl.add(messaggioRicevutoEl);	
		Element idEl = (Element)segnaturaDocument.selectSingleNode("/Segnatura/Intestazione/Identificatore");
		messaggioRicevutoEl.add(createIdentificatoreEl(idEl.elementText("CodiceAmministrazione"), idEl.elementText("CodiceAOO"), idEl.elementText("CodiceRegistro"), idEl.elementText("NumeroRegistrazione"), idEl.elementText("DataRegistrazione")));
		
		//send interopPA email - Conferma.xml
		RifEsterno rifEsterno = doc.getRifEsterni().get(0);
		MimeBodyPart part = MailClientHelper.createMimeBodyPart(Utils.dom4jdocumentToString(confermaRicezioneDoc, "UTF-8", false).getBytes("UTF-8"), "application/xml", "Conferma.xml", false);
		MimeBodyPart []mimeBodyParts = {part};
        mailSender.sendMail(conf.getSmtpEmail(), "", rifEsterno.getEmailCertificata(), null, "Conferma Ricezione: " + subject, mimeBodyParts);

        //add Conferma.xml attachment to rif esterno
		InteroperabilitaItem interopItem = new InteroperabilitaItem();
		interopItem.setName("Conferma.xml");
		interopItem.setData(currentDate);
		interopItem.setOra(currentDate);
		interopItem.setInfo("Invio Conferma Ricezione (Conferma.xml)");
		interopItem.setMessageId("");
		interopItem.setContentProvider(new PartContentProvider(part));
		rifEsterno.addInteroperabilitaItem(interopItem);
	}
	
	private Element createIdentificatoreEl(String codAmm, String codAoo, String codRegistro, String numProt, String dataProt) {
		Element idEl = DocumentHelper.createElement("Identificatore");
		
		//CodiceAmministrazione
		Element codAmmEl = DocumentHelper.createElement("CodiceAmministrazione");
		codAmmEl.setText(codAmm);
		idEl.add(codAmmEl);
		
		//CodiceAOO
		Element codAooEl = DocumentHelper.createElement("CodiceAOO");
		codAooEl.setText(codAoo);
		idEl.add(codAooEl);
		
		//CodiceRegistro
		if (codRegistro != null) {
			Element codRegistroEl = DocumentHelper.createElement("CodiceRegistro");
			codRegistroEl.setText(codRegistro);
			idEl.add(codRegistroEl);			
		}
		
		//NumeroRegistrazione
		Element numProtEl = DocumentHelper.createElement("NumeroRegistrazione");
		numProtEl.setText(numProt);
		idEl.add(numProtEl);
		
		//DataRegistrazione
		Element dataProtEl = DocumentHelper.createElement("DataRegistrazione");
		dataProtEl.setText(dataProt);
		idEl.add(dataProtEl);		
		
		return idEl;
	}
	
	protected void sendNotificaEccezioneInteropPAMessage(ParsedMessage parsedMessage, DocwayDocument doc, String codRegistro, String numProt, String dataProt, String subject, String motivazioneNotificaEccezione) throws Exception {
		DocwayMailboxConfiguration conf = (DocwayMailboxConfiguration)getConfiguration();
		
		if (logger.isDebugEnabled())
			logger.debug("[" + conf.getAddress() + "] invio della notifica di eccezione per il messaggio di Interoperabilita'. messageId = " + parsedMessage.getMessageId());
		
		DocwayParsedMessage dcwParsedMessage = (DocwayParsedMessage)parsedMessage;
		Document segnaturaDocument = dcwParsedMessage.getSegnaturaInteropPADocument();		
		
		Element notificaEccezioneEl = DocumentHelper.createElement("NotificaEccezione");
		Document eccezioneDoc = DocumentHelper.createDocument(notificaEccezioneEl);		
		
		//NotificaEccezione/Identificatore
		if (numProt != null)
			notificaEccezioneEl.add(createIdentificatoreEl(conf.getCodAmmInteropPA(), conf.getCodAooInteropPA(), codRegistro, numProt, dataProt));
	
		//NotificaEccezione/MessaggioRicevuto/Identificatore
		Element messaggioRicevutoEl = DocumentHelper.createElement("MessaggioRicevuto");
		notificaEccezioneEl.add(messaggioRicevutoEl);	
		Element idEl = (Element)segnaturaDocument.selectSingleNode("/Segnatura/Intestazione/Identificatore");
		messaggioRicevutoEl.add(createIdentificatoreEl(idEl.elementText("CodiceAmministrazione"), idEl.elementText("CodiceAOO"), idEl.elementText("CodiceRegistro"), idEl.elementText("NumeroRegistrazione"), idEl.elementText("DataRegistrazione")));
		
		//NotificaEccezione/Motivo
		Element motivoEl = DocumentHelper.createElement("Motivo");
		motivoEl.setText(motivazioneNotificaEccezione);
		notificaEccezioneEl.add(motivoEl);
		
		//send interopPA email - Eccezione.xml
		RifEsterno rifEsterno = doc.getRifEsterni().get(0);
		MimeBodyPart part = MailClientHelper.createMimeBodyPart(Utils.dom4jdocumentToString(eccezioneDoc, "UTF-8", false).getBytes("UTF-8"), "application/xml", "Eccezione.xml", false);
		MimeBodyPart []mimeBodyParts = {part};
        mailSender.sendMail(conf.getSmtpEmail(), "", rifEsterno.getEmailCertificata(), null, "Notifica Eccezione: " + subject, mimeBodyParts);

        //add Eccezione.xml attachment to rif esterno
		InteroperabilitaItem interopItem = new InteroperabilitaItem();
		interopItem.setName("Eccezione.xml");
		interopItem.setData(currentDate);
		interopItem.setOra(currentDate);
		interopItem.setInfo("Invio Notifica Eccezione (Eccezione.xml)");
		interopItem.setMessageId("");
		interopItem.setContentProvider(new PartContentProvider(part));
		rifEsterno.addInteroperabilitaItem(interopItem);
	}

	private DocwayDocument createDocwayDocumentByFatturaPAMessage(ParsedMessage  parsedMessage) throws Exception {
		DocwayMailboxConfiguration conf = (DocwayMailboxConfiguration) getConfiguration();
		
		if (logger.isDebugEnabled())
			logger.debug("[" + conf.getAddress() + "] creazione del documento da messaggio FatturaPA. messageId = " + parsedMessage.getMessageId());
		
		DocwayParsedMessage dcwParsedMessage = (DocwayParsedMessage)parsedMessage;
		Document fatturaPADocument = dcwParsedMessage.getFatturaPADocument();
		Document fileMetadatiDocument = dcwParsedMessage.getFileMetadatiDocument();

		//costruzione standard da document model
		DocwayDocument doc = createDocwayDocumentByMessage(parsedMessage);
		
		if (doc.getTipo().equalsIgnoreCase(DocwayMailboxConfiguration.DOC_TIPO_ARRIVO)) {
			doc.setAnno((new SimpleDateFormat("yyyy")).format(currentDate));
			
			//repertorio fatturaPA
			doc.setRepertorio(conf.getRepertorioFtrPA());
			doc.setRepertorioCod(conf.getRepertorioCodFtrPA());
			
			//bozza, num_prot, num_rep
			if (conf.isProtocollaFattura()) {
				doc.setRepertorioNum(buildNewNumrepStringForSavingDocument(doc.getRepertorioCod()));
				if (doc.isBozza()) {
		    		doc.setBozza(false);
		    		doc.setDataProt((new SimpleDateFormat("yyyyMMdd")).format(currentDate));
		    		doc.setNumProt(buildNewNumprotStringForSavingDocument());					
				}
			}
		    
	    	//classif fatturaPA
	    	doc.setClassif(conf.getClassifFtrPA());
	    	doc.setClassifCod(conf.getClassifCodFtrPA());
	    	
	    	//voce indice fatturaPA
	    	if (!conf.getVoceIndiceFtrPA().isEmpty())
	    		doc.setVoceIndice(conf.getVoceIndiceFtrPA());
		    
		    // gestione del mittente del documento:
		    // Occorre individuare il mittente della fattura, ricercarlo in ACL. Se il mittente e' presente in ACL lo si assegna
		    // direttamente al documento, in caso contrario, prima lo si inserisce in ACL e successivamento lo si assegna
		    // al documento.
	    	doc.getRifEsterni().clear();
	    	doc.addRifEsterno(createMittenteFatturaPA(parsedMessage));

		    //oggetto
	    	OggettoParseMode oggettoParseMode = conf.getOggettoParseMode();
			if (oggettoParseMode != null && oggettoParseMode != OggettoParseMode.NO_OVERWRITE) {
				doc.setOggetto(FatturaPAUtils.getOggettoFatturaPA(fatturaPADocument, false, new OggettoDocumentoBuilder(oggettoParseMode, conf.getTemplateOggetto())));
			}

		    FatturaPAItem fatturaPAItem = new FatturaPAItem();
		    doc.setFatturaPA(fatturaPAItem);
			fatturaPAItem.setState(FatturaPAUtils.ATTESA_NOTIFICHE); // stato della fattura / lotto di fatture
		    if (dcwParsedMessage.getSentDate() != null)
		    	fatturaPAItem.setSendDate(dcwParsedMessage.getSentDate());
		    fatturaPAItem.setVersione(FatturaPAUtils.getVersioneFatturaPA(fatturaPADocument)); // versione di fatturaPA
		    
		    String emailFrom = dcwParsedMessage.getMittenteAddressFromDatiCertPec();
			if (emailFrom != null)
				fatturaPAItem.setEmailSdI(emailFrom);

			fatturaPAItem.setEmailToFattPassiva(conf.getEmail());
		    
		    //aggiungo i dati estratti dal file dei metadati
		    FatturaPAUtils.appendDatiFileMetadatiToDocument(fileMetadatiDocument, fatturaPAItem);
            
		    //aggiungo i dati estratti dal file di fattura elettronica
		    FatturaPAUtils.appendDatiFatturaToDocument(fatturaPADocument, fatturaPAItem);
		}
		
		//aggiunta allegati contenuti nella fattura
		int index = 0;
		for (List<Object> fileAttrsL: FatturaPAUtils.getAllegatiFatturaPA(fatturaPADocument)) {
			DocwayFile file = createDocwayFile();
			file.setName((String)fileAttrsL.get(0));
			file.setFromFatturaPA(true);
			file.setContentByProvider((ContentProvider)fileAttrsL.get(1));					
			doc.addFile(index++, file);			
		}
		
		return doc;
	}	

	@Override
    public void messageStored(ParsedMessage parsedMessage) throws Exception {
    	if (!ignoreMessage)
    		super.messageStored(parsedMessage);
    	else
    		// mbernardini 25/01/2019 : registrazione nell'audit di msa dello skip
    		super.messageSkipped(parsedMessage); // in realta' il messaggio risulta skippato
    }
	
}
