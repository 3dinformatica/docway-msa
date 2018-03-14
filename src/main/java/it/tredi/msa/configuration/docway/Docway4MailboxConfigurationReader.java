package it.tredi.msa.configuration.docway;

import java.util.ArrayList;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.DESKeySpec;

import org.apache.commons.codec.binary.Base64;
import org.dom4j.Document;
import org.dom4j.Element;

import it.highwaytech.db.QueryResult;
import it.tredi.extraway.ExtrawayClient;
import it.tredi.msa.Services;
import it.tredi.msa.configuration.MailboxConfigurationReader;
import it.tredi.msa.entity.MailboxConfiguration;
import it.tredi.msa.entity.docway.AssegnatarioMailboxConfiguration;
import it.tredi.msa.entity.docway.Docway4MailboxConfiguration;

import it.tredi.utils.properties.PropertiesReader;

public class Docway4MailboxConfigurationReader extends MailboxConfigurationReader {
	
	private static final String MAILBOX_MSA_CRYPTOR_KEY = "Th3S3cR3tTr3d1M41lb0xKey"; 
	
	public final static String DOCWAY4MAILBOXMANAGER_XW_HOST_PROPERTY = "docway4mailboxmanager.xw.host";
	public final static String DOCWAY4MAILBOXMANAGER_XW_PORT_PROPERTY = "docway4mailboxmanager.xw.port";
	public final static String DOCWAY4MAILBOXMANAGER_XW_USER_PROPERTY = "docway4mailboxmanager.xw.user";
	public final static String DOCWAY4MAILBOXMANAGER_XW_PASSWORD_PROPERTY = "docway4mailboxmanager.xw.password";
	public final static String DOCWAY4MAILBOXMANAGER_STORE_EML = "docway4mailboxmanager.storeEml";
	
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
		
//TODO - gestire correttamente i try catch (per eXtraWay Client)
		
		//connect to extraway server
		ExtrawayClient xwClient = new ExtrawayClient(host, port, db, user, password);
		xwClient.connect();
		
		//read standard mailboxes
		int count = xwClient.search(query);
		QueryResult qr = xwClient.getQueryResult();
		for (int i=0; i<count; i++) { //iterate xw selection
			xwClient.setQueryResult(qr); //fix - inner documentModel search changes current query result
			Document xmlDocument = xwClient.loadDocByQueryResult(i);
			
			//every doc in the selection could contain more mailboxes info (see xPathInfo)
			String []xpaths = XPathInfo.split(";");
			for (String xpath:xpaths) { //iterate xpaths
	            List<Element> elsL = xmlDocument.selectNodes(xpath + "[./mailbox_in/@host!='']");
	            for (Element casellaEl:elsL) { //for each mailbox relative to the current xpath
	            	Docway4MailboxConfiguration conf = createDocway4MailboxConfigurationByConfig(casellaEl);
	            	mailboxConfigurations.add(conf);
	            	
	        		//parse documentModel
	        		if (xwClient.search("[docmodelname]=" + casellaEl.attributeValue("documentModel")) > 0) {
	        			Document dmDocument = xwClient.loadDocByQueryResult(0);
	        			parseDocumentModel(conf, dmDocument);	
	        		}
//TODO - cosa fare se il documentModel non viene trovato???
	        		
	            }				
			}
			
		}
		
		xwClient.disconnect();
		
		//read interoperabilità mailboxes
//TODO		
		
