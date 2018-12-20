package it.tredi.msa.test.manager;

import java.util.ArrayList;
import java.util.List;

import it.tredi.msa.configuration.docway.AssegnatarioMailboxConfiguration;
import it.tredi.msa.configuration.docway.DocwayMailboxConfiguration;
import it.tredi.msa.mailboxmanager.ParsedMessage;
import it.tredi.msa.mailboxmanager.docway.DocwayDocument;
import it.tredi.msa.mailboxmanager.docway.DocwayMailboxManager;
import it.tredi.msa.mailboxmanager.docway.DocwayParsedMessage;
import it.tredi.msa.mailboxmanager.docway.RifEsterno;
import it.tredi.msa.mailboxmanager.docway.RifInterno;

/**
 * Manager DocWay di test per caselle PEC (utilizzato su alcuni UnitTest di parsing di messaggi PEC)
 */
public class DocWayDummyPecMailboxManager extends DocwayMailboxManager {

	@Override
	protected Object saveNewDocument(DocwayDocument doc, ParsedMessage parsedMessage) throws Exception {
		System.out.println("saveNewDocument... ");
		return doc;
	}

	@Override
	protected Object updatePartialDocument(DocwayDocument doc) throws Exception {
		System.out.println("updatePartialDocument... ");
		return doc;
	}

	@Override
	protected Object updateDocumentWithRecipient(DocwayDocument doc) throws Exception {
		System.out.println("updateDocumentWithRecipient... ");
		return doc;
	}

	@Override
	protected RifEsterno createRifEsterno(String name, String address) throws Exception {
		RifEsterno rifEsterno = new RifEsterno();
        rifEsterno.setEmail(address);
        rifEsterno.setNome(name);
        return rifEsterno;
	}

	@Override
	protected List<RifInterno> createRifInterni(ParsedMessage parsedMessage) throws Exception {
		DocwayMailboxConfiguration conf = (DocwayMailboxConfiguration) getConfiguration();
		List<RifInterno> rifInterni = new ArrayList<RifInterno>();
		
		//RPA
		if (conf.getResponsabile() != null) {
			RifInterno rpa = new RifInterno();
			rpa.setNomePersona(conf.getResponsabile().getNomePersona());
			rpa.setNomeUff(conf.getResponsabile().getNomeUff());
			rpa.setCodPersona(conf.getResponsabile().getCodPersona());
			rpa.setCodUff(conf.getResponsabile().getCodUff());
			rpa.setRuolo(conf.getResponsabile().getNomeRuolo(), conf.getResponsabile().getCodRuolo());
			rpa.setDiritto("RPA");
			rifInterni.add(rpa);
		}
		
		//CC
		if (conf.isDaCopiaConoscenza()) {
			String[] ccs = parsedMessage.getCcAddressesAsString().split(",");
			if (ccs != null && ccs.length > 0) {
				for (String cc : ccs) {
					if (cc != null && !cc.trim().isEmpty()) {
						RifInterno ccrif = new RifInterno();
						ccrif.setNomePersona(cc.trim());
						ccrif.setDiritto("CC");
						rifInterni.add(ccrif);
					}
				}
			}
		}
		
		if (conf.getAssegnatariCC() != null) {
			for (AssegnatarioMailboxConfiguration assegnatario: conf.getAssegnatariCC()) {
				RifInterno cc = new RifInterno();
				cc.setNomePersona(assegnatario.getNomePersona());
				cc.setNomeUff(assegnatario.getNomeUff());
				cc.setCodPersona(assegnatario.getCodPersona());
				cc.setCodUff(assegnatario.getCodUff());
				cc.setRuolo(assegnatario.getNomeRuolo(), assegnatario.getCodRuolo());
				cc.setDiritto("CC");
				rifInterni.add(cc);
			}
		}
		
		return rifInterni;
	}

	@Override
	protected void sendNotificationEmails(DocwayDocument doc, Object saveDocRetObj) {
		System.out.println("sendNotificationEmails... ");
	}

