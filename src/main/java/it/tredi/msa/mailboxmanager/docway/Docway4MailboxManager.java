package it.tredi.msa.mailboxmanager.docway;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.mail.internet.InternetAddress;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;

import it.highwaytech.db.QueryResult;
import it.tredi.extraway.ExtrawayClient;
import it.tredi.extraway.LockedDocument;
import it.tredi.mail.MailSender;
import it.tredi.msa.MailboxesManagersMap;
import it.tredi.msa.Services;
import it.tredi.msa.configuration.docway.AssegnatarioMailboxConfiguration;
import it.tredi.msa.configuration.docway.Docway4MailboxConfiguration;
import it.tredi.msa.configuration.docway.DocwayMailboxConfiguration;
import it.tredi.msa.mailboxmanager.MailboxManager;
import it.tredi.msa.mailboxmanager.MessageContentProvider;
import it.tredi.msa.mailboxmanager.ParsedMessage;
import it.tredi.msa.mailboxmanager.docway.exception.MultipleFoldersException;
import it.tredi.msa.mailboxmanager.docway.fatturapa.ErroreItem;
import it.tredi.msa.mailboxmanager.docway.fatturapa.NotificaItem;
import it.tredi.msa.mailboxmanager.docway.fatturapa.utils.FatturaPAUtils;
import it.tredi.msa.mailboxmanager.utils.DateUtils;
import it.tredi.msa.notification.MailNotificationSender;
import it.tredi.msa.notification.NotificationSender;

/**
 * Estensione della gestione delle mailbox (lettura, elaborazione messaggi, ecc.) specifica per DocWay4 (es. chiamate eXtraWay)
 */
public class Docway4MailboxManager extends DocwayMailboxManager {

	protected ExtrawayClient xwClient;
	protected ExtrawayClient aclClient;
	protected boolean extRestrictionsOnAcl;
	private int physDocToUpdate;
	private int physDocForAttachingFile;
	
	private static final Logger logger = LogManager.getLogger(Docway4MailboxManager.class.getName());
	
	private final static String STANDARD_DOCUMENT_STORAGE_BASE_MESSAGE = "Il messaggio è stato archiviato come documento ordinario: ";
	private final static String DOC_NOT_FOUND_FOR_ATTACHING_FILE = STANDARD_DOCUMENT_STORAGE_BASE_MESSAGE + "non è stato possibile individuare il documento a cui associare la ricevuta/notifica. \n%s";
	private final static String INVIO_INTEROP_PA_MESSAGE_FAILED = "Non è stato possibile inviare il messaggio di %s di interoperabilità tra PA a causa di un errore: \n%s";
	private final static String DOC_NOT_FOUND_FOR_ATTACHING_NOTIFICA_PA_FILE = "Non è stato possibile individuare il documento a cui associare la notifica di Fattura PA. \n%s";
	
	@Override
    public void openSession() throws Exception {
		super.openSession();
		
		Docway4MailboxConfiguration conf = (Docway4MailboxConfiguration)getConfiguration();
		
		// mbernardini 17/12/2018 : aggiunta del riferimento al thread corrente allo username dell'utente per xw
		// Eliminazione di possibili errori di "Protezione file non riuscita" dovuta alla gestione multithread delle caselle di posta
		String xwUser = conf.getXwUser();
		try {
			String threadName = Thread.currentThread().getName();
			int index = threadName.indexOf("thread-");
			if (index != -1)
				threadName = threadName.substring(index);
			xwUser = xwUser + "." + threadName;
			if (logger.isInfoEnabled())
				logger.info("Add current thread name to xway user... xwUser = " + xwUser);
		}
		catch(Exception e) {
			logger.error("Unable to append thread name to xway user [xwUser = " + xwUser + "]... " + e.getMessage(), e);
		}
		
		xwClient = new ExtrawayClient(conf.getXwHost(), conf.getXwPort(), conf.getXwDb(), xwUser, conf.getXwPassword());
		xwClient.connect();
		aclClient = new ExtrawayClient(conf.getXwHost(), conf.getXwPort(), conf.getAclDb(), xwUser, conf.getXwPassword());
		aclClient.connect();
		extRestrictionsOnAcl = checkExtRestrictionsOnAcl();
    }
	
	@Override
    public void closeSession() {
		Docway4MailboxConfiguration conf = (Docway4MailboxConfiguration)getConfiguration();
    	super.closeSession();
		try {
			if (xwClient != null)
				xwClient.disconnect();
		}
		catch (Exception e) {
			logger.warn("[" + conf.getAddress() + "] failed to close eXtraWay session [" + conf.getXwDb() + "]", e);			
		}
		try {
			if (aclClient != null)
				aclClient.disconnect();
		}
		catch (Exception e) {
			logger.warn("[" + conf.getAddress() + "] failed to close eXtraWay session [" + conf.getAclDb() + "]", e);
		}
	}
	
	@Override
	protected StoreType decodeStoreType(ParsedMessage parsedMessage) throws Exception {
		StoreType storeType = null;
		Docway4MailboxConfiguration conf = (Docway4MailboxConfiguration)getConfiguration();
		DocwayParsedMessage dcwParsedMessage = (DocwayParsedMessage)parsedMessage;
		
		// mbernardini 18/04/2019 : corretto bug nel salvataggio delle ricevute PEC orfane come doc non protocollati (caso di email salvate parzialmente o gia' presenti ma non eliminate dalla cartella inbox)
		boolean isPecReceiptAsVarie = false;
		
		if (conf.isPec()) { //casella PEC
			
			if (dcwParsedMessage.isPecReceipt() || dcwParsedMessage.isNotificaInteropPAMessage(conf.getCodAmmInteropPA(), conf.getCodAooInteropPA()) ||
					(conf.isEnableFatturePA() && dcwParsedMessage.isNotificaFatturaPAMessage(conf.getSdiDomainAddress()))) { //messaggio è una ricevuta PEC oppure è una notifica (messaggio di ritorno) di interoperabilità PA oppure è una notifica di fatturaPA
				String query = "([/doc/rif_esterni/rif/interoperabilita/@messageId]=\"" + dcwParsedMessage.getMessageId() + "\" OR [/doc/rif_esterni/interoperabilita_multipla/interoperabilita/@messageId]=\"" + dcwParsedMessage.getMessageId() + "\""
						+ " OR [/doc/extra/fatturaPA/notifica/@messageId]=\"" + dcwParsedMessage.getMessageId() + "\") AND [/doc/@cod_amm_aoo/]=\"" + conf.getCodAmmAoo() + "\"";
				QueryResult qr = xwClient.search(query);
				if (qr.elements > 0)
					return StoreType.SKIP_DOCUMENT;
					
				query = "";
				if (dcwParsedMessage.isPecReceiptForInteropPA(conf.getCodAmmInteropPA(), conf.getCodAooInteropPA())) { //1st try: individuazione ricevuta PEC di messaggio di interoperabilità (tramite identificazione degli allegati del messaggio originale)
					query = dcwParsedMessage.buildQueryForDocway4DocumentFromInteropPAPecReceipt(conf.getCodAmm(), conf.getCodAoo());
					storeType = StoreType.ATTACH_INTEROP_PA_PEC_RECEIPT;
				}
				if (query.isEmpty() && dcwParsedMessage.isPecReceiptForInteropPAbySubject()) { //2nd try: non sempre nelle ricevute è presente il messaggio originale -> si cerca il numero di protocollo nel subject
					query = dcwParsedMessage.buildQueryForDocway4DocumentFromInteropPASubject();
					storeType = StoreType.ATTACH_INTEROP_PA_PEC_RECEIPT;
				}
				else if (dcwParsedMessage.isPecReceiptForFatturaPAbySubject()) { //ricevuta PEC di messaggio per la fatturaPA
					query = dcwParsedMessage.buildQueryForDocway4DocumentFromFatturaPASubject();
					storeType = StoreType.ATTACH_FATTURA_PA_PEC_RECEIPT;
				}
				else if (dcwParsedMessage.isNotificaInteropPAMessage(conf.getCodAmmInteropPA(), conf.getCodAooInteropPA())) { //notifia di interoperabilità PA
					query = dcwParsedMessage.buildQueryForDocway4DocumentFromInteropPANotification(conf.getCodAmm(), conf.getCodAoo());
					storeType = StoreType.ATTACH_INTEROP_PA_NOTIFICATION;
				}
				else if (dcwParsedMessage.isNotificaFatturaPAMessage(conf.getSdiDomainAddress())) { //notifia di fattura PA
					query = dcwParsedMessage.buildQueryForDocway4DocumentFromFatturaPANotification();
					QueryResult qr1 = xwClient.search(query);
					int count = qr1.elements;
					for (int i=0; i<count; i++) {
						Document xmlDocument = xwClient.loadDocByQueryResult(i, qr1);
						Node node = xmlDocument.selectSingleNode("/doc/extra/fatturaPA[@fileNameFattura='" + dcwParsedMessage.getFileNameFatturaRiferita() + "']");
						if (node != null) {
							this.physDocForAttachingFile = xwClient.getPhysdocByQueryResult(i, qr1);
							return StoreType.ATTACH_FATTURA_PA_NOTIFICATION;
						}
					}
					throw new Exception(String.format(DOC_NOT_FOUND_FOR_ATTACHING_NOTIFICA_PA_FILE, query));
				}
				
				if (query.length() > 0) { //trovato doc a cui allegare file
					QueryResult qr1 = xwClient.search(query);
					int count = qr1.elements;
					if (count > 0) {
						this.physDocForAttachingFile = xwClient.getPhysdocByQueryResult(0, qr1);
						return storeType;
					}
					else {
						dcwParsedMessage.addRelevantMessage(String.format(DOC_NOT_FOUND_FOR_ATTACHING_FILE, query));
						
						// mbernardini 18/04/2019 : anche su questa tipologia di notifiche deve essere verificata la configurazione
						// di salvataggio come doc non protocollato
						if (conf.isOrphanPecReceiptsAsVarie()) {
							isPecReceiptAsVarie = true;
						}
					}
				}
				else if (dcwParsedMessage.isPecReceipt()) { //ricevuta PEC (non relativa a interopPA/fatturaPA)
					if (conf.isIgnoreStandardOrphanPecReceipts()) {
						// property attiva per evitare l'archiviazione -> il messaggio viene ignorato e rimane sulla casella di posta
						return StoreType.IGNORE_MESSAGE;
					}
					// mbernardini 21/01/2019 : se il salvataggio riguarda ricevute orfane potrebbe essere stato richiesto il salvataggio
					// come documento non protocollato
					else if (conf.isOrphanPecReceiptsAsVarie()) {
						isPecReceiptAsVarie = true;
					}
				}
			}
			else if (dcwParsedMessage.isSegnaturaInteropPAMessage(conf.getCodAmmInteropPA(), conf.getCodAooInteropPA())) { //messaggio di segnatura di interoperabilità PA
				String query = "[/doc/@messageId]=\"" + parsedMessage.getMessageId() + "\" AND [/doc/@cod_amm_aoo]=\"" + conf.getCodAmmAoo() + "\"";
				QueryResult qr = xwClient.search(query);
				if (qr.elements == 0) { //2nd try: potrebbe essere la stessa segnatura su messaggi diversi
					query = dcwParsedMessage.buildQueryForDocway4DocumentFromInteropPASegnatura(conf.getCodAmm(), conf.getCodAoo());
					if (query != null && !query.isEmpty())
						qr = xwClient.search(query);
				}

				if (qr.elements > 0) { //messageId found
					Document xmlDocument = xwClient.loadDocByQueryResult(0, qr);
					Element archiviatoreEl = (Element)xmlDocument.selectSingleNode("/doc/archiviatore[@recipientEmail='" + conf.getEmail() + "']");
					if (archiviatoreEl != null) { //same mailbox
						if (archiviatoreEl.attribute("completed") != null && archiviatoreEl.attributeValue("completed").equals("no")) {
							this.physDocToUpdate = xwClient.getPhysdocByQueryResult(0, qr);
							return StoreType.UPDATE_PARTIAL_DOCUMENT_INTEROP_PA;
						}
						else
							return StoreType.SKIP_DOCUMENT;						
					}
					else { //different mailbox
						this.physDocToUpdate = xwClient.getPhysdocByQueryResult(0, qr);
						return StoreType.UPDATE_NEW_RECIPIENT_INTEROP_PA;
					}
				}
				else //messageId not found
					return StoreType.SAVE_NEW_DOCUMENT_INTEROP_PA;
			}
			else if (conf.isEnableFatturePA() && dcwParsedMessage.isFatturaPAMessage(conf.getSdiDomainAddress())) { //messaggio fattura PA
				String query = "[/doc/@messageId]=\"" + parsedMessage.getMessageId() + "\" AND [/doc/@cod_amm_aoo]=\"" + conf.getCodAmmAoo() + "\"";
				QueryResult qr = xwClient.search(query);
				if (qr.elements > 0) { //messageId found
					Document xmlDocument = xwClient.loadDocByQueryResult(0, qr);
					Element archiviatoreEl = (Element)xmlDocument.selectSingleNode("/doc/archiviatore[@recipientEmail='" + conf.getEmail() + "']");
					if (archiviatoreEl.attribute("completed") != null && archiviatoreEl.attributeValue("completed").equals("no")) {
						this.physDocToUpdate = xwClient.getPhysdocByQueryResult(0, qr);
						return StoreType.UPDATE_PARTIAL_DOCUMENT_FATTURA_PA;
					}
					else
						return StoreType.SKIP_DOCUMENT;
				}
				else //messageId not found
					return StoreType.SAVE_NEW_DOCUMENT_FATTURA_PA;
			}

		}
		
		//TODO - inserire altre casistiche	(segnatura, notifiche interoperabilità, fattura pa, notifiche fattura PA)
		//casella ordinaria oppure casella PEC ma messaggio ordinario (oppure casella PEC ma non trovato documento a cui allegare ricevuta PEC o notifica di interoperabilità PA)
		String query = "[/doc/@messageId]=\"" + parsedMessage.getMessageId() + "\" AND [/doc/@cod_amm_aoo]=\"" + conf.getCodAmmAoo() + "\"";
		if (!conf.isCreateSingleDocByMessageId())
			query += " AND [/doc/archiviatore/@recipientEmail]=\"" + conf.getEmail() + "\"";
		
		QueryResult qr = xwClient.search(query);
		if (qr.elements > 0) { //messageId found
			Document xmlDocument = xwClient.loadDocByQueryResult(0, qr);
			Element archiviatoreEl = (Element)xmlDocument.selectSingleNode("/doc/archiviatore[@recipientEmail='" + conf.getEmail() + "']");
			if (archiviatoreEl != null) { //same mailbox
				if (archiviatoreEl.attribute("completed") != null && archiviatoreEl.attributeValue("completed").equals("no")) {
					this.physDocToUpdate = xwClient.getPhysdocByQueryResult(0, qr);
					return StoreType.UPDATE_PARTIAL_DOCUMENT;
				}
				else
					return StoreType.SKIP_DOCUMENT;
			}
			else { //different mailbox
				this.physDocToUpdate = xwClient.getPhysdocByQueryResult(0, qr);
				return StoreType.UPDATE_NEW_RECIPIENT;
			}
		}
		else {
			//messageId not found
			if (isPecReceiptAsVarie)
				return StoreType.SAVE_ORPHAN_PEC_RECEIPT_AS_VARIE;
			else
				return StoreType.SAVE_NEW_DOCUMENT;
		}
	}  	
	
