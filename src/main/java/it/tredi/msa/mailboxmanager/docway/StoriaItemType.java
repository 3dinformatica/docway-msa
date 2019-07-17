package it.tredi.msa.mailboxmanager.docway;

public enum StoriaItemType {

	CREAZIONE("creazione"),
	RESPONSABILITA("responsabilita"),
	RESPONSABILITA_MINUTA("responsabilita_minuta"),
	ASSEGNAZIONE_CC("assegnazione_cc"),
	ASSEGNAZIONE_CDS("assegnazione_cds"),
	ASSEGNAZIONE_OP("assegnazione_op"),
	ASSEGNAZIONE_OPM("assegnazione_opm"),
	IN_FASCICOLO("in_fascicolo");
	
	private String text;

	StoriaItemType(String text) {
		this.text = text;
	}

	public String getText() {
		return this.text;
	}
	
}
