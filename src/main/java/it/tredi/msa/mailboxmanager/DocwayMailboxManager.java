package it.tredi.msa.mailboxmanager;

import javax.mail.Message;

import it.tredi.msa.entity.DocwayDocument;
import it.tredi.msa.entity.DocwayMailboxConfiguration;
import it.tredi.msa.entity.ParsedMessage;

public abstract class DocwayMailboxManager extends MailboxManager {
	
	
	@Override
    public boolean isMessageStorable(ParsedMessage parsedMessage) {
    	return true;
//TODO - per ora true    	
    }
	
	@Override
    public void storeMessage(ParsedMessage parsedMessage) throws Exception {
//TODO - realizzare lo store del messaggio
		
		//build new Docway document
		DocwayMailboxConfiguration conf = (DocwayMailboxConfiguration)getConfiguration();
		DocwayDocument doc = new DocwayDocument();
		
		//tipo doc
		doc.setTipo(conf.getTipoDoc());

		//oggetto
		doc.setOggetto(parsedMessage.getSubject());
		
		//save new document
		saveNewDocument(doc);
		
	}	
	
	protected abstract void saveNewDocument(DocwayDocument doc) throws Exception;
	

}
