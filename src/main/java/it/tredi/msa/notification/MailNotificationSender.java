package it.tredi.msa.notification;

public class MailNotificationSender extends NotificationSender {

	private String host;
	private int port;
	private String protocol;
	private String password;
	private String senderAdress;
	private String admEmailAddress;
	private int socketTimeout;
	private int connectionTimeout;
	private boolean javamailDebug;
	
	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public int getPort() {
		return port;
	}
	
	public void setPort(String port) {
		this.setPort(Integer.parseInt(port));
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

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getSenderAdress() {
		return senderAdress;
	}

	public void setSenderAdress(String senderAdress) {
		this.senderAdress = senderAdress;
	}

	public String getAdmEmailAddress() {
		return admEmailAddress;
	}

	public void setAdmEmailAddress(String admEmailAddress) {
		this.admEmailAddress = admEmailAddress;
	}

	public int getSocketTimeout() {
		return socketTimeout;
	}

	public void setSocketTimeout(String socketTimeout) {
		this.setSocketTimeout(Integer.parseInt(socketTimeout));
	}
	
	public void setSocketTimeout(int socketTimeout) {
		this.socketTimeout = socketTimeout;
	}

	public int getConnectionTimeout() {
		return connectionTimeout;
	}

	public void setConnectionTimeout(String connectionTimeout) {
		this.setConnectionTimeout(Integer.parseInt(connectionTimeout));
	}	
	
	public void setConnectionTimeout(int connectionTimeout) {
		this.connectionTimeout = connectionTimeout;
	}

	public boolean isJavamailDebug() {
		return javamailDebug;
	}

	public void setJavamailDebug(String javamailDebug) {
		this.setJavamailDebug(Boolean.parseBoolean(javamailDebug));
	}	
	
	public void setJavamailDebug(boolean javamailDebug) {
		this.javamailDebug = javamailDebug;
	}

	@Override
	public void notifiyAdmin(String message) {
		// TODO Auto-generated method stub
		
	}

}
