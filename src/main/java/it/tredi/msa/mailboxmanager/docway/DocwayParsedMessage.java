package it.tredi.msa.mailboxmanager.docway;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.Message;
import javax.mail.Part;

import org.apache.commons.codec.binary.Base64;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import it.tredi.mail.entity.MailAttach;
import it.tredi.msa.mailboxmanager.ParsedMessage;
import it.tredi.msa.mailboxmanager.PartContentProvider;

/**
 * Parsing aggiuntivo del messaggio in base alle finalita' di DocWay. Estrazioni di ulteriori informazioni (rispetto a quelle di base) necessarie all'elaborazione
 * del messaggio email come documento di DocWay
 */
public class DocwayParsedMessage extends ParsedMessage {
	
	//interopPA
	private Document segnaturaInteropPADocument;
	private boolean segnaturaInteropPADocumentInCache = false;
	
	private Document confermaRicezioneInteropPADocument;
	private boolean confermaRicezioneInteropPADocumentInCache = false;

	private Document notificaEccezioneInteropPADocument;
	private boolean notificaEccezioneInteropPADocumentInCache = false;

	private Document aggiornamentoConfermaInteropPADocument;
	private boolean aggiornamentoConfermaInteropPADocumentInCache = false;

	private Document annullamentoProtocollazioneInteropPADocument;
	private boolean annullamentoProtocollazioneInteropPADocumentInCache = false;
	
	private Document interopPaDocument;
	
	private String motivazioneNotificaEccezioneToSend;
	
	private final static String INTEROP_PA_FAILED_BASE_MESSAGE = "Il messaggio è stato archiviato come documento ordinario: ";
	private final static String MORE_INTEROP_PA_XML_FILE_FOUND_MESSAGE = INTEROP_PA_FAILED_BASE_MESSAGE + "sono stati individuati (%s) file %s";
	private final static String INTEROP_PA_XML_FILE_PARSING_ERROR_MESSAGE = INTEROP_PA_FAILED_BASE_MESSAGE + "si è verificato un errore durante il parsing di %s";
	private final static String INTEROP_PA_XML_FILE_ROOT_MISMATCH_MESSAGE = INTEROP_PA_FAILED_BASE_MESSAGE + "l'elemento radice del file %s non corrisponde a quello previsto dalle specifiche di interoperabilità tra PA: %s";
	private final static String INTEROP_PA_XML_FILE_COD_AMM_MISMATCH_MESSAGE = INTEROP_PA_FAILED_BASE_MESSAGE + "il Codice Amministrazione individuato nel file %s non corrisponde a quello previsto: %s";
	private final static String INTEROP_PA_XML_FILE_COD_AOO_MISMATCH_MESSAGE = INTEROP_PA_FAILED_BASE_MESSAGE + "il Codice AOO individuato nel file %s non corrisponde a quello previsto: %s";
	private final static String SEGNATURA_COD_AMM_AOO_MATCH_MESSAGE = INTEROP_PA_FAILED_BASE_MESSAGE + "il codice Amministrazione e il codice AOO contenuti nel file Segnatura.xml coincidono con quelli dell'archivio corrente.";
	
	//fatturaPA
	private Document fatturaPADocument;
	private boolean fatturaPADocumentInCache = false;
	private Document fileMetadatiDocument;

	private Document notificaFatturaPADocument;
	private boolean notificaFatturaPADocumentInCache = false;
	private String tipoNotificaFatturaPA;
	private String fileNameNotificaFatturaPA;
	private String fileNameFatturaRiferita;
	
	public DocwayParsedMessage(Message message) throws Exception {
		super(message);
	}
	
	public Document getInteropPaDocument() {
		return interopPaDocument;
	}

	public void setInteropPaDocument(Document interopPaDocument) {
		this.interopPaDocument = interopPaDocument;
	}

	public String getMotivazioneNotificaEccezioneToSend() {
		return motivazioneNotificaEccezioneToSend;
	}

	public void setMotivazioneNotificaEccezioneToSend(String motivazioneNotificaEccezioneToSend) {
		this.motivazioneNotificaEccezioneToSend = motivazioneNotificaEccezioneToSend;
	}

