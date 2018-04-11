package it.tredi.msa.mailboxmanager.docway;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.Message;

import it.tredi.msa.mailboxmanager.ParsedMessage;

public class DocwayParsedMessage extends ParsedMessage {

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
	
	public String extractNumProtFromOriginalSubject() throws Exception {
		String originalSubject = super.getSubjectFromDatiCertPec();
		return originalSubject.substring(0, originalSubject.indexOf("("));
	}
	
	
	
	public boolean isPecReceiptForFatturaPAbySubject() {
		return false;
//TODO - fare		
	}
	
	public boolean isInteroperablitaMessage() {
//In-Reply-To: CHECK HEADER (!reply or forward)		
//Received:
//In-Reply-To and References
		return false;
//TODO - fare		
	}

	public boolean isFatturaPAMessage() {
		return false;
//TODO - fare		
	}	
	
}
