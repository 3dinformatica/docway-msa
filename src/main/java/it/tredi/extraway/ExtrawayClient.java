package it.tredi.extraway;

import java.sql.SQLException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import it.highwaytech.broker.Broker;
import it.highwaytech.broker.XMLCommand;
import it.highwaytech.db.Doc;
import it.highwaytech.db.QueryResult;
import it.tredi.msa.Utils;

/**
 * Client utilizzato per la conessione ad eXtraWay. Utilizzato sull'implementazione di MSA per DocWay4
 */
public class ExtrawayClient {
	
	private static final Logger logger = LogManager.getLogger(ExtrawayClient.class.getName());

	private Broker broker;
	private String host;
	private int port;
	private String db;
	private String user;
	private String password;
	private int connId;
	private QueryResult queryResult;
	private final String ENCODING = "UTF-8";
	private static final String XW_NAMESPACE = "http://www.3di.it/ns/xw-200303121136";
	private String theLock;
	
	/**
	 * Costruttore. Init del Broker
	 * @param host Host del server eXtraWay
	 * @param port Porta del server eXtraWay
	 * @param db Nome dell'archivio eXtraWay sul quale operare
	 * @param user Eventuale username da utilizzare per l'accesso ad eXtraWay
	 * @param password Eventuale password da utilizzare per l'accesso ad eXtraWay
	 */
	public ExtrawayClient(String host, int port, String db, String user, String password) {
		this.broker = new Broker();
		this.host = host;
		this.port = port;
		this.user = user;
		this.password = password;
		this.db = db;
		this.connId = -1;
	}

	/**
	 * Connessione con il server eXtraWay
	 * @throws SQLException
	 */
	public void connect() throws SQLException  {
		disconnect();
		connId = broker.acquireConnection(host, port, db, user, password, -1);
        try {
        	if (logger.isDebugEnabled())
	    		logger.debug("ExtrawayClient: Connect by connId " + connId);
        	
            broker.Connect(connId, host, port, db, -1, user, password, "", "");
        }
        catch (SQLException e) {
            disconnect();
            throw e;
        }			
	}
	
	/**
	 * Disconnessione dal server eXtraWay
	 * @throws SQLException
	 */
	public void disconnect() throws SQLException {
		if (connId != -1) {
			if (logger.isDebugEnabled())
	    		logger.debug("ExtrawayClient: Release connection " + connId);
			
			broker.releaseConnection(connId);
			connId = -1;			
		}
	}
	
	/**
	 * Ricerca di documenti XML in base alla query specificata. Viene restituito il numero di risultati ottenuto dalla ricerca
	 * @param query Query in formato eXtraWay da eseguire 
	 * @return Numero di risultati ottenuto in base ai filtri specificati
	 * @throws SQLException
	 */
	public int search(String query) throws SQLException {
		return this.search(query, null, "", 0, -1);
	}
	
	/**
	 * Ricerca di documenti XML in base alla query specificata. Viene restituito il numero di risultati ottenuto dalla ricerca
	 * @param query Query in formato eXtraWay da eseguire
	 * @param selToRefine Eventuale identificativo di selezione da raffinare
	 * @param sort Eventuale criterio di ordinamento
	 * @param hwQOpts
	 * @param adj
	 * @return Numero di risultati ottenuto in base ai filtri specificati
	 * @throws SQLException
	 */
    public int search(String query, String selToRefine, String sort, int hwQOpts, int adj) throws SQLException {
        hwQOpts |= (sort != null && sort.length() > 0 ? it.highwaytech.broker.ServerCommand.find_SORT : 0);
        if (selToRefine != null && !selToRefine.isEmpty()) {
            query += " AND [?SEL]=\"" + selToRefine + "\"";
        }
        
        if (logger.isInfoEnabled())
    		logger.info("ExtrawayClient: Execute Query = " + query);
        
        this.queryResult = broker.find(connId, db, query, sort, hwQOpts, adj ,0, null);
        return queryResult.elements;
    }	
	
    /**
     * Caricamento di un documento XML in base alla posizione su una selezione
     * @param position Posizione all'interno della selezione della ricerca
     * @return
     * @throws Exception
     */
    public Document loadDocByQueryResult(int position) throws Exception {
    	return loadDoc(position, true);
    }
    
    /**
     * Caricamento di un documento XML in base al numero fisico
     * @param physdoc Numero fisico del documento da caricare
     * @return
     * @throws Exception
     */
    public Document loadDocByPhysdoc(int physdoc) throws Exception {
    	return loadDoc(physdoc, false);
    }
    
    /**
     * Caricamento di un documento XML in base alla posizione su una selezione o al proprio numero fisico
     * @param docNum Posizione nella selezione o numero fisico del documento
     * @param fromQueryResult true se occorre caricare il doc in base alla posizione nella selezione, false in caso di caricamento tramite numero fisico
     * @return
     * @throws Exception
     */
    private Document loadDoc(int docNum, boolean fromQueryResult) throws Exception {
    	Doc doc = null;
        if (fromQueryResult) {
        	if (logger.isInfoEnabled())
        		logger.info("ExtrawayClient: Load document by physDoc = " + docNum);
        	
            doc = broker.getDoc(connId, db, queryResult, docNum, it.highwaytech.broker.ServerCommand.subcmd_NONE, "");
        }
        else {
        	if (logger.isInfoEnabled())
        		logger.info("ExtrawayClient: Load document from QueryResult. Position = " + docNum);
        	
            doc = broker.getDoc(connId, db, docNum, 0);
        }
        Document document = DocumentHelper.parseText(doc.XML());
        return document;
    }

    /**
     * Ritorna le informazioni sulla selezione di una ricerca
     * @return
     */
	public QueryResult getQueryResult() {
		return queryResult;
	}

