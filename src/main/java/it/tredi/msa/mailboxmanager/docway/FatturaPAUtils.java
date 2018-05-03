package it.tredi.msa.mailboxmanager.docway;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import org.apache.commons.codec.binary.Base64;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;

import it.tredi.msa.mailboxmanager.ByteArrayContentProvider;

public class FatturaPAUtils {
	private static HashMap<String, String> tipiDocumento = new HashMap<String, String>();
	
	public static final String TIPO_MESSAGGIO_RC = "RC"; // Ricevuta di consegna
	public static final String TIPO_MESSAGGIO_NS = "NS"; // Notifica di scarto
	public static final String TIPO_MESSAGGIO_MC = "MC"; // Notifica di mancata consegna
	public static final String TIPO_MESSAGGIO_NE = "NE"; // Notifica esito cedente / prestatore
	public static final String TIPO_MESSAGGIO_MT = "MT"; // File dei metadati
	public static final String TIPO_MESSAGGIO_EC = "EC"; // Notifica di esito cessionario / committente
	public static final String TIPO_MESSAGGIO_SE = "SE"; // Notifica di scarto esito cessionario / committente
	public static final String TIPO_MESSAGGIO_DT = "DT"; // Notifica decorrenza termini
	public static final String TIPO_MESSAGGIO_AT = "AT"; // Attestazione di avvenuta trasmissione della fattura con impossibilitÃ  di recapito

	public static final String TIPO_MESSAGGIO_SEND = "SEND"; // Invio della fatturaPA al SdI
	
	public static final String ATTESA_NOTIFICHE = "ATTESA";
	public static final String ATTESA_INVIO = "ATTESAINVIO";	

	static { 
		tipiDocumento.put("TD01", "Fattura");
		tipiDocumento.put("TD02", "Acconto/Anticipo su fattura");
		tipiDocumento.put("TD03", "Acconto/Anticipo su parcella");
		tipiDocumento.put("TD04", "Nota di Credito");
		tipiDocumento.put("TD05", "Nota di Debito");
		tipiDocumento.put("TD06", "Parcella");
	}
	
	public static String getVersioneFatturaPA(Document fatturaPADocument) {
		String versione = fatturaPADocument.getRootElement().attributeValue("versione", "");
		if (versione.length() == 0)
			versione = "LATEST"; // utilizzato per forzare, in fase di anteprima della fattura, l'utilizzo dell'xslt dell'ultima versione in caso di errore nel recupero della versione 
		return versione;
	}
	
	@SuppressWarnings("unchecked")
	public static List<List<Object>> getAllegatiFatturaPA(Document fatturaPADocument) {
		@SuppressWarnings("rawtypes")
		List<List<Object>> dcwAttachmentsL = new ArrayList();
		List<Element> attachElsL = fatturaPADocument.selectNodes("//FatturaElettronicaBody/Allegati");
		for (Element attachEl: attachElsL) {
			String fileName = attachEl.elementText("NomeAttachment");
			
			// mbernardini 07/05/2015 : verificata la correttezza del nome file indicato in base all'analisi degli altri 
			// parametri riguardanti il file (formato e compressione)
			
			// non sempre il nome del file contiene anche l'estensione, in questo caso appendo la stringa specificata
			// come formato attachmente
			if (fileName.indexOf(".") == -1) {
				String formatoAttachment = attachEl.elementText("FormatoAttachment");
				if (formatoAttachment != null && formatoAttachment.length() > 0)
					fileName += "." + formatoAttachment.toLowerCase();
			}
			
			// con l'algoritmo di compressione si indica il vero formato del file allegato. NomeAttachment non contiene
			// la reale estensione del file
			String algoritmoCompressione = attachEl.elementText("AlgoritmoCompressione");
			if (algoritmoCompressione != null && algoritmoCompressione.length() > 0) {
				if (!fileName.toLowerCase().endsWith(algoritmoCompressione.toLowerCase()))
					fileName += "." + algoritmoCompressione.toLowerCase();
			}
			
			String base64content = attachEl.elementText("Attachment");
			byte[] fileContent = null;
			try {
				fileContent = Base64.decodeBase64(base64content);
			}
			catch (Exception e) {
				fileName = fileName + "base64error.txt";
				fileContent = base64content.getBytes();
			}
			
			//create DocwayFile
			@SuppressWarnings("rawtypes")
			List fileAttrsL = new ArrayList();
			fileAttrsL.add(fileName);
			fileAttrsL.add(new ByteArrayContentProvider(fileContent));
			dcwAttachmentsL.add(fileAttrsL);
		}
		return dcwAttachmentsL;
	}
	
	public static String getOggettoFatturaPA(Document fatturaPADocument, boolean attiva) {
		String oggetto = "";
		List<?> fatturaBody = fatturaPADocument.selectNodes("//FatturaElettronicaBody");
		if (fatturaBody.size() == 1) {
			Document singolaFattura = DocumentHelper.createDocument();
            singolaFattura.setRootElement(((Element) fatturaBody.get(0)).createCopy());
			String causale = extractCausaliFromFattura(singolaFattura);
			if (!causale.equals(""))
				return causale;
		}
		// in caso di oggetto non valorizzato (impossibile il recupero della causale) si procede con la generazione
		// di un oggetto custom
		if (oggetto.length() == 0)
			oggetto = produceOggettoFattura(fatturaPADocument, attiva);
		return oggetto;
	}
	
