package it.tredi.msa.mailboxmanager.docway;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import it.highwaytech.db.QueryResult;
import it.tredi.extraway.ExtrawayClient;
import it.tredi.mail.MailSender;
import it.tredi.msa.Services;
import it.tredi.msa.configuration.docway.AssegnatarioMailboxConfiguration;
import it.tredi.msa.configuration.docway.Docway4MailboxConfiguration;
import it.tredi.msa.mailboxmanager.ParsedMessage;
import it.tredi.msa.notification.MailNotificationSender;

public class Docway4MailboxManager extends DocwayMailboxManager {

	protected ExtrawayClient xwClient;
	protected ExtrawayClient aclClient;
	private int lastSavedDocumentPhysDoc;
	private boolean extRestrictionsOnAcl;
	
	private static final Logger logger = LogManager.getLogger(Docway4MailboxManager.class.getName());
	
	//for notification emails
    private static final String _DB_ ="%DB%";
    private static final String _ALIAS_ ="%ALIAS%";
    private static final String _NRECORD_ ="%NRECORD%";
    private static final int MITT_DEST_LENGTH = 25;
    private static final int OGGETTO_LENGTH = 25;    
    public final static String TUTTI_COD = "tutti";
	
	@Override
    public void openSession() throws Exception {
		super.openSession();
		Docway4MailboxConfiguration conf = (Docway4MailboxConfiguration)getConfiguration();
		xwClient = new ExtrawayClient(conf.getXwHost(), conf.getXwPort(), conf.getXwDb(), conf.getXwUser(), conf.getXwPassword());
		xwClient.connect();
		aclClient = new ExtrawayClient(conf.getXwHost(), conf.getXwPort(), conf.getAclDb(), conf.getXwUser(), conf.getXwPassword());
		aclClient.connect();
		extRestrictionsOnAcl = checkExtRestrictionsOnAcl();
    }
	
	@Override
    public void closeSession() {
		Docway4MailboxConfiguration conf = (Docway4MailboxConfiguration)getConfiguration();
    	super.closeSession();
		try {
			xwClient.disconnect();
		}
		catch (Exception e) {
			logger.warn("[" + conf.getName() + "] failed to close eXtraWay session [" + conf.getXwDb() + "]", e);			
		}
		try {
			aclClient.disconnect();
		}
		catch (Exception e) {
			logger.warn("[" + conf.getName() + "] failed to close eXtraWay session [" + conf.getAclDb() + "]", e);
		}
	}  	
	
	@Override
	protected Object saveNewDocument(DocwayDocument doc) throws Exception {
		Docway4MailboxConfiguration conf = (Docway4MailboxConfiguration)getConfiguration();
		
		//save new document in Extraway
		Document xmlDocument = docwayDocumentToXml(doc);
		lastSavedDocumentPhysDoc = xwClient.saveNewDocument(xmlDocument);
		
		//load and lock document
		xmlDocument = xwClient.loadAndLockDocument(lastSavedDocumentPhysDoc, conf.getXwLockOpAttempts(), conf.getXwLockOpDelay());
		
		//upload files
		boolean uploaded = false;
		for (DocwayFile file:doc.getFiles()) {
			file.setId(xwClient.addAttach(file.getName(), file.getContent(), conf.getXwLockOpAttempts(), conf.getXwLockOpDelay()));
			uploaded = true;
		}

		//upload immagini
		for (DocwayFile file:doc.getImmagini()) {
			file.setId(xwClient.addAttach(file.getName(), file.getContent(), conf.getXwLockOpAttempts(), conf.getXwLockOpDelay()));
			uploaded = true;
		}
		
		//update document with uploaded xw:file(s)
		if (uploaded) {
			updateXmlWithDocwayFiles(xmlDocument, doc);
			xwClient.saveDocument(xmlDocument, lastSavedDocumentPhysDoc);
		}
		else { //no filed uploaded -> unlock document
			xwClient.unlockDocument(lastSavedDocumentPhysDoc);
		}

		return xmlDocument;
	}
	