	/**
	 * Imposta le informazioni sulla selezione di una ricerca
	 * @param queryResult
	 */
	public void setQueryResult(QueryResult queryResult) {
		this.queryResult = queryResult;
	}
	
	/**
	 * Salvataggio di un nuovo documento XML
	 * @param xmlDocument
	 * @return
	 * @throws Exception
	 */
	public int saveNewDocument(Document xmlDocument) throws Exception {
		return saveDocument(xmlDocument, 0);
	}
	
	/**
	 * Salvataggio di un documento XML
	 * @param xmlDocument
	 * @param docNum
	 * @return
	 * @throws Exception
	 */
	public int saveDocument(Document xmlDocument, int docNum) throws Exception {
		if (logger.isInfoEnabled()) {
			if (docNum == 0)
				logger.info("ExtrawayClient: Save new document...");
			else
				logger.info("ExtrawayClient: Save document with docNum = " + docNum + "...");
		}
		if (logger.isDebugEnabled())
			logger.debug("ExtrawayClient: doc content = " + xmlDocument);
		
		Element rootEl = xmlDocument.getRootElement();
		if (rootEl.getNamespaceForPrefix("xw") == null) //add xmlns:xw to root element
			rootEl.addNamespace("xw", XW_NAMESPACE);
		String xml = Utils.dom4jdocumentToString(xmlDocument, ENCODING, true);
        XMLCommand theCommand = new XMLCommand(XMLCommand.SaveDocument, XMLCommand.SaveDocument_Save, docNum, xml, rootEl.getName(), "", docNum == 0? null : theLock);
        theCommand.encoding = ENCODING;
        String result = broker.XMLCommand(connId, db, theCommand.toString());
        return Integer.parseInt(XMLCommand.getDval(result, "ndoc"));
	}

	public String getUniqueRuleDb(String udName) {
		return broker.getUniqueRuleDb(connId, db, udName);	
	}

	/**
	 * Ritorna il numero fisico di un documento identificato tramite la propria posizione su una selezione risultante da una ricerca
	 * @param position
	 * @return
	 * @throws SQLException
	 */
	public int getPhysdocByQueryResult(int position) throws SQLException {
		return broker.getNumDoc(connId, db, queryResult, position);
	}
	
	/**
	 * Caricamento con LOCK di un documento in base al numero fisico
	 * @param physdoc
	 * @return
	 * @throws Exception
	 */
	public Document loadAndLockDocument(int physdoc) throws Exception {
		return loadAndLockDocument(physdoc, 1, 0);
	}
	
	/**
	 * Caricamento con LOCK di un documento in base al numero fisico
	 * @param physdoc
	 * @param attempts
	 * @param delay
	 * @return
	 * @throws Exception
	 */
	public Document loadAndLockDocument(int physdoc, int attempts, long delay) throws Exception {
        XMLCommand theCommand = new XMLCommand(it.highwaytech.broker.XMLCommand.LoadDocument, XMLCommand.LoadDocument_Lock, physdoc);
        theCommand.encoding = ENCODING;
        String xresponse = null;
        for (int i = 0; (i < attempts); i++) {
            try {
            	if (logger.isInfoEnabled())
            		logger.info("ExtrawayClient: Try to load document " + physdoc + " with LOCK [attempt " + (i+1) + "]...");
            	
                xresponse = broker.XMLCommand(connId, db, theCommand.toString());
                break;
            }
            catch (Exception e) {
            	logger.warn("ExtrawayClient: Unable to load and LOCK document " + physdoc + " [attempt " + (i+1) + "]... " + e.getMessage());
            	
                Thread.sleep(delay);
                if (i+1 == attempts)
                    throw e;
            }
        } //end-for
        
        this.theLock = XMLCommand.getLockCode(xresponse);
        String theContent = xresponse.substring(XMLCommand.getBstContentStartOffset(xresponse), XMLCommand.getBstContentStopOffset(xresponse));        
        Document document = DocumentHelper.parseText(theContent);
        return document;
	}
	
	/**
	 * Unlock di un documento precedentemente bloccato
	 * @param physdoc
	 * @throws Exception
	 */
	public void unlockDocument(int physdoc) throws Exception {
		if (logger.isInfoEnabled())
    		logger.info("ExtrawayClient: Unlock document " + physdoc + "...");
		
        XMLCommand theCommand = new XMLCommand(physdoc, theLock);
        broker.XMLCommand(connId, db, theCommand.toString());
	}
	
	/**
	 * Caricamento di un file
	 * @param fileName
	 * @param fileContent
	 * @return
	 * @throws Exception
	 */
	public String addAttach(String fileName, byte[] fileContent) throws Exception {
		return addAttach(fileName, fileContent, 1, 0);
	}
	
	/**
	 * Caricamento di un file
	 * @param fileName
	 * @param fileContent
	 * @param attempts
	 * @param delay
	 * @return
	 * @throws Exception
	 */
	public String addAttach(String fileName, byte[] fileContent, int attempts, long delay) throws Exception {
        for (int i = 0; (i < attempts); i++) {
            try {
            	if (logger.isInfoEnabled())
            		logger.info("ExtrawayClient: Try to attach file " + fileName + " [attempt " + (i+1) + "]...");
            	
                return broker.addAttach(connId, db, fileContent, fileName);
            }
            catch (Exception e) {
            	logger.warn("ExtrawayClient: Unable to attach file " + fileName + " [attempt " + (i+1) + "]... " + e.getMessage());
            	
                Thread.sleep(delay);
                if (i+1 == attempts)
                    throw e;
            }
        } //end-for		
        return null;
	}
	
}
