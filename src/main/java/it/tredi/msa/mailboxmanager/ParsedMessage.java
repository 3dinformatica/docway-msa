package it.tredi.msa.mailboxmanager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Part;
import javax.mail.internet.InternetAddress;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.tredi.mail.MessageParser;
import it.tredi.mail.entity.MailAttach;

public class ParsedMessage {
	
	private static final Logger logger = LoggerFactory.getLogger(ParsedMessage.class);
	
	private MessageParser parser;
	
	private boolean pecMessage = false;
	private boolean isPecMessageInCache = false;
	
	private boolean pecNotification = false;
	private boolean isPecNotificationInCache = false;
	
	private Document datiCertDocument;
	private boolean datiCertDocumentInCache = false;
	
	private List<String> relevantMssages = new ArrayList<String>();
	
	public ParsedMessage(Message message) throws Exception {
		this.parser = new MessageParser(message);
		relevantMssages = new ArrayList<>();
		getMessageId(); //force setting messageId
	}

	public Message getMessage() {
		return parser.getMessage();
	}
	
	public String getSubject() throws MessagingException {
		String subject = this.parser.getMessage().getSubject();
		if (subject == null || subject.isEmpty())
			subject = "[NESSUN OGGETTO]";
		return subject;
	}
	
	public String getFromAddress() throws MessagingException {
		return this.parser.getFromAddress();
	}

	public String getFromPersonal() throws MessagingException {
		return this.parser.getFromPersonal();
	}

	public Date getSentDate() throws MessagingException {
		return this.parser.getMessage().getSentDate();
	}
	
	private String cleanMessageId(String messageId) {
		messageId = messageId.replaceAll("<", "");
		messageId = messageId.replaceAll(">", "");
		return messageId;
	}
	
	public String getMessageId() throws Exception {
		return cleanMessageId(this.parser.getMessageId());
	}
	
	public String getToAddressesAsString() throws MessagingException {
		return internetAddressesToString(this.parser.getTo());
	}
	
	public String getCcAddressesAsString() throws MessagingException {
		return internetAddressesToString(this.parser.getCc());
	}
	
	/**
	 * Dato un array di iternetAddress restituisce una stringa con indicato l'elenco di indirizzi 
	 * @param addresses
	 * @return
	 */
	private String internetAddressesToString(InternetAddress[] addresses) {
		String out = "";
		if (addresses != null && addresses.length > 0) {
			for (InternetAddress addr : addresses) {
				if (addr != null && addr.getAddress() != null && !addr.getAddress().isEmpty()) {
					out += ", " + addr.getAddress();
				}
			}
		}
		if (out.length() >= 2)
			return out.substring(2);
		else
			return "";
	}
	
	
	public List<Part> getLeafPartsL() {
		return this.parser.getMessageParts();
	}
	
	public List<MailAttach> getAttachments() {
		return this.parser.getAttachments();
	}
	
	public String getTextParts() throws MessagingException, IOException {
		return this.parser.getText();
	}

	public String getHtmlParts() throws MessagingException, IOException {
		return this.parser.getHtml();
	}
	
	public String getTextPartsWithHeaders() throws MessagingException, IOException {
		if (getTextParts().isEmpty())
			return "";
		String headers = "From: " + getFromAddress() + "\n";
		headers += "To: " + getToAddressesAsString() + "\n";
		headers += "Cc: " + getCcAddressesAsString() + "\n";
		headers += "Sent: " + getSentDate() + "\n";
		headers += "Subject: " + getSubject() + "\n\n";
		return headers + getTextParts();
	}

	public boolean isPecMessage() {
		if (!isPecMessageInCache) {
			pecMessage = this.parser.isPecMessage();
			isPecMessageInCache = true;
		}
		return pecMessage;
	}	
	
	public boolean isPecReceipt() {
		if (!isPecNotificationInCache) {
			pecNotification = this.parser.isPecReceipt();
			isPecNotificationInCache = true;
		}
		return pecNotification;
	}
	
