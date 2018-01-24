package ir.tredi.msa.configuration;

import it.tredi.msa.ObjectFactoryConfiguration;
import it.tredi.msa.entity.MSAConfiguration;
import it.tredi.utils.properties.PropertiesReader;

public class MSAConfigurationReader {
	
	private final static String PROPERTIES_FILENAME = "it.tredi.msa.properties";
	private final static String AUDIT_WRITER_PROPERTY = "audit.writer";
	private final static String NOTIFICATION_SENDER__PROPERTY = "notification.sender";
	private final static String MAILBOXCONFIGURATION_READERS_PROPERTY = "mailboxconfiguration.readers";

	public MSAConfiguration read() throws Exception {
		//try {
			PropertiesReader propertiesReader = new PropertiesReader(PROPERTIES_FILENAME);
			
			MSAConfiguration msaConfiguration = new MSAConfiguration();
			
			//AuditWriter configuration
			msaConfiguration.setAuditWriterConfiguration(readConfiguration(propertiesReader, AUDIT_WRITER_PROPERTY));
			
			//NotificationSender configuration
			msaConfiguration.setNotificationSenderConfiguration(readConfiguration(propertiesReader, NOTIFICATION_SENDER__PROPERTY));
			
			//MailboxConfigurationReader(s) configuration
			msaConfiguration.setMailboxConfigurationReadersConfiguration(readConfigurations(propertiesReader, MAILBOXCONFIGURATION_READERS_PROPERTY));
			
			return msaConfiguration;			
		//}
		//catch (Exception e) {
//TODO - generare una ConfigurationException indicando l'errore esatto
		//}
		//return null;

	}
	
	public ObjectFactoryConfiguration readConfiguration(PropertiesReader propertiesReader, String property) throws Exception {
		return this.readConfigurations(propertiesReader, property)[0];
	}
	
	public ObjectFactoryConfiguration []readConfigurations(PropertiesReader propertiesReader, String property) throws Exception {
		String s_names = propertiesReader.getProperty(property, "");
		if (s_names.isEmpty())
			throw new Exception("Wrong MSA configuration. Property not found or empty: " + property);
		String []names = s_names.split(",");
		ObjectFactoryConfiguration []configurations = new ObjectFactoryConfiguration[names.length];
		int i = 0;
		for (String name:names) {
			name = name.trim();
			if (name.isEmpty())
				throw new Exception("Wrong MSA configuration. Property not found or empty: " + property);
			String className = propertiesReader.getProperty(name + ".class", "");
			if (className.isEmpty())
				throw new Exception("Wrong MSA configuration. Property not found or empty: " + name + ".class");
			String params = propertiesReader.getProperty(name + ".params", "");
			if (className.isEmpty())
				throw new Exception("Wrong MSA configuration. Property not found or empty: " + name + ".params");
			configurations[i++] = new ObjectFactoryConfiguration(className, params);
		}
		return configurations;
	}

}

