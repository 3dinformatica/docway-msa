package it.tredi.msa.entity;

public class DocwayMailboxconfiguration extends MailboxConfiguration {
	
	private boolean storeEml = false;
	
	private String xwHost;
	private int xwPort;
	private int xwUser;
	private int xwPassword;
	
	public boolean isStoreEml() {
		return storeEml;
	}
	
	public void setStoreEml(boolean storeEml) {
		this.storeEml = storeEml;
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
	
	public int getXwUser() {
		return xwUser;
	}
	
	public void setXwUser(int xwUser) {
		this.xwUser = xwUser;
	}
	
	public int getXwPassword() {
		return xwPassword;
	}
	
	public void setXwPassword(int xwPassword) {
		this.xwPassword = xwPassword;
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
