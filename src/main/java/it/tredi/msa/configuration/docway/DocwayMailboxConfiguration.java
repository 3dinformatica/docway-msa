package it.tredi.msa.configuration.docway;

import java.util.List;

import it.tredi.msa.configuration.MailboxConfiguration;

public class DocwayMailboxConfiguration extends MailboxConfiguration {
	
	private boolean storeEml = false;
	
	//email address
	private String email;
	
	private String tipoDoc; //arrivo, partenza, interno, varie
	private boolean bozza;
	private String codAmmAoo;
	private String tipologia;
	private String mezzoTrasmissione;
	private boolean currentYear;
	private boolean currentDate;
	
	//classificazione
	private String classif;
	private String classifCod;
	private String voceIndice;
	
	//operatore
	private String oper;
	private String uffOper;
	private boolean noteAutomatiche;
	
	//repertorio
	private String repertorio;
	private String repertorioCod;
	
	//responsabile/assegnatari
	private AssegnatarioMailboxConfiguration responsabile;
	private boolean daDestinatario;
	private boolean daMittente;
	private List<AssegnatarioMailboxConfiguration> assegnatariCC;
	private boolean daCopiaConoscenza;
	
	//email di notifica
	private boolean notifyRPA;
	private boolean notifyCC;
	private String notificationAppUri;
	private String notificationAppHost;
	private String notificationAppHost1;
	private boolean notificationEnabled;
	
	//creazione di un singolo documento per messageId
	private boolean createSingleDocByMessageId;
	
	public boolean isStoreEml() {
		return storeEml;
	}
	
	public void setStoreEml(boolean storeEml) {
		this.storeEml = storeEml;
	}
	
	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getTipoDoc() {
		return tipoDoc;
	}

	public void setTipoDoc(String tipoDoc) {
		this.tipoDoc = tipoDoc;
	}
	
	public boolean isBozza() {
		return bozza;
	}

	public void setBozza(boolean bozza) {
		this.bozza = bozza;
	}

	public String getCodAmmAoo() {
		return codAmmAoo;
	}

	public void setCodAmmAoo(String codAmmAoo) {
		this.codAmmAoo = codAmmAoo;
	}

	public String getTipologia() {
		return tipologia;
	}

	public void setTipologia(String tipologia) {
		this.tipologia = tipologia;
	}

	public String getMezzoTrasmissione() {
		return mezzoTrasmissione;
	}

	public void setMezzoTrasmissione(String mezzoTrasmissione) {
		this.mezzoTrasmissione = mezzoTrasmissione;
	}

	public boolean isCurrentYear() {
		return currentYear;
	}

	public void setCurrentYear(boolean currentYear) {
		this.currentYear = currentYear;
	}

	public boolean isCurrentDate() {
		return currentDate;
	}

	public void setCurrentDate(boolean currentDate) {
		this.currentDate = currentDate;
	}

	public String getClassif() {
		return classif;
	}

	public void setClassif(String classif) {
		this.classif = classif;
	}

	public String getClassifCod() {
		return classifCod;
	}

	public void setClassifCod(String classifCod) {
		this.classifCod = classifCod;
	}

	public String getOper() {
		return oper;
	}

	public void setOper(String oper) {
		this.oper = oper;
	}

	public String getUffOper() {
		return uffOper;
	}

	public void setUffOper(String uffOper) {
		this.uffOper = uffOper;
	}

	public boolean isNoteAutomatiche() {
		return noteAutomatiche;
	}

	public void setNoteAutomatiche(boolean noteAutomatiche) {
		this.noteAutomatiche = noteAutomatiche;
	}

	public String getVoceIndice() {
		return voceIndice;
	}

	public void setVoceIndice(String voceIndice) {
		this.voceIndice = voceIndice;
	}

	public String getRepertorio() {
		return repertorio;
	}

	public void setRepertorio(String repertorio) {
		this.repertorio = repertorio;
	}

	public String getRepertorioCod() {
		return repertorioCod;
	}

	public void setRepertorioCod(String repertorioCod) {
		this.repertorioCod = repertorioCod;
	}

	public AssegnatarioMailboxConfiguration getResponsabile() {
		return responsabile;
	}

	public void setResponsabile(AssegnatarioMailboxConfiguration responsabile) {
		this.responsabile = responsabile;
	}

	public boolean isDaDestinatario() {
		return daDestinatario;
	}

	public void setDaDestinatario(boolean daDestinatario) {
		this.daDestinatario = daDestinatario;
	}

	public boolean isDaMittente() {
		return daMittente;
	}

	public void setDaMittente(boolean daMittente) {
		this.daMittente = daMittente;
	}

	public List<AssegnatarioMailboxConfiguration> getAssegnatariCC() {
		return assegnatariCC;
	}