	public static String produceOggettoFattura(Document fatturaPADocument, boolean attiva) {
		String oggetto = "";
		
		// in base al tipo di fattura recupero la denominazione del mittente/destinatario
		String azienda = "";
		if (attiva)
			azienda = getCessionarioCommittente(fatturaPADocument);
		else
			azienda = getCedentePrestatore(fatturaPADocument);
		
		List<?> fatturaBody = fatturaPADocument.selectNodes("//FatturaElettronicaBody");
		if (fatturaBody.size() == 1) {
			// costruzione dell'oggetto in base ai dati della fattura
			String tipoDocumento = getDescrizioneTipoDocumento(fatturaPADocument);
			Node node = fatturaPADocument.selectSingleNode("//FatturaElettronicaBody/DatiGenerali/DatiGeneraliDocumento/Numero");
			String numFattura = (node == null)? "" : node.getText();
			node = fatturaPADocument.selectSingleNode("//FatturaElettronicaBody/DatiGenerali/DatiGeneraliDocumento/Data");
			String dataFattura = (node == null)? "" : node.getText();
			
			oggetto = tipoDocumento;
			if (!azienda.equals(""))
				oggetto += (attiva ? " per " : " di ") + azienda;
			oggetto += " n. " + numFattura;
			if (!dataFattura.equals("")) {
				// visualizzazione della data in formato italiano dd/mm/yyyy da yyyy-mm-dd
				oggetto += " del " + formatDataYYYYMMDD(dataFattura, "dd/MM/yyyy");
			}
		}
		else { // lotto di fatture (fatture multiple)
			oggetto = "Lotto di fatture" + (attiva ? " per " : " di ") + azienda + 	azienda + " ricevuto il " + new SimpleDateFormat("dd/MM/yyyy").format(new Date()); 
		}
		
		return oggetto;
	}
	
	private static String getDescrizioneTipoDocumento(Document fatturaPADocument) {
		Node node = fatturaPADocument.selectSingleNode("//FatturaElettronicaBody/DatiGenerali/DatiGeneraliDocumento/TipoDocumento");
		String value = (node == null)? "" : node.getText();
		if (!value.isEmpty() && tipiDocumento.containsKey(value)) {
			return (String)tipiDocumento.get(value);
		}
		return value;
	}
	
	private static String getCedentePrestatore(Document fatturaPADocument) {
		Node node = fatturaPADocument.selectSingleNode("//FatturaElettronicaHeader/CedentePrestatore/DatiAnagrafici/Anagrafica/Denominazione");
		String denominazione = (node == null)? "" : node.getText();
		if (denominazione.isEmpty()) { // tento il recupero di nome e cognome
			node = fatturaPADocument.selectSingleNode("//FatturaElettronicaHeader/CedentePrestatore/DatiAnagrafici/Anagrafica/Nome");
			if (node != null)
				denominazione = node.getText();
			node = fatturaPADocument.selectSingleNode("//FatturaElettronicaHeader/CedentePrestatore/DatiAnagrafici/Anagrafica/Cognome");
			if (node != null)
				denominazione += " " + node.getText();			
			denominazione = denominazione.trim();
		}
		return denominazione;
	}
	
	private static String getCessionarioCommittente(Document fatturaPADocument) {
		Node node = fatturaPADocument.selectSingleNode("//FatturaElettronicaHeader/CessionarioCommittente/DatiAnagrafici/Anagrafica/Denominazione");
		String denominazione = (node == null)? "" : node.getText();
		if (denominazione.isEmpty()) { // tento il recupero di nome e cognome
			node = fatturaPADocument.selectSingleNode("//FatturaElettronicaHeader/CessionarioCommittente/DatiAnagrafici/Anagrafica/Nome");
			if (node != null)
				denominazione = node.getText();
			node = fatturaPADocument.selectSingleNode("//FatturaElettronicaHeader/CessionarioCommittente/DatiAnagrafici/Anagrafica/Cognome");
			if (node != null)
				denominazione += " " + node.getText();			
			denominazione = denominazione.trim();
		}
		return denominazione;		
	}

	public static void appendDatiFatturaToDocument(Document fatturaPADocument, FatturaPAItem fatturaPAItem) {
		List<?> fatturaBody = fatturaPADocument.selectNodes("//FatturaElettronicaBody");
	    for (int i=0; i<fatturaBody.size(); i++) {
	    	Element element = (Element) fatturaBody.get(i);
            Document fattura = DocumentHelper.createDocument(element.createCopy());
            
            DatiFatturaContainer datiFattura = new DatiFatturaContainer();
            fatturaPAItem.addDatiFattura(datiFattura);
            
            addFtrDatiGeneraliDocumento(datiFattura, fattura);
            datiFattura.setDatiOrdineAcquisto(getFtrDati(fattura, "FatturaElettronicaBody/DatiGenerali/DatiOrdineAcquisto"));
            datiFattura.setDatiContratto(getFtrDati(fattura, "FatturaElettronicaBody/DatiGenerali/DatiContratto"));
            datiFattura.setDatiConvenzione(getFtrDati(fattura, "FatturaElettronicaBody/DatiGenerali/DatiConvenzione"));
            datiFattura.setDatiRicezione(getFtrDati(fattura, "FatturaElettronicaBody/DatiGenerali/DatiRicezione"));
            datiFattura.setDatiFattureCollegate(getFtrDati(fattura, "FatturaElettronicaBody/DatiGenerali/DatiFattureCollegate"));            
		    datiFattura.setRiferimentoFaseSAL(addFtrDatiSAL(fattura));
            datiFattura.setDatiDDT(addFtrDatiDDT(fattura));
		    datiFattura.setDatiBeniServizi(addFtrDatiBeniServizi(fattura));
		    
		    datiFattura.setDatiRegistroFatture(addFtrDatiRegistroFatture(fattura)); // aggiunta della sezione ralativa al registro delle fatture
	    }
	}
	
