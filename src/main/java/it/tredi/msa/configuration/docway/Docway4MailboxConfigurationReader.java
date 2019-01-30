package it.tredi.msa.configuration.docway;

import java.util.ArrayList;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.DESKeySpec;

import it.tredi.msa.mailboxmanager.docway.fatturapa.conf.OggettoParseMode;
import org.apache.commons.codec.binary.Base64;
import org.dom4j.Document;
import org.dom4j.Element;

import it.highwaytech.db.QueryResult;
import it.tredi.extraway.ExtrawayClient;
import it.tredi.msa.Services;
import it.tredi.msa.configuration.MailboxConfiguration;
import it.tredi.msa.configuration.MailboxConfigurationReader;
import it.tredi.msa.mailboxmanager.StoredMessagePolicy;
import it.tredi.utils.properties.PropertiesReader;

public class Docway4MailboxConfigurationReader extends MailboxConfigurationReader {
	
	private static final String MAILBOX_MSA_CRYPTOR_KEY = "Th3S3cR3tTr3d1M41lb0xKey"; 
	
	public final static String DOCWAY4MAILBOXMANAGER_XW_HOST_PROPERTY = "docway4mailboxmanager.xw.host";
	public final static String DOCWAY4MAILBOXMANAGER_XW_PORT_PROPERTY = "docway4mailboxmanager.xw.port";
	public final static String DOCWAY4MAILBOXMANAGER_XW_USER_PROPERTY = "docway4mailboxmanager.xw.user";
	public final static String DOCWAY4MAILBOXMANAGER_XW_PASSWORD_PROPERTY = "docway4mailboxmanager.xw.password";
	public final static String DOCWAY4MAILBOXMANAGER_STORE_EML = "docway4mailboxmanager.store-eml.enable";
	public final static String DOCWAY4MAILBOXMANAGER_MOVE_STORED_MESSAGES = "docway4mailboxmanager.move-stored-messages.enable";
	public final static String DOCWAY4MAILBOXMANAGER_MOVE_STORED_MESSAGES_FOLDER = "docway4mailboxmanager.move-stored-messages.folder-name";
	public final static String DOCWAY4MAILBOXMANAGER_XW_LOCK_OP_ATTEMPTS = "docway4mailboxmanager.xw.lock-op.attempts";
	public final static String DOCWAY4MAILBOXMANAGER_XW_LOCK_OP_DELAY = "docway4mailboxmanager.xw.lock-op.delay";
	public final static String DOCWAY4MAILBOXMANAGER_MAIL_READER_SOCKET_TIMEOUT = "docway4mailboxmanager.mail-reader.socket-timeout";
	public final static String DOCWAY4MAILBOXMANAGER_MAIL_READER_CONNECTION_TIMEOUT = "docway4mailboxmanager.mail-reader.connection-timeout";
	public final static String DOCWAY4MAILBOXMANAGER_NOTIFICATION_EMAILS = "docway4mailboxmanager.notification-emails.enable";
	public final static String DOCWAY4MAILBOXMANAGER_CREATE_SINGLE_DOC_BY_MESSAGE_ID = "docway4mailboxmanager.create-single-doc-by-message-id";
	public final static String DOCWAY4MAILBOXMANAGER_INTEROP_PA_PROTOCOLLA_SEGNATURA = "docway4mailboxmanager.interop-pa.protocolla-segnatura.enable";
	public final static String DOCWAY4MAILBOXMANAGER_INTEROP_PA_MEZZO_TRAMISSIONE_SEGNATURA = "docway4mailboxmanager.interop-pa.mezzo-trasmissione-segnatura";
	public final static String DOCWAY4MAILBOXMANAGER_INTEROP_PA_TIPOLOGIA_SEGNATURA = "docway4mailboxmanager.interop-pa.tipologia-segnatura";
	public final static String DOCWAY4MAILBOXMANAGER_FTR_PA_ENABLE = "docway4mailboxmanager.ftr-pa.enable";
	public final static String DOCWAY4MAILBOXMANAGER_FTR_PA_SDI_DOMAIN_ADDRESS = "docway4mailboxmanager.ftr-pa.sdi-domain-address";
	public final static String DOCWAY4MAILBOXMANAGER_FTR_PA_REPERTORIO = "docway4mailboxmanager.ftr-pa.repertorio";
	public final static String DOCWAY4MAILBOXMANAGER_FTR_PA_REPERTORIO_COD = "docway4mailboxmanager.ftr-pa.repertorio.cod";
	public final static String DOCWAY4MAILBOXMANAGER_FTR_PA_CLASSIF = "docway4mailboxmanager.ftr-pa.classif";
	public final static String DOCWAY4MAILBOXMANAGER_FTR_PA_CLASSIF_COD = "docway4mailboxmanager.ftr-pa.classif.cod";
	public final static String DOCWAY4MAILBOXMANAGER_FTR_PA_VOCE_INDICE = "docway4mailboxmanager.ftr-pa.voce-indice";
	public final static String DOCWAY4MAILBOXMANAGER_FTR_PA_TEMPLATE_OGGETTO = "docway4mailboxmanager.ftr-pa.template-oggetto";
	public final static String DOCWAY4MAILBOXMANAGER_FTR_PA_OGGETTO_PARSE_MODE = "docway4mailboxmanager.ftr-pa.oggetto-parse-mode";
	public final static String DOCWAY4MAILBOXMANAGER_PEC_IGNORE_STANDARD_ORPHAN_RECEIPTS = "docway4mailboxmanager.pec.ignore-standard-orphan-receipts";
	public final static String DOCWAY4MAILBOXMANAGER_PEC_ORPHAN_RECEIPTS_AS_VARIE = "docway4mailboxmanager.pec.orphan-receipts-as-varie";
	public final static String DOCWAY4MAILBOXMANAGER_MAIL_SENDER_SOCKET_TIMEOUT = "docway4mailboxmanager.mail-sender.socket-timeout";
	public final static String DOCWAY4MAILBOXMANAGER_MAIL_SENDER_CONNECTION_TIMEOUT = "docway4mailboxmanager.mail-sender.connection-timeout";
	
