package it.tredi.msa.configuration;

import java.util.ArrayList;
import java.util.List;

import org.dom4j.Document;
import org.dom4j.Element;

import it.tredi.extraway.ExtrawayClient;
import it.tredi.msa.entity.DocwayMailboxconfiguration;
import it.tredi.msa.entity.MailboxConfiguration;

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
	public MailboxConfiguration[] readMailboxConfigurations() throws Exception {
		List<MailboxConfiguration> mailboxConfigurations = new ArrayList<MailboxConfiguration>();
		
		//connect to extraway server
		ExtrawayClient xwClient = new ExtrawayClient(host, port, db, user, password);
		xwClient.connect();
		
		//read standard mailboxes
		int count = xwClient.search(query);
		for (int i=0; i<count; i++) { //iterate xw selection
			Document xmlDocument = xwClient.loadDocByQueryResult(i);
			
			//every doc in the selection could contain more mailboxes info (see xPathInfo)
			String []xpaths = XPathInfo.split(";");
			for (String xpath:xpaths) { //iterate xpaths
	            List<Element> elsL = xmlDocument.selectNodes(xpath + "[./mailbox_in/@host!='']");
	            for (Element casellaEl:elsL) { //for each mailbox relative to the current xpath
	            	DocwayMailboxconfiguration conf = new DocwayMailboxconfiguration();
	            	mailboxConfigurations.add(conf);
	            	conf.setName(casellaEl.attributeValue("nome"));

//TODO - RIEMPIRE l'OGGETTO CONF
	            	
	            }				
			}
			
		}
		
		xwClient.disconnect();
		
		//read interoperabilità mailboxes
		
		return mailboxConfigurations.toArray(new MailboxConfiguration[mailboxConfigurations.size()]);
	}
	
}