	private static void addFtrDatiGeneraliDocumento(DatiFatturaContainer datiFattura, Document xmlfattura) {
		Node node = xmlfattura.selectSingleNode("FatturaElettronicaBody/DatiGenerali/DatiGeneraliDocumento/TipoDocumento");
		datiFattura.setTipoDocumento_dg(node == null? "" : node.getText());
		
		node = xmlfattura.selectSingleNode("FatturaElettronicaBody/DatiGenerali/DatiGeneraliDocumento/Divisa");
		datiFattura.setDivisa_dg(node == null? "" : node.getText());

		node = xmlfattura.selectSingleNode("FatturaElettronicaBody/DatiGenerali/DatiGeneraliDocumento/Data");
		datiFattura.setData_dg(node == null? "" : formatDataYYYYMMDD(node.getText(), "yyyyMMdd"));

		node = xmlfattura.selectSingleNode("FatturaElettronicaBody/DatiGenerali/DatiGeneraliDocumento/Numero");
		datiFattura.setNumero_dg(node == null? "" : node.getText());

		node = xmlfattura.selectSingleNode("FatturaElettronicaBody/DatiGenerali/DatiGeneraliDocumento/ImportoTotaleDocumento");
		datiFattura.setImportoTotaleDocumento_dg(node == null? "" : node.getText());
		
		node = xmlfattura.selectSingleNode("FatturaElettronicaBody/DatiGenerali/DatiGeneraliDocumento/Arrotondamento");
		datiFattura.setArrotondamento_dg(node == null? "" : node.getText());
		
		node = xmlfattura.selectSingleNode("FatturaElettronicaBody/DatiGenerali/DatiGeneraliDocumento/Art73");
		datiFattura.setArt73_dg(node == null? "" : node.getText());
		
		// mbernardini 25/02/2015 : adeguamento alla ver. 1.1 di fatturePA (modificata la molteplicita' dell'elemento Causale)
		String causale = extractCausaliFromFattura(xmlfattura);
		if (!causale.isEmpty())
			datiFattura.setCausale_dg(causale);
	}
	
	private static String extractCausaliFromFattura(Document xmlfattura) {
		String causale = "";
		List<?> causali = xmlfattura.selectNodes("FatturaElettronicaBody/DatiGenerali/DatiGeneraliDocumento/Causale");
		if (causali != null && causali.size() > 0) {
			for (int i=0; i<causali.size(); i++) {
				Element elcausale = (Element) causali.get(i);
				if (elcausale != null && elcausale.getTextTrim() != null && elcausale.getTextTrim().length() > 0)
					causale += elcausale.getTextTrim() + " ";
			}
			
			if (causale.length() > 1)
				causale = causale.substring(0, causale.length()-1);
		}
		
		return causale;
	}

	private static List<DatiFatturaPAItem> getFtrDati(Document xmlfattura, String xpath) {
		List<DatiFatturaPAItem> itemsL = new ArrayList<>();
		List<?> nodes = xmlfattura.selectNodes(xpath);
		if (nodes != null && nodes.size() > 0) {
			for (int i=0; i<nodes.size(); i++) {
				Element dati = (Element) nodes.get(i);
				DatiFatturaPAItem item = new DatiFatturaPAItem();
				itemsL.add(item);
				
				if (dati.elementText("RiferimentoNumeroLinea") != null)
					item.setRiferimentoNumeroLinea(dati.elementText("RiferimentoNumeroLinea"));
				if (dati.elementText("IdDocumento") != null)
					item.setIdDocumento(dati.elementText("IdDocumento"));
				if (dati.elementText("Data") != null)
					item.setData(formatDataYYYYMMDD(dati.elementText("Data"), "yyyyMMdd"));
				if (dati.elementText("NumItem") != null)
					item.setNumItem(dati.elementText("NumItem"));
				if (dati.elementText("CodiceCommessaConvenzione") != null)
					item.setCodiceCommessaConvenzione(dati.elementText("CodiceCommessaConvenzione"));
				if (dati.elementText("CodiceCUP") != null)
					item.setCodiceCUP(dati.elementText("CodiceCUP"));
				if (dati.elementText("CodiceCIG") != null)
					item.setCodiceCIG(dati.elementText("CodiceCIG"));
			}
		}
		return itemsL;
	}
	
	private static List<String> addFtrDatiSAL(Document xmlfattura) {
		List<String> datiSALL = new ArrayList<>();
		List<?> nodes = xmlfattura.selectNodes("FatturaElettronicaBody/DatiGenerali/DatiSAL");
		if (nodes != null && nodes.size() > 0) {
			for (int i=0; i<nodes.size(); i++) {
				Element datiSAL = (Element) nodes.get(i);
				datiSALL.add(datiSAL.elementText("RiferimentoFase"));
			}
		}
		return datiSALL;
	}
	
