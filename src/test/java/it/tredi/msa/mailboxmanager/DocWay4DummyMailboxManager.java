package it.tredi.msa.mailboxmanager;

import java.util.List;

import org.dom4j.Document;

import it.tredi.extraway.ExtrawayClient;
import it.tredi.msa.configuration.docway.Docway4MailboxConfiguration;
import it.tredi.msa.mailboxmanager.docway.DocTipoEnum;
import it.tredi.msa.mailboxmanager.docway.Docway4EntityToXmlUtils;
import it.tredi.msa.mailboxmanager.docway.Docway4MailboxManager;
import it.tredi.msa.mailboxmanager.docway.DocwayDocument;
import it.tredi.msa.mailboxmanager.docway.FascicoloReference;
import it.tredi.msa.mailboxmanager.docway.exception.MultipleFoldersException;

/**
 * Manager DocWay di test per caselle PEC (utilizzato su alcuni UnitTest di parsing di messaggi PEC)
 */
public class DocWay4DummyMailboxManager extends Docway4MailboxManager {

	@Override
    public void openSession() throws Exception {
		Docway4MailboxConfiguration conf = (Docway4MailboxConfiguration)getConfiguration();
		
		xwClient = new ExtrawayClient(conf.getXwHost(), conf.getXwPort(), conf.getXwDb(), conf.getXwUser(), conf.getXwPassword());
		xwClient.connect();
		aclClient = new ExtrawayClient(conf.getXwHost(), conf.getXwPort(), conf.getAclDb(), conf.getXwUser(), conf.getXwPassword());
		aclClient.connect();
		extRestrictionsOnAcl = true;
    }
	
	@Override
	protected Object updateDocumentWithRecipient(DocwayDocument doc) throws Exception {
		System.out.println("UPDATE DOCUMENT WITH RECIPIENT... ");
		
		return docwayDocumentToXml(doc);
	}
	
	@Override
	protected Object saveNewDocument(DocwayDocument doc, ParsedMessage parsedMessage) throws Exception {
		System.out.println("SAVE NEW DOCUMENT... ");
		
		return docwayDocumentToXml(doc);
	}
	
	@Override
	protected Object updatePartialDocument(DocwayDocument doc) throws Exception {
		System.out.println("UPDATE PARTIAL DOCUMENT... ");
	
		return docwayDocumentToXml(doc);
	}
	
	/**
	 * Conversione del documento da model a XML
	 * @param doc
	 * @return
	 */
	private Document docwayDocumentToXml(DocwayDocument doc) {
		Document xmlDocument = null;
		if (doc != null) {
			Docway4MailboxConfiguration conf = (Docway4MailboxConfiguration)getConfiguration();
			
			xmlDocument = Docway4EntityToXmlUtils.docwayDocumentToXml(doc, super.currentDate, conf.getAspettoClassificazione());
			System.out.println("XML:\n" + xmlDocument.asXML());
		}
		return xmlDocument;
	}
	
	@Override
	protected void sendNotificationEmails(DocwayDocument doc, Object saveDocRetObj) {
		System.out.println("sendNotificationEmails... ");
	}
	
	@Override
    public void messageSkipped(ParsedMessage parsedMessage) throws Exception {
		System.out.println("MESSAGE SKIPPED");
    }
    
	@Override
    public void messageStored(ParsedMessage parsedMessage) throws Exception {
    	System.out.println("MESSAGE STORED");
    }
	
	@Override
    public void handleError(Throwable t, Object obj) {
		System.err.println("HANDLE ERROR: " + t.getMessage());
    }
	
	/**
	 * Pubblicazione del metodo findCodFascicoloByTags per chiamata diretta da unit-test
	 */
	public FascicoloReference fascicoloByTags(String codammaoo, List<String> tags) throws MultipleFoldersException, Exception {
		return this.findCodFascicoloByTags(codammaoo, tags);
	}
	
	/**
	 * Pubblicazione del metodo isMittenteInterno per chiamata diretta da unit-test
	 */
	public boolean isMittenteRifInt(ParsedMessage message) throws Exception {
		return this.isMittenteInterno(message);
	}
	
	/**
	 * Pubblicazione del metodo containsDestinatariEsterni per chiamata diretta da unit-test
	 */
	public boolean containsDestinatariRifEst(ParsedMessage message) throws Exception {
		return this.containsDestinatariEsterni(message);
	}
	
	/**
	 * Pubblicazione del metodo containsDestinatariRifInt per chiamata diretta da unit-test
	 */
	public boolean containsDestinatariRifInt(ParsedMessage message) throws Exception {
		return this.containsDestinatariInterni(message);
	}
	
	/**
	 * Pubblicazione del metodo getFlussoDocByRecipients per chiamata diretta da unit-test
	 */
	public DocTipoEnum flussoByRecipients(ParsedMessage message) throws Exception {
		return this.getFlussoDocByRecipients(message);
	}
	
	/**
	 * Pubblicazione del metodo createDocwayDocumentByMessage per chiamata diretta da unit-test
	 */
	public DocwayDocument buildDocwayDocument(ParsedMessage message, boolean fatturaPa) throws Exception {
		return this.createDocwayDocumentByMessage(message, fatturaPa);
	}
	
	/**
	 * Pubblicazione del metodo createDocwayDocumentByInteropPAMessage per chiamata diretta da unit-test
	 */
	public DocwayDocument buildDocWayDocumentByInterop(ParsedMessage message) throws Exception {
		return this.createDocwayDocumentByInteropPAMessage(message);
	}
	
	/**
	 * Pubblicazione del metodo createDocwayDocumentByFatturaPAMessage per chiamata diretta da unit-test
	 */
	public DocwayDocument buildDocwayDocumentByFatturaPAMessage(ParsedMessage message) throws Exception {
		return this.createDocwayDocumentByFatturaPAMessage(message);
	}
	
}
