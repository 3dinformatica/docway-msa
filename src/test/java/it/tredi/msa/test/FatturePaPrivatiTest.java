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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.ResourceUtils;

import it.tredi.msa.configuration.docway.AssegnatarioMailboxConfiguration;
import it.tredi.msa.configuration.docway.DocwayMailboxConfiguration;
import it.tredi.msa.mailboxmanager.DocWayDummyMailboxManager;
import it.tredi.msa.mailboxmanager.DummyMailReader;
import it.tredi.msa.mailboxmanager.docway.DocTipoEnum;
import it.tredi.msa.mailboxmanager.docway.DocwayDocument;
import it.tredi.msa.mailboxmanager.docway.DocwayParsedMessage;
import it.tredi.msa.mailboxmanager.docway.fatturapa.utils.FatturaPAUtils;
import it.tredi.msa.test.conf.MsaTesterApplication;

/**
 * UnitTest su estrazione contenuto di eml relativi a fatturePa tra privati
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = MsaTesterApplication.class)
@ActiveProfiles({ "local", "jenkins" })
public class FatturePaPrivatiTest extends EmlReader {
	
	private static final String FATTUREPA_EML_LOCATION = "fatturepa";

	private static final String COD_AMM = "3DIN";
	private static final String COD_AOO = "TES";
	
	/**
	 * Manager di test per l'elaborazione dei messaggi
	 */
	private DocWayDummyMailboxManager mailboxManager;
	
	/**
	 * Inizializzazione del manager di test della casella di posta
	 */
	@Before
	public void initManager() {
		this.mailboxManager = new DocWayDummyMailboxManager();
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
		configuration.setTipoDoc(DocwayMailboxConfiguration.DOC_TIPO_ARRIVO);
		configuration.setEnableFatturePA(true);
		configuration.setSdiDomainAddress("");
		
		AssegnatarioMailboxConfiguration responsabile = new AssegnatarioMailboxConfiguration();
		responsabile.setNomePersona("Thomas Iommi");
		responsabile.setCodPersona("PI0000001");
		responsabile.setNomeUff("Servizio Tecnico Bologna");
		responsabile.setCodUff("SI0000001");
		configuration.setResponsabile(responsabile);
		
		return configuration;
	}
	
	/**
	 * Test di analisi di una fattura passiva inviata ad un ente privato (CASO 1)
	 */
	@Test
	public void fatturaPassiva01Test() throws Exception {
		String fileName = "fattura_passiva_privati_1.eml";
		File file = ResourceUtils.getFile("classpath:" + EML_LOCATION + "/" + FATTUREPA_EML_LOCATION + "/" + fileName);
		
		System.out.println("input file = " + fileName);
		
		DocwayParsedMessage parsed = new DocwayParsedMessage(readEmlFile(file), false);
		
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
		
		assertTrue(parsed.isFatturaPAMessage(""));
		
		Document fatturaDocument = parsed.getFatturaPADocument();
		assertNotNull(fatturaDocument);
		
		DocwayDocument document = this.mailboxManager.buildDocwayDocumentByFatturaPAMessage(parsed);
		assertNotNull(document);
		assertEquals(DocTipoEnum.ARRIVO.getText(), document.getTipo());
		assertNotNull(document.getFatturaPA());
		assertEquals(FatturaPAUtils.CODICE_DESTINATARIO_PRIVATO, document.getFatturaPA().getCodiceDestinatario());
		assertEquals(FatturaPAUtils.TIPO_MESSAGGIO_EC, document.getFatturaPA().getState());
		assertEquals(1, document.getFatturaPA().getDatiFatturaL().size());
		assertEquals("IT01641790702_fLrNw", document.getFatturaPA().getFileNameFattura());
		
		this.mailboxManager.processMessage(parsed);
		
		for (String message : parsed.getRelevantMssages()) {
			System.out.println("alert message = " + message);
		}
		
		assertNotNull(parsed.getRelevantMssages());
		assertEquals(0, parsed.getRelevantMssages().size());
	}
	
	/**
	 * Test di analisi di una fattura passiva inviata ad un ente privato (Caso 2)
	 */
	@Test
	public void fatturaPassiva02Test() throws Exception {
		String fileName = "fattura_passiva_privati_2.eml";
		File file = ResourceUtils.getFile("classpath:" + EML_LOCATION + "/" + FATTUREPA_EML_LOCATION + "/" + fileName);
		
		System.out.println("input file = " + fileName);
		
		DocwayParsedMessage parsed = new DocwayParsedMessage(readEmlFile(file), false);
		
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
		
		assertTrue(parsed.isFatturaPAMessage(""));
		
		Document fatturaDocument = parsed.getFatturaPADocument();
		assertNotNull(fatturaDocument);
		
		DocwayDocument document = this.mailboxManager.buildDocwayDocumentByFatturaPAMessage(parsed);
		assertNotNull(document);
		assertEquals(DocTipoEnum.ARRIVO.getText(), document.getTipo());
		assertNotNull(document.getFatturaPA());
		assertEquals(FatturaPAUtils.CODICE_DESTINATARIO_PRIVATO, document.getFatturaPA().getCodiceDestinatario());
		assertEquals(FatturaPAUtils.TIPO_MESSAGGIO_EC, document.getFatturaPA().getState());
		assertEquals(1, document.getFatturaPA().getDatiFatturaL().size());
		assertEquals("IT02098391200_3oaRR", document.getFatturaPA().getFileNameFattura());
		
		this.mailboxManager.processMessage(parsed);
		
		for (String message : parsed.getRelevantMssages()) {
			System.out.println("alert message = " + message);
		}
		
		assertNotNull(parsed.getRelevantMssages());
		assertEquals(0, parsed.getRelevantMssages().size());
	}
	
}