	public boolean isPecReceiptForInteropPAbySubject() throws Exception {
		if (isPecReceipt()) {
			String originalSubject = super.getSubjectFromDatiCertPec();
			originalSubject = cleanInteropPASubject(originalSubject);
			if (originalSubject.indexOf(" ") != -1) {
				originalSubject = originalSubject.substring(0, originalSubject.indexOf(" "));
				Pattern pattern = Pattern.compile("\\d{4}-\\w{7}-\\d{7}\\((\\*|\\d{1,5})\\)"); //anno-cod_amm_aoo-num_prot(rif_est_index)
				Matcher matcher = pattern.matcher(originalSubject);
				if (matcher.matches())
					return true;
				pattern = Pattern.compile("\\d{5,10}\\((\\*|\\d{1,5})\\)"); //nrecord(rif_est_index)
				matcher = pattern.matcher(originalSubject);				
				return matcher.matches();
			}
		}
		return false;
	}
	
	/**
	 * Costruzione della query di ricerca del documento al quale agganciare una notifica di interoperabilita' attraverso 
	 * l'analisi dell'oggetto del messaggio email relativo alla notifica stessa
	 * @return
	 * @throws Exception
	 */
	public String buildQueryForDocway4DocumentFromInteropPASubject() throws Exception {
		String originalSubject = super.getSubjectFromDatiCertPec();
		originalSubject = cleanInteropPASubject(originalSubject);	
		String numero = originalSubject.substring(0, originalSubject.indexOf("("));
		// mbernardini 12/12/2018 : query su num_protocollo anziche' nrecord (task #16797)
		return (numero.indexOf("-") != -1) ? "[/doc/@num_prot]=\"" + numero + "\"" : "[/doc/@nrecord]=\"" + numero + "\"";
	}
	
	private String cleanInteropPASubject(String subject) {
		if (subject.startsWith("Conferma Ricezione: "))
			subject = subject.substring(20);
		else if (subject.startsWith("Annullamento Protocollazione: "))
			subject = subject.substring(30);
		else if (subject.startsWith("Notifica Eccezione: "))
			subject = subject.substring(20);
		return subject;
	}
	
	public Document getSegnaturaInteropPADocument() {
		if (!segnaturaInteropPADocumentInCache) {
			segnaturaInteropPADocument = getInteropPAMessageDocument("Segnatura.xml", "Segnatura");
			segnaturaInteropPADocumentInCache = true;
		}
		return segnaturaInteropPADocument; 		
	}
	
	private Document getConfermaRicezioneInteropPADocument() {
		if (!confermaRicezioneInteropPADocumentInCache) {
			confermaRicezioneInteropPADocument = getInteropPAMessageDocument("Conferma.xml", "ConfermaRicezione");
			confermaRicezioneInteropPADocumentInCache = true;
		}
		return confermaRicezioneInteropPADocument; 				
	}	
	
	private Document getNotificaEccezioneInteropPADocument() {
		if (!notificaEccezioneInteropPADocumentInCache) {
			notificaEccezioneInteropPADocument = getInteropPAMessageDocument("Eccezione.xml", "NotificaEccezione");
			notificaEccezioneInteropPADocumentInCache = true;
		}
		return notificaEccezioneInteropPADocument; 		
	}
	
	private Document getAggiornamentoConfermaInteropPADocument() {
		if (!aggiornamentoConfermaInteropPADocumentInCache) {
			aggiornamentoConfermaInteropPADocument = getInteropPAMessageDocument("Aggiornamento.xml", "AggiornamentoConferma");
			notificaEccezioneInteropPADocumentInCache = true;
		}
		return aggiornamentoConfermaInteropPADocument; 			
	}	
	
	private Document getAnnullamentoProtocollazioneInteropPADocument() {
		if (!annullamentoProtocollazioneInteropPADocumentInCache) {
			annullamentoProtocollazioneInteropPADocument = getInteropPAMessageDocument("Annullamento.xml", "AnnullamentoProtocollazione");
			annullamentoProtocollazioneInteropPADocumentInCache = true;
		}
		return annullamentoProtocollazioneInteropPADocument;
	}
	
