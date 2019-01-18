package it.tredi.msa.mailboxmanager.docway.fatturapa;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import it.tredi.msa.mailboxmanager.docway.fatturapa.conf.OggettoDocumentoBuilder;
import it.tredi.msa.mailboxmanager.docway.fatturapa.conf.OggettoParseMode;
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
	public static final String TIPO_MESSAGGIO_AT = "AT"; // Attestazione di avvenuta trasmissione della fattura con impossibilita' di recapito

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

	/**
	 * Ritorna l'oggetto della fattura recuperandolo dal file XML.
	 * Il campo oggetto del documento deve essere valorizzato con la causale della fattura. In caso di causale vuota
	 * valorizzare il campo nel modo seguente:
	 * [TIPO (Fattura, ec..)] di [PRESTATORE] n. [NUM_FATTURA] del [DATA_FATTURA]
	 *
	 * @param fatturaPADocument Documento XML della fattura.
	 * @param attiva se true genera l'oggetto per la fattura attiva, altrimenti estrapola i dati per la fattura passiva
	 * @param oggettoDocumentoBuilder modalita' di generazione dell'oggetto del documento
	 * @return
	 */
	public static String getOggettoFatturaPA(Document fatturaPADocument, boolean attiva, OggettoDocumentoBuilder oggettoDocumentoBuilder) {
		String oggetto = "";
		if (fatturaPADocument != null) {
			// Lettura della causale solo nel caso in cui si richieda il parseMode CAUSALE o CUSTOM
			String causale = "";
			if (oggettoDocumentoBuilder.getParseMode() == OggettoParseMode.CAUSALE || oggettoDocumentoBuilder.getParseMode() == OggettoParseMode.CUSTOM) {
				List<?> fatturaBody = fatturaPADocument.selectNodes("//FatturaElettronicaBody");
				if (fatturaBody.size() == 1) {
					Document singolaFattura = DocumentHelper.createDocument();
					if (fatturaBody.get(0) != null)
						singolaFattura.setRootElement(((Element) fatturaBody.get(0)).createCopy());
					causale = extractCausaliFromFattura(singolaFattura);
				}
				if (oggettoDocumentoBuilder.getParseMode() == OggettoParseMode.CAUSALE)
					oggetto = causale;
				else // CUSTOM
					oggetto = produceOggettoByCustom(fatturaPADocument, attiva, oggettoDocumentoBuilder.getTemplate(), causale);
			}
			// L'oggetto viene generato in modalita' PREFEDINITA se richiesto o se risulta impossibile produrlo
			// attraverso le altre
			if (oggetto.isEmpty() || oggettoDocumentoBuilder.getParseMode() == OggettoParseMode.PREDEFINITO)
				oggetto = produceOggettoFattura(fatturaPADocument, attiva);
		}
		return oggetto;
	}

	/**
	 * Definizione dell'oggetto del documento che contiene la fattura in base all'applicazione di un formato custom
	 * passato
	 * @param fatturaPADocument documento XML della fattura.
	 * @param attiva se true genera l'oggetto per la fattura attiva, altrimenti estrapola i dati per la fattura passiva.
	 * @param formatoOggetto formato da utilizzare per la costruzione dell'oggetto in caso di parseMode = CUSTOM.
	 * @param causale causale estratta dalla fattura.
	 * @return
	 */
	private static String produceOggettoByCustom(Document fatturaPADocument, boolean attiva, String formatoOggetto, String causale) {
		String oggetto = "", azienda = "";
		// in base al tipo di fattura recupero la denominazione del mittente/destinatario
		if (attiva) azienda = getCessionarioCommittente(fatturaPADocument);
		else azienda = getCedentePrestatore(fatturaPADocument);
		if (formatoOggetto != null && fatturaPADocument != null) {
			List<?> fatturaBody = fatturaPADocument.selectNodes("//FatturaElettronicaBody");
			if (fatturaBody.size() == 1) { // fattura singola
				// estraggo i dati necessari dal template
				oggetto = formatoOggetto;
				// sostituzione delle parole in base al flag attiva passiva {attiva|passiva}
				String regex = "\\{[^\\}]+\\}"; // matcha tutte le parti di una string da { a } (altrimenti va dal primo { all'ultimo }
				Pattern pattern = Pattern.compile(regex);
				Matcher matcher = pattern.matcher(oggetto);
				List<String> toSubstitute = new ArrayList<String>();
				while (matcher.find()) {
					toSubstitute.add(matcher.group());
				}
				for (String s : toSubstitute) {
					if (attiva) {
						String sub = s.substring(1, s.indexOf("|"));
						oggetto = oggetto.replace(s, sub);
					} else {
						String sub = s.substring(s.indexOf("|")+1, s.length()-1);
						oggetto = oggetto.replace(s, sub);
					}
				}
				if (oggetto.contains("[AZIENDA]")) {
					oggetto = oggetto.replace("[AZIENDA]", azienda);
				}
				if (oggetto.contains("[TIPODOC]")) {
					String tipoDocumento = getDescrizioneTipoDocumento(fatturaPADocument);
					oggetto = oggetto.replace("[TIPODOC]", tipoDocumento);
				}
				if (oggetto.contains("[NUMFATTURA]")) {
					String numFattura = "";
					Element numFatturaElem = (Element) fatturaPADocument.selectSingleNode("//FatturaElettronicaBody/DatiGenerali/DatiGeneraliDocumento/Numero");
					if (numFatturaElem != null && numFatturaElem.getTextTrim() != null)
						numFattura = numFatturaElem.getTextTrim();
					oggetto = oggetto.replace("[NUMFATTURA]", numFattura);
				}
				if (oggetto.contains("[DATAFATTURA]")) {
					String dataFattura = "";
					Element dataFatturaElem = (Element) fatturaPADocument.selectSingleNode("//FatturaElettronicaBody/DatiGenerali/DatiGeneraliDocumento/Data");
					if (dataFatturaElem != null && dataFatturaElem.getTextTrim() != null)
						dataFattura = dataFatturaElem.getTextTrim();
					oggetto = oggetto.replace("[DATAFATTURA]", formatDataYYYYMMDD(dataFattura, "dd/MM/yyyy"));
				}
				if (oggetto.contains("[CAUSALE]")) {
					oggetto = oggetto.replace("[CAUSALE]", (causale.isEmpty() || oggetto.startsWith("[CAUSALE]")) ? causale : " - " + causale);
				}
			}
			else { // lotto di fatture (fatture multiple)
				oggetto = "Lotto di fatture" + (attiva ? " per " : " di ") + azienda + " ricevuto il " + new SimpleDateFormat("dd/MM/yyyy").format(new Date());
			}
		}
		return oggetto;
	}

	/**
	 * Definisce l'oggetto della fattura recuperando i dati dal file XML. L'oggetto generato ha il seguente formato:
	 * [TIPO (Fattura, ec..)] di [PRESTATORE] n. [NUM_FATTURA] del [DATA_FATTURA]
	 *
	 * @param fatturaPADocument documento XML della fattura.
	 * @param attiva se true genera l'oggetto per la fattura attiva, altrimenti estrapola i dati per la fattura passiva
	 * @return
	 */
	private static String produceOggettoFattura(Document fatturaPADocument, boolean attiva) {
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
			oggetto = "Lotto di fatture" + (attiva ? " per " : " di ") + azienda + " ricevuto il " + new SimpleDateFormat("dd/MM/yyyy").format(new Date());
		}
		
		return oggetto;
	}

	/**
	 * Ritorna la descrizione in base al valore TipoDocumento estratto:
	 * TD03 = Acconto/Anticipo su parcella
	 * @param fatturaPADocument documento XML della fattura
	 * @return
	 */
	private static String getDescrizioneTipoDocumento(Document fatturaPADocument) {
		Node node = fatturaPADocument.selectSingleNode("//FatturaElettronicaBody/DatiGenerali/DatiGeneraliDocumento/TipoDocumento");
		String value = (node == null)? "" : node.getText();
		if (!value.isEmpty() && tipiDocumento.containsKey(value)) {
			return (String)tipiDocumento.get(value);
		}
		return value;
	}

	/**
	 * ritorna il nome del cedente/prestatore della fattura
	 * @param fatturaPADocument documento XML della fattura
	 * @return
	 */
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

	/**
	 * Ritorna il nome del cessionario/committente della fattura
	 * @return
	 */
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

	/**
	 * aggiunge i dati estratti dal file della fattura al documento da registrare su DocWay
	 * all'interno della sezione extra
	 *
	 * @param fatturaPADocument documento XML della fattura
	 * @param fatturaPAItem riferimento alla singola fattura all'interno del documento su DocWay
	 */
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

	/**
	 * Aggiunta dei dati generali relativi ad una fatturaPA
	 */
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

	/**
	 * recupero della causale della fattura. Dalla versione 1.1 di fatturaPA puo' essere contenuta in un elemento ripetibile (per permettere
	 * l'inserimento di causali con piu' di 200 caratteri)
	 *
	 * @param xmlfattura
	 * @return
	 */
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
	
	public static String getInfoNotifica(String tipoNotifica, String fileNameNotifica) {
		String info = "";
		if (tipoNotifica.equals(TIPO_MESSAGGIO_RC))
			info = "Ricevuta di consegna";
		else if (tipoNotifica.equals(TIPO_MESSAGGIO_NS))
			info = "Notifica di scarto";
		else if (tipoNotifica.equals(TIPO_MESSAGGIO_MC))
			info = "Notifica di mancata consegna";
		else if (tipoNotifica.equals(TIPO_MESSAGGIO_NE))
			info = "Notifica esito cedente / prestatore";
		else if (tipoNotifica.equals(TIPO_MESSAGGIO_MT))
			info = "File dei metadati";
		else if (tipoNotifica.equals(TIPO_MESSAGGIO_EC))
			info = "Notifica di esito cessionario / committente";
		else if (tipoNotifica.equals(TIPO_MESSAGGIO_SE))
			info = "Notifica di scarto esito cessionario / committente";
		else if (tipoNotifica.equals(TIPO_MESSAGGIO_DT))
			info = "Notifica decorrenza termini";
		else if (tipoNotifica.equals(TIPO_MESSAGGIO_AT))
			info = "Attestazione di avvenuta trasmissione della fattura con impossibilità  di recapito";
		else if (tipoNotifica.equals(TIPO_MESSAGGIO_SEND))
			info = "FatturaPA inviata al Sistema di Interscambio";
		
		if (!tipoNotifica.equals(TIPO_MESSAGGIO_SEND))
			info = info + " [" + tipoNotifica + "]";
		
		if (fileNameNotifica != null && !fileNameNotifica.equals(""))
			info = info + " (" + fileNameNotifica + ")";
		
		return info;
	}
	
	public static String getEsitoNotifica(Document notificaFatturaPADocument, String tipoNotifica) {
		Node node = null;
		if (tipoNotifica.equals(TIPO_MESSAGGIO_EC))
			node = notificaFatturaPADocument.selectSingleNode("//Esito");
		else if (tipoNotifica.equals(TIPO_MESSAGGIO_SE))
			node = notificaFatturaPADocument.selectSingleNode("//Scarto");
		else if (tipoNotifica.equals(TIPO_MESSAGGIO_NE))
			node = notificaFatturaPADocument.selectSingleNode("//EsitoCommittente/Esito");
		return (node == null)? "" : node.getText();
	}
	
	public static String getNoteNotifica(Document notificaFatturaPADocument, String tipoNotifica) {
		if (tipoNotifica.equals(TIPO_MESSAGGIO_NS)) {
			List<?> errori = notificaFatturaPADocument.selectNodes("//ListaErrori/Errore");
			if (errori != null && errori.size() > 0) {
				String note = "";
				for (int i=0; i<errori.size(); i++) {
					Element errore = (Element) errori.get(i);
					if (errore != null) {
						note += "[" + errore.elementText("Codice") + "] " + errore.elementText("Descrizione") + "; ";
					}
				}
				if (note.length() > 0)
					note = note.substring(0, note.length() - 2);
				
				return note;
			}
		}
		else if (tipoNotifica.equals(TIPO_MESSAGGIO_NE)) { // eventuale motivazione di rifiuto di una fatturaPA attiva
			Node node = notificaFatturaPADocument.selectSingleNode("//EsitoCommittente/Descrizione");
			return (node == null)? "" : node.getText();
		}
		else {
			Node node = notificaFatturaPADocument.selectSingleNode("//Note");
			return (node == null)? "" : node.getText();			
		}
		return "";
	}	

	public static List<ErroreItem> getListaErroriNotifica(Document notificaFatturaPADocument, String tipoNotifica) {
		List<ErroreItem> errori = new ArrayList<ErroreItem>();
		
		if (tipoNotifica.equals(TIPO_MESSAGGIO_NS)) { // Notifica di scarto
			List<?> nsErrors = notificaFatturaPADocument.selectNodes("//ListaErrori/Errore");
			if (nsErrors != null && nsErrors.size() > 0) {
				for (int i=0; i<nsErrors.size(); i++) {
					Element nsError = (Element) nsErrors.get(i);
					if (nsError != null) {
						String code = nsError.elementText("Codice");
						String description = nsError.elementText("Descrizione");
						if (code != null && code.length() > 0) {
							if (description == null)
								description = "";
							ErroreItem erroreItem = new ErroreItem();
							erroreItem.setCodice(code);
							erroreItem.setDescrizione(description);
							errori.add(erroreItem);
						}
					}
				}
			}
		}
		
		return errori;
	}	

	public static String getTipoNotificaRiferita(String tipoNotifica) {
		if (tipoNotifica.equals(TIPO_MESSAGGIO_SE))
			return TIPO_MESSAGGIO_EC;
		else if (tipoNotifica.equals(TIPO_MESSAGGIO_NS))
			return TIPO_MESSAGGIO_SEND;
		// TODO gestire i casi mancanti
		
		return "";
	}	
	
}
