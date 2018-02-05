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
	 */

}
