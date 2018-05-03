package it.tredi.msa.mailboxmanager.docway;

import java.util.List;

public class DatiBeniServiziItem {
	
	private List<DatiLineaItem>  linea;
	private List<DatiRiepilogoItem>  riepilogo;
	
	public List<DatiLineaItem> getLinea() {
		return linea;
	}
	
	public void setLinea(List<DatiLineaItem> linea) {
		this.linea = linea;
	}

	public void addLinea(DatiLineaItem lineaItem) {
		this.linea.add(lineaItem);
	}
	
	public List<DatiRiepilogoItem> getRiepilogo() {
		return riepilogo;
	}
	
	public void setRiepilogo(List<DatiRiepilogoItem> riepilogo) {
		this.riepilogo = riepilogo;
	}
	
	public void addRiepilogo(DatiRiepilogoItem riepilogoItem) {
		this.riepilogo.add(riepilogoItem);
	}
	
}