	public final static String DOCWAY4MAILBOXMANAGER_MAIL_READER_MIME_ADDRESS_STRICT = "docway4mailboxmanager.mail-reader.mail-mime-address-strict";
	public final static String DOCWAY4MAILBOXMANAGER_MAIL_READER_MIME_ALLOW_UTF8 = "docway4mailboxmanager.mail-reader.mail-mime-allowutf8";
	
	private String host;
	private int port;
	private String user;
	private String password;
	private String db;
	private String query;
	private String queryPec;
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

	public String getQueryPec() {
		return queryPec;
	}

	public void setQueryPec(String queryPec) {
		this.queryPec = queryPec;
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
		ExtrawayClient xwClient = null;
		try {
			xwClient = new ExtrawayClient(host, port, db, user, password);
			xwClient.connect();
			
			//read standard mailboxes
			mailboxConfigurations.addAll(readMailboxConfigurations(false, query, xwClient));
	
			//read Pec mailboxes
			mailboxConfigurations.addAll(readMailboxConfigurations(true, queryPec, xwClient));
			
		}
		catch (Exception e) {
			throw (e);
		}
		finally {
			if (xwClient != null)
				xwClient.disconnect();
		}
		
		return mailboxConfigurations.toArray(new MailboxConfiguration[mailboxConfigurations.size()]);
	}
	
