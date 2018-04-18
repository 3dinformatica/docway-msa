package it.tredi.msa.mailboxmanager.docway;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.Part;
import javax.mail.Message.RecipientType;
import javax.mail.internet.InternetAddress;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.Element;

import it.tredi.msa.configuration.docway.DocwayMailboxConfiguration;
import it.tredi.msa.mailboxmanager.MailboxManager;
import it.tredi.msa.mailboxmanager.MessageContentProvider;
import it.tredi.msa.mailboxmanager.ParsedMessage;
import it.tredi.msa.mailboxmanager.PartContentProvider;
import it.tredi.msa.mailboxmanager.StringContentProvider;

public abstract class DocwayMailboxManager extends MailboxManager {
	
	protected Date currentDate;
	protected ParsedMessage parsedMessage;
	
	protected static final String TESTO_EMAIL_FILENAME = "testo email.txt";
	protected static final String TESTO_HTML_EMAIL_FILENAME = "testo email.html";
	protected static final String MESSAGGIO_ORIGINALE_EMAIL_FILENAME = "MessaggioOriginale.eml";
	protected static final String DEFAULT_ALLEGATO = "0 - nessun allegato";
	
	private static final Logger logger = LogManager.getLogger(DocwayMailboxManager.class.getName());
	
	public enum StoreType {
	    SAVE_NEW_DOCUMENT,
	    UPDATE_PARTIAL_DOCUMENT,
	    UPDATE_NEW_RECIPIENT,
	    SKIP_DOCUMENT,
	    ATTACH_INTEROP_PA_PEC_RECEIPT,
	    ATTACH_INTEROP_PA_NOTIFICATION,
	    SAVE_NEW_DOCUMENT_INTEROP_PA,
	    UPDATE_PARTIAL_DOCUMENT_INTEROP_PA
	}
	
	protected abstract Object saveNewDocument(DocwayDocument doc) throws Exception;
	protected abstract Object updatePartialDocument(DocwayDocument doc) throws Exception;
	protected abstract Object updateDocumentWithRecipient(DocwayDocument doc) throws Exception;
	protected abstract RifEsterno createRifEsterno(String name, String address) throws Exception;
	protected abstract List<RifInterno> createRifInterni(ParsedMessage parsedMessage) throws Exception;
	protected abstract void sendNotificationEmails(DocwayDocument doc, Object saveDocRetObj);
	protected abstract StoreType decodeStoreType(ParsedMessage parsedMessage) throws Exception;
	protected abstract void attachInteropPAPecReceiptToDocument(ParsedMessage parsedMessage) throws Exception;
	protected abstract void attachInteropPANotificationToDocument(ParsedMessage parsedMessage) throws Exception;	
	protected abstract String buildNewNumprotStringForSavingDocument() throws Exception;
	
	@Override
    public ParsedMessage parseMessage(Message message) throws Exception {
    	return new DocwayParsedMessage(message);
    }	
	
