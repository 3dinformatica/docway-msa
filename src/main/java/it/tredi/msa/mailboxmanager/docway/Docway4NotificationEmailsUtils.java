package it.tredi.msa.mailboxmanager.docway;

import java.util.List;

import org.dom4j.Document;
import org.dom4j.Element;

import it.tredi.msa.configuration.docway.DocwayMailboxConfiguration;

public class Docway4NotificationEmailsUtils {

	private static final String _DB_ ="%DB%";
	private static final String _ALIAS_ ="%ALIAS%";
	private static final String _NRECORD_ ="%NRECORD%";
	private static final int MITT_DEST_LENGTH = 25;
	private static final int OGGETTO_LENGTH = 25;    
	public final static String TUTTI_COD = "tutti";	

	public static String getBodyForEmail(String httpHost, String httpHost1, String theURL, String db, Document document) throws java.net.MalformedURLException {
		String nrecord = document.getRootElement().attributeValue("nrecord");
		String tipo_doc = document.getRootElement().attributeValue("tipo");
		String data_prot = document.getRootElement().attributeValue("data_prot");

		String num_prot = document.getRootElement().attributeValue("num_prot");
		if (num_prot != null && num_prot.length() > 0) {
			if (num_prot.length() >= 13)
				num_prot = "N. " + deleteZeros(num_prot.substring(13)) + " del " + data_prot + " (" + num_prot + ")";
			else {
				String d = document.selectSingleNode("/doc/storia/creazione/@data").getText();
				num_prot = "Bozza del " + dateFormat(d);
			}
		}
		// Federico 19/12/05: aggiunto test su 'data_prot' in quanto può essere assente [RW 0032968]
		else if (data_prot != null && data_prot.length() > 0) {
			num_prot = "Documento non protocollato del " + dateFormat(data_prot);
		}
		else {
			num_prot = "Documento non protocollato";
		}

		String mittOrDest = "";
		if (tipo_doc == null)
			num_prot = tipo_doc = "";
		if (tipo_doc.equals(DocwayMailboxConfiguration.DOC_TIPO_ARRIVO))
			mittOrDest = "\nMittente: ";
		else if (tipo_doc.equals(DocwayMailboxConfiguration.DOC_TIPO_PARTENZA))
			mittOrDest = "\nDestinatario: ";
		if (mittOrDest.length() > 0) {
			@SuppressWarnings("unchecked")
			List<Element> l = (List<Element>)document.selectNodes("/doc/rif_esterni/rif/nome");
			if (l.size() > 0)
				mittOrDest += ((Element) l.get(0)).getText();
		}

		String oggetto = "\nOggetto: " + document.getRootElement().elementText("oggetto");
		String tmpURL = getNotifyURL(theURL, httpHost, httpHost1, db, "docnrecord", nrecord);
		String ret = num_prot + mittOrDest + oggetto + "\n\nPer visualizzare:\n " + tmpURL;

		return ret;
	}

	private static String deleteZeros(String s) {
		while (s.charAt(0) == '0')
			s = s.substring(1);
		return s;
	}    

	private static String dateFormat(String s) {
		if (s.length() == 8)
			return s.substring(6, 8) + "/" + s.substring(4, 6) + "/" + s.substring(0, 4);
		else
			return s;
	}   

