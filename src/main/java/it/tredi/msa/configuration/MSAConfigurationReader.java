package it.tredi.msa.configuration;

import it.tredi.msa.ObjectFactoryConfiguration;
import it.tredi.utils.properties.PropertiesReader;

public class MSAConfigurationReader {
	
	private final static String PROPERTIES_FILENAME = "it.tredi.msa.properties";
	private final static String AUDIT_WRITER_PROPERTY = "audit.writer";
	private final static String NOTIFICATION_SENDER__PROPERTY = "notification.sender";
	private final static String MAILBOXCONFIGURATION_READERS_PROPERTY = "mailboxconfiguration.readers";
	private final static String MAILBOXMANAGERS_DELAY_PROPERTY = "mailboxmanagers.delay";
	private final static String MAILBOXMANAGERS_POOLSIZE_PROPERTY = "mailboxmanagers.poolsize";
	private final static String MAILBOXMANAGERS_REFRESHTIME_PROPERTY = "mailboxmanagers.refresh.time";
	private final static String MAILBOXMANAGERS_HOT_RELOADING_PROPERTY = "mailboxmanagers.hot-reloading";
	public final static String MAILBOXMANAGERS_ALLOW_EMAIL_DUPLICATES_PROPERTY = "mailboxmanagers.allow-email-duplicates";

	public MSAConfiguration read() throws Exception {
		PropertiesReader propertiesReader = new PropertiesReader(PROPERTIES_FILENAME);
		
		MSAConfiguration msaConfiguration = new MSAConfiguration();
		
		//raw data
		msaConfiguration.setRawData(propertiesReader);

		//default params
		msaConfiguration.setMailboxManagersDelay(propertiesReader.getIntProperty(MAILBOXMANAGERS_DELAY_PROPERTY, 600));
		msaConfiguration.setMailboxManagersPoolsize(propertiesReader.getIntProperty(MAILBOXMANAGERS_POOLSIZE_PROPERTY, 1));
		msaConfiguration.setMailboxManagersRefreshTime(propertiesReader.getIntProperty(MAILBOXMANAGERS_REFRESHTIME_PROPERTY, 1));
		msaConfiguration.setAllowEmailDuplicates(propertiesReader.getBooleanProperty(MAILBOXMANAGERS_ALLOW_EMAIL_DUPLICATES_PROPERTY, false));
		msaConfiguration.setMailboxManagersHotReloading(propertiesReader.getBooleanProperty(MAILBOXMANAGERS_HOT_RELOADING_PROPERTY, false));
		
		//AuditWriter configuration
		msaConfiguration.setAuditWriterConfiguration(readConfiguration(propertiesReader, AUDIT_WRITER_PROPERTY));
		
		//NotificationSender configuration
		String notificationSender = propertiesReader.getProperty(NOTIFICATION_SENDER__PROPERTY, "");
		if (notificationSender.isEmpty())
			msaConfiguration.setNotificationSenderConfiguration(new ObjectFactoryConfiguration("it.tredi.msa.notification.DummySender", ""));
		else
			msaConfiguration.setNotificationSenderConfiguration(readConfiguration(propertiesReader, NOTIFICATION_SENDER__PROPERTY));

		//MailboxConfigurationReader(s) configuration
		msaConfiguration.setMailboxConfigurationReadersConfiguration(readConfigurations(propertiesReader, MAILBOXCONFIGURATION_READERS_PROPERTY));
		
		return msaConfiguration;			
	}
	
	private ObjectFactoryConfiguration readConfiguration(PropertiesReader propertiesReader, String property) throws Exception {
		return this.readConfigurations(propertiesReader, property)[0];
	}
	
	private ObjectFactoryConfiguration []readConfigurations(PropertiesReader propertiesReader, String property) throws Exception {
		String s_names = propertiesReader.getProperty(property, "");
		if (s_names.isEmpty())
			throw new Exception("Wrong MSA configuration. Property not found or empty: " + property);
		String []names = s_names.split(",");
		ObjectFactoryConfiguration []configurations = new ObjectFactoryConfiguration[names.length];
		int i = 0;
		for (String name:names) {
			name = name.trim();
			if (name.isEmpty())
				throw new Exception("Wrong MSA configuration. Property not configured correctly: " + property);
			String className = propertiesReader.getProperty(name + ".class", "");
			if (className.isEmpty())
				throw new Exception("Wrong MSA configuration. Property not found or empty: " + name + ".class");
			String params = propertiesReader.getProperty(name + ".params", "");
			if (params.isEmpty())
				throw new Exception("Wrong MSA configuration. Property not found or empty: " + name + ".params");
			configurations[i++] = new ObjectFactoryConfiguration(className, params);
		}
		return configurations;
	}

}