	private Document docwayDocumentToXml(DocwayDocument doc) {
		//DocwayDocument -> xml
		Element docEl = DocumentHelper.createElement("doc");
		Document xmlDocument =DocumentHelper.createDocument(docEl);
		
		//nrecord
		docEl.addAttribute("nrecord", ".");
		
		//tipoDoc
		docEl.addAttribute("tipo", doc.getTipo());
		
		//bozza
		docEl.addAttribute("bozza", doc.isBozza()? "si" : "no");
		
		//cod_amm_aoo
		docEl.addAttribute("cod_amm_aoo", doc.getCodAmmAoo());
		
		//anno
		docEl.addAttribute("anno", doc.getAnno());
		
		//data_prot
		docEl.addAttribute("data_prot", doc.getDataProt());
		
		//num_prot
		if (doc.isBozza())
			docEl.addAttribute("num_prot", doc.getNumProt());
		
		//annullato
		docEl.addAttribute("annullato", doc.isAnnullato()? "si" : "no");
		
		//messageId
		docEl.addAttribute("messageId", doc.getMessageId());
		
		//recipientEmail
		Element archiviatoreEl = DocumentHelper.createElement("archiviatore");
		docEl.add(archiviatoreEl);
		archiviatoreEl.addAttribute("recipientEmail", doc.getRecipientEmail());
		
		//autore
		if (doc.getAutore() != null && !doc.getAutore().isEmpty()) {
			Element autoreEl = DocumentHelper.createElement("autore");
			docEl.add(autoreEl);
			autoreEl.addAttribute("xml:space", "preserve");
			autoreEl.setText(doc.getAutore());			
		}			
		
		//tipologia
		if (doc.getTipologia() != null && !doc.getTipologia().isEmpty()) {
			Element tipologiaEl = DocumentHelper.createElement("tipologia");
			docEl.add(tipologiaEl);
			tipologiaEl.addAttribute("cod", doc.getTipologia());
		}
		
		//mezzo_trasmissione
		if (doc.getMezzoTrasmissione() != null && !doc.getMezzoTrasmissione().isEmpty()) {
			Element mezzoTrasmissioneEl = DocumentHelper.createElement("mezzo_trasmissione");
			docEl.add(mezzoTrasmissioneEl);
			mezzoTrasmissioneEl.addAttribute("cod", doc.getMezzoTrasmissione());			
		}

		//rif_esterni
		if (doc.getRifEsterni().size() > 0) {
			Element rifEstEl = DocumentHelper.createElement("rif_esterni");
			docEl.add(rifEstEl);
			for (RifEsterno rifEsterno:doc.getRifEsterni())
				rifEstEl.add(rifEsternoToXml(rifEsterno));
		}
		
		//oggetto
		Element oggettoEl = DocumentHelper.createElement("oggetto");
		docEl.add(oggettoEl);
		oggettoEl.addAttribute("xml:space", "preserve");
//TODO - effettuare la pulizia dell'oggetto - vedi vecchio archiviatore
		oggettoEl.setText(doc.getOggetto());		

		//voce_indice
		if (doc.getVoceIndice() != null && !doc.getVoceIndice().isEmpty()) {
			Element voceIndicefEl = DocumentHelper.createElement("voce_indice");
			docEl.add(voceIndicefEl);
			voceIndicefEl.addAttribute("xml:space", "preserve");
			voceIndicefEl.setText(doc.getVoceIndice());
		}		
		
		//classificazione
		if (doc.getClassifCod() != null && !doc.getClassifCod().isEmpty()) {
			Element classifEl = DocumentHelper.createElement("classif");
			docEl.add(classifEl);
			classifEl.addAttribute("xml:space", "preserve");
			classifEl.setText(doc.getClassif());
			classifEl.addAttribute("cod", doc.getClassifCod());
		}

		//rif_interni
		if (doc.getRifInterni().size() > 0) {
			Element rifIntEl = DocumentHelper.createElement("rif_interni");
			docEl.add(rifIntEl);
			for (RifInterno rifInterno:doc.getRifInterni())
				rifIntEl.add(rifInternoToXml(rifInterno));
		}		
		
		//allegato
		for(String allegato:doc.getAllegato())
			docEl.add(allegatoToXml(allegato));
		
		//fascicolo
//TODO		

		//note
		if (doc.getNote() != null && !doc.getNote().isEmpty()) {
			Element noteEl = DocumentHelper.createElement("note");
			docEl.add(noteEl);
			noteEl.addAttribute("xml:space", "preserve");
			noteEl.setText(doc.getNote());			
		}
		
		//repertorio
		if (doc.getRepertorioCod() != null && !doc.getRepertorioCod().isEmpty()) {
			Element repertorioEl = DocumentHelper.createElement("repertorio");
			docEl.add(repertorioEl);
			repertorioEl.setText(doc.getRepertorio());
			repertorioEl.addAttribute("cod", doc.getRepertorioCod());
			if (doc.isBozza())
				repertorioEl.addAttribute("numero", "");
			else
				repertorioEl.addAttribute("numero", doc.getRepertorioCod() + "^" + doc.getCodAmmAoo() + "-" + (new SimpleDateFormat("yyyy")).format(super.currentDate) + ".");
		}

		//scarto
//TODO		
		
		//postit
//TODO		
		
		//storia
		Element storiaEl = DocumentHelper.createElement("storia");
		docEl.add(storiaEl);
		for (StoriaItem storiaItem:doc.getStoria())
			storiaEl.add(storiaItemToXml(storiaItem));		
		
		return xmlDocument;
	}
	
	private void updateXmlWithDocwayFiles(Document xmlDocument, DocwayDocument doc) {
		
		//files
		List<DocwayFile> files = doc.getFiles();
		if (files.size() > 0) {
			Element filesEl = DocumentHelper.createElement("files");
			xmlDocument.getRootElement().add(filesEl);
			updateXmlWithDocwayFileList(filesEl, files, true);
		}
		
		//immagini
		List<DocwayFile> immagini = doc.getImmagini();
		if (immagini.size() > 0) {
			Element immaginiEl = DocumentHelper.createElement("immagini");
			xmlDocument.getRootElement().add(immaginiEl);
			updateXmlWithDocwayFileList(immaginiEl, immagini, false);
		}		
		
	}