	/**
	 * Recupero del contenuto XML relativo al file daticert.xml del messaggio (se PEC)
	 * @return
	 * @throws Exception
	 */
	private Document getDatiCertDocument() throws Exception {
		if (!datiCertDocumentInCache) {
			if (isPecMessage() || isPecReceipt()) {
				List<Part> list = getAttachmentPartsByName("daticert.xml");
				if (list != null && !list.isEmpty()) {
					if (list.size() == 1) {
						// Solo 1 daticert trovato, lo restituisco
						datiCertDocument = partToDocument(list.get(0)); 
						datiCertDocumentInCache = true;
					}
					else {
						// Trovati piu' daticert, ritorno quello relativo al documento corrente
						for (Part part : list) {
							if (part != null) {
								try {
									Document tmp = partToDocument(part);
									if (tmp != null) {
										
										// Verifico che effettivamente il daticert.xml sia quello del messaggio ricervuto (e non di un eventuale
										// messaggio inoltrato incluso)
										String identificativo = cleanMessageId(tmp.selectSingleNode("/postacert/dati/identificativo").getText());
										if (identificativo.equals(this.getMessageId())) {
											datiCertDocument = tmp;
											datiCertDocumentInCache = true;
										}
									}
								}
								catch(Exception e) {
									logger.error("ParsedMessage: Unable to extract daticert.xml from current part... " + e.getMessage(), e);
								}
							}
						}
					}
				}
			}
		}
		return datiCertDocument;
	}	
	
	private Document partToDocument(Part part) throws Exception {
		Document doc = null;
		if (part != null) {
			byte[] b = (new PartContentProvider(part)).getContent();
			String content = new String(b, "UTF-8");
			doc = DocumentHelper.parseText(content);
		}
		return doc;
	}
	
	@Deprecated
	public String getMessageIdFromDatiCertPec() throws Exception {
		// FIXME il messageId dovrebbe corrispondere all'elemento 'identificativo'
		return cleanMessageId(getDatiCertDocument().selectSingleNode("/postacert/dati/msgid").getText()); 
	}

	public String getSubjectFromDatiCertPec() throws Exception {
		return getDatiCertDocument().selectSingleNode("/postacert/intestazione/oggetto").getText();
	}	
	
	@SuppressWarnings("unchecked")
	public String getRealToAddressFromDatiCertPec() throws Exception {
		List<Element> l = (List<Element>)getDatiCertDocument().selectNodes("/postacert/intestazione/destinatari");
		if (l.size() == 1)
			return l.get(0).getText();
		Element el = (Element)getDatiCertDocument().selectSingleNode("/postacert/dati/consegna");
		if (el != null)
			return el.getText();
		return null;
	}

	public String getMittenteAddressFromDatiCertPec() throws Exception {
		Element el = (Element)getDatiCertDocument().selectSingleNode("/postacert/intestazione/mittente");
		if (el != null)
			return el.getText();
		return null;
	}	
	
	public List<String> getRelevantMssages() {
		return relevantMssages;
	}

	public void setRelevantMssages(List<String> relevantMssages) {
		this.relevantMssages = relevantMssages;
	}
	
	public void addRelevantMessage(String message) {
		this.relevantMssages.add(message);
	}
	
	public void clearRelevantMessages() {
		this.relevantMssages.clear();
	}
	
	public boolean isReplyOrForward() {
		return this.parser.isReplyOrForward();
	}
	
	/**
	 * Ritorna il primo allegato trovato nel messaggio in base al nome indicato. Se non viene trovato l'allegato richiesto viene restituito NULL.
	 * @param fileName Nome dell'allegato da caricare
	 * @return
	 */
	public Part getFirstAttachmentByName(String fileName) {
		List<Part> list = this.parser.getAttachmentPartsByName(fileName);
		if (list != null && list.size() > 0)
			// TODO metodo sbagliato, non si ha controllo se l'allegato viene estratto dalla mail principale o da una eventuale inoltrata (parte interna all'originale)
			return list.get(0);
		else
			return null;
	}
	
	/**
	 * Ritorna tutti gli allegati trovati nel messaggio in base al nome indicato.
	 * @param fileName
	 * @return
	 */
	public List<Part> getAttachmentPartsByName(String fileName) {
		return this.parser.getAttachmentPartsByName(fileName);
	}
	
	/**
	 * Ritorna i nomi di tutti gli allegati inclusi al messaggio
	 * @return
	 */
	public List<String> getAttachmentsName() {
		List<String> names = new ArrayList<>();
		if (this.getAttachments() != null && !this.getAttachments().isEmpty()) {
			for (MailAttach attach : this.getAttachments()) {
				if (attach != null && attach.getFileName() != null && !attach.getFileName().isEmpty())
					names.add(attach.getFileName());
			}
		}
		return names;
	}
	
}
