package it.tredi.msa.entity;

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
	
	private int delay = -1; //delay (mailbox manager polling time)
	private StoredMessagePolicy storedMessagePolicy = StoredMessagePolicy.DELETE_FROM_FOLDER;
	private String storedMessageFolderName;
	
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
	
}
