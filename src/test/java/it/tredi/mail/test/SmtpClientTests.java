package it.tredi.mail.test;

import org.junit.Test;
import org.junit.runners.MethodSorters;

import it.tredi.mail.MailClientHelper;
import it.tredi.mail.MailSender;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.junit.Assert;
import org.junit.FixMethodOrder;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class SmtpClientTests {
	
	private static String fromAddress;
	private static String fromPersonal;
	private static String toAddress;
	private static String host;
	private static int port;
	private static String userName;
	private static String password;
	private static String protocol;
	
	static {
		Properties properties = new Properties();
		try {
			properties.load(ImapClientTests.class.getResourceAsStream("smtp.properties"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		host = properties.getProperty("smtp.host");
		port = Integer.parseInt(properties.getProperty("smtp.port"));
		userName = properties.getProperty("smtp.userName");
		password = properties.getProperty("smtp.password");
		protocol = properties.getProperty("smtp.protocol");
		fromAddress = properties.getProperty("fromAddress");		
		fromPersonal = properties.getProperty("fromPersonal");
		toAddress = properties.getProperty("toAddress");
	}
	
	//MailSender Creation Tests

	@Test(expected=java.lang.IllegalArgumentException.class)
	public void test_001_mailSenderCreationOnIllegalProtocol() throws MessagingException {
		MailClientHelper.createMailSender("", -1, "", "", "foo");
	}	

	@Test
	public void test_002_mailSenderCreationOnLegalProtocols() throws MessagingException {
		String []legalProtocols = {"smtp", "smtp-tls", "smtp-tls-ssl"};
		for (String protocol: legalProtocols)
			MailClientHelper.createMailSender("", -1, "", "", protocol);
	}
	
	//Connection Test
	
	@Test(expected=com.sun.mail.util.MailConnectException.class)
	public void test_003_connectionOnFailure() throws MessagingException {
		MailSender mailSender = MailClientHelper.createMailSender("", -1, "", "", "smtp");
		mailSender.testConnection();
	}

	@Test
	public void test_004_connection() throws MessagingException, IOException {
		MailSender mailSender = MailClientHelper.createMailSender(host, port, userName, password, protocol);
		mailSender.testConnection();
	}
	
	//SendaMail Tests
	
	@Test
	public void test_005_sendMail() throws MessagingException, IOException {
		MailSender mailSender = MailClientHelper.createMailSender(host, port, userName, password, protocol);
		mailSender.connect();
		mailSender.sendMail(fromAddress, fromPersonal, toAddress, "simple mail with plain text body", "plain text");
		mailSender.disconnect();
	}

	@Test
	public void test_006_sendMailWithAttachments() throws MessagingException, IOException {
		MailSender mailSender = MailClientHelper.createMailSender(host, port, userName, password, protocol);
		mailSender.connect();
		
		String ccAddresses[] = {toAddress};
		
        MimeBodyPart htmlBody = MailClientHelper.createMimeBodyPart(("<html>This should be an <b style=\"font-size:300%;\">html</b> mail body. This email should have 2 attachments"
        		+ "<ul><li>readMe.txt</li><li>readMe.pdf</li></ul></html>").getBytes(), "text/html", null, false);        
		
        //attachment 1
        MimeBodyPart attach1 = MailClientHelper.createAttachmentBodyPart("simple text attachment".getBytes(), "text/plain", "readMe.txt");

        //attachment 2
        MimeBodyPart attach2 = MailClientHelper.createAttachmentBodyPart(new File(ImapClientTests.class.getResource("readMe.pdf").getFile()), "application/pdf", "readMe.pdf");
        
        MimeBodyPart []mimeBodyParts = {htmlBody, attach1, attach2};
        
		mailSender.sendMail(fromAddress, fromPersonal, toAddress, ccAddresses, "mail with attachments(2) and CC(1)", mimeBodyParts);
		mailSender.disconnect();
	}	
	
	@Test
	public void test_007_sendCustomMail() throws MessagingException, IOException {
		MailSender mailSender = MailClientHelper.createMailSender(host, port, userName, password, protocol);
		mailSender.connect();
		MimeMessage message = new MimeMessage(mailSender.getSession());
		
		//from
        message.setFrom(new InternetAddress(fromAddress, fromPersonal));
        
        //to
        InternetAddress[] addressL = { new InternetAddress(toAddress) };
        message.setRecipients(Message.RecipientType.TO, addressL);

        //subject
        message.setSubject("custom mail with text-and-html body and html attachment");

        //body
        Multipart parts = new MimeMultipart();
        message.setContent(parts);
        
        //text
        MimeBodyPart textBody = new MimeBodyPart();        
        textBody.setText("plain text");
        parts.addBodyPart(textBody);
        
        //html
        MimeBodyPart htmlPart = MailClientHelper.createMimeBodyPart("<html><h1>HTML PART</h1></html>".getBytes(), "text/html", null, false);
        parts.addBodyPart(htmlPart);

        //html attachment
        MimeBodyPart htmlAttachment = MailClientHelper.createAttachmentBodyPart("<html><h1>HTML ATTACHMENT</h1></html>".getBytes(), "text/html", "attachment.html");
        parts.addBodyPart(htmlAttachment);        
        
        //send the message
        message.saveChanges();
		mailSender.sendMail(message);
		mailSender.disconnect();
	}	
	
}
