package it.tredi.msa.mailboxmanager.docway;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.Message;
import javax.mail.Part;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;

import it.tredi.mail.MessageUtils;
import it.tredi.msa.mailboxmanager.ParsedMessage;
import it.tredi.msa.mailboxmanager.PartContentProvider;

public class DocwayParsedMessage extends ParsedMessage {
	
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

	public DocwayParsedMessage(Message message) throws Exception {
		super(message);
	}

	public boolean isPecReceiptForInteropPAbySubject() throws Exception {
		if (isPecReceipt()) {
			String originalSubject = super.getSubjectFromDatiCertPec();
			if (originalSubject.indexOf(" ") != -1) {
				originalSubject = originalSubject.substring(0, originalSubject.indexOf(" "));
				Pattern pattern = Pattern.compile("\\d{4}-\\w{7}-\\d{7}\\((\\*|\\d{1,5})\\)");
				Matcher matcher = pattern.matcher(originalSubject);
				return matcher.matches();
			}
		}
		return false;
	}
	
	public boolean isPecReceiptForInteropPA(String codAmmInteropPA, String codAooInteropPA) throws Exception {
		if (isPecReceipt()) {
			if (getSegnaturaInteropPADocument(codAmmInteropPA, codAooInteropPA) != null)
				return true;
			if (getConfermaRicezioneInteropPADocument(codAmmInteropPA, codAooInteropPA) != null)
				return true;
			if (getNotificaEccezioneInteropPADocument(codAmmInteropPA, codAooInteropPA) != null)
				return true;
			if (getAggiornamentoConfermaInteropPADocument(codAmmInteropPA, codAooInteropPA) != null)
				return true;
			if (getAnnullamentoProtocollazioneInteropPADocument(codAmmInteropPA, codAooInteropPA) != null)
				return true;
		}
		return false;
	}	
	
	public String extractNumProtFromOriginalSubject() throws Exception {
		String originalSubject = super.getSubjectFromDatiCertPec();
		return originalSubject.substring(0, originalSubject.indexOf("("));
	}
	
	public boolean isSegnaturaInteropPAMessage(String codAmmInteropPA, String codAooInteropPA) {
		if (isPecMessage() && !MessageUtils.isReplyOrForward(message) && getSegnaturaInteropPADocument(codAmmInteropPA, codAooInteropPA) != null)
			return true;
		return false;
	}
	
	public Document getSegnaturaInteropPADocument(String codAmmInteropPA, String codAooInteropPA) {
		if (!segnaturaInteropPADocumentInCache) {
			segnaturaInteropPADocument = getInteropPAMessageDocument("Segnatura.xml", "Segnatura", codAmmInteropPA, codAooInteropPA, "/Segnatura/Intestazione/Identificatore");
			segnaturaInteropPADocumentInCache = true;
		}
		return segnaturaInteropPADocument; 		
	}	
	
	public boolean isConfermaRicezioneInteropPAMessage(String codAmmInteropPA, String codAooInteropPA) {
		if (isPecMessage() && !MessageUtils.isReplyOrForward(message) && getConfermaRicezioneInteropPADocument(codAmmInteropPA, codAooInteropPA) != null)
			return true;
		return false;
	}	
	
	public Document getConfermaRicezioneInteropPADocument(String codAmmInteropPA, String codAooInteropPA) {
		if (!confermaRicezioneInteropPADocumentInCache) {
			confermaRicezioneInteropPADocument = getInteropPAMessageDocument("Conferma.xml", "ConfermaRicezione", codAmmInteropPA, codAooInteropPA, "/ConfermaRicezione/Identificatore");
			confermaRicezioneInteropPADocumentInCache = true;
		}
		return confermaRicezioneInteropPADocument; 				
	}

	public boolean isNotificaEccezioneInteropPAMessage(String codAmmInteropPA, String codAooInteropPA) {
		if (isPecMessage() && !MessageUtils.isReplyOrForward(message) && getNotificaEccezioneInteropPADocument(codAmmInteropPA, codAooInteropPA) != null)
			return true;
		return false;
	}	
	
	public Document getNotificaEccezioneInteropPADocument(String codAmmInteropPA, String codAooInteropPA) {
		if (!notificaEccezioneInteropPADocumentInCache) {
			notificaEccezioneInteropPADocument = getInteropPAMessageDocument("Eccezione.xml", "NotificaEccezione", codAmmInteropPA, codAooInteropPA, "/NotificaEccezione/Identificatore");
			notificaEccezioneInteropPADocumentInCache = true;
		}
		return notificaEccezioneInteropPADocument; 		
	}	
	
	public boolean isAggiornamentoConfermaInteropPAMessage(String codAmmInteropPA, String codAooInteropPA) {
		if (isPecMessage() && !MessageUtils.isReplyOrForward(message) && getAggiornamentoConfermaInteropPADocument(codAmmInteropPA, codAooInteropPA) != null)
			return true;
		return false;
	}	
	
	public Document getAggiornamentoConfermaInteropPADocument(String codAmmInteropPA, String codAooInteropPA) {
		if (!aggiornamentoConfermaInteropPADocumentInCache) {
			aggiornamentoConfermaInteropPADocument = getInteropPAMessageDocument("Aggiornamento.xml", "AggiornamentoConferma", codAmmInteropPA, codAooInteropPA, "/AggiornamentoConferma/Identificatore");
			notificaEccezioneInteropPADocumentInCache = true;
		}
		return aggiornamentoConfermaInteropPADocument; 			
	}	
	
	public boolean isAnnullamentoProtocollazioneInteropPAMessage(String codAmmInteropPA, String codAooInteropPA) {
		if (isPecMessage() && !MessageUtils.isReplyOrForward(message) && getAnnullamentoProtocollazioneInteropPADocument(codAmmInteropPA, codAooInteropPA) != null)
			return true;
		return false;
	}	
	
