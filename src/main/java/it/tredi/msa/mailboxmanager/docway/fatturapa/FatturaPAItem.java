package it.tredi.msa.mailboxmanager.docway.fatturapa;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class FatturaPAItem {
	
	private String identificativoSdI;
	private String emailSdI;
	private String emailToFattPassiva;
	private String fileNameFattura;
	private String extensionFattura;
	private String codiceDestinatario;
	private String formato;
	private String state;
	private Date sendDate;
	private String versione;
	private String tentativiInvio;
	private String messageId;
	private String note;
	
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

	public String getIdentificativoSdI() {
		return identificativoSdI;
	}

	public void setIdentificativoSdI(String identificativoSdI) {
		this.identificativoSdI = identificativoSdI;
	}

	public String getEmailSdI() {
		return emailSdI;
	}

	public void setEmailSdI(String emailSdI) {
		this.emailSdI = emailSdI;
	}

	public String getEmailToFattPassiva() {
		return emailToFattPassiva;
	}

	public void setEmailToFattPassiva(String emailToFattPassiva) {
		this.emailToFattPassiva = emailToFattPassiva;
	}

	public String getCodiceDestinatario() {
		return codiceDestinatario;
	}

	public void setCodiceDestinatario(String codiceDestinatario) {
		this.codiceDestinatario = codiceDestinatario;
	}

	public String getFormato() {
		return formato;
	}

	public void setFormato(String formato) {
		this.formato = formato;
	}

	public String getTentativiInvio() {
		return tentativiInvio;
	}

	public void setTentativiInvio(String tentativiInvio) {
		this.tentativiInvio = tentativiInvio;
	}

	public String getMessageId() {
		return messageId;
	}

	public void setMessageId(String messageId) {
		this.messageId = messageId;
	}

	public String getNote() {
		return note;
	}

	public void setNote(String note) {
		this.note = note;
	}
	
}
