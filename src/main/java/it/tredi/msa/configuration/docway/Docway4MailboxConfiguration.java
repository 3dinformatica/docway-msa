package it.tredi.msa.configuration.docway;

public class Docway4MailboxConfiguration extends DocwayMailboxConfiguration {
	
	public final static int DEFAULT_XW_LOCK_OP_ATTEMPTS = 10;
	public final static long DEFAULT_XW_LOCK_OP_DELAY = 6000;
	
	//xw
	private String xwHost;
	private int xwPort;
	private String xwUser;
	private String xwPassword;
	private String xwDb;
	private String aclDb;
	private int xwLockOpAttempts;
	private long xwLockOpDelay;
	
	public String getXwHost() {
		return xwHost;
	}
	
	public void setXwHost(String xwHost) {
		this.xwHost = xwHost;
	}
	
	public int getXwPort() {
		return xwPort;
	}
	
	public void setXwPort(int xwPort) {
		this.xwPort = xwPort;
	}
	
	public String getXwUser() {
		return xwUser;
	}
	
	public void setXwUser(String xwUser) {
		this.xwUser = xwUser;
	}
	
	public String getXwPassword() {
		return xwPassword;
	}
	
	public void setXwPassword(String xwPassword) {
		this.xwPassword = xwPassword;
	}
	
	public String getXwDb() {
		return xwDb;
	}
	
	public void setXwDb(String xwDb) {
		this.xwDb = xwDb;
	}
	
	public String getAclDb() {
		return aclDb;
	}
	
	public void setAclDb(String aclDb) {
		this.aclDb = aclDb;
	}

	public int getXwLockOpAttempts() {
		return xwLockOpAttempts;
	}

	public void setXwLockOpAttempts(int xwLockOpAttempts) {
		this.xwLockOpAttempts = xwLockOpAttempts;
	}

	public long getXwLockOpDelay() {
		return xwLockOpDelay;
	}

	public void setXwLockOpDelay(long xwLockOpDelay) {
		this.xwLockOpDelay = xwLockOpDelay;
	}

}
