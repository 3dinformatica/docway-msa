package it.tredi.mail;

public class MailAccount {
	
	private String host;
	private int port;
	private String username;
	private String password;
	private Protocol protocol;
	
	public MailAccount(String host, int port, String userName, String password, Protocol protocol) {
		this.host = host;
		this.port = port;
		this.username = userName;
		this.password = password;
		this.protocol = protocol;
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

	public String getUserName() {
		return username;
	}

	public void setUserName(String userName) {
		this.username = userName;
	}

	public String getPassword() {
		if (password == null && username != null)
			return "";
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public Protocol getProtocol() {
		return protocol;
	}

	public void setProtocol(Protocol protocol) {
		this.protocol = protocol;
	}
	
}
