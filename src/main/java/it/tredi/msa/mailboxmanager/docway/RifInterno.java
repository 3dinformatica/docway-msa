package it.tredi.msa.mailboxmanager.docway;

public class RifInterno {
	
	private String nomePersona;
	private String nomeUff;
	private String codPersona;
	private String codUff;
	private String diritto;
	private String codFasc;
	private boolean intervento;
	private boolean ruolo = false;
	private boolean notify = false;
	
	public String getNomePersona() {
		return nomePersona;
	}
	
	public void setNomePersona(String nomePersona) {
		this.nomePersona = nomePersona;
	}
	
	public String getNomeUff() {
		return nomeUff;
	}
	
	public void setNomeUff(String nomeUff) {
		this.nomeUff = nomeUff;
	}
	
	public String getCodPersona() {
		return codPersona;
	}
	
	public void setCodPersona(String codPersona) {
		this.codPersona = codPersona;
	}
	
	public String getCodUff() {
		return codUff;
	}
	
	public void setCodUff(String codUff) {
		this.codUff = codUff;
	}
	
	public String getDiritto() {
		return diritto;
	}
	
	public void setDiritto(String diritto) {
		this.diritto = diritto;
	}
	
	public String getCodFasc() {
		return codFasc;
	}
	
	public void setCodFasc(String codFasc) {
		this.codFasc = codFasc;
	}

	public String getTipoUff() {
		return isRuolo() ? "ruolo" : "";
	}

	public boolean isIntervento() {
		return intervento;
	}

	public void setIntervento(boolean intervento) {
		this.intervento = intervento;
	}

	public boolean isRuolo() {
		return ruolo;
	}

	public void setRuolo(String nomeRuolo, String codRuolo) {
		this.ruolo = true;
		this.nomePersona = "Tutti";
		this.codPersona = "tutti_" + codRuolo;
		this.nomeUff = nomeRuolo;
		this.codUff = codRuolo;
	}

	public boolean isNotify() {
		return notify;
	}

	public void setNotify(boolean notify) {
		this.notify = notify;
	}
	
}