	private void updateXmlWithDocwayFileList(Element filesContinerEl, List<DocwayFile> files, boolean convert) {
		for (DocwayFile file:files) {
			
			//xw:file
			Element xwFileEl = DocumentHelper.createElement("xw:file");
			filesContinerEl.add(xwFileEl);
			xwFileEl.addAttribute("name", file.getId());
			xwFileEl.addAttribute("title", file.getName());
			if (convert)
				xwFileEl.addAttribute("convert", "yes");
			
			//checkin
			Element chkinEl = DocumentHelper.createElement("chkin");
			xwFileEl.add(chkinEl);
			chkinEl.addAttribute("operatore", file.getOperatore());
			chkinEl.addAttribute("cod_operatore", file.getCodOperatore());
			chkinEl.addAttribute("data", file.getData());
			chkinEl.addAttribute("ora", file.getOra());
		}
	}	
	
	
	private Element storiaItemToXml(StoriaItem storiaItem) {
		Element el = DocumentHelper.createElement(storiaItem.getType());
		if (storiaItem.getOper() != null && !storiaItem.getOper().isEmpty())
			el.addAttribute("oper", storiaItem.getOper());
		if (storiaItem.getCodOper() != null && !storiaItem.getCodOper().isEmpty())
			el.addAttribute("cod_oper", storiaItem.getCodOper());		
		if (storiaItem.getUffOper() != null && !storiaItem.getUffOper().isEmpty())
			el.addAttribute("uff_oper", storiaItem.getUffOper());
		if (storiaItem.getCodUffOper() != null && !storiaItem.getCodUffOper().isEmpty())
			el.addAttribute("cod_uff_oper", storiaItem.getCodUffOper());
		if (storiaItem.getNomePersona() != null && !storiaItem.getNomePersona().isEmpty())
			el.addAttribute("nome_persona", storiaItem.getNomePersona());
		if (storiaItem.getCodPersona() != null && !storiaItem.getCodPersona().isEmpty())
			el.addAttribute("cod_persona", storiaItem.getCodPersona());		
		if (storiaItem.getNomeUff() != null && !storiaItem.getNomeUff().isEmpty())
			el.addAttribute("nome_uff", storiaItem.getNomeUff());
		if (storiaItem.getCodUff() != null && !storiaItem.getCodUff().isEmpty())
			el.addAttribute("cod_uff", storiaItem.getCodUff());		
		if (storiaItem.getOperatore() != null && !storiaItem.getOperatore().isEmpty())
			el.addAttribute("operatore", storiaItem.getOperatore());
		if (storiaItem.getCodOperatore() != null && !storiaItem.getCodOperatore().isEmpty())
			el.addAttribute("cod_operatore", storiaItem.getCodOperatore());
		if (storiaItem.getData() != null && !storiaItem.getData().isEmpty())
			el.addAttribute("data", storiaItem.getData());
		if (storiaItem.getOra() != null && !storiaItem.getOra().isEmpty())
			el.addAttribute("ora", storiaItem.getOra());
		return el;
	}

	private Element rifEsternoToXml(RifEsterno rifEsterno) {
		Element rifEl = DocumentHelper.createElement("rif");
		
		//nome
		Element nomeEl = DocumentHelper.createElement("nome");
		rifEl.add(nomeEl);
		nomeEl.addAttribute("xml:space", "preserve");
		nomeEl.setText(rifEsterno.getNome());
		
		//cod
		if (rifEsterno.getCod() != null && !rifEsterno.getCod().isEmpty())
			nomeEl.addAttribute("cod", rifEsterno.getCod());

		//email_certificata
		if (rifEsterno.getEmailCertificata() != null && !rifEsterno.getEmailCertificata().isEmpty()) {
			Element emailCertificataEl = DocumentHelper.createElement("email_certificata");
			rifEl.add(emailCertificataEl);
			emailCertificataEl.addAttribute("addr", rifEsterno.getEmailCertificata());
		}
		
		//codice_fiscale
		if (rifEsterno.getCodiceFiscale() != null && !rifEsterno.getCodiceFiscale().isEmpty())
			rifEl.addAttribute("codice_fiscale", rifEsterno.getCodiceFiscale());
			
		//partita_iva
		if (rifEsterno.getPartitaIva() != null && !rifEsterno.getPartitaIva().isEmpty())
			rifEl.addAttribute("partita_iva", rifEsterno.getPartitaIva());
		
		//indirizzo
		if ( (rifEsterno.getIndirizzo() != null && !rifEsterno.getIndirizzo().isEmpty()) || (rifEsterno.getEmail() != null && !rifEsterno.getEmail().isEmpty()) || 
				(rifEsterno.getFax() != null && !rifEsterno.getFax().isEmpty()) || (rifEsterno.getTel()!= null && !rifEsterno.getTel().isEmpty()) ) {
			
			Element indirizzoEl = DocumentHelper.createElement("indirizzo");
			rifEl.add(indirizzoEl);
			
			//indirizzo
			if (rifEsterno.getIndirizzo() != null && !rifEsterno.getIndirizzo().isEmpty()) {
				indirizzoEl.addAttribute("xml:space", "preserve");
				indirizzoEl.setText(rifEsterno.getIndirizzo());
			}
			
			//email
			if (rifEsterno.getEmail() != null && !rifEsterno.getEmail().isEmpty())
				indirizzoEl.addAttribute("email", rifEsterno.getEmail());
			
			//fax
			if (rifEsterno.getFax() != null && !rifEsterno.getFax().isEmpty())
				indirizzoEl.addAttribute("fax", rifEsterno.getFax());
			
			//tel
			if (rifEsterno.getTel() != null && !rifEsterno.getTel().isEmpty())
				indirizzoEl.addAttribute("tel", rifEsterno.getTel());
		}

		//referente
		if (rifEsterno.getReferenteNominativo() != null && !rifEsterno.getReferenteNominativo().isEmpty()) {
			Element referenteEl = DocumentHelper.createElement("referente");
			rifEl.add(referenteEl);
			
			//nominativo
			referenteEl.addAttribute("nominativo", rifEsterno.getReferenteNominativo());
			
			//cod
			if (rifEsterno.getReferenteCod() != null && !rifEsterno.getReferenteCod().isEmpty())
				referenteEl.addAttribute("cod", rifEsterno.getReferenteCod());
		}
		
			
		return rifEl;
	}
	
	private boolean checkExtRestrictionsOnAcl() {
		boolean restrictions = false;
		String uniquerule = xwClient.getUniqueRuleDb("struttura_esterna");
		if (uniquerule != null && !uniquerule.isEmpty()) {
			// Verifica delle restrizione in base alla unique_rule specificata.
			// FIXME il controllo andrebbe fatto in base all'analisi degli and, or, ecc... per il momento ci accontentiamo di questa NON soluzione
			int indexCodUff = uniquerule.indexOf("[XML,/struttura_esterna/@cod_uff]");
			int indexParentesi = uniquerule.indexOf("(");
			if (indexCodUff != -1 && indexParentesi != -1 && indexParentesi < indexCodUff)
				restrictions = true;
		}
		return restrictions;
	}
	
