package it.tredi.msa.mailboxmanager.docway.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Gestione di file ZIP (estrazione di file da allegati ZIP contenuti nei
 * messaggi della casella di posta)
 */
public class ZipManager {

	private static final Logger logger = LogManager.getLogger(ZipManager.class.getName());

	/**
	 * Indirizzo email della casella di posta elettronica che si sta processando
	 */
	private String mailboxAddress;

	/**
	 * Directory di lavoro per la compressione/decompressione di file ZIP
	 */
	private File workDir;

	/**
	 * Costruttore
	 * @param mailboxAddress Indirizzo email della casella di archiviazione che si sta processando
	 */
	public ZipManager(String mailboxAddress) {
		this.mailboxAddress = mailboxAddress;

		this.workDir = new File(System.getProperty("java.io.tmpdir"), mailboxAddressToFolder(mailboxAddress));
		if (!workDir.exists())
			workDir.mkdir();

		if (logger.isDebugEnabled())
			logger.debug("[" + this.mailboxAddress + "] ZIP files work dir = " + this.workDir.getAbsolutePath());
	}

	/**
	 * Dato l'indirizzo della casella di posta elettronica da processare, ritorna il
	 * nome da assegnare alla directory di lavoro (pulizia di tutti i caratteri
	 * speciali)
	 * @param mailboxAddress
	 * @return
	 */
	private String mailboxAddressToFolder(String mailboxAddress) {
		String folderName = null;
		if (mailboxAddress != null) {
			folderName = mailboxAddress.replace("@", "_");
			folderName = folderName.replace(".", "_");
		}
		return folderName;
	}

	/**
	 * Reset della directory di lavoro della casella di posta (eliminazione di eventuali residui di precedenti
	 * elaborazioni)
	 */
	private void resetWorkDir() {
		this.deleteFolder(this.workDir, false);
	}

	/**
	 * Cancellazione di una directory temporanea (e del suo contenuto)
	 * @param dir directory da ripulire
	 * @param deleteCurrentFolder TRUE se occorre eliminare la directory corrente, FALSE altrimenti
	 */
	private void deleteFolder(File folder, boolean deleteCurrentFolder) {
		File[] files = folder.listFiles();
		if (files != null) {
			for (File f : files) {
				if (f.isDirectory())
					deleteFolder(f, true);
				else
					f.delete();
			}
		}
		if (deleteCurrentFolder)
			folder.delete();
	}
	
	/**
	 * Estrazione di tutti i file inclusi in un file ZIP passato come parametro.
	 * Ritorna la lista di file decompressi.
	 * @param zipContent File ZIP da decomprimere
	 * @return Elenco di file estratti dal file ZIP
	 * @throws Exception
	 */
	public List<File> unzipArchive(byte[] zipContent) throws Exception {
		List<File> files = null;
		if (zipContent != null) {
			this.resetWorkDir();
			
			File zipFile = new File(this.workDir, "temp.zip");
			FileUtils.writeByteArrayToFile(zipFile, zipContent);
			files = this.unzipArchive(zipFile, false);
		}
		return files;
	}

	/**
	 * Estrazione di tutti i file inclusi in un file ZIP passato come parametro.
	 * Ritorna la lista di file decompressi.
	 * @param zip File ZIP da decomprimere
	 * @return Elenco di file estratti dal file ZIP
	 * @throws Exception
	 */
	public List<File> unzipArchive(File zip) throws Exception {
		return this.unzipArchive(zip, true);
	}
	
	/**
	 * Estrazione di tutti i file inclusi in un file ZIP passato come parametro.
	 * Ritorna la lista di file decompressi.
	 * @param zip File ZIP da decomprimere
	 * @param resetWorkDir
	 * @return Elenco di file estratti dal file ZIP
	 * @throws Exception
	 */
	private List<File> unzipArchive(File zip, boolean resetWorkDir) throws Exception {
		List<File> files = null;
		if (zip != null && zip.exists()) {
			if (resetWorkDir)
				this.resetWorkDir();
			files = new ArrayList<File>();

			ZipFile zipFile = new ZipFile(zip);
			try {
				Enumeration<?> enu = zipFile.entries();
				while (enu.hasMoreElements()) {

					ZipEntry zipEntry = (ZipEntry) enu.nextElement();
					String name = zipEntry.getName();

					if (logger.isDebugEnabled()) {
						long size = zipEntry.getSize();
						long compressedSize = zipEntry.getCompressedSize();
						logger.debug("[" + this.mailboxAddress + "] ZIP MANAGER: Trovata zip entry... [name: " + name
								+ ", size: " + size + ", compressed size: " + compressedSize + "]");
					}

					File file = new File(this.workDir, name);
					if (name.endsWith("/")) {
						file.mkdirs();
						continue;
					}
					File parent = file.getParentFile();
					if (parent != null) {
						parent.mkdirs();
					}

					InputStream is = zipFile.getInputStream(zipEntry);
					FileOutputStream fos = new FileOutputStream(file);
					byte[] bytes = new byte[1024];
					int length;
					while ((length = is.read(bytes)) >= 0) {
						fos.write(bytes, 0, length);
					}
					is.close();
					fos.close();

					files.add(file);
				}
			} finally {
				zipFile.close();
			}
		}
		if (logger.isDebugEnabled())
			logger.debug("[" + this.mailboxAddress + "] ZIP MANAGER: Terminata procedura di unzipping! Resitituiti "
					+ (files != null ? files.size() : 0) + " files");

		return files;
	}
	
	/**
	 * Ritorna l'elenco di nomi di files inclusi nel file ZIP passato
	 * Ritorna la lista di file decompressi.
	 * @param zipContent File ZIP da processare
	 * @return Elenco di nomi di files contenuti nel file ZIP
	 * @throws Exception
	 */
	public List<String> listArchive(byte[] zipContent) throws Exception {
		List<String> files = null;
		if (zipContent != null) {
			this.resetWorkDir();
			
			File zipFile = new File(this.workDir, "temp.zip");
			FileUtils.writeByteArrayToFile(zipFile, zipContent);
			files = this.listArchive(zipFile, false);
		}
		return files;
	}
	
	/**
	 * Ritorna l'elenco di nomi di files inclusi nel file ZIP passato
	 * @param zip File ZIP da processare
	 * @return Elenco di nomi di files contenuti nel file ZIP
	 * @throws Exception
	 */
	public List<String> listArchive(File zip) throws Exception {
		return this.listArchive(zip, true);
	}

	/**
	 * Ritorna l'elenco di nomi di files inclusi nel file ZIP passato
	 * @param zip File ZIP da processare
	 * @param resetWorkDir
	 * @return Elenco di nomi di files contenuti nel file ZIP
	 * @throws Exception
	 */
	private List<String> listArchive(File zip, boolean resetWorkDir) throws Exception {
		List<String> files = null;
		if (zip != null && zip.exists()) {
			if (resetWorkDir)
				this.resetWorkDir();
			files = new ArrayList<String>();

			ZipFile zipFile = new ZipFile(zip);
			try {
				Enumeration<?> enu = zipFile.entries();
				while (enu.hasMoreElements()) {

					ZipEntry zipEntry = (ZipEntry) enu.nextElement();
					String name = zipEntry.getName();

					if (!name.endsWith("/"))
						files.add(name);
				}
			} finally {
				zipFile.close();
			}

		}
		return files;
	}

}
