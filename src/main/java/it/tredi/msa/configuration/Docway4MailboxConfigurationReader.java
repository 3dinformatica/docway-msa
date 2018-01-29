package it.tredi.msa.configuration;


public class Docway4MailboxConfigurationReader extends MailboxConfigurationReader {
	
	private String host;
	private int port;
	private String user;
	private String password;
	private String db;
	private String query;
	private String queryInterop;
	private String XPathInfo;
	
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
	
	public String getDb() {
		return db;
	}
	
	public void setDb(String db) {
		this.db = db;
	}
	
	public String getQuery() {
		return query;
	}
	
	public void setQuery(String query) {
		this.query = query;
	}
	
	public String getQueryInterop() {
		return queryInterop;
	}
	
	public void setQueryInterop(String queryInterop) {
		this.queryInterop = queryInterop;
	}
	
	public String getXPathInfo() {
		return XPathInfo;
	}
	
	public void setXPathInfo(String xPathInfo) {
		this.XPathInfo = xPathInfo;
	}
	
	@Override
	public Object getRawData() {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public Docway4MailboxConfigurationReader readConfiguration() {
		// TODO Auto-generated method stub
		return null;
	}
}