    public RifEsterno createRifEsterno(String name, String address) throws Exception {
        RifEsterno rifEsterno = new RifEsterno();
        rifEsterno.setEmail(address);

        //in caso di archivio con anagrafiche esterne replicate su AOO differenti occorre filtrare anche sull'AOO della casella di archiviazione
        String query = "[struest_emailaddr]=\"" + address + "\" OR [persest_recapitoemailaddr]=\"" + address + "\" OR " +
        		"[/struttura_esterna/email_certificata/@addr/]=\"" + address + "\" OR [/persona_esterna/recapito/email_certificata/@addr]=\"" + address + "\"";
        if (extRestrictionsOnAcl) {
        	String codAmmAoo = ((Docway4MailboxConfiguration)super.getConfiguration()).getCodAmmAoo();
        	if (codAmmAoo != null && !codAmmAoo.isEmpty()) {
	        	query = "(([struest_emailaddr]=\"" + address + "\" OR [/struttura_esterna/email_certificata/@addr/]=\"" + address + "\") AND [/struttura_esterna/#cod_ammaoo]=\"" + codAmmAoo + "\")"
	        			+ " OR"
	        			+ " (([persest_recapitoemailaddr]=\"" + address + "\" OR [/persona_esterna/recapito/email_certificata/@addr]=\"" + address + "\") AND [/persona_esterna/#cod_ammaoo]=\"" + codAmmAoo + "\")";
        	}
        }

        // first try: search email address
        int count = aclClient.search(query, null, "ud(xpart:/xw/@UdType)", 0, 0);
        if (count == 0) { // sender is not present in ACL
            rifEsterno.setNome(name);
        }
        else { // extract sender info from ACL
            Document document = aclClient.loadDocByQueryResult(0);
            if (document.getRootElement().getName().equals("struttura_esterna")) { // struttura_esterna
                rifEsterno.setNome(document.getRootElement().element("nome").getText());
                rifEsterno.setCod(document.getRootElement().attributeValue("cod_uff"));
                rifEsterno.setCodiceFiscale(document.getRootElement().attributeValue("codice_fiscale") == null? "" : document.getRootElement().attributeValue("codice_fiscale"));
                rifEsterno.setPartitaIva(document.getRootElement().attributeValue("partita_iva") == null? "" : document.getRootElement().attributeValue("partita_iva"));
                // MASSIMILIANO 02/07/2013: l'email ce l'abbiamo gia'
                //email = document.getAttributeValue("/struttura_esterna/email/@addr", "");
                Attribute tempAttr = (Attribute)document.selectSingleNode("/struttura_esterna/email_certificata/@addr");
                rifEsterno.setEmailCertificata(tempAttr == null? "" : tempAttr.getValue());
                tempAttr = (Attribute)document.selectSingleNode("/struttura_esterna/telefono[@tipo='tel']/@num");
                rifEsterno.setTel(tempAttr == null? "" : tempAttr.getValue());
                tempAttr = (Attribute)document.selectSingleNode("/struttura_esterna/telefono[@tipo='fax']/@num");
                rifEsterno.setFax(tempAttr == null? "" : tempAttr.getValue());
                Element el = (Element)document.selectSingleNode("/struttura_esterna/indirizzo");
                String indirizzo = "";
                String indirizzo1 = "";
                if (el != null) {
                    indirizzo = el.getText();
                    indirizzo1 = (el.attributeValue("cap") == null || el.attributeValue("cap").length() == 0) ? ""
                            : " " + el.attributeValue("cap");
                    indirizzo1 += (el.attributeValue("comune") == null || el.attributeValue("comune").length() == 0) ? ""
                            : " " + el.attributeValue("comune");
                    indirizzo1 += (el.attributeValue("prov") == null || el.attributeValue("prov").length() == 0) ? ""
                            : " (" + el.attributeValue("prov") + ")";
                    indirizzo1 += (el.attributeValue("nazione") == null || el.attributeValue("nazione").length() == 0) ? ""
                            : " - " + el.attributeValue("nazione");
                }
                if (indirizzo1.length() > 0)
                    indirizzo += " -" + indirizzo1;
                rifEsterno.setIndirizzo(indirizzo);
            }
            else { // persona_esterna
                rifEsterno.setNome(document.getRootElement().attributeValue("cognome") + " " + document.getRootElement().attributeValue("nome"));
                rifEsterno.setCod(document.getRootElement().attributeValue("matricola"));
                rifEsterno.setCodiceFiscale(document.getRootElement().attributeValue("codice_fiscale") == null? "" : document.getRootElement().attributeValue("codice_fiscale"));
                rifEsterno.setPartitaIva("");
                // MASSIMILIANO 02/07/2013: l'email ce l'abbiamo gia'
                //email = document.getAttributeValue("/persona_esterna/recapito/email/@addr", "");
                Attribute tempAttr = (Attribute)document.selectSingleNode("/persona_esterna/recapito/email_certificata/@addr");
                rifEsterno.setEmailCertificata(tempAttr == null? "" : tempAttr.getValue());
                tempAttr = (Attribute)document.selectSingleNode("/persona_esterna/recapito/telefono[@tipo='tel']/@num");
                rifEsterno.setTel(tempAttr == null? "" : tempAttr.getValue());
                tempAttr = (Attribute)document.selectSingleNode("/persona_esterna/recapito/telefono[@tipo='fax']/@num");
                rifEsterno.setFax(tempAttr == null? "" : tempAttr.getValue());
                Element el = (Element)document.selectSingleNode("/persona_esterna/recapito/indirizzo");
                String indirizzo = "";
                String indirizzo1 = "";
                if (el != null) {
                    indirizzo = el.getText();
                    indirizzo1 = (el.attributeValue("cap") == null || el.attributeValue("cap").length() == 0) ? ""
                            : " " + el.attributeValue("cap");
                    indirizzo1 += (el.attributeValue("comune") == null || el.attributeValue("comune").length() == 0) ? ""
                            : " " + el.attributeValue("comune");
                    indirizzo1 += (el.attributeValue("prov") == null || el.attributeValue("prov").length() == 0) ? ""
                            : " (" + el.attributeValue("prov") + ")";
                    indirizzo1 += (el.attributeValue("nazione") == null || el.attributeValue("nazione").length() == 0) ? ""
                            : " - " + el.attributeValue("nazione");
                }
                if (indirizzo1.length() > 0)
                    indirizzo += " -" + indirizzo1;
                rifEsterno.setIndirizzo(indirizzo);

                // search eventual struttura_esterna
                List<Element> l = document.selectNodes("persona_esterna/appartenenza");
                String appartenenze = "";
                for (int i = 0; i < l.size(); i++)
                    appartenenze += " OR \"" + ((Element)l.get(i)).attributeValue("cod_uff") + "\"";
                if (appartenenze.length() > 3)
                    appartenenze = appartenenze.substring(3);
                if (appartenenze.length() > 0) {

                	String cod_amm = document.getRootElement().attributeValue("cod_amm", "");
                	String cod_aoo = document.getRootElement().attributeValue("cod_amm", "");

                	String queryStruest = "[struest_coduff]=" + appartenenze;
                	if (!cod_amm.isEmpty() && !cod_aoo.isEmpty())
                		queryStruest += " AND [/struttura_esterna/#cod_ammaoo]=\"" + cod_amm + cod_aoo + "\"";
                	count = aclClient.search(queryStruest);
                	QueryResult selezione = aclClient.getQueryResult();

                    if (count > 0) { // at least one struttura_esterna found
                        if (count > 1) {
                            String emailDomain = address.substring(address.indexOf("@"));
                            queryStruest = "[struest_emailaddr]=\"*" + emailDomain + "\"";
                            if (!cod_amm.isEmpty() && !cod_aoo.isEmpty())
                        		queryStruest += " AND [/struttura_esterna/#cod_ammaoo]=\"" + cod_amm + cod_aoo + "\"";

                            int count1 = aclClient.search(queryStruest, selezione.id, "", 0, 0);
                            if (count1 > 0) {
                                ; //uso la nuova selezione (quella raffinata)
                            }
                            else {
                            	aclClient.setQueryResult(selezione);
                            }
                        }
                        document = aclClient.loadDocByQueryResult(0);

                        rifEsterno.setReferenteNominativo(rifEsterno.getNome());
                        rifEsterno.setReferenteCod(rifEsterno.getCod());
                        	
                        rifEsterno.setNome(document.getRootElement().element("nome").getText());
                        rifEsterno.setCod(document.getRootElement().attributeValue("cod_uff"));

                        if (rifEsterno.getCodiceFiscale().isEmpty())
                        	rifEsterno.setCodiceFiscale(document.getRootElement().attributeValue("codice_fiscale") == null? "" : document.getRootElement().attributeValue("codice_fiscale"));

                    	rifEsterno.setPartitaIva(document.getRootElement().attributeValue("partita_iva") == null? "" : document.getRootElement().attributeValue("partita_iva"));
                        
                        if (rifEsterno.getTel().length() == 0) {
                        	tempAttr = (Attribute)document.selectSingleNode("/struttura_esterna/telefono[@tipo='tel']/@num");
                        	rifEsterno.setTel(tempAttr == null? "" : tempAttr.getValue());
                        }
                        if (rifEsterno.getFax().length() == 0) {
                        	tempAttr = (Attribute)document.selectSingleNode("/struttura_esterna/telefono[@tipo='fax']/@num");
                        	rifEsterno.setFax(tempAttr == null? "" : tempAttr.getValue());
                        }
                        if (rifEsterno.getIndirizzo().length() == 0) {
                            el = (Element)document.selectSingleNode("/struttura_esterna/indirizzo");
                            indirizzo1 = "";
                            if (el != null) {
                                indirizzo = el.getText();
                                indirizzo1 = (el.attributeValue("cap") == null || el.attributeValue("cap").length() == 0) ? ""
                                        : " " + el.attributeValue("cap");
                                indirizzo1 += (el.attributeValue("comune") == null || el.attributeValue("comune")
                                        .length() == 0) ? "" : " " + el.attributeValue("comune");
                                indirizzo1 += (el.attributeValue("prov") == null || el.attributeValue("prov").length() == 0) ? ""
                                        : " (" + el.attributeValue("prov") + ")";
                                indirizzo1 += (el.attributeValue("nazione") == null || el.attributeValue("nazione")
                                        .length() == 0) ? "" : " - " + el.attributeValue("nazione");
                            }
                            if (indirizzo1.length() > 0)
                                indirizzo += " -" + indirizzo1;
                            rifEsterno.setIndirizzo(indirizzo);
                        }

                    }
                }
            }

        }
        
        return rifEsterno;
    }	

