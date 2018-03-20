package it.tredi.msa.mailboxmanager.docway;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.mail.MessagingException;
import javax.mail.Part;

import it.tredi.msa.entity.MessageContentProvider;
import it.tredi.msa.entity.ParsedMessage;
import it.tredi.msa.entity.PartContentProvider;
import it.tredi.msa.entity.StringContentProvider;
import it.tredi.msa.entity.docway.DocwayDocument;
import it.tredi.msa.entity.docway.DocwayFile;
import it.tredi.msa.entity.docway.DocwayMailboxConfiguration;
import it.tredi.msa.entity.docway.RifEsterno;
import it.tredi.msa.entity.docway.RifInterno;
import it.tredi.msa.entity.docway.StoriaItem;
import it.tredi.msa.mailboxmanager.MailboxManager;

public abstract class DocwayMailboxManager extends MailboxManager {
	
	protected Date currentDate;
	protected ParsedMessage parsedMessage;
	
	protected static final String TESTO_EMAIL_FILENAME = "testo email.txt";
	protected static final String TESTO_HTML_EMAIL_FILENAME = "testo email.html";
	protected static final String MESSAGGIO_ORIGINALE_EMAIL_FILENAME = "MessaggioOriginale.eml";
	
	@Override
    public boolean isMessageStorable(ParsedMessage parsedMessage) {
    	return true;
//TODO - per ora true    	
    }
	
	@Override
    public void storeMessage(ParsedMessage parsedMessage) throws Exception {
		super.storeMessage(parsedMessage);
		
		this.currentDate = new Date();
		this.parsedMessage = parsedMessage;
		
//TODO - realizzare lo store del messaggio
//inserire tutta la logica di archiviazione
		
		//build new Docway document
		DocwayDocument doc = createDocwayDocumentByMessage(parsedMessage);

		//save new document
		saveNewDocument(doc);
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
			doc.setAutore(parsedMessage.getFromPersonal().isEmpty()? parsedMessage.getFromAddress() : parsedMessage.getFromPersonal());

		//oggetto
		doc.setOggetto(parsedMessage.getSubject());
		
		//tipologia
		doc.setTipologia(conf.getTipologia());
		
		//mezzo trasmissione
		doc.setMezzoTrasmissione(conf.getMezzoTrasmissione());
		
		//rif esterni
		if (doc.getTipo().toUpperCase().equals("ARRIVO"))
			doc.addRifEsterno(createRifEsterno(parsedMessage.getFromPersonal(), parsedMessage.getFromAddress()));
		else if (doc.getTipo().toUpperCase().equals("PARTENZA"))
			;
//TODO - gestire rif_esterni per i doc in partenza
		
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
		
		//files + immagini
		createDocwayFiles(parsedMessage, doc);
		
//TODO - effettuare la gestione delle email di notifica
		
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
		}
		
		//EML
		if (((DocwayMailboxConfiguration)getConfiguration()).isStoreEml()) {
			file = createDocwayFile();
			file.setContentProvider(new MessageContentProvider(parsedMessage.getMessage(), true));
			file.setName(MESSAGGIO_ORIGINALE_EMAIL_FILENAME);
			doc.addFile(file);			
		}

	}
	
	protected abstract void saveNewDocument(DocwayDocument doc) throws Exception;
	protected abstract RifEsterno createRifEsterno(String name, String address) throws Exception;
	protected abstract List<RifInterno> createRifInterni(ParsedMessage parsedMessage) throws Exception;
	
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
