package it.tredi.msa.test.misc;

import java.io.StringWriter;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

public class Main {
	
	public static void main(String []args) throws Exception {
		
		Element rootEl = DocumentHelper.createElement("doc");
		Document document = DocumentHelper.createDocument(rootEl);
		Element noteEl = DocumentHelper.createElement("note");
		rootEl.add(noteEl);
		noteEl.addAttribute("xml:space", "preserve");
		noteEl.setText("prima riga\ndeconda riga\n\tterza riga");
		
		System.out.println(document.asXML());
		
		System.out.println(print1(document));
		
		System.out.println(print2(document));
		
	}

	private static String print1(Document xmlDocument) throws Exception {
		StringWriter sw = new StringWriter();
		OutputFormat outformat = OutputFormat.createPrettyPrint();
		outformat.setEncoding("UTF-8");
	    XMLWriter writer = new XMLWriter(sw, outformat);
	    writer.write(xmlDocument);
	    return sw.toString();		
	}

	private static String print2(Document document) throws Exception {
	    StringWriter sw = new StringWriter();
	    OutputFormat eidon = new OutputFormat("  ",true,"UTF-8");
	    XMLWriter coreWriter = new XMLWriter(sw,eidon);
        coreWriter.write(document);
	    sw.flush();
	    return sw.toString();
	}	

	
}