	private static String getNotifyURL(String theURL, String httpHost, String httpHost1, String db, String alias, String nrecord) throws java.net.MalformedURLException {
		String newURL = theURL;
		String newURL1 = theURL;
		boolean completeUrl = false;
		boolean completeUrl1 = false;

		// sstagni - 15 Nov 2006 - se url contiene hcadm.dll viene modificata in hcprot.dll
		// FindBug: Dead store to theURL in it.highwaytech.apps.generic.Protocollo.getNotifyURL()
		//if (theURL.indexOf("hcadm.dll") != -1)
		//    theURL = theURL.replaceAll("hcadm.dll", "hcprot.dll");

		if (httpHost.length() > 0) {
			// si verifica se la property è già  esaustiva...
			if ( (httpHost.indexOf(_DB_) > -1)    ||
					(httpHost.indexOf(_ALIAS_) > -1) ||
					(httpHost.indexOf(_NRECORD_) > -1) ) {
				completeUrl = true;
				newURL = ((httpHost.replaceAll(_DB_, db)).replaceAll(_ALIAS_, alias)).replaceAll(_NRECORD_, nrecord);
			}
			else if (newURL.indexOf("//") == -1) newURL = httpHost + newURL;
			else {
				int index = newURL.indexOf("//");
				index = newURL.indexOf("/", index + 2);
				newURL = httpHost + newURL.substring(index);
			}
		}
		if (httpHost1.length() > 0) {
			// si verifica se la property è già  esaustiva...
			if ( (httpHost1.indexOf(_DB_) > -1)    ||
					(httpHost1.indexOf(_ALIAS_) > -1) ||
					(httpHost1.indexOf(_NRECORD_) > -1) ) {
				completeUrl1 = true;
				newURL1 = ((httpHost1.replaceAll(_DB_, db)).replaceAll(_ALIAS_, alias)).replaceAll(_NRECORD_, nrecord);
			}
			else if (newURL1.indexOf("//") == -1) newURL1 = httpHost1 + newURL1;
			else {
				int index = newURL1.indexOf("//");
				index = newURL1.indexOf("/", index + 2);
				newURL1 = httpHost1 + newURL1.substring(index);
			}
		}
		else newURL1 = "";

		String  tmpURL = "";
		String tmpURL1 = "";
		if ( !completeUrl )   tmpURL = "?db=" + db + "&verbo=queryplain&query=%5B" + alias + "%5D%3D" + nrecord;
		if ( !completeUrl1 ) tmpURL1 = "?db=" + db + "&verbo=queryplain&query=%5B" + alias + "%5D%3D" + nrecord;

		String defURL = newURL + tmpURL;
		java.net.URL url = new java.net.URL(defURL);
		defURL = url.toExternalForm();

		String defURL1 = "";
		if (newURL1.length() > 0) {
			defURL1 = newURL1 + tmpURL1;
			java.net.URL url1 = new java.net.URL(defURL1);
			defURL1 = url1.toExternalForm();
		}

		String ret = defURL;
		if (defURL1.length() > 0)
			ret += "\n\n" + defURL1;
		return ret;
	}    

	public static String getSubjectForEmail(String type, Document document) throws java.net.MalformedURLException {
		String tipo_doc = document.getRootElement().attributeValue("tipo");

		String mittOrDest = "";
		if (tipo_doc.equals(DocwayMailboxConfiguration.DOC_TIPO_ARRIVO) || tipo_doc.equals(DocwayMailboxConfiguration.DOC_TIPO_PARTENZA)) {
			@SuppressWarnings("unchecked")
			List<Element> l = (List<Element>)document.selectNodes("/doc/rif_esterni/rif/nome");            
			if (l.size() > 0)
				mittOrDest += ((Element) l.get(0)).getText();
		}
		String oggetto = document.getRootElement().elementText("oggetto");
		String ret = "[" + type + "]" + getMittDestSubjectFor(mittOrDest) + ":" + getOggettoSubjectFor(oggetto);
		return ret;
	}   

	private static String getMittDestSubjectFor(String mittOrDest) {
		mittOrDest = mittOrDest.replaceAll("\n", " ");

		if (mittOrDest.length() > MITT_DEST_LENGTH)
			return mittOrDest.substring(0, MITT_DEST_LENGTH) + "...";
		else
			return mittOrDest;
	}

	private static String getOggettoSubjectFor(String oggetto) {
		oggetto = oggetto.replaceAll("\n", " ");
		if (oggetto.length() > OGGETTO_LENGTH)
			return oggetto.substring(0, OGGETTO_LENGTH) + "...";
		else
			return oggetto;
	} 	

}
