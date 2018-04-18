package it.tredi.msa.mailboxmanager.docway;

import java.util.ArrayList;
import java.util.List;

public class RifEsterno {
	
	private String nome;
	private String cod;
	private String emailCertificata;
	private String codiceFiscale;
	private String partitaIva;
	private String indirizzo;
	private String email;
	private String fax;
	private String tel;
	private String referenteNominativo;
	private String referenteCod;
	private String CodiceAmministrazione;
	private String CodiceAOO;
	private String dataProt;
	private String nProt;
	private List<InteroperabilitaItem> interoperabilitaItemL;
	
	public RifEsterno() {
		super();
		this.interoperabilitaItemL = new ArrayList<>();
	}

	public String getNome() {
		return nome;
	}
	
	public void setNome(String nome) {
		this.nome = nome;
	}
	
	public String getCod() {
		return cod;
	}
	
	public void setCod(String cod) {
		this.cod = cod;
	}
	
	public String getEmailCertificata() {
		return emailCertificata;
	}
	
	public void setEmailCertificata(String emailCertificata) {
		this.emailCertificata = emailCertificata;
	}
	
	public String getCodiceFiscale() {
		return codiceFiscale;
	}
	
	public void setCodiceFiscale(String codiceFiscale) {
		this.codiceFiscale = codiceFiscale;
	}
	
	public String getPartitaIva() {
		return partitaIva;
	}
	
	public void setPartitaIva(String partitaIva) {
		this.partitaIva = partitaIva;
	}
	
	public String getIndirizzo() {
		return indirizzo;
	}
	
	public void setIndirizzo(String indirizzo) {
		this.indirizzo = indirizzo;
	}
	
	public String getEmail() {
		return email;
	}
	
	public void setEmail(String email) {
		this.email = email;
	}
	
	public String getFax() {
		return fax;
	}
	
	public void setFax(String fax) {
		this.fax = fax;
	}
	
	public String getTel() {
		return tel;
	}
	
	public void setTel(String tel) {
		this.tel = tel;
	}
	
	public String getReferenteNominativo() {
		return referenteNominativo;
	}
	
	public void setReferenteNominativo(String referenteNominativo) {
		this.referenteNominativo = referenteNominativo;
	}
	
	public String getReferenteCod() {
		return referenteCod;
	}
	
	public void setReferenteCod(String referenteCod) {
		this.referenteCod = referenteCod;
	}

	public String getCodiceAmministrazione() {
		return CodiceAmministrazione;
	}

	public void setCodiceAmministrazione(String codiceAmministrazione) {
		CodiceAmministrazione = codiceAmministrazione;
	}

	public String getCodiceAOO() {
		return CodiceAOO;
	}

	public void setCodiceAOO(String codiceAOO) {
		CodiceAOO = codiceAOO;
	}

	public String getDataProt() {
		return dataProt;
	}

	public void setDataProt(String dataProt) {
		this.dataProt = dataProt;
	}

	public String getnProt() {
		return nProt;
	}

	public void setnProt(String nProt) {
		this.nProt = nProt;
	}

	public List<InteroperabilitaItem> getInteroperabilitaItemL() {
		return interoperabilitaItemL;
	}

	public void setInteroperabilitaItemL(List<InteroperabilitaItem> interoperabilitaItemL) {
		this.interoperabilitaItemL = interoperabilitaItemL;
	}

	public void addInteroperabilitaItem(InteroperabilitaItem interoperabilitaItem) {
		this.interoperabilitaItemL.add(interoperabilitaItem);
	}
	
}
