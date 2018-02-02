package it.tredi.msa.configuration;

import java.util.ArrayList;
import java.util.List;

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
	public MailboxConfiguration[] readMailboxConfigurations() {
		List<MailboxConfiguration> mailboxConfigurations = new ArrayList<MailboxConfiguration>();
		
		//read normal mailboxes

		/**
		 * 
    private static Vector<String> getMbmsFromDoc(String xml, String xPath) throws Exception {
        Vector<String> v = new Vector<String>();
        XMLDocumento document = new XMLDocumento(xml);

        Vector<?> v1 = it.tredi.utils.string.Text.split(xPath, ",");
        for (int j = 0; j < v1.size(); j++) { //for each xpath specified in property file
            //sstagni - 28 Feb 2006 - escludo le mailbox vuote
            List<?> l = document.selectNodes(v1.get(j) + "[./mailbox_in/@host!='']");
            for (int i = 0; i < l.size(); i++) { //for each mailbox relative to the current xpath
            	Element l3 = (Element)l.get(i);
                v.add(l3.asXML());
                logger.debug("new MBM: " + l3.asXML());
            }

        }

        return v;
    }
		 * 
		 */
		
		//read interoperabilità mailboxes
		
		return (MailboxConfiguration[]) mailboxConfigurations.toArray();
	}
	
}
