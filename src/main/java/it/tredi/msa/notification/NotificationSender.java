package it.tredi.msa.notification;

public abstract class NotificationSender {
	
	private boolean notifyRemainingError;
	
	public abstract boolean notifyError(String message) throws Exception;

	public boolean isNotifyRemainingError() {
		return notifyRemainingError;
	}

	public void setNotifyRemainingError(String notifyRemainingError) {
		this.setNotifyRemainingError(Boolean.parseBoolean(notifyRemainingError));
	}
	
	public void setNotifyRemainingError(boolean notifyRemainingError) {
		this.notifyRemainingError = notifyRemainingError;
	}	
	
}
