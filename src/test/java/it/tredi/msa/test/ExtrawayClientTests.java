package it.tredi.msa.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.sql.SQLException;

import org.dom4j.Document;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import it.tredi.extraway.ExtrawayClient;
import it.tredi.msa.test.conf.MsaTesterApplication;

/**
 * UnitTest di integrazione con il server eXtraWay
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = MsaTesterApplication.class)
public class ExtrawayClientTests {
	
	private static final String XWAY_HOST = "127.0.0.1";
	private static final int XWAY_PORT = 4859;
	private static final String XWAY_DBNAME = "acl";
	private static final String XWAY_USERNAME = "reader";
	private static final String XWAY_PASSWORD = "";
	
	private ExtrawayClient extrawayClient;
	
	/**
	 * Connessione con il server eXtraWay
	 * @throws SQLException
	 */
	@Before
	public void connect() throws SQLException {
		extrawayClient = new ExtrawayClient(XWAY_HOST, XWAY_PORT, XWAY_DBNAME, XWAY_USERNAME, XWAY_PASSWORD);
		extrawayClient.connect();
	}
	
	/**
	 * Disconnessione dal server eXtraWay
	 * @throws SQLException
	 */
	@After
	public void disconnect() throws SQLException {
		extrawayClient.disconnect();
	}
	
	/**
	 * Test di ricerca su eXtraWay (archivio ACL)
	 * @throws Exception 
	 */
	@Test
	@Ignore
	public void searchTest() throws Exception {
		int count = extrawayClient.search("[/comune/@nazione/]=\"italia\"");
		assertTrue(count > 0);
		
		// caricamento di un record
		int position = 0;
		if (count > 1)
			position = 1;
		Document doc = extrawayClient.loadDocByQueryResult(position);
		assertNotNull(doc);
		assertNotNull(doc.getRootElement());
		
		String nrecord = doc.getRootElement().attributeValue("nrecord", "");
		assertNotEquals("", nrecord);
		
		String nome = doc.getRootElement().attributeValue("nome", "");
		assertNotEquals("", nome);
		
		count = extrawayClient.search("[/comune/@nome/]=\" " + nome + "\"");
		assertEquals(1, count);
	}

}