	@Override
	protected Object saveNewDocument(DocwayDocument doc, ParsedMessage parsedMessage) throws Exception {
		Docway4MailboxConfiguration conf = (Docway4MailboxConfiguration)getConfiguration();
		DocwayParsedMessage dcwParsedMessage = (DocwayParsedMessage)parsedMessage;
		
		//save new document in Extraway
		Document xmlDocument = Docway4EntityToXmlUtils.docwayDocumentToXml(doc, super.currentDate, conf.getAspettoClassificazione());
		int lastSavedDocumentPhysDoc = xwClient.saveNewDocument(xmlDocument);
		parsedMessage.clearRelevantMessages();
		
		//load and lock document
		LockedDocument lockedDoc = xwClient.loadAndLockDocument(lastSavedDocumentPhysDoc, conf.getXwLockOpAttempts(), conf.getXwLockOpDelay());
		xmlDocument = lockedDoc.getDoc();

		if (conf.isPec() && dcwParsedMessage.isSegnaturaInteropPAMessage(conf.getCodAmmInteropPA(), conf.getCodAooInteropPA())) { //casella PEC e interopPA Segnatura message

			//invio conferma ricezione - se doc è stato protocollato (anche se non richiesto dal mittente)
			//circolare_23_gennaio_2013_n.60_segnatura_protocollo_informatico_-_rev_aipa_n.28-2001
			//"In attuazione del principio generale della trasparenza dell’azione amministrativa sarebbe comunque opportuno inviare sempre in automatico il messaggio di conferma di ricezione."
			if (conf.isProtocollaSegnatura()) {
				String numProt = xmlDocument.selectSingleNode("/doc/@num_prot").getText();
				if (!numProt.isEmpty()) { //check se è stato realmente protocollato
					try { 
						String emailSubject = numProt + "(0) " + getOggettoForEmailSubject(xmlDocument.selectSingleNode("/doc/oggetto").getText());
				        Date dataProtD = new SimpleDateFormat("yyyyMMdd").parse(xmlDocument.selectSingleNode("/doc/@data_prot").getText());
						sendConfermaRicezioneInteropPAMessage(parsedMessage, doc, "PROTOCOLLO", numProt.substring(numProt.lastIndexOf("-") + 1), new SimpleDateFormat("yyyy-MM-dd").format(dataProtD), emailSubject);
					}
					catch (Exception e) {
						logger.error("[" + conf.getAddress() + "] error sending ConfermaRicezione InteropPA message.", e);
						parsedMessage.addRelevantMessage(String.format(INVIO_INTEROP_PA_MESSAGE_FAILED, "Conferma Ricezione", e.getMessage()));
					}				
				}				
			}
			
			//invio notifica eccezione
			if (dcwParsedMessage.getMotivazioneNotificaEccezioneToSend() != null && !dcwParsedMessage.getMotivazioneNotificaEccezioneToSend().isEmpty()) {
				String numProt = xmlDocument.selectSingleNode("/doc/@num_prot").getText();
				String numero = numProt.isEmpty()? xmlDocument.selectSingleNode("/doc/@nrecord").getText() : numProt;
				try {
					String emailSubject = numero + "(0) " + getOggettoForEmailSubject(xmlDocument.selectSingleNode("/doc/oggetto").getText());
					Date dataProtD = new SimpleDateFormat("yyyyMMdd").parse(xmlDocument.selectSingleNode("/doc/@data_prot").getText());
					sendNotificaEccezioneInteropPAMessage(parsedMessage, doc, "PROTOCOLLO", numProt.isEmpty() ? null : numProt.substring(numProt.lastIndexOf("-") + 1), new SimpleDateFormat("yyyy-MM-dd").format(dataProtD), emailSubject, dcwParsedMessage.getMotivazioneNotificaEccezioneToSend());
				}
				catch (Exception e) {
					logger.error("[" + conf.getAddress() + "] error sending NotificaEccezione InteropPA message.", e);
					parsedMessage.addRelevantMessage(String.format(INVIO_INTEROP_PA_MESSAGE_FAILED, "Notifica Eccezione", e.getMessage()));
				}			
			}
			
			//se occorre inserire i postit con i relevantMessages
			if (!dcwParsedMessage.getRelevantMssages().isEmpty()) {
				for (String message: dcwParsedMessage.getRelevantMssages()) {
					Postit postit = new Postit();
					postit.setText(message);
					postit.setOperatore(conf.getOperatore());
					postit.setData(currentDate);
					postit.setOra(currentDate);
					xmlDocument.getRootElement().add(Docway4EntityToXmlUtils.postitToXml(postit));
				}
				
				//salvataggio immediato
				xwClient.saveDocument(xmlDocument, lastSavedDocumentPhysDoc, lockedDoc.getTheLock());
				lockedDoc = xwClient.loadAndLockDocument(lastSavedDocumentPhysDoc, conf.getXwLockOpAttempts(), conf.getXwLockOpDelay());
				xmlDocument = lockedDoc.getDoc();
			}
			

		}
		
		try {
			boolean uploaded = false;
			
			//upload interopPA message files
			if (doc.getRifEsterni().size() > 0) {
				for (InteroperabilitaItem interopItem:doc.getRifEsterni().get(0).getInteroperabilitaItemL()) {
					interopItem.setId(xwClient.addAttach(interopItem.getName(), interopItem.getContent(), conf.getXwLockOpAttempts(), conf.getXwLockOpDelay()));
					uploaded = true;
				}
			}
			
			//upload files
			for (DocwayFile file:doc.getFiles()) {
				file.setId(xwClient.addAttach(file.getName(), file.getContent(), conf.getXwLockOpAttempts(), conf.getXwLockOpDelay()));
				uploaded = true;
			}

			//upload immagini
			for (DocwayFile file:doc.getImmagini()) {
				file.setId(xwClient.addAttach(file.getName(), file.getContent(), conf.getXwLockOpAttempts(), conf.getXwLockOpDelay()));
				uploaded = true;
			}
			
			//update document with uploaded xw:file(s)
			if (uploaded) {
				updateXmlWithDocwayFiles(xmlDocument, doc);
				setCompletedInDoc(xmlDocument, doc.getRecipientEmail());
				xwClient.saveDocument(xmlDocument, lastSavedDocumentPhysDoc, lockedDoc.getTheLock());
			}
			else { //no filed uploaded -> unlock document
				xwClient.unlockDocument(lastSavedDocumentPhysDoc, lockedDoc.getTheLock());
			}			
		}
		catch (Exception e) {
			try {
				xwClient.unlockDocument(lastSavedDocumentPhysDoc, lockedDoc.getTheLock());
			}
			catch (Exception unlockE) {
				; //do nothing
			}
			throw e;
		}

		return xmlDocument;
	}
	
	private void updateXmlWithDocwayFiles(Document xmlDocument, DocwayDocument doc) {
		
		//interopPA message files
		if (doc.getRifEsterni().size() > 0) {
			Element rifEstEl = (Element)xmlDocument.selectSingleNode("/doc/rif_esterni/rif");
			for (InteroperabilitaItem interopItem:doc.getRifEsterni().get(0).getInteroperabilitaItemL()) {
				if (interopItem.getId() != null)
					rifEstEl.add(Docway4EntityToXmlUtils.interoperabilitaItemToXml(interopItem));
			}
		}
		
		//files
		List<DocwayFile> files = doc.getFiles();
		if (files.size() > 0) {
			Element filesEl = (Element)xmlDocument.selectSingleNode("/doc/files");
			if (filesEl == null) {
				filesEl = DocumentHelper.createElement("files");
				xmlDocument.getRootElement().add(filesEl);				
			}
			updateXmlWithDocwayFileList(filesEl, files, true);
		}
		
		//immagini
		List<DocwayFile> immagini = doc.getImmagini();
		if (immagini.size() > 0) {
			Element immaginiEl = (Element)xmlDocument.selectSingleNode("/doc/immagini");
			if (immaginiEl == null) {
				immaginiEl = DocumentHelper.createElement("immagini");
				xmlDocument.getRootElement().add(immaginiEl);				
			}
			updateXmlWithDocwayFileList(immaginiEl, immagini, false);
		}		
		
	}

	private void updateXmlWithDocwayFileList(Element filesContinerEl, List<DocwayFile> files, boolean convert) {
		for (DocwayFile file:files) {
			if (file.getId() != null) {
				//xw:file
				Element xwFileEl = DocumentHelper.createElement("xw:file");
				filesContinerEl.add(xwFileEl);
				xwFileEl.addAttribute("name", file.getId());
				xwFileEl.addAttribute("title", file.getName());
				if (convert)
					xwFileEl.addAttribute("convert", "yes");
				if (file.isFromFatturaPA())
					xwFileEl.addAttribute("fromFatturaPA", "si");
				
				//checkin
				Element chkinEl = DocumentHelper.createElement("chkin");
				xwFileEl.add(chkinEl);
				chkinEl.addAttribute("operatore", file.getOperatore());
				chkinEl.addAttribute("cod_operatore", file.getCodOperatore());
				chkinEl.addAttribute("data", file.getData());
				chkinEl.addAttribute("ora", file.getOra());				
			}
		}
	}	
	
	private void setCompletedInDoc(Document xmlDocument, String recipientEmail) {
		Attribute completedAtt = (Attribute)xmlDocument.selectSingleNode("/doc/archiviatore[@recipientEmail='" + recipientEmail + "']/@completed");
		if (completedAtt != null)
			completedAtt.detach();
	}
	
	private boolean checkExtRestrictionsOnAcl() {
		boolean restrictions = false;
		String uniquerule = xwClient.getUniqueRuleDb("struttura_esterna");
		if (uniquerule != null && !uniquerule.isEmpty()) {
			// Verifica delle restrizione in base alla unique_rule specificata.
			// FIXME il controllo andrebbe fatto in base all'analisi degli and, or, ecc... per il momento ci accontentiamo di questa NON soluzione
			int indexCodUff = uniquerule.indexOf("[XML,/struttura_esterna/@cod_uff]");
			int indexParentesi = uniquerule.indexOf("(");
			if (indexCodUff != -1 && indexParentesi != -1 && indexParentesi < indexCodUff)
				restrictions = true;
		}
		return restrictions;
	}
	