	@Override
    public void storeMessage(ParsedMessage parsedMessage) throws Exception {
		DocwayMailboxConfiguration conf = (DocwayMailboxConfiguration)getConfiguration();
		super.storeMessage(parsedMessage);
		
		this.currentDate = new Date();
		this.parsedMessage = parsedMessage;
		
		StoreType storeType = decodeStoreType(parsedMessage);
		if (logger.isInfoEnabled())
			logger.info("[" + conf.getName() + "] message [" + parsedMessage.getMessageId() + "] store type [" + storeType + "]");
		
		if (storeType == StoreType.SAVE_NEW_DOCUMENT || storeType == StoreType.UPDATE_PARTIAL_DOCUMENT || storeType == StoreType.UPDATE_NEW_RECIPIENT) { //save new document or update existing one
			//build new Docway document
			DocwayDocument doc = createDocwayDocumentByMessage(parsedMessage);

			//save new document
			Object retObj = null;
			if (storeType == StoreType.SAVE_NEW_DOCUMENT) //1. doc not found by messageId -> save new document
				retObj = saveNewDocument(doc); 
			else if (storeType == StoreType.UPDATE_PARTIAL_DOCUMENT) //2. doc found by messageId flagged as partial (attachments upload not completed) -> update document adding missing attachments
				retObj = updatePartialDocument(doc);			
			else if (storeType == StoreType.UPDATE_NEW_RECIPIENT) //3. doc found with different recipient email (same email sent to different mailboxes) -> update document adding new CCs
				retObj = updateDocumentWithRecipient(doc);			
			
			//notify emails
			if (conf.isNotificationEnabled() && (conf.isNotifyRPA() || conf.isNotifyCC())) { //if notification is activated
				if (logger.isInfoEnabled())
					logger.info("[" + conf.getName() + "] sending notification emails [" + parsedMessage.getMessageId() + "]");
				sendNotificationEmails(doc, retObj);
			}							
		}
		else if (storeType == StoreType.SAVE_NEW_DOCUMENT_INTEROP_PA || storeType == StoreType.UPDATE_PARTIAL_DOCUMENT_INTEROP_PA) { //save new interopPA document (Segnatura.xml) or update existing one
			//build new Docway document
			DocwayDocument doc = createDocwayDocumentByInteropPAMessage(parsedMessage);

//TODO - gestire la logica per l'invio dei messaggi di confermaricezione (se abilitata la protocollazione automatica)
//TODO - gestire la logice dei controlli in ingresso e dell'invio della notifica di eccezione nel caso che le cose non funzionino
			
			//save new document
			Object retObj = null;
			if (storeType == StoreType.SAVE_NEW_DOCUMENT_INTEROP_PA) //1. doc not found by messageId -> save new document
				retObj = saveNewDocument(doc); 
			else if (storeType == StoreType.UPDATE_PARTIAL_DOCUMENT_INTEROP_PA) //2. doc found by messageId flagged as partial (attachments upload not completed) -> update document adding missing attachments
				retObj = updatePartialDocument(doc);			
			
			//notify emails
			if (conf.isNotificationEnabled() && (conf.isNotifyRPA() || conf.isNotifyCC())) { //if notification is activated
				if (logger.isInfoEnabled())
					logger.info("[" + conf.getName() + "] sending notification emails [" + parsedMessage.getMessageId() + "]");
				sendNotificationEmails(doc, retObj);
			}
		}		
		else if (storeType == StoreType.ATTACH_INTEROP_PA_PEC_RECEIPT) { //PEC receipt for interopPA/fatturaPA message/notification
			attachInteropPAPecReceiptToDocument(parsedMessage);
		}
		else if (storeType == StoreType.ATTACH_INTEROP_PA_NOTIFICATION) { //interopPA notification (Aggiornamento.xml, Eccezione.xml, Annullamento.xml, Conferma.xml)
			attachInteropPANotificationToDocument(parsedMessage);
		}		
		else if (storeType == StoreType.SKIP_DOCUMENT) //4. there's nothing to do (maybe previous message deletion/move failed)
			; 
		else
			throw new Exception("Unsupported store type: " + storeType);
	}
	
	private DocwayDocument createDocwayDocumentByMessage(ParsedMessage  parsedMessage) throws Exception {
		DocwayMailboxConfiguration conf = (DocwayMailboxConfiguration)getConfiguration();
		DocwayDocument doc = new DocwayDocument();
		
		//tipo doc
		doc.setTipo(conf.getTipoDoc());
		
		//bozza
		doc.setBozza(conf.isBozza());
		
		//cod_amm_aoo
		doc.setCodAmmAoo(conf.getCodAmmAoo());
		
		//anno
		doc.setAnno(conf.isCurrentYear()? (new SimpleDateFormat("yyyy")).format(currentDate) : "");
		
		//data prot
		doc.setDataProt(conf.isCurrentDate()? (new SimpleDateFormat("yyyyMMdd")).format(currentDate) : "");
		
		//num_prot
		doc.setNumProt(conf.getNumProt());
		
		//messageId
		doc.setMessageId(parsedMessage.getMessageId());
		
		//recipientEmail
		doc.setRecipientEmail(conf.getEmail());
		
		//annullato
		doc.setAnnullato(false);
		
		//autore
		if (doc.getTipo().toUpperCase().equals("VARIE"))
			doc.setAutore((parsedMessage.getFromPersonal() == null || parsedMessage.getFromPersonal().isEmpty())? parsedMessage.getFromAddress() : parsedMessage.getFromPersonal());

		//oggetto
		doc.setOggetto(parsedMessage.getSubject());
		
		//tipologia
		doc.setTipologia(conf.getTipologia());
		
		//mezzo trasmissione
		doc.setMezzoTrasmissione(conf.getMezzoTrasmissione());
		
		//rif esterni
		if (doc.getTipo().toUpperCase().equals("ARRIVO"))
			doc.addRifEsterno(createRifEsterno((parsedMessage.getFromPersonal() == null || parsedMessage.getFromPersonal().isEmpty())? parsedMessage.getFromAddress() : parsedMessage.getFromPersonal(), parsedMessage.getFromAddress()));
		else if (doc.getTipo().toUpperCase().equals("PARTENZA")) {
			Address []recipients = parsedMessage.getMessage().getRecipients(RecipientType.TO);
			for (Address recipient:recipients) {
				String personal = ((InternetAddress)recipient).getPersonal();
				String address = ((InternetAddress)recipient).getAddress();
				doc.addRifEsterno(createRifEsterno((personal==null || personal.isEmpty())? address : personal, address));
			}
		}
		
		//voce di indice
		doc.setVoceIndice(conf.getVoceIndice());
		
		//classif
		doc.setClassif(conf.getClassif());
		doc.setClassifCod(conf.getClassifCod());
		
		//repertorio
		doc.setRepertorio(conf.getRepertorio());
		doc.setRepertorioCod(conf.getRepertorioCod());
		
		//note
		if (conf.isNoteAutomatiche()) {
			doc.setNote(parsedMessage.getTextPartsWithHeaders());
		}
		
		//rif interni
		List<RifInterno> rifInterni = createRifInterni(parsedMessage);
		for (RifInterno rifInterno:rifInterni)
			doc.addRifInterno(rifInterno);
		
		//parsedMessage.relevantMessages -> postit
		for (String relevantMessage:parsedMessage.getRelevantMssages()) {
			Postit postit = new Postit();
			postit.setText(relevantMessage);
			postit.setOperatore(conf.getOperatore());
			postit.setData(currentDate);
			postit.setOra(currentDate);
			doc.addPostit(postit);
		}
		
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
		createDocwayFiles(parsedMessage, doc);
		
		return doc;
	}

