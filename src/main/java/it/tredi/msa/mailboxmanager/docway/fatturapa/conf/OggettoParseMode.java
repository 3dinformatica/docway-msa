package it.tredi.msa.mailboxmanager.docway.fatturapa.conf;

public enum OggettoParseMode {

	/**
	 * Viene mantenuto l'oggetto del documento (nessuna lettura dalla fatturaPA inclusa)
	 */
	NO_OVERWRITE,

	/**
	 * L'oggetto del documento viene compilato con il contenuto della causale della fattura. Se la causale non è
	 * definita si ottiene lo stesso comportamento del caso PREDEFINITO
	 */
	CAUSALE,

	/**
	 * L'oggetto del documento viene formattato nel modo seguente:
	 * "Fattura di [AZIENDA] n. [NUMFATTURA] del [DATAFATTURA]" se fattura singola e causale non definita;
	 * "Lotto di fatture di [AZIENDA] del [DATAFATTURA]" se lotto di fatture e causale non definita.
	 */
	PREDEFINITO,

	/**
	 * L'oggetto del documento viene generato attraverso uno specifico modello (da passare come parametro). Nel caso il modello
	 * non sia definito si ottiene lo stesso comportamento del caso PREDEFINITO
	 */
	CUSTOM,
	;


	/**
	 * Recupero della modalita' di parsing data una stringa
	 * @param label
	 * @return
	 */
	public static OggettoParseMode getParseMode(String label) {
		if (label == null)
			label = "";

		if (label.equals("nooverwrite"))
			return NO_OVERWRITE;
		else if (label.equals("causale"))
			return CAUSALE;
		else if (label.equals("custom"))
			return CUSTOM;
		else // caso di default = "predefinito"
			return PREDEFINITO;
	}

}
