package it.tredi.msa.test.misc;


public class ExecutorHandlerTest implements Runnable {

	private ExecutorTest exec;

	public ExecutorHandlerTest(ExecutorTest ex) {
		// TODO Auto-generated constructor stub
		this.exec = ex;
	}

	public void sleeper(ExecutorTest ex) {
		// TODO Auto-generated method stub
		try {
			Thread.sleep(3000); 
			ex.shutdown();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		sleeper(exec);
	}
    
	
}
