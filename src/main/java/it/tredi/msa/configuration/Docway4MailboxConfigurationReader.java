package it.tredi.msa.configuration;

import java.util.ArrayList;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.DESKeySpec;

import org.apache.commons.codec.binary.Base64;
import org.dom4j.Document;
import org.dom4j.Element;

import it.tredi.extraway.ExtrawayClient;
import it.tredi.msa.Services;
import it.tredi.msa.entity.DocwayMailboxconfiguration;
import it.tredi.msa.entity.MailboxConfiguration;

public class Docway4MailboxConfigurationReader extends MailboxConfigurationReader {
	
	private static final String MAILBOX_MSA_CRYPTOR_KEY = "Th3S3cR3tTr3d1M41lb0xKey"; 
	
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

	@SuppressWarnings("unchecked")
	@Override
	public MailboxConfiguration[] readMailboxConfigurations() throws Exception {
		List<MailboxConfiguration> mailboxConfigurations = new ArrayList<MailboxConfiguration>();
		
//TODO - gestire correttamente i try catch		
		
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
	            	
	            	//className
	            	conf.setMailboxManagerClassName("it.tredi.msa.mailboxmanager.Docway4MailboxManager");

	            	//name
	            	conf.setName(casellaEl.attributeValue("nome"));
	            	
	            	//delay
	            	conf.setDelay(Services.getConfigurationService().getMSAConfiguration().getMailboxManagersDelay());
	            	
	            	//host
	            	Element mailboxInEl = casellaEl.element("mailbox_in");
	            	conf.setHost(mailboxInEl.attributeValue("host"));
	            	
	            	//port
	            	conf.setPort(Integer.parseInt(mailboxInEl.attributeValue("port", "-1")));
	            	
	            	//user
	            	conf.setUser(mailboxInEl.attributeValue("login"));
	            	
	            	//password
	            	conf.setPassword(decryptPassword(mailboxInEl.attributeValue("password")));
	            	
	            	//protocol
	            	conf.setProtocol(mailboxInEl.attributeValue("protocol"));
	            	
	            	
	            	
//TODO - RIEMPIRE l'OGGETTO CONF
	            	
	            }				
			}
			
		}
		
		xwClient.disconnect();
		
		//read interoperabilità mailboxes
		
		return mailboxConfigurations.toArray(new MailboxConfiguration[mailboxConfigurations.size()]);
	}
	
	private String decryptPassword(String encPassword) throws Exception {
        SecretKey key = new javax.crypto.spec.SecretKeySpec(new DESKeySpec(MAILBOX_MSA_CRYPTOR_KEY.getBytes()).getKey(), "DES");
        Cipher cipher = Cipher.getInstance("DES");
        cipher.init(Cipher.DECRYPT_MODE, key);
        byte[] passwordB = cipher.doFinal(new Base64().decode(encPassword.getBytes()));
        return new String(passwordB);        
	}
	
}

/**

<casellaPostaElettronica	 	splitByAttachments = "false" oper = "Archiviatore Email" nome = "Prova" uff_oper = "Protocollo" documentModel = "bozze_arrivo" interop = "no" cod_amm = "3DIN" cod_aoo = "BOL" nrecord = "00001322" cod_aoo_segnatura = "" db = "" cod_amm_segnatura = "" protocollaFattura = "false" >
- 	
<gestori_mailbox	>
	
<gestore	 	nome_pers = "Pascale Marvin" matricola = "PI000155" livello = "titolare" />
</gestori_mailbox>
	
<mailbox_in	 	email = "test-archiviatore-xw@libero.it" host = "imapmail.libero.it" login = "test-archiviatore-xw@libero.it" protocol = "imaps" password = "U/dAdqJZ4JwlhmYdWrtBgA==" port = "993" />
	
<responsabile	 	cod_uff = "SI000010" daCopiaConoscenza = "no" daDestinatario = "no" daMittente = "no" matricola = "PI000056" nome_uff = "Servizio archivistico" nome_pers = "Candelora Nicola" cod_ruolo = "" nome_ruolo = "" />
+	
<storia	>
</storia>
	
<mailbox_out	 	email = "test-archiviatore-xw@libero.it" port = "25" login = "test-archiviatore-xw@libero.it" host = "smtp.libero.it" protocol = "smtp" password = "U/dAdqJZ4JwlhmYdWrtBgA==" />
	
<notify	 	uri = "" rpa = "false" httpHost = "" />
	
<tag	 	value = "abilitata" />
- 	
<assegnazione_cc	>
	
<assegnatario	 	intervento = "no" />
</assegnazione_cc>
**/