	private static List<DatiDDTItem> addFtrDatiDDT(Document xmlfattura) {
		List<DatiDDTItem> datiDDTL = new ArrayList<>();
		
		List<?> nodes = xmlfattura.selectNodes("FatturaElettronicaBody/DatiGenerali/DatiDDT");
		if (nodes != null && nodes.size() > 0) {
			for (int i=0; i<nodes.size(); i++) {
				DatiDDTItem item = new DatiDDTItem();
				datiDDTL.add(item);
				
				Element datiDDT = (Element) nodes.get(i);
				
				item.setNumero(datiDDT.elementText("NumeroDDT"));
				item.setData(formatDataYYYYMMDD(datiDDT.elementText("DataDDT"), "yyyyMMdd"));
				
				// NB. non gestito il RiferimentoNumeroLinea perche' fa riferimento ad un altra sezione 
				//     della fattura che non gestiamo (2.2.1.1 - DatiBeniServizi > NumeroLinea) 
			}
		}
		return datiDDTL;
	}

	private static DatiBeniServiziItem addFtrDatiBeniServizi(Document xmlfattura) {
		DatiBeniServiziItem datiBeniServiziItem = new DatiBeniServiziItem();
		
		List<?> nodes = xmlfattura.selectNodes("FatturaElettronicaBody/DatiBeniServizi/DettaglioLinee");
		if (nodes != null && nodes.size() > 0) {
			for (int i=0; i<nodes.size(); i++) {
				DatiLineaItem lineaItem = new DatiLineaItem();
				datiBeniServiziItem.addLinea(lineaItem);

				Element datiLinea = (Element) nodes.get(i);
				lineaItem .setDescrizione(datiLinea.elementText("Descrizione"));
				lineaItem.setPrezzoTotale(datiLinea.elementText("PrezzoTotale"));
			}
		}
		
		nodes = xmlfattura.selectNodes("FatturaElettronicaBody/DatiBeniServizi/DatiRiepilogo");
		if (nodes != null && nodes.size() > 0) {
			for (int i=0; i<nodes.size(); i++) {
				DatiRiepilogoItem riepilogoItem = new DatiRiepilogoItem();
				datiBeniServiziItem.addRiepilogo(riepilogoItem);
				
				Element datiRiepilogo = (Element) nodes.get(i);
				riepilogoItem.setAliquotaIVA(datiRiepilogo.elementText("AliquotaIVA"));
				riepilogoItem.setImponibileImporto(datiRiepilogo.elementText("ImponibileImporto"));
				riepilogoItem.setImposta(datiRiepilogo.elementText("Imposta"));
			}
		}
		
		return datiBeniServiziItem;
	}
	
