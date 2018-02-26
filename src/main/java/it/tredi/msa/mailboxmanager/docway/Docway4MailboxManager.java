package it.tredi.msa.mailboxmanager.docway;

import java.text.SimpleDateFormat;
import java.util.List;

import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import it.highwaytech.db.QueryResult;
import it.tredi.extraway.ExtrawayClient;
import it.tredi.msa.entity.docway.DocwayDocument;
import it.tredi.msa.entity.docway.DocwayMailboxConfiguration;
import it.tredi.msa.entity.docway.RifEsterno;
import it.tredi.msa.entity.docway.StoriaItem;

public class Docway4MailboxManager extends DocwayMailboxManager {

	protected ExtrawayClient xwClient;
	protected ExtrawayClient aclClient;
	private int lastSavedDocumentPhysDoc;
	private boolean extRestrictionsOnAcl;
	

	@Override
    public void openSession() throws Exception {
		super.openSession();
		DocwayMailboxConfiguration conf = (DocwayMailboxConfiguration)getConfiguration();
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
	protected void saveNewDocument(DocwayDocument doc) throws Exception {
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
		
		//allegato
		
		//fascicolo

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
		
		//postit
		
		//storia
		Element storiaEl = DocumentHelper.createElement("storia");
		docEl.add(storiaEl);
		for (StoriaItem storiaItem:doc.getStoria())
			storiaEl.add(storiaItemToXml(storiaItem));
		
		//save in Extraway
//		lastSavedDocumentPhysDoc = xwClient.saveNewDocument(xmlDocument);
//TODO - attualmente il salvataggio è disabilitato
		
		

/*
 * COMMENTATO TEMPORANEAMENTE IL CODICE PER LA MODIFICA DEL DOCUMENTO APPENA SALVATO
		Document document = xwClient.loadAndLockDocument(lastSavedDocumentPhysDoc);
		document.getRootElement().element("oggetto").setText("Oggetto modificato");
		xwClient.saveDocument(document, lastSavedDocumentPhysDoc);
*/	
		
		int ret = 0;
		ret++;

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
			
			//ruolo
			if (rifEsterno.getReferenteRuolo() != null && !rifEsterno.getReferenteRuolo().isEmpty())
				referenteEl.addAttribute("ruolo", rifEsterno.getReferenteRuolo());
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
	
    public RifEsterno buildRifEsterno(String name, String address) throws Exception {
        RifEsterno rifEsterno = new RifEsterno();
        rifEsterno.setEmail(address);

        //in caso di archivio con anagrafiche esterne replicate su AOO differenti occorre filtrare anche sull'AOO della casella di archiviazione
        String query = "[struest_emailaddr]=\"" + address + "\" OR [persest_recapitoemailaddr]=\"" + address + "\" OR " +
        		"[/struttura_esterna/email_certificata/@addr/]=\"" + address + "\" OR [/persona_esterna/recapito/email_certificata/@addr]=\"" + address + "\"";
        if (extRestrictionsOnAcl) {
        	String codeSedeAoo = ((DocwayMailboxConfiguration)super.getConfiguration()).getCodAmmAoo();
        	if (codeSedeAoo != null && !codeSedeAoo.isEmpty()) {
	        	query = "(([struest_emailaddr]=\"" + address + "\" OR [/struttura_esterna/email_certificata/@addr/]=\"" + address + "\") AND [/struttura_esterna/#cod_ammaoo]=\"" + codeSedeAoo + "\")"
	        			+ " OR"
	        			+ " (([persest_recapitoemailaddr]=\"" + address + "\" OR [/persona_esterna/recapito/email_certificata/@addr]=\"" + address + "\") AND [/persona_esterna/#cod_ammaoo]=\"" + codeSedeAoo + "\")";
        	}
        }

        // first try: search email address
        int count = aclClient.search(query, null, "ud(xpart:/xw/@UdType)", 0, 0);
        if (count == 0) { // sender is not present in ACL
            rifEsterno.setNome(name);
//TODO            MailStorageAgent.logger.info("\t\t no document matching '" + rifExtEmail + "'");
        }
        else { // extract sender info from ACL
            Document document = aclClient.loadDocByQueryResult(0);
            if (document.getRootElement().getName().equals("struttura_esterna")) { // struttura_esterna
//TODO                MailStorageAgent.logger.info("\t\t found " + count + " 'struttura_esterna' matching '" + rifExtEmail + "'");
//TODO                MailStorageAgent.logger.info("\t\t ...first chosen");
                rifEsterno.setNome(document.getRootElement().element("nome").getText());
                rifEsterno.setCod(document.getRootElement().attributeValue("cod_uff"));
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
//TODO                MailStorageAgent.logger.info("\t\t found " + count + " 'persona_esterna' matching '" + rifExtEmail + "'");
//TODO                MailStorageAgent.logger.info("\t\t ...first chosen");
                rifEsterno.setNome(document.getRootElement().attributeValue("cognome") + " " + document.getRootElement().attributeValue("nome"));
                rifEsterno.setCod(document.getRootElement().attributeValue("matricola"));
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

                	// mbernardini 28/10/2015 : restrizione su AOO.
                	// Recupero cod_amm_aoo da persona esterna in caso di caricamento della struttura di appartenenza
                	String cod_amm = document.getRootElement().attributeValue("cod_amm", "");
                	String cod_aoo = document.getRootElement().attributeValue("cod_amm", "");

                	String queryStruest = "[struest_coduff]=" + appartenenze;
                	if (!cod_amm.isEmpty() && !cod_aoo.isEmpty())
                		queryStruest += " AND [/struttura_esterna/#cod_ammaoo]=\"" + cod_amm + cod_aoo + "\"";
                	count = aclClient.search(queryStruest);
                	QueryResult selezione = aclClient.getQueryResult();

                    if (count > 0) { // at least one struttura_esterna found
//TODO                        MailStorageAgent.logger.info("\t\t found " + selezione.getCount() + " related 'struttura_esterna'");
                        if (count > 1) {
                            String emailDomain = address.substring(address.indexOf("@"));
//TODO                            MailStorageAgent.logger.info("\t\t searching 'struttura_esterna' with email matching " + emailDomain + "...");
                            queryStruest = "[struest_emailaddr]=\"*" + emailDomain + "\"";
                            if (!cod_amm.isEmpty() && !cod_aoo.isEmpty())
                        		queryStruest += " AND [/struttura_esterna/#cod_ammaoo]=\"" + cod_amm + cod_aoo + "\"";

                            int count1 = aclClient.search(queryStruest, selezione.id, "", 0, 0);
                            
//TODO                            MailStorageAgent.logger.info("\t\t ..." + selezione1.getCount() + " found");
                            if (count1 > 0) {
                                ; //uso la nuova selezione (quella raffinata)
                            }
                            else {
                            	aclClient.setQueryResult(selezione);
                            }
                        }
//TODO                        MailStorageAgent.logger.info("\t\t ...first chosen");
                        document = aclClient.loadDocByQueryResult(0);

                        rifEsterno.setReferenteNominativo(rifEsterno.getNome());
                        rifEsterno.setReferenteCod(rifEsterno.getCod());
                        	
                        rifEsterno.setNome(document.getRootElement().element("nome").getText());
                        rifEsterno.setCod(document.getRootElement().attributeValue("cod_uff"));

                        // MASSIMILIANO 02/07/2013: l'email ce l'abbiamo gia'
                        //if (email.length() == 0)
                        //    email = document.getAttributeValue("/struttura_esterna/email/@addr", "");
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

        /*
    	doc/rif_esterni/rif/nome - ragione sociale o cognome e nome del riferimento esterno + el1.addAttribute("xml:space", "preserve");
    	doc/rif_esterni/rif/nome/@cod - codice identificativo in ACL del riferimento esterno

    	doc/rif_esterni/rif/email_certificata/@addr - indirizzo di posta elettronica certificata
    	doc/rif_esterni/rif/@codice_fiscale - codice fiscale (NUOVO - METTERE???)
    	doc/rif_esterni/rif/@partita_iva -partitra iva (NUOVO - METTERE???)
    	
    	doc/rif_esterni/rif/indirizzo - indirizzo vero e proprio (xml:space)
    	doc/rif_esterni/rif/indirizzo/@email - indirizzo email
    	doc/rif_esterni/rif/indirizzo/@fax - fax
    	doc/rif_esterni/rif/indirizzo/@tel - numero telefonico

    	doc/rif_esterni/rif/referente/@cod - codice identificativo univoco della persona_esterna
    	doc/rif_esterni/rif/referente/@nominativo - cognome e nome della persona_esterna
    	doc/rif_esterni/rif/referente/@ruolo - eventuale ruolo ricoperto dalla persona_esterna all’interno della struttura_esterna (NUOVO - METTERE???)

    */  

//TODO - manca la gestione di RUOLO, CODICE_FISCALE, PARTITA_IVA
        
    }	

    
	/**
	 * 
<doc    tipo="arrivo" bozza="si" cod_amm_aoo="3DINBOL" nrecord="00117865" anno="" data_prot="20180213" num_prot="" annullato="no" messageId="B0DA6608-4932-4178-962C-F146C2084FF5@3di.it">
  <oggetto xml:space="preserve">aaa</oggetto>
  <postit cod_operatore="TEST" data="20180213" operatore="TEST TSET">aaa</postit>
  <tipologia cod="E-mail"/>
  <rif_esterni>
    <rif>
      <nome xml:space="preserve">Simone Stagni</nome>
      <indirizzo email="sstagni@3di.it" xml:space="preserve"/>
    </rif>
  </rif_esterni>
  <allegato xml:space="preserve">0 - nessun allegato</allegato>
  <storia>
    <creazione oper="Archiviatore Email" uff_oper="Protocollo" data="20180213" ora="08:46:46"/>
    <responsabilita nome_persona="Candelora Nicola" cod_persona="PI000056" data="20180213" ora="08:46:46" cod_operatore="" operatore="Archiviatore Email(Protocollo)" nome_uff="Servizio archivistico" cod_uff="SI000010"/>
  </storia>
  <rif_interni>
    <rif diritto="RPA" nome_persona="Candelora Nicola" cod_persona="PI000056" nome_uff="Servizio archivistico" cod_uff="SI000010"/>
  </rif_interni>
  <note xml:space="preserve">From: Simone Stagni
To: test-archiviatore-xw@libero.it
Cc: 
Sent: Tue, 13 Feb 2018 08:46:38 +0100
Subject: aaa

aaaa
</note>  <archiviatore recipientEmail="test-archiviatore-xw@libero.it"/>  <files>    <xw:file name="48582.txt" title="testo email" size="136" impronta="2omIAODJriZH0XRbYa98d26qNlSalLuGSAwA7AMLNZc=" tipoImpronta="SHA256" index="yes" agent.meta="ignore">      <chkin operatore="Archiviatore Email(Protocollo)" cod_operatore="" data="20180213" ora="08:46:46"/>    </xw:file>    <xw:file name="48583.eml" title="MessaggioOriginale.eml" readonly="si" size="3973" impronta="TPTshu0cSaM66SyPPzPXPfCRfrgzyctpJoBv80zEVPQ=" tipoImpronta="SHA256" der_to="48584.txt" agent.meta="ignore">      <chkin operatore="Archiviatore Email(Protocollo)" cod_operatore="" data="20180213" ora="08:46:46"/>    </xw:file>    <xw:file name="48584.txt" title="MessaggioOriginale.txt" der_from="48583.eml" index="yes" size="8">      <chkin operatore="convertitore" cod_operatore="convertitore" data="20180213" ora="08:46:59"/>    </xw:file>  </files><?xw-meta Dbms="ExtraWay" DbmsVer="25.9.4" OrgNam="3D Informatica" OrgVer="22.3.1.6" Classif="1.0" ManGest="1.0" ManTec="0.0.4" InsUser="xw.msa" InsTime="20180213084646" ModUser="xw.docway-test.bo.priv.76.fcs" ModTime="20180213084659"?><?xw-crc key32=2491d4a5-10000051?></doc>
	 * 
	 */    
	
}
