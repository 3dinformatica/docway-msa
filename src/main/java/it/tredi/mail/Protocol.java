package it.tredi.mail;

public enum Protocol {
	
	POP3,
	POP3S,
	IMAP,
	IMAPS,
	SMTP,
	SMTP_TLS,
	SMTP_TLS_SSL;
	
	public static Protocol parse(String s) {
		s = s.toUpperCase();
		s = s.replaceAll("-", "_");
		return Protocol.valueOf(s);
		
		//TODO - inserire controllo se protocollo non trovato
	}
	
}
