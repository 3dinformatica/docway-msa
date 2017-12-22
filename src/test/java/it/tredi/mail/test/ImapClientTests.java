package it.tredi.mail.test;

import org.junit.Test;
import org.junit.runners.MethodSorters;

import it.tredi.mail.MailClientHelper;
import it.tredi.mail.MessageUtils;
import it.tredi.mail.MailReader;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Store;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;

import org.junit.Assert;
import org.junit.FixMethodOrder;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ImapClientTests {
	
	private static String host;
	private static int port;
	private static String userName;
	private static String password;
	private static String protocol;
	private static String folderName;
	
	static {
		Properties properties = new Properties();
		try {
			properties.load(ImapClientTests.class.getResourceAsStream("imap.properties"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		host = properties.getProperty("imap.host");
		port = Integer.parseInt(properties.getProperty("imap.port"));
		userName = properties.getProperty("imap.userName");
		password = properties.getProperty("imap.password");
		protocol = properties.getProperty("imap.protocol");
		folderName = properties.getProperty("imap.folder.source");
	}
	
	//init environment
	
	@Test
	public void test_000_initTestEnvironment() throws MessagingException, IOException {
		MailReader mailReader = MailClientHelper.createMailReader(host, port, userName, password, protocol);
		mailReader.connect();

		Store store = mailReader.getStore();
		Folder folder = store.getFolder("JUNIT_FOLDER");
		if (folder.exists())
			folder.delete(true);

		mailReader.disconnect();
	}	
	
	//MailReader Creation Tests

	@Test(expected=java.lang.IllegalArgumentException.class)
	public void test_001_mailReaderCreationOnIllegalProtocol() throws MessagingException {
		MailClientHelper.createMailReader("", -1, "", "", "foo");
	}	

	@Test
	public void test_002_mailReaderCreationOnLegalProtocols() throws MessagingException {
		String []legalProtocols = {"imap", "imaps"};
		for (String protocol: legalProtocols)
			MailClientHelper.createMailReader("", -1, "", "", protocol);
	}
	
	//Connection Test
	
	@Test(expected=com.sun.mail.util.MailConnectException.class)
	public void test_003_connectionOnFailure() throws MessagingException {
		MailReader mailReader = MailClientHelper.createMailReader("", -1, "", "", "imap");
		mailReader.testConnection();
	}

	@Test
	public void test_004_connection() throws MessagingException, IOException {
		MailReader mailReader = MailClientHelper.createMailReader(host, port, userName, password, protocol);
		mailReader.testConnection();
	}
	
	//Imap Tests
	
	@Test
	public void test_005_openInboxFolder() throws MessagingException, IOException {
		MailReader mailReader = MailClientHelper.createMailReader(host, port, userName, password, protocol);
		mailReader.connect();
		mailReader.openInboxFolder();
		mailReader.closeFolder();
		mailReader.disconnect();
	}	
	
	@Test
	public void test_006_openFolder() throws MessagingException, IOException {
		MailReader mailReader = MailClientHelper.createMailReader(host, port, userName, password, protocol);
		mailReader.connect();
		mailReader.openFolder(folderName);
		mailReader.closeFolder();
		mailReader.disconnect();
	}	

	@Test(expected=javax.mail.MessagingException.class)
	public void test_007_openFolderOnFailure() throws MessagingException, IOException {
		MailReader mailReader = null;
		try {
			mailReader = MailClientHelper.createMailReader(host, port, userName, password, protocol);
			mailReader.connect();
			mailReader.openFolder("!foo!");
			mailReader.closeFolder();
				
		}
		finally{
			mailReader.disconnect();
		}
	}	
	
	@Test
	public void test_008_createFolder() throws MessagingException, IOException {
		MailReader mailReader = MailClientHelper.createMailReader(host, port, userName, password, protocol);
		mailReader.connect();
		boolean created = mailReader.createFolder("JUNIT_FOLDER");
		mailReader.disconnect();
		Assert.assertTrue(created);
	}
	
	@Test
	public void test_009_copyMessageToFolder() throws MessagingException, IOException {
		MailReader mailReader = MailClientHelper.createMailReader(host, port, userName, password, protocol);
		mailReader.connect();
		mailReader.openInboxFolder();
		Message []messages = mailReader.getMessages();
		
		//code for retrieving current message count
		Folder _destFolder = mailReader.getStore().getFolder("JUNIT_FOLDER");
		_destFolder.open(Folder.READ_ONLY);
		int count = _destFolder.getMessageCount();
		_destFolder.close();
		
		mailReader.copyMessageToFolder(messages[0], "JUNIT_FOLDER");
		
		//code for retrieving post-copy message count
		_destFolder.open(Folder.READ_ONLY);
		int count1 = _destFolder.getMessageCount();
		_destFolder.close();		
		
		mailReader.closeFolder();
		mailReader.disconnect();
		
		Assert.assertEquals(count+1, count1);
	}

	@Test
	public void test_010_deleteMessage() throws MessagingException, IOException {
		MailReader mailReader = MailClientHelper.createMailReader(host, port, userName, password, protocol);
		mailReader.connect();
		mailReader.openFolder("JUNIT_FOLDER");

		//code for retrieving current message count
		int count = mailReader.getFolder().getMessageCount();
		
		mailReader.deleteMessage(mailReader.getMessages()[0]);
		mailReader.closeFolder();
		
		//code for retrieving post-delete message count
		Folder _destFolder = mailReader.getStore().getFolder("JUNIT_FOLDER");
		_destFolder.open(Folder.READ_ONLY);
		int count1 = _destFolder.getMessageCount();
		_destFolder.close();		
		
		mailReader.disconnect();
		
		Assert.assertEquals(count-1, count1);
	}	
	
	@Test
	public void test_011_deleteFolder() throws MessagingException, IOException {
		MailReader mailReader = MailClientHelper.createMailReader(host, port, userName, password, protocol);
		mailReader.connect();
		boolean created = mailReader.deleteFolder("JUNIT_FOLDER");
		mailReader.disconnect();
		Assert.assertTrue(created);
	}	
	
	
	/*
	@Test
	public void testGetMessages() throws MessagingException, IOException {
		MailReader mailReader = MailClientHelper.createMailReader(host, port, userName, password, protocol);
		mailReader.connect();
		mailReader.openFolder(folderName);
		MessageUtils []messages = mailReader.getMessages();
		mailReader.closeFolder();
		mailReader.disconnect();
	}	*/
	
}
