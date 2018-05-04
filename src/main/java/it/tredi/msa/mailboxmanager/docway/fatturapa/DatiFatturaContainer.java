package it.tredi.msa.mailboxmanager.docway.fatturapa;

import java.util.List;

public class DatiFatturaContainer {
	
	//DatiGeneraliDocumento
	private String tipoDocumento_dg;
	private String divisa_dg;
	private String data_dg;
	private String numero_dg;
	private String importoTotaleDocumento_dg;
	private String arrotondamento_dg;
	private String art73_dg;
	private String causale_dg;
	
	//DatiOrdineAcquisto
	private List<DatiFatturaPAItem> datiOrdineAcquisto;
	
	//DatiContratto
	private List<DatiFatturaPAItem> datiContratto;		
	
	//DatiConvenzione
	private List<DatiFatturaPAItem> datiConvenzione;	
	
	//DatiRicezione
	private List<DatiFatturaPAItem> datiRicezione;
	
	//DatiFattureCollegate
	private List<DatiFatturaPAItem> datiFattureCollegate;
	
	//DatiSAL
	private List<String> riferimentoFaseSAL;
	
	//DatiDDT
	private List<DatiDDTItem> datiDDT;
	
	//DatiBeniServizi
	private DatiBeniServiziItem datiBeniServizi;
	
	//DatiRegistroFatture
	private DatiRegistroFattureItem datiRegistroFatture;

	public String getTipoDocumento_dg() {
		return tipoDocumento_dg;
	}

	public void setTipoDocumento_dg(String tipoDocumento_dg) {
		this.tipoDocumento_dg = tipoDocumento_dg;
	}

	public String getDivisa_dg() {
		return divisa_dg;
	}

	public void setDivisa_dg(String divisa_dg) {
		this.divisa_dg = divisa_dg;
	}

	public String getData_dg() {
		return data_dg;
	}

	public void setData_dg(String data_dg) {
		this.data_dg = data_dg;
	}

	public String getNumero_dg() {
		return numero_dg;
	}

	public void setNumero_dg(String numero_dg) {
		this.numero_dg = numero_dg;
	}

	public String getImportoTotaleDocumento_dg() {
		return importoTotaleDocumento_dg;
	}

	public void setImportoTotaleDocumento_dg(String importoTotaleDocumento_dg) {
		this.importoTotaleDocumento_dg = importoTotaleDocumento_dg;
	}

	public String getArrotondamento_dg() {
		return arrotondamento_dg;
	}

	public void setArrotondamento_dg(String arrotondamento_dg) {
		this.arrotondamento_dg = arrotondamento_dg;
	}

	public String getArt73_dg() {
		return art73_dg;
	}

	public void setArt73_dg(String art73_dg) {
		this.art73_dg = art73_dg;
	}

	public String getCausale_dg() {
		return causale_dg;
	}

	public void setCausale_dg(String causale_dg) {
		this.causale_dg = causale_dg;
	}

	public List<DatiFatturaPAItem> getDatiOrdineAcquisto() {
		return datiOrdineAcquisto;
	}

	public void setDatiOrdineAcquisto(List<DatiFatturaPAItem> datiOrdineAcquisto) {
		this.datiOrdineAcquisto = datiOrdineAcquisto;
	}

	public List<DatiFatturaPAItem> getDatiContratto() {
		return datiContratto;
	}

	public void setDatiContratto(List<DatiFatturaPAItem> datiContratto) {
		this.datiContratto = datiContratto;
	}

	public List<DatiFatturaPAItem> getDatiConvenzione() {
		return datiConvenzione;
	}

	public void setDatiConvenzione(List<DatiFatturaPAItem> datiConvenzione) {
		this.datiConvenzione = datiConvenzione;
	}

	public List<DatiFatturaPAItem> getDatiRicezione() {
		return datiRicezione;
	}

	public void setDatiRicezione(List<DatiFatturaPAItem> datiRicezione) {
		this.datiRicezione = datiRicezione;
	}

	public List<DatiFatturaPAItem> getDatiFattureCollegate() {
		return datiFattureCollegate;
	}

	public void setDatiFattureCollegate(List<DatiFatturaPAItem> datiFattureCollegate) {
		this.datiFattureCollegate = datiFattureCollegate;
	}

	public List<String> getRiferimentoFaseSAL() {
		return riferimentoFaseSAL;
	}

	public void setRiferimentoFaseSAL(List<String> riferimentoFaseSAL) {
		this.riferimentoFaseSAL = riferimentoFaseSAL;
	}

	public List<DatiDDTItem> getDatiDDT() {
		return datiDDT;
	}

	public void setDatiDDT(List<DatiDDTItem> datiDDT) {
		this.datiDDT = datiDDT;
	}

	public DatiBeniServiziItem getDatiBeniServizi() {
		return datiBeniServizi;
	}

	public void setDatiBeniServizi(DatiBeniServiziItem datiBeniServizi) {
		this.datiBeniServizi = datiBeniServizi;
	}

	public DatiRegistroFattureItem getDatiRegistroFatture() {
		return datiRegistroFatture;
	}

	public void setDatiRegistroFatture(DatiRegistroFattureItem datiRegistroFatture) {
		this.datiRegistroFatture = datiRegistroFatture;
	}
	
}