	private static DatiRegistroFattureItem addFtrDatiRegistroFatture(Document xmlfattura) {
		DatiRegistroFattureItem datiRegistroFattureItem = new DatiRegistroFattureItem();
		
		//el.addAttribute("progrReg", "."); //occorre gestirlo come progressivo oppure puo' essere il num repertorio?
		Node node = xmlfattura.selectSingleNode("FatturaElettronicaBody/DatiGenerali/DatiGeneraliDocumento/Numero");
		datiRegistroFattureItem.setNumeroFattura(node == null? "" : node.getText());
		
		node = xmlfattura.selectSingleNode("FatturaElettronicaBody/DatiGenerali/DatiGeneraliDocumento/Data");
		datiRegistroFattureItem.setDataEmissioneFattura(node == null? "" : formatDataYYYYMMDD(node.getText(), "yyyyMMdd"));
		
		// mbernardini 03/03/2015 : adeguamento alla ver. 1.1 di fatturePA (modificata la molteplicita' dell'elemento Causale)
		String oggettoFornitura = extractCausaliFromFattura(xmlfattura);
		//String oggettoFornitura = xmlfattura.getElementText("FatturaElettronicaBody/DatiGenerali/DatiGeneraliDocumento/Causale", "");
		if (oggettoFornitura.equals("")) {
			// recupero l'oggetto della fornitura dalle linee della fattura
			List<?> lineefattura = xmlfattura.selectNodes("FatturaElettronicaBody/DatiBeniServizi/DettaglioLinee/Descrizione");
			if (lineefattura != null && lineefattura.size() > 0) {
				for (int i=0; i<lineefattura.size(); i++) {
					Node descrEl = (Node) lineefattura.get(i);
					if (descrEl != null && descrEl.getText() != null && descrEl.getText().length() > 0)
						oggettoFornitura += descrEl.getText() + "; ";
				}
				if (oggettoFornitura.endsWith("; "))
					oggettoFornitura = oggettoFornitura.substring(0, oggettoFornitura.length()-2);
			}
		}
		
		datiRegistroFattureItem.setOggettoFornitura(oggettoFornitura);
		
		try {
			node = xmlfattura.selectSingleNode("FatturaElettronicaBody/DatiGenerali/DatiGeneraliDocumento/ImportoTotaleDocumento");
			double importoTotale = new Double(node == null? "0,0": node.getText()).doubleValue();
			if (importoTotale == 0) {
				// calcolo dell'importo totale tramite la somma dei riepiloghi
				List<?> lineeriepilogo = xmlfattura.selectNodes("FatturaElettronicaBody/DatiBeniServizi/DatiRiepilogo");
				if (lineeriepilogo != null && lineeriepilogo.size() > 0) {
					for (int i=0; i<lineeriepilogo.size(); i++) {
						Element riepilogoEl = (Element) lineeriepilogo.get(i);
						if (riepilogoEl != null 
												&& riepilogoEl.elementText("ImponibileImporto") != null && riepilogoEl.elementText("ImponibileImporto").length() > 0
												&& riepilogoEl.elementText("Imposta") != null && riepilogoEl.elementText("Imposta").length() > 0)
							importoTotale = importoTotale + new Double(riepilogoEl.elementText("ImponibileImporto")).doubleValue() + new Double(riepilogoEl.elementText("Imposta")).doubleValue();
					}
				}
			}
			datiRegistroFattureItem.setImportoTotale(formatImporto(importoTotale));
		}
		catch (Exception e) {
			//log.error("Fattura.addFtrDatiRegistroFatture(): error in importoTotale: " + e.getMessage());
		}
		
		// calcolo della data di scadenza della fattura (eventuale data dell'ultimo pagamento)
		try {
			String dataScadenzaFattura = "";
			
			// recupero dei termini di pagamento
			List<?> dettaglipagamento = xmlfattura.selectNodes("FatturaElettronicaBody/DatiPagamento/DettaglioPagamento");
			if (dettaglipagamento != null && dettaglipagamento.size() > 0) {
				Date dateScadFattura = null; 
				for (int i=0; i<dettaglipagamento.size(); i++) {
					Element pagamentoEl = (Element) dettaglipagamento.get(i);
					if (pagamentoEl != null 
							&& ((pagamentoEl.elementText("DataScadenzaPagamento") != null && pagamentoEl.elementText("DataScadenzaPagamento").length() > 0 ) || (pagamentoEl.elementText("DataRiferimentoTerminiPagamento") != null && pagamentoEl.elementText("DataRiferimentoTerminiPagamento").length() > 0 ))) {
						// individuata una scadenza di pagamento...
						if (pagamentoEl.elementText("DataScadenzaPagamento") != null && pagamentoEl.elementText("DataScadenzaPagamento").length() > 0 ) {
							// valorizzato il campo dataScadenzaPagamento
							dateScadFattura = calcDataScadenzaFattura(dateScadFattura, new SimpleDateFormat("yyyy-MM-dd").parse(pagamentoEl.elementText("DataScadenzaPagamento")));
						}
						else {
							// valorizzato il campo dataRiferimentoTerminiPagamento
							Date dataRiferimentoTerminiPagamento = new SimpleDateFormat("yyyy-MM-dd").parse(pagamentoEl.elementText("DataRiferimentoTerminiPagamento"));
							int giorniTerminiPagamento = new Integer(pagamentoEl.elementText("GiorniTerminiPagamento")).intValue();
							
							Calendar c = Calendar.getInstance();
							c.setTime(dataRiferimentoTerminiPagamento);
							c.add(Calendar.DAY_OF_MONTH, giorniTerminiPagamento);
							
							dateScadFattura = calcDataScadenzaFattura(dateScadFattura, c.getTime());
						}
					}
				}
				if (dateScadFattura != null)
					dataScadenzaFattura = formatDataYYYYMMDD(new SimpleDateFormat("yyyy-MM-dd").format(dateScadFattura), "yyyyMMdd");
			}
			
			datiRegistroFattureItem.setDataScadenzaFattura(dataScadenzaFattura);
		}
		catch (Exception e) {
			//log.error("Fattura.addFtrDatiRegistroFatture(): error in dataScadenzaFattura: " + e.getMessage());
		}
		
		// parametri da inserire da interfaccia da parte dell'operatore
		//el.addElement("estremiImpegno", "");
		//el.addAttribute("finiIVA", "");
		
		// recupero dei codici CIG e CUP
		try {
			// recupero dei codici CIG
			List<?> codiciCIG = xmlfattura.selectNodes("FatturaElettronicaBody//CodiceCIG");
			if (codiciCIG != null && codiciCIG.size() > 0) {
				String cig = "";
				for (int i=0; i<codiciCIG.size(); i++) {
					Element codiceCigEl = (Element) codiciCIG.get(i);
					if (codiceCigEl != null && codiceCigEl.getText() != null && codiceCigEl.getText().length() > 0) {
						if (!cig.contains(codiceCigEl.getText() + ","))
							cig += codiceCigEl.getText() + ",";
					}
				}
				if (cig != null && cig.length() > 0) {
					if (cig.endsWith(","))
						cig = cig.substring(0, cig.length()-1);
					datiRegistroFattureItem.setCig(cig);
				}
			}
		}
		catch (Exception e) {
			//log.error("Fattura.addFtrDatiRegistroFatture(): error in cig: " + e.getMessage());
		}
		try {
			// recupero dei codici CUP
			List<?> codiciCUP = xmlfattura.selectNodes("FatturaElettronicaBody//CodiceCUP");
			if (codiciCUP != null && codiciCUP.size() > 0) {
				String cup = "";
				for (int i=0; i<codiciCUP.size(); i++) {
					Element codiceCupEl = (Element) codiciCUP.get(i);
					if (codiceCupEl != null && codiceCupEl.getText() != null && codiceCupEl.getText().length() > 0) {
						if (!cup.contains(codiceCupEl.getText() + ","))
							cup += codiceCupEl.getText() + ",";
					}
				}
				if (cup != null && cup.length() > 0) {
					if (cup.endsWith(","))
						cup = cup.substring(0, cup.length()-1);
					datiRegistroFattureItem.setCup(cup);
				}
			}
		}
		catch (Exception e) {
			//log.error("Fattura.addFtrDatiRegistroFatture(): error in cup: " + e.getMessage());
		}
		
		return datiRegistroFattureItem;
	}
	
	private static Date calcDataScadenzaFattura(Date currentDate, Date newDate) {
		if (newDate != null) {
			if (currentDate == null) {
				return newDate;
			}
			else {
				if (newDate.getTime() - currentDate.getTime() > 0)
					return newDate;
				else
					return currentDate;
			}
		}
		
		return currentDate;
	}
	
