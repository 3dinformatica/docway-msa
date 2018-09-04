package it.tredi.msa.mailboxmanager.docway.fatturapa.conf;

public class OggettoDocumentoBuilder {

	private OggettoParseMode parseMode;
	private String template;

	public OggettoDocumentoBuilder(OggettoParseMode parseMode) {
		this(parseMode, null);
	}

	public OggettoDocumentoBuilder(OggettoParseMode parseMode, String template) {
		if (parseMode == OggettoParseMode.CUSTOM && (template == null || template.isEmpty()))
			this.parseMode = OggettoParseMode.PREDEFINITO;
		else {
			this.parseMode = parseMode;
			this.template = template;
		}
	}

	public OggettoParseMode getParseMode() {
		return parseMode;
	}

	public String getTemplate() {
		return template;
	}

	public void setParseMode(OggettoParseMode parseMode) {
		this.parseMode = parseMode;
	}

	public void setTemplate(String template) {
		this.template = template;
	}

}