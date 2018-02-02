package it.tredi.extraway;

import java.sql.SQLException;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;

import it.highwaytech.broker.Broker;
import it.highwaytech.db.Doc;
import it.highwaytech.db.QueryResult;

public class ExtrawayClient {
	
	private Broker broker;
	private String host;
	private int port;
	private String db;
	private String user;
	private String password;
	private int connId;
	private QueryResult queryResult;
	
	public ExtrawayClient(String host, int port, String db, String user, String password) {
		this.broker = new Broker();
		this.host = host;
		this.port = port;
		this.user = user;
		this.password = password;
		this.db = db;
		this.connId = -1;
	}

	public void connect() throws Exception  {
		disconnect();
		connId = broker.acquireConnection(host, port, db, user, password, -1);
        try {
            broker.Connect(connId, host, port, db, -1, user, password, "", "");
        }
        catch (java.sql.SQLException e) {
            disconnect();
            throw e;
        }			
	}
	
	public void disconnect() throws Exception {
		if (connId != -1) {
			broker.releaseConnection(connId);
			connId = -1;			
		}
	}
	
	public int search(String query) throws java.sql.SQLException {
		return this.search(query, null, "", 0, -1);
	}
	
    public int search(String query, String selToRefine, String sort, int hwQOpts, int adj) throws java.sql.SQLException {
        hwQOpts |= (sort != null && sort.length() > 0 ? it.highwaytech.broker.ServerCommand.find_SORT : 0);
        if (selToRefine != null && !selToRefine.isEmpty()) {
            query += " AND [?SEL]=\"" + selToRefine + "\"";
        }
        queryResult = broker.find(connId, db, query, sort, hwQOpts, adj ,0, null);
        return queryResult.elements;
    }	
	
    public Document loadDocByQueryResult(int position) throws Exception {
    	return loadDoc(position, true);
    }
    
    public Document loadDocByPhysdoc(int physdoc) throws Exception {
    	return loadDoc(physdoc, false);
    }
    
    private Document loadDoc(int docNum, boolean fromQueryResult) throws Exception {
    	Doc doc = null;
        if (fromQueryResult)
            doc = broker.getDoc(connId, db, queryResult, docNum, it.highwaytech.broker.ServerCommand.subcmd_NONE, "");
        else
            doc = broker.getDoc(connId, db, docNum, 0);
        Document document = DocumentHelper.parseText(doc.XML());
        return document;
    }
    
}
