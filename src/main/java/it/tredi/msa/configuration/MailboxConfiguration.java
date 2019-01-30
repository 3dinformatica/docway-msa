package it.tredi.msa.configuration;

import it.tredi.msa.mailboxmanager.StoredMessagePolicy;

public abstract class MailboxConfiguration {

	private String name;
	private String mailboxManagerClassName;
	
	//pop3/imap parameters
	private String host;
	private int port;
	private String protocol;
	private String user;
	private String password;
	private int mailserverSocketTimeout = -1;
	private int mailserverConnectionTimeout = -1;
	private String folderName;
	
	// The mail.mime.address.strict session property controls the parsing of address headers. By default, strict parsing of 
	// address headers is done. If this property is set to "false", strict parsing is not done and many illegal addresses that 
	// sometimes occur in real messages are allowed. See the InternetAddress class for details.
	private boolean mailMimeAddressStrict = true;
	// If set to "true", UTF-8 strings are allowed in message headers, e.g., in addresses. This should only be set 
	// if the mail server also supports UTF-8. 
	private boolean mailMimeAllowutf8 = false;
	
	boolean isPec;
	
	private int delay = -1; //delay (mailbox manager polling time)
	private StoredMessagePolicy storedMessagePolicy = StoredMessagePolicy.DELETE_FROM_FOLDER;
	private String storedMessageFolderName;
	
	private final static String DEFAULT_FOLDER_NAME = "INBOX";
	private final static String DEFAULT_STORED_MESSAGE_FOLDER_NAME = "MSA_STORED_MESSAGES";
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getMailboxManagerClassName() {
		return mailboxManagerClassName;
	}
	
	public void setMailboxManagerClassName(String mailboxManagerClassName) {
		this.mailboxManagerClassName = mailboxManagerClassName;
	}
	
	public String getHost() {
		return host;
	}
	
	public void setHost(String host) {
		this.host = host;
	}
	
	public int getPort() {
		return port;
	}
	
	public void setPort(int port) {
		this.port = port;
	}
	
	public String getProtocol() {
		return protocol;
	}
	
	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}
	
	public String getUser() {
		return user;
	}
	
	public void setUser(String user) {
		this.user = user;
	}
	
	public String getPassword() {
		return password;
	}
	
	public void setPassword(String password) {
		this.password = password;
	}
	
	public int getMailserverSocketTimeout() {
		return mailserverSocketTimeout;
	}
	
	public void setMailserverSocketTimeout(int mailserverSocketTimeout) {
		this.mailserverSocketTimeout = mailserverSocketTimeout;
	}
	
	public int getMailserverConnectionTimeout() {
		return mailserverConnectionTimeout;
	}
	
	public void setMailserverConnectionTimeout(int mailserverConnectionTimeout) {
		this.mailserverConnectionTimeout = mailserverConnectionTimeout;
	}

	public String getFolderName() {
		return (folderName == null || folderName.isEmpty())? DEFAULT_FOLDER_NAME : folderName;
	}

	public void setFolderName(String folderName) {
		this.folderName = folderName;
	}

	public int getDelay() {
		return delay;
	}

	public void setDelay(int delay) {
		this.delay = delay;
	}

	public StoredMessagePolicy getStoredMessagePolicy() {
		return storedMessagePolicy;
	}

	public void setStoredMessagePolicy(StoredMessagePolicy storedMessagePolicy) {
		this.storedMessagePolicy = storedMessagePolicy;
	}

	public String getStoredMessageFolderName() {
		return (storedMessageFolderName == null || storedMessageFolderName.isEmpty())? DEFAULT_STORED_MESSAGE_FOLDER_NAME : storedMessageFolderName;
	}

	public void setStoredMessageFolderName(String storedMessageFolderName) {
		this.storedMessageFolderName = storedMessageFolderName;
	}

	public boolean isPec() {
		return isPec;
	}

	public void setPec(boolean isPec) {
		this.isPec = isPec;
	}
	
	public boolean isMailMimeAddressStrict() {
		return mailMimeAddressStrict;
	}

	public void setMailMimeAddressStrict(boolean mailMimeAddressStrict) {
		this.mailMimeAddressStrict = mailMimeAddressStrict;
	}

	public boolean isMailMimeAllowutf8() {
		return mailMimeAllowutf8;
	}

	public void setMailMimeAllowutf8(boolean mailMimeAllowutf8) {
		this.mailMimeAllowutf8 = mailMimeAllowutf8;
	}
	
}
