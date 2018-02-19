package it.tredi.msa.mailboxmanager;

import java.text.SimpleDateFormat;
import java.util.Date;

import javax.mail.Message;
import javax.mail.MessagingException;

import it.tredi.msa.entity.DocwayDocument;
import it.tredi.msa.entity.DocwayMailboxConfiguration;
import it.tredi.msa.entity.ParsedMessage;

public abstract class DocwayMailboxManager extends MailboxManager {
	
	private Date currentDate;
	
	@Override
    public boolean isMessageStorable(ParsedMessage parsedMessage) {
    	return true;
//TODO - per ora true    	
    }
	
	@Override
    public void storeMessage(ParsedMessage parsedMessage) throws Exception {
		this.currentDate = new Date();
		
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

		//oggetto
		doc.setOggetto(parsedMessage.getSubject());
		
		//tipologia
		doc.setTipologia(conf.getTipologia());
		
		//mezzo trasmissione
		doc.setMezzoTrasmissione(conf.getMezzoTrasmissione());

		return doc;
	}
	
	protected abstract void saveNewDocument(DocwayDocument doc) throws Exception;
	

}