	public Document getAnnullamentoProtocollazioneInteropPADocument(String codAmmInteropPA, String codAooInteropPA) {
		if (!annullamentoProtocollazioneInteropPADocumentInCache) {
			annullamentoProtocollazioneInteropPADocument = getInteropPAMessageDocument("Annullamento.xml", "AnnullamentoProtocollazione", codAmmInteropPA, codAooInteropPA, "/AnnullamentoProtocollazione/Identificatore");
			annullamentoProtocollazioneInteropPADocumentInCache = true;
		}
		return annullamentoProtocollazioneInteropPADocument;
	}	
	
	private Document getInteropPAMessageDocument(String fileName, String rootElName, String codAmmInteropPA, String codAooInteropPA, String identificatoreElXpath) {
		Document document = null;
		try {
			Part part = MessageUtils.getAttachmentPartByName(getAttachments(), fileName);
			if (part != null) {
				byte []b = (new PartContentProvider(part)).getContent();
				document = DocumentHelper.parseText(new String(b));
				if (!document.getRootElement().getName().equals(rootElName))
					return null;
				if (!document.selectSingleNode(identificatoreElXpath + "/CodiceAmministrazione").getText().equals(codAmmInteropPA))
					return null;
				if (!document.selectSingleNode(identificatoreElXpath + "/CodiceAOO").getText().equals(codAooInteropPA))
					return null;
			}
		}
		catch (Exception e) {
			; //do nothing
		}
		return document; 
	}		
	
	public String extractNumProtFromInteropPAMessage(String codAmm, String codAoo, String codAmmInteropPA, String codAooInteropPA) {
		if (getSegnaturaInteropPADocument(codAmmInteropPA, codAooInteropPA) != null)
			return extractNumProtFromSegnaturaInteropPADocument(codAmm, codAoo, codAmmInteropPA, codAooInteropPA);
		if (getConfermaRicezioneInteropPADocument(codAmmInteropPA, codAooInteropPA) != null)
			return extractNumProtFromConfermaRicezioneInteropPADocument(codAmm, codAoo, codAmmInteropPA, codAooInteropPA);
		if (getNotificaEccezioneInteropPADocument(codAmmInteropPA, codAooInteropPA) != null)
			return extractNumProtFromNotificaEccezioneInteropPADocument(codAmm, codAoo, codAmmInteropPA, codAooInteropPA);
		if (getAggiornamentoConfermaInteropPADocument(codAmmInteropPA, codAooInteropPA) != null)
			return extractNumProtFromAggiornamentoConfermaInteropPADocument(codAmm, codAoo, codAmmInteropPA, codAooInteropPA);
		if (getAnnullamentoProtocollazioneInteropPADocument(codAmmInteropPA, codAooInteropPA) != null)
			return extractNumProtFromAnnullamentoProtocollazioneInteropPADocument(codAmm, codAoo, codAmmInteropPA, codAooInteropPA);
		return null;
	}
	
	public String extractNumProtFromSegnaturaInteropPADocument(String codAmm, String codAoo, String codAmmInteropPA, String codAooInteropPA) {
		return extractNumProtFromInteropPAMessageDocument(getSegnaturaInteropPADocument(codAmmInteropPA, codAooInteropPA), codAmm, codAoo, "/Segnatura/Intestazione/Identificatore");
	}
	
	public String extractNumProtFromConfermaRicezioneInteropPADocument(String codAmm, String codAoo, String codAmmInteropPA, String codAooInteropPA) {
		return extractNumProtFromInteropPAMessageDocument(getConfermaRicezioneInteropPADocument(codAmmInteropPA, codAooInteropPA), codAmm, codAoo, "/ConfermaRicezione/Identificatore");
	}
	
	public String extractNumProtFromNotificaEccezioneInteropPADocument(String codAmm, String codAoo, String codAmmInteropPA, String codAooInteropPA) {
		return extractNumProtFromInteropPAMessageDocument(getNotificaEccezioneInteropPADocument(codAmmInteropPA, codAooInteropPA), codAmm, codAoo, "/NotificaEccezione/Identificatore");
	}	
	
	public String extractNumProtFromAggiornamentoConfermaInteropPADocument(String codAmm, String codAoo, String codAmmInteropPA, String codAooInteropPA) {
		return extractNumProtFromInteropPAMessageDocument(getAggiornamentoConfermaInteropPADocument(codAmmInteropPA, codAooInteropPA), codAmm, codAoo, "/AggiornamentoConferma/Identificatore");
	}	
	
	public String extractNumProtFromAnnullamentoProtocollazioneInteropPADocument(String codAmm, String codAoo, String codAmmInteropPA, String codAooInteropPA) {
		return extractNumProtFromInteropPAMessageDocument(getAnnullamentoProtocollazioneInteropPADocument(codAmmInteropPA, codAooInteropPA), codAmm, codAoo, "/AnnullamentoProtocollazione/Identificatore");
	}
	
	private String extractNumProtFromInteropPAMessageDocument(Document document, String codAmm, String codAoo, String identificatoreElXpath) {
		String data = document.selectSingleNode(identificatoreElXpath + "/DataRegistrazione").getText();
		String numero = document.selectSingleNode(identificatoreElXpath + "/NumeroRegistrazione").getText();
		return data.substring(0, 4) + "-" + codAmm + codAoo + "-" + numero;
		
	}
	
	
	
	
	
	
	public boolean isPecReceiptForFatturaPAbySubject() {
		return false;
//TODO - fare		
	}

	public boolean isFatturaPAMessage() {
		return false;
//TODO - fare		
	}	
	
}
