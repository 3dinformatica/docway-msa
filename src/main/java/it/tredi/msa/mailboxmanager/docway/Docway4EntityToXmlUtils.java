package it.tredi.msa.mailboxmanager.docway;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Pattern;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import it.tredi.msa.mailboxmanager.docway.fatturapa.DatiDDTItem;
import it.tredi.msa.mailboxmanager.docway.fatturapa.DatiFatturaContainer;
import it.tredi.msa.mailboxmanager.docway.fatturapa.DatiFatturaPAItem;
import it.tredi.msa.mailboxmanager.docway.fatturapa.DatiLineaItem;
import it.tredi.msa.mailboxmanager.docway.fatturapa.DatiRiepilogoItem;
import it.tredi.msa.mailboxmanager.docway.fatturapa.ErroreItem;
import it.tredi.msa.mailboxmanager.docway.fatturapa.FatturaPAItem;
import it.tredi.msa.mailboxmanager.docway.fatturapa.NotificaItem;

/**
 * Conversione dell'oggetto di model del documento in doc XML per DocWay4
 */
public class Docway4EntityToXmlUtils {

	public static Document docwayDocumentToXml(DocwayDocument doc, Date currentDate) {
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
		docEl.addAttribute("num_prot", doc.getNumProt());
		
		//annullato
		docEl.addAttribute("annullato", doc.isAnnullato()? "si" : "no");
		
		//messageId
		docEl.addAttribute("messageId", doc.getMessageId());
		
		//archiviatore (recipientEmail + completed)
		Element archiviatoreEl = DocumentHelper.createElement("archiviatore");
		docEl.add(archiviatoreEl);
		archiviatoreEl.addAttribute("recipientEmail", doc.getRecipientEmail());
		archiviatoreEl.addAttribute("completed", "no"); //set partial status (not completes)
		
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
		String oggetto = doc.getOggetto();
		oggetto = oggetto.replaceAll(Pattern.quote("|"), "-"); //il pipe disturba i titoli di extraway
		oggetto = oggetto.replaceAll(Pattern.quote("\""), ""); //anche le virgolette disturbano il funzionamento
		oggettoEl.setText(oggetto);

		//voce_indice
		if (doc.getVoceIndice() != null && !doc.getVoceIndice().isEmpty()) {
			Element voceIndicefEl = DocumentHelper.createElement("voce_indice");
			docEl.add(voceIndicefEl);
			voceIndicefEl.addAttribute("xml:space", "preserve");
			voceIndicefEl.setText(doc.getVoceIndice());
		}		
		
		//classificazione
		if (doc.getClassif() != null && !doc.getClassif().isEmpty()) {
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
				repertorioEl.addAttribute("numero", doc.getRepertorioCod() + "^" + doc.getCodAmmAoo() + "-" + (new SimpleDateFormat("yyyy")).format(currentDate) + ".");
		}

		//scarto
//TODO
		
		//postit
		for (Postit postit: doc.getPostitL())
			docEl.add(postitToXml(postit));
		
		//fatturaPA
		if (doc.getFatturaPA() != null) {
			Element extraEl = docEl.element("extra");
			if (extraEl == null) {
				extraEl = DocumentHelper.createElement("extra");
				docEl.add(extraEl);
			}
			extraEl.add(fatturaPAToXml(doc.getFatturaPA()));
		}
		
		//storia
		Element storiaEl = DocumentHelper.createElement("storia");
		docEl.add(storiaEl);
		for (StoriaItem storiaItem:doc.getStoria())
			storiaEl.add(storiaItemToXml(storiaItem));		
		
		return xmlDocument;
	}
	
