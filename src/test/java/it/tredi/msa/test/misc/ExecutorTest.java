package it.tredi.msa.test.misc;

public class ExecutorTest implements Runnable {

	private String command;
	private boolean shutdown = false;

	public ExecutorTest (String s) {

		this.command = s;
	}

	@Override

	public void run() {

		if (!shutdown) {
			System.out.println(Thread.currentThread().getName() + " start. Command = " + command);
			//processCommand();
			System.out.println(Thread.currentThread().getName()+" End.");
		}
	}


	private void processCommand() {
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public void shutdown() {
		shutdown = true;
		Thread.currentThread().interrupt();
	}

	@Override
	public String toString(){
		return this.command;
	}
}
