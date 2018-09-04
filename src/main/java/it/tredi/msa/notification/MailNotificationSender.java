package it.tredi.msa.notification;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.tredi.mail.MailClientHelper;
import it.tredi.mail.MailSender;

public class MailNotificationSender extends NotificationSender {
	
	private final static String SUBJECT_ERROR = "Mail Storage Agent Error";
	private static final Logger logger = LogManager.getLogger(MailNotificationSender.class.getName());

	private String host;
	private int port;
	private String protocol;
	private String user;
	private String password;
	private String senderAdress;
	private String senderPersonal;
	private String []admEmailAddresses;
	private int socketTimeout = -1;
	private int connectionTimeout = -1;
	
	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host.trim();
	}

	public int getPort() {
		return port;
	}
	
	public void setPort(String port) {
		port = port.trim();
		if (port.isEmpty())
			port = "-1";
		this.setPort(Integer.parseInt(port));
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getProtocol() {
		return protocol;
	}

	public void setProtocol(String protocol) {
		this.protocol = protocol.trim();
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user.trim();
	}	
	
	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password.trim();
	}

	public String getSenderAdress() {
		return senderAdress;
	}

	public void setSenderAdress(String senderAdress) {
		this.senderAdress = senderAdress.trim();
	}

	public String getSenderPersonal() {
		return senderPersonal;
	}

	public void setSenderPersonal(String senderPersonal) {
		this.senderPersonal = senderPersonal.trim();
	}

	public String []getAdmEmailAddresses() {
		return admEmailAddresses;
	}

	public void setAdmEmailAddresses(String []admEmailAddresses) {
		this.admEmailAddresses = admEmailAddresses;
	}
	
	public void setAdmEmailAddresses(String admEmailAddresses) {
		this.admEmailAddresses = admEmailAddresses.trim().split(";");
	}	

	public int getSocketTimeout() {
		return socketTimeout;
	}

	public void setSocketTimeout(String socketTimeout) {
		socketTimeout = socketTimeout.trim();
		if (socketTimeout.isEmpty())
			socketTimeout = "-1";
		this.setSocketTimeout(Integer.parseInt(socketTimeout));
	}
	
	public void setSocketTimeout(int socketTimeout) {
		this.socketTimeout = socketTimeout;
	}

	public int getConnectionTimeout() {
		return connectionTimeout;
	}

	public void setConnectionTimeout(String connectionTimeout) {
		connectionTimeout = connectionTimeout.trim();
		if (connectionTimeout.isEmpty())
			connectionTimeout = "-1";
		this.setConnectionTimeout(Integer.parseInt(connectionTimeout));
	}	
	
	public void setConnectionTimeout(int connectionTimeout) {
		this.connectionTimeout = connectionTimeout;
	}

	@Override
	public boolean notifyError(String message) throws Exception {
		boolean done = false;
		MailSender mailSender = createMailSender();
		mailSender.connect();
		for (String toAddress: admEmailAddresses) {
			try {
				mailSender.sendMail(senderAdress, senderPersonal, toAddress, SUBJECT_ERROR, message);
				done = true;
			}
			catch (Exception e) {
				logger.warn(String.format(NotificationService.NOTIFICATION_ERROR_MESSAGE_DEST, toAddress, message), e);
			}			
		}
		mailSender.disconnect();
		return done;
	}
	
	public MailSender createMailSender() {
		MailSender mailSender = MailClientHelper.createMailSender(host, port, user, password, protocol);
		if (connectionTimeout != -1)
			mailSender.setConnectionTimeout(connectionTimeout);
		if (socketTimeout != -1)
			mailSender.setSocketTimeout(socketTimeout);
		return mailSender;
	}

}
