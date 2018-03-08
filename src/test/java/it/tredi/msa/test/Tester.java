package it.tredi.msa.test;

import java.util.concurrent.ExecutorService;
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

			Thread.sleep(3000); 

			test2.shutdown();

			executor.awaitTermination(99999, TimeUnit.SECONDS);            
			executor.shutdown();

		} 
		catch (Exception e) {
			throw e;
		}

	}

}