	/**
	 * Costruzione della query di ricerca di anagrafiche esterne (persone/strutture esterne) in base all'indirizzo email passato
	 * @param address
	 * @return
	 */
	private String buildQueryRifEsternoByEmailAddress(String address) {
		//in caso di archivio con anagrafiche esterne replicate su AOO differenti occorre filtrare anche sull'AOO della casella di archiviazione
        String query = "[struest_emailaddr]=\"" + address + "\" OR [persest_recapitoemailaddr]=\"" + address + "\" OR " +
        		"[/struttura_esterna/email_certificata/@addr/]=\"" + address + "\" OR [/persona_esterna/recapito/email_certificata/@addr]=\"" + address + "\"";
        if (extRestrictionsOnAcl) {
        	String codAmmAoo = ((Docway4MailboxConfiguration)super.getConfiguration()).getCodAmmAoo();
        	if (codAmmAoo != null && !codAmmAoo.isEmpty()) {
	        	query = "(([struest_emailaddr]=\"" + address + "\" OR [/struttura_esterna/email_certificata/@addr/]=\"" + address + "\") AND [/struttura_esterna/#cod_ammaoo]=\"" + codAmmAoo + "\")"
	        			+ " OR"
	        			+ " (([persest_recapitoemailaddr]=\"" + address + "\" OR [/persona_esterna/recapito/email_certificata/@addr]=\"" + address + "\") AND [/persona_esterna/#cod_ammaoo]=\"" + codAmmAoo + "\")";
        	}
        }
        return query;
	}
	
	@Override
    public RifEsterno createRifEsterno(String name, String address) throws Exception {
        RifEsterno rifEsterno = new RifEsterno();
        rifEsterno.setEmail(address);

        // first try: search email address
        QueryResult qr = aclClient.search(buildQueryRifEsternoByEmailAddress(address), null, "ud(xpart:/xw/@UdType)", 0, 0);
        if (qr.elements == 0) { // sender is not present in ACL
            rifEsterno.setNome(name);
        }
        else { // extract sender info from ACL
            Document document = aclClient.loadDocByQueryResult(0, qr);
            if (document.getRootElement().getName().equals("struttura_esterna")) { // struttura_esterna
                rifEsterno.setNome(document.getRootElement().element("nome").getText());
                rifEsterno.setCod(document.getRootElement().attributeValue("cod_uff"));
                rifEsterno.setCodiceFiscale(document.getRootElement().attributeValue("codice_fiscale") == null? "" : document.getRootElement().attributeValue("codice_fiscale"));
                rifEsterno.setPartitaIva(document.getRootElement().attributeValue("partita_iva") == null? "" : document.getRootElement().attributeValue("partita_iva"));
                // MASSIMILIANO 02/07/2013: l'email ce l'abbiamo gia'
                //email = document.getAttributeValue("/struttura_esterna/email/@addr", "");
                Attribute tempAttr = (Attribute)document.selectSingleNode("/struttura_esterna/email_certificata/@addr");
                rifEsterno.setEmailCertificata(tempAttr == null? "" : tempAttr.getValue());
                if (rifEsterno.getEmailCertificata().equals(address)) {
                	tempAttr = (Attribute)document.selectSingleNode("/struttura_esterna/email/@addr");
                    rifEsterno.setEmail(tempAttr == null? "" : tempAttr.getValue());
                }
                tempAttr = (Attribute)document.selectSingleNode("/struttura_esterna/telefono[@tipo='tel']/@num");
                rifEsterno.setTel(tempAttr == null? "" : tempAttr.getValue());
                tempAttr = (Attribute)document.selectSingleNode("/struttura_esterna/telefono[@tipo='fax']/@num");
                rifEsterno.setFax(tempAttr == null? "" : tempAttr.getValue());
                Element el = (Element)document.selectSingleNode("/struttura_esterna/indirizzo");
                String indirizzo = "";
                String indirizzo1 = "";
                if (el != null) {
                    indirizzo = el.getText();
                    indirizzo1 = (el.attributeValue("cap") == null || el.attributeValue("cap").length() == 0) ? ""
                            : " " + el.attributeValue("cap");
                    indirizzo1 += (el.attributeValue("comune") == null || el.attributeValue("comune").length() == 0) ? ""
                            : " " + el.attributeValue("comune");
                    indirizzo1 += (el.attributeValue("prov") == null || el.attributeValue("prov").length() == 0) ? ""
                            : " (" + el.attributeValue("prov") + ")";
                    indirizzo1 += (el.attributeValue("nazione") == null || el.attributeValue("nazione").length() == 0) ? ""
                            : " - " + el.attributeValue("nazione");
                }
                if (indirizzo1.length() > 0)
                    indirizzo += " -" + indirizzo1;
                rifEsterno.setIndirizzo(indirizzo);
            }
            else { // persona_esterna
                rifEsterno.setNome(document.getRootElement().attributeValue("cognome") + " " + document.getRootElement().attributeValue("nome"));
                rifEsterno.setCod(document.getRootElement().attributeValue("matricola"));
                rifEsterno.setCodiceFiscale(document.getRootElement().attributeValue("codice_fiscale") == null? "" : document.getRootElement().attributeValue("codice_fiscale"));
                rifEsterno.setPartitaIva("");
                // MASSIMILIANO 02/07/2013: l'email ce l'abbiamo gia'
                //email = document.getAttributeValue("/persona_esterna/recapito/email/@addr", "");
                Attribute tempAttr = (Attribute)document.selectSingleNode("/persona_esterna/recapito/email_certificata/@addr");
                rifEsterno.setEmailCertificata(tempAttr == null? "" : tempAttr.getValue());
                if (rifEsterno.getEmailCertificata().equals(address)) {
                	tempAttr = (Attribute)document.selectSingleNode("/persona_esterna/recapito/email/@addr");
                    rifEsterno.setEmail(tempAttr == null? "" : tempAttr.getValue());
                }
                tempAttr = (Attribute)document.selectSingleNode("/persona_esterna/recapito/telefono[@tipo='tel']/@num");
                rifEsterno.setTel(tempAttr == null? "" : tempAttr.getValue());
                tempAttr = (Attribute)document.selectSingleNode("/persona_esterna/recapito/telefono[@tipo='fax']/@num");
                rifEsterno.setFax(tempAttr == null? "" : tempAttr.getValue());
                Element el = (Element)document.selectSingleNode("/persona_esterna/recapito/indirizzo");
                String indirizzo = "";
                String indirizzo1 = "";
                if (el != null) {
                    indirizzo = el.getText();
                    indirizzo1 = (el.attributeValue("cap") == null || el.attributeValue("cap").length() == 0) ? ""
                            : " " + el.attributeValue("cap");
                    indirizzo1 += (el.attributeValue("comune") == null || el.attributeValue("comune").length() == 0) ? ""
                            : " " + el.attributeValue("comune");
                    indirizzo1 += (el.attributeValue("prov") == null || el.attributeValue("prov").length() == 0) ? ""
                            : " (" + el.attributeValue("prov") + ")";
                    indirizzo1 += (el.attributeValue("nazione") == null || el.attributeValue("nazione").length() == 0) ? ""
                            : " - " + el.attributeValue("nazione");
                }
                if (indirizzo1.length() > 0)
                    indirizzo += " -" + indirizzo1;
                rifEsterno.setIndirizzo(indirizzo);

                // search eventual struttura_esterna
                @SuppressWarnings("unchecked")
				List<Element> l = document.selectNodes("persona_esterna/appartenenza");
                String appartenenze = "";
                for (int i = 0; i < l.size(); i++)
                    appartenenze += " OR \"" + ((Element)l.get(i)).attributeValue("cod_uff") + "\"";
                if (appartenenze.length() > 3)
                    appartenenze = appartenenze.substring(3);
                if (appartenenze.length() > 0) {

                	String cod_amm = document.getRootElement().attributeValue("cod_amm", "");
                	String cod_aoo = document.getRootElement().attributeValue("cod_amm", "");

                	String queryStruest = "[struest_coduff]=" + appartenenze;
                	if (!cod_amm.isEmpty() && !cod_aoo.isEmpty())
                		queryStruest += " AND [/struttura_esterna/#cod_ammaoo]=\"" + cod_amm + cod_aoo + "\"";
                	QueryResult qrStruEst = aclClient.search(queryStruest);
                	
                    if (qrStruEst.elements > 0) { // at least one struttura_esterna found
                        if (qrStruEst.elements > 1) {
                            String emailDomain = address.substring(address.indexOf("@"));
                            queryStruest = "[struest_emailaddr]=\"*" + emailDomain + "\"";
                            if (!cod_amm.isEmpty() && !cod_aoo.isEmpty())
                        		queryStruest += " AND [/struttura_esterna/#cod_ammaoo]=\"" + cod_amm + cod_aoo + "\"";

                            QueryResult qrRefine = aclClient.search(queryStruest, qrStruEst.id, "", 0, 0);
                            if (qrRefine.elements > 0)
                            	qrStruEst = qrRefine;
                        }
                        document = aclClient.loadDocByQueryResult(0, qrStruEst);

                        rifEsterno.setReferenteNominativo(rifEsterno.getNome());
                        rifEsterno.setReferenteCod(rifEsterno.getCod());
                        	
                        rifEsterno.setNome(document.getRootElement().element("nome").getText());
                        rifEsterno.setCod(document.getRootElement().attributeValue("cod_uff"));

                        if (rifEsterno.getCodiceFiscale().isEmpty())
                        	rifEsterno.setCodiceFiscale(document.getRootElement().attributeValue("codice_fiscale") == null? "" : document.getRootElement().attributeValue("codice_fiscale"));

                    	rifEsterno.setPartitaIva(document.getRootElement().attributeValue("partita_iva") == null? "" : document.getRootElement().attributeValue("partita_iva"));
                        
                        if (rifEsterno.getTel().length() == 0) {
                        	tempAttr = (Attribute)document.selectSingleNode("/struttura_esterna/telefono[@tipo='tel']/@num");
                        	rifEsterno.setTel(tempAttr == null? "" : tempAttr.getValue());
                        }
                        if (rifEsterno.getFax().length() == 0) {
                        	tempAttr = (Attribute)document.selectSingleNode("/struttura_esterna/telefono[@tipo='fax']/@num");
                        	rifEsterno.setFax(tempAttr == null? "" : tempAttr.getValue());
                        }
                        if (rifEsterno.getIndirizzo().length() == 0) {
                            el = (Element)document.selectSingleNode("/struttura_esterna/indirizzo");
                            indirizzo1 = "";
                            if (el != null) {
                                indirizzo = el.getText();
                                indirizzo1 = (el.attributeValue("cap") == null || el.attributeValue("cap").length() == 0) ? ""
                                        : " " + el.attributeValue("cap");
                                indirizzo1 += (el.attributeValue("comune") == null || el.attributeValue("comune")
                                        .length() == 0) ? "" : " " + el.attributeValue("comune");
                                indirizzo1 += (el.attributeValue("prov") == null || el.attributeValue("prov").length() == 0) ? ""
                                        : " (" + el.attributeValue("prov") + ")";
                                indirizzo1 += (el.attributeValue("nazione") == null || el.attributeValue("nazione")
                                        .length() == 0) ? "" : " - " + el.attributeValue("nazione");
                            }
                            if (indirizzo1.length() > 0)
                                indirizzo += " -" + indirizzo1;
                            rifEsterno.setIndirizzo(indirizzo);
                        }

                    }
                }
            }

        }
        
        return rifEsterno;
    }	
	
	private List<RifInterno> createRifInterniByPersintQuery(String query) throws Exception {
		List<RifInterno> rifsL = new ArrayList<RifInterno>();
		QueryResult qr = aclClient.search(query);
		if (qr.elements == 0)
			return null;
		for (int i=0; i<qr.elements; i++) { //per ogni persona interna
			RifInterno rifInterno = new RifInterno();
	        Document document = aclClient.loadDocByQueryResult(i, qr);
	        String codPersona = ((Attribute)document.selectSingleNode("persona_interna/@matricola")).getValue();
	        String nomePersona = ((Attribute)document.selectSingleNode("persona_interna/@cognome")).getValue() + " " + ((Attribute)document.selectSingleNode("persona_interna/@nome")).getValue();
	        String codUff = ((Attribute)document.selectSingleNode("persona_interna/@cod_uff")).getValue();
	        String codAmmAoo = ((Attribute)document.selectSingleNode("persona_interna/@cod_amm")).getValue() + ((Attribute)document.selectSingleNode("persona_interna/@cod_aoo")).getValue();
	        rifInterno.setCodPersona(codPersona);
	        rifInterno.setNomePersona(nomePersona);
	        rifInterno.setCodUff(codUff);
	        rifsL.add(rifInterno);
			QueryResult qr1 = aclClient.search("[struint_coduff]=\"" + rifInterno.getCodUff() + "\" AND [/struttura_interna/#cod_ammaoo/]=\"" + codAmmAoo + "\""); //estrazione nome ufficio
	        document = aclClient.loadDocByQueryResult(0, qr1);
	        String nomeUff = document.getRootElement().elementText("nome").trim();
	        rifInterno.setNomeUff(nomeUff);	        
		}
		return rifsL;
	}
	
