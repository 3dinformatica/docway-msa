package it.tredi.msa.mailboxmanager.docway;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class FatturaPAItem {
	
	private String fileNameFattura;
	private String extensionFattura;
	private String state;
	private Date sendDate;
	private String versione;
	
	private List<DatiFatturaContainer> datiFatturaL;

	public FatturaPAItem() {
		this.datiFatturaL = new ArrayList<>();
	}

	public String getFileNameFattura() {
		return fileNameFattura;
	}

	public void setFileNameFattura(String fileNameFattura) {
		this.fileNameFattura = fileNameFattura;
	}

	public String getExtensionFattura() {
		return extensionFattura;
	}

	public void setExtensionFattura(String extensionFattura) {
		this.extensionFattura = extensionFattura;
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	public Date getSendDate() {
		return sendDate;
	}

	public void setSendDate(Date sendDate) {
		this.sendDate = sendDate;
	}

	public String getVersione() {
		return versione;
	}

	public void setVersione(String versione) {
		this.versione = versione;
	}

	public List<DatiFatturaContainer> getDatiFatturaL() {
		return datiFatturaL;
	}

	public void setDatiFatturaL(List<DatiFatturaContainer> datiFatturaL) {
		this.datiFatturaL = datiFatturaL;
	}

	public void addDatiFattura(DatiFatturaContainer datiFattura) {
		this.datiFatturaL.add(datiFattura);
	}
	
}
