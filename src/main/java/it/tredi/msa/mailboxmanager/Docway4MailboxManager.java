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
		
		//storia
		
		//rif_interni
		
		//note
		
		//repertorio
		
		//classificazione
		
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
	
	
	
	
}
