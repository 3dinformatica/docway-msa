package it.tredi.msa.audit;

public abstract class AuditWriter {
	
	private boolean full;
	
	public abstract void writeAudit(String message);

	public boolean isFull() {
		return full;
	}

	public void setFull(String full) {
		this.setFull(Boolean.parseBoolean(full));
	}	
	
	public void setFull(boolean full) {
		this.full = full;
	}
	
}