	public static Element storiaItemToXml(StoriaItem storiaItem) {
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

	public static Element rifEsternoToXml(RifEsterno rifEsterno) {
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
		
		//CodiceAmministrazione
		if (rifEsterno.getCodiceAmministrazione() != null && !rifEsterno.getCodiceAmministrazione().isEmpty())
			rifEl.addAttribute("CodiceAmministrazione", rifEsterno.getCodiceAmministrazione());

		//CodiceAOO
		if (rifEsterno.getCodiceAOO() != null && !rifEsterno.getCodiceAOO().isEmpty())
			rifEl.addAttribute("CodiceAOO", rifEsterno.getCodiceAOO());
		
		//n_prot
		if (rifEsterno.getnProt() != null && !rifEsterno.getnProt().isEmpty())
			rifEl.addAttribute("n_prot", rifEsterno.getnProt());
		
		//data_prot
		if (rifEsterno.getDataProt() != null && !rifEsterno.getDataProt().isEmpty())
			rifEl.addAttribute("data_prot", rifEsterno.getDataProt());
		
		return rifEl;
	}	
	
	public static Element rifInternoToXml(RifInterno rifInterno) {
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
	
	public static Element allegatoToXml(String descrizione_allegato) {
		Element allegatoEl = DocumentHelper.createElement("allegato");
		allegatoEl.setText(descrizione_allegato);
		allegatoEl.addAttribute("xml:space", "preserve");
		return allegatoEl;
	}	
	
	public static Element postitToXml(Postit postit) {
		Element postitEl = DocumentHelper.createElement("postit");
		postitEl.addAttribute("xml:space", "preserve");
		postitEl.addAttribute("operatore", postit.getOperatore());
		if (postit.getCodOperatore() != null && !postit.getCodOperatore().isEmpty())
			postitEl.addAttribute("cod_operatore", postit.getCodOperatore());
		postitEl.addAttribute("data", postit.getData());
		postitEl.addAttribute("ora", postit.getOra());
		postitEl.setText(postit.getText());
		return postitEl;
	}
	
	public static Element interoperabilitaItemToXml(InteroperabilitaItem interopItem) {
        Element interopEl = DocumentHelper.createElement("interoperabilita");
        interopEl.addAttribute("name", interopItem.getId());
        interopEl.addAttribute("title", interopItem.getName());
        interopEl.addAttribute("data", interopItem.getData());
        interopEl.addAttribute("ora", interopItem.getOra());
        interopEl.addAttribute("info", interopItem.getInfo());
        interopEl.addAttribute("messageId", interopItem.getMessageId());	
		return interopEl;
	}
	
	public static Element fatturaPAToXml(FatturaPAItem fatturaPA) {
		Element fatturaPAEl = DocumentHelper.createElement("fatturaPA");
		
		fatturaPAEl.addAttribute("fileNameFattura", fatturaPA.getFileNameFattura());
		fatturaPAEl.addAttribute("extensionFattura", fatturaPA.getExtensionFattura());
		fatturaPAEl.addAttribute("state", fatturaPA.getState());
		fatturaPAEl.addAttribute("sendDate", (new SimpleDateFormat("yyyyMMdd")).format(fatturaPA.getSendDate()));
		fatturaPAEl.addAttribute("versione", fatturaPA.getVersione());
		fatturaPAEl.addAttribute("identificativoSdI", fatturaPA.getIdentificativoSdI());
		if (fatturaPA.getEmailSdI() != null)
			fatturaPAEl.addAttribute("emailSdI", fatturaPA.getEmailSdI());
		if (fatturaPA.getEmailToFattPassiva() != null)
			fatturaPAEl.addAttribute("emailToFattPassiva", fatturaPA.getEmailToFattPassiva().toLowerCase());
		fatturaPAEl.addAttribute("codiceDestinatario", fatturaPA.getCodiceDestinatario());
		fatturaPAEl.addAttribute("formato", fatturaPA.getFormato());
		fatturaPAEl.addAttribute("tentativiInvio", fatturaPA.getTentativiInvio());
		fatturaPAEl.addAttribute("messageId", fatturaPA.getMessageId());
		if (fatturaPA.getNote() != null && !fatturaPA.getNote().isEmpty()) {
			Element noteEl = fatturaPAEl.addElement("note");
			noteEl.setText(fatturaPA.getNote());
		}

		//Dati fattura
		for (DatiFatturaContainer datiFattura: fatturaPA.getDatiFatturaL())
			fatturaPAEl.add(datiFatturaContainerToXml(datiFattura));

		return fatturaPAEl;
	}
	
	public static Element datiFatturaContainerToXml(DatiFatturaContainer datiFattura) {
		Element datiFatturaEl = DocumentHelper.createElement("datiFattura");

		//DatiGenerali
		Element dgEl = datiFatturaEl.addElement("datiGeneraliDocumento");
		dgEl.addAttribute("tipoDocumento", datiFattura.getTipoDocumento_dg());
		dgEl.addAttribute("divisa", datiFattura.getDivisa_dg());
		dgEl.addAttribute("data", datiFattura.getData_dg());
		dgEl.addAttribute("numero", datiFattura.getNumero_dg());
		dgEl.addAttribute("importoTotaleDocumento", datiFattura.getImportoTotaleDocumento_dg());
		dgEl.addAttribute("arrotondamento", datiFattura.getArrotondamento_dg());
		dgEl.addAttribute("art73", datiFattura.getArt73_dg());
		if (datiFattura.getCausale_dg() != null && !datiFattura.getCausale_dg().isEmpty()) {
			Element causaleEl = DocumentHelper.createElement("causale");
			causaleEl.addText(datiFattura.getCausale_dg());
			dgEl.add(causaleEl);			
		}
		
		//DatiOrdineAcquisto
		for (DatiFatturaPAItem datiOrdineAcquisto: datiFattura.getDatiOrdineAcquisto())
			datiFatturaEl.add(datiOrdineAcquistoToXml(datiOrdineAcquisto));
		
		//DatiContratto
		for (DatiFatturaPAItem datiContratto: datiFattura.getDatiContratto())
			datiFatturaEl.add(datiContrattoToXml(datiContratto));
		
		//DatiConvenzione
		for (DatiFatturaPAItem datiConvenzione: datiFattura.getDatiConvenzione())
			datiFatturaEl.add(datiConvenzioneToXml(datiConvenzione));
		
		//DatiRicezione
		for (DatiFatturaPAItem datiRicezione: datiFattura.getDatiRicezione())
			datiFatturaEl.add(datiRicezioneToXml(datiRicezione));		
		
		//DatiFattureCollegate
		for (DatiFatturaPAItem datiFattureCollegate: datiFattura.getDatiFattureCollegate())
			datiFatturaEl.add(datiFattureCollegateToXml(datiFattureCollegate));		
		
		//DatiSAL
		for (String riferimentoFase: datiFattura.getRiferimentoFaseSAL()) {
			Element datiSALEl = datiFatturaEl.addElement("datiSAL");
			datiSALEl.addAttribute("riferimentoFase", riferimentoFase);
		}

		//DatiDDT
		for (DatiDDTItem datiDDT: datiFattura.getDatiDDT()) {
			Element datiDDTEl = datiFatturaEl.addElement("datiDDT");
			datiDDTEl.addAttribute("numeroDDT", datiDDT.getNumero());
			datiDDTEl.addAttribute("dataDDT", datiDDT.getData());
		}
			
		//DatiBeniServizi
		Element datiBeniServiziEl = datiFatturaEl.addElement("datiBeniServizi");
		for (DatiLineaItem datiLinea: datiFattura.getDatiBeniServizi().getLinea()) {
			Element lineaEl = datiBeniServiziEl.addElement("linea");
			lineaEl.setText(datiLinea.getDescrizione());
			lineaEl.addAttribute("prezzoTotale", datiLinea.getPrezzoTotale());
		}
		for (DatiRiepilogoItem datiRiepilogo: datiFattura.getDatiBeniServizi().getRiepilogo()) {
			Element riepilogoEl = datiBeniServiziEl.addElement("riepilogo");
			riepilogoEl.addAttribute("aliquotaIVA", datiRiepilogo.getAliquotaIVA());
			riepilogoEl.addAttribute("imponibileImporto", datiRiepilogo.getImponibileImporto());
			riepilogoEl.addAttribute("imposta", datiRiepilogo.getImposta());
		}
			
		//DatiRegistroFatture
		Element datiRegistroFattureEl = datiFatturaEl.addElement("datiRegistroFatture");
		datiRegistroFattureEl.addAttribute("numFattura", datiFattura.getDatiRegistroFatture().getNumeroFattura());
		datiRegistroFattureEl.addAttribute("dataEmissioneFattura", datiFattura.getDatiRegistroFatture().getDataEmissioneFattura());
		Element oggettoFornituraEl = datiRegistroFattureEl.addElement("oggettoFornitura");
		oggettoFornituraEl.addCDATA(datiFattura.getDatiRegistroFatture().getOggettoFornitura());
		datiRegistroFattureEl.addAttribute("importoTotale", datiFattura.getDatiRegistroFatture().getImportoTotale());
		if (datiFattura.getDatiRegistroFatture().getDataScadenzaFattura() != null)
			datiRegistroFattureEl.addAttribute("dataScadenzaFattura", datiFattura.getDatiRegistroFatture().getDataScadenzaFattura());
		
		return datiFatturaEl;
	}
	
	public static Element datiOrdineAcquistoToXml(DatiFatturaPAItem datiOrdineAcquisto) {
		return datiFatturaPAItemToXml("datiOrdineAcquisto", datiOrdineAcquisto);
	}

	public static Element datiContrattoToXml(DatiFatturaPAItem datiContratto) {
		return datiFatturaPAItemToXml("datiContratto", datiContratto);
	}

	public static Element datiConvenzioneToXml(DatiFatturaPAItem datiConvenzione) {
		return datiFatturaPAItemToXml("datiConvenzione", datiConvenzione);
	}	
	
	public static Element datiRicezioneToXml(DatiFatturaPAItem datiRicezione) {
		return datiFatturaPAItemToXml("datiRicezione", datiRicezione);
	}	

	public static Element datiFattureCollegateToXml(DatiFatturaPAItem datiFattureCollegate) {
		return datiFatturaPAItemToXml("datiFattureCollegate", datiFattureCollegate);
	}		
	
	private static Element datiFatturaPAItemToXml(String elName, DatiFatturaPAItem dati) {
		Element el = DocumentHelper.createElement(elName);
		
		el.addAttribute("riferimentoNumeroLinea", dati.getRiferimentoNumeroLinea());
		el.addAttribute("idDocumento", dati.getIdDocumento());
		el.addAttribute("data", dati.getData());
		el.addAttribute("numItem", dati.getNumItem());
		el.addAttribute("codiceCommessaConvenzione", dati.getCodiceCommessaConvenzione());
		el.addAttribute("codiceCUP", dati.getCodiceCUP());
		el.addAttribute("codiceCIG", dati.getCodiceCIG());
		
		return el;
	}

	public static Element notificaItemToXml(NotificaItem notificaItem) {
		Element notificaEl = DocumentHelper.createElement("notifica");
		
		notificaEl.addAttribute("name", notificaItem.getName());
		notificaEl.addAttribute("title", notificaItem.getTitle());
		
		if (notificaItem.getTipo() != null)
			notificaEl.addAttribute("tipo", notificaItem.getTipo());
		
		notificaEl.addAttribute("data", notificaItem.getData());
		notificaEl.addAttribute("ora", notificaItem.getOra());
		notificaEl.addAttribute("info", notificaItem.getInfo());

		if (notificaItem.getNumeroFattura() != null && !notificaItem.getNumeroFattura().isEmpty())
			notificaEl.addAttribute("numeroFattura", notificaItem.getNumeroFattura());
		
		if (notificaItem.getAnnoFattura() != null && !notificaItem.getAnnoFattura().isEmpty())
			notificaEl.addAttribute("annoFattura", notificaItem.getAnnoFattura());
		
		notificaEl.addAttribute("messageId", notificaItem.getMessageId());

		if (notificaItem.getEsito() != null && !notificaItem.getEsito().isEmpty()) {
			notificaEl.addAttribute("esito", notificaItem.getEsito());
		}
		
		if (notificaItem.getNote() != null && !notificaItem.getNote().isEmpty()) {
			Element elNote = notificaEl.addElement("note");
			elNote.addCDATA(notificaItem.getNote());
		}
		
		if (notificaItem.getErrori().size() > 0) {
			Element elErrori = notificaEl.addElement("errori");
			for (ErroreItem erroreItem: notificaItem.getErrori()) {
				Element elErrore = elErrori.addElement("errore");
				elErrore.addAttribute("codice", erroreItem.getCodice());
				elErrore.addCDATA(erroreItem.getDescrizione());			
			}			
		}
		
		if (notificaItem.getRiferita() != null)
			notificaEl.addAttribute("riferita", notificaItem.getRiferita());
		
		return notificaEl;
	}
	
}
		    
