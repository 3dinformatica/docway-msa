package it.tredi.msa;

import java.io.IOException;
import java.io.StringWriter;

import org.dom4j.Document;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;


public class Utils {
	
	public static String dom4jdocumentToString(Document xmlDocument, String encoding, boolean omitDeclaration) throws IOException {
		StringWriter sw = new StringWriter();
		OutputFormat outformat = new OutputFormat("  ", true, encoding);
		outformat.setEncoding(encoding);
        XMLWriter writer = new XMLWriter(sw, outformat);
        writer.write(omitDeclaration? xmlDocument.getRootElement() : xmlDocument);
        return sw.toString();	
	}

}
