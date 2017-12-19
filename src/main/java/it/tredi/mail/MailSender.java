package it.tredi.mail;

import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

public class MailSender extends MailClient {
	private Transport transport;
	
	public MailSender() {
	}

	@Override
	public void connect() throws MessagingException {
		super.connect();
        transport = session.getTransport(sessionProperties.getProperty("mail.transport.protocol"));
        if (account.getPort() != -1)
        	transport.connect(account.getHost(), account.getPort(), account.getUserName(), account.getPassword());
        else
        	transport.connect(account.getHost(), account.getUserName(), account.getPassword());
	}

	@SuppressWarnings("incomplete-switch")
	@Override
	protected Properties buildSessionProperties() {
		Properties props = new Properties(System.getProperties());
		
		switch (account.getProtocol()) {
			case SMTP:
				props.put("mail.transport.protocol", "smtp");
				break;
			case SMTP_TLS:
				props.put("mail.transport.protocol", "smtps");
				props.put("mail.smtp.starttls.enable","true");
				break;
			case SMTP_TLS_SSL:
				props.put("mail.transport.protocol", "smtps");
				props.put("mail.smtp.starttls.enable","true");
	            props.put("mail.smtp.socketFactory.port", account.getPort());
	            props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
	            props.put("mail.smtp.socketFactory.fallback", "false");				
				break;
		}
		
		props.put("mail.smtp.host", account.getHost());
		if (account.getPort() != -1)
			props.put("mail.smtp.port", Integer.toString(account.getPort()));
		props.put("mail.smtp.auth", authRequired? "true" : "false");
		props.put("mail.smtp.connectiontimeout", Long.toString(connectionTimeout));
		props.put("mail.smtp.timeout", Long.toString(socketTimeout));

		return props;
	}	
	
	@Override
	public void disconnect() throws MessagingException {
		transport.close();
		transport = null;
	}
	
	public Transport getTransport() {
		return transport;
	}

	public void sendMail(String fromAddress, String fromPersonal, String toAddress, String subject, String text) throws UnsupportedEncodingException, MessagingException {
        MimeBodyPart textBody = new MimeBodyPart();        
        textBody.setText(text);
        MimeBodyPart []mimeBodyParts = {textBody};

        //send the message
        sendMail(fromAddress, fromPersonal, toAddress, null, subject, mimeBodyParts);
	}

	public void sendMail(String fromAddress, String fromPersonal, String toAddress, String ccAddresses[], String subject, MimeBodyPart []mimeBodyParts) throws UnsupportedEncodingException, MessagingException {
        MimeMessage message = new MimeMessage(session);

        //from
        message.setFrom(new InternetAddress(fromAddress, fromPersonal));

        //to
        InternetAddress[] toAddressArr = { new InternetAddress(toAddress) };
        message.setRecipients(Message.RecipientType.TO, toAddressArr);
        
        //cc
        if (ccAddresses != null) {
            InternetAddress[] ccAddressArr = new InternetAddress[ccAddresses.length];
            int index = 0;
            for (String ccAddress:ccAddresses)
            	ccAddressArr[index++] = new InternetAddress(ccAddress);
            message.setRecipients(Message.RecipientType.CC, ccAddressArr);        	
        }

        //subject
        message.setSubject(subject);

        //body
        Multipart parts = new MimeMultipart();
        for (MimeBodyPart part:mimeBodyParts)
        	parts.addBodyPart(part);
        message.setContent(parts);

        //send the message
        sendMail(message);
	}	
	
	public void sendMail(MimeMessage message) throws UnsupportedEncodingException, MessagingException {
        //date header
        message.setSentDate(new Date());
        
        message.saveChanges();
        transport.sendMessage(message, message.getAllRecipients()); 
	}
	
}