	public static void appendDatiFileMetadatiToDocument(Document fileMetadatiDocument, FatturaPAItem fatturaPAItem) {
		Node node = fileMetadatiDocument.selectSingleNode("//IdentificativoSdI");
		fatturaPAItem.setIdentificativoSdI(node == null? "" : node.getText());
		
		node = fileMetadatiDocument.selectSingleNode("//NomeFile");
		String nomeFile = (node == null)? "" : node.getText();
		String fileNameFattura = nomeFile;
		String extensionFattura = "";
		int index = nomeFile.indexOf(".");
		if (index != -1) {
			fileNameFattura = nomeFile.substring(0, index);
			extensionFattura = nomeFile.substring(index+1);
		}
		fatturaPAItem.setFileNameFattura(fileNameFattura);
		fatturaPAItem.setExtensionFattura(extensionFattura);
		
		node = fileMetadatiDocument.selectSingleNode("//CodiceDestinatario");
		fatturaPAItem.setCodiceDestinatario(node == null? "" : node.getText());

		node = fileMetadatiDocument.selectSingleNode("//Formato");
		fatturaPAItem.setFormato(node == null? "" : node.getText());

		node = fileMetadatiDocument.selectSingleNode("//TentativiInvio");
		fatturaPAItem.setTentativiInvio(node == null? "" : node.getText());		

		node = fileMetadatiDocument.selectSingleNode("//MessageId");
		fatturaPAItem.setMessageId(node == null? "" : node.getText());
		
		node = fileMetadatiDocument.selectSingleNode("//Note");
		String note = (node == null)? "" : node.getText();
		if (!note.isEmpty())
			fatturaPAItem.setNote(note);   
	}
	
	
	
	
	
