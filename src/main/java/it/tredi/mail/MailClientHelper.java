package it.tredi.mail;

import java.io.File;
import java.io.IOException;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;
import javax.mail.util.ByteArrayDataSource;

public class MailClientHelper {
	
	public static MailReader createMailReader(String host, int port, String protocol) {
		return createMailReader(host, port, null, null, protocol);
	}		
	
	public static MailReader createMailReader(String host, int port, String userName, String password, String protocol) {
		return (MailReader) new MailReader().init(createMailAccount(host, port, userName, password, protocol));
	}	
	
	public static MailSender createMailSender(String host, int port, String protocol) {
		return createMailSender(host, port, null, null, protocol);
	}		
	
	public static MailSender createMailSender(String host, int port, String userName, String password, String protocol) {
		return (MailSender) new MailSender().init(createMailAccount(host, port, userName, password, protocol));
	}
	
	private static MailAccount createMailAccount(String host, int port, String userName, String password, String protocol) {
		return new MailAccount(host, port, userName, password, Protocol.parse(protocol));
	}

	public static MimeBodyPart createAttachmentBodyPart(byte []content, String mimeType, String fileName) throws MessagingException, IOException {
        return createMimeBodyPart(content, mimeType, fileName, true);
	}		
	
	public static MimeBodyPart createAttachmentBodyPart(File file, String mimeType, String fileName) throws MessagingException, IOException {
        return createMimeBodyPart(new FileDataSource(file), mimeType, fileName, true);		
	}	
	
	public static MimeBodyPart createMimeBodyPart(byte []content, String mimeType, String fileName, boolean attachmentDisposition) throws MessagingException, IOException {
		return createMimeBodyPart(new ByteArrayDataSource(content, mimeType), mimeType, fileName, attachmentDisposition);			
	}
	
	public static MimeBodyPart createMimeBodyPart(DataSource dataSource, String mimeType, String fileName, boolean attachmentDisposition) throws MessagingException, IOException {
        MimeBodyPart mimeBodyPart = new MimeBodyPart();
        mimeBodyPart.setDataHandler(new DataHandler(dataSource));
        if (fileName != null && !fileName.isEmpty())
        	mimeBodyPart.setHeader("Content-Type", mimeType + "; name=" + fileName);
        if (attachmentDisposition)
        	mimeBodyPart.setHeader("Content-Disposition","attachment");
        return mimeBodyPart;		
	}	
	
}