	private void createDocwayFiles(ParsedMessage parsedMessage, DocwayDocument doc) throws Exception {

		//email body html/text attachment
		DocwayFile file = createDocwayFile();
		file.setName(TESTO_HTML_EMAIL_FILENAME);
		String text = parsedMessage.getHtmlParts().trim();
		if (text.isEmpty()) { //no html -> switch to text version
			file.setName(TESTO_EMAIL_FILENAME);
			text = parsedMessage.getTextPartsWithHeaders();
		}
		file.setContentProvider(new StringContentProvider(text));
		doc.addFile(file);			

		//email attachments (files + immagini)
		List<Part> attachments = parsedMessage.getAttachments();
		for (Part attachment:attachments) {
			file = createDocwayFile();
			file.setContentProvider(new PartContentProvider(attachment));
			file.setName(attachment.getFileName());
			if (isImage(file.getName())) //immagine
					doc.addImmagine(file);
			else //file
				doc.addFile(file);
			
			//allegato
			doc.addAllegato(file.getName());
		}

		//allegato - default
		if (doc.getAllegato().isEmpty())
			doc.addAllegato(DEFAULT_ALLEGATO);
		
		//EML
		if (((DocwayMailboxConfiguration)getConfiguration()).isStoreEml()) {
			file = createDocwayFile();
			file.setContentProvider(new MessageContentProvider(parsedMessage.getMessage(), true));
			file.setName(MESSAGGIO_ORIGINALE_EMAIL_FILENAME);
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
		DocwayParsedMessage dcwParsedMessage = (DocwayParsedMessage)parsedMessage;
		Document segnaturaDocument = dcwParsedMessage.getSegnaturaInteropPADocument();

		//create DocwayDocument
		DocwayDocument doc = new DocwayDocument();
		
		//tipo doc
		doc.setTipo("arrivo");
		
		//bozza
		doc.setBozza(!conf.isProtocollaSegnatura());
		
		//cod_amm_aoo
		doc.setCodAmmAoo(conf.getCodAmmAoo());
		
		//anno
		doc.setAnno(conf.isProtocollaSegnatura()? (new SimpleDateFormat("yyyy")).format(currentDate) : "");
		
		//data prot
		doc.setDataProt((new SimpleDateFormat("yyyyMMdd")).format(currentDate));
		
		//num_prot
		doc.setNumProt(conf.isProtocollaSegnatura()? buildNewNumprotStringForSavingDocument() : "");
		
		//messageId
		doc.setMessageId(parsedMessage.getMessageId());
		
		//recipientEmail
		doc.setRecipientEmail(conf.getEmail());
		
		//annullato
		doc.setAnnullato(false);
		
		//oggetto
		doc.setOggetto(segnaturaDocument.selectSingleNode("/Segnatura/Intestazione/Oggetto").getText());
		
		//tipologia
		doc.setTipologia(conf.getTipologiaSegnatura());
		
		//mezzo trasmissione
		doc.setMezzoTrasmissione(conf.getMezzoTrasmissioneSegnatura());
		
		//rif esterno
		RifEsterno rifEsterno = new RifEsterno();		
		doc.addRifEsterno(rifEsterno);
		String codiceAmministrazione = segnaturaDocument.selectSingleNode("/Segnatura/Intestazione/Identificatore/CodiceAmministrazione").getText();
        String codiceAOO = segnaturaDocument.selectSingleNode("/Segnatura/Intestazione/Identificatore/CodiceAOO").getText();
        String nProt = segnaturaDocument.selectSingleNode("/Segnatura/Intestazione/Identificatore/NumeroRegistrazione").getText();
        String dataProt = segnaturaDocument.selectSingleNode("/Segnatura/Intestazione/Identificatore/DataRegistrazione").getText();
        rifEsterno.setCodiceAmministrazione(codiceAmministrazione);
        rifEsterno.setCodiceAOO(codiceAOO);
        rifEsterno.setDataProt(dataProt);
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
        rifEsterno.setNome(denominazione);
        
        //rif esterno: indirizzo, tel, fax, email, email_certificata
        String indirizzo = "";
        Attribute att = (Attribute)segnaturaDocument.selectSingleNode("/Segnatura/Intestazione/Origine/Mittente/Amministrazione/UnitaOrganizzativa/IndirizzoPostale/Toponimo/@dug");
        if (att != null && !att.getText().isEmpty())
        	indirizzo += att.getText();
        @SuppressWarnings("unchecked")
		List<Element> els = segnaturaDocument.selectNodes("/Segnatura/Intestazione/Origine/Mittente/Amministrazione/IndirizzoPostale/*");
        for (Element el:els) {
        	if (!el.getText().isEmpty()) {
        		if (!indirizzo.isEmpty())
        			indirizzo += " - ";
        		indirizzo += el.getText();
        	}
        }
        if (!indirizzo.trim().isEmpty())
        	rifEsterno.setIndirizzo(indirizzo.trim());
        rifEsterno.setEmailCertificata(segnaturaDocument.selectSingleNode("/Segnatura/Intestazione/Origine/IndirizzoTelematico").getText());
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

		//classif
        String classif = "";
        el = (Element)segnaturaDocument.selectSingleNode("/Segnatura/Intestazione/Classifica/Denominazione");
        if (el != null && !el.getText().isEmpty()) {
        	doc.setClassif(classif);
        	if (classif.indexOf(" ") > 0)
        		doc.setClassifCod(classif.substring(0, classif.indexOf(" ")));
//TODO - sentire nicola x cosa fare        	
        }
		
		//rif interni
		List<RifInterno> rifInterni = createRifInterni(parsedMessage);
		for (RifInterno rifInterno:rifInterni)
			doc.addRifInterno(rifInterno);
		
		//parsedMessage.relevantMessages -> postit
		for (String relevantMessage:parsedMessage.getRelevantMssages()) {
			Postit postit = new Postit();
			postit.setText(relevantMessage);
			postit.setOperatore(conf.getOperatore());
			postit.setData(currentDate);
			postit.setOra(currentDate);
			doc.addPostit(postit);
		}
		
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
		createDocwayFilesForInteropPAMessage(parsedMessage, doc);
//TODO - gestire correttamente i file per la registrazione della segnatura	
//<interoperabilita	 	data = "20171012" info = "Ricezione Telematica (Segnatura.xml)" name = "47339.xml" ora = "12:59:07" title = "Segnatura.xml" />
//<interoperabilita	 	data = "20171012" info = "Ricezione telematica" name = "47343.eml" ora = "12:59:06" title = "Ricezione telematica.eml" />		
		
		//aggiungere elementi di interoperabilità
		
		
		return doc;
	}	

	private void createDocwayFilesForInteropPAMessage(ParsedMessage parsedMessage, DocwayDocument doc) throws Exception {
		//get rif esterno
		RifEsterno rifEsterno = doc.getRifEsterni().get(0);

		//email attachments (files + immagini)
		List<Part> attachments = parsedMessage.getAttachments();
		for (Part attachment:attachments) {
			if (attachment.getFileName().equals("Segnatura.xml")) {
				InteroperabilitaItem interopItem = new InteroperabilitaItem();
				interopItem.setName("Segnatura.xml");
				interopItem.setData(currentDate);
				interopItem.setOra(currentDate);
				interopItem.setInfo("Ricezione Telematica (Segnatura.xml)");
				interopItem.setMessageId(parsedMessage.getMessageId());
				interopItem.setContentProvider(new PartContentProvider(attachment));
				rifEsterno.addInteroperabilitaItem(interopItem);
			}
			/*
			
			DocwayFile file = createDocwayFile();
			file.setContentProvider(new PartContentProvider(attachment));
			file.setName(attachment.getFileName());
			if (isImage(file.getName())) //immagine
					doc.addImmagine(file);
			else //file
				doc.addFile(file);
			
			//allegato
			doc.addAllegato(file.getName());*/
		}

		//EML
		InteroperabilitaItem interopItem = new InteroperabilitaItem();
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

//TODO - gestione del campo allegato nel caso della segnatura

	}	
	
}