	private Element rifInternoToXml(RifInterno rifInterno) {
		Element rifEl = DocumentHelper.createElement("rif");
		rifEl.addAttribute("diritto", rifInterno.getDiritto());
		rifEl.addAttribute("nome_persona", rifInterno.getNomePersona());
		rifEl.addAttribute("cod_persona", rifInterno.getCodPersona());		
		rifEl.addAttribute("nome_uff", rifInterno.getNomeUff());
		rifEl.addAttribute("cod_uff", rifInterno.getCodUff());
		
		if (!rifInterno.getTipoUff().isEmpty())
			rifEl.addAttribute("tipo_uff", rifInterno.getTipoUff());		

		if (!rifInterno.getDiritto().equalsIgnoreCase("RPA"))
			rifEl.addAttribute("intervento", rifInterno.isIntervento()? "si" : "no");
		
		if (rifInterno.getCodFasc() != null && !rifInterno.getCodFasc().isEmpty())
			rifEl.addAttribute("cod_fasc", rifInterno.getCodFasc());
		return rifEl;
	}
	
	private Element allegatoToXml(String descrizione_allegato) {
		Element allegatoEl = DocumentHelper.createElement("allegato");
		allegatoEl.setText(descrizione_allegato);
		allegatoEl.addAttribute("xml:space", "preserve");
		return allegatoEl;
	}
	
