package it.tredi.msa.mailboxmanager.docway.utils;

/**
 * Utility di formattazione in base all'aspetto di classificazione settato.
 * Metodi estratti dal codice di DocWay4
 */
public class AspettoClassificazioneUtils {

	public final static String DEFAULT_ASPETTO_CLASSIFICAZIONE = "R/D";

	/**
	 * simone - 22 Nov 2004 Restituisce il numero di fascicolo stile docway.
	 * Esempio:<br>
	 * <br>
	 *
	 * input: 2004-3DINBOL-04/03/02.00001.00002.00004<br>
	 * output: 2004-IV/3/2.1.2.4<br>
	 * <br>
	 *
	 * input: 2004-3DINBOL-00001.00002.00004<br>
	 * output: 2004-1.2.4
	 *
	 * @param numero        numero del fascicolo nel formato del file xml
	 * @param classifFormat formattazione
	 *
	 * @return numero di fascicolo stile docway
	 *
	 */
	public static String printFascNum(String numero, String classifFormat) {
		int index;
		String ret;
		if (numero.indexOf("/") != -1) {
			// numero di fascicolo con classificazione
			// RW0049984 - Prendere primo '.' dopo la classificazione per possibili punti
			// nella amm_aoo
			index = numero.indexOf(".", numero.indexOf("/"));

			// mbernardini 28/02/2017 : corretto bug in formattazione della classificazione
			String[] parts = numero.substring(0, index).split("-");
			if (parts.length == 3)
				ret = parts[0] + "-" + printClassif(parts[2], classifFormat) + ".";
			else // TODO teoricamente non dovrebbe mai entrare nel ramo else
				ret = numero.substring(0, 5) + printClassif(numero.substring(13, index), classifFormat) + ".";
		} else {
			// numero di fascicolo privo di classificazione
			index = numero.lastIndexOf("-");
			ret = numero.substring(0, 5);
		}

		numero = numero.substring(index + 1);

		while ((index = numero.indexOf(".")) != -1) {
			ret += trimLeftZeros(numero.substring(0, index)) + ".";
			numero = numero.substring(index + 1);
		}

		ret += trimLeftZeros(numero);

		return ret;
	}

	/**
	 * simone - 22 Nov 2004 Restituisce la classificazione stile docway.
	 * Esempio:<br>
	 * <br>
	 *
	 * input: 04/03/02 output: IV/3/2
	 *
	 * @param classif numero del fascicolo nel formato del file xml
	 * @param format  formattazione
	 *
	 * @return numero di fascicolo stile docway
	 *
	 */
	public static String printClassif(String classif, String format) {
		if (classif.length() == 0)
			return "";

		if (format == null || format.length() == 0)
			format = DEFAULT_ASPETTO_CLASSIFICAZIONE;

		String titoloFormat = getPartialFormat(format);// titolo

		if (titoloFormat.length() == 2)
			format = format.substring(2);
		else
			format = format.substring(1);

		String separator = format.substring(0, 1);// separatore

		String classeFormat = getPartialFormat(format.substring(1));// classe

		int index = classif.indexOf("/");
		if (index == -1) // sstagni - 6 Mar 2006 - un solo livello
			return getFormattedPart(classif, titoloFormat);

		String ret = getFormattedPart(classif.substring(0, index), titoloFormat);

		// classe
		classif = classif.substring(index + 1);
		while ((index = classif.indexOf("/")) != -1) {
			ret += separator + getFormattedPart(classif.substring(0, index), classeFormat);
			classif = classif.substring(index + 1);
		}
		ret += separator + getFormattedPart(classif, classeFormat);
		return ret;
	}

	// usato da: printClassif(String, String)
	private static String getPartialFormat(String format) {
		if (format.startsWith("AA"))
			return "AA";
		else if (format.startsWith("A"))
			return "A";
		else if (format.startsWith("DD"))
			return "DD";
		else if (format.startsWith("D"))
			return "D";
		if (format.startsWith("R"))
			return "R";

		return (format.length() <= 2) ? "D" : "R"; // default per titolo e classe
	}

	// usato da: printClassif(String, String)
	private static String getFormattedPart(String val, String format) {
		String[] alfa = { "0", "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R",
				"S", "T", "U", "V", "W", "X", "Y", "Z" };
		val = trimLeftZeros(val);
		if (format.equals("R"))
			return arabToRoman(val);
		else if (format.equals("DD")) {
			if (val.equals("0"))
				return val;
			else
				return fillChars(val, "0", 2);
		} else if (format.equals("D"))
			return val;
		else if (format.equals("AA")) {
			int x = Integer.parseInt(val);
			if (x <= 26 && x > 0)
				return "A" + alfa[x];
			else
				return alfa[x];
		} else if (format.equals("A")) {
			int x = Integer.parseInt(val);
			return alfa[x];
		}

		return "";
	}

	/**
	 * Autore: simone - 9 Set 2004 Trasforma un numero intero compreso tra 0 e 30
	 * (espersso come stringa) in un numero romano
	 * 
	 * @param num numero arabo in ingresso
	 * @return numero romano corrispondente
	 */
	private static String arabToRoman(String num) {
		if (num.equals("0"))
			return "0";
		else if (num.equals("1"))
			return "I";
		else if (num.equals("2"))
			return "II";
		else if (num.equals("3"))
			return "III";
		else if (num.equals("4"))
			return "IV";
		else if (num.equals("5"))
			return "V";
		else if (num.equals("6"))
			return "VI";
		else if (num.equals("7"))
			return "VII";
		else if (num.equals("8"))
			return "VIII";
		else if (num.equals("9"))
			return "IX";
		else if (num.equals("10"))
			return "X";
		else if (num.equals("11"))
			return "XI";
		else if (num.equals("12"))
			return "XII";
		else if (num.equals("13"))
			return "XIII";
		else if (num.equals("14"))
			return "XIV";
		else if (num.equals("15"))
			return "XV";
		else if (num.equals("16"))
			return "XVI";
		else if (num.equals("17"))
			return "XVII";
		else if (num.equals("18"))
			return "XVIII";
		else if (num.equals("19"))
			return "XIX";
		else if (num.equals("20"))
			return "XX";
		else if (num.equals("21"))
			return "XXI";
		else if (num.equals("22"))
			return "XXII";
		else if (num.equals("23"))
			return "XXIII";
		else if (num.equals("24"))
			return "XXIV";
		else if (num.equals("25"))
			return "XXV";
		else if (num.equals("26"))
			return "XXVI";
		else if (num.equals("27"))
			return "XXVII";
		else if (num.equals("28"))
			return "XXVIII";
		else if (num.equals("29"))
			return "XXIX";
		else if (num.equals("30"))
			return "XXX";

		return "";
	}

	private static String fillChars(String s, String c, int n) {
		for (int i = s.length(); i < n; i++)
			s = c + s;
		return s;
	}

	/**
	 * Autore: simone - 9 Set 2004 Elimina gli zeri in testa a una stringa. Se la
	 * string è fatta di tutti zeri restituisce "0"
	 * 
	 * @param s stringa da elaborare
	 * @return stringa elaborata
	 */
	public static String trimLeftZeros(String s) {
		if (s != null && s.length() > 0)
			while (s.length() > 0 && s.charAt(0) == '0')
				s = s.substring(1);

		if (s.length() == 0)
			return "0";

		return s;
	}
}
