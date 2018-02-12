package it.tredi.msa.mailboxmanager;

import it.tredi.extraway.ExtrawayClient;
import it.tredi.msa.entity.DocwayDocument;
import it.tredi.msa.entity.DocwayMailboxConfiguration;

public class Docway4MailboxManager extends DocwayMailboxManager {

	protected ExtrawayClient xwClient;
	

	@Override
    public void openSession() throws Exception {
		super.openSession();
		DocwayMailboxConfiguration conf = (DocwayMailboxConfiguration)getConfiguration();
		xwClient = new ExtrawayClient(conf.getXwHost(), conf.getXwPort(), conf.getXwDb(), conf.getXwUser(), conf.getXwPassword());
		xwClient.connect();
    }
	
	@Override
    public void closeSession() {
    	super.closeSession();
		try {
			xwClient.disconnect();
		}
		catch (Exception e) {
//TODO - log warning			
		}    	
    }  	
	
	@Override
	protected void saveNewDocument(DocwayDocument doc) throws Exception {
//TODO personalizzazione di docway4 come la connessione a xw e la scrittura su db
		
		
	}

	
}