	private Document getInteropPAMessageDocument(String fileName, String rootElName) {
		Document document = null;
		try {
			List<Part> partsL = this.getAttachmentPartsByName(fileName);
			if (partsL.size() == 1) {
				byte []b = (new PartContentProvider(partsL.get(0))).getContent();
				try {
					document = DocumentHelper.parseText(new String(b));
					if (!document.getRootElement().getName().equals(rootElName)) {
						super.addRelevantMessage(String.format(INTEROP_PA_XML_FILE_ROOT_MISMATCH_MESSAGE, fileName, rootElName));
						document = null;
					}
				}
				catch (Exception e) {
					super.addRelevantMessage(String.format(INTEROP_PA_XML_FILE_PARSING_ERROR_MESSAGE, fileName));
					document = null;
				}
			}
			else if (partsL.size() > 1)
				super.addRelevantMessage(String.format(MORE_INTEROP_PA_XML_FILE_FOUND_MESSAGE, partsL.size(), fileName));
		}
		catch (Exception e) {
			document = null;
		}
		if (document != null)
			interopPaDocument = document;
		return document; 
	}	
	
	public boolean isPecReceiptForInteropPA(String codAmmInteropPA, String codAooInteropPA) throws Exception {
		if (isPecReceipt()) {
			Document document;
			if ((document = getSegnaturaInteropPADocument()) != null)
				return checkInteropPADocument(document, "Segnatura.xml", codAmmInteropPA, codAooInteropPA, "/Segnatura/Intestazione/Identificatore", false);
			if ((document = getConfermaRicezioneInteropPADocument()) != null)
				return checkInteropPADocument(document, "Conferma.xml", codAmmInteropPA, codAooInteropPA, "/ConfermaRicezione/MessaggioRicevuto/Identificatore", false);
			if ((document = getNotificaEccezioneInteropPADocument()) != null)
				return checkInteropPADocument(document, "Eccezione.xml", codAmmInteropPA, codAooInteropPA, "/NotificaEccezione/MessaggioRicevuto/Identificatore", false);
			if ((document = getAggiornamentoConfermaInteropPADocument()) != null)
				return checkInteropPADocument(document, "Aggiornamento.xml", codAmmInteropPA, codAooInteropPA, "/AggiornamentoConferma/MessaggioRicevuto/Identificatore", false);
			if ((document = getAnnullamentoProtocollazioneInteropPADocument()) != null)
				return checkInteropPADocument(document, "Annullamento.xml", codAmmInteropPA, codAooInteropPA, "/AnnullamentoProtocollazione/Identificatore", false);
		}
		return false;
	}	
	
	private boolean checkInteropPADocument(Document document, String fileName, String codAmmInteropPA, String codAooInteropPA, String identificatoreElXpath, boolean identificatoreOpzionale) {
		if (identificatoreOpzionale && document.selectSingleNode(identificatoreElXpath) == null)
			return true;
		try {
			if (!document.selectSingleNode(identificatoreElXpath + "/CodiceAmministrazione").getText().equals(codAmmInteropPA)) {
				super.addRelevantMessage(String.format(INTEROP_PA_XML_FILE_COD_AMM_MISMATCH_MESSAGE, fileName, codAmmInteropPA));		
				return false;
			}
			if (!document.selectSingleNode(identificatoreElXpath + "/CodiceAOO").getText().equals(codAooInteropPA)) {
				super.addRelevantMessage(String.format(INTEROP_PA_XML_FILE_COD_AOO_MISMATCH_MESSAGE, fileName, codAooInteropPA));
				return false;					
			}
		}
		catch (Exception e) {
			super.addRelevantMessage(String.format(INTEROP_PA_XML_FILE_PARSING_ERROR_MESSAGE, fileName));
			return false;
		}
		return true;
	}
	
