package it.tredi.msa.mailboxmanager.docway;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;

import it.highwaytech.db.QueryResult;
import it.tredi.extraway.ExtrawayClient;
import it.tredi.mail.MailSender;
import it.tredi.msa.Services;
import it.tredi.msa.configuration.docway.AssegnatarioMailboxConfiguration;
import it.tredi.msa.configuration.docway.Docway4MailboxConfiguration;
import it.tredi.msa.mailboxmanager.MessageContentProvider;
import it.tredi.msa.mailboxmanager.ParsedMessage;
import it.tredi.msa.mailboxmanager.docway.fatturapa.ErroreItem;
import it.tredi.msa.mailboxmanager.docway.fatturapa.FatturaPAUtils;
import it.tredi.msa.mailboxmanager.docway.fatturapa.NotificaItem;
import it.tredi.msa.notification.MailNotificationSender;

/**
 * Estensione della gestione delle mailbox (lettura, elaborazione messaggi, ecc.) specifica per DocWay4 (es. chiamate eXtraWay)
 */
public class Docway4MailboxManager extends DocwayMailboxManager {

	protected ExtrawayClient xwClient;
	protected ExtrawayClient aclClient;
	private boolean extRestrictionsOnAcl;
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
			logger.warn("[" + conf.getName() + "] failed to close eXtraWay session [" + conf.getXwDb() + "]", e);			
		}
		try {
			if (aclClient != null)
				aclClient.disconnect();
		}
		catch (Exception e) {
			logger.warn("[" + conf.getName() + "] failed to close eXtraWay session [" + conf.getAclDb() + "]", e);
		}
	}
	
	@Override
	protected StoreType decodeStoreType(ParsedMessage parsedMessage) throws Exception {
		StoreType storeType = null;
		Docway4MailboxConfiguration conf = (Docway4MailboxConfiguration)getConfiguration();
		DocwayParsedMessage dcwParsedMessage = (DocwayParsedMessage)parsedMessage;
		
		if (conf.isPec()) { //casella PEC
			
			if (dcwParsedMessage.isPecReceipt() || dcwParsedMessage.isNotificaInteropPAMessage(conf.getCodAmmInteropPA(), conf.getCodAooInteropPA()) ||
					(conf.isEnableFatturePA() && dcwParsedMessage.isNotificaFatturaPAMessage(conf.getSdiDomainAddress()))) { //messaggio è una ricevuta PEC oppure è una notifica (messaggio di ritorno) di interoperabilità PA oppure è una notifica di fatturaPA
				String query = "([/doc/rif_esterni/rif/interoperabilita/@messageId]=\"" + dcwParsedMessage.getMessageId() + "\" OR [/doc/rif_esterni/interoperabilita_multipla/interoperabilita/@messageId]=\"" + dcwParsedMessage.getMessageId() + "\""
						+ " OR [/doc/extra/fatturaPA/notifica/@messageId]=\"" + dcwParsedMessage.getMessageId() + "\") AND [/doc/@cod_amm_aoo/]=\"" + conf.getCodAmmAoo() + "\"";
				if (xwClient.search(query) > 0)
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
					int count = xwClient.search(query);
					for (int i=0; i<count; i++) {
						Document xmlDocument = xwClient.loadDocByQueryResult(i);
						Node node = xmlDocument.selectSingleNode("/doc/extra/fatturaPA[@fileNameFattura='" + dcwParsedMessage.getFileNameFatturaRiferita() + "']");
						if (node != null) {
							this.physDocForAttachingFile = xwClient.getPhysdocByQueryResult(i);
							return StoreType.ATTACH_FATTURA_PA_NOTIFICATION;
						}
					}
					throw new Exception(String.format(DOC_NOT_FOUND_FOR_ATTACHING_NOTIFICA_PA_FILE, query));
				}
				
				if (query.length() > 0) { //trovato doc a cui allegare file
					int count = xwClient.search(query);
					if (count > 0) {
						this.physDocForAttachingFile = xwClient.getPhysdocByQueryResult(0);
						return storeType;
					}
					else
						dcwParsedMessage.addRelevantMessage(String.format(DOC_NOT_FOUND_FOR_ATTACHING_FILE, query));
				}
				else if (dcwParsedMessage.isPecReceipt()) { //ricevuta PEC (non relativa a interopPA/fatturaPA)
					if (conf.isIgnoreStandardOrphanPecReceipts()) {
						// property attiva per evitare l'archiviazione -> il messaggio viene ignorato e rimane sulla casella di posta
						return StoreType.IGNORE_MESSAGE;
					}
					// mbernardini 21/01/2019 : se il salvataggio riguarda ricevute orfane potrebbe essere stato richiesto il salvataggio
					// come documento non protocollato
					else if (conf.isOrphanPecReceiptsAsVarie()) {
						return StoreType.SAVE_ORPHAN_PEC_RECEIPT_AS_VARIE;
					}
				}
			}
			else if (dcwParsedMessage.isSegnaturaInteropPAMessage(conf.getCodAmmInteropPA(), conf.getCodAooInteropPA())) { //messaggio di segnatura di interoperabilità PA
				String query = "[/doc/@messageId]=\"" + parsedMessage.getMessageId() + "\" AND [/doc/@cod_amm_aoo]=\"" + conf.getCodAmmAoo() + "\"";
				int count = xwClient.search(query);

				if (count == 0) { //2nd try: potrebbe essere la stessa segnatura su messaggi diversi
					query = dcwParsedMessage.buildQueryForDocway4DocumentFromInteropPASegnatura(conf.getCodAmm(), conf.getCodAoo());
					count = xwClient.search(query);
				}

				if (count > 0) { //messageId found
					Document xmlDocument = xwClient.loadDocByQueryResult(0);
					Element archiviatoreEl = (Element)xmlDocument.selectSingleNode("/doc/archiviatore[@recipientEmail='" + conf.getEmail() + "']");
					if (archiviatoreEl != null) { //same mailbox
						if (archiviatoreEl.attribute("completed") != null && archiviatoreEl.attributeValue("completed").equals("no")) {
							this.physDocToUpdate = xwClient.getPhysdocByQueryResult(0);
							return StoreType.UPDATE_PARTIAL_DOCUMENT_INTEROP_PA;
						}
						else
							return StoreType.SKIP_DOCUMENT;						
					}
					else { //different mailbox
						this.physDocToUpdate = xwClient.getPhysdocByQueryResult(0);
						return StoreType.UPDATE_NEW_RECIPIENT_INTEROP_PA;
					}
				}
				else //messageId not found
					return StoreType.SAVE_NEW_DOCUMENT_INTEROP_PA;
			}
			else if (conf.isEnableFatturePA() && dcwParsedMessage.isFatturaPAMessage(conf.getSdiDomainAddress())) { //messaggio fattura PA
				String query = "[/doc/@messageId]=\"" + parsedMessage.getMessageId() + "\" AND [/doc/@cod_amm_aoo]=\"" + conf.getCodAmmAoo() + "\"";
				int count = xwClient.search(query);
				if (count > 0) { //messageId found
					Document xmlDocument = xwClient.loadDocByQueryResult(0);
					Element archiviatoreEl = (Element)xmlDocument.selectSingleNode("/doc/archiviatore[@recipientEmail='" + conf.getEmail() + "']");
					if (archiviatoreEl.attribute("completed") != null && archiviatoreEl.attributeValue("completed").equals("no")) {
						this.physDocToUpdate = xwClient.getPhysdocByQueryResult(0);
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
		
		int count = xwClient.search(query);
		if (count > 0) { //messageId found
			Document xmlDocument = xwClient.loadDocByQueryResult(0);
			Element archiviatoreEl = (Element)xmlDocument.selectSingleNode("/doc/archiviatore[@recipientEmail='" + conf.getEmail() + "']");
			if (archiviatoreEl != null) { //same mailbox
				if (archiviatoreEl.attribute("completed") != null && archiviatoreEl.attributeValue("completed").equals("no")) {
					this.physDocToUpdate = xwClient.getPhysdocByQueryResult(0);
					return StoreType.UPDATE_PARTIAL_DOCUMENT;
				}
				else
					return StoreType.SKIP_DOCUMENT;
			}
			else { //different mailbox
				this.physDocToUpdate = xwClient.getPhysdocByQueryResult(0);
				return StoreType.UPDATE_NEW_RECIPIENT;
			}
		}
		else {
			//messageId not found
			return StoreType.SAVE_NEW_DOCUMENT;
		}
	}  	
	
	@Override
	protected Object saveNewDocument(DocwayDocument doc, ParsedMessage parsedMessage) throws Exception {
		Docway4MailboxConfiguration conf = (Docway4MailboxConfiguration)getConfiguration();
		DocwayParsedMessage dcwParsedMessage = (DocwayParsedMessage)parsedMessage;
		
		//save new document in Extraway
		Document xmlDocument = Docway4EntityToXmlUtils.docwayDocumentToXml(doc, super.currentDate);
		int lastSavedDocumentPhysDoc = xwClient.saveNewDocument(xmlDocument);
		parsedMessage.clearRelevantMessages();
		
		//load and lock document
		xmlDocument = xwClient.loadAndLockDocument(lastSavedDocumentPhysDoc, conf.getXwLockOpAttempts(), conf.getXwLockOpDelay());

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
						logger.error("[" + conf.getName() + "] error sending ConfermaRicezione InteropPA message.", e);
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
					logger.error("[" + conf.getName() + "] error sending NotificaEccezione InteropPA message.", e);
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
				xwClient.saveDocument(xmlDocument, lastSavedDocumentPhysDoc);
				xmlDocument = xwClient.loadAndLockDocument(lastSavedDocumentPhysDoc, conf.getXwLockOpAttempts(), conf.getXwLockOpDelay());
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
				xwClient.saveDocument(xmlDocument, lastSavedDocumentPhysDoc);
			}
			else { //no filed uploaded -> unlock document
				xwClient.unlockDocument(lastSavedDocumentPhysDoc);
			}			
		}
		catch (Exception e) {
			try {
				xwClient.unlockDocument(lastSavedDocumentPhysDoc);
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
	
	@Override
    public RifEsterno createRifEsterno(String name, String address) throws Exception {
        RifEsterno rifEsterno = new RifEsterno();
        rifEsterno.setEmail(address);

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

        // first try: search email address
        int count = aclClient.search(query, null, "ud(xpart:/xw/@UdType)", 0, 0);
        if (count == 0) { // sender is not present in ACL
            rifEsterno.setNome(name);
        }
        else { // extract sender info from ACL
            Document document = aclClient.loadDocByQueryResult(0);
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
                	count = aclClient.search(queryStruest);
                	QueryResult selezione = aclClient.getQueryResult();

                    if (count > 0) { // at least one struttura_esterna found
                        if (count > 1) {
                            String emailDomain = address.substring(address.indexOf("@"));
                            queryStruest = "[struest_emailaddr]=\"*" + emailDomain + "\"";
                            if (!cod_amm.isEmpty() && !cod_aoo.isEmpty())
                        		queryStruest += " AND [/struttura_esterna/#cod_ammaoo]=\"" + cod_amm + cod_aoo + "\"";

                            int count1 = aclClient.search(queryStruest, selezione.id, "", 0, 0);
                            if (count1 > 0) {
                                ; //uso la nuova selezione (quella raffinata)
                            }
                            else {
                            	aclClient.setQueryResult(selezione);
                            }
                        }
                        document = aclClient.loadDocByQueryResult(0);

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
		int count = aclClient.search(query);
		if (count == 0)
			return null;
		for (int i=0; i<count; i++) { //per ogni persona interna
			RifInterno rifInterno = new RifInterno();
	        Document document = aclClient.loadDocByQueryResult(i);
	        String codPersona = ((Attribute)document.selectSingleNode("persona_interna/@matricola")).getValue();
	        String nomePersona = ((Attribute)document.selectSingleNode("persona_interna/@cognome")).getValue() + " " + ((Attribute)document.selectSingleNode("persona_interna/@nome")).getValue();
	        String codUff = ((Attribute)document.selectSingleNode("persona_interna/@cod_uff")).getValue();
	        String codAmmAoo = ((Attribute)document.selectSingleNode("persona_interna/@cod_amm")).getValue() + ((Attribute)document.selectSingleNode("persona_interna/@cod_aoo")).getValue();
	        rifInterno.setCodPersona(codPersona);
	        rifInterno.setNomePersona(nomePersona);
	        rifInterno.setCodUff(codUff);
	        rifsL.add(rifInterno);
			aclClient.search("[struint_coduff]=\"" + rifInterno.getCodUff() + "\" AND [/struttura_interna/#cod_ammaoo/]=\"" + codAmmAoo + "\""); //estrazione nome ufficio
	        document = aclClient.loadDocByQueryResult(0);
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
			aclClient.search(query);
	        Document document = aclClient.loadDocByQueryResult(0);
	        String nomeRuolo = document.getRootElement().elementText("nome").trim();
			rifInterno.setRuolo(nomeRuolo, assegnatario.getCodRuolo());
			rifInterno.setIntervento(assegnatario.isIntervento());			        
		}
		else { //persona-ufficio
			rifInterno.setCodPersona(assegnatario.getCodPersona());
			rifInterno.setCodUff(assegnatario.getCodUff());
			rifInterno.setIntervento(assegnatario.isIntervento());
			
			aclClient.search("[struint_coduff]=\"" + rifInterno.getCodUff() + "\" AND [/struttura_interna/#cod_ammaoo/]=\"" + codAmmAoo + "\"");
	        Document document = aclClient.loadDocByQueryResult(0);
	        String nomeUff = document.getRootElement().elementText("nome").trim();
	        rifInterno.setNomeUff(nomeUff);				

			aclClient.search("[/persona_interna/@matricola]=\"" + rifInterno.getCodPersona() + "\" AND [persint_codammaoo]=\"" + codAmmAoo + "\"");
	        document = aclClient.loadDocByQueryResult(0);
	        String nomePersona = ((Attribute)document.selectSingleNode("persona_interna/@cognome")).getValue() + " " + ((Attribute)document.selectSingleNode("persona_interna/@nome")).getValue();
			rifInterno.setNomePersona(nomePersona);
		}
		return rifInterno;
	}
	
	@Override
	protected List<RifInterno> createRifInterni(ParsedMessage parsedMessage) throws Exception {
		Docway4MailboxConfiguration conf = (Docway4MailboxConfiguration)getConfiguration();
		List<RifInterno> rifInterni = new ArrayList<RifInterno>();
		String codAmmAoo = ((Docway4MailboxConfiguration)super.getConfiguration()).getCodAmmAoo();
		
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
				rifsL = createRifInterniByPersintQuery("[persint_recapitoemailaddr]=\"" + parsedMessage.getFromAddress() + "\" AND [persint_codammaoo]=\"" + codAmmAoo + "\"");
				for (RifInterno cc:rifsL) {
					cc.setDiritto("CC");
					rifInterni.add(cc);
				}
			}
		}
		
		for (AssegnatarioMailboxConfiguration assegnatario: conf.getAssegnatariCC()) {
			RifInterno cc = createRifInternoByAssegnatario(assegnatario);
			cc.setDiritto("CC");
			rifInterni.add(cc);
		}
		
		return rifInterni;
	}

	@Override
	protected void sendNotificationEmails(DocwayDocument doc, Object saveDocRetObj) {
		Docway4MailboxConfiguration conf = (Docway4MailboxConfiguration)getConfiguration();
		MailNotificationSender notificationSender = (MailNotificationSender)Services.getNotificationService().getNotificationSender();
		MailSender mailSender = notificationSender.createMailSender();
		try {
			mailSender.connect();
			String body = Docway4NotificationEmailsUtils.getBodyForEmail(conf.getNotificationAppHost(), conf.getNotificationAppHost1(), conf.getNotificationAppUri(), conf.getXwDb(), (Document)saveDocRetObj);
			
			Set<String>	notifiedAddresses = new HashSet<String>();
			for (RifInterno rifInterno:doc.getRifInterni()) {
				if (rifInterno.isNotify()) { //if rif interno has to be notified
					if ((rifInterno.getDiritto().equals("RPA") && conf.isNotifyRPA()) || (!rifInterno.getDiritto().equals("RPA") && conf.isNotifyCC()))
						sendNotificationEmail(mailSender, notificationSender.getSenderAdress(), notificationSender.getSenderPersonal(), rifInterno.getCodPersona(), rifInterno.getDiritto().equals("RPA"), doc, (Document)saveDocRetObj, body, conf.getCodAmmAoo(), notifiedAddresses);
				}
			}				
		} 
		catch (Exception e) {
			logger.error("[" + conf.getName() + "] unexpected error sending notification emails", e);
		}
		finally {
			try {
				mailSender.disconnect();
			} 
			catch (Exception e) {
				logger.warn("[" + conf.getName() + "] failed to close mailSender session", e);
			}				
		}
	}

	private void sendNotificationEmail(MailSender mailSender, String senderAddress, String senderPersonal, String matricola, boolean isRPA, DocwayDocument doc, Document savedDocument, String body, String codAmmAooDestinatario, Set<String> notifiedAddresses) {
		Docway4MailboxConfiguration conf = (Docway4MailboxConfiguration)getConfiguration();
		try {
			String subject = Docway4NotificationEmailsUtils.getSubjectForEmail(isRPA?"RPA":"CC", savedDocument);
			String destEmail = getEmailWithMatricola(matricola, codAmmAooDestinatario);
			String []destinatari = destEmail.split(",");
			for (String dest:destinatari) {
				if (!dest.isEmpty() && !notifiedAddresses.contains(dest)) {
					try {
						if (logger.isInfoEnabled())
							logger.info("[" + conf.getName() + "] sending notification email [" + dest + "]");
						notifiedAddresses.add(dest);
						mailSender.sendMail(senderAddress, senderPersonal, dest, subject, body);	
					}
					catch (Exception e) {
						logger.error("[" + conf.getName() + "] unexpected error sending notification email [" + dest + "]", e);
					}
				}
			}
			
		} 
		catch (Exception e) {
			logger.error("[" + conf.getName() + "] unexpected error extracting email address for matricola [" + matricola + "]", e);
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

		int count = aclClient.search(query);
		for (int i=0; i<count; i++) {
			Document document = aclClient.loadDocByQueryResult(i);
			Attribute indirizzoEl = (Attribute)document.selectSingleNode("/persona_interna/recapito/email/@addr");
			if (indirizzoEl != null) {
				String indirizzo = indirizzoEl.getText().trim();
				if (!indirizzo.isEmpty())
					res += "," + indirizzo;
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
		Document xmlDocument = xwClient.loadAndLockDocument(this.physDocToUpdate, conf.getXwLockOpAttempts(), conf.getXwLockOpDelay());
		
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
				xwClient.saveDocument(xmlDocument, this.physDocToUpdate);
			}
			else { //no filed uploaded -> unlock document
				xwClient.unlockDocument(this.physDocToUpdate);
			}			
		}
		catch (Exception e) {
			try {
				xwClient.unlockDocument(this.physDocToUpdate);
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
		Document xmlDocument = xwClient.loadAndLockDocument(this.physDocToUpdate, conf.getXwLockOpAttempts(), conf.getXwLockOpDelay());
		
		try {
			Element docEl = xmlDocument.getRootElement();
			Element rifIntEl = docEl.element("rif_interni");
			
			//update document with new mailbox and CCs
			Element archiviatoreEl = DocumentHelper.createElement("archiviatore");
			docEl.add(archiviatoreEl);
			archiviatoreEl.addAttribute("recipientEmail", doc.getRecipientEmail());
			for (RifInterno rifInterno:doc.getRifInterni()) {
				//RPA deve essere trasformato in CC con diritto di intervento
				if (rifInterno.getDiritto().equals("RPA")) {
					rifInterno.setDiritto("CC");
					rifInterno.setIntervento(true);
				}
				if (isNewRifInterno(rifInterno, rifIntEl)) {
					rifIntEl.add(Docway4EntityToXmlUtils.rifInternoToXml(rifInterno));
				}
				else 
					rifInterno.setNotify(false);
			}
			
			updateXmlWithDocwayFiles(xmlDocument, doc);
			xwClient.saveDocument(xmlDocument, this.physDocToUpdate);
		}
		catch (Exception e) {
			try {
				xwClient.unlockDocument(this.physDocToUpdate);
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
		if (identificatoreEl != null)
			numero = identificatoreEl.elementText("DataRegistrazione").substring(0, 4) + "-" + identificatoreEl.elementText("CodiceAmministrazione") + identificatoreEl.elementText("CodiceAOO") + "-" + identificatoreEl.elementText("NumeroRegistrazione");
		attachInteropPAFileToDocument(parsedMessage, info, dcwParsedMessage.getMittenteAddressFromDatiCertPec(), numero);
	}
	
	private void attachInteropPAFileToDocument(ParsedMessage parsedMessage, String fileInfo, String rifEstAddress, String numero) throws Exception {
		Docway4MailboxConfiguration conf = (Docway4MailboxConfiguration)getConfiguration();
		
		//load and lock existing document
		Document xmlDocument = xwClient.loadAndLockDocument(this.physDocForAttachingFile, conf.getXwLockOpAttempts(), conf.getXwLockOpDelay());
		
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
                	Element emailCertificataEl = el.element("email_certificata");
                	if (emailCertificataEl != null && emailCertificataEl.attributeValue("addr", "").equals(rifEstAddress)) {
                		rifEl = el;
                		if (numero != null && !numero.isEmpty() && rifEl.attributeValue("n_prot", "").isEmpty()) //si aggiunge il numero di protocollo al rif esterno se manca
                			rifEl.addAttribute("n_prot", numero);
                		break;
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
            
			xwClient.saveDocument(xmlDocument, this.physDocForAttachingFile);
		}
		catch (Exception e) {
			try {
				xwClient.unlockDocument(this.physDocForAttachingFile);
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

			int count = aclClient.search("([/struttura_esterna/@partita_iva/]=\"" + piva + "\" AND [/struttura_esterna/#cod_ammaoo/]=\"" + conf.getCodAmmAoo() + "\") OR ([/persona_esterna/@partita_iva/]=\"" + piva + "\" AND [/persona_esterna/#cod_ammaoo/]=\"" + conf.getCodAmmAoo() + "\")");
			if (count == 1) { // e' stata individuata una struttura esterna con la partita iva indicata
				if (logger.isInfoEnabled())
					logger.info("[" + conf.getName() + "] found rif esterno in ACL. Piva [" + piva + "]");
				
				found = true;
				rifEsterno = getRifEsternoFromAcl(aclClient.loadDocByQueryResult(0));
			}
		}
		if (!found && !cf.isEmpty()) { // ricerca in anagrafica su campo codice fiscale
			int count = aclClient.search("([/struttura_esterna/@codice_fiscale/]=\"" + cf + "\" AND [/struttura_esterna/#cod_ammaoo/]=\"" + conf.getCodAmmAoo()+ "\") OR ([/persona_esterna/@codice_fiscale/]=\"" + cf + "\" AND [/persona_esterna/#cod_ammaoo/]=\"" + conf.getCodAmmAoo() + "\")");
			if (count == 1) { // e' stata individuata una struttura esterna con la partita iva indicata
				if (logger.isInfoEnabled())
					logger.info("[" + conf.getName() + "] found rif esterno in ACL. CF [" + cf + "]");
				
				found = true;
				rifEsterno = getRifEsternoFromAcl(aclClient.loadDocByQueryResult(0));
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
					logger.info("[" + conf.getName() + "] rif esterno NOT found in ACL. Inserting SE [" + denominazione + "]");				
				
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
					logger.info("[" + conf.getName() + "] rif esterno NOT found in ACL. Inserting PE [" + root.attributeValue("cognome") + " " + root.attributeValue("nome") + "]");
				
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
	 * @param rif
	 * @param rifElemNameInFatturaPA
	 * @return
	 */
	private RifEsterno updateRifEsternoByDatiFattura(RifEsterno rifEsterno, Document fatturaPADocument, String rifElemNameInFatturaPA) {
		if (rifEsterno != null) {
			// Aggiornamento del nome recuperato da ACL con quello letto dalla fattura
			Node node = fatturaPADocument.selectSingleNode("//FatturaElettronicaHeader/" + rifElemNameInFatturaPA + "/DatiAnagrafici/Anagrafica/Denominazione");
			String nome = (node == null) ? "" : node.getText();
			if (nome.isEmpty()) {
				node = fatturaPADocument.selectSingleNode("//FatturaElettronicaHeader/" + rifElemNameInFatturaPA + "/DatiAnagrafici/Anagrafica/Cognome");
				nome = (node == null) ? "" : node.getText();
				node = fatturaPADocument.selectSingleNode("//FatturaElettronicaHeader/" + rifElemNameInFatturaPA + "/DatiAnagrafici/Anagrafica/Nome");
				nome = (node == null) ? "" : " " + node.getText();
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
		Document xmlDocument = xwClient.loadAndLockDocument(this.physDocForAttachingFile, conf.getXwLockOpAttempts(), conf.getXwLockOpDelay());
		
		try {
			//upload file
			byte []fileContent = (new MessageContentProvider(parsedMessage.getMessage(), false)).getContent();
			String fileId = xwClient.addAttach(dcwParsedMessage.getFileNameNotificaFatturaPA(), fileContent, conf.getXwLockOpAttempts(), conf.getXwLockOpDelay());

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
            
			xwClient.saveDocument(xmlDocument, this.physDocForAttachingFile);
			
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
				xwClient.unlockDocument(this.physDocForAttachingFile);
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
	            int count = aclClient.search("[/struttura_interna/@cod_uff/]=\"" + codUff + "\"");
				if (count > 0) {
					Document document = aclClient.loadDocByQueryResult(0);
					Node node = document.selectSingleNode("/struttura_interna/fatturaPA/@emailSdI");
					if (node == null || node.getText().isEmpty()) {
						if (logger.isInfoEnabled())
							logger.info("[" + conf.getName() + "] updating SdI email [" + emailAddressSdI + "] for SI [" + codUff + "]");
						
						int pD = aclClient.getPhysdocByQueryResult(0);
						try {
							document = aclClient.loadAndLockDocument(pD, conf.getXwLockOpAttempts(), conf.getXwLockOpDelay());

							Element fatturaPAEl = document.getRootElement().element("fatturaPA");
							if (fatturaPAEl == null)
								fatturaPAEl = document.getRootElement().addElement("fatturaPA");
							fatturaPAEl.addAttribute("emailSdI", emailAddressSdI);
							
							aclClient.saveDocument(document, pD);
						}
						catch (Exception e) {
							logger.error("[" + conf.getName() + "]. Unexpected error updating SdI email [" + emailAddressSdI + "] for SI [" + codUff + "]", e);
							try {
								aclClient.unlockDocument(pD);
							}
							catch (Exception e1) {
								; //do nothing
							}
						}						
					}
				}
			}
			catch (Exception e) {
				logger.error("[" + conf.getName() + "]. Unexpected error updating SdI email [" + emailAddressSdI + "] for SI [" + codUff + "]", e);
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
			logger.warn("[" + conf.getName() + "]. Unexpected error parsing fatturaPA index number from subject [" + parsedMessage.getSubject() + "]", e);
		}		
		
		//load and lock existing document
		Document xmlDocument = xwClient.loadAndLockDocument(this.physDocForAttachingFile, conf.getXwLockOpAttempts(), conf.getXwLockOpDelay());
		
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
        			logger.warn("[" + conf.getName() + "]. Unexpected error identifing fatturaPA [index: " + fatturaPosition + "] in document [physodc: " + this.physDocForAttachingFile + "]", e);
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
            
			xwClient.saveDocument(xmlDocument, this.physDocForAttachingFile);
		}
		catch (Exception e) {
			try {
				xwClient.unlockDocument(this.physDocForAttachingFile);
			}
			catch (Exception unlockE) {
				; //do nothing
			}
			throw e;
		}				
	}
	
}
