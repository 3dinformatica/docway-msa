package it.tredi.msa.mailboxmanager.docway.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.mail.Part;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.Node;

import it.tredi.mail.entity.MailAttach;
import it.tredi.msa.configuration.docway.DocwayMailboxConfiguration;
import it.tredi.msa.mailboxmanager.ParsedMessage;
import it.tredi.msa.mailboxmanager.PartContentProvider;
import it.tredi.msa.mailboxmanager.docway.DocwayParsedMessage;

/**
 * Gestione del rifiuto di documenti in base ad allegati non supportati inclusi nel messaggio (estensioni vietate)
 */
public class RifiutoHandler {

	private static final Logger logger = LogManager.getLogger(RifiutoHandler.class.getName());
	
	/**
	 * Elenco di estensioni che devono obbigatoriamente essere accettate (perche' incluse in qualsiasi messaggio
	 * PEC ricevuto)
	 */
	private final String[] ACCEPTED_EXTENSIONS = new String[] { "xml", "p7s" };

	/**
	 * Configurazione della mailbox che si sta processando
	 */
	private DocwayMailboxConfiguration conf;
	
	/**
	 * Costruttore
	 * @param conf Configurazione della mailbox che si sta processando
	 */
	public RifiutoHandler(DocwayMailboxConfiguration conf) {
		this.conf = conf;
	}
	
	/**
	 * Ritorna TRUE se il rifiuto in base ad estensioni vietate e' abilitato, FALSE altrimenti
	 * @return
	 */
	private boolean isRifiutoEnabled() {
		return this.conf.getRifiutoByAttachments() != null && this.conf.getRifiutoByAttachments().isEnabled();
	}
	
	/**
	 * Ritorna TRUE se il messaggio passato come parametro deve essere rifiutato in base ad allegati non supportati 
	 * inclusi, FALSE altrimenti
	 * @param parsedMessage Messaggio da verificare
	 * @return
	 */
	public boolean toRefuse(DocwayParsedMessage parsedMessage) {
		boolean refuse = false;
		if (parsedMessage != null) {
			if (parsedMessage.isSegnaturaInteropPAMessage(conf.getCodAmmInteropPA(), conf.getCodAooInteropPA())) {
				// messaggio di interoperabilita' (segnatura.xml)
				List<String> invalid = this.getInvalidAttachmentsSegnatura(parsedMessage.getSegnaturaInteropPADocument(), parsedMessage);
				return (invalid == null || invalid.isEmpty());
			}
			else {
				// messaggio classico (PEC o non)
				List<String> invalid = this.getInvalidAttachments(parsedMessage);
				refuse = invalid != null && !invalid.isEmpty();
			}
		}
		return refuse;
	}
	
	/**
	 * Ritorna l'elenco di allegati non supportati presenti all'interno del messaggio
	 * @param parsedMessage Messaggio da verificare
	 * @return Eventuale elenco di allegati non supportati
	 */
	public List<String> getInvalidAttachments(ParsedMessage parsedMessage) {
		List<String> attachments = null;
		
		if (this.isRifiutoEnabled() && parsedMessage.getAttachments() != null && !parsedMessage.getAttachments().isEmpty()) {
			attachments = new ArrayList<String>();
			
			for (MailAttach attach : parsedMessage.getAttachments()) {
				if (attach != null) {
					String extension = attach.getFileExtension().toLowerCase();
					if (!extension.isEmpty()) {
					
						if (extension.equals("zip") && conf.isExtractZip()) {
							// estrazione da file ZIP abilitata... verifico le estensioni di tutti i file inclusi nello ZIP
							try {
								List<String> invalidFromZip = this.listInvalidFilesFromArchive(new PartContentProvider(attach.getPart()).getContent());
								if (invalidFromZip != null && !invalidFromZip.isEmpty())
									attachments.addAll(invalidFromZip);
							}
							catch(Exception e) {
								// Impossibile caricare il contenuto del file ZIP (probabilmente si tratta di un allegato danneggiato)... log
								// dell'eccezione e allegato considerato come valido (valutazione a carico di un operatore)
								logger.warn("[" + this.conf.getAddress() + "] Unable to evaluate rejection by attach... " + e.getMessage(), e);
							}
						}
						else {
							// controllo del file corrente...
							
							if (isInvalidExtension(extension))
								attachments.add(attach.getFileName());								
						}
						
					}
				}
			}
		}
		
		return attachments;
	}
	
	/**
	 * Ritorna l'elenco di allegati non supportati contenuti all'interno di un archivio ZIP
	 * @param zipContent
	 * @return
	 * @throws Exception
	 */
	private List<String> listInvalidFilesFromArchive(byte[] zipContent) throws Exception {
		List<String> files = null;
		if (zipContent != null) {
			files = new ArrayList<String>();
			ZipManager zipManager = new ZipManager(conf.getAddress());
			
			List<String> filesInZip = zipManager.listArchive(zipContent);
			if (filesInZip != null && !filesInZip.isEmpty()) {
				
				for (String file : filesInZip) {
					if (this.isInvalidExtension(this.getFileExtension(file)))
						files.add(file);
				}
			}
		}
		return files;
	}
	