	public boolean isSegnaturaInteropPAMessage(String codAmmInteropPA, String codAooInteropPA) {
		if (isPecMessage() && !isReplyOrForward()) {
			Document document = getSegnaturaInteropPADocument();
			if (document != null) {
				try {
					String codAmmInSegnatura = document.selectSingleNode("/Segnatura/Intestazione/Identificatore/CodiceAmministrazione").getText();
					String codAooInSegnatura = document.selectSingleNode("/Segnatura/Intestazione/Identificatore/CodiceAOO").getText();
					if (codAmmInSegnatura.equals(codAmmInSegnatura) && codAooInSegnatura.equals(codAooInteropPA)) {
						super.addRelevantMessage(SEGNATURA_COD_AMM_AOO_MATCH_MESSAGE);
						return false;
					}
					return true;
				}
				catch (Exception e) {
					super.addRelevantMessage(String.format(INTEROP_PA_XML_FILE_PARSING_ERROR_MESSAGE, "Segnatura.xml"));
					return false;
				}				
			}
		}
		return false;
	}
	
	public boolean isNotificaInteropPAMessage(String codAmmInteropPA, String codAooInteropPA) {
		if (isConfermaRicezioneInteropPAMessage(codAmmInteropPA, codAooInteropPA))
			return true;
		if (isNotificaEccezioneInteropPAMessage(codAmmInteropPA, codAooInteropPA))
			return true;
		if (isAggiornamentoConfermaInteropPAMessage(codAmmInteropPA, codAooInteropPA))
			return true;
		if (isAnnullamentoProtocollazioneInteropPAMessage(codAmmInteropPA, codAooInteropPA))
			return true;
		return false;
	}

	public boolean isConfermaRicezioneInteropPAMessage(String codAmmInteropPA, String codAooInteropPA) {
		Document document;
		if (isPecMessage() && !isReplyOrForward() && (document = getConfermaRicezioneInteropPADocument()) != null)
			return checkInteropPADocument(document, "Conferma.xml", codAmmInteropPA, codAooInteropPA, "/ConfermaRicezione/MessaggioRicevuto/Identificatore", false);
		return false;
	}
	
	public boolean isNotificaEccezioneInteropPAMessage(String codAmmInteropPA, String codAooInteropPA) {
		Document document;
		if (isPecMessage() && !isReplyOrForward() && (document = getNotificaEccezioneInteropPADocument()) != null)
			return checkInteropPADocument(document, "Eccezione.xml", codAmmInteropPA, codAooInteropPA, "/NotificaEccezione/MessaggioRicevuto/Identificatore", false);
		return false;
	}
	
	public boolean isAggiornamentoConfermaInteropPAMessage(String codAmmInteropPA, String codAooInteropPA) {
		Document document;
		if (isPecMessage() && !isReplyOrForward() && (document = getAggiornamentoConfermaInteropPADocument()) != null)
			return checkInteropPADocument(document, "Aggiornamento.xml", codAmmInteropPA, codAooInteropPA, "/AggiornamentoConferma/MessaggioRicevuto/Identificatore", false);
		return false;
	}
	
	public boolean isAnnullamentoProtocollazioneInteropPAMessage(String codAmmInteropPA, String codAooInteropPA) {
		if (isPecMessage() && !isReplyOrForward() && getAnnullamentoProtocollazioneInteropPADocument() != null)
			return true;
		return false;
	}

	public String buildQueryForDocway4DocumentFromInteropPASegnatura(String codAmm, String codAoo) {
		String query = "";
		if (this.interopPaDocument != null) {
			Element identificatoreEl = (Element)this.interopPaDocument.selectSingleNode("/Segnatura/Intestazione/Identificatore");
			query = "[/doc/rif_esterni/rif/@n_prot]=\"" + identificatoreEl.elementText("DataRegistrazione").substring(0, 4) + "-" + identificatoreEl.elementText("CodiceAmministrazione") +
					identificatoreEl.elementText("CodiceAOO") + "-" + identificatoreEl.elementText("NumeroRegistrazione") + "\" AND [/doc/@cod_amm_aoo]=\"" + codAmm + codAoo + "\"";
		}
		return query;
	}
	
