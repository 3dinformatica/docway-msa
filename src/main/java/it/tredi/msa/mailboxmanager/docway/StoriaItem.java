package it.tredi.msa.mailboxmanager.docway;

import java.text.SimpleDateFormat;
import java.util.Date;

public class StoriaItem {
	
	private String type;
	private String oper;
	private String codOper;
	private String uffOper;
	private String codUffOper;
	private String nomePersona;
	private String codPersona;
	private String nomeUff;
	private String codUff;
	private String operatore;
	private String codOperatore;
	private String data;
	private String ora;
	
	public StoriaItem(String type) {
		this.type = type;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getOper() {
		return oper;
	}
	
	public void setOper(String oper) {
		this.oper = oper;
	}
	
	public String getCodOper() {
		return codOper;
	}
	
	public void setCodOper(String codOper) {
		this.codOper = codOper;
	}
	
	public String getUffOper() {
		return uffOper;
	}
	
	public void setUffOper(String uffOper) {
		this.uffOper = uffOper;
	}
	
	public String getCodUffOper() {
		return codUffOper;
	}
	
	public void setCodUffOper(String codUffOper) {
		this.codUffOper = codUffOper;
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
	
	public String getOperatore() {
		return operatore;
	}
	
	public void setOperatore(String operatore) {
		this.operatore = operatore;
	}
	
	public String getCodOperatore() {
		return codOperatore;
	}
	
	public void setCodOperatore(String codOperatore) {
		this.codOperatore = codOperatore;
	}
	
	public String getData() {
		return data;
	}
	
	public void setData(Date date) {
		this.data = new SimpleDateFormat("yyyyMMdd").format(date);
	}
	
	public void setData(String data) {
		this.data = data;
	}
	
	public String getOra() {
		return ora;
	}
	
	public void setOra(Date date) {
		this.ora = new SimpleDateFormat("HH:mm:ss").format(date);
	}	
	
	public void setOra(String ora) {
		this.ora = ora;
	}
	
	public static StoriaItem createFromRifInterno(RifInterno rifInterno) {
		String type = "";
		if (rifInterno.getDiritto().equalsIgnoreCase("RPA"))
			type = "responsabilita";
		else if (rifInterno.getDiritto().equalsIgnoreCase("CC"))
			type = "assegnazione_cc";
		else if (rifInterno.getDiritto().equalsIgnoreCase("CDS"))
			type = "assegnazione_cds";
		
		StoriaItem storiaItem = new StoriaItem(type);
		storiaItem.setNomePersona(rifInterno.getNomePersona());
		storiaItem.setCodPersona(rifInterno.getCodPersona());
		storiaItem.setNomeUff(rifInterno.getNomeUff());
		storiaItem.setCodUff(rifInterno.getCodUff());
		return storiaItem;
	}
	
}
