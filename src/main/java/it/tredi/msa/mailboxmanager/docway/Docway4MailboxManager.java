package it.tredi.msa.mailboxmanager.docway;

import java.util.List;

import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import it.highwaytech.db.QueryResult;
import it.tredi.extraway.ExtrawayClient;
import it.tredi.msa.entity.docway.DocwayDocument;
import it.tredi.msa.entity.docway.DocwayMailboxConfiguration;

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
		
		//oggetto
		Element oggettoEl = DocumentHelper.createElement("oggetto");
		docEl.add(oggettoEl);
		oggettoEl.addAttribute("xml:space", "preserve");
//TODO - effettuare la pulizia dell'oggetto - vedi vecchio archiviatore
		oggettoEl.setText(doc.getOggetto());
		
		//tipologia
		Element tipologiaEl = DocumentHelper.createElement("tipologia");
		docEl.add(tipologiaEl);
		tipologiaEl.addAttribute("cod", doc.getTipologia());
		
		//mezzo_trasmissione
		Element mezzoTrasmissioneEl = DocumentHelper.createElement("mezzo_trasmissione");
		docEl.add(mezzoTrasmissioneEl);
		mezzoTrasmissioneEl.addAttribute("cod", doc.getMezzoTrasmissione());
		
		//allegato
		
		//rif_esterni
		if (doc.getTipo().toUpperCase().equals("ARRIVO"))
			addRifExtFromACLLookup("/doc/rif_esterni/rif", super.parsedMessage.getFromPersonal(), super.parsedMessage.getFromAddress(), xmlDocument);
		
		//storia
		
		//rif_interni
		
		//note
		
		//repertorio
		
		//classificazione
		if (doc.getClassifCod() != null && !doc.getClassifCod().isEmpty()) {
			Element classifEl = DocumentHelper.createElement("classif");
			docEl.add(classifEl);
			classifEl.addAttribute("xml:space", "preserve");
			classifEl.setText(doc.getClassif());
			classifEl.addAttribute("cod", doc.getClassifCod());
		}
		
		//scarto
		
		//autore
		
		//postit
		
		//save in Extraway