	public RifInterno createRifInternoByAssegnatario(AssegnatarioMailboxConfiguration assegnatario) throws Exception {
		String codAmmAoo = ((Docway4MailboxConfiguration)super.getConfiguration()).getCodAmmAoo();
		RifInterno rifInterno = new RifInterno();
		if (assegnatario.isRuolo()) { //ruolo
			String query = "[ruoli_id]=\"" + assegnatario.getCodRuolo() + "\" AND [/ruolo/#cod_ammaoo/]=\"" + codAmmAoo + "\"";
			QueryResult qr = aclClient.search(query);
	        Document document = aclClient.loadDocByQueryResult(0, qr);
	        String nomeRuolo = document.getRootElement().elementText("nome").trim();
			rifInterno.setRuolo(nomeRuolo, assegnatario.getCodRuolo());
			rifInterno.setIntervento(assegnatario.isIntervento());			        
		}
		else { //persona-ufficio
			rifInterno.setCodPersona(assegnatario.getCodPersona());
			rifInterno.setCodUff(assegnatario.getCodUff());
			rifInterno.setIntervento(assegnatario.isIntervento());
			
			QueryResult qr = aclClient.search("[struint_coduff]=\"" + rifInterno.getCodUff() + "\" AND [/struttura_interna/#cod_ammaoo/]=\"" + codAmmAoo + "\"");
	        Document document = aclClient.loadDocByQueryResult(0, qr);
	        String nomeUff = document.getRootElement().elementText("nome").trim();
	        rifInterno.setNomeUff(nomeUff);				

	        qr = aclClient.search("[/persona_interna/@matricola]=\"" + rifInterno.getCodPersona() + "\" AND [persint_codammaoo]=\"" + codAmmAoo + "\"");
	        document = aclClient.loadDocByQueryResult(0, qr);
	        String nomePersona = ((Attribute)document.selectSingleNode("persona_interna/@cognome")).getValue() + " " + ((Attribute)document.selectSingleNode("persona_interna/@nome")).getValue();
			rifInterno.setNomePersona(nomePersona);
		}
		return rifInterno;
	}
	
	@Override
	protected List<RifInterno> createRifInterni(ParsedMessage parsedMessage) throws Exception {
		List<RifInterno> rifInterni = new ArrayList<RifInterno>();
		DocwayMailboxConfiguration conf = (DocwayMailboxConfiguration) super.getConfiguration();
		String currentEmailAddress = conf.getAddress();
		
		// mbernardini 26/02/2019 : override degli assegnatari se smistamento fatturePA risulta abilitato sulla mailbox
		if (conf.isSmistamentoFatturePA() && parsedMessage instanceof DocwayParsedMessage) {
			// lettura dell'indirizzo email del destinatario dal file XML della fattura
			DocwayParsedMessage dwParsedMessage = (DocwayParsedMessage) parsedMessage;
			if (dwParsedMessage.isFatturaPAMessage(conf.getSdiDomainAddress())) {
				String fatturaPaEmailTo = FatturaPAUtils.getPECDestinatarioFromFatturaPA(dwParsedMessage.getFatturaPADocument());
				if (fatturaPaEmailTo != null && !fatturaPaEmailTo.isEmpty()) {
					if (logger.isDebugEnabled())
						logger.debug("[" + currentEmailAddress + "] smistamento assegnati fattura su " + fatturaPaEmailTo + ". messageId = " + parsedMessage.getMessageId());
					
					// ricerca della mailbox relativa all'indirizzo letto
					MailboxManager anotherManager = MailboxesManagersMap.getInstance().getManager(fatturaPaEmailTo);
					if (anotherManager != null && anotherManager.getConfiguration() != null && anotherManager.getConfiguration() instanceof DocwayMailboxConfiguration) {
						if (logger.isDebugEnabled())
							logger.debug("[" + currentEmailAddress + "] mailbox " + fatturaPaEmailTo + " found... override configuration in use!");
						
						// override degli assegnatari del documento in base a quelli recuperati dalla mailbox caricata
						conf = (DocwayMailboxConfiguration) anotherManager.getConfiguration();
					}
					else {
						if (logger.isWarnEnabled())
							logger.warn("[" + currentEmailAddress + "] nessuna mailbox configurata su " + fatturaPaEmailTo + ". Smistamento NON possibile!");
					}
				}
			}
		}
		
		String codAmmAoo = conf.getCodAmmAoo();
		
		//RPA
		List<RifInterno> rifsL = null;
		if (conf.isDaDestinatario()) {
			String to = parsedMessage.getFromAddress();
            to = to.substring(to.indexOf("+") + 1, to.indexOf("@"));
            rifsL = createRifInterniByPersintQuery("[persint_loginname]=\"" + to + "\" AND [persint_codammaoo]=\"" + codAmmAoo + "\"");
		}
		
		if (conf.isDaMittente() && rifsL == null) {
			rifsL = createRifInterniByPersintQuery("[persint_recapitoemailaddr]=\"" + parsedMessage.getFromAddress() + "\" AND [persint_codammaoo]=\"" + codAmmAoo + "\"");
		}

		RifInterno rpa = (rifsL == null)? createRifInternoByAssegnatario(conf.getResponsabile()) : rifsL.get(0);
		rpa.setDiritto("RPA");
		rpa.setIntervento(true);
		rifInterni.add(rpa);
		
		//CC
		if (conf.isDaCopiaConoscenza()) {
			String query = parsedMessage.getCcAddressesAsString().replaceAll(",", "\" OR \"");
			if (!query.isEmpty()) {
				// mbernardini 20/03/2019 : se 'daCopiaConoscenza' risulta attiva la query sulle persone interne deve essere fatto sugli indirizzi in CC e non sul TO
				rifsL = createRifInterniByPersintQuery("[persint_recapitoemailaddr]=\"" + query + "\" AND [persint_codammaoo]=\"" + codAmmAoo + "\"");
				// mbernardini 20/03/2019 : la lista dei rifs potrebbe anche essere nulla
				if (rifsL != null) {
					for (RifInterno cc:rifsL) {
						if (cc != null) {
							cc.setDiritto("CC");
							rifInterni.add(cc);
						}
					}
				}
			}
		}
		
		for (AssegnatarioMailboxConfiguration assegnatario: conf.getAssegnatariCC()) {
			if (assegnatario != null) {
				RifInterno cc = createRifInternoByAssegnatario(assegnatario);
				cc.setDiritto("CC");
				rifInterni.add(cc);
			}
		}
		
		return rifInterni;
	}

	@Override
	protected void sendNotificationEmails(DocwayDocument doc, Object saveDocRetObj) {
		Docway4MailboxConfiguration conf = (Docway4MailboxConfiguration)getConfiguration();
		// mbernardini 14/03/2019 : verifico che effettivamente le notifiche via email siano abilitate sul notification sender
		NotificationSender notificationSender = Services.getNotificationService().getNotificationSender();
		if (notificationSender != null && notificationSender instanceof MailNotificationSender) {
			MailNotificationSender mailNotification = (MailNotificationSender) notificationSender;
			MailSender mailSender = mailNotification.createMailSender();
			try {
				mailSender.connect();
				String body = Docway4NotificationEmailsUtils.getBodyForEmail(conf.getNotificationAppHost(), conf.getNotificationAppHost1(), conf.getNotificationAppUri(), conf.getXwDb(), (Document)saveDocRetObj);
				
				Set<String>	notifiedAddresses = new HashSet<String>();
				for (RifInterno rifInterno:doc.getRifInterni()) {
					if (rifInterno.isNotify()) { //if rif interno has to be notified
						if ((rifInterno.getDiritto().equals("RPA") && conf.isNotifyRPA()) || (!rifInterno.getDiritto().equals("RPA") && conf.isNotifyCC()))
							sendNotificationEmail(mailSender, mailNotification.getSenderAdress(), mailNotification.getSenderPersonal(), rifInterno.getCodPersona(), rifInterno.getDiritto().equals("RPA"), doc, (Document)saveDocRetObj, body, conf.getCodAmmAoo(), notifiedAddresses);
					}
				}				
			} 
			catch (Exception e) {
				logger.error("[" + conf.getAddress() + "] unexpected error sending notification emails", e);
			}
			finally {
				try {
					mailSender.disconnect();
				} 
				catch (Exception e) {
					logger.warn("[" + conf.getAddress() + "] failed to close mailSender session", e);
				}				
			}
		}
	}

	private void sendNotificationEmail(MailSender mailSender, String senderAddress, String senderPersonal, String matricola, boolean isRPA, DocwayDocument doc, Document savedDocument, String body, String codAmmAooDestinatario, Set<String> notifiedAddresses) {
		Docway4MailboxConfiguration conf = (Docway4MailboxConfiguration)getConfiguration();
		try {
			boolean matricolaOnSubject = conf.isAddMatricolaOnNotification();
			String subject = Docway4NotificationEmailsUtils.getSubjectForEmail(isRPA?"RPA":"CC", savedDocument, matricolaOnSubject ? matricola : null);
			String destEmail = getEmailWithMatricola(matricola, codAmmAooDestinatario);
			String []destinatari = destEmail.split(",");
			for (String dest:destinatari) {
				if (!dest.isEmpty() && !notifiedAddresses.contains(dest)) {
					try {
						if (logger.isInfoEnabled())
							logger.info("[" + conf.getAddress() + "] sending notification email [" + dest + "]");
						notifiedAddresses.add(dest);
						mailSender.sendMail(senderAddress, senderPersonal, dest, subject, body);	
					}
					catch (Exception e) {
						logger.error("[" + conf.getAddress() + "] unexpected error sending notification email [" + dest + "]", e);
					}
				}
			}
			
		} 
		catch (Exception e) {
			logger.error("[" + conf.getAddress() + "] unexpected error extracting email address for matricola [" + matricola + "]", e);
		}
	}
    
	public String getEmailWithMatricola(String matricola, String codAmmAoo) throws Exception {
		String res = "";

		String query = "";
		if (matricola.startsWith(Docway4NotificationEmailsUtils.TUTTI_COD + "_")) {
			String codUff = matricola.substring(matricola.indexOf("_") + 1);
			query = "([persint_coduff]=" + codUff + " OR [persint_gruppoappartenenzacod]=" + codUff + " OR [persint_mansionecod]=" + codUff + ") AND [/persona_interna/#cod_ammaoo/]=" + codAmmAoo;
		}
		else {
			query = "[persint_matricola]=" + matricola + " AND [/persona_interna/#cod_ammaoo/]=" + codAmmAoo;
		}

		QueryResult qr = aclClient.search(query);
		int count = qr.elements;
		for (int i=0; i<count; i++) {
			Document document = aclClient.loadDocByQueryResult(i, qr);
			
			// mbernardini 07/06/2019 : caricamento di tutti gli indirizzi email associati ad una persona interna per la notifica su documenti assegnati
			List<?> indirizzi = document.selectNodes("/persona_interna/recapito/email/@addr");
			if (indirizzi != null && indirizzi.size() > 0) {
				for (int j=0; j<indirizzi.size(); j++) {
					Attribute indirizzoEl = (Attribute) indirizzi.get(j);
					if (indirizzoEl != null && indirizzoEl.getValue() != null) {
						String indirizzo = indirizzoEl.getValue().trim();
						if (!indirizzo.isEmpty())
							res += "," + indirizzo;
					}
				}
			}
		}

		if (!res.isEmpty())
			res = res.substring(1);
		
		return res;
	}