	private List<MailboxConfiguration> readMailboxConfigurations(boolean isPec, String query, ExtrawayClient xwClient) throws Exception {
		List<MailboxConfiguration> mailboxConfigurations = new ArrayList<MailboxConfiguration>();
		int count = xwClient.search(query);
		QueryResult qr = xwClient.getQueryResult();
		for (int i=0; i<count; i++) { //iterate xw selection
			xwClient.setQueryResult(qr); //fix - inner documentModel search changes current query result
			Document xmlDocument = xwClient.loadDocByQueryResult(i);
			
			//every doc in the selection could contain more mailboxes info (see xPathInfo)
			String []xpaths = XPathInfo.split(";");
			for (String xpath:xpaths) { //iterate xpaths
	            @SuppressWarnings("unchecked")
				List<Element> elsL = xmlDocument.selectNodes(xpath + "[./mailbox_in/@host!='']");
	            for (Element casellaEl:elsL) { //for each mailbox relative to the current xpath
	            	Docway4MailboxConfiguration conf = createDocway4MailboxConfigurationByConfig(casellaEl, isPec);
	            	mailboxConfigurations.add(conf);
	            	
	        		//parse documentModel
	        		if (xwClient.search("[docmodelname]=" + casellaEl.attributeValue("documentModel")) > 0) {
	        			Document dmDocument = xwClient.loadDocByQueryResult(0);
	        			parseDocumentModel(conf, dmDocument);
	        		}
	        		else
	        			throw new Exception("Document model non trovato: " + casellaEl.attributeValue("documentModel"));
	        		
	            }				
			}
		}	
		return mailboxConfigurations;
	}
	
