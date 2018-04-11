package it.tredi.msa.mailboxmanager.docway;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Part;
import javax.mail.Message.RecipientType;
import javax.mail.internet.InternetAddress;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
	    ATTACH_PEC_RECEIPT
	}
	
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
		else if (storeType == StoreType.ATTACH_PEC_RECEIPT) {
			attachPecReceiptToDocument(parsedMessage);
		}
		
		
//TODO - gestire altre casistiche
		else if (storeType == StoreType.SKIP_DOCUMENT) //4. there's nothing to do (maybe message deletion/move failed)
			; 

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
		doc.setNumProt("");
		
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

	private void createDocwayFiles(ParsedMessage parsedMessage, DocwayDocument doc) throws MessagingException, IOException {

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
	
	protected abstract Object saveNewDocument(DocwayDocument doc) throws Exception;
	protected abstract Object updatePartialDocument(DocwayDocument doc) throws Exception;
	protected abstract Object updateDocumentWithRecipient(DocwayDocument doc) throws Exception;
	protected abstract RifEsterno createRifEsterno(String name, String address) throws Exception;
	protected abstract List<RifInterno> createRifInterni(ParsedMessage parsedMessage) throws Exception;
	protected abstract void sendNotificationEmails(DocwayDocument doc, Object saveDocRetObj);
	protected abstract StoreType decodeStoreType(ParsedMessage parsedMessage) throws Exception;
	protected abstract void attachPecReceiptToDocument(ParsedMessage parsedMessage) throws Exception;
	
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
	
}