	/*

	public void assegnaMittente(Connessione connessione, Document document, String codamm, String codaoo, String dbacl, String xwuser) throws Exception {
		if (document.getRootElement().attributeValue("tipo", "").equalsIgnoreCase("arrivo"))
			assegnaRifEsterno("CedentePrestatore", connessione, document, codamm, codaoo, dbacl, xwuser);
		else
			throw new Exception("Impossibile assegnare il mittente: Il documento corrente non è di tipo 'arrivo'");
	}
	

	public void assegnaDestonatario(Connessione connessione, Document document, String codamm, String codaoo, String dbacl, String xwuser) throws Exception {
		if (document.getRootElement().attributeValue("tipo", "").equalsIgnoreCase("partenza"))
			assegnaRifEsterno("CessionarioCommittente", connessione, document, codamm, codaoo, dbacl, xwuser);
		else
			throw new Exception("Impossibile assegnare il destinatario: Il documento corrente non è di tipo 'partenza'");
	}

	private void assegnaRifEsterno(String rifElemNameInFatturaPA, Connessione connessione, Document document, String codamm, String codaoo, String dbacl, String xwuser) throws Exception {
		log.info("Fattura.assegnaRifEsterno(): tipologia di rif esterno da gestire: " + rifElemNameInFatturaPA);
		
		if (xmldocFattura != null) {
			String piva = xmldocFattura.getElementText("//FatturaElettronicaHeader/" + rifElemNameInFatturaPA + "/DatiAnagrafici/IdFiscaleIVA/IdCodice", "");
			String cf = xmldocFattura.getElementText("//FatturaElettronicaHeader/" + rifElemNameInFatturaPA + "/DatiAnagrafici/CodiceFiscale", "");
			
			try {
				connessione.connect(dbacl);
				Selezione selezione = new Selezione(connessione);
				
				log.info("Fattura.assegnaRifEsterno(): connected to " + dbacl);
				if (xwuser != null && xwuser.length() > 0) {
					log.info("Fattura.assegnaRifEsterno(): notifying user " + xwuser);
		            connessione.notifyUser(xwuser);
				}
				
				boolean trovato = false;
				
				Element rif = null;
				
				if (!piva.equals("")) {
					// ricerca in anagrafica su campo partita iva
					String query = "[/struttura_esterna/@partita_iva/]=\"" + piva + "\" AND [/struttura_esterna/#cod_ammaoo/]=\"" + codamm + codaoo + "\"";
					log.info("Fattura.assegnaRifEsterno(): ricerca su piva: " + query);
					int count = selezione.Search(query, null, "", 0, 0);
					if (count == 1) { // e' stata individuata una struttura esterna con la partita iva indicata
						trovato = true;
						log.info("\t " + count + " piva found");
						
						Documento documento = new Documento(connessione, selezione);
						rif = getRifEsternoFromAcl(DocumentHelper.parseText(documento.getDoc(0).XML()));
					}
					
				}
				if (!trovato && !cf.equals("")) {
					// ricerca in anagrafica su campo codice fiscale
					String query = "([/struttura_esterna/@codice_fiscale/]=\"" + cf + "\" AND [/struttura_esterna/#cod_ammaoo/]=\"" + codamm + codaoo + "\") OR ([/persona_esterna/@codice_fiscale/]=\"" + cf + "\" AND [/persona_esterna/#cod_ammaoo/]=\"" + codamm + codaoo + "\")";
					log.info("Fattura.assegnaRifEsterno(): ricerca su cf: " + query);
					int count = selezione.Search(query, null, "", 0, 0);
					if (count == 1) { // e' stata individuata una struttura esterna con la partita iva indicata
						trovato = true;
						log.info("\t " + count + " cf found");
						
						Documento documento = new Documento(connessione, selezione);
						rif = getRifEsternoFromAcl(DocumentHelper.parseText(documento.getDoc(0).XML()));
					}
				}
				
				if (!trovato) {
					// inserimento nuova struttura/persona esterna:
					// almento uno fra denominazione e nome/cognome deve essere valorizzato
					
					Document aclDocument = DocumentHelper.createDocument();
					
					String rootElementName = "persona_esterna";
					String denominazione = xmldocFattura.getElementText("//FatturaElettronicaHeader/" + rifElemNameInFatturaPA + "/DatiAnagrafici/Anagrafica/Denominazione", "");
					if (!denominazione.equals(""))
						rootElementName = "struttura_esterna";
						
					Element root = aclDocument.addElement(rootElementName);
					root.addAttribute("nrecord", ".");
					
					root.addAttribute("cod_amm", codamm);
					root.addAttribute("cod_aoo", codaoo);
					
					String indirizzo = xmldocFattura.getElementText("//FatturaElettronicaHeader/" + rifElemNameInFatturaPA + "/Sede/Indirizzo", "");
					indirizzo = indirizzo + " " + xmldocFattura.getElementText("//FatturaElettronicaHeader/" + rifElemNameInFatturaPA + "/Sede/NumeroCivico", "");
					String cap = xmldocFattura.getElementText("//FatturaElettronicaHeader/" + rifElemNameInFatturaPA + "/Sede/CAP", "");
					String comune = xmldocFattura.getElementText("//FatturaElettronicaHeader/" + rifElemNameInFatturaPA + "/Sede/Comune", "");
					String provincia = xmldocFattura.getElementText("//FatturaElettronicaHeader/" + rifElemNameInFatturaPA + "/Sede/Provincia", "");
					String nazione = xmldocFattura.getElementText("//FatturaElettronicaHeader/" + rifElemNameInFatturaPA + "/Sede/Nazione", "");
					
					String email = xmldocFattura.getElementText("//FatturaElettronicaHeader/" + rifElemNameInFatturaPA + "/Contatti/Email", "");
					String fax = xmldocFattura.getElementText("//FatturaElettronicaHeader/" + rifElemNameInFatturaPA + "/Contatti/Fax", "");
					
					if (rootElementName.equals("struttura_esterna")) {
						root.addAttribute("cod_uff", ".");
						
						Element nomeEl = root.addElement("nome");
						nomeEl.addText(denominazione);
						
						root.addAttribute("partita_iva", piva);
						root.addAttribute("codice_fiscale", cf);
						
						// gestione del recapito (sede azienda)
						Element elindirizzo = root.addElement("indirizzo");
						elindirizzo.addText(indirizzo);
						elindirizzo.addAttribute("cap", cap);
						elindirizzo.addAttribute("comune", comune);
						elindirizzo.addAttribute("prov", provincia);
						elindirizzo.addAttribute("nazione", nazione);
						
						if (!email.equals("")) {
							Element elemail = root.addElement("email");
							elemail.addAttribute("addr", email);
						}
						if (!fax.equals("")) {
							Element eltelefono = root.addElement("telefono");
							eltelefono.addAttribute("tipo", "fax");
							eltelefono.addAttribute("num", fax);
						}
					}
					else {
						root.addAttribute("matricola", ".");
						
						root.addAttribute("nome", xmldocFattura.getElementText("//FatturaElettronicaHeader/" + rifElemNameInFatturaPA + "/DatiAnagrafici/Anagrafica/Nome", ""));
						root.addAttribute("cognome", xmldocFattura.getElementText("//FatturaElettronicaHeader/" + rifElemNameInFatturaPA + "/DatiAnagrafici/Anagrafica/Cognome", ""));
						
						root.addAttribute("codice_fiscale", cf);
						
						// gestione del recapito (recapito attivita')
						Element recapito = root.addElement("recapito");
						Element elindirizzo = recapito.addElement("indirizzo");
						elindirizzo.addText(indirizzo);
						elindirizzo.addAttribute("cap", cap);
						elindirizzo.addAttribute("comune", comune);
						elindirizzo.addAttribute("prov", provincia);
						elindirizzo.addAttribute("nazione", nazione);
						
						if (!email.equals("")) {
							Element elemail = recapito.addElement("email");
							elemail.addAttribute("addr", email);
						}
						if (!fax.equals("")) {
							Element eltelefono = recapito.addElement("telefono");
							eltelefono.addAttribute("tipo", "fax");
							eltelefono.addAttribute("num", fax);
						}
					}
					
					String xml = XmlReplacer.unmountXML(aclDocument, ""); // per mantenere encoding iso-8859-1
					
					// salavataggio del documento costruito in ACL...
					it.highwaytech.broker.XMLCommand theCommand = new it.highwaytech.broker.XMLCommand(
		                    it.highwaytech.broker.XMLCommand.SaveDocument, it.highwaytech.broker.XMLCommand.SaveDocument_Save,
		                    0, xml, rootElementName, "", null);
		            String result = connessione.getBroker().XMLCommand(connessione.getConnection(), connessione.getDb(), theCommand.toString());
					
		            Vector<?> v = XmlReplacer.getMultistAttrOrElm(result, "dtl/@dval", "dtype=\"ndoc\"", true);
		            int physDoc = Integer.parseInt((String)v.get(0));
		            XmlDoc xmlDoc = new XmlDoc(null, -1, physDoc, connessione);
		            xmlDoc.load();
	
					// e costruzione del rif_esterno per l'assegnazione del mittente del documento
		            rif = getRifEsternoFromAcl(xmlDoc.getDocument().getDocument());
				}
				
				if (rif != null) {
					// sostituzione del rifEsterno nel documento della fatturaPA
					List<?> l = document.selectNodes("/doc/rif_esterni/rif");
					for (int i = 0; i < l.size(); i++) ((Node) l.get(i)).detach();
					
					Element el = (Element) document.selectSingleNode("/doc/rif_esterni");
					if (el == null) // se l'elemento rif_esterni non esiste occorre crearlo..
						el = document.getRootElement().addElement("rif_esterni"); 
					el.add(rif);
				}
			}
			catch (Exception e) {
				log.error("Fattura.assegnaRifEsterno(): " + e.getMessage(), e);
				throw e;
			}
			finally {
				// restore della connection..
				connessione.restoreConnect();
			}
		}
	}
	

	@SuppressWarnings("unchecked")
	private Element getRifEsternoFromAcl(Document doc) {
		Element rif = null;
		
		if (doc != null) {
			String pne = doc.getRootElement().getQualifiedName();
			rif = DocumentHelper.createElement("rif");
			
			Element el = rif.addElement("nome");
			if (pne.equals("struttura_esterna")) {
				el.addText(((Element) doc.selectSingleNode("struttura_esterna/nome")).getTextTrim());
				el.addAttribute("cod", ((Attribute) doc.selectSingleNode("struttura_esterna/@cod_uff")).getValue());
			}
			else { // pne = persona_esterna
				el.addText(((Attribute) doc.selectSingleNode("persona_esterna/@cognome")).getValue() + " " + ((Attribute) doc.selectSingleNode("persona_esterna/@nome")).getValue());
				el.addAttribute("cod", ((Attribute) doc.selectSingleNode("persona_esterna/@matricola")).getValue());
			}
			
			if (doc.selectSingleNode(pne + "/@partita_iva") != null)
				rif.addAttribute("partita_iva", ((Attribute) doc.selectSingleNode(pne + "/@partita_iva")).getValue());
			if (doc.selectSingleNode(pne + "/@codice_fiscale") != null)
				rif.addAttribute("codice_fiscale", ((Attribute) doc.selectSingleNode(pne + "/@codice_fiscale")).getValue());
			
			String elementoRecapito = pne.equals("struttura_esterna") ? "" : "/recapito";
			
			// costruzione dell'indirizzo
			String indirizzo = "";
			
			if (doc.selectSingleNode(pne + elementoRecapito + "/indirizzo") != null)
				indirizzo = ((Element) doc.selectSingleNode(pne + elementoRecapito + "/indirizzo")).getTextTrim();
			if (doc.selectSingleNode(pne + elementoRecapito + "/indirizzo/@cap") != null)
				indirizzo = indirizzo + " - " + ((Attribute) doc.selectSingleNode(pne + elementoRecapito + "/indirizzo/@cap")).getValue();
			if (doc.selectSingleNode(pne + elementoRecapito + "/indirizzo/@comune") != null)
				indirizzo = indirizzo + " " + ((Attribute) doc.selectSingleNode(pne + elementoRecapito + "/indirizzo/@comune")).getValue();
			if (doc.selectSingleNode(pne + elementoRecapito + "/indirizzo/@prov") != null)
				indirizzo = indirizzo + " (" + ((Attribute) doc.selectSingleNode(pne + elementoRecapito + "/indirizzo/@prov")).getValue() + ")";
			if (doc.selectSingleNode(pne + elementoRecapito + "/indirizzo/@nazione") != null)
				indirizzo = indirizzo + " - " + ((Attribute) doc.selectSingleNode(pne + elementoRecapito + "/indirizzo/@nazione")).getValue();
			
			String email = "";
			List<Attribute> emails = doc.selectNodes(pne + elementoRecapito + "/email/@addr");
			if (emails != null && emails.size() > 0) {
				for (int i=0; i<emails.size(); i++) {
					if (emails.get(i) != null && emails.get(i).getValue() != null && !emails.get(i).getValue().equals(""))
						email = email + emails.get(i).getValue() + ";";
				}
				
				if (email.length() > 0)
					email = email.substring(0, email.length()-1); // eliminazione dell'ultimo ;
			}
			
			String fax = "";
			if (doc.selectSingleNode(pne + elementoRecapito + "/telefono[@tipo = 'fax']") != null)
				fax = ((Element) doc.selectSingleNode(pne + elementoRecapito + "/telefono[@tipo = 'fax']")).getTextTrim();
			
			if (!indirizzo.equals("") || !email.equals("") || !fax.equals("")) {
				el = rif.addElement("indirizzo");
				if (!indirizzo.equals(""))
					el.addText(indirizzo);
				if (!email.equals(""))
					el.addAttribute("email", email);
				if (!fax.equals(""))
					el.addAttribute("fax", fax);
			}
			
		}
		
		return rif;
	}
	*/

	private static String formatDataYYYYMMDD(String data, String formato) {
		if (data != null && data.length() > 0) {
			try {
				if (formato == null || formato.equals(""))
					formato = "yyyyMMdd";
				Date date = new SimpleDateFormat("yyyy-MM-dd").parse(data);
				if (date != null) 
					data = new SimpleDateFormat(formato).format(date);
			}
			catch (Exception ex) {
				//do nothing
			}
		}
		return data;
	}
	
	private static String formatImporto(double value) {
		try {
			DecimalFormat df = new DecimalFormat("#.00", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
			return df.format(value);
		}
		catch (Exception ex) {
			return value + "";
		}
	}
	
}
