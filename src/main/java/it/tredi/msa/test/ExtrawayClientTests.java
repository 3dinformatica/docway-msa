package it.tredi.msa.test;

import org.junit.Test;
import org.junit.runners.MethodSorters;

import it.tredi.extraway.ExtrawayClient;
import it.tredi.msa.Services;

import org.dom4j.Document;
import org.junit.Assert;
import org.junit.FixMethodOrder;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ExtrawayClientTests {
	
	private static ExtrawayClient client; 
	
	@Test
	public void test_001_connect() throws Exception {
		client = new ExtrawayClient("docway-test.bo.priv", 4859, "acl", "lettore", "reader");
		client.connect();
	}

	@Test
	public void test_003_search() throws Exception {
		int count = client.search("[casellapostaelettronica_interop]=\"no\"");
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
