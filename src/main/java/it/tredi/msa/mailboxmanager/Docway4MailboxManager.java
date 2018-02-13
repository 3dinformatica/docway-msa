package it.tredi.msa.mailboxmanager;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import it.tredi.extraway.ExtrawayClient;
import it.tredi.msa.entity.DocwayDocument;
import it.tredi.msa.entity.DocwayMailboxConfiguration;

public class Docway4MailboxManager extends DocwayMailboxManager {

	protected ExtrawayClient xwClient;
	private int lastSavedDocumentPhysDoc;
	

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
		
		//DocwayDocument -> xml
		Element docEl = DocumentHelper.createElement("doc");
		Document xmlDocument =DocumentHelper.createDocument(docEl);
		
		//nrecord
		docEl.addAttribute("nrecord", ".");
		
		//tipoDoc
		
		//bozza
		
		//cod_amm_aoo
		
		//anno
		
		//data_prot
		
		//num_prot
		
		//annullato
		docEl.addAttribute("annullato", "no");
		
		//messageId
		
		//oggetto
		Element oggettoEl = DocumentHelper.createElement("oggetto");
		docEl.add(oggettoEl);
		oggettoEl.setText(doc.getOggetto());
		
		//tipologia
		
		//mezzo_trasmissione
		
		//allegato
		
		//save in Extraway
		lastSavedDocumentPhysDoc = xwClient.saveDocument(xmlDocument);
	}

	
}
