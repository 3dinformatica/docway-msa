package ir.tredi.msa.configuration;

import it.tredi.msa.ObjectFactoryConfiguration;
import it.tredi.msa.entity.MSAConfiguration;
import it.tredi.utils.properties.PropertiesReader;

public class MSAConfigurationReader {
	
	private final static String PROPERTIES_FILENAME = "it.tredi.msa.properties";
	private final static String AUDIT_WRITER_PROPERTY = "audit.writer";

	public MSAConfiguration read() throws Exception {
		//try {
			PropertiesReader propertiesReader = new PropertiesReader(PROPERTIES_FILENAME);
			
			MSAConfiguration msaConfiguration = new MSAConfiguration();
			
			//AuditWriter configuration
			msaConfiguration.setAuditWriterConfiguration(readConfiguration(propertiesReader, AUDIT_WRITER_PROPERTY));
			
			return msaConfiguration;			
		//}
		//catch (Exception e) {
//TODO - generare una ConfigurationException indicando l'errore esatto
		//}
		//return null;

	}
	
	public ObjectFactoryConfiguration readConfiguration(PropertiesReader propertiesReader, String property) throws Exception {
		String name = propertiesReader.getProperty(property, "");
		if (name.isEmpty())
			throw new Exception("Wrong MSA configuration. Property not found or empty: " + property);
		String className = propertiesReader.getProperty(name + ".class", "");
		if (className.isEmpty())
			throw new Exception("Wrong MSA configuration. Property not found or empty: " + name + ".class");
		String params = propertiesReader.getProperty(name + ".params", "");
		if (className.isEmpty())
			throw new Exception("Wrong MSA configuration. Property not found or empty: " + name + ".params");
		return new ObjectFactoryConfiguration(className, params);
	}

}