	public void setAssegnatariCC(List<AssegnatarioMailboxConfiguration> assegnatariCC) {
		this.assegnatariCC = assegnatariCC;
	}

	public boolean isDaCopiaConoscenza() {
		return daCopiaConoscenza;
	}

	public void setDaCopiaConoscenza(boolean daCopiaConoscenza) {
		this.daCopiaConoscenza = daCopiaConoscenza;
	}
	
	public String getOperatore() {
		return oper + "(" + uffOper + ")";
	}

	public boolean isNotifyRPA() {
		return notifyRPA;
	}

	public void setNotifyRPA(boolean notifyRPA) {
		this.notifyRPA = notifyRPA;
	}

	public boolean isNotifyCC() {
		return notifyCC;
	}

	public void setNotifyCC(boolean notifyCC) {
		this.notifyCC = notifyCC;
	}

	public String getNotificationAppUri() {
		return notificationAppUri;
	}

	public void setNotificationAppUri(String notificationAppUri) {
		this.notificationAppUri = notificationAppUri;
	}

	public String getNotificationAppHost() {
		return notificationAppHost;
	}

	public void setNotificationAppHost(String notificationAppHost) {
		this.notificationAppHost = notificationAppHost;
	}

	public String getNotificationAppHost1() {
		return notificationAppHost1;
	}

	public void setNotificationAppHost1(String notificationAppHost1) {
		this.notificationAppHost1 = notificationAppHost1;
	}

	public boolean isNotificationEnabled() {
		return notificationEnabled;
	}

	public void setNotificationEnabled(boolean notificationEnabled) {
		this.notificationEnabled = notificationEnabled;
	}

	public boolean isCreateSingleDocByMessageId() {
		return createSingleDocByMessageId;
	}

	public void setCreateSingleDocByMessageId(boolean createSingleDocByMessageId) {
		this.createSingleDocByMessageId = createSingleDocByMessageId;
	}
	
	
	/**
	 * 
splitEmail.globalAttachments=daticert.xml,smime.p7s
splitEmail.allowedExtensions=doc,docx,odt,ott,xls,xlsx,ods,ots,ppt,pptx,odp,otp,rtf,pdf,txt,tiff,tiff,p7m
splitEmail.enableLinkInterni=false

-fcs

enableRifiutoPecByAttachments=false
rifiutoPec.allowedAttachments=
rifiutoPec.mailRifiuto.testo=

enableFatturePA=false
sdiDomainAddress=@pec.fatturapa.it
fatturePA.codRepertorio=FTRPAP
fatturePA.descrRepertorio=Fattura Passiva
fatturePA.descrClassificazione=00/00 - Non Classificato
fatturePA.voceIndice=
fatturaPA.overwriteOggettoEmail=false

unknownInteropDestiny=import
useFileNameForAttachments=true

commandEncoding=UTF-8

createOneDocByMessageId=false
	 * 
	 * 
	 * 
	 * 
	 * 
	 * 
	 * 
	 * 
 	
<casellaPostaElettronica	 	splitByAttachments = "false" oper = "Archiviatore Email" nome = "Prova" uff_oper = "Protocollo" documentModel = "bozze_arrivo" interop = "no" cod_amm = "3DIN" cod_aoo = "BOL" nrecord = "00001322" cod_aoo_segnatura = "" db = "" cod_amm_segnatura = "" protocollaFattura = "false" >
- 	
<gestori_mailbox	>
	
<gestore	 	nome_pers = "Pascale Marvin" matricola = "PI000155" livello = "titolare" />
</gestori_mailbox>
	
<mailbox_in	 	email = "test-archiviatore-xw@libero.it" host = "imapmail.libero.it" login = "test-archiviatore-xw@libero.it" protocol = "imaps" password = "U/dAdqJZ4JwlhmYdWrtBgA==" port = "993" />
	
<responsabile	 	cod_uff = "SI000010" daCopiaConoscenza = "no" daDestinatario = "no" daMittente = "no" matricola = "PI000056" nome_uff = "Servizio archivistico" nome_pers = "Candelora Nicola" cod_ruolo = "" nome_ruolo = "" />
+	
<storia	>
</storia>
	
<mailbox_out	 	email = "test-archiviatore-xw@libero.it" port = "25" login = "test-archiviatore-xw@libero.it" host = "smtp.libero.it" protocol = "smtp" password = "U/dAdqJZ4JwlhmYdWrtBgA==" />
	
<notify	 	uri = "" rpa = "false" httpHost = "" />
	
<tag	 	value = "abilitata" />
- 	
<assegnazione_cc	>
	
<assegnatario	 	intervento = "no" />
</assegnazione_cc>
</casellaPostaElettronica>
	 * 
	 * 
	 * 
	 * 
	 */

}
