package ir.tredi.msa.configuration;

import it.tredi.msa.entity.MSAConfiguration;
import it.tredi.utils.properties.PropertiesReader;

public class MSAConfigurationReader {
	
	private final static String PROPERTIES_FILENAME = "it.tredi.msa.properties";

	public MSAConfiguration read() {
		try {
			PropertiesReader propertiesReader = new PropertiesReader(PROPERTIES_FILENAME);
			
			MSAConfiguration msaConfiguration = new MSAConfiguration();
			
			return msaConfiguration;			
		}
		catch (Exception e) {
//TODO - generare una ConfigurationException indicando l'errore esatto
		}
		return null;

	}

}
