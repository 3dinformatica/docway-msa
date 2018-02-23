package it.tredi.msa.entity.docway;

import it.tredi.msa.entity.MailboxConfiguration;

public class DocwayMailboxConfiguration extends MailboxConfiguration {
	
	private boolean storeEml = false;
	
	private String email;
	
	//xw
	private String xwHost;
	private int xwPort;
	private String xwUser;
	private String xwPassword;
	private String xwDb;
	private String aclDb;
	
	private String tipoDoc; //arrivo, partenza, interno, varie
	private boolean bozza;
	private String codAmmAoo;
	private String tipologia;
	private String mezzoTrasmissione;
	private boolean currentYear;
	private boolean currentDate;
	private String classif;
	private String classifCod;	
	private String oper;
	private String uffOper;
	private boolean noteAutomatiche;
	private String voceIndice;
	private String repertorio;
	private String repertorioCod;
	
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

	public String getXwHost() {
		return xwHost;
	}
	
	public void setXwHost(String xwHost) {
		this.xwHost = xwHost;
	}
	
	public int getXwPort() {
		return xwPort;
	}
	
	public void setXwPort(int xwPort) {
		this.xwPort = xwPort;
	}
	
	public String getXwUser() {
		return xwUser;
	}

	public void setXwUser(String xwUser) {
		this.xwUser = xwUser;
	}

	public String getXwPassword() {
		return xwPassword;
	}

	public void setXwPassword(String xwPassword) {
		this.xwPassword = xwPassword;
	}

	public String getXwDb() {
		return xwDb;
	}

	public void setXwDb(String xwDb) {
		this.xwDb = xwDb;
	}
	
	public String getAclDb() {
		return aclDb;
	}

	public void setAclDb(String aclDb) {
		this.aclDb = aclDb;
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

addOriginalMessageToDocument=true

loadAndLockAttemps=10
loadAndLockDelay=6000
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