	@Override
	protected StoreType decodeStoreType(ParsedMessage parsedMessage) throws Exception {
		StoreType storeType = null;
		DocwayParsedMessage dcwParsedMessage = (DocwayParsedMessage) parsedMessage;
		
		DocwayMailboxConfiguration conf = (DocwayMailboxConfiguration) this.getConfiguration();
		
			
		if (dcwParsedMessage.isPecReceipt() || dcwParsedMessage.isNotificaInteropPAMessage(conf.getCodAmmInteropPA(), conf.getCodAooInteropPA()) ||
				(conf.isEnableFatturePA() && dcwParsedMessage.isNotificaFatturaPAMessage(conf.getSdiDomainAddress()))) { 
			//messaggio è una ricevuta PEC oppure è una notifica (messaggio di ritorno) di interoperabilità PA oppure è una notifica di fatturaPA
			String query = "([/doc/rif_esterni/rif/interoperabilita/@messageId]=\"" + dcwParsedMessage.getMessageId() + "\" OR [/doc/rif_esterni/interoperabilita_multipla/interoperabilita/@messageId]=\"" + dcwParsedMessage.getMessageId() + "\""
					+ " OR [/doc/extra/fatturaPA/notifica/@messageId]=\"" + dcwParsedMessage.getMessageId() + "\") AND [/doc/@cod_amm_aoo/]=\"" + conf.getCodAmmAoo() + "\"";
			
			System.out.println("decodeStoreType.query[1] = " + query);
			//if (xwClient.search(query) > 0)
			//	return StoreType.SKIP_DOCUMENT;
				
			query = "";
			if (dcwParsedMessage.isPecReceiptForInteropPA(conf.getCodAmmInteropPA(), conf.getCodAooInteropPA())) { //1st try: individuazione ricevuta PEC di messaggio di interoperabilità (tramite identificazione degli allegati del messaggio originale)
				query = dcwParsedMessage.buildQueryForDocway4DocumentFromInteropPAPecReceipt(conf.getCodAmm(), conf.getCodAoo());
				System.out.println("decodeStoreType.query[2] = " + query);
				
				storeType = StoreType.ATTACH_INTEROP_PA_PEC_RECEIPT;
			}
			if (query.isEmpty() && dcwParsedMessage.isPecReceiptForInteropPAbySubject()) { //2nd try: non sempre nelle ricevute è presente il messaggio originale -> si cerca il numero di protocollo nel subject
				query = dcwParsedMessage.buildQueryForDocway4DocumentFromInteropPASubject();
				System.out.println("decodeStoreType.query[3] = " + query);
				
				storeType = StoreType.ATTACH_INTEROP_PA_PEC_RECEIPT;
			}
			else if (dcwParsedMessage.isPecReceiptForFatturaPAbySubject()) { //ricevuta PEC di messaggio per la fatturaPA
				query = dcwParsedMessage.buildQueryForDocway4DocumentFromFatturaPASubject();
				System.out.println("decodeStoreType.query[4] = " + query);
				
				storeType = StoreType.ATTACH_FATTURA_PA_PEC_RECEIPT;
			}
			else if (dcwParsedMessage.isNotificaInteropPAMessage(conf.getCodAmmInteropPA(), conf.getCodAooInteropPA())) { //notifia di interoperabilità PA
				query = dcwParsedMessage.buildQueryForDocway4DocumentFromInteropPANotification(conf.getCodAmm(), conf.getCodAoo());
				System.out.println("decodeStoreType.query[5] = " + query);
				
				storeType = StoreType.ATTACH_INTEROP_PA_NOTIFICATION;
			}
			else if (dcwParsedMessage.isNotificaFatturaPAMessage(conf.getSdiDomainAddress())) { //notifia di fattura PA
				query = dcwParsedMessage.buildQueryForDocway4DocumentFromFatturaPANotification();
				System.out.println("decodeStoreType.query[6] = " + query);
				
				return StoreType.ATTACH_FATTURA_PA_NOTIFICATION;
			}
			else if (dcwParsedMessage.isPecReceipt() && conf.isIgnoreStandardOrphanPecReceipts()) { //ricevuta PEC (non relativa a interopPA/fatturaPA) e property attiva per evitare l'archiviazione -> il messaggio viene ignorato e rimane sulla casella di posta
				return StoreType.IGNORE_MESSAGE;
			}
		}
		else if (dcwParsedMessage.isSegnaturaInteropPAMessage(conf.getCodAmmInteropPA(), conf.getCodAooInteropPA())) { //messaggio di segnatura di interoperabilità PA
			String query = "[/doc/@messageId]=\"" + parsedMessage.getMessageId() + "\" AND [/doc/@cod_amm_aoo]=\"" + conf.getCodAmmAoo() + "\"";
			System.out.println("decodeStoreType.query[7] = " + query);

			return StoreType.SAVE_NEW_DOCUMENT_INTEROP_PA;
		}
		else if (conf.isEnableFatturePA() && dcwParsedMessage.isFatturaPAMessage(conf.getSdiDomainAddress())) { //messaggio fattura PA
			String query = "[/doc/@messageId]=\"" + parsedMessage.getMessageId() + "\" AND [/doc/@cod_amm_aoo]=\"" + conf.getCodAmmAoo() + "\"";
			System.out.println("decodeStoreType.query[8] = " + query);
			
			return StoreType.SAVE_NEW_DOCUMENT_FATTURA_PA;
		}
		return storeType;
	}

	@Override
	protected void attachInteropPAPecReceiptToDocument(ParsedMessage parsedMessage) throws Exception {
		System.out.println("attachInteropPAPecReceiptToDocument... ");
	}

	@Override
	protected void attachInteropPANotificationToDocument(ParsedMessage parsedMessage) throws Exception {
		System.out.println("attachInteropPANotificationToDocument... ");
	}

	@Override
	protected String buildNewNumprotStringForSavingDocument() throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected String buildNewNumrepStringForSavingDocument(String repertorioCod) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected RifEsterno createMittenteFatturaPA(ParsedMessage parsedMessage) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected void attachFatturaPANotificationToDocument(ParsedMessage parsedMessage) throws Exception {
		System.out.println("attachFatturaPANotificationToDocument... ");
	}

	@Override
	protected void attachFatturaPAPecReceiptToDocument(ParsedMessage parsedMessage) throws Exception {
		System.out.println("attachFatturaPAPecReceiptToDocument... ");
	}
	
	@Override
	public void messageStored(ParsedMessage parsedMessage) throws Exception {
		// Nothing to do...
		System.out.println("messageStored!");
	}

}