	/**
	 * Recupero di tutti i file specificati nel file segnatura.xml (caso di messaggio di interoperabilita')
	 * @param segnaturaDocument
	 * @return
	 */
	private List<String> getFilesFromSegnatura(Document segnaturaDocument) {
		List<String> files = null;
		if (segnaturaDocument != null) {
			files = new ArrayList<String>();
			
			// Recupero del documento principale
			Node node = segnaturaDocument.selectSingleNode("/Segnatura/Descrizione/Documento/@nome");
			if (node != null && node.getText() != null)
				files.add(node.getText().trim());
			
			// Recupero di altri allegati specificati nel file segnatura
			List<?> nodes = segnaturaDocument.selectNodes("/Segnatura/Descrizione/Allegati/Documento/@nome");
			if (nodes != null && !nodes.isEmpty()) {
				for (int i=0; i<nodes.size(); i++) {
					node = (Node) nodes.get(i);
					if (node != null && node.getText() != null)
						files.add(node.getText().trim());
				}
			}
		}
		return files;
	}
	
	/**
	 * Recupero di eventuali files non supportati specificati all'interno del file di segnatura
	 * @param segnaturaDocument
	 * @param parsedMessage
	 * @return
	 */
	public List<String> getInvalidAttachmentsSegnatura(Document segnaturaDocument, ParsedMessage parsedMessage) {
		List<String> attachments = null;
		if (segnaturaDocument != null && parsedMessage != null) {
			List<String> files = this.getFilesFromSegnatura(segnaturaDocument);
			
			if (this.isRifiutoEnabled() && files != null && !files.isEmpty()) {
				attachments = new ArrayList<String>();
				
				for (String attach : files) {
					if (attach != null) {
						String extension = this.getFileExtension(attach.toLowerCase());
						if (!extension.isEmpty()) {
						
							if (extension.equals("zip") && conf.isExtractZip()) {
								// estrazione da file ZIP abilitata... verifico le estensioni di tutti i file inclusi nello ZIP
								try {
									Part attachPart = parsedMessage.getFirstAttachmentByName(attach);
									if (attachPart != null) {
										List<String> invalidFromZip = this.listInvalidFilesFromArchive(new PartContentProvider(attachPart).getContent());
										if (invalidFromZip != null && !invalidFromZip.isEmpty())
											attachments.addAll(invalidFromZip);
									}
								}
								catch(Exception e) {
									// Impossibile caricare il contenuto del file ZIP (probabilmente si tratta di un allegato danneggiato)... log
									// dell'eccezione e allegato considerato come valido (valutazione a carico di un operatore)
									logger.warn("[" + this.conf.getAddress() + "] Unable to evaluate rejection from segnatura by attach... " + e.getMessage(), e);
								}
							}
							else {
								// controllo del file corrente...
								
								if (isInvalidExtension(extension))
									attachments.add(attach);								
							}
							
						}
					}
				}
			}
		}
		
		return attachments;
	}
	
	/**
	 * Ritorna TRUE se l'estensione passata non e' valida, FALSE altrimenti (file accettato)
	 * @param extension
	 * @return
	 */
	private boolean isInvalidExtension(String extension) {
		return !extension.isEmpty() 
				&& !Arrays.asList(ACCEPTED_EXTENSIONS).contains(extension) 
				&& !conf.getRifiutoByAttachments().getAllowedExtensions().contains(extension);
	}
	
	/**
	 * Ritorna l'estensione del file. In caso di estensione non trovata viene restituita la 
	 * stringa vuota
	 * @return
	 */
	private String getFileExtension(String fileName) {
		String ext = "";
		if (fileName != null) {
			int index = fileName.lastIndexOf(".");
			if (index != -1) 
				ext = fileName.substring(index+1);
		}
		return ext;
	}
	
	/**
	 * Ritorna TRUE se l'archivio ZIP passato come parametro contiene dei file non supportati dal
	 * sistema, FALSE altrimenti
	 * @param zipContent
	 * @return
	 */
	public boolean hasArchiveInvalidFiles(byte[] zipContent) {
		List<String> invalidFiles = null;
		if (this.isRifiutoEnabled()) {
			try {
				invalidFiles = this.listInvalidFilesFromArchive(zipContent);				
			}
			catch(Exception e) {
				logger.warn("[" + this.conf.getAddress() + "] Unable to extract invalid files from ZIP archive... " + e.getMessage(), e);
			}
		}
		return invalidFiles != null && invalidFiles.size() > 0;
	}
	
}