	@Override
	protected Object updatePartialDocument(DocwayDocument doc) throws Exception {
		Docway4MailboxConfiguration conf = (Docway4MailboxConfiguration)getConfiguration();
		
		//load and lock existing document
		LockedDocument lockedDoc = xwClient.loadAndLockDocument(this.physDocToUpdate, conf.getXwLockOpAttempts(), conf.getXwLockOpDelay());
		Document xmlDocument = lockedDoc.getDoc();
		
		try {
			boolean uploaded = false;
			
			//upload interopPA message files
			if (doc.getRifEsterni().size() > 0) {
				for (InteroperabilitaItem interopItem:doc.getRifEsterni().get(0).getInteroperabilitaItemL()) {
					if (isInteropPAFileNew(interopItem.getName(), xmlDocument)) {
						interopItem.setId(xwClient.addAttach(interopItem.getName(), interopItem.getContent(), conf.getXwLockOpAttempts(), conf.getXwLockOpDelay()));
						uploaded = true;
					}
					else
						interopItem.setId(null);
				}
			}			
			
			//upload files
			for (DocwayFile file:doc.getFiles()) {
				if (isFileNew(file.getName(), xmlDocument, "files")) {
					file.setId(xwClient.addAttach(file.getName(), file.getContent(), conf.getXwLockOpAttempts(), conf.getXwLockOpDelay()));
					uploaded = true;				
				}
				else
					file.setId(null);
			}

			//upload immagini
			for (DocwayFile file:doc.getImmagini()) {
				if (isFileNew(file.getName(), xmlDocument, "immagini")) {
					file.setId(xwClient.addAttach(file.getName(), file.getContent(), conf.getXwLockOpAttempts(), conf.getXwLockOpDelay()));
					uploaded = true;
				}
				else
					file.setId(null);			
			}
			//update document with uploaded xw:file(s)
			if (uploaded) {
				updateXmlWithDocwayFiles(xmlDocument, doc);
				setCompletedInDoc(xmlDocument, doc.getRecipientEmail());
				xwClient.saveDocument(xmlDocument, this.physDocToUpdate, lockedDoc.getTheLock());
			}
			else { //no filed uploaded -> unlock document
				xwClient.unlockDocument(this.physDocToUpdate, lockedDoc.getTheLock());
			}			
		}
		catch (Exception e) {
			try {
				xwClient.unlockDocument(this.physDocToUpdate, lockedDoc.getTheLock());
			}
			catch (Exception unlockE) {
				; //do nothing
			}
			throw e;
		}

		return xmlDocument;
	}

	private boolean isFileNew(String fileName, Document xmlDocument, String fileContainerElName) {
		@SuppressWarnings("unchecked")
		List<Element> xwFilesL = xmlDocument.selectNodes("/doc/" + fileContainerElName + "/*[name()='xw:file'][count(.//*[name()='xw:file'])=0][count(@der_from)=0]");
		for (Element fileEl:xwFilesL) {
			if (fileEl.attributeValue("title").equals(fileName))
				return false;
		}
		return true;
	}
	
	private boolean isInteropPAFileNew(String fileName, Document xmlDocument) {
		@SuppressWarnings("unchecked")
		List<Element> filesL = xmlDocument.selectNodes("/doc/rif_esterni/rif/interoperabilita");
		for (Element fileEl:filesL) {
			if (fileEl.attributeValue("title").equals(fileName))
				return false;
		}
		return true;
	}	

	@Override
	protected Object updateDocumentWithRecipient(DocwayDocument doc) throws Exception {
		Docway4MailboxConfiguration conf = (Docway4MailboxConfiguration)getConfiguration();
		
		//load and lock existing document
		LockedDocument lockedDoc = xwClient.loadAndLockDocument(this.physDocToUpdate, conf.getXwLockOpAttempts(), conf.getXwLockOpDelay());
		Document xmlDocument = lockedDoc.getDoc();
		
		try {
			Element docEl = xmlDocument.getRootElement();
			Element rifIntEl = docEl.element("rif_interni");
			
			Element storiaEl = docEl.element("storia");
			
			//update document with new mailbox and CCs
			Element archiviatoreEl = DocumentHelper.createElement("archiviatore");
			docEl.add(archiviatoreEl);
			archiviatoreEl.addAttribute("recipientEmail", doc.getRecipientEmail());
			// mbernardini 20/04/2019 : registrazione della data di invio del messaggio email
			archiviatoreEl.addAttribute("sentDate", DateUtils.dateToXwFormat(doc.getSentDate()));
			archiviatoreEl.addAttribute("sentTime", DateUtils.timeToXwFormat(doc.getSentDate()));
			for (RifInterno rifInterno:doc.getRifInterni()) {
				//RPA deve essere trasformato in CC con diritto di intervento
				if (rifInterno.getDiritto().equals("RPA")) {
					rifInterno.setDiritto("CC");
					rifInterno.setIntervento(true);
				}
				if (isNewRifInterno(rifInterno, rifIntEl)) {
					rifIntEl.add(Docway4EntityToXmlUtils.rifInternoToXml(rifInterno, conf.getAspettoClassificazione()));
					
					// mbernardini 23/04/2019 : in caso di aggiunta di rif interni ad un documento gia' registrato occorre aggiornare anche i dati relativi alla storia
					// questo scenario si ottiene da un invio di un messaggio email a 2 distinte caselle di posta configurate su msa
					StoriaItem storiaItem = StoriaItem.createFromRifInterno(rifInterno);
					storiaItem.setOperatore(conf.getOperatore());
					storiaItem.setData(currentDate);
					storiaItem.setOra(currentDate);
					storiaEl.add(Docway4EntityToXmlUtils.storiaItemToXml(storiaItem));
					
				}
				else 
					rifInterno.setNotify(false);
			}
			
			updateXmlWithDocwayFiles(xmlDocument, doc);
			xwClient.saveDocument(xmlDocument, this.physDocToUpdate, lockedDoc.getTheLock());
		}
		catch (Exception e) {
			try {
				xwClient.unlockDocument(this.physDocToUpdate, lockedDoc.getTheLock());
			}
			catch (Exception unlockE) {
				; //do nothing
			}
			throw e;
		}		
		
		return xmlDocument;
	}
    
	@SuppressWarnings("unchecked")
	private boolean isNewRifInterno(RifInterno rifInterno, Element rifIntEl) {
		for (Element rifEl: (List<Element>)rifIntEl.elements()) {
			if (rifEl.attributeValue("diritto").equals(rifInterno.getDiritto()) && rifEl.attributeValue("cod_persona").equals(rifInterno.getCodPersona()) && rifEl.attributeValue("cod_uff").equals(rifInterno.getCodUff()))
				return false;
		}
		return true;
	}

	@Override
	protected void attachInteropPAPecReceiptToDocument(ParsedMessage parsedMessage) throws Exception {
		String receiptTypeBySubject = parsedMessage.getSubject().substring(0, parsedMessage.getSubject().indexOf(":"));
		receiptTypeBySubject = receiptTypeBySubject.substring(0, 1).toUpperCase() + receiptTypeBySubject.substring(1).toLowerCase(); //capitalize only first letter		
		String realToAddress = parsedMessage.getRealToAddressFromDatiCertPec();
		attachInteropPAFileToDocument(parsedMessage, receiptTypeBySubject, realToAddress, "");
	}

	@Override
	protected void attachInteropPANotificationToDocument(ParsedMessage parsedMessage) throws Exception {
		Docway4MailboxConfiguration conf = (Docway4MailboxConfiguration)getConfiguration();
		DocwayParsedMessage dcwParsedMessage = (DocwayParsedMessage)parsedMessage;
		String info = "";
		if (dcwParsedMessage.isConfermaRicezioneInteropPAMessage(conf.getCodAmmInteropPA(), conf.getCodAooInteropPA()))
			info = "Ricezione: Conferma Ricezione";
		else if (dcwParsedMessage.isAggiornamentoConfermaInteropPAMessage(conf.getCodAmmInteropPA(), conf.getCodAooInteropPA()))
			info = "Ricezione: Aggiornamento Conferma";			
		else if (dcwParsedMessage.isAnnullamentoProtocollazioneInteropPAMessage(conf.getCodAmmInteropPA(), conf.getCodAooInteropPA()))
			info = "Ricezione: Annullamento Protocollazione";
		else if (dcwParsedMessage.isNotificaEccezioneInteropPAMessage(conf.getCodAmmInteropPA(), conf.getCodAooInteropPA()))
			info = "Ricezione: Notifica Eccezione";		
		
		String numero = "";
		Element identificatoreEl = dcwParsedMessage.getInteropPaDocument().getRootElement().element("Identificatore");
		if (identificatoreEl != null) {
			// mbernardini 09/07/2019 : gestiti errori di notifiche con dati mancanti sul protocollo del mittente
			String dataRegistrazione = identificatoreEl.elementText("DataRegistrazione");
			String codiceAmministrazione = identificatoreEl.elementText("CodiceAmministrazione");
			String codiceAoo = identificatoreEl.elementText("CodiceAOO");
			String numeroRegistrazione = identificatoreEl.elementText("NumeroRegistrazione");
			if (dataRegistrazione != null && !dataRegistrazione.isEmpty() 
					&& codiceAmministrazione != null && !codiceAmministrazione.isEmpty() 
					&& codiceAoo != null && !codiceAoo.isEmpty()
					&& numeroRegistrazione != null && !numeroRegistrazione.isEmpty()) {
				
				numero = dataRegistrazione.substring(0, 4) + "-" + codiceAmministrazione + codiceAoo + "-" + numeroRegistrazione;
			}
		}
		attachInteropPAFileToDocument(parsedMessage, info, dcwParsedMessage.getMittenteAddressFromDatiCertPec(), numero);
	}
	
	private void attachInteropPAFileToDocument(ParsedMessage parsedMessage, String fileInfo, String rifEstAddress, String numero) throws Exception {
		Docway4MailboxConfiguration conf = (Docway4MailboxConfiguration)getConfiguration();
		
		//load and lock existing document
		LockedDocument lockedDoc = xwClient.loadAndLockDocument(this.physDocForAttachingFile, conf.getXwLockOpAttempts(), conf.getXwLockOpDelay());
		Document xmlDocument = lockedDoc.getDoc();
		
		try {
			//upload file
			String fileName =  fileInfo.replaceAll(":", "") + ".eml";
			byte []fileContent = (new MessageContentProvider(parsedMessage.getMessage(), false)).getContent();
			String fileId = xwClient.addAttach(fileName, fileContent, conf.getXwLockOpAttempts(), conf.getXwLockOpDelay());
			
			//build interoperabilita element
			InteroperabilitaItem interopItem = new InteroperabilitaItem();
			interopItem.setId(fileId);
			interopItem.setName(fileName);
			interopItem.setData(currentDate);
			interopItem.setOra(currentDate);
			interopItem.setInfo(fileInfo);
			interopItem.setMessageId(parsedMessage.getMessageId());
				
			//try to attach interopEl to rif esterno (by email)
            Element rifEsterniEl = (Element)xmlDocument.selectSingleNode("/doc/rif_esterni");
            @SuppressWarnings("unchecked")
			List<Element> rifsL = rifEsterniEl.elements("rif");
            Element rifEl = null;
            if (rifEstAddress != null && !rifEstAddress.isEmpty()) {
                for (Element el:rifsL) {
                	// mbernardini 02/04/2019 : occorre cercare il riferimento alla mail anche sugli indirizzi di mail ordinaria oltre che pec
                	
                	rifEstAddress = rifEstAddress.trim();
                	
                	// Ricerca su caselle PEC del rif esterno
                	Element emailCertificataEl = el.element("email_certificata");
                	if (emailCertificataEl != null && emailCertificataEl.attributeValue("addr", "").trim().equalsIgnoreCase(rifEstAddress))
                		rifEl = el;
                	
                	// Ricerca su caselle ordinarie del rif esterno
                	Element indirizzoEl = el.element("indirizzo");
                	if (indirizzoEl != null && indirizzoEl.attributeValue("email", "").trim().toLowerCase().contains(rifEstAddress.toLowerCase())) // potrebbero essere più indirizzi separati da punto e virgola
                		rifEl = el;
                	
                	if (rifEl != null) {
	                	if (numero != null && !numero.isEmpty() && rifEl.attributeValue("n_prot", "").isEmpty()) //si aggiunge il numero di protocollo al rif esterno se manca
	            			rifEl.addAttribute("n_prot", numero);
	            		break;
                	}
                }            	
            }
            
            // mbernardini 07/05/2019 : identificazioni alernative del rif esterno al quale agganciare la notifica
            if (rifEl == null && parsedMessage instanceof DocwayParsedMessage) {
            	
            	// Nel caso sia presente un solo rif esterno la notifica deve per forza fare riferimento a questo
            	if (rifsL.size() == 1)
            		rifEl = rifsL.get(0);
            	
            	// Tentativo di identificazione del rif tramite analisi dell'oggetto (individuazione della posizione del rif esterno)
            	if (rifEl == null && ((DocwayParsedMessage) parsedMessage).isPecReceiptForInteropPAbySubject()) {
            		String subject = parsedMessage.getSubject();
            		if (subject != null) {
            			subject = subject.trim();
            			
            			int index = subject.indexOf("(");
                        int index1 = subject.indexOf(")");
                        if (index != -1 && index1 != -1 && index < index1) {
	                        String indexValue = subject.substring(index+1, index1);
	                		if (!indexValue.equals("*")) {
	                			int rifPosition = -1;
	                			try {
	                				rifPosition = Integer.parseInt(indexValue);
	                			}
	                			catch (NumberFormatException e) {
	                				logger.warn("[" + conf.getAddress() + "]. Got exception while parsing recipient index number from subject. ", e);
								}
	                			if (rifPosition >= 0 && rifPosition < rifsL.size()) {
	                				if (logger.isDebugEnabled())
	                					logger.debug("[" + conf.getAddress() + "] Assign interop file by rif. position (from subject)... position = " + rifPosition);
	                				
	                				rifEl = rifsL.get(rifPosition);
	                			}
	                		}
                        }
            		}
            	}
            }

            if (rifEl != null) {
            	rifEl.add(Docway4EntityToXmlUtils.interoperabilitaItemToXml(interopItem));
            }
            else { 	//default -> attach interopEl to interoperabilita_multipla
    			Element interoperabilitaMultiplaEl = rifEsterniEl.element("interoperabilita_multipla");
    			if (interoperabilitaMultiplaEl == null) {
    				interoperabilitaMultiplaEl = DocumentHelper.createElement("interoperabilita_multipla");
    				rifEsterniEl.add(interoperabilitaMultiplaEl);
    			}
    			interoperabilitaMultiplaEl.add(Docway4EntityToXmlUtils.interoperabilitaItemToXml(interopItem));
            }
            
			xwClient.saveDocument(xmlDocument, this.physDocForAttachingFile, lockedDoc.getTheLock());
		}
		catch (Exception e) {
			try {
				xwClient.unlockDocument(this.physDocForAttachingFile, lockedDoc.getTheLock());
			}
			catch (Exception unlockE) {
				; //do nothing
			}
			throw e;
		}		
	}