	private List<RifInterno> createRifInterniByPersintQuery(String query) throws Exception {
		List<RifInterno> rifsL = new ArrayList<RifInterno>();
		int count = aclClient.search(query);
		if (count == 0)
			return null;
		for (int i=0; i<count; i++) { //per ogni persona interna
			RifInterno rifInterno = new RifInterno();
	        Document document = aclClient.loadDocByQueryResult(i);
	        String codPersona = ((Attribute)document.selectSingleNode("persona_interna/@matricola")).getValue();
	        String nomePersona = ((Attribute)document.selectSingleNode("persona_interna/@cognome")).getValue() + " " + ((Attribute)document.selectSingleNode("persona_interna/@nome")).getValue();
	        String codUff = ((Attribute)document.selectSingleNode("persona_interna/@cod_uff")).getValue();
	        String codAmmAoo = ((Attribute)document.selectSingleNode("persona_interna/@cod_amm")).getValue() + ((Attribute)document.selectSingleNode("persona_interna/@cod_aoo")).getValue();
	        rifInterno.setCodPersona(codPersona);
	        rifInterno.setNomePersona(nomePersona);
	        rifInterno.setCodUff(codUff);
	        rifsL.add(rifInterno);
			aclClient.search("[struint_coduff]=\"" + rifInterno.getCodUff() + "\" AND [/struttura_interna/#cod_ammaoo/]=\"" + codAmmAoo + "\""); //estrazione nome ufficio
	        document = aclClient.loadDocByQueryResult(0);
	        String nomeUff = document.getRootElement().elementText("nome").trim();
	        rifInterno.setNomeUff(nomeUff);	        
		}
		return rifsL;
	}
	
	public RifInterno createRifInternoByAssegnatario(AssegnatarioMailboxConfiguration assegnatario) throws Exception {
		String codAmmAoo = ((Docway4MailboxConfiguration)super.getConfiguration()).getCodAmmAoo();
		RifInterno rifInterno = new RifInterno();
		if (assegnatario.isRuolo()) { //ruolo
			String query = "[ruoli_id]=\"" + assegnatario.getCodRuolo() + "\" AND [/ruolo/#cod_ammaoo/]=\"" + codAmmAoo + "\"";
			aclClient.search(query);
	        Document document = aclClient.loadDocByQueryResult(0);
	        String nomeRuolo = document.getRootElement().elementText("nome").trim();
			rifInterno.setRuolo(nomeRuolo, assegnatario.getCodRuolo());
			rifInterno.setIntervento(assegnatario.isIntervento());			        
		}
		else { //persona-ufficio
			rifInterno.setCodPersona(assegnatario.getCodPersona());
			rifInterno.setCodUff(assegnatario.getCodUff());
			rifInterno.setIntervento(assegnatario.isIntervento());
			
			aclClient.search("[struint_coduff]=\"" + rifInterno.getCodUff() + "\" AND [/struttura_interna/#cod_ammaoo/]=\"" + codAmmAoo + "\"");
	        Document document = aclClient.loadDocByQueryResult(0);
	        String nomeUff = document.getRootElement().elementText("nome").trim();
	        rifInterno.setNomeUff(nomeUff);				

			aclClient.search("[/persona_interna/@matricola]=\"" + rifInterno.getCodPersona() + "\" AND [persint_codammaoo]=\"" + codAmmAoo + "\"");
	        document = aclClient.loadDocByQueryResult(0);
	        String nomePersona = ((Attribute)document.selectSingleNode("persona_interna/@cognome")).getValue() + " " + ((Attribute)document.selectSingleNode("persona_interna/@nome")).getValue();
			rifInterno.setNomePersona(nomePersona);
		}
		return rifInterno;
	}
	
	protected List<RifInterno> createRifInterni(ParsedMessage parsedMessage) throws Exception {
		Docway4MailboxConfiguration conf = (Docway4MailboxConfiguration)getConfiguration();
		List<RifInterno> rifInterni = new ArrayList<RifInterno>();
		String codAmmAoo = ((Docway4MailboxConfiguration)super.getConfiguration()).getCodAmmAoo();
		
		//RPA
		List<RifInterno> rifsL = null;
		if (conf.isDaDestinatario()) {
			String to = parsedMessage.getFromAddress();
            to = to.substring(to.indexOf("+") + 1, to.indexOf("@"));
            rifsL = createRifInterniByPersintQuery("[persint_loginname]=\"" + to + "\" AND [persint_codammaoo]=\"" + codAmmAoo + "\"");
		}
		
		if (conf.isDaMittente() && rifsL == null) {
			rifsL = createRifInterniByPersintQuery("[persint_recapitoemailaddr]=\"" + parsedMessage.getFromAddress() + "\" AND [persint_codammaoo]=\"" + codAmmAoo + "\"");
		}

		RifInterno rpa = (rifsL == null)? createRifInternoByAssegnatario(conf.getResponsabile()) : rifsL.get(0);
		rpa.setDiritto("RPA");
		rpa.setIntervento(true);
		rifInterni.add(rpa);
		
		//CC
		if (conf.isDaCopiaConoscenza()) {
			String query = parsedMessage.getCcAddressesAsString().replaceAll(",", "\" OR \"");
			if (!query.isEmpty()) {
				rifsL = createRifInterniByPersintQuery("[persint_recapitoemailaddr]=\"" + parsedMessage.getFromAddress() + "\" AND [persint_codammaoo]=\"" + codAmmAoo + "\"");
				for (RifInterno cc:rifsL) {
					cc.setDiritto("CC");
					rifInterni.add(cc);
				}
			}
		}
		
		for (AssegnatarioMailboxConfiguration assegnatario: conf.getAssegnatariCC()) {
			RifInterno cc = createRifInternoByAssegnatario(assegnatario);
			cc.setDiritto("CC");
			rifInterni.add(cc);
		}
		
		return rifInterni;
	}

