package it.tredi.msa.test;

import org.junit.Test;
import org.junit.runners.MethodSorters;

import it.tredi.extraway.ExtrawayClient;
import it.tredi.msa.Services;

import java.io.IOException;
import java.util.Properties;

import org.dom4j.Document;
import org.junit.Assert;
import org.junit.FixMethodOrder;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ExtrawayClientTests {
	
	private static ExtrawayClient client; 
	
	private static String host;
	private static int port;
	private static String user;
	private static String password;
	private static String db;
	private static String query;
	
	static {
		Properties properties = new Properties();
		try {
			properties.load(ExtrawayClientTests.class.getResourceAsStream("extraway.properties"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		host = properties.getProperty("extraway.host");
		port = Integer.parseInt(properties.getProperty("extraway.port"));
		user = properties.getProperty("extraway.user");
		password = properties.getProperty("extraway.password");
		db = properties.getProperty("extraway.db");
		query = properties.getProperty("extraway.query");
	}
	
	@Test
	public void test_001_connect() throws Exception {
		client = new ExtrawayClient(host, port, db, user, password);
		client.connect();
	}

	@Test
	public void test_003_search() throws Exception {
		int count = client.search(query);
		Assert.assertTrue(count > 0);
	}
	
	@Test
	public void test_004_loadDocument() throws Exception {
		Document document = client.loadDocByQueryResult(0);
		
		org.dom4j.Element el = document.getRootElement();
	}	
	
	@Test
	public void test_999_disconnect() throws Exception {
		client.disconnect();
	}		

}
