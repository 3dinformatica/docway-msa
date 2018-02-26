package it.tredi.msa.mailboxmanager.docway;

import java.text.SimpleDateFormat;
import java.util.Date;

import javax.mail.Message;
import javax.mail.MessagingException;

import it.tredi.msa.entity.ParsedMessage;
import it.tredi.msa.entity.docway.DocwayDocument;
import it.tredi.msa.entity.docway.DocwayMailboxConfiguration;
import it.tredi.msa.entity.docway.RifEsterno;
import it.tredi.msa.entity.docway.StoriaItem;
import it.tredi.msa.mailboxmanager.MailboxManager;

public abstract class DocwayMailboxManager extends MailboxManager {
	
	protected Date currentDate;
	protected ParsedMessage parsedMessage;
	
	@Override
    public boolean isMessageStorable(ParsedMessage parsedMessage) {
    	return true;
//TODO - per ora true    	
    }
	
	@Override
    public void storeMessage(ParsedMessage parsedMessage) throws Exception {
		this.currentDate = new Date();
		this.parsedMessage = parsedMessage;
		
//TODO - realizzare lo store del messaggio
//inserire tutta la logica di archiviazione
		
		//build new Docway document
		DocwayDocument doc = buildDocwayDocument(parsedMessage);

		//save new document
		saveNewDocument(doc);
	}	
	
	private DocwayDocument buildDocwayDocument(ParsedMessage  parsedMessage) throws Exception {
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
			doc.addRifEsterno(buildRifEsterno(parsedMessage.getFromPersonal(), parsedMessage.getFromAddress()));
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
			String note = "From: " + parsedMessage.getFromAddress() + "\n";
			note += "To: " + parsedMessage.getToAddressesAsString() + "\n";
			note += "Cc: " + parsedMessage.getCcAddressesAsString() + "\n";
			note += "Sent: " + parsedMessage.getSentDate() + "\n";
			note += "Subject: " + parsedMessage.getSubject() + "\n\n";

//TODO - aggiungere getMailBody(TEXT)			
			
			doc.setNote(note);
		}
		
		//storia creazione
		StoriaItem creazione = new StoriaItem("creazione");
		creazione.setOper(conf.getOper());
		creazione.setUffOper(conf.getUffOper());
		creazione.setData(currentDate);
		creazione.setOra(currentDate);
		doc.addStoriaItem(creazione);
		
		return doc;
	}
	
	protected abstract void saveNewDocument(DocwayDocument doc) throws Exception;
	protected abstract RifEsterno buildRifEsterno(String name, String address) throws Exception;
	

}
