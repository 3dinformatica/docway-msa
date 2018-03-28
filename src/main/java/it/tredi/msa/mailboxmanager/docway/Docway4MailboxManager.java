package it.tredi.msa.mailboxmanager.docway;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import javax.mail.MessagingException;

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
import it.tredi.msa.entity.ParsedMessage;
import it.tredi.msa.entity.docway.AssegnatarioMailboxConfiguration;
import it.tredi.msa.entity.docway.Docway4MailboxConfiguration;
import it.tredi.msa.entity.docway.DocwayDocument;
import it.tredi.msa.entity.docway.DocwayFile;
import it.tredi.msa.entity.docway.RifEsterno;
import it.tredi.msa.entity.docway.RifInterno;
import it.tredi.msa.entity.docway.StoriaItem;
import it.tredi.msa.notification.MailNotificationSender;

public class Docway4MailboxManager extends DocwayMailboxManager {

	protected ExtrawayClient xwClient;
	protected ExtrawayClient aclClient;
	private int lastSavedDocumentPhysDoc;
	private boolean extRestrictionsOnAcl;
	
	private static final Logger logger = LogManager.getLogger(Docway4MailboxManager.class.getName());
	
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
    	super.closeSession();
		try {
			xwClient.disconnect();
		}
		catch (Exception e) {
//TODO - log warning			
		}
		try {
			aclClient.disconnect();
		}
		catch (Exception e) {
//TODO - log warning			
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
		
		if (conf.isNotifyRPA() || conf.isNotifyCC()) { //if notification is activated
			Document document = (Document)saveDocRetObj;
			MailSender mailSender = ((MailNotificationSender)Services.getNotificationService().getNotificationSender()).createMailSender();
logger.info(document.getRootElement().attributeValue("nrecord"));
			
			for (RifInterno rifInterno:doc.getRifInterni()) {
				if (rifInterno.isNotify()) { //if rif interno has to be notified
					
					
					
logger.info("NOTIFICA " + rifInterno.getDiritto() + " - " + rifInterno.getCodPersona());
					
					//TODO - realizzare qua tutto il codice per l'invio di email di notifica
					
				}
			}				

			try {
				mailSender.disconnect();
			} 
			catch (MessagingException e) {
//TODO - log warn
			}
		}
	}

	
}
