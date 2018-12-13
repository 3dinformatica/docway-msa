package it.tredi.msa.test.misc;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Tester extends Thread {
	
	public static void main(String[] args) throws Exception {

		try {

			ScheduledExecutorService executor = Executors.newScheduledThreadPool(5);
		
            ExecutorTest test1 = new ExecutorTest("1");
            ExecutorTest test2 = new ExecutorTest("2");
            ExecutorTest test3 = new ExecutorTest("3");
            
            executor.scheduleWithFixedDelay(test1, 0, 1, TimeUnit.SECONDS);
            executor.scheduleWithFixedDelay(test2, 0, 1, TimeUnit.SECONDS);
            executor.scheduleWithFixedDelay(test3, 0, 1, TimeUnit.SECONDS);
               
            ExecutorHandlerTest sleep = new ExecutorHandlerTest(test2);
            
            executor.scheduleWithFixedDelay(sleep, 0, 5, TimeUnit.SECONDS);
           
            
            executor.awaitTermination(5, TimeUnit.SECONDS);
            executor.shutdown();
        
    } catch (Exception e) {
    	throw e;
    }
	
	}

}
