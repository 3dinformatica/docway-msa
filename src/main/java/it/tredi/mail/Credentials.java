package it.tredi.mail;

import java.util.Properties;

public class Credentials {
	
	private String username;
	private String password;
	private String smtpHost;
	private String smtpPort;
	private String pop3sHost;
	private String pop3sPort;
	private String imapHost;
	private String imapPort;
	private Properties props;
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public String getSmtpHost() {
		return smtpHost;
	}
	public void setSmtpHost(String smtpHost) {
		this.smtpHost = smtpHost;
	}
	public String getSmtpPort() {
		return smtpPort;
	}
	public void setSmtpPort(String smtpPort) {
		this.smtpPort = smtpPort;
	}
	public String getPop3sHost() {
		return pop3sHost;
	}
	public void setPop3sHost(String pop3sHost) {
		this.pop3sHost = pop3sHost;
	}
	public String getPop3sPort() {
		return pop3sPort;
	}
	public void setPop3sPort(String pop3sPort) {
		this.pop3sPort = pop3sPort;
	}
	public String getImapHost() {
		return imapHost;
	}
	public void setImapHost(String imapHost) {
		this.imapHost = imapHost;
	}
	public String getImapPort() {
		return imapPort;
	}
	public void setImapPort(String imapPort) {
		this.imapPort = imapPort;
	}
	public Properties getProps() {
		return props;
	}
	public void setProps(Properties props) {
		this.props = props;
	}
	
	boolean equals(Credentials cred){
		if((cred.getUsername() != null && !username.equals(cred.getUsername()))
				|| (cred.getPassword() != null && !password.equals(cred.getPassword()))
				|| (cred.getSmtpHost() != null && !smtpHost.equals(cred.getSmtpHost()))
				|| (cred.getSmtpPort() != null && !smtpPort.equals(cred.getSmtpPort()))
				|| (cred.getPop3sHost() != null && !pop3sHost.equals(cred.getPop3sHost()))
				|| (cred.getPop3sPort() != null && !pop3sPort.equals(cred.getPop3sPort()))
				|| (cred.getImapHost() != null && !imapHost.equals(cred.getImapHost()))
				|| (cred.getImapPort() != null && !imapPort.equals(cred.getImapPort()))
				|| (cred.getProps() != null && !props.equals(cred.getProps())))
			return false;
		
		return true;
	}
	
}