		return mailboxConfigurations.toArray(new MailboxConfiguration[mailboxConfigurations.size()]);
	}
	
	private Docway4MailboxConfiguration createDocway4MailboxConfigurationByConfig(Element casellaEl) throws Exception {
    	Docway4MailboxConfiguration conf = new Docway4MailboxConfiguration();
    	
    	//className
    	conf.setMailboxManagerClassName("it.tredi.msa.mailboxmanager.docway.Docway4MailboxManager");

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
    	
    	//email
    	conf.setEmail(mailboxInEl.attributeValue("email"));
    	
    	//cod_amm_aoo
    	conf.setCodAmmAoo(casellaEl.attributeValue("cod_amm") + casellaEl.attributeValue("cod_aoo"));
    	
    	//oper, uff_oper
    	conf.setOper(casellaEl.attributeValue("oper"));
    	conf.setUffOper(casellaEl.attributeValue("uff_oper"));

    	//responsabile
    	Element responsabileEl = casellaEl.element("responsabile");
    	if (responsabileEl != null) {
    		conf.setResponsabile(createAssegnatarioByConfig("RPA", responsabileEl));
    		conf.setDaDestinatario(responsabileEl.attributeValue("daDestinatario", "no").equalsIgnoreCase("si"));
    		conf.setDaMittente(responsabileEl.attributeValue("daMittente", "no").equalsIgnoreCase("si"));
    		conf.setDaCopiaConoscenza(responsabileEl.attributeValue("daCopiaConoscenza", "no").equalsIgnoreCase("si"));
    	}
    	
    	//assegnatari cc
    	List<AssegnatarioMailboxConfiguration> ccS = new ArrayList<AssegnatarioMailboxConfiguration>();
    	List<Element> ccElsL = casellaEl.element("assegnazione_cc").elements("assegnatario");
    	for (Element ccEl:ccElsL) {
    		AssegnatarioMailboxConfiguration cc = createAssegnatarioByConfig("CC", ccEl);
    		if (!cc.getCodPersona().isEmpty() || !cc.getCodUff().isEmpty() || !cc.getCodRuolo().isEmpty()) //purtroppo nell'xml se non ci sono CC compare un assegnatario vuoto
    			ccS.add(cc);
    	}
    	conf.setAssegnatariCC(ccS);
    	
    	//default xw params (xwHost, xwPort, xwUser, xwPassword)
    	PropertiesReader propertiesReader = (PropertiesReader)Services.getConfigurationService().getMSAConfiguration().getRawData();
    	conf.setXwHost(propertiesReader.getProperty(DOCWAY4MAILBOXMANAGER_XW_HOST_PROPERTY, "localhost"));
    	conf.setXwPort(propertiesReader.getIntProperty(DOCWAY4MAILBOXMANAGER_XW_PORT_PROPERTY, -1));
    	conf.setXwUser(propertiesReader.getProperty(DOCWAY4MAILBOXMANAGER_XW_USER_PROPERTY, "lettore"));
    	conf.setXwPassword(propertiesReader.getProperty(DOCWAY4MAILBOXMANAGER_XW_PASSWORD_PROPERTY, "reader"));
    	conf.setAclDb(db);
    	
    	//other docway4 global params
    	conf.setStoreEml(propertiesReader.getBooleanProperty(DOCWAY4MAILBOXMANAGER_STORE_EML, false));
    	
//TODO - COMPLETARE	
		return conf;
	}
	
	private String decryptPassword(String encPassword) throws Exception {
        SecretKey key = new javax.crypto.spec.SecretKeySpec(new DESKeySpec(MAILBOX_MSA_CRYPTOR_KEY.getBytes()).getKey(), "DES");
        Cipher cipher = Cipher.getInstance("DES");
        cipher.init(Cipher.DECRYPT_MODE, key);
        byte[] passwordB = cipher.doFinal(new Base64().decode(encPassword.getBytes()));
        return new String(passwordB);        
	}
	
	public void parseDocumentModel(Docway4MailboxConfiguration conf, Document dmDocument) {
		
    	//xwDb
    	conf.setXwDb(dmDocument.getRootElement().attributeValue("db"));
    	
		//tipo doc
		String tipoDoc = ((Element)dmDocument.selectSingleNode("/documentModel/item[@xpath='doc/@tipo']")).attributeValue("value");
		conf.setTipoDoc(tipoDoc);
		
		//bozza
		String bozzaS = ((Element)dmDocument.selectSingleNode("/documentModel/item[@xpath='doc/@bozza']")).attributeValue("value");
		conf.setBozza(bozzaS.equalsIgnoreCase("si"));

		//data prot
		conf.setCurrentDate(((Element)dmDocument.selectSingleNode("/documentModel/item[@xpath='doc/@data_prot']")).attributeValue("value").equals("getDate()")? true : false);
		
		//anno
		conf.setCurrentYear(((Element)dmDocument.selectSingleNode("/documentModel/item[@xpath='doc/@anno']")).attributeValue("value").equals("getYear()")? true : false);
		
		//tipologia
		Element itemEl = (Element)dmDocument.selectSingleNode("/documentModel/item[@xpath='doc/tipologia/@cod']");
		if (itemEl != null)
			conf.setTipologia(itemEl.attributeValue("value"));
		
		//mezzo trasmissione
		itemEl = (Element)dmDocument.selectSingleNode("/documentModel/item[@xpath='doc/mezzo_trasmissione/@cod']");
		if (itemEl != null)
			conf.setMezzoTrasmissione(itemEl.attributeValue("value"));

		//classificazione
		itemEl = (Element)dmDocument.selectSingleNode("/documentModel/item[@xpath='doc/classif/@cod']");
		if (itemEl != null) {
			conf.setClassifCod(itemEl.attributeValue("value"));
			String classif = ((Element)dmDocument.selectSingleNode("/documentModel/item[@xpath='doc/classif']")).attributeValue("value");
			conf.setClassif(classif);
		}
		
		//note automatiche
		if (dmDocument.selectSingleNode("/documentModel/item[@xpath='doc/note'][@value='From: ']") != null && dmDocument.selectSingleNode("/documentModel/item[@xpath='doc/note'][@value='getMailBody(TEXT)']") != null)
			conf.setNoteAutomatiche(true);
		else
			conf.setNoteAutomatiche(false);

		//voce_indice
		itemEl = (Element)dmDocument.selectSingleNode("/documentModel/item[@xpath='doc/voce_indice']");
		if (itemEl != null)
			conf.setVoceIndice(itemEl.attributeValue("value"));
		
		//repertorio
		itemEl = (Element)dmDocument.selectSingleNode("/documentModel/item[@xpath='doc/repertorio/@cod']");
		if (itemEl != null) {
			conf.setRepertorioCod(itemEl.attributeValue("value"));
			String repertorio = ((Element)dmDocument.selectSingleNode("/documentModel/item[@xpath='doc/repertorio']")).attributeValue("value");
			conf.setRepertorio(repertorio);
		}		
		
//TODO - continuare ad analizzare il documentModel
	}
	
	private AssegnatarioMailboxConfiguration createAssegnatarioByConfig(String tipo, Element assegnatarioEl) {
		AssegnatarioMailboxConfiguration assegnatario = new AssegnatarioMailboxConfiguration();
		assegnatario.setTipo(tipo);
		assegnatario.setNomePersona(assegnatarioEl.attributeValue("nome_pers", ""));
		assegnatario.setCodPersona(assegnatarioEl.attributeValue("matricola", ""));
		assegnatario.setNomeUff(assegnatarioEl.attributeValue("nome_uff", ""));
		assegnatario.setCodUff(assegnatarioEl.attributeValue("cod_uff", ""));
		assegnatario.setNomeRuolo(assegnatarioEl.attributeValue("nome_ruolo", ""));
		assegnatario.setCodRuolo(assegnatarioEl.attributeValue("cod_ruolo", ""));
		assegnatario.setIntervento(assegnatarioEl.attributeValue("intervento", "no").equalsIgnoreCase("si"));
		return assegnatario;
	}

}

