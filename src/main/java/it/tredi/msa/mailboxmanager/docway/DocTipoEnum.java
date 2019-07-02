package it.tredi.msa.mailboxmanager.docway;

import it.tredi.msa.configuration.docway.DocwayMailboxConfiguration;

/**
 * Tipologie di documento supportate da docway
 */
public enum DocTipoEnum {

	/**
	 * Documento non protocollato
	 */
	VARIE(DocwayMailboxConfiguration.DOC_TIPO_VARIE) {
		@Override
		public boolean isValidValue() {
			return true;
		}

		@Override
		public boolean isProtocollabile() {
			return false;
		}
	},
	
	/**
	 * Documento in entrata
	 */
	ARRIVO(DocwayMailboxConfiguration.DOC_TIPO_ARRIVO) {
		@Override
		public boolean isValidValue() {
			return true;
		}

		@Override
		public boolean isProtocollabile() {
			return true;
		}
	},
	
	/**
	 * Documento in uscita
	 */
	PARTENZA(DocwayMailboxConfiguration.DOC_TIPO_PARTENZA) {
		@Override
		public boolean isValidValue() {
			return true;
		}

		@Override
		public boolean isProtocollabile() {
			return true;
		}
	},
	
	/**
	 * Documento tra uffici
	 */
	INTERNO(DocwayMailboxConfiguration.DOC_TIPO_INTERNO) {
		@Override
		public boolean isValidValue() {
			return true;
		}

		@Override
		public boolean isProtocollabile() {
			return true;
		}
	},
	
	/**
	 * Nessuna tipologia indicata
	 */
	NOONE("") {
		@Override
		public boolean isValidValue() {
			return false;
		}

		@Override
		public boolean isProtocollabile() {
			return false;
		}
	};
	
	private String text;

	DocTipoEnum(String text) {
		this.text = text;
	}

	public String getText() {
		return this.text;
	}
	
	/**
	 * Ritorna TRUE se il valore dell'enum fa riferimento ad un tipo doc valido, FALSE altrimenti
	 * @return
	 */
	public abstract boolean isValidValue();
	
	/**
	 * Ritorna TRUE se il valore dell'enum corrisponde ad un tipo doc protocollabile, FALSE altrimenti
	 * @return
	 */
	public abstract boolean isProtocollabile();

	public static DocTipoEnum fromString(String text) {
		if (text != null && !text.isEmpty()) {
			for (DocTipoEnum type : DocTipoEnum.values()) {
				if (type.text.equalsIgnoreCase(text)) {
					return type;
				}
			}
		}
		return NOONE;
	}
	
}