	@Override
	protected void sendNotificationMails(DocwayDocument doc, Object saveDocRetObj) {
		Docway4MailboxConfiguration conf = (Docway4MailboxConfiguration)getConfiguration();
		MailNotificationSender notificationSender = (MailNotificationSender)Services.getNotificationService().getNotificationSender();
		MailSender mailSender = notificationSender.createMailSender();
		try {
			mailSender.connect();
			String body = getBodyForEmail(conf.getNotificationAppHost(), conf.getNotificationAppHost1(), conf.getNotificationAppUri(), conf.getXwDb(), (Document)saveDocRetObj);
			
			Set<String>	notifiedAddresses = new HashSet<String>();
			for (RifInterno rifInterno:doc.getRifInterni()) {
				if (rifInterno.isNotify()) { //if rif interno has to be notified
					if ((rifInterno.getDiritto().equals("RPA") && conf.isNotifyRPA()) || (!rifInterno.getDiritto().equals("RPA") && conf.isNotifyCC()))
						sendNotificationMail(mailSender, notificationSender.getSenderAdress(), notificationSender.getSenderPersonal(), rifInterno.getCodPersona(), rifInterno.getDiritto().equals("RPA"), doc, (Document)saveDocRetObj, body, conf.getCodAmmAoo(), notifiedAddresses);
				}
			}				
		} 
		catch (Exception e) {
			logger.error("[" + conf.getName() + "] unexpected error sending notification emails", e);
		}
		finally {
			try {
				mailSender.disconnect();
			} 
			catch (Exception e) {
				logger.warn("[" + conf.getName() + "] failed to close mailSender session", e);
			}				
		}
	}

	private void sendNotificationMail(MailSender mailSender, String senderAddress, String senderPersonal, String matricola, boolean isRPA, DocwayDocument doc, Document savedDocument, String body, String codAmmAooDestinatario, Set<String> notifiedAddresses) {
		Docway4MailboxConfiguration conf = (Docway4MailboxConfiguration)getConfiguration();
		try {
			String subject = getSubjectForEmail(isRPA?"RPA":"CC", savedDocument);
			String destEmail = getEmailWithMatricola(matricola, codAmmAooDestinatario);
			String []destinatari = destEmail.split(",");
			for (String dest:destinatari) {
				if (!dest.isEmpty() && !notifiedAddresses.contains(dest)) {
					try {
						if (logger.isInfoEnabled())
							logger.warn("[" + conf.getName() + "] sending notification email [" + dest + "]");
						notifiedAddresses.add(dest);
						mailSender.sendMail(senderAddress, senderPersonal, dest, subject, body);	
					}
					catch (Exception e) {
						logger.error("[" + conf.getName() + "] unexpected error sending notification email [" + dest + "]", e);
					}
				}
			}
			
		} 
		catch (Exception e) {
			logger.error("[" + conf.getName() + "] unexpected error extracting email address for matricola [" + matricola + "]", e);
		}
	}
	
    public String getBodyForEmail(String httpHost, String httpHost1, String theURL, String db, Document document) throws java.net.MalformedURLException {
        String nrecord = document.getRootElement().attributeValue("nrecord");
        String tipo_doc = document.getRootElement().attributeValue("tipo");
        String data_prot = document.getRootElement().attributeValue("data_prot");

        String num_prot = document.getRootElement().attributeValue("num_prot");
        if (num_prot != null && num_prot.length() > 0) {
            if (num_prot.length() >= 13)
                num_prot = "N. " + deleteZeros(num_prot.substring(13)) + " del " + data_prot + " (" + num_prot + ")";
            else {
                String d = document.selectSingleNode("/doc/storia/creazione/@data").getText();
                num_prot = "Bozza del " + dateFormat(d);
            }
        }
        // Federico 19/12/05: aggiunto test su 'data_prot' in quanto può essere assente [RW 0032968]
        else if (data_prot != null && data_prot.length() > 0) {
                 num_prot = "Documento non protocollato del " + dateFormat(data_prot);
             }
             else {
                 num_prot = "Documento non protocollato";
             }

        String mittOrDest = "";
        if (tipo_doc == null)
            num_prot = tipo_doc = "";
        if (tipo_doc.equals("arrivo"))
            mittOrDest = "\nMittente: ";
        else if (tipo_doc.equals("partenza"))
            mittOrDest = "\nDestinatario: ";
        if (mittOrDest.length() > 0) {
            @SuppressWarnings("unchecked")
			List<Element> l = (List<Element>)document.selectNodes("/doc/rif_esterni/rif/nome");
            if (l.size() > 0)
                mittOrDest += ((Element) l.get(0)).getText();
        }

        String oggetto = "\nOggetto: " + document.getRootElement().elementText("oggetto");
        String tmpURL = getNotifyURL(theURL, httpHost, httpHost1, db, "docnrecord", nrecord);
        String ret = num_prot + mittOrDest + oggetto + "\n\nPer visualizzare:\n " + tmpURL;

        return ret;
    }
    
    private String deleteZeros(String s) {
        while (s.charAt(0) == '0')
            s = s.substring(1);
        return s;
    }    
    
    public String dateFormat(String s) {
        if (s.length() == 8)
            return s.substring(6, 8) + "/" + s.substring(4, 6) + "/" + s.substring(0, 4);
        else
            return s;
    }   
    