/**

<casellaPostaElettronica	 	splitByAttachments = "false" oper = "Archiviatore Email" nome = "Prova" uff_oper = "Protocollo" documentModel = "bozze_arrivo" interop = "no" cod_amm = "3DIN" cod_aoo = "BOL" nrecord = "00001322" cod_aoo_segnatura = "" db = "" cod_amm_segnatura = "" protocollaFattura = "false" >
- 	
<gestori_mailbox	>
	
<gestore	 	nome_pers = "Pascale Marvin" matricola = "PI000155" livello = "titolare" />
</gestori_mailbox>
	
<mailbox_in	 	email = "test-archiviatore-xw@libero.it" host = "imapmail.libero.it" login = "test-archiviatore-xw@libero.it" protocol = "imaps" password = "U/dAdqJZ4JwlhmYdWrtBgA==" port = "993" />
	
<responsabile	 	cod_uff = "SI000010" daCopiaConoscenza = "no" daDestinatario = "no" daMittente = "no" matricola = "PI000056" 
nome_uff = "Servizio archivistico" nome_pers = "Candelora Nicola" cod_ruolo = "" nome_ruolo = "" />
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



<documentModel    db="xdocwaydoc" name="bozze_arrivo" nrecord="90000003">
  <notify cc="true" httpHost="http://localhost:8080" rpa="true" uri="/DocWay4/docway/loadtitles.pf"/>
  <item value="arrivo" xpath="doc/@tipo"/>
  <item value="si" xpath="doc/@bozza"/>
  <item value="getXPathValue(/casellaPostaElettronica/@cod_amm)" xpath="doc/@cod_amm_aoo"/>
  <item value="getXPathValue(/casellaPostaElettronica/@cod_aoo)" xpath="doc/@cod_amm_aoo"/>
  <item value="." xpath="doc/@nrecord"/>
  <item value="" xpath="doc/@anno"/>
  <item value="getDate()" xpath="doc/@data_prot"/>
  <item value="" xpath="doc/@num_prot"/>
  <item value="no" xpath="doc/@annullato"/>
  <item value="getSubject()" xpath="doc/oggetto"/>
  <item value="preserve" xpath="doc/oggetto/@xml:space"/>
  <item value="getSubject()" xpath="doc/postit"/>
  <item value="TEST" xpath="doc/postit/@cod_operatore"/>
  <item value="getDate()" xpath="doc/postit/@data"/>
  <item value="TEST TSET" xpath="doc/postit/@operatore"/>
  <item value="E-mail" xpath="doc/tipologia/@cod"/>
  <item value="addSenderFromACLLookup(/doc/rif_esterni/rif)" xpath=""/>
  <item value="addAllegatoForEmailAttachs()" xpath=""/>
  <item value="addStoriaCreazione()" xpath=""/>
  <item value="addRPA()" xpath=""/>
  <item value="addCC()" xpath=""/>
  <item value="From: " xpath="doc/note"/>
  <item value="getFromName()" xpath="doc/note"/>
  <item value="newLine()" xpath="doc/note"/>
  <item value="To: " xpath="doc/note"/>
  <item value="getTo()" xpath="doc/note"/>
  <item value="newLine()" xpath="doc/note"/>
  <item value="Cc: " xpath="doc/note"/>
  <item value="getCc()" xpath="doc/note"/>
  <item value="newLine()" xpath="doc/note"/>
  <item value="Sent: " xpath="doc/note"/>
  <item value="getHeader(Date)" xpath="doc/note"/>
  <item value="newLine()" xpath="doc/note"/>
  <item value="Subject: " xpath="doc/note"/>
  <item value="getSubject()" xpath="doc/note"/>
  <item value="newLine()" xpath="doc/note"/>
  <item value="newLine()" xpath="doc/note"/>
  <item value="getMailBody(TEXT)" xpath="doc/note"/>
  <item value="preserve" xpath="doc/note/@xml:space"/>
  <attach_item value="addAndUploadEmailBodyAttach(/doc/files,TEXT,testo email,.txt)" xpath=""/>
  <attach_item value="addAndUploadEmailBodyAttach(/doc/files,HTML,testo email html,.html)" xpath=""/>
  <attach_item value="addAndUploadEmailAttachFiles(/doc/files)" xpath=""/>
  <attach_item value="addAndUploadEmailAttachImages(/doc/immagini)" xpath=""/>
  <attach_item value="computeAndAddFootprint(/doc/impronta)" xpath=""/>
  <attach_item value="assignChkin()" xpath=""/><?xw-meta Dbms="ExtraWay" DbmsVer="25.9.4" OrgNam="3D Informatica" OrgVer="22.3.1.6" Classif="1.0" ManGest="1.0" ManTec="0.0.4" InsUser="unknown" InsTime="20151023170419" ModUser="admin" ModTime="20171220150550"?>
<?xw-crc key32=f1759431-50910104?>

</documentModel>
*
**/