//		lastSavedDocumentPhysDoc = xwClient.saveDocument(xmlDocument);
//TODO - attualmente il salvataggio è disabilitato
		
		int ret = 0;
		ret++;

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
	
    public void addRifExtFromACLLookup(final String xPath, final String rifExtName, final String rifExtEmail, Document document) throws Exception {
        String riferimento = "", riferimento_cod = "", indirizzo = "", fax = "", tel = "", referente = "", referente_cod = "", email_certificata = "";

        // mbernardini 27/10/2015 : in caso di archivio con anagrafiche esterne replicate su AOO differenti occorre filtrare anche sull'AOO della
        // casella di archiviazione
        String query = "[struest_emailaddr]=\"" + rifExtEmail + "\" OR [persest_recapitoemailaddr]=\"" + rifExtEmail + "\" OR " +
        		"[/struttura_esterna/email_certificata/@addr/]=\"" + rifExtEmail + "\" OR [/persona_esterna/recapito/email_certificata/@addr]=\"" + rifExtEmail + "\"";
        if (extRestrictionsOnAcl) {
        	String codeSedeAoo = ((DocwayMailboxConfiguration)super.getConfiguration()).getCodAmmAoo();
        	if (codeSedeAoo != null && !codeSedeAoo.isEmpty()) {
	        	query = "(([struest_emailaddr]=\"" + rifExtEmail + "\" OR [/struttura_esterna/email_certificata/@addr/]=\"" + rifExtEmail + "\") AND [/struttura_esterna/#cod_ammaoo]=\"" + codeSedeAoo + "\")"
	        			+ " OR"
	        			+ " (([persest_recapitoemailaddr]=\"" + rifExtEmail + "\" OR [/persona_esterna/recapito/email_certificata/@addr]=\"" + rifExtEmail + "\") AND [/persona_esterna/#cod_ammaoo]=\"" + codeSedeAoo + "\")";
        	}
        }

        // first try: search email address
        int count = aclClient.search(query, null, "ud(xpart:/xw/@UdType)", 0, 0);
        if (count == 0) { // sender is not present in ACL
            riferimento = rifExtName;
//TODO            MailStorageAgent.logger.info("\t\t no document matching '" + rifExtEmail + "'");
        }
        else { // extract sender info from ACL
            document = aclClient.loadDocByQueryResult(0);
            if (document.getRootElement().getName().equals("struttura_esterna")) { // struttura_esterna
//TODO                MailStorageAgent.logger.info("\t\t found " + count + " 'struttura_esterna' matching '" + rifExtEmail + "'");
//TODO                MailStorageAgent.logger.info("\t\t ...first chosen");
                riferimento = document.getRootElement().element("nome").getText();
                riferimento_cod = document.getRootElement().attributeValue("cod_uff");
                // MASSIMILIANO 02/07/2013: l'email ce l'abbiamo gia'
                //email = document.getAttributeValue("/struttura_esterna/email/@addr", "");
                Attribute tempAttr = (Attribute)document.selectSingleNode("/struttura_esterna/email_certificata/@addr");
                email_certificata = tempAttr == null? "" : tempAttr.getValue();
                tempAttr = (Attribute)document.selectSingleNode("/struttura_esterna/telefono[@tipo='tel']/@num");
                tel = tempAttr == null? "" : tempAttr.getValue();
                tempAttr = (Attribute)document.selectSingleNode("/struttura_esterna/telefono[@tipo='fax']/@num");
                fax = tempAttr == null? "" : tempAttr.getValue();
                Element el = (Element)document.selectSingleNode("/struttura_esterna/indirizzo");
                indirizzo = "";
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
            }
            else { // persona_esterna
//TODO                MailStorageAgent.logger.info("\t\t found " + count + " 'persona_esterna' matching '" + rifExtEmail + "'");
//TODO                MailStorageAgent.logger.info("\t\t ...first chosen");
                riferimento = document.getRootElement().attributeValue("cognome") + " " + document.getRootElement().attributeValue("nome");
                riferimento_cod = document.getRootElement().attributeValue("matricola");
                // MASSIMILIANO 02/07/2013: l'email ce l'abbiamo gia'
                //email = document.getAttributeValue("/persona_esterna/recapito/email/@addr", "");
                Attribute tempAttr = (Attribute)document.selectSingleNode("/persona_esterna/recapito/email_certificata/@addr");
                email_certificata = tempAttr == null? "" : tempAttr.getValue();
                tempAttr = (Attribute)document.selectSingleNode("/persona_esterna/recapito/telefono[@tipo='tel']/@num");
                tel = tempAttr == null? "" : tempAttr.getValue();
                tempAttr = (Attribute)document.selectSingleNode("/persona_esterna/recapito/telefono[@tipo='fax']/@num");
                fax = tempAttr == null? "" : tempAttr.getValue();
                Element el = (Element)document.selectSingleNode("/persona_esterna/recapito/indirizzo");
                indirizzo = "";
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
                            String emailDomain = rifExtEmail.substring(rifExtEmail.indexOf("@"));
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

                        referente = riferimento;
                        referente_cod = riferimento_cod;

                        riferimento = document.getRootElement().element("nome").getText();
                        riferimento_cod = document.getRootElement().attributeValue("cod_uff");

                        // MASSIMILIANO 02/07/2013: l'email ce l'abbiamo gia'
                        //if (email.length() == 0)
                        //    email = document.getAttributeValue("/struttura_esterna/email/@addr", "");
                        if (tel.length() == 0) {
                        	tempAttr = (Attribute)document.selectSingleNode("/struttura_esterna/telefono[@tipo='tel']/@num");
                            tel = tempAttr == null? "" : tempAttr.getValue();
                        }
                        if (fax.length() == 0) {
                        	tempAttr = (Attribute)document.selectSingleNode("/struttura_esterna/telefono[@tipo='fax']/@num");
                            fax = tempAttr == null? "" : tempAttr.getValue();
                        }
                        if (indirizzo.length() == 0) {
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
                        }

                    }
                }
            }

        }

        // Massimiliano 02/07/2013: fix per destinatari multipli (solo docway)
        String xPath4Multi = xPath;
        int  xPath4MultiIndex = xPath.lastIndexOf("/");

        if ( xPath4MultiIndex != -1 ) {
        	xPath4Multi = xPath.substring(xPath.lastIndexOf("/"));

        	if ( xPath4Multi != null && xPath4Multi.equals("/rif")) {
        		xPath4Multi = xPath.substring(0, xPath4MultiIndex);
        	}
        }
        // Fine fix per destinatari multipli

        // insert sender info in document
        Element el = makeXPath(xPath4Multi, document);
        
        Element el0 = DocumentHelper.createElement("rif");
        el.add(el0);
        Element el1 = DocumentHelper.createElement("nome");
        el0.add(el1);
        el1.addText(riferimento);
        el1.addAttribute("xml:space", "preserve");
        if (riferimento_cod.length() > 0) // if cod is present
            el1.addAttribute("cod", riferimento_cod);

//TODO        MailStorageAgent.logger.debug("MailBoxManager.addRifExtFromACLLookup(): indirizzo = " + indirizzo + ", rifExtEmail = " + rifExtEmail + ", fax = " + fax + ", tel = " + tel);

        if (indirizzo.length() > 0 || rifExtEmail.length() > 0 || fax.length() > 0 || tel.length() > 0) {
            Element el2 = DocumentHelper.createElement("indirizzo");
            if (rifExtEmail.length() > 0)
                el2.addAttribute("email", rifExtEmail);
            if (fax.length() > 0)
                el2.addAttribute("fax", fax);
            if (tel.length() > 0)
                el2.addAttribute("tel", tel);
            if (indirizzo.length() > 0)
                el2.addText(indirizzo);
            el2.addAttribute("xml:space", "preserve");
            el0.add(el2);
        }

        if (email_certificata.length() > 0) { // email_certificata is present
            Element el3 = DocumentHelper.createElement("email_certificata");
            el3.addAttribute("addr", email_certificata);
            el0.add(el3);
        }

        if (referente.length() > 0) { // referente is present
            Element el4 = DocumentHelper.createElement("referente");
            el4.addAttribute("nominativo", referente);
            el4.addAttribute("cod", referente_cod);
            el0.add(el4);
        }

//TODO        MailStorageAgent.logger.info("\t ...done");

    }	
	

    /**
     * Builds all elements necessary to obtain the xpath passed as parameter
     * @param xPath
     * @return
     */
    protected Element makeXPath(String xPath, Document document) {
        Element el2 = DocumentHelper.makeElement(document, xPath);
        if (el2 == null)
            el2 = (Element)document.selectSingleNode(xPath);
        return el2;
    }    
	
}
