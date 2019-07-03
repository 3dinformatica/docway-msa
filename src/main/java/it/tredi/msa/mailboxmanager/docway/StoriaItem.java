package it.tredi.msa.mailboxmanager.docway;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Set completo di dati che identificano un elemento della storia del documento.
 */
// TODO sarebbe meglio gestirlo come set minimale di dati ed estendere una classe base con tutte le specializzazioni delle tipologie di azione
public class StoriaItem {
	
	/**
	 * Identifica la tipologia di item della storia (nome dell'elemento su dw4)
	 */
	private StoriaItemType itemType;
	
	/**
	 * Nome e Cognome dell'operatore
	 */
	private String oper;
	
	/**
	 * Codice dell'operatore
	 */
	private String codOper;
	
	/**
	 * Ufficio di appartenenza dell'operatore
	 */
	private String uffOper;
	
	/**
	 * Codice dell'ufficio di appartenenza dell'operatore
	 */
	private String codUffOper;
	
	/**
	 * Identificazione completa dell'operatore (nome e cognome + ufficio di appartenenza)
	 */
	private String operatore;
	
	/**
	 * Data dell'attivita'
	 */
	private String data;
	
	/**
	 * Ora dell'attivita'
	 */
	private String ora;
	
	// -------------------------------------------------------------------------------------------------------------
	// PARAMETRI RELATIVI ALLE ASSEGNAZIONI : INIZIO
	// -------------------------------------------------------------------------------------------------------------
	
	/**
	 * Nome della persona oggetto dell'azione (es. aggiunta di un utente come assegnatario del documento)
	 */
	private String nomePersona;
	
	/**
	 * Matricola della persona oggetto dell'azione (es. aggiunta di un utente come assegnatario del documento)
	 */
	private String codPersona;
	
	/**
	 * Nome dell'ufficio/gruppo/ruolo oggetto dell'azione (es. aggiunta di un utente/ruolo/... come assegnatario del documento)
	 */
	private String nomeUff;
	
	/**
	 * Codice dell'ufficio/gruppo/ruolo oggetto dell'azione (es. aggiunta di un utente/ruolo/... come assegnatario del documento)
	 */
	private String codUff;
	
	// -------------------------------------------------------------------------------------------------------------
	// PARAMETRI RELATIVI ALLE ASSEGNAZIONI : FINE
	// -------------------------------------------------------------------------------------------------------------
	
	/**
	 * Eventuale dettaglio dell'operazione (es. inserimento/rimozione in/da fasicolo)
	 */
	private String tipo;
	
	/**
	 * Codice del fascicolo in caso di fascicolazione automatica di documenti
	 */
	// mbernardini 03/07/2019 : aggiunto il codice del fascicolo in caso di fascicolazione automatica di documenti
	private String codice;
	
	/**
	 * Costruttore
	 * @param itemType Tipo di elemento della storia
	 */
	public StoriaItem(StoriaItemType itemType) throws RuntimeException {
		if (itemType == null)
			throw new RuntimeException("Impossibile identificare il tipo di azione sulla storia del documento... type = " + itemType);
		
		this.itemType = itemType;
	}
	
	public StoriaItemType getItemType() {
		return itemType;
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
	
	public String getCodice() {
		return codice;
	}

	public void setCodice(String codice) {
		this.codice = codice;
	}
	
	public String getTipo() {
		return tipo;
	}

	public void setTipo(String tipo) {
		this.tipo = tipo;
	}

	/**
	 * Creazione di un elemento della storia relativo all'aggiunta di un assegnatario ad un documento
	 * @param rifInterno Informazioni dell'assegnatario
	 * @return
	 */
	public static StoriaItem createFromRifInterno(RifInterno rifInterno) {
		StoriaItemType type = null;
		if (rifInterno.getDiritto().equalsIgnoreCase("RPA"))
			type = StoriaItemType.RESPONSABILITA;
		else if (rifInterno.getDiritto().equalsIgnoreCase("RPAM"))
			type = StoriaItemType.RESPONSABILITA_MINUTA;
		else if (rifInterno.getDiritto().equalsIgnoreCase("CC"))
			type = StoriaItemType.ASSEGNAZIONE_CC;
		else if (rifInterno.getDiritto().equalsIgnoreCase("CDS"))
			type = StoriaItemType.ASSEGNAZIONE_CDS;
		else if (rifInterno.getDiritto().equalsIgnoreCase("OP"))
			type = StoriaItemType.ASSEGNAZIONE_OP;
		else if (rifInterno.getDiritto().equalsIgnoreCase("OPM"))
			type = StoriaItemType.ASSEGNAZIONE_OPM;
		
		StoriaItem storiaItem = new StoriaItem(type);
		storiaItem.setNomePersona(rifInterno.getNomePersona());
		storiaItem.setCodPersona(rifInterno.getCodPersona());
		storiaItem.setNomeUff(rifInterno.getNomeUff());
		storiaItem.setCodUff(rifInterno.getCodUff());
		return storiaItem;
	}
	
	/**
	 * Creazione di un elemento della storia rilativo alla fascicolazione di un documento
	 * @param fascicolo Informazioni relative al fascicolo
	 * @return
	 */
	public static StoriaItem createFromAddFascicolo(FascicoloReference fascicolo) {
		StoriaItem storiaItem = new StoriaItem(StoriaItemType.IN_FASCICOLO);
		storiaItem.setTipo("inserimento");
		storiaItem.setCodice(fascicolo.getCodFascicolo());
		return storiaItem;
	}
	
}