	/**
	 * Costruzione della query per il recupero del documento al quale agganciare una notifica relativa ad un messaggio email di interoperabilita'
	 * @param codAmm
	 * @param codAoo
	 * @return
	 */
	public String buildQueryForDocway4DocumentFromInteropPAPecReceipt(String codAmm, String codAoo) {
		String query = "";
		if (interopPaDocument != null) {
			Element el = interopPaDocument.getRootElement();
			if (el.getName().equals("Segnatura"))
				el = el.element("Intestazione");
			Element identificatoreEl = el.element("Identificatore");
			if (identificatoreEl == null) {
				identificatoreEl = el.element("MessaggioRicevuto").element("Identificatore");
//per ora disabilitata questa opzione perchè potrebbero esserci più documenti con lo stesso numero prot mittente -> si preferisce utilizzare il subject
//				query = "[/doc/rif_esterni/rif/@n_prot]=\"" + identificatoreEl.elementText("DataRegistrazione").substring(0, 4) + "-" + identificatoreEl.elementText("CodiceAmministrazione") +
//						identificatoreEl.elementText("CodiceAOO") + "-" + identificatoreEl.elementText("NumeroRegistrazione") + "\" AND [/doc/@cod_amm_aoo]=\"" + codAmm + codAoo + "\"";
			}
			else {
				query = "[/doc/@num_prot]=\"" + identificatoreEl.elementText("DataRegistrazione").substring(0, 4) + "-" + codAmm + codAoo + "-" + identificatoreEl.elementText("NumeroRegistrazione") + "\"";
			}
		}
		return query;
	}

	public String buildQueryForDocway4DocumentFromInteropPANotification(String codAmm, String codAoo) {
		String query = "";
		if (interopPaDocument != null) {
			if (interopPaDocument.getRootElement().getName().equals("AnnullamentoProtocollazione")) {
				Element identificatoreEl = interopPaDocument.getRootElement().element("Identificatore");
				query = "[/doc/rif_esterni/rif/@n_prot]=\"" + identificatoreEl.elementText("DataRegistrazione").substring(0, 4) + "-" + identificatoreEl.elementText("CodiceAmministrazione") +
						identificatoreEl.elementText("CodiceAOO") + "-" + identificatoreEl.elementText("NumeroRegistrazione") + "\" AND [/doc/@cod_amm_aoo]=\"" + codAmm + codAoo + "\"";				
			}
			else {
				Element identificatoreEl = interopPaDocument.getRootElement().element("MessaggioRicevuto").element("Identificatore");
				query = "[/doc/@num_prot]=\"" + identificatoreEl.elementText("DataRegistrazione").substring(0, 4) + "-" + codAmm + codAoo + "-" + identificatoreEl.elementText("NumeroRegistrazione") + "\"";
			}
		}
		return query;
	}
	
	private Document getFileInterscambioFatturaPADocument(boolean isNotifica) throws Exception {
		for (MailAttach attachment:super.getAttachments()) {
			String fileName = attachment.getFileName();
			if (fileName.toUpperCase().endsWith(".XML") || fileName.toUpperCase().endsWith(".XML.P7M")) {
				int underscoreOccurrences = fileName.replaceAll("[^_]", "").length();
				if (isNotifica && underscoreOccurrences > 1 || !isNotifica && underscoreOccurrences == 1) {
					
					if (isNotifica) { //estrazione tipo notifica e nome file fattura riferita
						fileNameNotificaFatturaPA = fileName;
						String fileNameParts[] = fileName.split("_"); // es nome file notifica: ITAAABBB99T99X999W_00001_AT_001.xml
						tipoNotificaFatturaPA = fileNameParts[2];
						fileNameFatturaRiferita = fileNameParts[0] + "_" + fileNameParts[1];
					}
					
					String regex = "";
					if (fileName.toUpperCase().startsWith("IT"))
						regex = "^[a-zA-Z]{2}[a-zA-Z0-9]{11,16}_[a-zA-Z0-9]{1,5}";
					else
						regex = "^[a-zA-Z]{2}[a-zA-Z0-9]{2,28}_[a-zA-Z0-9]{1,5}";
					Pattern pattern = Pattern.compile(regex);
					Matcher matcher = pattern.matcher(fileName);
					if (matcher.find()) {
						byte []b = (new PartContentProvider(attachment.getPart())).getContent();
						
			            //se base64 -> decode
			            if (Base64.isBase64(b)) {
			            	b = Base64.decodeBase64(b);
			            }
			            
			            //se extensionFattura = p7m -> sbustamento
			            if (fileName.toUpperCase().endsWith(".P7M")) {
			            	CMSSignedData csd = new CMSSignedData(b);
			            	CMSProcessableByteArray cpb = (CMSProcessableByteArray)csd.getSignedContent();
			            	b = (byte[])cpb.getContent();
			            }
			            
			            return DocumentHelper.parseText(new String(b));
					}						
				}
			}
		}
		return null;
	}

