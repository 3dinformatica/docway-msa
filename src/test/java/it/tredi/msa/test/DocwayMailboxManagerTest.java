package it.tredi.msa.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.ResourceUtils;

import it.tredi.msa.mailboxmanager.ParsedMessage;
import it.tredi.msa.mailboxmanager.docway.Docway4MailboxManager;
import it.tredi.msa.test.conf.MsaTesterApplication;

/**
 * UnitTest su salvataggio messaggi eml come documenti di DocWay
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = MsaTesterApplication.class)
public class DocwayMailboxManagerTest extends EmlReader {
	
	/**
	 * Errore su encoded stream su BASE64Decoder (it.tredi.msa.mailboxmanager.PartContentProvider.getContent(), riga 22)
	 * @throws Exception
	 */
	@Test
	@Ignore
	public void base64DecoderErrorExtraction() throws Exception {
		String fileName = "base64decoderError.eml";
		File file = ResourceUtils.getFile("classpath:" + EML_LOCATION + "/" + fileName);
		
		System.out.println("input file = " + fileName);
		
		ParsedMessage parsed = new ParsedMessage(readEmlFile(file));
		
		assertNotNull(parsed);
		assertNotNull(parsed.getMessageId());
		
		System.out.println("messageId = " + parsed.getMessageId());
		System.out.println("subject = " + parsed.getSubject());
		System.out.println("from address = " + parsed.getFromAddress());
		
		List<String> attachments = parsed.getAttachmentsName();
		System.out.println("attachments count = " + attachments.size());
		for (String name : attachments)
			System.out.println("\tattach name = " + name);
		
		assertEquals(7, parsed.getAttachments().size());
		
		Docway4MailboxManager docwayManager = new Docway4MailboxManager();
		docwayManager.storeMessage(parsed);
		
		// TODO completamento verifica
	}
	
}
