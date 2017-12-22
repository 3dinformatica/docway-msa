package it.tredi.mail;

import java.util.Properties;

import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.Session;

public abstract class MailClient {
	
	private final static long SOCKET_TIMEOUT =  10000; //10 secs
	private final static long CONNECTION_TIMEOUT =  60000; //60 sec

	protected MailAccount account;
	protected Session session;
	protected Properties sessionProperties;
	protected boolean authRequired;
	protected long socketTimeout;
	protected long connectionTimeout;
	
	public void connect() throws NoSuchProviderException, MessagingException {
		sessionProperties = buildSessionProperties();
		session = authRequired? Session.getInstance(sessionProperties, new Authenticator(account.getUserName(), account.getPassword())) : Session.getInstance(sessionProperties);
	}
	
	public MailClient init(MailAccount account) {
		socketTimeout = SOCKET_TIMEOUT; 
		connectionTimeout = CONNECTION_TIMEOUT;		
		this.account = account;
		authRequired = account.getUserName() != null;
		return this;
	}	
	
	protected abstract Properties buildSessionProperties();
	public abstract void disconnect() throws MessagingException;

	public void testConnection() throws MessagingException {
		try {
			connect();	
		}
		finally {
			try {
				disconnect();
			}
			catch (Exception e) {
//TODO - log warning				
				;
			}
		}
	}	

	public long getSocketTimeout() {
		return socketTimeout;
	}

	public void setSocketTimeout(long socketTimeout) {
		this.socketTimeout = socketTimeout;
	}

	public long getConnectionTimeout() {
		return connectionTimeout;
	}

	public void setConnectionTimeout(long connectionTimeout) {
		this.connectionTimeout = connectionTimeout;
	}

	public Session getSession() {
		return session;
	}

}