	@Override
	protected String buildNewNumprotStringForSavingDocument() throws Exception {
		Docway4MailboxConfiguration conf = (Docway4MailboxConfiguration)getConfiguration();
		return (new SimpleDateFormat("yyyy")).format(currentDate) + "-" + conf.getCodAmmAoo() + "-.";
	}

	@Override
	protected String buildNewNumrepStringForSavingDocument(String repertorioCod) throws Exception {
		Docway4MailboxConfiguration conf = (Docway4MailboxConfiguration)getConfiguration();
		return repertorioCod + "^" + conf.getCodAmmAoo() + "-" + (new SimpleDateFormat("yyyy")).format(currentDate) + ".";
	}	
	
	protected String getOggettoForEmailSubject(String oggetto) {
		oggetto = oggetto.replaceAll("\n", " ");
		if (oggetto.length() > 255)
			oggetto = oggetto.substring(0, 255);
		return oggetto;
	}

	@Override
	protected RifEsterno createMittenteFatturaPA(ParsedMessage parsedMessage) throws Exception {
		return createRifEsternoFatturaPA("CedentePrestatore", parsedMessage);
	}	
	
	private RifEsterno createRifEsternoFatturaPA(String rifElemNameInFatturaPA, ParsedMessage parsedMessage) throws Exception {
		Docway4MailboxConfiguration conf = (Docway4MailboxConfiguration)getConfiguration();
		DocwayParsedMessage dcwParsedMessage = (DocwayParsedMessage)parsedMessage;
		Document fatturaPADocument = dcwParsedMessage.getFatturaPADocument();		
		
		Node node = fatturaPADocument.selectSingleNode("//FatturaElettronicaHeader/" + rifElemNameInFatturaPA + "/DatiAnagrafici/IdFiscaleIVA/IdCodice");
		String piva = (node == null)? "" : node.getText();
		node = fatturaPADocument.selectSingleNode("//FatturaElettronicaHeader/" + rifElemNameInFatturaPA + "/DatiAnagrafici/CodiceFiscale");
		String cf = (node == null)? "" : node.getText();
		
		boolean found = false;
		RifEsterno rifEsterno = null;
		
		if (!piva.isEmpty()) { // ricerca in anagrafica su campo partita iva

			QueryResult qr = aclClient.search("([/struttura_esterna/@partita_iva/]=\"" + piva + "\" AND [/struttura_esterna/#cod_ammaoo/]=\"" + conf.getCodAmmAoo() + "\") OR ([/persona_esterna/@partita_iva/]=\"" + piva + "\" AND [/persona_esterna/#cod_ammaoo/]=\"" + conf.getCodAmmAoo() + "\")");
			if (qr.elements == 1) { // e' stata individuata una struttura esterna con la partita iva indicata
				if (logger.isInfoEnabled())
					logger.info("[" + conf.getAddress() + "] found rif esterno in ACL. Piva [" + piva + "]");
				
				found = true;
				rifEsterno = getRifEsternoFromAcl(aclClient.loadDocByQueryResult(0, qr));
			}
		}
		if (!found && !cf.isEmpty()) { // ricerca in anagrafica su campo codice fiscale
			QueryResult qr = aclClient.search("([/struttura_esterna/@codice_fiscale/]=\"" + cf + "\" AND [/struttura_esterna/#cod_ammaoo/]=\"" + conf.getCodAmmAoo()+ "\") OR ([/persona_esterna/@codice_fiscale/]=\"" + cf + "\" AND [/persona_esterna/#cod_ammaoo/]=\"" + conf.getCodAmmAoo() + "\")");
			if (qr.elements == 1) { // e' stata individuata una struttura esterna con la partita iva indicata
				if (logger.isInfoEnabled())
					logger.info("[" + conf.getAddress() + "] found rif esterno in ACL. CF [" + cf + "]");
				
				found = true;
				rifEsterno = getRifEsternoFromAcl(aclClient.loadDocByQueryResult(0, qr));
			}
		}
		
		if (!found) { // inserimento nuova struttura/persona esterna: almento uno fra denominazione e nome/cognome deve essere valorizzato
			Document aclDocument = DocumentHelper.createDocument();
			
			node = fatturaPADocument.selectSingleNode("//FatturaElettronicaHeader/" + rifElemNameInFatturaPA + "/DatiAnagrafici/Anagrafica/Denominazione");
			String denominazione = (node == null)? "" : node.getText();
			String rootElementName = (!denominazione.isEmpty())? "struttura_esterna" : "persona_esterna"; 
			
			Element root = aclDocument.addElement(rootElementName);
			root.addAttribute("nrecord", ".");
			root.addAttribute("cod_amm", conf.getCodAmm());
			root.addAttribute("cod_aoo", conf.getCodAoo());
			
			node = fatturaPADocument.selectSingleNode("//FatturaElettronicaHeader/" + rifElemNameInFatturaPA + "/Sede/Indirizzo");
			String indirizzo = (node == null)? "" : node.getText();
			node = fatturaPADocument.selectSingleNode("//FatturaElettronicaHeader/" + rifElemNameInFatturaPA + "/Sede/NumeroCivico");
			if (node != null && !node.getText().isEmpty())
				indirizzo += " " + node.getText();
			
			node = fatturaPADocument.selectSingleNode("//FatturaElettronicaHeader/" + rifElemNameInFatturaPA + "/Sede/CAP");
			String cap = (node == null)? "" : node.getText();
			
			node = fatturaPADocument.selectSingleNode("//FatturaElettronicaHeader/" + rifElemNameInFatturaPA + "/Sede/Comune");
			String comune = (node == null)? "" : node.getText();
			
			node = fatturaPADocument.selectSingleNode("//FatturaElettronicaHeader/" + rifElemNameInFatturaPA + "/Sede/Provincia");
			String provincia = (node == null)? "" : node.getText();
			
			node = fatturaPADocument.selectSingleNode("//FatturaElettronicaHeader/" + rifElemNameInFatturaPA + "/Sede/Nazione");
			String nazione = (node == null)? "" : node.getText();
			
			node = fatturaPADocument.selectSingleNode("//FatturaElettronicaHeader/" + rifElemNameInFatturaPA + "/Contatti/Email");
			String email = (node == null)? "" : node.getText();
			
			node = fatturaPADocument.selectSingleNode("//FatturaElettronicaHeader/" + rifElemNameInFatturaPA + "/Contatti/Fax");
			String fax = (node == null)? "" : node.getText();
			
			if (rootElementName.equals("struttura_esterna")) { //struttura esterna
				root.addAttribute("cod_uff", ".");
				
				Element nomeEl = root.addElement("nome");
				nomeEl.addText(denominazione);
				
				if (logger.isInfoEnabled())
					logger.info("[" + conf.getAddress() + "] rif esterno NOT found in ACL. Inserting SE [" + denominazione + "]");				
				
				root.addAttribute("partita_iva", piva);
				root.addAttribute("codice_fiscale", cf);
				
				// gestione del recapito (sede azienda)
				Element elindirizzo = root.addElement("indirizzo");
				elindirizzo.addText(indirizzo);
				elindirizzo.addAttribute("cap", cap);
				elindirizzo.addAttribute("comune", comune);
				elindirizzo.addAttribute("prov", provincia);
				elindirizzo.addAttribute("nazione", nazione);
				
				if (!email.equals("")) {
					Element elemail = root.addElement("email");
					elemail.addAttribute("addr", email);
				}
				if (!fax.equals("")) {
					Element eltelefono = root.addElement("telefono");
					eltelefono.addAttribute("tipo", "fax");
					eltelefono.addAttribute("num", fax);
				}
			}
			else { //persona esterna
				root.addAttribute("matricola", ".");
				
				node = fatturaPADocument.selectSingleNode("//FatturaElettronicaHeader/" + rifElemNameInFatturaPA + "/DatiAnagrafici/Anagrafica/Nome");
				root.addAttribute("nome", node == null? "" : node.getText());
				
				node = fatturaPADocument.selectSingleNode("//FatturaElettronicaHeader/" + rifElemNameInFatturaPA + "/DatiAnagrafici/Anagrafica/Cognome");
				root.addAttribute("cognome", node == null? "" : node.getText());
				
				if (logger.isInfoEnabled())
					logger.info("[" + conf.getAddress() + "] rif esterno NOT found in ACL. Inserting PE [" + root.attributeValue("cognome") + " " + root.attributeValue("nome") + "]");
				
				root.addAttribute("partita_iva", piva);
				root.addAttribute("codice_fiscale", cf);

				// gestione del recapito (recapito attivita')
				Element recapito = root.addElement("recapito");
				Element elindirizzo = recapito.addElement("indirizzo");
				elindirizzo.addText(indirizzo);
				elindirizzo.addAttribute("cap", cap);
				elindirizzo.addAttribute("comune", comune);
				elindirizzo.addAttribute("prov", provincia);
				elindirizzo.addAttribute("nazione", nazione);
				
				if (!email.equals("")) {
					Element elemail = recapito.addElement("email");
					elemail.addAttribute("addr", email);
				}
				if (!fax.equals("")) {
					Element eltelefono = recapito.addElement("telefono");
					eltelefono.addAttribute("tipo", "fax");
					eltelefono.addAttribute("num", fax);
				}
			}
			
			//salvataggio nuova struttura/persona esterna
			int pD = aclClient.saveNewDocument(aclDocument);
			rifEsterno = getRifEsternoFromAcl(aclClient.loadDocByPhysdoc(pD));
		}
		else {
			
			// mbernardini 17/01/2019 : sovrascrittura del rif esterno recuperato da ACL con i dati contenuti nella fatturaPA ricervuta
			rifEsterno = updateRifEsternoByDatiFattura(rifEsterno, fatturaPADocument, rifElemNameInFatturaPA);
		}
		
		return rifEsterno;
	}

