package it.tredi.msa.configuration.docway;

public class AssegnatarioMailboxConfiguration {
	
	private String tipo;
	private String nomePersona;
	private String codPersona;
	private String nomeUff;
	private String codUff;
	private String nomeRuolo;
	private String codRuolo;
    private boolean intervento = false;
	
    public String getTipo() {
		return tipo;
	}

	public void setTipo(String tipo) {
		this.tipo = tipo;
	}

	public String getNomePersona() {
		return nomePersona;
	}
	
	public void setNomePersona(String nomePersona) {
		this.nomePersona = nomePersona;
	}
	
	public String getCodPersona() {
		return codPersona;
	}
	
	public void setCodPersona(String codPersona) {
		this.codPersona = codPersona;
	}
	
	public String getNomeUff() {
		return nomeUff;
	}
	
	public void setNomeUff(String nomeUff) {
		this.nomeUff = nomeUff;
	}
	
	public String getCodUff() {
		return codUff;
	}
	
	public void setCodUff(String codUff) {
		this.codUff = codUff;
	}
	
	public String getNomeRuolo() {
		return nomeRuolo;
	}
	
	public void setNomeRuolo(String nomeRuolo) {
		this.nomeRuolo = nomeRuolo;
	}
	
	public String getCodRuolo() {
		return codRuolo;
	}
	
	public void setCodRuolo(String codRuolo) {
		this.codRuolo = codRuolo;
	}
	
	public boolean isRuolo() {
		return (codRuolo != null && !codRuolo.isEmpty());
	}
	
	public boolean isIntervento() {
		return isRPA()? true : intervento;
	}

	public void setIntervento(boolean intervento) {
		this.intervento = intervento;
	}
    
	public boolean isRPA() {
		return tipo.equalsIgnoreCase("RPA");
	}
	
}