	public Document getFatturaPADocument() throws Exception {
		if (!fatturaPADocumentInCache) {
			fatturaPADocument = getFileInterscambioFatturaPADocument(false);
			fileMetadatiDocument = getFileInterscambioFatturaPADocument(true);
			fatturaPADocumentInCache = true;
		}
		return fatturaPADocument;
	}
	
	public Document getFileMetadatiDocument() throws Exception {
		return fileMetadatiDocument;
	}
	
	public boolean isFatturaPAMessage(String sdiDomainAddress) throws Exception {
		if (isPecMessage()) { // && MessageUtils.isReplyOrForward(message)
			if (sdiDomainAddress.isEmpty() || super.getMittenteAddressFromDatiCertPec().contains(sdiDomainAddress)) { // controllo su mittente
				return getFatturaPADocument() != null; 
			}
		}
		return false;
	}
	
	public Document getNotificaFatturaPADocument() throws Exception {
		if (!notificaFatturaPADocumentInCache) {
			notificaFatturaPADocument = getFileInterscambioFatturaPADocument(true);
			notificaFatturaPADocumentInCache = true;
		}
		return notificaFatturaPADocument;
	}	
	
	public boolean isNotificaFatturaPAMessage(String sdiDomainAddress) throws Exception {
		if (!isFatturaPAMessage(sdiDomainAddress)) { //check per evitare di essere ingannati dal file dei metadati (di fatto viene riconosciuto come una notifica)
			if (isPecMessage()) { // && MessageUtils.isReplyOrForward(message)
				if (sdiDomainAddress.isEmpty() || super.getMittenteAddressFromDatiCertPec().contains(sdiDomainAddress)) { // controllo su mittente
					return getNotificaFatturaPADocument() != null; 
				}
			}			
		}
		return false;
	}	
	
	public String buildQueryForDocway4DocumentFromFatturaPANotification() throws Exception {
		return "([/doc/extra/fatturaPA/@fileNameFattura]=\"" + fileNameFatturaRiferita + "\")";
	}
	
	public String getFileNameNotificaFatturaPA() {
		return this.fileNameNotificaFatturaPA;
	}
	
	public String getTipoNotificaFatturaPA() {
		return this.tipoNotificaFatturaPA;
	}
	
	public String getFileNameFatturaRiferita() {
		return this.fileNameFatturaRiferita;
	}
	
	public boolean isPecReceiptForFatturaPAbySubject() throws Exception {
		if (isPecReceipt()) {
			String originalSubject = super.getSubjectFromDatiCertPec();
			if (originalSubject.indexOf(" ") != -1) {
				originalSubject = originalSubject.substring(0, originalSubject.indexOf(" "));
				Pattern pattern = Pattern.compile("\\d{4}-\\w{7}-\\d{7}\\((FTRPA-doc|FTRPA-\\d{1,5})\\)"); //anno-cod_amm_aoo-num_prot(FTRPA-ftr_index)
				Matcher matcher = pattern.matcher(originalSubject);
				return matcher.matches();
			}
		}
		return false;
	}
	
	public String buildQueryForDocway4DocumentFromFatturaPASubject() throws Exception {
		String originalSubject = super.getSubjectFromDatiCertPec();
		String numero = originalSubject.substring(0, originalSubject.indexOf("("));
		// mbernardini 12/12/2018 : query su num_protocollo anziche' nrecord (task #16797)
		return (numero.indexOf("-") != -1) ? "[/doc/@num_prot]=\"" + numero + "\"" : "[/doc/@nrecord]=\"" + numero + "\"";
	}
	
}