	/**
	 * Dato un documento recuperato da ACL, si occupa di recuperare tutte le informazioni del rif. esterno da associare al documento
	 * inerente la fatturaPA ricevuta
	 * @param doc
	 * @return
	 */
	private RifEsterno getRifEsternoFromAcl(Document doc) {
		RifEsterno rif = new RifEsterno();
	
		String pne = doc.getRootElement().getQualifiedName();
		if (pne.equals("struttura_esterna")) {
			rif.setNome(((Element) doc.selectSingleNode("struttura_esterna/nome")).getTextTrim());
			rif.setCod(((Attribute) doc.selectSingleNode("struttura_esterna/@cod_uff")).getValue());
		}
		else { // pne = persona_esterna
			rif.setNome(((Attribute) doc.selectSingleNode("persona_esterna/@cognome")).getValue() + " " + ((Attribute) doc.selectSingleNode("persona_esterna/@nome")).getValue());
			rif.setCod(((Attribute) doc.selectSingleNode("persona_esterna/@matricola")).getValue());
		}
		
		if (doc.selectSingleNode(pne + "/@partita_iva") != null)
			rif.setPartitaIva(((Attribute) doc.selectSingleNode(pne + "/@partita_iva")).getValue());
		if (doc.selectSingleNode(pne + "/@codice_fiscale") != null)
			rif.setCodiceFiscale(((Attribute) doc.selectSingleNode(pne + "/@codice_fiscale")).getValue());
		
		// costruzione dell'indirizzo
		String elementoRecapito = pne.equals("struttura_esterna") ? "" : "/recapito";
		String indirizzo = "";
		if (doc.selectSingleNode(pne + elementoRecapito + "/indirizzo") != null)
			indirizzo = ((Element) doc.selectSingleNode(pne + elementoRecapito + "/indirizzo")).getTextTrim();
		if (doc.selectSingleNode(pne + elementoRecapito + "/indirizzo/@cap") != null)
			indirizzo = indirizzo + " - " + ((Attribute) doc.selectSingleNode(pne + elementoRecapito + "/indirizzo/@cap")).getValue();
		if (doc.selectSingleNode(pne + elementoRecapito + "/indirizzo/@comune") != null)
			indirizzo = indirizzo + " " + ((Attribute) doc.selectSingleNode(pne + elementoRecapito + "/indirizzo/@comune")).getValue();
		if (doc.selectSingleNode(pne + elementoRecapito + "/indirizzo/@prov") != null)
			indirizzo = indirizzo + " (" + ((Attribute) doc.selectSingleNode(pne + elementoRecapito + "/indirizzo/@prov")).getValue() + ")";
		if (doc.selectSingleNode(pne + elementoRecapito + "/indirizzo/@nazione") != null)
			indirizzo = indirizzo + " - " + ((Attribute) doc.selectSingleNode(pne + elementoRecapito + "/indirizzo/@nazione")).getValue();
		
		String email = "";
		List<?> emails = doc.selectNodes(pne + elementoRecapito + "/email/@addr");
		if (emails != null && emails.size() > 0) {
			for (int i=0; i<emails.size(); i++) {
				Attribute emailAttr = (Attribute) emails.get(i);
				if (emailAttr != null && emailAttr.getValue() != null && !emailAttr.getValue().equals(""))
					email = email + emailAttr.getValue() + ";";
			}
			
			if (email.length() > 0)
				email = email.substring(0, email.length()-1); // eliminazione dell'ultimo ;
		}
		
		String fax = "";
		if (doc.selectSingleNode(pne + elementoRecapito + "/telefono[@tipo = 'fax']") != null)
			fax = ((Element) doc.selectSingleNode(pne + elementoRecapito + "/telefono[@tipo = 'fax']")).getTextTrim();
		
		if (!indirizzo.isEmpty()|| !email.isEmpty() || !fax.isEmpty()) {
			if (!indirizzo.equals(""))
				rif.setIndirizzo(indirizzo);
			if (!email.equals(""))
				rif.setEmail(email);
			if (!fax.equals(""))
				rif.setFax(fax);
		}
		
		return rif;
	}
	
	/**
	 * Aggiornamento del rif esterno prodotto tramite query su ACL con i dati estratti dalla fatturaPA
	 * @param rifEsterno
	 * @param fatturaPADocument
	 * @param rifElemNameInFatturaPA
	 * @return
	 */
	private RifEsterno updateRifEsternoByDatiFattura(RifEsterno rifEsterno, Document fatturaPADocument, String rifElemNameInFatturaPA) {
		if (rifEsterno != null) {
			// Aggiornamento del nome recuperato da ACL con quello letto dalla fattura
			Node node = fatturaPADocument.selectSingleNode("//FatturaElettronicaHeader/" + rifElemNameInFatturaPA + "/DatiAnagrafici/Anagrafica/Denominazione");
			String nome = (node == null) ? "" : node.getText();
			if (nome.isEmpty()) {
				// mbernardini 04/04/2019 : corretto bug in lettura del nome completo della persona riferita nella fattura
				node = fatturaPADocument.selectSingleNode("//FatturaElettronicaHeader/" + rifElemNameInFatturaPA + "/DatiAnagrafici/Anagrafica/Cognome");
				if (node != null)
					nome = node.getText();
				node = fatturaPADocument.selectSingleNode("//FatturaElettronicaHeader/" + rifElemNameInFatturaPA + "/DatiAnagrafici/Anagrafica/Nome");
				if (node != null)
					nome = nome + " " + node.getText();
				nome = nome.trim();
			}
			if (!nome.isEmpty())
				rifEsterno.setNome(nome);
			
			// Costruzione dell'indirizzo
			node = fatturaPADocument.selectSingleNode("//FatturaElettronicaHeader/" + rifElemNameInFatturaPA + "/Sede/Indirizzo");
			String indirizzo = (node == null)? "" : node.getText();
			node = fatturaPADocument.selectSingleNode("//FatturaElettronicaHeader/" + rifElemNameInFatturaPA + "/Sede/NumeroCivico");
			if (node != null && !node.getText().isEmpty())
				indirizzo += " " + node.getText();
			node = fatturaPADocument.selectSingleNode("//FatturaElettronicaHeader/" + rifElemNameInFatturaPA + "/Sede/CAP");
			if (node != null && !node.getText().isEmpty())
				indirizzo = indirizzo + " - " + node.getText();
			node = fatturaPADocument.selectSingleNode("//FatturaElettronicaHeader/" + rifElemNameInFatturaPA + "/Sede/Comune");
			if (node != null && !node.getText().isEmpty())
				indirizzo = indirizzo + " " + node.getText();
			node = fatturaPADocument.selectSingleNode("//FatturaElettronicaHeader/" + rifElemNameInFatturaPA + "/Sede/Provincia");
			if (node != null && !node.getText().isEmpty())
				indirizzo = indirizzo + " (" + node.getText() + ")";
			node = fatturaPADocument.selectSingleNode("//FatturaElettronicaHeader/" + rifElemNameInFatturaPA + "/Sede/Nazione");
			if (node != null && !node.getText().isEmpty())
				indirizzo = indirizzo + " - " + node.getText();
			indirizzo = indirizzo.trim();
			if (!indirizzo.isEmpty())
				rifEsterno.setIndirizzo(indirizzo);
			
			// Recupero email e fax
			node = fatturaPADocument.selectSingleNode("//FatturaElettronicaHeader/" + rifElemNameInFatturaPA + "/Contatti/Email");
			String email = (node == null)? "" : node.getText();
			if (!email.isEmpty())
				rifEsterno.setEmail(email);
			
			node = fatturaPADocument.selectSingleNode("//FatturaElettronicaHeader/" + rifElemNameInFatturaPA + "/Contatti/Fax");
			String fax = (node == null)? "" : node.getText();
			if (!fax.isEmpty())
				rifEsterno.setFax(fax);
		}
		return rifEsterno;
	}

	@Override
	protected void attachFatturaPANotificationToDocument(ParsedMessage parsedMessage) throws Exception {
		Docway4MailboxConfiguration conf = (Docway4MailboxConfiguration)getConfiguration();
		DocwayParsedMessage dcwParsedMessage = (DocwayParsedMessage)parsedMessage;
		
		//load and lock existing document
		LockedDocument lockedDoc = xwClient.loadAndLockDocument(this.physDocForAttachingFile, conf.getXwLockOpAttempts(), conf.getXwLockOpDelay());
		Document xmlDocument = lockedDoc.getDoc();
		
		try {
			//upload file
			byte []fileContent = (new MessageContentProvider(parsedMessage.getMessage(), false)).getContent();
			// mbernardini 04/04/2019 : aggiunta estensione eml alla notifica relativa alla fatturaPA (stiamo salvando l'intero messaggio e non solo il file XML)
			String fileId = xwClient.addAttach(dcwParsedMessage.getFileNameNotificaFatturaPA()+".eml", fileContent, conf.getXwLockOpAttempts(), conf.getXwLockOpDelay());

			Element fatturaPAEl = (Element)xmlDocument.selectSingleNode("//extra/fatturaPA");
			NotificaItem notificaItem = new NotificaItem();
			notificaItem.setName(fileId);
			notificaItem.setTitle(dcwParsedMessage.getFileNameNotificaFatturaPA());
			notificaItem.setTipo(dcwParsedMessage.getTipoNotificaFatturaPA());
			notificaItem.setData(super.currentDate);
			notificaItem.setOra(super.currentDate);
			notificaItem.setInfo(FatturaPAUtils.getInfoNotifica(notificaItem.getTipo(), notificaItem.getTitle()));

			Node node = dcwParsedMessage.getNotificaFatturaPADocument().selectSingleNode("//RiferimentoFattura/NumeroFattura");
			if (node != null)
				notificaItem.setNumeroFattura(node.getText());
			
			node = dcwParsedMessage.getNotificaFatturaPADocument().selectSingleNode("//RiferimentoFattura/AnnoFattura");
			if (node != null)
				notificaItem.setAnnoFattura(node.getText());			

			notificaItem.setMessageId(dcwParsedMessage.getMessageId());
			notificaItem.setEsito(FatturaPAUtils.getEsitoNotifica(dcwParsedMessage.getNotificaFatturaPADocument(), notificaItem.getTipo()));
			notificaItem.setNote(FatturaPAUtils.getNoteNotifica(dcwParsedMessage.getNotificaFatturaPADocument(), notificaItem.getTipo()));
			notificaItem.setRiferita("");
			
			for (ErroreItem erroreItem: FatturaPAUtils.getListaErroriNotifica(dcwParsedMessage.getNotificaFatturaPADocument(), notificaItem.getTipo()))
				notificaItem.addErrore(erroreItem);

			// in base al tipo di notifica occorre aggiornare lo stato del documento in termini di fatturaPA
			// TODO occorre gestire le altre tipologie di notifiche (fatture attive)
			if (notificaItem.getTipo().equals(FatturaPAUtils.TIPO_MESSAGGIO_DT))
				fatturaPAEl.addAttribute("state", FatturaPAUtils.TIPO_MESSAGGIO_DT);
			else if (notificaItem.getTipo().equals(FatturaPAUtils.TIPO_MESSAGGIO_SE) && !fatturaPAEl.attributeValue("state", "").equals(FatturaPAUtils.TIPO_MESSAGGIO_DT))
				fatturaPAEl.addAttribute("state", FatturaPAUtils.ATTESA_NOTIFICHE);
			else if (notificaItem.getTipo().equals(FatturaPAUtils.TIPO_MESSAGGIO_NS))
				fatturaPAEl.addAttribute("state", FatturaPAUtils.ATTESA_INVIO); // occorrera' eseguire un nuovo invio
			
			// tentativo di collegamento fra le diverse ricevute sul documento
			Element elNotifica = getNotificaRiferita(xmlDocument, notificaItem.getTipo(), notificaItem.getNumeroFattura(), notificaItem.getAnnoFattura());
			if (elNotifica != null)
				elNotifica.addAttribute("riferita", notificaItem.getTipo());
				
			fatturaPAEl.add(Docway4EntityToXmlUtils.notificaItemToXml(notificaItem));
            
			xwClient.saveDocument(xmlDocument, this.physDocForAttachingFile, lockedDoc.getTheLock());
			
            // se si sta analizzando una notifica relativa ad un invio di fatturaPA attiva occorre verificare se in ACL e' gia' registrato
            // l'indirizzo email del SdI da utilizzare per successive comunicazioni. In caso contrario occorre registrarlo sull'ufficio specificato
            // come RPA del documento
			String codUffRpa = "";
            if (notificaItem.getTipo().equals(FatturaPAUtils.TIPO_MESSAGGIO_RC) || notificaItem.getTipo().equals(FatturaPAUtils.TIPO_MESSAGGIO_NS) || notificaItem.getTipo().equals(FatturaPAUtils.TIPO_MESSAGGIO_MC)) {
    			Element rpa = (Element) xmlDocument.selectSingleNode("/doc/rif_interni/rif[@diritto = 'RPA']"); // recupero il codice ufficio impostato come RPA
    			if (rpa != null)
    				codUffRpa = rpa.attributeValue("cod_uff", "");
    			String emailFrom = dcwParsedMessage.getMittenteAddressFromDatiCertPec();
            	updateEmailSdIinACL(codUffRpa, emailFrom);
            }				
			
		}
		catch (Exception e) {
			try {
				xwClient.unlockDocument(this.physDocForAttachingFile, lockedDoc.getTheLock());
			}
			catch (Exception unlockE) {
				; //do nothing
			}
			throw e;
		}		
	}
	
	private Element getNotificaRiferita(Document document, String tipoNotificaCorrente, String numeroFattura, String annoFattura) {
		Element el = null;
		String tipoNotificaRiferita = FatturaPAUtils.getTipoNotificaRiferita(tipoNotificaCorrente);
		
		if (tipoNotificaRiferita != null && tipoNotificaRiferita.length() > 0 
				&& document != null && tipoNotificaCorrente != null && tipoNotificaCorrente.length() > 0) {
			
			// tento di recuperare la notifica alla quale si riferisce la notifica corrente
			if (numeroFattura != null && numeroFattura.length() > 0 && annoFattura != null && annoFattura.length() > 0)
				// notifica su specifica fattura
				el = (Element) document.selectSingleNode("//extra/fatturaPA/notifica[@numeroFattura='" + numeroFattura + "' and @annoFattura='" + annoFattura + "' and @tipo='" + tipoNotificaRiferita + "' and @riferita='']");
			else
				// notifica su intero documento (fattura singola o intero lotto di fatture)
				el = (Element) document.selectSingleNode("//extra/fatturaPA/notifica[@tipo='" + tipoNotificaRiferita + "' and @riferita='']");
		}
		return el;
	}
	
