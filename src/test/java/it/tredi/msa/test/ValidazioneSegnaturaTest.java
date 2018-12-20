package it.tredi.msa.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.List;

import org.dom4j.Document;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.ResourceUtils;

import it.tredi.msa.configuration.docway.AssegnatarioMailboxConfiguration;
import it.tredi.msa.configuration.docway.DocwayMailboxConfiguration;
import it.tredi.msa.mailboxmanager.docway.DocwayMailboxManager;
import it.tredi.msa.mailboxmanager.docway.DocwayParsedMessage;
import it.tredi.msa.test.conf.MsaTesterApplication;
import it.tredi.msa.test.manager.DocWayDummyPecMailboxManager;
import it.tredi.msa.test.manager.DummyMailReader;

/**
 * UnitTest su estrazione contenuto di eml PEC di interoperabilita' (validazione di segnatura.xml)
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = MsaTesterApplication.class)
public class ValidazioneSegnaturaTest extends EmlReader {

	private static final String COD_AMM = "3DIN";
	private static final String COD_AOO = "TES";
	
	
	private DocwayMailboxManager mailboxManager;
	
	/**
	 * Inizializzazione del manager di test della casella di posta
	 */
	@Before
	public void initManager() {
		this.mailboxManager = new DocWayDummyPecMailboxManager();
		this.mailboxManager.setConfiguration(buildConfiguration());
		this.mailboxManager.setMailReader(new DummyMailReader());
	}
	
	/**
	 * Costruzione della configurazione mailbox di test
	 * @return
	 */
	private DocwayMailboxConfiguration buildConfiguration() {
		DocwayMailboxConfiguration configuration = new DocwayMailboxConfiguration();
		configuration.setName("CONF-TEST");
		configuration.setCodAmmAoo(COD_AMM);
		configuration.setCodAmmInteropPA(COD_AMM);
		configuration.setCodAoo(COD_AMM);
		configuration.setCodAooInteropPA(COD_AOO);
		
		AssegnatarioMailboxConfiguration responsabile = new AssegnatarioMailboxConfiguration();
		responsabile.setNomePersona("Thomas Iommi");
		responsabile.setCodPersona("PI0000001");
		responsabile.setNomeUff("Servizio Tecnico Bologna");
		responsabile.setCodUff("SI0000001");
		configuration.setResponsabile(responsabile);
		
		return configuration;
	}
	
	/**
	 * Test di parsing di messaggio email con segnatura non valida per via della dichiarazione 
	 * degli allegati
	 * @throws Exception
	 */
	@Test
	public void segnaturaAllegatiNonValidiExtraction() throws Exception {
		String fileName = "segnatura_allegati_non_validi.eml";
		File file = ResourceUtils.getFile("classpath:" + EML_LOCATION + "/" + fileName);
		
		System.out.println("input file = " + fileName);
		
		DocwayParsedMessage parsed = new DocwayParsedMessage(readEmlFile(file));
		
		assertNotNull(parsed);
		assertNotNull(parsed.getMessageId());
		
		assertTrue(parsed.isPecMessage());
		assertFalse(parsed.isPecReceipt());
		
		System.out.println("messageId = " + parsed.getMessageId());
		System.out.println("subject = " + parsed.getSubject());
		System.out.println("from address = " + parsed.getFromAddress());
		
		List<String> attachments = parsed.getAttachmentsName();
		System.out.println("attachments count = " + attachments.size());
		for (String name : attachments)
			System.out.println("\tattach name = " + name);
		
		assertEquals(4, parsed.getAttachments().size());
		
		Document interopDocument = parsed.getSegnaturaInteropPADocument();
		assertNotNull(interopDocument);
		
		assertTrue(parsed.isSegnaturaInteropPAMessage(COD_AMM, COD_AOO));
		
		this.mailboxManager.processMessage(parsed);
		
		for (String message : parsed.getRelevantMssages()) {
			System.out.println("alert message = " + message);
		}
		
		assertNotNull(parsed.getRelevantMssages());
		assertEquals(1, parsed.getRelevantMssages().size());
		assertTrue(parsed.getRelevantMssages().get(0).indexOf("testo_di_prova.pdf") != -1);
	}
	
	/**
	 * Test di parsing di messaggio email con segnatura priva di allegati ma con testo indicato (VALIDA)
	 * @throws Exception
	 */
	@Test
	public void segnaturaSoloTestoExtraction() throws Exception {
		String fileName = "segnatura_solo_testo.eml";
		File file = ResourceUtils.getFile("classpath:" + EML_LOCATION + "/" + fileName);
		
		System.out.println("input file = " + fileName);
		
		DocwayParsedMessage parsed = new DocwayParsedMessage(readEmlFile(file));
		
		assertNotNull(parsed);
		assertNotNull(parsed.getMessageId());
		
		assertTrue(parsed.isPecMessage());
		assertFalse(parsed.isPecReceipt());
		
		System.out.println("messageId = " + parsed.getMessageId());
		System.out.println("subject = " + parsed.getSubject());
		System.out.println("from address = " + parsed.getFromAddress());
		
		List<String> attachments = parsed.getAttachmentsName();
		System.out.println("attachments count = " + attachments.size());
		for (String name : attachments)
			System.out.println("\tattach name = " + name);
		
		assertEquals(3, parsed.getAttachments().size());
		
		Document interopDocument = parsed.getSegnaturaInteropPADocument();
		assertNotNull(interopDocument);
		
		// TODO stampa del testo
		
		assertTrue(parsed.isSegnaturaInteropPAMessage(COD_AMM, COD_AOO));
		
		this.mailboxManager.processMessage(parsed);
		assertEquals(0, parsed.getRelevantMssages().size());
	}

}
