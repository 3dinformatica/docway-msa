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
	private String address;
	private int mailserverSocketTimeout;
	private int mailserverConnectionTimeout;
	private int delay = -1;
	
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
	
	public String getAddress() {
		return address;
	}
	
	public void setAddress(String address) {
		this.address = address;
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

	
	/*
	-removeFromServer
	-gestire spostamento IMAP
	notifyRemainingError

	javamailDebug=false	
	
	*/
	
}