	private void updateEmailSdIinACL(String codUff, String emailAddressSdI) throws Exception {
		Docway4MailboxConfiguration conf = (Docway4MailboxConfiguration)getConfiguration();
		if (codUff != null && codUff.length() > 0 && emailAddressSdI != null && emailAddressSdI.length() > 0) {
			try {
	            QueryResult qr = aclClient.search("[/struttura_interna/@cod_uff/]=\"" + codUff + "\"");
				if (qr.elements > 0) {
					Document document = aclClient.loadDocByQueryResult(0, qr);
					Node node = document.selectSingleNode("/struttura_interna/fatturaPA/@emailSdI");
					if (node == null || node.getText().isEmpty()) {
						if (logger.isInfoEnabled())
							logger.info("[" + conf.getAddress() + "] updating SdI email [" + emailAddressSdI + "] for SI [" + codUff + "]");
						
						int pD = aclClient.getPhysdocByQueryResult(0, qr);
						LockedDocument lockedDoc = aclClient.loadAndLockDocument(pD, conf.getXwLockOpAttempts(), conf.getXwLockOpDelay());
						try {
							document = lockedDoc.getDoc();

							Element fatturaPAEl = document.getRootElement().element("fatturaPA");
							if (fatturaPAEl == null)
								fatturaPAEl = document.getRootElement().addElement("fatturaPA");
							fatturaPAEl.addAttribute("emailSdI", emailAddressSdI);
							
							aclClient.saveDocument(document, pD, lockedDoc.getTheLock());
						}
						catch (Exception e) {
							logger.error("[" + conf.getAddress() + "]. Unexpected error updating SdI email [" + emailAddressSdI + "] for SI [" + codUff + "]", e);
							try {
								aclClient.unlockDocument(pD, lockedDoc.getTheLock());
							}
							catch (Exception e1) {
								; //do nothing
							}
						}						
					}
				}
			}
			catch (Exception e) {
				logger.error("[" + conf.getAddress() + "]. Unexpected error updating SdI email [" + emailAddressSdI + "] for SI [" + codUff + "]", e);
			}
		}
	}

	@Override
	protected void attachFatturaPAPecReceiptToDocument(ParsedMessage parsedMessage) throws Exception {
		Docway4MailboxConfiguration conf = (Docway4MailboxConfiguration)getConfiguration();
		
		String subject = parsedMessage.getSubject().trim();
		String receiptTypeBySubject = subject.substring(0, parsedMessage.getSubject().indexOf(":"));
		receiptTypeBySubject = receiptTypeBySubject.substring(0, 1).toUpperCase() + receiptTypeBySubject.substring(1).toLowerCase(); //capitalize only first letter		
		
  		int fatturaPosition = -1;
		try {
			String identificazioneFattura = subject.substring(subject.indexOf("(FTRPA-") + 7, subject.indexOf(")") );
			if (!identificazioneFattura.equals("doc"))
				fatturaPosition = Integer.parseInt(identificazioneFattura);
		}
		catch (NumberFormatException e) {
			logger.warn("[" + conf.getAddress() + "]. Unexpected error parsing fatturaPA index number from subject [" + parsedMessage.getSubject() + "]", e);
		}		
		
		//load and lock existing document
		LockedDocument lockedDoc = xwClient.loadAndLockDocument(this.physDocForAttachingFile, conf.getXwLockOpAttempts(), conf.getXwLockOpDelay());
		Document xmlDocument = lockedDoc.getDoc();
		
		try {
			//upload file
			byte []fileContent = (new MessageContentProvider(parsedMessage.getMessage(), false)).getContent();
			String fileId = xwClient.addAttach(receiptTypeBySubject + ".eml", fileContent, conf.getXwLockOpAttempts(), conf.getXwLockOpDelay());
			
			//identificazione fattura per aggangio ricevuta PEC
			Element fatturaPAEl = (Element)xmlDocument.selectSingleNode("//extra/fatturaPA");
        	String numFattura = "";
        	String annoFattura = "";
        	if (fatturaPosition > 0) {
        		try {
            		List<?> listafatture = fatturaPAEl.elements("datiFattura");
            		if ((fatturaPosition-1) < listafatture.size()) {
            			Element datiSingolaFattura = (Element) listafatture.get(fatturaPosition-1);
            			if (datiSingolaFattura != null) {
            				numFattura = datiSingolaFattura.element("datiGeneraliDocumento").attributeValue("numero");
            				String dataFattura = datiSingolaFattura.element("datiGeneraliDocumento").attributeValue("data");
            				if (dataFattura != null && dataFattura.length() > 0)
            					annoFattura = dataFattura.substring(0, 4);
            			}
            		}
        		}
        		catch (Exception e) {
        			logger.warn("[" + conf.getAddress() + "]. Unexpected error identifing fatturaPA [index: " + fatturaPosition + "] in document [physodc: " + this.physDocForAttachingFile + "]", e);
        		}
        	}			
			
			NotificaItem notificaItem = new NotificaItem();
			notificaItem.setName(fileId);
			notificaItem.setTitle(receiptTypeBySubject + ".eml");
			notificaItem.setData(super.currentDate);
			notificaItem.setOra(super.currentDate);
			notificaItem.setInfo(receiptTypeBySubject);
			notificaItem.setMessageId(parsedMessage.getMessageId());
            if (numFattura != null && numFattura.trim().length() > 0 && annoFattura != null && annoFattura.trim().length() > 0) { //si collega la ricevuta a una specifica fattura
    			notificaItem.setNumeroFattura(numFattura);
    			notificaItem.setAnnoFattura(annoFattura);
    		}
			fatturaPAEl.add(Docway4EntityToXmlUtils.notificaItemToXml(notificaItem));
            
			xwClient.saveDocument(xmlDocument, this.physDocForAttachingFile, lockedDoc.getTheLock());
		}
		catch (Exception e) {
			try {
				xwClient.unlockDocument(this.physDocForAttachingFile, lockedDoc.getTheLock());
			}
			catch (Exception unlockE) {
				; //do nothing
			}
			throw e;
		}				
	}

	@Override
	protected FascicoloReference findCodFascicoloByTags(String codammaoo, List<String> tags) throws MultipleFoldersException, Exception {
		FascicoloReference fascicolo = null;
		
		// costruzione della query extraway in base ai TAGS indicati
		String query = "";
		if (tags != null && !tags.isEmpty()) {
			for (String tag : tags) {
				if (tag != null && !tag.isEmpty()) {
					query += "[/fascicolo/tags/tag/@value]=\"" + tag + "\" AND ";
				}
			}
			if (query.endsWith(" AND "))
				query = query.substring(0, query.length()-5);
			
			if (!query.trim().isEmpty() && codammaoo != null && !codammaoo.isEmpty())
				query += " AND [/fascicolo/@cod_amm_aoo]=\"" + codammaoo + "\"";
		}
		
		if (!query.isEmpty()) {
			QueryResult qr = xwClient.search(query);
			if (qr.elements == 1) {
				
				Document doc = xwClient.loadDocByQueryResult(0, qr);
				if (doc != null) {
					Element root = doc.getRootElement();
					fascicolo = new FascicoloReference(root.attributeValue("numero", ""), root.elementTextTrim("oggetto"));
					
					// recupero della classificazione del fascicolo
					Element classif = root.element("classif");
					if (classif != null) {
						String codClassif = classif.attributeValue("cod");
						if (codClassif != null && !codClassif.isEmpty()) {
							fascicolo.setCodClassif(codClassif);
							fascicolo.setDescrClassif(classif.getTextTrim());
						}
					}
					
					// recupero dei rif interni del fascicolo...
					List<RifInterno> rifsL = new ArrayList<>();
					List<?> nodes = doc.selectNodes("/fascicolo/rif_interni/rif");
					if (nodes != null && !nodes.isEmpty()) {
						for (int i=0; i<nodes.size(); i++) {
							Element el = (Element) nodes.get(i);
							if (el != null) {
								RifInterno rifInterno = new RifInterno();
								boolean isRuolo = el.attributeValue("tipo_uff", "").equalsIgnoreCase("ruolo");
								if (isRuolo) {
									rifInterno.setRuolo(el.attributeValue("nome_uff"), el.attributeValue("cod_uff"));									
								}
								else {
									rifInterno.setCodPersona(el.attributeValue("cod_persona"));
									rifInterno.setNomePersona(el.attributeValue("nome_persona"));
									rifInterno.setCodUff(el.attributeValue("cod_uff"));
									rifInterno.setNomeUff(el.attributeValue("nome_uff"));
								}
								rifInterno.setDiritto(el.attributeValue("diritto"));
								rifInterno.setIntervento(el.attributeValue("diritto", "").equalsIgnoreCase("rpa") || el.attributeValue("intervento", "").equalsIgnoreCase("si"));
								
								rifsL.add(rifInterno);
							}
						}
					}
					if (rifsL.size() > 0)
						fascicolo.setRifs(rifsL);
				}
			}
			else if (qr.elements > 1) {
				// trovati piu' fascicoli in base ai TAGS specificati
				throw new MultipleFoldersException(tags);
			}
		}
		
		return fascicolo;
	}

	@Override
	protected boolean isMittenteInterno(ParsedMessage message) throws Exception {
		return this.isEmailAddressInterno(message.getFromAddress());
	}
	
	/**
	 * Ritorna true se la mail indicato come parametro si riferisce ad una persona interna, false altrimenti
	 * @param address Indirizzo da verificare
	 * @return
	 * @throws Exception
	 */
	private boolean isEmailAddressInterno(String address) throws Exception {
		if (address != null && !address.isEmpty()) {
			Docway4MailboxConfiguration conf = (Docway4MailboxConfiguration)getConfiguration();
			QueryResult qr = aclClient.search("[persint_recapitoemailaddr]=\"" + address + "\" AND [persint_codammaoo]=\"" + conf.getCodAmmAoo() + "\"");
			return qr != null && qr.elements > 0;
		}
		else
			return false;
	}

	@Override
	protected boolean containsDestinatariEsterni(ParsedMessage message) throws Exception {
		int to = message.getToAddresses() != null ? message.getToAddresses().length : 0;
		int cc = message.getCcAddresses() != null ? message.getCcAddresses().length : 0;
		return countRifInterniByEmailAddresses(message.getToAddresses()) < to || countRifInterniByEmailAddresses(message.getCcAddresses()) < cc;
	}
	
	@Override
	protected boolean containsDestinatariInterni(ParsedMessage message) throws Exception {
		return countRifInterniByEmailAddresses(message.getToAddresses()) > 0 || countRifInterniByEmailAddresses(message.getCcAddresses()) > 0;
	}
	
	/**
	 * Verifica se almeno uno degli indirizzi email passati corrisponde ad un rif. interno (persona/struttura interna o 
	 * ruolo)
	 * @param addresses
	 * @return
	 * @throws Exception
	 */
	private int countRifInterniByEmailAddresses(InternetAddress[] addresses) throws Exception {
		int count = 0;
		if (addresses != null && addresses.length > 0) {
			Docway4MailboxConfiguration conf = (Docway4MailboxConfiguration)getConfiguration();
			
			for (int i=0; i<addresses.length; i++) {
				if (addresses[i] != null) {
					String query = "(([/persona_interna/recapito/email/@addr/]=\"" 
							+ addresses[i].getAddress()
							+ "\" OR [/persona_interna/recapito/email_certificata/@addr/]=\"" 
							+ addresses[i].getAddress()
							+ "\") AND [persint_codammaoo]=\"" 
							+ conf.getCodAmmAoo() 
							+ "\") OR "
							+ "(([/struttura_interna/email/@addr/]=\"" 
							+ addresses[i].getAddress()
							+ "\" OR [/struttura_interna/email_certificata/@addr/]=\"" 
							+ addresses[i].getAddress()
							+ "\") AND [struint_codammaoo]=\"" 
							+ conf.getCodAmmAoo() 
							+ "\") OR "
							+ "(([/ruolo/email/@addr/]=\"" 
							+ addresses[i].getAddress()
							+ "\" OR [/ruolo/email_certificata/@addr/]=\"" 
							+ addresses[i].getAddress()
							+ "\") AND [ruoli_codammaoo]=\"" 
							+ conf.getCodAmmAoo() 
							+ "\") OR"
							+ "([/casellaPostaElettronica/mailbox_in/@email/]=\"" 
							+ addresses[i].getAddress() 
							+ "\" AND [casellapostaelettronica_codammaoo]=\"" 
							+ conf.getCodAmmAoo() + "\")";
					
					QueryResult qr = aclClient.search(query);
					if (qr != null && qr.elements > 0)
						count++;
				}
			}
		}
		return count;
	}
	
}