    public String getNotifyURL(String theURL, String httpHost, String httpHost1, String db, String alias, String nrecord) throws java.net.MalformedURLException {
    	String newURL = theURL;
    	String newURL1 = theURL;
    	boolean completeUrl = false;
    	boolean completeUrl1 = false;

    	// sstagni - 15 Nov 2006 - se url contiene hcadm.dll viene modificata in hcprot.dll
    	// FindBug: Dead store to theURL in it.highwaytech.apps.generic.Protocollo.getNotifyURL()
    	//if (theURL.indexOf("hcadm.dll") != -1)
    	//    theURL = theURL.replaceAll("hcadm.dll", "hcprot.dll");

    	if (httpHost.length() > 0) {
    		// si verifica se la property è già  esaustiva...
    		if ( (httpHost.indexOf(_DB_) > -1)    ||
    				(httpHost.indexOf(_ALIAS_) > -1) ||
    				(httpHost.indexOf(_NRECORD_) > -1) ) {
    			completeUrl = true;
    			newURL = ((httpHost.replaceAll(_DB_, db)).replaceAll(_ALIAS_, alias)).replaceAll(_NRECORD_, nrecord);
    		}
    		else if (newURL.indexOf("//") == -1) newURL = httpHost + newURL;
    		else {
    			int index = newURL.indexOf("//");
    			index = newURL.indexOf("/", index + 2);
    			newURL = httpHost + newURL.substring(index);
    		}
    	}
    	if (httpHost1.length() > 0) {
    		// si verifica se la property è già  esaustiva...
    		if ( (httpHost1.indexOf(_DB_) > -1)    ||
    				(httpHost1.indexOf(_ALIAS_) > -1) ||
    				(httpHost1.indexOf(_NRECORD_) > -1) ) {
    			completeUrl1 = true;
    			newURL1 = ((httpHost1.replaceAll(_DB_, db)).replaceAll(_ALIAS_, alias)).replaceAll(_NRECORD_, nrecord);
    		}
    		else if (newURL1.indexOf("//") == -1) newURL1 = httpHost1 + newURL1;
    		else {
    			int index = newURL1.indexOf("//");
    			index = newURL1.indexOf("/", index + 2);
    			newURL1 = httpHost1 + newURL1.substring(index);
    		}
    	}
    	else newURL1 = "";

    	String  tmpURL = "";
    	String tmpURL1 = "";
    	if ( !completeUrl )   tmpURL = "?db=" + db + "&verbo=queryplain&query=%5B" + alias + "%5D%3D" + nrecord;
    	if ( !completeUrl1 ) tmpURL1 = "?db=" + db + "&verbo=queryplain&query=%5B" + alias + "%5D%3D" + nrecord;

    	String defURL = newURL + tmpURL;
    	java.net.URL url = new java.net.URL(defURL);
    	defURL = url.toExternalForm();

    	String defURL1 = "";
    	if (newURL1.length() > 0) {
    		defURL1 = newURL1 + tmpURL1;
    		java.net.URL url1 = new java.net.URL(defURL1);
    		defURL1 = url1.toExternalForm();
    	}

    	String ret = defURL;
    	if (defURL1.length() > 0)
    		ret += "\n\n" + defURL1;
    	return ret;
    }    
    
    public String getSubjectForEmail(String type, Document document) throws java.net.MalformedURLException {
        String tipo_doc = document.getRootElement().attributeValue("tipo");

        String mittOrDest = "";
        if (tipo_doc.equals("arrivo") || tipo_doc.equals("partenza")) {
            @SuppressWarnings("unchecked")
			List<Element> l = (List<Element>)document.selectNodes("/doc/rif_esterni/rif/nome");            
            if (l.size() > 0)
                mittOrDest += ((Element) l.get(0)).getText();
        }
        String oggetto = document.getRootElement().elementText("oggetto");
        String ret = "[" + type + "]" + getMittDestSubjectFor(mittOrDest) + ":" + getOggettoSubjectFor(oggetto);
        return ret;
    }   
    
    private String getMittDestSubjectFor(String mittOrDest) {
        mittOrDest = mittOrDest.replaceAll("\n", " ");

        if (mittOrDest.length() > MITT_DEST_LENGTH)
            return mittOrDest.substring(0, MITT_DEST_LENGTH) + "...";
        else
            return mittOrDest;
    }

    private String getOggettoSubjectFor(String oggetto) {
        oggetto = oggetto.replaceAll("\n", " ");
        if (oggetto.length() > OGGETTO_LENGTH)
            return oggetto.substring(0, OGGETTO_LENGTH) + "...";
        else
            return oggetto;
    }    
    
	public String getEmailWithMatricola(String matricola, String codAmmAoo) throws Exception {
		String res = "";

		String query = "";
		if (matricola.startsWith(TUTTI_COD + "_")) {
			String codUff = matricola.substring(matricola.indexOf("_") + 1);
			query = "([persint_coduff]=" + codUff + " OR [persint_gruppoappartenenzacod]=" + codUff + " OR [persint_mansionecod]=" + codUff + ") AND [/persona_interna/#cod_ammaoo/]=" + codAmmAoo;
		}
		else {
			query = "[persint_matricola]=" + matricola + " AND [/persona_interna/#cod_ammaoo/]=" + codAmmAoo;
		}

		int count = aclClient.search(query);
		for (int i=0; i<count; i++) {
			Document document = aclClient.loadDocByQueryResult(i);
			Attribute indirizzoEl = (Attribute)document.selectSingleNode("/persona_interna/recapito/email/@addr");
			if (indirizzoEl != null) {
				String indirizzo = indirizzoEl.getText().trim();
				if (!indirizzo.isEmpty())
					res += "," + indirizzo;
			}
		}

		if (!res.isEmpty())
			res = res.substring(1);
		
		return res;
	}    
    
}
