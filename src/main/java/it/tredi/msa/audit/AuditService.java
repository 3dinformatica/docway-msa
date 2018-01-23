package it.tredi.msa.audit;

import it.tredi.msa.ObjectFactory;
import it.tredi.msa.Services;

public class AuditService {
	
	private static AuditService instance;
	
	private AuditWriter auditWriter;
	
	private AuditService() {
	}

	public static synchronized AuditService getInstance() {
	    if (instance == null) {
	        instance = new AuditService();
	    }
	    return instance;
	}

	public void init() {
		auditWriter = ObjectFactory.createAuditWriter(Services.getConfigurationService().getMSAConfiguration().getAuditWriterConfiguration());
	}

	

}