	private Docway4MailboxConfiguration createDocway4MailboxConfigurationByConfig(Element casellaEl, boolean isPec) throws Exception {
    	Docway4MailboxConfiguration conf = new Docway4MailboxConfiguration();
    	
    	//isPec
    	conf.setPec(isPec);
    	
    	//className
    	conf.setMailboxManagerClassName("it.tredi.msa.mailboxmanager.docway.Docway4MailboxManager");

    	//name
    	conf.setName(casellaEl.attributeValue("nome"));
    	
    	//delay
    	conf.setDelay(Services.getConfigurationService().getMSAConfiguration().getMailboxManagersDelay());
    	
    	/* *************************** mailbox-in ****************************************** */
    	Element mailboxInEl = casellaEl.element("mailbox_in");
    	
    	//host
    	conf.setHost(mailboxInEl.attributeValue("host"));
    	
    	//port
    	String port = mailboxInEl.attributeValue("port", "-1");
    	conf.setPort(Integer.parseInt(port.isEmpty()?"-1":port)); 
    	
    	//user
    	conf.setUser(mailboxInEl.attributeValue("login"));
    	
    	//password
    	conf.setPassword(decryptPassword(mailboxInEl.attributeValue("password")));
    	
    	//protocol
    	conf.setProtocol(mailboxInEl.attributeValue("protocol"));
    	
    	//email
    	conf.setEmail(mailboxInEl.attributeValue("email"));
    	
    	//folderName
    	conf.setFolderName(mailboxInEl.attributeValue("folder", "INBOX"));
    	/* ********************************************************************************* */
    	
    	/* *************************** mailbox-out ***************************************** */
    	Element mailboxOutEl = casellaEl.element("mailbox_out");
    	if (mailboxOutEl != null && !mailboxOutEl.attributeValue("host", "").isEmpty()) {
        	//host
        	conf.setSmtpHost(mailboxOutEl.attributeValue("host"));
        	
        	//port
        	port = mailboxOutEl.attributeValue("port", "-1");
        	conf.setSmtpPort(Integer.parseInt(port.isEmpty()?"-1":port));
        	
        	//user
        	conf.setSmtpUser(mailboxOutEl.attributeValue("login"));
        	
        	//password
        	conf.setSmtpPassword(decryptPassword(mailboxOutEl.attributeValue("password")));
        	
        	//protocol
        	conf.setSmtpProtocol(mailboxOutEl.attributeValue("protocol"));
        	
        	//email
        	conf.setSmtpEmail(mailboxOutEl.attributeValue("email"));
        	
        	//timeout
        	PropertiesReader propertiesReader = (PropertiesReader)Services.getConfigurationService().getMSAConfiguration().getRawData();
        	conf.setSmtpSocketTimeout(propertiesReader.getIntProperty(DOCWAY4MAILBOXMANAGER_MAIL_SENDER_SOCKET_TIMEOUT, -1));
        	conf.setSmtpConnectionTimeout(propertiesReader.getIntProperty(DOCWAY4MAILBOXMANAGER_MAIL_SENDER_CONNECTION_TIMEOUT, -1));
    	}
		/* ******************************************************************************** */
    	
    	//cod_amm_aoo
    	conf.setCodAmm(casellaEl.attributeValue("cod_amm"));
    	conf.setCodAoo(casellaEl.attributeValue("cod_aoo"));
    	conf.setCodAmmAoo(conf.getCodAmm() + conf.getCodAoo());
    	
    	//cod_amm_aoo segnatura
    	conf.setCodAmmInteropPA(casellaEl.attributeValue("cod_amm_segnatura", conf.getCodAmm()));
    	conf.setCodAooInteropPA(casellaEl.attributeValue("cod_aoo_segnatura", conf.getCodAoo()));
    	
    	//xwDb
		conf.setXwDb(casellaEl.attributeValue("db"));
    	
		//mail di notifica
		Element notifyEl = casellaEl.element("notify");
		if (notifyEl != null) {
			conf.setNotifyRPA(Boolean.parseBoolean(notifyEl.attributeValue("rpa", "false")));
			conf.setNotifyCC(Boolean.parseBoolean(notifyEl.attributeValue("cc", "false")));
			conf.setNotificationAppHost(notifyEl.attributeValue("httpHost", ""));
			conf.setNotificationAppHost1(notifyEl.attributeValue("httpHost1", ""));
			String uri = notifyEl.attributeValue("uri", "");
			if (!uri.isEmpty())
				uri = "// " + uri;
			conf.setNotificationAppUri(uri);
		}
		else {
			conf.setNotifyRPA(false);
			conf.setNotifyCC(false);			
		}		
		
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
    	if (casellaEl.element("assegnazione_cc") != null) {
        	@SuppressWarnings("unchecked")
    		List<Element> ccElsL = casellaEl.element("assegnazione_cc").elements("assegnatario");
        	for (Element ccEl:ccElsL) {
        		AssegnatarioMailboxConfiguration cc = createAssegnatarioByConfig("CC", ccEl);
        		if (!cc.getCodPersona().isEmpty() || !cc.getCodUff().isEmpty() || !cc.getCodRuolo().isEmpty()) //purtroppo nell'xml se non ci sono CC compare un assegnatario vuoto
        			ccS.add(cc);
        	}
    	}
    	conf.setAssegnatariCC(ccS);
    	
    	//default xw params (xwHost, xwPort, xwUser, xwPassword)
    	PropertiesReader propertiesReader = (PropertiesReader)Services.getConfigurationService().getMSAConfiguration().getRawData();
    	conf.setXwHost(propertiesReader.getProperty(DOCWAY4MAILBOXMANAGER_XW_HOST_PROPERTY, "localhost"));
    	conf.setXwPort(propertiesReader.getIntProperty(DOCWAY4MAILBOXMANAGER_XW_PORT_PROPERTY, -1));
    	conf.setXwUser(propertiesReader.getProperty(DOCWAY4MAILBOXMANAGER_XW_USER_PROPERTY, "xw.msa"));
    	conf.setXwPassword(propertiesReader.getProperty(DOCWAY4MAILBOXMANAGER_XW_PASSWORD_PROPERTY, ""));
    	conf.setAclDb(db);
    	conf.setXwLockOpAttempts(propertiesReader.getIntProperty(DOCWAY4MAILBOXMANAGER_XW_LOCK_OP_ATTEMPTS, Docway4MailboxConfiguration.DEFAULT_XW_LOCK_OP_ATTEMPTS));
    	conf.setXwLockOpDelay(propertiesReader.getLongProperty(DOCWAY4MAILBOXMANAGER_XW_LOCK_OP_DELAY, Docway4MailboxConfiguration.DEFAULT_XW_LOCK_OP_DELAY));
    	
    	//other docway4 global params
    	conf.setStoreEml(propertiesReader.getBooleanProperty(DOCWAY4MAILBOXMANAGER_STORE_EML, false));
    	if (propertiesReader.getBooleanProperty(DOCWAY4MAILBOXMANAGER_MOVE_STORED_MESSAGES, false)) { //move stored message policy enabled
    		conf.setStoredMessagePolicy(StoredMessagePolicy.MOVE_TO_FOLDER);
    		conf.setStoredMessageFolderName(propertiesReader.getProperty(DOCWAY4MAILBOXMANAGER_MOVE_STORED_MESSAGES_FOLDER, ""));
    	}
    	conf.setMailserverSocketTimeout(propertiesReader.getIntProperty(DOCWAY4MAILBOXMANAGER_MAIL_READER_SOCKET_TIMEOUT, -1));
    	conf.setMailserverConnectionTimeout(propertiesReader.getIntProperty(DOCWAY4MAILBOXMANAGER_MAIL_READER_CONNECTION_TIMEOUT, -1));
    	conf.setNotificationEnabled(propertiesReader.getBooleanProperty(DOCWAY4MAILBOXMANAGER_NOTIFICATION_EMAILS, false));
    	conf.setCreateSingleDocByMessageId(propertiesReader.getBooleanProperty(DOCWAY4MAILBOXMANAGER_CREATE_SINGLE_DOC_BY_MESSAGE_ID, false));
    	conf.setProtocollaSegnatura(propertiesReader.getBooleanProperty(DOCWAY4MAILBOXMANAGER_INTEROP_PA_PROTOCOLLA_SEGNATURA, false));
    	conf.setMezzoTrasmissioneSegnatura(propertiesReader.getProperty(DOCWAY4MAILBOXMANAGER_INTEROP_PA_MEZZO_TRAMISSIONE_SEGNATURA, ""));
    	conf.setTipologiaSegnatura(propertiesReader.getProperty(DOCWAY4MAILBOXMANAGER_INTEROP_PA_TIPOLOGIA_SEGNATURA, ""));
    	conf.setEnableFatturePA(propertiesReader.getBooleanProperty(DOCWAY4MAILBOXMANAGER_FTR_PA_ENABLE, false));
    	conf.setSdiDomainAddress(propertiesReader.getProperty(DOCWAY4MAILBOXMANAGER_FTR_PA_SDI_DOMAIN_ADDRESS, ""));
    	conf.setRepertorioFtrPA(propertiesReader.getProperty(DOCWAY4MAILBOXMANAGER_FTR_PA_REPERTORIO, Docway4MailboxConfiguration.DEFAULT_FTR_PA_REPERTORIO));
    	conf.setRepertorioCodFtrPA(propertiesReader.getProperty(DOCWAY4MAILBOXMANAGER_FTR_PA_REPERTORIO_COD, Docway4MailboxConfiguration.DEFAULT_FTR_PA_REPERTORIO_COD));
    	conf.setClassifFtrPA(propertiesReader.getProperty(DOCWAY4MAILBOXMANAGER_FTR_PA_CLASSIF, Docway4MailboxConfiguration.DEFAULT_FTR_PA_CLASSIF));
    	conf.setClassifCodFtrPA(propertiesReader.getProperty(DOCWAY4MAILBOXMANAGER_FTR_PA_CLASSIF_COD, Docway4MailboxConfiguration.DEFAULT_FTR_PA_CLASSIF_COD));
    	conf.setVoceIndiceFtrPA(propertiesReader.getProperty(DOCWAY4MAILBOXMANAGER_FTR_PA_VOCE_INDICE, ""));
    	conf.setOggettoParseMode(OggettoParseMode.getParseMode(propertiesReader.getProperty(DOCWAY4MAILBOXMANAGER_FTR_PA_OGGETTO_PARSE_MODE, "predefinito")));
    	conf.setTemplateOggetto(propertiesReader.getProperty(DOCWAY4MAILBOXMANAGER_FTR_PA_TEMPLATE_OGGETTO, ""));
    	
    	// mbernardini 04/09/2018 : protocollazione della fattura recuperato dalla configurazione della casella e non dal file di properties di MSA
    	conf.setProtocollaFattura(casellaEl.attributeValue("protocollaFattura", "false").equalsIgnoreCase("true"));
    	//conf.setProtocollaFattura(propertiesReader.getBooleanProperty(DOCWAY4MAILBOXMANAGER_FTR_PA_PROTOCOLLA_FATTURA, true));
    	
    	conf.setIgnoreStandardOrphanPecReceipts(propertiesReader.getBooleanProperty(DOCWAY4MAILBOXMANAGER_PEC_IGNORE_STANDARD_ORPHAN_RECEIPTS, true));
    	// mbernardini 18/01/2019 : salvataggio di ricevute PEC orfane come doc non protocollati
    	conf.setOrphanPecReceiptsAsVarie(propertiesReader.getBooleanProperty(DOCWAY4MAILBOXMANAGER_PEC_ORPHAN_RECEIPTS_AS_VARIE, true));
    	
    	// mbernardini 29/01/2019 : parametri aggiuntivi per javamail di configurazione del parsing dell'header del messaggio
    	conf.setMailMimeAddressStrict(propertiesReader.getBooleanProperty(DOCWAY4MAILBOXMANAGER_MAIL_READER_MIME_ADDRESS_STRICT, true));
    	conf.setMailMimeAddressStrict(propertiesReader.getBooleanProperty(DOCWAY4MAILBOXMANAGER_MAIL_READER_MIME_ALLOW_UTF8, false));
    	
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
		if (conf.getXwDb() == null || conf.getXwDb().isEmpty()) //ha la precedenza la configurazione sulla casella di posta a quella del document model
			conf.setXwDb(dmDocument.getRootElement().attributeValue("db"));
    	
		//tipo doc
		String tipoDoc = ((Element)dmDocument.selectSingleNode("/documentModel/item[@xpath='doc/@tipo']")).attributeValue("value");
		conf.setTipoDoc(tipoDoc);
		
		//bozza
		Element itemEl = (Element)dmDocument.selectSingleNode("/documentModel/item[@xpath='doc/@bozza']");
		String bozzaS =  (itemEl == null)? "no" : itemEl.attributeValue("value", "no");
		conf.setBozza(bozzaS.equalsIgnoreCase("si"));

		//data prot
		conf.setCurrentDate(((Element)dmDocument.selectSingleNode("/documentModel/item[@xpath='doc/@data_prot']")).attributeValue("value").equals("getDate()")? true : false);
		
		//anno
		conf.setCurrentYear(((Element)dmDocument.selectSingleNode("/documentModel/item[@xpath='doc/@anno']")).attributeValue("value").equals("getYear()")? true : false);
		
		//tipologia
		itemEl = (Element)dmDocument.selectSingleNode("/documentModel/item[@xpath='doc/tipologia/@cod']");
		if (itemEl != null)
			conf.setTipologia(itemEl.attributeValue("value"));
		
		//numero protocollo
		itemEl = (Element)dmDocument.selectSingleNode("/documentModel/item[@xpath='doc/@num_prot']");
		conf.setNumProt(itemEl == null? "" : itemEl.attributeValue("value", ""));
		
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
		
		//mail di notifica
		if (conf.getNotificationAppHost() == null || conf.getNotificationAppHost().isEmpty()) { //ha la precedenza la configurazione sulla casella di posta a quella del document model
			Element notifyEl = dmDocument.getRootElement().element("notify");
			if (notifyEl != null) {
				conf.setNotifyRPA(Boolean.parseBoolean(notifyEl.attributeValue("rpa", "false")));
				conf.setNotifyCC(Boolean.parseBoolean(notifyEl.attributeValue("cc", "false")));
				conf.setNotificationAppHost(notifyEl.attributeValue("httpHost", ""));
				conf.setNotificationAppHost1(notifyEl.attributeValue("httpHost1", ""));
				String uri = notifyEl.attributeValue("uri", "");
				if (!uri.isEmpty())
					uri = "// " + uri;
				conf.setNotificationAppUri(uri);
			}
			else {
				conf.setNotifyRPA(false);
				conf.setNotifyCC(false);			
			}			
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





